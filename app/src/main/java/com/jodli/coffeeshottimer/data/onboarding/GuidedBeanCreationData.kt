package com.jodli.coffeeshottimer.data.onboarding

import com.jodli.coffeeshottimer.data.model.Bean
import java.time.LocalDate

/**
 * Enum representing the different phases of guided bean creation
 */
enum class BeanCreationPhase {
    EDUCATION,  // Learning about bean tracking
    FORM,       // Filling out bean details
    SUCCESS     // Bean successfully created
}

/**
 * Enum representing different form fields for validation
 */
enum class BeanFormField {
    NAME,
    ROAST_DATE,
    PHOTO_PATH,
    NOTES
}

/**
 * Data class representing the state of the bean creation form
 */
data class BeanFormState(
    val name: String = "",
    val roastDate: LocalDate? = null,
    val photoPath: String? = null,
    val notes: String = "",
    val nameError: String? = null,
    val roastDateError: String? = null,
    val isSubmitting: Boolean = false
) {
    /**
     * Checks if the form is valid for submission
     * Only bean name is required - roast date, photo, and notes are optional
     */
    val isValid: Boolean
        get() = name.isNotBlank() && 
                nameError == null && 
                roastDateError == null

    /**
     * Checks if the form has any content (for dirty state detection)
     */
    val hasContent: Boolean
        get() = name.isNotBlank() || roastDate != null || !photoPath.isNullOrBlank() || notes.isNotBlank()
}

/**
 * Data class representing the complete UI state of guided bean creation
 */
data class GuidedBeanCreationUiState(
    val currentPhase: BeanCreationPhase = BeanCreationPhase.EDUCATION,
    val formState: BeanFormState = BeanFormState(),
    val createdBean: Bean? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Checks if the user can navigate back
     */
    val canNavigateBack: Boolean
        get() = currentPhase != BeanCreationPhase.EDUCATION

    /**
     * Checks if the creation process is complete
     */
    val isComplete: Boolean
        get() = currentPhase == BeanCreationPhase.SUCCESS && createdBean != null
}