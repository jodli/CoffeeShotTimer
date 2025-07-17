package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.example.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for adding and editing coffee bean profiles.
 * Handles form validation, bean creation, and updates.
 */
@HiltViewModel
class AddEditBeanViewModel @Inject constructor(
    private val addBeanUseCase: AddBeanUseCase,
    private val updateBeanUseCase: UpdateBeanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditBeanUiState())
    val uiState: StateFlow<AddEditBeanUiState> = _uiState.asStateFlow()

    private var editingBeanId: String? = null

    fun initializeForEdit(beanId: String) {
        editingBeanId = beanId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = updateBeanUseCase.getBeanForEditing(beanId)
            if (result.isSuccess) {
                val bean = result.getOrNull()
                if (bean != null) {
                    _uiState.value = _uiState.value.copy(
                        name = bean.name,
                        roastDate = bean.roastDate,
                        notes = bean.notes,
                        isActive = bean.isActive,
                        lastGrinderSetting = bean.lastGrinderSetting ?: "",
                        isLoading = false,
                        isEditMode = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Bean not found"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load bean"
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = null
        )
        validateName(name)
    }

    fun updateRoastDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            roastDate = date,
            roastDateError = null
        )
        validateRoastDate(date)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(
            notes = notes,
            notesError = null
        )
        validateNotes(notes)
    }

    fun updateIsActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isActive = isActive)
    }

    fun updateLastGrinderSetting(setting: String) {
        _uiState.value = _uiState.value.copy(lastGrinderSetting = setting)
    }

    private fun validateName(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            when {
                trimmedName.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(nameError = "Bean name is required")
                }
                trimmedName.length > 100 -> {
                    _uiState.value = _uiState.value.copy(nameError = "Bean name cannot exceed 100 characters")
                }
                trimmedName.length < 2 -> {
                    _uiState.value = _uiState.value.copy(nameError = "Bean name must be at least 2 characters")
                }
                !trimmedName.matches(Regex("^[a-zA-Z0-9\\s\\-_&.()]+$")) -> {
                    _uiState.value = _uiState.value.copy(nameError = "Bean name contains invalid characters")
                }
                else -> {
                    // Check uniqueness
                    val isAvailable = if (editingBeanId != null) {
                        updateBeanUseCase.isBeanNameAvailableForUpdate(editingBeanId!!, trimmedName)
                    } else {
                        addBeanUseCase.isBeanNameAvailable(trimmedName)
                    }
                    
                    if (isAvailable.isSuccess) {
                        if (isAvailable.getOrNull() == false) {
                            _uiState.value = _uiState.value.copy(nameError = "Bean name already exists")
                        } else {
                            _uiState.value = _uiState.value.copy(nameError = null)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(nameError = "Unable to validate bean name")
                    }
                }
            }
        }
    }

    private fun validateRoastDate(date: LocalDate) {
        val today = LocalDate.now()
        val error = when {
            date.isAfter(today) -> "Roast date cannot be in the future"
            date.isBefore(today.minusDays(365)) -> "Roast date cannot be more than 365 days ago"
            else -> null
        }
        _uiState.value = _uiState.value.copy(roastDateError = error)
    }

    private fun validateNotes(notes: String) {
        val error = if (notes.length > 500) {
            "Notes cannot exceed 500 characters"
        } else null
        _uiState.value = _uiState.value.copy(notesError = error)
    }

    fun saveBean() {
        val currentState = _uiState.value
        
        // Validate all fields
        validateName(currentState.name)
        validateRoastDate(currentState.roastDate)
        validateNotes(currentState.notes)
        
        // Check if there are any validation errors
        if (currentState.nameError != null || 
            currentState.roastDateError != null || 
            currentState.notesError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            val result = if (editingBeanId != null) {
                updateBeanUseCase.execute(
                    beanId = editingBeanId!!,
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                    lastGrinderSetting = currentState.lastGrinderSetting.trim().takeIf { it.isNotEmpty() }
                )
            } else {
                addBeanUseCase.execute(
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                    lastGrinderSetting = currentState.lastGrinderSetting.trim().takeIf { it.isNotEmpty() }
                )
            }
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save bean"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    /**
     * Validate grinder setting input.
     */
    fun updateAndValidateGrinderSetting(setting: String) {
        val trimmedSetting = setting.trim()
        val error = when {
            trimmedSetting.length > 50 -> "Grinder setting cannot exceed 50 characters"
            trimmedSetting.isNotEmpty() && !trimmedSetting.matches(Regex("^[a-zA-Z0-9\\s\\-_.#]+$")) -> 
                "Grinder setting contains invalid characters"
            else -> null
        }
        
        _uiState.value = _uiState.value.copy(
            lastGrinderSetting = setting,
            grinderSettingError = error
        )
    }

    /**
     * Validate all fields at once (useful for form submission).
     */
    fun validateAllFields(): Boolean {
        val currentState = _uiState.value
        
        validateName(currentState.name)
        validateRoastDate(currentState.roastDate)
        validateNotes(currentState.notes)
        updateAndValidateGrinderSetting(currentState.lastGrinderSetting)
        
        // Wait for validation to complete and check for errors
        return currentState.nameError == null && 
               currentState.roastDateError == null && 
               currentState.notesError == null &&
               currentState.grinderSettingError == null
    }

    /**
     * Reset form to default values.
     */
    fun resetForm() {
        editingBeanId = null
        _uiState.value = AddEditBeanUiState()
    }

    /**
     * Check if the form has unsaved changes.
     */
    fun hasUnsavedChanges(): Boolean {
        val currentState = _uiState.value
        return if (currentState.isEditMode) {
            // In edit mode, check if any field has changed from original values
            // This would require storing original values, for now return true if any field has content
            currentState.name.isNotBlank() || 
            currentState.notes.isNotBlank() || 
            currentState.lastGrinderSetting.isNotBlank()
        } else {
            // In add mode, check if any field has been modified from defaults
            currentState.name.isNotBlank() || 
            currentState.roastDate != LocalDate.now() ||
            currentState.notes.isNotBlank() || 
            currentState.lastGrinderSetting.isNotBlank() ||
            !currentState.isActive
        }
    }

    /**
     * Get validation summary for display.
     */
    fun getValidationSummary(): List<String> {
        val currentState = _uiState.value
        val errors = mutableListOf<String>()
        
        currentState.nameError?.let { errors.add("Name: $it") }
        currentState.roastDateError?.let { errors.add("Roast Date: $it") }
        currentState.notesError?.let { errors.add("Notes: $it") }
        currentState.grinderSettingError?.let { errors.add("Grinder Setting: $it") }
        
        return errors
    }

    /**
     * Quick save with minimal validation (for draft functionality).
     */
    fun quickSave() {
        val currentState = _uiState.value
        
        // Only validate required fields for quick save
        if (currentState.name.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Bean name is required for saving")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            val result = if (editingBeanId != null) {
                updateBeanUseCase.execute(
                    beanId = editingBeanId!!,
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                    lastGrinderSetting = currentState.lastGrinderSetting.trim().takeIf { it.isNotEmpty() }
                )
            } else {
                addBeanUseCase.execute(
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                    lastGrinderSetting = currentState.lastGrinderSetting.trim().takeIf { it.isNotEmpty() }
                )
            }
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save bean"
                )
            }
        }
    }
}

/**
 * UI state for the add/edit bean screen.
 */
data class AddEditBeanUiState(
    val name: String = "",
    val roastDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isActive: Boolean = true,
    val lastGrinderSetting: String = "",
    val nameError: String? = null,
    val roastDateError: String? = null,
    val notesError: String? = null,
    val grinderSettingError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)