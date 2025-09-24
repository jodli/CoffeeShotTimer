package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.round

/**
 * Use case for calculating grind adjustment recommendations based on shot performance.
 *
 * This use case implements the core business logic for providing actionable grind advice
 * to users based on their taste feedback and extraction metrics. The recommendations
 * follow established espresso extraction principles:
 *
 * - Sour taste or fast extraction (< 25s) → Grind finer to increase extraction
 * - Bitter taste or slow extraction (> 30s) → Grind coarser to decrease extraction
 * - Perfect taste with optimal timing (25-30s) → No change needed
 *
 * The adjustment magnitude scales with the severity of the deviation, and all
 * recommendations respect the user's grinder configuration limits.
 */
@Singleton
class CalculateGrindAdjustmentUseCase @Inject constructor(
    private val grinderConfigRepository: GrinderConfigRepository
) {

    companion object {
        // Optimal extraction time range based on espresso brewing standards
        private const val OPTIMAL_MIN_TIME = 25
        private const val OPTIMAL_MAX_TIME = 30

        // Deviation thresholds for adjustment step calculation
        private const val MINOR_DEVIATION_THRESHOLD = 3
        private const val MODERATE_DEVIATION_THRESHOLD = 6

        // Step adjustment amounts
        private const val MINOR_ADJUSTMENT_STEPS = 1
        private const val MODERATE_ADJUSTMENT_STEPS = 2
        private const val MAJOR_ADJUSTMENT_STEPS = 3
    }

    /**
     * Calculate grind adjustment recommendation based on shot performance.
     *
     * @param currentGrindSetting The grind setting used for the current shot
     * @param extractionTimeSeconds The extraction time in seconds
     * @param tasteFeedback Optional taste feedback from the user
     * @return Result containing the grind adjustment recommendation or error
     */
    suspend fun calculateAdjustment(
        currentGrindSetting: String,
        extractionTimeSeconds: Int,
        tasteFeedback: TastePrimary?
    ): Result<GrindAdjustmentRecommendation> {
        return try {
            // Get grinder configuration
            val configResult = grinderConfigRepository.getCurrentConfig()
            if (configResult.isFailure) {
                return Result.failure(
                    configResult.exceptionOrNull() ?: DomainException(
                        com.jodli.coffeeshottimer.domain.model.DomainErrorCode.GRINDER_CONFIG_NOT_FOUND,
                        "Failed to get grinder configuration"
                    )
                )
            }

            val grinderConfig = configResult.getOrNull()
                ?: return Result.failure(
                    DomainException(
                        com.jodli.coffeeshottimer.domain.model.DomainErrorCode.GRINDER_CONFIG_NOT_FOUND,
                        "No grinder configuration found"
                    )
                )

            // Validate current grind setting format
            val currentValue = currentGrindSetting.toDoubleOrNull()
                ?: return Result.failure(
                    DomainException(
                        com.jodli.coffeeshottimer.domain.model.DomainErrorCode.INVALID_GRIND_SETTING,
                        "Invalid grind setting format: $currentGrindSetting"
                    )
                )

            // Validate extraction time
            if (extractionTimeSeconds < 0) {
                return Result.failure(
                    DomainException(
                        com.jodli.coffeeshottimer.domain.model.DomainErrorCode.INVALID_EXTRACTION_TIME,
                        "Extraction time cannot be negative: $extractionTimeSeconds"
                    )
                )
            }

            // Calculate time deviation from optimal range
            val timeDeviation = calculateTimeDeviation(extractionTimeSeconds)

            // Determine adjustment based on taste feedback and extraction time
            // Prioritize taste feedback over timing when they conflict
            val recommendation = when {
                // First priority: Taste feedback (direct indicator of extraction quality)
                tasteFeedback == TastePrimary.BITTER -> {
                    calculateCoarserAdjustment(
                        grinderConfig,
                        currentValue,
                        timeDeviation,
                        tasteFeedback
                    )
                }
                tasteFeedback == TastePrimary.SOUR -> {
                    calculateFinerAdjustment(
                        grinderConfig,
                        currentValue,
                        timeDeviation,
                        tasteFeedback
                    )
                }
                // Second priority: Timing-based recommendations (when no taste feedback)
                extractionTimeSeconds < OPTIMAL_MIN_TIME -> {
                    calculateFinerAdjustment(
                        grinderConfig,
                        currentValue,
                        timeDeviation,
                        tasteFeedback
                    )
                }
                extractionTimeSeconds > OPTIMAL_MAX_TIME -> {
                    calculateCoarserAdjustment(
                        grinderConfig,
                        currentValue,
                        timeDeviation,
                        tasteFeedback
                    )
                }
                else -> {
                    createNoChangeRecommendation(currentGrindSetting, extractionTimeSeconds, tasteFeedback)
                }
            }

            Result.success(recommendation)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    com.jodli.coffeeshottimer.domain.model.DomainErrorCode.CALCULATION_ERROR,
                    "Failed to calculate grind adjustment",
                    exception
                )
            )
        }
    }

    /**
     * Calculate time deviation from optimal extraction range.
     * @param extractionTimeSeconds The actual extraction time
     * @return Deviation in seconds (negative = too fast, positive = too slow, 0 = optimal)
     */
    private fun calculateTimeDeviation(extractionTimeSeconds: Int): Int {
        return when {
            extractionTimeSeconds < OPTIMAL_MIN_TIME -> extractionTimeSeconds - OPTIMAL_MIN_TIME
            extractionTimeSeconds > OPTIMAL_MAX_TIME -> extractionTimeSeconds - OPTIMAL_MAX_TIME
            else -> 0
        }
    }

    /**
     * Calculate a finer grind adjustment recommendation.
     */
    private fun calculateFinerAdjustment(
        config: GrinderConfiguration,
        currentValue: Double,
        timeDeviation: Int,
        tasteFeedback: TastePrimary?
    ): GrindAdjustmentRecommendation {
        val adjustmentSteps = calculateAdjustmentSteps(abs(timeDeviation))
        val adjustmentAmount = adjustmentSteps * config.stepSize
        val newValue = (currentValue - adjustmentAmount).coerceAtLeast(config.scaleMin.toDouble())

        // If we hit the minimum limit, adjust steps accordingly
        val actualAdjustmentAmount = currentValue - newValue
        val actualSteps = round(actualAdjustmentAmount / config.stepSize).toInt()

        val confidence = calculateConfidence(timeDeviation, tasteFeedback)

        return GrindAdjustmentRecommendation(
            currentGrindSetting = config.formatGrindValue(currentValue),
            suggestedGrindSetting = config.formatGrindValue(newValue),
            adjustmentDirection = AdjustmentDirection.FINER,
            adjustmentSteps = actualSteps,
            extractionTimeDeviation = timeDeviation,
            tasteIssue = tasteFeedback,
            confidence = confidence
        )
    }

    /**
     * Calculate a coarser grind adjustment recommendation.
     */
    private fun calculateCoarserAdjustment(
        config: GrinderConfiguration,
        currentValue: Double,
        timeDeviation: Int,
        tasteFeedback: TastePrimary?
    ): GrindAdjustmentRecommendation {
        val adjustmentSteps = calculateAdjustmentSteps(abs(timeDeviation))
        val adjustmentAmount = adjustmentSteps * config.stepSize
        val newValue = (currentValue + adjustmentAmount).coerceAtMost(config.scaleMax.toDouble())

        // If we hit the maximum limit, adjust steps accordingly
        val actualAdjustmentAmount = newValue - currentValue
        val actualSteps = round(actualAdjustmentAmount / config.stepSize).toInt()

        val confidence = calculateConfidence(timeDeviation, tasteFeedback)

        return GrindAdjustmentRecommendation(
            currentGrindSetting = config.formatGrindValue(currentValue),
            suggestedGrindSetting = config.formatGrindValue(newValue),
            adjustmentDirection = AdjustmentDirection.COARSER,
            adjustmentSteps = actualSteps,
            extractionTimeDeviation = timeDeviation,
            tasteIssue = tasteFeedback,
            confidence = confidence
        )
    }

    /**
     * Create a no-change recommendation for optimal shots.
     */
    private fun createNoChangeRecommendation(
        currentGrindSetting: String,
        extractionTimeSeconds: Int,
        tasteFeedback: TastePrimary?
    ): GrindAdjustmentRecommendation {
        return GrindAdjustmentRecommendation(
            currentGrindSetting = currentGrindSetting,
            suggestedGrindSetting = currentGrindSetting,
            adjustmentDirection = AdjustmentDirection.NO_CHANGE,
            adjustmentSteps = 0,
            extractionTimeDeviation = calculateTimeDeviation(extractionTimeSeconds),
            tasteIssue = tasteFeedback,
            confidence = ConfidenceLevel.HIGH
        )
    }

    /**
     * Calculate the number of adjustment steps based on deviation magnitude.
     * @param deviationMagnitude Absolute value of the time deviation
     * @return Number of steps to adjust
     */
    private fun calculateAdjustmentSteps(deviationMagnitude: Int): Int {
        return when {
            deviationMagnitude <= MINOR_DEVIATION_THRESHOLD -> MINOR_ADJUSTMENT_STEPS
            deviationMagnitude <= MODERATE_DEVIATION_THRESHOLD -> MODERATE_ADJUSTMENT_STEPS
            else -> MAJOR_ADJUSTMENT_STEPS
        }
    }

    /**
     * Calculate confidence level based on available evidence.
     * @param timeDeviation Deviation from optimal extraction time
     * @param tasteFeedback User's taste feedback
     * @return Confidence level of the recommendation
     */
    private fun calculateConfidence(timeDeviation: Int, tasteFeedback: TastePrimary?): ConfidenceLevel {
        val hasStrongTimeEvidence = abs(timeDeviation) >= MINOR_DEVIATION_THRESHOLD
        val hasTasteEvidence = tasteFeedback != null && tasteFeedback != TastePrimary.PERFECT

        return when {
            hasStrongTimeEvidence && hasTasteEvidence -> ConfidenceLevel.HIGH
            hasStrongTimeEvidence || hasTasteEvidence -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
}
