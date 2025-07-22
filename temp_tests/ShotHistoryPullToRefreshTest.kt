package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.PaginatedResult
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.util.MemoryOptimizer
import com.jodli.coffeeshottimer.domain.usecase.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryPullToRefreshTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ShotHistoryViewModel
    private val getShotHistoryUseCase: GetShotHistoryUseCase = mockk()
    private val getActiveBeansUseCase: GetActiveBeansUseCase = mockk()
    private val getShotStatisticsUseCase: GetShotStatisticsUseCase = mockk()
    private val memoryOptimizer: MemoryOptimizer = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock default returns
        every { getActiveBeansUseCase.execute() } returns flowOf(Result.success(emptyList()))
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )
        coEvery { getShotStatisticsUseCase.getOverallStatistics() } returns Result.success(OverallStatistics.empty())
        coEvery { getShotStatisticsUseCase.getShotTrends(any(), any()) } returns Result.success(ShotTrends.empty())
        coEvery { getShotStatisticsUseCase.getBrewRatioAnalysis(any()) } returns Result.success(BrewRatioAnalysis.empty())
        coEvery { getShotStatisticsUseCase.getExtractionTimeAnalysis(any()) } returns Result.success(ExtractionTimeAnalysis.empty())
        coEvery { getShotStatisticsUseCase.getGrinderSettingAnalysis(any()) } returns Result.success(GrinderSettingAnalysis.empty())

        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, memoryOptimizer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshDataPullToRefresh sets isRefreshing state correctly`() = runTest {
        // Given - initial state
        assertFalse(viewModel.uiState.value.isRefreshing)

        // When - trigger pull to refresh
        viewModel.refreshDataPullToRefresh()

        // Then - should have called loadInitialData and cleared refresh state
        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify { getShotHistoryUseCase.getShotsPaginated(any()) }
    }

    @Test
    fun `refreshDataPullToRefresh also refreshes analysis when shown`() = runTest {
        // Given - analysis view is shown
        viewModel.toggleAnalysisView()
        testDispatcher.scheduler.advanceTimeBy(500)

        // When - trigger pull to refresh
        viewModel.refreshDataPullToRefresh()

        // Then - should have refreshed analysis data
        coVerify { getShotStatisticsUseCase.getOverallStatistics() }
    }

    @Test
    fun `refreshDataPullToRefresh clears error state`() = runTest {
        // Given - there's an error state
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.failure(Exception("Test error"))
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase, getShotStatisticsUseCase, memoryOptimizer)
        testDispatcher.scheduler.advanceTimeBy(500)

        // Verify error exists
        assertNotNull(viewModel.uiState.value.error)

        // Reset mock to succeed
        coEvery { getShotHistoryUseCase.getShotsPaginated(any()) } returns Result.success(
            PaginatedResult(emptyList(), 0, 0, 20, false, false)
        )

        // When - trigger pull to refresh
        viewModel.refreshDataPullToRefresh()

        // Then - error should be cleared
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `isEmpty property considers isRefreshing state`() = runTest {
        // Given - empty shots but refreshing
        val uiState = ShotHistoryUiState(
            isRefreshing = true,
            shots = emptyList(),
            isLoading = false
        )

        // Then - should not be empty while refreshing
        assertFalse(uiState.isEmpty)

        // Given - empty shots and not refreshing
        val uiStateNotRefreshing = uiState.copy(isRefreshing = false)

        // Then - should be empty
        assertTrue(uiStateNotRefreshing.isEmpty)
    }
}
