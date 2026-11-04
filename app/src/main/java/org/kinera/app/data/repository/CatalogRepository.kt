package org.kinera.app.data.repository

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.Paged
import org.kinera.app.data.mangadex.MangaDexSource

/**
 * Single entry point to the online catalog. More sources can be added later
 * behind the same interface.
 */
class CatalogRepository(private val source: MangaDexSource) {

    suspend fun popular(offset: Int = 0): Paged<Manga> = source.popular(offset)

    suspend fun latest(offset: Int = 0): Paged<Manga> = source.latest(offset)

    suspend fun search(query: String, offset: Int = 0): Paged<Manga> = source.search(query, offset)

    suspend fun details(mangaId: String): Manga = source.details(mangaId)

    suspend fun chapters(mangaId: String): List<MangaChapter> = source.chapters(mangaId)

    suspend fun pages(chapterId: String): List<MangaPage> = source.pages(chapterId)
}
