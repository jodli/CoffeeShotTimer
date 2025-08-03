package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to provide string resources in ViewModels and other classes that don't have direct access to Context.
 * This allows for proper localization and easier testing.
 */
@Singleton
class StringResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get a string resource by its ID.
     */
    fun getString(@StringRes stringRes: Int): String {
        return context.getString(stringRes)
    }

    /**
     * Get a formatted string resource with arguments.
     */
    fun getString(@StringRes stringRes: Int, vararg formatArgs: Any): String {
        return context.getString(stringRes, *formatArgs)
    }
}
