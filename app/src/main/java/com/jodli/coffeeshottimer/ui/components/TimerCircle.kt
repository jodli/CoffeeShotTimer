package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.*

/**
 * Automatic timer circle with color-coded extraction feedback.
 * 
 * Color coding based on extraction time:
 * - Gray (idle): 0 seconds - ready to start
 * - Orange (too fast): < 20 seconds - under-extracted
 * - Green (optimal): 20-30 seconds - perfect extraction
 * - Red (too slow): > 30 seconds - over-extracted
 * 
 * @param size Size of the timer circle
 * @param fontSize Font size for the time display
 * @param isRunning Whether the timer is currently running
 * @param elapsedTimeMs Elapsed time in milliseconds
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
    val elapsedSeconds = (elapsedTimeMs / 1000f)
    
    // Color-coded based on extraction time
    val borderColor = getExtractionColor(elapsedSeconds)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
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
                        
                        // Draw background circle
                        drawCircle(
                            color = borderColor.copy(alpha = 0.2f),
                            radius = radius - strokeWidth / 2,
                            style = Stroke(width = strokeWidth)
                        )
                        
                        // Draw progress arc if running or has time
                        if (elapsedTimeMs > 0) {
                            val sweepAngle = ((elapsedSeconds % 60) / 60f) * 360f
                            
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0f to borderColor.copy(alpha = 0.7f),
                                    0.5f to borderColor,
                                    1f to borderColor.copy(alpha = 0.7f)
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
                    // Time display - seconds only
                    Text(
                        text = stringResource(R.string.format_timer_seconds, elapsedSeconds.toInt()),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (elapsedTimeMs > 0) borderColor else MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Reset button (only show when paused and has time)
                    if (!isRunning && elapsedTimeMs > 0) {
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
 * Get the color for the timer based on extraction time.
 * 
 * Color logic:
 * - 0 seconds: Gray (idle)
 * - < 20 seconds: Orange (too fast)
 * - 20-30 seconds: Green (optimal)
 * - > 30 seconds: Red (too slow)
 */
@Composable
private fun getExtractionColor(elapsedSeconds: Float): Color {
    return when {
        elapsedSeconds == 0f -> ExtractionIdle // Gray - idle
        elapsedSeconds < 20f -> ExtractionTooFast // Orange - too fast
        elapsedSeconds <= 30f -> ExtractionOptimal // Green - optimal
        else -> ExtractionTooSlow // Red - too slow
    }
}

// ============================================================================
// PREVIEW COMPOSABLES
// ============================================================================

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
