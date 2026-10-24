package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.ExtractionZone
import com.jodli.coffeeshottimer.ui.theme.zoneColors

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
 * Shared primary taste button component used in the recorded-shot dialog and
 * the taste editor. The swatch carries the extraction-zone colour for the taste.
 */
@Composable
fun TastePrimaryButton(
    taste: TastePrimary,
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
            containerColor = Color.Transparent
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MaterialTheme.zoneColors.fill(taste.zone()))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
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

private fun TastePrimary.zone(): ExtractionZone = when (this) {
    TastePrimary.SOUR -> ExtractionZone.FAST
    TastePrimary.PERFECT -> ExtractionZone.IDEAL
    TastePrimary.BITTER -> ExtractionZone.SLOW
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
 * Shared row of secondary taste qualifiers: two square label buttons; tapping
 * the selected one clears it.
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
        TasteSecondaryButton(
            label = stringResource(R.string.taste_weak),
            isSelected = selectedSecondary == TasteSecondary.WEAK,
            onClick = {
                onSecondarySelected(
                    if (selectedSecondary == TasteSecondary.WEAK) null else TasteSecondary.WEAK
                )
            },
            modifier = Modifier.weight(1f)
        )

        TasteSecondaryButton(
            label = stringResource(R.string.taste_strong),
            isSelected = selectedSecondary == TasteSecondary.STRONG,
            onClick = {
                onSecondarySelected(
                    if (selectedSecondary == TasteSecondary.STRONG) null else TasteSecondary.STRONG
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TasteSecondaryButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDesc = if (isSelected) {
        stringResource(R.string.cd_taste_qualifier_selected, label)
    } else {
        stringResource(R.string.cd_taste_qualifier_not_selected, label)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = contentDesc },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
