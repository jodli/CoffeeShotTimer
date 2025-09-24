package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.util.Locale
import kotlin.math.roundToInt

// Constants removed - all slider ranges now come from basket and grinder configuration

/**
 * Parse a float value that may use either decimal point (.) or comma (,) as decimal separator.
 * This handles locale differences where some locales use comma as decimal separator.
 */
private fun parseLocaleAwareFloat(value: String): Float? {
    if (value.isBlank()) return null

    return try {
        // First try direct parsing (works for US locale with decimal point)
        value.toFloatOrNull() ?: run {
            // If that fails, try replacing comma with decimal point and parse
            value.replace(',', '.').toFloatOrNull()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Weight Slider Components for Espresso Shot Tracker
 *
 * This file implements Task 14: Implement slider for weight measurements
 *
 * Features implemented:
 * - Custom slider component with whole gram increments (no decimal places)
 * - Visual indicators for typical weight ranges (5-20g for input, 10-55g for output)
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
    icon: ImageVector,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    val spacing = LocalSpacing.current
    val hapticFeedback = LocalHapticFeedback.current

    // Convert string value to float, default to minWeight if invalid
    val currentValue = parseLocaleAwareFloat(value)?.coerceIn(minWeight, maxWeight) ?: minWeight

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
                    modifier = Modifier.size(spacing.iconSmall + spacing.extraSmall)
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
                    text = stringResource(R.string.format_weight_display_value_g, displayValue.roundToInt()),
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
                text = stringResource(R.string.format_weight_display_min_g, minWeight.roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.format_weight_display_max_g, maxWeight.roundToInt()),
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
 * Specialized weight slider for coffee input weight.
 * Supports dynamic min/max values from basket configuration.
 */
@Composable
fun CoffeeWeightInSlider(
    value: String,
    onValueChange: (String) -> Unit,
    minWeight: Float,
    maxWeight: Float,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.label_coffee_in),
        modifier = modifier,
        minWeight = minWeight,
        maxWeight = maxWeight,
        icon = Icons.AutoMirrored.Filled.Input,
        errorMessage = errorMessage,
        enabled = enabled
    )
}

/**
 * Specialized weight slider for coffee output weight.
 * Supports dynamic min/max values from basket configuration.
 */
@Composable
fun CoffeeWeightOutSlider(
    value: String,
    onValueChange: (String) -> Unit,
    minWeight: Float,
    maxWeight: Float,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.label_coffee_out),
        modifier = modifier,
        minWeight = minWeight,
        maxWeight = maxWeight,
        icon = Icons.Default.Output,
        errorMessage = errorMessage,
        enabled = enabled
    )
}

/**
 * Format grind value according to the step size precision.
 */
private fun formatGrindValueForStepSize(value: Float, stepSize: Float): String {
    return when {
        stepSize >= 1.0f && value % 1.0f == 0.0f -> value.toInt().toString()
        stepSize >= 0.5f -> String.format(java.util.Locale.US, "%.1f", value)
        stepSize >= 0.1f -> String.format(java.util.Locale.US, "%.1f", value)
        stepSize >= 0.01f -> String.format(java.util.Locale.US, "%.2f", value)
        else -> String.format(java.util.Locale.US, "%.2f", value)
    }
}

/**
 * Specialized slider for grinder settings with configurable increment steps.
 */
@Composable
fun GrinderSettingSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    suggestedSetting: String? = null,
    previousSuccessfulSettings: List<String> = emptyList(),
    minSetting: Float,
    maxSetting: Float,
    stepSize: Float = 0.5f
) {
    val spacing = LocalSpacing.current
    val hapticFeedback = LocalHapticFeedback.current

    val safeMin = if (minSetting < 0f) 0f else minSetting
    val safeMax = if (maxSetting <= safeMin) safeMin + 1f else maxSetting
    val defaultValue = (safeMin + safeMax) / 2f

    // Convert string value to float, supporting both decimal point and comma
    val currentValue = parseLocaleAwareFloat(value)?.coerceIn(safeMin, safeMax) ?: defaultValue

    // Ensure the value follows the configured step size increments using integer tenths to avoid float drift
    val tenthsStep = (stepSize * 10f).roundToInt().coerceAtLeast(1)
    val minTenths = (safeMin * 10f).roundToInt()
    val currentTenths = (currentValue * 10f).roundToInt()
    val stepsFromMinInt = ((currentTenths - minTenths + tenthsStep / 2) / tenthsStep)
    val displayTenths = minTenths + (stepsFromMinInt * tenthsStep)
    val displayValue = displayTenths / 10f

    // Track previous value for haptic feedback
    var previousValue by remember { mutableStateOf(currentValue) }

    // Formatter for min/max labels and current value
    fun formatValue(v: Float): String = if (v == v.toInt().toFloat()) {
        v.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", v)
    }

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
                    imageVector = Icons.Filled.Engineering,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(spacing.iconSmall + spacing.extraSmall)
                )
                Text(
                    text = stringResource(R.string.text_grinder_setting),
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
                    text = formatValue(displayValue),
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
            minSetting = safeMin,
            maxSetting = safeMax,
            suggestedSetting = suggestedSetting?.toFloatOrNull(),
            previousSuccessfulSettings = previousSuccessfulSettings.mapNotNull { it.toFloatOrNull() },
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Slider with configurable step size increments
        Slider(
            value = displayValue,
            onValueChange = { newValue ->
                // Round to nearest step increment using integer tenths to avoid float drift
                val tenthsStep = (stepSize * 10f).roundToInt().coerceAtLeast(1)
                val minTenths = (safeMin * 10f).roundToInt()
                val newTenths = (newValue * 10f).roundToInt()
                val stepsFromMinInt = ((newTenths - minTenths + tenthsStep / 2) / tenthsStep)
                val roundedTenths = minTenths + (stepsFromMinInt * tenthsStep)
                val roundedValue = roundedTenths / 10f

                // Provide haptic feedback when value changes
                if (roundedValue != previousValue) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    previousValue = roundedValue
                }

                // Format output using appropriate precision based on step size
                val formattedValue = formatGrindValueForStepSize(roundedValue, stepSize)

                onValueChange(formattedValue)
            },
            valueRange = safeMin..safeMax,
            steps = run {
                val tenthsStep = (stepSize * 10f).roundToInt().coerceAtLeast(1)
                val minTenths = (safeMin * 10f).roundToInt()
                val maxTenths = (safeMax * 10f).roundToInt()
                kotlin.math.max(0, ((maxTenths - minTenths) / tenthsStep) - 1)
            },
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
                text = formatValue(safeMin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatValue(safeMax),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Suggestion hint
        if (suggestedSetting != null && value.isEmpty()) {
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = stringResource(R.string.format_suggested_setting, suggestedSetting),
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
            .height(spacing.sliderHeight)
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(spacing.cornerLarge))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        BoxWithConstraints {
            val totalWidth = maxWidth

            // Previous successful settings indicators
            previousSuccessfulSettings.take(3).forEach { setting ->
                val settingPercent = (setting - minSetting) / (maxSetting - minSetting)
                Box(
                    modifier = Modifier
                        .size(spacing.qualityIndicator - 2.dp)
                        .offset(
                            x = (totalWidth * settingPercent) - (spacing.qualityIndicator - 2.dp) / 2,
                            y = spacing.medium - 3.dp
                        )
                        .clip(RoundedCornerShape(spacing.cornerSmall - 1.dp))
                        .background(
                            if (enabled) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                )
            }

            // Suggested setting indicator
            suggestedSetting?.let { suggestion ->
                val suggestionPercent = (suggestion - minSetting) / (maxSetting - minSetting)
                Box(
                    modifier = Modifier
                        .size(spacing.qualityIndicator)
                        .offset(
                            x = (totalWidth * suggestionPercent) - spacing.extraSmall,
                            y = spacing.medium - spacing.extraSmall
                        )
                        .clip(RoundedCornerShape(spacing.cornerSmall))
                        .background(
                            if (enabled) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                )
            }

            // Current value indicator
            val currentPercent = (currentValue - minSetting) / (maxSetting - minSetting)
            Box(
                modifier = Modifier
                    .size(spacing.qualityIndicator + 2.dp)
                    .offset(
                        x = (totalWidth * currentPercent) - (spacing.qualityIndicator + 2.dp) / 2,
                        y = spacing.medium - 5.dp
                    )
                    .clip(RoundedCornerShape(spacing.cornerSmall + 1.dp))
                    .background(
                        if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
            )
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.extraLarge + spacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (previousSuccessfulSettings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.symbol_previous_successful),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (suggestedSetting != null) {
                Text(
                    text = stringResource(R.string.symbol_square),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
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
    coffeeWeightOut: String,
    onCoffeeWeightOutChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    basketCoffeeInMin: Float,
    basketCoffeeInMax: Float,
    basketCoffeeOutMin: Float,
    basketCoffeeOutMax: Float
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Scale,
            title = stringResource(R.string.text_weight_measurements)
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Coffee Weight In Slider
        CoffeeWeightInSlider(
            value = coffeeWeightIn,
            onValueChange = onCoffeeWeightInChange,
            errorMessage = null, // Suppress validation errors since sliders clamp values
            enabled = enabled,
            minWeight = basketCoffeeInMin,
            maxWeight = basketCoffeeInMax,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Coffee Weight Out Slider
        CoffeeWeightOutSlider(
            value = coffeeWeightOut,
            onValueChange = onCoffeeWeightOutChange,
            errorMessage = null, // Suppress validation errors since sliders clamp values
            enabled = enabled,
            minWeight = basketCoffeeOutMin,
            maxWeight = basketCoffeeOutMax,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
