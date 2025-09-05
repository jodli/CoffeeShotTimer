package com.jodli.coffeeshottimer.ui.viewmodel

import android.util.Log
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
                            stepSize = config.stepSize.toString(),
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
                                    stepSize = defaultConfig.stepSize.toString(),
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

    fun updateStepSize(value: String) {
        _uiState.value = _uiState.value.copy(stepSize = value, stepSizeError = null, generalError = null)
        validateForm()
    }

    fun setStepSizePreset(stepSize: Double) {
        _uiState.value = _uiState.value.copy(
            stepSize = stepSize.toString(),
            stepSizeError = null,
            generalError = null
        )
        validateForm()
    }

    private fun validateForm() {
        val current = _uiState.value
        var minError: String? = null
        var maxError: String? = null
        var stepSizeError: String? = null
        var generalError: String? = null
        val minVal = current.scaleMin.toIntOrNull()
        val maxVal = current.scaleMax.toIntOrNull()
        val stepSizeVal = current.stepSize.toDoubleOrNull()

        if (current.scaleMin.isNotBlank() && minVal == null) minError = getValidationNumberError()
        if (current.scaleMax.isNotBlank() && maxVal == null) maxError = getValidationNumberError()
        if (current.stepSize.isNotBlank() && stepSizeVal == null) stepSizeError = getValidationNumberError()

        if (minVal != null && maxVal != null && stepSizeVal != null) {
            val config = GrinderConfiguration(scaleMin = minVal, scaleMax = maxVal, stepSize = stepSizeVal)
            val validation = config.validate()
            if (!validation.isValid) generalError = validation.errors.firstOrNull()
        }

        _uiState.value = current.copy(
            minError = minError,
            maxError = maxError,
            stepSizeError = stepSizeError,
            generalError = generalError,
            isFormValid = minError == null && maxError == null && stepSizeError == null && 
                         generalError == null && minVal != null && maxVal != null && stepSizeVal != null
        )
    }

    fun save(onSuccess: () -> Unit) {
        Log.d(TAG, "save: Starting grinder save process")
        val current = _uiState.value
        val minVal = current.scaleMin.toIntOrNull()
        val maxVal = current.scaleMax.toIntOrNull()
        val stepSizeVal = current.stepSize.toDoubleOrNull()
        
        Log.d(TAG, "save: Current state - scaleMin=${current.scaleMin}, scaleMax=${current.scaleMax}, stepSize=${current.stepSize}, isFormValid=${current.isFormValid}")
        Log.d(TAG, "save: Parsed values - minVal=$minVal, maxVal=$maxVal, stepSizeVal=$stepSizeVal")
        
        if (!current.isFormValid || minVal == null || maxVal == null || stepSizeVal == null) {
            Log.e(TAG, "save: Form validation failed")
            _uiState.value = current.copy(generalError = getFixValidationErrorsMessage())
            return
        }
        
        val config = GrinderConfiguration(scaleMin = minVal, scaleMax = maxVal, stepSize = stepSizeVal)
        Log.d(TAG, "save: Created GrinderConfiguration - $config")
        
        _uiState.value = current.copy(isLoading = true)
        Log.d(TAG, "save: Set loading state to true")
        
        viewModelScope.launch {
            try {
                val result = grinderConfigRepository.saveConfig(config)
                Log.d(TAG, "save: Repository saveConfig returned - isSuccess=${result.isSuccess}")
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "save: Save successful, calling onSuccess")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "save: Save failed with exception", exception)
                        _uiState.value = _uiState.value.copy(isLoading = false, generalError = getFailedToSaveConfigurationMessage())
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "save: Unexpected exception in coroutine", exception)
                _uiState.value = _uiState.value.copy(isLoading = false, generalError = getFailedToSaveConfigurationMessage())
            }
        }
    }

    companion object {
        private const val TAG = "EquipmentSettingsViewModel"
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
}

data class EquipmentSettingsUiState(
    val scaleMin: String = "",
    val scaleMax: String = "",
    val stepSize: String = "0.5",
    val minError: String? = null,
    val maxError: String? = null,
    val stepSizeError: String? = null,
    val generalError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false
)

