package com.jodli.coffeeshottimer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Bean entity representing a coffee bean profile in the database.
 * Stores information about coffee beans including name, roast date, and grinder settings.
 */
@Entity(
    tableName = "beans",
    indices = [
        androidx.room.Index(value = ["name"], unique = true),
        androidx.room.Index(value = ["isActive"]),
        androidx.room.Index(value = ["roastDate"]),
        androidx.room.Index(value = ["createdAt"]),
        androidx.room.Index(value = ["photoPath"])
    ]
)
data class Bean(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roastDate: LocalDate,
    val notes: String = "",
    val isActive: Boolean = true,
    val lastGrinderSetting: String? = null,
    val photoPath: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Validates the bean data according to business rules.
     * @return ValidationResult indicating if the bean is valid and any error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate bean name
        if (name.isBlank()) {
            errors.add("Bean name cannot be empty")
        } else if (name.length > 100) {
            errors.add("Bean name cannot exceed 100 characters")
        }

        // Validate roast date
        val today = LocalDate.now()
        if (roastDate.isAfter(today)) {
            errors.add("Roast date cannot be in the future")
        } else if (roastDate.isBefore(today.minusDays(365))) {
            errors.add("Roast date cannot be more than 365 days ago")
        }

        // Validate notes
        if (notes.length > 500) {
            errors.add("Notes cannot exceed 500 characters")
        }

        // Validate photo path
        photoPath?.let { path ->
            if (path.isBlank()) {
                errors.add("Photo path cannot be empty if provided")
            } else if (path.length > 500) {
                errors.add("Photo path cannot exceed 500 characters")
            } else if (!isValidPhotoPath(path)) {
                errors.add("Photo path must be a valid file path")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Calculates the number of days since the bean was roasted.
     * @return Number of days since roast date
     */
    fun daysSinceRoast(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(roastDate, LocalDate.now())
    }

    /**
     * Checks if the bean is considered fresh (within optimal brewing window).
     * Generally, espresso beans are considered optimal 4-21 days after roasting.
     * @return true if the bean is within the fresh window
     */
    fun isFresh(): Boolean {
        val daysSinceRoast = daysSinceRoast()
        return daysSinceRoast in 4..21
    }

    /**
     * Checks if the bean has a photo associated with it.
     * @return true if photoPath is not null and not blank
     */
    fun hasPhoto(): Boolean {
        return !photoPath.isNullOrBlank()
    }

    /**
     * Validates if the provided photo path is valid.
     * @param path The photo path to validate
     * @return true if the path is valid
     */
    private fun isValidPhotoPath(path: String): Boolean {
        // Basic validation for photo path
        // Should not contain invalid characters and should have reasonable structure
        val invalidChars = listOf("<", ">", ":", "\"", "|", "?", "*")
        return !invalidChars.any { path.contains(it) } && 
               path.trim() == path && // No leading/trailing whitespace
               !path.contains("..") // No directory traversal
    }
}

