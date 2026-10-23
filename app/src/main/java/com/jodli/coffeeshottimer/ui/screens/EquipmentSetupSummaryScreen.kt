package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.DataCell
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.LedgerSection
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Summary screen for the equipment setup flow.
 * Shows all configured settings and allows user to confirm and save.
 */
@Composable
fun EquipmentSetupSummaryScreen(
    grinderMin: String,
    grinderMax: String,
    grinderStepSize: String,
    coffeeInMin: String,
    coffeeInMax: String,
    coffeeOutMin: String,
    coffeeOutMax: String,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Spacer(modifier = Modifier.height(spacing.medium))

        // Title
        Text(
            text = stringResource(R.string.equipment_setup_summary_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Description
        Text(
            text = stringResource(R.string.equipment_setup_summary_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        LedgerSection(
            title = stringResource(R.string.title_equipment_settings),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            DataCell(label = stringResource(R.string.summary_scale_range_label)) {
                Text(
                    text = "$grinderMin – $grinderMax",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DataCell(label = stringResource(R.string.summary_step_size_label)) {
                Text(
                    text = grinderStepSize,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DataCell(label = stringResource(R.string.summary_steps_label)) {
                Text(
                    text = calculateGrinderSteps(grinderMin, grinderMax, grinderStepSize).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LedgerSection(title = stringResource(R.string.title_basket_settings)) {
            DataCell(label = stringResource(R.string.summary_coffee_in_label)) {
                Text(
                    text = "$coffeeInMin – $coffeeInMax g",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DataCell(label = stringResource(R.string.summary_coffee_out_label)) {
                Text(
                    text = "$coffeeOutMin – $coffeeOutMax g",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DataCell(label = stringResource(R.string.summary_basket_type_label)) {
                Text(
                    text = determineBasketType(coffeeInMin, coffeeInMax),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Error card if present
        error?.let { errorMessage ->
            ErrorCard(
                title = stringResource(R.string.error_saving_configuration),
                message = errorMessage,
                onRetry = onConfirm,
                onDismiss = onClearError
            )
        }

        // Loading indicator
        if (isLoading) {
            LoadingIndicator(
                message = stringResource(R.string.text_saving_configuration),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_back),
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            )

            CoffeePrimaryButton(
                text = stringResource(R.string.button_save_and_continue),
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Calculate the number of grinder steps based on range and step size
 */
private fun calculateGrinderSteps(grinderMin: String, grinderMax: String, stepSize: String): Int {
    val minValue = grinderMin.toIntOrNull() ?: return 0
    val maxValue = grinderMax.toIntOrNull() ?: return 0
    val stepSizeValue = stepSize.toDoubleOrNull() ?: return 0

    if (stepSizeValue <= 0) return 0

    val rangeSize = maxValue - minValue
    return (rangeSize / stepSizeValue).toInt() + 1 // +1 because we include both endpoints
}

@Composable
private fun determineBasketType(coffeeInMin: String, coffeeInMax: String): String {
    val inMin = coffeeInMin.toFloatOrNull() ?: return stringResource(R.string.basket_type_custom)
    val inMax = coffeeInMax.toFloatOrNull() ?: return stringResource(R.string.basket_type_custom)

    return when {
        inMin <= 10 && inMax <= 14 -> stringResource(R.string.basket_type_single)
        inMin >= 14 && inMax >= 20 -> stringResource(R.string.basket_type_double)
        else -> stringResource(R.string.basket_type_custom)
    }
}
