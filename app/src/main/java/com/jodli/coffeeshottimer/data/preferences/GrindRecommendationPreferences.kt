package com.jodli.coffeeshottimer.data.preferences

import android.content.SharedPreferences
import com.jodli.coffeeshottimer.di.RecommendationPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of persistent storage for grind recommendations using SharedPreferences.
 * Stores recommendations per bean ID and handles serialization/deserialization.
 * All operations are performed on the IO dispatcher to avoid blocking the main thread.
 */
@Singleton
class GrindRecommendationPreferences @Inject constructor(
    @param:RecommendationPrefs private val sharedPreferences: SharedPreferences
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Save a grind recommendation for a specific bean.
     *
     * @param beanId The ID of the bean this recommendation is for
     * @param recommendation The recommendation data to store
     */
    suspend fun saveRecommendation(
        beanId: String,
        recommendation: SerializableGrindRecommendation
    ) = withContext(Dispatchers.IO) {
        val key = createRecommendationKey(beanId)
        val recommendationJson = json.encodeToString(recommendation)

        sharedPreferences.edit()
            .putString(key, recommendationJson)
            .apply()
    }

    /**
     * Get the stored grind recommendation for a specific bean.
     *
     * @param beanId The ID of the bean to get recommendation for
     * @return The stored recommendation, or null if none exists
     */
    suspend fun getRecommendation(beanId: String): SerializableGrindRecommendation? =
        withContext(Dispatchers.IO) {
            val key = createRecommendationKey(beanId)
            val recommendationJson = sharedPreferences.getString(key, null)

            if (recommendationJson != null) {
                try {
                    json.decodeFromString<SerializableGrindRecommendation>(recommendationJson)
                } catch (e: Exception) {
                    // If deserialization fails, return null and clean up bad data
                    clearRecommendation(beanId)
                    null
                }
            } else {
                null
            }
        }

    /**
     * Clear the stored recommendation for a specific bean.
     *
     * @param beanId The ID of the bean to clear recommendation for
     */
    suspend fun clearRecommendation(beanId: String) = withContext(Dispatchers.IO) {
        val key = createRecommendationKey(beanId)
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }

    /**
     * Mark a recommendation as followed by updating the wasFollowed flag.
     *
     * @param beanId The ID of the bean to update recommendation for
     */
    suspend fun markRecommendationFollowed(beanId: String) = withContext(Dispatchers.IO) {
        val currentRecommendation = getRecommendation(beanId)
        if (currentRecommendation != null) {
            val updatedRecommendation = currentRecommendation.copy(wasFollowed = true)
            saveRecommendation(beanId, updatedRecommendation)
        }
    }

    /**
     * Get all bean IDs that have stored recommendations.
     * Useful for cleanup or analytics.
     *
     * @return List of bean IDs with stored recommendations
     */
    suspend fun getAllRecommendationBeanIds(): List<String> = withContext(Dispatchers.IO) {
        sharedPreferences.all.keys
            .filter { it.startsWith(RECOMMENDATION_KEY_PREFIX) }
            .map { it.removePrefix(RECOMMENDATION_KEY_PREFIX) }
    }

    /**
     * Clear all stored recommendations.
     * Useful for testing or user data reset.
     */
    suspend fun clearAllRecommendations() = withContext(Dispatchers.IO) {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys
            .filter { it.startsWith(RECOMMENDATION_KEY_PREFIX) }
            .forEach { key ->
                editor.remove(key)
            }
        editor.apply()
    }

    private fun createRecommendationKey(beanId: String): String {
        return "$RECOMMENDATION_KEY_PREFIX$beanId"
    }

    companion object {
        private const val RECOMMENDATION_KEY_PREFIX = "grind_recommendation_"
    }
}

/**
 * Serializable version of grind recommendation for JSON storage.
 * Contains all necessary data to recreate a PersistentGrindRecommendation.
 */
@Serializable
data class SerializableGrindRecommendation(
    val beanId: String,
    val suggestedGrindSetting: String,
    val adjustmentDirection: String, // Stored as string for stability
    val reason: String,
    val recommendedDose: Double,
    val targetExtractionTimeMin: Int,
    val targetExtractionTimeMax: Int,
    val timestamp: String, // ISO-8601 formatted string
    val wasFollowed: Boolean = false,
    val basedOnTaste: Boolean,
    val confidence: String // Stored as string for stability
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * Create from domain data with current timestamp.
         */
        fun create(
            beanId: String,
            suggestedGrindSetting: String,
            adjustmentDirection: String,
            reason: String,
            recommendedDose: Double,
            basedOnTaste: Boolean,
            confidence: String
        ): SerializableGrindRecommendation {
            return SerializableGrindRecommendation(
                beanId = beanId,
                suggestedGrindSetting = suggestedGrindSetting,
                adjustmentDirection = adjustmentDirection,
                reason = reason,
                recommendedDose = recommendedDose,
                targetExtractionTimeMin = 25,
                targetExtractionTimeMax = 30,
                timestamp = LocalDateTime.now().format(dateFormatter),
                wasFollowed = false,
                basedOnTaste = basedOnTaste,
                confidence = confidence
            )
        }

        /**
         * Parse timestamp from stored string.
         */
        fun parseTimestamp(timestampString: String): LocalDateTime {
            return LocalDateTime.parse(timestampString, dateFormatter)
        }
    }

    /**
     * Get the timestamp as LocalDateTime.
     */
    fun getTimestamp(): LocalDateTime {
        return parseTimestamp(timestamp)
    }

    /**
     * Get the target extraction time as IntRange.
     */
    fun getTargetExtractionTimeRange(): IntRange {
        return targetExtractionTimeMin..targetExtractionTimeMax
    }
}
