package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.time.format.DateTimeFormatter

/**
 * Performance-optimized shot item component.
 * Uses stable parameters and minimal recomposition to improve list performance.
 */
@Composable
fun OptimizedShotItem(
    shot: Shot,
    beanName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize expensive calculations
    val formattedDate = remember(shot.timestamp) {
        shot.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
    }

    val formattedBrewRatio = remember(shot.brewRatio) {
        shot.getFormattedBrewRatio()
    }

    val formattedExtractionTime = remember(shot.extractionTimeSeconds) {
        shot.getFormattedExtractionTime()
    }

    val isOptimalTime = remember(shot.extractionTimeSeconds) {
        shot.isOptimalExtractionTime()
    }

    val isTypicalRatio = remember(shot.brewRatio) {
        shot.isTypicalBrewRatio()
    }

    val spacing = LocalSpacing.current

    CoffeeCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left side - main info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Bean name and timestamp
                Text(
                    text = beanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Key metrics row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    OptimizedMetricChip(
                        label = "Ratio",
                        value = formattedBrewRatio,
                        isGood = isTypicalRatio
                    )

                    OptimizedMetricChip(
                        label = "Time",
                        value = formattedExtractionTime,
                        isGood = isOptimalTime
                    )

                    OptimizedMetricChip(
                        label = "Grinder",
                        value = shot.grinderSetting,
                        isNeutral = true
                    )
                }
            }

            // Right side - weights and quality
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${shot.coffeeWeightIn}g → ${shot.coffeeWeightOut}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Quality indicators
                OptimizedQualityIndicator(
                    isOptimalTime = isOptimalTime,
                    isTypicalRatio = isTypicalRatio
                )
            }
        }
    }
}

/**
 * Performance-optimized metric chip component.
 * Uses stable colors and minimal recomposition.
 */
@Composable
private fun OptimizedMetricChip(
    label: String,
    value: String,
    isGood: Boolean = false,
    isNeutral: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Memoize colors to avoid recomputation
    val backgroundColor = remember(isGood, isNeutral) {
        when {
            isNeutral -> null // Will use MaterialTheme.colorScheme.surfaceVariant
            isGood -> null // Will use MaterialTheme.colorScheme.primaryContainer
            else -> null // Will use MaterialTheme.colorScheme.errorContainer
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isNeutral -> MaterialTheme.colorScheme.surfaceVariant
            isGood -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    isGood -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    isGood -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

/**
 * Performance-optimized quality indicator component.
 * Uses stable state and minimal recomposition.
 */
@Composable
private fun OptimizedQualityIndicator(
    isOptimalTime: Boolean,
    isTypicalRatio: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
    ) {
        // Time indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isOptimalTime) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
        )

        // Ratio indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isTypicalRatio) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
        )
    }
}