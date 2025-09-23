package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.AddPhotoToBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.CheckPhotoCapabilityUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanPhotoUseCase
import com.jodli.coffeeshottimer.domain.usecase.RemovePhotoFromBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import com.jodli.coffeeshottimer.ui.validation.validateBeanNameEnhanced
import com.jodli.coffeeshottimer.ui.validation.validateNotesEnhanced
import com.jodli.coffeeshottimer.ui.validation.validateRoastDateEnhanced
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Enum representing different photo operations for retry functionality
 */
enum class PhotoOperation {
    ADD_PHOTO,
    REPLACE_PHOTO,
    DELETE_PHOTO
}

/**
 * ViewModel for adding and editing coffee bean profiles.
 * Handles form validation, bean creation, and updates.
 */
@HiltViewModel
class AddEditBeanViewModel @Inject constructor(
    private val addBeanUseCase: AddBeanUseCase,
    private val updateBeanUseCase: UpdateBeanUseCase,
    private val addPhotoToBeanUseCase: AddPhotoToBeanUseCase,
    private val removePhotoFromBeanUseCase: RemovePhotoFromBeanUseCase,
    private val getBeanPhotoUseCase: GetBeanPhotoUseCase,
    private val checkPhotoCapabilityUseCase: CheckPhotoCapabilityUseCase,
    private val photoCaptureManager: PhotoCaptureManager,
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
                        photoPath = bean.photoPath,
                        pendingPhotoUri = null, // Clear any pending photo in edit mode
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
        _uiState.value = _uiState.value.copy()
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
                // Edit mode: update existing bean
                updateBeanUseCase.execute(
                    beanId = editingBeanId!!,
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                )
            } else {
                // Create mode: add new bean
                addBeanUseCase.execute(
                    name = currentState.name.trim(),
                    roastDate = currentState.roastDate,
                    notes = currentState.notes.trim(),
                    isActive = currentState.isActive,
                )
            }

            if (result.isSuccess) {
                val savedBean = result.getOrNull()

                // If we're in create mode and have a pending photo, add it to the newly created bean
                if (editingBeanId == null && currentState.pendingPhotoUri != null) {
                    if (savedBean != null) {
                        val photoResult = addPhotoToBeanUseCase.execute(savedBean.id, currentState.pendingPhotoUri)
                        if (photoResult.isFailure) {
                            // Photo failed to save, but bean was created successfully
                            // Show warning but don't fail the entire operation
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = true,
                                savedBean = savedBean,
                                photoError = domainErrorTranslator.translateResultError(photoResult)
                            )
                            return@launch
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    savedBean = savedBean
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
        _uiState.value = _uiState.value.copy(saveSuccess = false, savedBean = null)
    }

    /**
     * Adds or replaces a photo for the bean.
     * In create mode, stores the photo URI temporarily until bean is saved.
     * In edit mode, immediately updates the bean's photo.
     */
    fun addPhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPhotoLoading = true,
                photoError = null,
                photoSuccessMessage = null,
                canRetryPhotoOperation = false,
                lastFailedPhotoOperation = null
            )

            if (editingBeanId != null) {
                // Edit mode: immediately update the bean's photo
                val result = addPhotoToBeanUseCase.execute(editingBeanId!!, imageUri)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        photoPath = result.getOrNull(),
                        isPhotoLoading = false,
                        pendingPhotoUri = null, // Clear any pending photo
                        photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_saved)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPhotoLoading = false,
                        photoError = domainErrorTranslator.translateResultError(result),
                        canRetryPhotoOperation = true,
                        lastFailedPhotoOperation = PhotoOperation.ADD_PHOTO
                    )
                }
            } else {
                // Create mode: store photo URI temporarily
                _uiState.value = _uiState.value.copy(
                    pendingPhotoUri = imageUri,
                    isPhotoLoading = false,
                    photoPath = null, // Clear any existing photo path
                    photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_will_be_saved)
                )
            }
        }
    }

    /**
     * Replaces the current photo with a new one.
     * This is essentially the same as addPhoto but provides semantic clarity.
     */
    fun replacePhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPhotoLoading = true,
                photoError = null,
                photoSuccessMessage = null,
                canRetryPhotoOperation = false,
                lastFailedPhotoOperation = null
            )

            if (editingBeanId != null) {
                // Edit mode: replace the bean's photo
                val result = addPhotoToBeanUseCase.execute(editingBeanId!!, imageUri)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        photoPath = result.getOrNull(),
                        isPhotoLoading = false,
                        pendingPhotoUri = null,
                        photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_replaced)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPhotoLoading = false,
                        photoError = domainErrorTranslator.translateResultError(result),
                        canRetryPhotoOperation = true,
                        lastFailedPhotoOperation = PhotoOperation.REPLACE_PHOTO
                    )
                }
            } else {
                // Create mode: replace pending photo URI
                _uiState.value = _uiState.value.copy(
                    pendingPhotoUri = imageUri,
                    isPhotoLoading = false,
                    photoPath = null,
                    photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_will_be_saved)
                )
            }
        }
    }

    /**
     * Removes the photo from the bean.
     * In create mode, clears the pending photo URI.
     * In edit mode, removes the photo from the bean and storage.
     */
    fun removePhoto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPhotoLoading = true,
                photoError = null,
                photoSuccessMessage = null,
                canRetryPhotoOperation = false,
                lastFailedPhotoOperation = null
            )

            if (editingBeanId != null) {
                // Edit mode: remove photo from bean and storage
                val result = removePhotoFromBeanUseCase.execute(editingBeanId!!)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        photoPath = null,
                        isPhotoLoading = false,
                        pendingPhotoUri = null,
                        photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_deleted)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPhotoLoading = false,
                        photoError = domainErrorTranslator.translateResultError(result),
                        canRetryPhotoOperation = true,
                        lastFailedPhotoOperation = PhotoOperation.DELETE_PHOTO
                    )
                }
            } else {
                // Create mode: just clear the pending photo URI
                _uiState.value = _uiState.value.copy(
                    pendingPhotoUri = null,
                    isPhotoLoading = false,
                    photoPath = null,
                    photoSuccessMessage = stringResourceProvider.getString(R.string.text_photo_deleted)
                )
            }
        }
    }

    fun clearPhotoError() {
        _uiState.value = _uiState.value.copy(
            photoError = null,
            canRetryPhotoOperation = false,
            lastFailedPhotoOperation = null
        )
    }

    fun clearPhotoSuccessMessage() {
        _uiState.value = _uiState.value.copy(photoSuccessMessage = null)
    }

    /**
     * Retries the last failed photo operation
     */
    fun retryPhotoOperation() {
        val currentState = _uiState.value
        val lastOperation = currentState.lastFailedPhotoOperation ?: return

        when (lastOperation) {
            PhotoOperation.ADD_PHOTO -> {
                // For retry, we need the URI from the last attempt
                // This would need to be stored in the state for retry to work
                // For now, we'll clear the error and let user try again
                clearPhotoError()
            }
            PhotoOperation.REPLACE_PHOTO -> {
                clearPhotoError()
            }
            PhotoOperation.DELETE_PHOTO -> {
                removePhoto()
            }
        }
    }

    /**
     * Checks photo capabilities before showing photo options
     */
    fun checkPhotoCapabilities() {
        viewModelScope.launch {
            val result = checkPhotoCapabilityUseCase.execute()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    photoError = domainErrorTranslator.translateResultError(result),
                    canRetryPhotoOperation = true,
                    lastFailedPhotoOperation = PhotoOperation.ADD_PHOTO
                )
            }
        }
    }

    /**
     * Checks if the bean currently has a photo (either saved or pending).
     */
    fun hasPhoto(): Boolean {
        val currentState = _uiState.value
        return currentState.photoPath != null || currentState.pendingPhotoUri != null
    }

    /**
     * Gets the current photo URI for display purposes.
     * Returns the pending photo URI in create mode, or null if no photo.
     */
    fun getCurrentPhotoUri(): Uri? {
        val currentState = _uiState.value
        return currentState.pendingPhotoUri
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
                currentState.pendingPhotoUri != null
        } else {
            // In add mode, check if any field has been modified from defaults
            currentState.name.isNotBlank() ||
                currentState.roastDate != LocalDate.now() ||
                currentState.notes.isNotBlank() ||
                !currentState.isActive ||
                currentState.pendingPhotoUri != null
        }
    }

    // Camera-related methods that delegate to PhotoCaptureManager

    /**
     * Checks if camera permission is granted.
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return photoCaptureManager.isCameraPermissionGranted(context)
    }

    /**
     * Creates a camera capture intent with a temporary file URI.
     */
    fun createCameraIntent(context: Context): Pair<Intent, Uri>? {
        return try {
            photoCaptureManager.createImageCaptureIntent()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans up a temporary camera file.
     */
    fun cleanupTempCameraFile(uri: Uri) {
        viewModelScope.launch {
            photoCaptureManager.cleanupTempFile(uri)
        }
    }

    /**
     * Checks if camera is available on the device.
     */
    fun isCameraAvailable(context: Context): Boolean {
        return photoCaptureManager.isCameraAvailable(context)
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
    val photoPath: String? = null,
    val pendingPhotoUri: Uri? = null, // Photo URI pending save in create mode
    val nameError: String? = null,
    val roastDateError: String? = null,
    val notesError: String? = null,
    val grinderSettingError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val savedBean: Bean? = null, // The saved/created bean for onboarding mode
    val error: String? = null,
    val isEditMode: Boolean = false,
    val isPhotoLoading: Boolean = false,
    val photoError: String? = null,
    val photoSuccessMessage: String? = null,
    val canRetryPhotoOperation: Boolean = false,
    val lastFailedPhotoOperation: PhotoOperation? = null
)
