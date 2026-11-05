package org.kinera.app.data.repository

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.Paged
import org.kinera.app.data.desume.DesuMeSource
import org.kinera.app.data.komga.KomgaSource
import org.kinera.app.data.localsource.FolderMangaSource
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.LocalMangaSource
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.mangadex.MangaDexSource
import org.kinera.app.data.mangalib.MangaLibSource
import org.kinera.app.data.remanga.RemangaSource
import org.kinera.app.data.suwayomi.SuwayomiSource

/**
 * Single entry point to all catalog sources. Manga and chapter ids carry a
 * prefix for every source except MangaDex ("mlib:" for MangaLib, "rm:" for
 * Remanga, "desu:" for Desu.me, "komga:" for Komga, "swyd:" for Suwayomi,
 * "local:" for CBZ archives, "lfold:" for folders of images), everything
 * else goes to MangaDex.
 */
open class CatalogRepository(
    private val mangadex: MangaDexSource,
    private val mangalib: MangaLibSource,
    private val remanga: RemangaSource,
    private val desume: DesuMeSource,
    private val komga: KomgaSource,
    private val suwayomi: SuwayomiSource,
    private val local: LocalMangaSource,
    private val folders: FolderMangaSource,
) {

    open suspend fun popular(source: MangaSource = MangaSource.MANGADEX, offset: Int = 0): Paged<Manga> =
        when (source) {
            MangaSource.MANGADEX -> mangadex.popular(offset)
            MangaSource.MANGALIB -> mangalib.popular(offset)
            MangaSource.REMANGA -> remanga.popular(offset)
            MangaSource.DESUME -> desume.popular(offset)
            MangaSource.KOMGA -> komga.popular(offset)
            MangaSource.SUWAYOMI -> suwayomi.popular(offset)
            MangaSource.LOCAL -> Paged(emptyList())
        }

    open suspend fun latest(source: MangaSource = MangaSource.MANGADEX, offset: Int = 0): Paged<Manga> =
        when (source) {
            MangaSource.MANGADEX -> mangadex.latest(offset)
            MangaSource.MANGALIB -> mangalib.latest(offset)
            MangaSource.REMANGA -> remanga.latest(offset)
            MangaSource.DESUME -> desume.latest(offset)
            MangaSource.KOMGA -> komga.latest(offset)
            MangaSource.SUWAYOMI -> suwayomi.latest(offset)
            MangaSource.LOCAL -> Paged(emptyList())
        }

    open suspend fun search(
        query: String,
        source: MangaSource = MangaSource.MANGADEX,
        offset: Int = 0,
    ): Paged<Manga> = when (source) {
        MangaSource.MANGADEX -> mangadex.search(query, offset)
        MangaSource.MANGALIB -> mangalib.search(query, offset)
        MangaSource.REMANGA -> remanga.search(query, offset)
        MangaSource.DESUME -> desume.search(query, offset)
        MangaSource.KOMGA -> komga.search(query, offset)
        MangaSource.SUWAYOMI -> suwayomi.search(query, offset)
        MangaSource.LOCAL -> Paged(emptyList())
    }

    open suspend fun details(mangaId: String): Manga = when {
        mangaId.startsWith(LocalSourceStorage.MANGA_ID_PREFIX) -> local.details(mangaId)
        mangaId.startsWith(FolderSourceStorage.MANGA_ID_PREFIX) -> folders.details(mangaId)
        mangaId.startsWith(MangaLibSource.MANGA_ID_PREFIX) -> mangalib.details(mangaId)
        mangaId.startsWith(RemangaSource.MANGA_ID_PREFIX) -> remanga.details(mangaId)
        mangaId.startsWith(DesuMeSource.MANGA_ID_PREFIX) -> desume.details(mangaId)
        mangaId.startsWith(KomgaSource.MANGA_ID_PREFIX) -> komga.details(mangaId)
        mangaId.startsWith(SuwayomiSource.MANGA_ID_PREFIX) -> suwayomi.details(mangaId)
        else -> mangadex.details(mangaId)
    }

    open suspend fun chapters(mangaId: String): List<MangaChapter> = when {
        mangaId.startsWith(LocalSourceStorage.MANGA_ID_PREFIX) -> local.chapters(mangaId)
        mangaId.startsWith(FolderSourceStorage.MANGA_ID_PREFIX) -> folders.chapters(mangaId)
        mangaId.startsWith(MangaLibSource.MANGA_ID_PREFIX) -> mangalib.chapters(mangaId)
        mangaId.startsWith(RemangaSource.MANGA_ID_PREFIX) -> remanga.chapters(mangaId)
        mangaId.startsWith(DesuMeSource.MANGA_ID_PREFIX) -> desume.chapters(mangaId)
        mangaId.startsWith(KomgaSource.MANGA_ID_PREFIX) -> komga.chapters(mangaId)
        mangaId.startsWith(SuwayomiSource.MANGA_ID_PREFIX) -> suwayomi.chapters(mangaId)
        else -> mangadex.chapters(mangaId)
    }

    open suspend fun pages(chapterId: String): List<MangaPage> = when {
        chapterId.startsWith(LocalSourceStorage.CHAPTER_ID_PREFIX) -> local.pages(chapterId)
        chapterId.startsWith(FolderSourceStorage.CHAPTER_ID_PREFIX) -> folders.pages(chapterId)
        chapterId.startsWith(MangaLibSource.CHAPTER_ID_PREFIX) -> mangalib.pages(chapterId)
        chapterId.startsWith(RemangaSource.CHAPTER_ID_PREFIX) -> remanga.pages(chapterId)
        chapterId.startsWith(DesuMeSource.CHAPTER_ID_PREFIX) -> desume.pages(chapterId)
        chapterId.startsWith(KomgaSource.CHAPTER_ID_PREFIX) -> komga.pages(chapterId)
        chapterId.startsWith(SuwayomiSource.CHAPTER_ID_PREFIX) -> suwayomi.pages(chapterId)
        else -> mangadex.pages(chapterId)
    }
}
