package com.jodli.coffeeshottimer.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.QualityTier
import com.jodli.coffeeshottimer.ui.theme.zoneColors

/** Fixed widths of the ledger's right-aligned figure columns; sized for "Verhältnis". */
val LEDGER_TIME_COLUMN_WIDTH: Dp = 60.dp
val LEDGER_RATIO_COLUMN_WIDTH: Dp = 88.dp
val LEDGER_SCORE_COLUMN_WIDTH: Dp = 68.dp

/** The tier's display word. */
@StringRes
fun QualityTier.labelRes(): Int = when (this) {
    QualityTier.EXCELLENT -> R.string.tier_excellent
    QualityTier.GOOD -> R.string.tier_good
    QualityTier.NEEDS_WORK -> R.string.tier_needs_work
}

/** A row ruled top and bottom, like a line of the website's step table. */
@Composable
fun RuledRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** A data label over a value. Editable cells get a pencil so they don't read as static figures. */
@Composable
fun DataCell(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    value: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClickLabel = clickLabel, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, content = value)
    }
}

/** Square-cornered segmented switch, the Record Shot control applied everywhere. */
@Composable
fun SquareSegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { if (index != selectedIndex) onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = MaterialTheme.shapes.small
                ),
                border = SegmentedButtonDefaults.borderStroke(MaterialTheme.colorScheme.outline),
                icon = {}
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** A condensed caps section label with an optional trailing slot (counts, tier words). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/** Quality score as a square zone-colour swatch plus the tabular figure. */
@Composable
fun ScoreFigure(
    score: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    swatch: Dp = 8.dp
) {
    val tierWord = stringResource(QualityTier.forScore(score).labelRes())
    val description = stringResource(R.string.cd_quality_score, score, tierWord)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(swatch)
                .background(MaterialTheme.zoneColors.forScore(score))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = score.toString(),
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
