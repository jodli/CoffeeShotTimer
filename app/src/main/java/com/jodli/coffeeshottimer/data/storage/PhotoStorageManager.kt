package com.jodli.coffeeshottimer.data.storage

import android.net.Uri
import java.io.File

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
     * Retrieves a photo file from storage.
     * 
     * @param photoPath The path of the photo file
     * @return The File object if it exists, null otherwise
     */
    suspend fun getPhotoFile(photoPath: String): File?
    
    /**
     * Compresses an image to optimize storage size while maintaining quality.
     * 
     * @param imageUri The URI of the image to compress
     * @return Result containing the compressed image URI or error
     */
    suspend fun compressImage(imageUri: Uri): Result<Uri>
    
    /**
     * Cleans up orphaned photo files that are no longer referenced by any bean.
     * 
     * @param referencedPhotoPaths Set of photo paths that are currently referenced
     * @return Result indicating the number of files cleaned up or error
     */
    suspend fun cleanupOrphanedFiles(referencedPhotoPaths: Set<String>): Result<Int>
    
    /**
     * Gets the total storage size used by photos.
     * 
     * @return The total size in bytes
     */
    suspend fun getTotalStorageSize(): Long
}