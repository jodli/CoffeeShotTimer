package com.jodli.coffeeshottimer.ui.validation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Example integration of enhanced validation system with ViewModels and UI.
 * This demonstrates how to use the new validation components in practice.
 */

/**
 * Enhanced ViewModel base class with integrated validation support.
 */
abstract class ValidatedViewModel(
    protected val validationUtils: ValidationUtils
) : ViewModel() {

    protected val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    protected val _isFormValid = MutableStateFlow(true)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

}

/**
 * Example enhanced shot recording ViewModel with integrated validation.
 */
class EnhancedShotRecordingViewModel(
    validationUtils: ValidationUtils
) : ValidatedViewModel(validationUtils) {

    // Form fields
    private val _coffeeWeightIn = MutableStateFlow("")

    private val _coffeeWeightOut = MutableStateFlow("")

    private val _grinderSetting = MutableStateFlow("")

    private val _notes = MutableStateFlow("")

    // Individual field errors for real-time feedback
    private val _coffeeWeightInError = MutableStateFlow<String?>(null)

    private val _coffeeWeightOutError = MutableStateFlow<String?>(null)

    private val _grinderSettingError = MutableStateFlow<String?>(null)

    // Contextual warnings
    private val _brewRatioWarnings = MutableStateFlow<List<String>>(emptyList())

    private val _extractionTimeWarnings = MutableStateFlow<List<String>>(emptyList())

    /**
     * Validates the entire form and updates brew ratio warnings.
     */
    private fun validateFormAndUpdateBrewRatio() {
        validateForm()
        updateBrewRatioWarnings()
    }

    /**
     * Validates the entire form using comprehensive validation.
     */
    private fun validateForm() {
        viewModelScope.launch {
            val validationResult = validateCompleteShot(
                coffeeWeightIn = _coffeeWeightIn.value,
                coffeeWeightOut = _coffeeWeightOut.value,
                extractionTimeSeconds = 27, // Would come from timer
                grinderSetting = _grinderSetting.value,
                notes = _notes.value,
                validationUtils
            )

            // Update overall validation state
            _validationErrors.value = validationResult.errors
            _isFormValid.value = validationResult.isValid
        }
    }

    /**
     * Updates brew ratio warnings based on current weights.
     */
    private fun updateBrewRatioWarnings() {
        val weightIn = _coffeeWeightIn.value.toDoubleOrNull()
        val weightOut = _coffeeWeightOut.value.toDoubleOrNull()

        if (weightIn != null && weightOut != null && weightIn > 0) {
            val brewRatio = weightOut / weightIn
            _brewRatioWarnings.value = brewRatio.getBrewRatioWarnings(validationUtils)
        } else {
            _brewRatioWarnings.value = emptyList()
        }
    }

}

