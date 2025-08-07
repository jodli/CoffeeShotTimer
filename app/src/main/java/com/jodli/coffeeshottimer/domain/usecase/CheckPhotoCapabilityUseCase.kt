package com.jodli.coffeeshottimer.domain.usecase

import android.content.Context
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for checking photo capture capabilities and permissions.
 * Provides comprehensive validation before attempting photo operations.
 */
@Singleton
class CheckPhotoCapabilityUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoCaptureManager: PhotoCaptureManager
) {

    /**
     * Data class representing photo capability status
     */
    data class PhotoCapability(
        val canUseCamera: Boolean,
        val canUseGallery: Boolean,
        val cameraPermissionGranted: Boolean,
        val storagePermissionGranted: Boolean,
        val cameraAvailable: Boolean,
        val errorCode: DomainErrorCode? = null,
        val errorMessage: String? = null
    )

    /**
     * Checks all photo capture capabilities and permissions.
     * 
     * @return Result containing PhotoCapability status
     */
    suspend fun execute(): Result<PhotoCapability> {
        return try {
            val cameraAvailable = photoCaptureManager.isCameraAvailable(context)
            val cameraPermissionGranted = photoCaptureManager.isCameraPermissionGranted(context)
            
            // For gallery access, we need storage permission
            val storagePermissionGranted = checkStoragePermission()
            
            val canUseCamera = cameraAvailable && cameraPermissionGranted
            val canUseGallery = storagePermissionGranted
            
            // Determine if any photo functionality is available
            if (!canUseCamera && !canUseGallery) {
                val errorCode = when {
                    !cameraAvailable -> DomainErrorCode.CAMERA_UNAVAILABLE
                    !cameraPermissionGranted && !storagePermissionGranted -> DomainErrorCode.CAMERA_PERMISSION_DENIED
                    !cameraPermissionGranted -> DomainErrorCode.CAMERA_PERMISSION_DENIED
                    !storagePermissionGranted -> DomainErrorCode.STORAGE_PERMISSION_DENIED
                    else -> DomainErrorCode.CAMERA_UNAVAILABLE
                }
                
                return Result.success(
                    PhotoCapability(
                        canUseCamera = false,
                        canUseGallery = false,
                        cameraPermissionGranted = cameraPermissionGranted,
                        storagePermissionGranted = storagePermissionGranted,
                        cameraAvailable = cameraAvailable,
                        errorCode = errorCode
                    )
                )
            }
            
            Result.success(
                PhotoCapability(
                    canUseCamera = canUseCamera,
                    canUseGallery = canUseGallery,
                    cameraPermissionGranted = cameraPermissionGranted,
                    storagePermissionGranted = storagePermissionGranted,
                    cameraAvailable = cameraAvailable
                )
            )
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error checking photo capabilities",
                    exception
                )
            )
        }
    }

    /**
     * Checks specifically for camera capability.
     * 
     * @return Result indicating if camera can be used
     */
    suspend fun canUseCamera(): Result<Boolean> {
        return try {
            val available = photoCaptureManager.isCameraAvailable(context)
            val permitted = photoCaptureManager.isCameraPermissionGranted(context)
            
            if (!available) {
                Result.failure(DomainException(DomainErrorCode.CAMERA_UNAVAILABLE))
            } else if (!permitted) {
                Result.failure(DomainException(DomainErrorCode.CAMERA_PERMISSION_DENIED))
            } else {
                Result.success(true)
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Error checking camera capability",
                    exception
                )
            )
        }
    }

    /**
     * Checks specifically for gallery capability.
     * 
     * @return Result indicating if gallery can be used
     */
    suspend fun canUseGallery(): Result<Boolean> {
        return try {
            val permitted = checkStoragePermission()
            
            if (!permitted) {
                Result.failure(DomainException(DomainErrorCode.STORAGE_PERMISSION_DENIED))
            } else {
                Result.success(true)
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Error checking gallery capability",
                    exception
                )
            )
        }
    }

    /**
     * Gets the required permissions for photo operations.
     * 
     * @return Array of permission strings
     */
    fun getRequiredPermissions(): Array<String> {
        return photoCaptureManager.getRequiredPermissions()
    }

    private fun checkStoragePermission(): Boolean {
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES
        // For older versions, we need READ_EXTERNAL_STORAGE
        val permissions = photoCaptureManager.getRequiredPermissions()
        return permissions.any { permission ->
            when (permission) {
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, 
                        permission
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                else -> true
            }
        }
    }
}