package com.jodli.coffeeshottimer.ui.validation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.components.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Example integration of enhanced validation system with ViewModels and UI.
 * This demonstrates how to use the new validation components in practice.
 */

/**
 * Enhanced ViewModel base class with integrated validation support.
 */
abstract class ValidatedViewModel : ViewModel() {
    
    protected val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()
    
    protected val _validationWarnings = MutableStateFlow<List<String>>(emptyList())
    val validationWarnings: StateFlow<List<String>> = _validationWarnings.asStateFlow()
    
    protected val _isFormValid = MutableStateFlow(true)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()
    
    /**
     * Updates validation state based on field validation results.
     */
    protected fun updateValidationState(fieldValidations: Map<String, ValidationResult>) {
        val allErrors = mutableListOf<String>()
        val allWarnings = mutableListOf<String>()
        
        fieldValidations.values.forEach { result ->
            allErrors.addAll(result.errors)
            // Warnings would be separate from errors in a more sophisticated implementation
        }
        
        _validationErrors.value = allErrors
        _validationWarnings.value = allWarnings
        _isFormValid.value = fieldValidations.values.all { it.isValid }
    }
    
    /**
     * Clears all validation errors and warnings.
     */
    fun clearValidation() {
        _validationErrors.value = emptyList()
        _validationWarnings.value = emptyList()
        _isFormValid.value = true
    }
}

/**
 * Example enhanced shot recording ViewModel with integrated validation.
 */
class EnhancedShotRecordingViewModel : ValidatedViewModel() {
    
    // Form fields
    private val _coffeeWeightIn = MutableStateFlow("")
    val coffeeWeightIn: StateFlow<String> = _coffeeWeightIn.asStateFlow()
    
    private val _coffeeWeightOut = MutableStateFlow("")
    val coffeeWeightOut: StateFlow<String> = _coffeeWeightOut.asStateFlow()
    
    private val _grinderSetting = MutableStateFlow("")
    val grinderSetting: StateFlow<String> = _grinderSetting.asStateFlow()
    
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()
    
    // Individual field errors for real-time feedback
    private val _coffeeWeightInError = MutableStateFlow<String?>(null)
    val coffeeWeightInError: StateFlow<String?> = _coffeeWeightInError.asStateFlow()
    
    private val _coffeeWeightOutError = MutableStateFlow<String?>(null)
    val coffeeWeightOutError: StateFlow<String?> = _coffeeWeightOutError.asStateFlow()
    
    private val _grinderSettingError = MutableStateFlow<String?>(null)
    val grinderSettingError: StateFlow<String?> = _grinderSettingError.asStateFlow()
    
    // Contextual warnings
    private val _brewRatioWarnings = MutableStateFlow<List<String>>(emptyList())
    val brewRatioWarnings: StateFlow<List<String>> = _brewRatioWarnings.asStateFlow()
    
    private val _extractionTimeWarnings = MutableStateFlow<List<String>>(emptyList())
    val extractionTimeWarnings: StateFlow<List<String>> = _extractionTimeWarnings.asStateFlow()
    
    /**
     * Updates coffee weight in with enhanced validation.
     */
    fun updateCoffeeWeightIn(value: String) {
        _coffeeWeightIn.value = value
        
        val validationResult = value.validateCoffeeWeightIn()
        _coffeeWeightInError.value = validationResult.errors.firstOrNull()
        
        validateFormAndUpdateBrewRatio()
    }
    
    /**
     * Updates coffee weight out with enhanced validation.
     */
    fun updateCoffeeWeightOut(value: String) {
        _coffeeWeightOut.value = value
        
        val validationResult = value.validateCoffeeWeightOut()
        _coffeeWeightOutError.value = validationResult.errors.firstOrNull()
        
        validateFormAndUpdateBrewRatio()
    }
    
    /**
     * Updates grinder setting with enhanced validation.
     */
    fun updateGrinderSetting(value: String) {
        _grinderSetting.value = value
        
        val validationResult = value.validateGrinderSettingEnhanced()
        _grinderSettingError.value = validationResult.errors.firstOrNull()
        
        validateForm()
    }
    
    /**
     * Updates notes with validation.
     */
    fun updateNotes(value: String) {
        _notes.value = value
        validateForm()
    }
    
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
                notes = _notes.value
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
            _brewRatioWarnings.value = brewRatio.getBrewRatioWarnings()
        } else {
            _brewRatioWarnings.value = emptyList()
        }
    }
    
    /**
     * Updates extraction time warnings.
     */
    fun updateExtractionTimeWarnings(timeSeconds: Int) {
        _extractionTimeWarnings.value = timeSeconds.getExtractionTimeWarnings()
    }
    
    /**
     * Validates and records the shot with comprehensive error handling.
     */
    fun recordShot() {
        viewModelScope.launch {
            // First, validate everything
            val validationResult = validateCompleteShot(
                coffeeWeightIn = _coffeeWeightIn.value,
                coffeeWeightOut = _coffeeWeightOut.value,
                extractionTimeSeconds = 27, // Would come from timer
                grinderSetting = _grinderSetting.value,
                notes = _notes.value
            )
            
            if (!validationResult.isValid) {
                _validationErrors.value = validationResult.errors
                return@launch
            }
            
            // Proceed with recording if validation passes
            // Implementation would call the actual use case here
        }
    }
}

/**
 * Example UI component using enhanced validation.
 */
@Composable
fun EnhancedShotRecordingForm(
    viewModel: EnhancedShotRecordingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formStateManager = rememberFormStateManager(
        formId = "shot_recording",
        context = context
    )
    
    // Collect form state
    val coffeeWeightIn by viewModel.coffeeWeightIn.collectAsState()
    val coffeeWeightOut by viewModel.coffeeWeightOut.collectAsState()
    val grinderSetting by viewModel.grinderSetting.collectAsState()
    val notes by viewModel.notes.collectAsState()
    
    // Collect validation state
    val coffeeWeightInError by viewModel.coffeeWeightInError.collectAsState()
    val coffeeWeightOutError by viewModel.coffeeWeightOutError.collectAsState()
    val grinderSettingError by viewModel.grinderSettingError.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()
    val brewRatioWarnings by viewModel.brewRatioWarnings.collectAsState()
    val extractionTimeWarnings by viewModel.extractionTimeWarnings.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    
    // Handle form state persistence
    FormStatePersistenceEffect(formStateManager)
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display validation errors if any
        ValidationErrorDisplay(
            errors = validationErrors,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Display contextual warnings
        val allWarnings = brewRatioWarnings + extractionTimeWarnings
        ValidationWarningDisplay(
            warnings = allWarnings,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Coffee weight input field with enhanced validation
        WeightTextField(
            value = coffeeWeightIn,
            onValueChange = { viewModel.updateCoffeeWeightIn(it) },
            label = "Coffee Weight In",
            errorMessage = coffeeWeightInError,
            minWeight = ValidationUtils.MIN_COFFEE_WEIGHT_IN,
            maxWeight = ValidationUtils.MAX_COFFEE_WEIGHT_IN,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Coffee weight output field with enhanced validation
        WeightTextField(
            value = coffeeWeightOut,
            onValueChange = { viewModel.updateCoffeeWeightOut(it) },
            label = "Coffee Weight Out",
            errorMessage = coffeeWeightOutError,
            minWeight = ValidationUtils.MIN_COFFEE_WEIGHT_OUT,
            maxWeight = ValidationUtils.MAX_COFFEE_WEIGHT_OUT,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Grinder setting field with enhanced validation
        ValidatedTextField(
            value = grinderSetting,
            onValueChange = { viewModel.updateGrinderSetting(it) },
            label = "Grinder Setting",
            errorMessage = grinderSettingError,
            isRequired = true,
            maxLength = ValidationUtils.MAX_GRINDER_SETTING_LENGTH,
            placeholder = "e.g., 15, Fine, 2.5",
            supportingText = "Record your grinder setting to remember what worked",
            modifier = Modifier.fillMaxWidth()
        )
        
        // Notes field with character count
        ValidatedTextField(
            value = notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = "Notes",
            isRequired = false,
            maxLength = ValidationUtils.MAX_NOTES_LENGTH,
            singleLine = false,
            placeholder = "Optional notes about this shot...",
            supportingText = "Describe the taste, aroma, or any observations",
            modifier = Modifier.fillMaxWidth()
        )
        
        // Record button with validation state
        Button(
            onClick = { viewModel.recordShot() },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Record Shot")
        }
        
        // Form validation summary for debugging/development
        if (validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Form Status",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Valid: $isFormValid",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Errors: ${validationErrors.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Warnings: ${allWarnings.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Example usage in a screen composable.
 */
@Composable
fun ExampleEnhancedShotRecordingScreen() {
    val viewModel = remember { EnhancedShotRecordingViewModel() }
    
    EnhancedShotRecordingForm(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
    )
}