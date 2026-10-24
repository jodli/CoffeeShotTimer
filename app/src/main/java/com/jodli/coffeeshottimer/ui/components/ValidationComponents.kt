package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
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

        // Manual timer constants
        const val MANUAL_TIMER_MIN_SECONDS = 5
        const val MANUAL_TIMER_MAX_SECONDS = 60
        const val MANUAL_TIMER_DEFAULT_SECONDS = 25 // Default to optimal extraction time

        // Text validation constants
        private const val MIN_BEAN_NAME_LENGTH = 2
        private const val MAX_BEAN_NAME_LENGTH = 100
        const val MAX_NOTES_LENGTH = 500

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

    // Brew ratio constants
    val MIN_TYPICAL_BREW_RATIO = Companion.MIN_TYPICAL_BREW_RATIO
    val MAX_TYPICAL_BREW_RATIO = Companion.MAX_TYPICAL_BREW_RATIO
    val OPTIMAL_BREW_RATIO_MIN = Companion.OPTIMAL_BREW_RATIO_MIN
    val OPTIMAL_BREW_RATIO_MAX = Companion.OPTIMAL_BREW_RATIO_MAX

    private val decimalFormat = DecimalFormat("#.#")

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
 * Gentle validation message component that provides helpful feedback with suggestions.
 * Used for onboarding and user-friendly error messaging.
 */
@Composable
fun GentleValidationMessage(
    message: String,
    suggestion: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            if (suggestion.isNotBlank()) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
