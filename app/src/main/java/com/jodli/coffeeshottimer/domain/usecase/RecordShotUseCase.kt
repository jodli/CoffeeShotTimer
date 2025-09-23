package com.jodli.coffeeshottimer.domain.usecase

import android.os.SystemClock
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for recording espresso shots with validation and business rules.
 * Handles timer functionality, validation, and automatic brew ratio calculation.
 */
@Singleton
class RecordShotUseCase @Inject constructor(
    private val shotRepository: ShotRepository
) {

    // Timer state management
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Shot recording state
    private val _recordingState = MutableStateFlow(ShotRecordingState())
    val recordingState: StateFlow<ShotRecordingState> = _recordingState.asStateFlow()

    /**
     * Start the extraction timer using SystemClock.elapsedRealtime() for accuracy.
     */
    fun startTimer() {
        val currentState = _timerState.value
        if (!currentState.isRunning) {
            _timerState.value = currentState.copy(
                isRunning = true,
                startTime = SystemClock.elapsedRealtime() - (currentState.elapsedTimeSeconds * 1000L)
            )
        }
    }

    /**
     * Pause the extraction timer and return the elapsed time.
     * Uses SystemClock.elapsedRealtime() for accurate timing.
     * @return Elapsed time in seconds
     */
    fun pauseTimer(): Int {
        val currentState = _timerState.value
        if (currentState.isRunning) {
            val elapsedTime = ((SystemClock.elapsedRealtime() - currentState.startTime) / 1000).toInt()
            _timerState.value = currentState.copy(
                isRunning = false,
                elapsedTimeSeconds = elapsedTime
            )
            return elapsedTime
        }
        return currentState.elapsedTimeSeconds
    }

    /**
     * Stop the extraction timer and return the elapsed time.
     * @return Elapsed time in seconds
     */
    fun stopTimer(): Int {
        return pauseTimer() // Stop is the same as pause for now
    }

    /**
     * Reset the timer to initial state.
     */
    fun resetTimer() {
        _timerState.value = TimerState()
    }

    /**
     * Restore timer state from saved data (for configuration changes).
     * @param isRunning Whether the timer was running
     * @param startTime The start time in SystemClock.elapsedRealtime()
     * @param elapsedTimeSeconds The elapsed time in seconds
     */
    fun restoreTimerState(isRunning: Boolean, startTime: Long, elapsedTimeSeconds: Int) {
        _timerState.value = TimerState(
            isRunning = isRunning,
            startTime = startTime,
            elapsedTimeSeconds = elapsedTimeSeconds
        )
    }

    /**
     * Update the timer state (called periodically while timer is running).
     * Uses SystemClock.elapsedRealtime() for accurate timing.
     */
    fun updateTimer() {
        val currentState = _timerState.value
        if (currentState.isRunning) {
            val elapsedTime = ((SystemClock.elapsedRealtime() - currentState.startTime) / 1000).toInt()
            _timerState.value = currentState.copy(elapsedTimeSeconds = elapsedTime)
        }
    }

    /**
     * Calculate brew ratio from input and output weights.
     * @param coffeeWeightIn Input coffee weight in grams
     * @param coffeeWeightOut Output coffee weight in grams
     * @return Calculated brew ratio or null if invalid inputs
     */
    fun calculateBrewRatio(coffeeWeightIn: Double, coffeeWeightOut: Double): Double? {
        return if (coffeeWeightIn > 0 && coffeeWeightOut > 0) {
            kotlin.math.round((coffeeWeightOut / coffeeWeightIn) * 100) / 100
        } else {
            null
        }
    }

    /**
     * Validate shot parameters before recording.
     * @param beanId Selected bean ID
     * @param coffeeWeightIn Input coffee weight
     * @param coffeeWeightOut Output coffee weight
     * @param extractionTimeSeconds Extraction time in seconds
     * @param grinderSetting Grinder setting
     * @param notes Optional notes
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateShotParameters(
        beanId: String,
        coffeeWeightIn: Double,
        coffeeWeightOut: Double,
        extractionTimeSeconds: Int,
        grinderSetting: String,
        notes: String = ""
    ): ValidationResult {
        val shot = Shot(
            beanId = beanId,
            coffeeWeightIn = coffeeWeightIn,
            coffeeWeightOut = coffeeWeightOut,
            extractionTimeSeconds = extractionTimeSeconds,
            grinderSetting = grinderSetting,
            notes = notes
        )

        return shotRepository.validateShot(shot)
    }

    /**
     * Record a new espresso shot with full validation and business rules.
     * @param beanId Selected bean ID
     * @param coffeeWeightIn Input coffee weight in grams
     * @param coffeeWeightOut Output coffee weight in grams
     * @param extractionTimeSeconds Extraction time in seconds
     * @param grinderSetting Grinder setting
     * @param notes Optional notes
     * @return Result indicating success or failure
     */
    suspend fun recordShot(
        beanId: String,
        coffeeWeightIn: Double,
        coffeeWeightOut: Double,
        extractionTimeSeconds: Int,
        grinderSetting: String,
        notes: String = ""
    ): Result<Shot> {
        return try {
            // Update recording state to indicate recording in progress
            _recordingState.value = _recordingState.value.copy(
                isRecording = true,
                error = null
            )

            // Create shot with current timestamp
            val shot = Shot(
                beanId = beanId,
                coffeeWeightIn = coffeeWeightIn,
                coffeeWeightOut = coffeeWeightOut,
                extractionTimeSeconds = extractionTimeSeconds,
                grinderSetting = grinderSetting,
                notes = notes,
                timestamp = LocalDateTime.now()
            )

            // Record the shot through repository
            val result = shotRepository.recordShot(shot)

            if (result.isSuccess) {
                // Update recording state to indicate success
                _recordingState.value = _recordingState.value.copy(
                    isRecording = false,
                    lastRecordedShot = shot,
                    error = null
                )

                // Reset timer after successful recording
                resetTimer()

                Result.success(shot)
            } else {
                // Update recording state with error
                val error = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                _recordingState.value = _recordingState.value.copy(
                    isRecording = false,
                    error = error
                )

                Result.failure(
                    result.exceptionOrNull() ?: DomainException(DomainErrorCode.SHOT_RECORDING_FAILED, error)
                )
            }
        } catch (exception: Exception) {
            // Update recording state with error
            _recordingState.value = _recordingState.value.copy(
                isRecording = false,
                error = exception.message ?: "Unknown error occurred"
            )

            Result.failure(
                if (exception is DomainException) {
                    exception
                } else {
                    DomainException(DomainErrorCode.UNKNOWN_ERROR, "Unexpected error recording shot", exception)
                }
            )
        }
    }

    /**
     * Record a shot using the current timer state.
     * Convenience method that uses the current timer's elapsed time.
     * @param beanId Selected bean ID
     * @param coffeeWeightIn Input coffee weight in grams
     * @param coffeeWeightOut Output coffee weight in grams
     * @param grinderSetting Grinder setting
     * @param notes Optional notes
     * @return Result indicating success or failure
     */
    suspend fun recordShotWithCurrentTimer(
        beanId: String,
        coffeeWeightIn: Double,
        coffeeWeightOut: Double,
        grinderSetting: String,
        notes: String = ""
    ): Result<Shot> {
        val extractionTime = if (_timerState.value.isRunning) {
            stopTimer()
        } else {
            _timerState.value.elapsedTimeSeconds
        }

        return recordShot(
            beanId = beanId,
            coffeeWeightIn = coffeeWeightIn,
            coffeeWeightOut = coffeeWeightOut,
            extractionTimeSeconds = extractionTime,
            grinderSetting = grinderSetting,
            notes = notes
        )
    }

    /**
     * Get suggested grinder setting for a bean.
     * @param beanId Bean ID to get suggestion for
     * @return Result containing suggested grinder setting or null
     */
    suspend fun getSuggestedGrinderSetting(beanId: String): Result<String?> {
        return shotRepository.getSuggestedGrinderSetting(beanId)
    }

    /**
     * Clear any recording errors.
     */
    fun clearError() {
        _recordingState.value = _recordingState.value.copy(error = null)
    }

    /**
     * Check if extraction time is within optimal range (25-30 seconds).
     * @param extractionTimeSeconds Extraction time to check
     * @return true if within optimal range
     */
    fun isOptimalExtractionTime(extractionTimeSeconds: Int): Boolean {
        return extractionTimeSeconds in 25..30
    }

    /**
     * Check if brew ratio is within typical espresso range (1:1.5 to 1:3.0).
     * @param brewRatio Brew ratio to check
     * @return true if within typical range
     */
    fun isTypicalBrewRatio(brewRatio: Double): Boolean {
        return brewRatio in 1.5..3.0
    }

    /**
     * Format brew ratio as a string (e.g., "1:2.5").
     * @param brewRatio Brew ratio to format
     * @return Formatted brew ratio string
     */
    fun formatBrewRatio(brewRatio: Double): String {
        return "1:${String.format(java.util.Locale.ROOT, "%.1f", brewRatio)}"
    }

    /**
     * Format extraction time as MM:SS.
     * @param extractionTimeSeconds Extraction time in seconds
     * @return Formatted time string
     */
    fun formatExtractionTime(extractionTimeSeconds: Int): String {
        val minutes = extractionTimeSeconds / 60
        val seconds = extractionTimeSeconds % 60
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Data class representing the timer state.
 */
data class TimerState(
    val isRunning: Boolean = false,
    val startTime: Long = 0L,
    val elapsedTimeSeconds: Int = 0
)

/**
 * Data class representing the shot recording state.
 */
data class ShotRecordingState(
    val isRecording: Boolean = false,
    val lastRecordedShot: Shot? = null,
    val error: String? = null
)
