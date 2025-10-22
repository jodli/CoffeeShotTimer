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

    companion object {
        // Quality score point allocations
        private const val MAX_EXTRACTION_TIME_POINTS = 25
        private const val GOOD_EXTRACTION_TIME_POINTS = 15
        private const val MIN_EXTRACTION_TIME_POINTS = 5

        private const val MAX_BREW_RATIO_POINTS = 20
        private const val GOOD_BREW_RATIO_POINTS = 12
        private const val MIN_BREW_RATIO_POINTS = 4

        private const val PERFECT_TASTE_POINTS = 30
        private const val NEUTRAL_TASTE_POINTS = 15
        private const val INFORMATIVE_TASTE_POINTS = 10

        private const val MAX_CONSISTENCY_POINTS = 15
        private const val MIN_CONSISTENCY_POINTS = 5

        private const val DEVIATION_BONUS_POINTS = 5

        // Quality thresholds
        private const val MIN_SHOTS_FOR_RANKING = 3
        private const val EXCELLENT_SCORE_THRESHOLD = 85
        private const val GOOD_SCORE_THRESHOLD = 60

        // Extraction time ranges
        private const val MIN_OPTIMAL_EXTRACTION = 25
        private const val MAX_OPTIMAL_EXTRACTION = 30
        private const val MIN_ACCEPTABLE_EXTRACTION = 20
        private const val MAX_ACCEPTABLE_EXTRACTION = 35

        // Brew ratio ranges
        private const val MIN_ACCEPTABLE_RATIO = 1.3
        private const val MAX_ACCEPTABLE_RATIO = 2.8

        // Deviation thresholds
        private const val TIGHT_RATIO_DEVIATION = 0.1
        private const val TIGHT_TIME_DEVIATION = 2
    }

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
            if (relatedShots.size > MIN_SHOTS_FOR_RANKING) {
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
     * Calculates average brew ratio, extraction time, coffee weight in, and coffee weight out
     * for a list of shots.
     */
    private fun calculateAverages(beanShots: List<Shot>): ShotAverages {
        if (beanShots.isEmpty()) {
            return ShotAverages(0.0, 0.0, 0.0, 0.0)
        }

        val totalBrewRatio = beanShots.sumOf { it.brewRatio }
        val totalExtractionTime = beanShots.sumOf { it.extractionTimeSeconds.toDouble() }
        val totalWeightIn = beanShots.sumOf { it.coffeeWeightIn }
        val totalWeightOut = beanShots.sumOf { it.coffeeWeightOut }

        return ShotAverages(
            avgBrewRatio = totalBrewRatio / beanShots.size,
            avgExtractionTime = totalExtractionTime / beanShots.size,
            avgWeightIn = totalWeightIn / beanShots.size,
            avgWeightOut = totalWeightOut / beanShots.size
        )
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
        val averages = calculateAverages(beanShots)
        val avgBrewRatio = averages.avgBrewRatio
        val avgExtractionTime = averages.avgExtractionTime
        val avgWeightIn = averages.avgWeightIn
        val avgWeightOut = averages.avgWeightOut

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
        val extractionTimePoints = calculateExtractionTimePoints(shot, isOptimalExtraction)
        val brewRatioPoints = calculateBrewRatioPoints(shot, isTypicalRatio)
        val tastePoints = calculateTastePoints(shot)
        val consistencyPoints = if (isConsistentWithHistory) MAX_CONSISTENCY_POINTS else MIN_CONSISTENCY_POINTS
        val deviationBonusPoints = calculateDeviationBonus(brewRatioDeviation, extractionTimeDeviation)

        val qualityScore = (
            extractionTimePoints + brewRatioPoints + tastePoints +
                consistencyPoints + deviationBonusPoints
            ).coerceIn(0, 100)

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
     * Calculate extraction time points based on shot timing.
     */
    private fun calculateExtractionTimePoints(shot: Shot, isOptimal: Boolean): Int {
        return when {
            isOptimal -> MAX_EXTRACTION_TIME_POINTS
            shot.extractionTimeSeconds in MIN_ACCEPTABLE_EXTRACTION..MAX_ACCEPTABLE_EXTRACTION ->
                GOOD_EXTRACTION_TIME_POINTS
            else -> MIN_EXTRACTION_TIME_POINTS
        }
    }

    /**
     * Calculate brew ratio points based on shot ratio.
     */
    private fun calculateBrewRatioPoints(shot: Shot, isTypical: Boolean): Int {
        return when {
            isTypical -> MAX_BREW_RATIO_POINTS
            shot.brewRatio in MIN_ACCEPTABLE_RATIO..MAX_ACCEPTABLE_RATIO -> GOOD_BREW_RATIO_POINTS
            else -> MIN_BREW_RATIO_POINTS
        }
    }

    /**
     * Calculate taste feedback points.
     */
    private fun calculateTastePoints(shot: Shot): Int {
        return when (shot.tastePrimary) {
            com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT -> PERFECT_TASTE_POINTS
            com.jodli.coffeeshottimer.domain.model.TastePrimary.SOUR -> INFORMATIVE_TASTE_POINTS
            com.jodli.coffeeshottimer.domain.model.TastePrimary.BITTER -> INFORMATIVE_TASTE_POINTS
            null -> NEUTRAL_TASTE_POINTS
        }
    }

    /**
     * Calculate deviation bonus points for precision.
     */
    private fun calculateDeviationBonus(
        brewRatioDeviation: Double,
        extractionTimeDeviation: Double
    ): Int {
        var bonus = 0
        if (kotlin.math.abs(brewRatioDeviation) < TIGHT_RATIO_DEVIATION) bonus += DEVIATION_BONUS_POINTS
        if (kotlin.math.abs(extractionTimeDeviation) < TIGHT_TIME_DEVIATION) bonus += DEVIATION_BONUS_POINTS
        return bonus
    }

    /**
     * Generate improvement path suggestion based on current score.
     */
    @Suppress("ReturnCount")
    private fun generateImprovementPath(
        currentScore: Int,
        extractionTimePoints: Int,
        brewRatioPoints: Int,
        tastePoints: Int,
        shot: Shot
    ): ImprovementPath? {
        // Already excellent
        if (currentScore >= EXCELLENT_SCORE_THRESHOLD) return null

        val action = determineImprovementAction(
            extractionTimePoints,
            brewRatioPoints,
            tastePoints,
            shot
        ) ?: return null

        val pointsNeeded = EXCELLENT_SCORE_THRESHOLD - currentScore
        val targetTier = if (currentScore >= GOOD_SCORE_THRESHOLD) {
            QualityTier.EXCELLENT
        } else {
            QualityTier.GOOD
        }

        return ImprovementPath(
            action = action,
            pointsNeeded = pointsNeeded,
            targetTier = targetTier
        )
    }

    /**
     * Determine the most important improvement action based on analysis.
     */
    @Suppress("ReturnCount")
    private fun determineImprovementAction(
        extractionTimePoints: Int,
        brewRatioPoints: Int,
        tastePoints: Int,
        shot: Shot
    ): ImprovementAction? {
        // Check what needs the most improvement (priority order)

        // 1. Extraction time is most impactful
        if (extractionTimePoints < MAX_BREW_RATIO_POINTS && !shot.isOptimalExtractionTime()) {
            return if (shot.extractionTimeSeconds < MIN_OPTIMAL_EXTRACTION) {
                ImprovementAction.GRIND_FINER
            } else {
                ImprovementAction.GRIND_COARSER
            }
        }

        // 2. Taste feedback suggests general dialing in
        if (tastePoints < MAX_BREW_RATIO_POINTS &&
            shot.tastePrimary != com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT
        ) {
            return ImprovementAction.DIAL_IN_BASED_ON_TASTE
        }

        // 3. Brew ratio adjustment
        if (brewRatioPoints < GOOD_EXTRACTION_TIME_POINTS && !shot.isTypicalBrewRatio()) {
            return ImprovementAction.ADJUST_BREW_RATIO
        }

        return null
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
    val improvementPath: ImprovementPath? = null
)

/**
 * Structured improvement path for translation.
 */
data class ImprovementPath(
    val action: ImprovementAction,
    val pointsNeeded: Int,
    val targetTier: QualityTier
)

/**
 * Actions that can be taken to improve shot quality.
 */
enum class ImprovementAction {
    GRIND_FINER,
    GRIND_COARSER,
    ADJUST_BREW_RATIO,
    DIAL_IN_BASED_ON_TASTE
}

/**
 * Quality score tiers.
 */
enum class QualityTier {
    NEEDS_WORK, // < 60
    GOOD, // 60-84
    EXCELLENT // 85+
}

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

/**
 * Data class to hold average shot metrics.
 */
data class ShotAverages(
    val avgBrewRatio: Double,
    val avgExtractionTime: Double,
    val avgWeightIn: Double,
    val avgWeightOut: Double
)
