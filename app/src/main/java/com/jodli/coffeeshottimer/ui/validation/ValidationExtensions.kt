package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import java.time.LocalDate

/**
 * Extension functions for enhanced validation with user-friendly messages.
 */

/**
 * Validates coffee input weight with contextual error messages.
 */
fun String.validateCoffeeWeightIn(): ValidationResult {
    return ValidationUtils.validateCoffeeWeight(
        value = this,
        fieldName = "Coffee input weight",
        minWeight = ValidationUtils.MIN_COFFEE_WEIGHT_IN,
        maxWeight = ValidationUtils.MAX_COFFEE_WEIGHT_IN
    ).let { result ->
        if (!result.isValid) {
            // Add contextual advice for common issues
            val enhancedErrors = result.errors.toMutableList()
            val weight = this.toDoubleOrNull()
            
            when {
                weight != null && weight < 5.0 -> 
                    enhancedErrors.add("Tip: Most espresso shots use 15-20g of coffee")
                weight != null && weight > 25.0 -> 
                    enhancedErrors.add("Tip: Standard espresso doses are typically 15-20g")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates coffee output weight with contextual error messages.
 */
fun String.validateCoffeeWeightOut(): ValidationResult {
    return ValidationUtils.validateCoffeeWeight(
        value = this,
        fieldName = "Coffee output weight",
        minWeight = ValidationUtils.MIN_COFFEE_WEIGHT_OUT,
        maxWeight = ValidationUtils.MAX_COFFEE_WEIGHT_OUT
    ).let { result ->
        if (!result.isValid) {
            // Add contextual advice for common issues
            val enhancedErrors = result.errors.toMutableList()
            val weight = this.toDoubleOrNull()
            
            when {
                weight != null && weight < 15.0 -> 
                    enhancedErrors.add("Tip: Most espresso shots yield 25-40g")
                weight != null && weight > 50.0 -> 
                    enhancedErrors.add("Tip: Standard espresso yields are typically 25-40g")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates grinder setting with enhanced error messages.
 */
fun String.validateGrinderSettingEnhanced(isRequired: Boolean = true): ValidationResult {
    return ValidationUtils.validateGrinderSetting(this, isRequired).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            
            // Add helpful tips for grinder settings
            if (this.trim().isEmpty() && isRequired) {
                enhancedErrors.add("Tip: Record your grinder setting to remember what worked well")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates bean name with enhanced error messages and suggestions.
 */
fun String.validateBeanNameEnhanced(existingNames: List<String> = emptyList()): ValidationResult {
    return ValidationUtils.validateBeanName(this, existingNames).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            val trimmedName = this.trim()
            
            when {
                trimmedName.length == 1 -> 
                    enhancedErrors.add("Tip: Try a more descriptive name like 'Ethiopian Yirgacheffe'")
                trimmedName.contains(Regex("[^a-zA-Z0-9\\s\\-_&.()]")) -> 
                    enhancedErrors.add("Tip: Use letters, numbers, spaces, and basic punctuation only")
                existingNames.any { it.equals(trimmedName, ignoreCase = true) } -> 
                    enhancedErrors.add("Tip: Try adding the roaster name or roast level to make it unique")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates roast date with enhanced contextual messages.
 */
fun LocalDate.validateRoastDateEnhanced(): ValidationResult {
    return ValidationUtils.validateRoastDate(this).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            val today = LocalDate.now()
            
            when {
                this.isAfter(today) -> 
                    enhancedErrors.add("Tip: Use today's date if you're not sure of the exact roast date")
                this.isBefore(today.minusDays(30)) -> 
                    enhancedErrors.add("Note: Beans older than 30 days may have lost some flavor")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Validates notes with character count and helpful suggestions.
 */
fun String.validateNotesEnhanced(): ValidationResult {
    return ValidationUtils.validateNotes(this).let { result ->
        val warnings = mutableListOf<String>()
        
        // Add helpful suggestions for notes
        when {
            this.trim().isEmpty() -> 
                warnings.add("Tip: Notes help you remember what you liked about this bean")
            this.length > ValidationUtils.MAX_NOTES_LENGTH * 0.8 -> 
                warnings.add("Note: You're approaching the character limit")
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
fun Int.validateExtractionTimeEnhanced(): ValidationResult {
    return ValidationUtils.validateExtractionTime(this).let { result ->
        if (!result.isValid) {
            val enhancedErrors = result.errors.toMutableList()
            
            when {
                this < ValidationUtils.MIN_EXTRACTION_TIME -> 
                    enhancedErrors.add("Tip: Very short extractions often taste sour")
                this > ValidationUtils.MAX_EXTRACTION_TIME -> 
                    enhancedErrors.add("Tip: Very long extractions often taste bitter")
            }
            
            ValidationResult(false, enhancedErrors)
        } else result
    }
}

/**
 * Generates warnings for brew ratio based on espresso standards.
 */
fun Double.getBrewRatioWarnings(): List<String> {
    val warnings = mutableListOf<String>()
    
    when {
        this < ValidationUtils.MIN_TYPICAL_BREW_RATIO -> 
            warnings.add("This ratio is quite concentrated - it might taste very strong")
        this > ValidationUtils.MAX_TYPICAL_BREW_RATIO -> 
            warnings.add("This ratio is quite diluted - it might taste weak")
        this < ValidationUtils.OPTIMAL_BREW_RATIO_MIN -> 
            warnings.add("Consider a slightly higher ratio for better balance")
        this > ValidationUtils.OPTIMAL_BREW_RATIO_MAX -> 
            warnings.add("Consider a slightly lower ratio for more intensity")
    }
    
    return warnings
}

/**
 * Generates warnings for extraction time based on espresso standards.
 */
fun Int.getExtractionTimeWarnings(): List<String> {
    val warnings = mutableListOf<String>()
    
    when {
        this < ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN -> 
            warnings.add("Consider grinding finer or using more coffee for longer extraction")
        this > ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX -> 
            warnings.add("Consider grinding coarser or using less coffee for shorter extraction")
        this in ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN..ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX -> 
            warnings.add("Great extraction time! This is in the optimal range for espresso")
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
    notes: String
): ValidationResult {
    val allErrors = mutableListOf<String>()
    val allWarnings = mutableListOf<String>()
    
    // Validate individual fields
    val weightInResult = coffeeWeightIn.validateCoffeeWeightIn()
    val weightOutResult = coffeeWeightOut.validateCoffeeWeightOut()
    val timeResult = extractionTimeSeconds.validateExtractionTimeEnhanced()
    val grinderResult = grinderSetting.validateGrinderSettingEnhanced()
    val notesResult = notes.validateNotesEnhanced()
    
    // Collect errors
    allErrors.addAll(weightInResult.errors)
    allErrors.addAll(weightOutResult.errors)
    allErrors.addAll(timeResult.errors)
    allErrors.addAll(grinderResult.errors)
    allErrors.addAll(notesResult.errors)
    
    // If basic validation passes, check relationships
    if (allErrors.isEmpty()) {
        val weightIn = coffeeWeightIn.toDoubleOrNull()
        val weightOut = coffeeWeightOut.toDoubleOrNull()
        
        if (weightIn != null && weightOut != null) {
            val brewRatio = weightOut / weightIn
            allWarnings.addAll(brewRatio.getBrewRatioWarnings())
        }
        
        allWarnings.addAll(extractionTimeSeconds.getExtractionTimeWarnings())
        
        // Cross-field validation
        if (weightIn != null && weightOut != null && weightOut < weightIn) {
            allErrors.add("Output weight cannot be less than input weight")
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
    existingNames: List<String> = emptyList()
): ValidationResult {
    val allErrors = mutableListOf<String>()
    val allWarnings = mutableListOf<String>()
    
    // Validate individual fields
    val nameResult = name.validateBeanNameEnhanced(existingNames)
    val dateResult = roastDate.validateRoastDateEnhanced()
    val notesResult = notes.validateNotesEnhanced()
    val grinderResult = grinderSetting.validateGrinderSettingEnhanced(false) // Not required
    
    // Collect errors and warnings
    allErrors.addAll(nameResult.errors)
    allErrors.addAll(dateResult.errors)
    allErrors.addAll(notesResult.errors)
    allErrors.addAll(grinderResult.errors)
    
    // Add contextual warnings
    val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(roastDate, LocalDate.now())
    when {
        daysSinceRoast < 2 -> 
            allWarnings.add("Very fresh beans - consider waiting 2-4 days for optimal flavor")
        daysSinceRoast in 2..4 -> 
            allWarnings.add("Fresh beans - perfect timing for espresso!")
        daysSinceRoast in 15..30 -> 
            allWarnings.add("Beans are getting older but still good for espresso")
        daysSinceRoast > 30 -> 
            allWarnings.add("Older beans - flavor may be diminished")
    }
    
    return ValidationResult(allErrors.isEmpty(), allErrors + allWarnings)
}