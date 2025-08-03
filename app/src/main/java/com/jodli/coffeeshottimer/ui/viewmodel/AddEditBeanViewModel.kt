package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.validation.validateBeanNameEnhanced
import com.jodli.coffeeshottimer.ui.validation.validateGrinderSettingEnhanced
import com.jodli.coffeeshottimer.ui.validation.validateNotesEnhanced
import com.jodli.coffeeshottimer.ui.validation.validateRoastDateEnhanced
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.jodli.coffeeshottimer.R

/**
 * ViewModel for adding and editing coffee bean profiles.
 * Handles form validation, bean creation, and updates.
 */
@HiltViewModel
class AddEditBeanViewModel @Inject constructor(
    private val addBeanUseCase: AddBeanUseCase,
    private val updateBeanUseCase: UpdateBeanUseCase,
    private val stringResourceProvider: StringResourceProvider,
    private val validationStringProvider: ValidationStringProvider,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    // Create validation utils instance
    private val validationUtils = ValidationUtils(validationStringProvider)

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
                        error = domainErrorTranslator.translateError(
                            result.exceptionOrNull()
                        )
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateResultError(result)
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
            // Get existing bean names for uniqueness check
            val existingNames = try {
                // This would need to be implemented in the use case
                // For now, we'll do basic validation and handle uniqueness in the use case
                emptyList<String>()
            } catch (e: Exception) {
                emptyList()
            }

            // Use enhanced validation with contextual tips
            val validationResult = name.validateBeanNameEnhanced(validationUtils, existingNames)

            if (!validationResult.isValid) {
                // Join all errors and tips into a single message
                _uiState.value = _uiState.value.copy(nameError = validationResult.errors.joinToString("\n"))
            } else {
                // Check uniqueness with the use case
                val isAvailable = if (editingBeanId != null) {
                    updateBeanUseCase.isBeanNameAvailableForUpdate(editingBeanId!!, name.trim())
                } else {
                    addBeanUseCase.isBeanNameAvailable(name.trim())
                }

                if (isAvailable.isSuccess) {
                    if (isAvailable.getOrNull() == false) {
                        _uiState.value = _uiState.value.copy(nameError = stringResourceProvider.getString(R.string.text_bean_name_already_exists_tip))
                    } else {
                        _uiState.value = _uiState.value.copy(nameError = null)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(nameError = domainErrorTranslator.translateError(isAvailable.exceptionOrNull()))
                }
            }
        }
    }

    private fun validateRoastDate(date: LocalDate) {
        // Use enhanced validation with contextual tips
        val validationResult = date.validateRoastDateEnhanced(validationUtils)
        _uiState.value = _uiState.value.copy(roastDateError = if (validationResult.errors.isNotEmpty()) validationResult.errors.joinToString("\n") else null)
    }

    private fun validateNotes(notes: String) {
        // Use enhanced validation with helpful suggestions
        val validationResult = notes.validateNotesEnhanced(validationUtils)
        _uiState.value = _uiState.value.copy(notesError = if (validationResult.errors.isNotEmpty()) validationResult.errors.joinToString("\n") else null)
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
            currentState.notesError != null
        ) {
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
                    lastGrinderSetting = currentState.lastGrinderSetting.trim()
                        .takeIf { it.isNotEmpty() }
                )
            } else {
                addBeanUseCase.execute(
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                    lastGrinderSetting = currentState.lastGrinderSetting.trim()
                        .takeIf { it.isNotEmpty() }
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
                    error = domainErrorTranslator.translateResultError(result)
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
     * Validate grinder setting input using enhanced validation.
     */
    fun updateAndValidateGrinderSetting(setting: String) {
        // Use enhanced validation with helpful tips
        val validationResult = setting.validateGrinderSettingEnhanced(validationUtils, false) // Not required for beans

        _uiState.value = _uiState.value.copy(
            lastGrinderSetting = setting,
            grinderSettingError = if (validationResult.errors.isNotEmpty()) validationResult.errors.joinToString("\n") else null
        )
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