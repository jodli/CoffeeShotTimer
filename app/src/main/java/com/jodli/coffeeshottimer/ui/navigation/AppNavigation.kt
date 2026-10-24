package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jodli.coffeeshottimer.ui.screens.AddEditBeanScreen
import com.jodli.coffeeshottimer.ui.screens.BeanManagementScreen
import com.jodli.coffeeshottimer.ui.screens.EquipmentSetupFlowScreen
import com.jodli.coffeeshottimer.ui.screens.IntroductionScreen
import com.jodli.coffeeshottimer.ui.screens.RecordShotScreen
import com.jodli.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.jodli.coffeeshottimer.ui.screens.ShotHistoryScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavigationDestinations.RecordShot.route
) {
    // Root-scoped view model to update onboarding state when skipping
    val mainActivityViewModel: com.jodli.coffeeshottimer.ui.viewmodel.MainActivityViewModel = hiltViewModel()

    // Leave onboarding for Record Shot. Onboarding can start on the introduction,
    // equipment setup or guided bean creation, so pop the graph's start destination
    // rather than a fixed route.
    val exitOnboarding: () -> Unit = {
        navController.navigate(NavigationDestinations.RecordShot.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        }
    }

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

        composable(
            route = NavigationDestinations.ShotHistory.route,
            arguments = listOf(
                androidx.navigation.navArgument(NavigationDestinations.ShotHistory.BEAN_ID_ARG) {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getString(NavigationDestinations.ShotHistory.BEAN_ID_ARG)
            ShotHistoryScreen(
                onShotClick = { shotId ->
                    navController.navigate(NavigationDestinations.ShotDetails.createRoute(shotId))
                },
                initialBeanIdFilter = beanId
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
                onNavigateToShotHistory = { beanId ->
                    navController.navigate(NavigationDestinations.ShotHistory.createRoute(beanId))
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
                    exitOnboarding()
                }
            )
        }

        composable(NavigationDestinations.OnboardingEquipmentSetup.route) {
            EquipmentSetupFlowScreen(
                onComplete = {
                    // Check if the user should skip bean creation (they already have beans)
                    mainActivityViewModel.handleEquipmentSetupComplete { shouldSkipBeanCreation ->
                        if (shouldSkipBeanCreation) {
                            // User has beans - go directly to main app
                            exitOnboarding()
                        } else {
                            // User needs beans - continue to bean creation
                            navController.navigate(NavigationDestinations.OnboardingGuidedBeanCreation.route)
                        }
                    }
                }
            )
        }

        composable(NavigationDestinations.OnboardingGuidedBeanCreation.route) {
            com.jodli.coffeeshottimer.ui.screens.GuidedBeanCreationScreen(
                onComplete = { exitOnboarding() },
                onSkip = { exitOnboarding() }
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
