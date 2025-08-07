package com.jodli.coffeeshottimer.data.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PhotoCaptureManager that handles camera and gallery integration.
 */
@Singleton
class PhotoCaptureManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoCaptureManager {
    
    companion object {
        private const val TEMP_PHOTOS_DIR = "temp_photos"
        private const val TEMP_PHOTO_PREFIX = "temp_photo_"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val FILE_PROVIDER_AUTHORITY = "com.jodli.coffeeshottimer.fileprovider"
    }
    
    private val tempPhotosDir: File by lazy {
        File(context.cacheDir, TEMP_PHOTOS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    override fun createImageCaptureIntent(): Pair<Intent, Uri> {
        // Verify camera is available before creating intent
        if (!isCameraAvailable(context)) {
            throw IllegalStateException("Camera is not available on this device")
        }
        
        // Create temporary file for camera capture
        val tempFile = try {
            File.createTempFile(
                TEMP_PHOTO_PREFIX,
                PHOTO_EXTENSION,
                tempPhotosDir
            )
        } catch (e: IOException) {
            throw IllegalStateException("Cannot create temporary file for camera capture", e)
        }
        
        // Create URI using FileProvider for security
        val tempUri = try {
            FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                tempFile
            )
        } catch (e: IllegalArgumentException) {
            tempFile.delete() // Clean up the temp file
            throw IllegalStateException("Cannot create file URI for camera capture", e)
        }
        
        // Create camera intent
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
            // Grant temporary permissions to camera app
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Verify that there's a camera app available to handle the intent
        if (intent.resolveActivity(context.packageManager) == null) {
            tempFile.delete() // Clean up the temp file
            throw IllegalStateException("No camera app available to handle photo capture")
        }
        
        return Pair(intent, tempUri)
    }
    
    override fun createImagePickerIntent(): Intent {
        val pickIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        
        // Alternative for devices that don't support ACTION_PICK
        return if (pickIntent.resolveActivity(context.packageManager) == null) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        } else {
            pickIntent
        }
    }
    
    override fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun isCameraAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    
    override fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
    
    override suspend fun cleanupTempFile(tempUri: Uri) = withContext(Dispatchers.IO) {
        try {
            // Extract file path from URI
            val file = File(tempUri.path ?: return@withContext)
            if (file.exists() && file.parentFile == tempPhotosDir) {
                file.delete()
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is best effort
            // In a real app, you might want to log this
        }
    }
    
    /**
     * Cleans up all temporary photo files older than a specified time.
     * This can be called periodically to prevent temp files from accumulating.
     */
    suspend fun cleanupOldTempFiles(maxAgeMillis: Long = 24 * 60 * 60 * 1000) = withContext(Dispatchers.IO) {
        try {
            if (!tempPhotosDir.exists()) return@withContext
            
            val currentTime = System.currentTimeMillis()
            tempPhotosDir.listFiles()?.forEach { file ->
                if (file.isFile && (currentTime - file.lastModified()) > maxAgeMillis) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is best effort
        }
    }
}