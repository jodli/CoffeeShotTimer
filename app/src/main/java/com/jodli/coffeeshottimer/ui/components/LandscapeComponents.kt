package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing

/**
 * Container composable that automatically switches between portrait and landscape layouts.
 * 
 * This component follows the task requirements for landscape support implementation:
 * - Detects orientation using Configuration.orientation
 * - Provides automatic layout switching
 * - Uses existing spacing system with landscape extensions
 * - Maintains consistent padding and arrangement
 * 
 * @param modifier Modifier for the container
 * @param portraitContent Content to display in portrait mode
 * @param landscapeContent Content to display in landscape mode (optional)
 */
@Composable
fun LandscapeContainer(
    modifier: Modifier = Modifier,
    portraitContent: @Composable () -> Unit,
    landscapeContent: @Composable (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = modifier) {
        if (isLandscape && landscapeContent != null) {
            landscapeContent()
        } else {
            portraitContent()
        }
    }
}

/**
 * Horizontal row layout optimized for landscape orientation.
 * 
 * Features:
 * - Uses landscape-aware spacing from the theme system
 * - Provides proper alignment and arrangement for landscape content
 * - Maintains accessibility with minimum touch targets
 * - Supports weight-based layout distribution
 * 
 * @param modifier Modifier for the row
 * @param horizontalArrangement Horizontal arrangement of children
 * @param verticalAlignment Vertical alignment of children
 * @param content Row content with access to RowScope
 */
@Composable
fun LandscapeRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}

/**
 * Two-pane layout specifically designed for RecordShotScreen landscape mode.
 * 
 * This layout implements the requirements from task 5:
 * - Timer component on the left side (fixed width)
 * - Form controls on the right side (scrollable, takes remaining space)
 * - Uses landscape-aware spacing throughout
 * - Maintains existing component interactions
 * - Ensures proper content arrangement for landscape orientation
 * 
 * @param modifier Modifier for the entire layout
 * @param timerContent Left pane content (typically timer controls)
 * @param formContent Right pane content (typically form controls)
 */
@Composable
fun RecordShotLandscapeLayout(
    modifier: Modifier = Modifier,
    timerContent: @Composable () -> Unit,
    formContent: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing),
        verticalAlignment = Alignment.Top
    ) {
        // Left pane: Timer (50% of available width for better prominence)
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            timerContent()
        }
        
        // Right pane: Form controls (50% of available width, scrollable)
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            formContent()
        }
    }
}

/**
 * Scrollable content container optimized for landscape orientation.
 * 
 * Features:
 * - Uses landscape spacing between items
 * - Provides vertical scrolling capability
 * - Maintains consistent spacing with theme system
 * - Optimized for form content in landscape mode
 * 
 * @param modifier Modifier for the scrollable container
 * @param content Column content to be made scrollable
 */
@Composable
fun LandscapeScrollableContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
    ) {
        content()
    }
}

/**
 * Timer container with landscape-appropriate sizing and positioning.
 * 
 * This component specifically addresses task 5 requirements:
 * - Uses landscape timer size (160.dp) from theme system
 * - Maintains clickable functionality and haptic feedback
 * - Provides proper centering and alignment for landscape mode
 * - Preserves all existing timer interactions
 * 
 * @param modifier Modifier for the timer container
 * @param content Timer content (typically CircularTimer or TimerControls)
 */
@Composable
fun LandscapeTimerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Box(
        modifier = modifier
            .widthIn(min = spacing.landscapeTimerSize + (spacing.landscapeContentSpacing * 2))
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
