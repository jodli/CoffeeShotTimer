package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

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

    Box(
        modifier = modifier.then(
            if (isLandscape) Modifier.statusBarsPadding() else Modifier
        )
    ) {
        if (isLandscape && landscapeContent != null) {
            landscapeContent()
        } else {
            portraitContent()
        }
    }
}
