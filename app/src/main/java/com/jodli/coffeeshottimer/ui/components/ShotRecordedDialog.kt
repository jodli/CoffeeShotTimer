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
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

private const val EXTRACTION_TIME_FAST_THRESHOLD = -3
private const val EXTRACTION_TIME_SLOW_THRESHOLD = 3

/**
 * Dialog shown after successfully recording a shot, with optional recommendations.
 */
@Composable
fun ShotRecordedDialog(
    brewRatio: String,
    extractionTime: String,
    suggestedTaste: TastePrimary? = null,
    grindAdjustment: GrindAdjustmentRecommendation? = null,
    onTasteSelected: ((TastePrimary?, TasteSecondary?) -> Unit)? = null, // Reactive UI updates
    onSubmit: ((TastePrimary?, TasteSecondary?) -> Unit)? = null, // Save to database
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

                // Simple contextual explanation (if grind adjustment exists with meaningful content)
                if (grindAdjustment != null) {
                    // Determine the explanation text first
                    val explanationText = when {
                        grindAdjustment.extractionTimeDeviation < EXTRACTION_TIME_FAST_THRESHOLD ->
                            stringResource(R.string.feedback_shot_ran_fast)
                        grindAdjustment.extractionTimeDeviation > EXTRACTION_TIME_SLOW_THRESHOLD ->
                            stringResource(R.string.feedback_shot_ran_slow)
                        grindAdjustment.tasteIssue == TastePrimary.SOUR ->
                            stringResource(R.string.feedback_sour_taste)
                        grindAdjustment.tasteIssue == TastePrimary.BITTER ->
                            stringResource(R.string.feedback_bitter_taste)
                        else -> "" // Don't show for perfect shots
                    }

                    // Only render the explanation if there's actual content
                    if (explanationText.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.small))

                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            CoffeePrimaryButton(
                text = stringResource(R.string.text_dialog_ok), // "Done"
                onClick = {
                    // Save taste feedback to database
                    if (onSubmit != null) {
                        onSubmit(selectedPrimary, selectedSecondary)
                    }
                    onDismiss()
                },
                modifier = Modifier.widthIn(min = 120.dp)
            )
        },
        dismissButton = {
            if (onViewDetails != null) {
                TextButton(
                    onClick = {
                        // Save taste feedback to database before navigating
                        if (onSubmit != null) {
                            onSubmit(selectedPrimary, selectedSecondary)
                        }
                        onViewDetails()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.button_view_details))
                }
            }
        }
    )
}
