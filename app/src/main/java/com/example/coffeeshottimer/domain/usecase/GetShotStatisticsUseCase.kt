package com.example.coffeeshottimer.domain.usecase

import com.example.coffeeshottimer.data.dao.ShotStatistics
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.data.repository.ShotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for calculating and retrieving shot statistics and analysis.
 * Provides comprehensive analytics for shot performance, trends, and insights.
 */
@Singleton
class GetShotStatisticsUseCase @Inject constructor(
    private val shotRepository: ShotRepository
) {
    
    /**
     * Get basic shot statistics for a specific bean.
     * @param beanId The ID of the bean
     * @return Result containing shot statistics or error
     */
    suspend fun getBeanStatistics(beanId: String): Result<ShotStatistics?> {
        return shotRepository.getShotStatistics(beanId)
    }
    
    /**
     * Get comprehensive shot analytics for a specific bean.
     * @param beanId The ID of the bean
     * @return Result containing detailed analytics
     */
    suspend fun getBeanAnalytics(beanId: String): Result<BeanAnalytics> {
        return try {
            val result = shotRepository.getShotsByBean(beanId).first()
            
            result.fold(
                onSuccess = { shots ->
                    if (shots.isEmpty()) {
                        Result.success(BeanAnalytics.empty())
                    } else {
                        val analytics = calculateBeanAnalytics(shots)
                        Result.success(analytics)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Get overall shot statistics across all beans.
     * @return Result containing overall statistics
     */
    suspend fun getOverallStatistics(): Result<OverallStatistics> {
        return try {
            val result = shotRepository.getAllShots().first()
            
            result.fold(
                onSuccess = { shots ->
                    if (shots.isEmpty()) {
                        Result.success(OverallStatistics.empty())
                    } else {
                        val statistics = calculateOverallStatistics(shots)
                        Result.success(statistics)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Get shot trends over time.
     * @param beanId Optional bean ID to filter by
     * @param days Number of days to analyze (default 30)
     * @return Result containing trend analysis
     */
    suspend fun getShotTrends(beanId: String? = null, days: Int = 30): Result<ShotTrends> {
        return try {
            val endDate = LocalDateTime.now()
            val startDate = endDate.minusDays(days.toLong())
            
            val shotsFlow = if (beanId != null) {
                shotRepository.getFilteredShots(beanId, startDate, endDate)
            } else {
                shotRepository.getShotsByDateRange(startDate, endDate)
            }
            
            val result = shotsFlow.first()
            
            result.fold(
                onSuccess = { shots ->
                    val trends = calculateShotTrends(shots, days)
                    Result.success(trends)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Get grinder setting analysis for a specific bean.
     * @param beanId The ID of the bean
     * @return Result containing grinder setting analysis
     */
    suspend fun getGrinderSettingAnalysis(beanId: String): Result<GrinderSettingAnalysis> {
        return try {
            val result = shotRepository.getShotsByBean(beanId).first()
            
            result.fold(
                onSuccess = { shots ->
                    val analysis = calculateGrinderSettingAnalysis(shots)
                    Result.success(analysis)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Get brew ratio distribution analysis.
     * @param beanId Optional bean ID to filter by
     * @return Result containing brew ratio analysis
     */
    suspend fun getBrewRatioAnalysis(beanId: String? = null): Result<BrewRatioAnalysis> {
        return try {
            val shotsFlow = if (beanId != null) {
                shotRepository.getShotsByBean(beanId)
            } else {
                shotRepository.getAllShots()
            }
            
            val result = shotsFlow.first()
            
            result.fold(
                onSuccess = { shots ->
                    val analysis = calculateBrewRatioAnalysis(shots)
                    Result.success(analysis)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Get extraction time analysis.
     * @param beanId Optional bean ID to filter by
     * @return Result containing extraction time analysis
     */
    suspend fun getExtractionTimeAnalysis(beanId: String? = null): Result<ExtractionTimeAnalysis> {
        return try {
            val shotsFlow = if (beanId != null) {
                shotRepository.getShotsByBean(beanId)
            } else {
                shotRepository.getAllShots()
            }
            
            val result = shotsFlow.first()
            
            result.fold(
                onSuccess = { shots ->
                    val analysis = calculateExtractionTimeAnalysis(shots)
                    Result.success(analysis)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    /**
     * Calculate comprehensive bean analytics from shot data.
     */
    private fun calculateBeanAnalytics(shots: List<Shot>): BeanAnalytics {
        if (shots.isEmpty()) return BeanAnalytics.empty()
        
        val totalShots = shots.size
        val avgBrewRatio = shots.map { it.brewRatio }.average()
        val avgExtractionTime = shots.map { it.extractionTimeSeconds }.average()
        val avgWeightIn = shots.map { it.coffeeWeightIn }.average()
        val avgWeightOut = shots.map { it.coffeeWeightOut }.average()
        
        val optimalExtractionCount = shots.count { it.isOptimalExtractionTime() }
        val typicalRatioCount = shots.count { it.isTypicalBrewRatio() }
        
        val bestShot = shots.maxByOrNull { shot ->
            var score = 0
            if (shot.isOptimalExtractionTime()) score += 2
            if (shot.isTypicalBrewRatio()) score += 2
            score
        }
        
        val consistencyScore = calculateConsistencyScore(shots)
        val improvementTrend = calculateImprovementTrend(shots)
        
        return BeanAnalytics(
            totalShots = totalShots,
            avgBrewRatio = avgBrewRatio,
            avgExtractionTime = avgExtractionTime,
            avgWeightIn = avgWeightIn,
            avgWeightOut = avgWeightOut,
            optimalExtractionPercentage = (optimalExtractionCount.toDouble() / totalShots) * 100,
            typicalRatioPercentage = (typicalRatioCount.toDouble() / totalShots) * 100,
            bestShot = bestShot,
            consistencyScore = consistencyScore,
            improvementTrend = improvementTrend,
            lastShotDate = shots.maxByOrNull { it.timestamp }?.timestamp,
            firstShotDate = shots.minByOrNull { it.timestamp }?.timestamp
        )
    }
    
    /**
     * Calculate overall statistics across all shots.
     */
    private fun calculateOverallStatistics(shots: List<Shot>): OverallStatistics {
        if (shots.isEmpty()) return OverallStatistics.empty()
        
        val totalShots = shots.size
        val uniqueBeans = shots.map { it.beanId }.distinct().size
        val avgBrewRatio = shots.map { it.brewRatio }.average()
        val avgExtractionTime = shots.map { it.extractionTimeSeconds }.average()
        
        val optimalExtractionCount = shots.count { it.isOptimalExtractionTime() }
        val typicalRatioCount = shots.count { it.isTypicalBrewRatio() }
        
        val mostUsedGrinderSetting = shots.groupBy { it.grinderSetting }
            .maxByOrNull { it.value.size }?.key
        
        val recentShots = shots.sortedByDescending { it.timestamp }.take(7)
        val recentAvgRatio = if (recentShots.isNotEmpty()) {
            recentShots.map { it.brewRatio }.average()
        } else 0.0
        
        return OverallStatistics(
            totalShots = totalShots,
            uniqueBeans = uniqueBeans,
            avgBrewRatio = avgBrewRatio,
            avgExtractionTime = avgExtractionTime,
            optimalExtractionPercentage = (optimalExtractionCount.toDouble() / totalShots) * 100,
            typicalRatioPercentage = (typicalRatioCount.toDouble() / totalShots) * 100,
            mostUsedGrinderSetting = mostUsedGrinderSetting,
            recentAvgBrewRatio = recentAvgRatio,
            lastShotDate = shots.maxByOrNull { it.timestamp }?.timestamp,
            firstShotDate = shots.minByOrNull { it.timestamp }?.timestamp
        )
    }
    
    /**
     * Calculate shot trends over time.
     */
    private fun calculateShotTrends(shots: List<Shot>, days: Int): ShotTrends {
        if (shots.isEmpty()) return ShotTrends.empty().copy(daysAnalyzed = days)
        
        val sortedShots = shots.sortedBy { it.timestamp }
        val midPoint = sortedShots.size / 2
        
        val firstHalf = sortedShots.take(midPoint)
        val secondHalf = sortedShots.drop(midPoint)
        
        val firstHalfAvgRatio = if (firstHalf.isNotEmpty()) {
            firstHalf.map { it.brewRatio }.average()
        } else 0.0
        
        val secondHalfAvgRatio = if (secondHalf.isNotEmpty()) {
            secondHalf.map { it.brewRatio }.average()
        } else 0.0
        
        val firstHalfAvgTime = if (firstHalf.isNotEmpty()) {
            firstHalf.map { it.extractionTimeSeconds }.average()
        } else 0.0
        
        val secondHalfAvgTime = if (secondHalf.isNotEmpty()) {
            secondHalf.map { it.extractionTimeSeconds }.average()
        } else 0.0
        
        val brewRatioTrend = secondHalfAvgRatio - firstHalfAvgRatio
        val extractionTimeTrend = secondHalfAvgTime - firstHalfAvgTime
        
        val shotsPerDay = shots.size.toDouble() / days
        
        return ShotTrends(
            totalShots = shots.size,
            daysAnalyzed = days,
            shotsPerDay = shotsPerDay,
            brewRatioTrend = brewRatioTrend,
            extractionTimeTrend = extractionTimeTrend,
            firstHalfAvgRatio = firstHalfAvgRatio,
            secondHalfAvgRatio = secondHalfAvgRatio,
            firstHalfAvgTime = firstHalfAvgTime,
            secondHalfAvgTime = secondHalfAvgTime,
            isImproving = brewRatioTrend > -0.1 && kotlin.math.abs(extractionTimeTrend) < 2
        )
    }
    
    /**
     * Calculate grinder setting analysis.
     */
    private fun calculateGrinderSettingAnalysis(shots: List<Shot>): GrinderSettingAnalysis {
        if (shots.isEmpty()) return GrinderSettingAnalysis.empty()
        
        val settingGroups = shots.groupBy { it.grinderSetting }
        val settingStats = settingGroups.map { (setting, settingShots) ->
            GrinderSettingStats(
                setting = setting,
                shotCount = settingShots.size,
                avgBrewRatio = settingShots.map { it.brewRatio }.average(),
                avgExtractionTime = settingShots.map { it.extractionTimeSeconds }.average(),
                optimalExtractionPercentage = (settingShots.count { it.isOptimalExtractionTime() }.toDouble() / settingShots.size) * 100
            )
        }.sortedByDescending { it.shotCount }
        
        val mostUsedSetting = settingStats.firstOrNull()
        val bestPerformingSetting = settingStats.maxByOrNull { it.optimalExtractionPercentage }
        
        return GrinderSettingAnalysis(
            totalSettings = settingStats.size,
            settingStats = settingStats,
            mostUsedSetting = mostUsedSetting,
            bestPerformingSetting = bestPerformingSetting
        )
    }
    
    /**
     * Calculate brew ratio analysis.
     */
    private fun calculateBrewRatioAnalysis(shots: List<Shot>): BrewRatioAnalysis {
        if (shots.isEmpty()) return BrewRatioAnalysis.empty()
        
        val ratios = shots.map { it.brewRatio }
        val avgRatio = ratios.average()
        val minRatio = ratios.minOrNull() ?: 0.0
        val maxRatio = ratios.maxOrNull() ?: 0.0
        val medianRatio = ratios.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            } else {
                sorted[sorted.size / 2]
            }
        }
        
        val typicalRatioCount = shots.count { it.isTypicalBrewRatio() }
        val underExtractedCount = shots.count { it.brewRatio < 1.5 }
        val overExtractedCount = shots.count { it.brewRatio > 3.0 }
        
        // Create ratio distribution buckets
        val buckets = mapOf(
            "Under 1.5" to underExtractedCount,
            "1.5-2.0" to shots.count { it.brewRatio in 1.5..2.0 },
            "2.0-2.5" to shots.count { it.brewRatio in 2.0..2.5 },
            "2.5-3.0" to shots.count { it.brewRatio in 2.5..3.0 },
            "Over 3.0" to overExtractedCount
        )
        
        return BrewRatioAnalysis(
            totalShots = shots.size,
            avgRatio = avgRatio,
            minRatio = minRatio,
            maxRatio = maxRatio,
            medianRatio = medianRatio,
            typicalRatioPercentage = (typicalRatioCount.toDouble() / shots.size) * 100,
            underExtractedPercentage = (underExtractedCount.toDouble() / shots.size) * 100,
            overExtractedPercentage = (overExtractedCount.toDouble() / shots.size) * 100,
            distribution = buckets
        )
    }
    
    /**
     * Calculate extraction time analysis.
     */
    private fun calculateExtractionTimeAnalysis(shots: List<Shot>): ExtractionTimeAnalysis {
        if (shots.isEmpty()) return ExtractionTimeAnalysis.empty()
        
        val times = shots.map { it.extractionTimeSeconds }
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0
        val medianTime = times.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
        }
        
        val optimalCount = shots.count { it.isOptimalExtractionTime() }
        val tooFastCount = shots.count { it.extractionTimeSeconds < 25 }
        val tooSlowCount = shots.count { it.extractionTimeSeconds > 30 }
        
        // Create time distribution buckets
        val buckets = mapOf(
            "Under 20s" to shots.count { it.extractionTimeSeconds < 20 },
            "20-25s" to shots.count { it.extractionTimeSeconds in 20..24 },
            "25-30s" to optimalCount,
            "30-35s" to shots.count { it.extractionTimeSeconds in 30..35 },
            "Over 35s" to shots.count { it.extractionTimeSeconds > 35 }
        )
        
        return ExtractionTimeAnalysis(
            totalShots = shots.size,
            avgTime = avgTime,
            minTime = minTime,
            maxTime = maxTime,
            medianTime = medianTime,
            optimalTimePercentage = (optimalCount.toDouble() / shots.size) * 100,
            tooFastPercentage = (tooFastCount.toDouble() / shots.size) * 100,
            tooSlowPercentage = (tooSlowCount.toDouble() / shots.size) * 100,
            distribution = buckets
        )
    }
    
    /**
     * Calculate consistency score based on standard deviation of key metrics.
     */
    private fun calculateConsistencyScore(shots: List<Shot>): Double {
        if (shots.size < 2) return 100.0
        
        val ratios = shots.map { it.brewRatio }
        val times = shots.map { it.extractionTimeSeconds.toDouble() }
        
        val ratioStdDev = calculateStandardDeviation(ratios)
        val timeStdDev = calculateStandardDeviation(times)
        
        // Lower standard deviation = higher consistency score
        val ratioScore = (1.0 - (ratioStdDev / 2.0).coerceAtMost(1.0)) * 50
        val timeScore = (1.0 - (timeStdDev / 10.0).coerceAtMost(1.0)) * 50
        
        return (ratioScore + timeScore).coerceIn(0.0, 100.0)
    }
    
    /**
     * Calculate improvement trend based on recent vs older shots.
     */
    private fun calculateImprovementTrend(shots: List<Shot>): Double {
        if (shots.size < 4) return 0.0
        
        val sortedShots = shots.sortedBy { it.timestamp }
        val quarterSize = sortedShots.size / 4
        
        val oldShots = sortedShots.take(quarterSize)
        val recentShots = sortedShots.takeLast(quarterSize)
        
        val oldOptimalPercentage = (oldShots.count { it.isOptimalExtractionTime() }.toDouble() / oldShots.size) * 100
        val recentOptimalPercentage = (recentShots.count { it.isOptimalExtractionTime() }.toDouble() / recentShots.size) * 100
        
        return recentOptimalPercentage - oldOptimalPercentage
    }
    
    /**
     * Calculate standard deviation for a list of values.
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

// Data classes for statistics and analysis results
data class BeanAnalytics(
    val totalShots: Int,
    val avgBrewRatio: Double,
    val avgExtractionTime: Double,
    val avgWeightIn: Double,
    val avgWeightOut: Double,
    val optimalExtractionPercentage: Double,
    val typicalRatioPercentage: Double,
    val bestShot: Shot?,
    val consistencyScore: Double,
    val improvementTrend: Double,
    val lastShotDate: LocalDateTime?,
    val firstShotDate: LocalDateTime?
) {
    companion object {
        fun empty() = BeanAnalytics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, 0.0, 0.0, null, null)
    }
}

data class OverallStatistics(
    val totalShots: Int,
    val uniqueBeans: Int,
    val avgBrewRatio: Double,
    val avgExtractionTime: Double,
    val optimalExtractionPercentage: Double,
    val typicalRatioPercentage: Double,
    val mostUsedGrinderSetting: String?,
    val recentAvgBrewRatio: Double,
    val lastShotDate: LocalDateTime?,
    val firstShotDate: LocalDateTime?
) {
    companion object {
        fun empty() = OverallStatistics(0, 0, 0.0, 0.0, 0.0, 0.0, null, 0.0, null, null)
    }
}

data class ShotTrends(
    val totalShots: Int,
    val daysAnalyzed: Int,
    val shotsPerDay: Double,
    val brewRatioTrend: Double,
    val extractionTimeTrend: Double,
    val firstHalfAvgRatio: Double,
    val secondHalfAvgRatio: Double,
    val firstHalfAvgTime: Double,
    val secondHalfAvgTime: Double,
    val isImproving: Boolean
) {
    companion object {
        fun empty() = ShotTrends(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false)
    }
}

data class GrinderSettingAnalysis(
    val totalSettings: Int,
    val settingStats: List<GrinderSettingStats>,
    val mostUsedSetting: GrinderSettingStats?,
    val bestPerformingSetting: GrinderSettingStats?
) {
    companion object {
        fun empty() = GrinderSettingAnalysis(0, emptyList(), null, null)
    }
}

data class GrinderSettingStats(
    val setting: String,
    val shotCount: Int,
    val avgBrewRatio: Double,
    val avgExtractionTime: Double,
    val optimalExtractionPercentage: Double
)

data class BrewRatioAnalysis(
    val totalShots: Int,
    val avgRatio: Double,
    val minRatio: Double,
    val maxRatio: Double,
    val medianRatio: Double,
    val typicalRatioPercentage: Double,
    val underExtractedPercentage: Double,
    val overExtractedPercentage: Double,
    val distribution: Map<String, Int>
) {
    companion object {
        fun empty() = BrewRatioAnalysis(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyMap())
    }
}

data class ExtractionTimeAnalysis(
    val totalShots: Int,
    val avgTime: Double,
    val minTime: Int,
    val maxTime: Int,
    val medianTime: Double,
    val optimalTimePercentage: Double,
    val tooFastPercentage: Double,
    val tooSlowPercentage: Double,
    val distribution: Map<String, Int>
) {
    companion object {
        fun empty() = ExtractionTimeAnalysis(0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, emptyMap())
    }
}