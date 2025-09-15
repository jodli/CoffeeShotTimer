package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.ConfidenceLevel
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.usecase.ShotDetails
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.GrindAdjustmentCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.TasteFeedbackDisplay
import com.jodli.coffeeshottimer.ui.components.TasteFeedbackEditSheet
import com.jodli.coffeeshottimer.ui.util.formatForDisplay
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
    val editNotesState by viewModel.editNotesState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTasteEditor by remember { mutableStateOf(false) }

    // Load shot details when screen is first displayed
    LaunchedEffect(shotId) {
        viewModel.loadShotDetails(shotId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
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
                                editNotesState = editNotesState,
                                onStartEditingNotes = { viewModel.startEditingNotes() },
                                onUpdateNotes = { viewModel.updateNotes(it) },
                                onSaveNotes = { viewModel.saveNotes() },
                                onCancelEditingNotes = { viewModel.cancelEditingNotes() },
                                onEditTaste = { showTasteEditor = true },
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        landscapeContent = {
                            ShotDetailsLandscapeContent(
                                shotDetails = uiState.shotDetails!!,
                                editNotesState = editNotesState,
                                onStartEditingNotes = { viewModel.startEditingNotes() },
                                onUpdateNotes = { viewModel.updateNotes(it) },
                                onSaveNotes = { viewModel.saveNotes() },
                                onCancelEditingNotes = { viewModel.cancelEditingNotes() },
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

    editNotesState.error?.let { error ->
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
    editNotesState: com.jodli.coffeeshottimer.ui.viewmodel.EditNotesState,
    onStartEditingNotes: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onCancelEditingNotes: () -> Unit,
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
        // Shot Overview Card
        item {
            ShotOverviewCard(
                shotDetails = shotDetails,
                onEditTaste = onEditTaste
            )
        }

        // Bean Information Card
        item {
            BeanInformationCard(shotDetails = shotDetails)
        }

        // Shot Parameters Card
        item {
            ShotParametersCard(shotDetails = shotDetails)
        }

        // Analysis & Recommendations Card (combined)
        item {
            ShotAnalysisAndRecommendationsCard(shotDetails = shotDetails)
        }

        // Grind Adjustment Recommendation (always show when available)
        item {
            NextShotGrindAdjustmentCard(
                shotDetails = shotDetails,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Notes Card
        item {
            ShotNotesCard(
                shotDetails = shotDetails,
                editNotesState = editNotesState,
                onStartEditingNotes = onStartEditingNotes,
                onUpdateNotes = onUpdateNotes,
                onSaveNotes = onSaveNotes,
                onCancelEditingNotes = onCancelEditingNotes
            )
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
                Column(modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.medium)) {
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
private fun ShotNotesCard(
    shotDetails: ShotDetails,
    editNotesState: com.jodli.coffeeshottimer.ui.viewmodel.EditNotesState,
    onStartEditingNotes: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onCancelEditingNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.label_notes),
            actions = {
                if (!editNotesState.isEditing) {
                    IconButton(onClick = onStartEditingNotes) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_notes)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        if (editNotesState.isEditing) {
            // Edit mode
            CoffeeTextField(
                value = editNotesState.notes,
                onValueChange = onUpdateNotes,
                label = stringResource(R.string.label_shot_notes),
                placeholder = stringResource(R.string.placeholder_shot_notes),
                singleLine = false,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = onCancelEditingNotes,
                    icon = Icons.Default.Close,
                    modifier = Modifier.weight(1f)
                )

                CoffeePrimaryButton(
                    text = if (editNotesState.isSaving) stringResource(R.string.cd_saving) else stringResource(R.string.cd_save),
                    onClick = onSaveNotes,
                    enabled = !editNotesState.isSaving && editNotesState.hasChanges,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }

            if (editNotesState.error != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = editNotesState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Display mode
            if (shotDetails.shot.notes.isNotBlank()) {
                Text(
                    text = shotDetails.shot.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.text_no_notes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            text = stringResource(R.string.format_shot_context, shotDetails.relatedShotsCount, shotDetails.relatedShotsCount, shotDetails.bean.name),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(spacing.small))

        shotDetails.previousShot?.let { previousShot ->
            Text(
                text = stringResource(R.string.format_previous_shot, previousShot.timestamp.format( DateTimeFormatter.ofPattern( "MMM dd, HH:mm")), previousShot.getFormattedBrewRatio()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        shotDetails.nextShot?.let { nextShot ->
            Text(
                text = stringResource(R.string.format_next_shot, nextShot.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")), nextShot.getFormattedBrewRatio()),
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
            text = stringResource(R.string.format_deviation_display, if (deviation >= 0) "+" else "", format.format(deviation) + suffix),
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
    editNotesState: com.jodli.coffeeshottimer.ui.viewmodel.EditNotesState,
    onStartEditingNotes: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onCancelEditingNotes: () -> Unit,
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
        // Left column - Primary information
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

            // Shot Parameters Card
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

        // Right column - Secondary information
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(vertical = spacing.small)
        ) {
            // Bean Information Card
            item {
                BeanInformationCard(shotDetails = shotDetails)
            }

            // Analysis & Recommendations Card
            item {
                ShotAnalysisAndRecommendationsCard(shotDetails = shotDetails)
            }

            // Notes Card
            item {
                ShotNotesCard(
                    shotDetails = shotDetails,
                    editNotesState = editNotesState,
                    onStartEditingNotes = onStartEditingNotes,
                    onUpdateNotes = onUpdateNotes,
                    onSaveNotes = onSaveNotes,
                    onCancelEditingNotes = onCancelEditingNotes
                )
            }
        }
    }
}
