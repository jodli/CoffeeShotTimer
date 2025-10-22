package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.CoachingInsights

/**
 * Card component that displays retrospective coaching insights about the user's brewing journey.
 * Shows bean-specific trends, dial-in status, and grind coaching effectiveness.
 * 
 * Expandable/collapsible for space efficiency while providing rich insights.
 */
@Composable
fun CoachingInsightsCard(
    insights: CoachingInsights,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        // Header row with emoji + title + expand/collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji indicator
                Text(
                    text = "📊",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Title
                val title = if (insights.beanName != null) {
                    stringResource(R.string.coaching_insights_title_with_bean, insights.beanName)
                } else {
                    stringResource(R.string.coaching_insights_title)
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Expand/collapse icon
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) {
                    stringResource(R.string.cd_collapse_insights)
                } else {
                    stringResource(R.string.cd_expand_insights)
                },
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(spacing.iconSmall)
            )
        }

        // Always show the most important insight when collapsed
        if (!isExpanded) {
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Show first available insight
            val summaryText = when {
                insights.recentTrend != null -> insights.recentTrend.consistencyMessage
                insights.dialInStatus != null -> insights.dialInStatus.statusMessage
                insights.grindCoachingEffectiveness != null -> insights.grindCoachingEffectiveness.effectivenessMessage
                else -> null
            }

            summaryText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Expanded content with all insights
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Spacer(modifier = Modifier.height(spacing.small))

                // Recent trend insight
                insights.recentTrend?.let { trend ->
                    InsightSection(
                        emoji = "🎯",
                        title = stringResource(R.string.coaching_insights_section_recent),
                        content = trend.consistencyMessage
                    )
                }

                // Dial-in status
                insights.dialInStatus?.let { status ->
                    InsightSection(
                        emoji = "☕",
                        title = stringResource(R.string.coaching_insights_section_progress),
                        content = status.statusMessage
                    )
                }

                // Grind coaching effectiveness
                insights.grindCoachingEffectiveness?.let { effectiveness ->
                    InsightSection(
                        emoji = "💡",
                        title = stringResource(R.string.coaching_insights_section_coaching),
                        content = effectiveness.effectivenessMessage
                    )
                }
            }
        }
    }
}

/**
 * Individual insight section with emoji, title, and content.
 */
@Composable
private fun InsightSection(
    emoji: String,
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.Top
    ) {
        // Emoji indicator
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge
        )

        // Content column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
