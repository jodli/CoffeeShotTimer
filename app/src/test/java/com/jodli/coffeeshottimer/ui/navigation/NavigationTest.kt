package com.jodli.coffeeshottimer.ui.navigation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for navigation destinations and configuration
 */
class NavigationTest {

    @Test
    fun `navigation destinations have correct routes`() {
        assertEquals("record_shot", NavigationDestinations.RecordShot.route)
        assertEquals("shot_history", NavigationDestinations.ShotHistory.route)
        assertEquals("bean_management", NavigationDestinations.BeanManagement.route)
        assertEquals("shot_details/{shotId}", NavigationDestinations.ShotDetails.route)
        assertEquals("add_edit_bean?beanId={beanId}", NavigationDestinations.AddEditBean.route)
    }

    @Test
    fun `shot details creates correct route with parameter`() {
        val shotId = "test-shot-123"
        val expectedRoute = "shot_details/$shotId"
        assertEquals(expectedRoute, NavigationDestinations.ShotDetails.createRoute(shotId))
    }

    @Test
    fun `add edit bean creates correct routes`() {
        // Test add new bean (no ID)
        assertEquals("add_edit_bean", NavigationDestinations.AddEditBean.createRoute())
        
        // Test edit existing bean (with ID)
        val beanId = "test-bean-456"
        val expectedRoute = "add_edit_bean?beanId=$beanId"
        assertEquals(expectedRoute, NavigationDestinations.AddEditBean.createRoute(beanId))
    }

    @Test
    fun `bottom navigation items are configured correctly`() {
        assertEquals(3, bottomNavigationItems.size)
        
        val recordShotItem = bottomNavigationItems[0]
        assertEquals(NavigationDestinations.RecordShot.route, recordShotItem.route)
        assertEquals("Record Shot", recordShotItem.label)
        
        val historyItem = bottomNavigationItems[1]
        assertEquals(NavigationDestinations.ShotHistory.route, historyItem.route)
        assertEquals("History", historyItem.label)
        
        val beansItem = bottomNavigationItems[2]
        assertEquals(NavigationDestinations.BeanManagement.route, beansItem.route)
        assertEquals("Beans", beansItem.label)
    }
}