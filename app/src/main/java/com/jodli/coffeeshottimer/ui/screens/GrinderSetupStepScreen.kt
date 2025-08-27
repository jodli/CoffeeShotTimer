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
import androidx.compose.ui.text.style.TextAlign
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.GrinderScaleSetup
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Screen for the grinder setup step in the equipment setup flow.
 * Allows users to configure their grinder's scale range.
 */
@Composable
fun GrinderSetupStepScreen(
    scaleMin: String,
    scaleMax: String,
    minError: String?,
    maxError: String?,
    generalError: String?,
    isValid: Boolean,
    onScaleMinChange: (String) -> Unit,
    onScaleMaxChange: (String) -> Unit,
    onPresetSelected: (Int, Int) -> Unit,
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
            text = stringResource(R.string.equipment_setup_grinder_step_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Description
        Text(
            text = stringResource(R.string.equipment_setup_grinder_step_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        // Grinder configuration card
        GrinderScaleSetup(
            scaleMin = scaleMin,
            scaleMax = scaleMax,
            onScaleMinChange = onScaleMinChange,
            onScaleMaxChange = onScaleMaxChange,
            onPresetSelected = onPresetSelected,
            minError = minError,
            maxError = maxError,
            generalError = generalError,
            validationSuggestion = getValidationSuggestion(generalError),
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
                enabled = isValid || (scaleMin.isBlank() && scaleMax.isBlank()),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(spacing.large))
    }
}

@Composable
private fun getValidationSuggestion(error: String?): String {
    if (error == null) return ""
    return when {
        error.contains("must be less than") -> stringResource(R.string.suggestion_increase_max_or_decrease_min)
        error.contains("cannot be negative") -> stringResource(R.string.suggestion_grinder_scales_start_at_zero_or_one)
        error.contains("cannot exceed 1000") -> stringResource(R.string.suggestion_most_grinder_scales_dont_go_above_100)
        error.contains("at least 3 steps") -> stringResource(R.string.suggestion_range_of_at_least_3_steps)
        error.contains("cannot exceed 100 steps") -> stringResource(R.string.suggestion_smaller_range_is_easier)
        else -> ""
    }
}
