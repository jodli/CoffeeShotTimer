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
import com.jodli.coffeeshottimer.ui.components.BasketSetup
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.validation.BasketValidationHelpers
import com.jodli.coffeeshottimer.ui.viewmodel.BasketSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BasketSettingsViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Debug logging for UI state changes
    LaunchedEffect(uiState) {
        android.util.Log.d("BasketSettingsScreen", "UI State changed: isLoading=${uiState.isLoading}, isBasketValid=${uiState.isBasketValid}, error=${uiState.error}")
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.title_basket_settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                LoadingIndicator(message = stringResource(R.string.text_saving_configuration), modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    text = stringResource(R.string.text_basket_settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                BasketSetup(
                    coffeeInMin = uiState.coffeeInMin,
                    coffeeInMax = uiState.coffeeInMax,
                    coffeeOutMin = uiState.coffeeOutMin,
                    coffeeOutMax = uiState.coffeeOutMax,
                    onCoffeeInMinChange = viewModel::updateCoffeeInMin,
                    onCoffeeInMaxChange = viewModel::updateCoffeeInMax,
                    onCoffeeOutMinChange = viewModel::updateCoffeeOutMin,
                    onCoffeeOutMaxChange = viewModel::updateCoffeeOutMax,
                    onPresetSelected = viewModel::setBasketPreset,
                    coffeeInMinError = uiState.coffeeInMinError,
                    coffeeInMaxError = uiState.coffeeInMaxError,
                    coffeeOutMinError = uiState.coffeeOutMinError,
                    coffeeOutMaxError = uiState.coffeeOutMaxError,
                    generalError = uiState.basketGeneralError,
                    validationSuggestion = BasketValidationHelpers.getValidationSuggestion(uiState.basketGeneralError),
                    showDescription = false,
                    showPresets = true
                )

                CoffeePrimaryButton(
                    text = stringResource(id = R.string.button_save),
                    onClick = { 
                        android.util.Log.d("BasketSettingsScreen", "Save button clicked - isBasketValid=${uiState.isBasketValid}, isLoading=${uiState.isLoading}")
                        viewModel.saveConfiguration(
                            onSuccess = {
                                android.util.Log.d("BasketSettingsScreen", "Save successful, navigating back")
                                onNavigateBack()
                            },
                            onError = { error ->
                                android.util.Log.e("BasketSettingsScreen", "Save failed with error: $error")
                            }
                        )
                    },
                    enabled = uiState.isBasketValid && !uiState.isLoading,
                )
            }
        }
    }
}