package com.jodli.coffeeshottimer.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Interface for managing photo capture operations including camera and gallery integration.
 */
interface PhotoCaptureManager {
    /**
     * Creates an intent for capturing an image using the device camera.
     * 
     * @return Intent for camera capture with a temporary file URI
     */
    fun createImageCaptureIntent(): Pair<Intent, Uri>
    
    /**
     * Creates an intent for selecting an image from the device gallery.
     * 
     * @return Intent for gallery image selection
     */
    fun createImagePickerIntent(): Intent
    
    /**
     * Checks if the camera permission is granted.
     * 
     * @param context The application context
     * @return true if camera permission is granted, false otherwise
     */
    fun isCameraPermissionGranted(context: Context): Boolean
    
    /**
     * Checks if the device has a camera available.
     * 
     * @param context The application context
     * @return true if camera is available, false otherwise
     */
    fun isCameraAvailable(context: Context): Boolean
    
    /**
     * Gets the required permissions for photo capture functionality.
     * 
     * @return Array of permission strings needed for photo capture
     */
    fun getRequiredPermissions(): Array<String>
    
    /**
     * Cleans up temporary files created during photo capture.
     * 
     * @param tempUri The temporary URI to clean up
     */
    suspend fun cleanupTempFile(tempUri: Uri)
}