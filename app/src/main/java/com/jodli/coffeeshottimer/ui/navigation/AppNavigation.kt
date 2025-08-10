package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jodli.coffeeshottimer.ui.screens.AddEditBeanScreen
import com.jodli.coffeeshottimer.ui.screens.BeanManagementScreen
import com.jodli.coffeeshottimer.ui.screens.IntroductionScreen
import com.jodli.coffeeshottimer.ui.screens.RecordShotScreen
import com.jodli.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.jodli.coffeeshottimer.ui.screens.ShotHistoryScreen
import com.jodli.coffeeshottimer.ui.screens.EquipmentSetupScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavigationDestinations.RecordShot.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Main bottom navigation screens
        composable(NavigationDestinations.RecordShot.route) {
            RecordShotScreen(
                onNavigateToBeanManagement = {
                    navController.navigate(NavigationDestinations.BeanManagement.route)
                },
                onNavigateToShotDetails = { shotId ->
                    navController.navigate(NavigationDestinations.ShotDetails.createRoute(shotId))
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

        // Onboarding screens
        composable(NavigationDestinations.OnboardingIntroduction.route) {
            IntroductionScreen(
                onComplete = {
                    // Allow normal navigation to equipment setup, keeping introduction in back stack
                    navController.navigate(NavigationDestinations.OnboardingEquipmentSetup.route)
                },
                onSkip = {
                    navController.navigate(NavigationDestinations.RecordShot.route) {
                        // Clear onboarding from back stack when skipping
                        popUpTo(NavigationDestinations.OnboardingIntroduction.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationDestinations.OnboardingEquipmentSetup.route) {
            EquipmentSetupScreen(
                onComplete = { _ ->
                    navController.navigate(NavigationDestinations.RecordShot.route) {
                        // Clear all onboarding screens from back stack after completion
                        popUpTo(NavigationDestinations.OnboardingIntroduction.route) { inclusive = true }
                    }
                },
                onBack = {
                    // Allow going back to introduction if user wants to review
                    navController.popBackStack()
                }
            )
        }

        // More tab
        composable(NavigationDestinations.More.route) {
            com.jodli.coffeeshottimer.ui.screens.MoreScreen(
                onNavigateToEquipmentSettings = {
                    navController.navigate(NavigationDestinations.EquipmentSettings.route)
                }
            )
        }

        // Equipment settings (accessible outside onboarding)
        composable(NavigationDestinations.EquipmentSettings.route) {
            com.jodli.coffeeshottimer.ui.screens.EquipmentSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
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

