package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * One-tap taste feedback component for quick shot quality capture.
 * Shows three primary taste buttons with optional secondary qualifiers.
 */
@Composable
fun TasteQuickPick(
    suggested: TastePrimary? = null,
    onSelectPrimary: (TastePrimary) -> Unit,
    onSelectSecondary: ((TasteSecondary?) -> Unit)? = null,
    selectedSecondary: TasteSecondary? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showSecondaryOptions: Boolean = false
) {
    val spacing = LocalSpacing.current
    var localSelectedSecondary by remember(selectedSecondary) {
        mutableStateOf(selectedSecondary)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = stringResource(R.string.text_how_did_it_taste),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = spacing.medium)
        )

        // Primary taste buttons
        TastePrimaryButtonRow(
            suggestedTaste = suggested,
            onTasteSelected = { taste ->
                if (taste != null) {
                    onSelectPrimary(taste)
                }
            },
            allowDeselection = false
        )

        // Optional secondary qualifiers
        if (showSecondaryOptions && onSelectSecondary != null) {
            Spacer(modifier = Modifier.height(spacing.medium))

            TasteSecondaryChipRow(
                selectedSecondary = localSelectedSecondary,
                onSecondarySelected = { newValue ->
                    localSelectedSecondary = newValue
                    onSelectSecondary(newValue)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Skip button
        if (onSkip != null) {
            Spacer(modifier = Modifier.height(spacing.small))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.semantics {
                    contentDescription = "Skip taste feedback"
                }
            ) {
                Text(
                    text = stringResource(R.string.button_skip_taste),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Suggestion hint
        if (suggested != null) {
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = stringResource(R.string.text_suggested_based_on_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TasteQuickPickPreview() {
    TasteQuickPick(
        suggested = TastePrimary.PERFECT,
        onSelectPrimary = {},
        onSelectSecondary = {},
        showSecondaryOptions = true,
        onSkip = {}
    )
}
