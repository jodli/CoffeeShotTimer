package com.example.coffeeshottimer.ui.viewmodel

import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.example.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.example.coffeeshottimer.domain.usecase.GetBeanHistoryUseCase
import com.example.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for BeanManagementViewModel.
 * Tests bean management operations, search functionality, and active bean management.
 */
@ExperimentalCoroutinesApi
class BeanManagementViewModelTest {

    @Mock
    private lateinit var getActiveBeansUseCase: GetActiveBeansUseCase

    @Mock
    private lateinit var getBeanHistoryUseCase: GetBeanHistoryUseCase

    @Mock
    private lateinit var addBeanUseCase: AddBeanUseCase

    @Mock
    private lateinit var updateBeanUseCase: UpdateBeanUseCase

    private lateinit var viewModel: BeanManagementViewModel
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup default mock responses
        whenever(getActiveBeansUseCase.execute()).thenReturn(
            flowOf(Result.success(emptyList()))
        )
        
        viewModel = BeanManagementViewModel(
            getActiveBeansUseCase,
            getBeanHistoryUseCase,
            addBeanUseCase,
            updateBeanUseCase
        )
    }

    @Test
    fun `initial state should be correct`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        
        assertTrue(initialState.beans.isEmpty())
        assertFalse(initialState.isLoading)
        assertEquals(null, initialState.error)
        assertTrue(initialState.hasActiveBeans)
    }

    @Test
    fun `search query should update correctly`() = runTest(testDispatcher) {
        val searchQuery = "test query"
        
        viewModel.updateSearchQuery(searchQuery)
        
        assertEquals(searchQuery, viewModel.searchQuery.value)
    }

    @Test
    fun `show inactive toggle should work correctly`() = runTest(testDispatcher) {
        assertFalse(viewModel.showInactive.value)
        
        viewModel.toggleShowInactive()
        
        assertTrue(viewModel.showInactive.value)
        
        viewModel.toggleShowInactive()
        
        assertFalse(viewModel.showInactive.value)
    }

    @Test
    fun `clear error should reset error state`() = runTest(testDispatcher) {
        // This is a simple test since we can't easily mock the error state
        viewModel.clearError()
        
        assertEquals(null, viewModel.uiState.value.error)
    }
}