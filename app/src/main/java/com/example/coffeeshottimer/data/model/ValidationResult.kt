package com.example.coffeeshottimer.data.model

/**
 * Data class representing the result of data validation.
 * Used across different models to provide consistent validation feedback.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)