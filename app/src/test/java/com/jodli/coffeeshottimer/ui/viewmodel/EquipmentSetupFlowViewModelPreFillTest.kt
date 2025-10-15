package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.repository.BasketConfigRepository
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EquipmentSetupFlowViewModel pre-filling behavior.
 * Tests that existing configurations are loaded and pre-filled in the UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EquipmentSetupFlowViewModelPreFillTest {

    private lateinit var grinderConfigRepository: GrinderConfigRepository
    private lateinit var basketConfigRepository: BasketConfigRepository
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var viewModel: EquipmentSetupFlowViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        grinderConfigRepository = mockk()
        basketConfigRepository = mockk()
        onboardingManager = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when existing grinder config exists, pre-fills grinder fields`() = runTest {
        // Given
        val existingGrinderConfig = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.5
        )

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(existingGrinderConfig)
        coEvery { basketConfigRepository.getActiveConfig() } returns Result.success(null)

        // When
        viewModel = EquipmentSetupFlowViewModel(grinderConfigRepository, basketConfigRepository, onboardingManager)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("1", uiState.grinderScaleMin)
        assertEquals("10", uiState.grinderScaleMax)
        assertEquals("0.5", uiState.grinderStepSize)
        assertTrue("Grinder should be valid after pre-filling", uiState.isGrinderValid)
    }

    @Test
    fun `when existing basket config exists, pre-fills basket fields`() = runTest {
        // Given
        val existingBasketConfig = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f,
            isActive = true
        )

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(null)
        coEvery { basketConfigRepository.getActiveConfig() } returns Result.success(existingBasketConfig)

        // When
        viewModel = EquipmentSetupFlowViewModel(grinderConfigRepository, basketConfigRepository, onboardingManager)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("14", uiState.coffeeInMin)
        assertEquals("22", uiState.coffeeInMax)
        assertEquals("28", uiState.coffeeOutMin)
        assertEquals("55", uiState.coffeeOutMax)
        assertTrue("Basket should be valid after pre-filling", uiState.isBasketValid)
    }

    @Test
    fun `when both configs exist, pre-fills both grinder and basket fields`() = runTest {
        // Given
        val existingGrinderConfig = GrinderConfiguration(
            scaleMin = 30,
            scaleMax = 80,
            stepSize = 1.0
        )
        val existingBasketConfig = BasketConfiguration(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            coffeeOutMin = 20f,
            coffeeOutMax = 40f,
            isActive = true
        )

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(existingGrinderConfig)
        coEvery { basketConfigRepository.getActiveConfig() } returns Result.success(existingBasketConfig)

        // When
        viewModel = EquipmentSetupFlowViewModel(grinderConfigRepository, basketConfigRepository, onboardingManager)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()

        // Grinder fields
        assertEquals("30", uiState.grinderScaleMin)
        assertEquals("80", uiState.grinderScaleMax)
        assertEquals("1.0", uiState.grinderStepSize)
        assertTrue("Grinder should be valid after pre-filling", uiState.isGrinderValid)

        // Basket fields
        assertEquals("7", uiState.coffeeInMin)
        assertEquals("12", uiState.coffeeInMax)
        assertEquals("20", uiState.coffeeOutMin)
        assertEquals("40", uiState.coffeeOutMax)
        assertTrue("Basket should be valid after pre-filling", uiState.isBasketValid)
    }

    @Test
    fun `when no existing configs, uses default empty values`() = runTest {
        // Given
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(null)
        coEvery { basketConfigRepository.getActiveConfig() } returns Result.success(null)

        // When
        viewModel = EquipmentSetupFlowViewModel(grinderConfigRepository, basketConfigRepository, onboardingManager)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()

        // Should use defaults from EquipmentSetupFlowUiState
        assertEquals("", uiState.grinderScaleMin)
        assertEquals("", uiState.grinderScaleMax)
        assertEquals("0.5", uiState.grinderStepSize) // Default step size
        assertEquals("", uiState.coffeeInMin)
        assertEquals("", uiState.coffeeInMax)
        assertEquals("", uiState.coffeeOutMin)
        assertEquals("", uiState.coffeeOutMax)
    }

    @Test
    fun `when repository fails, gracefully continues with defaults`() = runTest {
        // Given
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.failure(Exception("Database error"))
        coEvery { basketConfigRepository.getActiveConfig() } returns Result.failure(Exception("Database error"))

        // When
        viewModel = EquipmentSetupFlowViewModel(grinderConfigRepository, basketConfigRepository, onboardingManager)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()

        // Should gracefully handle errors and use defaults
        assertEquals("", uiState.grinderScaleMin)
        assertEquals("", uiState.grinderScaleMax)
        assertEquals("0.5", uiState.grinderStepSize)
        assertEquals("", uiState.coffeeInMin)
        assertEquals("", uiState.coffeeInMax)
        assertEquals("", uiState.coffeeOutMin)
        assertEquals("", uiState.coffeeOutMax)
    }
}
