package com.jodli.coffeeshottimer.ui.components.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Wrapper component for displaying metrics in a card format.
 *
 * Layout: Icon (left) + Column(primary value large, secondary text small).
 * Uses [CoffeeCard] as base container following design system patterns.
 *
 * @param icon Leading icon for the metric
 * @param primaryText Large primary value (e.g., "92")
 * @param secondaryText Small secondary description (e.g., "Quality Score")
 * @param modifier Modifier for the card
 * @param onClick Optional click handler for interaction
 */
@Composable
fun SimplifiedMetricCard(
    icon: ImageVector,
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current

    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    CoffeeCard(modifier = cardModifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconLarge)
            )

            // Text content column
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                // Primary text (large value)
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Secondary text (description)
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Preview function
@Preview(showBackground = true)
@Composable
private fun SimplifiedMetricCardPreview() {
    CoffeeShotTimerTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SimplifiedMetricCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                primaryText = "92",
                secondaryText = "Quality Score"
            )

            SimplifiedMetricCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                primaryText = "85%",
                secondaryText = "In Optimal Zone",
                onClick = { }
            )
        }
    }
}
