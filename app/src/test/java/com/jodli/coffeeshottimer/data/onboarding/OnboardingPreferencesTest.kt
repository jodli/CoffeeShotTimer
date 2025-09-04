package com.jodli.coffeeshottimer.data.onboarding

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OnboardingPreferences.
 * Tests all onboarding state management functionality including persistence, backup, and restore.
 */
class OnboardingPreferencesTest {

    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var onboardingPreferences: OnboardingPreferences

    @Before
    fun setup() {
        mockSharedPreferences = mockk()
        mockEditor = mockk(relaxed = true)

        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit

        onboardingPreferences = OnboardingPreferences(mockSharedPreferences)
    }

    @Test
    fun `isFirstTimeUser returns true when onboarding not complete`() = runTest {
        // Given
        every { mockSharedPreferences.getBoolean("onboarding_complete", false) } returns false

        // When
        val result = onboardingPreferences.isFirstTimeUser()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isFirstTimeUser returns false when onboarding complete`() = runTest {
        // Given
        every { mockSharedPreferences.getBoolean("onboarding_complete", false) } returns true

        // When
        val result = onboardingPreferences.isFirstTimeUser()

        // Then
        assertFalse(result)
    }

    @Test
    fun `markOnboardingComplete sets all flags to true`() = runTest {
        // Given
        every { mockSharedPreferences.getString("onboarding_progress", null) } returns null

        // When
        onboardingPreferences.markOnboardingComplete()

        // Then
        verify { mockEditor.putBoolean("onboarding_complete", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getOnboardingProgress returns default when no saved state`() = runTest {
        // Given
        every { mockSharedPreferences.getString("onboarding_progress", null) } returns null

        // When
        val progress = onboardingPreferences.getOnboardingProgress()

        // Then
        assertFalse(progress.hasSeenIntroduction)
        assertFalse(progress.hasCompletedEquipmentSetup)
        assertFalse(progress.hasCreatedFirstBean)
        assertFalse(progress.hasRecordedFirstShot)
        assertNull(progress.grinderConfigurationId)
    }

    @Test
    fun `getOnboardingProgress returns saved state when available`() = runTest {
        // Given
        val savedJson = """
            {
                "hasSeenIntroduction": true,
                "hasCompletedEquipmentSetup": false,
                "hasCreatedFirstBean": true,
                "hasRecordedFirstShot": false,
                "grinderConfigurationId": "test-config-id",
                "onboardingStartedAt": 1234567890,
                "lastUpdatedAt": 1234567890
            }
        """.trimIndent()
        every { mockSharedPreferences.getString("onboarding_progress", null) } returns savedJson

        // When
        val progress = onboardingPreferences.getOnboardingProgress()

        // Then
        assertTrue(progress.hasSeenIntroduction)
        assertFalse(progress.hasCompletedEquipmentSetup)
        assertTrue(progress.hasCreatedFirstBean)
        assertFalse(progress.hasRecordedFirstShot)
        assertEquals("test-config-id", progress.grinderConfigurationId)
    }

    @Test
    fun `getOnboardingProgress handles corrupted JSON gracefully`() = runTest {
        // Given
        every { mockSharedPreferences.getString("onboarding_progress", null) } returns "invalid json"

        // When
        val progress = onboardingPreferences.getOnboardingProgress()

        // Then
        assertFalse(progress.hasSeenIntroduction)
        assertFalse(progress.hasCompletedEquipmentSetup)
        assertFalse(progress.hasCreatedFirstBean)
        assertFalse(progress.hasRecordedFirstShot)
    }

    @Test
    fun `updateOnboardingProgress saves progress and updates completion status`() = runTest {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = false,
            hasRecordedFirstShot = false,
            grinderConfigurationId = "test-config"
        )

        // When
        onboardingPreferences.updateOnboardingProgress(progress)

        // Then
        val progressSlot = slot<String>()
        verify { mockEditor.putString("onboarding_progress", capture(progressSlot)) }
        verify { mockEditor.putBoolean("onboarding_complete", false) } // Not complete yet
        verify { mockEditor.apply() }

        // Verify the JSON contains expected data
        assertTrue(progressSlot.captured.contains("\"hasSeenIntroduction\":true"))
        assertTrue(progressSlot.captured.contains("\"hasCompletedEquipmentSetup\":true"))
        assertTrue(progressSlot.captured.contains("\"hasCreatedFirstBean\":false"))
        assertTrue(progressSlot.captured.contains("\"hasRecordedFirstShot\":false"))
    }

    @Test
    fun `updateOnboardingProgress marks complete when all steps done`() = runTest {
        // Given
        val completeProgress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        onboardingPreferences.updateOnboardingProgress(completeProgress)

        // Then
        verify { mockEditor.putBoolean("onboarding_complete", true) }
    }

    @Test
    fun `resetOnboardingState removes all onboarding keys`() = runTest {
        // When
        onboardingPreferences.resetOnboardingState()

        // Then
        verify { mockEditor.remove("onboarding_complete") }
        verify { mockEditor.remove("onboarding_progress") }
        verify { mockEditor.remove("grinder_config_id") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `backupOnboardingState creates valid JSON backup`() = runTest {
        // Given
        every { mockSharedPreferences.getString("onboarding_progress", null) } returns null
        every { mockSharedPreferences.getBoolean("onboarding_complete", false) } returns false

        // When
        val backup = onboardingPreferences.backupOnboardingState()

        // Then
        assertNotNull(backup)
        assertTrue(backup.contains("\"progress\""))
        assertTrue(backup.contains("\"isComplete\""))
        assertTrue(backup.contains("\"backupTimestamp\""))
    }

    @Test
    fun `restoreOnboardingState restores from valid backup`() = runTest {
        // Given
        val backupJson = """
            {
                "progress": {
                    "hasSeenIntroduction": true,
                    "hasCompletedEquipmentSetup": false,
                    "hasCreatedFirstBean": true,
                    "hasRecordedFirstShot": false,
                    "grinderConfigurationId": null,
                    "onboardingStartedAt": 1234567890,
                    "lastUpdatedAt": 1234567890
                },
                "isComplete": false,
                "backupTimestamp": 1234567890
            }
        """.trimIndent()

        // When
        val result = onboardingPreferences.restoreOnboardingState(backupJson)

        // Then
        assertTrue(result)
        verify { mockEditor.putString("onboarding_progress", any()) }
        verify { mockEditor.putBoolean("onboarding_complete", false) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `restoreOnboardingState returns false for invalid backup`() = runTest {
        // Given
        val invalidBackup = "invalid json"

        // When
        val result = onboardingPreferences.restoreOnboardingState(invalidBackup)

        // Then
        assertFalse(result)
    }

    @Test
    fun `getGrinderConfigurationId returns stored value`() = runTest {
        // Given
        every { mockSharedPreferences.getString("grinder_config_id", null) } returns "test-config-123"

        // When
        val configId = onboardingPreferences.getGrinderConfigurationId()

        // Then
        assertEquals("test-config-123", configId)
    }

    @Test
    fun `getGrinderConfigurationId returns null when not set`() = runTest {
        // Given
        every { mockSharedPreferences.getString("grinder_config_id", null) } returns null

        // When
        val configId = onboardingPreferences.getGrinderConfigurationId()

        // Then
        assertNull(configId)
    }

    @Test
    fun `setGrinderConfigurationId stores the value`() = runTest {
        // Given
        val configId = "new-config-456"

        // When
        onboardingPreferences.setGrinderConfigurationId(configId)

        // Then
        verify { mockEditor.putString("grinder_config_id", configId) }
        verify { mockEditor.apply() }
    }
}

/**
 * Unit tests for OnboardingProgress data class.
 */
class OnboardingProgressTest {

    @Test
    fun `isComplete returns false when not all steps done`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            hasCreatedFirstBean = false,
            hasRecordedFirstShot = false
        )

        // When & Then
        assertFalse(progress.isComplete())
    }

    @Test
    fun `isComplete returns true when all steps done`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When & Then
        assertTrue(progress.isComplete())
    }

    @Test
    fun `getNextStep returns INTRODUCTION when nothing done`() {
        // Given
        val progress = OnboardingProgress()

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertEquals(OnboardingStep.INTRODUCTION, nextStep)
    }

    @Test
    fun `getNextStep returns EQUIPMENT_SETUP when introduction done`() {
        // Given
        val progress = OnboardingProgress(hasSeenIntroduction = true)

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertEquals(OnboardingStep.EQUIPMENT_SETUP, nextStep)
    }

    @Test
    fun `getNextStep returns GUIDED_BEAN_CREATION when equipment setup done`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertEquals(OnboardingStep.GUIDED_BEAN_CREATION, nextStep)
    }

    @Test
    fun `getNextStep returns FIRST_SHOT when bean creation done`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertEquals(OnboardingStep.FIRST_SHOT, nextStep)
    }

    @Test
    fun `getNextStep returns null when all steps complete`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertNull(nextStep)
    }
    
    @Test
    fun `isComplete returns false when equipment setup version is outdated`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION - 1
        )

        // When & Then
        assertFalse(progress.isComplete())
    }
    
    @Test
    fun `getNextStep returns EQUIPMENT_SETUP when version is outdated`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION - 1
        )

        // When
        val nextStep = progress.getNextStep()

        // Then
        assertEquals(OnboardingStep.EQUIPMENT_SETUP, nextStep)
    }
    
    @Test
    fun `needsEquipmentSetup returns true when equipment setup not completed`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When & Then
        assertTrue(progress.needsEquipmentSetup())
    }
    
    @Test
    fun `needsEquipmentSetup returns true when equipment setup version is outdated`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION - 1
        )

        // When & Then
        assertTrue(progress.needsEquipmentSetup())
    }
    
    @Test
    fun `needsEquipmentSetup returns false when equipment setup is complete and current version`() {
        // Given
        val progress = OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When & Then
        assertFalse(progress.needsEquipmentSetup())
    }
}
