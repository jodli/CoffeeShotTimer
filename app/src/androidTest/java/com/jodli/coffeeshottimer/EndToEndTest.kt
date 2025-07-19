package com.jodli.coffeeshottimer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for the complete user workflows.
 * Tests the entire app flow from bean management to shot recording to history viewing.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EndToEndTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completeUserWorkflow_addBeanRecordShotViewHistory() {
        // Step 1: Navigate to Bean Management and add a new bean
        composeTestRule.onNodeWithText("Beans").performClick()
        
        // Add new bean
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        
        // Fill bean form
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Test Espresso Blend")
        composeTestRule.onNodeWithText("Origin").performTextInput("Colombia")
        composeTestRule.onNodeWithText("Roaster").performTextInput("Local Roastery")
        
        // Set roast date (today)
        composeTestRule.onNodeWithText("Roast Date").performClick()
        composeTestRule.onNodeWithText("OK").performClick()
        
        // Save bean
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify bean was added
        composeTestRule.onNodeWithText("Test Espresso Blend").assertIsDisplayed()
        
        // Step 2: Navigate to Shot Recording and record a shot
        composeTestRule.onNodeWithText("Record").performClick()
        
        // Select the bean we just created
        composeTestRule.onNodeWithText("Select Bean").performClick()
        composeTestRule.onNodeWithText("Test Espresso Blend").performClick()
        
        // Fill shot recording form
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("18.0")
        composeTestRule.onNodeWithText("Grinder Setting").performTextInput("15")
        
        // Start timer
        composeTestRule.onNodeWithText("Start Timer").performClick()
        
        // Wait a moment then stop timer
        Thread.sleep(2000)
        composeTestRule.onNodeWithText("Stop Timer").performClick()
        
        // Fill output weight
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("36.0")
        
        // Add notes
        composeTestRule.onNodeWithText("Notes (optional)").performTextInput("Great shot, perfect extraction")
        
        // Save shot
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify success message
        composeTestRule.onNodeWithText("Shot recorded successfully!").assertIsDisplayed()
        
        // Step 3: Navigate to Shot History and verify the shot appears
        composeTestRule.onNodeWithText("History").performClick()
        
        // Verify shot appears in history
        composeTestRule.onNodeWithText("Test Espresso Blend").assertIsDisplayed()
        composeTestRule.onNodeWithText("18.0g → 36.0g").assertIsDisplayed()
        
        // Click on shot to view details
        composeTestRule.onNodeWithText("Test Espresso Blend").performClick()
        
        // Verify shot details
        composeTestRule.onNodeWithText("Shot Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great shot, perfect extraction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grinder: 15").assertIsDisplayed()
        
        // Go back to history
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Step 4: Test filtering functionality
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        
        // Filter by bean
        composeTestRule.onNodeWithText("Filter by Bean").performClick()
        composeTestRule.onNodeWithText("Test Espresso Blend").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        
        // Verify filtered results
        composeTestRule.onNodeWithText("Filtered results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Espresso Blend").assertIsDisplayed()
    }

    @Test
    fun beanManagement_fullCrudOperations() {
        // Navigate to Bean Management
        composeTestRule.onNodeWithText("Beans").performClick()
        
        // Add bean
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("CRUD Test Bean")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify bean was added
        composeTestRule.onNodeWithText("CRUD Test Bean").assertIsDisplayed()
        
        // Edit bean
        composeTestRule.onNodeWithText("CRUD Test Bean").performClick()
        composeTestRule.onNodeWithContentDescription("Edit bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextClearance()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Updated CRUD Bean")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify bean was updated
        composeTestRule.onNodeWithText("Updated CRUD Bean").assertIsDisplayed()
        
        // Delete bean
        composeTestRule.onNodeWithText("Updated CRUD Bean").performClick()
        composeTestRule.onNodeWithContentDescription("Delete bean").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        
        // Verify bean was deleted
        composeTestRule.onNodeWithText("Updated CRUD Bean").assertDoesNotExist()
    }

    @Test
    fun shotRecording_validationAndErrorHandling() {
        // Navigate to Shot Recording
        composeTestRule.onNodeWithText("Record").performClick()
        
        // Try to save without required fields
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify validation errors
        composeTestRule.onNodeWithText("Please select a bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee weight in is required").assertIsDisplayed()
        
        // Fill invalid data
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("0")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("-5")
        
        // Try to save
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Verify validation errors for invalid ranges
        composeTestRule.onNodeWithText("Weight must be greater than 0").assertIsDisplayed()
    }

    @Test
    fun shotHistory_filteringAndSorting() {
        // First, ensure we have some test data by adding a bean and shot
        setupTestData()
        
        // Navigate to Shot History
        composeTestRule.onNodeWithText("History").performClick()
        
        // Test date range filtering
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        composeTestRule.onNodeWithText("Date Range").performClick()
        
        // Select last 7 days
        composeTestRule.onNodeWithText("Last 7 days").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        
        // Verify filter is applied
        composeTestRule.onNodeWithText("Filtered results").assertIsDisplayed()
        
        // Clear filters
        composeTestRule.onNodeWithText("Clear All").performClick()
        
        // Test analysis view
        composeTestRule.onNodeWithContentDescription("Show analysis").performClick()
        composeTestRule.onNodeWithText("Overall Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shot Trends").assertIsDisplayed()
    }

    @Test
    fun accessibility_screenReaderSupport() {
        // Test that all interactive elements have proper content descriptions
        composeTestRule.onNodeWithText("Record").assertHasClickAction()
        composeTestRule.onNodeWithText("History").assertHasClickAction()
        composeTestRule.onNodeWithText("Beans").assertHasClickAction()
        
        // Navigate to Shot Recording and test accessibility
        composeTestRule.onNodeWithText("Record").performClick()
        
        // Verify form fields have proper labels
        composeTestRule.onNodeWithText("Coffee Weight In (g)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grinder Setting").assertIsDisplayed()
        
        // Test timer accessibility
        composeTestRule.onNodeWithContentDescription("Start timer").assertIsDisplayed()
        
        // Test bean selection accessibility
        composeTestRule.onNodeWithContentDescription("Select bean").assertIsDisplayed()
    }

    @Test
    fun performanceWithLargeDataset() {
        // This test would ideally create a large dataset and test performance
        // For now, we'll test the pagination functionality
        
        composeTestRule.onNodeWithText("History").performClick()
        
        // Scroll to bottom to trigger pagination
        composeTestRule.onNodeWithTag("shot_history_list").performScrollToIndex(19)
        
        // Verify loading indicator appears
        composeTestRule.onNodeWithText("Loading more shots...").assertIsDisplayed()
    }

    private fun setupTestData() {
        // Helper function to set up test data
        // This would typically involve creating test beans and shots
        // For now, we'll assume the database is empty and create minimal test data
        
        // Navigate to Bean Management and add a test bean
        composeTestRule.onNodeWithText("Beans").performClick()
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Test Bean for History")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Navigate to Shot Recording and record a test shot
        composeTestRule.onNodeWithText("Record").performClick()
        composeTestRule.onNodeWithText("Select Bean").performClick()
        composeTestRule.onNodeWithText("Test Bean for History").performClick()
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("18.0")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("36.0")
        composeTestRule.onNodeWithText("Grinder Setting").performTextInput("15")
        composeTestRule.onNodeWithText("Save Shot").performClick()
    }
}