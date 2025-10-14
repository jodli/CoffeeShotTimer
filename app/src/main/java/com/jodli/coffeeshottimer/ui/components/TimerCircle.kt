package com.jodli.coffeeshottimer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.ValidationUtils.Companion.OPTIMAL_EXTRACTION_TIME_MAX
import com.jodli.coffeeshottimer.ui.components.ValidationUtils.Companion.OPTIMAL_EXTRACTION_TIME_MIN
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.ExtractionIdle
import com.jodli.coffeeshottimer.ui.theme.ExtractionOptimal
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooFast
import com.jodli.coffeeshottimer.ui.theme.ExtractionTooSlow
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Automatic timer circle with color-coded extraction feedback and smooth animation.
 *
 * Features:
 * - Smooth millisecond-based ring animation when running (60 FPS)
 * - Color coding based on extraction time:
 *   - Gray (idle): 0 seconds - ready to start
 *   - Orange (too fast): < 25 seconds - under-extracted
 *   - Green (optimal): 25-30 seconds - perfect extraction
 *   - Red (too slow): > 30 seconds - over-extracted
 * - Visual feedback: paused state shows muted colors (50% opacity)
 * - Subtle pulsing animation on the progress arc when active
 * - Clean, centered seconds display
 *
 * @param size Size of the timer circle
 * @param fontSize Font size for the time display
 * @param isRunning Whether the timer is currently running
 * @param elapsedTimeMs Elapsed time in milliseconds (updates per second from parent)
 * @param onToggle Callback for start/pause toggle (single tap)
 * @param onReset Callback for timer reset (visible when paused with time)
 * @param modifier Modifier for the composable
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
    // Track smooth milliseconds for animation
    var smoothElapsedMs by remember { mutableLongStateOf(elapsedTimeMs) }
    var lastUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update smooth timer for running state
    LaunchedEffect(isRunning) {
        if (isRunning) {
            lastUpdateTime = System.currentTimeMillis()
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val delta = currentTime - lastUpdateTime
                smoothElapsedMs += delta
                lastUpdateTime = currentTime
                kotlinx.coroutines.delay(16) // ~60 FPS
            }
        }
    }

    // Sync with external timer when not running or reset
    LaunchedEffect(elapsedTimeMs, isRunning) {
        if (!isRunning) {
            smoothElapsedMs = elapsedTimeMs
        } else if (elapsedTimeMs == 0L) {
            smoothElapsedMs = 0L
        }
    }

    val displayMs = if (isRunning) smoothElapsedMs else elapsedTimeMs
    val elapsedSeconds = (displayMs / 1000f)

    // Color-coded based on extraction time and running state
    val borderColor = getExtractionColor(elapsedSeconds, isRunning)

    // Pulsing animation for running state - only affects the progress arc
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .pointerInput(isRunning, onToggle) {
                    detectTapGestures(
                        onTap = { onToggle() }
                    )
                },
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val strokeWidth = 24f
                        val radius = size.toPx() / 2f

                        // Draw background circle with consistent alpha
                        drawCircle(
                            color = borderColor.copy(alpha = 0.2f),
                            radius = radius - strokeWidth / 2,
                            style = Stroke(width = strokeWidth)
                        )

                        // Draw smooth progress arc if running or has time
                        if (displayMs > 0) {
                            // Use milliseconds for smooth animation (60 second cycle)
                            val sweepAngle = ((displayMs % 60000) / 60000f) * 360f

                            // Only apply pulse alpha when running, keep it subtle
                            val arcAlpha = if (isRunning) pulseAlpha else 0.9f

                            drawArc(
                                brush = Brush.sweepGradient(
                                    0f to borderColor.copy(alpha = 0.6f),
                                    0.5f to borderColor.copy(alpha = arcAlpha),
                                    1f to borderColor.copy(alpha = 0.6f)
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Time display - centered in the ring
                    Text(
                        text = stringResource(R.string.format_timer_seconds, elapsedSeconds.toInt()),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (displayMs > 0) borderColor else MaterialTheme.colorScheme.onSurface
                    )

                    // Reset button (only show when paused and has time)
                    if (!isRunning && displayMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onReset) {
                            Text(
                                text = stringResource(R.string.button_reset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Manual timer circle with circular slider for time input.
 *
 * Features:
 * - Circular slider with drag gesture handling
 * - Non-linear scale (20-40s range gets more arc space)
 * - Color coding based on extraction quality
 * - Haptic feedback on value changes
 * - Center tap to open precise text input dialog
 *
 * @param size Size of the timer circle
 * @param fontSize Font size for the time display
 * @param manualTimeSeconds Current manually set time in seconds
 * @param onTimeChange Callback when time is changed via slider
 * @param onTapToEdit Callback when user taps center to edit time
 * @param modifier Modifier for the composable
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
    var currentAngle by remember { mutableFloatStateOf(TimerScaleMapper.timeToAngle(manualTimeSeconds)) }

    // Track if user has performed haptic feedback recently (debounce)
    var lastHapticTime by remember { mutableLongStateOf(0L) }

    // Update angle when manualTimeSeconds changes externally (e.g., from dialog)
    LaunchedEffect(manualTimeSeconds) {
        currentAngle = TimerScaleMapper.timeToAngle(manualTimeSeconds)
    }

    // Color coding based on extraction quality (manual mode is always "running" for display)
    val borderColor = getExtractionColor(manualTimeSeconds.toFloat(), isRunning = true)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()

                        // Calculate angle from center
                        val centerX = size.toPx() / 2
                        val centerY = size.toPx() / 2
                        val touchX = change.position.x - centerX
                        val touchY = change.position.y - centerY

                        // Calculate angle in degrees (0° at top, clockwise)
                        val angle = (atan2(touchY, touchX) * 180f / PI.toFloat() + 90f + 360f) % 360f
                        currentAngle = angle

                        // Convert angle to time
                        val newTime = TimerScaleMapper.angleToTime(angle)

                        // Only trigger callback and haptics if time actually changed
                        if (newTime != manualTimeSeconds) {
                            onTimeChange(newTime)

                            // Debounced haptic feedback
                            val now = System.currentTimeMillis()
                            if (now - lastHapticTime > 100) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastHapticTime = now
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTapToEdit() }
                    )
                },
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val strokeWidth = 24f
                        val radius = size.toPx() / 2f

                        // Draw background circle
                        drawCircle(
                            color = borderColor.copy(alpha = 0.2f),
                            radius = radius - strokeWidth / 2,
                            style = Stroke(width = strokeWidth)
                        )

                        // Draw progress arc from top (-90°) to current angle
                        val sweepAngle = currentAngle
                        if (sweepAngle > 0) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0f to borderColor.copy(alpha = 0.6f),
                                    0.5f to borderColor.copy(alpha = 0.9f),
                                    1f to borderColor.copy(alpha = 0.6f)
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Time display - centered
                    Text(
                        text = stringResource(R.string.format_timer_seconds, manualTimeSeconds),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )

                    // Hint text with consistent spacing (matches Reset button in AutomaticTimerCircle)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onTapToEdit) {
                        Text(
                            text = stringResource(R.string.cd_tap_to_edit_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get the color for the timer based on extraction time.
 *
 * Color logic:
 * - 0 seconds: Gray (idle)
 * - < 25 seconds: Orange (too fast)
 * - 25-30 seconds: Green (optimal)
 * - > 30 seconds: Red (too slow)
 * - Paused: Muted version of the extraction color
 */
@Composable
private fun getExtractionColor(elapsedSeconds: Float, isRunning: Boolean): Color {
    val baseColor = when {
        elapsedSeconds == 0f -> ExtractionIdle // Gray - idle
        elapsedSeconds < OPTIMAL_EXTRACTION_TIME_MIN -> ExtractionTooFast // Orange - too fast
        elapsedSeconds <= OPTIMAL_EXTRACTION_TIME_MAX -> ExtractionOptimal // Green - optimal
        else -> ExtractionTooSlow // Red - too slow
    }

    // Apply muted/grayed effect when paused (except at 0 seconds)
    return if (!isRunning && elapsedSeconds > 0f) {
        baseColor.copy(alpha = 0.5f)
    } else {
        baseColor
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
        AutomaticTimerCircle(
            size = 280.dp,
            fontSize = 64.sp,
            isRunning = false,
            elapsedTimeMs = 0L,
            onToggle = {},
            onReset = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Too Fast (15s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_TooFast() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(
            size = 280.dp,
            fontSize = 64.sp,
            isRunning = true,
            elapsedTimeMs = 15000L,
            onToggle = {},
            onReset = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Optimal (25s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_Optimal() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(
            size = 280.dp,
            fontSize = 64.sp,
            isRunning = true,
            elapsedTimeMs = 25000L,
            onToggle = {},
            onReset = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Too Slow (35s)", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_TooSlow() {
    CoffeeShotTimerTheme {
        AutomaticTimerCircle(
            size = 280.dp,
            fontSize = 64.sp,
            isRunning = false,
            elapsedTimeMs = 35000L,
            onToggle = {},
            onReset = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Timer - Dark Mode", showBackground = true)
@Composable
private fun AutomaticTimerCirclePreview_Dark() {
    CoffeeShotTimerTheme(darkTheme = true) {
        AutomaticTimerCircle(
            size = 280.dp,
            fontSize = 64.sp,
            isRunning = true,
            elapsedTimeMs = 25000L,
            onToggle = {},
            onReset = {}
        )
    }
}
