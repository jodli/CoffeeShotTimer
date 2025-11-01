package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.time.LocalDateTime
import kotlin.math.abs

private const val MIN_SHOTS_FOR_TRENDS = 5
private const val RATIO_STABLE_THRESHOLD = 0.1
private const val TIME_STABLE_THRESHOLD = 2.0
private const val COLOR_GREEN = 0xFF4CAF50
private const val COLOR_ORANGE = 0xFF9800
private const val COLOR_RED = 0xFF5722
private const val PREVIEW_SHOT_COUNT = 10
private const val PREVIEW_BASE_SCORE = 65
private const val PREVIEW_SCORE_INCREMENT = 3

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
                text = stringResource(R.string.analytics_trends_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (trends == null || trends.totalShots < MIN_SHOTS_FOR_TRENDS) {
                // Minimum data message
                Text(
                    text = if (trends == null || trends.totalShots == 0) {
                        stringResource(R.string.analytics_trend_insufficient_data, MIN_SHOTS_FOR_TRENDS)
                    } else {
                        stringResource(
                            R.string.analytics_trend_min_shots_format,
                            MIN_SHOTS_FOR_TRENDS - trends.totalShots
                        )
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
                    title = stringResource(R.string.analytics_raw_metrics),
                    isExpanded = isExpanded,
                    onToggle = { isExpanded = !isExpanded }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        MetricRow(
                            label = stringResource(R.string.analytics_label_avg_brew_ratio),
                            value = String.format("%.2f:1", trends.secondHalfAvgRatio)
                        )
                        MetricRow(
                            label = stringResource(R.string.analytics_label_avg_extraction_time),
                            value = String.format("%.1fs", trends.secondHalfAvgTime)
                        )
                        MetricRow(
                            label = stringResource(R.string.label_shots_per_day),
                            value = String.format("%.1f", trends.shotsPerDay)
                        )
                        MetricRow(
                            label = stringResource(R.string.analytics_label_days_analyzed),
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
@Composable
private fun getTrendIndicator(trends: ShotTrends): Triple<ImageVector, String, androidx.compose.ui.graphics.Color> {
    val ratioChange = trends.brewRatioTrend
    val timeChange = trends.extractionTimeTrend

    return if (trends.isImproving) {
        val changePoints = abs(ratioChange + timeChange).toInt()
        Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            stringResource(R.string.analytics_trend_improving_format, changePoints),
            androidx.compose.ui.graphics.Color(COLOR_GREEN)
        )
    } else if (abs(ratioChange) < RATIO_STABLE_THRESHOLD && abs(timeChange) < TIME_STABLE_THRESHOLD) {
        Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            stringResource(R.string.analytics_trend_stable_text),
            androidx.compose.ui.graphics.Color(COLOR_ORANGE)
        )
    } else {
        val changePoints = abs(ratioChange + timeChange).toInt()
        Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            stringResource(R.string.analytics_trend_declining_format, changePoints),
            androidx.compose.ui.graphics.Color(COLOR_RED)
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

        val sampleScores = (0 until PREVIEW_SHOT_COUNT).map { index ->
            Pair(
                LocalDateTime.now().minusDays((PREVIEW_SHOT_COUNT - 1 - index).toLong()),
                PREVIEW_BASE_SCORE + index * PREVIEW_SCORE_INCREMENT
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
