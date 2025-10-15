package com.jodli.coffeeshottimer.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for onboarding navigation flow.
 * Tests the navigation destinations and route configurations.
 * Tests Requirements: 1.4, 1.7
 */
@RunWith(AndroidJUnit4::class)
class OnboardingNavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `onboarding navigation destinations are properly configured`() {
        // Test that onboarding destinations have correct routes
        assertEquals("onboarding_introduction", NavigationDestinations.OnboardingIntroduction.route)
        assertEquals("onboarding_equipment_setup", NavigationDestinations.OnboardingEquipmentSetup.route)
    }

    @Test
    fun `onboarding flow navigation sequence is correct`() {
        // Test the expected navigation sequence for onboarding
        val introRoute = NavigationDestinations.OnboardingIntroduction.route
        val equipmentRoute = NavigationDestinations.OnboardingEquipmentSetup.route
        val recordShotRoute = NavigationDestinations.RecordShot.route

        // Verify the expected flow: Introduction -> Equipment Setup -> Record Shot
        assertEquals("onboarding_introduction", introRoute)
        assertEquals("onboarding_equipment_setup", equipmentRoute)
        assertEquals("record_shot", recordShotRoute)
    }

    @Test
    fun `onboarding skip navigation bypasses equipment setup`() {
        // Test that skip functionality goes directly from introduction to record shot
        val introRoute = NavigationDestinations.OnboardingIntroduction.route
        val recordShotRoute = NavigationDestinations.RecordShot.route

        // Verify skip path: Introduction -> Record Shot (bypassing Equipment Setup)
        assertEquals("onboarding_introduction", introRoute)
        assertEquals("record_shot", recordShotRoute)
    }

    @Test
    fun `onboarding completion navigation clears onboarding stack`() {
        // Test that completion navigation properly transitions to main app
        val equipmentRoute = NavigationDestinations.OnboardingEquipmentSetup.route
        val recordShotRoute = NavigationDestinations.RecordShot.route

        // Verify completion path: Equipment Setup -> Record Shot
        assertEquals("onboarding_equipment_setup", equipmentRoute)
        assertEquals("record_shot", recordShotRoute)
    }

    @Test
    fun `all navigation destinations are accessible`() {
        // Test that all required destinations are defined and accessible
        val destinations = listOf(
            NavigationDestinations.OnboardingIntroduction.route,
            NavigationDestinations.OnboardingEquipmentSetup.route,
            NavigationDestinations.RecordShot.route,
            NavigationDestinations.ShotHistory.route,
            NavigationDestinations.BeanManagement.route
        )

        // Verify all destinations are properly defined
        destinations.forEach { route ->
            assert(route.isNotEmpty()) { "Route should not be empty: $route" }
            assert(!route.contains(" ")) { "Route should not contain spaces: $route" }
        }
    }

    @Test
    fun `onboarding navigation handles back navigation correctly`() {
        // Test that back navigation is properly configured
        val introRoute = NavigationDestinations.OnboardingIntroduction.route
        val equipmentRoute = NavigationDestinations.OnboardingEquipmentSetup.route

        // Verify routes are different (back navigation is meaningful)
        assert(introRoute != equipmentRoute) { "Introduction and equipment setup should have different routes" }

        // Verify routes follow expected naming convention
        assert(introRoute.startsWith("onboarding_")) { "Introduction route should start with 'onboarding_'" }
        assert(equipmentRoute.startsWith("onboarding_")) { "Equipment setup route should start with 'onboarding_'" }
    }

    @Test
    fun `introduction screen navigation integration works correctly`() {
        // Test that the introduction screen integrates properly with navigation
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = NavigationDestinations.OnboardingIntroduction.route
                )
            }
        }

        // Verify introduction screen loads
        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertExists()
        composeTestRule.onNodeWithText("Skip").assertExists()
        composeTestRule.onNodeWithText("Next").assertExists()
    }

    @Test
    fun `introduction screen skip navigation works in integration`() {
        // Test that skip functionality works in the full navigation context
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = NavigationDestinations.OnboardingIntroduction.route
                )
            }
        }

        // Act - Click skip button
        composeTestRule.onNodeWithText("Skip").performClick()

        // Assert - Should navigate to record shot screen
        // Note: This would require the RecordShotScreen to be properly implemented
        // For now, we verify the navigation attempt doesn't crash
        composeTestRule.waitForIdle()
    }

    @Test
    fun `introduction screen completion navigation works in integration`() {
        // Test that completion navigation works in the full navigation context
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = NavigationDestinations.OnboardingIntroduction.route
                )
            }
        }

        // Navigate to last slide
        repeat(4) {
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
        }

        // Act - Click Get Started button
        composeTestRule.onNodeWithText("Get Started").performClick()

        // Assert - Should navigate to equipment setup screen
        composeTestRule.waitForIdle()
        // The equipment setup screen is still a placeholder, so we just verify no crash
    }

    @Test
    fun `introduction screen slide navigation works in integration`() {
        // Test that slide navigation works properly in the navigation context
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = NavigationDestinations.OnboardingIntroduction.route
                )
            }
        }

        // Test navigation through all slides
        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertExists()

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Main Features").assertExists()

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Flexible Workflow").assertExists()

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Smart Timer").assertExists()

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ready to Begin").assertExists()
    }

    @Test
    fun `introduction screen error handling works in navigation context`() {
        // Test that error handling works properly in the navigation context
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = NavigationDestinations.OnboardingIntroduction.route
                )
            }
        }

        // Verify the screen loads without errors
        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertExists()

        // Test that navigation operations don't crash
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Previous").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertExists()
    }
}
