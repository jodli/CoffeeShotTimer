package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis
import com.jodli.coffeeshottimer.domain.usecase.QualityTier
import com.jodli.coffeeshottimer.domain.usecase.TrendDirection
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Dashboard card displaying aggregate quality score with gauge and distribution.
 *
 * Layout:
 * - QualityScoreGauge (circular gauge visual)
 * - Distribution text: "X excellent • Y good • Z needs work"
 *
 * @param analysis Aggregate quality analysis data, null shows empty state
 * @param modifier Modifier for the card
 */
@Composable
fun QualityScoreGaugeCard(
    analysis: AggregateQualityAnalysis?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        if (analysis == null || analysis.totalShots == 0) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = "Record your first shot to see quality score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Quality score gauge
                QualityScoreGauge(
                    score = analysis.overallQualityScore,
                    tier = getTierDisplayText(analysis.qualityTier),
                    modifier = Modifier
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Distribution chips text
                Text(
                    text = "${analysis.excellentCount} excellent • " +
                            "${analysis.goodCount} good • " +
                            "${analysis.needsWorkCount} needs work",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Convert quality tier enum to display text.
 */
private fun getTierDisplayText(tier: QualityTier): String {
    return when (tier) {
        QualityTier.EXCELLENT -> "Excellent"
        QualityTier.GOOD -> "Good"
        QualityTier.NEEDS_WORK -> "Needs Work"
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun QualityScoreGaugeCardPreview() {
    CoffeeShotTimerTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // With data
            QualityScoreGaugeCard(
                analysis = AggregateQualityAnalysis(
                    totalShots = 50,
                    overallQualityScore = 82,
                    qualityTier = QualityTier.GOOD,
                    excellentCount = 15,
                    goodCount = 28,
                    needsWorkCount = 7,
                    trendDirection = TrendDirection.IMPROVING,
                    recentAverage = 85,
                    overallAverage = 80,
                    improvementRate = 6.25,
                    consistencyScore = 78
                ),
                modifier = Modifier
            )

            // Empty state
            QualityScoreGaugeCard(
                analysis = null,
                modifier = Modifier
            )
        }
    }
}
