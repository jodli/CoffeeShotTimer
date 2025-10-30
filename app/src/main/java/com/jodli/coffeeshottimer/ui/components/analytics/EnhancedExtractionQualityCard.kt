package com.jodli.coffeeshottimer.ui.components.analytics

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

private const val PREVIEW_SAMPLE_TOTAL_SHOTS = 50
private const val PREVIEW_SAMPLE_MIN_TIME = 22
private const val PREVIEW_SAMPLE_MAX_TIME = 35
private const val PREVIEW_DIST_20_25 = 6
private const val PREVIEW_DIST_25_30 = 36
private const val PREVIEW_DIST_30_35 = 7
private const val PREVIEW_DIST_35_PLUS = 1

/**
 * Enhanced extraction quality card with visual zone indicator.
 *
 * Replaces text-heavy ExtractionTimeAnalysisCard with:
 * - CardHeader ("Extraction Quality")
 * - QualityZoneIndicator (horizontal bar with zones)
 * - Large percentage text ("X% in optimal zone")
 * - ExpandableAnalyticsSection with distribution and stats
 *
 * @param analysis Extraction time analysis data
 * @param modifier Modifier for the card
 */
@Composable
fun EnhancedExtractionQualityCard(
    analysis: ExtractionTimeAnalysis?,
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
                text = stringResource(R.string.analytics_extraction_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (analysis == null || analysis.totalShots == 0) {
                // Empty state
                Text(
                    text = stringResource(R.string.analytics_extraction_min_shots),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Quality zone indicator
                QualityZoneIndicator(
                    optimalPercentage = analysis.optimalTimePercentage.toInt(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Large percentage text
                Text(
                    text = stringResource(
                        R.string.analytics_optimal_zone_format,
                        analysis.optimalTimePercentage.toInt()
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Expandable details section
                ExpandableAnalyticsSection(
                    title = stringResource(R.string.analytics_extraction_details),
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
                                text = stringResource(R.string.text_distribution),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DistributionRow(
                                label = stringResource(R.string.analytics_zone_too_fast),
                                percentage = analysis.tooFastPercentage.toInt()
                            )
                            DistributionRow(
                                label = stringResource(R.string.analytics_zone_optimal),
                                percentage = analysis.optimalTimePercentage.toInt()
                            )
                            DistributionRow(
                                label = stringResource(R.string.analytics_zone_too_slow),
                                percentage = analysis.tooSlowPercentage.toInt()
                            )
                        }

                        // Statistics
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            Text(
                                text = stringResource(R.string.analytics_statistics),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            MetricRow(
                                label = stringResource(R.string.analytics_label_average_time),
                                value = String.format("%.1fs", analysis.avgTime)
                            )
                            MetricRow(
                                label = stringResource(R.string.analytics_label_median_time),
                                value = String.format("%.1fs", analysis.medianTime)
                            )
                            MetricRow(
                                label = stringResource(R.string.label_range),
                                value = "${analysis.minTime}s - ${analysis.maxTime}s"
                            )
                        }
                    }
                }
            }
        }
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
private fun EnhancedExtractionQualityCardPreview() {
    CoffeeShotTimerTheme {
        val sampleAnalysis = ExtractionTimeAnalysis(
            totalShots = PREVIEW_SAMPLE_TOTAL_SHOTS,
            avgTime = 27.3,
            minTime = PREVIEW_SAMPLE_MIN_TIME,
            maxTime = PREVIEW_SAMPLE_MAX_TIME,
            medianTime = 27.0,
            optimalTimePercentage = 73.0,
            tooFastPercentage = 12.0,
            tooSlowPercentage = 15.0,
            distribution = mapOf(
                "20-25s" to PREVIEW_DIST_20_25,
                "25-30s" to PREVIEW_DIST_25_30,
                "30-35s" to PREVIEW_DIST_30_35,
                "35s+" to PREVIEW_DIST_35_PLUS
            )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnhancedExtractionQualityCard(
                analysis = sampleAnalysis,
                modifier = Modifier
            )

            // Empty state
            EnhancedExtractionQualityCard(
                analysis = null,
                modifier = Modifier
            )
        }
    }
}
