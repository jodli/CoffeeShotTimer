package com.jodli.coffeeshottimer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.ui.accessibility.accessibleButton
import com.jodli.coffeeshottimer.ui.accessibility.accessibleDataDisplay
import com.jodli.coffeeshottimer.ui.accessibility.accessibleError
import com.jodli.coffeeshottimer.ui.accessibility.accessibleSuccess
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Enhanced UI components with improved animations, accessibility, and polish.
 */

/**
 * Enhanced success indicator with animation and accessibility.
 */
@Composable
fun EnhancedSuccessIndicator(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleSuccess(message),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                onDismiss?.let {
                    TextButton(
                        onClick = it,
                        modifier = Modifier.accessibleButton(
                            "Dismiss",
                            action = "dismiss success message"
                        )
                    ) {
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced error indicator with animation and accessibility.
 */
@Composable
fun EnhancedErrorIndicator(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleError(message),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalSpacing.current.medium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onRetry != null || onDismiss != null) {
                    Spacer(modifier = Modifier.height(LocalSpacing.current.small))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
                    ) {
                        onRetry?.let {
                            TextButton(
                                onClick = it,
                                modifier = Modifier.accessibleButton(
                                    "Retry",
                                    action = "retry failed operation"
                                )
                            ) {
                                Text(
                                    text = "Retry",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        onDismiss?.let {
                            TextButton(
                                onClick = it,
                                modifier = Modifier.accessibleButton(
                                    "Dismiss",
                                    action = "dismiss error message"
                                )
                            ) {
                                Text(
                                    text = "Dismiss",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced loading indicator with better animation and accessibility.
 */
@Composable
fun EnhancedLoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.medium)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .scale(scale),
            strokeWidth = 4.dp
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Enhanced metric display with improved visual hierarchy and accessibility.
 */
@Composable
fun EnhancedMetricDisplay(
    label: String,
    value: String,
    unit: String? = null,
    isGood: Boolean? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = when (isGood) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (isGood) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val fullValue = if (unit != null) "$value $unit" else value

    Surface(
        modifier = modifier
            .accessibleDataDisplay(label, fullValue)
            .then(
                if (onClick != null) Modifier.accessibleButton(
                    label,
                    action = "view details"
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(LocalSpacing.current.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(LocalSpacing.current.extraSmall))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.extraSmall)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                unit?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }

            // Quality indicator
            isGood?.let { good ->
                Spacer(modifier = Modifier.height(LocalSpacing.current.extraSmall))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (good) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                )
            }
        }
    }
}

/**
 * Enhanced status badge with animation and accessibility.
 */
@Composable
fun EnhancedStatusBadge(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier,
    isAnimated: Boolean = false
) {
    val colors = when (type) {
        StatusType.Success -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        StatusType.Warning -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        StatusType.Error -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        StatusType.Info -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val icon = when (type) {
        StatusType.Success -> Icons.Default.Check
        StatusType.Warning -> Icons.Default.Warning
        StatusType.Error -> Icons.Default.Error
        StatusType.Info -> Icons.Default.Info
    }

    val scale by animateFloatAsState(
        targetValue = if (isAnimated) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badge_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .accessibleDataDisplay("Status", text, context = type.name),
        shape = RoundedCornerShape(20.dp),
        color = colors.first
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = LocalSpacing.current.small,
                vertical = LocalSpacing.current.extraSmall
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.extraSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.second,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colors.second,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced floating action button with improved accessibility and animation.
 */
@Composable
fun EnhancedFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isExtended: Boolean = false,
    text: String? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isExtended) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_scale"
    )

    if (isExtended && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .accessibleButton(contentDescription, action = "activate"),
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            text = {
                Text(text = text)
            }
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .accessibleButton(contentDescription, action = "activate")
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

/**
 * Enhanced progress indicator with percentage and accessibility.
 */
@Composable
fun EnhancedProgressIndicator(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    Column(
        modifier = modifier.accessibleDataDisplay(
            label = label,
            value = "${(progress * 100).toInt()}%",
            context = "progress"
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showPercentage) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(LocalSpacing.current.extraSmall))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

enum class StatusType {
    Success, Warning, Error, Info
}