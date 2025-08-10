package com.jodli.coffeeshottimer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * GrinderConfiguration entity representing grinder scale settings in the database.
 * Stores the minimum and maximum values for the grinder scale range.
 */
@Entity(
    tableName = "grinder_configuration",
    indices = [
        androidx.room.Index(value = ["createdAt"])
    ]
)
data class GrinderConfiguration(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scaleMin: Int,
    val scaleMax: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Validates the grinder configuration according to business rules.
     * @return ValidationResult indicating if the configuration is valid and any error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate scale range
        if (scaleMin >= scaleMax) {
            errors.add("Minimum scale value must be less than maximum scale value")
        }

        // Validate reasonable range bounds
        if (scaleMin < 0) {
            errors.add("Minimum scale value cannot be negative")
        }

        if (scaleMax > 1000) {
            errors.add("Maximum scale value cannot exceed 1000")
        }

        // Validate range size
        val rangeSize = scaleMax - scaleMin
        if (rangeSize < 3) {
            errors.add("Scale range must have at least 3 steps (current range: $rangeSize)")
        }

        if (rangeSize > 100) {
            errors.add("Scale range cannot exceed 100 steps (current range: $rangeSize)")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Gets the range size (number of steps between min and max).
     * @return The number of steps in the range
     */
    fun getRangeSize(): Int {
        return scaleMax - scaleMin
    }

    /**
     * Gets the middle value of the range.
     * @return The middle value, useful as a default starting point
     */
    fun getMiddleValue(): Int {
        return (scaleMin + scaleMax) / 2
    }

    /**
     * Checks if a given value is within the configured range.
     * @param value The value to check
     * @return true if the value is within the range (inclusive)
     */
    fun isValueInRange(value: Int): Boolean {
        return value in scaleMin..scaleMax
    }

    /**
     * Clamps a value to the configured range.
     * @param value The value to clamp
     * @return The value clamped to the range bounds
     */
    fun clampValue(value: Int): Int {
        return value.coerceIn(scaleMin, scaleMax)
    }

    companion object {
        /**
         * Common preset configurations for popular grinder types.
         */
        val COMMON_PRESETS = listOf(
            GrinderConfiguration(scaleMin = 1, scaleMax = 10),
            GrinderConfiguration(scaleMin = 30, scaleMax = 80),
            GrinderConfiguration(scaleMin = 50, scaleMax = 60),
            GrinderConfiguration(scaleMin = 0, scaleMax = 100)
        )

        /**
         * Default configuration for new users.
         */
        val DEFAULT_CONFIGURATION = GrinderConfiguration(scaleMin = 1, scaleMax = 10)
    }
}