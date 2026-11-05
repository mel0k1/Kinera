package org.kinera.app.data.desume

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.desume.dto.DesuMeChapter
import org.kinera.app.data.desume.dto.DesuMeManga
import org.kinera.app.data.desume.dto.DesuMeMangaShort
import org.kinera.app.data.settings.SettingsRepository
import java.net.HttpURLConnection
import java.net.URL

/**
 * Catalog source for Desu.me (https://desu.me) — a Russian anime & manga
 * portal. Manga ids keep the numeric API id under the "desu:" prefix,
 * chapter ids are "desuch:<chapterId>".
 *
 * The portal migrates between mirrors every few months, so the first call
 * probes the known domains in order and the first one that answers wins;
 * the choice is cached for the lifetime of the process.
 *
 * The catalog honors the genre filter stored in Settings (empty set means
 * "everything"). The API has no server-side status filter, so only genres
 * are offered.
 */
class DesuMeSource(
    private val api: DesuMeApi,
    private val client: OkHttpClient,
    private val settings: SettingsRepository,
) {

    val source: MangaSource = MangaSource.DESUME

    private val domainMutex = Mutex()

    @Volatile
    private var resolvedDomain: String? = null

    @Volatile
    private var genreCache: List<DesuMeGenreOption>? = null

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, order = ORDER_POPULAR, query = null)

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, order = ORDER_UPDATED, query = null)

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, order = ORDER_POPULAR, query = query.takeIf { it.isNotBlank() })

    suspend fun details(mangaId: String): Manga {
        val numeric = mangaId.removePrefix(MANGA_ID_PREFIX)
        return api.manga(mangaUrl(numeric)).response.toDomain()
    }

    /** Chapters oldest-first; the API lists them newest first. */
    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val numeric = mangaId.removePrefix(MANGA_ID_PREFIX)
        val details = api.manga(mangaUrl(numeric)).response
        val chapters = details.chapters?.list.orEmpty()
        return chapters
            .map { it.toDomain(numeric) }
            .reversed()
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val parsed = parseChapterId(chapterId)
            ?: error("Malformed Desu.me chapter id: $chapterId")
        val response = api.pages(chapterUrl(parsed.mangaId, parsed.chapterId)).response
        val pages = response.list.filter { it.img.isNotBlank() }
        check(pages.isNotEmpty()) { "Chapter has no readable pages" }
        return pages.mapIndexed { index, page ->
            MangaPage(url = absolutize(page.img), index = index)
        }
    }

    /* ----- catalog ----- */

    private suspend fun list(offset: Int, order: String, query: String?): Paged<Manga> {
        val domain = domain()
        val page = offset / PAGE_SIZE + 1
        val genres = settings.snapshot.value.desumeGenres
            .filter { it.isNotBlank() }
            .joinToString(",")
        val url = buildString {
            append("https://").append(domain).append("/manga/api/?limit=").append(PAGE_SIZE)
            append("&order=").append(order)
            append("&page=").append(page)
            if (genres.isNotEmpty()) append("&genres=").append(urlEncode(genres))
            if (query != null) append("&search=").append(urlEncode(query))
        }
        val response = api.catalog(url)
        return Paged(
            items = response.response.map { it.toDomain() },
            offset = offset,
            total = -1,
            // The envelope carries no total; rely on a shorter page.
            hasNext = response.response.size >= PAGE_SIZE,
        )
    }

    /**
     * Genres offered by the catalog filter on the site. The API has no JSON
     * endpoint for them, so the source reads the filter block of the manga
     * page the same way the reference Kotatsu parser does; result is cached
     * for the lifetime of the process.
     */
    suspend fun genres(): List<DesuMeGenreOption> {
        genreCache?.let { return it }
        val html = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder()
                        .url("https://${domain()}/manga/")
                        .build(),
                ).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.string()
                }
            }.getOrNull()
        }.orEmpty()
        val list = INPUT_TAG_REGEX.findAll(html)
            .mapNotNull { match ->
                val tag = match.value
                val slug = SLUG_ATTR_REGEX.find(tag)?.groupValues?.get(1).orEmpty()
                val name = NAME_ATTR_REGEX.find(tag)?.groupValues?.get(1).orEmpty()
                if (slug.isBlank() || name.isBlank()) {
                    null
                } else {
                    DesuMeGenreOption(slug = slug, name = name)
                }
            }
            .distinctBy { it.slug }
            .sortedBy { it.name.lowercase() }
            .toList()
        if (list.isNotEmpty()) genreCache = list
        return list
    }

    /* ----- domain resolution ----- */

    /**
     * Returns the mirror that currently answers. The probe is a cheap HEAD
     * against the catalog endpoint with a short timeout; failures fall
     * through to the next candidate.
     */
    private suspend fun domain(): String {
        resolvedDomain?.let { return it }
        return domainMutex.withLock {
            resolvedDomain ?: DOMAIN_CANDIDATES.firstOrNull { probe(it) }?.also {
                resolvedDomain = it
            } ?: DOMAIN_CANDIDATES.first()
        }
    }

    private suspend fun probe(domain: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://$domain/manga/api/?limit=1&page=1&order=popular")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = PROBE_TIMEOUT_MS
            connection.readTimeout = PROBE_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            try {
                connection.responseCode in 200..299
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(false)
    }

    /* ----- mapping ----- */

    private fun DesuMeMangaShort.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + id,
        source = source,
        title = russian ?: name ?: "Untitled",
        altTitle = name,
        coverUrl = image?.preview ?: image?.original,
        description = description,
        state = mapState(status),
    )

    private fun DesuMeManga.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + id,
        source = source,
        title = russian ?: name ?: "Untitled",
        altTitle = name,
        coverUrl = image?.preview ?: image?.original,
        description = description,
        tags = genres
            .filter { !it.russian.isNullOrBlank() }
            .map { MangaTag(id = it.text ?: it.russian!!, title = it.russian!!) }
            .distinctBy { it.id },
        state = mapState(status),
        chaptersCount = chapters?.list?.size ?: 0,
    )

    private fun DesuMeChapter.toDomain(mangaNumericId: String): MangaChapter = MangaChapter(
        id = chapterId(mangaNumericId, id),
        source = source,
        number = ch?.let(::formatNumber),
        title = title?.takeIf { it.isNotBlank() },
        volume = vol?.takeIf { it > 0 }?.toString(),
        scanlator = null,
        uploadedAt = date * 1000, // unix seconds → millis
        pagesCount = 0,
    )

    /** Desu.me statuses follow the Shikimori vocabulary. */
    private fun mapState(status: String?): MangaState = when (status?.lowercase()) {
        "ongoing" -> MangaState.ONGOING
        "released" -> MangaState.FINISHED
        "paused" -> MangaState.PAUSED
        "discontinued", "cancelled" -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun absolutize(url: String): String {
        if (url.startsWith("http")) return url
        val domain = resolvedDomain ?: DOMAIN_CANDIDATES.first()
        return "https://$domain$url"
    }

    private fun urlEncode(raw: String): String =
        java.net.URLEncoder.encode(raw, Charsets.UTF_8.name())

    companion object {
        const val PAGE_SIZE = 20
        const val MANGA_ID_PREFIX = "desu:"
        const val CHAPTER_ID_PREFIX = "desuch:"
        const val ORDER_POPULAR = "popular"
        const val ORDER_UPDATED = "updated"
        const val USER_AGENT = "Kinera (github.com/mel0k1/Kinera)"
        private const val PROBE_TIMEOUT_MS = 4_000

        /** desu.me is the canonical host; mirrors follow the Kotatsu parser. */
        private val DOMAIN_CANDIDATES = listOf("desu.me", "x.desu.city", "desu.city")

        /** Filter inputs of the site carry the genre slug and localized name. */
        private val INPUT_TAG_REGEX = Regex("<input[^>]*>", RegexOption.IGNORE_CASE)
        private val SLUG_ATTR_REGEX = Regex("data-genre-slug=\"([^\"]*)\"")
        private val NAME_ATTR_REGEX = Regex("data-genre-name=\"([^\"]*)\"")

        fun mangaUrl(id: String): String = "/manga/api/$id"

        fun chapterUrl(mangaId: String, chapterId: Long): String =
            "/manga/api/$mangaId/chapter/$chapterId"

        /** Builds "desuch:<mangaId>|<chapterId>" — both halves are numeric. */
        fun chapterId(mangaId: String, chapterId: Long): String =
            CHAPTER_ID_PREFIX + mangaId + "|" + chapterId

        /** Splits "desuch:678|12345" back into its parts. */
        fun parseChapterId(chapterId: String): ParsedChapter? {
            val rest = chapterId.removePrefix(CHAPTER_ID_PREFIX)
            val parts = rest.split('|')
            if (parts.size != 2) return null
            val (manga, chapter) = parts
            if (manga.isBlank()) return null
            val chapterNum = chapter.toLongOrNull() ?: return null
            return ParsedChapter(mangaId = manga, chapterId = chapterNum)
        }

        data class ParsedChapter(
            val mangaId: String,
            val chapterId: Long,
        )
    }
}

/** One genre of the Desu.me catalog filter: URL slug plus localized name. */
data class DesuMeGenreOption(
    val slug: String,
    val name: String,
)
