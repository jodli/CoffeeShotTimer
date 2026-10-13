package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.usecase.QualityTier
import com.jodli.coffeeshottimer.domain.usecase.ShotAnalysis
import com.jodli.coffeeshottimer.domain.usecase.ShotDetails
import com.jodli.coffeeshottimer.ui.components.ChartShot
import com.jodli.coffeeshottimer.ui.components.DataCell
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.RuledRow
import com.jodli.coffeeshottimer.ui.components.ScoreFigure
import com.jodli.coffeeshottimer.ui.components.SectionLabel
import com.jodli.coffeeshottimer.ui.components.ShotChart
import com.jodli.coffeeshottimer.ui.components.TasteFeedbackEditSheet
import com.jodli.coffeeshottimer.ui.components.TasteMark
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.components.labelRes
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.extractionZone
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.viewmodel.ShotDetailsViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val DIAL_IN_PATH_SHOT_LIMIT = 8
private val DIAL_IN_CHART_HEIGHT_PORTRAIT = 200.dp
private val DIAL_IN_CHART_HEIGHT_LANDSCAPE = 180.dp

/** Interpretation context, same thresholds the analysis uses. */
private const val OPTIMAL_MIN_TIME_SECONDS = ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN
private const val OPTIMAL_MAX_TIME_SECONDS = ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX
private const val CONCENTRATED_RATIO_THRESHOLD = ValidationUtils.MIN_TYPICAL_BREW_RATIO
private const val DILUTED_RATIO_THRESHOLD = ValidationUtils.OPTIMAL_BREW_RATIO_MAX

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotDetailsScreen(
    shotId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToShot: (String) -> Unit = {},
    viewModel: ShotDetailsViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val isLandscape = LocalIsLandscape.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTasteEditor by remember { mutableStateOf(false) }

    // Load shot details when screen is first displayed
    LaunchedEffect(shotId) {
        viewModel.loadShotDetails(shotId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar (hidden in landscape to save vertical space)
        if (!isLandscape) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_shot_details),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    // Navigation between shots; always present, enabled when a neighbour exists
                    IconButton(
                        onClick = {
                            viewModel.navigateToPreviousShot { id -> onNavigateToShot(id) }
                        },
                        enabled = uiState.shotDetails?.previousShot != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_previous_shot)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.navigateToNextShot { id -> onNavigateToShot(id) }
                        },
                        enabled = uiState.shotDetails?.nextShot != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_shot)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = uiState.shotDetails != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_shot),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } // End TopAppBar conditional

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        message = stringResource(R.string.loading_shot_details)
                    )
                }

                uiState.error != null -> {
                    ErrorState(
                        title = stringResource(R.string.error_loading_shot_details),
                        message = uiState.error ?: "Unknown error occurred",
                        onRetry = { viewModel.refreshShotDetails() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.shotDetails != null -> {
                    LandscapeContainer(
                        modifier = Modifier.fillMaxSize(),
                        portraitContent = {
                            ShotDetailsContent(
                                shotDetails = uiState.shotDetails!!,
                                onEditTaste = { showTasteEditor = true },
                                onNavigateToShot = onNavigateToShot,
                                recommendation = uiState.recommendation,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        landscapeContent = {
                            ShotDetailsTwoColumns(
                                shotDetails = uiState.shotDetails!!,
                                onEditTaste = { showTasteEditor = true },
                                onNavigateToShot = onNavigateToShot,
                                recommendation = uiState.recommendation,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.button_delete_shot)) },
            text = { Text(stringResource(R.string.format_delete_shot_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteShot {
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.text_bean_management_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.text_dialog_cancel))
                }
            }
        )
    }

    // Show error snackbar if needed
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Auto-clear error after showing
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    // Taste feedback editor sheet
    if (showTasteEditor && uiState.shotDetails != null) {
        TasteFeedbackEditSheet(
            currentTastePrimary = uiState.shotDetails!!.shot.tastePrimary,
            currentTasteSecondary = uiState.shotDetails!!.shot.tasteSecondary,
            extractionTimeSeconds = uiState.shotDetails!!.shot.extractionTimeSeconds,
            onSave = { primary, secondary ->
                viewModel.updateTasteFeedback(primary, secondary)
            },
            onDismiss = { showTasteEditor = false }
        )
    }
}

/** The full ledger column: verdict figures, dial-in path, next shot, breakdown, context. */
@Composable
private fun ShotDetailsContent(
    shotDetails: ShotDetails,
    onEditTaste: () -> Unit,
    onNavigateToShot: (String) -> Unit,
    recommendation: GrindAdjustmentRecommendation?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = LocalSpacing.current.medium)
    ) {
        item { HeaderSection(shotDetails) }
        item { FigureRows(shotDetails, onEditTaste) }
        item { DialInPathSection(shotDetails, DIAL_IN_CHART_HEIGHT_PORTRAIT) }
        item { NextShotSection(shotDetails, recommendation) }
        item { ScoreBreakdownSection(shotDetails.analysis) }
        if (shotDetails.relatedShotsCount > 1) {
            item { ComparedToUsualSection(shotDetails) }
        }
        item { BeanSection(shotDetails) }
        item { ContextSection(shotDetails, onNavigateToShot) }
    }
}

/** Landscape: verdict + path on the left, breakdown + context on the right. */
@Composable
private fun ShotDetailsTwoColumns(
    shotDetails: ShotDetails,
    onEditTaste: () -> Unit,
    onNavigateToShot: (String) -> Unit,
    recommendation: GrindAdjustmentRecommendation?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HeaderSection(shotDetails)
            FigureRows(shotDetails, onEditTaste)
            DialInPathSection(shotDetails, DIAL_IN_CHART_HEIGHT_LANDSCAPE)
            NextShotSection(shotDetails, recommendation)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = spacing.medium)
        ) {
            item { ScoreBreakdownSection(shotDetails.analysis) }
            if (shotDetails.relatedShotsCount > 1) {
                item { ComparedToUsualSection(shotDetails) }
            }
            item { BeanSection(shotDetails) }
            item { ContextSection(shotDetails, onNavigateToShot) }
        }
    }
}

@Composable
private fun HeaderSection(shotDetails: ShotDetails) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy · HH:mm")
    val index = shotDetails.beanShots.indexOfFirst { it.id == shotDetails.shot.id }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = shotDetails.bean.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (index > 0) {
            Text(
                text = buildString {
                    append(shotDetails.shot.timestamp.format(dateFormatter))
                    append(" · ")
                    append(
                        stringResource(
                            R.string.format_shot_position,
                            index + 1,
                            shotDetails.beanShots.size
                        )
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The verdict: time, ratio, score over in→out, grind, taste. */
@Composable
private fun FigureRows(
    shotDetails: ShotDetails,
    onEditTaste: () -> Unit
) {
    val shot = shotDetails.shot
    val zones = MaterialTheme.zoneColors
    Column(modifier = Modifier.fillMaxWidth()) {
        RuledRow {
            DataCell(label = stringResource(R.string.label_time), modifier = Modifier.weight(1f)) {
                Text(
                    text = shot.getFormattedExtractionTime(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = zones.ink(extractionZone(shot.extractionTimeSeconds)),
                    maxLines = 1
                )
            }
            DataCell(label = stringResource(R.string.label_ratio), modifier = Modifier.weight(1f)) {
                Text(
                    text = shot.getFormattedBrewRatio(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            DataCell(label = stringResource(R.string.label_score), modifier = Modifier.weight(1f)) {
                ScoreFigure(
                    score = shotDetails.analysis.qualityScore,
                    style = MaterialTheme.typography.headlineSmall,
                    swatch = 10.dp
                )
            }
        }
        RuledRow {
            DataCell(
                label = stringResource(R.string.label_coffee_in),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        R.string.format_weight_in_out,
                        shot.coffeeWeightIn.toInt(),
                        shot.coffeeWeightOut.toInt()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            DataCell(
                label = stringResource(R.string.label_grinder_setting),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = shot.grinderSetting.ifBlank {
                        stringResource(R.string.display_grinder_setting_empty)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            DataCell(
                label = stringResource(R.string.label_taste_feedback),
                modifier = Modifier.weight(1f),
                onClick = onEditTaste,
                clickLabel = stringResource(R.string.cd_edit_taste)
            ) {
                if (shot.tastePrimary != null) {
                    TasteMark(
                        taste = shot.tastePrimary!!,
                        secondary = shot.tasteSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = stringResource(R.string.text_no_taste_recorded),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Where this shot sits on the bean's dial-in run. */
@Composable
private fun DialInPathSection(shotDetails: ShotDetails, chartHeight: Dp) {
    val path = shotDetails.beanShots
        .filter { it.timestamp <= shotDetails.shot.timestamp }
        .takeLast(DIAL_IN_PATH_SHOT_LIMIT)
        .ifEmpty { listOf(shotDetails.shot) }
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.label_dial_in_path))
        ShotChart(
            shots = path.mapIndexed { i, shot ->
                ChartShot(
                    seconds = shot.extractionTimeSeconds,
                    ratio = shot.brewRatio,
                    number = i + 1
                )
            },
            connect = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        )
    }
}

/** The one next step: grind adjustment headline plus the interpretation. */
@Composable
private fun NextShotSection(
    shotDetails: ShotDetails,
    recommendation: GrindAdjustmentRecommendation?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.label_next_shot))
        if (recommendation != null) {
            val setting = recommendation.suggestedGrindSetting
            val finer = recommendation.adjustmentDirection == AdjustmentDirection.FINER
            val single = recommendation.adjustmentSteps == 1
            val headline = when {
                !recommendation.hasAdjustment() ->
                    stringResource(R.string.grind_adjustment_summary_no_change)
                finer && single -> stringResource(R.string.grind_adjustment_summary_finer_single, setting)
                finer -> stringResource(
                    R.string.grind_adjustment_summary_finer_multiple,
                    recommendation.adjustmentSteps,
                    setting
                )
                single -> stringResource(R.string.grind_adjustment_summary_coarser_single, setting)
                else -> stringResource(
                    R.string.grind_adjustment_summary_coarser_multiple,
                    recommendation.adjustmentSteps,
                    setting
                )
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = if (recommendation.hasAdjustment()) {
                    MaterialTheme.zoneColors.idealInk
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        InterpretationText(shot = shotDetails.shot, analysis = shotDetails.analysis)
    }
}

/** Transparent scoring: one quiet bar per category, no colour grading. */
@Composable
private fun ScoreBreakdownSection(analysis: ShotAnalysis) {
    val tier = QualityTier.forScore(analysis.qualityScore)
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(R.string.text_quality_score_breakdown),
            trailing = {
                Text(
                    text = stringResource(tier.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        ScoreBarRow(
            label = stringResource(R.string.label_taste_feedback),
            points = analysis.tastePoints,
            maxPoints = 30
        )
        ScoreBarRow(
            label = stringResource(R.string.label_extraction_time),
            points = analysis.extractionTimePoints,
            maxPoints = 25
        )
        ScoreBarRow(
            label = stringResource(R.string.label_brew_ratio),
            points = analysis.brewRatioPoints,
            maxPoints = 20
        )
        ScoreBarRow(
            label = stringResource(R.string.label_consistency),
            points = analysis.consistencyPoints,
            maxPoints = 15
        )
        ScoreBarRow(
            label = stringResource(R.string.label_precision),
            points = analysis.deviationBonusPoints,
            maxPoints = 10
        )
    }
}

/** Relative width of the breakdown bar against the label column. */
private const val SCORE_BAR_WEIGHT = 1.4f

@Composable
private fun ScoreBarRow(label: String, points: Int, maxPoints: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .weight(SCORE_BAR_WEIGHT)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(points.toFloat() / maxPoints)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
        Text(
            text = "$points/$maxPoints",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
    }
}

/** How this shot compares to the bean's usual result. */
@Composable
private fun ComparedToUsualSection(shotDetails: ShotDetails) {
    val analysis = shotDetails.analysis
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.label_compared_to_usual))
        if (analysis.avgExtractionTimeForBean > 0) {
            RuledRow {
                DataCell(
                    label = stringResource(R.string.label_extraction_time),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(
                            R.string.format_usual_time,
                            analysis.avgExtractionTimeForBean.roundToInt()
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                DataCell(
                    label = stringResource(R.string.label_brew_ratio),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(
                            R.string.format_usual_ratio,
                            "1:" + String.format(Locale.US, "%.1f", analysis.avgBrewRatioForBean)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** The bean this shot belongs to. */
@Composable
private fun BeanSection(shotDetails: ShotDetails) {
    val bean = shotDetails.bean
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.text_bean_information))
        Text(
            text = bean.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        RuledRow {
            DataCell(label = stringResource(R.string.text_roast_date), modifier = Modifier.weight(1f)) {
                Text(
                    text = bean.roastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            DataCell(
                label = stringResource(R.string.text_days_since_roast),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.format_days, shotDetails.daysSinceRoast),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (bean.isFresh()) {
                        MaterialTheme.zoneColors.idealInk
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
            }
        }
        if (bean.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bean.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Personal best, ranking, and the neighbouring shots. */
@Composable
private fun ContextSection(shotDetails: ShotDetails, onNavigateToShot: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.text_shot_context))
        if (shotDetails.isPersonalBest) {
            Text(
                text = stringResource(R.string.text_personal_best),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.zoneColors.idealInk
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        shotDetails.rankingForBean?.let { ranking ->
            Text(
                text = stringResource(
                    R.string.format_ranking_with_bean,
                    ranking,
                    shotDetails.relatedShotsCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NeighbourShotRow(
            shotDetails = shotDetails,
            isPrevious = true,
            onNavigateToShot = onNavigateToShot
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NeighbourShotRow(
            shotDetails = shotDetails,
            isPrevious = false,
            onNavigateToShot = onNavigateToShot
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NeighbourShotRow(
    shotDetails: ShotDetails,
    isPrevious: Boolean,
    onNavigateToShot: (String) -> Unit
) {
    val neighbour = if (isPrevious) shotDetails.previousShot else shotDetails.nextShot
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (neighbour != null) {
                    Modifier.clickable(
                        onClickLabel = neighbour.id,
                        onClick = { onNavigateToShot(neighbour.id) }
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                if (isPrevious) R.string.cd_previous_shot else R.string.cd_next_shot
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (neighbour != null) {
            Text(
                text = buildString {
                    append(neighbour.timestamp.format(dateFormatter))
                    append(" · ")
                    append(neighbour.getFormattedBrewRatio())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InterpretationText(
    shot: com.jodli.coffeeshottimer.data.model.Shot,
    analysis: ShotAnalysis,
    modifier: Modifier = Modifier
) {
    val parts = buildList {
        // Taste interpretation
        when (shot.tastePrimary) {
            com.jodli.coffeeshottimer.domain.model.TastePrimary.PERFECT -> {
                add(stringResource(R.string.interpretation_perfect_taste))
            }
            com.jodli.coffeeshottimer.domain.model.TastePrimary.SOUR -> {
                add(stringResource(R.string.interpretation_sour_taste))
            }
            com.jodli.coffeeshottimer.domain.model.TastePrimary.BITTER -> {
                add(stringResource(R.string.interpretation_bitter_taste))
            }
            null -> {
                add(stringResource(R.string.interpretation_no_taste))
            }
        }

        // Extraction time context
        if (!analysis.isOptimalExtraction) {
            if (shot.extractionTimeSeconds < OPTIMAL_MIN_TIME_SECONDS) {
                add(
                    stringResource(
                        R.string.interpretation_too_fast,
                        OPTIMAL_MIN_TIME_SECONDS - shot.extractionTimeSeconds
                    )
                )
            } else {
                add(
                    stringResource(
                        R.string.interpretation_too_slow,
                        shot.extractionTimeSeconds - OPTIMAL_MAX_TIME_SECONDS
                    )
                )
            }
        } else {
            add(
                stringResource(
                    R.string.interpretation_optimal_time,
                    shot.extractionTimeSeconds
                )
            )
        }

        // Ratio context
        if (!analysis.isTypicalRatio) {
            if (shot.brewRatio < CONCENTRATED_RATIO_THRESHOLD) {
                add(
                    stringResource(
                        R.string.interpretation_concentrated,
                        shot.getFormattedBrewRatio()
                    )
                )
            } else if (shot.brewRatio > DILUTED_RATIO_THRESHOLD) {
                add(
                    stringResource(
                        R.string.interpretation_diluted,
                        shot.getFormattedBrewRatio()
                    )
                )
            }
        }

        // Consistency note
        if (analysis.isConsistentWithHistory && analysis.avgExtractionTimeForBean > 0) {
            add(stringResource(R.string.interpretation_consistent))
        }
    }

    Text(
        text = parts.joinToString(" "),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 20.sp,
        modifier = modifier
    )
}
