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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoThumbnail
import com.jodli.coffeeshottimer.ui.components.BeanStatusMark
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.components.SquareSegmentedSwitch
import com.jodli.coffeeshottimer.ui.components.ink
import com.jodli.coffeeshottimer.ui.components.labelRes
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.util.BeanStatus
import com.jodli.coffeeshottimer.ui.util.formatLastUsed
import com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementUiState
import com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementViewModel
import java.time.LocalDateTime

private val RowMinHeight = 72.dp
private val RowInset = 12.dp
private val RowVerticalPadding = 12.dp
private val RowThumbSize = 56.dp
private val CurrentBeanBarWidth = 3.dp
private val EditTargetSize = 48.dp
private val LinkArrowSize = 16.dp
private val GridColumnSpacing = 24.dp
private val ListBottomPadding = 88.dp

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

    var showPhotoViewer by remember { mutableStateOf<String?>(null) }

    BeanManagementContent(
        uiState = uiState,
        searchQuery = searchQuery,
        showInactive = showInactive,
        onAddBeanClick = onAddBeanClick,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onToggleShowInactive = viewModel::toggleShowInactive,
        onEditBeanClick = onEditBeanClick,
        onSelectBean = { bean ->
            if (bean.isActive) {
                viewModel.setCurrentBean(bean.id)
            }
        },
        onPhotoClick = { showPhotoViewer = it },
        onNavigateToShotHistory = onNavigateToShotHistory,
        onRetry = {
            viewModel.clearError()
            viewModel.refresh()
        }
    )

    // Photo Viewer
    showPhotoViewer?.let { photoPath ->
        PhotoViewer(
            photoPath = photoPath,
            photoUri = null,
            onDismiss = { showPhotoViewer = null }
        )
    }
}

/**
 * The beans ledger: one ruled row per bean, the bag in use marked with an ink
 * bar and a "Brewing" note, roast age as a figure, edit at the row end.
 * Portrait and landscape share this single content pass; only the list
 * container and the outer padding differ.
 */
@Composable
private fun BeanManagementContent(
    uiState: BeanManagementUiState,
    searchQuery: String,
    showInactive: Boolean,
    onAddBeanClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowInactive: () -> Unit,
    onEditBeanClick: (String) -> Unit,
    onSelectBean: (Bean) -> Unit,
    onPhotoClick: (String) -> Unit,
    onNavigateToShotHistory: (String) -> Unit,
    onRetry: () -> Unit
) {
    val spacing = LocalSpacing.current
    var showSearchDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBeanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                    pressedElevation = 0.dp
                )
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
            IntegratedSearchAndFilterBar(
                searchQuery = searchQuery,
                showInactive = showInactive,
                onShowSearchDialog = { showSearchDialog = true },
                onToggleShowInactive = onToggleShowInactive,
                onClearFilters = { onSearchQueryChange("") }
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = spacing.small),
                color = MaterialTheme.colorScheme.outline
            )

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
                        message = uiState.error ?: stringResource(R.string.text_unknown_error),
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
                        }
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
                        onSelectBean = onSelectBean,
                        onNavigateToShotHistory = onNavigateToShotHistory,
                        onPhotoClick = onPhotoClick
                    )
                }
            }

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
 * Active|All switch left, search chip and (while searching) a clear button
 * right — the same bar pattern as the shot history screen.
 */
@Composable
private fun IntegratedSearchAndFilterBar(
    searchQuery: String,
    showInactive: Boolean,
    onShowSearchDialog: () -> Unit,
    onToggleShowInactive: () -> Unit,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareSegmentedSwitch(
            options = listOf(
                stringResource(R.string.text_active),
                stringResource(R.string.text_all)
            ),
            selectedIndex = if (showInactive) 1 else 0,
            onSelect = { onToggleShowInactive() }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = searchQuery.isNotEmpty(),
                onClick = onShowSearchDialog,
                shape = MaterialTheme.shapes.small,
                label = { Text(stringResource(R.string.label_search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(EditTargetSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.button_clear_all),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(EditTargetSize))
            }
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

/** Portrait: one column. Landscape: two. Every row closes with a hairline rule. */
@Composable
private fun BeanList(
    beans: List<Bean>,
    beanStatuses: Map<String, BeanStatus>,
    beanShotCounts: Map<String, Int>,
    beanLastUsed: Map<String, LocalDateTime?>,
    beanGrinderSettings: Map<String, String?>,
    currentBeanId: String?,
    onEditBeanClick: (String) -> Unit,
    onSelectBean: (Bean) -> Unit,
    onNavigateToShotHistory: (String) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val rowFor: @Composable (Bean) -> Unit = { bean ->
        BeanLedgerItem(
            bean = bean,
            beanStatus = beanStatuses[bean.id] ?: BeanStatus.FRESH_START,
            shotCount = beanShotCounts[bean.id] ?: 0,
            lastUsedDate = beanLastUsed[bean.id],
            grinderSetting = beanGrinderSettings[bean.id],
            isCurrentBean = bean.id == currentBeanId && bean.isActive,
            onEdit = { onEditBeanClick(bean.id) },
            onSelect = { onSelectBean(bean) },
            onViewHistory = { onNavigateToShotHistory(bean.id) },
            onPhotoClick = onPhotoClick
        )
    }
    if (LocalIsLandscape.current) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(GridColumnSpacing),
            contentPadding = PaddingValues(bottom = ListBottomPadding)
        ) {
            items(items = beans, key = { bean -> bean.id }) { bean ->
                rowFor(bean)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ListBottomPadding)
        ) {
            items(items = beans, key = { bean -> bean.id }) { bean ->
                rowFor(bean)
            }
        }
    }
}

/** Row plus its closing hairline rule. */
@Composable
private fun BeanLedgerItem(
    bean: Bean,
    beanStatus: BeanStatus,
    shotCount: Int,
    lastUsedDate: LocalDateTime?,
    grinderSetting: String?,
    isCurrentBean: Boolean,
    onEdit: () -> Unit,
    onSelect: () -> Unit,
    onViewHistory: () -> Unit,
    onPhotoClick: ((String) -> Unit)?
) {
    Column {
        BeanLedgerRow(
            bean = bean,
            beanStatus = beanStatus,
            shotCount = shotCount,
            lastUsedDate = lastUsedDate,
            grinderSetting = grinderSetting,
            isCurrentBean = isCurrentBean,
            onEdit = onEdit,
            onSelect = onSelect,
            onViewHistory = onViewHistory,
            onPhotoClick = onPhotoClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * One bean as a ledger line. Tapping the row selects the bean for Record Shot
 * (inactive beans open the edit form instead); the photo thumbnail and the
 * "N shots" link keep their own click targets. The currently-brewing bean is
 * marked with a 3dp ink bar on the leading edge and a note in the status line.
 */
@Composable
private fun BeanLedgerRow(
    bean: Bean,
    beanStatus: BeanStatus,
    shotCount: Int,
    lastUsedDate: LocalDateTime?,
    grinderSetting: String?,
    isCurrentBean: Boolean,
    onEdit: () -> Unit,
    onSelect: () -> Unit,
    onViewHistory: () -> Unit,
    onPhotoClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val daysSinceRoast = bean.daysSinceRoast()
    val freshness = bean.roastFreshness()
    val roastAgeDescription = stringResource(
        R.string.cd_roast_age,
        daysSinceRoast,
        stringResource(freshness.labelRes())
    )
    val barColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .heightIn(min = RowMinHeight)
            .fillMaxWidth()
            .drawBehind {
                if (isCurrentBean) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset.Zero,
                        size = Size(CurrentBeanBarWidth.toPx(), size.height)
                    )
                }
            }
            .clickable(
                onClickLabel = stringResource(
                    if (bean.isActive) R.string.bean_select_button else R.string.cd_edit_bean
                ),
                onClick = { if (bean.isActive) onSelect() else onEdit() }
            )
            .padding(
                start = RowInset,
                top = RowVerticalPadding,
                bottom = RowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bean.photoPath?.takeIf { it.isNotBlank() }?.let { photoPath ->
            BeanPhotoThumbnail(
                photoPath = photoPath,
                size = RowThumbSize,
                onPhotoClick = onPhotoClick?.let { callback -> { callback(photoPath) } }
            )
            Spacer(modifier = Modifier.width(RowInset))
        }

        BeanRowDetails(
            bean = bean,
            beanStatus = beanStatus,
            shotCount = shotCount,
            lastUsedDate = lastUsedDate,
            grinderSetting = grinderSetting,
            isCurrentBean = isCurrentBean,
            onViewHistory = onViewHistory,
            modifier = Modifier.weight(1f)
        )

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .semantics { contentDescription = roastAgeDescription }
        ) {
            Text(
                text = stringResource(R.string.format_roast_age_days, daysSinceRoast),
                style = MaterialTheme.typography.titleMedium,
                color = freshness.ink(),
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.label_roast),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(EditTargetSize)) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit_bean),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The row's middle column: name, grind/last-used line, then the status mark
 * with the "Brewing" note for the bean in use and the shots link at the end.
 */
@Composable
private fun BeanRowDetails(
    bean: Bean,
    beanStatus: BeanStatus,
    shotCount: Int,
    lastUsedDate: LocalDateTime?,
    grinderSetting: String?,
    isCurrentBean: Boolean,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val grindText = grinderSetting?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.format_grind_inline, it) }
    val metaLine = listOfNotNull(
        grindText,
        formatLastUsed(lastUsedDate, context)
    ).joinToString(" · ")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
    ) {
        Text(
            text = bean.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (bean.isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metaLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BeanStatusMark(status = beanStatus, isActive = bean.isActive)
            if (isCurrentBean) {
                Text(
                    text = " · " + stringResource(R.string.label_brewing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.zoneColors.idealInk
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (shotCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = EditTargetSize)
                        .clickable(onClick = onViewHistory)
                        .semantics {
                            contentDescription =
                                context.getString(R.string.cd_view_shot_history)
                        }
                ) {
                    Text(
                        text = stringResource(R.string.bean_shot_count, shotCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(spacing.extraSmall))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(LinkArrowSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.bean_no_shots),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
