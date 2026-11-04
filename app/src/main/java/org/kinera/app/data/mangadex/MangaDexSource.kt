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
import java.time.OffsetDateTime

/**
 * Catalog source backed by the official public MangaDex API.
 * The app never hosts content itself; it only talks to api.mangadex.org.
 */
class MangaDexSource(private val api: MangaDexApi) {

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
            val page = api.feed(mangaId, offset = offset)
            collected += page.data
            offset += page.limit
            if (page.data.isEmpty() || offset >= page.total || page.data.size < page.limit) break
        }
        return collected
            .filter { it.attributes.externalUrl == null }
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

    private suspend fun list(
        offset: Int,
        limit: Int,
        title: String? = null,
        order: Map<String, String>,
    ): Paged<Manga> {
        val response = api.listManga(title = title, limit = limit, offset = offset, order = order)
        return Paged(
            items = response.data.map { it.toDomain() },
            offset = response.offset,
            total = response.total,
            hasNext = response.offset + response.data.size < response.total && response.data.isNotEmpty(),
        )
    }

    private fun MangaDexManga.toDomain(): Manga {
        val attrs = attributes
        val cover = relationships.firstOrNull { it.type == "cover_art" }
            ?.attributes?.fileName
        val author = (
            relationships.firstOrNull { it.type == "author" }?.attributes?.name
                ?: relationships.firstOrNull { it.type == "artist" }?.attributes?.name
            )
        return Manga(
            id = id,
            source = source,
            title = attrs.title.pick("en", "ru") ?: "Untitled",
            altTitle = attrs.altTitles.firstNotNullOfOrNull { it.pick("en", "ru") },
            coverUrl = cover?.let { "https://uploads.mangadex.org/covers/$id/$it" },
            description = attrs.description.pick("en", "ru"),
            tags = attrs.tags.map { MangaTag(id = it.id, title = it.attributes.name.pick("en") ?: "?") },
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

    private fun Map<String, String>?.pick(vararg locales: String): String? {
        if (this == null) return null
        locales.firstNotNullOfOrNull { this[it] }?.let { return it }
        return values.firstOrNull { it.isNotBlank() }
    }

    companion object {
        const val PAGE_SIZE = 24
    }
}
