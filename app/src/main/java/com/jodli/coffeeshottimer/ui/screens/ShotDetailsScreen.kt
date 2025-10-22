package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.ShotDetails
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.GrindAdjustmentCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.TasteFeedbackDisplay
import com.jodli.coffeeshottimer.ui.components.TasteFeedbackEditSheet
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.ShotDetailsViewModel
import java.time.format.DateTimeFormatter

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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
            },
            actions = {
                // Navigation between shots
                uiState.shotDetails?.let { details ->
                    if (details.previousShot != null) {
                        IconButton(
                            onClick = {
                                viewModel.navigateToPreviousShot { shotId ->
                                    onNavigateToShot(shotId)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.cd_previous_shot)
                            )
                        }
                    }

                    if (details.nextShot != null) {
                        IconButton(
                            onClick = {
                                viewModel.navigateToNextShot { shotId ->
                                    onNavigateToShot(shotId)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.cd_next_shot)
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_shot),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
        }  // End TopAppBar conditional

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
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        landscapeContent = {
                            ShotDetailsLandscapeContent(
                                shotDetails = uiState.shotDetails!!,
                                onEditTaste = { showTasteEditor = true },
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

@Composable
private fun ShotDetailsContent(
    shotDetails: ShotDetails,
    onEditTaste: () -> Unit,
    viewModel: ShotDetailsViewModel,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium)
    ) {
        // 1. Shot Overview Card
        item {
            ShotOverviewCard(
                shotDetails = shotDetails,
                onEditTaste = onEditTaste
            )
        }

        // Note: Removed "What To Do Next" card - doesn't make sense for historical shots
        // Users can see recommendations on the RecordShotScreen after recording

        // 3. Quality Score Breakdown (new - transparent scoring)
        item {
            QualityScoreBreakdownCard(
                analysis = shotDetails.analysis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. What Happened & Why (merged parameters + analysis)
        item {
            WhatHappenedCard(
                shotDetails = shotDetails,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Comparison Context (new - ranking)
        if (shotDetails.rankingForBean != null) {
            item {
                ComparisonContextCard(
                    shotDetails = shotDetails,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 6. Bean Information
        item {
            BeanInformationCard(shotDetails = shotDetails)
        }

        // Context Card (Previous/Next shots)
        if (shotDetails.previousShot != null || shotDetails.nextShot != null) {
            item {
                ShotContextCard(shotDetails = shotDetails)
            }
        }
    }
}

@Composable
private fun ShotOverviewCard(
    shotDetails: ShotDetails,
    onEditTaste: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val shot = shotDetails.shot
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm")

    CoffeeCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.medium)
                ) {
                    Text(
                        text = shot.timestamp.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Large brew ratio display
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = shot.getFormattedBrewRatio(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (shot.isTypicalBrewRatio()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = stringResource(R.string.text_brew_ratio),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // Taste feedback display
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.text_taste_feedback),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TasteFeedbackDisplay(
                        tastePrimary = shot.tastePrimary,
                        tasteSecondary = shot.tasteSecondary,
                        onEditClick = onEditTaste
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // Quality score - moved outside columns for full width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_quality_score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.format_quality_score, shotDetails.analysis.qualityScore),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    color = when {
                        shotDetails.analysis.qualityScore >= 80 -> MaterialTheme.colorScheme.primary
                        shotDetails.analysis.qualityScore >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Key metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricDisplay(
                    label = stringResource(R.string.label_input),
                    value = stringResource(R.string.format_weight_display_g, shot.coffeeWeightIn.toInt()),
                    modifier = Modifier.weight(1f)
                )
                MetricDisplay(
                    label = stringResource(R.string.label_output),
                    value = stringResource(R.string.format_weight_display_g, shot.coffeeWeightOut.toInt()),
                    modifier = Modifier.weight(1f)
                )
                MetricDisplay(
                    label = stringResource(R.string.label_time),
                    value = shot.getFormattedExtractionTime(),
                    isGood = shot.isOptimalExtractionTime(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BeanInformationCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val bean = shotDetails.bean

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
            title = stringResource(R.string.text_bean_information)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Bean name
        Text(
            text = bean.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Roast information
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.text_roast_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = bean.roastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.text_days_since_roast),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.format_days, shotDetails.daysSinceRoast),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bean.isFresh()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        // Bean notes if available
        if (bean.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = stringResource(R.string.text_bean_notes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = bean.notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ShotParametersCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val shot = shotDetails.shot

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.text_shot_parameters)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Parameters grid
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem(
                    label = stringResource(R.string.label_coffee_weight_in),
                    value = stringResource(R.string.format_weight_display_g, shot.coffeeWeightIn.toInt()),
                    modifier = Modifier.weight(1f)
                )
                ParameterItem(
                    label = stringResource(R.string.label_coffee_weight_out),
                    value = stringResource(R.string.format_weight_display_g, shot.coffeeWeightOut.toInt()),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem(
                    label = stringResource(R.string.label_extraction_time),
                    value = shot.getFormattedExtractionTime(),
                    isOptimal = shot.isOptimalExtractionTime(),
                    modifier = Modifier.weight(1f)
                )
                ParameterItem(
                    label = stringResource(R.string.label_brew_ratio),
                    value = shot.getFormattedBrewRatio(),
                    isOptimal = shot.isTypicalBrewRatio(),
                    modifier = Modifier.weight(1f)
                )
            }

            ParameterItem(
                label = stringResource(R.string.label_grinder_setting),
                value = shot.grinderSetting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShotAnalysisAndRecommendationsCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val analysis = shotDetails.analysis

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_shot_analysis)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Quality indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QualityIndicatorChip(
                label = stringResource(R.string.label_extraction_time),
                isGood = analysis.isOptimalExtraction,
                modifier = Modifier.weight(1f)
            )
            QualityIndicatorChip(
                label = stringResource(R.string.label_brew_ratio),
                isGood = analysis.isTypicalRatio,
                modifier = Modifier.weight(1f)
            )
            QualityIndicatorChip(
                label = stringResource(R.string.label_consistency),
                isGood = analysis.isConsistentWithHistory,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Deviations from average
        if (shotDetails.relatedShotsCount > 1) {
            Text(
                text = stringResource(R.string.text_compared_to_average),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                DeviationItem(
                    label = stringResource(R.string.label_brew_ratio),
                    deviation = analysis.brewRatioDeviation,
                    format = stringResource(R.string.format_decimal_two_place)
                )
                DeviationItem(
                    label = stringResource(R.string.label_extraction_time),
                    deviation = analysis.extractionTimeDeviation,
                    format = stringResource(R.string.format_decimal_zero_place),
                    suffix = "s"
                )
                DeviationItem(
                    label = stringResource(R.string.label_weight_in),
                    deviation = analysis.weightInDeviation,
                    format = stringResource(R.string.format_decimal_one_place),
                    suffix = "g"
                )
                DeviationItem(
                    label = stringResource(R.string.label_weight_out),
                    deviation = analysis.weightOutDeviation,
                    format = stringResource(R.string.format_decimal_one_place),
                    suffix = "g"
                )
            }
        }
    }
}

@Composable
private fun QualityScoreBreakdownCard(
    analysis: com.jodli.coffeeshottimer.domain.usecase.ShotAnalysis,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_quality_score_breakdown)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Total score with tier
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.text_total_score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${analysis.qualityScore}/100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        analysis.qualityScore >= EXCELLENT_THRESHOLD -> MaterialTheme.colorScheme.primary
                        analysis.qualityScore >= GOOD_THRESHOLD -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = stringResource(
                        when {
                            analysis.qualityScore >= EXCELLENT_THRESHOLD -> R.string.tier_excellent
                            analysis.qualityScore >= GOOD_THRESHOLD -> R.string.tier_good
                            else -> R.string.tier_needs_work
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Breakdown by category
        ScoreBreakdownItem(
            label = stringResource(R.string.label_taste_feedback),
            points = analysis.tastePoints,
            maxPoints = 30
        )
        ScoreBreakdownItem(
            label = stringResource(R.string.label_extraction_time),
            points = analysis.extractionTimePoints,
            maxPoints = 25
        )
        ScoreBreakdownItem(
            label = stringResource(R.string.label_brew_ratio),
            points = analysis.brewRatioPoints,
            maxPoints = 20
        )
        ScoreBreakdownItem(
            label = stringResource(R.string.label_consistency),
            points = analysis.consistencyPoints,
            maxPoints = 15
        )
        ScoreBreakdownItem(
            label = stringResource(R.string.label_precision),
            points = analysis.deviationBonusPoints,
            maxPoints = 10
        )

        // Improvement path if available
        analysis.improvementPath?.let { path ->
            Spacer(modifier = Modifier.height(spacing.medium))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.cornerMedium),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(spacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.text_to_improve),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                    ) {
                        Text(
                            text = stringResource(path.action.toStringRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "→ +${path.pointsNeeded} pts →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = stringResource(path.targetTier.toStringRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBreakdownItem(
    label: String,
    points: Int,
    maxPoints: Int,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        // Fixed width container for bar+score to ensure alignment
        Row(
            modifier = Modifier.width(130.dp),  // Fixed width ensures all rows align
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Progress bar - normalized to common max (30) for visual alignment
            val commonMax = 30f
            val visualProgress = points.toFloat() / commonMax
            val scoreRatio = points.toFloat() / maxPoints
            
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(visualProgress)
                        .background(
                            when {
                                scoreRatio >= SCORE_THRESHOLD_HIGH -> MaterialTheme.colorScheme.primary
                                scoreRatio >= SCORE_THRESHOLD_MEDIUM -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                )
            }
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            Text(
                text = "$points/$maxPoints",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(45.dp),  // Fixed width for score text
                textAlign = TextAlign.End
            )
        }
    }
}

// Constants for quality score thresholds
private const val EXCELLENT_THRESHOLD = 85
private const val GOOD_THRESHOLD = 60
private const val SCORE_THRESHOLD_HIGH = 0.8f
private const val SCORE_THRESHOLD_MEDIUM = 0.5f

// Extension functions for enum to string resource
private fun com.jodli.coffeeshottimer.domain.usecase.ImprovementAction.toStringRes(): Int {
    return when (this) {
        com.jodli.coffeeshottimer.domain.usecase.ImprovementAction.GRIND_FINER ->
            R.string.action_grind_finer
        com.jodli.coffeeshottimer.domain.usecase.ImprovementAction.GRIND_COARSER ->
            R.string.action_grind_coarser
        com.jodli.coffeeshottimer.domain.usecase.ImprovementAction.ADJUST_BREW_RATIO ->
            R.string.action_adjust_brew_ratio
        com.jodli.coffeeshottimer.domain.usecase.ImprovementAction.DIAL_IN_BASED_ON_TASTE ->
            R.string.action_dial_in_taste
    }
}

private fun com.jodli.coffeeshottimer.domain.usecase.QualityTier.toStringRes(): Int {
    return when (this) {
        com.jodli.coffeeshottimer.domain.usecase.QualityTier.EXCELLENT -> R.string.tier_excellent
        com.jodli.coffeeshottimer.domain.usecase.QualityTier.GOOD -> R.string.tier_good
        com.jodli.coffeeshottimer.domain.usecase.QualityTier.NEEDS_WORK -> R.string.tier_needs_work
    }
}

@Composable
private fun ComparisonContextCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_comparison_context)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        if (shotDetails.isPersonalBest) {
            Text(
                text = stringResource(R.string.text_personal_best),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.small))
        }

        shotDetails.rankingForBean?.let { ranking ->
            Text(
                text = stringResource(
                    R.string.format_ranking,
                    ranking,
                    shotDetails.relatedShotsCount
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.text_with_this_bean),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WhatHappenedCard(
    @Suppress("UNUSED_PARAMETER") shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_what_happened)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // TODO: full implementation in next iteration
        Text(
            text = stringResource(R.string.text_your_shot),
            style = MaterialTheme.typography.bodyMedium
        )

        // Show key parameters with context
    }
}

@Composable
private fun ShotContextCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_shot_context)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        Text(
            text = stringResource(
                R.string.format_shot_context,
                shotDetails.relatedShotsCount,
                shotDetails.relatedShotsCount,
                shotDetails.bean.name
            ),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(spacing.small))

        shotDetails.previousShot?.let { previousShot ->
            Text(
                text = stringResource(
                    R.string.format_previous_shot,
                    previousShot.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                    previousShot.getFormattedBrewRatio()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        shotDetails.nextShot?.let { nextShot ->
            Text(
                text = stringResource(
                    R.string.format_next_shot,
                    nextShot.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                    nextShot.getFormattedBrewRatio()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card showing grind adjustment recommendation for the next shot based on taste feedback.
 */
@Composable
private fun NextShotGrindAdjustmentCard(
    shotDetails: ShotDetails,
    viewModel: ShotDetailsViewModel,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val shot = shotDetails.shot

    // Get grind adjustment recommendation from ViewModel
    val grindAdjustmentRecommendation by viewModel.grindAdjustmentRecommendation.collectAsState()

    // Only show if we have a recommendation
    grindAdjustmentRecommendation?.let { recommendation ->
        CoffeeCard(modifier = modifier) {
            CardHeader(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.text_recommendations_for_next_shot)
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            GrindAdjustmentCard(
                recommendation = recommendation,
                isCompact = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Helper Composables

@Composable
private fun MetricDisplay(
    label: String,
    value: String,
    isGood: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isGood) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ParameterItem(
    label: String,
    value: String,
    isOptimal: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (!isOptimal) {
                Spacer(modifier = Modifier.width(spacing.extraSmall))
                Box(
                    modifier = Modifier
                        .size(spacing.qualityIndicator - 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun QualityIndicatorChip(
    label: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier.padding(horizontal = spacing.extraSmall),
        shape = RoundedCornerShape(spacing.cornerLarge),
        color = if (isGood) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.qualityIndicator)
                    .clip(CircleShape)
                    .background(
                        if (isGood) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
            )
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isGood) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeviationItem(
    label: String,
    deviation: Double,
    format: String,
    suffix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.format_deviation_display,
                if (deviation >= 0) "+" else "",
                format.format(deviation) + suffix
            ),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                kotlin.math.abs(deviation) < 0.1 -> MaterialTheme.colorScheme.primary
                kotlin.math.abs(deviation) < 0.3 -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun ShotDetailsLandscapeContent(
    shotDetails: ShotDetails,
    onEditTaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Left column - Actionable information
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(vertical = spacing.small)
        ) {
            // Shot Overview Card
            item {
                ShotOverviewCard(
                    shotDetails = shotDetails,
                    onEditTaste = onEditTaste
                )
            }

            // Quality Score Breakdown
            item {
                QualityScoreBreakdownCard(
                    analysis = shotDetails.analysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // What Happened & Why
            item {
                WhatHappenedCard(
                    shotDetails = shotDetails,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Right column - Contextual information
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(vertical = spacing.small)
        ) {
            // Comparison Context
            if (shotDetails.rankingForBean != null) {
                item {
                    ComparisonContextCard(
                        shotDetails = shotDetails,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bean Information Card
            item {
                BeanInformationCard(shotDetails = shotDetails)
            }

            // Shot Parameters (detailed)
            item {
                ShotParametersCard(shotDetails = shotDetails)
            }

            // Context Card (if available)
            if (shotDetails.previousShot != null || shotDetails.nextShot != null) {
                item {
                    ShotContextCard(shotDetails = shotDetails)
                }
            }
        }
    }
}
