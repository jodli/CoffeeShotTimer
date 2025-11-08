package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.jodli.coffeeshottimer.R

/**
 * Bottom navigation items configuration
 */
data class BottomNavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun getBottomNavigationItems(): List<BottomNavigationItem> = listOf(
    BottomNavigationItem(
        route = NavigationDestinations.RecordShot.baseRoute,
        icon = Icons.Default.Coffee,
        label = stringResource(R.string.view_home)
    ),
    BottomNavigationItem(
        route = NavigationDestinations.ShotHistory.createRoute(null), // Navigate to shot history without filter
        icon = Icons.AutoMirrored.Filled.List,
        label = stringResource(R.string.view_history)
    ),
    BottomNavigationItem(
        route = NavigationDestinations.BeanManagement.baseRoute,
        icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
        label = stringResource(R.string.view_beans)
    ),
    BottomNavigationItem(
        route = NavigationDestinations.More.baseRoute,
        icon = Icons.Default.Settings,
        label = stringResource(R.string.view_more)
    )
)
