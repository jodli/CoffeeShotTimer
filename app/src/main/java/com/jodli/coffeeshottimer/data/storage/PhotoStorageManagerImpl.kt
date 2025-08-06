package com.jodli.coffeeshottimer.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PhotoStorageManager that handles photo storage, compression, and cleanup.
 */
@Singleton
class PhotoStorageManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoStorageManager {
    
    companion object {
        private const val PHOTOS_DIR = "photos"
        private const val PHOTO_QUALITY = 85
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val PHOTO_EXTENSION = ".jpg"
    }
    
    private val photosDir: File by lazy {
        File(context.filesDir, PHOTOS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    override suspend fun savePhoto(imageUri: Uri, beanId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Compress the image first
            val compressedUri = compressImage(imageUri).getOrThrow()
            
            // Generate filename
            val filename = "${beanId}_photo$PHOTO_EXTENSION"
            val photoFile = File(photosDir, filename)
            
            // Copy compressed image to app storage
            context.contentResolver.openInputStream(compressedUri)?.use { inputStream ->
                FileOutputStream(photoFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open input stream for image")
            
            Result.success(photoFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deletePhoto(photoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(photoPath)
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to delete photo file: $photoPath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPhotoFile(photoPath: String): File? = withContext(Dispatchers.IO) {
        val file = File(photoPath)
        if (file.exists() && file.isFile) file else null
    }
    
    override suspend fun compressImage(imageUri: Uri): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Read the original image
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw IOException("Cannot open input stream for URI: $imageUri")
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw IOException("Cannot decode bitmap from URI: $imageUri")
            
            inputStream.close()
            
            // Handle image rotation based on EXIF data
            val rotatedBitmap = handleImageRotation(imageUri, originalBitmap)
            
            // Calculate new dimensions while maintaining aspect ratio
            val (newWidth, newHeight) = calculateOptimalDimensions(
                rotatedBitmap.width,
                rotatedBitmap.height
            )
            
            // Scale the bitmap if needed
            val scaledBitmap = if (newWidth != rotatedBitmap.width || newHeight != rotatedBitmap.height) {
                Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true).also {
                    if (it != rotatedBitmap) rotatedBitmap.recycle()
                }
            } else {
                rotatedBitmap
            }
            
            // Create temporary file for compressed image
            val tempFile = File.createTempFile("compressed_", PHOTO_EXTENSION, context.cacheDir)
            
            // Compress and save
            FileOutputStream(tempFile).use { outputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, outputStream)
            }
            
            // Clean up bitmap
            scaledBitmap.recycle()
            
            Result.success(Uri.fromFile(tempFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cleanupOrphanedFiles(referencedPhotoPaths: Set<String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!photosDir.exists()) {
                return@withContext Result.success(0)
            }
            
            val referencedFilenames = referencedPhotoPaths.map { path ->
                File(path).name
            }.toSet()
            
            var deletedCount = 0
            photosDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name !in referencedFilenames) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTotalStorageSize(): Long = withContext(Dispatchers.IO) {
        if (!photosDir.exists()) return@withContext 0L
        
        photosDir.listFiles()?.sumOf { file ->
            if (file.isFile) file.length() else 0L
        } ?: 0L
    }
    
    private fun handleImageRotation(imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } catch (e: Exception) {
            // If we can't read EXIF data, return original bitmap
            bitmap
        }
    }
    
    private fun calculateOptimalDimensions(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
        if (originalWidth <= MAX_IMAGE_WIDTH && originalHeight <= MAX_IMAGE_HEIGHT) {
            return Pair(originalWidth, originalHeight)
        }
        
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        return if (aspectRatio > 1) {
            // Landscape orientation
            val newWidth = MAX_IMAGE_WIDTH
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Portrait orientation
            val newHeight = MAX_IMAGE_HEIGHT
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }
}