package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

private val GRINDER_SCALE_PRESETS = listOf(1 to 10, 30 to 80, 50 to 60, 0 to 100)

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
    showDescription: Boolean = true
) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.text_grinder_scale_range))

        if (showDescription) {
            Text(
                text = stringResource(R.string.text_grinder_scale_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        SquareSegmentedSwitch(
            options = GRINDER_SCALE_PRESETS.map { (min, max) -> "$min–$max" },
            selectedIndex = GRINDER_SCALE_PRESETS.indexOfFirst {
                it.first == scaleMin.toIntOrNull() && it.second == scaleMax.toIntOrNull()
            },
            onSelect = { index ->
                val (min, max) = GRINDER_SCALE_PRESETS[index]
                onPresetSelected(min, max)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        StepSizeSelector(
            stepSize = stepSize,
            onStepSizeChange = onStepSizeChange,
            onPresetSelected = onStepSizePresetSelected,
            stepSizeError = stepSizeError,
            scaleMin = scaleMin.toIntOrNull(),
            scaleMax = scaleMax.toIntOrNull(),
            modifier = Modifier.fillMaxWidth()
        )

        if (generalError != null) {
            GentleValidationMessage(
                message = generalError,
                suggestion = validationSuggestion.orEmpty(),
                modifier = Modifier.padding(top = spacing.medium)
            )
        }
    }
}
