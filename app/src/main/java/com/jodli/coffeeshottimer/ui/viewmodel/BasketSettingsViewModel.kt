package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import com.jodli.coffeeshottimer.data.model.BasketPreset
import com.jodli.coffeeshottimer.data.repository.BasketConfigRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel for the BasketSettingsScreen.
 * Manages basket configuration form state, validation, and saving.
 */
@HiltViewModel
class BasketSettingsViewModel @Inject constructor(
    private val basketConfigRepository: BasketConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BasketSettingsUiState())
    val uiState: StateFlow<BasketSettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentConfiguration()
    }

    /**
     * Load the current basket configuration and populate the form
     */
    private fun loadCurrentConfiguration() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val result = basketConfigRepository.getActiveConfig()
                result.fold(
                    onSuccess = { config ->
                        if (config != null) {
                            // Pre-fill form with current values, converting to whole numbers
                            _uiState.value = _uiState.value.copy(
                                coffeeInMin = config.coffeeInMin.roundToInt().toString(),
                                coffeeInMax = config.coffeeInMax.roundToInt().toString(),
                                coffeeOutMin = config.coffeeOutMin.roundToInt().toString(),
                                coffeeOutMax = config.coffeeOutMax.roundToInt().toString(),
                                isLoading = false,
                                error = null
                            )
                            validateBasket()
                        } else {
                            // No config exists, use default values
                            val defaultConfig = BasketConfiguration.DEFAULT
                            _uiState.value = _uiState.value.copy(
                                coffeeInMin = defaultConfig.coffeeInMin.roundToInt().toString(),
                                coffeeInMax = defaultConfig.coffeeInMax.roundToInt().toString(),
                                coffeeOutMin = defaultConfig.coffeeOutMin.roundToInt().toString(),
                                coffeeOutMax = defaultConfig.coffeeOutMax.roundToInt().toString(),
                                isLoading = false,
                                error = null
                            )
                            validateBasket()
                        }
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is RepositoryException.DatabaseError -> "Failed to load configuration"
                            else -> "An error occurred while loading configuration"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred while loading configuration"
                )
            }
        }
    }

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
        // Convert preset floats to whole numbers
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
     * Save the basket configuration
     */
    fun saveConfiguration(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        
        // Parse all values as integers then convert to floats
        val coffeeInMin = currentState.coffeeInMin.toIntOrNull()?.toFloat()
        val coffeeInMax = currentState.coffeeInMax.toIntOrNull()?.toFloat()
        val coffeeOutMin = currentState.coffeeOutMin.toIntOrNull()?.toFloat()
        val coffeeOutMax = currentState.coffeeOutMax.toIntOrNull()?.toFloat()
        
        if (coffeeInMin == null || coffeeInMax == null || 
            coffeeOutMin == null || coffeeOutMax == null) {
            onError("Please complete all configuration fields")
            return
        }
        
        _uiState.value = currentState.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val basketConfig = BasketConfiguration(
                    coffeeInMin = coffeeInMin,
                    coffeeInMax = coffeeInMax,
                    coffeeOutMin = coffeeOutMin,
                    coffeeOutMax = coffeeOutMax,
                    isActive = true
                )
                
                val result = basketConfigRepository.saveConfig(basketConfig)
                
                result.fold(
                    onSuccess = {
                        _uiState.value = currentState.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is RepositoryException.ValidationError -> 
                                exception.message ?: "Configuration validation failed"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the basket settings screen
 */
data class BasketSettingsUiState(
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