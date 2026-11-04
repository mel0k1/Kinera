package org.kinera.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.kinera.app.R
import org.kinera.app.ui.components.pressScale

private data class KineraNavItem(
    val labelRes: Int,
    val active: ImageVector,
    val inactive: ImageVector,
)

private val navItems = listOf(
    KineraNavItem(R.string.nav_library, Icons.Rounded.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
    KineraNavItem(R.string.nav_explore, Icons.Rounded.Explore, Icons.Outlined.Explore),
    KineraNavItem(R.string.nav_downloads, Icons.Rounded.Download, Icons.Outlined.Download),
    KineraNavItem(R.string.nav_history, Icons.Rounded.History, Icons.Outlined.History),
    KineraNavItem(R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
)

private fun navItemFor(route: Any?): KineraNavItem? = when (route) {
    is LibraryRoute -> navItems[0]
    is ExploreRoute -> navItems[1]
    is DownloadsRoute -> navItems[2]
    is HistoryRoute -> navItems[3]
    is SettingsRoute -> navItems[4]
    else -> null
}

@Composable
fun KineraApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedRoute = topLevelRoutes.firstOrNull { destination?.hasRoute(it::class) == true }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = selectedRoute != null,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it },
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                ) { it },
            ) {
                KineraNavBar(
                    selectedRoute = selectedRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        KineraNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        )
    }
}

/**
 * Floating pill navigation bar (M3E): the selected item expands into a
 * soft container pill with a label, unselected items stay icon-only.
 */
@Composable
private fun KineraNavBar(
    selectedRoute: Any?,
    onSelect: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 3.dp,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            topLevelRoutes.forEach { route ->
                val item = navItemFor(route) ?: return@forEach
                NavPill(
                    selected = route::class == selectedRoute?.let { it::class },
                    label = stringResource(item.labelRes),
                    activeIcon = item.active,
                    inactiveIcon = item.inactive,
                    onClick = { onSelect(route) },
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    selected: Boolean,
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>(),
        label = "navPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>(),
        label = "navPillContent",
    )

    Row(
        modifier = Modifier
            .pressScale(pressedScale = 0.94f, interactionSource = interaction)
            .clip(CircleShape)
            .background(container, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkHorizontally(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
