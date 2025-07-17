package com.example.coffeeshottimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.coffeeshottimer.ui.components.BottomNavigationBar
import com.example.coffeeshottimer.ui.navigation.AppNavigation
import com.example.coffeeshottimer.ui.navigation.NavigationDestinations
import com.example.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoffeeShotTimerTheme {
                EspressoShotTrackerApp()
            }
        }
    }
}

@Composable
fun EspressoShotTrackerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom navigation should be shown
    val showBottomNavigation = when (currentRoute) {
        NavigationDestinations.RecordShot.route,
        NavigationDestinations.ShotHistory.route,
        NavigationDestinations.BeanManagement.route -> true
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}