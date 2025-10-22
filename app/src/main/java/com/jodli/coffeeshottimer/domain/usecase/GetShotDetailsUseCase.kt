package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving detailed shot information.
 * Provides comprehensive shot details including related bean information,
 * analysis metrics, and contextual data for detailed shot views.
 */
@Singleton
class GetShotDetailsUseCase @Inject constructor(
    private val shotRepository: ShotRepository,
    private val beanRepository: BeanRepository
) {

    /**
     * Get detailed shot information by ID.
     * @param shotId The ID of the shot to retrieve
     * @return Result containing detailed shot information or error
     */
    suspend fun getShotDetails(shotId: String): Result<ShotDetails> {
        return try {
            // Get the shot
            val shotResult = shotRepository.getShotById(shotId)
            if (shotResult.isFailure) {
                return Result.failure(
                    shotResult.exceptionOrNull() ?: DomainException(DomainErrorCode.FAILED_TO_GET_SHOT)
                )
            }

            val shot = shotResult.getOrNull()
                ?: return Result.failure(DomainException(DomainErrorCode.SHOT_NOT_FOUND))

            // Get the associated bean
            val beanResult = beanRepository.getBeanById(shot.beanId)
            if (beanResult.isFailure) {
                return Result.failure(
                    beanResult.exceptionOrNull() ?: DomainException(DomainErrorCode.FAILED_TO_GET_BEAN)
                )
            }

            val bean = beanResult.getOrNull()
                ?: return Result.failure(DomainException(DomainErrorCode.ASSOCIATED_BEAN_NOT_FOUND))

            // Calculate additional metrics
            val daysSinceRoast = ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now()).toInt()

            // Get related shots for context
            val relatedShotsResult = shotRepository.getShotsByBean(shot.beanId).first()
            val relatedShots = if (relatedShotsResult.isSuccess) {
                relatedShotsResult.getOrNull() ?: emptyList()
            } else {
                emptyList()
            }

            // Find previous and next shots chronologically
            val sortedShots = relatedShots.sortedBy { it.timestamp }
            val currentIndex = sortedShots.indexOfFirst { it.id == shot.id }
            val previousShot = if (currentIndex > 0) sortedShots[currentIndex - 1] else null
            val nextShot =
                if (currentIndex < sortedShots.size - 1) sortedShots[currentIndex + 1] else null

            // Calculate shot analysis
            val analysis = calculateShotAnalysis(shot, relatedShots, bean)

            // Calculate ranking (only if enough shots exist)
            var rankingForBean: Int? = null
            var isPersonalBest = false
            if (relatedShots.size > 3) {
                // Calculate quality score for all shots
                val shotsWithScores = relatedShots.map { relatedShot ->
                    val relatedAnalysis = calculateShotAnalysis(relatedShot, relatedShots, bean)
                    Pair(relatedShot.id, relatedAnalysis.qualityScore)
                }

                // Sort by score descending (highest first)
                val sortedByScore = shotsWithScores.sortedByDescending { it.second }
                val position = sortedByScore.indexOfFirst { it.first == shot.id } + 1
                rankingForBean = position
                isPersonalBest = position == 1
            }

            val shotDetails = ShotDetails(
                shot = shot,
                bean = bean,
                daysSinceRoast = daysSinceRoast,
                previousShot = previousShot,
                nextShot = nextShot,
                analysis = analysis,
                relatedShotsCount = relatedShots.size,
                rankingForBean = rankingForBean,
                isPersonalBest = isPersonalBest
            )

            Result.success(shotDetails)
        } catch (exception: Exception) {
            Result.failure(
                if (exception is DomainException) {
                    exception
                } else {
                    DomainException(DomainErrorCode.UNKNOWN_ERROR, "Unexpected error getting shot details", exception)
                }
            )
        }
    }

    /**
     * Get shot comparison data for analyzing differences between shots.
     * @param shotId1 ID of the first shot
     * @param shotId2 ID of the second shot
     * @return Result containing shot comparison data
     */
    suspend fun compareShotsDetails(shotId1: String, shotId2: String): Result<ShotComparison> {
        return try {
            val shot1Result = getShotDetails(shotId1)
            val shot2Result = getShotDetails(shotId2)

            if (shot1Result.isFailure) {
                return Result.failure(
                    shot1Result.exceptionOrNull() ?: DomainException(DomainErrorCode.FAILED_TO_GET_SHOT)
                )
            }
            if (shot2Result.isFailure) {
                return Result.failure(
                    shot2Result.exceptionOrNull() ?: DomainException(DomainErrorCode.FAILED_TO_GET_SHOT)
                )
            }

            val shot1Details = shot1Result.getOrNull()!!
            val shot2Details = shot2Result.getOrNull()!!

            val comparison = ShotComparison(
                shot1 = shot1Details,
                shot2 = shot2Details,
                weightInDifference = shot2Details.shot.coffeeWeightIn - shot1Details.shot.coffeeWeightIn,
                weightOutDifference = shot2Details.shot.coffeeWeightOut - shot1Details.shot.coffeeWeightOut,
                extractionTimeDifference = shot2Details.shot.extractionTimeSeconds - shot1Details.shot.extractionTimeSeconds,
                brewRatioDifference = shot2Details.shot.brewRatio - shot1Details.shot.brewRatio,
                sameBeanUsed = shot1Details.bean.id == shot2Details.bean.id,
                sameGrinderSetting = shot1Details.shot.grinderSetting == shot2Details.shot.grinderSetting
            )

            Result.success(comparison)
        } catch (exception: Exception) {
            Result.failure(
                if (exception is DomainException) {
                    exception
                } else {
                    DomainException(DomainErrorCode.UNKNOWN_ERROR, "Unexpected error comparing shots", exception)
                }
            )
        }
    }

    /**
     * Get the last shot for a specific bean (useful for comparison).
     * @param beanId The ID of the bean
     * @return Result containing the last shot details or null if no shots exist
     */
    suspend fun getLastShotForBean(beanId: String): Result<ShotDetails?> {
        return try {
            val lastShotResult = shotRepository.getLastShotForBean(beanId)
            if (lastShotResult.isFailure) {
                return Result.failure(
                    lastShotResult.exceptionOrNull() ?: DomainException(DomainErrorCode.FAILED_TO_GET_SHOT)
                )
            }

            val lastShot = lastShotResult.getOrNull() ?: return Result.success(null)

            getShotDetails(lastShot.id)
        } catch (exception: Exception) {
            Result.failure(
                if (exception is DomainException) {
                    exception
                } else {
                    DomainException(
                        DomainErrorCode.UNKNOWN_ERROR,
                        "Unexpected error getting last shot for bean",
                        exception
                    )
                }
            )
        }
    }

    /**
     * Calculate comprehensive shot analysis metrics.
     */
    private fun calculateShotAnalysis(
        shot: Shot,
        relatedShots: List<Shot>,
        bean: Bean
    ): ShotAnalysis {
        val beanShots = relatedShots.filter { it.beanId == bean.id }

        // Calculate averages for this bean
        val avgBrewRatio = if (beanShots.isNotEmpty()) {
            beanShots.map { it.brewRatio }.average()
        } else {
            0.0
        }

        val avgExtractionTime = if (beanShots.isNotEmpty()) {
            beanShots.map { it.extractionTimeSeconds }.average()
        } else {
            0.0
        }

        val avgWeightIn = if (beanShots.isNotEmpty()) {
            beanShots.map { it.coffeeWeightIn }.average()
        } else {
            0.0
        }

        val avgWeightOut = if (beanShots.isNotEmpty()) {
            beanShots.map { it.coffeeWeightOut }.average()
        } else {
            0.0
        }

        // Calculate deviations from average
        val brewRatioDeviation = shot.brewRatio - avgBrewRatio
        val extractionTimeDeviation = shot.extractionTimeSeconds - avgExtractionTime
        val weightInDeviation = shot.coffeeWeightIn - avgWeightIn
        val weightOutDeviation = shot.coffeeWeightOut - avgWeightOut

        // Determine quality indicators
        val isOptimalExtraction = shot.isOptimalExtractionTime()
        val isTypicalRatio = shot.isTypicalBrewRatio()
        val isConsistentWithHistory = kotlin.math.abs(brewRatioDeviation) < 0.3 &&
            kotlin.math.abs(extractionTimeDeviation) < 5

        // Calculate quality score with transparent breakdown (0-100)
        // Extraction Time: 0-25 points
        val extractionTimePoints = when {
            isOptimalExtraction -> 25 // Perfect range (25-30s)
            shot.extractionTimeSeconds in 20..35 -> 15 // Close to optimal
            else -> 5 // Needs significant adjustment
        }

        // Brew Ratio: 0-20 points
        val brewRatioPoints = when {
            isTypicalRatio -> 20 // Typical range (1.5-2.5)
            shot.brewRatio in 1.3..2.8 -> 12 // Close to typical
            else -> 4 // Unusual ratio
        }

        // Taste Feedback: 0-30 points (most important - what user actually experienced)
        val tastePoints = when (shot.tastePrimary) {
            com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT -> 30 // User loved it
            com.jodli.coffeeshottimer.domain.model.TastePrimary.SOUR -> 10 // Under-extracted but informative
            com.jodli.coffeeshottimer.domain.model.TastePrimary.BITTER -> 10 // Over-extracted but informative
            null -> 15 // No feedback yet - neutral score
        }

        // Consistency: 0-15 points
        val consistencyPoints = if (isConsistentWithHistory) 15 else 5

        // Deviation Bonus: 0-10 points (precision in execution)
        var deviationBonusPoints = 0
        if (kotlin.math.abs(brewRatioDeviation) < 0.1) deviationBonusPoints += 5
        if (kotlin.math.abs(extractionTimeDeviation) < 2) deviationBonusPoints += 5

        val qualityScore = (extractionTimePoints + brewRatioPoints + tastePoints +
            consistencyPoints + deviationBonusPoints).coerceIn(0, 100)

        // Generate improvement path
        val improvementPath = generateImprovementPath(
            qualityScore,
            extractionTimePoints,
            brewRatioPoints,
            tastePoints,
            shot
        )

        // Generate recommendations
        val recommendations = mutableListOf<ShotRecommendation>()

        if (!isOptimalExtraction) {
            if (shot.extractionTimeSeconds < 25) {
                recommendations.add(
                    ShotRecommendation(
                        type = RecommendationType.GRIND_FINER,
                        priority = RecommendationPriority.HIGH,
                        currentValue = shot.extractionTimeSeconds.toDouble(),
                        targetRange = 25.0..30.0,
                        context = mapOf("currentTime" to shot.extractionTimeSeconds.toString())
                    )
                )
            } else {
                recommendations.add(
                    ShotRecommendation(
                        type = RecommendationType.GRIND_COARSER,
                        priority = RecommendationPriority.HIGH,
                        currentValue = shot.extractionTimeSeconds.toDouble(),
                        targetRange = 25.0..30.0,
                        context = mapOf("currentTime" to shot.extractionTimeSeconds.toString())
                    )
                )
            }
        }

        if (!isTypicalRatio) {
            if (shot.brewRatio < 1.5) {
                recommendations.add(
                    ShotRecommendation(
                        type = RecommendationType.INCREASE_YIELD,
                        priority = RecommendationPriority.MEDIUM,
                        currentValue = shot.brewRatio,
                        targetRange = 1.5..3.0,
                        context = mapOf("currentRatio" to String.format(java.util.Locale.ROOT, "%.2f", shot.brewRatio))
                    )
                )
            } else if (shot.brewRatio > 3.0) {
                recommendations.add(
                    ShotRecommendation(
                        type = RecommendationType.DECREASE_YIELD,
                        priority = RecommendationPriority.MEDIUM,
                        currentValue = shot.brewRatio,
                        targetRange = 1.5..3.0,
                        context = mapOf("currentRatio" to String.format(java.util.Locale.ROOT, "%.2f", shot.brewRatio))
                    )
                )
            }
        }

        if (kotlin.math.abs(brewRatioDeviation) > 0.5) {
            recommendations.add(
                ShotRecommendation(
                    type = RecommendationType.RATIO_INCONSISTENCY,
                    priority = RecommendationPriority.LOW,
                    currentValue = shot.brewRatio,
                    targetRange = (avgBrewRatio - 0.3)..(avgBrewRatio + 0.3),
                    context = mapOf(
                        "deviation" to String.format(java.util.Locale.ROOT, "%.2f", brewRatioDeviation),
                        "avgRatio" to String.format(java.util.Locale.ROOT, "%.2f", avgBrewRatio)
                    )
                )
            )
        }

        return ShotAnalysis(
            qualityScore = qualityScore,
            isOptimalExtraction = isOptimalExtraction,
            isTypicalRatio = isTypicalRatio,
            isConsistentWithHistory = isConsistentWithHistory,
            brewRatioDeviation = brewRatioDeviation,
            extractionTimeDeviation = extractionTimeDeviation,
            weightInDeviation = weightInDeviation,
            weightOutDeviation = weightOutDeviation,
            avgBrewRatioForBean = avgBrewRatio,
            avgExtractionTimeForBean = avgExtractionTime,
            avgWeightInForBean = avgWeightIn,
            avgWeightOutForBean = avgWeightOut,
            recommendations = recommendations,
            extractionTimePoints = extractionTimePoints,
            brewRatioPoints = brewRatioPoints,
            tastePoints = tastePoints,
            consistencyPoints = consistencyPoints,
            deviationBonusPoints = deviationBonusPoints,
            improvementPath = improvementPath
        )
    }

    /**
     * Generate improvement path suggestion based on current score.
     */
    private fun generateImprovementPath(
        currentScore: Int,
        extractionTimePoints: Int,
        brewRatioPoints: Int,
        tastePoints: Int,
        shot: Shot
    ): String? {
        // Already excellent
        if (currentScore >= 85) return null

        val suggestions = mutableListOf<String>()

        // Check what needs the most improvement
        if (extractionTimePoints < 20 && !shot.isOptimalExtractionTime()) {
            val action = if (shot.extractionTimeSeconds < 25) "Grind finer" else "Grind coarser"
            suggestions.add(action)
        }

        if (tastePoints < 20 && shot.tastePrimary != com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT) {
            // Taste feedback suggests adjustment
            if (suggestions.isEmpty()) {
                suggestions.add("Dial in based on taste")
            }
        }

        if (brewRatioPoints < 15 && !shot.isTypicalBrewRatio()) {
            suggestions.add("Adjust brew ratio")
        }

        if (suggestions.isEmpty()) return null

        val pointsNeeded = 85 - currentScore
        val nextTier = if (currentScore >= 60) "Excellent" else "Good"
        return "${suggestions.first()} → +$pointsNeeded points → $nextTier"
    }
}

/**
 * Data class containing comprehensive shot details.
 */
data class ShotDetails(
    val shot: Shot,
    val bean: Bean,
    val daysSinceRoast: Int,
    val previousShot: Shot?,
    val nextShot: Shot?,
    val analysis: ShotAnalysis,
    val relatedShotsCount: Int,
    val rankingForBean: Int? = null, // Position ranked by quality score (1 = best)
    val isPersonalBest: Boolean = false
)

/**
 * Data class containing shot analysis metrics.
 */
data class ShotAnalysis(
    val qualityScore: Int, // 0-100 quality score
    val isOptimalExtraction: Boolean,
    val isTypicalRatio: Boolean,
    val isConsistentWithHistory: Boolean,
    val brewRatioDeviation: Double,
    val extractionTimeDeviation: Double,
    val weightInDeviation: Double,
    val weightOutDeviation: Double,
    val avgBrewRatioForBean: Double,
    val avgExtractionTimeForBean: Double,
    val avgWeightInForBean: Double,
    val avgWeightOutForBean: Double,
    val recommendations: List<ShotRecommendation>,
    // Quality score breakdown for transparency
    val extractionTimePoints: Int, // 0-25 points
    val brewRatioPoints: Int, // 0-20 points
    val tastePoints: Int, // 0-30 points
    val consistencyPoints: Int, // 0-15 points
    val deviationBonusPoints: Int, // 0-10 points
    val improvementPath: String? = null // e.g., "Grind coarser → +5 points → Excellent"
)

/**
 * Data class for comparing two shots.
 */
data class ShotComparison(
    val shot1: ShotDetails,
    val shot2: ShotDetails,
    val weightInDifference: Double,
    val weightOutDifference: Double,
    val extractionTimeDifference: Int,
    val brewRatioDifference: Double,
    val sameBeanUsed: Boolean,
    val sameGrinderSetting: Boolean
) {
    /**
     * Get formatted difference strings for UI display.
     */
    fun getFormattedDifferences(): Map<String, String> {
        return mapOf(
            "weightIn" to "${if (weightInDifference >= 0) "+" else ""}${
                String.format(
                    java.util.Locale.ROOT,
                    "%.1f",
                    weightInDifference
                )
            }g",
            "weightOut" to "${if (weightOutDifference >= 0) "+" else ""}${
                String.format(
                    java.util.Locale.ROOT,
                    "%.1f",
                    weightOutDifference
                )
            }g",
            "extractionTime" to "${if (extractionTimeDifference >= 0) "+" else ""}${extractionTimeDifference}s",
            "brewRatio" to "${if (brewRatioDifference >= 0) "+" else ""}${
                String.format(
                    java.util.Locale.ROOT,
                    "%.2f",
                    brewRatioDifference
                )
            }"
        )
    }
}

/**
 * Represents a structured recommendation for shot improvement.
 */
data class ShotRecommendation(
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val currentValue: Double,
    val targetRange: ClosedFloatingPointRange<Double>,
    val context: Map<String, String> = emptyMap()
)

/**
 * Types of recommendations that can be made for shot improvement.
 */
enum class RecommendationType {
    GRIND_FINER,
    GRIND_COARSER,
    INCREASE_YIELD,
    DECREASE_YIELD,
    RATIO_INCONSISTENCY,
    DOSE_ADJUSTMENT,
    TIMING_CONSISTENCY
}

/**
 * Priority levels for recommendations.
 */
enum class RecommendationPriority {
    HIGH, // Critical for shot quality
    MEDIUM, // Important for consistency
    LOW // Nice to have improvements
}
