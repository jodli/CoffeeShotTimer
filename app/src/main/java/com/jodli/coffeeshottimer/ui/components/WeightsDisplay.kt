package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme

/**
 * Ultra-compact single-line weights display component.
 *
 * Format: "18g → 36g (1:2.0)"
 *
 * Features:
 * - Coffee In: Tap to edit (opens dialog)
 * - Coffee Out: +/- buttons for quick 1g adjustments
 * - Brew ratio: Auto-calculated and color-coded (green when 1:1.5-2.5)
 *
 * @param coffeeIn Coffee weight in (grams)
 * @param coffeeOut Coffee weight out (grams)
 * @param onCoffeeInClick Callback when coffee in is clicked (opens dialog)
 * @param onCoffeeOutDecrease Callback to decrease coffee out by 1g
 * @param onCoffeeOutIncrease Callback to increase coffee out by 1g
 * @param modifier Modifier for the composable
 */
@Composable
fun WeightsDisplay(
    coffeeIn: Double,
    coffeeOut: Double,
    onCoffeeInClick: () -> Unit,
    onCoffeeOutDecrease: () -> Unit,
    onCoffeeOutIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate brew ratio
    val ratio = if (coffeeIn > 0) coffeeOut / coffeeIn else 0.0
    val isOptimalRatio = ratio in 1.5..2.5

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Display weights and ratio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coffee In (tap to edit)
                Text(
                    text = stringResource(R.string.format_weight_grams, coffeeIn.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onCoffeeInClick)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Arrow
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Coffee Out
                Text(
                    text = stringResource(R.string.format_weight_grams, coffeeOut.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Ratio (calculated)
                Text(
                    text = stringResource(R.string.format_ratio, ratio),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isOptimalRatio) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isOptimalRatio) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Right: Quick adjust buttons for Coffee Out
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCoffeeOutDecrease,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_coffee_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCoffeeOutIncrease,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_increase_coffee_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREVIEW COMPOSABLES
// ============================================================================

@Suppress("UnusedPrivateMember")
@Preview(name = "Weights - Optimal Ratio (1:2.0)", showBackground = true)
@Composable
private fun WeightsDisplayPreview_Optimal() {
    CoffeeShotTimerTheme {
        WeightsDisplay(
            coffeeIn = 18.0,
            coffeeOut = 36.0,
            onCoffeeInClick = {},
            onCoffeeOutDecrease = {},
            onCoffeeOutIncrease = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Weights - Low Ratio (1:1.2)", showBackground = true)
@Composable
private fun WeightsDisplayPreview_Low() {
    CoffeeShotTimerTheme {
        WeightsDisplay(
            coffeeIn = 18.0,
            coffeeOut = 22.0,
            onCoffeeInClick = {},
            onCoffeeOutDecrease = {},
            onCoffeeOutIncrease = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Weights - High Ratio (1:3.0)", showBackground = true)
@Composable
private fun WeightsDisplayPreview_High() {
    CoffeeShotTimerTheme {
        WeightsDisplay(
            coffeeIn = 18.0,
            coffeeOut = 54.0,
            onCoffeeInClick = {},
            onCoffeeOutDecrease = {},
            onCoffeeOutIncrease = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Weights - Dark Mode", showBackground = true)
@Composable
private fun WeightsDisplayPreview_Dark() {
    CoffeeShotTimerTheme(darkTheme = true) {
        WeightsDisplay(
            coffeeIn = 18.0,
            coffeeOut = 36.0,
            onCoffeeInClick = {},
            onCoffeeOutDecrease = {},
            onCoffeeOutIncrease = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}
