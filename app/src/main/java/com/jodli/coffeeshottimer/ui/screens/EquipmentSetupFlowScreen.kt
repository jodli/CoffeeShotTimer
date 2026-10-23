package com.jodli.coffeeshottimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.equipment.EquipmentSetupStep
import com.jodli.coffeeshottimer.ui.viewmodel.EquipmentSetupFlowViewModel

/**
 * Main screen for the multi-step equipment setup flow during onboarding.
 * Manages navigation between setup steps and displays progress.
 */
@Composable
fun EquipmentSetupFlowScreen(
    onComplete: () -> Unit,
    viewModel: EquipmentSetupFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.currentStep != EquipmentSetupStep.WELCOME) {
        viewModel.navigateBackward()
    }

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
                            stepSize = uiState.grinderStepSize,
                            minError = uiState.grinderMinError,
                            maxError = uiState.grinderMaxError,
                            stepSizeError = uiState.grinderStepSizeError,
                            generalError = uiState.grinderGeneralError,
                            isValid = uiState.isGrinderValid,
                            onScaleMinChange = viewModel::updateGrinderMin,
                            onScaleMaxChange = viewModel::updateGrinderMax,
                            onStepSizeChange = viewModel::updateGrinderStepSize,
                            onPresetSelected = viewModel::setGrinderPreset,
                            onStepSizePresetSelected = viewModel::setGrinderStepSizePreset,
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
                            grinderStepSize = uiState.grinderStepSize,
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
private fun EquipmentSetupProgress(
    currentStep: EquipmentSetupStep,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val currentStepIndex = EquipmentSetupStep.entries.indexOf(currentStep)
    val currentStepNumber = currentStepIndex + 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(
                R.string.onboarding_progress_step_title,
                currentStepNumber,
                totalSteps,
                getStepTitle(currentStep)
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index <= currentStepIndex) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
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
