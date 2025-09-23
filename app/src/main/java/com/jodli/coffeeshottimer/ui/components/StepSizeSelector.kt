package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Component for selecting grinder step size with preset options and custom input.
 * Allows users to choose from common step sizes or enter a custom value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepSizeSelector(
    stepSize: String,
    onStepSizeChange: (String) -> Unit,
    onPresetSelected: (Double) -> Unit,
    stepSizeError: String? = null,
    scaleMin: Int? = null,
    scaleMax: Int? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Parse current step size for comparison with presets
    val currentStepSize by remember(stepSize) {
        derivedStateOf { stepSize.toDoubleOrNull() }
    }

    Column(modifier = modifier) {
        // Title
        Text(
            text = stringResource(R.string.text_step_size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Description
        Text(
            text = stringResource(R.string.text_step_size_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Preset chips
        Text(
            text = stringResource(R.string.text_common_step_sizes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.small))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            GrinderConfiguration.STEP_SIZE_PRESETS.forEach { preset ->
                FilterChip(
                    selected = currentStepSize == preset,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            text = formatStepSize(preset),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Custom input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            CoffeeTextField(
                value = stepSize,
                onValueChange = onStepSizeChange,
                label = stringResource(R.string.label_custom_step_size),
                placeholder = stringResource(R.string.placeholder_step_size),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = stepSizeError != null,
                errorMessage = stepSizeError,
                modifier = Modifier.weight(1f)
            )
        }

        // Show validation info if range is provided
        if (scaleMin != null && scaleMax != null && stepSize.isNotBlank()) {
            val stepValue = stepSize.toDoubleOrNull()
            if (stepValue != null && stepValue > 0) {
                val rangeSize = scaleMax - scaleMin
                val numberOfSteps = (rangeSize / stepValue).toInt()

                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = stringResource(
                        R.string.text_step_size_info,
                        numberOfSteps,
                        scaleMin,
                        formatStepSize(stepValue),
                        scaleMax
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (stepSizeError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

/**
 * Format step size for display, showing appropriate decimal places.
 */
private fun formatStepSize(stepSize: Double): String {
    return when {
        stepSize >= 1.0 && stepSize % 1.0 == 0.0 -> stepSize.toInt().toString()
        else -> String.format(java.util.Locale.ROOT, "%.2f", stepSize).trimEnd('0').trimEnd('.')
    }
}
