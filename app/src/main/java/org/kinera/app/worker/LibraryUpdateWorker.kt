package org.kinera.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import org.kinera.app.data.repository.LibraryUpdater

/**
 * Background pass over the library: refreshes chapter counts of every
 * favourite so the shelf can badge titles with new chapters. All the
 * actual logic lives in [LibraryUpdater]; the worker only adapts it to
 * WorkManager scheduling.
 */
class LibraryUpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val updater: LibraryUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        updater.updateAll()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        const val TAG = "kinera_library_update"
        const val PERIODIC_NAME = "kinera_library_update_periodic"
        const val ONESHOT_NAME = "kinera_library_update_now"
        private const val MAX_ATTEMPTS = 3
    }
}
