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
    val stepSize: Double = 0.5,
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

        // Validate step size
        if (stepSize < 0.01) {
            errors.add("Step size must be at least 0.01")
        }

        if (stepSize > 10.0) {
            errors.add("Step size cannot exceed 10.0")
        }

        // Only allow 0.x steps (multiples of 0.1) when step size is below 1.0
        if (stepSize < 1.0) {
            val isMultipleOfTenth = kotlin.math.abs(stepSize * 10 - kotlin.math.round(stepSize * 10)) < 1e-6
            if (!isMultipleOfTenth) {
                errors.add("Step size must be a multiple of 0.1")
            }
        }

        // Check if step size makes sense for range
        if (stepSize > rangeSize) {
            errors.add("Step size cannot be larger than the range")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Formats a grind value according to the step size precision.
     * @param value The grind value to format
     * @return Formatted string representation
     */
    fun formatGrindValue(value: Double): String {
        return when {
            stepSize >= 1.0 -> value.toInt().toString()
            else -> String.format(java.util.Locale.ROOT, "%.1f", value)
        }
    }

    companion object {
        /**
         * Common preset configurations for popular grinder types.
         */
        val COMMON_PRESETS = listOf(
            GrinderConfiguration(scaleMin = 1, scaleMax = 10, stepSize = 0.5),
            GrinderConfiguration(scaleMin = 30, scaleMax = 80, stepSize = 1.0),
            GrinderConfiguration(scaleMin = 50, scaleMax = 60, stepSize = 0.1),
            GrinderConfiguration(scaleMin = 0, scaleMax = 100, stepSize = 1.0)
        )

        /**
         * Common step size presets.
         */
        val STEP_SIZE_PRESETS = listOf(0.1, 0.2, 0.5, 1.0)

        /**
         * Default configuration for new users.
         */
        val DEFAULT_CONFIGURATION = GrinderConfiguration(scaleMin = 1, scaleMax = 10, stepSize = 0.5)
    }
}
