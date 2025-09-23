package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.data.onboarding.BeanFormField
import com.jodli.coffeeshottimer.data.onboarding.BeanFormState
import com.jodli.coffeeshottimer.data.onboarding.GuidedBeanCreationUiState
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the guided bean creation flow during onboarding.
 * Manages the multi-phase process of educating users about bean tracking
 * and helping them create their first bean entry.
 */
@HiltViewModel
class GuidedBeanCreationViewModel @Inject constructor(
    private val beanRepository: BeanRepository,
    private val onboardingManager: OnboardingManager,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuidedBeanCreationUiState())
    val uiState: StateFlow<GuidedBeanCreationUiState> = _uiState.asStateFlow()

    /**
     * Proceed from education phase to form phase
     */
    fun proceedToForm() {
        _uiState.value = _uiState.value.copy(
            currentPhase = BeanCreationPhase.FORM,
            error = null
        )
    }

    /**
     * Return from form phase to education phase
     */
    fun returnToEducation() {
        _uiState.value = _uiState.value.copy(
            currentPhase = BeanCreationPhase.EDUCATION,
            error = null
        )
    }

    /**
     * Update a form field with validation
     */
    fun updateField(field: BeanFormField, value: String) {
        val currentFormState = _uiState.value.formState

        val updatedFormState = when (field) {
            BeanFormField.NAME -> {
                currentFormState.copy(
                    name = value,
                    nameError = validateBeanName(value)
                )
            }
            BeanFormField.NOTES -> {
                currentFormState.copy(notes = value)
            }
            BeanFormField.PHOTO_PATH -> {
                currentFormState.copy(photoPath = value.ifBlank { null })
            }
            BeanFormField.ROAST_DATE -> {
                try {
                    val date = LocalDate.parse(value)
                    currentFormState.copy(
                        roastDate = date,
                        roastDateError = validateRoastDate(date)
                    )
                } catch (e: Exception) {
                    currentFormState.copy(
                        roastDate = null,
                        roastDateError = "Invalid date format"
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            formState = updatedFormState,
            error = null
        )
    }

    /**
     * Update roast date field
     */
    fun updateRoastDate(date: LocalDate) {
        val updatedFormState = _uiState.value.formState.copy(
            roastDate = date,
            roastDateError = validateRoastDate(date)
        )

        _uiState.value = _uiState.value.copy(
            formState = updatedFormState,
            error = null
        )
    }

    /**
     * Create the bean with validation
     */
    fun createBean() {
        val formState = _uiState.value.formState

        // Validate all fields before submission
        val validatedFormState = validateForm(formState)

        if (!validatedFormState.isValid) {
            _uiState.value = _uiState.value.copy(
                formState = validatedFormState,
                error = "Please fill in all required fields correctly"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            formState = validatedFormState.copy(isSubmitting = true),
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val bean = Bean(
                    id = UUID.randomUUID().toString(),
                    name = formState.name.trim(),
                    roastDate = formState.roastDate ?: LocalDate.now(), // Use current date if not specified
                    notes = formState.notes.trim().ifBlank { null } ?: "",
                    photoPath = formState.photoPath,
                    isActive = true,
                    lastGrinderSetting = null
                )

                val result = beanRepository.addBean(bean)

                result.fold(
                    onSuccess = {
                        // Update onboarding progress
                        val currentProgress = onboardingManager.getOnboardingProgress()
                        val updatedProgress = currentProgress.copy(hasCreatedFirstBean = true)
                        onboardingManager.updateOnboardingProgress(updatedProgress)

                        // Move to success phase
                        _uiState.value = _uiState.value.copy(
                            currentPhase = BeanCreationPhase.SUCCESS,
                            createdBean = bean,
                            isLoading = false,
                            formState = formState.copy(isSubmitting = false)
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = domainErrorTranslator.translateError(exception),
                            formState = formState.copy(isSubmitting = false)
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateError(e),
                    formState = formState.copy(isSubmitting = false)
                )
            }
        }
    }

    /**
     * Handle bean created from AddEditBeanScreen in onboarding mode
     */
    fun onBeanCreated(bean: Bean) {
        viewModelScope.launch {
            // Update onboarding progress
            val currentProgress = onboardingManager.getOnboardingProgress()
            val updatedProgress = currentProgress.copy(hasCreatedFirstBean = true)
            onboardingManager.updateOnboardingProgress(updatedProgress)

            // Move to success phase
            _uiState.value = _uiState.value.copy(
                currentPhase = BeanCreationPhase.SUCCESS,
                createdBean = bean,
                isLoading = false
            )
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Comprehensive validation for the entire form
     */
    private fun validateForm(formState: BeanFormState): BeanFormState {
        val nameError = validateBeanName(formState.name)
        val roastDateError = formState.roastDate?.let { validateRoastDate(it) }

        return formState.copy(
            nameError = nameError,
            roastDateError = roastDateError
        )
    }

    /**
     * Validate bean name
     */
    private fun validateBeanName(name: String): String? {
        val trimmedName = name.trim()
        return when {
            trimmedName.isBlank() -> "Bean name is required"
            trimmedName.length < 2 -> "Bean name must be at least 2 characters"
            trimmedName.length > 100 -> "Bean name cannot exceed 100 characters"
            !trimmedName.matches(Regex("^[a-zA-Z0-9\\s\\-_.'&()]+$")) ->
                "Bean name contains invalid characters"
            else -> null
        }
    }

    /**
     * Validate roast date
     */
    private fun validateRoastDate(date: LocalDate): String? {
        val today = LocalDate.now()
        val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(date, today)

        return when {
            date.isAfter(today) -> "Roast date cannot be in the future"
            daysSinceRoast > 365 -> "Roast date cannot be more than 365 days ago"
            else -> null
        }
    }

    /**
     * Get freshness message for the created bean
     */
    fun getFreshnessMessage(bean: Bean): String {
        val today = LocalDate.now()
        val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(bean.roastDate, today)

        // If roast date is today (likely a default), show a neutral message
        if (daysSinceRoast == 0L) {
            return "Your bean is ready for brewing!"
        }

        return when {
            daysSinceRoast < 4 -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_fresh
            )
            daysSinceRoast <= 21 -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_good
            )
            else -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_aging
            )
        }
    }
}
