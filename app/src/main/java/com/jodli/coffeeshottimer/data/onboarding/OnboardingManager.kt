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
    val onboardingStartedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the onboarding process is completely finished.
     * 
     * @return true if all onboarding steps are complete
     */
    fun isComplete(): Boolean {
        return hasSeenIntroduction && hasCompletedEquipmentSetup && hasCreatedFirstBean && hasRecordedFirstShot
    }
    
    /**
     * Gets the next step that needs to be completed.
     * 
     * @return OnboardingStep enum representing the next step, or null if complete
     */
    fun getNextStep(): OnboardingStep? {
        return when {
            !hasSeenIntroduction -> OnboardingStep.INTRODUCTION
            !hasCompletedEquipmentSetup -> OnboardingStep.EQUIPMENT_SETUP
            !hasCreatedFirstBean -> OnboardingStep.GUIDED_BEAN_CREATION
            !hasRecordedFirstShot -> OnboardingStep.FIRST_SHOT
            else -> null
        }
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