package com.jodli.coffeeshottimer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import javax.inject.Inject

/**
 * Integration test for the complete taste feedback flow.
 * Tests the end-to-end functionality from shot recording to taste feedback recording to display.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TasteFeedbackIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Clear database and add test data
        runBlocking {
            database.clearAllTables()
            
            // Add a test bean
            val testBean = Bean(
                id = "test-bean-id",
                name = "Test Ethiopian",
                roastDate = LocalDate.now().minusDays(7),
                notes = "Fruity and bright",
                isActive = true
            )
            database.beanDao().insertBean(testBean)
        }
    }

    @Test
    fun completeTasteFeedbackFlow_recordShotWithTaste_appearsInHistory() {
        // Wait for app to load and navigate to record shot
        composeTestRule.waitForIdle()
        
        // Navigate to record shot if not already there
        try {
            composeTestRule.onNodeWithText("Record Shot").performClick()
        } catch (e: AssertionError) {
            // Already on record shot screen
        }
        
        // Select the test bean
        composeTestRule.onNodeWithText("Selected Bean").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Ethiopian").performClick()
        composeTestRule.waitForIdle()
        
        // Set shot parameters
        // Note: In a real test environment, you might need to interact with sliders
        // For now, we'll assume default values are acceptable
        
        // Start and immediately stop timer to simulate shot
        composeTestRule.onNodeWithContentDescription("Start timer").performClick()
        composeTestRule.waitForIdle()
        
        // Wait a moment then stop
        Thread.sleep(1000) // Simulate 1 second extraction
        composeTestRule.onNodeWithContentDescription("Stop timer").performClick()
        composeTestRule.waitForIdle()
        
        // Record the shot
        composeTestRule.onNodeWithText("Save Shot").performClick()
        composeTestRule.waitForIdle()
        
        // The ShotRecordedDialog should appear with taste feedback options
        composeTestRule.onNodeWithText("Shot Recorded!").assertIsDisplayed()
        composeTestRule.onNodeWithText("How did it taste?").assertIsDisplayed()
        
        // Since it's a 1s extraction (very short), it should recommend SOUR
        composeTestRule.onNodeWithContentDescription("Sour (recommended)").assertIsDisplayed()
        
        // Select SOUR taste with WEAK secondary
        composeTestRule.onNodeWithText("Sour").performClick()
        
        // Dialog should close after selection, and we should be back on record shot screen
        composeTestRule.waitForIdle()
        
        // Navigate to shot history to verify the taste feedback is displayed
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Should see the shot in history with taste indicators
        composeTestRule.onNodeWithText("😖").assertIsDisplayed() // Sour emoji
        
        // Tap on the shot to view details
        composeTestRule.onAllNodesWithText("Test Ethiopian").onFirst().performClick()
        composeTestRule.waitForIdle()
        
        // In shot details, verify taste feedback is shown
        composeTestRule.onNodeWithText("Taste Feedback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sour").assertIsDisplayed()
    }

    @Test
    fun tasteFeedbackEdit_fromShotDetails_updatesCorrectly() {
        // First create a shot with taste feedback
        runBlocking {
            val shot = Shot(
                id = "test-shot-id",
                beanId = "test-bean-id",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "15",
                notes = "",
                tastePrimary = TastePrimary.SOUR,
                tasteSecondary = TasteSecondary.WEAK
            )
            database.shotDao().insertShot(shot)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to shot history
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Tap on the shot to view details
        composeTestRule.onAllNodesWithText("Test Ethiopian").onFirst().performClick()
        composeTestRule.waitForIdle()
        
        // Verify initial taste feedback
        composeTestRule.onNodeWithText("Sour").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()
        
        // Edit taste feedback
        composeTestRule.onNodeWithContentDescription("Edit notes").performClick()
        composeTestRule.waitForIdle()
        
        // Change to Perfect + Strong
        composeTestRule.onNodeWithText("Perfect").performClick()
        composeTestRule.onNodeWithText("Strong").performClick()
        
        // Save changes (assuming there's a save button in the editor)
        try {
            composeTestRule.onNodeWithText("Save").performClick()
        } catch (e: AssertionError) {
            // Might auto-save on selection
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the changes are reflected
        composeTestRule.onNodeWithText("Perfect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Strong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sour").assertDoesNotExist()
        composeTestRule.onNodeWithText("Weak").assertDoesNotExist()
    }

    @Test
    fun tastePreselection_basedOnExtractionTime_worksCorrectly() {
        composeTestRule.waitForIdle()
        
        // Navigate to record shot
        try {
            composeTestRule.onNodeWithText("Record Shot").performClick()
        } catch (e: AssertionError) {
            // Already on record shot screen
        }
        
        // Select bean
        composeTestRule.onNodeWithText("Selected Bean").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Ethiopian").performClick()
        composeTestRule.waitForIdle()
        
        // Test different extraction times and their recommendations
        val testCases = listOf(
            // Very short extraction should recommend SOUR
            Pair(500L, "Sour"), // 0.5 seconds
            // Long extraction should recommend BITTER  
            Pair(35000L, "Bitter") // 35 seconds
        )
        
        testCases.forEach { (extractionTime, expectedRecommendation) ->
            // Start timer
            composeTestRule.onNodeWithContentDescription("Start timer").performClick()
            composeTestRule.waitForIdle()
            
            // Wait for specified extraction time
            Thread.sleep(extractionTime)
            
            // Stop timer
            composeTestRule.onNodeWithContentDescription("Stop timer").performClick()
            composeTestRule.waitForIdle()
            
            // Record shot
            composeTestRule.onNodeWithText("Save Shot").performClick()
            composeTestRule.waitForIdle()
            
            // Verify correct recommendation appears
            composeTestRule.onNodeWithContentDescription("$expectedRecommendation (recommended)")
                .assertIsDisplayed()
            
            // Skip taste feedback to proceed to next test case
            composeTestRule.onNodeWithText("Skip").performClick()
            composeTestRule.waitForIdle()
            
            // Reset for next iteration
            composeTestRule.onNodeWithContentDescription("Reset timer").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun skipTasteFeedback_doesNotRecordTaste() {
        composeTestRule.waitForIdle()
        
        // Navigate to record shot and create a shot
        try {
            composeTestRule.onNodeWithText("Record Shot").performClick()
        } catch (e: AssertionError) {
            // Already on record shot screen
        }
        
        composeTestRule.onNodeWithText("Selected Bean").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Ethiopian").performClick()
        composeTestRule.waitForIdle()
        
        // Record a shot
        composeTestRule.onNodeWithContentDescription("Start timer").performClick()
        Thread.sleep(1000)
        composeTestRule.onNodeWithContentDescription("Stop timer").performClick()
        composeTestRule.onNodeWithText("Save Shot").performClick()
        composeTestRule.waitForIdle()
        
        // Skip taste feedback
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to history
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Tap on shot details
        composeTestRule.onAllNodesWithText("Test Ethiopian").onFirst().performClick()
        composeTestRule.waitForIdle()
        
        // Verify no taste feedback is recorded
        composeTestRule.onNodeWithText("No taste recorded").assertIsDisplayed()
        
        // But taste emojis should not be present
        composeTestRule.onNodeWithText("😖").assertDoesNotExist()
        composeTestRule.onNodeWithText("😊").assertDoesNotExist()
        composeTestRule.onNodeWithText("😣").assertDoesNotExist()
    }

    @Test
    fun tasteFeedbackDisplay_inHistoryList_showsCompactView() {
        // Create multiple shots with different taste feedback
        runBlocking {
            val shots = listOf(
                Shot(
                    id = "shot-1",
                    beanId = "test-bean-id",
                    coffeeWeightIn = 18.0,
                    coffeeWeightOut = 36.0,
                    extractionTimeSeconds = 22,
                    grinderSetting = "15",
                    tastePrimary = TastePrimary.SOUR
                ),
                Shot(
                    id = "shot-2",
                    beanId = "test-bean-id",
                    coffeeWeightIn = 18.0,
                    coffeeWeightOut = 36.0,
                    extractionTimeSeconds = 28,
                    grinderSetting = "14",
                    tastePrimary = TastePrimary.PERFECT,
                    tasteSecondary = TasteSecondary.STRONG
                ),
                Shot(
                    id = "shot-3",
                    beanId = "test-bean-id",
                    coffeeWeightIn = 18.0,
                    coffeeWeightOut = 36.0,
                    extractionTimeSeconds = 35,
                    grinderSetting = "16",
                    tastePrimary = TastePrimary.BITTER,
                    tasteSecondary = TasteSecondary.WEAK
                )
            )
            
            shots.forEach { shot ->
                database.shotDao().insertShot(shot)
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to history
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        
        // Verify compact taste displays are shown for each shot
        // Note: The exact implementation might vary based on how the history list displays items
        composeTestRule.onNodeWithText("😖").assertIsDisplayed() // Sour
        composeTestRule.onNodeWithText("😊").assertIsDisplayed() // Perfect (might be 😋 based on implementation)
        composeTestRule.onNodeWithText("😣").assertIsDisplayed() // Bitter
        composeTestRule.onNodeWithText("💪").assertIsDisplayed() // Strong
        composeTestRule.onNodeWithText("💧").assertIsDisplayed() // Weak
    }
}
