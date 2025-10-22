package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Display component for taste feedback with optional edit capability.
 */
@Composable
fun TasteFeedbackDisplay(
    tastePrimary: TastePrimary?,
    tasteSecondary: TasteSecondary? = null,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tastePrimary != null) {
            // Primary taste badge
            TasteBadge(
                taste = tastePrimary,
                modifier = Modifier
            )

            // Secondary taste badge if present
            tasteSecondary?.let { secondary ->
                TasteQualifierBadge(
                    qualifier = secondary,
                    modifier = Modifier
                )
            }
        } else {
            // No taste recorded - add spacer when edit button is present
            if (onEditClick != null) {
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = stringResource(R.string.text_no_taste_recorded),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Edit button if callback provided
        onEditClick?.let { onClick ->
            if (tastePrimary != null) {
                Spacer(modifier = Modifier.weight(1f))
            }
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit_notes),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Badge component for displaying primary taste.
 */
@Composable
fun TasteBadge(
    taste: TastePrimary,
    modifier: Modifier = Modifier
) {
    val (emoji, label, color) = when (taste) {
        TastePrimary.SOUR -> Triple("😖", stringResource(R.string.taste_sour), MaterialTheme.colorScheme.error)
        TastePrimary.PERFECT -> Triple("😋", stringResource(R.string.taste_perfect), MaterialTheme.colorScheme.primary)
        TastePrimary.BITTER -> Triple("😣", stringResource(R.string.taste_bitter), MaterialTheme.colorScheme.tertiary)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 14.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

/**
 * Badge component for displaying secondary taste qualifier.
 */
@Composable
fun TasteQualifierBadge(
    qualifier: TasteSecondary,
    modifier: Modifier = Modifier
) {
    val (emoji, label) = when (qualifier) {
        TasteSecondary.WEAK -> "💧" to stringResource(R.string.taste_weak)
        TasteSecondary.STRONG -> "💪" to stringResource(R.string.taste_strong)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 12.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Compact taste display for list items.
 */
@Composable
fun CompactTasteDisplay(
    tastePrimary: TastePrimary?,
    tasteSecondary: TasteSecondary? = null,
    modifier: Modifier = Modifier
) {
    if (tastePrimary != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Just show emojis for compact display
            val primaryEmoji = when (tastePrimary) {
                TastePrimary.SOUR -> "😖"
                TastePrimary.PERFECT -> "😋"
                TastePrimary.BITTER -> "😣"
            }

            Text(
                text = primaryEmoji,
                fontSize = 16.sp
            )

            tasteSecondary?.let { secondary ->
                val secondaryEmoji = when (secondary) {
                    TasteSecondary.WEAK -> "💧"
                    TasteSecondary.STRONG -> "💪"
                }
                Text(
                    text = secondaryEmoji,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TasteFeedbackDisplayPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        TasteFeedbackDisplay(
            tastePrimary = TastePrimary.PERFECT,
            tasteSecondary = null,
            onEditClick = {}
        )

        TasteFeedbackDisplay(
            tastePrimary = TastePrimary.SOUR,
            tasteSecondary = TasteSecondary.WEAK,
            onEditClick = {}
        )

        TasteFeedbackDisplay(
            tastePrimary = null,
            tasteSecondary = null,
            onEditClick = {}
        )

        CompactTasteDisplay(
            tastePrimary = TastePrimary.BITTER,
            tasteSecondary = TasteSecondary.STRONG
        )
    }
}
