package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import java.time.LocalDate

/**
 * Extension functions for enhanced validation with user-friendly messages.
 * These extensions work with ValidationUtils that require a ValidationStringProvider.
 */

// Weight validation functions removed - sliders now handle all weight validation by constraining values to basket configuration ranges
// Grinder validation function removed - GrinderSettingSlider constrains values to grinder configuration ranges

/**
 * Validates bean name with enhanced error messages and suggestions.
 */
fun String.validateBeanNameEnhanced(
    validationUtils: ValidationUtils,
    existingNames: List<String> = emptyList()
): ValidationResult {
    return validationUtils.validateBeanName(this, existingNames).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            val trimmedName = this.trim()

            when {
                trimmedName.length == 1 ->
                    enhancedErrors.add(validationUtils.stringProvider.getDescriptiveNameTip())

                trimmedName.contains(Regex("[^a-zA-Z0-9\\s\\-_&.()]")) ->
                    enhancedErrors.add(validationUtils.stringProvider.getBasicPunctuationTip())

                existingNames.any { it.equals(trimmedName, ignoreCase = true) } ->
                    enhancedErrors.add(validationUtils.stringProvider.getUniqueNameTip())
            }

            ValidationResult(false, enhancedErrors)
        } else {
            result
        }
    }
}

/**
 * Validates roast date with enhanced contextual messages.
 */
fun LocalDate.validateRoastDateEnhanced(validationUtils: ValidationUtils): ValidationResult {
    return validationUtils.validateRoastDate(this).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            val today = LocalDate.now()

            when {
                this.isAfter(today) ->
                    enhancedErrors.add(validationUtils.stringProvider.getRoastDateTodayTip())

                this.isBefore(today.minusDays(30)) ->
                    enhancedErrors.add(validationUtils.stringProvider.getOldBeansNote())
            }

            ValidationResult(false, enhancedErrors)
        } else {
            result
        }
    }
}

/**
 * Validates notes with character count and helpful suggestions.
 */
fun String.validateNotesEnhanced(validationUtils: ValidationUtils): ValidationResult {
    return validationUtils.validateNotes(this).let { result ->
        val warnings = mutableListOf<String>()

        // Add helpful suggestions for notes
        when {
            this.length > ValidationUtils.MAX_NOTES_LENGTH * 0.8 ->
                warnings.add(validationUtils.stringProvider.getCharacterLimitNote())
        }

        if (!result.isValid) {
            ValidationResult(false, result.errors + warnings)
        } else {
            ValidationResult(true, warnings)
        }
    }
}

/**
 * Generates warnings for brew ratio based on espresso standards.
 */
fun Double.getBrewRatioWarnings(validationUtils: ValidationUtils): List<String> {
    val warnings = mutableListOf<String>()

    when {
        this < ValidationUtils.MIN_TYPICAL_BREW_RATIO ->
            warnings.add(validationUtils.stringProvider.getRatioConcentratedWarning())

        this > ValidationUtils.MAX_TYPICAL_BREW_RATIO ->
            warnings.add(validationUtils.stringProvider.getRatioDilutedWarning())

        this < ValidationUtils.OPTIMAL_BREW_RATIO_MIN ->
            warnings.add(validationUtils.stringProvider.getRatioHigherWarning())

        this > ValidationUtils.OPTIMAL_BREW_RATIO_MAX ->
            warnings.add(validationUtils.stringProvider.getRatioLowerWarning())
    }

    return warnings
}
