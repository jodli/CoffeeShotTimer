package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.domain.usecase.GetShotDetailsUseCase
import com.example.coffeeshottimer.domain.usecase.ShotDetails
import com.example.coffeeshottimer.data.repository.ShotRepository
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
    private val shotRepository: ShotRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShotDetailsUiState())
    val uiState: StateFlow<ShotDetailsUiState> = _uiState.asStateFlow()
    
    private val _editNotesState = MutableStateFlow(EditNotesState())
    val editNotesState: StateFlow<EditNotesState> = _editNotesState.asStateFlow()
    
    /**
     * Load shot details by ID
     */
    fun loadShotDetails(shotId: String) {
        if (shotId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid shot ID"
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
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load shot details"
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
                        error = exception.message ?: "Failed to save notes"
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
                        error = exception.message ?: "Failed to delete shot"
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
}

/**
 * UI state for the Shot Details screen
 */
data class ShotDetailsUiState(
    val isLoading: Boolean = false,
    val shotDetails: ShotDetails? = null,
    val error: String? = null,
    val isDeletingShot: Boolean = false
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