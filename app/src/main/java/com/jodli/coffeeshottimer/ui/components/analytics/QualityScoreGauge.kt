@file:Suppress("MagicNumber") // Animation and arc drawing require literal numeric values

package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.ExtractionOptimal
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooFast
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooSlow
import kotlin.math.min

/**
 * Circular arc gauge displaying quality score with color-coded zones.
 *
 * Color zones:
 * - Red (0-59): Needs work
 * - Orange (60-84): Good
 * - Green (85-100): Excellent
 *
 * @param score Quality score from 0 to 100
 * @param tier Quality tier label (e.g., "Excellent", "Good", "Needs Work")
 * @param modifier Modifier for the gauge
 * @param size Gauge diameter (default adapts between 200-300dp)
 */
@Composable
fun QualityScoreGauge(
    score: Int,
    tier: String,
    modifier: Modifier = Modifier,
    size: Dp? = null
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "quality_score_animation"
    )

    val contentDescriptionText = "Quality score: $score out of 100, $tier"

    BoxWithConstraints(
        modifier = modifier.semantics { contentDescription = contentDescriptionText },
        contentAlignment = Alignment.Center
    ) {
        val gaugeSize = size ?: minOf(maxWidth.value, maxHeight.value).dp.coerceIn(200.dp, 300.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.size(gaugeSize)
        ) {
            // Circular gauge canvas
            Canvas(
                modifier = Modifier
                    .size(gaugeSize)
                    .weight(1f)
            ) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height
                val diameter = min(canvasWidth, canvasHeight)
                val strokeWidth = diameter * 0.12f // 12% of diameter
                val radius = (diameter - strokeWidth) / 2f
                val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                val arcSize = Size(radius * 2f, radius * 2f)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)

                // Background arc (light gray)
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Colored arc based on score
                val sweepAngle = (animatedScore / 100f) * 270f
                val arcColor = when {
                    score >= 85 -> ExtractionOptimal // Green
                    score >= 60 -> ExtractionTooFast // Orange
                    else -> ExtractionTooSlow // Red
                }

                drawArc(
                    color = arcColor,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center content with score and tier
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = score.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = tier,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun QualityScoreGaugePreview() {
    CoffeeShotTimerTheme {
        Column {
            QualityScoreGauge(
                score = 92,
                tier = "Excellent",
                modifier = Modifier
            )

            QualityScoreGauge(
                score = 75,
                tier = "Good",
                modifier = Modifier
            )

            QualityScoreGauge(
                score = 45,
                tier = "Needs Work",
                modifier = Modifier
            )
        }
    }
}
