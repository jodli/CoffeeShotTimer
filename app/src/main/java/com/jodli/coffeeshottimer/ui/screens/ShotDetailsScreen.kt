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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.domain.usecase.ShotDetails
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
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

    // Load shot details when screen is first displayed
    LaunchedEffect(shotId) {
        viewModel.loadShotDetails(shotId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Shot Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
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
                                contentDescription = "Previous shot"
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
                                contentDescription = "Next shot"
                            )
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete shot",
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
                        message = "Loading shot details..."
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading shot details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        CoffeePrimaryButton(
                            text = "Retry",
                            onClick = { viewModel.refreshShotDetails() },
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                }

                uiState.shotDetails != null -> {
                    ShotDetailsContent(
                        shotDetails = uiState.shotDetails!!,
                        editNotesState = editNotesState,
                        onStartEditingNotes = { viewModel.startEditingNotes() },
                        onUpdateNotes = { viewModel.updateNotes(it) },
                        onSaveNotes = { viewModel.saveNotes() },
                        onCancelEditingNotes = { viewModel.cancelEditingNotes() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Shot") },
            text = { Text("Are you sure you want to delete this shot? This action cannot be undone.") },
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
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
}

@Composable
private fun ShotDetailsContent(
    shotDetails: ShotDetails,
    editNotesState: com.jodli.coffeeshottimer.ui.viewmodel.EditNotesState,
    onStartEditingNotes: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onCancelEditingNotes: () -> Unit,
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
            ShotOverviewCard(shotDetails = shotDetails)
        }

        // Bean Information Card
        item {
            BeanInformationCard(shotDetails = shotDetails)
        }

        // Shot Parameters Card
        item {
            ShotParametersCard(shotDetails = shotDetails)
        }

        // Analysis Card
        item {
            ShotAnalysisCard(shotDetails = shotDetails)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shot.timestamp.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(spacing.small))

                    // Quality score
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quality Score: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${shotDetails.analysis.qualityScore}/100",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                shotDetails.analysis.qualityScore >= 80 -> MaterialTheme.colorScheme.primary
                                shotDetails.analysis.qualityScore >= 60 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
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
                        text = "Brew Ratio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Key metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricDisplay(
                    label = "Input",
                    value = "${shot.coffeeWeightIn}g",
                    modifier = Modifier.weight(1f)
                )
                MetricDisplay(
                    label = "Output",
                    value = "${shot.coffeeWeightOut}g",
                    modifier = Modifier.weight(1f)
                )
                MetricDisplay(
                    label = "Time",
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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bean Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
                        text = "Roast Date",
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
                        text = "Days Since Roast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${shotDetails.daysSinceRoast} days",
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
                    text = "Bean Notes",
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
}

@Composable
private fun ShotParametersCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val shot = shotDetails.shot

    CoffeeCard(modifier = modifier) {
        Column {
            Text(
                text = "Shot Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                        label = "Coffee Weight In",
                        value = "${shot.coffeeWeightIn}g",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterItem(
                        label = "Coffee Weight Out",
                        value = "${shot.coffeeWeightOut}g",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParameterItem(
                        label = "Extraction Time",
                        value = shot.getFormattedExtractionTime(),
                        isOptimal = shot.isOptimalExtractionTime(),
                        modifier = Modifier.weight(1f)
                    )
                    ParameterItem(
                        label = "Brew Ratio",
                        value = shot.getFormattedBrewRatio(),
                        isOptimal = shot.isTypicalBrewRatio(),
                        modifier = Modifier.weight(1f)
                    )
                }

                ParameterItem(
                    label = "Grinder Setting",
                    value = shot.grinderSetting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShotAnalysisCard(
    shotDetails: ShotDetails,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val analysis = shotDetails.analysis

    CoffeeCard(modifier = modifier) {
        Column {
            Text(
                text = "Shot Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            // Quality indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QualityIndicatorChip(
                    label = "Extraction Time",
                    isGood = analysis.isOptimalExtraction,
                    modifier = Modifier.weight(1f)
                )
                QualityIndicatorChip(
                    label = "Brew Ratio",
                    isGood = analysis.isTypicalRatio,
                    modifier = Modifier.weight(1f)
                )
                QualityIndicatorChip(
                    label = "Consistency",
                    isGood = analysis.isConsistentWithHistory,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Deviations from average
            if (shotDetails.relatedShotsCount > 1) {
                Text(
                    text = "Compared to your average for this bean:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.small))

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    DeviationItem(
                        label = "Brew Ratio",
                        deviation = analysis.brewRatioDeviation,
                        format = "%.2f"
                    )
                    DeviationItem(
                        label = "Extraction Time",
                        deviation = analysis.extractionTimeDeviation,
                        format = "%.0f",
                        suffix = "s"
                    )
                    DeviationItem(
                        label = "Weight In",
                        deviation = analysis.weightInDeviation,
                        format = "%.1f",
                        suffix = "g"
                    )
                    DeviationItem(
                        label = "Weight Out",
                        deviation = analysis.weightOutDeviation,
                        format = "%.1f",
                        suffix = "g"
                    )
                }
            }

            // Recommendations
            if (analysis.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.medium))

                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.small))

                analysis.recommendations.forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!editNotesState.isEditing) {
                    IconButton(onClick = onStartEditingNotes) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit notes"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            if (editNotesState.isEditing) {
                // Edit mode
                CoffeeTextField(
                    value = editNotesState.notes,
                    onValueChange = onUpdateNotes,
                    label = "Shot Notes",
                    placeholder = "Add notes about this shot...",
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
                        text = "Cancel",
                        onClick = onCancelEditingNotes,
                        icon = Icons.Default.Close,
                        modifier = Modifier.weight(1f)
                    )

                    CoffeePrimaryButton(
                        text = if (editNotesState.isSaving) "Saving..." else "Save",
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
                        text = "No notes for this shot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Column {
            Text(
                text = "Shot Context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = "This is shot ${shotDetails.relatedShotsCount} of ${shotDetails.relatedShotsCount} with ${shotDetails.bean.name}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(spacing.small))

            shotDetails.previousShot?.let { previousShot ->
                Text(
                    text = "Previous shot: ${
                        previousShot.timestamp.format(
                            DateTimeFormatter.ofPattern(
                                "MMM dd, HH:mm"
                            )
                        )
                    } (${previousShot.getFormattedBrewRatio()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            shotDetails.nextShot?.let { nextShot ->
                Text(
                    text = "Next shot: ${nextShot.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))} (${nextShot.getFormattedBrewRatio()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
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
        shape = RoundedCornerShape(16.dp),
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
                    .size(8.dp)
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
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${if (deviation >= 0) "+" else ""}${format.format(deviation)}$suffix",
            style = MaterialTheme.typography.bodySmall,
            color = when {
                kotlin.math.abs(deviation) < 0.1 -> MaterialTheme.colorScheme.primary
                kotlin.math.abs(deviation) < 0.3 -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}