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
    @param:ApplicationContext private val context: Context,
    private val photoCaptureManager: PhotoCaptureManager
) {

    /**
     * Data class representing photo capability status
     */
    data class PhotoCapability(
        val canUseCamera: Boolean,
        val cameraPermissionGranted: Boolean,
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

            val canUseCamera = cameraAvailable && cameraPermissionGranted

            // Determine if any photo functionality is available
            if (!canUseCamera) {
                val errorCode = when {
                    !cameraAvailable -> DomainErrorCode.CAMERA_UNAVAILABLE
                    !cameraPermissionGranted -> DomainErrorCode.CAMERA_PERMISSION_DENIED
                    else -> DomainErrorCode.CAMERA_UNAVAILABLE
                }

                return Result.success(
                    PhotoCapability(
                        canUseCamera = false,
                        cameraPermissionGranted = cameraPermissionGranted,
                        cameraAvailable = cameraAvailable,
                        errorCode = errorCode
                    )
                )
            }

            Result.success(
                PhotoCapability(
                    canUseCamera = canUseCamera,
                    cameraPermissionGranted = cameraPermissionGranted,
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

    // Gallery (Photo Picker) requires no storage permission; no explicit check needed.
}
