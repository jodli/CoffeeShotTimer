package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

@Composable
fun GrinderScaleSetup(
    scaleMin: String,
    scaleMax: String,
    stepSize: String = "0.5",
    onScaleMinChange: (String) -> Unit,
    onScaleMaxChange: (String) -> Unit,
    onStepSizeChange: (String) -> Unit = {},
    onPresetSelected: (Int, Int) -> Unit,
    onStepSizePresetSelected: (Double) -> Unit = {},
    minError: String?,
    maxError: String?,
    stepSizeError: String? = null,
    generalError: String?,
    validationSuggestion: String?,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true,
    showPresets: Boolean = true,
    showStepSize: Boolean = true
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.text_grinder_scale_range)
        )

        Spacer(modifier = Modifier.height(spacing.small))

        if (showDescription) {
            Text(
                text = stringResource(R.string.text_grinder_scale_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Common presets
        if (showPresets) {
            Text(
            text = stringResource(R.string.text_common_presets),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.small))

        val presets = listOf(
            1 to 10,
            30 to 80,
            50 to 60,
            0 to 100
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            presets.take(2).forEach { (min, max) ->
                CoffeeSecondaryButton(
                    text = "$min-$max",
                    onClick = { onPresetSelected(min, max) },
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            presets.drop(2).forEach { (min, max) ->
                CoffeeSecondaryButton(
                    text = "$min-$max",
                    onClick = { onPresetSelected(min, max) },
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Custom range inputs
        Text(
            text = stringResource(R.string.text_custom_range),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeTextField(
                value = scaleMin,
                onValueChange = onScaleMinChange,
                label = stringResource(R.string.label_grinder_minimum),
                placeholder = stringResource(R.string.placeholder_grinder_min),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = minError != null,
                errorMessage = minError,
                modifier = Modifier.weight(1f)
            )

            CoffeeTextField(
                value = scaleMax,
                onValueChange = onScaleMaxChange,
                label = stringResource(R.string.label_grinder_maximum),
                placeholder = stringResource(R.string.placeholder_grinder_max),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = maxError != null,
                errorMessage = maxError,
                modifier = Modifier.weight(1f)
            )
        }

        // Step size selector
        if (showStepSize) {
            Spacer(modifier = Modifier.height(spacing.medium))
            
            StepSizeSelector(
                stepSize = stepSize,
                onStepSizeChange = onStepSizeChange,
                onPresetSelected = onStepSizePresetSelected,
                stepSizeError = stepSizeError,
                scaleMin = scaleMin.toIntOrNull(),
                scaleMax = scaleMax.toIntOrNull(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (generalError != null) {
            Spacer(modifier = Modifier.height(spacing.medium))
            GentleValidationMessage(
                message = generalError,
                suggestion = validationSuggestion ?: "",
                modifier = Modifier.fillMaxWidth(),
                extraContentPadding = PaddingValues(
                    horizontal = spacing.medium,
                    vertical = spacing.small
                )
            )
            Spacer(modifier = Modifier.height(spacing.small))
        }
    }
}
