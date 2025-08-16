package com.jodli.coffeeshottimer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jodli.coffeeshottimer.ui.components.BottomNavigationBar
import com.jodli.coffeeshottimer.ui.navigation.AppNavigation
import com.jodli.coffeeshottimer.ui.navigation.NavigationDestinations
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.viewmodel.MainActivityViewModel
import com.jodli.coffeeshottimer.ui.viewmodel.RoutingState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoffeeShotTimerTheme {
                EspressoShotTrackerApp(mainActivityViewModel)
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes (orientation, screen size, etc.)
        // Compose will automatically recompose when configuration changes
        // No additional handling needed as ViewModels preserve state
    }
}

@Composable
fun EspressoShotTrackerApp(mainActivityViewModel: MainActivityViewModel) {
    val routingState by mainActivityViewModel.routingState.collectAsState()

    when (val state = routingState) {
        is RoutingState.Loading -> {
            // Show loading indicator while determining route
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is RoutingState.Success -> {
            // Create a fresh NavController keyed by the start destination to avoid unwanted transitions
            androidx.compose.runtime.key(state.route) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Determine if bottom navigation should be shown
                val showBottomNavigation = when (currentRoute) {
                    NavigationDestinations.RecordShot.route,
                    NavigationDestinations.ShotHistory.route,
                    NavigationDestinations.BeanManagement.route,
                    NavigationDestinations.More.route -> true
                    // Hide bottom navigation during onboarding
                    NavigationDestinations.OnboardingIntroduction.route,
                    NavigationDestinations.OnboardingEquipmentSetup.route -> false
                    else -> false
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNavigation) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        startDestination = state.route
                    )
                }
            }
        }
        is RoutingState.Error -> {
            // On error, show app with fallback route without transitions
            val fallback = state.fallbackRoute
            androidx.compose.runtime.key(fallback) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomNavigation = when (currentRoute) {
                    NavigationDestinations.RecordShot.route,
                    NavigationDestinations.ShotHistory.route,
                    NavigationDestinations.BeanManagement.route,
                    NavigationDestinations.More.route -> true
                    NavigationDestinations.OnboardingIntroduction.route,
                    NavigationDestinations.OnboardingEquipmentSetup.route -> false
                    else -> false
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNavigation) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        startDestination = fallback
                    )
                }
            }
        }
    }
}
