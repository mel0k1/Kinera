package org.kinera.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.kinera.app.R

@Composable
fun KineraApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isTopLevel = topLevelRoutes.any { destination?.hasRoute(it::class) == true }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevel,
                enter = slideInVertically(spring()) { it },
                exit = slideOutVertically(spring()) { it },
            ) {
                NavigationBar {
                    topLevelRoutes.forEach { route ->
                        val selected = destination?.hasRoute(route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (route) {
                                        is LibraryRoute -> Icons.Rounded.CollectionsBookmark
                                        is ExploreRoute -> Icons.Rounded.Explore
                                        is DownloadsRoute -> Icons.Rounded.Download
                                        is HistoryRoute -> Icons.Rounded.History
                                        else -> Icons.Rounded.Settings
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    text = when (route) {
                                        is LibraryRoute -> stringResource(R.string.nav_library)
                                        is ExploreRoute -> stringResource(R.string.nav_explore)
                                        is DownloadsRoute -> stringResource(R.string.nav_downloads)
                                        is HistoryRoute -> stringResource(R.string.nav_history)
                                        else -> stringResource(R.string.nav_settings)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        KineraNavHost(
            navController = navController,
            modifier = Modifier.padding(padding),
        )
    }
}
