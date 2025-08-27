package com.jodli.coffeeshottimer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * BasketConfiguration entity representing basket size settings in the database.
 * Stores the minimum and maximum weight ranges for coffee in and coffee out.
 */
@Entity(
    tableName = "basket_configuration",
    indices = [
        androidx.room.Index(value = ["createdAt"]),
        androidx.room.Index(value = ["isActive"])
    ]
)
data class BasketConfiguration(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val coffeeInMin: Float,
    val coffeeInMax: Float,
    val coffeeOutMin: Float,
    val coffeeOutMax: Float,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true
) {
    /**
     * Validates the basket configuration according to business rules.
     * @return ValidationResult indicating if the configuration is valid and any error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate coffee in range
        if (coffeeInMin < 5f) {
            errors.add("Coffee in minimum cannot be less than 5g")
        }
        if (coffeeInMax > 30f) {
            errors.add("Coffee in maximum cannot exceed 30g")
        }
        if (coffeeInMin >= coffeeInMax) {
            errors.add("Coffee in minimum must be less than maximum")
        }
        
        // Validate coffee out range
        if (coffeeOutMin < 10f) {
            errors.add("Coffee out minimum cannot be less than 10g")
        }
        if (coffeeOutMax > 80f) {
            errors.add("Coffee out maximum cannot exceed 80g")
        }
        if (coffeeOutMin >= coffeeOutMax) {
            errors.add("Coffee out minimum must be less than maximum")
        }
        
        // Ensure minimum range size
        val coffeeInRange = coffeeInMax - coffeeInMin
        if (coffeeInRange < 3f) {
            errors.add("Coffee in range must be at least 3g")
        }
        
        val coffeeOutRange = coffeeOutMax - coffeeOutMin
        if (coffeeOutRange < 10f) {
            errors.add("Coffee out range must be at least 10g")
        }
        
        // Ensure reasonable ratio range (at least 1:1 to 1:4 possible)
        if (coffeeOutMin / coffeeInMax < 0.8f) {
            errors.add("Weight ranges don't allow reasonable brew ratios")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Gets the coffee in range size.
     * @return The range size in grams
     */
    fun getCoffeeInRangeSize(): Float {
        return coffeeInMax - coffeeInMin
    }
    
    /**
     * Gets the coffee out range size.
     * @return The range size in grams
     */
    fun getCoffeeOutRangeSize(): Float {
        return coffeeOutMax - coffeeOutMin
    }
    
    /**
     * Gets the middle value of the coffee in range.
     * @return The middle value, useful as a default starting point
     */
    fun getCoffeeInMiddleValue(): Float {
        return (coffeeInMin + coffeeInMax) / 2
    }
    
    /**
     * Gets the middle value of the coffee out range.
     * @return The middle value, useful as a default starting point
     */
    fun getCoffeeOutMiddleValue(): Float {
        return (coffeeOutMin + coffeeOutMax) / 2
    }
    
    /**
     * Checks if a coffee in value is within the configured range.
     * @param value The value to check
     * @return true if the value is within the range (inclusive)
     */
    fun isCoffeeInValueInRange(value: Float): Boolean {
        return value in coffeeInMin..coffeeInMax
    }
    
    /**
     * Checks if a coffee out value is within the configured range.
     * @param value The value to check
     * @return true if the value is within the range (inclusive)
     */
    fun isCoffeeOutValueInRange(value: Float): Boolean {
        return value in coffeeOutMin..coffeeOutMax
    }
    
    /**
     * Clamps a coffee in value to the configured range.
     * @param value The value to clamp
     * @return The value clamped to the range bounds
     */
    fun clampCoffeeInValue(value: Float): Float {
        return value.coerceIn(coffeeInMin, coffeeInMax)
    }
    
    /**
     * Clamps a coffee out value to the configured range.
     * @param value The value to clamp
     * @return The value clamped to the range bounds
     */
    fun clampCoffeeOutValue(value: Float): Float {
        return value.coerceIn(coffeeOutMin, coffeeOutMax)
    }
    
    companion object {
        /**
         * Single shot basket preset configuration.
         */
        val SINGLE_SHOT = BasketConfiguration(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            coffeeOutMin = 20f,
            coffeeOutMax = 40f
        )
        
        /**
         * Double shot basket preset configuration.
         */
        val DOUBLE_SHOT = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
        
        /**
         * Default configuration for new users (double shot).
         */
        val DEFAULT = DOUBLE_SHOT
    }
}

