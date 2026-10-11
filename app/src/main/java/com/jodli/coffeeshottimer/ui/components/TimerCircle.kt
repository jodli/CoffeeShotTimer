package com.jodli.coffeeshottimer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.ExtractionZone
import com.jodli.coffeeshottimer.ui.theme.extractionZone
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val WINDOW_START = ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN
private const val WINDOW_END = ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX
private const val TICK_EVERY_S = 5
private const val LABEL_EVERY_S = 10
private const val FRAME_MS = 16L
private const val MS_PER_SECOND = 1000L
private val MANUAL_RANGE =
    ValidationUtils.MANUAL_TIMER_MIN_SECONDS.toFloat()..ValidationUtils.MANUAL_TIMER_MAX_SECONDS.toFloat()

// Dial proportions, matched to the app icon (scripts/generate-icons.py)
private const val RING_OF_SIDE = 0.062f
private const val BAND_OF_RING = 1.8f
private const val HEAD_OF_BAND = 0.5f
private const val GRABBER_OF_BAND = 0.7f
private const val GRABBER_CORE_OF_BAND = 0.22f
private const val BAND_TINT_ALPHA = 0.32f
private const val LABEL_SPACE_DP = 22f
private const val TICK_LENGTH_DP = 7f
private const val TICK_GAP_DP = 5f
private const val TICK_WIDTH_DP = 1.5f
private const val HEAD_RING_DP = 2.5f
private const val LABEL_NUDGE_DP = 2f
private const val DEGREES_PER_HALF_TURN = 180f

/**
 * Automatic timer: tap the dial to start and pause.
 *
 * The dial is the app icon's: a 45 s ring with the 25-30 s target band. The arc runs in ink
 * until the shot reaches the band, fills the band green while the shot is inside it, and turns
 * rust past 30 s. The head is the shot dot from the website chart, solid ink while running.
 */
@Composable
fun AutomaticTimerCircle(
    size: Dp,
    fontSize: TextUnit,
    isRunning: Boolean,
    elapsedTimeMs: Long,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The parent ticks once per second; interpolate locally so the arc moves smoothly.
    var smoothElapsedMs by remember { mutableLongStateOf(elapsedTimeMs) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            var last = System.currentTimeMillis()
            while (true) {
                val now = System.currentTimeMillis()
                smoothElapsedMs += now - last
                last = now
                kotlinx.coroutines.delay(FRAME_MS)
            }
        }
    }
    LaunchedEffect(elapsedTimeMs, isRunning) {
        if (!isRunning || elapsedTimeMs == 0L) smoothElapsedMs = elapsedTimeMs
    }

    val displayMs = if (isRunning) smoothElapsedMs else elapsedTimeMs
    val wholeSeconds = (displayMs / MS_PER_SECOND).toInt()
    val timeText = stringResource(R.string.format_timer_seconds, wholeSeconds)

    TimerDial(
        size = size,
        secondsProvider = { displayMs / MS_PER_SECOND.toFloat() },
        head = when {
            displayMs == 0L -> HeadStyle.NONE
            isRunning -> HeadStyle.RUNNING
            else -> HeadStyle.STOPPED
        },
        modifier = modifier
            .semantics {
                role = Role.Button
                stateDescription = timeText
            }
            .pointerInput(onToggle) { detectTapGestures(onTap = { onToggle() }) }
    ) {
        TimeReadout(seconds = wholeSeconds, text = timeText, fontSize = fontSize)
        // Reserve the reset row's height so the numeral doesn't jump when it appears
        Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
            if (!isRunning && displayMs > 0) {
                TextButton(onClick = onReset) {
                    Text(
                        text = stringResource(R.string.button_reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Manual timer: drag round the dial to set the time, tap the centre for exact entry.
 *
 * Same dial and scale as the automatic timer. Dragging covers 5-45 s; longer times
 * (up to the manual maximum) come from the entry dialog and show as a full ring.
 */
@Composable
fun ManualTimerCircle(
    size: Dp,
    fontSize: TextUnit,
    manualTimeSeconds: Int,
    onTimeChange: (Int) -> Unit,
    onTapToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val currentTime by rememberUpdatedState(manualTimeSeconds)
    val currentOnTimeChange by rememberUpdatedState(onTimeChange)
    val timeText = stringResource(R.string.format_timer_seconds, manualTimeSeconds)

    TimerDial(
        size = size,
        secondsProvider = { manualTimeSeconds.toFloat() },
        head = HeadStyle.GRABBER,
        modifier = modifier
            .semantics {
                stateDescription = timeText
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = manualTimeSeconds.toFloat(),
                    range = MANUAL_RANGE,
                    steps = ValidationUtils.MANUAL_TIMER_MAX_SECONDS - ValidationUtils.MANUAL_TIMER_MIN_SECONDS - 1
                )
                setProgress { target ->
                    currentOnTimeChange(
                        target.roundToInt().coerceIn(
                            ValidationUtils.MANUAL_TIMER_MIN_SECONDS,
                            ValidationUtils.MANUAL_TIMER_MAX_SECONDS
                        )
                    )
                    true
                }
            }
            .pointerInput(Unit) {
                var dragTime = currentTime
                detectDragGestures(
                    onDragStart = { dragTime = currentTime },
                    onDrag = { change, _ ->
                        change.consume()
                        val dx = change.position.x - this.size.width / 2f
                        val dy = change.position.y - this.size.height / 2f
                        val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 90f + 360f) % 360f
                        val next = TimerScaleMapper.dragToTime(angle, dragTime)
                        if (next != dragTime) {
                            // A firmer tick on the band edges, so the window can be felt without looking
                            val crossedEdge = next == WINDOW_START || next == WINDOW_END
                            view.performHapticFeedback(
                                if (crossedEdge) {
                                    HapticFeedbackConstants.CONTEXT_CLICK
                                } else {
                                    HapticFeedbackConstants.CLOCK_TICK
                                }
                            )
                            dragTime = next
                            currentOnTimeChange(next)
                        }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTapToEdit() }) }
    ) {
        TimeReadout(seconds = manualTimeSeconds, text = timeText, fontSize = fontSize)
        Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = onTapToEdit) {
                Text(
                    text = stringResource(R.string.cd_tap_to_edit_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class HeadStyle { NONE, RUNNING, STOPPED, GRABBER }

/** The readout takes the zone's colour only once the shot is in or past the window. */
@Composable
private fun TimeReadout(seconds: Int, text: String, fontSize: TextUnit) {
    val zones = MaterialTheme.zoneColors
    val color = when {
        seconds == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        extractionZone(seconds) == ExtractionZone.FAST -> MaterialTheme.colorScheme.onSurface
        else -> zones.ink(extractionZone(seconds))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize, lineHeight = fontSize),
        color = color
    )
}

@Composable
private fun TimerDial(
    size: Dp,
    secondsProvider: () -> Float,
    head: HeadStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DialColors(
        track = MaterialTheme.colorScheme.outlineVariant,
        ink = MaterialTheme.colorScheme.onSurface,
        tick = MaterialTheme.colorScheme.onSurfaceVariant,
        paper = MaterialTheme.colorScheme.surface,
        // the target has to read before the shot starts, on the card ground as well as on paper
        bandTint = MaterialTheme.zoneColors.ideal.copy(alpha = BAND_TINT_ALPHA),
        band = MaterialTheme.zoneColors.ideal,
        slow = MaterialTheme.zoneColors.slow
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.tick)
    val labels = remember(measurer, labelStyle) {
        (0 until TimerScaleMapper.DIAL_SECONDS step LABEL_EVERY_S).map { it to measurer.measure("$it", labelStyle) }
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val g = DialGeometry(this.size.minDimension, density)
                drawDial(g, colors, secondsProvider())
                // axis numerals outside the ticks, like the website chart's time axis
                labels.forEach { (s, layout) ->
                    val p = g.point(s.toFloat(), g.labelRadius)
                    drawText(
                        layout,
                        topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f)
                    )
                }
                drawHead(g, colors, secondsProvider(), head)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp)) // balances the action row below the numeral
            content()
        }
    }
}

private class DialColors(
    val track: Color,
    val ink: Color,
    val tick: Color,
    val paper: Color,
    val bandTint: Color,
    val band: Color,
    val slow: Color
)

/** Proportions follow the app icon: ring, wider band, ticks, then numerals outside. */
private class DialGeometry(side: Float, density: Float) {
    val center = Offset(side / 2f, side / 2f)
    val ringWidth = side * RING_OF_SIDE
    val bandWidth = ringWidth * BAND_OF_RING
    private val labelSpace = LABEL_SPACE_DP * density
    private val tickLength = TICK_LENGTH_DP * density
    private val tickGap = TICK_GAP_DP * density
    val tickWidth = TICK_WIDTH_DP * density
    val headRing = HEAD_RING_DP * density
    val radius = side / 2f - labelSpace - tickLength - tickGap - bandWidth / 2f
    val tickInner = radius + bandWidth / 2f + tickGap
    val tickOuter = tickInner + tickLength
    val labelRadius = tickOuter + labelSpace / 2f + LABEL_NUDGE_DP * density

    fun point(seconds: Float, r: Float = radius): Offset {
        val a = TimerScaleMapper.timeToAngle(seconds) * PI.toFloat() / DEGREES_PER_HALF_TURN
        return Offset(center.x + r * sin(a), center.y - r * cos(a))
    }
}

private fun DrawScope.arc(g: DialGeometry, from: Float, to: Float, color: Color, width: Float) {
    if (to <= from) return
    val start = TimerScaleMapper.timeToAngle(from)
    val sweep = TimerScaleMapper.timeToAngle(to) - start
    drawArc(
        color = color,
        startAngle = start - 90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(g.center.x - g.radius, g.center.y - g.radius),
        size = Size(g.radius * 2, g.radius * 2),
        style = Stroke(width = width)
    )
}

private fun DrawScope.drawDial(g: DialGeometry, c: DialColors, seconds: Float) {
    val dial = TimerScaleMapper.DIAL_SECONDS.toFloat()
    val t = seconds.coerceIn(0f, dial)
    val start = WINDOW_START.toFloat()
    val end = WINDOW_END.toFloat()

    drawCircle(color = c.track, radius = g.radius, center = g.center, style = Stroke(width = g.ringWidth))
    arc(g, start, end, c.bandTint, g.bandWidth)
    arc(g, 0f, minOf(t, start), c.ink, g.ringWidth)
    arc(g, start, minOf(t, end), c.band, g.bandWidth)
    arc(g, end, t, c.slow, g.ringWidth)

    for (s in 0 until TimerScaleMapper.DIAL_SECONDS step TICK_EVERY_S) {
        drawLine(
            color = c.tick,
            start = g.point(s.toFloat(), g.tickInner),
            end = g.point(s.toFloat(), g.tickOuter),
            strokeWidth = g.tickWidth
        )
    }
}

private fun DrawScope.drawHead(g: DialGeometry, c: DialColors, seconds: Float, head: HeadStyle) {
    if (head == HeadStyle.NONE) return
    val p = g.point(seconds)
    when (head) {
        HeadStyle.RUNNING -> drawCircle(c.ink, radius = g.bandWidth * HEAD_OF_BAND, center = p)
        HeadStyle.STOPPED -> {
            drawCircle(c.paper, radius = g.bandWidth * HEAD_OF_BAND, center = p)
            drawCircle(c.ink, radius = g.bandWidth * HEAD_OF_BAND, center = p, style = Stroke(g.headRing))
        }
        // bigger, and solid with a paper core so it reads as something to grab
        HeadStyle.GRABBER -> {
            drawCircle(c.ink, radius = g.bandWidth * GRABBER_OF_BAND, center = p)
            drawCircle(c.paper, radius = g.bandWidth * GRABBER_CORE_OF_BAND, center = p)
        }
        HeadStyle.NONE -> Unit
    }
}

// ============================================================================
// PREVIEW COMPOSABLES
// ============================================================================

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Idle (0s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_Idle() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(280.dp, 64.sp, isRunning = false, elapsedTimeMs = 0L, onToggle = {}, onReset = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Running, before the window (15s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_TooFast() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(280.dp, 64.sp, isRunning = true, elapsedTimeMs = 15000L, onToggle = {}, onReset = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Stopped in the window (27s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_Optimal() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(280.dp, 64.sp, isRunning = false, elapsedTimeMs = 27000L, onToggle = {}, onReset = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Past the window (36s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_TooSlow() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(280.dp, 64.sp, isRunning = true, elapsedTimeMs = 36000L, onToggle = {}, onReset = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Manual timer - Dark", showBackground = true, backgroundColor = 0xFF10161D)
@Composable
private fun ManualTimerCirclePreview_Dark() {
    CoffeeShotTimerTheme(darkTheme = true) {
        ManualTimerCircle(280.dp, 64.sp, manualTimeSeconds = 28, onTimeChange = {}, onTapToEdit = {})
    }
}
