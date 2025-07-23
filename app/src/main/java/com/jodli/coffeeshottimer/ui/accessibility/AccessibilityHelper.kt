package com.jodli.coffeeshottimer.ui.accessibility

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/**
 * Accessibility helper functions and extensions for improved app accessibility.
 */
object AccessibilityHelper {

    /**
     * Minimum touch target size for accessibility (48dp as per Material Design guidelines).
     */
    val MinTouchTargetSize = 48.dp

    /**
     * Enhanced touch target size for better accessibility (56dp).
     */
    val EnhancedTouchTargetSize = 56.dp
}

/**
 * Modifier extension to ensure minimum touch target size for accessibility.
 */
fun Modifier.accessibleTouchTarget(): Modifier = this.size(AccessibilityHelper.MinTouchTargetSize)

/**
 * Modifier extension for enhanced touch target size.
 */
fun Modifier.enhancedTouchTarget(): Modifier =
    this.size(AccessibilityHelper.EnhancedTouchTargetSize)

/**
 * Modifier extension to add comprehensive accessibility semantics for form fields.
 */
fun Modifier.accessibleFormField(
    label: String,
    value: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    hint: String? = null
): Modifier = this.semantics {
    contentDescription = label
    text = AnnotatedString(value)

    if (isError && errorMessage != null) {
        error(errorMessage)
    }

    if (isRequired) {
        stateDescription = "Required field"
    }

    hint?.let {
        // Note: hintText is not available in current Compose version
        // Using stateDescription as alternative
        if (stateDescription.isEmpty()) {
            stateDescription = it
        }
    }
}

/**
 * Modifier extension for accessible buttons with enhanced descriptions.
 */
fun Modifier.accessibleButton(
    label: String,
    enabled: Boolean = true,
    role: Role = Role.Button,
    action: String? = null
): Modifier = this.semantics {
    contentDescription = if (action != null) "$label, $action" else label
    this.role = role

    if (!enabled) {
        disabled()
    }
}

/**
 * Modifier extension for accessible navigation elements.
 */
fun Modifier.accessibleNavigation(
    label: String,
    isSelected: Boolean = false,
    position: String? = null
): Modifier = this.semantics {
    contentDescription = label
    selected = isSelected
    role = Role.Tab

    position?.let {
        stateDescription = it
    }
}

/**
 * Modifier extension for accessible data display elements.
 */
fun Modifier.accessibleDataDisplay(
    label: String,
    value: String,
    unit: String? = null,
    context: String? = null
): Modifier = this.semantics {
    val fullDescription = buildString {
        append(label)
        append(": ")
        append(value)
        unit?.let { append(" $it") }
        context?.let { append(", $it") }
    }
    contentDescription = fullDescription
    role = Role.Image // Use Image role for data display elements
}

/**
 * Modifier extension for accessible timer elements.
 */
fun Modifier.accessibleTimer(
    currentTime: String,
    isRunning: Boolean,
    action: String
): Modifier = this.semantics {
    val description = if (isRunning) {
        "Timer running, current time $currentTime, $action"
    } else {
        "Timer stopped at $currentTime, $action"
    }
    contentDescription = description
    role = Role.Button

    if (isRunning) {
        liveRegion = LiveRegionMode.Polite
    }
}

/**
 * Modifier extension for accessible list items.
 */
fun Modifier.accessibleListItem(
    title: String,
    subtitle: String? = null,
    metadata: String? = null,
    position: Int? = null,
    totalItems: Int? = null
): Modifier = this.semantics {
    val description = buildString {
        append(title)
        subtitle?.let { append(", $it") }
        metadata?.let { append(", $it") }

        if (position != null && totalItems != null) {
            append(", item $position of $totalItems")
        }
    }
    contentDescription = description
    role = Role.Button
}

/**
 * Modifier extension for accessible progress indicators.
 */
fun Modifier.accessibleProgress(
    label: String,
    progress: Float? = null,
    isIndeterminate: Boolean = false
): Modifier = this.semantics {
    val description = when {
        isIndeterminate -> "$label, loading"
        progress != null -> "$label, ${(progress * 100).toInt()}% complete"
        else -> label
    }
    contentDescription = description

    progress?.let {
        progressBarRangeInfo = ProgressBarRangeInfo(it, 0f..1f)
    }
}

/**
 * Modifier extension for accessible toggle elements.
 */
fun Modifier.accessibleToggle(
    label: String,
    isChecked: Boolean,
    role: Role = Role.Switch
): Modifier = this.semantics {
    val state = if (isChecked) "on" else "off"
    contentDescription = "$label, $state"
    this.role = role
    selected = isChecked
}

/**
 * Modifier extension for accessible error states.
 */
fun Modifier.accessibleError(
    errorMessage: String
): Modifier = this.semantics {
    error(errorMessage)
    liveRegion = LiveRegionMode.Assertive
}

/**
 * Modifier extension for accessible success states.
 */
fun Modifier.accessibleSuccess(
    successMessage: String
): Modifier = this.semantics {
    contentDescription = successMessage
    liveRegion = LiveRegionMode.Polite
    role = Role.Image
}

/**
 * Composable function to announce important changes to screen readers.
 */
@Composable
fun AccessibilityAnnouncement(
    message: String,
    priority: LiveRegionMode = LiveRegionMode.Polite
) {
    // This would typically use a system service to announce the message
    // For now, we'll use semantics to mark it as a live region
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.semantics {
            contentDescription = message
            liveRegion = priority
        }
    )
}

/**
 * Helper function to format time for accessibility.
 */
fun formatTimeForAccessibility(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return when {
        minutes == 0 -> "$remainingSeconds seconds"
        remainingSeconds == 0 -> "$minutes minutes"
        else -> "$minutes minutes and $remainingSeconds seconds"
    }
}

/**
 * Helper function to format weight for accessibility.
 */
fun formatWeightForAccessibility(weight: Double): String {
    return "${weight} grams"
}

/**
 * Helper function to format brew ratio for accessibility.
 */
fun formatBrewRatioForAccessibility(ratio: Double): String {
    return "brew ratio 1 to ${String.format("%.1f", ratio)}"
}

/**
 * Helper function to format date for accessibility.
 */
fun formatDateForAccessibility(dateString: String): String {
    // This would typically parse the date and format it in a more accessible way
    return dateString.replace("/", " ").replace("-", " ")
}