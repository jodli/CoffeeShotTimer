package com.jodli.coffeeshottimer.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a persistent grind recommendation for a specific bean.
 * This model extends the temporary GrindAdjustmentRecommendation with persistence capabilities
 * and additional context needed for displaying next-shot guidance.
 *
 * Unlike GrindAdjustmentRecommendation which is calculated on-demand, this model is stored
 * and retrieved across app sessions to provide consistent guidance.
 */
data class PersistentGrindRecommendation(
    /** The ID of the bean this recommendation applies to */
    val beanId: String,

    /** The suggested grind setting (e.g., "5.5") */
    val suggestedGrindSetting: String,

    /** Direction of adjustment from previous setting */
    val adjustmentDirection: AdjustmentDirection,

    /** Human-readable explanation of why this adjustment is recommended */
    val reason: String,

    /** Recommended dose for this bean (from bean settings) */
    val recommendedDose: Double,

    /** Target extraction time range (typically 25-30s for espresso) */
    val targetExtractionTime: IntRange,

    /** When this recommendation was created */
    val timestamp: LocalDateTime,

    /** Whether the user has applied this recommendation */
    val wasFollowed: Boolean = false,

    /** Whether this recommendation was based on taste feedback (true) or timing only (false) */
    val basedOnTaste: Boolean,

    /** Confidence level of this recommendation */
    val confidence: ConfidenceLevel
) {

    /**
     * Check if this recommendation suggests any change from current grind setting.
     * @return true if an adjustment is recommended, false for no-change recommendations
     */
    fun hasAdjustment(): Boolean = adjustmentDirection != AdjustmentDirection.NO_CHANGE

    /**
     * Create a new recommendation marked as followed.
     * @return A copy of this recommendation with wasFollowed = true
     */
    fun markAsFollowed(): PersistentGrindRecommendation {
        return copy(wasFollowed = true)
    }

    companion object {
        /**
         * Create a PersistentGrindRecommendation from a GrindAdjustmentRecommendation
         * and additional bean/shot context.
         *
         * @param beanId The bean this recommendation is for
         * @param recommendation The calculated grind adjustment
         * @param recommendedDose The dose for this bean
         * @param reason Human-readable explanation
         * @param basedOnTaste Whether taste feedback was used
         * @return New persistent recommendation
         */
        fun fromGrindAdjustmentRecommendation(
            beanId: String,
            recommendation: GrindAdjustmentRecommendation,
            recommendedDose: Double,
            reason: String,
            basedOnTaste: Boolean
        ): PersistentGrindRecommendation {
            return PersistentGrindRecommendation(
                beanId = beanId,
                suggestedGrindSetting = recommendation.suggestedGrindSetting,
                adjustmentDirection = recommendation.adjustmentDirection,
                reason = reason,
                recommendedDose = recommendedDose,
                targetExtractionTime = 25..30, // Standard espresso range
                timestamp = LocalDateTime.now(),
                wasFollowed = false,
                basedOnTaste = basedOnTaste,
                confidence = recommendation.confidence
            )
        }

        /**
         * Create a reason string based on shot data and taste feedback.
         *
         * @param extractionTimeSeconds The extraction time of the shot
         * @param tasteFeedback The taste feedback (null if none provided)
         * @return Human-readable reason string
         */
        fun createReasonString(
            extractionTimeSeconds: Int,
            tasteFeedback: TastePrimary?
        ): String {
            return when {
                tasteFeedback == TastePrimary.SOUR -> "Last shot was sour (${extractionTimeSeconds}s)"
                tasteFeedback == TastePrimary.BITTER -> "Last shot was bitter (${extractionTimeSeconds}s)"
                tasteFeedback == TastePrimary.PERFECT -> "Last shot was perfect (${extractionTimeSeconds}s)"
                extractionTimeSeconds < 25 -> "Last shot ran too fast (${extractionTimeSeconds}s)"
                extractionTimeSeconds > 30 -> "Last shot ran too slow (${extractionTimeSeconds}s)"
                else -> "Based on previous shot (${extractionTimeSeconds}s)"
            }
        }
    }
}
