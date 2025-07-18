package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items configuration
 */
data class BottomNavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavigationItems = listOf(
    BottomNavigationItem(
        route = NavigationDestinations.RecordShot.route,
        icon = Icons.Default.PlayArrow,
        label = "Record Shot"
    ),
    BottomNavigationItem(
        route = NavigationDestinations.ShotHistory.route,
        icon = Icons.Default.List,
        label = "History"
    ),
    BottomNavigationItem(
        route = NavigationDestinations.BeanManagement.route,
        icon = Icons.Default.Settings,
        label = "Beans"
    )
)