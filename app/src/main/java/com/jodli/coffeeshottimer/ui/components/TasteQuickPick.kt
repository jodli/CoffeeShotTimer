package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * One-tap taste feedback component for quick shot quality capture.
 * Shows three primary taste buttons with optional secondary qualifiers.
 */
@Composable
fun TasteQuickPick(
    recommended: TastePrimary? = null,
    onSelectPrimary: (TastePrimary) -> Unit,
    onSelectSecondary: ((TasteSecondary?) -> Unit)? = null,
    selectedSecondary: TasteSecondary? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showSecondaryOptions: Boolean = false
) {
    val spacing = LocalSpacing.current
    var localSelectedSecondary by remember(selectedSecondary) { 
        mutableStateOf(selectedSecondary) 
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = stringResource(R.string.text_how_did_it_taste),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = spacing.medium)
        )

        // Primary taste buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TastePrimaryButton(
                taste = TastePrimary.SOUR,
                emoji = "😖",
                label = stringResource(R.string.taste_sour),
                isRecommended = recommended == TastePrimary.SOUR,
                onClick = { 
                    onSelectPrimary(TastePrimary.SOUR)
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            TastePrimaryButton(
                taste = TastePrimary.PERFECT,
                emoji = "😊",
                label = stringResource(R.string.taste_perfect),
                isRecommended = recommended == TastePrimary.PERFECT,
                onClick = { 
                    onSelectPrimary(TastePrimary.PERFECT)
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            TastePrimaryButton(
                taste = TastePrimary.BITTER,
                emoji = "😣",
                label = stringResource(R.string.taste_bitter),
                isRecommended = recommended == TastePrimary.BITTER,
                onClick = { 
                    onSelectPrimary(TastePrimary.BITTER)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Optional secondary qualifiers
        if (showSecondaryOptions && onSelectSecondary != null) {
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TasteSecondaryChip(
                    taste = TasteSecondary.WEAK,
                    emoji = "💧",
                    label = stringResource(R.string.taste_weak),
                    isSelected = localSelectedSecondary == TasteSecondary.WEAK,
                    onClick = {
                        val newValue = if (localSelectedSecondary == TasteSecondary.WEAK) {
                            null
                        } else {
                            TasteSecondary.WEAK
                        }
                        localSelectedSecondary = newValue
                        onSelectSecondary(newValue)
                    }
                )
                
                Spacer(modifier = Modifier.width(spacing.small))
                
                TasteSecondaryChip(
                    taste = TasteSecondary.STRONG,
                    emoji = "💪",
                    label = stringResource(R.string.taste_strong),
                    isSelected = localSelectedSecondary == TasteSecondary.STRONG,
                    onClick = {
                        val newValue = if (localSelectedSecondary == TasteSecondary.STRONG) {
                            null
                        } else {
                            TasteSecondary.STRONG
                        }
                        localSelectedSecondary = newValue
                        onSelectSecondary(newValue)
                    }
                )
            }
        }

        // Skip button
        if (onSkip != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            
            TextButton(
                onClick = onSkip,
                modifier = Modifier.semantics {
                    contentDescription = "Skip taste feedback"
                }
            ) {
                Text(
                    text = stringResource(R.string.button_skip_taste),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Recommendation hint
        if (recommended != null) {
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = stringResource(R.string.text_recommended_based_on_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual primary taste button with emoji and label.
 */
@Composable
private fun TastePrimaryButton(
    taste: TastePrimary,
    emoji: String,
    label: String,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isRecommended) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        label = "border_color"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isRecommended) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "container_color"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(80.dp)
            .semantics {
                contentDescription = if (isRecommended) {
                    "$label (recommended)"
                } else {
                    label
                }
            },
        border = BorderStroke(
            width = if (isRecommended) 2.dp else 1.dp,
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
                fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Secondary taste qualifier chip (Weak/Strong).
 */
@Composable
private fun TasteSecondaryChip(
    taste: TasteSecondary,
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "chip_background"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .semantics {
                contentDescription = "$label qualifier ${if (isSelected) "selected" else "not selected"}"
            },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TasteQuickPickPreview() {
    TasteQuickPick(
        recommended = TastePrimary.PERFECT,
        onSelectPrimary = {},
        onSelectSecondary = {},
        showSecondaryOptions = true,
        onSkip = {}
    )
}
