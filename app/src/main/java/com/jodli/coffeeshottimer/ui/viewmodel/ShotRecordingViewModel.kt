package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.domain.usecase.CalculateGrindAdjustmentUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotDetailsUseCase
import com.jodli.coffeeshottimer.domain.usecase.ManageGrindRecommendationUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordTasteFeedbackUseCase
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.domain.usecase.TimerMode
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val getShotDetailsUseCase: GetShotDetailsUseCase,
    private val recordTasteFeedbackUseCase: RecordTasteFeedbackUseCase,
    private val calculateGrindAdjustmentUseCase: CalculateGrindAdjustmentUseCase,
    private val manageGrindRecommendationUseCase: ManageGrindRecommendationUseCase,
    private val beanRepository: BeanRepository,
    private val shotRepository: ShotRepository,
    private val domainErrorTranslator: DomainErrorTranslator,
    private val stringResourceProvider: StringResourceProvider,
    private val validationStringProvider: ValidationStringProvider,
    private val grinderConfigRepository: com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository,
    private val basketConfigRepository: com.jodli.coffeeshottimer.data.repository.BasketConfigRepository,
    @param:ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Create validation utils instance
    private val validationUtils = ValidationUtils(validationStringProvider)

    // Bean management state
    private val _activeBeans = MutableStateFlow<List<Bean>>(emptyList())
    val activeBeans: StateFlow<List<Bean>> = _activeBeans.asStateFlow()

    private val _selectedBean = MutableStateFlow<Bean?>(null)
    val selectedBean: StateFlow<Bean?> = _selectedBean.asStateFlow()

    // Form state
    private val _coffeeWeightIn = MutableStateFlow("")
    val coffeeWeightIn: StateFlow<String> = _coffeeWeightIn.asStateFlow()

    private val _coffeeWeightOut = MutableStateFlow("")
    val coffeeWeightOut: StateFlow<String> = _coffeeWeightOut.asStateFlow()

    private val _grinderSetting = MutableStateFlow("")
    val grinderSetting: StateFlow<String> = _grinderSetting.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // Brew ratio calculation
    private val _formattedBrewRatio = MutableStateFlow<String?>(null)
    val formattedBrewRatio: StateFlow<String?> = _formattedBrewRatio.asStateFlow()

    // Timer state (delegated to use case)
    val timerState = recordShotUseCase.timerState
    val recordingState = recordShotUseCase.recordingState

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Grinder scale range from user configuration (with fallbacks)
    private val _grinderScaleMin = MutableStateFlow(0.5f)
    val grinderScaleMin: StateFlow<Float> = _grinderScaleMin.asStateFlow()

    private val _grinderScaleMax = MutableStateFlow(20.0f)
    val grinderScaleMax: StateFlow<Float> = _grinderScaleMax.asStateFlow()

    private val _grinderStepSize = MutableStateFlow(0.5f)
    val grinderStepSize: StateFlow<Float> = _grinderStepSize.asStateFlow()

    // Basket configuration for weight ranges
    private val _basketCoffeeInMin = MutableStateFlow(5f)
    val basketCoffeeInMin: StateFlow<Float> = _basketCoffeeInMin.asStateFlow()

    private val _basketCoffeeInMax = MutableStateFlow(22f)
    val basketCoffeeInMax: StateFlow<Float> = _basketCoffeeInMax.asStateFlow()

    private val _basketCoffeeOutMin = MutableStateFlow(10f)
    private val _basketCoffeeOutMax = MutableStateFlow(55f)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    // Success feedback state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Shot recorded dialog state
    private val _showShotRecordedDialog = MutableStateFlow(false)
    val showShotRecordedDialog: StateFlow<Boolean> = _showShotRecordedDialog.asStateFlow()

    private val _recordedShotData = MutableStateFlow<RecordedShotData?>(null)
    val recordedShotData: StateFlow<RecordedShotData?> = _recordedShotData.asStateFlow()

    // Draft auto-save state
    private val _isDraftSaved = MutableStateFlow(false)
    val isDraftSaved: StateFlow<Boolean> = _isDraftSaved.asStateFlow()

    private val _lastDraftSaveTime = MutableStateFlow<Long?>(null)
    val lastDraftSaveTime: StateFlow<Long?> = _lastDraftSaveTime.asStateFlow()

    // Grind adjustment recommendation state
    private val _grindAdjustmentRecommendation = MutableStateFlow<GrindAdjustmentRecommendation?>(null)
    val grindAdjustmentRecommendation: StateFlow<GrindAdjustmentRecommendation?> = _grindAdjustmentRecommendation.asStateFlow()

    // Persistent grind recommendation state (Epic 4)
    private val _persistentRecommendation = MutableStateFlow<PersistentGrindRecommendation?>(null)
    val persistentRecommendation: StateFlow<PersistentGrindRecommendation?> = _persistentRecommendation.asStateFlow()

    // Timer mode state (automatic vs manual)
    private val _timerMode = MutableStateFlow(TimerMode.AUTOMATIC)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _manualTimeSeconds = MutableStateFlow(ValidationUtils.MANUAL_TIMER_DEFAULT_SECONDS)
    val manualTimeSeconds: StateFlow<Int> = _manualTimeSeconds.asStateFlow()

    // Timer update job
    private var timerUpdateJob: Job? = null

    // Auto-save draft job
    private var autoSaveDraftJob: Job? = null

    // Timer state preservation constants
    companion object {
        private const val TIMER_IS_RUNNING_KEY = "timer_is_running"
        private const val TIMER_START_TIME_KEY = "timer_start_time"
        private const val TIMER_ELAPSED_SECONDS_KEY = "timer_elapsed_seconds"
    }

    init {
        loadGrinderConfiguration()
        loadBasketConfiguration()
        restoreTimerState()
        startTimerUpdates()
        observeRecordingState()
        startAutoSaveDraft()
        restoreDraftIfExists()
        // Load beans and current bean - persistent recommendation will be loaded in selectBean()
        loadActiveBeans()
    }

    /**
     * Load active beans for selection.
     * Epic 4: Enhanced to handle bean deactivation and ensure proper persistent recommendation cleanup.
     * Now ensures proper sequencing by calling loadCurrentBean() after beans are loaded.
     */
    private fun loadActiveBeans() {
        viewModelScope.launch {
            _isLoading.value = true
            var isFirstLoad = true
            beanRepository.getActiveBeans().collect { result ->
                result.fold(
                    onSuccess = { beans ->
                        _activeBeans.value = beans

                        // On first load, check for current bean from repository AFTER beans are loaded
                        if (isFirstLoad) {
                            isFirstLoad = false
                            loadCurrentBeanAfterBeansLoaded()
                        }

                        // Epic 4: Check if currently selected bean was deactivated
                        val selectedBean = _selectedBean.value
                        if (selectedBean != null && !beans.any { it.id == selectedBean.id }) {
                            // Currently selected bean is no longer active, clear its recommendation
                            _persistentRecommendation.value = null

                            // Select first available bean or clear selection
                            if (beans.isNotEmpty()) {
                                selectBean(beans.first())
                            } else {
                                _selectedBean.value = null
                            }
                        } else if (!isFirstLoad) {
                            // For subsequent loads (not first load), auto-select first bean if none selected
                            if (_selectedBean.value == null && beans.isNotEmpty()) {
                                selectBean(beans.first())
                            }
                        }
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = domainErrorTranslator.translateError(exception)
                    }
                )
                _isLoading.value = false
            }
        }
    }

    /**
     * Load the current bean from repository after active beans have been loaded.
     * This prevents race conditions where current bean validation fails because
     * active beans list is still empty.
     */
    private fun loadCurrentBeanAfterBeansLoaded() {
        viewModelScope.launch {
            val result = beanRepository.getCurrentBean()
            result.fold(
                onSuccess = { currentBean ->
                    if (currentBean != null) {
                        // Active beans are now guaranteed to be loaded
                        val activeBeans = _activeBeans.value
                        val isBeanStillActive = activeBeans.any { it.id == currentBean.id }

                        if (isBeanStillActive) {
                            selectBean(currentBean)
                        } else {
                            // Current bean is no longer active, clear it from repository
                            beanRepository.clearCurrentBean()
                            // Auto-select first available bean
                            if (activeBeans.isNotEmpty()) {
                                selectBean(activeBeans.first())
                            }
                        }
                    } else {
                        // No current bean set, auto-select first available bean
                        val activeBeans = _activeBeans.value
                        if (activeBeans.isNotEmpty() && _selectedBean.value == null) {
                            selectBean(activeBeans.first())
                        }
                    }
                },
                onFailure = { exception ->
                    // Silently handle error - current bean is optional
                    // Auto-select first available bean as fallback
                    val activeBeans = _activeBeans.value
                    if (activeBeans.isNotEmpty() && _selectedBean.value == null) {
                        selectBean(activeBeans.first())
                    }
                }
            )
        }
    }

    /**
     * Load grinder scale configuration and expose min/max values with fallbacks.
     * Uses reactive Flow to automatically update when configuration changes.
     */
    private fun loadGrinderConfiguration() {
        viewModelScope.launch {
            grinderConfigRepository.getCurrentConfigFlow().collect { result ->
                result.fold(
                    onSuccess = { config ->
                        if (config != null) {
                            _grinderScaleMin.value = config.scaleMin.toFloat()
                            _grinderScaleMax.value = config.scaleMax.toFloat()
                            _grinderStepSize.value = config.stepSize.toFloat()
                        } else {
                            // Create default config if none exists
                            val createResult = grinderConfigRepository.getOrCreateDefaultConfig()
                            createResult.fold(
                                onSuccess = { defaultConfig ->
                                    _grinderScaleMin.value = defaultConfig.scaleMin.toFloat()
                                    _grinderScaleMax.value = defaultConfig.scaleMax.toFloat()
                                    _grinderStepSize.value = defaultConfig.stepSize.toFloat()
                                },
                                onFailure = {
                                    // Fallback to hardcoded defaults if repository fails
                                    val fallbackConfig = com.jodli.coffeeshottimer.data.model.GrinderConfiguration.DEFAULT_CONFIGURATION
                                    _grinderScaleMin.value = fallbackConfig.scaleMin.toFloat()
                                    _grinderScaleMax.value = fallbackConfig.scaleMax.toFloat()
                                    _grinderStepSize.value = fallbackConfig.stepSize.toFloat()
                                }
                            )
                        }
                    },
                    onFailure = {
                        // Fallback to defaults if repository fails
                        val fallbackConfig = com.jodli.coffeeshottimer.data.model.GrinderConfiguration.DEFAULT_CONFIGURATION
                        _grinderScaleMin.value = fallbackConfig.scaleMin.toFloat()
                        _grinderScaleMax.value = fallbackConfig.scaleMax.toFloat()
                        _grinderStepSize.value = fallbackConfig.stepSize.toFloat()
                    }
                )
            }
        }
    }

    /**
     * Load basket configuration for weight ranges.
     * Uses reactive Flow to automatically update when configuration changes.
     */
    private fun loadBasketConfiguration() {
        viewModelScope.launch {
            basketConfigRepository.getActiveConfigFlow().collect { result ->
                result.fold(
                    onSuccess = { config ->
                        if (config != null) {
                            _basketCoffeeInMin.value = config.coffeeInMin
                            _basketCoffeeInMax.value = config.coffeeInMax
                            _basketCoffeeOutMin.value = config.coffeeOutMin
                            _basketCoffeeOutMax.value = config.coffeeOutMax
                        } else {
                            // Use default basket configuration if none exists
                            val defaultConfig = com.jodli.coffeeshottimer.data.model.BasketConfiguration.DEFAULT
                            _basketCoffeeInMin.value = defaultConfig.coffeeInMin
                            _basketCoffeeInMax.value = defaultConfig.coffeeInMax
                            _basketCoffeeOutMin.value = defaultConfig.coffeeOutMin
                            _basketCoffeeOutMax.value = defaultConfig.coffeeOutMax
                        }
                    },
                    onFailure = {
                        // Fallback to default basket configuration
                        val defaultConfig = com.jodli.coffeeshottimer.data.model.BasketConfiguration.DEFAULT
                        _basketCoffeeInMin.value = defaultConfig.coffeeInMin
                        _basketCoffeeInMax.value = defaultConfig.coffeeInMax
                        _basketCoffeeOutMin.value = defaultConfig.coffeeOutMin
                        _basketCoffeeOutMax.value = defaultConfig.coffeeOutMax
                    }
                )
            }
        }
    }

    /**
     * Start periodic timer updates when timer is running.
     */
    private fun startTimerUpdates() {
        timerUpdateJob = viewModelScope.launch {
            var lastSaveTime = 0L
            while (isActive) {
                delay(100L) // Update every 100ms for smooth display
                if (timerState.value.isRunning) {
                    recordShotUseCase.updateTimer()

                    // Save timer state every second to avoid excessive SavedStateHandle writes
                    val currentTime = SystemClock.elapsedRealtime()
                    if (currentTime - lastSaveTime >= 1000L) {
                        saveTimerState()
                        lastSaveTime = currentTime
                    }
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
     * Epic 4: Also loads persistent grind recommendations for the bean.
     */
    fun selectBean(bean: Bean) {
        val previousBean = _selectedBean.value
        _selectedBean.value = bean

        // Epic 4: Immediately clear old persistent recommendation when switching beans
        // This prevents showing stale recommendations from the previous bean
        if (previousBean != null && previousBean.id != bean.id) {
            _persistentRecommendation.value = null
        }

        // Load suggested settings from last successful shot with this bean
        viewModelScope.launch {
            loadSuggestedSettingsForBean(bean.id, previousBean)
            // Epic 4: Load persistent grind recommendation for this bean
            loadPersistentRecommendation()

            // Epic 4: Set current bean in repository for persistence across app restarts
            setCurrentBeanInRepository(bean)
        }

        validateForm()
    }

    /**
     * Load suggested settings for a bean from the last successful shot.
     * Prioritizes most recent successful settings over historical data.
     */
    private suspend fun loadSuggestedSettingsForBean(beanId: String, previousBean: Bean?) {
        // Load suggested grinder setting
        val suggestion = recordShotUseCase.getSuggestedGrinderSetting(beanId).getOrNull()

        // Auto-fill grinder setting if:
        // 1. Current setting is empty, OR
        // 2. We're switching from a different bean and have a suggestion
        val shouldAutoFillGrinder = _grinderSetting.value.isEmpty() ||
            (previousBean != null && previousBean.id != beanId && suggestion != null)

        if (shouldAutoFillGrinder && suggestion != null) {
            updateGrinderSetting(suggestion)
        }

        val lastShot = shotRepository.getLastShotForBean(beanId).getOrNull()

        // Auto-fill coffee weights if:
        // 1. Current values are empty, OR
        // 2. We're switching from a different bean
        val shouldAutoFillWeights = (_coffeeWeightIn.value.isEmpty() && _coffeeWeightOut.value.isEmpty()) ||
            (previousBean != null && previousBean.id != beanId)

        if (shouldAutoFillWeights && lastShot != null) {
            updateCoffeeWeightIn(lastShot.coffeeWeightIn.toString())
            updateCoffeeWeightOut(lastShot.coffeeWeightOut.toString())
        }
    }

    /**
     * Set the current bean in repository for persistence across app restarts.
     * Epic 4: Ensures bean selection persists and recommendations load correctly.
     */
    private suspend fun setCurrentBeanInRepository(bean: Bean) {
        beanRepository.setCurrentBean(bean.id).fold(
            onSuccess = {
                // Bean set successfully - no action needed
            },
            onFailure = {
                // Silently handle error - bean selection still works locally
            }
        )
    }

    /**
     * Update coffee weight in.
     */
    fun updateCoffeeWeightIn(value: String) {
        _coffeeWeightIn.value = value

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Update coffee weight out.
     */
    fun updateCoffeeWeightOut(value: String) {
        _coffeeWeightOut.value = value

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Nudge coffee weight out by whole grams, kept inside the basket's range.
     * Used by the -/+ buttons when reading the yield off the scale.
     */
    fun stepCoffeeWeightOut(deltaGrams: Int) {
        val current = _coffeeWeightOut.value.replace(',', '.').toDoubleOrNull()
            ?: _basketCoffeeOutMin.value.toDouble()
        val next = (current + deltaGrams).coerceIn(
            _basketCoffeeOutMin.value.toDouble(),
            _basketCoffeeOutMax.value.toDouble()
        )
        updateCoffeeWeightOut(next.toInt().toString())
    }

    /**
     * Update grinder setting.
     */
    fun updateGrinderSetting(value: String) {
        _grinderSetting.value = value

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

        if (ratio != null) {
            _formattedBrewRatio.value = recordShotUseCase.formatBrewRatio(ratio)
        } else {
            _formattedBrewRatio.value = null
        }
    }

    /**
     * Validate the entire form.
     */
    private fun validateForm() {
        val hasValidBean = _selectedBean.value != null
        val hasValidWeights = _coffeeWeightIn.value.isNotBlank() &&
            _coffeeWeightOut.value.isNotBlank()
        val hasValidGrinder = _grinderSetting.value.isNotBlank()

        // Use effective extraction time based on current mode
        val effectiveTime = getEffectiveExtractionTime()
        val hasValidTimer = effectiveTime >= ValidationUtils.MIN_EXTRACTION_TIME

        val isValid = hasValidBean && hasValidWeights && hasValidGrinder && hasValidTimer

        _isFormValid.value = isValid
    }

    /**
     * Start the extraction timer.
     */
    fun startTimer() {
        recordShotUseCase.startTimer()
        saveTimerState() // Save state after starting timer
        validateForm() // Revalidate as timer state affects form validity
    }

    /**
     * Pause the extraction timer.
     */
    fun pauseTimer() {
        recordShotUseCase.pauseTimer()
        saveTimerState() // Save state after pausing timer
        validateForm()
    }

    /**
     * Reset the extraction timer.
     */
    fun resetTimer() {
        recordShotUseCase.resetTimer()
        saveTimerState() // Save state after resetting timer
        validateForm()
    }

    /**
     * Toggle between automatic and manual timer modes.
     * Only works when timer is stopped to prevent mid-extraction mode changes.
     */
    fun toggleTimerMode() {
        // Only allow toggle when timer is not running
        if (!timerState.value.isRunning) {
            _timerMode.value = when (_timerMode.value) {
                TimerMode.AUTOMATIC -> {
                    // Transfer current elapsed time to manual mode
                    val currentSeconds = timerState.value.elapsedTimeSeconds
                    if (currentSeconds > 0) {
                        setManualTime(currentSeconds)
                    }
                    TimerMode.MANUAL
                }
                TimerMode.MANUAL -> TimerMode.AUTOMATIC
            }
            validateForm() // Revalidate form after mode change
        }
    }

    /**
     * Set manual time with validation.
     * Clamps value to valid range (5-60 seconds).
     */
    fun setManualTime(timeSeconds: Int) {
        val clampedTime = timeSeconds.coerceIn(
            ValidationUtils.MANUAL_TIMER_MIN_SECONDS,
            ValidationUtils.MANUAL_TIMER_MAX_SECONDS
        )
        _manualTimeSeconds.value = clampedTime
        validateForm() // Revalidate form after manual time change
    }

    /**
     * Get the effective extraction time based on current mode.
     * Returns automatic timer value in automatic mode, manual time in manual mode.
     */
    fun getEffectiveExtractionTime(): Int {
        return when (_timerMode.value) {
            TimerMode.AUTOMATIC -> timerState.value.elapsedTimeSeconds
            TimerMode.MANUAL -> _manualTimeSeconds.value
        }
    }

    /**
     * Record the current shot with validation.
     * Epic 4: Enhanced with recommendation tracking to learn user behavior.
     */
    fun recordShot() {
        val bean = _selectedBean.value
        val weightIn = _coffeeWeightIn.value.toDoubleOrNull()
        val weightOut = _coffeeWeightOut.value.toDoubleOrNull()
        val grinder = _grinderSetting.value
        val shotNotes = _notes.value

        if (bean == null || weightIn == null || weightOut == null || grinder.isBlank()) {
            _errorMessage.value = stringResourceProvider.getString(R.string.validation_fill_required)
            return
        }

        // Use effective extraction time based on current mode
        val effectiveExtractionTime = getEffectiveExtractionTime()

        // Check minimum extraction time
        if (effectiveExtractionTime < ValidationUtils.MIN_EXTRACTION_TIME) {
            _errorMessage.value = context.getString(R.string.error_extraction_time_minimum, ValidationUtils.MIN_EXTRACTION_TIME)
            return
        }

        viewModelScope.launch {
            // Epic 4: Track if user followed persistent recommendation before recording
            val currentRecommendation = _persistentRecommendation.value
            val followedRecommendation = checkIfRecommendationFollowed(grinder, currentRecommendation)

            // Capture extraction time BEFORE recording (use effective time based on mode)
            val extractionTimeSeconds = effectiveExtractionTime

            // Validate parameters first
            val validationResult = recordShotUseCase.validateShotParameters(
                beanId = bean.id,
                coffeeWeightIn = weightIn,
                coffeeWeightOut = weightOut,
                extractionTimeSeconds = extractionTimeSeconds,
                grinderSetting = grinder,
                notes = shotNotes
            )

            if (!validationResult.isValid) {
                _errorMessage.value = validationResult.errors.joinToString(", ")
                return@launch
            }

            // Record the shot with effective extraction time (automatic or manual)
            val result = recordShotUseCase.recordShot(
                beanId = bean.id,
                coffeeWeightIn = weightIn,
                coffeeWeightOut = weightOut,
                extractionTimeSeconds = extractionTimeSeconds,
                grinderSetting = grinder,
                notes = shotNotes
            )

            result.fold(
                onSuccess = { shot ->
                    // Epic 4: Handle recommendation follow-through tracking
                    if (followedRecommendation && currentRecommendation != null) {
                        // User followed the recommendation, mark it as followed
                        manageGrindRecommendationUseCase.markRecommendationFollowed(currentRecommendation.beanId)
                    }

                    // Epic 4: Log analytics for future learning features
                    currentRecommendation?.let { recommendation ->
                        logRecommendationAnalytics(
                            beanId = recommendation.beanId,
                            recommendationFollowed = followedRecommendation,
                            actualGrindSetting = grinder,
                            recommendedGrindSetting = recommendation.suggestedGrindSetting,
                            extractionTimeSeconds = extractionTimeSeconds
                        )
                    }

                    // Epic 4: Clear old recommendation since we're recording a new shot
                    // This clears the previous recommendation to make room for the new one
                    _persistentRecommendation.value = null

                    // Clear draft after successful recording
                    clearDraft()

                    // Clear form after successful recording
                    clearForm()
                    _errorMessage.value = null

                    // Load shot details with recommendations and show dialog
                    // Pass the captured extraction time and recorded grinder setting
                    loadShotDetailsAndShowDialog(
                        shot.id,
                        shot.getFormattedBrewRatio(),
                        shot.getFormattedExtractionTime(),
                        extractionTimeSeconds,
                        grinder
                    )
                },
                onFailure = { exception ->
                    _errorMessage.value = domainErrorTranslator.translateError(exception)
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

        // Keep the successful values
        _coffeeWeightIn.value = currentWeightIn
        _coffeeWeightOut.value = currentWeightOut
        _grinderSetting.value = currentGrinder

        // Only clear non-persistent fields
        _notes.value = ""
        _formattedBrewRatio.value = null
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
                        calculateBrewRatio()

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
     * Load shot details with recommendations and show the success dialog.
     */
    private suspend fun loadShotDetailsAndShowDialog(
        shotId: String,
        brewRatio: String,
        extractionTime: String,
        extractionTimeSeconds: Int,
        grinderSetting: String
    ) {
        // Pre-compute the suggested taste using shared utility
        val suggestedTaste = com.jodli.coffeeshottimer.ui.components.TasteUtils.getTasteRecommendation(
            extractionTimeSeconds
        )

        getShotDetailsUseCase.getShotDetails(shotId).fold(
            onSuccess = { shotDetails ->
                _recordedShotData.value = RecordedShotData(
                    brewRatio = brewRatio,
                    extractionTime = extractionTime,
                    extractionTimeSeconds = extractionTimeSeconds,
                    recommendations = shotDetails.analysis.recommendations,
                    shotId = shotId,
                    suggestedTaste = suggestedTaste,
                    grinderSetting = grinderSetting
                )
                _showShotRecordedDialog.value = true

                // Calculate initial grind adjustment based on extraction time alone
                // This ensures beginners always get recommendations even without selecting taste
                calculateInitialGrindAdjustment(extractionTimeSeconds, grinderSetting)
            },
            onFailure = {
                // Fallback to simple success message if we can't load recommendations
                _successMessage.value = stringResourceProvider.getString(
                    R.string.text_record_successfully,
                    brewRatio,
                    extractionTime
                )
            }
        )
    }

    /**
     * Hide the shot recorded dialog.
     */
    fun hideShotRecordedDialog() {
        _showShotRecordedDialog.value = false
        _recordedShotData.value = null
    }

    /**
     * Record taste feedback for the recently recorded shot.
     * This saves the taste to the database and updates persistent recommendations with taste data.
     * Epic 4: Enhanced to update persistent recommendations with taste feedback.
     */
    fun recordTasteFeedback(
        shotId: String,
        tastePrimary: TastePrimary,
        tasteSecondary: TasteSecondary? = null
    ) {
        viewModelScope.launch {
            recordTasteFeedbackUseCase(
                shotId = shotId,
                tastePrimary = tastePrimary,
                tasteSecondary = tasteSecondary
            ).fold(
                onSuccess = {
                    // Taste feedback successfully saved to database

                    // Epic 4: Update persistent recommendation with taste feedback
                    val recordedData = _recordedShotData.value
                    val currentBean = _selectedBean.value
                    if (recordedData != null && currentBean != null) {
                        // Calculate updated recommendation with taste feedback
                        calculateGrindAdjustmentUseCase.calculateAdjustment(
                            currentGrindSetting = recordedData.grinderSetting,
                            extractionTimeSeconds = recordedData.extractionTimeSeconds,
                            tasteFeedback = tastePrimary
                        ).fold(
                            onSuccess = { updatedRecommendation ->
                                // Create updated shot object with taste feedback
                                val updatedShot = com.jodli.coffeeshottimer.data.model.Shot(
                                    id = shotId,
                                    beanId = currentBean.id,
                                    coffeeWeightIn = _coffeeWeightIn.value.toDoubleOrNull() ?: 0.0,
                                    coffeeWeightOut = _coffeeWeightOut.value.toDoubleOrNull() ?: 0.0,
                                    extractionTimeSeconds = recordedData.extractionTimeSeconds,
                                    grinderSetting = recordedData.grinderSetting,
                                    notes = _notes.value,
                                    tastePrimary = tastePrimary,
                                    tasteSecondary = tasteSecondary
                                )
                                updatePersistentRecommendationWithTaste(updatedRecommendation, updatedShot)
                            },
                            onFailure = {
                                // Silently handle error - don't block taste feedback flow
                            }
                        )
                    }
                },
                onFailure = { exception ->
                    // Handle error silently or show a non-blocking message
                    // We don't want to block the user flow for taste feedback failures
                }
            )
        }
    }

    /**
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Clear error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Calculate initial grind adjustment based on extraction time only.
     * This is called immediately after recording a shot to provide timing-based recommendations.
     * Epic 4: Also saves persistent recommendation immediately.
     */
    private fun calculateInitialGrindAdjustment(extractionTimeSeconds: Int, grinderSetting: String) {
        viewModelScope.launch {
            calculateGrindAdjustmentUseCase.calculateAdjustment(
                currentGrindSetting = grinderSetting,
                extractionTimeSeconds = extractionTimeSeconds,
                tasteFeedback = null // No taste feedback yet
            ).fold(
                onSuccess = { recommendation ->
                    _grindAdjustmentRecommendation.value = recommendation

                    // Epic 4: Also save persistent recommendation immediately
                    val recordedData = _recordedShotData.value
                    val currentBean = _selectedBean.value
                    if (recordedData != null && currentBean != null) {
                        // Get the actual shot from database instead of creating a fake one
                        viewModelScope.launch {
                            shotRepository.getShotById(recordedData.shotId).fold(
                                onSuccess = { actualShot ->
                                    if (actualShot != null) {
                                        savePersistentRecommendation(recommendation, actualShot)
                                    }
                                },
                                onFailure = {
                                    // If we can't get the shot from DB, create a minimal one with required fields
                                    val fallbackShot = com.jodli.coffeeshottimer.data.model.Shot(
                                        id = recordedData.shotId,
                                        beanId = currentBean.id,
                                        coffeeWeightIn = _coffeeWeightIn.value.toDoubleOrNull() ?: 0.0,
                                        coffeeWeightOut = _coffeeWeightOut.value.toDoubleOrNull() ?: 0.0,
                                        extractionTimeSeconds = extractionTimeSeconds,
                                        grinderSetting = grinderSetting,
                                        notes = _notes.value,
                                        timestamp = java.time.LocalDateTime.now(),
                                        tastePrimary = null,
                                        tasteSecondary = null
                                    )
                                    savePersistentRecommendation(recommendation, fallbackShot)
                                }
                            )
                        }
                    }
                },
                onFailure = {
                    // Don't show recommendation on error, but don't block the dialog
                    _grindAdjustmentRecommendation.value = null
                }
            )
        }
    }

    /**
     * Calculate grind adjustment recommendation for given taste (reactive calculation).
     * This is called when user changes taste selection in the dialog.
     */
    fun calculateGrindAdjustmentForTaste(tasteFeedback: TastePrimary?) {
        viewModelScope.launch {
            val recordedData = _recordedShotData.value
            if (recordedData != null) {
                // Always calculate - even with null taste (falls back to timing-based)
                calculateGrindAdjustmentUseCase.calculateAdjustment(
                    currentGrindSetting = recordedData.grinderSetting,
                    extractionTimeSeconds = recordedData.extractionTimeSeconds,
                    tasteFeedback = tasteFeedback
                ).fold(
                    onSuccess = { recommendation ->
                        _grindAdjustmentRecommendation.value = recommendation
                    },
                    onFailure = {
                        // Keep existing recommendation on error
                        // Don't clear it since we always want to show something
                    }
                )
            }
        }
    }

    /**
     * Manually save draft (called when user navigates away or app goes to background).
     */
    fun saveDraftManually() {
        viewModelScope.launch {
            saveDraftIfNeeded()
        }
    }

    /**
     * Restore timer state from SavedStateHandle after configuration changes.
     */
    private fun restoreTimerState() {
        val isRunning = savedStateHandle.get<Boolean>(TIMER_IS_RUNNING_KEY) ?: false
        val startTime = savedStateHandle.get<Long>(TIMER_START_TIME_KEY) ?: 0L
        val elapsedSeconds = savedStateHandle.get<Int>(TIMER_ELAPSED_SECONDS_KEY) ?: 0

        if (isRunning || elapsedSeconds > 0) {
            // Restore timer state in the use case
            recordShotUseCase.restoreTimerState(
                isRunning = isRunning,
                startTime = startTime,
                elapsedTimeSeconds = elapsedSeconds
            )
        }
    }

    /**
     * Save timer state to SavedStateHandle for configuration change preservation.
     */
    private fun saveTimerState() {
        val currentTimerState = timerState.value
        savedStateHandle[TIMER_IS_RUNNING_KEY] = currentTimerState.isRunning
        savedStateHandle[TIMER_START_TIME_KEY] = currentTimerState.startTime
        savedStateHandle[TIMER_ELAPSED_SECONDS_KEY] = currentTimerState.elapsedTimeSeconds
    }

    // === PERSISTENT GRIND RECOMMENDATION METHODS (Epic 4) ===

    /**
     * Load the persistent grind recommendation for the currently active bean.
     * Called when the ViewModel is initialized or when the bean changes.
     * Epic 4: Enhanced to handle edge cases and ensure proper bean-specific recommendations.
     */
    private fun loadPersistentRecommendation() {
        viewModelScope.launch {
            val bean = _selectedBean.value
            if (bean != null) {
                manageGrindRecommendationUseCase.getRecommendation(bean.id).fold(
                    onSuccess = { recommendation ->
                        // Only set recommendation if it matches the currently selected bean
                        // This prevents race conditions when switching beans quickly
                        if (recommendation?.beanId == bean.id) {
                            _persistentRecommendation.value = recommendation
                        } else {
                            // Recommendation is for a different bean, clear it
                            _persistentRecommendation.value = null
                        }
                    },
                    onFailure = {
                        // Silently handle error - recommendation is optional
                        _persistentRecommendation.value = null
                    }
                )
            } else {
                // No bean selected, clear any existing recommendation
                _persistentRecommendation.value = null
            }
        }
    }

    /**
     * Dismiss the persistent grind recommendation.
     * Clears the recommendation from storage and UI.
     */
    fun dismissPersistentRecommendation() {
        val recommendation = _persistentRecommendation.value
        if (recommendation != null) {
            viewModelScope.launch {
                manageGrindRecommendationUseCase.clearRecommendation(recommendation.beanId)
                _persistentRecommendation.value = null
            }
        }
    }

    /**
     * Refresh the current bean selection from the repository.
     * Called when returning from other screens (e.g., BeanManagementScreen)
     * to ensure the selected bean is up-to-date with any changes made in other screens.
     */
    fun refreshCurrentBean() {
        viewModelScope.launch {
            val result = beanRepository.getCurrentBean()
            result.fold(
                onSuccess = { currentBean ->
                    if (currentBean != null) {
                        // Check if bean is still active
                        val activeBeans = _activeBeans.value
                        val isBeanStillActive = activeBeans.any { it.id == currentBean.id }

                        if (isBeanStillActive) {
                            // Only update if different from currently selected bean
                            if (_selectedBean.value?.id != currentBean.id) {
                                selectBean(currentBean)
                            }
                        } else {
                            // Current bean is no longer active, clear it from repository
                            beanRepository.clearCurrentBean()
                            // Auto-select first available bean
                            if (activeBeans.isNotEmpty()) {
                                selectBean(activeBeans.first())
                            }
                        }
                    }
                },
                onFailure = {
                    // Silently handle error - current bean is optional
                }
            )
        }
    }

    /**
     * Check if the current grinder setting matches the persistent recommendation within tolerance.
     * Epic 4: Used for tracking recommendation follow-through rates.
     */
    private fun checkIfRecommendationFollowed(
        currentGrinderSetting: String,
        recommendation: PersistentGrindRecommendation?
    ): Boolean {
        if (recommendation == null) return false

        val currentSetting = currentGrinderSetting.toDoubleOrNull() ?: return false
        val recommendedSetting = recommendation.suggestedGrindSetting.toDoubleOrNull() ?: return false

        // Define tolerance for "following" the recommendation
        // Allow ±0.5 step size difference to account for user adjustments
        val tolerance = _grinderStepSize.value // Use the actual step size from configuration
        val difference = kotlin.math.abs(currentSetting - recommendedSetting)

        return difference <= tolerance
    }

    /**
     * Log recommendation follow-through analytics.
     * Epic 4: Track user behavior for future learning features.
     */
    private fun logRecommendationAnalytics(
        beanId: String,
        recommendationFollowed: Boolean,
        actualGrindSetting: String,
        recommendedGrindSetting: String,
        extractionTimeSeconds: Int
    ) {
        // Future enhancement: Send analytics to a learning system
        // For now, we can log this information for debugging or future use
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "RecommendationTracking",
                "Bean: $beanId, Followed: $recommendationFollowed, " +
                    "Actual: $actualGrindSetting, Recommended: $recommendedGrindSetting, " +
                    "Time: ${extractionTimeSeconds}s"
            )
        }

        // Future: Could aggregate statistics:
        // - Follow-through rate per bean
        // - Accuracy of recommendations (based on subsequent taste feedback)
        // - Learning patterns (users tend to adjust +0.5 from recommendations)
    }

    /**
     * Save a persistent grind recommendation after recording a shot.
     * This is called from recordShot() to always save recommendations.
     */
    private fun savePersistentRecommendation(
        recommendation: GrindAdjustmentRecommendation,
        recordedShot: com.jodli.coffeeshottimer.data.model.Shot
    ) {
        viewModelScope.launch {
            manageGrindRecommendationUseCase.saveRecommendation(
                beanId = recordedShot.beanId,
                recommendation = recommendation,
                lastShot = recordedShot
            ).fold(
                onSuccess = { persistentRecommendation ->
                    // Update UI with new persistent recommendation
                    _persistentRecommendation.value = persistentRecommendation
                },
                onFailure = {
                    // Silently handle error - don't block shot recording flow
                }
            )
        }
    }

    /**
     * Update a persistent recommendation when taste feedback is added.
     * Called from recordTasteFeedback() to enhance recommendations with taste data.
     */
    private fun updatePersistentRecommendationWithTaste(
        updatedRecommendation: GrindAdjustmentRecommendation,
        updatedShot: com.jodli.coffeeshottimer.data.model.Shot
    ) {
        viewModelScope.launch {
            manageGrindRecommendationUseCase.updateRecommendationWithTaste(
                beanId = updatedShot.beanId,
                updatedRecommendation = updatedRecommendation,
                updatedShot = updatedShot
            ).fold(
                onSuccess = { updatedPersistent ->
                    // Update UI with enhanced recommendation
                    _persistentRecommendation.value = updatedPersistent
                },
                onFailure = {
                    // Silently handle error - don't block taste feedback flow
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Save timer state before clearing
        saveTimerState()

        timerUpdateJob?.cancel()
        autoSaveDraftJob?.cancel()

        // Save draft before clearing
        viewModelScope.launch {
            saveDraftIfNeeded()
        }
    }
}

/**
 * Data class for recorded shot information to display in success dialog.
 */
data class RecordedShotData(
    val brewRatio: String,
    val extractionTime: String,
    val extractionTimeSeconds: Int, // Added for taste preselection
    val recommendations: List<ShotRecommendation>,
    val shotId: String,
    val suggestedTaste: TastePrimary?, // Pre-computed suggested taste
    val grinderSetting: String // Grinder setting used for the recorded shot
)
