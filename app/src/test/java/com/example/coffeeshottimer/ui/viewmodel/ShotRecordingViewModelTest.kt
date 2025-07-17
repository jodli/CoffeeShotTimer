package com.example.coffeeshottimer.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.coffeeshottimer.data.repository.BeanRepository
import com.example.coffeeshottimer.data.repository.ShotRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertNotNull

/**
 * Test class for ShotRecordingViewModel to verify Hilt injection works correctly.
 */
@ExperimentalCoroutinesApi
class ShotRecordingViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var shotRepository: ShotRepository
    private lateinit var beanRepository: BeanRepository
    private lateinit var viewModel: ShotRecordingViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mock repositories
        shotRepository = mockk(relaxed = true)
        beanRepository = mockk(relaxed = true)
        
        // Create ViewModel with injected dependencies
        viewModel = ShotRecordingViewModel(shotRepository, beanRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `viewModel should be created with injected dependencies`() {
        // Verify that the ViewModel was created successfully with dependencies
        assertNotNull(viewModel)
        assertNotNull(viewModel.activeBeans)
        assertNotNull(viewModel.isLoading)
        assertNotNull(viewModel.errorMessage)
    }
}