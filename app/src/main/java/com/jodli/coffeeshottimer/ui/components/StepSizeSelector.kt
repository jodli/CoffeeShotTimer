package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Component for selecting grinder step size with preset options and custom input.
 * Allows users to choose from common step sizes or enter a custom value.
 */
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

    val currentStepSize = stepSize.toDoubleOrNull()

    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.text_step_size))

        Text(
            text = stringResource(R.string.text_step_size_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SquareSegmentedSwitch(
            options = GrinderConfiguration.STEP_SIZE_PRESETS.map(::formatStepSize),
            selectedIndex = GrinderConfiguration.STEP_SIZE_PRESETS.indexOfFirst { it == currentStepSize },
            onSelect = { onPresetSelected(GrinderConfiguration.STEP_SIZE_PRESETS[it]) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        CoffeeTextField(
            value = stepSize,
            onValueChange = onStepSizeChange,
            label = stringResource(R.string.label_custom_step_size),
            placeholder = stringResource(R.string.placeholder_step_size),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = stepSizeError != null,
            errorMessage = stepSizeError,
            modifier = Modifier.fillMaxWidth()
        )

        // Show validation info if range is provided
        if (scaleMin != null && scaleMax != null && currentStepSize != null) {
            if (currentStepSize > 0) {
                val rangeSize = scaleMax - scaleMin
                val numberOfSteps = (rangeSize / currentStepSize).toInt()

                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = stringResource(
                        R.string.text_step_size_info,
                        numberOfSteps,
                        scaleMin,
                        formatStepSize(currentStepSize),
                        scaleMax
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (stepSizeError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Format step size for display, showing appropriate decimal places.
 */
internal fun formatStepSize(stepSize: Double): String {
    return when {
        stepSize >= 1.0 && stepSize % 1.0 == 0.0 -> stepSize.toInt().toString()
        else -> String.format(java.util.Locale.ROOT, "%.2f", stepSize).trimEnd('0').trimEnd('.')
    }
}
