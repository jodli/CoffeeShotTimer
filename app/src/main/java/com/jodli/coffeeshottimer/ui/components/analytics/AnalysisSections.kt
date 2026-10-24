@file:Suppress("MagicNumber") // Table weights and preview samples are the design

package com.jodli.coffeeshottimer.ui.components.analytics

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis
import com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import com.jodli.coffeeshottimer.domain.usecase.GrinderSettingAnalysis
import com.jodli.coffeeshottimer.domain.usecase.GrinderSettingStats
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import com.jodli.coffeeshottimer.ui.components.ChartShot
import com.jodli.coffeeshottimer.ui.components.DataCell
import com.jodli.coffeeshottimer.ui.components.RuledRow
import com.jodli.coffeeshottimer.ui.components.ScoreFigure
import com.jodli.coffeeshottimer.ui.components.SectionLabel
import com.jodli.coffeeshottimer.ui.components.ShotChart
import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.viewmodel.CoachingInsights
import kotlin.math.abs
import kotlin.math.roundToInt

/** Maximum shots plotted in the analysis chart. */
private const val ANALYSIS_CHART_SHOT_LIMIT = 30

/** Swatch size of the chart legend. */
private val LEGEND_SWATCH_SIZE = 10.dp

/** Coaching as ruled rows: recent pattern, progress. */
@Composable
fun CoachingSection(insights: CoachingInsights?, modifier: Modifier = Modifier) {
    if (insights == null) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = insights.beanName?.let { stringResource(R.string.coaching_insights_title_with_bean, it) }
                ?: stringResource(R.string.label_coaching)
        )
        CoachingRuledRow(
            label = stringResource(R.string.coaching_insights_section_recent),
            message = insights.recentTrend?.consistencyMessage
        )
        CoachingRuledRow(
            label = stringResource(R.string.coaching_insights_section_progress),
            message = insights.dialInStatus?.statusMessage
        )
    }
}

@Composable
private fun CoachingRuledRow(label: String, message: String?) {
    if (message == null) return
    RuledRow {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** The control chart of the recent shots, with the zone legend. */
@Composable
fun ShotChartSection(
    shots: List<Shot>,
    connect: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(R.string.label_recent_shots),
            trailing = {
                Text(
                    text = stringResource(R.string.format_shots_count, shots.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        ShotChart(
            shots = shots.take(ANALYSIS_CHART_SHOT_LIMIT)
                .map { ChartShot(seconds = it.extractionTimeSeconds, ratio = it.brewRatio) }
                .reversed(),
            connect = connect,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendEntry(stringResource(R.string.analytics_zone_too_fast), MaterialTheme.zoneColors.fastContainer)
            LegendEntry(stringResource(R.string.analytics_zone_optimal), MaterialTheme.zoneColors.idealContainer)
            LegendEntry(stringResource(R.string.analytics_zone_too_slow), MaterialTheme.zoneColors.slowContainer)
        }
    }
}

@Composable
private fun LegendEntry(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(LEGEND_SWATCH_SIZE)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Three headline figures: average score, share of in-window shots, typical ratio. */
@Composable
fun AnalysisFiguresRow(
    aggregate: AggregateQualityAnalysis?,
    extraction: ExtractionTimeAnalysis?,
    brewRatio: BrewRatioAnalysis?,
    modifier: Modifier = Modifier
) {
    RuledRow(modifier = modifier) {
        DataCell(
            label = stringResource(R.string.label_avg_score),
            modifier = Modifier.weight(1f)
        ) {
            if (aggregate != null) {
                ScoreFigure(
                    score = aggregate.overallQualityScore,
                    style = MaterialTheme.typography.headlineSmall,
                    swatch = LEGEND_SWATCH_SIZE
                )
            } else {
                FigureText(null)
            }
        }
        DataCell(
            label = stringResource(
                R.string.format_label_in_window,
                ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN,
                ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX
            ),
            modifier = Modifier.weight(1f)
        ) {
            FigureText(
                extraction?.let {
                    stringResource(R.string.format_percent, it.optimalTimePercentage)
                }
            )
        }
        DataCell(
            label = stringResource(R.string.label_typical_ratio),
            modifier = Modifier.weight(1f)
        ) {
            FigureText(
                brewRatio?.let {
                    stringResource(R.string.format_percent, it.typicalRatioPercentage)
                }
            )
        }
    }
}

@Composable
private fun FigureText(value: String?) {
    Text(
        text = value ?: "–",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Stable-range thresholds copied from the old trends card; a sentence only when they move. */
private const val RATIO_STABLE_THRESHOLD = 0.1
private const val TIME_STABLE_THRESHOLD = 2.0

/** Quality trend: one sentence plus the tier-lined graph. */
@Composable
fun QualityTrendSection(
    trends: ShotTrends?,
    points: List<TrendDataPoint>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.label_quality_trend))
        if (trends != null) {
            val ratioStable = abs(trends.brewRatioTrend) < RATIO_STABLE_THRESHOLD
            val timeStable = abs(trends.extractionTimeTrend) < TIME_STABLE_THRESHOLD
            val sentence = when {
                trends.isImproving -> stringResource(
                    R.string.analytics_trend_improving_format,
                    abs(trends.brewRatioTrend + trends.extractionTimeTrend).toInt()
                )
                ratioStable && timeStable -> stringResource(R.string.analytics_trend_stable_text)
                else -> stringResource(
                    R.string.analytics_trend_declining_format,
                    abs(trends.brewRatioTrend + trends.extractionTimeTrend).toInt()
                )
            }
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        TrendLineGraph(dataPoints = points, modifier = Modifier.fillMaxWidth())
    }
}

/** Grinder settings as a table: setting, shots, average time, in-window share. */
@Composable
fun GrinderSettingsTable(
    analysis: GrinderSettingAnalysis?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.label_grind_settings))
        if (analysis == null || analysis.settingStats.isEmpty()) {
            Text(
                text = stringResource(R.string.analytics_grinder_requires_bean),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        GrinderTableRow(
            cells = listOf(
                stringResource(R.string.label_grind_setting_col),
                stringResource(R.string.label_shots),
                stringResource(R.string.label_avg_time),
                stringResource(
                    R.string.format_label_in_window,
                    ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN,
                    ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX
                )
            ),
            header = true
        )
        val stats = analysis.settingStats.sortedBy { it.setting.toDoubleOrNull() ?: Double.MAX_VALUE }
        stats.forEach { setting ->
            GrinderTableRow(
                cells = listOf(
                    setting.setting,
                    setting.shotCount.toString(),
                    "${setting.avgExtractionTime.roundToInt()}s",
                    stringResource(R.string.format_percent, setting.optimalExtractionPercentage)
                ),
                bestLabel = if (setting == analysis.bestPerformingSetting) {
                    stringResource(R.string.label_best)
                } else {
                    null
                },
                mostUsedLabel = if (setting == analysis.mostUsedSetting) {
                    stringResource(R.string.label_most_used)
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun GrinderTableRow(
    cells: List<String>,
    header: Boolean = false,
    bestLabel: String? = null,
    mostUsedLabel: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (header) cells[0].uppercase() else cells[0],
                style = if (header) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (header) FontWeight.Normal else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (bestLabel != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bestLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.zoneColors.idealInk
                )
            }
            if (mostUsedLabel != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mostUsedLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        cells.drop(1).forEach { cell ->
            Text(
                text = if (header) cell.uppercase() else cell,
                style = if (header) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Analysis sections")
@Composable
fun AnalysisSectionsPreview() {
    CoffeeShotTimerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CoachingSection(null)
            GrinderSettingsTable(
                GrinderSettingAnalysis(
                    totalSettings = 1,
                    settingStats = listOf(
                        GrinderSettingStats("2.6", 4, 2.1, 27.0, 75.0)
                    ),
                    mostUsedSetting = GrinderSettingStats("2.6", 4, 2.1, 27.0, 75.0),
                    bestPerformingSetting = GrinderSettingStats("2.6", 4, 2.1, 27.0, 75.0)
                )
            )
        }
    }
}
