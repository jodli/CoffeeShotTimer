package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoThumbnail
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.util.formatLastUsed
import com.jodli.coffeeshottimer.ui.util.getStatusColor
import com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanManagementScreen(
    onAddBeanClick: () -> Unit = {},
    onEditBeanClick: (String) -> Unit = {},
    onNavigateToShotHistory: (String) -> Unit = {},
    viewModel: BeanManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showInactive by viewModel.showInactive.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    var showDeleteDialog by remember { mutableStateOf<Bean?>(null) }
    var showPhotoViewer by remember { mutableStateOf<String?>(null) }

    LandscapeContainer(
        modifier = Modifier.fillMaxSize(),
        portraitContent = {
            BeanManagementContent(
                uiState = uiState,
                searchQuery = searchQuery,
                showInactive = showInactive,
                onAddBeanClick = onAddBeanClick,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onToggleShowInactive = viewModel::toggleShowInactive,
                onEditBeanClick = onEditBeanClick,
                onDeleteBean = { showDeleteDialog = it },
                onSelectBean = { bean ->
                    if (bean.isActive) {
                        viewModel.setCurrentBean(bean.id)
                    }
                },
                onReactivateBean = { viewModel.reactivateBean(it) },
                onPhotoClick = { showPhotoViewer = it },
                onNavigateToShotHistory = onNavigateToShotHistory,
                onRetry = {
                    viewModel.clearError()
                    viewModel.refresh()
                },
                spacing = spacing
            )
        },
        landscapeContent = {
            BeanManagementContent(
                uiState = uiState,
                searchQuery = searchQuery,
                showInactive = showInactive,
                onAddBeanClick = onAddBeanClick,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onToggleShowInactive = viewModel::toggleShowInactive,
                onEditBeanClick = onEditBeanClick,
                onDeleteBean = { showDeleteDialog = it },
                onSelectBean = { bean ->
                    if (bean.isActive) {
                        viewModel.setCurrentBean(bean.id)
                    }
                },
                onReactivateBean = { viewModel.reactivateBean(it) },
                onPhotoClick = { showPhotoViewer = it },
                onNavigateToShotHistory = onNavigateToShotHistory,
                onRetry = {
                    viewModel.clearError()
                    viewModel.refresh()
                },
                spacing = spacing
            )
        }
    )

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
private fun BeanManagementContent(
    uiState: com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementUiState,
    searchQuery: String,
    showInactive: Boolean,
    onAddBeanClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowInactive: () -> Unit,
    onEditBeanClick: (String) -> Unit,
    onDeleteBean: (Bean) -> Unit,
    onSelectBean: (Bean) -> Unit,
    onReactivateBean: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    onNavigateToShotHistory: (String) -> Unit,
    onRetry: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    var showSearchDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBeanClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.text_add_bean)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = spacing.medium,
                    top = spacing.medium,
                    end = spacing.medium,
                    bottom = paddingValues.calculateBottomPadding() + spacing.medium
                )
        ) {
            // Integrated Search and Filter Bar
            IntegratedSearchAndFilterBar(
                searchQuery = searchQuery,
                showInactive = showInactive,
                onShowSearchDialog = { showSearchDialog = true },
                onToggleShowInactive = onToggleShowInactive,
                onClearFilters = {
                    onSearchQueryChange("")
                    if (showInactive) onToggleShowInactive()
                },
                spacing = spacing
            )

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
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.beans.isEmpty() -> {
                    EmptyState(
                        icon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                        title = stringResource(R.string.text_no_beans_available),
                        description = if (searchQuery.isNotEmpty()) {
                            stringResource(R.string.text_search_beans_hint)
                        } else {
                            stringResource(R.string.text_add_first_bean)
                        },
                        actionText = if (searchQuery.isEmpty()) stringResource(R.string.text_add_bean) else null
                    )
                }

                else -> {
                    BeanList(
                        beans = uiState.beans,
                        beanStatuses = uiState.beanStatuses,
                        beanShotCounts = uiState.beanShotCounts,
                        beanLastUsed = uiState.beanLastUsed,
                        beanGrinderSettings = uiState.beanGrinderSettings,
                        currentBeanId = uiState.currentBeanId,
                        onEditBeanClick = onEditBeanClick,
                        onDeleteBean = onDeleteBean,
                        onSelectBean = onSelectBean,
                        onNavigateToShotHistory = onNavigateToShotHistory,
                        onReactivateBean = onReactivateBean,
                        onPhotoClick = onPhotoClick,
                        spacing = spacing
                    )
                }
            }

            // Search Dialog
            if (showSearchDialog) {
                BeanSearchDialog(
                    currentQuery = searchQuery,
                    onDismiss = { showSearchDialog = false },
                    onApply = onSearchQueryChange
                )
            }
        }
    }
}

/**
 * Integrated search and filter bar following ShotHistoryScreen pattern.
 * Displays search chip, Active/All segmented button, and clear button.
 */
@Composable
private fun IntegratedSearchAndFilterBar(
    searchQuery: String,
    showInactive: Boolean,
    onShowSearchDialog: () -> Unit,
    onToggleShowInactive: () -> Unit,
    onClearFilters: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Search chip with badge indicator
        Box {
            FilterChip(
                selected = searchQuery.isNotEmpty(),
                onClick = onShowSearchDialog,
                label = { Text(stringResource(R.string.label_search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Active search indicator badge
            if (searchQuery.isNotEmpty()) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(stringResource(R.string.badge_recommendation_alert))
                }
            }
        }

        // Center: Segmented button for Active/All toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(horizontal = spacing.small)
        ) {
            SegmentedButton(
                selected = !showInactive,
                onClick = { if (showInactive) onToggleShowInactive() },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(
                    text = stringResource(R.string.text_active),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            SegmentedButton(
                selected = showInactive,
                onClick = { if (!showInactive) onToggleShowInactive() },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(
                    text = stringResource(R.string.text_all),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Right: Clear filters button (when active)
        if (searchQuery.isNotEmpty() || showInactive) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.button_clear_all),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Spacer to maintain layout consistency
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

/**
 * Dialog for entering search query.
 * Opens when search chip is clicked in IntegratedSearchAndFilterBar.
 */
@Composable
private fun BeanSearchDialog(
    currentQuery: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var searchText by remember { mutableStateOf(currentQuery) }
    val spacing = LocalSpacing.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.label_search_beans))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.placeholder_enter_bean_name_search),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                CoffeeTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = stringResource(R.string.label_search_beans),
                    placeholder = stringResource(R.string.placeholder_enter_bean_name_search),
                    leadingIcon = Icons.Default.Search,
                    trailingIcon = if (searchText.isNotEmpty()) Icons.Default.Clear else null,
                    onTrailingIconClick = if (searchText.isNotEmpty()) {
                        { searchText = "" }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(searchText)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.button_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.text_dialog_cancel))
            }
        }
    )
}

@Composable
private fun BeanList(
    beans: List<Bean>,
    beanStatuses: Map<String, com.jodli.coffeeshottimer.ui.util.BeanStatus>,
    beanShotCounts: Map<String, Int>,
    beanLastUsed: Map<String, java.time.LocalDateTime?>,
    beanGrinderSettings: Map<String, String?>,
    currentBeanId: String?,
    onEditBeanClick: (String) -> Unit,
    onDeleteBean: (Bean) -> Unit,
    onSelectBean: (Bean) -> Unit,
    onNavigateToShotHistory: (String) -> Unit,
    onReactivateBean: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    LandscapeContainer(
        portraitContent = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                contentPadding = PaddingValues(bottom = spacing.large)
            ) {
                items(
                    items = beans,
                    key = { bean -> bean.id }
                ) { bean ->
                    BeanListItem(
                        bean = bean,
                        beanStatus = beanStatuses[bean.id]
                            ?: com.jodli.coffeeshottimer.ui.util.BeanStatus.FRESH_START,
                        shotCount = beanShotCounts[bean.id] ?: 0,
                        lastUsedDate = beanLastUsed[bean.id],
                        grinderSetting = beanGrinderSettings[bean.id],
                        isCurrentBean = bean.id == currentBeanId,
                        onEdit = { onEditBeanClick(bean.id) },
                        onDelete = { onDeleteBean(bean) },
                        onSelect = { onSelectBean(bean) },
                        onViewHistory = { onNavigateToShotHistory(bean.id) },
                        onReactivate = if (!bean.isActive) {
                            { onReactivateBean(bean.id) }
                        } else {
                            null
                        },
                        onPhotoClick = { photoPath ->
                            onPhotoClick(photoPath)
                        }
                    )
                }
            }
        },
        landscapeContent = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                contentPadding = PaddingValues(bottom = spacing.large)
            ) {
                items(
                    items = beans,
                    key = { bean -> bean.id }
                ) { bean ->
                    BeanListItem(
                        bean = bean,
                        beanStatus = beanStatuses[bean.id]
                            ?: com.jodli.coffeeshottimer.ui.util.BeanStatus.FRESH_START,
                        shotCount = beanShotCounts[bean.id] ?: 0,
                        lastUsedDate = beanLastUsed[bean.id],
                        grinderSetting = beanGrinderSettings[bean.id],
                        isCurrentBean = bean.id == currentBeanId,
                        onEdit = { onEditBeanClick(bean.id) },
                        onDelete = { onDeleteBean(bean) },
                        onSelect = { onSelectBean(bean) },
                        onViewHistory = { onNavigateToShotHistory(bean.id) },
                        onReactivate = if (!bean.isActive) {
                            { onReactivateBean(bean.id) }
                        } else {
                            null
                        },
                        onPhotoClick = { photoPath ->
                            onPhotoClick(photoPath)
                        }
                    )
                }
            }
        }
    )
}

/**
 * Displays a bean item card with status indicator, grinder setting, and statistics.
 *
 * @param bean The bean to display
 * @param beanStatus Calculated quality status (DIALED_IN, EXPERIMENTING, etc.)
 * @param shotCount Number of shots recorded with this bean
 * @param lastUsedDate Last date the bean was used for a shot
 * @param isCurrentBean Whether this is the currently selected/active bean
 * @param onEdit Callback when user wants to edit the bean
 * @param onDelete Callback when user wants to delete the bean
 * @param onSelect Callback when user selects this bean for shots
 * @param onViewHistory Callback to navigate to shot history filtered by this bean
 * @param onReactivate Callback to reactivate an inactive bean (null for active beans)
 * @param onPhotoClick Callback when user clicks the bean photo
 */
@Composable
private fun BeanListItem(
    bean: Bean,
    beanStatus: com.jodli.coffeeshottimer.ui.util.BeanStatus,
    shotCount: Int,
    lastUsedDate: java.time.LocalDateTime?,
    grinderSetting: String?,
    isCurrentBean: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onViewHistory: () -> Unit,
    onReactivate: (() -> Unit)? = null,
    onPhotoClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

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
                            shape = RoundedCornerShape(spacing.cornerSmall)
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
                            contentDescription = if (bean.isActive) {
                                stringResource(R.string.button_delete_bean)
                            } else {
                                stringResource(R.string.button_permanently_delete_bean)
                            },
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
            BeanPhotoThumbnail(
                photoPath = bean.photoPath,
                onPhotoClick = if (bean.hasPhoto() && onPhotoClick != null) {
                    { onPhotoClick(bean.photoPath!!) }
                } else {
                    null
                }
            )

            BeanInformation(
                bean = bean,
                beanStatus = beanStatus,
                shotCount = shotCount,
                lastUsedDate = lastUsedDate,
                grinderSetting = grinderSetting,
                onViewHistory = onViewHistory,
                spacing = spacing,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        BeanActionSection(
            isActive = bean.isActive,
            isCurrentBean = isCurrentBean,
            onSelect = onSelect,
            spacing = spacing
        )
    }
}

@Composable
private fun BeanInformation(
    bean: Bean,
    beanStatus: com.jodli.coffeeshottimer.ui.util.BeanStatus,
    shotCount: Int,
    lastUsedDate: java.time.LocalDateTime?,
    grinderSetting: String?,
    onViewHistory: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing,
    modifier: Modifier = Modifier
) {
    val daysSinceRoast = bean.daysSinceRoast().toInt()
    val isFresh = bean.isFresh()

    Column(modifier = modifier) {
        RoastFreshnessIndicator(
            daysSinceRoast = daysSinceRoast,
            isFresh = isFresh,
            spacing = spacing
        )

        Spacer(modifier = Modifier.height(spacing.small))

        GrinderSettingWithStatus(
            grinderSetting = grinderSetting,
            beanStatus = beanStatus,
            spacing = spacing
        )

        Spacer(modifier = Modifier.height(spacing.small))

        BeanStatistics(
            shotCount = shotCount,
            lastUsedDate = lastUsedDate
        )

        if (shotCount > 0) {
            Spacer(modifier = Modifier.height(spacing.small))
            ViewShotsLink(
                shotCount = shotCount,
                onViewHistory = onViewHistory,
                spacing = spacing
            )
        }
    }
}

@Composable
private fun RoastFreshnessIndicator(
    daysSinceRoast: Int,
    isFresh: Boolean,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Text(
            text = stringResource(R.string.format_roasted_days_ago, daysSinceRoast),
            style = MaterialTheme.typography.bodySmall,
            color = if (isFresh) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Surface(
            color = if (isFresh) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = CircleShape
        ) {
            Text(
                text = if (isFresh) {
                    stringResource(R.string.text_fresh)
                } else {
                    when {
                        daysSinceRoast < 4 -> stringResource(R.string.text_too_fresh)
                        daysSinceRoast <= 45 -> stringResource(R.string.text_good)
                        daysSinceRoast <= 90 -> stringResource(R.string.text_dialog_ok)
                        else -> stringResource(R.string.text_stale)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isFresh) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.padding(
                    horizontal = spacing.small,
                    vertical = spacing.extraSmall / 2
                )
            )
        }
    }
}

@Composable
private fun GrinderSettingWithStatus(
    grinderSetting: String?,
    beanStatus: com.jodli.coffeeshottimer.ui.util.BeanStatus,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .semantics {
                    contentDescription = when (beanStatus) {
                        com.jodli.coffeeshottimer.ui.util.BeanStatus.DIALED_IN ->
                            context.getString(R.string.bean_status_dialed_in)
                        com.jodli.coffeeshottimer.ui.util.BeanStatus.EXPERIMENTING ->
                            context.getString(R.string.bean_status_experimenting)
                        com.jodli.coffeeshottimer.ui.util.BeanStatus.NEEDS_WORK ->
                            context.getString(R.string.bean_status_needs_work)
                        com.jodli.coffeeshottimer.ui.util.BeanStatus.FRESH_START ->
                            context.getString(R.string.bean_status_fresh_start)
                    }
                }
        ) {
            Surface(
                color = getStatusColor(beanStatus),
                shape = CircleShape,
                modifier = Modifier.fillMaxSize()
            ) {}
        }

        Text(
            text = grinderSetting?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.bean_grinder_not_set),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BeanStatistics(
    shotCount: Int,
    lastUsedDate: java.time.LocalDateTime?
) {
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.medium)) {
        Text(
            text = if (shotCount > 0) {
                stringResource(R.string.bean_shot_count, shotCount)
            } else {
                stringResource(R.string.bean_no_shots)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = formatLastUsed(lastUsedDate, context),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ViewShotsLink(
    shotCount: Int,
    onViewHistory: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onViewHistory)
            .semantics {
                contentDescription = context.getString(R.string.cd_view_shot_history)
            }
    ) {
        Text(
            text = stringResource(R.string.bean_view_shots, shotCount),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BeanActionSection(
    isActive: Boolean,
    isCurrentBean: Boolean,
    onSelect: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing
) {
    if (isActive) {
        if (isCurrentBean) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(spacing.cornerSmall)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = spacing.small,
                        vertical = spacing.extraSmall
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        stringResource(R.string.bean_selected_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            CoffeePrimaryButton(
                text = stringResource(R.string.bean_select_button),
                onClick = onSelect,
                fillMaxWidth = true
            )
        }
    }
}
