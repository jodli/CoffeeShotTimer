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
import com.jodli.coffeeshottimer.domain.usecase.GetTastePreselectionUseCase
import com.jodli.coffeeshottimer.domain.usecase.ManageGrindRecommendationUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordTasteFeedbackUseCase
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import com.jodli.coffeeshottimer.ui.validation.getBrewRatioWarnings
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
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
    private val getTastePreselectionUseCase: GetTastePreselectionUseCase,
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
    val basketCoffeeOutMin: StateFlow<Float> = _basketCoffeeOutMin.asStateFlow()
    
    private val _basketCoffeeOutMax = MutableStateFlow(55f)
    val basketCoffeeOutMax: StateFlow<Float> = _basketCoffeeOutMax.asStateFlow()

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

    // Timer validation feedback state
    private val _showTimerValidation = MutableStateFlow(false)
    val showTimerValidation: StateFlow<Boolean> = _showTimerValidation.asStateFlow()

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
        // Note: loadCurrentBean() will be called from loadActiveBeans() once the active beans are loaded
        loadActiveBeans()
        // Note: loadPersistentRecommendation() is now called from selectBean() when a bean is selected
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
                        val previousBeans = _activeBeans.value
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
     * Load the current bean from repository if one is set.
     * Implements requirement 3.2 for connecting bean selection between screens.
     * Epic 4: Enhanced to verify current bean is still active before selecting it.
     */
    private fun loadCurrentBean() {
        viewModelScope.launch {
            val result = beanRepository.getCurrentBean()
            result.fold(
                onSuccess = { currentBean ->
                    if (currentBean != null) {
                        // Verify the current bean is still in the active beans list
                        val activeBeans = _activeBeans.value
                        val isBeanStillActive = activeBeans.any { it.id == currentBean.id }
                        
                        if (isBeanStillActive) {
                            selectBean(currentBean)
                        } else {
                            // Current bean is no longer active, clear it from repository
                            beanRepository.clearCurrentBean()
                        }
                    }
                },
                onFailure = { exception ->
                    // Silently handle error - current bean is optional
                }
            )
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
     * Load basket configuration for weight slider ranges.
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
            // Load previous successful grinder settings for visual indicators
            loadPreviousSuccessfulSettings(bean.id)
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
     * Load previous successful grinder settings for a bean to show as visual indicators.
     */
    private suspend fun loadPreviousSuccessfulSettings(beanId: String) {
        // Use first() instead of collect() to avoid infinite flow collection
        val result = shotRepository.getShotsByBean(beanId).first()
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

    /**
     * Update coffee weight in.
     * No validation needed as sliders constrain values to basket configuration ranges.
     */
    fun updateCoffeeWeightIn(value: String) {
        _coffeeWeightIn.value = value
        _coffeeWeightInError.value = null // Clear any previous errors

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Update coffee weight out.
     * No validation needed as sliders constrain values to basket configuration ranges.
     */
    fun updateCoffeeWeightOut(value: String) {
        _coffeeWeightOut.value = value
        _coffeeWeightOutError.value = null // Clear any previous errors

        calculateBrewRatio()
        validateForm()
    }

    /**
     * Update grinder setting.
     * No validation needed as slider constrains values to grinder configuration ranges.
     */
    fun updateGrinderSetting(value: String) {
        _grinderSetting.value = value
        _grinderSettingError.value = null // Clear any previous errors

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
            _brewRatioWarnings.value = ratio.getBrewRatioWarnings(validationUtils)
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
        val hasValidBean = _selectedBean.value != null
        val hasValidWeights = _coffeeWeightIn.value.isNotBlank() &&
                _coffeeWeightOut.value.isNotBlank() &&
                _coffeeWeightInError.value == null &&
                _coffeeWeightOutError.value == null
        val hasValidGrinder = _grinderSetting.value.isNotBlank() &&
                _grinderSettingError.value == null
        val hasValidTimer = timerState.value.elapsedTimeSeconds >= ValidationUtils.MIN_EXTRACTION_TIME

        val isValid = hasValidBean && hasValidWeights && hasValidGrinder && hasValidTimer

        _isFormValid.value = isValid

        // Show timer validation feedback if everything else is valid but timer is insufficient
        val shouldShowTimerValidation = hasValidBean && hasValidWeights && hasValidGrinder &&
                !hasValidTimer && timerState.value.elapsedTimeSeconds > 0

        if (shouldShowTimerValidation && !_showTimerValidation.value) {
            _showTimerValidation.value = true
        } else if (!shouldShowTimerValidation && _showTimerValidation.value) {
            _showTimerValidation.value = false
        }
    }

    /**
     * Start the extraction timer.
     */
    fun startTimer() {
        recordShotUseCase.startTimer()
        saveTimerState() // Save state after starting timer
        _showTimerValidation.value = false // Clear validation feedback when timer starts
        validateForm() // Revalidate as timer state affects form validity
    }

    /**
     * Pause the extraction timer.
     */
    fun pauseTimer() {
        recordShotUseCase.pauseTimer()
        saveTimerState() // Save state after pausing timer
        _showTimerValidation.value = false // Clear validation feedback when timer stops
        validateForm()
    }

    /**
     * Reset the extraction timer.
     */
    fun resetTimer() {
        recordShotUseCase.resetTimer()
        saveTimerState() // Save state after resetting timer
        _showTimerValidation.value = false // Clear validation feedback when timer resets
        validateForm()
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

        // Check minimum extraction time and show visual feedback
        if (timerState.value.elapsedTimeSeconds < ValidationUtils.MIN_EXTRACTION_TIME) {
            _showTimerValidation.value = true
            _errorMessage.value = context.getString(R.string.error_extraction_time_minimum, ValidationUtils.MIN_EXTRACTION_TIME)

            // Auto-hide validation feedback after 3 seconds
            viewModelScope.launch {
                delay(3000L)
                _showTimerValidation.value = false
            }
            return
        }

        viewModelScope.launch {
            // Epic 4: Track if user followed persistent recommendation before recording
            val currentRecommendation = _persistentRecommendation.value
            val followedRecommendation = checkIfRecommendationFollowed(grinder, currentRecommendation)
            
            // Capture extraction time BEFORE recording (it might be reset after recording)
            val extractionTimeSeconds = timerState.value.elapsedTimeSeconds
            
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
                    loadShotDetailsAndShowDialog(shot.id, shot.getFormattedBrewRatio(), shot.getFormattedExtractionTime(), extractionTimeSeconds, grinder)
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
     * Load shot details with recommendations and show the success dialog.
     */
    private suspend fun loadShotDetailsAndShowDialog(shotId: String, brewRatio: String, extractionTime: String, extractionTimeSeconds: Int, grinderSetting: String) {
        // Pre-compute the suggested taste using shared utility
        val suggestedTaste = com.jodli.coffeeshottimer.ui.components.TasteUtils.getTasteRecommendation(extractionTimeSeconds)
        
        
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
     * Get taste preselection based on extraction time.
     */
    fun getTastePreselectionFor(extractionTimeSeconds: Int): TastePrimary? {
        return com.jodli.coffeeshottimer.ui.components.TasteUtils.getTasteRecommendation(extractionTimeSeconds)
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
     * Clear timer validation feedback.
     */
    fun clearTimerValidation() {
        _showTimerValidation.value = false
    }

    /**
     * Apply grind adjustment recommendation.
     * Updates the current grinder setting with the suggested value.
     */
    fun applyGrindAdjustment() {
        val recommendation = _grindAdjustmentRecommendation.value
        if (recommendation?.hasAdjustment() == true) {
            _suggestedGrinderSetting.value = recommendation.suggestedGrindSetting
            _grinderSetting.value = recommendation.suggestedGrindSetting
            _grindAdjustmentRecommendation.value = null
            
            // Validate form with new grinder setting
            validateForm()
        }
    }

    /**
     * Dismiss grind adjustment recommendation.
     */
    fun dismissGrindAdjustment() {
        _grindAdjustmentRecommendation.value = null
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
     * Trigger timer validation feedback (useful for testing or manual validation).
     */
    fun triggerTimerValidation() {
        if (timerState.value.elapsedTimeSeconds < ValidationUtils.MIN_EXTRACTION_TIME) {
            _showTimerValidation.value = true

            // Auto-hide validation feedback after 3 seconds
            viewModelScope.launch {
                delay(3000L)
                _showTimerValidation.value = false
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
     * Apply the persistent grind recommendation.
     * Updates the current grinder setting and marks the recommendation as followed.
     * Epic 4: Enhanced with analytics tracking.
     */
    fun applyPersistentRecommendation() {
        val recommendation = _persistentRecommendation.value
        if (recommendation != null) {
            val oldSetting = _grinderSetting.value
            
            // Apply the grinder setting
            _grinderSetting.value = recommendation.suggestedGrindSetting
            _suggestedGrinderSetting.value = recommendation.suggestedGrindSetting
            
            // Mark as followed
            viewModelScope.launch {
                manageGrindRecommendationUseCase.markRecommendationFollowed(recommendation.beanId)
                // Update the local state to reflect it was followed
                _persistentRecommendation.value = recommendation.markAsFollowed()
            }
            
            // Epic 4: Log that user explicitly applied the recommendation
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    "RecommendationTracking",
                    "User applied recommendation: Bean=${recommendation.beanId}, "
                            + "From=$oldSetting To=${recommendation.suggestedGrindSetting}"
                )
            }
            
            // Validate form with new grinder setting
            validateForm()
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
     * Clear the persistent recommendation state immediately (e.g., when switching beans).
     * Epic 4: Used internally to prevent stale recommendation display during bean transitions.
     */
    private fun clearPersistentRecommendationState() {
        _persistentRecommendation.value = null
    }
    
    /**
     * Refresh persistent recommendations for the currently selected bean.
     * Epic 4: Called when returning from other screens to ensure recommendations are up-to-date.
     */
    fun refreshPersistentRecommendation() {
        loadPersistentRecommendation()
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
        val tolerance = _grinderStepSize.value  // Use the actual step size from configuration
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
                "Bean: $beanId, Followed: $recommendationFollowed, "
                        + "Actual: $actualGrindSetting, Recommended: $recommendedGrindSetting, "
                        + "Time: ${extractionTimeSeconds}s"
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
    val extractionTimeSeconds: Int,  // Added for taste preselection
    val recommendations: List<ShotRecommendation>,
    val shotId: String,
    val suggestedTaste: TastePrimary?, // Pre-computed suggested taste
    val grinderSetting: String // Grinder setting used for the recorded shot
)
