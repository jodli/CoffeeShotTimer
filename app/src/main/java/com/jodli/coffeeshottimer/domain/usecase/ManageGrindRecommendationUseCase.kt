package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.preferences.GrindRecommendationPreferences
import com.jodli.coffeeshottimer.data.preferences.SerializableGrindRecommendation
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.ConfidenceLevel
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing persistent grind recommendations.
 *
 * This use case handles the business logic for:
 * - Converting temporary recommendations to persistent storage
 * - Loading bean-specific recommendations
 * - Tracking recommendation follow-through
 * - Managing recommendation lifecycle
 *
 * Unlike temporary GrindAdjustmentRecommendation, persistent recommendations
 * survive app restarts and provide next-shot guidance.
 */
@Singleton
class ManageGrindRecommendationUseCase @Inject constructor(
    private val grindRecommendationPreferences: GrindRecommendationPreferences,
    private val beanRepository: BeanRepository
) {

    /**
     * Save a grind recommendation for a specific bean.
     * This method always saves regardless of whether taste feedback was provided,
     * ensuring beginners always get guidance.
     *
     * @param beanId The ID of the bean this recommendation is for
     * @param recommendation The calculated grind adjustment recommendation
     * @param lastShot The shot that generated this recommendation
     * @return Result containing the saved persistent recommendation or error
     */
    suspend fun saveRecommendation(
        beanId: String,
        recommendation: GrindAdjustmentRecommendation,
        lastShot: Shot
    ): Result<PersistentGrindRecommendation> {
        return try {
            // Get bean information for dose recommendation
            val beanResult = beanRepository.getBeanById(beanId)
            if (beanResult.isFailure) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.BEAN_NOT_FOUND,
                        "Bean not found: $beanId"
                    )
                )
            }

            val bean = beanResult.getOrNull()
                ?: return Result.failure(
                    DomainException(
                        DomainErrorCode.BEAN_NOT_FOUND,
                        "Bean data is null for ID: $beanId"
                    )
                )

            // Generate context-aware reason text
            val basedOnTaste = lastShot.tastePrimary != null
            val reason = PersistentGrindRecommendation.createReasonString(
                extractionTimeSeconds = lastShot.extractionTimeSeconds,
                tasteFeedback = lastShot.tastePrimary
            )

            // Create persistent recommendation from temporary one
            // Use default espresso dose since Bean model doesn't have dose field yet
            val defaultDose = 18.0 // Standard espresso dose
            val persistentRecommendation = PersistentGrindRecommendation.fromGrindAdjustmentRecommendation(
                beanId = beanId,
                recommendation = recommendation,
                recommendedDose = defaultDose,
                reason = reason,
                basedOnTaste = basedOnTaste
            )

            // Convert to serializable format for storage
            val serializableRecommendation = SerializableGrindRecommendation.create(
                beanId = persistentRecommendation.beanId,
                suggestedGrindSetting = persistentRecommendation.suggestedGrindSetting,
                adjustmentDirection = persistentRecommendation.adjustmentDirection.name,
                reason = persistentRecommendation.reason,
                recommendedDose = persistentRecommendation.recommendedDose,
                basedOnTaste = persistentRecommendation.basedOnTaste,
                confidence = persistentRecommendation.confidence.name
            )

            // Save to persistent storage
            grindRecommendationPreferences.saveRecommendation(beanId, serializableRecommendation)

            Result.success(persistentRecommendation)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to save recommendation for bean $beanId",
                    exception
                )
            )
        }
    }

    /**
     * Get the stored recommendation for a specific bean.
     *
     * @param beanId The ID of the bean to get recommendation for
     * @return Result containing the persistent recommendation or null if none exists
     */
    suspend fun getRecommendation(beanId: String): Result<PersistentGrindRecommendation?> {
        return try {
            val serializableRecommendation = grindRecommendationPreferences.getRecommendation(beanId)

            if (serializableRecommendation == null) {
                return Result.success(null)
            }

            // Convert from serializable format to domain model
            val persistentRecommendation = convertToPersistentRecommendation(serializableRecommendation)
            Result.success(persistentRecommendation)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to get recommendation for bean $beanId",
                    exception
                )
            )
        }
    }

    /**
     * Mark a recommendation as followed by the user.
     * This is useful for tracking recommendation success rates.
     *
     * @param beanId The ID of the bean to update recommendation for
     * @return Result indicating success or failure
     */
    suspend fun markRecommendationFollowed(beanId: String): Result<Unit> {
        return try {
            grindRecommendationPreferences.markRecommendationFollowed(beanId)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to mark recommendation as followed for bean $beanId",
                    exception
                )
            )
        }
    }

    /**
     * Clear the stored recommendation for a specific bean.
     * Used when user dismisses recommendation or when it becomes obsolete.
     *
     * @param beanId The ID of the bean to clear recommendation for
     * @return Result indicating success or failure
     */
    suspend fun clearRecommendation(beanId: String): Result<Unit> {
        return try {
            grindRecommendationPreferences.clearRecommendation(beanId)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to clear recommendation for bean $beanId",
                    exception
                )
            )
        }
    }

    /**
     * Update an existing recommendation with new taste feedback.
     * This is called when a user adds taste feedback after initially recording
     * a shot without it.
     *
     * @param beanId The ID of the bean to update recommendation for
     * @param updatedRecommendation New recommendation with taste feedback
     * @param updatedShot Shot with added taste feedback
     * @return Result containing the updated persistent recommendation or error
     */
    suspend fun updateRecommendationWithTaste(
        beanId: String,
        updatedRecommendation: GrindAdjustmentRecommendation,
        updatedShot: Shot
    ): Result<PersistentGrindRecommendation> {
        return try {
            // Get existing recommendation to preserve timestamp and followed status
            val existingResult = getRecommendation(beanId)
            if (existingResult.isFailure) {
                // If no existing recommendation, create new one
                return saveRecommendation(beanId, updatedRecommendation, updatedShot)
            }

            val existing = existingResult.getOrNull()
            if (existing == null) {
                // If no existing recommendation, create new one
                return saveRecommendation(beanId, updatedRecommendation, updatedShot)
            }

            // Create updated reason text with taste feedback
            val updatedReason = PersistentGrindRecommendation.createReasonString(
                extractionTimeSeconds = updatedShot.extractionTimeSeconds,
                tasteFeedback = updatedShot.tastePrimary
            )

            // Create updated persistent recommendation, preserving some existing data
            val updatedPersistent = existing.copy(
                suggestedGrindSetting = updatedRecommendation.suggestedGrindSetting,
                adjustmentDirection = updatedRecommendation.adjustmentDirection,
                reason = updatedReason,
                basedOnTaste = updatedShot.tastePrimary != null,
                confidence = updatedRecommendation.confidence
                // Note: preserve timestamp, wasFollowed, and other fields
            )

            // Convert to serializable format for storage
            val serializableRecommendation = SerializableGrindRecommendation(
                beanId = updatedPersistent.beanId,
                suggestedGrindSetting = updatedPersistent.suggestedGrindSetting,
                adjustmentDirection = updatedPersistent.adjustmentDirection.name,
                reason = updatedPersistent.reason,
                recommendedDose = updatedPersistent.recommendedDose,
                targetExtractionTimeMin = updatedPersistent.targetExtractionTime.first,
                targetExtractionTimeMax = updatedPersistent.targetExtractionTime.last,
                timestamp = updatedPersistent.timestamp.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                wasFollowed = updatedPersistent.wasFollowed,
                basedOnTaste = updatedPersistent.basedOnTaste,
                confidence = updatedPersistent.confidence.name
            )

            // Save updated recommendation
            grindRecommendationPreferences.saveRecommendation(beanId, serializableRecommendation)

            Result.success(updatedPersistent)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to update recommendation with taste for bean $beanId",
                    exception
                )
            )
        }
    }

    /**
     * Get all bean IDs that have stored recommendations.
     * Useful for cleanup operations or analytics.
     *
     * @return Result containing list of bean IDs with recommendations
     */
    suspend fun getAllRecommendationBeanIds(): Result<List<String>> {
        return try {
            val beanIds = grindRecommendationPreferences.getAllRecommendationBeanIds()
            Result.success(beanIds)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to get all recommendation bean IDs",
                    exception
                )
            )
        }
    }

    /**
     * Clear all stored recommendations.
     * Primarily used for testing or user data reset.
     *
     * @return Result indicating success or failure
     */
    suspend fun clearAllRecommendations(): Result<Unit> {
        return try {
            grindRecommendationPreferences.clearAllRecommendations()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to clear all recommendations",
                    exception
                )
            )
        }
    }

    /**
     * Convert SerializableGrindRecommendation to PersistentGrindRecommendation.
     * Handles enum conversion and data validation.
     */
    private fun convertToPersistentRecommendation(
        serializable: SerializableGrindRecommendation
    ): PersistentGrindRecommendation {
        return PersistentGrindRecommendation(
            beanId = serializable.beanId,
            suggestedGrindSetting = serializable.suggestedGrindSetting,
            adjustmentDirection = AdjustmentDirection.valueOf(serializable.adjustmentDirection),
            reason = serializable.reason,
            recommendedDose = serializable.recommendedDose,
            targetExtractionTime = serializable.getTargetExtractionTimeRange(),
            timestamp = serializable.getTimestamp(),
            wasFollowed = serializable.wasFollowed,
            basedOnTaste = serializable.basedOnTaste,
            confidence = ConfidenceLevel.valueOf(serializable.confidence)
        )
    }
}
