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
import com.jodli.coffeeshottimer.ui.screens.RecordShotScreen
import com.jodli.coffeeshottimer.ui.screens.ShotDetailsScreen
import com.jodli.coffeeshottimer.ui.screens.ShotHistoryScreen

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

        // Onboarding screens (placeholder implementations)
        composable(NavigationDestinations.OnboardingIntroduction.route) {
            OnboardingIntroductionScreen(
                onComplete = {
                    navController.navigate(NavigationDestinations.OnboardingEquipmentSetup.route)
                },
                onSkip = {
                    navController.navigate(NavigationDestinations.RecordShot.route)
                }
            )
        }

        composable(NavigationDestinations.OnboardingEquipmentSetup.route) {
            OnboardingEquipmentSetupScreen(
                onComplete = {
                    navController.navigate(NavigationDestinations.RecordShot.route)
                },
                onBack = {
                    navController.popBackStack()
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

// Placeholder composables for onboarding screens
// These will be replaced with actual implementations in future tasks

@Composable
private fun OnboardingIntroductionScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Onboarding Introduction Screen (Placeholder)",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun OnboardingEquipmentSetupScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Onboarding Equipment Setup Screen (Placeholder)",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}