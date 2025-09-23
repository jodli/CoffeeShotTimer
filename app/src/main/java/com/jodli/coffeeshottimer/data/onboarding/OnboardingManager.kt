package com.jodli.coffeeshottimer.data.onboarding

/**
 * Interface for managing onboarding state and progress.
 * Provides methods to track user progress through the onboarding flow
 * and determine if the user has completed the initial setup.
 */
interface OnboardingManager {

    /**
     * Checks if the current user is going through the app for the first time.
     *
     * @return true if this is a first-time user, false if they have completed onboarding
     */
    suspend fun isFirstTimeUser(): Boolean

    /**
     * Marks the onboarding process as complete.
     * This should be called when the user successfully completes their first shot.
     */
    suspend fun markOnboardingComplete()

    /**
     * Gets the current onboarding progress for the user.
     *
     * @return OnboardingProgress containing the current state of onboarding steps
     */
    suspend fun getOnboardingProgress(): OnboardingProgress

    /**
     * Updates the onboarding progress with new state.
     *
     * @param progress The updated progress state
     */
    suspend fun updateOnboardingProgress(progress: OnboardingProgress)

    /**
     * Resets all onboarding state (useful for testing or user-requested reset).
     */
    suspend fun resetOnboardingState()

    /**
     * Backs up the current onboarding state to a string format.
     *
     * @return JSON string representation of the onboarding state
     */
    suspend fun backupOnboardingState(): String

    /**
     * Restores onboarding state from a backup string.
     *
     * @param backupData JSON string containing the onboarding state
     * @return true if restore was successful, false otherwise
     */
    suspend fun restoreOnboardingState(backupData: String): Boolean

    // DEBUG METHODS FOR TESTING ONBOARDING FLOWS

    /**
     * Resets onboarding state to simulate a completely new user.
     * Clears all progress so the user will see the full onboarding flow.
     */
    suspend fun resetToNewUser()

    /**
     * Configures onboarding state to simulate an existing user who has created beans.
     * Sets appropriate flags to skip introduction and bean creation.
     */
    suspend fun resetToExistingUserWithBeans()

    /**
     * Configures onboarding state to simulate an existing user who hasn't created beans.
     * Sets appropriate flags to skip introduction but show equipment setup and bean creation.
     */
    suspend fun resetToExistingUserNoBeans()

    /**
     * Forces equipment setup to appear by setting the version to an older value.
     * Simulates the scenario where new equipment features have been added.
     */
    suspend fun forceEquipmentSetup()
}

/**
 * Data class representing the progress through onboarding steps.
 * Each boolean indicates whether that step has been completed.
 */
data class OnboardingProgress(
    val hasSeenIntroduction: Boolean = false,
    val hasCompletedEquipmentSetup: Boolean = false,
    val hasCreatedFirstBean: Boolean = false,
    val hasRecordedFirstShot: Boolean = false,
    val grinderConfigurationId: String? = null,
    val basketConfigurationId: String? = null, // NEW: Track basket configuration
    val equipmentSetupVersion: Int = 1, // NEW: Track equipment setup version for forcing updates
    val onboardingStartedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the onboarding process is completely finished.
     *
     * @return true if all onboarding steps are complete
     */
    fun isComplete(): Boolean {
        return hasSeenIntroduction &&
            hasCompletedEquipmentSetup &&
            equipmentSetupVersion >= CURRENT_EQUIPMENT_SETUP_VERSION &&
            hasCreatedFirstBean &&
            hasRecordedFirstShot
    }

    /**
     * Gets the next step that needs to be completed.
     *
     * @return OnboardingStep enum representing the next step, or null if complete
     */
    fun getNextStep(): OnboardingStep? {
        return when {
            !hasSeenIntroduction -> OnboardingStep.INTRODUCTION
            !hasCompletedEquipmentSetup || equipmentSetupVersion < CURRENT_EQUIPMENT_SETUP_VERSION -> OnboardingStep.EQUIPMENT_SETUP
            !hasCreatedFirstBean -> OnboardingStep.GUIDED_BEAN_CREATION
            !hasRecordedFirstShot -> OnboardingStep.FIRST_SHOT
            else -> null
        }
    }

    /**
     * Checks if equipment setup needs to be completed or updated.
     * This will be true for new users and existing users who haven't completed the latest equipment setup.
     */
    fun needsEquipmentSetup(): Boolean {
        return !hasCompletedEquipmentSetup || equipmentSetupVersion < CURRENT_EQUIPMENT_SETUP_VERSION
    }

    companion object {
        /**
         * Current version of equipment setup. Increment this to force existing users
         * to go through equipment setup again when new features are added.
         */
        const val CURRENT_EQUIPMENT_SETUP_VERSION = 2 // Incremented for basket configuration
    }
}

/**
 * Enum representing the different steps in the onboarding process.
 */
enum class OnboardingStep {
    INTRODUCTION,
    EQUIPMENT_SETUP,
    GUIDED_BEAN_CREATION,
    FIRST_SHOT,
    COMPLETED
}
