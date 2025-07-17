package com.example.coffeeshottimer.integration

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.data.model.ValidationResult
import com.example.coffeeshottimer.data.repository.BeanRepository
import com.example.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.example.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Integration test for shot recording functionality including draft auto-save.
 */
@ExperimentalCoroutinesApi
class ShotRecordingIntegrationTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var recordShotUseCase: RecordShotUseCase
    private lateinit var beanRepository: BeanRepository
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: ShotRecordingViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mock dependencies
        recordShotUseCase = mockk(relaxed = true)
        beanRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        // Mock SharedPreferences behavior
        every { context.getSharedPreferences("shot_drafts", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } just Runs
        every { sharedPreferences.getString("current_draft", null) } returns null
        
        // Mock bean repository to return test beans
        val testBean = Bean(
            id = "test-bean-1",
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(5),
            notes = "Test notes",
            isActive = true
        )
        every { beanRepository.getActiveBeans() } returns flowOf(Result.success(listOf(testBean)))
        
        // Mock use case validation and recording
        coEvery { recordShotUseCase.validateShotParameters(any(), any(), any(), any(), any(), any()) } returns 
            ValidationResult(isValid = true, errors = emptyList())
        
        val testShot = Shot(
            id = "test-shot-1",
            beanId = "test-bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = "Test shot"
        )
        coEvery { recordShotUseCase.recordShotWithCurrentTimer(any(), any(), any(), any(), any()) } returns 
            Result.success(testShot)
        
        // Create ViewModel
        viewModel = ShotRecordingViewModel(recordShotUseCase, beanRepository, context)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `should save draft to SharedPreferences when form has data`() = runTest {
        // Given: Form has some data
        viewModel.updateCoffeeWeightIn("18.0")
        viewModel.updateCoffeeWeightOut("36.0")
        viewModel.updateGrinderSetting("15")
        
        // When: Manual draft save is triggered
        viewModel.saveDraftManually()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: SharedPreferences should be called to save draft
        verify { editor.putString("current_draft", any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `should show success message after successful shot recording`() = runTest {
        // Given: Valid form data
        viewModel.updateCoffeeWeightIn("18.0")
        viewModel.updateCoffeeWeightOut("36.0")
        viewModel.updateGrinderSetting("15")
        
        // Mock bean selection
        val testBean = Bean(
            id = "test-bean-1",
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(5)
        )
        viewModel.selectBean(testBean)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: Shot is recorded
        viewModel.recordShot()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Success message should be set
        assertNotNull(viewModel.successMessage.value)
        assertTrue(viewModel.successMessage.value!!.contains("Shot recorded successfully"))
        assertTrue(viewModel.successMessage.value!!.contains("1:2.0")) // Brew ratio
    }
    
    @Test
    fun `should clear draft after successful shot recording`() = runTest {
        // Given: Form has data and draft exists
        viewModel.updateCoffeeWeightIn("18.0")
        viewModel.updateCoffeeWeightOut("36.0")
        viewModel.updateGrinderSetting("15")
        
        // Mock bean selection
        val testBean = Bean(
            id = "test-bean-1",
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(5)
        )
        viewModel.selectBean(testBean)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: Shot is recorded successfully
        viewModel.recordShot()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Draft should be cleared from SharedPreferences
        verify { editor.remove("current_draft") }
        verify { editor.apply() }
    }
    
    @Test
    fun `should handle validation errors gracefully`() = runTest {
        // Given: Invalid form data and validation failure
        coEvery { recordShotUseCase.validateShotParameters(any(), any(), any(), any(), any(), any()) } returns 
            ValidationResult(isValid = false, errors = listOf("Coffee weight is too low"))
        
        viewModel.updateCoffeeWeightIn("0.05") // Invalid weight
        viewModel.updateCoffeeWeightOut("36.0")
        viewModel.updateGrinderSetting("15")
        
        val testBean = Bean(id = "test-bean-1", name = "Test Bean", roastDate = LocalDate.now())
        viewModel.selectBean(testBean)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: Shot recording is attempted
        viewModel.recordShot()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Error message should be displayed
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Coffee weight is too low"))
        
        // And: Success message should not be set
        assertNull(viewModel.successMessage.value)
    }
}