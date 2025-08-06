package com.jodli.coffeeshottimer.domain.usecase

import android.net.Uri
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for adding photos to coffee beans.
 * Handles photo compression, storage, and database updates with proper error handling.
 */
@Singleton
class AddPhotoToBeanUseCase @Inject constructor(
    private val beanRepository: BeanRepository,
    private val photoStorageManager: PhotoStorageManager
) {

    /**
     * Adds a photo to a specific bean.
     * 
     * @param beanId The ID of the bean to add the photo to
     * @param imageUri The URI of the image to add
     * @return Result containing the saved photo path or error
     */
    suspend fun execute(beanId: String, imageUri: Uri): Result<String> {
        return try {
            // Validate bean ID
            if (beanId.trim().isEmpty()) {
                return Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
            }

            // Verify bean exists
            val beanResult = beanRepository.getBeanById(beanId.trim())
            if (beanResult.isFailure) {
                return Result.failure(
                    beanResult.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.FAILED_TO_GET_BEAN, "Failed to verify bean exists")
                )
            }

            val bean = beanResult.getOrNull()
                ?: return Result.failure(DomainException(DomainErrorCode.BEAN_NOT_FOUND))

            // If bean already has a photo, delete the old one first
            bean.photoPath?.let { oldPhotoPath ->
                val deleteResult = photoStorageManager.deletePhoto(oldPhotoPath)
                if (deleteResult.isFailure) {
                    // Log the error but don't fail the operation - we'll overwrite the reference
                    // This handles cases where the old file might already be missing
                }
            }

            // Save the new photo
            val saveResult = photoStorageManager.savePhoto(imageUri, beanId.trim())
            if (saveResult.isFailure) {
                return Result.failure(
                    saveResult.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, "Failed to save photo")
                )
            }

            val photoPath = saveResult.getOrNull()
                ?: return Result.failure(DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, "Photo path is null"))

            // Validate that the photo path is not empty
            if (photoPath.trim().isEmpty()) {
                return Result.failure(DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, "Photo path is empty"))
            }

            // Update bean with new photo path
            val updatedBean = bean.copy(photoPath = photoPath)
            val updateResult = beanRepository.updateBean(updatedBean)
            if (updateResult.isFailure) {
                // If database update fails, clean up the saved photo
                photoStorageManager.deletePhoto(photoPath)
                return Result.failure(
                    updateResult.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.FAILED_TO_UPDATE_BEAN, "Failed to update bean with photo")
                )
            }

            Result.success(photoPath)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error adding photo to bean",
                    exception
                )
            )
        }
    }
}