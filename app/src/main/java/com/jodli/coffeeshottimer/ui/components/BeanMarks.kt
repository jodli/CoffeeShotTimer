package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.RoastFreshness
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.util.BeanStatus
import com.jodli.coffeeshottimer.ui.util.getStatusColor
import com.jodli.coffeeshottimer.ui.util.labelRes

private val SWATCH_SIZE = 10.dp

/** Ink for the roast-age figure: resting gold, fresh green, past peak plain ink. */
@Composable
fun RoastFreshness.ink(): Color = when (this) {
    RoastFreshness.RESTING -> MaterialTheme.zoneColors.fastInk
    RoastFreshness.FRESH -> MaterialTheme.zoneColors.idealInk
    RoastFreshness.PAST -> MaterialTheme.colorScheme.onSurface
}

fun RoastFreshness.labelRes(): Int = when (this) {
    RoastFreshness.RESTING -> R.string.freshness_resting
    RoastFreshness.FRESH -> R.string.text_fresh
    RoastFreshness.PAST -> R.string.freshness_past
}

/** Dial-in status as a swatch plus word; finished (inactive) beans show the idle grey. */
@Composable
fun BeanStatusMark(
    status: BeanStatus,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    val swatchColor = if (isActive) getStatusColor(status) else MaterialTheme.zoneColors.idle
    val word = stringResource(if (isActive) status.labelRes() else R.string.label_bean_finished)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(SWATCH_SIZE).background(swatchColor))
        Text(
            text = word,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
