package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.ConfidenceLevel
import com.jodli.coffeeshottimer.domain.model.PersistentGrindRecommendation
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import java.time.LocalDateTime

/**
 * A prominent card that displays next-shot guidance based on the most recent shot.
 * This component implements Epic 4 requirements for persistent recommendation display.
 * 
 * Features:
 * - Visual prominence with elevated card design
 * - Direction indicator (↑/↓) for grind adjustments
 * - Context-aware messaging (taste-based vs timing-based)
 * - Confidence level indicator
 * - Action buttons for Apply/Dismiss
 */
@Composable
fun NextShotGuidanceCard(
    recommendation: PersistentGrindRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 10.dp,
            focusedElevation = 10.dp,
            hoveredElevation = 10.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and confidence indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.next_shot_guidance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                ConfidenceIndicator(
                    confidence = recommendation.confidence,
                    modifier = Modifier
                )
            }
            
            // Main recommendation content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction indicator
                AdjustmentDirectionIcon(
                    direction = recommendation.adjustmentDirection,
                    modifier = Modifier.size(32.dp)
                )
                
                // Recommendation details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Grind setting recommendation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_grinder_setting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = recommendation.suggestedGrindSetting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = recommendation.getAdjustmentDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Context/reason explanation
            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.button_apply))
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.button_dismiss))
                }
            }
        }
    }
}

/**
 * Displays the adjustment direction with an appropriate icon.
 */
@Composable
private fun AdjustmentDirectionIcon(
    direction: AdjustmentDirection,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (direction) {
        AdjustmentDirection.FINER -> Icons.Default.TrendingUp to MaterialTheme.colorScheme.primary
        AdjustmentDirection.COARSER -> Icons.Default.TrendingDown to MaterialTheme.colorScheme.secondary
        AdjustmentDirection.NO_CHANGE -> Icons.Default.TrendingFlat to MaterialTheme.colorScheme.tertiary
    }
    
    Icon(
        imageVector = icon,
        contentDescription = when (direction) {
            AdjustmentDirection.FINER -> stringResource(R.string.grind_finer)
            AdjustmentDirection.COARSER -> stringResource(R.string.grind_coarser)
            AdjustmentDirection.NO_CHANGE -> stringResource(R.string.no_change_needed)
        },
        modifier = modifier,
        tint = tint
    )
}

/**
 * Shows a subtle confidence indicator for the recommendation.
 */
@Composable
private fun ConfidenceIndicator(
    confidence: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (confidence) {
        ConfidenceLevel.HIGH -> stringResource(R.string.high_confidence) to MaterialTheme.colorScheme.primary
        ConfidenceLevel.MEDIUM -> stringResource(R.string.medium_confidence) to MaterialTheme.colorScheme.secondary
        ConfidenceLevel.LOW -> stringResource(R.string.low_confidence) to MaterialTheme.colorScheme.tertiary
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

/**
 * Animated wrapper for the NextShotGuidanceCard that handles show/hide animations.
 */
@Composable
fun AnimatedNextShotGuidanceCard(
    recommendation: PersistentGrindRecommendation?,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = recommendation != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        recommendation?.let {
            NextShotGuidanceCard(
                recommendation = it,
                onApply = onApply,
                onDismiss = onDismiss
            )
        }
    }
}

// Preview composables for different states
@Preview(name = "High Confidence Taste-Based", showBackground = true)
@Composable
private fun NextShotGuidanceCardTasteBasedPreview() {
    CoffeeShotTimerTheme {
        NextShotGuidanceCard(
            recommendation = PersistentGrindRecommendation(
                beanId = "test_bean",
                suggestedGrindSetting = "5.3",
                adjustmentDirection = AdjustmentDirection.FINER,
                reason = "Last shot was sour (24s)",
                recommendedDose = 18.0,
                targetExtractionTime = 25..30,
                timestamp = LocalDateTime.now(),
                wasFollowed = false,
                basedOnTaste = true,
                confidence = ConfidenceLevel.HIGH
            ),
            onApply = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Medium Confidence Timing-Based", showBackground = true)
@Composable
private fun NextShotGuidanceCardTimingBasedPreview() {
    CoffeeShotTimerTheme {
        NextShotGuidanceCard(
            recommendation = PersistentGrindRecommendation(
                beanId = "test_bean",
                suggestedGrindSetting = "5.8",
                adjustmentDirection = AdjustmentDirection.COARSER,
                reason = "Last shot ran too slow (33s)",
                recommendedDose = 18.5,
                targetExtractionTime = 25..30,
                timestamp = LocalDateTime.now(),
                wasFollowed = false,
                basedOnTaste = false,
                confidence = ConfidenceLevel.MEDIUM
            ),
            onApply = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Low Confidence No Change", showBackground = true)
@Composable
private fun NextShotGuidanceCardNoChangePreview() {
    CoffeeShotTimerTheme {
        NextShotGuidanceCard(
            recommendation = PersistentGrindRecommendation(
                beanId = "test_bean",
                suggestedGrindSetting = "5.5",
                adjustmentDirection = AdjustmentDirection.NO_CHANGE,
                reason = "Last shot was perfect (27s)",
                recommendedDose = 18.0,
                targetExtractionTime = 25..30,
                timestamp = LocalDateTime.now(),
                wasFollowed = false,
                basedOnTaste = true,
                confidence = ConfidenceLevel.LOW
            ),
            onApply = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Animated Card Visible", showBackground = true)
@Composable
private fun AnimatedNextShotGuidanceCardPreview() {
    CoffeeShotTimerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Content above the card",
                style = MaterialTheme.typography.bodyLarge
            )
            
            AnimatedNextShotGuidanceCard(
                recommendation = PersistentGrindRecommendation(
                    beanId = "test_bean",
                    suggestedGrindSetting = "5.3",
                    adjustmentDirection = AdjustmentDirection.FINER,
                    reason = "Last shot was sour (24s)",
                    recommendedDose = 18.0,
                    targetExtractionTime = 25..30,
                    timestamp = LocalDateTime.now(),
                    wasFollowed = false,
                    basedOnTaste = true,
                    confidence = ConfidenceLevel.HIGH
                ),
                onApply = {},
                onDismiss = {}
            )
            
            Text(
                text = "Content below the card",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
