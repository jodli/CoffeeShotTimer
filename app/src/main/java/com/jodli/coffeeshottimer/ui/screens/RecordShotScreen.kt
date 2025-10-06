package com.jodli.coffeeshottimer.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
import com.jodli.coffeeshottimer.domain.usecase.TimerState
import com.jodli.coffeeshottimer.ui.components.AutomaticTimerCircle
import com.jodli.coffeeshottimer.ui.components.ShotRecordedDialog
import com.jodli.coffeeshottimer.ui.components.WeightsDisplay
import com.jodli.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * RecordShotScreen - Production Implementation
 *
 * Intelligence-first design with massive timer and compact controls.
 *
 * Features:
 * - Color-coded timer (gray/orange/green/red based on extraction time)
 * - Automatic timer persistence across configuration changes
 * - Integration with ShotRecordingViewModel
 * - Bean-specific settings and recommendations
 * - Grinder adjustment with user configuration
 * - Coffee weight validation based on basket configuration
 *
 * Future enhancements:
 * - Manual timer mode (toggle button currently disabled)
 * - Separate landscape layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordShotScreen(
    onNavigateToBeanManagement: () -> Unit = {},
    onNavigateToShotDetails: (String) -> Unit = {},
    viewModel: ShotRecordingViewModel = hiltViewModel()
) {
    // ViewModel state
    val selectedBean by viewModel.selectedBean.collectAsState()
    val coffeeWeightIn by viewModel.coffeeWeightIn.collectAsState()
    val coffeeWeightOut by viewModel.coffeeWeightOut.collectAsState()
    val grinderSetting by viewModel.grinderSetting.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val showShotRecordedDialog by viewModel.showShotRecordedDialog.collectAsState()
    val recordedShotData by viewModel.recordedShotData.collectAsState()
    val grindAdjustment by viewModel.grindAdjustmentRecommendation.collectAsState()
    val persistentRecommendation by viewModel.persistentRecommendation.collectAsState()

    // Grinder configuration
    val grinderMin by viewModel.grinderScaleMin.collectAsState()
    val grinderMax by viewModel.grinderScaleMax.collectAsState()
    val grinderStep by viewModel.grinderStepSize.collectAsState()

    // Basket configuration
    val basketCoffeeInMin by viewModel.basketCoffeeInMin.collectAsState()
    val basketCoffeeInMax by viewModel.basketCoffeeInMax.collectAsState()
    val basketCoffeeOutMin by viewModel.basketCoffeeOutMin.collectAsState()
    val basketCoffeeOutMax by viewModel.basketCoffeeOutMax.collectAsState()

    // UI state
    var showGrinderSheet by remember { mutableStateOf(false) }
    var showCoffeeInDialog by remember { mutableStateOf(false) }

    RecordShotScreenContent(
        selectedBean = selectedBean,
        grinderSetting = grinderSetting,
        persistentRecommendation = persistentRecommendation,
        timerState = timerState,
        coffeeWeightIn = coffeeWeightIn,
        coffeeWeightOut = coffeeWeightOut,
        basketCoffeeOutMin = basketCoffeeOutMin,
        basketCoffeeOutMax = basketCoffeeOutMax,
        isFormValid = isFormValid,
        onNavigateToBeanManagement = onNavigateToBeanManagement,
        onShowGrinderSheet = { showGrinderSheet = true },
        onShowCoffeeInDialog = { showCoffeeInDialog = true },
        onPauseTimer = { viewModel.pauseTimer() },
        onStartTimer = { viewModel.startTimer() },
        onResetTimer = { viewModel.resetTimer() },
        onUpdateCoffeeWeightOut = { value -> viewModel.updateCoffeeWeightOut(value) },
        onRecordShot = { viewModel.recordShot() }
    )

    // Grinder adjustment bottom sheet
    if (showGrinderSheet) {
        GrinderAdjustmentBottomSheet(
            currentSetting = grinderSetting.toFloatOrNull() ?: grinderMin,
            minValue = grinderMin,
            maxValue = grinderMax,
            stepSize = grinderStep,
            persistentRecommendation = persistentRecommendation,
            onSettingChange = { newValue ->
                viewModel.updateGrinderSetting(newValue.toString())
            },
            onApplyRecommendation = {
                persistentRecommendation?.let {
                    viewModel.updateGrinderSetting(it.suggestedGrindSetting)
                    viewModel.dismissPersistentRecommendation()
                }
            },
            onDismiss = { showGrinderSheet = false }
        )
    }

    // Coffee In adjustment dialog
    if (showCoffeeInDialog) {
        CoffeeInDialog(
            currentValue = coffeeWeightIn.toDoubleOrNull() ?: basketCoffeeInMin.toDouble(),
            minValue = basketCoffeeInMin,
            maxValue = basketCoffeeInMax,
            onValueChange = { newValue ->
                viewModel.updateCoffeeWeightIn(newValue.toInt().toString())
            },
            onDismiss = { showCoffeeInDialog = false }
        )
    }

    // Shot recorded success dialog
    if (showShotRecordedDialog && recordedShotData != null) {
        val data = recordedShotData!!
        ShotRecordedDialog(
            brewRatio = data.brewRatio,
            extractionTime = data.extractionTime,
            suggestedTaste = data.suggestedTaste,
            grindAdjustment = grindAdjustment,
            onTasteSelected = { primary, secondary ->
                // Reactive UI updates - recalculate grind adjustment
                viewModel.calculateGrindAdjustmentForTaste(primary)
            },
            onSubmit = { primary, secondary ->
                // Save taste feedback to database
                if (primary != null) {
                    viewModel.recordTasteFeedback(data.shotId, primary, secondary)
                }
            },
            onDismiss = {
                viewModel.hideShotRecordedDialog()
            },
            onViewDetails = {
                onNavigateToShotDetails(data.shotId)
            }
        )
    }
}

/**
 * Main screen content for RecordShotScreen.
 */
@Composable
private fun RecordShotScreenContent(
    selectedBean: Bean?,
    grinderSetting: String,
    persistentRecommendation: PersistentGrindRecommendation?,
    timerState: TimerState,
    coffeeWeightIn: String,
    coffeeWeightOut: String,
    basketCoffeeOutMin: Float,
    basketCoffeeOutMax: Float,
    isFormValid: Boolean,
    onNavigateToBeanManagement: () -> Unit,
    onShowGrinderSheet: () -> Unit,
    onShowCoffeeInDialog: () -> Unit,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onUpdateCoffeeWeightOut: (String) -> Unit,
    onRecordShot: () -> Unit
) {
    val view = LocalView.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            HeaderSection(
                bean = selectedBean,
                grinderSetting = grinderSetting,
                hasPersistentRecommendation = persistentRecommendation?.hasAdjustment() == true,
                onBeanClick = onNavigateToBeanManagement,
                onGrinderClick = onShowGrinderSheet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TimerSection(
                isRunning = timerState.isRunning,
                elapsedTimeMs = timerState.elapsedTimeSeconds * 1000L,
                onPauseTimer = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onPauseTimer()
                },
                onStartTimer = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onStartTimer()
                },
                onTimerReset = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onResetTimer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeightsDisplay(
                coffeeIn = coffeeWeightIn.toDoubleOrNull() ?: 0.0,
                coffeeOut = coffeeWeightOut.toDoubleOrNull() ?: 0.0,
                onCoffeeInClick = onShowCoffeeInDialog,
                onCoffeeOutDecrease = {
                    val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                    val newValue = (current - 1).coerceAtLeast(basketCoffeeOutMin.toDouble())
                    onUpdateCoffeeWeightOut(newValue.toInt().toString())
                },
                onCoffeeOutIncrease = {
                    val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                    val newValue = (current + 1).coerceAtMost(basketCoffeeOutMax.toDouble())
                    onUpdateCoffeeWeightOut(newValue.toInt().toString())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SaveShotButton(
                enabled = isFormValid,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onRecordShot()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

/**
<<<<<<< HEAD
=======
 * Landscape layout for RecordShotScreen.
 * Horizontal layout with timer on the left and controls on the right.
 */
@Composable
private fun RecordShotScreenLandscape(
    selectedBean: Bean?,
    grinderSetting: String,
    persistentRecommendation: PersistentGrindRecommendation?,
    timerState: TimerState,
    coffeeWeightIn: String,
    coffeeWeightOut: String,
    basketCoffeeOutMin: Float,
    basketCoffeeOutMax: Float,
    isFormValid: Boolean,
    onNavigateToBeanManagement: () -> Unit,
    onShowGrinderSheet: () -> Unit,
    onShowCoffeeInDialog: () -> Unit,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onUpdateCoffeeWeightOut: (String) -> Unit,
    onRecordShot: () -> Unit,
    view: android.view.View
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left side: Timer section (takes up ~45% of width)
            Surface(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                // Use BoxWithConstraints to calculate optimal timer size
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Calculate timer size: 70% of the smaller dimension
                    // with a minimum of 180dp and maximum of 280dp for landscape
                    val availableSize = minOf(maxWidth, maxHeight)
                    val timerSize = (availableSize * 0.7f).coerceIn(180.dp, 280.dp)
                    val fontSize = (timerSize.value * 0.22f).coerceIn(44f, 70f).sp

                    AutomaticTimerCircle(
                        size = timerSize,
                        fontSize = fontSize,
                        isRunning = timerState.isRunning,
                        elapsedTimeMs = timerState.elapsedTimeSeconds * 1000L,
                        onToggle = {
                            if (timerState.isRunning) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onPauseTimer()
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onStartTimer()
                            }
                        },
                        onReset = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onResetTimer()
                        }
                    )
                }
            }

            // Right side: Scrollable form controls (takes up ~55% of width)
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with bean and grinder
                HeaderSection(
                    bean = selectedBean,
                    grinderSetting = grinderSetting,
                    hasPersistentRecommendation = persistentRecommendation?.hasAdjustment() == true,
                    onBeanClick = onNavigateToBeanManagement,
                    onGrinderClick = onShowGrinderSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                )

                // Weights display
                WeightsDisplay(
                    coffeeIn = coffeeWeightIn.toDoubleOrNull() ?: 0.0,
                    coffeeOut = coffeeWeightOut.toDoubleOrNull() ?: 0.0,
                    onCoffeeInClick = onShowCoffeeInDialog,
                    onCoffeeOutDecrease = {
                        val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                        val newValue = (current - 1).coerceAtLeast(basketCoffeeOutMin.toDouble())
                        onUpdateCoffeeWeightOut(newValue.toInt().toString())
                    },
                    onCoffeeOutIncrease = {
                        val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                        val newValue = (current + 1).coerceAtMost(basketCoffeeOutMax.toDouble())
                        onUpdateCoffeeWeightOut(newValue.toInt().toString())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                // Save button
                SaveShotButton(
                    enabled = isFormValid,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onRecordShot()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
    }
}

/**
>>>>>>> c03d903 (fixup! chore: fix detekt issues)
 * Header section with bean selector and grinder setting.
 */
@Composable
private fun HeaderSection(
    bean: Bean?,
    grinderSetting: String,
    hasPersistentRecommendation: Boolean,
    onBeanClick: () -> Unit,
    onGrinderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        onClick = onBeanClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Bean info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "☕",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = bean?.name ?: stringResource(R.string.text_select_bean),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_select_bean),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Days since roast
                if (bean != null) {
                    val daysSinceRoast = ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now()).toInt()
                    Text(
                        text = stringResource(R.string.format_days_since_roast, daysSinceRoast),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Right: Grinder setting with recommendation indicator
            Box {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    onClick = onGrinderClick
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Engineering,
                            contentDescription = stringResource(R.string.cd_adjust_grinder),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = grinderSetting.ifBlank { stringResource(R.string.display_grinder_setting_empty) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Recommendation indicator badge
                if (hasPersistentRecommendation) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(stringResource(R.string.badge_recommendation_alert))
                    }
                }
            }
        }
    }
}

/**
 * Timer section with automatic timer and disabled manual mode toggle.
 */
@Composable
private fun TimerSection(
    isRunning: Boolean,
    elapsedTimeMs: Long,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onTimerReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        // Use BoxWithConstraints to calculate maximum timer size
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Calculate timer size: 70% of the smaller dimension
            // with a minimum of 200dp and maximum of 400dp
            val availableSize = minOf(maxWidth, maxHeight)
            val timerSize = (availableSize * 0.7f).coerceIn(200.dp, 400.dp)
            val fontSize = (timerSize.value * 0.22f).coerceIn(48f, 96f).sp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Automatic timer circle
                AutomaticTimerCircle(
                    size = timerSize,
                    fontSize = fontSize,
                    isRunning = isRunning,
                    elapsedTimeMs = elapsedTimeMs,
                    onToggle = {
                        if (isRunning) {
                            onPauseTimer()
                        } else {
                            onStartTimer()
                        }
                    },
                    onReset = onTimerReset
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode toggle button (disabled for now - future feature)
                // Note: Manual timer mode will be implemented in a future update
                FilledIconButton(
                    onClick = { /* Disabled */ },
                    enabled = false,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_mode_toggle_disabled),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode description
                Text(
                    text = stringResource(R.string.text_auto_timer),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Save shot button.
 */
@Composable
private fun SaveShotButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (enabled) 2.dp else 0.dp,
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.button_save_shot),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
    }
}

/**
 * Recommendation card for grinder adjustment.
 */
@Composable
private fun GrinderRecommendationCard(
    recommendation: PersistentGrindRecommendation,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(
                        R.string.text_suggested_grinder_setting,
                        recommendation.suggestedGrindSetting
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            FilledTonalButton(
                onClick = onApply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.button_apply))
            }
        }
    }
}

/**
 * Grinder adjustment bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrinderAdjustmentBottomSheet(
    currentSetting: Float,
    minValue: Float,
    maxValue: Float,
    stepSize: Float,
    persistentRecommendation: PersistentGrindRecommendation?,
    onSettingChange: (Float) -> Unit,
    onApplyRecommendation: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var tempSetting by remember { mutableFloatStateOf(currentSetting) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dialog_adjust_grinder_setting),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.text_dialog_cancel)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recommendation suggestion (if available and has adjustment)
            if (persistentRecommendation?.hasAdjustment() == true) {
                GrinderRecommendationCard(
                    recommendation = persistentRecommendation,
                    onApply = {
                        onApplyRecommendation()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Current value display
            Text(
                text = String.format("%.1f", tempSetting),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Slider
            val steps = ((maxValue - minValue) / stepSize).toInt() - 1
            Slider(
                value = tempSetting,
                onValueChange = { newValue ->
                    // Round to 1 decimal place to avoid floating point errors
                    tempSetting = (newValue * 10).toInt() / 10f
                },
                valueRange = minValue..maxValue,
                steps = steps.coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )

            // Min/Max labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f", minValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f", maxValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Apply button
            Button(
                onClick = {
                    onSettingChange(tempSetting)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.button_apply_setting),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Coffee In adjustment dialog.
 */
@Composable
private fun CoffeeInDialog(
    currentValue: Double,
    minValue: Float,
    maxValue: Float,
    onValueChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toInt().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_adjust_coffee_in),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.text_enter_coffee_in_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        // Validate input (error messages set in supporting text)
                        val value = it.toDoubleOrNull()
                        errorMessage = when {
                            value == null -> "invalid"
                            value < minValue -> "too_low"
                            value > maxValue -> "too_high"
                            else -> null
                        }
                    },
                    label = { Text(stringResource(R.string.label_coffee_in)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        @Composable {
                            Text(
                                when (it) {
                                    "invalid" -> stringResource(R.string.validation_invalid_number)
                                    "too_low" -> stringResource(R.string.validation_coffee_in_too_low, minValue.toInt())
                                    "too_high" -> stringResource(
                                        R.string.validation_coffee_in_too_high,
                                        maxValue.toInt()
                                    )
                                    else -> it
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    textValue.toDoubleOrNull()?.let { value ->
                        if (value in minValue.toDouble()..maxValue.toDouble()) {
                            onValueChange(value)
                            onDismiss()
                        }
                    }
                },
                enabled = errorMessage == null && textValue.toDoubleOrNull() != null
            ) {
                Text(stringResource(R.string.button_apply_setting))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.text_dialog_cancel))
            }
        }
    )
}
