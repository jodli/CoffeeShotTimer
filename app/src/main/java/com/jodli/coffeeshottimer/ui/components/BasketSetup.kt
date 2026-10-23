package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.BasketPreset
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.zoneColors

/** Average coffee-in weight (g) below which the ranges read as a single-shot setup. */
private const val SINGLE_BASKET_MAX_AVG_COFFEE_IN = 13f

private fun BasketPreset.matches(
    inMin: String,
    inMax: String,
    outMin: String,
    outMax: String
): Boolean {
    return inMin.toFloatOrNull() == coffeeInMin &&
        inMax.toFloatOrNull() == coffeeInMax &&
        outMin.toFloatOrNull() == coffeeOutMin &&
        outMax.toFloatOrNull() == coffeeOutMax
}

@Composable
fun BasketSetup(
    coffeeInMin: String,
    coffeeInMax: String,
    coffeeOutMin: String,
    coffeeOutMax: String,
    onCoffeeInMinChange: (String) -> Unit,
    onCoffeeInMaxChange: (String) -> Unit,
    onCoffeeOutMinChange: (String) -> Unit,
    onCoffeeOutMaxChange: (String) -> Unit,
    onPresetSelected: (BasketPreset) -> Unit,
    coffeeInMinError: String? = null,
    coffeeInMaxError: String? = null,
    coffeeOutMinError: String? = null,
    coffeeOutMaxError: String? = null,
    generalError: String? = null,
    validationSuggestion: String? = null,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true
) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.text_basket_presets))

        if (showDescription) {
            Text(
                text = stringResource(R.string.text_basket_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        SquareSegmentedSwitch(
            options = listOf(
                stringResource(R.string.button_single_basket),
                stringResource(R.string.button_double_basket)
            ),
            selectedIndex = BasketPreset.entries.indexOfFirst {
                it.matches(coffeeInMin, coffeeInMax, coffeeOutMin, coffeeOutMax)
            },
            onSelect = { onPresetSelected(BasketPreset.entries[it]) },
            modifier = Modifier.fillMaxWidth()
        )

        SectionLabel(text = stringResource(R.string.text_manual_basket_range))

        // Coffee In range inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeTextField(
                value = coffeeInMin,
                onValueChange = onCoffeeInMinChange,
                label = stringResource(R.string.label_coffee_in_min),
                placeholder = stringResource(R.string.placeholder_coffee_in_min),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = coffeeInMinError != null,
                errorMessage = coffeeInMinError,
                modifier = Modifier.weight(1f)
            )

            CoffeeTextField(
                value = coffeeInMax,
                onValueChange = onCoffeeInMaxChange,
                label = stringResource(R.string.label_coffee_in_max),
                placeholder = stringResource(R.string.placeholder_coffee_in_max),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = coffeeInMaxError != null,
                errorMessage = coffeeInMaxError,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Coffee Out range inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeTextField(
                value = coffeeOutMin,
                onValueChange = onCoffeeOutMinChange,
                label = stringResource(R.string.label_coffee_out_min),
                placeholder = stringResource(R.string.placeholder_coffee_out_min),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = coffeeOutMinError != null,
                errorMessage = coffeeOutMinError,
                modifier = Modifier.weight(1f)
            )

            CoffeeTextField(
                value = coffeeOutMax,
                onValueChange = onCoffeeOutMaxChange,
                label = stringResource(R.string.label_coffee_out_max),
                placeholder = stringResource(R.string.placeholder_coffee_out_max),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = coffeeOutMaxError != null,
                errorMessage = coffeeOutMaxError,
                modifier = Modifier.weight(1f)
            )
        }

        val avgCoffeeIn = basketAvgIn(coffeeInMin, coffeeInMax, coffeeOutMin, coffeeOutMax)
        val hasFieldError = listOf(coffeeInMinError, coffeeInMaxError, coffeeOutMinError, coffeeOutMaxError)
            .any { it != null }

        if (generalError != null) {
            GentleValidationMessage(
                message = generalError,
                suggestion = validationSuggestion.orEmpty(),
                modifier = Modifier.padding(top = spacing.medium)
            )
        } else if (avgCoffeeIn != null && !hasFieldError) {
            BasketRangeConfirmation(avgCoffeeIn = avgCoffeeIn)
        }
    }
}

/** The confirmation line under a complete, valid basket range: check mark plus shot type. */
@Composable
private fun BasketRangeConfirmation(avgCoffeeIn: Float) {
    val spacing = LocalSpacing.current
    val shotTypeRes = if (avgCoffeeIn < SINGLE_BASKET_MAX_AVG_COFFEE_IN) {
        R.string.text_basket_validation_success_single
    } else {
        R.string.text_basket_validation_success_double
    }

    Row(
        modifier = Modifier.padding(top = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.zoneColors.idealInk,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(shotTypeRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Average coffee-in weight when all four fields parse and are ordered; null otherwise. */
private fun basketAvgIn(
    coffeeInMin: String,
    coffeeInMax: String,
    coffeeOutMin: String,
    coffeeOutMax: String
): Float? {
    val inRange = orderedPair(coffeeInMin, coffeeInMax)
    val outRange = orderedPair(coffeeOutMin, coffeeOutMax)
    return if (inRange != null && outRange != null) {
        (inRange.first + inRange.second) / 2
    } else {
        null
    }
}

/** The parsed (min, max) pair when both parse and min < max; null otherwise. */
private fun orderedPair(min: String, max: String): Pair<Float, Float>? {
    val minVal = min.toFloatOrNull()
    val maxVal = max.toFloatOrNull()
    return if (minVal != null && maxVal != null && minVal < maxVal) {
        minVal to maxVal
    } else {
        null
    }
}
