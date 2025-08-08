package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.components.WeightSliderConstants
import java.time.LocalDate

/**
 * Extension functions for enhanced validation with user-friendly messages.
 * These extensions work with ValidationUtils that require a ValidationStringProvider.
 */

/**
 * Validates coffee input weight with contextual error messages.
 */
fun String.validateCoffeeWeightIn(validationUtils: ValidationUtils): ValidationResult {
    return validationUtils.validateCoffeeWeight(
        value = this,
        fieldName = validationUtils.stringProvider.getCoffeeInputWeightLabel(),
        minWeight = WeightSliderConstants.COFFEE_IN_MIN_WEIGHT.toDouble(),
        maxWeight = WeightSliderConstants.COFFEE_IN_MAX_WEIGHT.toDouble()
    )
}

/**
 * Validates coffee output weight with contextual error messages.
 */
fun String.validateCoffeeWeightOut(validationUtils: ValidationUtils): ValidationResult {
    return validationUtils.validateCoffeeWeight(
        value = this,
        fieldName = validationUtils.stringProvider.getCoffeeOutputWeightLabel(),
        minWeight = WeightSliderConstants.COFFEE_OUT_MIN_WEIGHT.toDouble(),
        maxWeight = WeightSliderConstants.COFFEE_OUT_MAX_WEIGHT.toDouble()
    )
}

/**
 * Validates grinder setting with enhanced error messages.
 */
fun String.validateGrinderSettingEnhanced(validationUtils: ValidationUtils, isRequired: Boolean = true): ValidationResult {
    return validationUtils.validateGrinderSetting(this, isRequired).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()

            // Add helpful tips for grinder settings
            if (this.trim().isEmpty() && isRequired) {
                enhancedErrors.add(validationUtils.stringProvider.getGrinderSettingTip())
            }

            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates bean name with enhanced error messages and suggestions.
 */
fun String.validateBeanNameEnhanced(validationUtils: ValidationUtils, existingNames: List<String> = emptyList()): ValidationResult {
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
        } else result
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
        } else result
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
 * Validates extraction time with contextual feedback.
 */
fun Int.validateExtractionTimeEnhanced(validationUtils: ValidationUtils): ValidationResult {
    return validationUtils.validateExtractionTime(this).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()

            when {
                this < ValidationUtils.MIN_EXTRACTION_TIME ->
                    enhancedErrors.add(validationUtils.stringProvider.getShortExtractionSourTip())

                this > ValidationUtils.MAX_EXTRACTION_TIME ->
                    enhancedErrors.add(validationUtils.stringProvider.getLongExtractionBitterTip())
            }

            ValidationResult(false, enhancedErrors)
        } else result
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

/**
 * Generates warnings for extraction time based on espresso standards.
 */
fun Int.getExtractionTimeWarnings(validationUtils: ValidationUtils): List<String> {
    val warnings = mutableListOf<String>()

    when {
        this < ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN ->
            warnings.add(validationUtils.stringProvider.getGrindFinerWarning())

        this > ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX ->
            warnings.add(validationUtils.stringProvider.getGrindCoarserWarning())

        this in ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN..ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX ->
            warnings.add(validationUtils.stringProvider.getOptimalTimeSuccess())
    }

    return warnings
}

/**
 * Comprehensive shot validation with all parameters.
 */
fun validateCompleteShot(
    coffeeWeightIn: String,
    coffeeWeightOut: String,
    extractionTimeSeconds: Int,
    grinderSetting: String,
    notes: String,
    validationUtils: ValidationUtils
): ValidationResult {
    val allErrors = mutableListOf<String>()
    val allWarnings = mutableListOf<String>()

    // Validate individual fields
    val weightInResult = coffeeWeightIn.validateCoffeeWeightIn(validationUtils)
    val weightOutResult = coffeeWeightOut.validateCoffeeWeightOut(validationUtils)
    val timeResult = extractionTimeSeconds.validateExtractionTimeEnhanced(validationUtils)
    val grinderResult = grinderSetting.validateGrinderSettingEnhanced(validationUtils)
    val notesResult = notes.validateNotesEnhanced(validationUtils)

    // Collect errors
    allErrors.addAll(weightInResult.errors)
    allErrors.addAll(weightOutResult.errors)
    allErrors.addAll(timeResult.errors)
    allWarnings.addAll(grinderResult.errors)
    allWarnings.addAll(notesResult.errors)

    // If basic validation passes, check relationships
    if (allErrors.isEmpty()) {
				val weightIn = coffeeWeightIn.toDoubleOrNull()
				val weightOut = coffeeWeightOut.toDoubleOrNull()

				if (weightIn != null && weightOut != null) {
            val brewRatio = weightOut / weightIn
            allWarnings.addAll(brewRatio.getBrewRatioWarnings(validationUtils))
        }

        allWarnings.addAll(extractionTimeSeconds.getExtractionTimeWarnings(validationUtils))

        // Cross-field validation
        if (weightIn != null && weightOut != null && weightOut < weightIn) {
            allErrors.add(validationUtils.stringProvider.getOutputWeightLessThanInputError())
        }
    }

    return ValidationResult(allErrors.isEmpty(), allErrors + allWarnings)
}

/**
 * Comprehensive bean validation with all parameters.
 */
fun validateCompleteBean(
    name: String,
    roastDate: LocalDate,
    notes: String,
    grinderSetting: String,
    validationUtils: ValidationUtils,
    existingNames: List<String> = emptyList()
): ValidationResult {
    val allErrors = mutableListOf<String>()
    val allWarnings = mutableListOf<String>()

    // Validate individual fields
    val nameResult = name.validateBeanNameEnhanced(validationUtils, existingNames)
    val dateResult = roastDate.validateRoastDateEnhanced(validationUtils)
    val notesResult = notes.validateNotesEnhanced(validationUtils)
    val grinderResult = grinderSetting.validateGrinderSettingEnhanced(validationUtils, false) // Not required

    // Collect errors and warnings
    allErrors.addAll(nameResult.errors)
    allErrors.addAll(dateResult.errors)
    allWarnings.addAll(notesResult.errors)
    allWarnings.addAll(grinderResult.errors)

    // Add contextual warnings
    val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(roastDate, LocalDate.now())
    when {
        daysSinceRoast < 2 ->
            allWarnings.add(validationUtils.stringProvider.getVeryFreshBeansWarning())

        daysSinceRoast in 2..30 ->
            allWarnings.add(validationUtils.stringProvider.getFreshBeansSuccess())

        daysSinceRoast in 30..90 ->
            allWarnings.add(validationUtils.stringProvider.getAgingBeansWarning())

        daysSinceRoast > 90 ->
            allWarnings.add(validationUtils.stringProvider.getOldBeansWarning())
    }

    return ValidationResult(allErrors.isEmpty(), allErrors + allWarnings)
}
