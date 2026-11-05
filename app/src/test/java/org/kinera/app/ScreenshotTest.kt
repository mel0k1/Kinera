package org.kinera.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.Paged
import org.kinera.app.data.komga.KomgaApi
import org.kinera.app.data.komga.KomgaSource
import org.kinera.app.data.desume.DesuMeApi
import org.kinera.app.data.desume.DesuMeSource
import org.kinera.app.data.localsource.FolderMangaSource
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.LocalMangaSource
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.mangadex.MangaDexApi
import org.kinera.app.data.mangadex.MangaDexSource
import org.kinera.app.data.mangalib.MangaLibApi
import org.kinera.app.data.mangalib.MangaLibSource
import org.kinera.app.data.remanga.RemangaApi
import org.kinera.app.data.remanga.RemangaSource
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.LibraryRepository
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.suwayomi.SuwayomiApi
import org.kinera.app.data.suwayomi.SuwayomiSource
import org.kinera.app.ui.navigation.ExploreRoute
import org.kinera.app.ui.navigation.KineraApp
import org.kinera.app.ui.navigation.MangaRoute
import org.kinera.app.ui.navigation.ReaderRoute
import org.kinera.app.ui.theme.KineraTheme
import org.kinera.app.data.settings.ThemeMode
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Records real app screenshots on the JVM through Robolectric native
 * graphics + Roborazzi. The catalog is replaced with a fake so the screens
 * render deterministic content; composables, view models and theme are the
 * production ones. Cover and page images live in the test drawable
 * resources and are loaded by Coil via android.resource:// URIs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(NATIVE)
@Config(sdk = [34], application = KineraApplication::class, qualifiers = "w360dp-h800dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var navController: NavHostController? = null

    @Before
    fun setUp() {
        val context = GlobalContext.get().get<Context>()
        val dir = File(context.cacheDir, "shots").apply { mkdirs() }
        val covers = generateCovers(dir)
        loadKoinModules(
            module {
                single<CatalogRepository> { FakeCatalog(context, covers, generatePages(dir)) }
            },
        )
        // Seed the real database so the Library screen has content.
        val library = GlobalContext.get().get<LibraryRepository>()
        runBlocking { covers.forEach { library.toggleFavorite(it) } }
        prewarmMemoryCache(covers.map { it.coverUrl!! } + generatePages(dir).map { it.url })
    }

    /**
     * Coil delivers results through the (paused) Robolectric main looper, so
     * in-flight loads rarely land before a capture. Drop the decoded bitmaps
     * straight into the memory cache; AsyncImage then draws synchronously.
     */
    private fun prewarmMemoryCache(urls: List<String>) {
        val loader = coil.Coil.imageLoader(GlobalContext.get().get<Context>())
        val cache = loader.memoryCache ?: return
        urls.forEach { url ->
            val file = java.io.File(url.removePrefix("file://"))
            if (!file.exists()) return@forEach
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return@forEach
            // Coil keys file:// URIs by the bare path
            cache.set(
                coil.memory.MemoryCache.Key(file.absolutePath),
                coil.memory.MemoryCache.Value(bitmap),
            )
        }
    }

    @Test
    fun captureAllScreens() {
        // Warm every image into the memory cache before any UI exists:
        // on the JVM each decode is slow, and captures would race the loads.
        val context = GlobalContext.get().get<Context>()
        val loader = coil.Coil.imageLoader(context)
        val allUrls = (0 until 6).map { "cover_$it.png" } + (0 until 3).map { "pages/page_$it.png" }
        allUrls.forEach { rel ->
            val file = File(context.cacheDir, "shots/$rel")
            if (!file.exists()) return@forEach
            val done = java.util.concurrent.atomic.AtomicBoolean(false)
            loader.enqueue(
                coil.request.ImageRequest.Builder(context)
                    .data("file://" + file.absolutePath)
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, _ -> done.set(true) },
                        onError = { _, _ -> done.set(true) },
                    )
                    .build(),
            )
        }
        pump(20)

        setContentWithTheme()
        pump(2)

        // Library (start destination)
        pump(4)
        composeRule.onRoot().captureRoboImage(OUT + "library_light.png")

        // Explore
        navController?.navigate(ExploreRoute)
        pump(4)
        composeRule.onRoot().captureRoboImage(OUT + "explore_light.png")

        // Details
        navController?.navigate(MangaRoute("m0"))
        pump(4)
        composeRule.onRoot().captureRoboImage(OUT + "details_light.png")

        // Reader (dark)
        navController?.navigate(ReaderRoute(mangaId = "m0", chapterId = "m0ch2", page = 0))
        pump(5)
        composeRule.onRoot().captureRoboImage(OUT + "reader_dark.png")
    }

    private fun setContentWithTheme() {
        composeRule.setContent {
            KineraTheme {
                navController = rememberNavController()
                KineraApp(navController = navController!!)
            }
        }
    }

    /**
     * Coil delivers results on the Robolectric main looper, which is paused
     * during tests: plain sleeps never advance it. Pump the looper for a
     * while so async image loads actually land before the capture.
     */
    private fun pump(seconds: Long) {
        repeat((seconds * 10).toInt()) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            composeRule.waitForIdle()
            try {
                Thread.sleep(80)
            } catch (_: InterruptedException) {
            }
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeRule.waitForIdle()
    }

    companion object {
        private const val OUT = "build/outputs/roborazzi/"
    }
}

private val TITLES = listOf(
    "Whispering Blade", "Neon Alley", "Lantern Hill",
    "Stellar Forge", "Paper Crane", "Deep Current",
)

private val PALETTES = listOf(
    0xFF8A5300.toInt() to 0xFFFFDCAF.toInt(),
    0xFF55643B.toInt() to 0xFFD8EAB7.toInt(),
    0xFF673E00.toInt() to 0xFFFFB95C.toInt(),
    0xFF4E4436.toInt() to 0xFFEEE0CF.toInt(),
    0xFF715A3E.toInt() to 0xFFFCDDBB.toInt(),
    0xFF3F4C24.toInt() to 0xFFBDCE9D.toInt(),
)

private fun generateCovers(dir: File): List<Manga> = TITLES.mapIndexed { index, title ->
    val (top, bottom) = PALETTES[index % PALETTES.size]
    val file = File(dir, "cover_$index.png")
    if (!file.exists()) {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            0f, 0f, 200f, 300f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 300f, 450f, top, bottom, Shader.TileMode.CLAMP)
            },
        )
        val initial = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B1700.toInt()
            textSize = 150f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(title.take(1), 100f, 140f, initial)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B1700.toInt()
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, 100f, 177f, label)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    Manga(
        id = "m$index",
        source = MangaSource.MANGADEX,
        title = title,
        coverUrl = "file://${file.absolutePath}",
        description = "A demo description for $title, generated for the screenshot run. " +
            "Kinera keeps your library, history and bookmarks on the device.",
        state = MangaState.ONGOING,
        author = "A. Author",
        chaptersCount = 24,
    )
}

private fun generatePages(dir: File): List<MangaPage> {
    val pageDir = File(dir, "pages").apply { mkdirs() }
    return (0 until 3).map { index ->
        val file = File(pageDir, "page_$index.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(360, 560, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(0xFFF7EFDC.toInt())
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF201B12.toInt() }
            canvas.drawRoundRect(48f, 72f, 312f, 184f, 22f, 22f, ink)
            canvas.drawRoundRect(48f, 216f, 312f, 304f, 22f, 22f, ink)
            canvas.drawRoundRect(48f, 336f, 312f, 400f, 22f, 22f, ink)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A5300.toInt()
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Page ${index + 1} / 3", 180f, 500f, label)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        MangaPage(url = "file://${file.absolutePath}", index = index)
    }
}

/** Deterministic catalog used by the screenshot run. */
private class FakeCatalog(
    context: Context,
    private val mangas: List<Manga>,
    private val pages: List<MangaPage>,
) : CatalogRepository(
    mangadex = MangaDexSource(retrofitOf(MangaDexApi::class.java), SettingsRepository(context)),
    mangalib = MangaLibSource(
        retrofitOf(MangaLibApi::class.java),
        okhttp3.OkHttpClient(),
        SettingsRepository(context),
    ),
    remanga = RemangaSource(
        retrofitOf(RemangaApi::class.java),
        okhttp3.OkHttpClient(),
    ),
    desume = DesuMeSource(
        retrofitOf(DesuMeApi::class.java),
        okhttp3.OkHttpClient(),
    ),
    komga = KomgaSource(retrofitOf(KomgaApi::class.java), SettingsRepository(context)),
    suwayomi = SuwayomiSource(
        retrofitOf(SuwayomiApi::class.java),
        SettingsRepository(context),
        Json { ignoreUnknownKeys = true },
    ),
    local = LocalMangaSource(LocalSourceStorage(context)),
    folders = FolderMangaSource(FolderSourceStorage(context)),
) {

    override suspend fun popular(source: MangaSource, offset: Int): Paged<Manga> = paged(offset)

    override suspend fun latest(source: MangaSource, offset: Int): Paged<Manga> = paged(offset)

    override suspend fun search(
        query: String,
        source: MangaSource,
        offset: Int,
    ): Paged<Manga> = paged(offset)

    override suspend fun details(mangaId: String): Manga =
        mangas.firstOrNull { it.id == mangaId } ?: mangas.first()

    override suspend fun chapters(mangaId: String): List<MangaChapter> =
        mangas.mapIndexed { index, manga ->
            MangaChapter(
                id = "${manga.id}ch${index + 1}",
                source = manga.source,
                number = "${index + 1}",
                title = "The Promise",
                volume = null,
                scanlator = "Kinera Scans",
                uploadedAt = 1_700_000_000_000L,
                pagesCount = pages.size,
            )
        }

    override suspend fun pages(chapterId: String): List<MangaPage> = pages

    private fun paged(offset: Int) = Paged(
        items = mangas,
        offset = offset,
        total = mangas.size,
        hasNext = false,
    )
}

private inline fun <reified T> retrofitOf(clazz: Class<T>): T =
    Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(okhttp3.OkHttpClient())
        .addConverterFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }.asConverterFactory("application/json".toMediaType()),
        )
        .build()
        .create(clazz)
