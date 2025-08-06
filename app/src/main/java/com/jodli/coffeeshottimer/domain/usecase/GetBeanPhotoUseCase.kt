package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving photo files for coffee beans.
 * Handles photo file access with proper error handling.
 */
@Singleton
class GetBeanPhotoUseCase @Inject constructor(
    private val photoStorageManager: PhotoStorageManager
) {

    /**
     * Retrieves a photo file for a bean.
     * 
     * @param photoPath The path of the photo file to retrieve
     * @return Result containing the photo File or error
     */
    suspend fun execute(photoPath: String): Result<File> {
        return try {
            // Validate photo path
            if (photoPath.trim().isEmpty()) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.PHOTO_INVALID_URI,
                        "Photo path cannot be empty"
                    )
                )
            }

            // Get the photo file
            val photoFile = photoStorageManager.getPhotoFile(photoPath.trim())
            if (photoFile == null) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.PHOTO_NOT_FOUND,
                        "Photo file not found at path: $photoPath"
                    )
                )
            }

            // Verify file exists and is readable
            if (!photoFile.exists()) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.PHOTO_NOT_FOUND,
                        "Photo file does not exist: ${photoFile.absolutePath}"
                    )
                )
            }

            if (!photoFile.canRead()) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.PHOTO_FILE_ACCESS_ERROR,
                        "Cannot read photo file: ${photoFile.absolutePath}"
                    )
                )
            }

            Result.success(photoFile)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error retrieving photo file",
                    exception
                )
            )
        }
    }

    /**
     * Checks if a photo exists at the given path.
     * 
     * @param photoPath The path of the photo file to check
     * @return Result containing true if photo exists and is accessible, false otherwise
     */
    suspend fun photoExists(photoPath: String): Result<Boolean> {
        return try {
            if (photoPath.trim().isEmpty()) {
                return Result.success(false)
            }

            val photoFile = photoStorageManager.getPhotoFile(photoPath.trim())
            val exists = photoFile?.exists() == true && photoFile.canRead()
            Result.success(exists)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error checking photo existence",
                    exception
                )
            )
        }
    }
}