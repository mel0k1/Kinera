package org.kinera.app.data.repository

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.Paged
import org.kinera.app.data.localsource.FolderMangaSource
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.LocalMangaSource
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.mangadex.MangaDexSource

/**
 * Single entry point to all catalog sources. Manga and chapter ids carry a
 * prefix for the local sources ("local:" for CBZ archives, "lfold:" for
 * folders of images), everything else goes to MangaDex. More online sources
 * can slot in behind the same calls later.
 */
class CatalogRepository(
    private val mangadex: MangaDexSource,
    private val local: LocalMangaSource,
    private val folders: FolderMangaSource,
) {

    suspend fun popular(offset: Int = 0): Paged<Manga> = mangadex.popular(offset)

    suspend fun latest(offset: Int = 0): Paged<Manga> = mangadex.latest(offset)

    suspend fun search(query: String, offset: Int = 0): Paged<Manga> = mangadex.search(query, offset)

    suspend fun details(mangaId: String): Manga = when {
        mangaId.startsWith(LocalSourceStorage.MANGA_ID_PREFIX) -> local.details(mangaId)
        mangaId.startsWith(FolderSourceStorage.MANGA_ID_PREFIX) -> folders.details(mangaId)
        else -> mangadex.details(mangaId)
    }

    suspend fun chapters(mangaId: String): List<MangaChapter> = when {
        mangaId.startsWith(LocalSourceStorage.MANGA_ID_PREFIX) -> local.chapters(mangaId)
        mangaId.startsWith(FolderSourceStorage.MANGA_ID_PREFIX) -> folders.chapters(mangaId)
        else -> mangadex.chapters(mangaId)
    }

    suspend fun pages(chapterId: String): List<MangaPage> = when {
        chapterId.startsWith(LocalSourceStorage.CHAPTER_ID_PREFIX) -> local.pages(chapterId)
        chapterId.startsWith(FolderSourceStorage.CHAPTER_ID_PREFIX) -> folders.pages(chapterId)
        else -> mangadex.pages(chapterId)
    }
}
