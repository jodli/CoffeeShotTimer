package com.jodli.coffeeshottimer.data.onboarding

import com.jodli.coffeeshottimer.data.model.Bean

/**
 * Enum representing the different phases of guided bean creation
 */
enum class BeanCreationPhase {
    EDUCATION, // Learning about bean tracking
    FORM, // Filling out bean details
    SUCCESS // Bean successfully created
}

/**
 * Data class representing the complete UI state of guided bean creation
 */
data class GuidedBeanCreationUiState(
    val currentPhase: BeanCreationPhase = BeanCreationPhase.EDUCATION,
    val createdBean: Bean? = null
)
