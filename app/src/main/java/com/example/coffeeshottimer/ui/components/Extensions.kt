package com.example.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension functions and utilities for UI components
 */

/**
 * Get status bar height as Dp
 */
@Composable
fun getStatusBarHeight(): Dp {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return statusBarHeight
}

/**
 * Get navigation bar height as Dp
 */
@Composable
fun getNavigationBarHeight(): Dp {
    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return navBarHeight
}

/**
 * Format time duration in milliseconds to human readable format
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (durationMs % 1000) / 10 // Show centiseconds
    
    return when {
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, milliseconds)
        else -> String.format("%d.%02d", seconds, milliseconds)
    }
}

/**
 * Format time for display in timer (MM:SS format)
 */
fun formatTimerDisplay(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Format weight with appropriate precision
 */
fun formatWeight(weight: Double?): String {
    return weight?.let { 
        if (it % 1.0 == 0.0) {
            String.format("%.0fg", it)
        } else {
            String.format("%.1fg", it)
        }
    } ?: "--"
}

/**
 * Format rating for display
 */
fun formatRating(rating: Int?): String {
    return rating?.let { "$it/5" } ?: "--"
}

/**
 * Get color for rating (for theming)
 */
fun getRatingColor(rating: Int?): androidx.compose.ui.graphics.Color {
    return when (rating) {
        5 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        4 -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Light Green
        3 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        2 -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Deep Orange
        1 -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gray
    }
}

/**
 * Calculate extraction ratio
 */
fun calculateRatio(doseWeight: Double?, yieldWeight: Double?): String {
    return if (doseWeight != null && yieldWeight != null && doseWeight > 0) {
        val ratio = yieldWeight / doseWeight
        String.format("1:%.1f", ratio)
    } else {
        "--"
    }
}

/**
 * Determine if extraction time is within target range
 */
fun isTimeInRange(actualTime: Long, targetTime: Long?, toleranceSeconds: Int = 3): Boolean {
    if (targetTime == null) return true
    val toleranceMs = toleranceSeconds * 1000L
    return actualTime in (targetTime - toleranceMs)..(targetTime + toleranceMs)
}

/**
 * Get time status (fast, good, slow) based on target
 */
fun getTimeStatus(actualTime: Long, targetTime: Long?): TimeStatus {
    if (targetTime == null) return TimeStatus.UNKNOWN
    
    val toleranceMs = 3000L // 3 seconds tolerance
    return when {
        actualTime < targetTime - toleranceMs -> TimeStatus.FAST
        actualTime > targetTime + toleranceMs -> TimeStatus.SLOW
        else -> TimeStatus.GOOD
    }
}

enum class TimeStatus {
    FAST, GOOD, SLOW, UNKNOWN
}

/**
 * Get display text for time status
 */
fun TimeStatus.getDisplayText(): String {
    return when (this) {
        TimeStatus.FAST -> "Fast"
        TimeStatus.GOOD -> "Good"
        TimeStatus.SLOW -> "Slow"
        TimeStatus.UNKNOWN -> ""
    }
}

/**
 * Get color for time status
 */
fun TimeStatus.getColor(): androidx.compose.ui.graphics.Color {
    return when (this) {
        TimeStatus.FAST -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        TimeStatus.GOOD -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        TimeStatus.SLOW -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        TimeStatus.UNKNOWN -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gray
    }
}