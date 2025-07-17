package com.example.coffeeshottimer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import com.example.coffeeshottimer.ui.theme.LocalSpacing
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular timer display with progress indicator
 */
@Composable
fun CircularTimer(
    currentTime: Long,
    targetTime: Long?,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val progress = if (targetTime != null && targetTime > 0) {
        (currentTime.toFloat() / targetTime.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    // Animation for the progress indicator
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "timer_progress"
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
            
            // Progress arc
            if (targetTime != null && animatedProgress > 0f) {
                drawArc(
                    color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
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
        
        // Timer text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(currentTime),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            if (targetTime != null) {
                Text(
                    text = "/ ${formatTime(targetTime)}",
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
    onStop: () -> Unit,
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
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Stop button
        FloatingActionButton(
            onClick = onStop,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.error
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                modifier = Modifier.size(20.dp)
            )
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
    showStatus: Boolean = true
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
            text = formatTime(currentTime),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (isRunning) 
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