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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.IconTextRow
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Welcome screen for the equipment setup flow.
 * Explains what will be configured and why it's important.
 */
@Composable
fun EquipmentSetupWelcomeScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
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
        Spacer(modifier = Modifier.height(spacing.large))

        // Title
        Text(
            text = stringResource(R.string.equipment_setup_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Description
        Text(
            text = stringResource(R.string.equipment_setup_welcome_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        IconTextRow(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.equipment_setup_grinder_title),
            description = stringResource(R.string.equipment_setup_grinder_description),
            modifier = Modifier.padding(top = 24.dp)
        )

        IconTextRow(
            icon = Icons.Default.FilterAlt,
            title = stringResource(R.string.equipment_setup_basket_title),
            description = stringResource(R.string.equipment_setup_basket_description)
        )

        IconTextRow(
            icon = Icons.Default.Speed,
            title = stringResource(R.string.equipment_setup_benefits_title),
            description = stringResource(R.string.equipment_setup_benefits_description)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_skip_for_now),
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            )

            CoffeePrimaryButton(
                text = stringResource(R.string.button_get_started),
                onClick = onContinue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
