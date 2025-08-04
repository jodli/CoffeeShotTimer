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
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import java.text.DecimalFormat
import java.time.LocalDate

/**
 * Validation utilities and components for consistent form validation across the app.
 */
class ValidationUtils(
    val stringProvider: ValidationStringProvider
) {

    companion object {
        // Constants remain accessible statically
        const val MIN_EXTRACTION_TIME = 5
        const val MAX_EXTRACTION_TIME = 120
        const val OPTIMAL_EXTRACTION_TIME_MIN = 25
        const val OPTIMAL_EXTRACTION_TIME_MAX = 30

        // Text validation constants
        private const val MIN_BEAN_NAME_LENGTH = 2
        private const val MAX_BEAN_NAME_LENGTH = 100
        const val MAX_NOTES_LENGTH = 500
        const val MAX_GRINDER_SETTING_LENGTH = 50

        // Brew ratio constants
        const val MIN_TYPICAL_BREW_RATIO = 1.5
        const val MAX_TYPICAL_BREW_RATIO = 3.0
        const val OPTIMAL_BREW_RATIO_MIN = 2.0
        const val OPTIMAL_BREW_RATIO_MAX = 2.5
    }

    // Time validation constants
    // NOTE: These are duplicated from companion object for instance access
    val MIN_EXTRACTION_TIME = Companion.MIN_EXTRACTION_TIME
    val MAX_EXTRACTION_TIME = Companion.MAX_EXTRACTION_TIME
    val OPTIMAL_EXTRACTION_TIME_MIN = Companion.OPTIMAL_EXTRACTION_TIME_MIN
    val OPTIMAL_EXTRACTION_TIME_MAX = Companion.OPTIMAL_EXTRACTION_TIME_MAX

    // Text validation constants
    private val MIN_BEAN_NAME_LENGTH = 2
    private val MAX_BEAN_NAME_LENGTH = 100
    val MAX_NOTES_LENGTH = Companion.MAX_NOTES_LENGTH
    val MAX_GRINDER_SETTING_LENGTH = Companion.MAX_GRINDER_SETTING_LENGTH

    // Brew ratio constants
    val MIN_TYPICAL_BREW_RATIO = Companion.MIN_TYPICAL_BREW_RATIO
    val MAX_TYPICAL_BREW_RATIO = Companion.MAX_TYPICAL_BREW_RATIO
    val OPTIMAL_BREW_RATIO_MIN = Companion.OPTIMAL_BREW_RATIO_MIN
    val OPTIMAL_BREW_RATIO_MAX = Companion.OPTIMAL_BREW_RATIO_MAX

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
                errors.add(stringProvider.getFieldRequiredError(fieldName))
            }
            return ValidationResult(errors.isEmpty(), errors)
        }

        val weight = value.toDoubleOrNull()
        when {
            weight == null -> errors.add(stringProvider.getValidNumberError())
            weight < minWeight -> errors.add(
                stringProvider.getMinimumWeightError(fieldName, decimalFormat.format(minWeight))
            )

            weight > maxWeight -> errors.add(
                stringProvider.getMaximumWeightError(fieldName, decimalFormat.format(maxWeight))
            )

            value.contains('.') && value.substringAfter('.').length > 1 ->
                errors.add(stringProvider.getOneDecimalPlaceError(fieldName))
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
                errors.add(stringProvider.getExtractionTimeMinimumError(MIN_EXTRACTION_TIME))

            timeSeconds > MAX_EXTRACTION_TIME ->
                errors.add(stringProvider.getExtractionTimeMaximumError(MAX_EXTRACTION_TIME))
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
            trimmedName.isEmpty() -> errors.add(stringProvider.getBeanNameRequiredError())
            trimmedName.length < MIN_BEAN_NAME_LENGTH ->
                errors.add(stringProvider.getBeanNameMinimumLengthError(MIN_BEAN_NAME_LENGTH))

            trimmedName.length > MAX_BEAN_NAME_LENGTH ->
                errors.add(stringProvider.getBeanNameMaximumLengthError(MAX_BEAN_NAME_LENGTH))

            !trimmedName.matches(Regex("^[a-zA-Z0-9\\s\\-_&.()]+$")) ->
                errors.add(stringProvider.getBeanNameInvalidCharactersError())

            existingNames.any { it.equals(trimmedName, ignoreCase = true) } ->
                errors.add(stringProvider.getBeanNameExistsError())
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
            date.isAfter(today) -> errors.add(stringProvider.getRoastDateFutureError())
            date.isBefore(today.minusDays(365)) ->
                errors.add(stringProvider.getRoastDateTooOldError())
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates notes with length checking.
     */
    fun validateNotes(notes: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (notes.length > MAX_NOTES_LENGTH) {
            errors.add(stringProvider.getNotesMaximumLengthError(MAX_NOTES_LENGTH))
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates grinder setting with length and character checking.
     * Supports both decimal points (.) and commas (,) for locale compatibility.
     */
    fun validateGrinderSetting(setting: String, isRequired: Boolean = true): ValidationResult {
        val errors = mutableListOf<String>()
        val trimmedSetting = setting.trim()

        when {
            trimmedSetting.isEmpty() && isRequired -> errors.add(stringProvider.getGrinderSettingRequiredError())
            trimmedSetting.length > MAX_GRINDER_SETTING_LENGTH ->
                errors.add(stringProvider.getGrinderSettingMaximumLengthError(MAX_GRINDER_SETTING_LENGTH))

            trimmedSetting.isNotEmpty() && !trimmedSetting.matches(Regex("^[a-zA-Z0-9\\s\\-_.,#]+$")) ->
                errors.add(stringProvider.getGrinderSettingInvalidCharactersError())
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
    validationUtils: ValidationUtils,
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
                            text = stringResource(R.string.validation_required_field),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
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
                        contentDescription = if (hasError) validationUtils.stringProvider.getErrorContentDescription() else validationUtils.stringProvider.getWarningContentDescription(),
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
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (hasWarning) {
                        Text(
                            text = warningMessage!!,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Character count for fields with max length
                    if (maxLength != null) {
                        Text(
                            text = stringResource(R.string.validation_character_count, value.length, maxLength),
                            style = MaterialTheme.typography.labelSmall,
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

