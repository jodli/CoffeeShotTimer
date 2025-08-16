package com.jodli.coffeeshottimer.domain.usecase

import android.os.SystemClock
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RecordShotUseCase.
 * Tests timer functionality, validation, brew ratio calculation, and shot recording logic.
 */
@ExperimentalCoroutinesApi
class RecordShotUseCaseTest {
    
    private lateinit var shotRepository: ShotRepository
    private lateinit var recordShotUseCase: RecordShotUseCase
    
    @Before
    fun setup() {
        // Mock SystemClock for consistent testing
        mockkStatic(SystemClock::class)
        
        shotRepository = mockk()
        recordShotUseCase = RecordShotUseCase(shotRepository)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }
    
    // Timer functionality tests
    
    @Test
    fun `startTimer should set timer to running state`() {
        // Given
        val mockTime = 1000L
        every { SystemClock.elapsedRealtime() } returns mockTime
        
        // When
        recordShotUseCase.startTimer()
        
        // Then
        val timerState = recordShotUseCase.timerState.value
        assertTrue("Timer should be running", timerState.isRunning)
        assertEquals("Start time should be set to mock time", mockTime, timerState.startTime)
        assertEquals("Elapsed time should be 0", 0, timerState.elapsedTimeSeconds)
    }
    
    @Test
    fun `startTimer should not restart if already running`() {
        // Given
        val mockTime = 2000L
        every { SystemClock.elapsedRealtime() } returns mockTime
        
        recordShotUseCase.startTimer()
        val initialStartTime = recordShotUseCase.timerState.value.startTime
        
        // When - try to start again
        recordShotUseCase.startTimer()
        
        // Then
        val timerState = recordShotUseCase.timerState.value
        assertEquals("Start time should not change", initialStartTime, timerState.startTime)
    }
    
    @Test
    fun `stopTimer should stop running timer and return elapsed time`() {
        // Given
        var mockTime = 3000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }
        
        recordShotUseCase.startTimer()
        
        // Advance time by 2 seconds
        mockTime += 2000L
        
        // When
        val elapsedTime = recordShotUseCase.stopTimer()
        
        // Then
        val timerState = recordShotUseCase.timerState.value
        assertFalse("Timer should not be running", timerState.isRunning)
        assertEquals("Elapsed time should be 2 seconds", 2, elapsedTime)
        assertEquals("Elapsed time should match state", elapsedTime, timerState.elapsedTimeSeconds)
    }
    
    @Test
    fun `stopTimer should return current elapsed time if not running`() {
        // Given - timer not started
        val initialElapsedTime = recordShotUseCase.timerState.value.elapsedTimeSeconds
        
        // When
        val elapsedTime = recordShotUseCase.stopTimer()
        
        // Then
        assertEquals("Should return current elapsed time", initialElapsedTime, elapsedTime)
    }
    
    @Test
    fun `resetTimer should reset timer to initial state`() {
        // Given
        val mockTime = 1000L
        every { SystemClock.elapsedRealtime() } returns mockTime
        
        recordShotUseCase.startTimer()
        recordShotUseCase.stopTimer()
        
        // When
        recordShotUseCase.resetTimer()
        
        // Then
        val timerState = recordShotUseCase.timerState.value
        assertFalse("Timer should not be running", timerState.isRunning)
        assertEquals("Start time should be 0", 0L, timerState.startTime)
        assertEquals("Elapsed time should be 0", 0, timerState.elapsedTimeSeconds)
    }
    
    // Brew ratio calculation tests
    
    @Test
    fun `calculateBrewRatio should return correct ratio for valid inputs`() {
        // Given
        val coffeeWeightIn = 18.0
        val coffeeWeightOut = 36.0
        
        // When
        val brewRatio = recordShotUseCase.calculateBrewRatio(coffeeWeightIn, coffeeWeightOut)
        
        // Then
        assertNotNull("Brew ratio should not be null", brewRatio)
        assertEquals("Brew ratio should be 2.0", 2.0, brewRatio!!, 0.01)
    }
    
    @Test
    fun `calculateBrewRatio should return null for zero input weight`() {
        // Given
        val coffeeWeightIn = 0.0
        val coffeeWeightOut = 36.0
        
        // When
        val brewRatio = recordShotUseCase.calculateBrewRatio(coffeeWeightIn, coffeeWeightOut)
        
        // Then
        assertNull("Brew ratio should be null for zero input", brewRatio)
    }
    
    @Test
    fun `calculateBrewRatio should return null for zero output weight`() {
        // Given
        val coffeeWeightIn = 18.0
        val coffeeWeightOut = 0.0
        
        // When
        val brewRatio = recordShotUseCase.calculateBrewRatio(coffeeWeightIn, coffeeWeightOut)
        
        // Then
        assertNull("Brew ratio should be null for zero output", brewRatio)
    }
    
    @Test
    fun `calculateBrewRatio should return null for negative weights`() {
        // Given
        val coffeeWeightIn = -18.0
        val coffeeWeightOut = 36.0
        
        // When
        val brewRatio = recordShotUseCase.calculateBrewRatio(coffeeWeightIn, coffeeWeightOut)
        
        // Then
        assertNull("Brew ratio should be null for negative input", brewRatio)
    }
    
    @Test
    fun `calculateBrewRatio should round to 2 decimal places`() {
        // Given
        val coffeeWeightIn = 18.5
        val coffeeWeightOut = 37.3
        
        // When
        val brewRatio = recordShotUseCase.calculateBrewRatio(coffeeWeightIn, coffeeWeightOut)
        
        // Then
        assertNotNull("Brew ratio should not be null", brewRatio)
        assertEquals("Brew ratio should be rounded", 2.02, brewRatio!!, 0.001)
    }
    
    // Validation tests
    
    @Test
    fun `validateShotParameters should return valid result for correct parameters`() = runTest {
        // Given
        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { shotRepository.validateShot(any()) } returns validationResult
        
        // When
        val result = recordShotUseCase.validateShotParameters(
            beanId = "bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        // Then
        assertTrue("Validation should pass", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }
    
    @Test
    fun `validateShotParameters should return invalid result for incorrect parameters`() = runTest {
        // Given
        val validationResult = ValidationResult(
            isValid = false, 
            errors = listOf("Coffee input weight must be at least 0.1g")
        )
        coEvery { shotRepository.validateShot(any()) } returns validationResult
        
        // When
        val result = recordShotUseCase.validateShotParameters(
            beanId = "bean-1",
            coffeeWeightIn = 0.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        // Then
        assertFalse("Validation should fail", result.isValid)
        assertFalse("Should have errors", result.errors.isEmpty())
        assertTrue("Should contain weight error", result.errors.any { it.contains("weight") })
    }
    
    // Shot recording tests
    
    @Test
    fun `recordShot should successfully record valid shot`() = runTest {
        // Given
        coEvery { shotRepository.recordShot(any()) } returns Result.success(Unit)
        
        // When
        val result = recordShotUseCase.recordShot(
            beanId = "bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = "Great shot!"
        )
        
        // Then
        assertTrue("Recording should succeed", result.isSuccess)
        assertNotNull("Should return shot", result.getOrNull())
        
        val recordingState = recordShotUseCase.recordingState.value
        assertFalse("Should not be recording", recordingState.isRecording)
        assertNotNull("Should have last recorded shot", recordingState.lastRecordedShot)
        assertNull("Should have no error", recordingState.error)
        
        coVerify { shotRepository.recordShot(any()) }
    }
    
    @Test
    fun `recordShot should handle repository failure`() = runTest {
        // Given
        val exception = RepositoryException.ValidationError("Bean does not exist")
        coEvery { shotRepository.recordShot(any()) } returns Result.failure(exception)
        
        // When
        val result = recordShotUseCase.recordShot(
            beanId = "invalid-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        // Then
        assertTrue("Recording should fail", result.isFailure)
        assertEquals("Should return correct exception", exception, result.exceptionOrNull())
        
        val recordingState = recordShotUseCase.recordingState.value
        assertFalse("Should not be recording", recordingState.isRecording)
        assertNotNull("Should have error", recordingState.error)
        assertTrue("Error should contain validation message", 
            recordingState.error!!.contains("Bean does not exist"))
    }
    
    @Test
    fun `recordShot should reset timer after successful recording`() = runTest {
        // Given
        val mockTime = 2000L
        every { SystemClock.elapsedRealtime() } returns mockTime
        
        coEvery { shotRepository.recordShot(any()) } returns Result.success(Unit)
        recordShotUseCase.startTimer()
        
        // When
        val result = recordShotUseCase.recordShot(
            beanId = "bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        // Then
        assertTrue("Recording should succeed", result.isSuccess)
        
        val timerState = recordShotUseCase.timerState.value
        assertFalse("Timer should not be running", timerState.isRunning)
        assertEquals("Timer should be reset", 0, timerState.elapsedTimeSeconds)
        assertEquals("Start time should be reset", 0L, timerState.startTime)
    }
    
    @Test
    fun `recordShotWithCurrentTimer should use current timer elapsed time`() = runTest {
        // Given
        var mockTime = 3000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }
        
        coEvery { shotRepository.recordShot(any()) } returns Result.success(Unit)
        recordShotUseCase.startTimer()
        
        // Simulate time passing - advance time by 2 seconds
        mockTime += 2000L
        recordShotUseCase.updateTimer()
        
        // When
        val result = recordShotUseCase.recordShotWithCurrentTimer(
            beanId = "bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            grinderSetting = "15"
        )
        
        // Then
        assertTrue("Recording should succeed", result.isSuccess)
        val shot = result.getOrNull()
        assertNotNull("Should return shot", shot)
        // The extraction time should be 2 seconds based on our mock time advancement
        assertEquals("Extraction time should match elapsed time", 2, shot!!.extractionTimeSeconds)
        
        coVerify { shotRepository.recordShot(any()) }
    }
    
    @Test
    fun `recordShotWithCurrentTimer should stop running timer`() = runTest {
        // Given
        val mockTime = 4000L
        every { SystemClock.elapsedRealtime() } returns mockTime
        
        coEvery { shotRepository.recordShot(any()) } returns Result.success(Unit)
        recordShotUseCase.startTimer()
        
        // When
        recordShotUseCase.recordShotWithCurrentTimer(
            beanId = "bean-1",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            grinderSetting = "15"
        )
        
        // Then
        val timerState = recordShotUseCase.timerState.value
        assertFalse("Timer should be stopped", timerState.isRunning)
    }
    
    // Grinder setting suggestion tests
    
    @Test
    fun `getSuggestedGrinderSetting should return repository result`() = runTest {
        // Given
        val expectedSetting = "15"
        coEvery { shotRepository.getSuggestedGrinderSetting("bean-1") } returns Result.success(expectedSetting)
        
        // When
        val result = recordShotUseCase.getSuggestedGrinderSetting("bean-1")
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return expected setting", expectedSetting, result.getOrNull())
        
        coVerify { shotRepository.getSuggestedGrinderSetting("bean-1") }
    }
    
    @Test
    fun `getSuggestedGrinderSetting should handle repository failure`() = runTest {
        // Given
        val exception = RepositoryException.DatabaseError("Database error")
        coEvery { shotRepository.getSuggestedGrinderSetting("bean-1") } returns Result.failure(exception)
        
        // When
        val result = recordShotUseCase.getSuggestedGrinderSetting("bean-1")
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertEquals("Should return correct exception", exception, result.exceptionOrNull())
    }
    
    // Utility method tests
    
    @Test
    fun `clearError should clear recording error`() {
        // Given - simulate error state
        recordShotUseCase.recordingState.value.copy(error = "Some error")
        
        // When
        recordShotUseCase.clearError()
        
        // Then
        val recordingState = recordShotUseCase.recordingState.value
        assertNull("Error should be cleared", recordingState.error)
    }
    
    @Test
    fun `isOptimalExtractionTime should return true for optimal range`() {
        // Test cases for optimal range (25-30 seconds)
        assertTrue("25 seconds should be optimal", recordShotUseCase.isOptimalExtractionTime(25))
        assertTrue("28 seconds should be optimal", recordShotUseCase.isOptimalExtractionTime(28))
        assertTrue("30 seconds should be optimal", recordShotUseCase.isOptimalExtractionTime(30))
    }
    
    @Test
    fun `isOptimalExtractionTime should return false for non-optimal range`() {
        // Test cases outside optimal range
        assertFalse("24 seconds should not be optimal", recordShotUseCase.isOptimalExtractionTime(24))
        assertFalse("31 seconds should not be optimal", recordShotUseCase.isOptimalExtractionTime(31))
        assertFalse("15 seconds should not be optimal", recordShotUseCase.isOptimalExtractionTime(15))
        assertFalse("45 seconds should not be optimal", recordShotUseCase.isOptimalExtractionTime(45))
    }
    
    @Test
    fun `isTypicalBrewRatio should return true for typical range`() {
        // Test cases for typical range (1.5-3.0)
        assertTrue("1.5 should be typical", recordShotUseCase.isTypicalBrewRatio(1.5))
        assertTrue("2.0 should be typical", recordShotUseCase.isTypicalBrewRatio(2.0))
        assertTrue("2.5 should be typical", recordShotUseCase.isTypicalBrewRatio(2.5))
        assertTrue("3.0 should be typical", recordShotUseCase.isTypicalBrewRatio(3.0))
    }
    
    @Test
    fun `isTypicalBrewRatio should return false for non-typical range`() {
        // Test cases outside typical range
        assertFalse("1.4 should not be typical", recordShotUseCase.isTypicalBrewRatio(1.4))
        assertFalse("3.1 should not be typical", recordShotUseCase.isTypicalBrewRatio(3.1))
        assertFalse("1.0 should not be typical", recordShotUseCase.isTypicalBrewRatio(1.0))
        assertFalse("4.0 should not be typical", recordShotUseCase.isTypicalBrewRatio(4.0))
    }
    
    @Test
    fun `formatBrewRatio should format correctly`() {
        assertEquals("Should format 2.0", "1:2.0", recordShotUseCase.formatBrewRatio(2.0))
        assertEquals("Should format 2.5", "1:2.5", recordShotUseCase.formatBrewRatio(2.5))
        assertEquals("Should format 1.75", "1:1.8", recordShotUseCase.formatBrewRatio(1.75))
    }
    
    @Test
    fun `formatExtractionTime should format correctly`() {
        assertEquals("Should format 25 seconds", "00:25", recordShotUseCase.formatExtractionTime(25))
        assertEquals("Should format 90 seconds", "01:30", recordShotUseCase.formatExtractionTime(90))
        assertEquals("Should format 125 seconds", "02:05", recordShotUseCase.formatExtractionTime(125))
        assertEquals("Should format 5 seconds", "00:05", recordShotUseCase.formatExtractionTime(5))
    }
}