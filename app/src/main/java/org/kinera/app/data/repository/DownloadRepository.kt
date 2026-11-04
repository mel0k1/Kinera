package org.kinera.app.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.data.downloads.DownloadStorage
import org.kinera.app.data.local.dao.DownloadsDao
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.local.entity.DownloadState
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.worker.DownloadWorker

/**
 * Coordinates the downloads table, the on-disk page cache and WorkManager
 * jobs. The repository is the only entry point the UI uses; the worker
 * touches the dao and storage directly through DI.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadsDao,
    private val storage: DownloadStorage,
) {

    val all: Flow<List<DownloadEntity>> = dao.observeAll()

    fun forManga(mangaId: String): Flow<List<DownloadEntity>> = dao.observeForManga(mangaId)

    fun observeDownloaded(chapterId: String): Flow<Boolean> = dao.observeDownloaded(chapterId)

    suspend fun enqueue(manga: Manga, chapters: List<MangaChapter>) {
        val online = chapters.filter { it.source != org.kinera.app.core.model.MangaSource.LOCAL }
        for (chapter in online) {
            storage.deleteChapter(manga.id, chapter.id)
            dao.insert(
                DownloadEntity(
                    chapterId = chapter.id,
                    mangaId = manga.id,
                    sourceId = manga.source.id,
                    title = manga.title,
                    chapterLabel = chapter.label,
                    coverUrl = manga.coverUrl,
                    state = DownloadState.QUEUED,
                    progress = 0,
                    total = chapter.pagesCount,
                    createdAt = System.currentTimeMillis(),
                    error = null,
                ),
            )
            // REPLACE, not KEEP: a previously failed work request stays in the
            // queue and KEEP would refuse to schedule a fresh one.
            workManager().enqueueUniqueWork(
                chapter.id,
                ExistingWorkPolicy.REPLACE,
                buildRequest(manga.id, chapter.id),
            )
        }
    }

    suspend fun cancel(chapterId: String) {
        workManager().cancelUniqueWork(chapterId)
        dao.get(chapterId)?.let { row ->
            storage.deleteChapter(row.mangaId, chapterId)
        }
        dao.delete(chapterId)
    }

    suspend fun retry(chapterId: String) {
        val row = dao.get(chapterId) ?: return
        dao.updateState(chapterId, DownloadState.QUEUED, 0, row.total, null)
        workManager().enqueueUniqueWork(
            chapterId,
            ExistingWorkPolicy.REPLACE,
            buildRequest(row.mangaId, chapterId),
        )
    }

    suspend fun deleteDownloaded(chapterId: String) {
        workManager().cancelUniqueWork(chapterId)
        val row = dao.get(chapterId)
        if (row != null) {
            storage.deleteChapter(row.mangaId, chapterId)
            dao.delete(chapterId)
        }
    }

    suspend fun deleteManga(mangaId: String) {
        val snapshot = dao.observeForManga(mangaId).first()
        snapshot.forEach { workManager().cancelUniqueWork(it.chapterId) }
        dao.deleteForManga(mangaId)
        storage.deleteManga(mangaId)
    }

    suspend fun localPages(mangaId: String, chapterId: String): List<MangaPage>? =
        storage.localPages(mangaId, chapterId)

    private fun buildRequest(mangaId: String, chapterId: String) =
        OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_MANGA_ID to mangaId,
                    DownloadWorker.KEY_CHAPTER_ID to chapterId,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(DownloadWorker.TAG)
            .build()

    private fun workManager(): WorkManager = WorkManager.getInstance(context)
}
