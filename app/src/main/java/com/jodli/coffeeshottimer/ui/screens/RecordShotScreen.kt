package com.jodli.coffeeshottimer.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
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
 * TODO: Manual timer mode (future feature) - toggle button currently disabled
 * TODO: Separate landscape layout
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
    
    // Haptic feedback
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
                .padding(bottom = 80.dp) // Padding for bottom navigation bar
        ) {
            // Header: Bean + Grinder (70dp)
            HeaderSection(
                bean = selectedBean,
                grinderSetting = grinderSetting,
                hasPersistentRecommendation = persistentRecommendation != null,
                onBeanClick = onNavigateToBeanManagement,
                onGrinderClick = { showGrinderSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timer Section - Expandable with .weight(1f)
            TimerSection(
                isRunning = timerState.isRunning,
                elapsedTimeMs = timerState.elapsedTimeSeconds * 1000L,
                onTimerToggle = {
                    if (timerState.isRunning) {
                        // Pause - medium haptic feedback
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.pauseTimer()
                    } else {
                        // Start - strong haptic feedback (reject for maximum intensity)
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        viewModel.startTimer()
                    }
                },
                onTimerReset = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    viewModel.resetTimer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes all remaining space
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weights Section (50dp)
            WeightsDisplay(
                coffeeIn = coffeeWeightIn.toDoubleOrNull() ?: 0.0,
                coffeeOut = coffeeWeightOut.toDoubleOrNull() ?: 0.0,
                onCoffeeInClick = { showCoffeeInDialog = true },
                onCoffeeOutDecrease = {
                    val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                    val newValue = (current - 1).coerceAtLeast(basketCoffeeOutMin.toDouble())
                    viewModel.updateCoffeeWeightOut(newValue.toInt().toString())
                },
                onCoffeeOutIncrease = {
                    val current = coffeeWeightOut.toDoubleOrNull() ?: 0.0
                    val newValue = (current + 1).coerceAtMost(basketCoffeeOutMax.toDouble())
                    viewModel.updateCoffeeWeightOut(newValue.toInt().toString())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Shot Button (56dp)
            SaveShotButton(
                enabled = isFormValid,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.recordShot()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
    
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
            recommendations = data.recommendations,
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
            onGrindAdjustmentApply = {
                viewModel.applyGrindAdjustment()
            },
            onGrindAdjustmentDismiss = {
                viewModel.dismissGrindAdjustment()
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_adjust_grinder),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = grinderSetting.ifBlank { "--" },
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
                        Text("!")
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
    onTimerToggle: () -> Unit,
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
                    onToggle = onTimerToggle,
                    onReset = onTimerReset
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mode toggle button (disabled for now - future feature)
                // TODO: Implement manual timer mode in future update
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
                color = if (enabled) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
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
            
            // Recommendation suggestion (if available)
            if (persistentRecommendation != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = persistentRecommendation.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Suggested: ${persistentRecommendation.suggestedGrindSetting}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        FilledTonalButton(
                            onClick = {
                                onApplyRecommendation()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Apply")
                        }
                    }
                }
                
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
                onValueChange = { tempSetting = it },
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
                                    "too_high" -> stringResource(R.string.validation_coffee_in_too_high, maxValue.toInt())
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
