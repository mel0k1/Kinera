package org.kinera.app.data.localsource

import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads imported CBZ archives. Identifiers carry prefixes added by
 * [LocalSourceStorage] so the catalog can route requests here instead of
 * MangaDex.
 */
class LocalMangaSource(private val storage: LocalSourceStorage) {

    suspend fun details(mangaId: String): Manga {
        val base = mangaId.removePrefix(LocalSourceStorage.MANGA_ID_PREFIX)
        val file = storage.resolveFile(base) ?: error("Archive not found: $base")
        return storage.buildManga(file) ?: error("No readable pages in $base.cbz")
    }

    suspend fun chapters(mangaId: String): List<MangaChapter> {
        val base = mangaId.removePrefix(LocalSourceStorage.MANGA_ID_PREFIX)
        val file = storage.resolveFile(base) ?: error("Archive not found: $base")
        val pages = ZipFile(file).use { zip ->
            zip.entries().asSequence().count { LocalSourceStorage.isImageEntry(it.name) }
        }
        return listOf(
            MangaChapter(
                id = LocalSourceStorage.chapterId(base),
                source = MangaSource.LOCAL,
                number = null,
                title = LocalSourceStorage.prettyTitle(base),
                volume = null,
                scanlator = null,
                uploadedAt = file.lastModified(),
                pagesCount = pages,
            ),
        )
    }

    suspend fun pages(chapterId: String): List<MangaPage> {
        val base = chapterId.removePrefix(LocalSourceStorage.CHAPTER_ID_PREFIX)
        return storage.extractPages(base).mapIndexed { index, url -> MangaPage(url, index) }
    }
}
