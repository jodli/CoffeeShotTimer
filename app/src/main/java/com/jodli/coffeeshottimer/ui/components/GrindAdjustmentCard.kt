package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.*
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.util.GrindAdjustmentFormatter

/**
 * Card component displaying grind adjustment recommendation.
 *
 * @param recommendation The grind adjustment recommendation to display
 * @param onApply Callback when user applies the adjustment
 * @param onDismiss Callback when user dismisses/skips the adjustment
 * @param modifier Modifier for the card
 * @param isCompact Whether to use compact layout (for dialogs)
 */
@Composable
fun GrindAdjustmentCard(
    recommendation: GrindAdjustmentRecommendation,
    onApply: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val formatter = remember(context) { GrindAdjustmentFormatter(context) }

    // Animation for arrow movement to indicate direction
    val infiniteTransition = rememberInfiniteTransition(label = "arrow-animation")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (recommendation.adjustmentDirection == AdjustmentDirection.FINER) -6f else 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow-offset"
    )

    val cardColors = getAdjustmentColors(recommendation.adjustmentDirection)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.cornerLarge),
        colors = CardDefaults.cardColors(
            containerColor = cardColors.background,
            contentColor = cardColors.content
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (isCompact) spacing.small else spacing.medium
            )
        ) {
            // Header (only show in full mode)
            if (!isCompact) {
                Text(
                    text = stringResource(R.string.text_grind_adjustment_recommendation),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = cardColors.content
                )
            }

            // Current → Suggested display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current setting
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            if (isCompact) {
                                R.string.text_current_short
                            } else {
                                R.string.text_current_setting
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cardColors.content.copy(alpha = 0.7f)
                    )
                    Text(
                        text = recommendation.currentGrindSetting,
                        style = if (isCompact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = cardColors.content
                    )
                }

                // Adjustment arrow with animation
                if (recommendation.hasAdjustment()) {
                    Box(
                        modifier = Modifier.offset(x = arrowOffset.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAdjustmentIcon(recommendation.adjustmentDirection),
                            contentDescription = getAdjustmentContentDescription(recommendation.adjustmentDirection),
                            tint = cardColors.accent,
                            modifier = Modifier.size(if (isCompact) spacing.iconMedium else spacing.iconLarge)
                        )
                    }
                } else {
                    // No change - show check icon without animation
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.cd_grind_adjustment_no_change),
                        tint = cardColors.accent,
                        modifier = Modifier.size(if (isCompact) spacing.iconMedium else spacing.iconLarge)
                    )
                }

                // Suggested setting
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            if (isCompact) {
                                R.string.text_try_short
                            } else {
                                R.string.text_try_next
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cardColors.content.copy(alpha = 0.7f)
                    )
                    Text(
                        text = recommendation.suggestedGrindSetting,
                        style = if (isCompact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (recommendation.hasAdjustment()) cardColors.accent else cardColors.content
                    )
                }
            }

            // Explanation text
            Text(
                text = formatter.formatExplanation(recommendation),
                style = if (isCompact) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                textAlign = TextAlign.Center,
                color = cardColors.content.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )

            // Action buttons (only if callbacks are provided)
            if ((onApply != null || onDismiss != null) && recommendation.hasAdjustment()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    if (onDismiss != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = cardColors.content
                            )
                        ) {
                            Text(stringResource(R.string.button_skip_adjustment))
                        }
                    }

                    if (onApply != null) {
                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cardColors.accent,
                                contentColor = cardColors.background
                            )
                        ) {
                            Text(stringResource(R.string.button_apply_adjustment))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getAdjustmentIcon(direction: AdjustmentDirection): ImageVector {
    return when (direction) {
        AdjustmentDirection.FINER -> Icons.AutoMirrored.Default.KeyboardArrowLeft
        AdjustmentDirection.COARSER -> Icons.AutoMirrored.Default.KeyboardArrowRight
        AdjustmentDirection.NO_CHANGE -> Icons.Default.CheckCircle
    }
}

@Composable
private fun getAdjustmentContentDescription(direction: AdjustmentDirection): String {
    return stringResource(
        when (direction) {
            AdjustmentDirection.FINER -> R.string.cd_grind_adjustment_finer
            AdjustmentDirection.COARSER -> R.string.cd_grind_adjustment_coarser
            AdjustmentDirection.NO_CHANGE -> R.string.cd_grind_adjustment_no_change
        }
    )
}

/**
 * Data class for adjustment direction colors
 */
@androidx.compose.runtime.Immutable
private data class AdjustmentColors(
    val background: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color
)

/**
 * Get color scheme for different adjustment directions
 */
@Composable
private fun getAdjustmentColors(direction: AdjustmentDirection): AdjustmentColors {
    return when (direction) {
        AdjustmentDirection.FINER -> AdjustmentColors(
            background = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            accent = MaterialTheme.colorScheme.tertiary
        )
        AdjustmentDirection.COARSER -> AdjustmentColors(
            background = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            accent = MaterialTheme.colorScheme.secondary
        )
        AdjustmentDirection.NO_CHANGE -> AdjustmentColors(
            background = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            accent = MaterialTheme.colorScheme.primary
        )
    }
}

// Preview functions
@Suppress("UnusedPrivateMember")
@Preview(name = "Finer Adjustment", showBackground = true)
@Composable
private fun GrindAdjustmentCardFinerPreview() {
    CoffeeShotTimerTheme {
        GrindAdjustmentCard(
            recommendation = GrindAdjustmentRecommendation(
                currentGrindSetting = "15.0",
                suggestedGrindSetting = "14.5",
                adjustmentDirection = AdjustmentDirection.FINER,
                adjustmentSteps = 1,
                extractionTimeDeviation = -3,
                tasteIssue = TastePrimary.SOUR,
                confidence = ConfidenceLevel.HIGH
            ),
            onApply = {},
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Coarser Adjustment", showBackground = true)
@Composable
private fun GrindAdjustmentCardCoarserPreview() {
    CoffeeShotTimerTheme {
        GrindAdjustmentCard(
            recommendation = GrindAdjustmentRecommendation(
                currentGrindSetting = "15.0",
                suggestedGrindSetting = "16.0",
                adjustmentDirection = AdjustmentDirection.COARSER,
                adjustmentSteps = 2,
                extractionTimeDeviation = 5,
                tasteIssue = TastePrimary.BITTER,
                confidence = ConfidenceLevel.HIGH
            ),
            onApply = {},
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "No Change", showBackground = true)
@Composable
private fun GrindAdjustmentCardNoChangePreview() {
    CoffeeShotTimerTheme {
        GrindAdjustmentCard(
            recommendation = GrindAdjustmentRecommendation(
                currentGrindSetting = "15.0",
                suggestedGrindSetting = "15.0",
                adjustmentDirection = AdjustmentDirection.NO_CHANGE,
                adjustmentSteps = 0,
                extractionTimeDeviation = 0,
                tasteIssue = TastePrimary.PERFECT,
                confidence = ConfidenceLevel.HIGH
            )
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Compact Mode", showBackground = true)
@Composable
private fun GrindAdjustmentCardCompactPreview() {
    CoffeeShotTimerTheme {
        GrindAdjustmentCard(
            recommendation = GrindAdjustmentRecommendation(
                currentGrindSetting = "15.0",
                suggestedGrindSetting = "14.5",
                adjustmentDirection = AdjustmentDirection.FINER,
                adjustmentSteps = 1,
                extractionTimeDeviation = -3,
                tasteIssue = TastePrimary.SOUR,
                confidence = ConfidenceLevel.HIGH
            ),
            onApply = {},
            onDismiss = {},
            isCompact = true
        )
    }
}
