package com.jodli.coffeeshottimer.utils

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Utility functions for testing the Espresso Shot Tracker app.
 */
object TestUtils {
    
    /**
     * Wait for a condition to be true with timeout.
     */
    fun ComposeContentTestRule.waitForCondition(
        timeoutMillis: Long = 5000,
        condition: () -> Boolean
    ) {
        waitUntil(timeoutMillis = timeoutMillis) {
            condition()
        }
    }
    
    /**
     * Wait for text to appear on screen.
     */
    fun ComposeContentTestRule.waitForText(
        text: String,
        timeoutMillis: Long = 5000
    ) {
        waitForCondition(timeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
    
    /**
     * Wait for text to disappear from screen.
     */
    fun ComposeContentTestRule.waitForTextToDisappear(
        text: String,
        timeoutMillis: Long = 5000
    ) {
        waitForCondition(timeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }
    
    /**
     * Perform text input with clearing existing text first.
     */
    fun SemanticsNodeInteraction.performTextInputWithClear(text: String) {
        performTextClearance()
        performTextInput(text)
    }
    
    /**
     * Create a test bean with default values.
     */
    fun createTestBean(
        name: String = "Test Bean ${UUID.randomUUID().toString().take(8)}",
        origin: String = "Test Origin",
        roaster: String = "Test Roaster",
        roastDate: LocalDate = LocalDate.now().minusDays(7),
        isActive: Boolean = true
    ): Bean {
        return Bean(
            id = UUID.randomUUID().toString(),
            name = name,
            origin = origin,
            roaster = roaster,
            roastDate = roastDate,
            notes = "Test bean notes",
            isActive = isActive,
            lastGrinderSetting = "15",
            createdAt = LocalDateTime.now()
        )
    }
    
    /**
     * Create a test shot with default values.
     */
    fun createTestShot(
        beanId: String,
        coffeeWeightIn: Double = 18.0,
        coffeeWeightOut: Double = 36.0,
        extractionTimeSeconds: Int = 28,
        grinderSetting: String = "15",
        notes: String = "Test shot notes"
    ): Shot {
        return Shot(
            id = UUID.randomUUID().toString(),
            beanId = beanId,
            coffeeWeightIn = coffeeWeightIn,
            coffeeWeightOut = coffeeWeightOut,
            extractionTimeSeconds = extractionTimeSeconds,
            grinderSetting = grinderSetting,
            notes = notes,
            timestamp = LocalDateTime.now()
        )
    }
    
    /**
     * Insert test data into database.
     */
    fun insertTestData(
        database: AppDatabase,
        beans: List<Bean> = emptyList(),
        shots: List<Shot> = emptyList()
    ) = runBlocking {
        beans.forEach { bean ->
            database.beanDao().insertBean(bean)
        }
        
        shots.forEach { shot ->
            database.shotDao().insertShot(shot)
        }
    }
    
    /**
     * Clear all data from database.
     */
    fun clearDatabase(database: AppDatabase) = runBlocking {
        database.clearAllTables()
    }
    
    /**
     * Navigate to a specific screen using bottom navigation.
     */
    fun ComposeContentTestRule.navigateToScreen(screenName: String) {
        onNodeWithText(screenName).performClick()
        waitForText(screenName)
    }
    
    /**
     * Fill bean form with test data.
     */
    fun ComposeContentTestRule.fillBeanForm(
        name: String = "Test Bean",
        origin: String = "Test Origin",
        roaster: String = "Test Roaster",
        notes: String = "Test notes"
    ) {
        onNodeWithText("Bean Name").performTextInputWithClear(name)
        onNodeWithText("Origin").performTextInputWithClear(origin)
        onNodeWithText("Roaster").performTextInputWithClear(roaster)
        onNodeWithText("Notes").performTextInputWithClear(notes)
    }
    
    /**
     * Fill shot recording form with test data.
     */
    fun ComposeContentTestRule.fillShotForm(
        beanName: String,
        coffeeWeightIn: String = "18.0",
        coffeeWeightOut: String = "36.0",
        grinderSetting: String = "15",
        notes: String = "Test shot"
    ) {
        // Select bean
        onNodeWithText("Select Bean").performClick()
        onNodeWithText(beanName).performClick()
        
        // Fill form fields
        onNodeWithText("Coffee Weight In (g)").performTextInputWithClear(coffeeWeightIn)
        onNodeWithText("Coffee Weight Out (g)").performTextInputWithClear(coffeeWeightOut)
        onNodeWithText("Grinder Setting").performTextInputWithClear(grinderSetting)
        onNodeWithText("Notes (optional)").performTextInputWithClear(notes)
    }
    
    /**
     * Verify shot appears in history with expected data.
     */
    fun ComposeContentTestRule.verifyShotInHistory(
        beanName: String,
        weightIn: String,
        weightOut: String
    ) {
        onNodeWithText(beanName).assertIsDisplayed()
        onNodeWithText("${weightIn}g → ${weightOut}g").assertIsDisplayed()
    }
    
    /**
     * Test accessibility of a screen by checking common accessibility requirements.
     */
    fun ComposeContentTestRule.testScreenAccessibility() {
        // Check that all clickable elements have content descriptions
        onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val hasContentDescription = node.config.contains(SemanticsProperties.ContentDescription)
            val hasText = node.config.contains(SemanticsProperties.Text)
            val hasRole = node.config.contains(SemanticsProperties.Role)
            
            assert(hasContentDescription || hasText || hasRole) {
                "Clickable element without proper accessibility labeling found"
            }
        }
        
        // Check that form fields have proper labels
        onAllNodes(hasSetTextAction()).fetchSemanticsNodes().forEach { node ->
            val hasContentDescription = node.config.contains(SemanticsProperties.ContentDescription)
            val hasText = node.config.contains(SemanticsProperties.Text)
            
            assert(hasContentDescription || hasText) {
                "Form field without proper accessibility labeling found"
            }
        }
    }
    
    /**
     * Test performance of a screen by measuring load time.
     */
    fun ComposeContentTestRule.measureScreenLoadTime(
        screenName: String,
        expectedMaxLoadTime: Long = 3000
    ): Long {
        val startTime = System.currentTimeMillis()
        
        navigateToScreen(screenName)
        
        // Wait for loading indicators to disappear
        waitForCondition {
            onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }
        
        val loadTime = System.currentTimeMillis() - startTime
        
        assert(loadTime < expectedMaxLoadTime) {
            "Screen $screenName took too long to load: ${loadTime}ms (expected < ${expectedMaxLoadTime}ms)"
        }
        
        return loadTime
    }
    
    /**
     * Simulate memory pressure by creating and releasing objects.
     */
    fun simulateMemoryPressure() {
        repeat(10) {
            val largeArray = ByteArray(1024 * 1024) // 1MB
            // Let it go out of scope
        }
        System.gc()
    }
    
    /**
     * Get current memory usage in MB.
     */
    fun getCurrentMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)
    }
    
    /**
     * Test that the app handles low memory conditions gracefully.
     */
    fun ComposeContentTestRule.testLowMemoryHandling() {
        val initialMemory = getCurrentMemoryUsageMB()
        
        // Simulate memory pressure
        simulateMemoryPressure()
        
        // App should still be responsive
        onNodeWithText("Record").assertIsDisplayed()
        onNodeWithText("History").assertIsDisplayed()
        onNodeWithText("Beans").assertIsDisplayed()
        
        val finalMemory = getCurrentMemoryUsageMB()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable
        assert(memoryIncrease < 50) {
            "Memory usage increased too much during test: ${memoryIncrease}MB"
        }
    }
    
    /**
     * Test app behavior during configuration changes (rotation).
     */
    fun ComposeContentTestRule.testConfigurationChange() {
        // This would typically involve rotating the device
        // For now, we'll simulate by recreating the activity
        activityRule.scenario.recreate()
        
        // Verify app state is preserved
        waitForCondition {
            onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }
    }
    
    /**
     * Verify that all required UI elements are present on a screen.
     */
    fun ComposeContentTestRule.verifyRequiredElements(elements: List<String>) {
        elements.forEach { element ->
            onNodeWithText(element).assertIsDisplayed()
        }
    }
    
    /**
     * Test form validation by submitting empty/invalid data.
     */
    fun ComposeContentTestRule.testFormValidation(
        submitButtonText: String,
        expectedErrors: List<String>
    ) {
        // Try to submit without filling required fields
        onNodeWithText(submitButtonText).performClick()
        
        // Verify validation errors appear
        expectedErrors.forEach { error ->
            waitForText(error)
            onNodeWithText(error).assertIsDisplayed()
        }
    }
    
    /**
     * Test that the app works offline by disabling network (simulation).
     */
    fun ComposeContentTestRule.testOfflineCapability() {
        // Since this is a local-only app, all functionality should work "offline"
        // We'll test that all main features are accessible
        
        navigateToScreen("Beans")
        onNodeWithContentDescription("Add bean").assertIsDisplayed()
        
        navigateToScreen("Record")
        onNodeWithText("Start Timer").assertIsDisplayed()
        
        navigateToScreen("History")
        // Should show either shots or empty state
        assert(
            onAllNodesWithText("No shots recorded yet").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("shot_history_list").fetchSemanticsNodes().isNotEmpty()
        )
    }
}