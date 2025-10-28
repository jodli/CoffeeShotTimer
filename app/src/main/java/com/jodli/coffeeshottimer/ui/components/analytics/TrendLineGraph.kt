@file:Suppress("MagicNumber") // Canvas drawing coordinates and graph calculations require literal values

package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.ExtractionOptimal
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooFast
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooSlow
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Data point for trend line graph.
 *
 * @param date Date of the shot
 * @param score Quality score (0-100)
 */
data class TrendDataPoint(
    val date: LocalDate,
    val score: Int
)

/**
 * Simple line chart visualizing quality scores over time.
 *
 * Features:
 * - Color-coded dots: Green (>=85), Orange (60-84), Red (<60)
 * - Connected dots with line
 * - Y-axis: 0-100 scale with labels (0, 50, 100)
 * - X-axis: Date labels (MMM dd format)
 * - Touch interaction: Show value on tap
 * - Empty state for less than 5 shots
 *
 * @param dataPoints List of trend data points (last 10-30 quality scores)
 * @param modifier Modifier for the graph
 */
@Suppress("LongMethod") // Canvas drawing logic is inherently sequential and verbose
@Composable
fun TrendLineGraph(
    dataPoints: List<TrendDataPoint>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (dataPoints.size < 5) {
        // Empty state
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Record 5+ shots to see trends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val textMeasurer = rememberTextMeasurer()
        val textStyle = TextStyle(
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(spacing.medium)
                .pointerInput(dataPoints) {
                    // Touch interaction to show value
                    detectTapGestures { offset ->
                        val graphWidth = size.width - 80f // Account for Y-axis labels
                        val pointSpacing = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)
                        val touchX = offset.x - 60f // Offset for Y-axis labels

                        // Find nearest point
                        val index = ((touchX / pointSpacing).roundToInt())
                            .coerceIn(0, dataPoints.size - 1)

                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 60f
            val graphWidth = canvasWidth - padding - 20f
            val graphHeight = canvasHeight - padding

            val yAxisLabelWidth = 40f
            val graphLeft = yAxisLabelWidth + 20f
            val graphBottom = canvasHeight - 40f

            // Y-axis labels (0, 50, 100)
            listOf(0, 50, 100).forEach { value ->
                val y = graphBottom - (value / 100f) * graphHeight
                val text = value.toString()
                val textLayoutResult = textMeasurer.measure(text, textStyle)

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = yAxisLabelWidth - textLayoutResult.size.width - 8f,
                        y = y - textLayoutResult.size.height / 2f
                    )
                )
            }

            // Y-axis line
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(graphLeft, graphBottom),
                end = Offset(graphLeft, graphBottom - graphHeight),
                strokeWidth = 2f
            )

            // X-axis line
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(graphLeft, graphBottom),
                end = Offset(canvasWidth - 20f, graphBottom),
                strokeWidth = 2f
            )

            // Plot data points
            val pointSpacing = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)
            val path = Path()
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

            dataPoints.forEachIndexed { index, point ->
                val x = graphLeft + (index * pointSpacing)
                val y = graphBottom - (point.score / 100f) * graphHeight

                // Draw line connecting points
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw colored dot
                val dotColor = when {
                    point.score >= 85 -> ExtractionOptimal // Green
                    point.score >= 60 -> ExtractionTooFast // Orange
                    else -> ExtractionTooSlow // Red
                }

                drawCircle(
                    color = dotColor,
                    radius = 6f,
                    center = Offset(x, y)
                )

                // Highlight selected point
                if (selectedIndex == index) {
                    drawCircle(
                        color = dotColor,
                        radius = 10f,
                        center = Offset(x, y),
                        alpha = 0.3f
                    )
                }

                // X-axis date labels (show every nth label to avoid crowding)
                val labelInterval = (dataPoints.size / 5).coerceAtLeast(1)
                if (index % labelInterval == 0 || index == dataPoints.size - 1) {
                    val dateText = point.date.format(dateFormatter)
                    val dateTextLayout = textMeasurer.measure(dateText, textStyle)

                    drawText(
                        textLayoutResult = dateTextLayout,
                        topLeft = Offset(
                            x = x - dateTextLayout.size.width / 2f,
                            y = graphBottom + 8f
                        )
                    )
                }
            }

            // Draw line path
            val lineColor = androidx.compose.ui.graphics.Color(0xFFFF8C42).copy(alpha = 0.5f)
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2f)
            )

            // Draw selected value tooltip
            selectedIndex?.let { index ->
                val point = dataPoints[index]
                val x = graphLeft + (index * pointSpacing)
                val y = graphBottom - (point.score / 100f) * graphHeight
                val tooltipText = "Score: ${point.score}"
                val tooltipLayout = textMeasurer.measure(
                    tooltipText,
                    textStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                // Tooltip background
                val tooltipBgColor = androidx.compose.ui.graphics.Color(0xFFF5EDE4)
                drawRoundRect(
                    color = tooltipBgColor,
                    topLeft = Offset(
                        x = x - tooltipLayout.size.width / 2f - 8f,
                        y = y - tooltipLayout.size.height - 16f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = tooltipLayout.size.width + 16f,
                        height = tooltipLayout.size.height + 8f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // Tooltip text
                drawText(
                    textLayoutResult = tooltipLayout,
                    topLeft = Offset(
                        x = x - tooltipLayout.size.width / 2f,
                        y = y - tooltipLayout.size.height - 12f
                    )
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun TrendLineGraphPreview() {
    CoffeeShotTimerTheme {
        val sampleData = listOf(
            TrendDataPoint(LocalDate.now().minusDays(20), 65),
            TrendDataPoint(LocalDate.now().minusDays(18), 70),
            TrendDataPoint(LocalDate.now().minusDays(15), 75),
            TrendDataPoint(LocalDate.now().minusDays(12), 72),
            TrendDataPoint(LocalDate.now().minusDays(10), 80),
            TrendDataPoint(LocalDate.now().minusDays(7), 85),
            TrendDataPoint(LocalDate.now().minusDays(5), 82),
            TrendDataPoint(LocalDate.now().minusDays(3), 88),
            TrendDataPoint(LocalDate.now().minusDays(1), 90),
            TrendDataPoint(LocalDate.now(), 92)
        )

        TrendLineGraph(
            dataPoints = sampleData,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun TrendLineGraphEmptyPreview() {
    CoffeeShotTimerTheme {
        TrendLineGraph(
            dataPoints = emptyList(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
