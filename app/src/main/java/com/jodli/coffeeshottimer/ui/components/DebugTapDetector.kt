package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jodli.coffeeshottimer.BuildConfig
import kotlinx.coroutines.delay

/**
 * Debug tap detector that activates debug functionality after 5 consecutive taps.
 * Only available in debug builds for security and performance reasons.
 *
 * @param onDebugActivated Callback invoked when debug mode is activated (5 taps detected)
 * @param content The content to wrap with tap detection
 */
@Composable
fun DebugTapDetector(
    onDebugActivated: () -> Unit,
    content: @Composable () -> Unit
) {
    if (BuildConfig.DEBUG) {
        var tapCount by remember { mutableIntStateOf(0) }
        var lastTapTime by remember { mutableLongStateOf(0L) }

        // Reset counter after 3 seconds of inactivity
        LaunchedEffect(tapCount) {
            if (tapCount > 0) {
                delay(3000)
                tapCount = 0
            }
        }

        Box(
            modifier = Modifier.clickable {
                val currentTime = System.currentTimeMillis()

                // Reset counter if more than 3 seconds have passed since last tap
                if (currentTime - lastTapTime > 3000) {
                    tapCount = 1
                } else {
                    tapCount++
                }

                lastTapTime = currentTime

                // Activate debug mode after 5 taps
                if (tapCount >= 5) {
                    onDebugActivated()
                    tapCount = 0 // Reset counter after activation
                }
            }
        ) {
            content()
        }
    } else {
        // In release builds, just render the content without tap detection
        content()
    }
}
