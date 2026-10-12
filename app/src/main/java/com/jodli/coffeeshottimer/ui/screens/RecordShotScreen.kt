package com.jodli.coffeeshottimer.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
import com.jodli.coffeeshottimer.domain.usecase.TimerMode
import com.jodli.coffeeshottimer.domain.usecase.TimerState
import com.jodli.coffeeshottimer.ui.components.AutomaticTimerCircle
import com.jodli.coffeeshottimer.ui.components.ManualTimerCircle
import com.jodli.coffeeshottimer.ui.components.ShotRecordedDialog
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel

// Timer dial sizing: the dial takes most of its area, the numeral scales with it
private const val TIMER_SHARE_OF_AREA = 0.88f
private val TIMER_MIN_SIZE = 220.dp
private val TIMER_MAX_SIZE = 440.dp
private const val TIMER_NUMERAL_OF_SIZE = 0.17f
private const val TIMER_NUMERAL_MIN_SP = 44f
private const val TIMER_NUMERAL_MAX_SP = 80f

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
 * - Haptic feedback for timer controls
 * - Landscape orientation support with optimized layout
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
    val formattedBrewRatio by viewModel.formattedBrewRatio.collectAsState()
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

    // Timer mode state
    val timerMode by viewModel.timerMode.collectAsState()
    val manualTimeSeconds by viewModel.manualTimeSeconds.collectAsState()

    // UI state
    var showGrinderSheet by remember { mutableStateOf(false) }
    var showCoffeeInDialog by remember { mutableStateOf(false) }
    var showManualTimeDialog by remember { mutableStateOf(false) }

    // Refresh current bean when screen becomes visible
    // This ensures the selected bean is updated if changed in BeanManagementScreen
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentBean()
    }

    RecordShotScreenContent(
        selectedBean = selectedBean,
        grinderSetting = grinderSetting,
        suggestedGrindSetting = persistentRecommendation
            ?.takeIf { it.hasAdjustment() }
            ?.suggestedGrindSetting,
        timerMode = timerMode,
        timerState = timerState,
        manualTimeSeconds = manualTimeSeconds,
        coffeeWeightIn = coffeeWeightIn,
        coffeeWeightOut = coffeeWeightOut,
        formattedBrewRatio = formattedBrewRatio,
        isFormValid = isFormValid,
        onNavigateToBeanManagement = onNavigateToBeanManagement,
        onShowGrinderSheet = { showGrinderSheet = true },
        onShowCoffeeInDialog = { showCoffeeInDialog = true },
        onToggleTimerMode = { viewModel.toggleTimerMode() },
        onManualTimeChange = { newTime -> viewModel.setManualTime(newTime) },
        onShowManualTimeDialog = { showManualTimeDialog = true },
        onPauseTimer = { viewModel.pauseTimer() },
        onStartTimer = { viewModel.startTimer() },
        onResetTimer = { viewModel.resetTimer() },
        onStepCoffeeWeightOut = { grams -> viewModel.stepCoffeeWeightOut(grams) },
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

    // Manual time adjustment dialog
    if (showManualTimeDialog) {
        ManualTimeDialog(
            currentValue = manualTimeSeconds,
            onValueChange = { newValue ->
                viewModel.setManualTime(newValue)
            },
            onDismiss = { showManualTimeDialog = false }
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
 * The screen follows the shot: bean, grind and dose before it, the dial during it,
 * yield and save after it. Portrait stacks the sections; landscape puts the dial on the left.
 */
@Composable
private fun RecordShotScreenContent(
    selectedBean: Bean?,
    grinderSetting: String,
    suggestedGrindSetting: String?,
    timerMode: TimerMode,
    timerState: TimerState,
    manualTimeSeconds: Int,
    coffeeWeightIn: String,
    coffeeWeightOut: String,
    formattedBrewRatio: String?,
    isFormValid: Boolean,
    onNavigateToBeanManagement: () -> Unit,
    onShowGrinderSheet: () -> Unit,
    onShowCoffeeInDialog: () -> Unit,
    onToggleTimerMode: () -> Unit,
    onManualTimeChange: (Int) -> Unit,
    onShowManualTimeDialog: () -> Unit,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onStepCoffeeWeightOut: (Int) -> Unit,
    onRecordShot: () -> Unit
) {
    val view = LocalView.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    fun withHaptic(feedback: Int, action: () -> Unit): () -> Unit = {
        view.performHapticFeedback(feedback)
        action()
    }

    val beanHeader = @Composable {
        BeanHeader(bean = selectedBean, onClick = onNavigateToBeanManagement)
    }
    val recipeRow = @Composable {
        RecipeRow(
            grinderSetting = grinderSetting,
            suggestedGrindSetting = suggestedGrindSetting,
            coffeeWeightIn = coffeeWeightIn,
            onGrinderClick = onShowGrinderSheet,
            onCoffeeInClick = onShowCoffeeInDialog
        )
    }
    val timerSection = @Composable { modifier: Modifier ->
        TimerSection(
            timerMode = timerMode,
            isRunning = timerState.isRunning,
            elapsedTimeMs = timerState.elapsedTimeSeconds * 1000L,
            manualTimeSeconds = manualTimeSeconds,
            onToggleTimerMode = onToggleTimerMode,
            onManualTimeChange = onManualTimeChange,
            onShowManualTimeDialog = onShowManualTimeDialog,
            onPauseTimer = withHaptic(HapticFeedbackConstants.LONG_PRESS, onPauseTimer),
            onStartTimer = withHaptic(HapticFeedbackConstants.CONTEXT_CLICK, onStartTimer),
            onTimerReset = withHaptic(HapticFeedbackConstants.LONG_PRESS, onResetTimer),
            modifier = modifier
        )
    }
    val yieldRow = @Composable {
        YieldRow(
            coffeeWeightOut = coffeeWeightOut,
            formattedBrewRatio = formattedBrewRatio,
            onStep = onStepCoffeeWeightOut
        )
    }
    val saveButton = @Composable {
        SaveShotButton(
            enabled = isFormValid,
            onClick = withHaptic(HapticFeedbackConstants.CONFIRM, onRecordShot),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            timerSection(
                Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                beanHeader()
                recipeRow()
                yieldRow()
                saveButton()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            beanHeader()
            Spacer(modifier = Modifier.height(8.dp))
            recipeRow()
            timerSection(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            yieldRow()
            Spacer(modifier = Modifier.height(16.dp))
            saveButton()
        }
    }
}

/**
 * The bean in use, with its age. Tapping opens bean management to switch.
 */
@Composable
private fun BeanHeader(
    bean: Bean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bean?.name ?: stringResource(R.string.text_select_bean),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (bean != null) {
                val daysSinceRoast = bean.daysSinceRoast().toInt()
                Text(
                    text = stringResource(R.string.format_days_since_roast, daysSinceRoast),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Before the shot: grind and dose as two ruled cells. A pending grind suggestion shows
 * next to the current setting in the ideal-zone ink, the way the website plots the next shot.
 */
@Composable
private fun RecipeRow(
    grinderSetting: String,
    suggestedGrindSetting: String?,
    coffeeWeightIn: String,
    onGrinderClick: () -> Unit,
    onCoffeeInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inGrams = coffeeWeightIn.toDoubleOrNull()?.toInt() ?: 0
    RuledRow(modifier = modifier) {
        RecipeCell(
            label = stringResource(R.string.label_grinder_setting),
            onClick = onGrinderClick,
            clickLabel = stringResource(R.string.cd_adjust_grinder),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = grinderSetting.ifBlank { stringResource(R.string.display_grinder_setting_empty) },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (suggestedGrindSetting != null) {
                val suggestion = stringResource(R.string.text_suggested_grinder_setting, suggestedGrindSetting)
                Text(
                    text = "→ $suggestedGrindSetting",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.zoneColors.idealInk,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .semantics { contentDescription = suggestion }
                )
            }
        }
        VerticalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        RecipeCell(
            label = stringResource(R.string.label_coffee_in),
            onClick = onCoffeeInClick,
            clickLabel = stringResource(R.string.dialog_adjust_coffee_in),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.format_weight_grams, inGrams),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * After the shot: the yield read off the scale, adjusted in 1 g steps, with the ratio it gives.
 */
@Composable
private fun YieldRow(
    coffeeWeightOut: String,
    formattedBrewRatio: String?,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outGrams = coffeeWeightOut.toDoubleOrNull()?.toInt() ?: 0
    RuledRow(modifier = modifier) {
        RecipeCell(
            label = stringResource(R.string.label_coffee_out),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.format_weight_grams, outGrams),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        RecipeCell(
            label = stringResource(R.string.label_ratio),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formattedBrewRatio ?: stringResource(R.string.display_grinder_setting_empty),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StepButton(
            icon = Icons.Default.Remove,
            contentDescription = stringResource(R.string.cd_decrease_coffee_out),
            onClick = { onStep(-1) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        StepButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.cd_increase_coffee_out),
            onClick = { onStep(1) }
        )
    }
}

/** A row ruled top and bottom, like a line of the website's step table. */
@Composable
private fun RuledRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** A data label over a value. Editable cells get a pencil so they don't read as static figures. */
@Composable
private fun RecipeCell(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    value: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClickLabel = clickLabel, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, content = value)
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/**
 * The dial on bare paper, with the timer/manual switch above it.
 * The switch hides while the timer runs; its space stays reserved so the dial doesn't jump.
 */
@Composable
private fun TimerSection(
    timerMode: TimerMode,
    isRunning: Boolean,
    elapsedTimeMs: Long,
    manualTimeSeconds: Int,
    onToggleTimerMode: () -> Unit,
    onManualTimeChange: (Int) -> Unit,
    onShowManualTimeDialog: () -> Unit,
    onPauseTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onTimerReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isRunning) {
                TimerModeSwitch(timerMode = timerMode, onToggleTimerMode = onToggleTimerMode)
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val availableSize = minOf(maxWidth, maxHeight)
            val timerSize = (availableSize * TIMER_SHARE_OF_AREA).coerceIn(TIMER_MIN_SIZE, TIMER_MAX_SIZE)
            val fontSize = (timerSize.value * TIMER_NUMERAL_OF_SIZE)
                .coerceIn(TIMER_NUMERAL_MIN_SP, TIMER_NUMERAL_MAX_SP).sp

            when (timerMode) {
                TimerMode.AUTOMATIC -> {
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
                }
                TimerMode.MANUAL -> {
                    ManualTimerCircle(
                        size = timerSize,
                        fontSize = fontSize,
                        manualTimeSeconds = manualTimeSeconds,
                        onTimeChange = onManualTimeChange,
                        onTapToEdit = onShowManualTimeDialog
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerModeSwitch(
    timerMode: TimerMode,
    onToggleTimerMode: () -> Unit
) {
    val options = listOf(
        TimerMode.AUTOMATIC to stringResource(R.string.text_mode_timer),
        TimerMode.MANUAL to stringResource(R.string.text_mode_manual)
    )
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = timerMode == mode,
                onClick = { if (timerMode != mode) onToggleTimerMode() },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = MaterialTheme.shapes.small
                ),
                border = SegmentedButtonDefaults.borderStroke(MaterialTheme.colorScheme.outline),
                icon = {}
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Save shot button: ink when the shot can be saved, a quiet outline until then.
 */
@Composable
private fun SaveShotButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = stringResource(R.string.button_save_shot),
            style = MaterialTheme.typography.titleLarge
        )
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
        shape = MaterialTheme.shapes.medium
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
                shape = MaterialTheme.shapes.small,
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
                shape = MaterialTheme.shapes.medium
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

/**
 * Manual time input dialog for precise time entry.
 */
@Composable
private fun ManualTimeDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_adjust_extraction_time),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.text_enter_extraction_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        val value = it.toIntOrNull()
                        errorMessage = when {
                            value == null -> "invalid"
                            value < ValidationUtils.MANUAL_TIMER_MIN_SECONDS -> "too_low"
                            value > ValidationUtils.MANUAL_TIMER_MAX_SECONDS -> "too_high"
                            else -> null
                        }
                    },
                    label = { Text(stringResource(R.string.label_extraction_time)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        @Composable {
                            Text(
                                when (it) {
                                    "invalid" -> stringResource(R.string.validation_invalid_number)
                                    "too_low" -> stringResource(
                                        R.string.validation_time_too_low,
                                        ValidationUtils.MANUAL_TIMER_MIN_SECONDS
                                    )
                                    "too_high" -> stringResource(
                                        R.string.validation_time_too_high,
                                        ValidationUtils.MANUAL_TIMER_MAX_SECONDS
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
                    textValue.toIntOrNull()?.let { value ->
                        if (value in ValidationUtils.MANUAL_TIMER_MIN_SECONDS..ValidationUtils.MANUAL_TIMER_MAX_SECONDS) {
                            onValueChange(value)
                            onDismiss()
                        }
                    }
                },
                enabled = errorMessage == null && textValue.toIntOrNull() != null
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
