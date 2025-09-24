package com.jodli.coffeeshottimer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.round

/**
 * Shot entity representing an espresso shot record in the database.
 * Stores information about espresso shots including weights, timing, and grinder settings.
 */
@Entity(
    tableName = "shots",
    foreignKeys = [
        ForeignKey(
            entity = Bean::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["beanId"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["grinderSetting"]),
        androidx.room.Index(value = ["beanId", "timestamp"])
    ]
)
data class Shot(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val beanId: String,
    val coffeeWeightIn: Double, // grams
    val coffeeWeightOut: Double, // grams
    val extractionTimeSeconds: Int,
    val grinderSetting: String,
    val notes: String = "",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val tastePrimary: TastePrimary? = null, // Primary taste feedback (Sour/Perfect/Bitter)
    val tasteSecondary: TasteSecondary? = null // Optional secondary qualifier (Weak/Strong)
) {
    /**
     * Calculates the brew ratio (output weight / input weight).
     * @return Brew ratio rounded to 2 decimal places
     */
    val brewRatio: Double
        get() = round((coffeeWeightOut / coffeeWeightIn) * 100) / 100

    /**
     * Validates the shot data according to business rules.
     * @return ValidationResult indicating if the shot is valid and any error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate coffee weight in (0.1g - 50.0g range)
        if (coffeeWeightIn < 0.1) {
            errors.add("Coffee input weight must be at least 0.1g")
        } else if (coffeeWeightIn > 50.0) {
            errors.add("Coffee input weight cannot exceed 50.0g")
        }

        // Validate coffee weight out (0.1g - 100.0g range)
        if (coffeeWeightOut < 0.1) {
            errors.add("Coffee output weight must be at least 0.1g")
        } else if (coffeeWeightOut > 100.0) {
            errors.add("Coffee output weight cannot exceed 100.0g")
        }

        // Validate extraction time (5 - 120 seconds)
        if (extractionTimeSeconds < 5) {
            errors.add("Extraction time must be at least 5 seconds")
        } else if (extractionTimeSeconds > 120) {
            errors.add("Extraction time cannot exceed 120 seconds")
        }

        // Validate grinder setting (required, max 50 characters)
        if (grinderSetting.isBlank()) {
            errors.add("Grinder setting cannot be empty")
        } else if (grinderSetting.length > 50) {
            errors.add("Grinder setting cannot exceed 50 characters")
        }

        // Validate bean ID (required)
        if (beanId.isBlank()) {
            errors.add("Bean ID cannot be empty")
        }

        // Validate notes (max 500 characters)
        if (notes.length > 500) {
            errors.add("Notes cannot exceed 500 characters")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Checks if the extraction time is within the optimal range for espresso (25-30 seconds).
     * @return true if the extraction time is optimal
     */
    fun isOptimalExtractionTime(): Boolean {
        return extractionTimeSeconds in 25..30
    }

    /**
     * Checks if the brew ratio is within a typical espresso range (1:1.5 to 1:3.0).
     * @return true if the brew ratio is within typical range
     */
    fun isTypicalBrewRatio(): Boolean {
        return brewRatio in 1.5..3.0
    }

    /**
     * Gets a formatted string representation of the brew ratio (e.g., "1:2.5").
     * @return Formatted brew ratio string
     */
    fun getFormattedBrewRatio(): String {
        return "1:${String.format(java.util.Locale.ROOT, "%.1f", brewRatio)}"
    }

    /**
     * Gets the extraction time formatted with seconds-only for times under 60s, MM:SS for longer times.
     * @return Formatted extraction time string
     */
    fun getFormattedExtractionTime(): String {
        val totalSeconds = maxOf(0, extractionTimeSeconds) // Handle negative values
        return when {
            totalSeconds < 60 -> "${totalSeconds}s"
            else -> {
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
