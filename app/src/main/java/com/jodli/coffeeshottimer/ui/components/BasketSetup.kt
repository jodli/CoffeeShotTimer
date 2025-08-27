package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.BasketPreset
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

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
    showDescription: Boolean = true,
    showPresets: Boolean = true
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Scale,
            title = stringResource(R.string.text_basket_configuration)
        )

        Spacer(modifier = Modifier.height(spacing.small))

        if (showDescription) {
            Text(
                text = stringResource(R.string.text_basket_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Preset buttons - Single and Double
        if (showPresets) {
            Text(
                text = stringResource(R.string.text_basket_presets),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.button_single_basket),
                    onClick = { onPresetSelected(BasketPreset.SINGLE) },
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false
                )

                CoffeeSecondaryButton(
                    text = stringResource(R.string.button_double_basket),
                    onClick = { onPresetSelected(BasketPreset.DOUBLE) },
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Manual range inputs
        Text(
            text = stringResource(R.string.text_manual_basket_range),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.small))

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

        // Validation messages
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
        } else if (coffeeInMin.isNotBlank() && coffeeInMax.isNotBlank() && 
                   coffeeOutMin.isNotBlank() && coffeeOutMax.isNotBlank()) {
            // Check if all values are valid numbers and within reasonable ranges
            val inMinVal = coffeeInMin.toFloatOrNull()
            val inMaxVal = coffeeInMax.toFloatOrNull()
            val outMinVal = coffeeOutMin.toFloatOrNull()
            val outMaxVal = coffeeOutMax.toFloatOrNull()
            
            if (inMinVal != null && inMaxVal != null && outMinVal != null && outMaxVal != null &&
                inMinVal < inMaxVal && outMinVal < outMaxVal &&
                coffeeInMinError == null && coffeeInMaxError == null &&
                coffeeOutMinError == null && coffeeOutMaxError == null) {
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                // Determine shot type based on average coffee in value
                val avgIn = (inMinVal + inMaxVal) / 2
                val shotType = if (avgIn < 13f) "single" else "double"
                
                Text(
                    text = stringResource(R.string.text_basket_validation_success, shotType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
