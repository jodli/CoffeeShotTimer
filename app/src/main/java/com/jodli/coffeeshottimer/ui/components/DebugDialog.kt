package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import kotlinx.coroutines.delay

/**
 * Debug dialog that provides database management and onboarding testing utilities for developers.
 * Only available in debug builds to prevent accidental use in production.
 *
 * Features:
 * - Fill database with realistic test data for screenshots
 * - Clear all database data for clean testing
 * - Reset onboarding state for testing different user scenarios
 * - Loading states and result feedback
 * - Confirmation dialogs for destructive operations
 *
 * @param isVisible Whether the dialog should be displayed
 * @param onDismiss Callback invoked when dialog should be dismissed
 * @param onFillDatabase Callback invoked when fill database button is tapped
 * @param onClearDatabase Callback invoked when clear database button is tapped
 * @param onResetToNewUser Callback invoked when reset to new user button is tapped
 * @param onResetToExistingUserNoBeans Callback invoked when reset to existing user without beans button is tapped
 * @param onForceEquipmentSetup Callback invoked when force equipment setup button is tapped
 * @param isLoading Whether a database operation is currently in progress
 * @param operationResult Result message from the last operation (success or error)
 * @param showConfirmation Whether to show confirmation dialog for clear operation
 * @param onShowConfirmation Callback to show confirmation dialog
 * @param onHideConfirmation Callback to hide confirmation dialog
 */
@Composable
fun DebugDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFillDatabase: () -> Unit,
    onClearDatabase: () -> Unit,
    onResetToNewUser: () -> Unit,
    onResetToExistingUserNoBeans: () -> Unit,
    onForceEquipmentSetup: () -> Unit,
    isLoading: Boolean,
    operationResult: String?,
    showConfirmation: Boolean,
    onShowConfirmation: () -> Unit,
    onHideConfirmation: () -> Unit
) {
    // Only render in debug builds
    if (!BuildConfig.DEBUG || !isVisible) return

    val spacing = LocalSpacing.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(spacing.cornerLarge),
            elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationDialog),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.large)
            ) {
                // Header with debug indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(spacing.iconMedium),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = stringResource(R.string.title_debug_tools),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_close_debug_dialog),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                // Debug mode indicator
                Text(
                    text = stringResource(R.string.text_debug_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.large))

                // Operation result feedback
                operationResult?.let { result ->
                    val isError = result.contains("Failed") || result.contains("Error")
                    if (isError) {
                        ErrorCard(
                            title = stringResource(R.string.error_operation_failed),
                            message = result,
                            onDismiss = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.medium)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Auto-dismiss after showing result
                    LaunchedEffect(result) {
                        delay(3000)
                        onDismiss()
                    }
                }

                // Loading indicator
                if (isLoading) {
                    LoadingIndicator(
                        message = stringResource(R.string.processing_database_operation),
                        modifier = Modifier.padding(vertical = spacing.large)
                    )
                } else {
                    // Action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        // Database Operations Section
                        Text(
                            text = "Database Operations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        // Fill Database button
                        CoffeePrimaryButton(
                            text = stringResource(R.string.button_fill_database),
                            onClick = onFillDatabase,
                            icon = Icons.Default.Storage,
                            enabled = !isLoading
                        )

                        Text(
                            text = stringResource(R.string.text_fill_database_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        // Clear Database button
                        CoffeeSecondaryButton(
                            text = stringResource(R.string.button_clear_database),
                            onClick = onShowConfirmation,
                            icon = Icons.Default.Delete,
                            enabled = !isLoading
                        )

                        Text(
                            text = stringResource(R.string.text_clear_database_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )

                        Spacer(modifier = Modifier.height(spacing.large))

                        // Onboarding Testing Section
                        Text(
                            text = "Onboarding Testing",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        // Reset to New User button
                        CoffeeSecondaryButton(
                            text = "New User",
                            onClick = onResetToNewUser,
                            icon = Icons.Default.PersonAdd,
                            enabled = !isLoading
                        )

                        Text(
                            text = "Show full onboarding flow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        // Reset to Existing User without Beans button
                        CoffeeSecondaryButton(
                            text = "User - No Beans",
                            onClick = onResetToExistingUserNoBeans,
                            icon = Icons.Default.PersonOutline,
                            enabled = !isLoading
                        )

                        Text(
                            text = "Trigger guided bean creation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        // Force Equipment Setup button
                        CoffeeSecondaryButton(
                            text = "Equipment Setup",
                            onClick = onForceEquipmentSetup,
                            icon = Icons.Default.Settings,
                            enabled = !isLoading
                        )

                        Text(
                            text = "Force equipment setup flow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for clear database operation
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = onHideConfirmation,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(spacing.iconMedium)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = stringResource(R.string.dialog_clear_database_title),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_clear_database_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHideConfirmation()
                        onClearDatabase()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.button_clear_database),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onHideConfirmation) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}