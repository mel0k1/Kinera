package org.kinera.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import org.kinera.app.di.kineraModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KineraApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KineraApplication)
            modules(kineraModules(this@KineraApplication))
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build(),
            )
            .crossfade(true)
            .build()
}
