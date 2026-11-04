package org.kinera.app.data.komga

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.komga.dto.KomgaBook
import org.kinera.app.data.komga.dto.KomgaSeries
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.settings.ServerConnection
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Catalog source for a self-hosted Komga server (https://komga.org).
 * Series map to manga, books map to chapters, page images are served by
 * the server itself and authenticated through [SelfHostAuthInterceptor]...
 * rather through the OkHttp interceptor wired in the DI graph.
 *
 * Komga has no popularity ranking: "Popular" shows recently added series,
 * "Latest" shows recently updated ones.
 */
class KomgaSource(
    private val api: KomgaApi,
    private val settings: SettingsRepository,
) {

    val source: MangaSource = MangaSource.KOMGA

    private suspend fun server(): ServerConnection =
        settings.snapshot.value.komga

    private suspend fun requireServer(): ServerConnection {
        val server = server()
        check(server.isConfigured) { "Komga server is not configured" }
        return server
    }

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, sort = "createdDate,desc")

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, sort = "lastModifiedDate,desc")

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        list(offset, limit, search = query, sort = null)

    suspend fun details(mangaId: String): Manga {
        val server = requireServer()
        val seriesId = mangaId.removePrefix(MANGA_ID_PREFIX)
        val series = api.seriesById(
            url = "${server.url}/api/v1/series/$seriesId",
        )
        return series.toDomain(server)
    }

    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val server = requireServer()
        val seriesId = mangaId.removePrefix(MANGA_ID_PREFIX)
        val books = mutableListOf<KomgaBook>()
        var page = 0
        while (true) {
            val response = api.books(
                url = "${server.url}/api/v1/series/$seriesId/books",
                page = page,
                size = 100,
            )
            books += response.content
            if (response.last || response.content.isEmpty()) break
            page += 1
        }
        return books
            .map { it.toDomain() }
            .sortedWith(compareBy { it.number?.toDoubleOrNull() ?: Double.MAX_VALUE })
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val server = requireServer()
        val bookId = chapterId.removePrefix(CHAPTER_ID_PREFIX)
        val book = api.bookById(
            url = "${server.url}/api/v1/books/$bookId",
        )
        val count = book.media.pagesCount
        check(count > 0) { "Book has no readable pages: $bookId" }
        // Komga page numbers are 1-based
        return (1..count).map { index ->
            MangaPage(url = "${server.url}/api/v1/books/$bookId/pages/$index", index = index - 1)
        }
    }

    private suspend fun list(
        offset: Int,
        limit: Int,
        search: String? = null,
        sort: String?,
    ): Paged<Manga> {
        val server = requireServer()
        val page = offset / limit
        val response = api.series(
            url = "${server.url}/api/v1/series",
            page = page,
            size = limit,
            search = search?.takeIf { it.isNotBlank() },
            sort = sort,
        )
        return Paged(
            items = response.content.map { it.toDomain(server) },
            offset = offset,
            total = response.totalElements.toInt(),
            hasNext = !response.last && response.content.isNotEmpty(),
        )
    }

    private fun KomgaSeries.toDomain(server: ServerConnection) = Manga(
        id = MANGA_ID_PREFIX + id,
        source = source,
        title = name.ifBlank { "Untitled" },
        coverUrl = "${server.url}/api/v1/series/$id/thumbnail",
        description = metadata.summary?.takeIf { it.isNotBlank() },
        tags = emptyList(),
        state = mapState(metadata.status),
        author = booksMetadata.authors
            .firstOrNull { it.role.equals("writer", ignoreCase = true) || it.role.equals("author", ignoreCase = true) }
            ?.name
            ?: booksMetadata.authors.firstOrNull()?.name,
        chaptersCount = booksCount,
    )

    private fun KomgaBook.toDomain() = MangaChapter(
        id = CHAPTER_ID_PREFIX + id,
        source = source,
        number = metadata.number?.takeIf { it.isNotBlank() },
        title = metadata.title?.takeIf { it.isNotBlank() },
        volume = null,
        scanlator = null,
        uploadedAt = parseDate(metadata.releaseDate),
        pagesCount = media.pagesCount,
    )

    private fun mapState(status: String?): MangaState = when (status) {
        "ONGOING" -> MangaState.ONGOING
        "ENDED" -> MangaState.FINISHED
        "HIATUS" -> MangaState.PAUSED
        "ABANDONED" -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun parseDate(raw: String?): Long = runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrElse {
        runCatching { LocalDate.parse(raw).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }
            .getOrDefault(0L)
    }

    companion object {
        const val PAGE_SIZE = 24
        const val MANGA_ID_PREFIX = "komga:"
        const val CHAPTER_ID_PREFIX = "komgach:"
    }
}
