package com.example.coffeeshottimer.ui.viewmodel

import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.example.coffeeshottimer.domain.usecase.GetShotHistoryUseCase
import com.example.coffeeshottimer.domain.usecase.ShotHistoryFilter
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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShotHistoryViewModelTest {

    private lateinit var viewModel: ShotHistoryViewModel
    private val getShotHistoryUseCase = mockk<GetShotHistoryUseCase>()
    private val getActiveBeansUseCase = mockk<GetActiveBeansUseCase>()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        every { getActiveBeansUseCase.execute() } returns flowOf(Result.success(emptyList()))
        every { getShotHistoryUseCase.getAllShots() } returns flowOf(Result.success(emptyList()))
        
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase)
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
        viewModel = ShotHistoryViewModel(getShotHistoryUseCase, getActiveBeansUseCase)
        
        // Wait for initial data load
        testScheduler.advanceUntilIdle()
        
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
}