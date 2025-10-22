package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use case for calculating shot quality scores and aggregate quality analysis.
 * Serves as the single source of truth for quality score calculations across the app.
 *
 * This use case consolidates the quality score algorithm originally from GetShotDetailsUseCase
 * and provides both individual shot scoring and aggregate analysis capabilities.
 */
@Singleton
class GetShotQualityAnalysisUseCase @Inject constructor(
    private val shotRepository: ShotRepository
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

        // Trend analysis
        private const val TREND_THRESHOLD = 5
        private const val MIN_SHOTS_FOR_RECENT_AVERAGE = 5

        // Consistency calculation
        private const val CONSISTENCY_DEVIATION_RATIO_THRESHOLD = 0.3
        private const val CONSISTENCY_DEVIATION_TIME_THRESHOLD = 5
        private const val MAX_QUALITY_SCORE = 100
        private const val PERCENTAGE_MULTIPLIER = 100
    }

    /**
     * Calculate quality score for a single shot.
     * Uses a 5-component algorithm for comprehensive quality assessment.
     *
     * @param shot The shot to evaluate
     * @param relatedShots Related shots for the same bean (for consistency comparison)
     * @return Quality score from 0-100
     */
    fun calculateShotQualityScore(shot: Shot, relatedShots: List<Shot>): Int {
        // Calculate bean-specific averages for consistency comparison
        val beanShots = relatedShots.filter { it.beanId == shot.beanId }
        val avgBrewRatio = if (beanShots.isNotEmpty()) {
            beanShots.sumOf { it.brewRatio } / beanShots.size
        } else {
            shot.brewRatio
        }
        val avgExtractionTime = if (beanShots.isNotEmpty()) {
            beanShots.sumOf { it.extractionTimeSeconds.toDouble() } / beanShots.size
        } else {
            shot.extractionTimeSeconds.toDouble()
        }

        // Calculate deviations
        val brewRatioDeviation = shot.brewRatio - avgBrewRatio
        val extractionTimeDeviation = shot.extractionTimeSeconds - avgExtractionTime

        // Component 1: Extraction time points (0-25)
        val extractionTimePoints = calculateExtractionTimePoints(shot)

        // Component 2: Brew ratio points (0-20)
        val brewRatioPoints = calculateBrewRatioPoints(shot)

        // Component 3: Taste feedback points (0-30)
        val tastePoints = calculateTastePoints(shot)

        // Component 4: Consistency points (0-15)
        val isConsistentWithHistory = abs(brewRatioDeviation) < CONSISTENCY_DEVIATION_RATIO_THRESHOLD &&
            abs(extractionTimeDeviation) < CONSISTENCY_DEVIATION_TIME_THRESHOLD
        val consistencyPoints = if (isConsistentWithHistory) MAX_CONSISTENCY_POINTS else MIN_CONSISTENCY_POINTS

        // Component 5: Deviation bonus (0-10)
        val deviationBonusPoints = calculateDeviationBonus(brewRatioDeviation, extractionTimeDeviation)

        // Total score (0-100)
        return (
            extractionTimePoints + brewRatioPoints + tastePoints +
                consistencyPoints + deviationBonusPoints
            ).coerceIn(0, MAX_QUALITY_SCORE)
    }

    /**
     * Calculate aggregate quality analysis for multiple shots.
     * Provides dashboard-level insights including distribution, trends, and consistency.
     *
     * @param shots The shots to analyze
     * @param allShots All shots available for context (used for per-shot quality calculation)
     * @return Aggregate quality analysis with trends and distribution
     */
    fun calculateAggregateQualityAnalysis(shots: List<Shot>, allShots: List<Shot>): AggregateQualityAnalysis {
        if (shots.isEmpty()) return AggregateQualityAnalysis.empty()

        // Calculate individual scores
        val scoresWithShots = shots.map { shot ->
            val score = calculateShotQualityScore(shot, allShots)
            Pair(shot, score)
        }

        val scores = scoresWithShots.map { it.second }

        // Distribution
        val excellentCount = scores.count { it >= EXCELLENT_SCORE_THRESHOLD }
        val goodCount = scores.count { it in GOOD_SCORE_THRESHOLD until EXCELLENT_SCORE_THRESHOLD }
        val needsWorkCount = scores.count { it < GOOD_SCORE_THRESHOLD }

        // Overall average
        val overallAverage = scores.average().toInt()

        // Recent average (last 5)
        val recentShots = scoresWithShots
            .sortedByDescending { it.first.timestamp }
            .take(MIN_SHOTS_FOR_RECENT_AVERAGE)
        val recentAverage = if (recentShots.isNotEmpty()) {
            recentShots.map { it.second }.average().toInt()
        } else {
            overallAverage
        }

        // Trend direction
        val trendDirection = when {
            recentAverage > overallAverage + TREND_THRESHOLD -> TrendDirection.IMPROVING
            recentAverage < overallAverage - TREND_THRESHOLD -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        // Improvement rate
        val improvementRate = if (overallAverage > 0) {
            ((recentAverage - overallAverage).toDouble() / overallAverage) * PERCENTAGE_MULTIPLIER
        } else {
            0.0
        }

        // Consistency (inverse of coefficient of variation)
        val stdDev = calculateStandardDeviation(scores.map { it.toDouble() })
        val consistencyScore = if (overallAverage > 0) {
            (MAX_QUALITY_SCORE - (stdDev / overallAverage * PERCENTAGE_MULTIPLIER)).coerceIn(
                0.0,
                MAX_QUALITY_SCORE.toDouble()
            ).toInt()
        } else {
            0
        }

        // Determine tier based on recent performance
        val qualityTier = when {
            recentAverage >= EXCELLENT_SCORE_THRESHOLD -> QualityTier.EXCELLENT
            recentAverage >= GOOD_SCORE_THRESHOLD -> QualityTier.GOOD
            else -> QualityTier.NEEDS_WORK
        }

        return AggregateQualityAnalysis(
            totalShots = shots.size,
            overallQualityScore = recentAverage, // Use recent as headline
            qualityTier = qualityTier,
            excellentCount = excellentCount,
            goodCount = goodCount,
            needsWorkCount = needsWorkCount,
            trendDirection = trendDirection,
            recentAverage = recentAverage,
            overallAverage = overallAverage,
            improvementRate = improvementRate,
            consistencyScore = consistencyScore
        )
    }

    /**
     * Calculate extraction time points based on shot timing.
     */
    private fun calculateExtractionTimePoints(shot: Shot): Int {
        return when {
            shot.isOptimalExtractionTime() -> MAX_EXTRACTION_TIME_POINTS
            shot.extractionTimeSeconds in MIN_ACCEPTABLE_EXTRACTION..MAX_ACCEPTABLE_EXTRACTION ->
                GOOD_EXTRACTION_TIME_POINTS
            else -> MIN_EXTRACTION_TIME_POINTS
        }
    }

    /**
     * Calculate brew ratio points based on shot ratio.
     */
    private fun calculateBrewRatioPoints(shot: Shot): Int {
        return when {
            shot.isTypicalBrewRatio() -> MAX_BREW_RATIO_POINTS
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
        if (abs(brewRatioDeviation) < TIGHT_RATIO_DEVIATION) bonus += DEVIATION_BONUS_POINTS
        if (abs(extractionTimeDeviation) < TIGHT_TIME_DEVIATION) bonus += DEVIATION_BONUS_POINTS
        return bonus
    }

    /**
     * Calculate standard deviation for a list of values.
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }
}

/**
 * Aggregate quality analysis for multiple shots.
 * Provides dashboard-level insights.
 */
data class AggregateQualityAnalysis(
    val totalShots: Int,
    val overallQualityScore: Int, // 0-100, weighted average
    val qualityTier: QualityTier,
    val excellentCount: Int, // >= 85
    val goodCount: Int, // 60-84
    val needsWorkCount: Int, // < 60
    val trendDirection: TrendDirection,
    val recentAverage: Int, // Last 5 shots average
    val overallAverage: Int, // All shots average
    val improvementRate: Double, // Percentage change recent vs overall
    val consistencyScore: Int // 0-100, based on std deviation
) {
    companion object {
        fun empty() = AggregateQualityAnalysis(
            totalShots = 0,
            overallQualityScore = 0,
            qualityTier = QualityTier.NEEDS_WORK,
            excellentCount = 0,
            goodCount = 0,
            needsWorkCount = 0,
            trendDirection = TrendDirection.STABLE,
            recentAverage = 0,
            overallAverage = 0,
            improvementRate = 0.0,
            consistencyScore = 0
        )
    }
}

/**
 * Trend direction for quality analysis.
 */
enum class TrendDirection {
    IMPROVING, // recentAvg > overallAvg + 5
    STABLE, // within ±5 points
    DECLINING // recentAvg < overallAvg - 5
}
