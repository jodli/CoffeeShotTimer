package com.jodli.coffeeshottimer.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Interface for managing photo capture operations including camera integration.
 */
interface PhotoCaptureManager {
    /**
     * Creates an intent for capturing an image using the device camera.
     *
     * @return Intent for camera capture with a temporary file URI
     */
    fun createImageCaptureIntent(): Pair<Intent, Uri>

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
     * Cleans up temporary files created during photo capture.
     *
     * @param tempUri The temporary URI to clean up
     */
    suspend fun cleanupTempFile(tempUri: Uri)
}
