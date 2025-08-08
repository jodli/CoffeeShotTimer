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
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Engineering
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Constants for weight slider bounds
 */
object WeightSliderConstants {
    const val COFFEE_IN_MIN_WEIGHT = 15f
    const val COFFEE_IN_MAX_WEIGHT = 20f
    const val COFFEE_OUT_MIN_WEIGHT = 25f
    const val COFFEE_OUT_MAX_WEIGHT = 55f
}

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
        label = stringResource(R.string.label_coffee_in),
        modifier = modifier,
        minWeight = WeightSliderConstants.COFFEE_IN_MIN_WEIGHT,
        maxWeight = WeightSliderConstants.COFFEE_IN_MAX_WEIGHT,
        icon = Icons.AutoMirrored.Filled.Input,
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
        label = stringResource(R.string.label_coffee_out),
        modifier = modifier,
        minWeight = WeightSliderConstants.COFFEE_OUT_MIN_WEIGHT,
        maxWeight = WeightSliderConstants.COFFEE_OUT_MAX_WEIGHT,
        icon = Icons.Default.Output,
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

    // Convert string value to float, supporting both decimal point and comma
    val currentValue = parseLocaleAwareFloat(value)?.coerceIn(0.5f, 20.0f) ?: 5.0f

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
                    text = if (displayValue == displayValue.toInt().toFloat()) {
                        stringResource(R.string.format_weight_display_int, displayValue.toInt())
                    } else {
                        // Use locale-independent formatting for consistency
                        String.format(Locale.US, "%.1f", displayValue)
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

                // Format output using locale-independent decimal point notation
                val formattedValue = if (roundedValue == roundedValue.toInt().toFloat()) {
                    roundedValue.toInt().toString()
                } else {
                    // Use US locale to ensure decimal point (.) instead of comma (,)
                    String.format(java.util.Locale.US, "%.1f", roundedValue)
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
                text = stringResource(R.string.weight_min_value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.weight_max_value),
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
                        .size(spacing.qualityIndicator)
                        .offset(
                            x = (totalWidth * suggestionPercent) - spacing.extraSmall,
                            y = spacing.medium - spacing.extraSmall
                        )
                        .clip(RoundedCornerShape(spacing.cornerSmall))
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
                    .size(spacing.qualityIndicator + 2.dp)
                    .offset(
                        x = (totalWidth * currentPercent) - (spacing.qualityIndicator + 2.dp) / 2,
                        y = spacing.medium - 5.dp
                    )
                    .clip(RoundedCornerShape(spacing.cornerSmall + 1.dp))
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
    coffeeWeightInError: String?,
    coffeeWeightOut: String,
    onCoffeeWeightOutChange: (String) -> Unit,
    coffeeWeightOutError: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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