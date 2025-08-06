package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for removing photos from coffee beans.
 * Handles photo file deletion and database updates with proper error handling.
 */
@Singleton
class RemovePhotoFromBeanUseCase @Inject constructor(
    private val beanRepository: BeanRepository,
    private val photoStorageManager: PhotoStorageManager
) {

    /**
     * Removes a photo from a specific bean.
     * 
     * @param beanId The ID of the bean to remove the photo from
     * @return Result indicating success or failure
     */
    suspend fun execute(beanId: String): Result<Unit> {
        return try {
            // Validate bean ID
            if (beanId.trim().isEmpty()) {
                return Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
            }

            // Get the bean to check if it has a photo
            val beanResult = beanRepository.getBeanById(beanId.trim())
            if (beanResult.isFailure) {
                return Result.failure(
                    beanResult.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.FAILED_TO_GET_BEAN, "Failed to get bean")
                )
            }

            val bean = beanResult.getOrNull()
                ?: return Result.failure(DomainException(DomainErrorCode.BEAN_NOT_FOUND))

            // Check if bean has a photo
            val photoPath = bean.photoPath
            if (photoPath.isNullOrEmpty()) {
                // Bean doesn't have a photo, nothing to remove
                return Result.success(Unit)
            }

            // Update bean to remove photo reference first
            val updatedBean = bean.copy(photoPath = null)
            val updateResult = beanRepository.updateBean(updatedBean)
            if (updateResult.isFailure) {
                return Result.failure(
                    updateResult.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.FAILED_TO_UPDATE_BEAN, "Failed to update bean")
                )
            }

            // Delete the photo file
            val deleteResult = photoStorageManager.deletePhoto(photoPath)
            if (deleteResult.isFailure) {
                // Photo file deletion failed, but database was updated successfully
                // This is not a critical error as the file might already be missing
                // Log the error but don't fail the operation
            }

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error removing photo from bean",
                    exception
                )
            )
        }
    }
}