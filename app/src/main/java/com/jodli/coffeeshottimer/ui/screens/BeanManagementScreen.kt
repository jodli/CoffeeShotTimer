package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.jodli.coffeeshottimer.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoThumbnail
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementViewModel

@Composable
fun BeanManagementScreen(
    onAddBeanClick: () -> Unit = {},
    onEditBeanClick: (String) -> Unit = {},
    onNavigateToRecordShot: () -> Unit = {},
    viewModel: BeanManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showInactive by viewModel.showInactive.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    var showDeleteDialog by remember { mutableStateOf<Bean?>(null) }
    var showPhotoViewer by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.medium)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.title_bean_management),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            CoffeePrimaryButton(
                text = stringResource(R.string.text_add_bean),
                onClick = onAddBeanClick,
                icon = Icons.Default.Add,
                fillMaxWidth = false
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Search and Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Field
            CoffeeTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = stringResource(R.string.label_search_beans),
                placeholder = stringResource(R.string.placeholder_enter_bean_name_search),
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (searchQuery.isNotEmpty()) Icons.Default.Clear else null,
                onTrailingIconClick = if (searchQuery.isNotEmpty()) {
                    { viewModel.updateSearchQuery("") }
                } else null,
                modifier = Modifier.weight(1f)
            )

            // Filter Toggle
            FilterChip(
                onClick = viewModel::toggleShowInactive,
                label = {
                    Text(
                        text = if (showInactive) stringResource(R.string.text_all) else stringResource(R.string.text_active),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = showInactive,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(spacing.iconSmall)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = stringResource(R.string.loading_beans))
                }
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.error_loading_beans),
                    message = uiState.error ?: "Unknown error occurred",
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.beans.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = stringResource(R.string.text_no_beans_available),
                    description = if (searchQuery.isNotEmpty()) {
                        stringResource(R.string.text_search_beans_hint)
                    } else {
                        stringResource(R.string.text_add_first_bean)
                    },
                    actionText = if (searchQuery.isEmpty()) stringResource(R.string.text_add_bean) else null,
                    onActionClick = if (searchQuery.isEmpty()) onAddBeanClick else null
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    contentPadding = PaddingValues(bottom = spacing.large)
                ) {
                    items(
                        items = uiState.beans,
                        key = { bean -> bean.id }
                    ) { bean ->
                        BeanListItem(
                            bean = bean,
                            onEdit = { onEditBeanClick(bean.id) },
                            onDelete = { showDeleteDialog = bean },
                            onUseForShot = {
                                if (bean.isActive) {
                                    viewModel.setCurrentBean(bean.id)
                                    onNavigateToRecordShot()
                                }
                            },
                            onReactivate = if (!bean.isActive) {
                                { viewModel.reactivateBean(bean.id) }
                            } else null,
                            onPhotoClick = { photoPath ->
                                showPhotoViewer = photoPath
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { bean ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(stringResource(R.string.button_delete_bean))
            },
            text = {
                Text(stringResource(R.string.format_delete_bean_confirmation, bean.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBean(bean.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.text_bean_management_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null }
                ) {
                    Text(stringResource(R.string.text_dialog_cancel))
                }
            }
        )
    }

    // Photo Viewer
    showPhotoViewer?.let { photoPath ->
        PhotoViewer(
            photoPath = photoPath,
            photoUri = null,
            onDismiss = { showPhotoViewer = null }
        )
    }
}

@Composable
private fun BeanListItem(
    bean: Bean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUseForShot: () -> Unit,
    onReactivate: (() -> Unit)? = null,
    onPhotoClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier,
        onClick = onEdit
    ) {
        CardHeader(
            icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
            title = bean.name,
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    if (!bean.isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(spacing.cornerSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.text_inactive),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(
                                    horizontal = spacing.small,
                                    vertical = spacing.extraSmall / 2
                                )
                            )
                        }
                    }

                    // Action buttons
                    if (bean.isActive) {
                        CoffeeSecondaryButton(
                            text = stringResource(R.string.text_use_for_shot),
                            onClick = onUseForShot,
                            modifier = Modifier.height(spacing.iconButtonSize),
                            fillMaxWidth = false
                        )
                    }

                    if (onReactivate != null) {
                        IconButton(
                            onClick = onReactivate,
                            modifier = Modifier.size(spacing.iconButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_reactivate_bean),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(spacing.iconSmall)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(spacing.iconButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (bean.isActive) stringResource(R.string.button_delete_bean) else stringResource(R.string.button_permanently_delete_bean),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(spacing.iconSmall)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Photo and content row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Photo thumbnail
            BeanPhotoThumbnail(
                photoPath = bean.photoPath,
                onPhotoClick = if (bean.hasPhoto() && onPhotoClick != null) {
                    { onPhotoClick(bean.photoPath!!) }
                } else null
            )

            // Bean information
            Column(
                modifier = Modifier.weight(1f)
            ) {

            // Days since roast with freshness indicator
            val daysSinceRoast = bean.daysSinceRoast()
            val isFresh = bean.isFresh()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.format_roasted_days_ago, daysSinceRoast),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFresh)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Freshness indicator
                Surface(
                    color = if (isFresh)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = if (isFresh) stringResource(R.string.text_fresh) else when {
                            daysSinceRoast < 4 -> stringResource(R.string.text_too_fresh)
                            daysSinceRoast <= 45 -> stringResource(R.string.text_good)
                            daysSinceRoast <= 90 -> stringResource(R.string.text_dialog_ok)
                            else -> stringResource(R.string.text_stale)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFresh)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall / 2)
                    )
                }
            }

            // Grinder setting if available
            if (!bean.lastGrinderSetting.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = stringResource(R.string.format_last_grind, bean.lastGrinderSetting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Notes if available
            if (bean.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = bean.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            }
        }
    }
}