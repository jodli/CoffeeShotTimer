package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.ErrorCard
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
        
        // Success icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        // Title
        Text(
            text = stringResource(R.string.equipment_setup_summary_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Description
        Text(
            text = stringResource(R.string.equipment_setup_summary_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Error card if present
        error?.let { errorMessage ->
            ErrorCard(
                title = stringResource(R.string.error_saving_configuration),
                message = errorMessage,
                onRetry = onConfirm,
                onDismiss = onClearError
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }
        
        // Loading indicator
        if (isLoading) {
            LoadingIndicator(
                message = stringResource(R.string.text_saving_configuration),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }
        
        // Grinder configuration summary
        ConfigurationSummaryCard(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.equipment_setup_grinder_title),
            items = listOf(
                stringResource(R.string.summary_grinder_range_format, grinderMin, grinderMax),
                stringResource(R.string.summary_grinder_steps_format, 
                    (grinderMax.toIntOrNull() ?: 0) - (grinderMin.toIntOrNull() ?: 0))
            )
        )
        
        // Basket configuration summary
        ConfigurationSummaryCard(
            icon = Icons.Default.FilterAlt,
            title = stringResource(R.string.equipment_setup_basket_title),
            items = listOf(
                stringResource(R.string.summary_coffee_in_format, coffeeInMin, coffeeInMax),
                stringResource(R.string.summary_coffee_out_format, coffeeOutMin, coffeeOutMax),
                stringResource(R.string.summary_basket_type_format, determineBasketType(coffeeInMin, coffeeInMax))
            )
        )
        
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
        
        Spacer(modifier = Modifier.height(spacing.large))
    }
}

/**
 * Card showing configuration summary
 */
@Composable
fun ConfigurationSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
