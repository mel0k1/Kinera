package org.kinera.app.di

import android.content.Context
import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kinera.app.BuildConfig
import org.kinera.app.data.downloads.DownloadStorage
import org.kinera.app.data.local.LibraryDatabase
import org.kinera.app.data.local.dao.BookmarksDao
import org.kinera.app.data.local.dao.CategoriesDao
import org.kinera.app.data.local.dao.DownloadsDao
import org.kinera.app.data.local.dao.FavouritesDao
import org.kinera.app.data.local.dao.HistoryDao
import org.kinera.app.data.localsource.FolderMangaSource
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.LocalMangaSource
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.mangadex.MangaDexApi
import org.kinera.app.data.mangadex.MangaDexSource
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.DownloadRepository
import org.kinera.app.data.repository.LibraryRepository
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.ui.screens.details.DetailsViewModel
import org.kinera.app.ui.screens.downloads.DownloadsViewModel
import org.kinera.app.ui.screens.explore.ExploreViewModel
import org.kinera.app.ui.screens.history.HistoryViewModel
import org.kinera.app.ui.screens.library.LibraryViewModel
import org.kinera.app.ui.screens.reader.ReaderViewModel
import org.kinera.app.ui.screens.search.SearchViewModel
import org.kinera.app.ui.screens.settings.SettingsViewModel
import org.kinera.app.worker.KineraWorkerFactory
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
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Kinera/${BuildConfig.VERSION_NAME} (github.com/mel0k1/Kinera)")
                        .build(),
                )
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
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
    single { LocalSourceStorage(androidContext()) }
    single { LocalMangaSource(get()) }
    single { FolderSourceStorage(androidContext()) }
    single { FolderMangaSource(get()) }
    single { CatalogRepository(get(), get(), get()) }

    single {
        Room.databaseBuilder(androidContext(), LibraryDatabase::class.java, "kinera_library.db")
            .addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3)
            .build()
    }

    single<FavouritesDao> { get<LibraryDatabase>().favouritesDao() }
    single<HistoryDao> { get<LibraryDatabase>().historyDao() }
    single<CategoriesDao> { get<LibraryDatabase>().categoriesDao() }
    single<DownloadsDao> { get<LibraryDatabase>().downloadsDao() }
    single<BookmarksDao> { get<LibraryDatabase>().bookmarksDao() }
    single { LibraryRepository(get(), get(), get(), get(), get()) }

    single { DownloadStorage(androidContext()) }
    single {
        DownloadRepository(androidContext(), get(), get())
    }

    single { SettingsRepository(androidContext()) }

    single {
        KineraWorkerFactory(
            catalog = get(),
            dao = get<DownloadsDao>(),
            storage = get(),
            client = get(),
        )
    }
}

val uiModule = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ExploreViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModel { (mangaId: String) -> DetailsViewModel(get(), get(), get(), mangaId) }
    viewModel { (mangaId: String, chapterId: String, startPage: Int) ->
        ReaderViewModel(get(), get(), get(), get(), mangaId, chapterId, startPage)
    }
}

fun kineraModules(context: Context) = listOf(dataModule, uiModule)
