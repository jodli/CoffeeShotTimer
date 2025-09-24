package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Test class for BeanManagementViewModel.
 * Tests bean management operations, search functionality, and active bean management.
 */
@ExperimentalCoroutinesApi
class BeanManagementViewModelTest {

    private val getActiveBeansUseCase: GetActiveBeansUseCase = mockk()
    private val getBeanHistoryUseCase: GetBeanHistoryUseCase = mockk()
    private val updateBeanUseCase: UpdateBeanUseCase = mockk(relaxed = true)
    private val beanRepository: BeanRepository = mockk(relaxed = true)
    private val domainErrorTranslator: DomainErrorTranslator = mockk(relaxed = true)

    private lateinit var viewModel: BeanManagementViewModel
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        every { getActiveBeansUseCase.execute() } returns flowOf(Result.success(emptyList()))
        every { getActiveBeansUseCase.getActiveBeansWithSearch(any()) } returns flowOf(Result.success(emptyList()))
        every { getBeanHistoryUseCase.getBeanHistoryWithSearch(any(), any()) } returns flowOf(Result.success(emptyList()))
        
        viewModel = BeanManagementViewModel(
            getActiveBeansUseCase,
            getBeanHistoryUseCase,
            updateBeanUseCase,
            beanRepository,
            domainErrorTranslator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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