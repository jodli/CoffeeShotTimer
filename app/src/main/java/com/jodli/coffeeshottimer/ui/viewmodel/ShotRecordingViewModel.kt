package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.jodli.coffeeshottimer.ui.validation.getBrewRatioWarnings
import com.jodli.coffeeshottimer.ui.validation.validateCoffeeWeightIn
import com.jodli.coffeeshottimer.ui.validation.validateCoffeeWeightOut
import com.jodli.coffeeshottimer.ui.validation.validateGrinderSettingEnhanced
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Data class representing a draft shot for auto-save functionality.
 */
@Serializable
data class ShotDraft(
    val selectedBeanId: String? = null,
    val coffeeWeightIn: String = "",
    val coffeeWeightOut: String = "",
    val grinderSetting: String = "",
    val notes: String = "",
    val elapsedTimeSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for shot recording screen.
 * Integrates with RecordShotUseCase for timer functionality and shot recording.
 */
@HiltViewModel
class ShotRecordingViewModel @Inject constructor(
    private val recordShotUseCase: RecordShotUseCase,
    private val beanRepository: BeanRepository,
    private val shotRepository: ShotRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Bean management state
    private val _activeBeans = MutableStateFlow<List<Bean>>(emptyList())
    val activeBeans: StateFlow<List<Bean>> = _activeBeans.asStateFlow()

    private val _selectedBean = MutableStateFlow<Bean?>(null)
    val selectedBean: StateFlow<Bean?> = _selectedBean.asStateFlow()

    private val _suggestedGrinderSetting = MutableStateFlow<String?>(null)
    val suggestedGrinderSetting: StateFlow<String?> = _suggestedGrinderSetting.asStateFlow()

    private val _previousSuccessfulSettings = MutableStateFlow<List<String>>(emptyList())
    val previousSuccessfulSettings: StateFlow<List<String>> =
        _previousSuccessfulSettings.asStateFlow()

    // Bean-specific suggested values for coffee weights
    private val _suggestedCoffeeWeightIn = MutableStateFlow<String?>(null)
    val suggestedCoffeeWeightIn: StateFlow<String?> = _suggestedCoffeeWeightIn.asStateFlow()

    private val _suggestedCoffeeWeightOut = MutableStateFlow<String?>(null)
    val suggestedCoffeeWeightOut: StateFlow<String?> = _suggestedCoffeeWeightOut.asStateFlow()

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

    // Enhanced validation warnings
    private val _brewRatioWarnings = MutableStateFlow<List<String>>(emptyList())

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

    // Success feedback state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Draft auto-save state
    private val _isDraftSaved = MutableStateFlow(false)
    val isDraftSaved: StateFlow<Boolean> = _isDraftSaved.asStateFlow()

    private val _lastDraftSaveTime = MutableStateFlow<Long?>(null)
    val lastDraftSaveTime: StateFlow<Long?> = _lastDraftSaveTime.asStateFlow()

    // Timer update job
    private var timerUpdateJob: Job? = null

    // Auto-save draft job
    private var autoSaveDraftJob: Job? = null

    init {
        loadActiveBeans()
        loadCurrentBean()
        startTimerUpdates()
        observeRecordingState()
        startAutoSaveDraft()
        restoreDraftIfExists()
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
                        // Auto-select first bean if none selected and no current bean
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
     * Load the current bean from repository if one is set.
     * Implements requirement 3.2 for connecting bean selection between screens.
     */
    private fun loadCurrentBean() {
        viewModelScope.launch {
            val result = beanRepository.getCurrentBean()
            result.fold(
                onSuccess = { currentBean ->
                    if (currentBean != null) {
                        selectBean(currentBean)
                    }
                },
                onFailure = { exception ->
                    // Silently handle error - current bean is optional
                }
            )
        }
    }

    /**
     * Start periodic timer updates when timer is running.
     */
    private fun startTimerUpdates() {
        timerUpdateJob = viewModelScope.launch {
            while (isActive) {
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
     * Select a bean and load its suggested settings.
     * Implements requirements 3.3 and 4.4 for remembering settings per bean.
     */
    fun selectBean(bean: Bean) {
        val previousBean = _selectedBean.value
        _selectedBean.value = bean

        // Load suggested settings from last successful shot with this bean
        viewModelScope.launch {
            loadSuggestedSettingsForBean(bean.id, previousBean)
            // Load previous successful grinder settings for visual indicators
            loadPreviousSuccessfulSettings(bean.id)
        }

        validateForm()
    }

    /**
     * Load suggested settings for a bean from the last successful shot.
     * Prioritizes most recent successful settings over historical data.
     */
    private suspend fun loadSuggestedSettingsForBean(beanId: String, previousBean: Bean?) {
        // Load suggested grinder setting
        val grinderResult = recordShotUseCase.getSuggestedGrinderSetting(beanId)
        grinderResult.fold(
            onSuccess = { suggestion ->
                _suggestedGrinderSetting.value = suggestion

                // Auto-fill grinder setting if:
                // 1. Current setting is empty, OR
                // 2. We're switching from a different bean and have a suggestion
                val shouldAutoFillGrinder = _grinderSetting.value.isEmpty() ||
                        (previousBean != null && previousBean.id != beanId && suggestion != null)

                if (shouldAutoFillGrinder && suggestion != null) {
                    updateGrinderSetting(suggestion)
                }
            },
            onFailure = {
                _suggestedGrinderSetting.value = null
            }
        )

        // Load suggested coffee weights from last successful shot
        val lastShotResult = shotRepository.getLastShotForBean(beanId)
        lastShotResult.fold(
            onSuccess = { lastShot ->
                if (lastShot != null) {
                    _suggestedCoffeeWeightIn.value = lastShot.coffeeWeightIn.toString()
                    _suggestedCoffeeWeightOut.value = lastShot.coffeeWeightOut.toString()

                    // Auto-fill coffee weights if:
                    // 1. Current values are empty, OR
                    // 2. We're switching from a different bean
                    val shouldAutoFillWeights = (_coffeeWeightIn.value.isEmpty() && _coffeeWeightOut.value.isEmpty()) ||
                            (previousBean != null && previousBean.id != beanId)

                    if (shouldAutoFillWeights) {
                        updateCoffeeWeightIn(lastShot.coffeeWeightIn.toString())
                        updateCoffeeWeightOut(lastShot.coffeeWeightOut.toString())
                    }
                } else {
                    // No previous shots, clear suggestions
                    _suggestedCoffeeWeightIn.value = null
                    _suggestedCoffeeWeightOut.value = null
                }
            },
            onFailure = {
                _suggestedCoffeeWeightIn.value = null
                _suggestedCoffeeWeightOut.value = null
            }
        )
    }

    /**
     * Load previous successful grinder settings for a bean to show as visual indicators.
     */
    private suspend fun loadPreviousSuccessfulSettings(beanId: String) {
        shotRepository.getShotsByBean(beanId).collect { result ->
            result.fold(
                onSuccess = { shots ->
                    // Get unique grinder settings from recent successful shots (last 10)
                    // Consider shots with brew ratios in typical range as "successful"
                    val successfulSettings = shots
                        .asSequence()
                        .filter { shot ->
                            shot.brewRatio in 1.5..3.0 // Typical espresso range
                        }
                        .take(10) // Last 10 shots
                        .map { it.grinderSetting }
                        .distinct()
                        .take(3)
                        .toList() // Show max 3 previous settings

                    _previousSuccessfulSettings.value = successfulSettings
                },
                onFailure = {
                    _previousSuccessfulSettings.value = emptyList()
                }
            )
        }
    }

    /**
     * Update coffee weight in and validate using enhanced validation.
     */
    fun updateCoffeeWeightIn(value: String) {
        _coffeeWeightIn.value = value

        // Use enhanced validation with contextual tips
        val validationResult = value.validateCoffeeWeightIn()
        _coffeeWeightInError.value = validationResult.errors.firstOrNull()

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Update coffee weight out and validate using enhanced validation.
     */
    fun updateCoffeeWeightOut(value: String) {
        _coffeeWeightOut.value = value

        // Use enhanced validation with contextual tips
        val validationResult = value.validateCoffeeWeightOut()
        _coffeeWeightOutError.value = validationResult.errors.firstOrNull()

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Update grinder setting and validate using enhanced validation.
     */
    fun updateGrinderSetting(value: String) {
        _grinderSetting.value = value

        // Use enhanced validation with helpful tips
        val validationResult = value.validateGrinderSettingEnhanced()
        _grinderSettingError.value = validationResult.errors.firstOrNull()

        validateForm()
    }

    /**
     * Update notes.
     */
    fun updateNotes(value: String) {
        _notes.value = value
    }

    /**
     * Calculate and update brew ratio in real-time with enhanced warnings.
     */
    private fun calculateBrewRatio() {
        val weightIn = _coffeeWeightIn.value.toDoubleOrNull()
        val weightOut = _coffeeWeightOut.value.toDoubleOrNull()

        val ratio = recordShotUseCase.calculateBrewRatio(weightIn ?: 0.0, weightOut ?: 0.0)
        _brewRatio.value = ratio

        if (ratio != null) {
            _formattedBrewRatio.value = recordShotUseCase.formatBrewRatio(ratio)
            _isOptimalBrewRatio.value = recordShotUseCase.isTypicalBrewRatio(ratio)

            // Update brew ratio warnings using enhanced validation
            _brewRatioWarnings.value = ratio.getBrewRatioWarnings()
        } else {
            _formattedBrewRatio.value = null
            _isOptimalBrewRatio.value = false
            _brewRatioWarnings.value = emptyList()
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
     * Pause the extraction timer.
     */
    fun pauseTimer() {
        recordShotUseCase.pauseTimer()
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
                onSuccess = { shot ->
                    // Show success feedback
                    val brewRatio = shot.getFormattedBrewRatio()
                    val extractionTime = shot.getFormattedExtractionTime()
                    _successMessage.value =
                        "Shot recorded successfully! Brew ratio: $brewRatio, Time: $extractionTime"

                    // Clear draft after successful recording
                    clearDraft()

                    // Clear form after successful recording
                    clearForm()
                    _errorMessage.value = null

                    // Auto-clear success message after 5 seconds
                    viewModelScope.launch {
                        delay(5000L)
                        _successMessage.value = null
                    }
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to record shot"
                }
            )
        }
    }

    /**
     * Clear the form after successful recording, keeping current successful settings as new defaults.
     */
    private fun clearForm() {
        // Keep current successful values as new defaults
        val currentWeightIn = _coffeeWeightIn.value
        val currentWeightOut = _coffeeWeightOut.value  
        val currentGrinder = _grinderSetting.value
        
        // Update suggestions with current successful values
        _suggestedCoffeeWeightIn.value = currentWeightIn
        _suggestedCoffeeWeightOut.value = currentWeightOut
        _suggestedGrinderSetting.value = currentGrinder
        
        // Keep the successful values
        _coffeeWeightIn.value = currentWeightIn
        _coffeeWeightOut.value = currentWeightOut
        _grinderSetting.value = currentGrinder
        
        // Only clear non-persistent fields
        _notes.value = ""
        _coffeeWeightInError.value = null
        _coffeeWeightOutError.value = null
        _grinderSettingError.value = null
        _brewRatio.value = null
        _formattedBrewRatio.value = null
        _isOptimalBrewRatio.value = false
        
        // Recalculate brew ratio with kept values
        calculateBrewRatio()
        validateForm()
    }

    /**
     * Start auto-save draft functionality.
     * Saves form data every 30 seconds to prevent data loss.
     */
    private fun startAutoSaveDraft() {
        autoSaveDraftJob = viewModelScope.launch {
            while (isActive) {
                delay(30000L) // Auto-save every 30 seconds
                saveDraftIfNeeded()
            }
        }
    }

    /**
     * Save draft if form has meaningful data.
     */
    private suspend fun saveDraftIfNeeded() {
        val hasData = _coffeeWeightIn.value.isNotBlank() ||
                _coffeeWeightOut.value.isNotBlank() ||
                _grinderSetting.value.isNotBlank() ||
                _notes.value.isNotBlank() ||
                timerState.value.elapsedTimeSeconds > 0

        if (hasData) {
            saveDraft()
        }
    }

    /**
     * Save current form state as draft.
     */
    private fun saveDraft() {
        try {
            val draft = ShotDraft(
                selectedBeanId = _selectedBean.value?.id,
                coffeeWeightIn = _coffeeWeightIn.value,
                coffeeWeightOut = _coffeeWeightOut.value,
                grinderSetting = _grinderSetting.value,
                notes = _notes.value,
                elapsedTimeSeconds = timerState.value.elapsedTimeSeconds,
                timestamp = System.currentTimeMillis()
            )

            val sharedPrefs = context.getSharedPreferences("shot_drafts", Context.MODE_PRIVATE)
            val draftJson = Json.encodeToString(draft)

            sharedPrefs.edit()
                .putString("current_draft", draftJson)
                .apply()

            _isDraftSaved.value = true
            _lastDraftSaveTime.value = System.currentTimeMillis()
        } catch (exception: Exception) {
            // Silently handle draft save errors to not interrupt user workflow
        }
    }

    /**
     * Restore draft if it exists.
     */
    private fun restoreDraftIfExists() {
        viewModelScope.launch {
            try {
                val sharedPrefs = context.getSharedPreferences("shot_drafts", Context.MODE_PRIVATE)
                val draftJson = sharedPrefs.getString("current_draft", null)

                if (draftJson != null) {
                    val draft = Json.decodeFromString<ShotDraft>(draftJson)

                    // Check if draft is not too old (e.g., within last 24 hours)
                    val draftAge = System.currentTimeMillis() - draft.timestamp
                    val maxDraftAge = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

                    if (draftAge <= maxDraftAge) {
                        // Restore form data
                        _coffeeWeightIn.value = draft.coffeeWeightIn
                        _coffeeWeightOut.value = draft.coffeeWeightOut
                        _grinderSetting.value = draft.grinderSetting
                        _notes.value = draft.notes

                        // Restore selected bean if it exists
                        draft.selectedBeanId?.let { beanId ->
                            _activeBeans.value.find { it.id == beanId }?.let { bean ->
                                selectBean(bean)
                            }
                        }

                        // Restore timer if it had meaningful time
                        if (draft.elapsedTimeSeconds > 0) {
                            recordShotUseCase.resetTimer()
                            // Note: We can't restore the exact timer state, but we can indicate there was time
                        }

                        _isDraftSaved.value = true
                        _lastDraftSaveTime.value = draft.timestamp

                        // Validate form after restoration
                        validateForm()
                    } else {
                        // Draft is too old, clear it
                        clearDraft()
                    }
                } else {
                    _isDraftSaved.value = false
                    _lastDraftSaveTime.value = null
                }
            } catch (exception: Exception) {
                // Silently handle draft restore errors and clear invalid draft
                clearDraft()
            }
        }
    }

    /**
     * Clear saved draft.
     */
    private fun clearDraft() {
        try {
            val sharedPrefs = context.getSharedPreferences("shot_drafts", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .remove("current_draft")
                .apply()

            _isDraftSaved.value = false
            _lastDraftSaveTime.value = null
        } catch (exception: Exception) {
            // Silently handle draft clear errors
        }
    }

    /**
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Manually save draft (called when user navigates away or app goes to background).
     */
    fun saveDraftManually() {
        viewModelScope.launch {
            saveDraftIfNeeded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerUpdateJob?.cancel()
        autoSaveDraftJob?.cancel()

        // Save draft before clearing
        viewModelScope.launch {
            saveDraftIfNeeded()
        }
    }

}