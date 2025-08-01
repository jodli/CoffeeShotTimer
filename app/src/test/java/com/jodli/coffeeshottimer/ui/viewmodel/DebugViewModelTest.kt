package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.util.DatabasePopulator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DebugViewModel class.
 * Tests state management, operation coordination, and conditional compilation behavior.
 */
@ExperimentalCoroutinesApi
class DebugViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var databasePopulator: DatabasePopulator
    private lateinit var viewModel: DebugViewModel

    @Before
    fun setup() {
        // Skip all tests in this class if not in debug build
        org.junit.Assume.assumeTrue("DebugViewModel tests only run in debug builds", com.jodli.coffeeshottimer.BuildConfig.DEBUG)
        
        Dispatchers.setMain(testDispatcher)
        databasePopulator = mockk(relaxed = true)
        viewModel = DebugViewModel(databasePopulator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() {
        // Given & When
        val initialState = viewModel.uiState.value

        // Then
        assertFalse("Dialog should not be visible initially", initialState.isDialogVisible)
        assertFalse("Should not be loading initially", initialState.isLoading)
        assertNull("Operation result should be null initially", initialState.operationResult)
        assertFalse("Confirmation should not be shown initially", initialState.showConfirmation)
    }

    @Test
    fun `showDialog should update state correctly in debug build`() {
        // When
        viewModel.showDialog()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Dialog should be visible", state.isDialogVisible)
        assertNull("Operation result should be cleared", state.operationResult)
        assertFalse("Confirmation should be hidden", state.showConfirmation)
    }

    @Test
    fun `hideDialog should reset state correctly`() {
        // Given
        viewModel.showDialog()
        
        // When
        viewModel.hideDialog()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Dialog should not be visible", state.isDialogVisible)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Operation result should be null", state.operationResult)
        assertFalse("Confirmation should not be shown", state.showConfirmation)
    }

    @Test
    fun `showConfirmation should update state correctly`() {
        // When
        viewModel.showConfirmation()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Confirmation should be shown", state.showConfirmation)
    }

    @Test
    fun `hideConfirmation should update state correctly`() {
        // Given
        viewModel.showConfirmation()
        
        // When
        viewModel.hideConfirmation()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Confirmation should be hidden", state.showConfirmation)
    }

    // Note: Release build behavior is tested in ConditionalCompilationTest

    @Test
    fun `fillDatabase should not execute when databasePopulator is null`() = runTest {
        // Given
        val viewModelWithNullPopulator = DebugViewModel(null)

        // When
        viewModelWithNullPopulator.fillDatabase()
        advanceUntilIdle()

        // Then
        val state = viewModelWithNullPopulator.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Operation result should be null", state.operationResult)
    }

    @Test
    fun `fillDatabase should execute successfully`() = runTest {
        // Given
        coEvery { databasePopulator.populateForScreenshots() } returns Unit

        // When
        viewModel.fillDatabase()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { databasePopulator.populateForScreenshots() }
        
        val state = viewModel.uiState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertEquals(
            "Database filled with test data successfully!",
            state.operationResult
        )
    }

    @Test
    fun `fillDatabase should handle errors correctly`() = runTest {
        // Given
        val errorMessage = "Database connection failed"
        coEvery { databasePopulator.populateForScreenshots() } throws Exception(errorMessage)

        // When
        viewModel.fillDatabase()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading after error", state.isLoading)
        assertTrue(
            "Should contain error message",
            state.operationResult?.contains("Failed to fill database") == true
        )
        assertTrue(
            "Should contain original error message",
            state.operationResult?.contains(errorMessage) == true
        )
    }

    // Note: Release build behavior is tested in ConditionalCompilationTest

    @Test
    fun `addMoreShots should execute successfully with default count`() = runTest {
        // Given
        coEvery { databasePopulator.addMoreShots(10) } returns Unit

        // When
        viewModel.addMoreShots()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { databasePopulator.addMoreShots(10) }
        
        val state = viewModel.uiState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertEquals(
            "Added 10 additional shots successfully!",
            state.operationResult
        )
    }

    @Test
    fun `addMoreShots should execute successfully with custom count`() = runTest {
        // Given
        val customCount = 5
        coEvery { databasePopulator.addMoreShots(customCount) } returns Unit

        // When
        viewModel.addMoreShots(customCount)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { databasePopulator.addMoreShots(customCount) }
        
        val state = viewModel.uiState.value
        assertEquals(
            "Added $customCount additional shots successfully!",
            state.operationResult
        )
    }

    @Test
    fun `addMoreShots should handle errors correctly`() = runTest {
        // Given
        val errorMessage = "No beans found"
        coEvery { databasePopulator.addMoreShots(any()) } throws Exception(errorMessage)

        // When
        viewModel.addMoreShots(5)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Should contain error message",
            state.operationResult?.contains("Failed to add shots") == true
        )
        assertTrue(
            "Should contain original error message",
            state.operationResult?.contains(errorMessage) == true
        )
    }

    // Note: Release build behavior is tested in ConditionalCompilationTest

    @Test
    fun `clearDatabase should execute successfully`() = runTest {
        // Given
        coEvery { databasePopulator.clearAllData() } returns Unit

        // When
        viewModel.clearDatabase()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { databasePopulator.clearAllData() }
        
        val state = viewModel.uiState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertFalse("Confirmation should be hidden after operation", state.showConfirmation)
        assertEquals(
            "Database cleared successfully!",
            state.operationResult
        )
    }

    @Test
    fun `clearDatabase should handle errors correctly`() = runTest {
        // Given
        val errorMessage = "Foreign key constraint violation"
        coEvery { databasePopulator.clearAllData() } throws Exception(errorMessage)

        // When
        viewModel.clearDatabase()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading after error", state.isLoading)
        assertFalse("Confirmation should be hidden after error", state.showConfirmation)
        assertTrue(
            "Should contain error message",
            state.operationResult?.contains("Failed to clear database") == true
        )
        assertTrue(
            "Should contain original error message",
            state.operationResult?.contains(errorMessage) == true
        )
    }

    @Test
    fun `clearResult should clear operation result`() {
        // Given
        viewModel.fillDatabase()
        // Simulate completion by manually setting result
        viewModel.clearResult()

        // When
        viewModel.clearResult()

        // Then
        val state = viewModel.uiState.value
        assertNull("Operation result should be cleared", state.operationResult)
    }

    @Test
    fun `operations should set loading state correctly`() = runTest {
        // Given
        coEvery { databasePopulator.populateForScreenshots() } coAnswers {
            // Simulate delay
            kotlinx.coroutines.delay(100)
        }

        // When
        viewModel.fillDatabase()

        // Execute immediate coroutine work to set loading state
        runCurrent()

        // Then - check loading state before completion
        var state = viewModel.uiState.value
        assertTrue("Should be loading during operation", state.isLoading)
        assertNull("Operation result should be null during loading", state.operationResult)

        advanceUntilIdle()

        // Then - check state after completion
        state = viewModel.uiState.value
        assertFalse("Should not be loading after completion", state.isLoading)
    }

    @Test
    fun `multiple operations should not interfere with each other`() = runTest {
        // Given
        coEvery { databasePopulator.populateForScreenshots() } returns Unit
        coEvery { databasePopulator.clearAllData() } returns Unit

        // When - start fill operation
        viewModel.fillDatabase()
        advanceUntilIdle()

        // Clear the result and start clear operation
        viewModel.clearResult()
        viewModel.clearDatabase()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { databasePopulator.populateForScreenshots() }
        coVerify(exactly = 1) { databasePopulator.clearAllData() }
        
        val state = viewModel.uiState.value
        assertEquals(
            "Database cleared successfully!",
            state.operationResult
        )
    }
}