package com.jodli.coffeeshottimer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController

/**
 * Adaptive navigation component that switches between bottom navigation and navigation rail
 * based on device orientation following Material Design best practices.
 *
 * Navigation patterns:
 * - Portrait: Bottom Navigation Bar (familiar mobile pattern)
 * - Landscape: Navigation Rail (better space utilization, follows Material 3 guidelines)
 *
 * This approach:
 * - Maximizes available content area in landscape mode
 * - Provides consistent navigation behavior across orientations
 * - Follows Material Design adaptive layout principles
 * - Improves UX for different screen aspect ratios
 *
 * @param navController NavController for navigation handling
 */
@Composable
fun AdaptiveNavigation(
    navController: NavController
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        AppNavigationRail(navController = navController)
    } else {
        BottomNavigationBar(navController = navController)
    }
}
