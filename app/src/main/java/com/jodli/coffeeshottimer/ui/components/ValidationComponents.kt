package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.data.model.ValidationResult
import java.text.DecimalFormat
import java.time.LocalDate

/**
 * Validation utilities and components for consistent form validation across the app.
 */
object ValidationUtils {
    
    // Weight validation constants
    const val MIN_COFFEE_WEIGHT_IN = 0.1
    const val MAX_COFFEE_WEIGHT_IN = 50.0
    const val MIN_COFFEE_WEIGHT_OUT = 0.1
    const val MAX_COFFEE_WEIGHT_OUT = 100.0
    
    // Time validation constants
    const val MIN_EXTRACTION_TIME = 5
    const val MAX_EXTRACTION_TIME = 120
    const val OPTIMAL_EXTRACTION_TIME_MIN = 25
    const val OPTIMAL_EXTRACTION_TIME_MAX = 30
    
    // Text validation constants
    const val MAX_BEAN_NAME_LENGTH = 100
    const val MIN_BEAN_NAME_LENGTH = 2
    const val MAX_NOTES_LENGTH = 500
    const val MAX_GRINDER_SETTING_LENGTH = 50
    
    // Brew ratio constants
    const val MIN_TYPICAL_BREW_RATIO = 1.5
    const val MAX_TYPICAL_BREW_RATIO = 3.0
    const val OPTIMAL_BREW_RATIO_MIN = 2.0
    const val OPTIMAL_BREW_RATIO_MAX = 2.5
    
    private val decimalFormat = DecimalFormat("#.#")
    
    /**
     * Validates coffee weight input with range checking.
     */
    fun validateCoffeeWeight(
        value: String,
        fieldName: String,
        minWeight: Double,
        maxWeight: Double,
        isRequired: Boolean = true
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (value.isBlank()) {
            if (isRequired) {
                errors.add("$fieldName is required")
            }
            return ValidationResult(errors.isEmpty(), errors)
        }
        
        val weight = value.toDoubleOrNull()
        when {
            weight == null -> errors.add("Please enter a valid number")
            weight < minWeight -> errors.add("$fieldName must be at least ${decimalFormat.format(minWeight)}g")
            weight > maxWeight -> errors.add("$fieldName cannot exceed ${decimalFormat.format(maxWeight)}g")
            value.contains('.') && value.substringAfter('.').length > 1 -> 
                errors.add("$fieldName can have at most 1 decimal place")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates extraction time with range checking.
     */
    fun validateExtractionTime(timeSeconds: Int): ValidationResult {
        val errors = mutableListOf<String>()
        
        when {
            timeSeconds < MIN_EXTRACTION_TIME -> 
                errors.add("Extraction time must be at least ${MIN_EXTRACTION_TIME} seconds")
            timeSeconds > MAX_EXTRACTION_TIME -> 
                errors.add("Extraction time cannot exceed ${MAX_EXTRACTION_TIME} seconds")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates bean name with length and character checking.
     */
    fun validateBeanName(name: String, existingNames: List<String> = emptyList(), currentBeanId: String? = null): ValidationResult {
        val errors = mutableListOf<String>()
        val trimmedName = name.trim()
        
        when {
            trimmedName.isEmpty() -> errors.add("Bean name is required")
            trimmedName.length < MIN_BEAN_NAME_LENGTH -> 
                errors.add("Bean name must be at least $MIN_BEAN_NAME_LENGTH characters")
            trimmedName.length > MAX_BEAN_NAME_LENGTH -> 
                errors.add("Bean name cannot exceed $MAX_BEAN_NAME_LENGTH characters")
            !trimmedName.matches(Regex("^[a-zA-Z0-9\\s\\-_&.()]+$")) -> 
                errors.add("Bean name contains invalid characters")
            existingNames.any { it.equals(trimmedName, ignoreCase = true) } -> 
                errors.add("Bean name already exists")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates roast date with reasonable range checking.
     */
    fun validateRoastDate(date: LocalDate): ValidationResult {
        val errors = mutableListOf<String>()
        val today = LocalDate.now()
        
        when {
            date.isAfter(today) -> errors.add("Roast date cannot be in the future")
            date.isBefore(today.minusDays(365)) -> 
                errors.add("Roast date cannot be more than 365 days ago")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates notes with length checking.
     */
    fun validateNotes(notes: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (notes.length > MAX_NOTES_LENGTH) {
            errors.add("Notes cannot exceed $MAX_NOTES_LENGTH characters")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validates grinder setting with length and character checking.
     */
    fun validateGrinderSetting(setting: String, isRequired: Boolean = true): ValidationResult {
        val errors = mutableListOf<String>()
        val trimmedSetting = setting.trim()
        
        when {
            trimmedSetting.isEmpty() && isRequired -> errors.add("Grinder setting is required")
            trimmedSetting.length > MAX_GRINDER_SETTING_LENGTH -> 
                errors.add("Grinder setting cannot exceed $MAX_GRINDER_SETTING_LENGTH characters")
            trimmedSetting.isNotEmpty() && !trimmedSetting.matches(Regex("^[a-zA-Z0-9\\s\\-_.#]+$")) -> 
                errors.add("Grinder setting contains invalid characters")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Checks if extraction time is optimal for espresso.
     */
    fun isOptimalExtractionTime(timeSeconds: Int): Boolean {
        return timeSeconds in OPTIMAL_EXTRACTION_TIME_MIN..OPTIMAL_EXTRACTION_TIME_MAX
    }
    
    /**
     * Checks if brew ratio is within typical espresso range.
     */
    fun isTypicalBrewRatio(ratio: Double): Boolean {
        return ratio in MIN_TYPICAL_BREW_RATIO..MAX_TYPICAL_BREW_RATIO
    }
    
    /**
     * Checks if brew ratio is optimal for espresso.
     */
    fun isOptimalBrewRatio(ratio: Double): Boolean {
        return ratio in OPTIMAL_BREW_RATIO_MIN..OPTIMAL_BREW_RATIO_MAX
    }
    
    /**
     * Formats a decimal number for display with appropriate precision.
     */
    fun formatDecimal(value: Double, maxDecimalPlaces: Int = 1): String {
        return when (maxDecimalPlaces) {
            0 -> "%.0f".format(value)
            1 -> decimalFormat.format(value)
            else -> "%.${maxDecimalPlaces}f".format(value)
        }
    }
    
    /**
     * Parses a string to double with validation.
     */
    fun parseDouble(value: String): Double? {
        return try {
            value.trim().toDoubleOrNull()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Sanitizes text input by removing invalid characters.
     */
    fun sanitizeTextInput(input: String, allowedPattern: Regex = Regex("^[a-zA-Z0-9\\s\\-_&.()]+$")): String {
        return input.filter { char -> char.toString().matches(allowedPattern) }
    }
}

/**
 * Enhanced text field with built-in validation and error display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    warningMessage: String? = null,
    isRequired: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    maxLength: Int? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onValidationChange: ((Boolean) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val hasError = errorMessage != null
    val hasWarning = warningMessage != null && !hasError
    
    // Notify parent of validation state changes
    LaunchedEffect(hasError) {
        onValidationChange?.invoke(!hasError)
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val filteredValue = if (maxLength != null) {
                    newValue.take(maxLength)
                } else {
                    newValue
                }
                onValueChange(filteredValue)
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label)
                    if (isRequired) {
                        Text(
                            text = " *",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon ?: if (hasError || hasWarning) {
                {
                    Icon(
                        imageVector = if (hasError) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = if (hasError) "Error" else "Warning",
                        tint = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            } else null,
            isError = hasError,
            singleLine = singleLine,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() }
            ),
            supportingText = {
                Column {
                    // Error message takes priority
                    if (hasError) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    } else if (hasWarning) {
                        Text(
                            text = warningMessage!!,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                    } else if (supportingText != null) {
                        Text(
                            text = supportingText,
                            fontSize = 12.sp
                        )
                    }
                    
                    // Character count for fields with max length
                    if (maxLength != null) {
                        Text(
                            text = "${value.length}/$maxLength",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Specialized text field for weight input with decimal validation.
 */
@Composable
fun WeightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    minWeight: Double,
    maxWeight: Double,
    isRequired: Boolean = true,
    enabled: Boolean = true,
    placeholder: String? = null,
    onValidationChange: ((Boolean) -> Unit)? = null
) {
    ValidatedTextField(
        value = value,
        onValueChange = { newValue ->
            // Filter to only allow numbers and one decimal point
            val filtered = newValue.filter { it.isDigit() || it == '.' }
                .let { str ->
                    // Ensure only one decimal point
                    val decimalIndex = str.indexOf('.')
                    if (decimalIndex != -1) {
                        str.substring(0, decimalIndex + 1) + 
                        str.substring(decimalIndex + 1).filter { it.isDigit() }.take(1)
                    } else {
                        str
                    }
                }
            onValueChange(filtered)
        },
        label = label,
        modifier = modifier,
        errorMessage = errorMessage,
        isRequired = isRequired,
        keyboardType = KeyboardType.Decimal,
        enabled = enabled,
        placeholder = placeholder,
        supportingText = "Range: ${ValidationUtils.formatDecimal(minWeight)}g - ${ValidationUtils.formatDecimal(maxWeight)}g",
        trailingIcon = {
            Text(
                text = "g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onValidationChange = onValidationChange
    )
}

/**
 * Display component for validation errors with consistent styling.
 */
@Composable
fun ValidationErrorDisplay(
    errors: List<String>,
    modifier: Modifier = Modifier,
    maxErrorsToShow: Int = 3
) {
    if (errors.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Validation errors",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Please fix the following issues:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                errors.take(maxErrorsToShow).forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 24.dp, bottom = 2.dp)
                    )
                }
                
                if (errors.size > maxErrorsToShow) {
                    Text(
                        text = "... and ${errors.size - maxErrorsToShow} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Display component for validation warnings with consistent styling.
 */
@Composable
fun ValidationWarningDisplay(
    warnings: List<String>,
    modifier: Modifier = Modifier,
    maxWarningsToShow: Int = 2
) {
    if (warnings.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Validation warnings",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                warnings.take(maxWarningsToShow).forEach { warning ->
                    Text(
                        text = "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(start = 24.dp, bottom = 2.dp)
                    )
                }
                
                if (warnings.size > maxWarningsToShow) {
                    Text(
                        text = "... and ${warnings.size - maxWarningsToShow} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Comprehensive form validation state manager.
 */
@Composable
fun rememberFormValidationState(): FormValidationState {
    return remember { FormValidationState() }
}

class FormValidationState {
    private val _fieldValidationStates = mutableStateMapOf<String, Boolean>()
    private val _fieldErrors = mutableStateMapOf<String, String?>()
    private val _fieldWarnings = mutableStateMapOf<String, String?>()
    
    val isFormValid: Boolean by derivedStateOf {
        _fieldValidationStates.values.all { it }
    }
    
    val allErrors: List<String>
        get() = _fieldErrors.values.filterNotNull()
    
    val allWarnings: List<String>
        get() = _fieldWarnings.values.filterNotNull()
    
    fun setFieldValidation(fieldName: String, isValid: Boolean) {
        _fieldValidationStates[fieldName] = isValid
    }
    
    fun setFieldError(fieldName: String, error: String?) {
        _fieldErrors[fieldName] = error
        _fieldValidationStates[fieldName] = error == null
    }
    
    fun setFieldWarning(fieldName: String, warning: String?) {
        _fieldWarnings[fieldName] = warning
    }
    
    fun getFieldError(fieldName: String): String? = _fieldErrors[fieldName]
    
    fun getFieldWarning(fieldName: String): String? = _fieldWarnings[fieldName]
    
    fun clearField(fieldName: String) {
        _fieldValidationStates.remove(fieldName)
        _fieldErrors.remove(fieldName)
        _fieldWarnings.remove(fieldName)
    }
    
    fun clearAllErrors() {
        _fieldErrors.clear()
        _fieldValidationStates.replaceAll { _, _ -> true }
    }
    
    fun clearAllWarnings() {
        _fieldWarnings.clear()
    }
    
    fun reset() {
        _fieldValidationStates.clear()
        _fieldErrors.clear()
        _fieldWarnings.clear()
    }
}