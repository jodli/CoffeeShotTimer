package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EquipmentSetupViewModelTest {

    private lateinit var viewModel: EquipmentSetupViewModel
    private lateinit var mockRepository: GrinderConfigRepository
    private lateinit var mockErrorTranslator: DomainErrorTranslator
    private lateinit var mockOnboardingManager: OnboardingManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        mockErrorTranslator = mockk()
        mockOnboardingManager = mockk()
        
        // Setup common mock responses for error translator
        every { mockErrorTranslator.getString(any()) } returns "Mock error message"
        every { mockErrorTranslator.getString(any(), any()) } returns "Mock error message"
        
        viewModel = EquipmentSetupViewModel(mockRepository, mockErrorTranslator, mockOnboardingManager)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val initialState = viewModel.uiState.first()
        
        assertEquals("", initialState.scaleMin)
        assertEquals("", initialState.scaleMax)
        assertNull(initialState.minError)
        assertNull(initialState.maxError)
        assertNull(initialState.generalError)
        assertFalse(initialState.isFormValid)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
    }

    @Test
    fun `updateScaleMin updates state and clears errors`() = runTest {
        // Set initial error state
        viewModel.updateScaleMin("invalid")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        // Update with valid value
        viewModel.updateScaleMin("5")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("5", state.scaleMin)
        assertNull(state.minError)
        assertNull(state.generalError)
    }

    @Test
    fun `updateScaleMax updates state and clears errors`() = runTest {
        // Set initial error state
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("invalid")
        advanceUntilIdle()
        
        // Update with valid value
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("10", state.scaleMax)
        assertNull(state.maxError)
        assertNull(state.generalError)
    }

    @Test
    fun `setPreset updates both values and clears errors`() = runTest {
        viewModel.setPreset(1, 10)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("1", state.scaleMin)
        assertEquals("10", state.scaleMax)
        assertNull(state.minError)
        assertNull(state.maxError)
        assertNull(state.generalError)
        assertTrue(state.isFormValid)
    }

    @Test
    fun `validation shows error for invalid numbers`() = runTest {
        viewModel.updateScaleMin("abc")
        viewModel.updateScaleMax("xyz")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("Mock error message", state.minError)
        assertEquals("Mock error message", state.maxError)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `validation shows error for invalid range`() = runTest {
        viewModel.updateScaleMin("10")
        viewModel.updateScaleMax("5")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertNull(state.minError)
        assertNull(state.maxError)
        assertEquals("Minimum scale value must be less than maximum scale value", state.generalError)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `validation passes for valid range`() = runTest {
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertNull(state.minError)
        assertNull(state.maxError)
        assertNull(state.generalError)
        assertTrue(state.isFormValid)
    }

    @Test
    fun `saveConfiguration succeeds with valid data`() = runTest {
        val config = GrinderConfiguration(scaleMin = 1, scaleMax = 10)
        coEvery { mockRepository.saveConfig(any()) } returns Result.success(Unit)
        coEvery { mockOnboardingManager.markOnboardingComplete() } returns Unit
        
        var successCalled = false
        var errorCalled = false
        var savedConfig: GrinderConfiguration? = null
        
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        viewModel.saveConfiguration(
            onSuccess = { 
                successCalled = true
                savedConfig = it
            },
            onError = { errorCalled = true }
        )
        advanceUntilIdle()
        
        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertNotNull(savedConfig)
        assertEquals(1, savedConfig?.scaleMin)
        assertEquals(10, savedConfig?.scaleMax)
        
        coVerify { mockRepository.saveConfig(any()) }
        coVerify { mockOnboardingManager.markOnboardingComplete() }
    }

    @Test
    fun `saveConfiguration fails with invalid form`() = runTest {
        var successCalled = false
        var errorCalled = false
        var errorMessage = ""
        
        // Don't set any values, form should be invalid
        viewModel.saveConfiguration(
            onSuccess = { successCalled = true },
            onError = { 
                errorCalled = true
                errorMessage = it
            }
        )
        advanceUntilIdle()
        
        assertFalse(successCalled)
        assertTrue(errorCalled)
        assertEquals("Mock error message", errorMessage)
        
        coVerify(exactly = 0) { mockRepository.saveConfig(any()) }
    }

    @Test
    fun `saveConfiguration handles repository validation error`() = runTest {
        coEvery { mockRepository.saveConfig(any()) } returns Result.failure(
            RepositoryException.ValidationError("Configuration already exists")
        )
        
        var successCalled = false
        var errorCalled = false
        var errorMessage = ""
        
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        viewModel.saveConfiguration(
            onSuccess = { successCalled = true },
            onError = { 
                errorCalled = true
                errorMessage = it
            }
        )
        advanceUntilIdle()
        
        assertFalse(successCalled)
        assertTrue(errorCalled)
        assertEquals("Configuration already exists", errorMessage)
        
        val state = viewModel.uiState.first()
        assertEquals("Configuration already exists", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `saveConfiguration handles repository database error`() = runTest {
        coEvery { mockRepository.saveConfig(any()) } returns Result.failure(
            RepositoryException.DatabaseError("Database connection failed", Exception())
        )
        
        var successCalled = false
        var errorCalled = false
        var errorMessage = ""
        
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        viewModel.saveConfiguration(
            onSuccess = { successCalled = true },
            onError = { 
                errorCalled = true
                errorMessage = it
            }
        )
        advanceUntilIdle()
        
        assertFalse(successCalled)
        assertTrue(errorCalled)
        assertEquals("Mock error message", errorMessage)
    }

    @Test
    fun `skipSetup saves default configuration`() = runTest {
        // ViewModel calls repository.getOrCreateDefaultConfig()
        coEvery { mockRepository.getOrCreateDefaultConfig() } returns Result.success(GrinderConfiguration.DEFAULT_CONFIGURATION)
        coEvery { mockOnboardingManager.markOnboardingComplete() } returns Unit
        
        var successCalled = false
        var errorCalled = false
        var savedConfig: GrinderConfiguration? = null
        
        viewModel.skipSetup(
            onSuccess = { 
                successCalled = true
                savedConfig = it
            },
            onError = { errorCalled = true }
        )
        advanceUntilIdle()
        
        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertNotNull(savedConfig)
        assertEquals(GrinderConfiguration.DEFAULT_CONFIGURATION.scaleMin, savedConfig?.scaleMin)
        assertEquals(GrinderConfiguration.DEFAULT_CONFIGURATION.scaleMax, savedConfig?.scaleMax)
        
        coVerify { mockOnboardingManager.markOnboardingComplete() }
    }

    @Test
    fun `getValidationSuggestion returns appropriate suggestions`() {
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Minimum scale value must be less than maximum scale value")
        )
        
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Minimum scale value cannot be negative")
        )
        
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Maximum scale value cannot exceed 1000")
        )
        
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Scale range must have at least 3 steps")
        )
        
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Scale range cannot exceed 100 steps")
        )
        
        assertEquals(
            "Mock error message",
            viewModel.getValidationSuggestion("Please enter a valid number")
        )
        
        assertEquals("", viewModel.getValidationSuggestion("Unknown error"))
        assertEquals("", viewModel.getValidationSuggestion(null))
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Set error state
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        coEvery { mockRepository.saveConfig(any()) } returns Result.failure(
            RepositoryException.DatabaseError("Test error", Exception())
        )
        
        viewModel.saveConfiguration(
            onSuccess = { },
            onError = { }
        )
        advanceUntilIdle()
        
        // Verify error is set
        var state = viewModel.uiState.first()
        assertNotNull(state.error)
        
        // Clear error
        viewModel.clearError()
        advanceUntilIdle()
        
        // Verify error is cleared
        state = viewModel.uiState.first()
        assertNull(state.error)
    }

    @Test
    fun `loading state is managed correctly during save`() = runTest {
        coEvery { mockRepository.saveConfig(any()) } returns Result.success(Unit)
        
        viewModel.updateScaleMin("1")
        viewModel.updateScaleMax("10")
        advanceUntilIdle()
        
        // Initial state should not be loading
        var state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        
        // Start save operation
        viewModel.saveConfiguration(
            onSuccess = { },
            onError = { }
        )
        
        // Should be loading during operation
        state = viewModel.uiState.first()
        assertTrue(state.isLoading)
        
        advanceUntilIdle()
        
        // Should not be loading after completion
        state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }
}