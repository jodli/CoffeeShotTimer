package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

/**
 * Dialog shown after successfully recording a shot, with optional recommendations.
 */
@Composable
fun ShotRecordedDialog(
    brewRatio: String,
    extractionTime: String,
    recommendations: List<ShotRecommendation> = emptyList(),
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_shot_recorded_success),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconLarge)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_shot_recorded_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Shot summary
                Text(
                    text = stringResource(R.string.text_record_successfully, brewRatio, extractionTime),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                // Recommendations if available
                if (recommendations.isNotEmpty()) {
                    HorizontalDivider()
                    
                    CompactRecommendationList(
                        recommendations = recommendations,
                        maxItems = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (recommendations.size > 2) {
                        Text(
                            text = stringResource(R.string.dialog_view_details_for_more_recommendations),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onViewDetails != null) {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_view_details),
                    onClick = {
                        onViewDetails()
                        onDismiss()
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            } else {
                CoffeePrimaryButton(
                    text = stringResource(R.string.text_dialog_ok),
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
        },
        dismissButton = if (onViewDetails != null) {
            {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.text_dialog_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
        } else null
    )
}

/**
 * Bottom sheet version for showing shot recorded with recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotRecordedBottomSheet(
    brewRatio: String,
    extractionTime: String,
    recommendations: List<ShotRecommendation> = emptyList(),
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.screenPadding)
                .padding(bottom = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Success icon and title
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_shot_recorded_success),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconEmptyState)
            )

            Text(
                text = stringResource(R.string.dialog_shot_recorded_successfully),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            // Shot summary
            Text(
                text = stringResource(R.string.text_record_successfully, brewRatio, extractionTime),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            // Recommendations if available
            if (recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.small))
                
                RecommendationCard(
                    recommendations = recommendations,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.text_dialog_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                if (onViewDetails != null) {
                    CoffeePrimaryButton(
                        text = stringResource(R.string.button_view_details),
                        onClick = {
                            onViewDetails()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}