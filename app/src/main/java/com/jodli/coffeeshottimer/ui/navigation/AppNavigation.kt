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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.ui.screens.AddEditBeanScreen
import com.jodli.coffeeshottimer.ui.screens.BeanManagementScreen
import com.jodli.coffeeshottimer.ui.screens.IntroductionScreen
import com.jodli.coffeeshottimer.ui.screens.RecordShotScreen
import com.jodli.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.jodli.coffeeshottimer.ui.screens.ShotHistoryScreen
import com.jodli.coffeeshottimer.ui.screens.EquipmentSetupFlowScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavigationDestinations.RecordShot.route
) {
    // Root-scoped view model to update onboarding state when skipping
    val mainActivityViewModel: com.jodli.coffeeshottimer.ui.viewmodel.MainActivityViewModel = hiltViewModel()

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
                    // Mark onboarding as complete when skipping from introduction
                    mainActivityViewModel.completeOnboarding()
                    navController.navigate(NavigationDestinations.RecordShot.route) {
                        // Clear onboarding from back stack when skipping
                        popUpTo(NavigationDestinations.OnboardingIntroduction.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationDestinations.OnboardingEquipmentSetup.route) {
            EquipmentSetupFlowScreen(
                onComplete = {
                    navController.navigate(NavigationDestinations.OnboardingGuidedBeanCreation.route)
                },
                onSkip = {
                    // Skip directly to guided bean creation
                    navController.navigate(NavigationDestinations.OnboardingGuidedBeanCreation.route)
                }
            )
        }

        composable(NavigationDestinations.OnboardingGuidedBeanCreation.route) {
            com.jodli.coffeeshottimer.ui.screens.GuidedBeanCreationScreen(
                onComplete = { bean ->
                    navController.navigate(NavigationDestinations.RecordShot.route) {
                        // Clear all onboarding screens from back stack after completion
                        popUpTo(NavigationDestinations.OnboardingIntroduction.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(NavigationDestinations.RecordShot.route) {
                        // Clear all onboarding screens from back stack when skipping
                        popUpTo(NavigationDestinations.OnboardingIntroduction.route) { inclusive = true }
                    }
                }
            )
        }

        // More tab
        composable(NavigationDestinations.More.route) {
            com.jodli.coffeeshottimer.ui.screens.MoreScreen(
                onNavigateToGrinderSettings = {
                    navController.navigate(NavigationDestinations.EquipmentSettings.route)
                },
                onNavigateToBasketSettings = {
                    navController.navigate(NavigationDestinations.BasketSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(NavigationDestinations.About.route)
                }
            )
        }

        // Grinder settings (accessible outside onboarding)
        composable(NavigationDestinations.EquipmentSettings.route) {
            com.jodli.coffeeshottimer.ui.screens.GrinderSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Basket settings
        composable(NavigationDestinations.BasketSettings.route) {
            com.jodli.coffeeshottimer.ui.screens.BasketSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // About screen
        composable(NavigationDestinations.About.route) {
            com.jodli.coffeeshottimer.ui.screens.AboutScreen(
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

