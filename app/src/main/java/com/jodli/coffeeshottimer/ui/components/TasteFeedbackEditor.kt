package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Bottom sheet for editing taste feedback for an existing shot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteFeedbackEditSheet(
    currentTastePrimary: TastePrimary?,
    currentTasteSecondary: TasteSecondary?,
    extractionTimeSeconds: Int? = null,
    onSave: (TastePrimary?, TasteSecondary?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var selectedPrimary by remember { mutableStateOf(currentTastePrimary) }
    var selectedSecondary by remember { mutableStateOf(currentTasteSecondary) }

    // Get the suggested taste using shared utility
    val suggestedTaste = TasteUtils.getTasteRecommendation(extractionTimeSeconds)

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
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.text_taste_feedback),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.text_how_did_it_taste),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // Primary taste selection
            TastePrimaryButtonRow(
                suggestedTaste = suggestedTaste,
                selectedTaste = selectedPrimary,
                onTasteSelected = { taste ->
                    selectedPrimary = taste
                },
                allowDeselection = true
            )

            // Secondary taste qualifiers (optional)
            if (selectedPrimary != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                        }
                    )
                }
            }

            // Suggestion hint
            TasteSuggestionHint(
                suggestedTaste = suggestedTaste,
                extractionTimeSeconds = extractionTimeSeconds
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // Action buttons
            TasteFeedbackActionButtons(
                currentTastePrimary = currentTastePrimary,
                selectedPrimary = selectedPrimary,
                selectedSecondary = selectedSecondary,
                onSave = onSave,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Suggestion hint card that shows extraction time feedback.
 */
@Composable
private fun TasteSuggestionHint(
    suggestedTaste: TastePrimary?,
    extractionTimeSeconds: Int?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    if (suggestedTaste != null && extractionTimeSeconds != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = when (suggestedTaste) {
                    TastePrimary.SOUR -> stringResource(
                        R.string.text_extraction_time_sour_hint,
                        extractionTimeSeconds
                    )
                    TastePrimary.PERFECT -> stringResource(
                        R.string.text_extraction_time_perfect_hint,
                        extractionTimeSeconds
                    )
                    TastePrimary.BITTER -> stringResource(
                        R.string.text_extraction_time_bitter_hint,
                        extractionTimeSeconds
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(spacing.small),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Action buttons for the taste feedback edit sheet.
 */
@Composable
private fun TasteFeedbackActionButtons(
    currentTastePrimary: TastePrimary?,
    selectedPrimary: TastePrimary?,
    selectedSecondary: TasteSecondary?,
    onSave: (TastePrimary?, TasteSecondary?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Clear button
        if (currentTastePrimary != null) {
            OutlinedButton(
                onClick = {
                    onSave(null, null)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.button_clear_all))
            }
        }

        // Cancel button
        CoffeeSecondaryButton(
            text = stringResource(R.string.text_dialog_cancel),
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        )

        // Save button
        CoffeePrimaryButton(
            text = stringResource(R.string.button_save),
            onClick = {
                onSave(selectedPrimary, if (selectedPrimary != null) selectedSecondary else null)
                onDismiss()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TasteFeedbackEditSheetPreview() {
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        TasteFeedbackEditSheet(
            currentTastePrimary = TastePrimary.PERFECT,
            currentTasteSecondary = null,
            extractionTimeSeconds = 27,
            onSave = { _, _ -> },
            onDismiss = { showSheet = false }
        )
    }
}
