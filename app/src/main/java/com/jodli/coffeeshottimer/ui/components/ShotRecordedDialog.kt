package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Dialog shown after successfully recording a shot, with optional recommendations.
 */
@Composable
fun ShotRecordedDialog(
    brewRatio: String,
    extractionTime: String,
    recommendations: List<ShotRecommendation> = emptyList(),
    suggestedTaste: TastePrimary? = null,
    grindAdjustment: GrindAdjustmentRecommendation? = null,
    onTasteSelected: ((TastePrimary?, TasteSecondary?) -> Unit)? = null, // Reactive UI updates
    onSubmit: ((TastePrimary?, TasteSecondary?) -> Unit)? = null, // Save to database
    onGrindAdjustmentApply: (() -> Unit)? = null,
    onGrindAdjustmentDismiss: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // State for taste selection
    var selectedPrimary by remember { mutableStateOf<TastePrimary?>(null) }
    var selectedSecondary by remember { mutableStateOf<TasteSecondary?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_shot_recorded_success),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconLarge)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_shot_recorded_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Shot summary
                Text(
                    text = stringResource(R.string.text_record_successfully, brewRatio, extractionTime),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                // Taste feedback section
                if (onTasteSelected != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = spacing.small))

                    Text(
                        text = stringResource(R.string.text_how_did_it_taste),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )

                    // Primary taste buttons
                    TastePrimaryButtonRow(
                        suggestedTaste = suggestedTaste,
                        selectedTaste = selectedPrimary,
                        onTasteSelected = { taste ->
                            selectedPrimary = taste
                            // Clear secondary when primary is deselected
                            if (taste == null) {
                                selectedSecondary = null
                            }
                            // Recalculate grind adjustment when taste selection handler is provided
                            onTasteSelected?.invoke(taste, selectedSecondary)
                        },
                        allowDeselection = true
                    )

                    // Secondary taste qualifiers (optional)
                    if (selectedPrimary != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(spacing.small))

                            Text(
                                text = stringResource(R.string.text_strength_modifier_optional),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(spacing.extraSmall))

                            TasteSecondaryChipRow(
                                selectedSecondary = selectedSecondary,
                                onSecondarySelected = { newValue ->
                                    selectedSecondary = newValue
                                    // Update grind adjustment with new secondary taste
                                    onTasteSelected?.invoke(selectedPrimary, newValue)
                                }
                            )
                        }
                    }
                }

                // Grind adjustment recommendation (always show when available)
                if (grindAdjustment != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = spacing.small))

                    GrindAdjustmentCard(
                        recommendation = grindAdjustment,
                        onApply = onGrindAdjustmentApply,
                        onDismiss = onGrindAdjustmentDismiss,
                        isCompact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Next steps based on this shot
                if (recommendations.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.text_next_steps_based_on_shot),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    CompactRecommendationList(
                        recommendations = recommendations,
                        maxItems = 2,
                        showAsNextSteps = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (recommendations.size > 2) {
                        Text(
                            text = stringResource(R.string.dialog_view_details_for_more_recommendations),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onViewDetails != null) {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_view_details),
                    onClick = {
                        // Save taste feedback to database before navigating
                        if (onSubmit != null) {
                            onSubmit(selectedPrimary, selectedSecondary)
                        }
                        onViewDetails()
                        onDismiss()
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
        },
        dismissButton = {
            if (onViewDetails != null) {
                CoffeeSecondaryButton(
                    text = if (selectedPrimary != null) {
                        // When taste is selected: "Save" (save and close)
                        stringResource(R.string.button_save)
                    } else {
                        // When no taste selected: "Skip" (close without saving)
                        stringResource(R.string.button_skip_taste)
                    },
                    onClick = {
                        // Save taste feedback to database
                        if (onSubmit != null) {
                            onSubmit(selectedPrimary, selectedSecondary)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
        }
    )
}

/**
 * Bottom sheet version for showing shot recorded with recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotRecordedBottomSheet(
    brewRatio: String,
    extractionTime: String,
    recommendations: List<ShotRecommendation> = emptyList(),
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.screenPadding)
                .padding(bottom = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Success icon and title
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_shot_recorded_success),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconEmptyState)
            )

            Text(
                text = stringResource(R.string.dialog_shot_recorded_successfully),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            // Shot summary
            Text(
                text = stringResource(R.string.text_record_successfully, brewRatio, extractionTime),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            // Next steps based on this shot
            if (recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = stringResource(R.string.text_next_steps_based_on_shot),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.small))

                RecommendationCard(
                    recommendations = recommendations,
                    showAsNextSteps = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.text_dialog_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                if (onViewDetails != null) {
                    CoffeePrimaryButton(
                        text = stringResource(R.string.button_view_details),
                        onClick = {
                            onViewDetails()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
