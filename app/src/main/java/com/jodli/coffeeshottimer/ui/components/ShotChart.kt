@file:Suppress("MagicNumber") // Chart geometry and tick values are the design

package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import kotlin.math.roundToInt

/** Chart bounds; x is seconds, y is brew ratio (higher is further up). */
private const val X_MIN_SECONDS = 10
private const val X_MAX_SECONDS = 45
private const val Y_MIN_RATIO = 1.5
private const val Y_MAX_RATIO = 3.0
private const val X_TICK_SECONDS = 5
private const val Y_TICK_RATIO = 0.25

/**
 * The control chart: extraction time against brew ratio with the sour and
 * bitter zones and the 25–30 s × 1:2.0–2.5 target box. Shots are dots; with
 * [connect] a dashed trajectory joins them chronologically (used when the
 * history is filtered to one bean, so the line is one dial-in run).
 */
@Composable
fun ShotChart(
    shots: List<ChartShot>,
    connect: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.zoneColors
    val tickStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val numberStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurface)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface

    val description = stringResource(
        R.string.cd_shot_chart,
        shots.size,
        shots.count { isInTarget(it) }
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
    ) {
        val plot = plotRect()
        drawZones(colors.fastContainer, colors.slowContainer, colors.idealContainer, colors.ideal, plot)
        drawGrid(gridColor, plot)
        drawAxes(onSurface, tickStyle, textMeasurer, plot)
        if (connect && shots.size > 1) {
            drawTrajectory(shots, onSurfaceVariant, plot)
        }
        drawShots(shots, surface, onSurface, colors.ideal, numberStyle, textMeasurer, plot)
    }
}

private fun DrawScope.plotRect(): Rect = Rect(
    left = 36.dp.toPx(),
    top = 8.dp.toPx(),
    right = size.width - 8.dp.toPx(),
    bottom = size.height - 24.dp.toPx()
)

private fun isInTarget(shot: ChartShot): Boolean =
    shot.seconds >= ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN &&
        shot.seconds <= ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX &&
        shot.ratio >= ValidationUtils.OPTIMAL_BREW_RATIO_MIN &&
        shot.ratio <= ValidationUtils.OPTIMAL_BREW_RATIO_MAX

/** Maps a brew ratio to its y position: higher ratios plot further up. */
private fun ratioY(ratio: Double, plot: Rect): Float =
    ((ratio - Y_MIN_RATIO) / (Y_MAX_RATIO - Y_MIN_RATIO)).toFloat() * plot.height

/** Maps extraction seconds to its x position. */
private fun secondsX(seconds: Int, plot: Rect): Float =
    plot.left + (seconds - X_MIN_SECONDS) / (X_MAX_SECONDS - X_MIN_SECONDS).toFloat() * plot.width

private fun ChartShot.position(plot: Rect): Offset {
    val raw = Offset(
        x = secondsX(seconds, plot),
        y = plot.bottom - ratioY(ratio, plot)
    )
    return Offset(raw.x.coerceIn(plot.left, plot.right), raw.y.coerceIn(plot.top, plot.bottom))
}

private fun DrawScope.drawZones(
    fastContainer: Color,
    slowContainer: Color,
    idealContainer: Color,
    ideal: Color,
    plot: Rect
) {
    val targetLeft = secondsX(ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN, plot)
    val targetRight = secondsX(ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX, plot)
    // Sour zone: left of the target window
    drawRect(
        color = fastContainer,
        topLeft = Offset(plot.left, plot.top),
        size = Size(targetLeft - plot.left, plot.height)
    )
    // Bitter zone: right of the target window
    drawRect(
        color = slowContainer,
        topLeft = Offset(targetRight, plot.top),
        size = Size(plot.right - targetRight, plot.height)
    )
    // Target box
    val boxTop = plot.bottom - ratioY(ValidationUtils.OPTIMAL_BREW_RATIO_MAX, plot)
    val boxBottom = plot.bottom - ratioY(ValidationUtils.OPTIMAL_BREW_RATIO_MIN, plot)
    drawRect(
        idealContainer,
        topLeft = Offset(targetLeft, boxTop),
        size = Size(targetRight - targetLeft, boxBottom - boxTop)
    )
    drawRect(
        ideal,
        topLeft = Offset(targetLeft, boxTop),
        size = Size(targetRight - targetLeft, boxBottom - boxTop),
        style = Stroke(width = 2.5.dp.toPx())
    )
}

private fun DrawScope.drawGrid(gridColor: Color, plot: Rect) {
    var seconds = X_MIN_SECONDS + X_TICK_SECONDS
    while (seconds < X_MAX_SECONDS) {
        val x = secondsX(seconds, plot)
        drawLine(gridColor, Offset(x, plot.top), Offset(x, plot.bottom), strokeWidth = 1.dp.toPx())
        seconds += X_TICK_SECONDS
    }
    var ratio = Y_MIN_RATIO + Y_TICK_RATIO
    while (ratio < Y_MAX_RATIO - 1e-9) {
        val y = plot.bottom - ratioY(ratio, plot)
        drawLine(gridColor, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1.dp.toPx())
        ratio += Y_TICK_RATIO
    }
}

private fun DrawScope.drawAxes(
    axisColor: Color,
    tickStyle: TextStyle,
    textMeasurer: TextMeasurer,
    plot: Rect
) {
    drawLine(axisColor, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), strokeWidth = 1.5.dp.toPx())
    drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), strokeWidth = 1.5.dp.toPx())

    for (seconds in listOf(10, 20, 30, 40)) {
        val x = secondsX(seconds, plot)
        val layout = textMeasurer.measure("$seconds", tickStyle, maxLines = 1)
        drawText(layout, topLeft = Offset(x - layout.size.width / 2f, plot.bottom + 4.dp.toPx()))
    }
    for (ratio in listOf(Y_MIN_RATIO, 2.0, 2.5, Y_MAX_RATIO)) {
        val y = plot.bottom - ratioY(ratio, plot)
        val label = "1:${if (ratio % 1.0 == 0.0) ratio.roundToInt().toString() else ratio.toString()}"
        val layout = textMeasurer.measure(label, tickStyle, maxLines = 1)
        drawText(layout, topLeft = Offset(plot.left - 4.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
    }
}

private fun DrawScope.drawTrajectory(shots: List<ChartShot>, color: Color, plot: Rect) {
    val path = Path()
    shots.forEachIndexed { index, shot ->
        val point = shot.position(plot)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        )
    )
}

private fun DrawScope.drawShots(
    shots: List<ChartShot>,
    paper: Color,
    ink: Color,
    ideal: Color,
    numberStyle: TextStyle,
    textMeasurer: TextMeasurer,
    plot: Rect
) {
    shots.forEachIndexed { index, shot ->
        val center = shot.position(plot)
        val isLast = index == shots.lastIndex
        val fill = if (isInTarget(shot)) ideal else paper
        drawCircle(fill, radius = 6.dp.toPx(), center = center)
        drawCircle(ink, radius = 6.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
        if (isLast) {
            drawCircle(ink, radius = 8.dp.toPx(), center = center)
        }
        shot.number?.let { number ->
            val layout = textMeasurer.measure(number.toString(), numberStyle, maxLines = 1)
            drawText(
                layout,
                color = if (isLast) paper else ink,
                topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - layout.size.height / 2f
                )
            )
        }
    }
}
