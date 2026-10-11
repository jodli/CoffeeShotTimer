@file:Suppress("MagicNumber") // Zone percentages and canvas drawing require literal values

package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.zoneColors

/**
 * Horizontal bar showing three extraction zones with current percentage marker.
 *
 * Zones (based on 25-30s optimal range):
 * - Left 25%: Orange zone (<25s, too fast)
 * - Middle 50%: Green zone (25-30s, optimal)
 * - Right 25%: Red zone (>30s, too slow)
 *
 * @param optimalPercentage Percentage of shots in the optimal zone (0-100)
 * @param modifier Modifier for the component
 */
@Composable
fun QualityZoneIndicator(
    optimalPercentage: Int,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val contentDescriptionText = stringResource(R.string.cd_zone_indicator, optimalPercentage)

    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.semantics { contentDescription = contentDescriptionText },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Horizontal bar with zones
        val zones = MaterialTheme.zoneColors
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barHeight = canvasHeight * 0.5f
            val barTop = (canvasHeight - barHeight) / 2f

            val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            // Left zone: too fast, tends sour (0-25%)
            drawRoundRect(
                color = zones.fast,
                topLeft = Offset(0f, barTop),
                size = Size(canvasWidth * 0.25f, barHeight),
                cornerRadius = cornerRadius
            )

            // Middle zone: ideal (25-75%)
            drawRect(
                color = zones.ideal,
                topLeft = Offset(canvasWidth * 0.25f, barTop),
                size = Size(canvasWidth * 0.5f, barHeight)
            )

            // Right zone: too slow, tends bitter (75-100%)
            drawRoundRect(
                color = zones.slow,
                topLeft = Offset(canvasWidth * 0.75f, barTop),
                size = Size(canvasWidth * 0.25f, barHeight),
                cornerRadius = cornerRadius
            )

            // Marker for current percentage (diamond shape)
            val markerX = (optimalPercentage.coerceIn(0, 100) / 100f) * canvasWidth
            val markerSize = 12.dp.toPx()
            val markerTop = barTop - markerSize - 4.dp.toPx()
            val markerCenterY = markerTop + markerSize / 2f

            // Draw diamond marker
            val markerPath = Path().apply {
                moveTo(markerX, markerCenterY - markerSize / 2f) // Top
                lineTo(markerX + markerSize / 2f, markerCenterY) // Right
                lineTo(markerX, markerCenterY + markerSize / 2f) // Bottom
                lineTo(markerX - markerSize / 2f, markerCenterY) // Left
                close()
            }

            drawPath(
                path = markerPath,
                color = markerColor.copy(alpha = 0.8f),
                style = Fill
            )
        }

        // Percentage text
        Text(
            text = stringResource(R.string.analytics_optimal_zone_format, optimalPercentage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun QualityZoneIndicatorPreview() {
    CoffeeShotTimerTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QualityZoneIndicator(
                optimalPercentage = 75,
                modifier = Modifier.fillMaxWidth()
            )

            QualityZoneIndicator(
                optimalPercentage = 45,
                modifier = Modifier.fillMaxWidth()
            )

            QualityZoneIndicator(
                optimalPercentage = 20,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
