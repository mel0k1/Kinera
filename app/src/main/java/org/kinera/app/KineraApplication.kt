package org.kinera.app

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kinera.app.di.kineraModules
import org.kinera.app.data.repository.LibraryUpdateScheduler
import org.kinera.app.worker.KineraWorkerFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class KineraApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Guard against double-start (tests reuse the JVM across runs).
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@KineraApplication)
                modules(kineraModules(this@KineraApplication))
            }
        }
        // Keep the periodic library update in sync with the saved settings.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { GlobalContext.get().get<LibraryUpdateScheduler>().applySettings() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(GlobalContext.get().get<KineraWorkerFactory>())
            .build()

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { GlobalContext.get().get<OkHttpClient>() }
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build(),
        )
        // No global crossfade: reader pages must pop in instantly to feel
        // responsive. Covers opt back in explicitly via CoverImage.
        .crossfade(false)
        .build()
}
