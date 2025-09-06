package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    // Get recommended taste based on extraction time
    val recommendedTaste = extractionTimeSeconds?.let { time ->
        when {
            time < 25 -> TastePrimary.SOUR
            time <= 30 -> TastePrimary.PERFECT
            else -> TastePrimary.BITTER
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                TastePrimaryEditButton(
                    taste = TastePrimary.SOUR,
                    emoji = "😖",
                    label = stringResource(R.string.taste_sour),
                    isSelected = selectedPrimary == TastePrimary.SOUR,
                    isRecommended = recommendedTaste == TastePrimary.SOUR,
                    onClick = { 
                        selectedPrimary = if (selectedPrimary == TastePrimary.SOUR) null else TastePrimary.SOUR
                    },
                    modifier = Modifier.weight(1f)
                )
                
                TastePrimaryEditButton(
                    taste = TastePrimary.PERFECT,
                    emoji = "😋",
                    label = stringResource(R.string.taste_perfect),
                    isSelected = selectedPrimary == TastePrimary.PERFECT,
                    isRecommended = recommendedTaste == TastePrimary.PERFECT,
                    onClick = { 
                        selectedPrimary = if (selectedPrimary == TastePrimary.PERFECT) null else TastePrimary.PERFECT
                    },
                    modifier = Modifier.weight(1f)
                )
                
                TastePrimaryEditButton(
                    taste = TastePrimary.BITTER,
                    emoji = "😣",
                    label = stringResource(R.string.taste_bitter),
                    isSelected = selectedPrimary == TastePrimary.BITTER,
                    isRecommended = recommendedTaste == TastePrimary.BITTER,
                    onClick = { 
                        selectedPrimary = if (selectedPrimary == TastePrimary.BITTER) null else TastePrimary.BITTER
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
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
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        TasteSecondaryEditChip(
                            taste = TasteSecondary.WEAK,
                            emoji = "💧",
                            label = stringResource(R.string.taste_weak),
                            isSelected = selectedSecondary == TasteSecondary.WEAK,
                            onClick = {
                                selectedSecondary = if (selectedSecondary == TasteSecondary.WEAK) null else TasteSecondary.WEAK
                            }
                        )
                        
                        TasteSecondaryEditChip(
                            taste = TasteSecondary.STRONG,
                            emoji = "💪",
                            label = stringResource(R.string.taste_strong),
                            isSelected = selectedSecondary == TasteSecondary.STRONG,
                            onClick = {
                                selectedSecondary = if (selectedSecondary == TasteSecondary.STRONG) null else TasteSecondary.STRONG
                            }
                        )
                    }
                }
            }
            
            // Recommendation hint
            if (recommendedTaste != null && extractionTimeSeconds != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when(recommendedTaste) {
                            TastePrimary.SOUR -> stringResource(R.string.text_extraction_time_sour_hint, extractionTimeSeconds)
                            TastePrimary.PERFECT -> stringResource(R.string.text_extraction_time_perfect_hint, extractionTimeSeconds)
                            TastePrimary.BITTER -> stringResource(R.string.text_extraction_time_bitter_hint, extractionTimeSeconds)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(spacing.small),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
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
    }
}

/**
 * Individual primary taste button for editing.
 */
@Composable
private fun TastePrimaryEditButton(
    taste: TastePrimary,
    emoji: String,
    label: String,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isRecommended -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isRecommended -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        border = BorderStroke(
            width = if (isSelected || isRecommended) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isRecommended && !isSelected) {
                Text(
                    text = stringResource(R.string.text_suggested),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Secondary taste qualifier chip for editing.
 */
@Composable
private fun TasteSecondaryEditChip(
    taste: TasteSecondary,
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Text(text = label)
            }
        },
        modifier = modifier
    )
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
