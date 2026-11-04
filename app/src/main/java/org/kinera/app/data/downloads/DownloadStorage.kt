package org.kinera.app.data.downloads

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kinera.app.core.model.MangaPage
import java.io.File

/**
 * Chapter pages saved for offline reading, laid out as
 * files/downloads/<manga id>/<chapter id>/0001.jpg…
 */
class DownloadStorage(private val context: Context) {

    private val root: File
        get() = File(context.filesDir, "downloads")

    fun pageFile(mangaId: String, chapterId: String, index: Int): File =
        File(chapterDir(mangaId, chapterId), "%04d.jpg".format(index))

    fun chapterFiles(mangaId: String, chapterId: String): List<File> =
        chapterDir(mangaId, chapterId).listFiles { file -> file.isFile }
            ?.sortedBy { it.name }
            .orEmpty()

    /** Pages already on disk, or null when the chapter is not downloaded. */
    suspend fun localPages(mangaId: String, chapterId: String): List<MangaPage>? =
        withContext(Dispatchers.IO) {
            val files = chapterFiles(mangaId, chapterId)
            if (files.isEmpty()) {
                null
            } else {
                files.mapIndexed { index, file ->
                    MangaPage(url = "file://${file.absolutePath}", index = index)
                }
            }
        }

    suspend fun deleteChapter(mangaId: String, chapterId: String) = withContext(Dispatchers.IO) {
        chapterDir(mangaId, chapterId).deleteRecursively()
        File(root, mangaId).takeIf { d -> d.isDirectory && d.listFiles()?.isEmpty() == true }?.delete()
        Unit
    }

    suspend fun deleteManga(mangaId: String) = withContext(Dispatchers.IO) {
        File(root, mangaId).deleteRecursively()
        Unit
    }

    private fun chapterDir(mangaId: String, chapterId: String): File =
        File(File(root, mangaId), chapterId)
}
