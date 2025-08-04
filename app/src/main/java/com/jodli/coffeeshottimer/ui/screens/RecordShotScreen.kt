package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanCard
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.DebugDialog
import com.jodli.coffeeshottimer.ui.components.DebugTapDetector
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.GrinderSettingSlider
import com.jodli.coffeeshottimer.ui.components.SectionHeader
import com.jodli.coffeeshottimer.ui.components.ShotRecordedDialog
import com.jodli.coffeeshottimer.ui.components.TimerControls
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.components.WeightSlidersSection
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.DebugViewModel
import com.jodli.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel

@Composable
fun RecordShotScreen(
    onNavigateToBeanManagement: () -> Unit = {},
    onNavigateToShotDetails: (String) -> Unit = {},
    viewModel: ShotRecordingViewModel = hiltViewModel(),
    debugViewModel: DebugViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()

    // State from ViewModel
    val activeBeans by viewModel.activeBeans.collectAsStateWithLifecycle()
    val selectedBean by viewModel.selectedBean.collectAsStateWithLifecycle()
    val coffeeWeightIn by viewModel.coffeeWeightIn.collectAsStateWithLifecycle()
    val coffeeWeightOut by viewModel.coffeeWeightOut.collectAsStateWithLifecycle()
    val grinderSetting by viewModel.grinderSetting.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val coffeeWeightInError by viewModel.coffeeWeightInError.collectAsStateWithLifecycle()
    val coffeeWeightOutError by viewModel.coffeeWeightOutError.collectAsStateWithLifecycle()
    val grinderSettingError by viewModel.grinderSettingError.collectAsStateWithLifecycle()
    val brewRatio by viewModel.brewRatio.collectAsStateWithLifecycle()
    val formattedBrewRatio by viewModel.formattedBrewRatio.collectAsStateWithLifecycle()
    val isOptimalBrewRatio by viewModel.isOptimalBrewRatio.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val showTimerValidation by viewModel.showTimerValidation.collectAsStateWithLifecycle()
    val showShotRecordedDialog by viewModel.showShotRecordedDialog.collectAsStateWithLifecycle()
    val recordedShotData by viewModel.recordedShotData.collectAsStateWithLifecycle()

    val suggestedGrinderSetting by viewModel.suggestedGrinderSetting.collectAsStateWithLifecycle()
    val previousSuccessfulSettings by viewModel.previousSuccessfulSettings.collectAsStateWithLifecycle()

    // Debug state (only in debug builds)
    val debugUiState by debugViewModel.uiState.collectAsStateWithLifecycle()

    // Local UI state
    var showBeanSelector by remember { mutableStateOf(false) }

    // Convert timer state for UI
    val uiTimerState = when {
        timerState.isRunning -> TimerState.RUNNING
        timerState.elapsedTimeSeconds > 0 -> TimerState.PAUSED
        else -> TimerState.STOPPED
    }
    val currentTime = (timerState.elapsedTimeSeconds * 1000).toLong()
    val targetTime: Long? = null // Can be set for target extraction time if needed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Header with debug tap detection (only in debug builds)
        if (BuildConfig.DEBUG) {
            DebugTapDetector(
                onDebugActivated = { debugViewModel.showDialog() }
            ) {
                Text(
                    text = stringResource(R.string.title_new_shot),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = stringResource(R.string.title_new_shot),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Bean Selection with navigation to bean management
        BeanSelectionCard(
            selectedBean = selectedBean,
            onBeanSelect = { showBeanSelector = true },
            onManageBeans = onNavigateToBeanManagement,
            modifier = Modifier.fillMaxWidth()
        )

        // Timer Section
        TimerSection(
            currentTime = currentTime,
            targetTime = targetTime,
            timerState = uiTimerState,
            showTimerValidation = showTimerValidation,
            onStartPause = {
                if (timerState.isRunning) {
                    viewModel.pauseTimer()
                } else {
                    viewModel.startTimer()
                }
            },
            onReset = {
                viewModel.resetTimer()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Weight Inputs with Sliders
        WeightSlidersSection(
            coffeeWeightIn = coffeeWeightIn,
            onCoffeeWeightInChange = viewModel::updateCoffeeWeightIn,
            coffeeWeightInError = coffeeWeightInError,
            coffeeWeightOut = coffeeWeightOut,
            onCoffeeWeightOutChange = viewModel::updateCoffeeWeightOut,
            coffeeWeightOutError = coffeeWeightOutError,
            modifier = Modifier.fillMaxWidth()
        )

        // Brew Ratio Display
        BrewRatioCard(
            brewRatio = brewRatio,
            formattedBrewRatio = formattedBrewRatio,
            isOptimal = isOptimalBrewRatio,
            modifier = Modifier.fillMaxWidth()
        )

        // Grinder Setting with suggestions
        GrinderSettingSection(
            grinderSetting = grinderSetting,
            onGrinderSettingChange = viewModel::updateGrinderSetting,
            grinderSettingError = grinderSettingError,
            suggestedSetting = suggestedGrinderSetting,
            previousSuccessfulSettings = previousSuccessfulSettings,
            onUseSuggestion = { suggestion ->
                viewModel.updateGrinderSetting(suggestion)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Notes (Optional)
        NotesSection(
            notes = notes,
            onNotesChange = viewModel::updateNotes,
            modifier = Modifier.fillMaxWidth()
        )

        // Save Button
        SaveShotButton(
            enabled = isFormValid,
            isLoading = isLoading,
            onClick = {
                viewModel.recordShot()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Success message
        successMessage?.let { success ->
            CoffeeCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = success,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = { viewModel.clearSuccessMessage() }
                    ) {
                        Text(
                            text = stringResource(R.string.button_dismiss),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            ErrorCard(
                title = stringResource(R.string.error_recording_error),
                message = error,
                onDismiss = { viewModel.clearErrorMessage() },
                onRetry = { viewModel.recordShot() }
            )
        }



        // Bottom spacing for navigation bar
        Spacer(modifier = Modifier.height(spacing.large))
    }

    // Shot Recorded Dialog with Recommendations
    if (showShotRecordedDialog && recordedShotData != null) {
        ShotRecordedDialog(
            brewRatio = recordedShotData!!.brewRatio,
            extractionTime = recordedShotData!!.extractionTime,
            recommendations = recordedShotData!!.recommendations,
            onDismiss = { viewModel.hideShotRecordedDialog() },
            onViewDetails = {
                onNavigateToShotDetails(recordedShotData!!.shotId)
            }
        )
    }

    // Debug Dialog (only in debug builds)
    if (BuildConfig.DEBUG) {
        DebugDialog(
            isVisible = debugUiState.isDialogVisible,
            onDismiss = { debugViewModel.hideDialog() },
            onFillDatabase = { debugViewModel.fillDatabase() },
            onAddMoreShots = { debugViewModel.addMoreShots() },
            onClearDatabase = { debugViewModel.clearDatabase() },
            isLoading = debugUiState.isLoading,
            operationResult = debugUiState.operationResult,
            showConfirmation = debugUiState.showConfirmation,
            onShowConfirmation = { debugViewModel.showConfirmation() },
            onHideConfirmation = { debugViewModel.hideConfirmation() }
        )
    }

    // Bean Selection Bottom Sheet
    if (showBeanSelector) {
        BeanSelectorBottomSheet(
            beans = activeBeans,
            selectedBean = selectedBean,
            onBeanSelected = { bean ->
                viewModel.selectBean(bean)
                showBeanSelector = false
            },
            onManageBeans = {
                showBeanSelector = false
                onNavigateToBeanManagement()
            },
            onDismiss = { showBeanSelector = false }
        )
    }
}

@Composable
private fun BeanSelectionCard(
    selectedBean: Bean?,
    onBeanSelect: () -> Unit,
    onManageBeans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        onClick = onBeanSelect,
        modifier = modifier
    ) {
        CardHeader(
            icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
            title = stringResource(R.string.text_selected_bean),
            actions = {
                TextButton(
                    onClick = onManageBeans,
                    contentPadding = PaddingValues(horizontal = spacing.small, vertical = spacing.extraSmall / 2)
                ) {
                    Text(
                        text = stringResource(R.string.button_manage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedBean?.name ?: stringResource(R.string.text_tap_to_select_bean),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedBean != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedBean != null) FontWeight.Medium else FontWeight.Normal
                )

                selectedBean?.let { bean ->
                    val daysSinceRoast = bean.daysSinceRoast()
                    Text(
                        text = stringResource(R.string.format_days_since_roast, daysSinceRoast),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bean.isFresh())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_select_bean),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerSection(
    currentTime: Long,
    targetTime: Long?,
    timerState: TimerState,
    showTimerValidation: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.text_extraction_timer)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Enhanced clickable timer - entire timer is now the start/stop button!
        // This dramatically improves usability with a ~200dp touch target vs 80dp
        TimerControls(
            isRunning = timerState == TimerState.RUNNING,
            onStartPause = onStartPause,
            onReset = onReset,
            currentTime = currentTime,
            targetTime = targetTime,
            showReset = currentTime > 0L,
            useClickableTimer = true, // Use the new clickable timer approach
            showColorCoding = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun BrewRatioCard(
    brewRatio: Double?,
    formattedBrewRatio: String?,
    isOptimal: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_brew_ratio),
            actions = {
                Text(
                    text = formattedBrewRatio ?: "--",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOptimal)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        brewRatio?.let {
            val status = when {
                it < 1.5 -> stringResource(R.string.text_strong_extraction)
                it > 3.0 -> stringResource(R.string.text_weak_extraction)
                else -> stringResource(R.string.text_typical_extraction)
            }

            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GrinderSettingSection(
    grinderSetting: String,
    onGrinderSettingChange: (String) -> Unit,
    grinderSettingError: String?,
    suggestedSetting: String?,
    previousSuccessfulSettings: List<String>,
    onUseSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        // Show suggestion button if available and different from current setting
        if (suggestedSetting != null && suggestedSetting != grinderSetting && grinderSetting.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onUseSuggestion(suggestedSetting) },
                    contentPadding = PaddingValues(horizontal = spacing.small, vertical = spacing.extraSmall / 2)
                ) {
                    Text(
                        text = stringResource(R.string.format_use_suggested, suggestedSetting),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.small))
        }

        // Grinder Setting Slider
        GrinderSettingSlider(
            value = grinderSetting,
            onValueChange = onGrinderSettingChange,
            errorMessage = grinderSettingError,
            suggestedSetting = suggestedSetting,
            previousSuccessfulSettings = previousSuccessfulSettings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.text_notes_optional)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.label_notes)) },
            placeholder = { Text(stringResource(R.string.placeholder_notes)) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SaveShotButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoffeePrimaryButton(
        text = if (isLoading) stringResource(R.string.cd_saving) else stringResource(R.string.cd_save_shot),
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeanSelectorBottomSheet(
    beans: List<Bean>,
    selectedBean: Bean?,
    onBeanSelected: (Bean) -> Unit,
    onManageBeans: () -> Unit,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_select_bean),
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(
                    onClick = onManageBeans
                ) {
                    Text(
                        text = stringResource(R.string.text_manage_beans),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            if (beans.isEmpty()) {
                EmptyState(
                    icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                    title = stringResource(R.string.text_no_beans_available),
                    description = stringResource(R.string.text_add_some_beans),
                    modifier = Modifier.padding(spacing.large)
                )
            } else {
                beans.forEach { bean ->
                    BeanCard(
                        bean = bean,
                        onEdit = { /* Not needed in selector */ },
                        onSelect = { onBeanSelected(bean) },
                        isSelected = bean.id == selectedBean?.id,
                        showActions = false,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}


enum class TimerState {
    STOPPED, RUNNING, PAUSED
}