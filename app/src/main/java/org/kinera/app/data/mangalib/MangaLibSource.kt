package org.kinera.app.data.mangalib

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.mangalib.dto.MangaLibChapter
import org.kinera.app.data.mangalib.dto.MangaLibConstants
import org.kinera.app.data.mangalib.dto.MangaLibImageServer
import org.kinera.app.data.mangalib.dto.MangaLibManga
import org.kinera.app.data.mangalib.dto.MangaLibMangaShort
import java.time.OffsetDateTime

/**
 * Catalog source for MangaLib (https://mangalib.me) — the largest Russian
 * manga hosting. The app talks to the public JSON API at api.mangalib.me;
 * no authorization is needed for reading.
 *
 * Manga ids keep the API "slug_url" ("<id>--<slug>") under the "mlib:" prefix,
 * chapter ids are "mlibch:<slugUrl>/<volume>/<number>/<branchId|->".
 *
 * Page images live on separate CDN servers; their availability differs by
 * region, so the source probes them in order and caches the first that works.
 */
class MangaLibSource(
    private val api: MangaLibApi,
    private val client: OkHttpClient,
) {

    val source: MangaSource = MangaSource.MANGALIB

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        catalog(offset, query = null)

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> {
        val page = offset / PAGE_SIZE + 1
        val response = api.latest(page = page)
        return Paged(
            items = response.data.map { it.toDomain() },
            offset = offset,
            total = -1,
            hasNext = response.meta.hasNextPage,
        )
    }

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        catalog(offset, query = query.takeIf { it.isNotBlank() })

    suspend fun details(mangaId: String): Manga {
        val slug = mangaId.removePrefix(MANGA_ID_PREFIX)
        return api.manga(slug).data.toDomain()
    }

    /**
     * Chapters oldest-first. Paid / restricted branches are hidden. When a
     * title has several translation branches, chapters of the server's
     * default branch win; otherwise the first open branch is used.
     */
    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val slug = mangaId.removePrefix(MANGA_ID_PREFIX)
        val chapters = api.chapters(slug).data
        if (chapters.isEmpty()) return emptyList()
        val multiBranch = chapters.any { it.branchesCount > 1 }
        val defaultBranchId = if (multiBranch) {
            val numericId = slug.substringBefore("--").toLongOrNull()
            runCatching { numericId?.let { api.branches(it).data.firstOrNull()?.id } }.getOrNull()
        } else {
            null
        }
        return chapters.mapNotNull { it.toDomain(slug, defaultBranchId) }
            // the API lists newest first, the app expects oldest first
            .reversed()
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val parsed = parseChapterId(chapterId)
            ?: error("Malformed MangaLib chapter id: $chapterId")
        val response = api.pages(
            slugUrl = parsed.slugUrl,
            volume = parsed.volume,
            number = parsed.number,
            branchId = parsed.branchId,
        )
        val relative = response.data.pages
            .filter { it.url.isNotBlank() }
            .sortedBy { it.slug }
        check(relative.isNotEmpty()) { "Chapter has no readable pages" }
        val server = resolveImageServer(relative.first().url)
        return relative.mapIndexed { index, page ->
            MangaPage(url = server + page.url, index = index)
        }
    }

    /* ----- catalog ----- */

    private suspend fun catalog(offset: Int, query: String?): Paged<Manga> {
        val page = offset / PAGE_SIZE + 1
        val response = api.catalog(page = page, query = query)
        return Paged(
            items = response.data.map { it.toDomain() },
            offset = offset,
            total = -1,
            hasNext = response.meta.hasNextPage,
        )
    }

    /* ----- mapping ----- */

    private fun MangaLibMangaShort.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + slugUrl,
        source = source,
        title = rusName ?: engName ?: name ?: "Untitled",
        altTitle = engName ?: name,
        coverUrl = cover.default ?: cover.thumbnail,
        state = MangaState.UNKNOWN,
    )

    private fun MangaLibManga.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + slugUrl,
        source = source,
        title = rusName ?: engName ?: name ?: "Untitled",
        altTitle = engName ?: name,
        coverUrl = cover.default ?: cover.thumbnail,
        description = summaryText,
        tags = (genres + tags)
            .mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }
            .distinct()
            .map { MangaTag(id = it, title = it) },
        state = mapState(status?.label),
        author = authors.firstOrNull()?.name ?: artists.firstOrNull()?.name,
        chaptersCount = 0,
    )

    private fun MangaLibChapter.toDomain(slug: String, defaultBranchId: Long?): MangaChapter? {
        val open = branches.filter { it.isOpen }
        if (open.isEmpty()) return null
        val branch = open.firstOrNull { it.branchId == defaultBranchId }
            ?: open.firstOrNull { it.branchId != null }
            ?: open.first()
        return MangaChapter(
            id = chapterId(slug, this, branch.branchId),
            source = source,
            number = number?.takeIf { it.isNotBlank() },
            title = name?.takeIf { it.isNotBlank() },
            volume = volume?.takeIf { it.isNotBlank() },
            scanlator = branch.teams.firstOrNull()?.name,
            uploadedAt = parseDate(branch.createdAt),
            pagesCount = 0,
        )
    }

    private fun mapState(label: String?): MangaState = when (label) {
        "Продолжается" -> MangaState.ONGOING
        "Завершён" -> MangaState.FINISHED
        "Заморожен" -> MangaState.PAUSED
        "Заброшен" -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun parseDate(raw: String?): Long = runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrDefault(0L)

    /* ----- image servers ----- */

    @Volatile
    private var imageServerCache: String? = null

    @Volatile
    private var constantsCache: MangaLibConstants? = null

    /**
     * Picks the CDN prefix for page images. Servers are tried in the order
     * the community parser uses (main, secondary, compress) and the first
     * one that actually serves the given image wins; the result is cached
     * for the lifetime of the process.
     */
    private suspend fun resolveImageServer(sampleRelativeUrl: String): String {
        imageServerCache?.let { return it }
        val candidates = imageServers()
        if (candidates.isEmpty()) error("MangaLib image servers unavailable")
        val chosen = candidates.firstOrNull { server -> probeImage(server + sampleRelativeUrl) }
            ?: candidates.first()
        imageServerCache = chosen
        return chosen
    }

    private suspend fun imageServers(): List<String> {
        val constants = runCatching {
            constantsCache ?: api.constants().data.also { constantsCache = it }
        }.getOrNull()
        val fromConstants = constants?.let(::serverUrls).orEmpty()
        if (fromConstants.isNotEmpty()) return fromConstants
        // live-verified fallback when the constants endpoint is down
        return FALLBACK_SERVERS
    }

    private fun serverUrls(constants: MangaLibConstants): List<String> {
        val bySite = constants.imageServers.filter { 1 in it.siteIds }
        val ordered = SERVER_ORDER.mapNotNull { id -> bySite.firstOrNull { it.id == id } }
        val rest = bySite.filter { it.id !in SERVER_ORDER }
        return (ordered + rest).mapNotNull { it.usableUrl() }
    }

    private fun MangaLibImageServer.usableUrl(): String? =
        url.takeIf { it.startsWith("http") }?.trimEnd('/')

    private suspend fun probeImage(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(
                Request.Builder().url(url).head().build(),
            ).execute().use { response ->
                response.isSuccessful && response.header("content-length", "0")?.toIntOrNull() != 0
            }
        }.getOrDefault(false)
    }

    companion object {
        const val PAGE_SIZE = 24
        const val MANGA_ID_PREFIX = "mlib:"
        const val CHAPTER_ID_PREFIX = "mlibch:"

        private val SERVER_ORDER = listOf("main", "secondary", "compress")
        private val FALLBACK_SERVERS = listOf(
            "https://img2.imglib.info",
            "https://img3.cdnrulib.link",
            "https://img3.cdnlibs.org",
        )

        /** Builds "mlibch:29916--slug/5/214/4296102" ("-" for the default branch). */
        fun chapterId(slugUrl: String, chapter: MangaLibChapter, branchId: Long?): String =
            CHAPTER_ID_PREFIX + slugUrl + "/" +
                chapter.volume.orEmpty() + "/" +
                chapter.number.orEmpty() + "/" +
                (branchId?.toString() ?: "-")

        /** Splits "mlibch:29916--slug/5/214/4296102" back into its parts. */
        fun parseChapterId(chapterId: String): ParsedChapter? {
            val rest = chapterId.removePrefix(CHAPTER_ID_PREFIX)
            val parts = rest.split('/')
            if (parts.size != 4) return null
            val (slug, volume, number, branch) = parts
            if (slug.isBlank()) return null
            return ParsedChapter(slug, volume, number, branch.toLongOrNull())
        }

        data class ParsedChapter(
            val slugUrl: String,
            val volume: String,
            val number: String,
            val branchId: Long?,
        )
    }
}
