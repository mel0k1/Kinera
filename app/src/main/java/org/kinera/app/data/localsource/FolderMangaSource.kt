package org.kinera.app.data.localsource

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage

/**
 * Catalog facade over [FolderSourceStorage]. Mirrors [LocalMangaSource] so the
 * repository can route "lfold:" ids the same way it routes "local:" ones.
 */
class FolderMangaSource(private val storage: FolderSourceStorage) {

    suspend fun details(mangaId: String): Manga {
        val key = mangaId.removePrefix(FolderSourceStorage.MANGA_ID_PREFIX)
        val entry = storage.entry(key) ?: error("Folder not found: $key")
        return storage.buildManga(entry) ?: error("No readable pages in ${entry.name}")
    }

    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val key = mangaId.removePrefix(FolderSourceStorage.MANGA_ID_PREFIX)
        val entry = storage.entry(key) ?: error("Folder not found: $key")
        return storage.chapters(entry)
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val (key, chapterKey) = FolderSourceStorage.parseChapterId(chapterId)
            ?: error("Malformed chapter id: $chapterId")
        val entry = storage.entry(key) ?: error("Folder not found: $key")
        return storage.pages(entry, chapterKey).mapIndexed { index, url -> MangaPage(url, index) }
    }
}
