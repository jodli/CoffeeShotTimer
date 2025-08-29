package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import com.jodli.coffeeshottimer.data.model.BasketPreset
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.repository.BasketConfigRepository
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.ui.equipment.EquipmentSetupStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel for the multi-step Equipment Setup flow during onboarding.
 * Manages form state, validation, and saving configuration for both grinder and basket.
 */
@HiltViewModel
class EquipmentSetupFlowViewModel @Inject constructor(
    private val grinderConfigRepository: GrinderConfigRepository,
    private val basketConfigRepository: BasketConfigRepository,
    private val onboardingManager: OnboardingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipmentSetupFlowUiState())
    val uiState: StateFlow<EquipmentSetupFlowUiState> = _uiState.asStateFlow()

    /**
     * Navigate to the next step in the equipment setup flow
     */
    fun navigateForward() {
        val currentState = _uiState.value
        val nextStep = when (currentState.currentStep) {
            EquipmentSetupStep.WELCOME -> EquipmentSetupStep.GRINDER_SETUP
            EquipmentSetupStep.GRINDER_SETUP -> {
                if (validateGrinder()) {
                    EquipmentSetupStep.BASKET_SETUP
                } else {
                    return // Don't navigate if validation fails
                }
            }
            EquipmentSetupStep.BASKET_SETUP -> {
                if (validateBasket()) {
                    EquipmentSetupStep.SUMMARY
                } else {
                    return // Don't navigate if validation fails
                }
            }
            EquipmentSetupStep.SUMMARY -> return // Can't go forward from summary
        }
        
        _uiState.value = currentState.copy(currentStep = nextStep)
    }

    /**
     * Navigate to the previous step in the equipment setup flow
     */
    fun navigateBackward() {
        val currentState = _uiState.value
        val previousStep = when (currentState.currentStep) {
            EquipmentSetupStep.WELCOME -> return // Can't go back from welcome
            EquipmentSetupStep.GRINDER_SETUP -> EquipmentSetupStep.WELCOME
            EquipmentSetupStep.BASKET_SETUP -> EquipmentSetupStep.GRINDER_SETUP
            EquipmentSetupStep.SUMMARY -> EquipmentSetupStep.BASKET_SETUP
        }
        
        _uiState.value = currentState.copy(currentStep = previousStep)
    }

    // Grinder configuration methods
    fun updateGrinderMin(value: String) {
        _uiState.value = _uiState.value.copy(
            grinderScaleMin = value,
            grinderMinError = null,
            grinderGeneralError = null
        )
        validateGrinder()
    }

    fun updateGrinderMax(value: String) {
        _uiState.value = _uiState.value.copy(
            grinderScaleMax = value,
            grinderMaxError = null,
            grinderGeneralError = null
        )
        validateGrinder()
    }

    fun setGrinderPreset(min: Int, max: Int) {
        _uiState.value = _uiState.value.copy(
            grinderScaleMin = min.toString(),
            grinderScaleMax = max.toString(),
            grinderMinError = null,
            grinderMaxError = null,
            grinderGeneralError = null
        )
        validateGrinder()
    }

    private fun validateGrinder(): Boolean {
        val currentState = _uiState.value
        val minValue = currentState.grinderScaleMin.toIntOrNull()
        val maxValue = currentState.grinderScaleMax.toIntOrNull()
        
        var minError: String? = null
        var maxError: String? = null
        var generalError: String? = null
        
        if (currentState.grinderScaleMin.isNotBlank() && minValue == null) {
            minError = "Please enter a valid number"
        }
        if (currentState.grinderScaleMax.isNotBlank() && maxValue == null) {
            maxError = "Please enter a valid number"
        }
        
        var isValid = false
        if (minValue != null && maxValue != null) {
            val config = GrinderConfiguration(scaleMin = minValue, scaleMax = maxValue)
            val validation = config.validate()
            if (!validation.isValid) {
                generalError = validation.errors.firstOrNull()
            } else {
                isValid = true
            }
        }
        
        _uiState.value = currentState.copy(
            grinderMinError = minError,
            grinderMaxError = maxError,
            grinderGeneralError = generalError,
            isGrinderValid = isValid
        )
        
        return isValid
    }

    // Basket configuration methods
    fun updateCoffeeInMin(value: String) {
        _uiState.value = _uiState.value.copy(
            coffeeInMin = value,
            coffeeInMinError = null,
            basketGeneralError = null
        )
        validateBasket()
    }

    fun updateCoffeeInMax(value: String) {
        _uiState.value = _uiState.value.copy(
            coffeeInMax = value,
            coffeeInMaxError = null,
            basketGeneralError = null
        )
        validateBasket()
    }

    fun updateCoffeeOutMin(value: String) {
        _uiState.value = _uiState.value.copy(
            coffeeOutMin = value,
            coffeeOutMinError = null,
            basketGeneralError = null
        )
        validateBasket()
    }

    fun updateCoffeeOutMax(value: String) {
        _uiState.value = _uiState.value.copy(
            coffeeOutMax = value,
            coffeeOutMaxError = null,
            basketGeneralError = null
        )
        validateBasket()
    }

    fun setBasketPreset(preset: BasketPreset) {
        _uiState.value = _uiState.value.copy(
            coffeeInMin = preset.coffeeInMin.roundToInt().toString(),
            coffeeInMax = preset.coffeeInMax.roundToInt().toString(),
            coffeeOutMin = preset.coffeeOutMin.roundToInt().toString(),
            coffeeOutMax = preset.coffeeOutMax.roundToInt().toString(),
            coffeeInMinError = null,
            coffeeInMaxError = null,
            coffeeOutMinError = null,
            coffeeOutMaxError = null,
            basketGeneralError = null
        )
        validateBasket()
    }

    private fun validateBasket(): Boolean {
        val currentState = _uiState.value
        val inMin = currentState.coffeeInMin.toIntOrNull()?.toFloat()
        val inMax = currentState.coffeeInMax.toIntOrNull()?.toFloat()
        val outMin = currentState.coffeeOutMin.toIntOrNull()?.toFloat()
        val outMax = currentState.coffeeOutMax.toIntOrNull()?.toFloat()
        
        var inMinError: String? = null
        var inMaxError: String? = null
        var outMinError: String? = null
        var outMaxError: String? = null
        var generalError: String? = null
        
        if (currentState.coffeeInMin.isNotBlank() && inMin == null) {
            inMinError = "Please enter a valid whole number"
        }
        if (currentState.coffeeInMax.isNotBlank() && inMax == null) {
            inMaxError = "Please enter a valid whole number"
        }
        if (currentState.coffeeOutMin.isNotBlank() && outMin == null) {
            outMinError = "Please enter a valid whole number"
        }
        if (currentState.coffeeOutMax.isNotBlank() && outMax == null) {
            outMaxError = "Please enter a valid whole number"
        }
        
        var isValid = false
        if (inMin != null && inMax != null && outMin != null && outMax != null) {
            val config = BasketConfiguration(
                coffeeInMin = inMin,
                coffeeInMax = inMax,
                coffeeOutMin = outMin,
                coffeeOutMax = outMax,
                isActive = true
            )
            val validation = config.validate()
            if (!validation.isValid) {
                generalError = validation.errors.firstOrNull()
            } else {
                isValid = true
            }
        }
        
        _uiState.value = currentState.copy(
            coffeeInMinError = inMinError,
            coffeeInMaxError = inMaxError,
            coffeeOutMinError = outMinError,
            coffeeOutMaxError = outMaxError,
            basketGeneralError = generalError,
            isBasketValid = isValid
        )
        
        return isValid
    }

    /**
     * Save all configurations and complete the equipment setup
     */
    fun saveConfiguration(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        
        // Parse all values
        val grinderMin = currentState.grinderScaleMin.toIntOrNull()
        val grinderMax = currentState.grinderScaleMax.toIntOrNull()
        val coffeeInMin = currentState.coffeeInMin.toIntOrNull()?.toFloat()
        val coffeeInMax = currentState.coffeeInMax.toIntOrNull()?.toFloat()
        val coffeeOutMin = currentState.coffeeOutMin.toIntOrNull()?.toFloat()
        val coffeeOutMax = currentState.coffeeOutMax.toIntOrNull()?.toFloat()
        
        if (grinderMin == null || grinderMax == null || 
            coffeeInMin == null || coffeeInMax == null || 
            coffeeOutMin == null || coffeeOutMax == null) {
            onError("Please complete all configuration fields")
            return
        }
        
        _uiState.value = currentState.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                // Save grinder configuration
                val grinderConfig = GrinderConfiguration(scaleMin = grinderMin, scaleMax = grinderMax)
                val grinderResult = grinderConfigRepository.saveConfig(grinderConfig)
                
                grinderResult.fold(
                    onSuccess = {
                        // Save basket configuration
                        val basketConfig = BasketConfiguration(
                            coffeeInMin = coffeeInMin,
                            coffeeInMax = coffeeInMax,
                            coffeeOutMin = coffeeOutMin,
                            coffeeOutMax = coffeeOutMax,
                            isActive = true
                        )
                        
                        val basketResult = basketConfigRepository.saveConfig(basketConfig)
                        
                        basketResult.fold(
                            onSuccess = {
                                // Mark onboarding as complete
                                onboardingManager.markOnboardingComplete()
                                _uiState.value = currentState.copy(isLoading = false)
                                onSuccess()
                            },
                            onFailure = { exception ->
                                val errorMessage = when (exception) {
                                    is RepositoryException.ValidationError -> 
                                        exception.message ?: "Basket configuration validation failed"
                                    is RepositoryException.DatabaseError -> 
                                        "Failed to save basket configuration"
                                    else -> "An unexpected error occurred"
                                }
                                
                                _uiState.value = currentState.copy(
                                    isLoading = false,
                                    error = errorMessage
                                )
                                onError(errorMessage)
                            }
                        )
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is RepositoryException.ValidationError -> 
                                exception.message ?: "Grinder configuration validation failed"
                            is RepositoryException.DatabaseError -> 
                                "Failed to save grinder configuration"
                            else -> "An unexpected error occurred"
                        }
                        
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        onError(errorMessage)
                    }
                )
            } catch (exception: Exception) {
                val errorMessage = "An unexpected error occurred"
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    /**
     * Skip the equipment setup and use default configurations
     */
    fun skipSetup(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Get or create default grinder configuration
                val grinderResult = grinderConfigRepository.getOrCreateDefaultConfig()

                grinderResult.fold(
                    onSuccess = {
                        // Create and save default basket configuration using Double preset
                        val defaultBasketConfig = BasketPreset.DOUBLE.toBasketConfiguration()
                        val basketResult = basketConfigRepository.saveConfig(defaultBasketConfig)
                        
                        basketResult.fold(
                            onSuccess = {
                                // Mark onboarding as complete
                                onboardingManager.markOnboardingComplete()
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onSuccess()
                            },
                            onFailure = {
                                val errorMessage = "Failed to save default configuration"
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = errorMessage
                                )
                                onError(errorMessage)
                            }
                        )
                    },
                    onFailure = {
                        val errorMessage = "Failed to save default configuration"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        onError(errorMessage)
                    }
                )
            } catch (exception: Exception) {
                val errorMessage = "An unexpected error occurred"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                onError(errorMessage)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Equipment Setup flow
 */
data class EquipmentSetupFlowUiState(
    val currentStep: EquipmentSetupStep = EquipmentSetupStep.WELCOME,
    
    // Grinder configuration state
    val grinderScaleMin: String = "",
    val grinderScaleMax: String = "",
    val grinderMinError: String? = null,
    val grinderMaxError: String? = null,
    val grinderGeneralError: String? = null,
    val isGrinderValid: Boolean = false,
    
    // Basket configuration state
    val coffeeInMin: String = "",
    val coffeeInMax: String = "",
    val coffeeOutMin: String = "",
    val coffeeOutMax: String = "",
    val coffeeInMinError: String? = null,
    val coffeeInMaxError: String? = null,
    val coffeeOutMinError: String? = null,
    val coffeeOutMaxError: String? = null,
    val basketGeneralError: String? = null,
    val isBasketValid: Boolean = false,
    
    // Overall state
    val isLoading: Boolean = false,
    val error: String? = null
)

