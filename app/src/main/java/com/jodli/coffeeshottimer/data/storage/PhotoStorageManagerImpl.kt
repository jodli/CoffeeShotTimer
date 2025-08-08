package com.jodli.coffeeshottimer.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
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
    @param:ApplicationContext private val context: Context
) : PhotoStorageManager {
    
    companion object {
        private const val PHOTOS_DIR = "photos"
        private const val PHOTO_QUALITY = 85
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val PHOTO_EXTENSION = ".jpg"
        private const val MIN_REQUIRED_SPACE = 10 * 1024 * 1024L // 10MB minimum space required
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
            // Check available storage space before proceeding
            val availableSpace = photosDir.usableSpace
            if (availableSpace < MIN_REQUIRED_SPACE) {
                return@withContext Result.failure(
                    IOException("Insufficient storage space. Available: ${availableSpace / 1024 / 1024}MB")
                )
            }
            
            // Compress the image first
            val compressedUri = compressImage(imageUri).getOrElse { exception ->
                return@withContext Result.failure(
                    IOException("Image compression failed: ${exception.message}", exception)
                )
            }
            
            // Generate filename
            val filename = "${beanId}_photo$PHOTO_EXTENSION"
            val photoFile = File(photosDir, filename)
            
            // Copy compressed image to app storage
            context.contentResolver.openInputStream(compressedUri)?.use { inputStream ->
                FileOutputStream(photoFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(
                IOException("Failed to open input stream for image URI: $imageUri")
            )
            
            // Verify the file was saved successfully
            if (!photoFile.exists() || photoFile.length() == 0L) {
                return@withContext Result.failure(
                    IOException("Photo file was not saved properly")
                )
            }
            
            Result.success(photoFile.absolutePath)
        } catch (e: SecurityException) {
            Result.failure(IOException("Permission denied accessing photo storage", e))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error saving photo: ${e.message}", e))
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
        var inputStream: java.io.InputStream? = null
        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        
        try {
            // Read the original image
            inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(
                    IOException("Cannot open input stream for URI: $imageUri")
                )
            
            originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(
                    IOException("Cannot decode bitmap from URI: $imageUri. The file may be corrupted or not a valid image.")
                )
            
            inputStream.close()
            inputStream = null
            
            // Validate bitmap dimensions
            if (originalBitmap.width <= 0 || originalBitmap.height <= 0) {
                return@withContext Result.failure(
                    IOException("Invalid image dimensions: ${originalBitmap.width}x${originalBitmap.height}")
                )
            }
            
            // Handle image rotation based on EXIF data
            rotatedBitmap = handleImageRotation(imageUri, originalBitmap)
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
                originalBitmap = null
            }
            
            // Calculate new dimensions while maintaining aspect ratio
            val (newWidth, newHeight) = calculateOptimalDimensions(
                rotatedBitmap.width,
                rotatedBitmap.height
            )
            
            // Scale the bitmap if needed
            scaledBitmap = if (newWidth != rotatedBitmap.width || newHeight != rotatedBitmap.height) {
                try {
                    Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)?.also {
                        if (it != rotatedBitmap) {
                            rotatedBitmap.recycle()
                            rotatedBitmap = null
                        }
                    } ?: return@withContext Result.failure(
                        IOException("Failed to create scaled bitmap")
                    )
                } catch (e: OutOfMemoryError) {
                    return@withContext Result.failure(
                        IOException("Out of memory while scaling image. Image may be too large.", e)
                    )
                }
            } else {
                rotatedBitmap
            }
            
            // Verify scaledBitmap is not null
            if (scaledBitmap == null) {
                return@withContext Result.failure(
                    IOException("Scaled bitmap is null")
                )
            }
            
            // Create temporary file for compressed image
            val tempFile = try {
                File.createTempFile("compressed_", PHOTO_EXTENSION, context.cacheDir)
            } catch (e: IOException) {
                return@withContext Result.failure(
                    IOException("Cannot create temporary file for compression", e)
                )
            }
            
            // Compress and save
            try {
                FileOutputStream(tempFile).use { outputStream ->
                    val compressionSuccess = scaledBitmap?.compress(
                        Bitmap.CompressFormat.JPEG, 
                        PHOTO_QUALITY, 
                        outputStream
                    ) ?: false
                    if (!compressionSuccess) {
                        return@withContext Result.failure(
                            IOException("Failed to compress image to JPEG format")
                        )
                    }
                }
            } catch (e: IOException) {
                tempFile.delete() // Clean up failed temp file
                return@withContext Result.failure(
                    IOException("Failed to write compressed image to file", e)
                )
            }
            
            // Verify the compressed file was created successfully
            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext Result.failure(
                    IOException("Compressed image file was not created properly")
                )
            }
            
            Result.success(Uri.fromFile(tempFile))
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory during image compression", e))
        } catch (e: SecurityException) {
            Result.failure(IOException("Permission denied accessing image", e))
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error during image compression: ${e.message}", e))
        } finally {
            // Clean up resources
            inputStream?.close()
            originalBitmap?.recycle()
            if (rotatedBitmap != originalBitmap && rotatedBitmap != scaledBitmap) {
                rotatedBitmap?.recycle()
            }
            if (scaledBitmap != rotatedBitmap) {
                scaledBitmap?.recycle()
            }
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