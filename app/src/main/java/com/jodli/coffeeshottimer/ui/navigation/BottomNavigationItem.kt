package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
        route = NavigationDestinations.RecordShot.route,
        icon = Icons.Default.Coffee,
        label = "New Shot"
    ),
    BottomNavigationItem(
        route = NavigationDestinations.ShotHistory.route,
        icon = Icons.AutoMirrored.Filled.List,
        label = "History"
    ),
    BottomNavigationItem(
        route = NavigationDestinations.BeanManagement.route,
        icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
        label = "Beans"
    )
)