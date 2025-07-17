package com.example.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying bean information
 */
@Composable
fun BeanCard(
    bean: Bean,
    onEdit: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showActions: Boolean = true
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(
        modifier = modifier,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        tint = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = bean.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                if (bean.origin.isNotBlank()) {
                    Text(
                        text = "Origin: ${bean.origin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (bean.roastLevel.isNotBlank()) {
                    Text(
                        text = "Roast: ${bean.roastLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (bean.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    Text(
                        text = bean.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (showActions) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit bean",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(spacing.small))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Currently Selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall)
                )
            }
        }
    }
}

/**
 * Compact bean selector component
 */
@Composable
fun BeanSelector(
    selectedBean: Bean?,
    onBeanSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    OutlinedCard(
        onClick = onBeanSelect,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Coffee,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedBean != null) "Selected Bean" else "Select Bean",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = selectedBean?.name ?: "No bean selected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Select bean",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card component for displaying shot information
 */
@Composable
fun ShotCard(
    shot: Shot,
    bean: Bean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBeanInfo: Boolean = true
) {
    val spacing = LocalSpacing.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    CoffeeCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Shot timing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    CompactTimer(
                        currentTime = shot.extractionTime,
                        isRunning = false,
                        showStatus = false
                    )
                    
                    if (shot.targetTime != null) {
                        Text(
                            text = "Target: ${formatTime(shot.targetTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                // Bean information
                if (showBeanInfo && bean != null) {
                    Text(
                        text = bean.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Shot details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    if (shot.grindSize != null) {
                        Text(
                            text = "Grind: ${shot.grindSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (shot.doseWeight != null) {
                        Text(
                            text = "Dose: ${shot.doseWeight}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (shot.yieldWeight != null) {
                    Text(
                        text = "Yield: ${shot.yieldWeight}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Date
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = dateFormat.format(Date(shot.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Rating or quality indicator
            if (shot.rating != null) {
                Surface(
                    color = when {
                        shot.rating >= 4 -> MaterialTheme.colorScheme.primaryContainer
                        shot.rating >= 3 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${shot.rating}/5",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            shot.rating >= 4 -> MaterialTheme.colorScheme.onPrimaryContainer
                            shot.rating >= 3 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall)
                    )
                }
            }
        }
    }
}

/**
 * Summary card showing shot statistics for a bean
 */
@Composable
fun BeanSummaryCard(
    bean: Bean,
    totalShots: Int,
    averageTime: Long?,
    bestTime: Long?,
    averageRating: Float?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Text(
            text = bean.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Shots",
                value = totalShots.toString()
            )
            
            StatItem(
                label = "Avg Time",
                value = averageTime?.let { formatTime(it) } ?: "--"
            )
            
            StatItem(
                label = "Best Time",
                value = bestTime?.let { formatTime(it) } ?: "--"
            )
            
            if (averageRating != null) {
                StatItem(
                    label = "Avg Rating",
                    value = String.format("%.1f", averageRating)
                )
            }
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
            style = MaterialTheme.typography.titleMedium,
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