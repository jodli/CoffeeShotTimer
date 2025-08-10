package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Equipment setup screen for configuring grinder scale settings during onboarding.
 * Allows users to set their grinder's minimum and maximum scale values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentSetupScreen(
    onComplete: (GrinderConfiguration) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    // Form state
    var scaleMin by remember { mutableStateOf("") }
    var scaleMax by remember { mutableStateOf("") }
    var minError by remember { mutableStateOf<String?>(null) }
    var maxError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    
    // Validate form and create configuration
    fun validateAndCreateConfig(): GrinderConfiguration? {
        // Clear previous errors
        minError = null
        maxError = null
        generalError = null
        
        val minValue = scaleMin.toIntOrNull()
        val maxValue = scaleMax.toIntOrNull()
        
        // Individual field validation
        if (scaleMin.isNotBlank() && minValue == null) {
            minError = "Please enter a valid number"
        }
        if (scaleMax.isNotBlank() && maxValue == null) {
            maxError = "Please enter a valid number"
        }
        
        // If both values are valid, validate the configuration
        if (minValue != null && maxValue != null) {
            val config = GrinderConfiguration(scaleMin = minValue, scaleMax = maxValue)
            val validation = config.validate()
            if (!validation.isValid) {
                generalError = validation.errors.firstOrNull()
            } else {
                return config
            }
        }
        
        return null
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.title_equipment_setup),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Introduction text
            Text(
                text = stringResource(R.string.text_equipment_setup_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Grinder scale setup card
            GrinderScaleSetup(
                scaleMin = scaleMin,
                scaleMax = scaleMax,
                onScaleMinChange = { 
                    scaleMin = it
                    minError = null // Clear error on input
                    generalError = null
                },
                onScaleMaxChange = { 
                    scaleMax = it
                    maxError = null // Clear error on input
                    generalError = null
                },
                minError = minError,
                maxError = maxError,
                generalError = generalError
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.button_skip),
                    onClick = {
                        // Use default configuration when skipping
                        onComplete(GrinderConfiguration.DEFAULT_CONFIGURATION)
                    },
                    modifier = Modifier.weight(1f)
                )
                
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_continue),
                    onClick = {
                        val config = validateAndCreateConfig()
                        if (config != null) {
                            onComplete(config)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Bottom padding for scrolling
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}

/**
 * Grinder scale setup component with min/max input fields and preset options.
 */
@Composable
private fun GrinderScaleSetup(
    scaleMin: String,
    scaleMax: String,
    onScaleMinChange: (String) -> Unit,
    onScaleMaxChange: (String) -> Unit,
    minError: String?,
    maxError: String?,
    generalError: String?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.text_grinder_scale_range)
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        Text(
            text = stringResource(R.string.text_grinder_scale_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Common presets section
        Text(
            text = stringResource(R.string.text_common_presets),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        // Preset buttons in a more compact layout
        val presets = listOf(
            Pair(1, 10),
            Pair(30, 80),
            Pair(50, 60),
            Pair(0, 100)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            presets.take(2).forEach { (min, max) ->
                CoffeeSecondaryButton(
                    text = "$min-$max",
                    onClick = {
                        onScaleMinChange(min.toString())
                        onScaleMaxChange(max.toString())
                    },
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
                    onClick = {
                        onScaleMinChange(min.toString())
                        onScaleMaxChange(max.toString())
                    },
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = false
                )
            }
        }
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        // Custom range inputs section
        Text(
            text = stringResource(R.string.text_custom_range),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.Top
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
            
            Text(
                text = "to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.medium)
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
        
        // General validation error (for range validation)
        if (generalError != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            
            Text(
                text = generalError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Provide helpful suggestion
            val suggestion = getValidationSuggestion(generalError)
            if (suggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (scaleMin.isNotBlank() && scaleMax.isNotBlank()) {
            // Show positive feedback when range is valid
            val minValue = scaleMin.toIntOrNull()
            val maxValue = scaleMax.toIntOrNull()
            
            if (minValue != null && maxValue != null && minValue < maxValue) {
                Spacer(modifier = Modifier.height(spacing.small))
                
                Text(
                    text = stringResource(R.string.text_range_validation_success, maxValue - minValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}



/**
 * Provides helpful suggestions for validation errors.
 */
private fun getValidationSuggestion(error: String): String {
    return when {
        error.contains("must be less than") -> "Try increasing the maximum value or decreasing the minimum value."
        error.contains("cannot be negative") -> "Grinder scales typically start at 0 or 1."
        error.contains("cannot exceed 1000") -> "Most grinder scales don't go above 100."
        error.contains("at least 3 steps") -> "A range of at least 3 steps gives you room to adjust your grind."
        error.contains("cannot exceed 100 steps") -> "A smaller range is usually easier to work with."
        error.contains("valid number") -> "Please enter whole numbers only (e.g., 1, 10, 50)."
        else -> ""
    }
}

@Preview(showBackground = true)
@Composable
private fun EquipmentSetupScreenPreview() {
    CoffeeShotTimerTheme {
        EquipmentSetupScreen(
            onComplete = { },
            onBack = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GrinderScaleSetupPreview() {
    CoffeeShotTimerTheme {
        GrinderScaleSetup(
            scaleMin = "1",
            scaleMax = "10",
            onScaleMinChange = { },
            onScaleMaxChange = { },
            minError = null,
            maxError = null,
            generalError = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GrinderScaleSetupWithErrorsPreview() {
    CoffeeShotTimerTheme {
        GrinderScaleSetup(
            scaleMin = "10",
            scaleMax = "5",
            onScaleMinChange = { },
            onScaleMaxChange = { },
            minError = null,
            maxError = null,
            generalError = "Minimum scale value must be less than maximum scale value",
            modifier = Modifier.padding(16.dp)
        )
    }
}