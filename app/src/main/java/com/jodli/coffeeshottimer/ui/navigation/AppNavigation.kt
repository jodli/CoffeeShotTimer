package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jodli.coffeeshottimer.ui.screens.AddEditBeanScreen
import com.jodli.coffeeshottimer.ui.screens.BeanManagementScreen
import com.jodli.coffeeshottimer.ui.screens.RecordShotScreen
import com.jodli.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.jodli.coffeeshottimer.ui.screens.ShotHistoryScreen

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
                },
                onNavigateToShot = { newShotId ->
                    navController.navigate(NavigationDestinations.ShotDetails.createRoute(newShotId)) {
                        popUpTo(NavigationDestinations.ShotDetails.route) { inclusive = true }
                    }
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