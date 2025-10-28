package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Enhanced trends card with line graph and expandable details.
 *
 * Layout:
 * - CardHeader ("Recent Trends")
 * - TrendLineGraph (primary visual)
 * - Trend indicator: icon + text ("Improving - up X points")
 * - ExpandableAnalyticsSection with raw metrics
 *
 * Requires minimum 5 shots for graph display.
 *
 * @param trends Shot trends data from statistics use case
 * @param qualityScores List of quality scores with dates for graphing
 * @param modifier Modifier for the card
 */
@Composable
fun EnhancedShotTrendsCard(
    trends: ShotTrends?,
    qualityScores: List<Pair<LocalDateTime, Int>>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var isExpanded by remember { mutableStateOf(false) }

    CoffeeCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Header
            Text(
                text = "Recent Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (trends == null || trends.totalShots < 5) {
                // Minimum data message
                Text(
                    text = if (trends == null || trends.totalShots == 0) {
                        "Record 5 shots to see trends"
                    } else {
                        "Record ${5 - trends.totalShots} more shots for trend analysis"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Convert quality scores to TrendDataPoint
                val dataPoints = qualityScores.map { (dateTime, score) ->
                    TrendDataPoint(
                        date = dateTime.toLocalDate(),
                        score = score
                    )
                }.sortedBy { it.date }

                // Trend line graph
                TrendLineGraph(
                    dataPoints = dataPoints,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Trend indicator
                val (trendIcon, trendText, trendColor) = getTrendIndicator(trends)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(spacing.iconMedium)
                    )
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = trendColor
                    )
                }

                // Expandable details section
                ExpandableAnalyticsSection(
                    title = "Raw Metrics",
                    isExpanded = isExpanded,
                    onToggle = { isExpanded = !isExpanded }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        MetricRow(
                            label = "Avg Brew Ratio",
                            value = String.format("%.2f:1", trends.secondHalfAvgRatio)
                        )
                        MetricRow(
                            label = "Avg Extraction Time",
                            value = String.format("%.1fs", trends.secondHalfAvgTime)
                        )
                        MetricRow(
                            label = "Shots Per Day",
                            value = String.format("%.1f", trends.shotsPerDay)
                        )
                        MetricRow(
                            label = "Days Analyzed",
                            value = trends.daysAnalyzed.toString()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to determine trend indicator display.
 */
private fun getTrendIndicator(trends: ShotTrends): Triple<ImageVector, String, androidx.compose.ui.graphics.Color> {
    val ratioChange = trends.brewRatioTrend
    val timeChange = trends.extractionTimeTrend
    
    return if (trends.isImproving) {
        val changePoints = abs(ratioChange + timeChange).toInt()
        Triple(
            Icons.Default.TrendingUp,
            "Improving - up $changePoints points",
            androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        )
    } else if (abs(ratioChange) < 0.1 && abs(timeChange) < 2.0) {
        Triple(
            Icons.Default.TrendingFlat,
            "Stable - consistent performance",
            androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        )
    } else {
        val changePoints = abs(ratioChange + timeChange).toInt()
        Triple(
            Icons.Default.TrendingDown,
            "Declining - down $changePoints points",
            androidx.compose.ui.graphics.Color(0xFFFF5722) // Red
        )
    }
}

/**
 * Simple row for displaying metric label and value.
 */
@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun EnhancedShotTrendsCardPreview() {
    CoffeeShotTimerTheme {
        val sampleTrends = ShotTrends(
            totalShots = 20,
            daysAnalyzed = 30,
            shotsPerDay = 0.67,
            brewRatioTrend = 0.15,
            extractionTimeTrend = 1.2,
            firstHalfAvgRatio = 2.0,
            secondHalfAvgRatio = 2.15,
            firstHalfAvgTime = 26.5,
            secondHalfAvgTime = 27.7,
            isImproving = true
        )

        val sampleScores = (0..9).map { index ->
            Pair(
                LocalDateTime.now().minusDays((9 - index).toLong()),
                65 + index * 3
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnhancedShotTrendsCard(
                trends = sampleTrends,
                qualityScores = sampleScores,
                modifier = Modifier
            )

            // Empty state
            EnhancedShotTrendsCard(
                trends = null,
                qualityScores = emptyList(),
                modifier = Modifier
            )
        }
    }
}
