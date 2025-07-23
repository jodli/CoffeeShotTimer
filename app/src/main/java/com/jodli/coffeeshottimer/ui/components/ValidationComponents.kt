package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.data.model.ValidationResult
import java.text.DecimalFormat
import java.time.LocalDate

/**
 * Validation utilities and components for consistent form validation across the app.
 */
object ValidationUtils {

    // Weight validation constants
    const val MIN_COFFEE_WEIGHT_IN = 8.0
    const val MAX_COFFEE_WEIGHT_IN = 35.0
    const val MIN_COFFEE_WEIGHT_OUT = 10.0
    const val MAX_COFFEE_WEIGHT_OUT = 80.0

    // Time validation constants
    const val MIN_EXTRACTION_TIME = 5
    const val MAX_EXTRACTION_TIME = 120
    const val OPTIMAL_EXTRACTION_TIME_MIN = 25
    const val OPTIMAL_EXTRACTION_TIME_MAX = 30

    // Text validation constants
    private const val MAX_BEAN_NAME_LENGTH = 100
    private const val MIN_BEAN_NAME_LENGTH = 2
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
            weight < minWeight -> errors.add(
                "$fieldName must be at least ${
                    decimalFormat.format(
                        minWeight
                    )
                }g"
            )

            weight > maxWeight -> errors.add(
                "$fieldName cannot exceed ${
                    decimalFormat.format(
                        maxWeight
                    )
                }g"
            )

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
                errors.add("Extraction time must be at least $MIN_EXTRACTION_TIME seconds")

            timeSeconds > MAX_EXTRACTION_TIME ->
                errors.add("Extraction time cannot exceed $MAX_EXTRACTION_TIME seconds")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates bean name with length and character checking.
     */
    fun validateBeanName(
        name: String,
        existingNames: List<String> = emptyList()
    ): ValidationResult {
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
     * Formats a decimal number for display with appropriate precision.
     */
    fun formatDecimal(value: Double, maxDecimalPlaces: Int = 1): String {
        return when (maxDecimalPlaces) {
            0 -> "%.0f".format(value)
            1 -> decimalFormat.format(value)
            else -> "%.${maxDecimalPlaces}f".format(value)
        }
    }

}

/**
 * Enhanced text field with built-in validation and error display.
 */
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

