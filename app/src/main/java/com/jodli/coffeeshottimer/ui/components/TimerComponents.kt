package com.jodli.coffeeshottimer.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ripple
import androidx.compose.ui.res.painterResource
import com.jodli.coffeeshottimer.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Circular timer display with progress indicator and color coding
 *
 * Enhanced version that makes the entire timer component clickable for start/stop,
 * dramatically improving usability with a large touch target (~200dp vs 80dp)
 */
@Composable
fun CircularTimer(
    currentTime: Long,
    targetTime: Long?,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showColorCoding: Boolean = true,
    onStartStop: (() -> Unit)? = null // New parameter for clickable timer
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val progress = if (targetTime != null && targetTime > 0) {
        (currentTime.toFloat() / targetTime.toFloat()).coerceIn(0f, 1f)
    } else if (showColorCoding) {
        // For color-coded timer, fill progressively up to 35 seconds (max optimal time)
        val maxOptimalTimeMs = 35 * 1000L
        (currentTime.toFloat() / maxOptimalTimeMs.toFloat()).coerceIn(0f, 1f)
    } else {
        // Fallback for non-color-coded timers
        if (currentTime > 0) 1f else 0f
    }

    // Cache elapsed seconds calculation for performance
    val elapsedSeconds = remember(currentTime) { (currentTime / 1000).toInt() }

    // Get color based on extraction time quality
    val timerColor = if (showColorCoding) {
        getExtractionTimeColor(elapsedSeconds, isRunning)
    } else {
        if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800)
    }

    // Animation for the progress indicator
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "timer_progress"
    )

    // Animation for color transitions
    val animatedColor by animateColorAsState(
        targetValue = timerColor,
        animationSpec = tween(durationMillis = 500),
        label = "timer_color"
    )

    // Clickable timer state for enhanced UX
    var isPressed by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debounceDelayMs = 300L

    // Scale animation for entire timer when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onStartStop != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "timer_scale"
    )

    // Click handler for the entire timer
    val handleTimerClick = onStartStop?.let { onClick ->
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceDelayMs) {
                lastClickTime = currentTime

                // Haptic feedback for timer interaction
                triggerHapticFeedback(context, !isRunning)

                // Visual press feedback
                isPressed = true
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    isPressed = false
                }

                onClick()
            }
        }
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(
                if (handleTimerClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 100.dp),
                        onClick = handleTimerClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = androidx.compose.ui.geometry.Offset(
                size.width / 2,
                size.height / 2
            )

            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc with color coding
            if (animatedProgress > 0f) {
                drawArc(
                    color = animatedColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }

        // Timer content with enhanced layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer text with enhanced formatting
            Text(
                text = formatExtractionTime(currentTime),
                fontSize = 28.sp, // Slightly larger for better visibility
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Show extraction quality indicator or play/stop hint
            if (showColorCoding && isRunning) {
                val qualityText = when (getExtractionQuality(elapsedSeconds, isRunning)) {
                    ExtractionQuality.UNDER_EXTRACTED -> "Under-extracted"
                    ExtractionQuality.OPTIMAL -> "Optimal range"
                    ExtractionQuality.OVER_EXTRACTED -> "Over-extracted"
                    ExtractionQuality.NEUTRAL -> ""
                }

                if (qualityText.isNotEmpty()) {
                    Text(
                        text = qualityText,
                        fontSize = 12.sp,
                        color = animatedColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (onStartStop != null) {
                // Show interaction hint for clickable timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = animatedColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "Tap to stop" else "Tap to start",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (targetTime != null) {
                // Show target time for non-color-coded timers
                Text(
                    text = "/ ${formatExtractionTime(targetTime)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Optional visual indicator for clickable state
        if (onStartStop != null && currentTime == 0L) {
            // Subtle pulse animation when timer is ready to start
            val pulseAlpha by animateFloatAsState(
                targetValue = if (isRunning) 0f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_animation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        animatedColor.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Timer control buttons with multiple interaction modes
 *
 * Supports:
 * - Enhanced separate button controls (useEnhanced = true, useClickableTimer = false)
 * - Clickable timer with repositioned reset (useClickableTimer = true) - RECOMMENDED
 * - Legacy controls (useEnhanced = false, useClickableTimer = false)
 */
@Composable
fun TimerControls(
    isRunning: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    showReset: Boolean = true,
    useEnhanced: Boolean = true,
    useClickableTimer: Boolean = true, // NEW: Use entire timer as button
    currentTime: Long = 0L, // Required for clickable timer mode
    targetTime: Long? = null,
    showColorCoding: Boolean = true
) {
    if (useClickableTimer) {
        // New clickable timer approach - MOST USER FRIENDLY
        ClickableTimerControls(
            isRunning = isRunning,
            currentTime = currentTime,
            targetTime = targetTime,
            onStartStop = onStartPause,
            onReset = onReset,
            modifier = modifier,
            showReset = showReset,
            showColorCoding = showColorCoding
        )
    } else if (useEnhanced) {
        // Enhanced separate button controls
        EnhancedTimerControls(
            isRunning = isRunning,
            onStartStop = onStartPause,
            onReset = onReset,
            modifier = modifier,
            showReset = showReset
        )
    } else {
        // Legacy implementation
        val spacing = LocalSpacing.current

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start/Pause button (legacy style)
            FloatingActionButton(
                onClick = onStartPause,
                modifier = Modifier.size(56.dp),
                containerColor = if (isRunning)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.primary
            ) {
                if (isRunning) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pause),
                        contentDescription = "Pause",
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Reset button (optional)
            if (showReset) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

/**
 * Enhanced timer controls with separate prominent button (previous implementation)
 *
 * Maintained for backward compatibility - use ClickableTimerControls for better UX
 */
@Composable
fun EnhancedTimerControls(
    isRunning: Boolean,
    onStartStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    showReset: Boolean = true,
    enabled: Boolean = true
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enhanced prominent timer button
        EnhancedTimerButton(
            isRunning = isRunning,
            onStartStop = onStartStop,
            enabled = enabled
        )

        // Reset button (optional) - styled to complement the enhanced timer button
        if (showReset) {
            OutlinedButton(
                onClick = {
                    // Light haptic feedback for reset
                    triggerHapticFeedback(context, false)
                    onReset()
                },
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 80.dp),
                enabled = enabled,
                shape = CircleShape,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp
                )
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Compact timer display for cards and lists
 */
@Composable
fun CompactTimer(
    currentTime: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
    showColorCoding: Boolean = true
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        if (showStatus) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .then(
                        if (isRunning) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
            )
        }

        Text(
            text = formatExtractionTime(currentTime),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (showColorCoding)
                getTimerColor(currentTime, isRunning)
            else if (isRunning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Timer statistics display
 */
@Composable
fun TimerStats(
    averageTime: Long?,
    bestTime: Long?,
    totalShots: Int,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Total Shots",
                value = totalShots.toString()
            )

            StatItem(
                label = "Average",
                value = averageTime?.let { formatTime(it) } ?: "--"
            )

            StatItem(
                label = "Best Time",
                value = bestTime?.let { formatTime(it) } ?: "--"
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    val spacing = LocalSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(spacing.extraSmall))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Format time in milliseconds to MM:SS format
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Extraction quality levels based on timing
 */
enum class ExtractionQuality {
    UNDER_EXTRACTED, // < 20s - Yellow
    OPTIMAL,         // 20-35s - Green
    OVER_EXTRACTED,  // > 35s - Red
    NEUTRAL          // Not running - Gray
}

/**
 * Enhanced time formatting for espresso extraction times.
 * Shows seconds only for times under 60 seconds, MM:SS for longer times.
 */
fun formatExtractionTime(timeMs: Long): String {
    val totalSeconds = maxOf(0, (timeMs / 1000).toInt()) // Handle negative values
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        else -> {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

/**
 * Determine extraction quality based on elapsed time in seconds
 */
fun getExtractionQuality(elapsedSeconds: Int, isRunning: Boolean): ExtractionQuality {
    return when {
        !isRunning -> ExtractionQuality.NEUTRAL
        elapsedSeconds < 20 -> ExtractionQuality.UNDER_EXTRACTED
        elapsedSeconds <= 35 -> ExtractionQuality.OPTIMAL
        else -> ExtractionQuality.OVER_EXTRACTED
    }
}

/**
 * Get timer color based on extraction quality
 */
fun getExtractionTimeColor(elapsedSeconds: Int, isRunning: Boolean): Color {
    return when (getExtractionQuality(elapsedSeconds, isRunning)) {
        ExtractionQuality.UNDER_EXTRACTED -> Color(0xFFFFC107) // Amber/Yellow
        ExtractionQuality.OPTIMAL -> Color(0xFF4CAF50) // Green
        ExtractionQuality.OVER_EXTRACTED -> Color(0xFFF44336) // Red
        ExtractionQuality.NEUTRAL -> Color.Gray.copy(alpha = 0.3f) // Light Gray
    }
}

/**
 * Get timer color based on elapsed time in milliseconds
 */
fun getTimerColor(timeMs: Long, isRunning: Boolean): Color {
    val elapsedSeconds = (timeMs / 1000).toInt()
    return getExtractionTimeColor(elapsedSeconds, isRunning)
}

// ========== TASK 5, 6, 7: ENHANCED TIMER BUTTON WITH HAPTIC FEEDBACK AND STATE MANAGEMENT ==========

/**
 * Data class representing the state of the timer button
 */
data class TimerButtonState(
    val isRunning: Boolean,
    val buttonColor: Color,
    val iconColor: Color,
    val icon: ImageVector,
    val contentDescription: String,
    val lastActionTime: Long = 0L
)

/**
 * Composable function to manage timer button state with debouncing
 */
@Composable
fun rememberTimerButtonState(isRunning: Boolean): TimerButtonState {
    var lastActionTime by remember { mutableLongStateOf(0L) }

    return remember(isRunning) {
        TimerButtonState(
            isRunning = isRunning,
            buttonColor = if (isRunning) Color(0xFFF44336) else Color(0xFF4CAF50), // Red for stop, Green for start
            iconColor = Color.White,
            icon = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Stop timer" else "Start timer",
            lastActionTime = lastActionTime
        ).also {
            lastActionTime = System.currentTimeMillis()
        }
    }
}

/**
 * Haptic feedback utility function
 */
fun triggerHapticFeedback(context: Context, isStartAction: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    vibrator?.let { vib ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use VibrationEffect for API 26+
            val effect = if (isStartAction) {
                // Light haptic feedback for start action (10ms)
                VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                // Medium haptic feedback for stop action (25ms)
                VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vib.vibrate(effect)
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            val duration = if (isStartAction) 10L else 25L
            vib.vibrate(duration)
        }
    }
}

/**
 * Enhanced timer button component with prominent design, haptic feedback, and smooth animations
 *
 * Features:
 * - Minimum 80dp diameter for accessibility
 * - Dynamic colors: Green for start, Red for stop
 * - Play/Stop icons that change based on state
 * - Material Design elevation with shadow effects
 * - Haptic feedback integration
 * - Ripple effect and press animations
 * - Debouncing to prevent rapid multiple taps (Task 14)
 */
@Composable
fun EnhancedTimerButton(
    isRunning: Boolean,
    onStartStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    debounceDelayMs: Long = 300L // Configurable debounce delay
) {
    val context = LocalContext.current
    val buttonState = rememberTimerButtonState(isRunning)

    // Debouncing state to prevent rapid taps (Task 14: Double-Tap Prevention)
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var isProcessing by remember { mutableStateOf(false) }

    // Animated colors for smooth transitions
    val animatedButtonColor by animateColorAsState(
        targetValue = buttonState.buttonColor,
        animationSpec = tween(durationMillis = 300),
        label = "button_color"
    )

    val animatedIconColor by animateColorAsState(
        targetValue = buttonState.iconColor,
        animationSpec = tween(durationMillis = 300),
        label = "icon_color"
    )

    // Scale animation for press feedback - immediate visual response (< 100ms as required)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh // High stiffness for immediate response
        ),
        label = "button_scale"
    )

    // Handle button click with comprehensive debouncing and state management
    val handleClick = {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastClick = currentTime - lastClickTime

        // Prevent rapid multiple taps with debouncing
        if (!isProcessing && timeSinceLastClick > debounceDelayMs) {
            lastClickTime = currentTime
            isProcessing = true

            // Trigger haptic feedback immediately
            triggerHapticFeedback(context, !isRunning)

            // Immediate visual feedback (within 100ms requirement)
            isPressed = true

            // Execute the action
            onStartStop()

            // Reset states after animation
            CoroutineScope(Dispatchers.Main).launch {
                delay(100) // Visual feedback duration
                isPressed = false
                delay(200) // Additional processing delay
                isProcessing = false
            }
        }
    }

    FloatingActionButton(
        onClick = handleClick,
        modifier = modifier
            .size(80.dp) // Minimum 80dp diameter as specified
            .graphicsLayer(scaleX = scale, scaleY = scale),
        containerColor = animatedButtonColor,
        contentColor = animatedIconColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp, // Material Design elevation as specified
            pressedElevation = 12.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 10.dp
        ),
        shape = CircleShape
    ) {
        Icon(
            imageVector = buttonState.icon,
            contentDescription = buttonState.contentDescription,
            modifier = Modifier.size(32.dp), // Large icon for visibility
            tint = animatedIconColor
        )
    }
}

/**
 * Enhanced timer controls with clickable timer component and repositioned reset button
 *
 * Features:
 * - Entire timer component is clickable (200dp vs 80dp touch target)
 * - Reset button elegantly positioned as a small floating action button
 * - Maintains all existing functionality with improved UX
 */
@Composable
fun ClickableTimerControls(
    isRunning: Boolean,
    currentTime: Long,
    targetTime: Long? = null,
    onStartStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    showReset: Boolean = true,
    showColorCoding: Boolean = true
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Main clickable timer component
        CircularTimer(
            currentTime = currentTime,
            targetTime = targetTime,
            isRunning = isRunning,
            showColorCoding = showColorCoding,
            onStartStop = onStartStop,
            modifier = Modifier.size(200.dp)
        )

        // Reset button positioned elegantly in the top-right area
        if (showReset && currentTime > 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                contentAlignment = Alignment.TopEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        onReset()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 8.dp, y = (-8).dp), // Slightly outside the timer boundary
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset timer",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}