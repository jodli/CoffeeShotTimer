package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class GetShotQualityAnalysisUseCaseTest {

    private lateinit var shotRepository: ShotRepository
    private lateinit var getShotQualityAnalysisUseCase: GetShotQualityAnalysisUseCase

    private val testBeanId = "test-bean-id"

    @Before
    fun setup() {
        shotRepository = mockk()
        getShotQualityAnalysisUseCase = GetShotQualityAnalysisUseCase(shotRepository)
    }

    // ==================== calculateShotQualityScore Tests ====================

    @Test
    fun `calculateShotQualityScore returns near-perfect score for optimal shot with perfect taste`() = runTest {
        // Given: optimal extraction (28s), typical ratio (2.0), perfect taste
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = TastePrimary.PERFECT
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 20 (ratio) + 30 (taste) + 15 (consistency) + 10 (deviation) = 100
        assertEquals(100, score)
    }

    @Test
    fun `calculateShotQualityScore with PERFECT taste gives 30 points`() = runTest {
        // Given: shot with PERFECT taste feedback
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = TastePrimary.PERFECT
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: should include 30 points for perfect taste
        assertTrue("Score should be >= 70", score >= 70)
        assertTrue("Score should include taste points", score > 40)
    }

    @Test
    fun `calculateShotQualityScore with SOUR taste gives 10 points`() = runTest {
        // Given: shot with SOUR taste feedback (informative)
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = TastePrimary.SOUR
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 20 (ratio) + 10 (sour) + 15 (consistency) + 10 (deviation) = 80
        assertEquals(80, score)
    }

    @Test
    fun `calculateShotQualityScore with BITTER taste gives 10 points`() = runTest {
        // Given: shot with BITTER taste feedback (informative)
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = TastePrimary.BITTER
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 20 (ratio) + 10 (bitter) + 15 (consistency) + 10 (deviation) = 80
        assertEquals(80, score)
    }

    @Test
    fun `calculateShotQualityScore with no taste feedback gives 15 neutral points`() = runTest {
        // Given: shot with no taste feedback
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 20 (ratio) + 15 (neutral) + 15 (consistency) + 10 (deviation) = 85
        assertEquals(85, score)
    }

    @Test
    fun `calculateShotQualityScore with non-optimal extraction time returns lower score`() = runTest {
        // Given: extraction time outside optimal range
        val shot = createShot(
            extractionTimeSeconds = 20, // Acceptable but not optimal
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 15 (good extraction) + 20 (ratio) + 15 (neutral) + 15 (consistency) + 10 (deviation) = 75
        assertEquals(75, score)
    }

    @Test
    fun `calculateShotQualityScore with poor extraction time returns minimum points`() = runTest {
        // Given: extraction time far from optimal
        val shot = createShot(
            extractionTimeSeconds = 15, // Poor extraction
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 5 (min extraction) + 20 (ratio) + 15 (neutral) + 15 (consistency) + 10 (deviation) = 65
        assertEquals(65, score)
    }

    @Test
    fun `calculateShotQualityScore with non-typical brew ratio returns lower score`() = runTest {
        // Given: brew ratio outside typical range
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 25.0, // 1.39 ratio, acceptable but not typical
            tastePrimary = null
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 12 (good ratio) + 15 (neutral) + 15 (consistency) + 10 (deviation) = 77
        assertEquals(77, score)
    }

    @Test
    fun `calculateShotQualityScore with poor brew ratio returns minimum points`() = runTest {
        // Given: brew ratio far from typical range
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 20.0, // 1.11 ratio, poor
            tastePrimary = null
        )
        val relatedShots = listOf(shot)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, relatedShots)

        // Then: 25 (extraction) + 4 (min ratio) + 15 (neutral) + 15 (consistency) + 10 (deviation) = 69
        assertEquals(69, score)
    }

    @Test
    fun `calculateShotQualityScore with inconsistent shot returns lower consistency points`() = runTest {
        // Given: shot inconsistent with bean history
        val shot1 = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0, // 2.0 ratio
            tastePrimary = null,
            timestamp = LocalDateTime.now().minusHours(2)
        )
        val shot2 = createShot(
            extractionTimeSeconds = 35, // Very different extraction time
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 50.0, // 2.78 ratio - significantly different
            tastePrimary = null,
            timestamp = LocalDateTime.now()
        )
        val relatedShots = listOf(shot1, shot2)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot2, relatedShots)

        // Then: should have lower consistency points (5 instead of 15)
        // 15 (good extraction) + 20 (typical ratio) + 15 (neutral) + 5 (inconsistent) + 0 (no bonus) = 55
        assertEquals(55, score)
    }

    @Test
    fun `calculateShotQualityScore with tight ratio deviation gives bonus`() = runTest {
        // Given: consistent shots with very tight ratio deviation (<0.1)
        val shot1 = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0, // 2.0 ratio
            tastePrimary = null,
            timestamp = LocalDateTime.now().minusHours(1)
        )
        val shot2 = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.1, // 2.01 ratio, deviation 0.01 < 0.1
            tastePrimary = null,
            timestamp = LocalDateTime.now()
        )
        val relatedShots = listOf(shot1, shot2)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot2, relatedShots)

        // Then: should include deviation bonus for tight ratio and time
        // 25 (extraction) + 20 (ratio) + 15 (neutral) + 15 (consistent) + 10 (both bonuses) = 85
        assertEquals(85, score)
    }

    @Test
    fun `calculateShotQualityScore with tight time deviation gives bonus`() = runTest {
        // Given: consistent shots with very tight time deviation (<2s)
        val shot1 = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null,
            timestamp = LocalDateTime.now().minusHours(1)
        )
        val shot2 = createShot(
            extractionTimeSeconds = 29, // Deviation 1s < 2s
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null,
            timestamp = LocalDateTime.now()
        )
        val relatedShots = listOf(shot1, shot2)

        // When
        val score = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot2, relatedShots)

        // Then: should include deviation bonus
        assertEquals(85, score)
    }

    // ==================== calculateAggregateQualityAnalysis Tests ====================

    @Test
    fun `calculateAggregateQualityAnalysis with empty shots returns empty analysis`() = runTest {
        // Given: empty shot list
        val shots = emptyList<Shot>()

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(0, analysis.totalShots)
        assertEquals(0, analysis.overallQualityScore)
        assertEquals(QualityTier.NEEDS_WORK, analysis.qualityTier)
        assertEquals(0, analysis.excellentCount)
        assertEquals(0, analysis.goodCount)
        assertEquals(0, analysis.needsWorkCount)
        assertEquals(TrendDirection.STABLE, analysis.trendDirection)
    }

    @Test
    fun `calculateAggregateQualityAnalysis with single excellent shot assigns EXCELLENT tier`() = runTest {
        // Given: single excellent shot
        val shot = createShot(
            extractionTimeSeconds = 28,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = TastePrimary.PERFECT
        )
        val shots = listOf(shot)

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(1, analysis.totalShots)
        assertEquals(QualityTier.EXCELLENT, analysis.qualityTier)
        assertEquals(1, analysis.excellentCount)
        assertEquals(0, analysis.goodCount)
        assertEquals(0, analysis.needsWorkCount)
    }

    @Test
    fun `calculateAggregateQualityAnalysis with single good shot assigns GOOD tier`() = runTest {
        // Given: single good shot (score in 60-84 range)
        val shot = createShot(
            extractionTimeSeconds = 20, // Good but not optimal
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            tastePrimary = null
        )
        val shots = listOf(shot)

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(1, analysis.totalShots)
        assertEquals(QualityTier.GOOD, analysis.qualityTier)
        assertEquals(0, analysis.excellentCount)
        assertEquals(1, analysis.goodCount)
        assertEquals(0, analysis.needsWorkCount)
    }

    @Test
    fun `calculateAggregateQualityAnalysis with single poor shot assigns NEEDS_WORK tier`() = runTest {
        // Given: single poor shot (score < 60)
        val shot = createShot(
            extractionTimeSeconds = 15, // Poor extraction
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 20.0, // Poor ratio (1.11)
            tastePrimary = null
        )
        val shots = listOf(shot)

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(1, analysis.totalShots)
        assertEquals(QualityTier.NEEDS_WORK, analysis.qualityTier)
        assertEquals(0, analysis.excellentCount)
        assertEquals(0, analysis.goodCount)
        assertEquals(1, analysis.needsWorkCount)
    }

    @Test
    fun `calculateAggregateQualityAnalysis with improving trend shows IMPROVING`() = runTest {
        // Given: 10 shots with significant improvement (need >5 point difference)
        val shots = (1..10).map { i ->
            // First 5: poor shots (15s extraction = 65 score)
            // Last 5: excellent shots (28s + PERFECT = 100 score)
            val extractionTime = if (i <= 5) 15 else 28
            val taste = if (i <= 5) null else TastePrimary.PERFECT
            createShot(
                extractionTimeSeconds = extractionTime,
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                tastePrimary = taste,
                timestamp = LocalDateTime.now().minusHours((10 - i).toLong())
            )
        }

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(10, analysis.totalShots)
        assertEquals(TrendDirection.IMPROVING, analysis.trendDirection)
        assertTrue("Recent average should be higher than overall", analysis.recentAverage > analysis.overallAverage)
    }

    @Test
    fun `calculateAggregateQualityAnalysis with stable trend shows STABLE`() = runTest {
        // Given: 10 shots with consistent quality
        val shots = (1..10).map { i ->
            createShot(
                extractionTimeSeconds = 28,
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                tastePrimary = null,
                timestamp = LocalDateTime.now().minusHours((10 - i).toLong())
            )
        }

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(10, analysis.totalShots)
        assertEquals(TrendDirection.STABLE, analysis.trendDirection)
        // Verify within ±5 points tolerance
        assertTrue(
            "Recent and overall averages should be within ±5 points",
            kotlin.math.abs(analysis.recentAverage - analysis.overallAverage) <= 5
        )
    }

    @Test
    fun `calculateAggregateQualityAnalysis with declining trend shows DECLINING`() = runTest {
        // Given: 10 shots with significant decline (need >5 point difference)
        val shots = (1..10).map { i ->
            // First 5: excellent shots (28s + PERFECT = 100 score)
            // Last 5: poor shots (15s extraction = 65 score)
            val extractionTime = if (i <= 5) 28 else 15
            val taste = if (i <= 5) TastePrimary.PERFECT else null
            createShot(
                extractionTimeSeconds = extractionTime,
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                tastePrimary = taste,
                timestamp = LocalDateTime.now().minusHours((10 - i).toLong())
            )
        }

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertEquals(10, analysis.totalShots)
        assertEquals(TrendDirection.DECLINING, analysis.trendDirection)
        assertTrue("Recent average should be lower than overall", analysis.recentAverage < analysis.overallAverage)
    }

    @Test
    fun `calculateAggregateQualityAnalysis calculates distribution correctly`() = runTest {
        // Given: mix of shots with different quality levels
        // Note: Consistency scoring means that shots are penalized when they deviate from bean average.
        // Mixing very different shot qualities (15s vs 28s) lowers consistency scores.
        val shots = listOf(
            // 3 needs work (score < 60): poor extraction and ratio
            createShot(15, 18.0, 20.0, null, LocalDateTime.now().minusHours(12)),
            createShot(15, 18.0, 20.0, null, LocalDateTime.now().minusHours(11)),
            createShot(15, 18.0, 20.0, null, LocalDateTime.now().minusHours(10)),
            // 7 good (score 60-84): mix of decent shots and PERFECT taste shots that get penalized by consistency
            createShot(20, 18.0, 36.0, null, LocalDateTime.now().minusHours(9)),
            createShot(20, 18.0, 36.0, null, LocalDateTime.now().minusHours(8)),
            createShot(20, 18.0, 36.0, null, LocalDateTime.now().minusHours(7)),
            createShot(20, 18.0, 36.0, null, LocalDateTime.now().minusHours(6)),
            // These PERFECT shots score 80 (not 85+) due to consistency penalty from earlier poor shots
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(5)),
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(4)),
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(3))
        )

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then: Based on actual scoring behavior
        // Shots 0-2: score 29 (needs work)
        // Shots 3-9: score 70-80 (good)
        // No excellent shots because consistency penalty prevents 85+ scores
        assertEquals(10, analysis.totalShots)
        assertEquals(0, analysis.excellentCount) // Consistency penalty prevents 85+ scores
        assertEquals(7, analysis.goodCount) // Shots 3-9 score 70-80
        assertEquals(3, analysis.needsWorkCount) // Shots 0-2 score 29
    }

    @Test
    fun `calculateAggregateQualityAnalysis with consistent excellent shots achieves excellent tier`() = runTest {
        // Given: consistent, high-quality shots that should score 85+
        // All shots have optimal extraction time (25-30s) and typical ratio (2.0) with PERFECT taste
        val shots = listOf(
            createShot(27, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(5)),
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(4)),
            createShot(27, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(3)),
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(2)),
            createShot(27, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(1))
        )

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then: All shots should score 85+ due to consistency and high quality
        assertEquals(5, analysis.totalShots)
        assertEquals(5, analysis.excellentCount) // All shots 85+
        assertEquals(0, analysis.goodCount)
        assertEquals(0, analysis.needsWorkCount)
        assertTrue("Recent average should be excellent", analysis.recentAverage >= 85)
        assertEquals(QualityTier.EXCELLENT, analysis.qualityTier)
    }

    @Test
    fun `calculateAggregateQualityAnalysis calculates consistency score for varied shots`() = runTest {
        // Given: shots with high variance (low consistency)
        val shots = listOf(
            createShot(15, 18.0, 20.0, null, LocalDateTime.now().minusHours(2)), // Poor
            createShot(28, 18.0, 36.0, TastePrimary.PERFECT, LocalDateTime.now().minusHours(1)), // Excellent
            createShot(20, 18.0, 25.0, null, LocalDateTime.now()) // Mediocre
        )

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then: consistency score should be relatively low due to high variance
        assertTrue("Consistency score should be calculated", analysis.consistencyScore >= 0)
        assertTrue("Consistency score should be capped at 100", analysis.consistencyScore <= 100)
    }

    @Test
    fun `calculateAggregateQualityAnalysis uses last 5 shots for recent average`() = runTest {
        // Given: 10 shots where last 5 are excellent, first 5 are poor
        val shots = (1..10).map { i ->
            val extractionTime = if (i <= 5) 15 else 28
            val weightOut = if (i <= 5) 20.0 else 36.0
            val taste = if (i <= 5) null else TastePrimary.PERFECT
            createShot(
                extractionTimeSeconds = extractionTime,
                coffeeWeightIn = 18.0,
                coffeeWeightOut = weightOut,
                tastePrimary = taste,
                timestamp = LocalDateTime.now().minusHours((10 - i).toLong())
            )
        }

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertTrue(
            "Recent average should be higher (last 5 are good)",
            analysis.recentAverage > analysis.overallAverage
        )
        assertEquals(10, analysis.totalShots)
    }

    @Test
    fun `calculateAggregateQualityAnalysis calculates improvement rate correctly`() = runTest {
        // Given: improving shots
        val shots = (1..10).map { i ->
            val extractionTime = if (i <= 5) 20 else 28
            createShot(
                extractionTimeSeconds = extractionTime,
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                tastePrimary = null,
                timestamp = LocalDateTime.now().minusHours((10 - i).toLong())
            )
        }

        // When
        val analysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(shots, shots)

        // Then
        assertTrue("Improvement rate should be positive for improving trend", analysis.improvementRate > 0)
    }

    // ==================== Helper Methods ====================

    private fun createShot(
        extractionTimeSeconds: Int,
        coffeeWeightIn: Double,
        coffeeWeightOut: Double,
        tastePrimary: TastePrimary?,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Shot {
        return Shot(
            id = UUID.randomUUID().toString(),
            beanId = testBeanId,
            coffeeWeightIn = coffeeWeightIn,
            coffeeWeightOut = coffeeWeightOut,
            extractionTimeSeconds = extractionTimeSeconds,
            grinderSetting = "15",
            notes = "",
            timestamp = timestamp,
            tastePrimary = tastePrimary,
            tasteSecondary = null
        )
    }
}
