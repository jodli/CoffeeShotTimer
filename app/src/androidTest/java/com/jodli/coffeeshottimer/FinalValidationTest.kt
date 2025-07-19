package com.jodli.coffeeshottimer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.utils.TestUtils
import com.jodli.coffeeshottimer.utils.TestUtils.fillBeanForm
import com.jodli.coffeeshottimer.utils.TestUtils.fillShotForm
import com.jodli.coffeeshottimer.utils.TestUtils.measureScreenLoadTime
import com.jodli.coffeeshottimer.utils.TestUtils.navigateToScreen
import com.jodli.coffeeshottimer.utils.TestUtils.testConfigurationChange
import com.jodli.coffeeshottimer.utils.TestUtils.testFormValidation
import com.jodli.coffeeshottimer.utils.TestUtils.testLowMemoryHandling
import com.jodli.coffeeshottimer.utils.TestUtils.testOfflineCapability
import com.jodli.coffeeshottimer.utils.TestUtils.testScreenAccessibility
import com.jodli.coffeeshottimer.utils.TestUtils.verifyRequiredElements
import com.jodli.coffeeshottimer.utils.TestUtils.verifyShotInHistory
import com.jodli.coffeeshottimer.utils.TestUtils.waitForText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Final comprehensive validation test for the Espresso Shot Tracker app.
 * This test validates all key functionality, performance, accessibility, and polish.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FinalValidationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
        TestUtils.clearDatabase(database)
    }

    @Test
    fun finalValidation_completeUserJourney() {
        // Test the complete user journey from start to finish
        
        // 1. App Launch and Navigation
        validateAppLaunch()
        
        // 2. Bean Management
        validateBeanManagement()
        
        // 3. Shot Recording
        validateShotRecording()
        
        // 4. Shot History and Analysis
        validateShotHistory()
        
        // 5. Performance and Memory
        validatePerformance()
        
        // 6. Accessibility
        validateAccessibility()
        
        // 7. Offline Capability
        validateOfflineCapability()
        
        // 8. Error Handling and Edge Cases
        validateErrorHandling()
        
        // 9. UI Polish and Animations
        validateUIPolish()
    }

    private fun validateAppLaunch() {
        // Verify app launches successfully
        composeTestRule.onNodeWithText("Record").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beans").assertIsDisplayed()
        
        // Test navigation between screens
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.navigateToScreen("History")
        composeTestRule.navigateToScreen("Record")
        
        // Measure screen load times
        val recordLoadTime = composeTestRule.measureScreenLoadTime("Record")
        val historyLoadTime = composeTestRule.measureScreenLoadTime("History")
        val beansLoadTime = composeTestRule.measureScreenLoadTime("Beans")
        
        // All screens should load quickly
        assert(recordLoadTime < 1000) { "Record screen load time too slow: ${recordLoadTime}ms" }
        assert(historyLoadTime < 2000) { "History screen load time too slow: ${historyLoadTime}ms" }
        assert(beansLoadTime < 1000) { "Beans screen load time too slow: ${beansLoadTime}ms" }
    }

    private fun validateBeanManagement() {
        composeTestRule.navigateToScreen("Beans")
        
        // Verify empty state
        composeTestRule.onNodeWithText("No beans added yet").assertIsDisplayed()
        
        // Add a new bean
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.fillBeanForm(
            name = "Validation Test Bean",
            origin = "Colombia",
            roaster = "Test Roastery",
            notes = "Perfect for testing"
        )
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify bean was added
        composeTestRule.waitForText("Validation Test Bean")
        composeTestRule.onNodeWithText("Validation Test Bean").assertIsDisplayed()
        composeTestRule.onNodeWithText("Colombia").assertIsDisplayed()
        
        // Test bean editing
        composeTestRule.onNodeWithText("Validation Test Bean").performClick()
        composeTestRule.onNodeWithContentDescription("Edit bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextClearance()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Updated Test Bean")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Verify update
        composeTestRule.waitForText("Updated Test Bean")
        composeTestRule.onNodeWithText("Updated Test Bean").assertIsDisplayed()
        
        // Test form validation
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.testFormValidation(
            submitButtonText = "Save Bean",
            expectedErrors = listOf("Bean name is required")
        )
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    private fun validateShotRecording() {
        composeTestRule.navigateToScreen("Record")
        
        // Verify required elements are present
        composeTestRule.verifyRequiredElements(listOf(
            "Select Bean",
            "Coffee Weight In (g)",
            "Coffee Weight Out (g)",
            "Grinder Setting",
            "Start Timer",
            "Save Shot"
        ))
        
        // Test form validation
        composeTestRule.testFormValidation(
            submitButtonText = "Save Shot",
            expectedErrors = listOf(
                "Please select a bean",
                "Coffee weight in is required"
            )
        )
        
        // Record a complete shot
        composeTestRule.fillShotForm(
            beanName = "Updated Test Bean",
            coffeeWeightIn = "18.5",
            coffeeWeightOut = "37.0",
            grinderSetting = "14",
            notes = "Perfect extraction for validation"
        )
        
        // Test timer functionality
        composeTestRule.onNodeWithText("Start Timer").performClick()
        composeTestRule.onNodeWithContentDescription("Stop timer").assertIsDisplayed()
        Thread.sleep(2000) // Let timer run for 2 seconds
        composeTestRule.onNodeWithText("Stop Timer").performClick()
        
        // Verify brew ratio calculation
        composeTestRule.onNodeWithText("1:2.0").assertIsDisplayed()
        
        // Save shot
        composeTestRule.onNodeWithText("Save Shot").performClick()
        composeTestRule.waitForText("Shot recorded successfully!")
        composeTestRule.onNodeWithText("Shot recorded successfully!").assertIsDisplayed()
        
        // Verify form reset
        composeTestRule.onNodeWithText("Coffee Weight In (g)").assertTextEquals("")
    }

    private fun validateShotHistory() {
        composeTestRule.navigateToScreen("History")
        
        // Verify shot appears in history
        composeTestRule.verifyShotInHistory(
            beanName = "Updated Test Bean",
            weightIn = "18.5",
            weightOut = "37.0"
        )
        
        // Test shot details
        composeTestRule.onNodeWithText("Updated Test Bean").performClick()
        composeTestRule.onNodeWithText("Shot Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect extraction for validation").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Test filtering
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        composeTestRule.onNodeWithText("Filter by Bean").performClick()
        composeTestRule.onNodeWithText("Updated Test Bean").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        
        // Verify filtered results
        composeTestRule.waitForText("Filtered results")
        composeTestRule.onNodeWithText("Filtered results").assertIsDisplayed()
        
        // Test analysis view
        composeTestRule.onNodeWithContentDescription("Show analysis").performClick()
        composeTestRule.onNodeWithText("Overall Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Shots").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed() // Should show 1 shot
    }

    private fun validatePerformance() {
        // Test memory handling
        composeTestRule.testLowMemoryHandling()
        
        // Test configuration changes
        composeTestRule.testConfigurationChange()
        
        // Verify app is still functional after configuration change
        composeTestRule.onNodeWithText("Record").assertIsDisplayed()
        composeTestRule.navigateToScreen("History")
        composeTestRule.onNodeWithText("Updated Test Bean").assertIsDisplayed()
        
        // Test rapid navigation (stress test)
        repeat(5) {
            composeTestRule.navigateToScreen("Record")
            composeTestRule.navigateToScreen("History")
            composeTestRule.navigateToScreen("Beans")
        }
        
        // App should still be responsive
        composeTestRule.onNodeWithText("Beans").assertIsDisplayed()
    }

    private fun validateAccessibility() {
        // Test accessibility on all screens
        composeTestRule.navigateToScreen("Record")
        composeTestRule.testScreenAccessibility()
        
        composeTestRule.navigateToScreen("History")
        composeTestRule.testScreenAccessibility()
        
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.testScreenAccessibility()
        
        // Test specific accessibility features
        composeTestRule.navigateToScreen("Record")
        
        // Verify form fields have proper labels
        composeTestRule.onNodeWithText("Coffee Weight In (g)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").assertIsDisplayed()
        
        // Verify buttons have proper content descriptions
        composeTestRule.onNodeWithContentDescription("Start timer").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select bean").assertIsDisplayed()
    }

    private fun validateOfflineCapability() {
        // Test that all functionality works offline
        composeTestRule.testOfflineCapability()
        
        // Verify data persistence
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.onNodeWithText("Updated Test Bean").assertIsDisplayed()
        
        composeTestRule.navigateToScreen("History")
        composeTestRule.onNodeWithText("Updated Test Bean").assertIsDisplayed()
        
        // Test that new data can be created offline
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.fillBeanForm(name = "Offline Bean")
        composeTestRule.onNodeWithText("Save Bean").performClick()
        composeTestRule.waitForText("Offline Bean")
        composeTestRule.onNodeWithText("Offline Bean").assertIsDisplayed()
    }

    private fun validateErrorHandling() {
        // Test various error conditions
        
        // Invalid input validation
        composeTestRule.navigateToScreen("Record")
        composeTestRule.onNodeWithText("Coffee Weight In (g)").performTextInput("0")
        composeTestRule.onNodeWithText("Coffee Weight Out (g)").performTextInput("-5")
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Should show validation errors
        composeTestRule.onNodeWithText("Weight must be greater than 0").assertIsDisplayed()
        
        // Test bean name uniqueness
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.onNodeWithContentDescription("Add bean").performClick()
        composeTestRule.onNodeWithText("Bean Name").performTextInput("Updated Test Bean") // Duplicate name
        composeTestRule.onNodeWithText("Save Bean").performClick()
        
        // Should show uniqueness error
        composeTestRule.waitForText("A bean with this name already exists")
        composeTestRule.onNodeWithText("A bean with this name already exists").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    private fun validateUIPolish() {
        // Test UI animations and polish
        composeTestRule.navigateToScreen("Record")
        
        // Test timer animation
        composeTestRule.onNodeWithText("Start Timer").performClick()
        Thread.sleep(1000)
        composeTestRule.onNodeWithText("Stop Timer").performClick()
        
        // Test success message animation
        composeTestRule.fillShotForm(
            beanName = "Updated Test Bean",
            coffeeWeightIn = "19.0",
            coffeeWeightOut = "38.0"
        )
        composeTestRule.onNodeWithText("Save Shot").performClick()
        
        // Success message should appear with animation
        composeTestRule.waitForText("Shot recorded successfully!")
        composeTestRule.onNodeWithText("Shot recorded successfully!").assertIsDisplayed()
        
        // Test navigation animations
        composeTestRule.navigateToScreen("History")
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.navigateToScreen("Record")
        
        // Test loading states
        composeTestRule.navigateToScreen("History")
        // Loading indicator should appear briefly then disappear
        
        // Test empty states
        TestUtils.clearDatabase(database)
        composeTestRule.navigateToScreen("History")
        composeTestRule.onNodeWithText("No shots recorded yet").assertIsDisplayed()
        
        composeTestRule.navigateToScreen("Beans")
        composeTestRule.onNodeWithText("No beans added yet").assertIsDisplayed()
    }

    @Test
    fun finalValidation_stressTest() {
        // Create multiple beans and shots to test performance
        val testBeans = (1..10).map { TestUtils.createTestBean("Stress Test Bean $it") }
        val testShots = testBeans.flatMap { bean ->
            (1..20).map { TestUtils.createTestShot(bean.id) }
        }
        
        TestUtils.insertTestData(database, testBeans, testShots)
        
        // Test that the app handles large datasets well
        val historyLoadTime = composeTestRule.measureScreenLoadTime("History", expectedMaxLoadTime = 5000)
        assert(historyLoadTime < 5000) { "History with large dataset took too long: ${historyLoadTime}ms" }
        
        // Test scrolling performance
        composeTestRule.onNodeWithTag("shot_history_list").performScrollToIndex(50)
        composeTestRule.onNodeWithTag("shot_history_list").performScrollToIndex(100)
        
        // Test filtering with large dataset
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        composeTestRule.onNodeWithText("Filter by Bean").performClick()
        composeTestRule.onNodeWithText("Stress Test Bean 1").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        
        // Should still be responsive
        composeTestRule.waitForText("Filtered results")
        composeTestRule.onNodeWithText("Filtered results").assertIsDisplayed()
        
        // Test analysis with large dataset
        composeTestRule.onNodeWithContentDescription("Show analysis").performClick()
        composeTestRule.waitForText("Overall Statistics")
        composeTestRule.onNodeWithText("Overall Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("200").assertIsDisplayed() // Should show 200 total shots
    }
}