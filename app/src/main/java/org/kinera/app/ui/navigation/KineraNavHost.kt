package org.kinera.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.kinera.app.ui.screens.details.DetailsScreen
import org.kinera.app.ui.screens.downloads.DownloadsScreen
import org.kinera.app.ui.screens.explore.ExploreScreen
import org.kinera.app.ui.screens.history.HistoryScreen
import org.kinera.app.ui.screens.library.LibraryScreen
import org.kinera.app.ui.screens.reader.ReaderScreen
import org.kinera.app.ui.screens.search.SearchScreen
import org.kinera.app.ui.screens.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object LibraryRoute
@Serializable data object ExploreRoute
@Serializable data object DownloadsRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute
@Serializable data object SearchRoute
@Serializable data class MangaRoute(val mangaId: String)
@Serializable data class ReaderRoute(val mangaId: String, val chapterId: String, val page: Int = 0)

val topLevelRoutes = listOf(LibraryRoute, ExploreRoute, DownloadsRoute, HistoryRoute, SettingsRoute)

@Composable
fun KineraNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
        // M3E motion: content eases in with a short spring and exits fast;
        // slow bouncy transitions read as lag when hopping between tabs.
        enterTransition = {
            fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    initialScale = 0.97f,
                )
        },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = {
            fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    initialScale = 0.97f,
                )
        },
        popExitTransition = {
            fadeOut(tween(120)) +
                scaleOut(
                    animationSpec = tween(120),
                    targetScale = 0.985f,
                )
        },
    ) {
        composable<LibraryRoute> {
            LibraryScreen(
                onMangaClick = { navController.navigate(MangaRoute(it.id)) },
                onSearchClick = { navController.navigate(SearchRoute) },
            )
        }
        composable<ExploreRoute> {
            ExploreScreen(
                onMangaClick = { navController.navigate(MangaRoute(it.id)) },
                onSearchClick = { navController.navigate(SearchRoute) },
            )
        }
        composable<DownloadsRoute> {
            DownloadsScreen()
        }
        composable<HistoryRoute> {
            HistoryScreen(
                onOpenManga = { manga, chapterId ->
                    navController.navigate(ReaderRoute(manga.id, chapterId))
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen()
        }
        composable<SearchRoute> {
            SearchScreen(
                onMangaClick = { navController.navigate(MangaRoute(it.id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<MangaRoute> { entry ->
            val route = entry.toRoute<MangaRoute>()
            DetailsScreen(
                mangaId = route.mangaId,
                onBack = { navController.popBackStack() },
                onReadChapter = { _, chapter, page ->
                    navController.navigate(ReaderRoute(route.mangaId, chapter.id, page))
                },
            )
        }
        composable<ReaderRoute> { entry ->
            val route = entry.toRoute<ReaderRoute>()
            ReaderScreen(
                mangaId = route.mangaId,
                chapterId = route.chapterId,
                startPage = route.page,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
