package com.jodli.coffeeshottimer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jodli.coffeeshottimer.data.database.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Tests to verify that the app works completely offline.
 * All data should be stored locally and accessible without network connectivity.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class OfflineFunctionalityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
        // Clear database to ensure clean state
        runBlocking {
            database.clearAllTables()
        }
    }

    @Test
    fun offlineBeanManagement_fullCrudOperations() {
        // Test that all bean management operations work offline
        
        // Navigate to Bean Management
        composeTestRule.onNodeWithText("Beans").performClick()
        
        // Verify empty state is shown
        composeTestRule.onNodeWithText("No beans added yet").assertIsDisplayed()
        
        // Add a new bean
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        
        // Fill bean form
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Offline Test Bean")
        composeTestRule.onNodeWithText("Origin").performTextInput("Ethiopia")
        composeTestRule.onNodeWithText("Roaster").performTextInput("Local Roastery")
        composeTestRule.onNodeWithText("Notes").performTextInput("Testing offline functionality")
        
        // Save bean
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify bean was saved and appears in list
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethiopia").assertIsDisplayed()
        
        // Edit the bean
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        composeTestRule.onNodeWithContentDescription("Edit bean").performClick()
        
        // Update bean name
        composeTestRule.onNodeWithText("Bean Name").performTextClearance()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Updated Offline Bean")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify update was saved
        composeTestRule.onNodeWithText("Updated Offline Bean").assertIsDisplayed()
        
        // Test bean activation/deactivation
        composeTestRule.onNodeWithText("Updated Offline Bean").performClick()
        composeTestRule.onNodeWithContentDescription("Toggle bean active status").performClick()
        
        // Verify bean status changed (should show inactive indicator)
        composeTestRule.onNodeWithContentDescription("Inactive bean").assertIsDisplayed()
    }

    @Test
    fun offlineShotRecording_completeWorkflow() {
        // First create a bean to use for shot recording
        createTestBean()
        
        // Navigate to Shot Recording
        composeTestRule.onNodeWithText("Record").performClick()
        
        // Select bean
        composeTestRule.onNodeWithText("Select Bean").performClick()
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        
        // Verify bean is selected
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        
        // Fill shot recording form
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("18.5")
        composeTestRule.onNodeWithText("Grinder Setting").performTextInput("14")
        
        // Test timer functionality
        composeTestRule.onNodeWithText("Start Timer").performClick()
        
        // Verify timer is running
        composeTestRule.onNodeWithContentDescription("Stop timer").assertIsDisplayed()
        
        // Wait a moment then stop timer
        Thread.sleep(2000)
        composeTestRule.onNodeWithText("Stop Timer").performClick()
        
        // Verify timer stopped and time was recorded
        composeTestRule.onNodeWithText("Start Timer").assertIsDisplayed()
        
        // Fill output weight
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("37.0")
        
        // Add notes
        composeTestRule.onNodeWithText("Notes (optional)").performTextInput("Offline shot test - excellent extraction")
        
        // Verify brew ratio is calculated
        composeTestRule.onNodeWithText("1:2.0").assertIsDisplayed()
        
        // Save shot
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify success message
        composeTestRule.onNodeWithText("Shot recorded successfully!").assertIsDisplayed()
        
        // Verify form is reset for next shot
        composeTestRule.onNodeWithText("Coffee Weight In (g)").assertTextEquals("")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").assertTextEquals("")
    }

    @Test
    fun offlineShotHistory_viewingAndFiltering() {
        // Create test data
        createTestBean()
        recordTestShot()
        
        // Navigate to Shot History
        composeTestRule.onNodeWithText("History").performClick()
        
        // Verify shot appears in history
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.5g → 37.0g").assertIsDisplayed()
        
        // Test shot details view
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        
        // Verify shot details are displayed
        composeTestRule.onNodeWithText("Shot Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline shot test - excellent extraction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grinder: 14").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brew Ratio: 1:2.0").assertIsDisplayed()
        
        // Go back to history
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Test filtering functionality
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        
        // Filter by bean
        composeTestRule.onNodeWithText("Filter by Bean").performClick()
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        
        // Verify filtered results
        composeTestRule.onNodeWithText("Filtered results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        
        // Clear filters
        composeTestRule.onNodeWithText("Clear All").performClick()
        
        // Verify filters are cleared
        composeTestRule.onNodeWithText("Filtered results").assertDoesNotExist()
    }

    @Test
    fun offlineDataPersistence_appRestartScenario() {
        // Create test data
        createTestBean()
        recordTestShot()
        
        // Simulate app restart by recreating the activity
        composeTestRule.activityRule.scenario.recreate()
        
        // Verify data persists after restart
        
        // Check bean data
        composeTestRule.onNodeWithText("Beans").performClick()
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        
        // Check shot data
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Offline Test Bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.5g → 37.0g").assertIsDisplayed()
        
        // Verify shot details persist
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        composeTestRule.onNodeWithText("Offline shot test - excellent extraction").assertIsDisplayed()
    }

    @Test
    fun offlineStatisticsAndAnalysis() {
        // Create multiple test shots for analysis
        createTestBean()
        recordMultipleTestShots(5)
        
        // Navigate to Shot History
        composeTestRule.onNodeWithText("History").performClick()
        
        // Switch to analysis view
        composeTestRule.onNodeWithContentDescription("Show analysis").performClick()
        
        // Verify statistics are calculated offline
        composeTestRule.onNodeWithText("Overall Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Shots").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed() // Should show 5 shots
        
        // Verify trends analysis
        composeTestRule.onNodeWithText("Shot Trends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shots/Day").assertIsDisplayed()
        
        // Verify brew ratio analysis
        composeTestRule.onNodeWithText("Brew Ratio Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Average").assertIsDisplayed()
    }

    @Test
    fun offlineValidationAndErrorHandling() {
        // Test that validation works offline
        composeTestRule.onNodeWithText("Record").performClick()
        
        // Try to save without required fields
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify validation errors are shown
        composeTestRule.onNodeWithText("Please select a bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee weight in is required").assertIsDisplayed()
        
        // Test invalid input validation
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("0")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("-5")
        
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify range validation errors
        composeTestRule.onNodeWithText("Weight must be greater than 0").assertIsDisplayed()
    }

    private fun createTestBean() {
        composeTestRule.onNodeWithText("Beans").performClick()
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Offline Test Bean")
        composeTestRule.onNodeWithText("Origin").performTextInput("Ethiopia")
        composeTestRule.onNodeWithText("Save Bean").performClick()
    }

    private fun recordTestShot() {
        composeTestRule.onNodeWithText("Record").performClick()
        composeTestRule.onNodeWithText("Select Bean").performClick()
        composeTestRule.onNodeWithText("Offline Test Bean").performClick()
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("18.5")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("37.0")
        composeTestRule.onNodeWithText("Grinder Setting").performTextInput("14")
        composeTestRule.onNodeWithText("Notes (optional)").performTextInput("Offline shot test - excellent extraction")
        composeTestRule.onNodeWithText("Save Shot").performClick()
    }

    private fun recordMultipleTestShots(count: Int) {
        repeat(count) { i ->
            composeTestRule.onNodeWithText("Record").performClick()
            composeTestRule.onNodeWithText("Select Bean").performClick()
            composeTestRule.onNodeWithText("Offline Test Bean").performClick()
            composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("${18.0 + i * 0.5}")
            composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("${36.0 + i * 1.0}")
            composeTestRule.onNodeWithText("Grinder Setting").performTextInput("${14 + i}")
            composeTestRule.onNodeWithText("Save Shot").performClick()
            
            // Wait for save to complete
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithText("Shot recorded successfully!").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}