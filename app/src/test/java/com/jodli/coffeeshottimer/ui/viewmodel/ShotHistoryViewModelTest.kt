package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.util.MemoryOptimizer
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotStatisticsUseCase
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.domain.usecase.OverallStatistics
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryViewModelTest {

    private lateinit var viewModel: ShotHistoryViewModel
    private val getShotHistoryUseCase = mockk<GetShotHistoryUseCase>()
    private val getActiveBeansUseCase = mockk<GetActiveBeansUseCase>()
    private val getShotStatisticsUseCase = mockk<GetShotStatisticsUseCase>()
    private val memoryOptimizer = mockk<MemoryOptimizer>(relaxed = true)
    
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
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(OverallStatistics.empty())
        coEvery { getShotStatisticsUseCase.getShotTrends(any(), any()) } returns Result.success(ShotTrends.empty())
        coEvery { getShotStatisticsUseCase.getBrewRatioAnalysis(any()) } returns Result.success(BrewRatioAnalysis.empty())
        coEvery { getShotStatisticsUseCase.getExtractionTimeAnalysis(any()) } returns Result.success(ExtractionTimeAnalysis.empty())
        coEvery { getShotStatisticsUseCase.getGrinderSettingAnalysis(any()) } returns Result.success(
            com.jodli.coffeeshottimer.domain.usecase.GrinderSettingAnalysis.empty()
        )
        
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, memoryOptimizer)
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
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, memoryOptimizer)
        
        // Wait for initial data load
        testDispatcher.scheduler.advanceUntilIdle()
        
        val beanName = viewModel.getBeanName("bean1")
        assertEquals("Ethiopian Yirgacheffe", beanName)
    }

    @Test
    fun `getBeanName returns unknown for non-existing bean`() {
        val beanName = viewModel.getBeanName("non-existing-id")
        assertEquals("Unknown Bean", beanName)
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
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showAnalysis)
        assertFalse(uiState.analysisLoading)
        assertTrue(uiState.error?.contains("Failed to load analysis") == true)
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Refresh analysis
        viewModel.refreshAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertEquals(mockStats, uiState.overallStatistics)
    }

    @Test
    fun `refreshAnalysis does nothing when analysis is not shown`() = runTest {
        // Don't show analysis
        assertFalse(viewModel.uiState.value.showAnalysis)
        
        // Refresh analysis (should do nothing)
        viewModel.refreshAnalysis()
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.hasAnalysisData)
    }

    @Test
    fun `hasAnalysisData returns false when no analysis data is present`() {
        assertFalse(viewModel.uiState.value.hasAnalysisData)
    }}
