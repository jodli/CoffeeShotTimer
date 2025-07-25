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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanCard
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.DebugDialog
import com.jodli.coffeeshottimer.ui.components.DebugTapDetector
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.GrinderSettingSlider
import com.jodli.coffeeshottimer.ui.components.SectionHeader
import com.jodli.coffeeshottimer.ui.components.TimerControls
import com.jodli.coffeeshottimer.ui.components.WeightSlidersSection
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.DebugViewModel
import com.jodli.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel

@Composable
fun RecordShotScreen(
    onNavigateToBeanManagement: () -> Unit = {},
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
    val isDraftSaved by viewModel.isDraftSaved.collectAsStateWithLifecycle()
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
                SectionHeader(
                    title = "Record Shot",
                    subtitle = "Track your espresso extraction"
                )
            }
        } else {
            SectionHeader(
                title = "Record Shot",
                subtitle = "Track your espresso extraction"
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
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
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(spacing.medium)
                )
            }
        }

        // Draft status indicator
        if (isDraftSaved) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Draft saved automatically",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(spacing.small)
                )
            }
        }

        // Bottom spacing for navigation bar
        Spacer(modifier = Modifier.height(spacing.large))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Bean",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onManageBeans,
                        contentPadding = PaddingValues(horizontal = spacing.small, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Manage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = selectedBean?.name ?: "Tap to select bean",
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
                        text = "$daysSinceRoast days since roast",
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
                contentDescription = "Select bean",
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
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Text(
                text = "Extraction Timer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brew Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formattedBrewRatio ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOptimal)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }

        brewRatio?.let {
            val status = when {
                it < 1.5 -> "Strong extraction"
                it > 3.0 -> "Weak extraction"
                else -> "Typical espresso range"
            }

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
                    contentPadding = PaddingValues(horizontal = spacing.small, vertical = 2.dp)
                ) {
                    Text(
                        text = "Use Suggested: $suggestedSetting",
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
        Text(
            text = "Notes (Optional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            placeholder = { Text("Tasting notes, adjustments, etc.") },
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
        text = if (isLoading) "Saving..." else "Save Shot",
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
                    text = "Select Bean",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(
                    onClick = onManageBeans
                ) {
                    Text(
                        text = "Manage Beans",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            if (beans.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Home,
                    title = "No Beans Available",
                    description = "Add some beans in Bean Management to get started",
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