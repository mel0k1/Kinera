package org.kinera.app.data.remanga

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.remanga.dto.RemangaChapter
import org.kinera.app.data.remanga.dto.RemangaCover
import org.kinera.app.data.remanga.dto.RemangaGenre
import org.kinera.app.data.remanga.dto.RemangaManga
import org.kinera.app.data.remanga.dto.RemangaMangaShort
import org.kinera.app.data.settings.SettingsRepository
import java.time.OffsetDateTime

/**
 * Catalog source for Remanga (https://remanga.org) — a large Russian manga
 * hosting. Reading needs no authorization; the public API lives at
 * api.remanga.org.
 *
 * Manga ids keep the API "dir" slug under the "rm:" prefix, chapter ids are
 * "rmch:<chapterId>". Chapters are fetched for the first translation branch
 * (most titles have exactly one); paid chapters that were not bought are
 * hidden.
 *
 * The catalog honors the genre / status filters stored in Settings (empty
 * sets mean "everything"); they apply to Popular and to search, not to the
 * latest-updates feed, which the API does not filter.
 */
class RemangaSource(
    private val api: RemangaApi,
    private val client: OkHttpClient,
    private val settings: SettingsRepository,
) {

    val source: MangaSource = MangaSource.REMANGA

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        catalog(offset, ordering = RemangaApi.ORDER_RATING)

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        catalog(offset, ordering = RemangaApi.ORDER_UPDATED)

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> {
        if (query.isBlank()) return popular(offset, limit)
        val page = offset / PAGE_SIZE + 1
        val filters = settings.snapshot.value
        val response = api.search(
            page = page,
            query = query,
            genres = filters.remangaGenres.map { it.toString() }.takeIf { it.isNotEmpty() },
            statuses = filters.remangaStatuses.toList().takeIf { it.isNotEmpty() },
        )
        return Paged(
            items = response.results.map { it.toDomain() },
            offset = offset,
            total = response.count,
            hasNext = response.next != null,
        )
    }

    /** One page of the rated catalog, newest-updated or top-rated. */
    private suspend fun catalog(offset: Int, ordering: String): Paged<Manga> {
        val page = offset / PAGE_SIZE + 1
        val filters = settings.snapshot.value
        val response = api.catalog(
            page = page,
            ordering = ordering,
            genres = filters.remangaGenres.map { it.toString() }.takeIf { it.isNotEmpty() },
            statuses = filters.remangaStatuses.toList().takeIf { it.isNotEmpty() },
        )
        return Paged(
            items = response.results.map { it.toDomain() },
            offset = offset,
            total = response.count,
            hasNext = response.next != null,
        )
    }

    /* ----- catalog ----- */

    /**
     * Genres available on the Remanga site, alphabetically; used by the
     * filter sheet in Explore. Cached for the lifetime of the process.
     */
    suspend fun genres(): List<RemangaGenre> {
        genreCache?.let { return it }
        val list = runCatching { api.genreForms().content.genres }
            .getOrNull()
            ?.filter { it.name.isNotBlank() }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
        if (list.isNotEmpty()) genreCache = list
        return list
    }

    suspend fun details(mangaId: String): Manga {
        val dir = mangaId.removePrefix(MANGA_ID_PREFIX)
        return api.manga(dir).toDomain()
    }

    /**
     * Chapters oldest-first. The site lists them newest first per branch,
     * so the result is reversed after fetching all pages of the branch.
     */
    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val dir = mangaId.removePrefix(MANGA_ID_PREFIX)
        val details = api.manga(dir)
        val branch = details.branches.firstOrNull()
            ?: return emptyList()
        val chapters = mutableListOf<RemangaChapter>()
        var page = 1
        while (true) {
            val response = api.chapters(branchId = branch.id, page = page)
            chapters += response.results
            if (response.next == null || response.results.isEmpty()) break
            page++
            if (page > MAX_CHAPTER_PAGES) break // safety valve
        }
        return chapters
            .filter { !it.is_paid || it.is_bought }
            .map { it.toDomain() }
            .reversed()
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val numeric = chapterId.removePrefix(CHAPTER_ID_PREFIX).toLongOrNull()
            ?: error("Malformed Remanga chapter id: $chapterId")
        val response = api.pages(numeric)
        val links = response.pages.flatMap { element ->
            when (element) {
                is JsonObject -> listOfNotNull(element["link"]?.jsonPrimitive?.content)
                is JsonArray -> element.mapNotNull { item ->
                    (item as? JsonObject)?.get("link")?.jsonPrimitive?.content
                }
                else -> emptyList()
            }
        }.filter { it.isNotBlank() }
        check(links.isNotEmpty()) { "Chapter has no readable pages" }
        return links.mapIndexed { index, link ->
            MangaPage(url = absolutize(link), index = index)
        }
    }

    /* ----- mapping ----- */

    private fun RemangaMangaShort.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + dir,
        source = source,
        title = main_name,
        altTitle = secondary_name,
        coverUrl = cover.absolutize(),
        tags = genres.map { MangaTag(id = it.id.toString(), title = it.name) },
        state = mapState(status?.id),
    )

    private fun RemangaManga.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + dir,
        source = source,
        title = main_name,
        altTitle = secondary_name ?: another_name,
        coverUrl = cover.absolutize(),
        description = description,
        tags = genres.map { MangaTag(id = it.id.toString(), title = it.name) },
        state = mapState(status?.id),
        author = branches.firstOrNull()?.publishers?.firstOrNull()?.name,
        chaptersCount = count_chapters,
    )

    private fun RemangaChapter.toDomain(): MangaChapter = MangaChapter(
        id = CHAPTER_ID_PREFIX + id,
        source = source,
        number = chapter?.let(::formatNumber),
        title = name?.takeIf { it.isNotBlank() },
        volume = tome?.toString(),
        scanlator = publishers.firstOrNull()?.name,
        uploadedAt = parseDate(upload_date),
        pagesCount = 0,
    )

    /**
     * Remanga status ids: 1 finished, 2 ongoing, 3 paused, 4 abandoned,
     * 5 upcoming, 6 restricted. The app state enum keeps the first four.
     */
    private fun mapState(statusId: Int?): MangaState = when (statusId) {
        1 -> MangaState.FINISHED
        2 -> MangaState.ONGOING
        3 -> MangaState.PAUSED
        4 -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun RemangaCover?.absolutize(): String? =
        this?.let { (it.mid ?: it.high ?: it.low)?.let(::absolutize) }

    private fun absolutize(url: String): String =
        if (url.startsWith("http")) url else API_BASE_URL.trimEnd('/') + url

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun parseDate(raw: String?): Long = runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrDefault(0L)

    @Volatile
    private var genreCache: List<RemangaGenre>? = null

    companion object {
        const val PAGE_SIZE = 20
        const val API_BASE_URL = "https://api.remanga.org/"
        const val MANGA_ID_PREFIX = "rm:"
        const val CHAPTER_ID_PREFIX = "rmch:"
        private const val MAX_CHAPTER_PAGES = 20
    }
}
