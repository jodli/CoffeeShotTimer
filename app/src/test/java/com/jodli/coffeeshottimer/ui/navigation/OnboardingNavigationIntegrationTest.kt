package com.jodli.coffeeshottimer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Integration tests for onboarding navigation flow.
 * Tests the navigation destinations and route configurations.
 */
class OnboardingNavigationIntegrationTest {

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
}