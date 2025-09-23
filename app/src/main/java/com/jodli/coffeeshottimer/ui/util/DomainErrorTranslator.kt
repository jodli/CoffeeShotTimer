package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import android.util.Log
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to translate domain error codes to user-facing strings.
 * This keeps the domain layer clean while providing localized error messages.
 * Also provides comprehensive error handling for ViewModels.
 */
@Singleton
class DomainErrorTranslator @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        private const val TAG = "DomainErrorTranslator"
    }

    /**
     * Translate a domain error code to a localized string.
     */
    fun translate(errorCode: DomainErrorCode, details: String? = null): String {
        return when (errorCode) {
            DomainErrorCode.BEAN_ID_EMPTY -> context.getString(R.string.error_bean_id_cannot_be_empty)
            DomainErrorCode.BEAN_NOT_FOUND -> context.getString(R.string.error_bean_not_found)
            DomainErrorCode.BEAN_NAME_EMPTY -> context.getString(R.string.error_bean_name_cannot_be_empty)
            DomainErrorCode.BEAN_NAME_EXISTS -> context.getString(R.string.text_bean_name_exists)
            DomainErrorCode.GRINDER_SETTING_EMPTY -> context.getString(R.string.error_grinder_setting_cannot_be_empty)

            DomainErrorCode.FAILED_TO_GET_BEAN -> context.getString(R.string.error_failed_to_get_bean)
            DomainErrorCode.FAILED_TO_UPDATE_BEAN -> context.getString(R.string.error_failed_to_update_bean)
            DomainErrorCode.FAILED_TO_ADD_BEAN -> context.getString(R.string.error_failed_to_add_bean)
            DomainErrorCode.FAILED_TO_UPDATE_GRINDER_SETTING -> context.getString(
                R.string.error_failed_to_update_grinder_setting
            )
            DomainErrorCode.FAILED_TO_UPDATE_ACTIVE_STATUS -> context.getString(
                R.string.error_failed_to_update_active_status
            )
            DomainErrorCode.FAILED_TO_CHECK_BEAN_NAME -> context.getString(R.string.error_failed_to_check_bean_name)
            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEANS -> context.getString(R.string.error_failed_to_get_active_beans)
            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEAN_COUNT -> context.getString(
                R.string.error_failed_to_get_active_bean_count
            )
            DomainErrorCode.FAILED_TO_CHECK_FOR_ACTIVE_BEANS -> context.getString(
                R.string.error_failed_to_check_for_active_beans
            )

            // Shot validation errors
            DomainErrorCode.SHOT_VALIDATION_FAILED -> context.getString(R.string.error_shot_validation_failed)
            DomainErrorCode.COFFEE_WEIGHT_IN_INVALID -> context.getString(R.string.error_coffee_weight_in_invalid)
            DomainErrorCode.COFFEE_WEIGHT_OUT_INVALID -> context.getString(R.string.error_coffee_weight_out_invalid)
            DomainErrorCode.EXTRACTION_TIME_INVALID -> context.getString(R.string.error_extraction_time_minimum, 5)
            DomainErrorCode.GRINDER_SETTING_INVALID -> context.getString(R.string.error_grinder_setting_cannot_be_empty)

            // Shot operation errors
            DomainErrorCode.SHOT_NOT_FOUND -> context.getString(R.string.error_loading_shot_details)
            DomainErrorCode.SHOT_RECORDING_FAILED -> context.getString(R.string.error_recording_error)
            DomainErrorCode.FAILED_TO_RECORD_SHOT -> context.getString(R.string.error_recording_error)
            DomainErrorCode.FAILED_TO_GET_SHOT_HISTORY -> context.getString(R.string.error_loading_shots)
            DomainErrorCode.FAILED_TO_GET_SHOT_STATISTICS -> context.getString(R.string.error_loading_shot_details)
            DomainErrorCode.FAILED_TO_DELETE_SHOT -> context.getString(R.string.error_deleting_data)
            DomainErrorCode.FAILED_TO_GET_SHOT -> context.getString(R.string.error_loading_shot_details)
            DomainErrorCode.ASSOCIATED_BEAN_NOT_FOUND -> context.getString(R.string.error_bean_not_found)

            // Grinder adjustment errors
            DomainErrorCode.GRINDER_CONFIG_NOT_FOUND -> context.getString(R.string.error_grinder_config_not_found)
            DomainErrorCode.INVALID_GRIND_SETTING -> context.getString(R.string.error_invalid_grind_setting)
            DomainErrorCode.INVALID_EXTRACTION_TIME -> context.getString(R.string.error_invalid_extraction_time)
            DomainErrorCode.CALCULATION_ERROR -> context.getString(R.string.error_calculation_failed)

            // Photo operation errors
            DomainErrorCode.PHOTO_SAVE_FAILED -> context.getString(R.string.error_photo_save_failed)
            DomainErrorCode.PHOTO_DELETE_FAILED -> context.getString(R.string.error_photo_delete_failed)
            DomainErrorCode.PHOTO_NOT_FOUND -> context.getString(R.string.error_photo_not_found)
            DomainErrorCode.PHOTO_COMPRESSION_FAILED -> context.getString(R.string.error_photo_compression_failed)
            DomainErrorCode.PHOTO_STORAGE_FULL -> context.getString(R.string.error_photo_storage_full)
            DomainErrorCode.PHOTO_INVALID_URI -> context.getString(R.string.error_photo_invalid_uri)
            DomainErrorCode.PHOTO_FILE_ACCESS_ERROR -> context.getString(R.string.error_photo_file_access_error)

            // Camera and permission errors
            DomainErrorCode.CAMERA_UNAVAILABLE -> context.getString(R.string.error_camera_unavailable)
            DomainErrorCode.CAMERA_PERMISSION_DENIED -> context.getString(R.string.error_camera_permission_denied)
            DomainErrorCode.STORAGE_PERMISSION_DENIED -> context.getString(R.string.error_storage_permission_denied)
            DomainErrorCode.PHOTO_CAPTURE_FAILED -> context.getString(R.string.error_photo_capture_failed)
            DomainErrorCode.PHOTO_SELECTION_CANCELLED -> context.getString(R.string.error_photo_selection_cancelled)

            // Storage errors
            DomainErrorCode.STORAGE_ERROR -> context.getString(R.string.error_saving_data)

            DomainErrorCode.VALIDATION_FAILED -> {
                if (details != null) {
                    context.getString(R.string.error_bean_validation_failed, details)
                } else {
                    context.getString(R.string.error_bean_validation_failed, "Unknown validation error")
                }
            }

            DomainErrorCode.UNKNOWN_ERROR -> {
                // Log unknown error details for diagnostics
                val extra = if (!details.isNullOrBlank()) ": $details" else ""
                Log.e(TAG, "Unknown error encountered$extra")
                if (details != null) {
                    context.getString(R.string.error_unknown_error_occurred) + ": $details"
                } else {
                    context.getString(R.string.error_unknown_error_occurred)
                }
            }
        }
    }

    /**
     * Translate any exception to a user-friendly error message.
     * Handles both domain exceptions and general exceptions.
     */
    fun translateError(exception: Throwable?): String {
        return when (exception) {
            is DomainException -> translate(exception.errorCode, exception.details)
            null -> {
                Log.e(TAG, "Unknown error: exception is null")
                context.getString(R.string.error_unknown_error_occurred)
            }
            else -> {
                // For non-domain exceptions, provide a generic error message
                // Log the actual exception for debugging
                Log.e(TAG, "Unknown non-domain exception", exception)
                context.getString(R.string.error_unknown_error_occurred)
            }
        }
    }

    /**
     * Translate Result.failure to user-friendly error message.
     * Common pattern for handling Result<T> in ViewModels.
     */
    fun <T> translateResultError(result: Result<T>): String {
        return result.exceptionOrNull()?.let { translateError(it) }
            ?: context.getString(R.string.error_unknown_error_occurred)
    }

    /**
     * Get loading error message for specific operations.
     */
    fun getLoadingError(operation: String): String {
        return context.getString(R.string.error_loading_data, operation)
    }

    /**
     * Get save error message.
     */
    fun getSaveError(): String {
        return context.getString(R.string.error_saving_data)
    }

    /**
     * Get delete error message.
     */
    fun getDeleteError(): String {
        return context.getString(R.string.error_deleting_data)
    }

    /**
     * Get string resource by ID.
     * Helper method for ViewModels to access string resources.
     */
    fun getString(stringResId: Int): String {
        return context.getString(stringResId)
    }

    /**
     * Get string resource by ID with format arguments.
     * Helper method for ViewModels to access string resources.
     */
    fun getString(stringResId: Int, vararg formatArgs: Any): String {
        return context.getString(stringResId, *formatArgs)
    }
}
