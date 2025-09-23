package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.domain.usecase.CalculateGrindAdjustmentUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotDetailsUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordTasteFeedbackUseCase
import com.jodli.coffeeshottimer.domain.usecase.ShotDetails
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Shot Details screen.
 * Manages shot details data, analysis, and user interactions.
 */
@HiltViewModel
class ShotDetailsViewModel @Inject constructor(
    private val getShotDetailsUseCase: GetShotDetailsUseCase,
    private val recordTasteFeedbackUseCase: RecordTasteFeedbackUseCase,
    private val calculateGrindAdjustmentUseCase: CalculateGrindAdjustmentUseCase,
    private val shotRepository: ShotRepository,
    private val stringResourceProvider: StringResourceProvider,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShotDetailsUiState())
    val uiState: StateFlow<ShotDetailsUiState> = _uiState.asStateFlow()

    private val _editNotesState = MutableStateFlow(EditNotesState())
    val editNotesState: StateFlow<EditNotesState> = _editNotesState.asStateFlow()

    private val _grindAdjustmentRecommendation = MutableStateFlow<GrindAdjustmentRecommendation?>(null)
    val grindAdjustmentRecommendation: StateFlow<GrindAdjustmentRecommendation?> = _grindAdjustmentRecommendation.asStateFlow()

    /**
     * Load shot details by ID
     */
    fun loadShotDetails(shotId: String) {
        if (shotId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = stringResourceProvider.getString(R.string.validation_empty_id)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            getShotDetailsUseCase.getShotDetails(shotId).fold(
                onSuccess = { shotDetails ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shotDetails = shotDetails,
                        error = null
                    )
                    // Initialize edit notes state
                    _editNotesState.value = _editNotesState.value.copy(
                        notes = shotDetails.shot.notes,
                        originalNotes = shotDetails.shot.notes
                    )
                    // Always calculate grind adjustment recommendation (timing-based if no taste)
                    calculateGrindAdjustmentRecommendation(shotDetails)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = domainErrorTranslator.translateError(exception)
                    )
                }
            )
        }
    }

    /**
     * Refresh shot details
     */
    fun refreshShotDetails() {
        _uiState.value.shotDetails?.let { details ->
            loadShotDetails(details.shot.id)
        }
    }

    /**
     * Start editing notes
     */
    fun startEditingNotes() {
        _editNotesState.value = _editNotesState.value.copy(
            isEditing = true,
            notes = _uiState.value.shotDetails?.shot?.notes ?: "",
            originalNotes = _uiState.value.shotDetails?.shot?.notes ?: ""
        )
    }

    /**
     * Update notes text
     */
    fun updateNotes(notes: String) {
        _editNotesState.value = _editNotesState.value.copy(
            notes = notes,
            hasChanges = notes != _editNotesState.value.originalNotes
        )
    }

    /**
     * Save notes changes
     */
    fun saveNotes() {
        val currentShot = _uiState.value.shotDetails?.shot ?: return
        val newNotes = _editNotesState.value.notes

        _editNotesState.value = _editNotesState.value.copy(isSaving = true)

        viewModelScope.launch {
            val updatedShot = currentShot.copy(notes = newNotes)
            shotRepository.updateShot(updatedShot).fold(
                onSuccess = {
                    _editNotesState.value = _editNotesState.value.copy(
                        isEditing = false,
                        isSaving = false,
                        hasChanges = false,
                        originalNotes = newNotes
                    )
                    // Refresh shot details to get updated data
                    refreshShotDetails()
                },
                onFailure = { exception ->
                    _editNotesState.value = _editNotesState.value.copy(
                        isSaving = false,
                        error = domainErrorTranslator.translateError(exception)
                    )
                }
            )
        }
    }

    /**
     * Cancel notes editing
     */
    fun cancelEditingNotes() {
        _editNotesState.value = _editNotesState.value.copy(
            isEditing = false,
            notes = _editNotesState.value.originalNotes,
            hasChanges = false,
            error = null
        )
    }

    /**
     * Delete the current shot
     */
    fun deleteShot(onDeleted: () -> Unit) {
        val currentShot = _uiState.value.shotDetails?.shot ?: return

        _uiState.value = _uiState.value.copy(isDeletingShot = true)

        viewModelScope.launch {
            shotRepository.deleteShot(currentShot).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeletingShot = false)
                    onDeleted()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeletingShot = false,
                        error = exception.message ?: stringResourceProvider.getString(R.string.error_failed_to_delete_shot)
                    )
                }
            )
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _editNotesState.value = _editNotesState.value.copy(error = null)
    }

    /**
     * Navigate to previous shot
     */
    fun navigateToPreviousShot(onNavigate: (String) -> Unit) {
        _uiState.value.shotDetails?.previousShot?.let { previousShot ->
            onNavigate(previousShot.id)
        }
    }

    /**
     * Navigate to next shot
     */
    fun navigateToNextShot(onNavigate: (String) -> Unit) {
        _uiState.value.shotDetails?.nextShot?.let { nextShot ->
            onNavigate(nextShot.id)
        }
    }

    /**
     * Update taste feedback for the current shot
     */
    fun updateTasteFeedback(
        tastePrimary: TastePrimary?,
        tasteSecondary: TasteSecondary?
    ) {
        val currentShot = _uiState.value.shotDetails?.shot ?: return

        _uiState.value = _uiState.value.copy(isUpdatingTaste = true)

        viewModelScope.launch {
            if (tastePrimary != null) {
                // Record taste feedback
                recordTasteFeedbackUseCase(
                    shotId = currentShot.id,
                    tastePrimary = tastePrimary,
                    tasteSecondary = tasteSecondary
                )
            } else {
                // Clear taste feedback
                recordTasteFeedbackUseCase.clearTasteFeedback(currentShot.id)
            }

            // Always refresh to show updated taste
            _uiState.value = _uiState.value.copy(isUpdatingTaste = false)
            refreshShotDetails()
        }
    }

    /**
     * Calculate grind adjustment recommendation for the current shot.
     * Uses the proper domain use case instead of hardcoded logic.
     */
    private fun calculateGrindAdjustmentRecommendation(shotDetails: ShotDetails) {
        viewModelScope.launch {
            calculateGrindAdjustmentUseCase.calculateAdjustment(
                currentGrindSetting = shotDetails.shot.grinderSetting,
                extractionTimeSeconds = shotDetails.shot.extractionTimeSeconds,
                tasteFeedback = shotDetails.shot.tastePrimary
            ).fold(
                onSuccess = { recommendation ->
                    _grindAdjustmentRecommendation.value = recommendation
                },
                onFailure = {
                    // Clear recommendation on error (don't block UI)
                    _grindAdjustmentRecommendation.value = null
                }
            )
        }
    }
}

/**
 * UI state for the Shot Details screen
 */
data class ShotDetailsUiState(
    val isLoading: Boolean = false,
    val shotDetails: ShotDetails? = null,
    val error: String? = null,
    val isDeletingShot: Boolean = false,
    val isUpdatingTaste: Boolean = false
)

/**
 * State for editing shot notes
 */
data class EditNotesState(
    val isEditing: Boolean = false,
    val notes: String = "",
    val originalNotes: String = "",
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
