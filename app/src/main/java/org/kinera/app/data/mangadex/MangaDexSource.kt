package org.kinera.app.data.mangadex

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.mangadex.dto.MangaDexChapter
import org.kinera.app.data.mangadex.dto.MangaDexManga
import org.kinera.app.data.settings.SUPPORTED_LANGUAGES
import org.kinera.app.data.settings.SettingsRepository
import java.time.OffsetDateTime

/**
 * Catalog source backed by the official public MangaDex API.
 * The app never hosts content itself; it only talks to api.mangadex.org.
 *
 * Chapter languages follow the user preference: catalog lists only manga
 * with chapters in the enabled languages, the chapter feed only returns
 * those translations. An empty preference means "no filter".
 */
class MangaDexSource(
    private val api: MangaDexApi,
    private val settings: SettingsRepository,
) {

    val source: MangaSource = MangaSource.MANGADEX

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, order = mapOf("followedCount" to "desc"))

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, order = mapOf("latestUploadedChapter" to "desc"))

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, title = query, order = mapOf("relevance" to "desc"))

    suspend fun details(mangaId: String): Manga =
        api.manga(mangaId).data.toDomain()

    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val collected = mutableListOf<MangaDexChapter>()
        var offset = 0
        while (true) {
            val page = api.feed(mangaId, offset = offset, languages = enabledLanguages())
            collected += page.data
            offset += page.limit
            if (page.data.isEmpty() || offset >= page.total || page.data.size < page.limit) break
        }
        return collected
            // Chapters hosted on official sites stay in the list; the details
            // screen opens them in a browser instead of the reader.
            .groupBy { it.attributes.chapter ?: it.id }
            .map { (_, group) -> group.maxBy { it.attributes.readableAt.orEmpty() } }
            .map { it.toDomain() }
            .sortedWith(compareBy(nullsLast<Double>()) { it.number?.toDoubleOrNull() })
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val atHome = api.pages(chapterId)
        val base = atHome.baseUrl.trimEnd('/')
        val hash = atHome.chapter.hash
        return atHome.chapter.data.mapIndexed { index, file ->
            MangaPage(url = "$base/data/$hash/$file", index = index)
        }
    }

    /**
     * Languages currently enabled in Settings, lowercase. Empty list makes
     * Retrofit omit the query parameter, which the API reads as "any language".
     */
    private fun enabledLanguages(): List<String> =
        settings.snapshot.value.mangadexLanguages
            .filter { it in SUPPORTED_LANGUAGES }
            .sorted()

    /** Locale order used to pick titles and descriptions: enabled first. */
    private fun preferredLocales(): List<String> {
        val enabled = enabledLanguages()
        return if (enabled.isEmpty()) DEFAULT_LOCALES else enabled + DEFAULT_LOCALES
    }

    private suspend fun list(
        offset: Int,
        limit: Int,
        title: String? = null,
        order: Map<String, String>,
    ): Paged<Manga> {
        val response = api.listManga(
            title = title,
            limit = limit,
            offset = offset,
            order = order,
            languages = enabledLanguages(),
        )
        return Paged(
            items = response.data.map { it.toDomain() },
            offset = response.offset,
            total = response.total,
            hasNext = response.offset + response.data.size < response.total && response.data.isNotEmpty(),
        )
    }

    private fun MangaDexManga.toDomain(): Manga {
        val attrs = attributes
        val locales = preferredLocales()
        val cover = relationships.firstOrNull { it.type == "cover_art" }
            ?.attributes?.fileName
        val author = (
            relationships.firstOrNull { it.type == "author" }?.attributes?.name
                ?: relationships.firstOrNull { it.type == "artist" }?.attributes?.name
            )
        return Manga(
            id = id,
            source = source,
            title = attrs.title.pick(locales) ?: "Untitled",
            altTitle = attrs.altTitles.firstNotNullOfOrNull { it.pick(locales) },
            coverUrl = cover?.let { "https://uploads.mangadex.org/covers/$id/$it" },
            description = attrs.description.pick(locales),
            tags = attrs.tags.map { MangaTag(id = it.id, title = it.attributes.name.pick(locales) ?: "?") },
            state = mapState(attrs.status),
            author = author,
            chaptersCount = attrs.lastChapter?.toDoubleOrNull()?.toInt() ?: 0,
        )
    }

    private fun MangaDexChapter.toDomain(): MangaChapter = MangaChapter(
        id = id,
        source = source,
        number = attributes.chapter,
        title = attributes.title?.takeIf { it.isNotBlank() },
        volume = attributes.volume,
        scanlator = relationships.firstOrNull { it.type == "scanlation_group" }?.attributes?.name,
        uploadedAt = parseDate(attributes.readableAt),
        pagesCount = attributes.pages,
        externalUrl = attributes.externalUrl?.takeIf { it.startsWith("http") },
    )

    private fun mapState(status: String?): MangaState = when (status) {
        "ongoing" -> MangaState.ONGOING
        "completed" -> MangaState.FINISHED
        "hiatus" -> MangaState.PAUSED
        "cancelled" -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun parseDate(raw: String?): Long = runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrDefault(0L)

    private fun Map<String, String>?.pick(locales: List<String>): String? {
        if (this == null) return null
        locales.firstNotNullOfOrNull { this[it] }?.let { return it }
        return values.firstOrNull { it.isNotBlank() }
    }

    companion object {
        const val PAGE_SIZE = 24

        /** Fallback locale order when no language filter is set. */
        private val DEFAULT_LOCALES = listOf("en", "ru")
    }
}
