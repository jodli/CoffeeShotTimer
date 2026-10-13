package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.ExtractionZone
import com.jodli.coffeeshottimer.ui.theme.zoneColors

/**
 * A taste reading as a zone-colour swatch plus the word. Swatch colour follows
 * the extraction zones: sour is fast (under), perfect is ideal, bitter is slow (over).
 */
@Composable
fun TasteMark(
    taste: TastePrimary,
    secondary: TasteSecondary? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    val swatchColor = MaterialTheme.zoneColors.fill(
        when (taste) {
            TastePrimary.SOUR -> ExtractionZone.FAST
            TastePrimary.PERFECT -> ExtractionZone.IDEAL
            TastePrimary.BITTER -> ExtractionZone.SLOW
        }
    )
    val word = stringResource(
        when (taste) {
            TastePrimary.SOUR -> R.string.taste_sour
            TastePrimary.PERFECT -> R.string.taste_perfect
            TastePrimary.BITTER -> R.string.taste_bitter
        }
    )
    val qualifier = secondary?.let {
        stringResource(
            if (it == TasteSecondary.WEAK) R.string.taste_weak else R.string.taste_strong
        )
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .background(swatchColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (qualifier != null) "$word · $qualifier" else word,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private val SWATCH_SIZE: Dp = 10.dp

@Preview(showBackground = true, name = "Taste marks")
@Composable
fun TasteMarkPreview() {
    CoffeeShotTimerTheme {
        Column {
            TasteMark(TastePrimary.PERFECT)
            TasteMark(TastePrimary.SOUR, TasteSecondary.WEAK)
            TasteMark(TastePrimary.BITTER, TasteSecondary.STRONG)
        }
    }
}
