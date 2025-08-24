package com.jodli.coffeeshottimer.data.onboarding

import android.content.SharedPreferences
import com.jodli.coffeeshottimer.di.OnboardingPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OnboardingManager using SharedPreferences for persistent storage.
 * All operations are performed on the IO dispatcher to avoid blocking the main thread.
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @param:OnboardingPrefs private val sharedPreferences: SharedPreferences
) : OnboardingManager {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun isFirstTimeUser(): Boolean = withContext(Dispatchers.IO) {
        !sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }
    
    override suspend fun markOnboardingComplete() = withContext(Dispatchers.IO) {
        val currentProgress = getOnboardingProgress()
        val completedProgress = currentProgress.copy(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            lastUpdatedAt = System.currentTimeMillis()
        )
        
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .putString(KEY_ONBOARDING_PROGRESS, json.encodeToString(completedProgress.toSerializable()))
            .apply()
    }
    
    override suspend fun getOnboardingProgress(): OnboardingProgress = withContext(Dispatchers.IO) {
        val progressJson = sharedPreferences.getString(KEY_ONBOARDING_PROGRESS, null)
        
        if (progressJson != null) {
            try {
                val serializable = json.decodeFromString<SerializableOnboardingProgress>(progressJson)
                return@withContext serializable.toOnboardingProgress()
            } catch (e: Exception) {
                // If deserialization fails, return default progress
                return@withContext OnboardingProgress()
            }
        }
        
        // Return default progress if no saved state exists
        OnboardingProgress()
    }
    
    override suspend fun updateOnboardingProgress(progress: OnboardingProgress) = withContext(Dispatchers.IO) {
        val updatedProgress = progress.copy(lastUpdatedAt = System.currentTimeMillis())
        val progressJson = json.encodeToString(updatedProgress.toSerializable())
        
        sharedPreferences.edit()
            .putString(KEY_ONBOARDING_PROGRESS, progressJson)
            .putBoolean(KEY_ONBOARDING_COMPLETE, updatedProgress.isComplete())
            .apply()
    }
    
    override suspend fun resetOnboardingState() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove(KEY_ONBOARDING_COMPLETE)
            .remove(KEY_ONBOARDING_PROGRESS)
            .remove(KEY_GRINDER_CONFIG_ID)
            .apply()
    }
    
    override suspend fun backupOnboardingState(): String = withContext(Dispatchers.IO) {
        val progress = getOnboardingProgress()
        val isComplete = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        
        val backupData = OnboardingBackup(
            progress = progress.toSerializable(),
            isComplete = isComplete,
            backupTimestamp = System.currentTimeMillis()
        )
        
        json.encodeToString(backupData)
    }
    
    override suspend fun restoreOnboardingState(backupData: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<OnboardingBackup>(backupData)
            val progress = backup.progress.toOnboardingProgress()
            
            sharedPreferences.edit()
                .putString(KEY_ONBOARDING_PROGRESS, json.encodeToString(backup.progress))
                .putBoolean(KEY_ONBOARDING_COMPLETE, backup.isComplete)
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the stored grinder configuration ID from onboarding.
     * 
     * @return The grinder configuration ID, or null if not set
     */
    suspend fun getGrinderConfigurationId(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_GRINDER_CONFIG_ID, null)
    }
    
    /**
     * Stores the grinder configuration ID from equipment setup.
     * 
     * @param configId The grinder configuration ID to store
     */
    suspend fun setGrinderConfigurationId(configId: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(KEY_GRINDER_CONFIG_ID, configId)
            .apply()
    }
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_ONBOARDING_PROGRESS = "onboarding_progress"
        private const val KEY_GRINDER_CONFIG_ID = "grinder_config_id"
    }
}

/**
 * Serializable version of OnboardingProgress for JSON storage.
 */
@Serializable
private data class SerializableOnboardingProgress(
    val hasSeenIntroduction: Boolean = false,
    val hasCompletedEquipmentSetup: Boolean = false,
    val hasCreatedFirstBean: Boolean = false,
    val hasRecordedFirstShot: Boolean = false,
    val grinderConfigurationId: String? = null,
    val onboardingStartedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    fun toOnboardingProgress(): OnboardingProgress {
        return OnboardingProgress(
            hasSeenIntroduction = hasSeenIntroduction,
            hasCompletedEquipmentSetup = hasCompletedEquipmentSetup,
            hasCreatedFirstBean = hasCreatedFirstBean,
            hasRecordedFirstShot = hasRecordedFirstShot,
            grinderConfigurationId = grinderConfigurationId,
            onboardingStartedAt = onboardingStartedAt,
            lastUpdatedAt = lastUpdatedAt
        )
    }
}

/**
 * Extension function to convert OnboardingProgress to serializable format.
 */
private fun OnboardingProgress.toSerializable(): SerializableOnboardingProgress {
    return SerializableOnboardingProgress(
        hasSeenIntroduction = hasSeenIntroduction,
        hasCompletedEquipmentSetup = hasCompletedEquipmentSetup,
        hasCreatedFirstBean = hasCreatedFirstBean,
        hasRecordedFirstShot = hasRecordedFirstShot,
        grinderConfigurationId = grinderConfigurationId,
        onboardingStartedAt = onboardingStartedAt,
        lastUpdatedAt = lastUpdatedAt
    )
}

/**
 * Data class for backing up onboarding state.
 */
@Serializable
private data class OnboardingBackup(
    val progress: SerializableOnboardingProgress,
    val isComplete: Boolean,
    val backupTimestamp: Long
)