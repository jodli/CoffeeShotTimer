package com.jodli.coffeeshottimer.ui.validation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.R

/**
 * Shared validation helpers for basket configuration
 */
object BasketValidationHelpers {
    
    /**
     * Get a user-friendly validation suggestion based on the error message
     */
    @Composable
    fun getValidationSuggestion(error: String?): String {
        if (error == null) return ""
        return when {
            error.contains("minimum must be less than maximum") -> 
                stringResource(R.string.suggestion_basket_min_less_than_max)
            error.contains("cannot be less than") -> 
                stringResource(R.string.suggestion_basket_check_minimum_values)
            error.contains("cannot exceed") -> 
                stringResource(R.string.suggestion_basket_check_maximum_values)
            error.contains("range must be at least") -> 
                stringResource(R.string.suggestion_basket_increase_range)
            error.contains("brew ratios") -> 
                stringResource(R.string.suggestion_basket_check_ratios)
            else -> ""
        }
    }
}