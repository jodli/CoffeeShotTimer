package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.util.formatForDisplay
import com.jodli.coffeeshottimer.ui.util.FormattedRecommendation

/**
 * Card component for displaying shot recommendations.
 */
@Composable
fun RecommendationCard(
    recommendations: List<ShotRecommendation>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val formattedRecommendations = recommendations.formatForDisplay()

    if (formattedRecommendations.isNotEmpty()) {
        CoffeeCard(modifier = modifier) {
            CardHeader(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.text_recommendations)
            )
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                formattedRecommendations.forEach { recommendation ->
                    RecommendationItem(
                        recommendation = recommendation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Individual recommendation item component following Coffee Shot Timer design patterns.
 */
@Composable
private fun RecommendationItem(
    recommendation: FormattedRecommendation,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.Top
    ) {
        // Priority indicator with coffee-themed styling
        PriorityIndicator(
            priority = recommendation.priority,
            modifier = Modifier.padding(top = 2.dp) // Align with text baseline
        )
        
        // Recommendation text with proper typography hierarchy
        Text(
            text = recommendation.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Priority indicator component with coffee-themed design.
 */
@Composable
private fun PriorityIndicator(
    priority: RecommendationPriority,
    size: Dp = LocalSpacing.current.qualityIndicator,
    modifier: Modifier = Modifier
) {
    val (color, contentDescription) = getPriorityColorAndDescription(priority)
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = color,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Empty circle for visual indicator
    }
}

/**
 * Get priority color and content description for consistent theming.
 */
@Composable
private fun getPriorityColorAndDescription(priority: RecommendationPriority): Pair<androidx.compose.ui.graphics.Color, String> {
    return when (priority) {
        RecommendationPriority.HIGH -> MaterialTheme.colorScheme.error to "High priority"
        RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary to "Medium priority"
        RecommendationPriority.LOW -> MaterialTheme.colorScheme.primary to "Low priority"
    }
}

/**
 * Compact version for smaller spaces following Coffee Shot Timer design patterns.
 */
@Composable
fun CompactRecommendationList(
    recommendations: List<ShotRecommendation>,
    modifier: Modifier = Modifier,
    maxItems: Int = 3
) {
    val spacing = LocalSpacing.current
    val formattedRecommendations = recommendations.formatForDisplay().take(maxItems)

    if (formattedRecommendations.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            // Section header following typography hierarchy
            Text(
                text = stringResource(R.string.text_recommendations),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            formattedRecommendations.forEach { recommendation ->
                CompactRecommendationItem(
                    recommendation = recommendation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Show "more" indicator if needed
            if (recommendations.size > maxItems) {
                Text(
                    text = stringResource(R.string.text_more, recommendations.size - maxItems),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = spacing.medium)
                )
            }
        }
    }
}

/**
 * Compact recommendation item for smaller displays.
 */
@Composable
private fun CompactRecommendationItem(
    recommendation: FormattedRecommendation,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        verticalAlignment = Alignment.Top
    ) {
        // Smaller priority indicator for compact view
        PriorityIndicator(
            priority = recommendation.priority,
            size = 6.dp,
            modifier = Modifier.padding(top = 6.dp) // Align with text baseline
        )
        
        // Recommendation text with caption typography
        Text(
            text = recommendation.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}