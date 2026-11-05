package org.kinera.app.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.worker.LibraryUpdateWorker
import java.util.concurrent.TimeUnit

/**
 * Keeps the periodic library update in sync with the settings. Called at
 * app start and whenever the user flips the toggle or the interval, so the
 * schedule always mirrors what the UI shows.
 */
class LibraryUpdateScheduler(
    private val context: Context,
    private val settings: SettingsRepository,
) {

    /** Enqueues or cancels the periodic work according to the settings. */
    suspend fun applySettings() {
        val snapshot = settings.snapshot.value
        val workManager = WorkManager.getInstance(context)
        if (!snapshot.libraryUpdateEnabled) {
            workManager.cancelUniqueWork(LibraryUpdateWorker.PERIODIC_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<LibraryUpdateWorker>(
            snapshot.libraryUpdateIntervalHours.toLong(), TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(LibraryUpdateWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            LibraryUpdateWorker.PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Runs one update pass right now (the "Check now" action). */
    fun checkNow() {
        val request = OneTimeWorkRequestBuilder<LibraryUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(LibraryUpdateWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            LibraryUpdateWorker.ONESHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
