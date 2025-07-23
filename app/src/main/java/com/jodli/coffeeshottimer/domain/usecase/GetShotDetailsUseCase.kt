package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
                    shotResult.exceptionOrNull() ?: Exception("Failed to get shot")
                )
            }

            val shot = shotResult.getOrNull()
                ?: return Result.failure(Exception("Shot not found"))

            // Get the associated bean
            val beanResult = beanRepository.getBeanById(shot.beanId)
            if (beanResult.isFailure) {
                return Result.failure(
                    beanResult.exceptionOrNull() ?: Exception("Failed to get bean")
                )
            }

            val bean = beanResult.getOrNull()
                ?: return Result.failure(Exception("Associated bean not found"))

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

            val shotDetails = ShotDetails(
                shot = shot,
                bean = bean,
                daysSinceRoast = daysSinceRoast,
                previousShot = previousShot,
                nextShot = nextShot,
                analysis = analysis,
                relatedShotsCount = relatedShots.size
            )

            Result.success(shotDetails)
        } catch (exception: Exception) {
            Result.failure(exception)
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
                    shot1Result.exceptionOrNull() ?: Exception("Failed to get first shot")
                )
            }
            if (shot2Result.isFailure) {
                return Result.failure(
                    shot2Result.exceptionOrNull() ?: Exception("Failed to get second shot")
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
            Result.failure(exception)
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
                    lastShotResult.exceptionOrNull() ?: Exception("Failed to get last shot")
                )
            }

            val lastShot = lastShotResult.getOrNull() ?: return Result.success(null)

            getShotDetails(lastShot.id)
        } catch (exception: Exception) {
            Result.failure(exception)
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
        } else 0.0

        val avgExtractionTime = if (beanShots.isNotEmpty()) {
            beanShots.map { it.extractionTimeSeconds }.average()
        } else 0.0

        val avgWeightIn = if (beanShots.isNotEmpty()) {
            beanShots.map { it.coffeeWeightIn }.average()
        } else 0.0

        val avgWeightOut = if (beanShots.isNotEmpty()) {
            beanShots.map { it.coffeeWeightOut }.average()
        } else 0.0

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

        // Calculate quality score (0-100)
        var qualityScore = 50 // Base score

        if (isOptimalExtraction) qualityScore += 20
        if (isTypicalRatio) qualityScore += 15
        if (isConsistentWithHistory) qualityScore += 10

        // Adjust based on deviations
        if (kotlin.math.abs(brewRatioDeviation) < 0.1) qualityScore += 5
        if (kotlin.math.abs(extractionTimeDeviation) < 2) qualityScore += 5

        qualityScore = qualityScore.coerceIn(0, 100)

        // Generate recommendations
        val recommendations = mutableListOf<String>()

        if (!isOptimalExtraction) {
            if (shot.extractionTimeSeconds < 25) {
                recommendations.add("Consider using a finer grind to slow extraction")
            } else {
                recommendations.add("Consider using a coarser grind to speed extraction")
            }
        }

        if (!isTypicalRatio) {
            if (shot.brewRatio < 1.5) {
                recommendations.add("Consider extracting more liquid for a higher yield")
            } else if (shot.brewRatio > 3.0) {
                recommendations.add("Consider stopping extraction earlier for a lower yield")
            }
        }

        if (kotlin.math.abs(brewRatioDeviation) > 0.5) {
            recommendations.add("This shot's ratio differs significantly from your average for this bean")
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
            recommendations = recommendations
        )
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
    val relatedShotsCount: Int
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
    val recommendations: List<String>
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
                    "%.1f",
                    weightInDifference
                )
            }g",
            "weightOut" to "${if (weightOutDifference >= 0) "+" else ""}${
                String.format(
                    "%.1f",
                    weightOutDifference
                )
            }g",
            "extractionTime" to "${if (extractionTimeDifference >= 0) "+" else ""}${extractionTimeDifference}s",
            "brewRatio" to "${if (brewRatioDifference >= 0) "+" else ""}${
                String.format(
                    "%.2f",
                    brewRatioDifference
                )
            }"
        )
    }
}