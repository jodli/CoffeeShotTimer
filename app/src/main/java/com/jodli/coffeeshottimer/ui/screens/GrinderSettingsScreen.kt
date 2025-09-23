package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.validation.GrinderValidationHelpers
import com.jodli.coffeeshottimer.ui.viewmodel.EquipmentSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrinderSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EquipmentSettingsViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    // Debug logging for UI state changes
    LaunchedEffect(uiState) {
        android.util.Log.d(
            "GrinderSettingsScreen",
            "UI State changed: isLoading=${uiState.isLoading}, isFormValid=${uiState.isFormValid}, generalError=${uiState.generalError}"
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_equipment_settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding, vertical = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(
                    message = stringResource(R.string.text_saving_configuration),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.text_equipment_settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                com.jodli.coffeeshottimer.ui.components.GrinderScaleSetup(
                    scaleMin = uiState.scaleMin,
                    scaleMax = uiState.scaleMax,
                    stepSize = uiState.stepSize,
                    onScaleMinChange = viewModel::updateScaleMin,
                    onScaleMaxChange = viewModel::updateScaleMax,
                    onStepSizeChange = viewModel::updateStepSize,
                    onPresetSelected = viewModel::setPreset,
                    onStepSizePresetSelected = viewModel::setStepSizePreset,
                    minError = uiState.minError,
                    maxError = uiState.maxError,
                    stepSizeError = uiState.stepSizeError,
                    generalError = uiState.generalError,
                    validationSuggestion = GrinderValidationHelpers.getValidationSuggestion(uiState.generalError),
                    showDescription = true,
                    showPresets = true,
                    showStepSize = true
                )

                CoffeePrimaryButton(
                    text = stringResource(id = R.string.button_save),
                    onClick = {
                        android.util.Log.d(
                            "GrinderSettingsScreen",
                            "Save button clicked - isFormValid=${uiState.isFormValid}, isLoading=${uiState.isLoading}"
                        )
                        viewModel.save(
                            onSuccess = {
                                android.util.Log.d("GrinderSettingsScreen", "Save successful, navigating back")
                                onNavigateBack()
                            }
                        )
                    },
                    enabled = uiState.isFormValid && !uiState.isLoading,
                )
            }
        }
    }
}
