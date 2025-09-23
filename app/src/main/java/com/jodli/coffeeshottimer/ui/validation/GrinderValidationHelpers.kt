package com.jodli.coffeeshottimer.ui.validation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.R

/**
 * Shared validation helpers for grinder configuration
 */
object GrinderValidationHelpers {

    /**
     * Get a user-friendly validation suggestion based on the error message
     */
    @Composable
    fun getValidationSuggestion(error: String?): String {
        if (error == null) return ""
        return when {
            error.contains("must be less than") ->
                stringResource(R.string.suggestion_increase_max_or_decrease_min)
            error.contains("cannot be negative") ->
                stringResource(R.string.suggestion_grinder_scales_start_at_zero_or_one)
            error.contains("cannot exceed 1000") ->
                stringResource(R.string.suggestion_most_grinder_scales_dont_go_above_100)
            error.contains("at least 3 steps") ->
                stringResource(R.string.suggestion_range_of_at_least_3_steps)
            error.contains("cannot exceed 100 steps") ->
                stringResource(R.string.suggestion_smaller_range_is_easier)
            else -> ""
        }
    }
}
