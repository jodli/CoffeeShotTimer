package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.ValidationResult
import com.example.coffeeshottimer.data.repository.BeanRepository
import com.example.coffeeshottimer.domain.usecase.RecordShotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for shot recording screen.
 * Integrates with RecordShotUseCase for timer functionality and shot recording.
 */
@HiltViewModel
class ShotRecordingViewModel @Inject constructor(
    private val recordShotUseCase: RecordShotUseCase,
    private val beanRepository: BeanRepository
) : ViewModel() {
    
    // Bean management state
    private val _activeBeans = MutableStateFlow<List<Bean>>(emptyList())
    val activeBeans: StateFlow<List<Bean>> = _activeBeans.asStateFlow()
    
    private val _selectedBean = MutableStateFlow<Bean?>(null)
    val selectedBean: StateFlow<Bean?> = _selectedBean.asStateFlow()
    
    private val _suggestedGrinderSetting = MutableStateFlow<String?>(null)
    val suggestedGrinderSetting: StateFlow<String?> = _suggestedGrinderSetting.asStateFlow()
    
    // Form state
    private val _coffeeWeightIn = MutableStateFlow("")
    val coffeeWeightIn: StateFlow<String> = _coffeeWeightIn.asStateFlow()
    
    private val _coffeeWeightOut = MutableStateFlow("")
    val coffeeWeightOut: StateFlow<String> = _coffeeWeightOut.asStateFlow()
    
    private val _grinderSetting = MutableStateFlow("")
    val grinderSetting: StateFlow<String> = _grinderSetting.asStateFlow()
    
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()
    
    // Validation state
    private val _coffeeWeightInError = MutableStateFlow<String?>(null)
    val coffeeWeightInError: StateFlow<String?> = _coffeeWeightInError.asStateFlow()
    
    private val _coffeeWeightOutError = MutableStateFlow<String?>(null)
    val coffeeWeightOutError: StateFlow<String?> = _coffeeWeightOutError.asStateFlow()
    
    private val _grinderSettingError = MutableStateFlow<String?>(null)
    val grinderSettingError: StateFlow<String?> = _grinderSettingError.asStateFlow()
    
    // Brew ratio calculation
    private val _brewRatio = MutableStateFlow<Double?>(null)
    val brewRatio: StateFlow<Double?> = _brewRatio.asStateFlow()
    
    private val _formattedBrewRatio = MutableStateFlow<String?>(null)
    val formattedBrewRatio: StateFlow<String?> = _formattedBrewRatio.asStateFlow()
    
    private val _isOptimalBrewRatio = MutableStateFlow(false)
    val isOptimalBrewRatio: StateFlow<Boolean> = _isOptimalBrewRatio.asStateFlow()
    
    // Timer state (delegated to use case)
    val timerState = recordShotUseCase.timerState
    val recordingState = recordShotUseCase.recordingState
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()
    
    // Timer update job
    private var timerUpdateJob: Job? = null
    
    init {
        loadActiveBeans()
        startTimerUpdates()
        observeRecordingState()
    }
    
    /**
     * Load active beans for selection.
     */
    private fun loadActiveBeans() {
        viewModelScope.launch {
            _isLoading.value = true
            beanRepository.getActiveBeans().collect { result ->
                result.fold(
                    onSuccess = { beans ->
                        _activeBeans.value = beans
                        // Auto-select first bean if none selected
                        if (_selectedBean.value == null && beans.isNotEmpty()) {
                            selectBean(beans.first())
                        }
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start periodic timer updates when timer is running.
     */
    private fun startTimerUpdates() {
        timerUpdateJob = viewModelScope.launch {
            while (true) {
                delay(100L) // Update every 100ms for smooth display
                if (timerState.value.isRunning) {
                    recordShotUseCase.updateTimer()
                }
            }
        }
    }
    
    /**
     * Observe recording state for error handling.
     */
    private fun observeRecordingState() {
        viewModelScope.launch {
            recordingState.collect { state ->
                _isLoading.value = state.isRecording
                state.error?.let { error ->
                    _errorMessage.value = error
                }
            }
        }
    }
    
    /**
     * Select a bean and load its suggested grinder setting.
     */
    fun selectBean(bean: Bean) {
        _selectedBean.value = bean
        
        // Load suggested grinder setting
        viewModelScope.launch {
            val result = recordShotUseCase.getSuggestedGrinderSetting(bean.id)
            result.fold(
                onSuccess = { suggestion ->
                    _suggestedGrinderSetting.value = suggestion
                    // Auto-fill grinder setting if available and current setting is empty
                    if (_grinderSetting.value.isEmpty() && suggestion != null) {
                        updateGrinderSetting(suggestion)
                    }
                },
                onFailure = {
                    _suggestedGrinderSetting.value = null
                }
            )
        }
        
        validateForm()
    }
    
    /**
     * Update coffee weight in and validate.
     */
    fun updateCoffeeWeightIn(value: String) {
        _coffeeWeightIn.value = value
        _coffeeWeightInError.value = validateWeight(value, "Coffee input weight", 0.1, 50.0)
        calculateBrewRatio()
        validateForm()
    }
    
    /**
     * Update coffee weight out and validate.
     */
    fun updateCoffeeWeightOut(value: String) {
        _coffeeWeightOut.value = value
        _coffeeWeightOutError.value = validateWeight(value, "Coffee output weight", 0.1, 100.0)
        calculateBrewRatio()
        validateForm()
    }
    
    /**
     * Update grinder setting and validate.
     */
    fun updateGrinderSetting(value: String) {
        _grinderSetting.value = value
        _grinderSettingError.value = validateGrinderSetting(value)
        validateForm()
    }
    
    /**
     * Update notes.
     */
    fun updateNotes(value: String) {
        _notes.value = value
    }
    
    /**
     * Calculate and update brew ratio in real-time.
     */
    private fun calculateBrewRatio() {
        val weightIn = _coffeeWeightIn.value.toDoubleOrNull()
        val weightOut = _coffeeWeightOut.value.toDoubleOrNull()
        
        val ratio = recordShotUseCase.calculateBrewRatio(weightIn ?: 0.0, weightOut ?: 0.0)
        _brewRatio.value = ratio
        
        if (ratio != null) {
            _formattedBrewRatio.value = recordShotUseCase.formatBrewRatio(ratio)
            _isOptimalBrewRatio.value = recordShotUseCase.isTypicalBrewRatio(ratio)
        } else {
            _formattedBrewRatio.value = null
            _isOptimalBrewRatio.value = false
        }
    }
    
    /**
     * Validate the entire form.
     */
    private fun validateForm() {
        val isValid = _selectedBean.value != null &&
                _coffeeWeightIn.value.isNotBlank() &&
                _coffeeWeightOut.value.isNotBlank() &&
                _grinderSetting.value.isNotBlank() &&
                _coffeeWeightInError.value == null &&
                _coffeeWeightOutError.value == null &&
                _grinderSettingError.value == null &&
                timerState.value.elapsedTimeSeconds > 0
        
        _isFormValid.value = isValid
    }
    
    /**
     * Start the extraction timer.
     */
    fun startTimer() {
        recordShotUseCase.startTimer()
        validateForm() // Revalidate as timer state affects form validity
    }
    
    /**
     * Stop the extraction timer.
     */
    fun stopTimer() {
        recordShotUseCase.stopTimer()
        validateForm()
    }
    
    /**
     * Reset the extraction timer.
     */
    fun resetTimer() {
        recordShotUseCase.resetTimer()
        validateForm()
    }
    
    /**
     * Record the current shot with validation.
     */
    fun recordShot() {
        val bean = _selectedBean.value
        val weightIn = _coffeeWeightIn.value.toDoubleOrNull()
        val weightOut = _coffeeWeightOut.value.toDoubleOrNull()
        val grinder = _grinderSetting.value
        val shotNotes = _notes.value
        
        if (bean == null || weightIn == null || weightOut == null || grinder.isBlank()) {
            _errorMessage.value = "Please fill in all required fields"
            return
        }
        
        viewModelScope.launch {
            // Validate parameters first
            val validationResult = recordShotUseCase.validateShotParameters(
                beanId = bean.id,
                coffeeWeightIn = weightIn,
                coffeeWeightOut = weightOut,
                extractionTimeSeconds = timerState.value.elapsedTimeSeconds,
                grinderSetting = grinder,
                notes = shotNotes
            )
            
            if (!validationResult.isValid) {
                _errorMessage.value = validationResult.errors.joinToString(", ")
                return@launch
            }
            
            // Record the shot using current timer
            val result = recordShotUseCase.recordShotWithCurrentTimer(
                beanId = bean.id,
                coffeeWeightIn = weightIn,
                coffeeWeightOut = weightOut,
                grinderSetting = grinder,
                notes = shotNotes
            )
            
            result.fold(
                onSuccess = {
                    // Clear form after successful recording
                    clearForm()
                    _errorMessage.value = null
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to record shot"
                }
            )
        }
    }
    
    /**
     * Clear the form after successful recording.
     */
    private fun clearForm() {
        _coffeeWeightIn.value = ""
        _coffeeWeightOut.value = ""
        _grinderSetting.value = _suggestedGrinderSetting.value ?: ""
        _notes.value = ""
        _coffeeWeightInError.value = null
        _coffeeWeightOutError.value = null
        _grinderSettingError.value = null
        _brewRatio.value = null
        _formattedBrewRatio.value = null
        _isOptimalBrewRatio.value = false
        validateForm()
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
        recordShotUseCase.clearError()
    }
    
    /**
     * Get formatted extraction time.
     */
    fun getFormattedExtractionTime(): String {
        return recordShotUseCase.formatExtractionTime(timerState.value.elapsedTimeSeconds)
    }
    
    /**
     * Check if extraction time is optimal.
     */
    fun isOptimalExtractionTime(): Boolean {
        return recordShotUseCase.isOptimalExtractionTime(timerState.value.elapsedTimeSeconds)
    }
    
    override fun onCleared() {
        super.onCleared()
        timerUpdateJob?.cancel()
    }
    
    // Validation helper functions
    private fun validateWeight(value: String, fieldName: String, min: Double, max: Double): String? {
        if (value.isBlank()) return "$fieldName is required"
        
        val weight = value.toDoubleOrNull()
        return when {
            weight == null -> "Please enter a valid number"
            weight < min -> "$fieldName must be at least ${min}g"
            weight > max -> "$fieldName cannot exceed ${max}g"
            else -> null
        }
    }
    
    private fun validateGrinderSetting(value: String): String? {
        return when {
            value.isBlank() -> "Grinder setting is required"
            value.length > 50 -> "Grinder setting cannot exceed 50 characters"
            else -> null
        }
    }
}