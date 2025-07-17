package com.example.coffeeshottimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coffeeshottimer.ui.screens.AddEditBeanScreen
import com.example.coffeeshottimer.ui.screens.BeanManagementScreen
import com.example.coffeeshottimer.ui.screens.RecordShotScreen
import com.example.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.example.coffeeshottimer.ui.screens.ShotHistoryScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestinations.RecordShot.route,
        modifier = modifier
    ) {
        // Main bottom navigation screens
        composable(NavigationDestinations.RecordShot.route) {
            RecordShotScreen(
                onNavigateToBeanManagement = {
                    navController.navigate(NavigationDestinations.BeanManagement.route)
                }
            )
        }
        
        composable(NavigationDestinations.ShotHistory.route) {
            ShotHistoryScreen(
                onShotClick = { shotId ->
                    navController.navigate(NavigationDestinations.ShotDetails.createRoute(shotId))
                }
            )
        }
        
        composable(NavigationDestinations.BeanManagement.route) {
            BeanManagementScreen(
                onAddBeanClick = {
                    navController.navigate(NavigationDestinations.AddEditBean.createRoute())
                },
                onEditBeanClick = { beanId ->
                    navController.navigate(NavigationDestinations.AddEditBean.createRoute(beanId))
                },
                onNavigateToRecordShot = {
                    navController.navigate(NavigationDestinations.RecordShot.route)
                }
            )
        }
        
        // Modal screens
        composable(
            route = NavigationDestinations.ShotDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument("shotId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val shotId = backStackEntry.arguments?.getString("shotId") ?: ""
            ShotDetailsScreen(
                shotId = shotId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = NavigationDestinations.AddEditBean.route,
            arguments = listOf(
                androidx.navigation.navArgument("beanId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getString("beanId")
            AddEditBeanScreen(
                beanId = beanId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}