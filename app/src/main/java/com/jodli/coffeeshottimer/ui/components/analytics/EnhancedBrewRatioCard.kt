package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Enhanced brew ratio card with visual range indicator.
 *
 * Layout:
 * - CardHeader ("Brew Ratio")
 * - Visual range indicator: "1.5 ← avg → 3.0"
 * - Large percentage text ("X% in typical range")
 * - ExpandableAnalyticsSection with distribution and stats
 *
 * @param analysis Brew ratio analysis data
 * @param modifier Modifier for the card
 */
@Composable
fun EnhancedBrewRatioCard(
    analysis: BrewRatioAnalysis?,
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
                text = "Brew Ratio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (analysis == null || analysis.totalShots == 0) {
                // Empty state
                Text(
                    text = "Record 3 shots to see brew ratio analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Visual range indicator
                BrewRatioRangeIndicator(
                    minRatio = 1.5,
                    avgRatio = analysis.avgRatio,
                    maxRatio = 3.0,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Large percentage text
                Text(
                    text = "${analysis.typicalRatioPercentage.toInt()}% in typical range",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Expandable details section
                ExpandableAnalyticsSection(
                    title = "Brew Ratio Details",
                    isExpanded = isExpanded,
                    onToggle = { isExpanded = !isExpanded }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        // Distribution buckets
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            Text(
                                text = "Distribution",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DistributionRow(
                                label = "Under-extracted (<1.5)",
                                percentage = analysis.underExtractedPercentage.toInt()
                            )
                            DistributionRow(
                                label = "Typical (1.5-3.0)",
                                percentage = analysis.typicalRatioPercentage.toInt()
                            )
                            DistributionRow(
                                label = "Over-extracted (>3.0)",
                                percentage = analysis.overExtractedPercentage.toInt()
                            )
                        }

                        // Statistics
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            Text(
                                text = "Statistics",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            MetricRow(
                                label = "Average Ratio",
                                value = String.format("%.2f:1", analysis.avgRatio)
                            )
                            MetricRow(
                                label = "Median Ratio",
                                value = String.format("%.2f:1", analysis.medianRatio)
                            )
                            MetricRow(
                                label = "Range",
                                value = String.format("%.2f - %.2f", analysis.minRatio, analysis.maxRatio)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual range indicator showing min, average, and max ratios.
 */
@Composable
private fun BrewRatioRangeIndicator(
    minRatio: Double,
    avgRatio: Double,
    maxRatio: Double,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        // Calculate position for average (normalized between min and max)
        val normalizedAvg = ((avgRatio - minRatio) / (maxRatio - minRatio))
            .coerceIn(0.0, 1.0)
            .toFloat()
        val avgX = canvasWidth * 0.2f + (canvasWidth * 0.6f * normalizedAvg)

        // Draw left arrow and text
        val minText = String.format("%.1f", minRatio)
        val minTextLayout = textMeasurer.measure(minText, textStyle)
        drawText(
            textLayoutResult = minTextLayout,
            topLeft = Offset(
                x = 0f,
                y = centerY - minTextLayout.size.height / 2f
            )
        )

        // Draw right arrow and text
        val maxText = String.format("%.1f", maxRatio)
        val maxTextLayout = textMeasurer.measure(maxText, textStyle)
        drawText(
            textLayoutResult = maxTextLayout,
            topLeft = Offset(
                x = canvasWidth - maxTextLayout.size.width,
                y = centerY - maxTextLayout.size.height / 2f
            )
        )

        // Draw center average text (bold)
        val avgText = String.format("%.2f", avgRatio)
        val avgTextStyle = textStyle.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        val avgTextLayout = textMeasurer.measure(avgText, avgTextStyle)
        drawText(
            textLayoutResult = avgTextLayout,
            topLeft = Offset(
                x = avgX - avgTextLayout.size.width / 2f,
                y = centerY - avgTextLayout.size.height / 2f
            )
        )

        // Draw arrows pointing to average
        val arrowY = centerY
        val arrowLeftEnd = avgX - avgTextLayout.size.width / 2f - 8f
        val arrowRightStart = avgX + avgTextLayout.size.width / 2f + 8f

        // Left arrow
        drawLine(
            color = Color.Gray,
            start = Offset(minTextLayout.size.width + 8f, arrowY),
            end = Offset(arrowLeftEnd, arrowY),
            strokeWidth = 2f
        )

        // Right arrow
        drawLine(
            color = Color.Gray,
            start = Offset(arrowRightStart, arrowY),
            end = Offset(canvasWidth - maxTextLayout.size.width - 8f, arrowY),
            strokeWidth = 2f
        )
    }
}

/**
 * Row for displaying distribution label and percentage.
 */
@Composable
private fun DistributionRow(
    label: String,
    percentage: Int
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
            text = "$percentage%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Row for displaying metric label and value.
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
private fun EnhancedBrewRatioCardPreview() {
    CoffeeShotTimerTheme {
        val sampleAnalysis = BrewRatioAnalysis(
            totalShots = 50,
            avgRatio = 2.15,
            minRatio = 1.8,
            maxRatio = 2.8,
            medianRatio = 2.1,
            typicalRatioPercentage = 85.0,
            underExtractedPercentage = 8.0,
            overExtractedPercentage = 7.0,
            distribution = mapOf(
                "1.5-2.0" to 12,
                "2.0-2.5" to 28,
                "2.5-3.0" to 8,
                "3.0+" to 2
            )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnhancedBrewRatioCard(
                analysis = sampleAnalysis,
                modifier = Modifier
            )

            // Empty state
            EnhancedBrewRatioCard(
                analysis = null,
                modifier = Modifier
            )
        }
    }
}
