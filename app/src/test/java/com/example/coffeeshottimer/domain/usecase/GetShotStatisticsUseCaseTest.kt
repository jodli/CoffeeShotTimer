package com.example.coffeeshottimer.domain.usecase

import com.example.coffeeshottimer.data.dao.ShotStatistics
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.data.repository.ShotRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class GetShotStatisticsUseCaseTest {
    
    private lateinit var shotRepository: ShotRepository
    private lateinit var getShotStatisticsUseCase: GetShotStatisticsUseCase
    
    private val testBeanId = "test-bean-id"
    
    private val testShot1 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 18.0,
        coffeeWeightOut = 36.0,
        extractionTimeSeconds = 28,
        grinderSetting = "15",
        notes = "Perfect shot",
        timestamp = LocalDateTime.now().minusHours(1)
    )
    
    private val testShot2 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 18.5,
        coffeeWeightOut = 37.0,
        extractionTimeSeconds = 30,
        grinderSetting = "15",
        notes = "Good extraction",
        timestamp = LocalDateTime.now().minusHours(2)
    )
    
    private val testShot3 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 17.5,
        coffeeWeightOut = 35.0,
        extractionTimeSeconds = 26,
        grinderSetting = "16",
        notes = "Fast extraction",
        timestamp = LocalDateTime.now().minusHours(3)
    )
    
    private val testShotStatistics = ShotStatistics(
        totalShots = 3,
        avgWeightIn = 18.0,
        avgWeightOut = 36.0,
        avgExtractionTime = 28.0,
        avgBrewRatio = 2.0
    )
    
    @Before
    fun setup() {
        shotRepository = mockk()
        getShotStatisticsUseCase = GetShotStatisticsUseCase(shotRepository)
    }
    
    @Test
    fun `getBeanStatistics returns statistics from repository`() = runTest {
        // Given
        coEvery { shotRepository.getShotStatistics(testBeanId) } returns Result.success(testShotStatistics)
        
        // When
        val result = getShotStatisticsUseCase.getBeanStatistics(testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testShotStatistics, result.getOrNull())
    }
    
    @Test
    fun `getBeanStatistics returns error when repository fails`() = runTest {
        // Given
        val exception = Exception("Database error")
        coEvery { shotRepository.getShotStatistics(testBeanId) } returns Result.failure(exception)
        
        // When
        val result = getShotStatisticsUseCase.getBeanStatistics(testBeanId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `getBeanAnalytics calculates comprehensive analytics`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2, testShot3)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getBeanAnalytics(testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analytics = result.getOrNull()!!
        
        assertEquals(3, analytics.totalShots)
        assertEquals(2.0, analytics.avgBrewRatio, 0.1) // Average of 2.0, 2.0, 2.0
        assertEquals(28.0, analytics.avgExtractionTime, 0.1) // Average of 28, 30, 26
        assertEquals(18.0, analytics.avgWeightIn, 0.1) // Average of 18.0, 18.5, 17.5
        assertEquals(36.0, analytics.avgWeightOut, 0.1) // Average of 36.0, 37.0, 35.0
        assertEquals(100.0, analytics.optimalExtractionPercentage, 0.1) // All shots are 25-30s
        assertEquals(100.0, analytics.typicalRatioPercentage, 0.1) // All shots are 1.5-3.0 ratio
        assertNotNull(analytics.bestShot)
        assertTrue(analytics.consistencyScore > 0)
        assertNotNull(analytics.lastShotDate)
        assertNotNull(analytics.firstShotDate)
    }
    
    @Test
    fun `getBeanAnalytics returns empty analytics for no shots`() = runTest {
        // Given
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(emptyList()))
        
        // When
        val result = getShotStatisticsUseCase.getBeanAnalytics(testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analytics = result.getOrNull()!!
        
        assertEquals(0, analytics.totalShots)
        assertEquals(0.0, analytics.avgBrewRatio, 0.01)
        assertEquals(0.0, analytics.avgExtractionTime, 0.01)
        assertNull(analytics.bestShot)
        assertNull(analytics.lastShotDate)
        assertNull(analytics.firstShotDate)
    }
    
    @Test
    fun `getBeanAnalytics handles repository error`() = runTest {
        // Given
        val exception = Exception("Repository error")
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.failure(exception))
        
        // When
        val result = getShotStatisticsUseCase.getBeanAnalytics(testBeanId)
        
        // Then
        assertTrue(result.isSuccess) // The use case catches the exception and returns empty analytics
        val analytics = result.getOrNull()!!
        assertEquals(0, analytics.totalShots) // Should return empty analytics when flow fails
    }
    
    @Test
    fun `getOverallStatistics calculates statistics across all shots`() = runTest {
        // Given
        val allShots = listOf(testShot1, testShot2, testShot3)
        every { shotRepository.getAllShots() } returns flowOf(Result.success(allShots))
        
        // When
        val result = getShotStatisticsUseCase.getOverallStatistics()
        
        // Then
        assertTrue(result.isSuccess)
        val statistics = result.getOrNull()!!
        
        assertEquals(3, statistics.totalShots)
        assertEquals(1, statistics.uniqueBeans) // All shots use same bean
        assertEquals(2.0, statistics.avgBrewRatio, 0.1)
        assertEquals(28.0, statistics.avgExtractionTime, 0.1)
        assertEquals(100.0, statistics.optimalExtractionPercentage, 0.1)
        assertEquals(100.0, statistics.typicalRatioPercentage, 0.1)
        assertNotNull(statistics.mostUsedGrinderSetting)
        assertTrue(statistics.recentAvgBrewRatio > 0)
        assertNotNull(statistics.lastShotDate)
        assertNotNull(statistics.firstShotDate)
    }
    
    @Test
    fun `getOverallStatistics returns empty statistics for no shots`() = runTest {
        // Given
        every { shotRepository.getAllShots() } returns flowOf(Result.success(emptyList()))
        
        // When
        val result = getShotStatisticsUseCase.getOverallStatistics()
        
        // Then
        assertTrue(result.isSuccess)
        val statistics = result.getOrNull()!!
        
        assertEquals(0, statistics.totalShots)
        assertEquals(0, statistics.uniqueBeans)
        assertEquals(0.0, statistics.avgBrewRatio, 0.01)
        assertEquals(0.0, statistics.avgExtractionTime, 0.01)
        assertNull(statistics.mostUsedGrinderSetting)
        assertNull(statistics.lastShotDate)
        assertNull(statistics.firstShotDate)
    }
    
    @Test
    fun `getShotTrends calculates trends over time`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2, testShot3)
        every { 
            shotRepository.getShotsByDateRange(any(), any()) 
        } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getShotTrends(days = 30)
        
        // Then
        assertTrue(result.isSuccess)
        val trends = result.getOrNull()!!
        
        assertEquals(3, trends.totalShots)
        assertEquals(30, trends.daysAnalyzed)
        assertEquals(0.1, trends.shotsPerDay, 0.01) // 3 shots / 30 days
        assertTrue(trends.firstHalfAvgRatio > 0)
        assertTrue(trends.secondHalfAvgRatio > 0)
        assertTrue(trends.firstHalfAvgTime > 0)
        assertTrue(trends.secondHalfAvgTime > 0)
    }
    
    @Test
    fun `getShotTrends with bean filter uses filtered shots`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2)
        every { 
            shotRepository.getFilteredShots(testBeanId, any(), any()) 
        } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getShotTrends(beanId = testBeanId, days = 7)
        
        // Then
        assertTrue(result.isSuccess)
        val trends = result.getOrNull()!!
        
        assertEquals(2, trends.totalShots)
        assertEquals(7, trends.daysAnalyzed)
    }
    
    @Test
    fun `getShotTrends returns empty trends for no shots`() = runTest {
        // Given
        every { 
            shotRepository.getShotsByDateRange(any(), any()) 
        } returns flowOf(Result.success(emptyList()))
        
        // When
        val result = getShotStatisticsUseCase.getShotTrends(days = 30)
        
        // Then
        assertTrue(result.isSuccess)
        val trends = result.getOrNull()!!
        
        assertEquals(0, trends.totalShots)
        assertEquals(30, trends.daysAnalyzed)
        assertEquals(0.0, trends.shotsPerDay, 0.01)
        assertFalse(trends.isImproving)
    }
    
    @Test
    fun `getGrinderSettingAnalysis calculates setting statistics`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2, testShot3) // Two "15" settings, one "16"
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getGrinderSettingAnalysis(testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(2, analysis.totalSettings) // "15" and "16"
        assertEquals(2, analysis.settingStats.size)
        assertNotNull(analysis.mostUsedSetting)
        assertEquals("15", analysis.mostUsedSetting?.setting) // Used twice
        assertNotNull(analysis.bestPerformingSetting)
    }
    
    @Test
    fun `getGrinderSettingAnalysis returns empty analysis for no shots`() = runTest {
        // Given
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(emptyList()))
        
        // When
        val result = getShotStatisticsUseCase.getGrinderSettingAnalysis(testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(0, analysis.totalSettings)
        assertTrue(analysis.settingStats.isEmpty())
        assertNull(analysis.mostUsedSetting)
        assertNull(analysis.bestPerformingSetting)
    }
    
    @Test
    fun `getBrewRatioAnalysis calculates ratio distribution`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2, testShot3) // All have 2.0 ratio
        every { shotRepository.getAllShots() } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getBrewRatioAnalysis()
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(3, analysis.totalShots)
        assertEquals(2.0, analysis.avgRatio, 0.01)
        assertEquals(2.0, analysis.minRatio, 0.01)
        assertEquals(2.0, analysis.maxRatio, 0.01)
        assertEquals(2.0, analysis.medianRatio, 0.01)
        assertEquals(100.0, analysis.typicalRatioPercentage, 0.01) // All are 1.5-3.0
        assertEquals(0.0, analysis.underExtractedPercentage, 0.01) // None under 1.5
        assertEquals(0.0, analysis.overExtractedPercentage, 0.01) // None over 3.0
        assertTrue(analysis.distribution.containsKey("2.0-2.5"))
        assertEquals(3, analysis.distribution["2.0-2.5"]) // All shots in this bucket
    }
    
    @Test
    fun `getBrewRatioAnalysis with bean filter uses bean shots`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getBrewRatioAnalysis(beanId = testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(2, analysis.totalShots)
    }
    
    @Test
    fun `getExtractionTimeAnalysis calculates time distribution`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2, testShot3) // 28s, 30s, 26s
        every { shotRepository.getAllShots() } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getExtractionTimeAnalysis()
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(3, analysis.totalShots)
        assertEquals(28.0, analysis.avgTime, 0.1) // (28+30+26)/3
        assertEquals(26, analysis.minTime)
        assertEquals(30, analysis.maxTime)
        assertEquals(28.0, analysis.medianTime, 0.1)
        assertEquals(100.0, analysis.optimalTimePercentage, 0.01) // All are 25-30s
        assertEquals(0.0, analysis.tooFastPercentage, 0.01) // None under 25s
        assertEquals(0.0, analysis.tooSlowPercentage, 0.01) // None over 30s
        assertTrue(analysis.distribution.containsKey("25-30s"))
        assertEquals(3, analysis.distribution["25-30s"]) // All shots in optimal range
    }
    
    @Test
    fun `getExtractionTimeAnalysis with bean filter uses bean shots`() = runTest {
        // Given
        val shots = listOf(testShot1, testShot2)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(shots))
        
        // When
        val result = getShotStatisticsUseCase.getExtractionTimeAnalysis(beanId = testBeanId)
        
        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        
        assertEquals(2, analysis.totalShots)
    }
    
    @Test
    fun `BeanAnalytics empty returns correct empty instance`() {
        // When
        val empty = BeanAnalytics.empty()
        
        // Then
        assertEquals(0, empty.totalShots)
        assertEquals(0.0, empty.avgBrewRatio, 0.01)
        assertEquals(0.0, empty.avgExtractionTime, 0.01)
        assertEquals(0.0, empty.avgWeightIn, 0.01)
        assertEquals(0.0, empty.avgWeightOut, 0.01)
        assertEquals(0.0, empty.optimalExtractionPercentage, 0.01)
        assertEquals(0.0, empty.typicalRatioPercentage, 0.01)
        assertNull(empty.bestShot)
        assertEquals(0.0, empty.consistencyScore, 0.01)
        assertEquals(0.0, empty.improvementTrend, 0.01)
        assertNull(empty.lastShotDate)
        assertNull(empty.firstShotDate)
    }
    
    @Test
    fun `OverallStatistics empty returns correct empty instance`() {
        // When
        val empty = OverallStatistics.empty()
        
        // Then
        assertEquals(0, empty.totalShots)
        assertEquals(0, empty.uniqueBeans)
        assertEquals(0.0, empty.avgBrewRatio, 0.01)
        assertEquals(0.0, empty.avgExtractionTime, 0.01)
        assertEquals(0.0, empty.optimalExtractionPercentage, 0.01)
        assertEquals(0.0, empty.typicalRatioPercentage, 0.01)
        assertNull(empty.mostUsedGrinderSetting)
        assertEquals(0.0, empty.recentAvgBrewRatio, 0.01)
        assertNull(empty.lastShotDate)
        assertNull(empty.firstShotDate)
    }
    
    @Test
    fun `ShotTrends empty returns correct empty instance`() {
        // When
        val empty = ShotTrends.empty()
        
        // Then
        assertEquals(0, empty.totalShots)
        assertEquals(0, empty.daysAnalyzed)
        assertEquals(0.0, empty.shotsPerDay, 0.01)
        assertEquals(0.0, empty.brewRatioTrend, 0.01)
        assertEquals(0.0, empty.extractionTimeTrend, 0.01)
        assertEquals(0.0, empty.firstHalfAvgRatio, 0.01)
        assertEquals(0.0, empty.secondHalfAvgRatio, 0.01)
        assertEquals(0.0, empty.firstHalfAvgTime, 0.01)
        assertEquals(0.0, empty.secondHalfAvgTime, 0.01)
        assertFalse(empty.isImproving)
    }
    
    @Test
    fun `GrinderSettingAnalysis empty returns correct empty instance`() {
        // When
        val empty = GrinderSettingAnalysis.empty()
        
        // Then
        assertEquals(0, empty.totalSettings)
        assertTrue(empty.settingStats.isEmpty())
        assertNull(empty.mostUsedSetting)
        assertNull(empty.bestPerformingSetting)
    }
    
    @Test
    fun `BrewRatioAnalysis empty returns correct empty instance`() {
        // When
        val empty = BrewRatioAnalysis.empty()
        
        // Then
        assertEquals(0, empty.totalShots)
        assertEquals(0.0, empty.avgRatio, 0.01)
        assertEquals(0.0, empty.minRatio, 0.01)
        assertEquals(0.0, empty.maxRatio, 0.01)
        assertEquals(0.0, empty.medianRatio, 0.01)
        assertEquals(0.0, empty.typicalRatioPercentage, 0.01)
        assertEquals(0.0, empty.underExtractedPercentage, 0.01)
        assertEquals(0.0, empty.overExtractedPercentage, 0.01)
        assertTrue(empty.distribution.isEmpty())
    }
    
    @Test
    fun `ExtractionTimeAnalysis empty returns correct empty instance`() {
        // When
        val empty = ExtractionTimeAnalysis.empty()
        
        // Then
        assertEquals(0, empty.totalShots)
        assertEquals(0.0, empty.avgTime, 0.01)
        assertEquals(0, empty.minTime)
        assertEquals(0, empty.maxTime)
        assertEquals(0.0, empty.medianTime, 0.01)
        assertEquals(0.0, empty.optimalTimePercentage, 0.01)
        assertEquals(0.0, empty.tooFastPercentage, 0.01)
        assertEquals(0.0, empty.tooSlowPercentage, 0.01)
        assertTrue(empty.distribution.isEmpty())
    }
}