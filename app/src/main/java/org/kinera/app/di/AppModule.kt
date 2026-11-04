package org.kinera.app.di

import android.content.Context
import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kinera.app.data.local.LibraryDatabase
import org.kinera.app.data.mangadex.MangaDexApi
import org.kinera.app.data.mangadex.MangaDexSource
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.LibraryRepository
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.ui.screens.details.DetailsViewModel
import org.kinera.app.ui.screens.explore.ExploreViewModel
import org.kinera.app.ui.screens.history.HistoryViewModel
import org.kinera.app.ui.screens.library.LibraryViewModel
import org.kinera.app.ui.screens.reader.ReaderViewModel
import org.kinera.app.ui.screens.search.SearchViewModel
import org.kinera.app.ui.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private const val MANGADEX_BASE_URL = "https://api.mangadex.org/"

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }

    single {
        val cache = Cache(File(androidContext().cacheDir, "http_cache"), 64L * 1024 * 1024)
        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(MANGADEX_BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MangaDexApi::class.java)
    }

    single { MangaDexSource(get()) }
    single { CatalogRepository(get()) }

    single {
        Room.databaseBuilder(androidContext(), LibraryDatabase::class.java, "kinera_library.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<LibraryDatabase>().favouritesDao() }
    single { get<LibraryDatabase>().historyDao() }
    single { get<LibraryDatabase>().categoriesDao() }
    single { LibraryRepository(get(), get(), get(), get()) }
    single { SettingsRepository(androidContext()) }
}

val uiModule = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ExploreViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { (mangaId: String) -> DetailsViewModel(get(), get(), mangaId) }
    viewModel { (mangaId: String, chapterId: String) ->
        ReaderViewModel(get(), get(), get(), mangaId, chapterId)
    }
}

fun kineraModules(context: Context) = listOf(dataModule, uiModule)
