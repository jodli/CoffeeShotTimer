package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Equipment Setup screen during onboarding.
 * Manages form state, validation, and saving grinder configuration.
 */
@HiltViewModel
class EquipmentSetupViewModel @Inject constructor(
    private val grinderConfigRepository: GrinderConfigRepository,
    private val errorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipmentSetupUiState())
    val uiState: StateFlow<EquipmentSetupUiState> = _uiState.asStateFlow()

    /**
     * Updates the minimum scale value and clears related errors.
     */
    fun updateScaleMin(value: String) {
        _uiState.value = _uiState.value.copy(
            scaleMin = value,
            minError = null,
            generalError = null
        )
        validateForm()
    }

    /**
     * Updates the maximum scale value and clears related errors.
     */
    fun updateScaleMax(value: String) {
        _uiState.value = _uiState.value.copy(
            scaleMax = value,
            maxError = null,
            generalError = null
        )
        validateForm()
    }

    /**
     * Sets a preset configuration from the common presets.
     */
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

    /**
     * Validates the current form state and updates error messages.
     */
    private fun validateForm() {
        val currentState = _uiState.value
        
        // Clear previous errors
        var minError: String? = null
        var maxError: String? = null
        var generalError: String? = null
        
        val minValue = currentState.scaleMin.toIntOrNull()
        val maxValue = currentState.scaleMax.toIntOrNull()
        
        // Individual field validation
        if (currentState.scaleMin.isNotBlank() && minValue == null) {
            minError = getValidationNumberError()
        }
        if (currentState.scaleMax.isNotBlank() && maxValue == null) {
            maxError = getValidationNumberError()
        }
        
        // If both values are valid, validate the configuration
        if (minValue != null && maxValue != null) {
            val config = GrinderConfiguration(scaleMin = minValue, scaleMax = maxValue)
            val validation = config.validate()
            if (!validation.isValid) {
                generalError = validation.errors.firstOrNull()
            }
        }
        
        _uiState.value = currentState.copy(
            minError = minError,
            maxError = maxError,
            generalError = generalError,
            isFormValid = minError == null && maxError == null && generalError == null && 
                         minValue != null && maxValue != null
        )
    }

    /**
     * Saves the grinder configuration to the database.
     */
    fun saveConfiguration(onSuccess: (GrinderConfiguration) -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        
        if (!currentState.isFormValid) {
            onError(getFixValidationErrorsMessage())
            return
        }
        
        val minValue = currentState.scaleMin.toIntOrNull()
        val maxValue = currentState.scaleMax.toIntOrNull()
        
        if (minValue == null || maxValue == null) {
            onError(getEnterValidNumbersMessage())
            return
        }
        
        val config = GrinderConfiguration(scaleMin = minValue, scaleMax = maxValue)
        
        _uiState.value = currentState.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val result = grinderConfigRepository.saveConfig(config)
                
                result.fold(
                    onSuccess = {
                        _uiState.value = currentState.copy(isLoading = false)
                        onSuccess(config)
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is RepositoryException.ValidationError -> exception.message ?: getValidationFailedMessage()
                            is RepositoryException.DatabaseError -> getFailedToSaveConfigurationMessage()
                            else -> getUnexpectedErrorMessage()
                        }
                        
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        onError(errorMessage)
                    }
                )
            } catch (exception: Exception) {
                val errorMessage = getUnexpectedErrorMessage()
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    /**
     * Skips the equipment setup and uses the default configuration.
     */
    fun skipSetup(onSuccess: (GrinderConfiguration) -> Unit, onError: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = grinderConfigRepository.getOrCreateDefaultConfig()

                result.fold(
                    onSuccess = { config ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess(config)
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is RepositoryException.DatabaseError -> getFailedToSaveDefaultConfigurationMessage()
                            is RepositoryException.ValidationError -> getFailedToSaveDefaultConfigurationMessage()
                            else -> getUnexpectedErrorMessage()
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        onError(errorMessage)
                    }
                )
            } catch (exception: Exception) {
                val errorMessage = getUnexpectedErrorMessage()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    /**
     * Retries the last failed operation.
     */
    fun retry(onSuccess: (GrinderConfiguration) -> Unit, onError: (String) -> Unit) {
        // Clear error state and retry saving
        _uiState.value = _uiState.value.copy(error = null)
        saveConfiguration(onSuccess, onError)
    }

    /**
     * Clears the current error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Gets validation suggestion for the current error.
     */
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

    // Helper methods for string resources
    private fun getValidationNumberError(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.validation_valid_number)
    }

    private fun getFixValidationErrorsMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_fix_validation_errors)
    }

    private fun getEnterValidNumbersMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_enter_valid_numbers)
    }

    private fun getValidationFailedMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_validation_failed_generic)
    }

    private fun getFailedToSaveConfigurationMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_failed_to_save_configuration)
    }

    private fun getFailedToSaveDefaultConfigurationMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_failed_to_save_default_configuration)
    }

    private fun getUnexpectedErrorMessage(): String {
        return errorTranslator.getString(com.jodli.coffeeshottimer.R.string.error_unexpected_error)
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

/**
 * UI state for the Equipment Setup screen.
 */
data class EquipmentSetupUiState(
    val scaleMin: String = "",
    val scaleMax: String = "",
    val minError: String? = null,
    val maxError: String? = null,
    val generalError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)