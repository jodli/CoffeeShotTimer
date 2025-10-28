package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Reusable expandable container for detail sections in analytics.
 *
 * Features:
 * - Header with title and animated chevron icon
 * - Chevron rotates 180° when expanded
 * - Content uses AnimatedVisibility with expandVertically() + fadeIn()
 * - Follows pattern from CoachingInsightsCard
 *
 * @param title Section title
 * @param isExpanded Whether the section is currently expanded
 * @param onToggle Callback when user toggles expansion
 * @param modifier Modifier for the container
 * @param content Composable content to show when expanded
 */
@Composable
fun ExpandableAnalyticsSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Header row with title and chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Chevron icon button with rotation
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(spacing.iconMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotationAngle
                    }
                )
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableAnalyticsSectionPreview() {
    CoffeeShotTimerTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ExpandableAnalyticsSection(
                title = "Quality Breakdown",
                isExpanded = true,
                onToggle = { }
            ) {
                Text(
                    text = "Excellent: 45 shots\nGood: 30 shots\nNeeds Work: 10 shots",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ExpandableAnalyticsSection(
                title = "Extraction Zones",
                isExpanded = false,
                onToggle = { }
            ) {
                Text(
                    text = "Too Fast: 15%\nOptimal: 70%\nToo Slow: 15%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
