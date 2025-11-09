package com.jodli.coffeeshottimer.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jodli.coffeeshottimer.ui.navigation.getBottomNavigationItems

/**
 * Navigation rail for landscape orientation following Material Design guidelines.
 *
 * This component provides:
 * - Vertical navigation layout optimized for landscape mode
 * - Better space utilization than bottom navigation in landscape
 * - Consistent navigation behavior with bottom navigation
 * - Material 3 design compliance
 */
@Composable
fun AppNavigationRail(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationRail {
        getBottomNavigationItems().forEach { item ->
            // Strip query parameters from current route for comparison
            val currentRouteBase = currentRoute?.substringBefore('?')

            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                selected = currentRouteBase == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination to avoid building up a large stack
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = false // Don't save state to avoid restoring stale filters
                        }
                        // Avoid multiple copies of the same destination when reselecting the same item
                        launchSingleTop = true
                        // Don't restore state to avoid bringing back filtered views
                        restoreState = false
                    }
                }
            )
        }
    }
}
