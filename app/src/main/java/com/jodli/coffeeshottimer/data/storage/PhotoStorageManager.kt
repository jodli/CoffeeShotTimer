package com.jodli.coffeeshottimer.data.storage

import android.net.Uri

/**
 * Interface for managing photo storage operations including compression, file management, and cleanup.
 */
interface PhotoStorageManager {
    /**
     * Saves a photo for a specific bean with compression and returns the file path.
     *
     * @param imageUri The URI of the image to save
     * @param beanId The ID of the bean this photo belongs to
     * @return Result containing the saved file path or error
     */
    suspend fun savePhoto(imageUri: Uri, beanId: String): Result<String>

    /**
     * Deletes a photo file from storage.
     *
     * @param photoPath The path of the photo file to delete
     * @return Result indicating success or failure
     */
    suspend fun deletePhoto(photoPath: String): Result<Unit>

    /**
     * Compresses an image to optimize storage size while maintaining quality.
     *
     * @param imageUri The URI of the image to compress
     * @return Result containing the compressed image URI or error
     */
    suspend fun compressImage(imageUri: Uri): Result<Uri>
}
