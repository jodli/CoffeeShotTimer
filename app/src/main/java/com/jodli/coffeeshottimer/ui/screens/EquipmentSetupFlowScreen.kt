package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.EquipmentSetupFlowViewModel
import com.jodli.coffeeshottimer.ui.equipment.EquipmentSetupStep

/**
 * Main screen for the multi-step equipment setup flow during onboarding.
 * Manages navigation between setup steps and displays progress.
 */
@Composable
fun EquipmentSetupFlowScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: EquipmentSetupFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress indicator at the top
            if (uiState.currentStep != EquipmentSetupStep.WELCOME) {
                EquipmentSetupProgress(
                    currentStep = uiState.currentStep,
                    totalSteps = 4
                )
            }
            
            // Content based on current step
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (uiState.currentStep) {
                    EquipmentSetupStep.WELCOME -> {
                        EquipmentSetupWelcomeScreen(
                            onContinue = { viewModel.navigateForward() },
                            onSkip = {
                                viewModel.skipSetup(
                                    onSuccess = onComplete,
                                    onError = { /* Handle error */ }
                                )
                            }
                        )
                    }
                    
                    EquipmentSetupStep.GRINDER_SETUP -> {
                        GrinderSetupStepScreen(
                            scaleMin = uiState.grinderScaleMin,
                            scaleMax = uiState.grinderScaleMax,
                            minError = uiState.grinderMinError,
                            maxError = uiState.grinderMaxError,
                            generalError = uiState.grinderGeneralError,
                            isValid = uiState.isGrinderValid,
                            onScaleMinChange = viewModel::updateGrinderMin,
                            onScaleMaxChange = viewModel::updateGrinderMax,
                            onPresetSelected = viewModel::setGrinderPreset,
                            onBack = { viewModel.navigateBackward() },
                            onContinue = { viewModel.navigateForward() }
                        )
                    }
                    
                    EquipmentSetupStep.BASKET_SETUP -> {
                        BasketSetupStepScreen(
                            coffeeInMin = uiState.coffeeInMin,
                            coffeeInMax = uiState.coffeeInMax,
                            coffeeOutMin = uiState.coffeeOutMin,
                            coffeeOutMax = uiState.coffeeOutMax,
                            coffeeInMinError = uiState.coffeeInMinError,
                            coffeeInMaxError = uiState.coffeeInMaxError,
                            coffeeOutMinError = uiState.coffeeOutMinError,
                            coffeeOutMaxError = uiState.coffeeOutMaxError,
                            generalError = uiState.basketGeneralError,
                            isValid = uiState.isBasketValid,
                            onCoffeeInMinChange = viewModel::updateCoffeeInMin,
                            onCoffeeInMaxChange = viewModel::updateCoffeeInMax,
                            onCoffeeOutMinChange = viewModel::updateCoffeeOutMin,
                            onCoffeeOutMaxChange = viewModel::updateCoffeeOutMax,
                            onPresetSelected = viewModel::setBasketPreset,
                            onBack = { viewModel.navigateBackward() },
                            onContinue = { viewModel.navigateForward() }
                        )
                    }
                    
                    EquipmentSetupStep.SUMMARY -> {
                        EquipmentSetupSummaryScreen(
                            grinderMin = uiState.grinderScaleMin,
                            grinderMax = uiState.grinderScaleMax,
                            coffeeInMin = uiState.coffeeInMin,
                            coffeeInMax = uiState.coffeeInMax,
                            coffeeOutMin = uiState.coffeeOutMin,
                            coffeeOutMax = uiState.coffeeOutMax,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onBack = { viewModel.navigateBackward() },
                            onConfirm = {
                                viewModel.saveConfiguration(
                                    onSuccess = onComplete,
                                    onError = { /* Handle error */ }
                                )
                            },
                            onClearError = viewModel::clearError
                        )
                    }
                }
            }
        }
    }
}

/**
 * Progress indicator showing current step in the equipment setup
 */
@Composable
fun EquipmentSetupProgress(
    currentStep: EquipmentSetupStep,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val currentStepNumber = when (currentStep) {
        EquipmentSetupStep.WELCOME -> 1
        EquipmentSetupStep.GRINDER_SETUP -> 2
        EquipmentSetupStep.BASKET_SETUP -> 3
        EquipmentSetupStep.SUMMARY -> 4
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.onboarding_progress_step_title, currentStepNumber, totalSteps, getStepTitle(currentStep)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        LinearProgressIndicator(
            progress = currentStepNumber.toFloat() / totalSteps.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun getStepTitle(step: EquipmentSetupStep): String {
    return when (step) {
        EquipmentSetupStep.WELCOME -> stringResource(R.string.equipment_setup_step_welcome)
        EquipmentSetupStep.GRINDER_SETUP -> stringResource(R.string.equipment_setup_step_grinder)
        EquipmentSetupStep.BASKET_SETUP -> stringResource(R.string.equipment_setup_step_basket)
        EquipmentSetupStep.SUMMARY -> stringResource(R.string.equipment_setup_step_summary)
    }
}
