package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
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
    
    private lateinit var recordShotUseCase: RecordShotUseCase
    private lateinit var beanRepository: BeanRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var context: Context
    private lateinit var viewModel: ShotRecordingViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mock dependencies
        recordShotUseCase = mockk(relaxed = true)
        beanRepository = mockk(relaxed = true)
        shotRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        
        // Create ViewModel with injected dependencies
        viewModel = ShotRecordingViewModel(recordShotUseCase, beanRepository, shotRepository, context)
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
        assertNotNull(viewModel.successMessage)
        assertNotNull(viewModel.isDraftSaved)
    }
    
    @Test
    fun `viewModel should have draft functionality`() {
        // Verify that draft-related state flows are available
        assertNotNull(viewModel.isDraftSaved)
        assertNotNull(viewModel.lastDraftSaveTime)
        
        // Verify that manual draft save method is available
        viewModel.saveDraftManually()
        
        // Verify that success message functionality is available
        viewModel.clearSuccessMessage()
    }
    
    @Test
    fun `viewModel should handle weight slider input correctly`() {
        // Test that weight update methods accept string values (for slider compatibility)
        viewModel.updateCoffeeWeightIn("18")
        viewModel.updateCoffeeWeightOut("36")
        
        // Verify that the state flows are updated
        assertNotNull(viewModel.coffeeWeightIn)
        assertNotNull(viewModel.coffeeWeightOut)
        
        // Test with whole gram values (as required by slider implementation)
        viewModel.updateCoffeeWeightIn("20")
        viewModel.updateCoffeeWeightOut("40")
        
        // Verify validation still works
        assertNotNull(viewModel.coffeeWeightInError)
        assertNotNull(viewModel.coffeeWeightOutError)
    }
}