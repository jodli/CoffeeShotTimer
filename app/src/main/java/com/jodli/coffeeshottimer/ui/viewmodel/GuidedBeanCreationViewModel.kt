package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.data.onboarding.GuidedBeanCreationUiState
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the guided bean creation flow during onboarding.
 * The bean form itself lives in AddEditBeanScreen (onboarding mode); this
 * view model only moves between education, form and success phases and
 * translates the created bean into a freshness message.
 */
@HiltViewModel
class GuidedBeanCreationViewModel @Inject constructor(
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
            currentPhase = BeanCreationPhase.FORM
        )
    }

    /**
     * Return from form phase to education phase
     */
    fun returnToEducation() {
        _uiState.value = _uiState.value.copy(
            currentPhase = BeanCreationPhase.EDUCATION
        )
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
                createdBean = bean
            )
        }
    }

    /**
     * Get freshness message for the created bean
     */
    fun getFreshnessMessage(bean: Bean): String {
        val daysSinceRoast = bean.daysSinceRoast()

        // If roast date is today (likely a default), show a neutral message
        if (daysSinceRoast == 0L) {
            return domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_new
            )
        }

        return when {
            daysSinceRoast < Bean.FRESH_MIN_DAYS -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_fresh
            )
            daysSinceRoast <= Bean.FRESH_MAX_DAYS -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_good
            )
            else -> domainErrorTranslator.getString(
                com.jodli.coffeeshottimer.R.string.bean_creation_success_freshness_aging
            )
        }
    }
}
