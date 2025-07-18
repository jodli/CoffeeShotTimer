package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Circular timer display with progress indicator and color coding
 */
@Composable
fun CircularTimer(
    currentTime: Long,
    targetTime: Long?,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showColorCoding: Boolean = true
) {
    val spacing = LocalSpacing.current
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
    
    Box(
        modifier = modifier.size(200.dp),
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
        
        // Timer text with enhanced formatting
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatExtractionTime(currentTime),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Show extraction quality indicator when color coding is enabled
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
    }
}

/**
 * Timer control buttons
 */
@Composable
fun TimerControls(
    isRunning: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    showReset: Boolean = true
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start/Pause button
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