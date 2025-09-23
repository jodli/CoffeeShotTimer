package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary

/**
 * Utility functions for taste feedback components
 */
object TasteUtils {

    /**
     * Get the recommended taste based on extraction time.
     * Uses the same logic as GetTastePreselectionUseCase.
     */
    fun getTasteRecommendation(extractionTimeSeconds: Int?): TastePrimary? {
        if (extractionTimeSeconds == null || extractionTimeSeconds <= 0) {
            return null
        }

        val result = when {
            extractionTimeSeconds < 25 -> TastePrimary.SOUR // Under-extracted
            extractionTimeSeconds <= 30 -> TastePrimary.PERFECT // Optimal range
            else -> TastePrimary.BITTER // Over-extracted
        }

        return result
    }
}

/**
 * Shared primary taste button component used in both TasteQuickPick and TasteFeedbackEditSheet.
 */
@Composable
fun TastePrimaryButton(
    taste: TastePrimary,
    emoji: String,
    label: String,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 80 // dp
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isRecommended -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "border_color"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isRecommended -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        label = "container_color"
    )

    // Compute content description using string resources
    val contentDesc = when {
        isRecommended -> stringResource(R.string.cd_taste_suggested, label)
        isSelected -> stringResource(R.string.cd_taste_selected, label)
        else -> label
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(height.dp)
            .semantics {
                contentDescription = contentDesc
            },
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
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected || isRecommended) FontWeight.Bold else FontWeight.Normal
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
 * Shared secondary taste chip component.
 */
@Composable
fun TasteSecondaryChip(
    taste: TasteSecondary,
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Compute content description using string resources
    val contentDesc = if (isSelected) {
        stringResource(R.string.cd_taste_qualifier_selected, label)
    } else {
        stringResource(R.string.cd_taste_qualifier_not_selected, label)
    }

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
        modifier = modifier.semantics {
            contentDescription = contentDesc
        }
    )
}

/**
 * Shared row of primary taste buttons.
 */
@Composable
fun TastePrimaryButtonRow(
    suggestedTaste: TastePrimary? = null,
    selectedTaste: TastePrimary? = null,
    onTasteSelected: (TastePrimary?) -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Int = 80, // dp
    allowDeselection: Boolean = false // For edit mode
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TastePrimaryButton(
            taste = TastePrimary.SOUR,
            emoji = "😖",
            label = stringResource(R.string.taste_sour),
            isSelected = selectedTaste == TastePrimary.SOUR,
            isRecommended = suggestedTaste == TastePrimary.SOUR,
            onClick = {
                if (allowDeselection && selectedTaste == TastePrimary.SOUR) {
                    onTasteSelected(null)
                } else {
                    onTasteSelected(TastePrimary.SOUR)
                }
            },
            modifier = Modifier.weight(1f),
            height = buttonHeight
        )

        TastePrimaryButton(
            taste = TastePrimary.PERFECT,
            emoji = "😊",
            label = stringResource(R.string.taste_perfect),
            isSelected = selectedTaste == TastePrimary.PERFECT,
            isRecommended = suggestedTaste == TastePrimary.PERFECT,
            onClick = {
                if (allowDeselection && selectedTaste == TastePrimary.PERFECT) {
                    onTasteSelected(null)
                } else {
                    onTasteSelected(TastePrimary.PERFECT)
                }
            },
            modifier = Modifier.weight(1f),
            height = buttonHeight
        )

        TastePrimaryButton(
            taste = TastePrimary.BITTER,
            emoji = "😣",
            label = stringResource(R.string.taste_bitter),
            isSelected = selectedTaste == TastePrimary.BITTER,
            isRecommended = suggestedTaste == TastePrimary.BITTER,
            onClick = {
                if (allowDeselection && selectedTaste == TastePrimary.BITTER) {
                    onTasteSelected(null)
                } else {
                    onTasteSelected(TastePrimary.BITTER)
                }
            },
            modifier = Modifier.weight(1f),
            height = buttonHeight
        )
    }
}

/**
 * Shared row of secondary taste chips.
 */
@Composable
fun TasteSecondaryChipRow(
    selectedSecondary: TasteSecondary?,
    onSecondarySelected: (TasteSecondary?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TasteSecondaryChip(
            taste = TasteSecondary.WEAK,
            emoji = "💧",
            label = stringResource(R.string.taste_weak),
            isSelected = selectedSecondary == TasteSecondary.WEAK,
            onClick = {
                onSecondarySelected(
                    if (selectedSecondary == TasteSecondary.WEAK) null else TasteSecondary.WEAK
                )
            }
        )

        TasteSecondaryChip(
            taste = TasteSecondary.STRONG,
            emoji = "💪",
            label = stringResource(R.string.taste_strong),
            isSelected = selectedSecondary == TasteSecondary.STRONG,
            onClick = {
                onSecondarySelected(
                    if (selectedSecondary == TasteSecondary.STRONG) null else TasteSecondary.STRONG
                )
            }
        )
    }
}
