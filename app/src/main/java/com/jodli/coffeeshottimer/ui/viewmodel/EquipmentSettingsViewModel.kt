package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EquipmentSettingsViewModel @Inject constructor(
    private val grinderConfigRepository: GrinderConfigRepository,
    private val errorTranslator: DomainErrorTranslator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipmentSettingsUiState())
    val uiState: StateFlow<EquipmentSettingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = grinderConfigRepository.getCurrentConfig()
            result.fold(
                onSuccess = { config ->
                    if (config != null) {
                        _uiState.value = _uiState.value.copy(
                            scaleMin = config.scaleMin.toString(),
                            scaleMax = config.scaleMax.toString(),
                            isLoading = false
                        )
                    } else {
                        // If no config exists yet, create or use defaults
                        val createResult = grinderConfigRepository.getOrCreateDefaultConfig()
                        createResult.fold(
                            onSuccess = { defaultConfig ->
                                _uiState.value = _uiState.value.copy(
                                    scaleMin = defaultConfig.scaleMin.toString(),
                                    scaleMax = defaultConfig.scaleMax.toString(),
                                    isLoading = false
                                )
                            },
                            onFailure = {
                                _uiState.value = _uiState.value.copy(isLoading = false)
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
            validateForm()
        }
    }

    fun updateScaleMin(value: String) {
        _uiState.value = _uiState.value.copy(scaleMin = value, minError = null, generalError = null)
        validateForm()
    }

    fun updateScaleMax(value: String) {
        _uiState.value = _uiState.value.copy(scaleMax = value, maxError = null, generalError = null)
        validateForm()
    }

    fun setPreset(min: Int, max: Int) {
        _uiState.value = _uiState.value.copy(
            scaleMin = min.toString(),
            scaleMax = max.toString(),
            minError = null,
            maxError = null,
            generalError = null
        )
        validateForm()
    }

    private fun validateForm() {
        val current = _uiState.value
        var minError: String? = null
        var maxError: String? = null
        var generalError: String? = null
        val minVal = current.scaleMin.toIntOrNull()
        val maxVal = current.scaleMax.toIntOrNull()

        if (current.scaleMin.isNotBlank() && minVal == null) minError = getValidationNumberError()
        if (current.scaleMax.isNotBlank() && maxVal == null) maxError = getValidationNumberError()

        if (minVal != null && maxVal != null) {
            val config = GrinderConfiguration(scaleMin = minVal, scaleMax = maxVal)
            val validation = config.validate()
            if (!validation.isValid) generalError = validation.errors.firstOrNull()
        }

        _uiState.value = current.copy(
            minError = minError,
            maxError = maxError,
            generalError = generalError,
            isFormValid = minError == null && maxError == null && generalError == null && minVal != null && maxVal != null
        )
    }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        val minVal = current.scaleMin.toIntOrNull()
        val maxVal = current.scaleMax.toIntOrNull()
        if (!current.isFormValid || minVal == null || maxVal == null) {
            _uiState.value = current.copy(generalError = getFixValidationErrorsMessage())
            return
        }
        val config = GrinderConfiguration(scaleMin = minVal, scaleMax = maxVal)
        _uiState.value = current.copy(isLoading = true)
        viewModelScope.launch {
            val result = grinderConfigRepository.saveConfig(config)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, generalError = getFailedToSaveConfigurationMessage())
                }
            )
        }
    }

    fun getValidationSuggestion(error: String?): String {
        if (error == null) return ""
        return when {
            error.contains("must be less than") -> getSuggestionIncreaseMaxOrDecreaseMin()
            error.contains("cannot be negative") -> getSuggestionGrinderScalesStartAtZeroOrOne()
            error.contains("cannot exceed 1000") -> getSuggestionMostGrinderScalesDontGoAbove100()
            error.contains("at least 3 steps") -> getSuggestionRangeOfAtLeast3Steps()
            error.contains("cannot exceed 100 steps") -> getSuggestionSmallerRangeIsEasier()
            error.contains("valid number") -> getSuggestionEnterWholeNumbersOnly()
            else -> ""
        }
    }

    private fun getValidationNumberError(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.validation_valid_number)
    }
    private fun getFixValidationErrorsMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_fix_validation_errors)
    }
    private fun getFailedToSaveConfigurationMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_failed_to_save_configuration)
    }
    private fun getSuggestionIncreaseMaxOrDecreaseMin(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_increase_max_or_decrease_min)
    }
    private fun getSuggestionGrinderScalesStartAtZeroOrOne(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_grinder_scales_start_at_zero_or_one)
    }
    private fun getSuggestionMostGrinderScalesDontGoAbove100(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_most_grinder_scales_dont_go_above_100)
    }
    private fun getSuggestionRangeOfAtLeast3Steps(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_range_of_at_least_3_steps)
    }
    private fun getSuggestionSmallerRangeIsEasier(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_smaller_range_is_easier)
    }
    private fun getSuggestionEnterWholeNumbersOnly(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.suggestion_enter_whole_numbers_only)
    }
}

data class EquipmentSettingsUiState(
    val scaleMin: String = "",
    val scaleMax: String = "",
    val minError: String? = null,
    val maxError: String? = null,
    val generalError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false
)

