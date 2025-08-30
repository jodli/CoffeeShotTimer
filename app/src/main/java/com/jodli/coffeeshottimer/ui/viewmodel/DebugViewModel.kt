package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.util.DatabasePopulator
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.R

/**
 * ViewModel for managing debug dialog state and database operations.
 * Handles dialog visibility, loading states, and operation results.
 * Only available in debug builds to prevent accidental use in production.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val databasePopulator: DatabasePopulator?,
    private val onboardingManager: OnboardingManager,
    private val stringResourceProvider: StringResourceProvider,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    /**
     * Shows the debug dialog.
     * Only functional in debug builds.
     */
    fun showDialog() {
        if (BuildConfig.DEBUG) {
            _uiState.value = _uiState.value.copy(
                isDialogVisible = true,
                operationResult = null,
                showConfirmation = false
            )
        }
    }

    /**
     * Hides the debug dialog and resets state.
     */
    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            isDialogVisible = false,
            isLoading = false,
            operationResult = null,
            showConfirmation = false
        )
    }

    /**
     * Shows confirmation dialog for destructive operations.
     */
    fun showConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = true)
    }

    /**
     * Hides confirmation dialog.
     */
    fun hideConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = false)
    }

    /**
     * Fills the database with realistic test data for screenshots.
     * Coordinates the operation through DatabasePopulator and manages UI state.
     */
    fun fillDatabase() {
        if (!BuildConfig.DEBUG || databasePopulator == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                databasePopulator.populateForScreenshots()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = stringResourceProvider.getString(R.string.text_database_filled)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = domainErrorTranslator.getLoadingError("fill database") + ": ${domainErrorTranslator.translateError(e)}"
                )
            }
        }
    }

    // ONBOARDING DEBUG METHODS

    /**
     * Resets onboarding state to simulate a new user.
     * Clears all onboarding progress so the user will see the full onboarding flow.
     */
    fun resetToNewUser() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                onboardingManager.resetToNewUser()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Reset to new user - will see full onboarding flow"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to reset onboarding: ${domainErrorTranslator.translateError(e)}"
                )
            }
        }
    }


    /**
     * Configures onboarding state to simulate an existing user who hasn't created beans.
     * Will skip introduction but show equipment setup and bean creation.
     */
    fun resetToExistingUserNoBeans() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                onboardingManager.resetToExistingUserNoBeans()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Set as existing user without beans - will see equipment + bean creation"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to set user state: ${domainErrorTranslator.translateError(e)}"
                )
            }
        }
    }

    /**
     * Forces equipment setup to appear by setting the version to an older value.
     * Simulates the scenario where new equipment features have been added.
     */
    fun forceEquipmentSetup() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                onboardingManager.forceEquipmentSetup()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Forced equipment setup to appear due to 'new features'"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to force equipment setup: ${domainErrorTranslator.translateError(e)}"
                )
            }
        }
    }

    /**
     * Clears all data from the database.
     * Coordinates the operation through DatabasePopulator and manages UI state.
     */
    fun clearDatabase() {
        if (!BuildConfig.DEBUG || databasePopulator == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null,
                showConfirmation = false
            )

            try {
                databasePopulator.clearAllData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = stringResourceProvider.getString(R.string.text_database_cleared)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = domainErrorTranslator.getDeleteError() + ": ${domainErrorTranslator.translateError(e)}"
                )
            }
        }
    }


    /**
     * Clears any operation result message.
     */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(operationResult = null)
    }
}

/**
 * UI state for the debug dialog.
 * Manages dialog visibility, loading states, and operation results.
 */
data class DebugUiState(
    val isDialogVisible: Boolean = false,
    val isLoading: Boolean = false,
    val operationResult: String? = null,
    val showConfirmation: Boolean = false
)