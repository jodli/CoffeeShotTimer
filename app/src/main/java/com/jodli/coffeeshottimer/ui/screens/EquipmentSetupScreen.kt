package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.GentleValidationMessage
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.OnboardingErrorCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.EquipmentSetupViewModel

/**
 * Equipment setup screen for configuring grinder scale settings during onboarding.
 * Allows users to set their grinder's minimum and maximum scale values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentSetupScreen(
    onComplete: (GrinderConfiguration) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EquipmentSetupViewModel = hiltViewModel(),
    isOnboardingMode: Boolean = false
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle error display
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar (only show in non-onboarding mode)
        if (!isOnboardingMode) {
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Show loading indicator when saving
            if (uiState.isLoading) {
                LoadingIndicator(
                    message = stringResource(R.string.text_saving_configuration),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Introduction text
                Text(
                    text = stringResource(R.string.text_equipment_setup_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Show error card if there's a general error
                uiState.error?.let { error ->
                    OnboardingErrorCard(
                        title = stringResource(R.string.text_equipment_setup_failed),
                        message = error,
                        onRetry = {
                            viewModel.retry(
                                onSuccess = onComplete,
                                onError = { error ->
                                    errorMessage = error
                                    showErrorDialog = true
                                }
                            )
                        },
                        onSkip = {
                            viewModel.skipSetup(
                                onSuccess = onComplete,
                                onError = { error ->
                                    errorMessage = error
                                    showErrorDialog = true
                                }
                            )
                        }
                    )
                } ?: run {
// Grinder scale setup card
com.jodli.coffeeshottimer.ui.components.GrinderScaleSetup(
                        scaleMin = uiState.scaleMin,
                        scaleMax = uiState.scaleMax,
                        onScaleMinChange = viewModel::updateScaleMin,
                        onScaleMaxChange = viewModel::updateScaleMax,
                        onPresetSelected = viewModel::setPreset,
                        minError = uiState.minError,
                        maxError = uiState.maxError,
                        generalError = uiState.generalError,
                        validationSuggestion = uiState.generalError?.let { viewModel.getValidationSuggestion(it) },
                        showDescription = true,
                        showPresets = true
                    )
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        CoffeeSecondaryButton(
                            text = if (isOnboardingMode) stringResource(R.string.button_back) else stringResource(R.string.button_skip),
                            onClick = if (isOnboardingMode) {
                                onBack
                            } else {
                                {
                                    viewModel.skipSetup(
                                        onSuccess = onComplete,
                                        onError = { error ->
                                            errorMessage = error
                                            showErrorDialog = true
                                        }
                                    )
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                        
                        CoffeePrimaryButton(
                            text = stringResource(R.string.button_continue),
                            onClick = {
                                viewModel.saveConfiguration(
                                    onSuccess = onComplete,
                                    onError = { error ->
                                        errorMessage = error
                                        showErrorDialog = true
                                    }
                                )
                            },
                            enabled = uiState.isFormValid && !uiState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Bottom padding for scrolling
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}

