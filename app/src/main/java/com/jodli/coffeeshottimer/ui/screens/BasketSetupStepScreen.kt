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
import androidx.compose.ui.text.font.FontWeight
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.BasketPreset
import com.jodli.coffeeshottimer.ui.components.BasketSetup
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Screen for the basket setup step in the equipment setup flow.
 * Allows users to configure their basket size and weight ranges.
 */
@Composable
fun BasketSetupStepScreen(
    coffeeInMin: String,
    coffeeInMax: String,
    coffeeOutMin: String,
    coffeeOutMax: String,
    coffeeInMinError: String?,
    coffeeInMaxError: String?,
    coffeeOutMinError: String?,
    coffeeOutMaxError: String?,
    generalError: String?,
    isValid: Boolean,
    onCoffeeInMinChange: (String) -> Unit,
    onCoffeeInMaxChange: (String) -> Unit,
    onCoffeeOutMinChange: (String) -> Unit,
    onCoffeeOutMaxChange: (String) -> Unit,
    onPresetSelected: (BasketPreset) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
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
            text = stringResource(R.string.equipment_setup_basket_step_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Description
        Text(
            text = stringResource(R.string.equipment_setup_basket_step_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        // Basket configuration card
        BasketSetup(
            coffeeInMin = coffeeInMin,
            coffeeInMax = coffeeInMax,
            coffeeOutMin = coffeeOutMin,
            coffeeOutMax = coffeeOutMax,
            onCoffeeInMinChange = onCoffeeInMinChange,
            onCoffeeInMaxChange = onCoffeeInMaxChange,
            onCoffeeOutMinChange = onCoffeeOutMinChange,
            onCoffeeOutMaxChange = onCoffeeOutMaxChange,
            onPresetSelected = onPresetSelected,
            coffeeInMinError = coffeeInMinError,
            coffeeInMaxError = coffeeInMaxError,
            coffeeOutMinError = coffeeOutMinError,
            coffeeOutMaxError = coffeeOutMaxError,
            generalError = generalError,
            validationSuggestion = getBasketValidationSuggestion(generalError),
            showDescription = false, // Already shown above
            showPresets = true
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
                modifier = Modifier.weight(1f)
            )
            
            CoffeePrimaryButton(
                text = stringResource(R.string.button_continue),
                onClick = onContinue,
                enabled = isValid || (coffeeInMin.isBlank() && coffeeInMax.isBlank() && 
                         coffeeOutMin.isBlank() && coffeeOutMax.isBlank()),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(spacing.large))
    }
}

@Composable
private fun getBasketValidationSuggestion(error: String?): String {
    if (error == null) return ""
    return when {
        error.contains("minimum must be less than maximum") -> 
            stringResource(R.string.suggestion_basket_min_less_than_max)
        error.contains("cannot be less than") -> 
            stringResource(R.string.suggestion_basket_check_minimum_values)
        error.contains("cannot exceed") -> 
            stringResource(R.string.suggestion_basket_check_maximum_values)
        error.contains("range must be at least") -> 
            stringResource(R.string.suggestion_basket_increase_range)
        error.contains("brew ratios") -> 
            stringResource(R.string.suggestion_basket_check_ratios)
        else -> ""
    }
}
