package org.kinera.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kinera.app.data.downloads.DownloadStorage
import org.kinera.app.data.local.dao.DownloadsDao
import org.kinera.app.data.local.entity.DownloadState
import org.kinera.app.data.repository.CatalogRepository

/**
 * Saves every page of one chapter to the private download directory.
 * Already present files are skipped, so an interrupted job resumes from
 * where it stopped instead of starting over.
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val catalog: CatalogRepository,
    private val dao: DownloadsDao,
    private val storage: DownloadStorage,
    private val client: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val mangaId = inputData.getString(KEY_MANGA_ID) ?: return Result.failure()
        val chapterId = inputData.getString(KEY_CHAPTER_ID) ?: return Result.failure()
        val row = dao.get(chapterId) ?: return Result.success() // cancelled from UI

        dao.updateState(chapterId, DownloadState.RUNNING, row.progress, row.total, null)
        return try {
            val pages = catalog.pages(chapterId)
            dao.updateState(chapterId, DownloadState.RUNNING, 0, pages.size, null)
            pages.forEachIndexed { index, page ->
                val file = storage.pageFile(mangaId, chapterId, index)
                if (!file.exists() || file.length() == 0L) {
                    downloadPage(page.url, file)
                }
                dao.updateProgress(chapterId, index + 1)
            }
            dao.updateState(chapterId, DownloadState.DONE, pages.size, pages.size, null)
            Result.success()
        } catch (cancelled: CancellationException) {
            // Stopped by the user or by the system: keep the row resumable.
            dao.get(chapterId)?.let {
                dao.updateState(chapterId, DownloadState.QUEUED, it.progress, it.total, null)
            }
            throw cancelled
        } catch (failure: Throwable) {
            dao.get(chapterId)?.let {
                dao.updateState(
                    chapterId = it.chapterId,
                    state = DownloadState.FAILED,
                    progress = it.progress,
                    total = it.total,
                    error = failure.message ?: failure.javaClass.simpleName,
                )
            }
            Result.failure()
        }
    }

    private fun downloadPage(url: String, target: java.io.File) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $url" }
            response.body.bytes().let { bytes -> target.writeBytes(bytes) }
        }
    }

    companion object {
        const val TAG = "kinera_download"
        const val KEY_MANGA_ID = "manga_id"
        const val KEY_CHAPTER_ID = "chapter_id"
        const val USER_AGENT = "Kinera/0.1.0 (github.com/mel0k1/Kinera)"
    }
}

/** Hand-rolled factory so the worker gets repositories through Koin. */
class KineraWorkerFactory(
    private val catalog: CatalogRepository,
    private val dao: DownloadsDao,
    private val storage: DownloadStorage,
    private val client: OkHttpClient,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): androidx.work.ListenableWorker? = when (workerClassName) {
        DownloadWorker::class.java.name ->
            DownloadWorker(appContext, workerParameters, catalog, dao, storage, client)
        else -> null
    }
}
