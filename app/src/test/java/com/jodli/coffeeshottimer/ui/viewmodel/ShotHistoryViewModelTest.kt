package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.util.MemoryOptimizer
import com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotStatisticsUseCase
import com.jodli.coffeeshottimer.domain.usecase.OverallStatistics
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryViewModelTest {

    private lateinit var viewModel: ShotHistoryViewModel
    private val getShotHistoryUseCase = mockk<GetShotHistoryUseCase>()
    private val getActiveBeansUseCase = mockk<GetActiveBeansUseCase>()
    private val getShotStatisticsUseCase = mockk<GetShotStatisticsUseCase>()
    private val getShotQualityAnalysisUseCase =
        mockk<com.jodli.coffeeshottimer.domain.usecase.GetShotQualityAnalysisUseCase>(relaxed = true)
    private val memoryOptimizer = mockk<MemoryOptimizer>(relaxed = true)
    private val stringResourceProvider = mockk<StringResourceProvider>(relaxed = true)
    private val domainErrorTranslator = mockk<DomainErrorTranslator>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Setup default mock responses
        every { getActiveBeansUseCase.execute() } returns flowOf(Result.success(emptyList()))
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )
        coEvery { getShotHistoryUseCase.getFilteredShotsPaginated(any(), any(), any(), any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )
        // Mock reactive flow methods for Phase 2 implementation
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(emptyList()))
        every { getShotHistoryUseCase.getFilteredShots(any()) } returns flowOf(Result.success(emptyList()))
        // Mock reactive flow methods
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(emptyList()))
        every { getShotHistoryUseCase.getFilteredShots(any()) } returns flowOf(Result.success(emptyList()))
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(OverallStatistics.empty())
        coEvery { getShotStatisticsUseCase.getShotTrends(any(), any()) } returns Result.success(ShotTrends.empty())
        coEvery { getShotStatisticsUseCase.getBrewRatioAnalysis(any()) } returns Result.success(BrewRatioAnalysis.empty())
        coEvery {
            getShotStatisticsUseCase.getExtractionTimeAnalysis(any())
        } returns Result.success(ExtractionTimeAnalysis.empty())
        coEvery { getShotStatisticsUseCase.getGrinderSettingAnalysis(any()) } returns Result.success(
            com.jodli.coffeeshottimer.domain.usecase.GrinderSettingAnalysis.empty()
        )

        // Setup string resource provider mocks
        every {
            stringResourceProvider.getString(com.jodli.coffeeshottimer.R.string.text_unknown_error)
        } returns "Unknown error occurred"
        every {
            stringResourceProvider.getString(com.jodli.coffeeshottimer.R.string.error_failed_to_load_analysis, any())
        } returns "Failed to load analysis: Unknown error occurred"
        every {
            stringResourceProvider.getString(com.jodli.coffeeshottimer.R.string.text_inactive_bean)
        } returns "Inactive Bean"

        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, getShotQualityAnalysisUseCase, memoryOptimizer, stringResourceProvider, domainErrorTranslator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val uiState = viewModel.uiState.value

        assertFalse(uiState.isLoading)
        assertTrue(uiState.shots.isEmpty())
        assertTrue(uiState.availableBeans.isEmpty())
        assertEquals(null, uiState.error)
    }

    @Test
    fun `filter has no filters initially`() {
        val filter = viewModel.currentFilter.value

        assertFalse(filter.hasFilters())
    }

    @Test
    fun `applyFilter updates current filter`() = runTest {
        val testFilter = ShotHistoryFilter(
            beanId = "test-bean-id",
            minBrewRatio = 1.5,
            maxBrewRatio = 3.0
        )

        every { getShotHistoryUseCase.getFilteredShots(testFilter) } returns
            flowOf(Result.success(emptyList()))

        viewModel.applyFilter(testFilter)

        val currentFilter = viewModel.currentFilter.value
        assertEquals("test-bean-id", currentFilter.beanId)
        assertEquals(1.5, currentFilter.minBrewRatio)
        assertEquals(3.0, currentFilter.maxBrewRatio)
        assertTrue(currentFilter.hasFilters())
    }

    @Test
    fun `clearFilters resets filter to empty`() = runTest {
        // First apply a filter
        val testFilter = ShotHistoryFilter(beanId = "test-bean-id")
        every { getShotHistoryUseCase.getFilteredShots(testFilter) } returns
            flowOf(Result.success(emptyList()))
        viewModel.applyFilter(testFilter)

        // Then clear filters
        viewModel.clearFilters()

        val currentFilter = viewModel.currentFilter.value
        assertFalse(currentFilter.hasFilters())
        assertEquals(null, currentFilter.beanId)
    }

    @Test
    fun `setBeanFilter updates bean filter`() = runTest {
        val beanId = "test-bean-id"

        viewModel.setBeanFilter(beanId)

        val currentFilter = viewModel.currentFilter.value
        assertEquals(beanId, currentFilter.beanId)
        assertTrue(currentFilter.hasFilters())
    }

    @Test
    fun `setDateRangeFilter updates date range`() = runTest {
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)

        viewModel.setDateRangeFilter(startDate, endDate)

        val currentFilter = viewModel.currentFilter.value
        assertEquals(startDate.atStartOfDay(), currentFilter.startDate)
        assertEquals(endDate.atTime(23, 59, 59, 999999999), currentFilter.endDate)
        assertTrue(currentFilter.hasFilters())
    }

    @Test
    fun `getBeanName returns correct name for existing bean`() = runTest {
        val testBeans = listOf(
            Bean(id = "bean1", name = "Ethiopian Yirgacheffe", roastDate = LocalDate.now().minusDays(7)),
            Bean(id = "bean2", name = "Colombian Supremo", roastDate = LocalDate.now().minusDays(5))
        )

        every { getActiveBeansUseCase.execute() } returns flowOf(Result.success(testBeans))

        // Recreate viewModel to pick up the new mock
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, getShotQualityAnalysisUseCase, memoryOptimizer, stringResourceProvider, domainErrorTranslator)

        // Wait for initial data load
        testDispatcher.scheduler.advanceTimeBy(500)

        val beanName = viewModel.getBeanName("bean1")
        assertEquals("Ethiopian Yirgacheffe", beanName)
    }

    @Test
    fun `getBeanName returns unknown for non-existing bean`() {
        val beanName = viewModel.getBeanName("non-existing-id")
        assertEquals("Inactive Bean", beanName)
    }

    @Test
    fun `toggleOptimalExtractionTimeFilter toggles correctly`() = runTest {
        // Initially false
        assertFalse(viewModel.currentFilter.value.onlyOptimalExtractionTime == true)

        // Toggle to true
        viewModel.toggleOptimalExtractionTimeFilter()
        assertTrue(viewModel.currentFilter.value.onlyOptimalExtractionTime == true)

        // Toggle back to null/false
        viewModel.toggleOptimalExtractionTimeFilter()
        assertFalse(viewModel.currentFilter.value.onlyOptimalExtractionTime == true)
    }

    @Test
    fun `toggleTypicalBrewRatioFilter toggles correctly`() = runTest {
        // Initially false
        assertFalse(viewModel.currentFilter.value.onlyTypicalBrewRatio == true)

        // Toggle to true
        viewModel.toggleTypicalBrewRatioFilter()
        assertTrue(viewModel.currentFilter.value.onlyTypicalBrewRatio == true)

        // Toggle back to null/false
        viewModel.toggleTypicalBrewRatioFilter()
        assertFalse(viewModel.currentFilter.value.onlyTypicalBrewRatio == true)
    }

    @Test
    fun `toggleAnalysisView toggles analysis state correctly`() = runTest {
        // Initially analysis is not shown
        assertFalse(viewModel.uiState.value.showAnalysis)

        // Toggle to show analysis
        viewModel.toggleAnalysisView()
        assertTrue(viewModel.uiState.value.showAnalysis)

        // Toggle back to hide analysis
        viewModel.toggleAnalysisView()
        assertFalse(viewModel.uiState.value.showAnalysis)
    }

    @Test
    fun `toggleAnalysisView loads analysis data when first shown`() = runTest {
        // Setup mock data
        val mockOverallStats = OverallStatistics(
            totalShots = 10,
            uniqueBeans = 3,
            avgBrewRatio = 2.5,
            avgExtractionTime = 28.0,
            optimalExtractionPercentage = 70.0,
            typicalRatioPercentage = 80.0,
            mostUsedGrinderSetting = "15",
            recentAvgBrewRatio = 2.3,
            lastShotDate = LocalDateTime.now(),
            firstShotDate = LocalDateTime.now().minusDays(30)
        )

        val mockTrends = ShotTrends(
            totalShots = 10,
            daysAnalyzed = 30,
            shotsPerDay = 0.33,
            brewRatioTrend = 0.1,
            extractionTimeTrend = -1.0,
            firstHalfAvgRatio = 2.4,
            secondHalfAvgRatio = 2.5,
            firstHalfAvgTime = 29.0,
            secondHalfAvgTime = 28.0,
            isImproving = true
        )

        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(mockOverallStats)
        coEvery { getShotStatisticsUseCase.getShotTrends(days = 30) } returns Result.success(mockTrends)

        // Toggle analysis view
        viewModel.toggleAnalysisView()

        // Wait for async operations
        testDispatcher.scheduler.advanceTimeBy(500)

        val uiState = viewModel.uiState.value
        assertTrue(uiState.showAnalysis)
        assertEquals(mockOverallStats, uiState.overallStatistics)
        assertEquals(mockTrends, uiState.shotTrends)
        assertTrue(uiState.hasAnalysisData)
    }

    @Test
    fun `analysis loading state is handled correctly`() = runTest {
        // Initially not loading analysis
        assertFalse(viewModel.uiState.value.analysisLoading)

        // Mock a delayed response
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(OverallStatistics.empty())
        }

        // Toggle analysis view (this should start loading)
        viewModel.toggleAnalysisView()

        // Should be loading initially
        assertTrue(viewModel.uiState.value.analysisLoading)

        // Wait for completion
        testDispatcher.scheduler.advanceTimeBy(500)

        // Should not be loading anymore
        assertFalse(viewModel.uiState.value.analysisLoading)
    }

    @Test
    fun `analysis error is handled correctly`() = runTest {
        val errorMessage = "Failed to load statistics"

        // Mock the first method to throw an exception (not just return Result.failure)
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } throws Exception(errorMessage)

        // Toggle analysis view
        viewModel.toggleAnalysisView()

        // Wait for async operations
        testDispatcher.scheduler.advanceTimeBy(500)

        val uiState = viewModel.uiState.value
        assertTrue(uiState.showAnalysis)
        assertFalse(uiState.analysisLoading)
        assertEquals("Failed to load analysis: Unknown error occurred", uiState.error)
    }

    @Test
    fun `refreshAnalysis reloads data when analysis is shown`() = runTest {
        val mockStats = OverallStatistics(
            totalShots = 5,
            uniqueBeans = 2,
            avgBrewRatio = 2.0,
            avgExtractionTime = 25.0,
            optimalExtractionPercentage = 60.0,
            typicalRatioPercentage = 70.0,
            mostUsedGrinderSetting = "12",
            recentAvgBrewRatio = 2.1,
            lastShotDate = LocalDateTime.now(),
            firstShotDate = LocalDateTime.now().minusDays(15)
        )

        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(mockStats)

        // First show analysis
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Refresh analysis
        viewModel.refreshAnalysis()
        testDispatcher.scheduler.advanceTimeBy(500)

        val uiState = viewModel.uiState.value
        assertEquals(mockStats, uiState.overallStatistics)
    }

    @Test
    fun `refreshAnalysis does nothing when analysis is not shown`() = runTest {
        // Don't show analysis
        assertFalse(viewModel.uiState.value.showAnalysis)

        // Refresh analysis (should do nothing)
        viewModel.refreshAnalysis()
        testDispatcher.scheduler.advanceTimeBy(500)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.showAnalysis)
        assertEquals(null, uiState.overallStatistics)
        assertFalse(uiState.hasAnalysisData)
    }

    @Test
    fun `hasAnalysisData returns true when any analysis data is present`() = runTest {
        val mockStats = OverallStatistics.empty().copy(totalShots = 1)
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(mockStats)

        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        assertTrue(viewModel.uiState.value.hasAnalysisData)
    }

    @Test
    fun `hasAnalysisData returns false when no analysis data is present`() {
        assertFalse(viewModel.uiState.value.hasAnalysisData)
    }

    @Test
    fun `refreshData triggers reactive updates`() = runTest {
        // Given - initial state
        assertFalse(viewModel.uiState.value.isLoading)

        // When - trigger refresh
        viewModel.refreshData()

        // Then - should have called loadInitialData
        coVerify { getShotHistoryUseCase.getShotsPaginated(any()) }
    }

    @Test
    fun `refreshData also refreshes analysis when shown`() = runTest {
        // Given - analysis view is shown
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // When - trigger refresh
        viewModel.refreshData()

        // Then - should have refreshed analysis data
        coVerify { getShotStatisticsUseCase.getOverallStatistics() }
    }

    @Test
    fun `reactive updates clear error state`() = runTest {
        // Given - setup error for manual refresh
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.failure(Exception("Test error"))
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.failure(Exception("Test error")))
        every { getShotHistoryUseCase.getFilteredShots(any()) } returns flowOf(Result.failure(Exception("Test error")))

        // Set error state through manual refresh
        viewModel.refreshData()
        testDispatcher.scheduler.advanceTimeBy(1000)

        // Verify error exists
        assertNotNull("Error should be set by manual refresh", viewModel.uiState.value.error)

        // Now setup reactive flows to succeed
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(emptyList()))
        every { getShotHistoryUseCase.getFilteredShots(any()) } returns flowOf(Result.success(emptyList()))
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )

        // Trigger a filter change which restarts reactive collection
        viewModel.setBeanFilter("test-bean-id")
        testDispatcher.scheduler.advanceTimeBy(2000)

        // Then - error should be cleared by reactive updates
        assertNull("Error should be cleared by reactive updates", viewModel.uiState.value.error)

        // Then - error should be cleared by reactive updates
        assertNull("Error should be cleared by reactive updates", viewModel.uiState.value.error)
    }

    @Test
    fun `isEmpty property considers loading state`() = runTest {
        // Given - empty shots but loading
        val uiState = ShotHistoryUiState(
            isLoading = true,
            shots = emptyList()
        )

        // Then - should not be empty while loading
        assertFalse(uiState.isEmpty)

        // Given - empty shots and not loading
        val uiStateNotLoading = uiState.copy(isLoading = false)

        // Then - should be empty
        assertTrue(uiStateNotLoading.isEmpty)
    }

    // Epic 5 Phase 2: Aggregate Quality Analysis Tests

    @Test
    fun `loadAggregateQualityAnalysis_withShots_updatesState`() = runTest {
        // Given - shots are present
        val testShots = listOf(
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                timestamp = LocalDateTime.now(),
                grinderSetting = "15",
                tastePrimary = com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT
            )
        )

        val mockAggregateAnalysis = com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis(
            totalShots = 1,
            overallQualityScore = 85,
            qualityTier = com.jodli.coffeeshottimer.domain.usecase.QualityTier.EXCELLENT,
            excellentCount = 1,
            goodCount = 0,
            needsWorkCount = 0,
            trendDirection = com.jodli.coffeeshottimer.domain.usecase.TrendDirection.STABLE,
            recentAverage = 85,
            overallAverage = 85,
            improvementRate = 0.0,
            consistencyScore = 100
        )

        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(testShots, 1, 0, 20, false, true)
        )
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(testShots))
        every {
            getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(testShots, testShots)
        } returns mockAggregateAnalysis

        // When - load shots first, then toggle analysis view
        viewModel.refreshData()
        testDispatcher.scheduler.advanceTimeBy(500)

        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Then - aggregate quality analysis should be present
        val uiState = viewModel.uiState.value
        assertNotNull("Aggregate quality analysis should be present", uiState.aggregateQualityAnalysis)
        assertEquals(mockAggregateAnalysis, uiState.aggregateQualityAnalysis)
        assertEquals(1, uiState.aggregateQualityAnalysis?.totalShots)
        assertEquals(85, uiState.aggregateQualityAnalysis?.overallQualityScore)
    }

    @Test
    fun `loadAggregateQualityAnalysis_noShots_setsNull`() = runTest {
        // Given - no shots present
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )

        // When - toggle analysis view
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Then - aggregate quality analysis should be null
        val uiState = viewModel.uiState.value
        assertNull(uiState.aggregateQualityAnalysis)
    }

    @Test
    fun `loadAnalysisData_includesAggregateQuality`() = runTest {
        // Given - shots with quality analysis
        val testShots = listOf(
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                timestamp = LocalDateTime.now(),
                grinderSetting = "15",
                tastePrimary = null
            ),
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot2",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 38.0,
                extractionTimeSeconds = 26,
                timestamp = LocalDateTime.now().minusHours(1),
                grinderSetting = "15",
                tastePrimary = com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT
            )
        )

        val mockAggregateAnalysis = com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis(
            totalShots = 2,
            overallQualityScore = 75,
            qualityTier = com.jodli.coffeeshottimer.domain.usecase.QualityTier.GOOD,
            excellentCount = 0,
            goodCount = 2,
            needsWorkCount = 0,
            trendDirection = com.jodli.coffeeshottimer.domain.usecase.TrendDirection.IMPROVING,
            recentAverage = 75,
            overallAverage = 72,
            improvementRate = 4.2,
            consistencyScore = 88
        )

        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(testShots, 2, 0, 20, false, true)
        )
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(testShots))
        every {
            getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(testShots, testShots)
        } returns mockAggregateAnalysis

        // When - load shots then toggle analysis view
        viewModel.refreshData()
        testDispatcher.scheduler.advanceTimeBy(500)

        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Then - should include aggregate quality in state
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showAnalysis)
        assertNotNull(uiState.aggregateQualityAnalysis)
        assertEquals(2, uiState.aggregateQualityAnalysis?.totalShots)
        assertEquals(
            com.jodli.coffeeshottimer.domain.usecase.TrendDirection.IMPROVING,
            uiState.aggregateQualityAnalysis?.trendDirection
        )
    }

    @Test
    fun `toggleAnalysisView_preservesAggregateQuality`() = runTest {
        // Given - analysis view with quality data loaded
        val testShots = listOf(
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                timestamp = LocalDateTime.now(),
                grinderSetting = "15",
                tastePrimary = com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT
            )
        )

        val mockAggregateAnalysis = com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis(
            totalShots = 1,
            overallQualityScore = 90,
            qualityTier = com.jodli.coffeeshottimer.domain.usecase.QualityTier.EXCELLENT,
            excellentCount = 1,
            goodCount = 0,
            needsWorkCount = 0,
            trendDirection = com.jodli.coffeeshottimer.domain.usecase.TrendDirection.STABLE,
            recentAverage = 90,
            overallAverage = 90,
            improvementRate = 0.0,
            consistencyScore = 100
        )

        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(testShots, 1, 0, 20, false, true)
        )
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(testShots))
        every {
            getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(testShots, testShots)
        } returns mockAggregateAnalysis

        // Load shots first
        viewModel.refreshData()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Load analysis data first time
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        val firstLoadState = viewModel.uiState.value
        assertNotNull(firstLoadState.aggregateQualityAnalysis)
        assertEquals(90, firstLoadState.aggregateQualityAnalysis?.overallQualityScore)

        // When - toggle away and back
        viewModel.toggleAnalysisView() // Hide
        testDispatcher.scheduler.advanceTimeBy(500)

        viewModel.toggleAnalysisView() // Show again
        testDispatcher.scheduler.advanceTimeBy(500)

        // Then - aggregate quality should still be present (cached)
        val finalState = viewModel.uiState.value
        assertTrue(finalState.showAnalysis)
        assertNotNull(finalState.aggregateQualityAnalysis)
    }

    @Test
    fun `applyFilter_refreshesAggregateQuality_whenAnalysisShown`() = runTest {
        // Given - analysis view is shown
        val initialShots = listOf(
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                timestamp = LocalDateTime.now(),
                grinderSetting = "15",
                tastePrimary = null
            ),
            com.jodli.coffeeshottimer.data.model.Shot(
                id = "shot2",
                beanId = "bean2",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 40.0,
                extractionTimeSeconds = 30,
                timestamp = LocalDateTime.now().minusHours(1),
                grinderSetting = "16",
                tastePrimary = null
            )
        )

        val filteredShots = listOf(initialShots[0]) // Only bean1

        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(initialShots, 2, 0, 20, false, true)
        )
        coEvery {
            getShotHistoryUseCase.getFilteredShotsPaginated("bean1", any(), any(), any())
        } returns Result.success(
            com.jodli.coffeeshottimer.data.model.PaginatedResult(filteredShots, 1, 0, 20, false, true)
        )

        val mockInitialAnalysis = com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis(
            totalShots = 2,
            overallQualityScore = 70,
            qualityTier = com.jodli.coffeeshottimer.domain.usecase.QualityTier.GOOD,
            excellentCount = 0,
            goodCount = 2,
            needsWorkCount = 0,
            trendDirection = com.jodli.coffeeshottimer.domain.usecase.TrendDirection.STABLE,
            recentAverage = 70,
            overallAverage = 70,
            improvementRate = 0.0,
            consistencyScore = 85
        )

        val mockFilteredAnalysis = com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis(
            totalShots = 1,
            overallQualityScore = 80,
            qualityTier = com.jodli.coffeeshottimer.domain.usecase.QualityTier.GOOD,
            excellentCount = 0,
            goodCount = 1,
            needsWorkCount = 0,
            trendDirection = com.jodli.coffeeshottimer.domain.usecase.TrendDirection.STABLE,
            recentAverage = 80,
            overallAverage = 80,
            improvementRate = 0.0,
            consistencyScore = 100
        )

        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(initialShots))
        every {
            getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(initialShots, initialShots)
        } returns mockInitialAnalysis
        every {
            getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(filteredShots, filteredShots)
        } returns mockFilteredAnalysis

        // Load shots first
        viewModel.refreshData()
        testDispatcher.scheduler.advanceTimeBy(500)

        // Show analysis first
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        assertEquals(2, viewModel.uiState.value.aggregateQualityAnalysis?.totalShots)

        // When - apply bean filter
        val testFilter = ShotHistoryFilter(beanId = "bean1")
        every { getShotHistoryUseCase.getFilteredShots(testFilter) } returns
            flowOf(Result.success(filteredShots))
        viewModel.applyFilter(testFilter)
        testDispatcher.scheduler.advanceTimeBy(500)

        // Then - aggregate quality should be updated for filtered shots
        val finalState = viewModel.uiState.value
        assertNotNull(finalState.aggregateQualityAnalysis)
        assertEquals(1, finalState.aggregateQualityAnalysis?.totalShots)
        assertEquals(80, finalState.aggregateQualityAnalysis?.overallQualityScore)
    }
}
