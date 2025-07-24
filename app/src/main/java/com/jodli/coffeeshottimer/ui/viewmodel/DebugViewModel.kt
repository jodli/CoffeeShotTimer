package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.util.DatabasePopulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing debug dialog state and database operations.
 * Handles dialog visibility, loading states, and operation results.
 * Only available in debug builds to prevent accidental use in production.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val databasePopulator: DatabasePopulator
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
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                databasePopulator.populateForScreenshots()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Database filled with test data successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to fill database: ${e.message}"
                )
            }
        }
    }

    /**
     * Adds additional shots to existing beans for testing purposes.
     * 
     * @param count Number of additional shots to create (default: 10)
     */
    fun addMoreShots(count: Int = 10) {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                operationResult = null
            )

            try {
                databasePopulator.addMoreShots(count)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Added $count additional shots successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to add shots: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears all data from the database.
     * Coordinates the operation through DatabasePopulator and manages UI state.
     */
    fun clearDatabase() {
        if (!BuildConfig.DEBUG) return

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
                    operationResult = "Database cleared successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationResult = "Failed to clear database: ${e.message}"
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