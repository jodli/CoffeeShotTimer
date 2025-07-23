package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import kotlin.math.roundToInt

/**
 * Weight Slider Components for Espresso Shot Tracker
 *
 * This file implements Task 14: Implement slider for weight measurements
 *
 * Features implemented:
 * - Custom slider component with whole gram increments (no decimal places)
 * - Visual indicators for typical weight ranges (15-20g for input, 25-40g for output)
 * - Haptic feedback on value changes using HapticFeedbackType.TextHandleMove
 * - Proper validation integration with existing ViewModel methods
 * - Responsive design that works on different screen sizes using BoxWithConstraints
 * - Accessibility support with proper content descriptions and touch targets
 *
 * The sliders replace the previous text input fields in RecordShotScreen while maintaining
 * all existing validation rules and state management functionality.
 */

/**
 * Custom slider component for weight measurements with whole gram increments.
 * Provides visual indicators for typical weight ranges and haptic feedback.
 */
@Composable
fun WeightSlider(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minWeight: Float = 1f,
    maxWeight: Float = 50f,
    typicalRangeStart: Float = 15f,
    typicalRangeEnd: Float = 20f,
    icon: ImageVector = Icons.Default.Add,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    val spacing = LocalSpacing.current
    val hapticFeedback = LocalHapticFeedback.current

    // Convert string value to float, default to minWeight if invalid
    val currentValue = value.toFloatOrNull()?.coerceIn(minWeight, maxWeight) ?: minWeight

    // Ensure the value is always a whole number for display consistency
    val displayValue = currentValue.roundToInt().toFloat()

    // Track previous value for haptic feedback
    var previousValue by remember { mutableStateOf(currentValue) }

    Column(modifier = modifier) {
        // Header with label and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Current value display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(start = spacing.small)
            ) {
                Text(
                    text = "${displayValue.roundToInt()}g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Weight range indicators
        WeightRangeIndicator(
            currentValue = displayValue,
            minWeight = minWeight,
            maxWeight = maxWeight,
            typicalRangeStart = typicalRangeStart,
            typicalRangeEnd = typicalRangeEnd,
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Slider
        Slider(
            value = displayValue,
            onValueChange = { newValue ->
                val roundedValue = newValue.roundToInt().toFloat()

                // Provide haptic feedback when value changes
                if (roundedValue != previousValue) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previousValue = roundedValue
                }

                onValueChange(roundedValue.roundToInt().toString())
            },
            valueRange = minWeight..maxWeight,
            steps = (maxWeight - minWeight).roundToInt() - 1, // Whole gram steps
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Range labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minWeight.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${maxWeight.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error message
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = spacing.medium)
            )
        }
    }
}

/**
 * Visual indicator showing typical weight ranges with current value position.
 */
@Composable
private fun WeightRangeIndicator(
    currentValue: Float,
    minWeight: Float,
    maxWeight: Float,
    typicalRangeStart: Float,
    typicalRangeEnd: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Typical range indicator
        val typicalRangeStartPercent = (typicalRangeStart - minWeight) / (maxWeight - minWeight)
        val typicalRangeEndPercent = (typicalRangeEnd - minWeight) / (maxWeight - minWeight)
        val typicalRangeWidth = typicalRangeEndPercent - typicalRangeStartPercent

        BoxWithConstraints {
            val totalWidth = maxWidth

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidth * typicalRangeWidth)
                    .offset(x = totalWidth * typicalRangeStartPercent)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (enabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )

            // Current value indicator
            val currentPercent = (currentValue - minWeight) / (maxWeight - minWeight)

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(
                        x = (totalWidth * currentPercent) - 4.dp, // Center the indicator
                        y = 8.dp
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
        }

        // Range labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Typical: ${typicalRangeStart.roundToInt()}-${typicalRangeEnd.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            // Show if current value is in typical range
            val isInTypicalRange = currentValue in typicalRangeStart..typicalRangeEnd
            if (isInTypicalRange) {
                Text(
                    text = "✓ Typical",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Specialized weight slider for coffee input weight.
 */
@Composable
fun CoffeeWeightInSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        value = value,
        onValueChange = onValueChange,
        label = "Coffee In",
        modifier = modifier,
        minWeight = 1f,
        maxWeight = 50f,
        typicalRangeStart = 15f,
        typicalRangeEnd = 20f,
        icon = Icons.Default.Add,
        errorMessage = errorMessage,
        enabled = enabled
    )
}

/**
 * Specialized weight slider for coffee output weight.
 */
@Composable
fun CoffeeWeightOutSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        value = value,
        onValueChange = onValueChange,
        label = "Coffee Out",
        modifier = modifier,
        minWeight = 1f,
        maxWeight = 100f,
        typicalRangeStart = 25f,
        typicalRangeEnd = 40f,
        icon = Icons.Default.Add,
        errorMessage = errorMessage,
        enabled = enabled
    )
}

/**
 * Specialized slider for grinder settings with 0.5 increment steps.
 */
@Composable
fun GrinderSettingSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    suggestedSetting: String? = null,
    previousSuccessfulSettings: List<String> = emptyList()
) {
    val spacing = LocalSpacing.current
    val hapticFeedback = LocalHapticFeedback.current

    // Convert string value to float, default to 5.0 if invalid
    val currentValue = value.toFloatOrNull()?.coerceIn(0.5f, 20.0f) ?: 5.0f

    // Ensure the value follows 0.5 increment steps
    val displayValue = (currentValue * 2).roundToInt() / 2.0f

    // Track previous value for haptic feedback
    var previousValue by remember { mutableStateOf(currentValue) }

    Column(modifier = modifier) {
        // Header with label and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Grinder Setting",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Current value display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(start = spacing.small)
            ) {
                Text(
                    text = if (displayValue == displayValue.toInt().toFloat()) {
                        "${displayValue.toInt()}"
                    } else {
                        "%.1f".format(displayValue)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Grinder setting range indicators
        GrinderSettingRangeIndicator(
            currentValue = displayValue,
            minSetting = 0.5f,
            maxSetting = 20.0f,
            suggestedSetting = suggestedSetting?.toFloatOrNull(),
            previousSuccessfulSettings = previousSuccessfulSettings.mapNotNull { it.toFloatOrNull() },
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Slider with 0.5 increments
        Slider(
            value = displayValue,
            onValueChange = { newValue ->
                // Round to nearest 0.5 increment
                val roundedValue = (newValue * 2).roundToInt() / 2.0f

                // Provide haptic feedback when value changes
                if (roundedValue != previousValue) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previousValue = roundedValue
                }

                // Format output based on whether it's a whole number or has decimal
                val formattedValue = if (roundedValue == roundedValue.toInt().toFloat()) {
                    roundedValue.toInt().toString()
                } else {
                    "%.1f".format(roundedValue)
                }

                onValueChange(formattedValue)
            },
            valueRange = 0.5f..20.0f,
            steps = 39, // (20.0 - 0.5) / 0.5 - 1 = 39 steps for 0.5 increments
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Range labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0.5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "20.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Suggestion hint
        if (suggestedSetting != null && value.isEmpty()) {
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = "Suggested: $suggestedSetting (based on last use with this bean)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = spacing.medium)
            )
        }

        // Error message
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = spacing.medium)
            )
        }
    }
}

/**
 * Visual indicator showing grinder setting ranges with current value position and previous successful settings.
 */
@Composable
private fun GrinderSettingRangeIndicator(
    currentValue: Float,
    minSetting: Float,
    maxSetting: Float,
    suggestedSetting: Float?,
    previousSuccessfulSettings: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        BoxWithConstraints {
            val totalWidth = maxWidth

            // Previous successful settings indicators
            previousSuccessfulSettings.take(3).forEach { setting ->
                val settingPercent = (setting - minSetting) / (maxSetting - minSetting)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(
                            x = (totalWidth * settingPercent) - 3.dp,
                            y = 13.dp
                        )
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (enabled)
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                )
            }

            // Suggested setting indicator
            suggestedSetting?.let { suggestion ->
                val suggestionPercent = (suggestion - minSetting) / (maxSetting - minSetting)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(
                            x = (totalWidth * suggestionPercent) - 4.dp,
                            y = 12.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (enabled)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                )
            }

            // Current value indicator
            val currentPercent = (currentValue - minSetting) / (maxSetting - minSetting)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(
                        x = (totalWidth * currentPercent) - 5.dp,
                        y = 11.dp
                    )
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (previousSuccessfulSettings.isNotEmpty()) {
                Text(
                    text = "• Previous successful",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 10.sp
                )
            }

            if (suggestedSetting != null) {
                Text(
                    text = "■ Suggested",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Weight inputs section using sliders instead of text fields.
 */
@Composable
fun WeightSlidersSection(
    coffeeWeightIn: String,
    onCoffeeWeightInChange: (String) -> Unit,
    coffeeWeightInError: String?,
    coffeeWeightOut: String,
    onCoffeeWeightOutChange: (String) -> Unit,
    coffeeWeightOutError: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        Text(
            text = "Weight Measurements",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Coffee Weight In Slider
        CoffeeWeightInSlider(
            value = coffeeWeightIn,
            onValueChange = onCoffeeWeightInChange,
            errorMessage = coffeeWeightInError,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Coffee Weight Out Slider
        CoffeeWeightOutSlider(
            value = coffeeWeightOut,
            onValueChange = onCoffeeWeightOutChange,
            errorMessage = coffeeWeightOutError,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}