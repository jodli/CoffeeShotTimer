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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoThumbnailLarge
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.util.formatLastUsed
import com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementViewModel

/**
 * Freshness badge tier thresholds (in days since roast) — see [BeanBadgesRow].
 * Mirrors the S1 spec: <4 = Too Fresh (warning), ≤45 = Fresh, ≤90 = OK, else Stale.
 */
private const val FRESHNESS_TOO_FRESH_MAX_DAYS = 4
private const val FRESHNESS_FRESH_MAX_DAYS = 45
private const val FRESHNESS_OK_MAX_DAYS = 90

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
                },
                spacing = spacing
            )
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

@Composable
private fun BeanManagementContent(
    uiState: com.jodli.coffeeshottimer.ui.viewmodel.BeanManagementUiState,
    searchQuery: String,
    showInactive: Boolean,
    onAddBeanClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowInactive: () -> Unit,
    onEditBeanClick: (String) -> Unit,
    onSelectBean: (Bean) -> Unit,
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
                        onSelectBean = onSelectBean,
                        onNavigateToShotHistory = onNavigateToShotHistory,
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
    onSelectBean: (Bean) -> Unit,
    onNavigateToShotHistory: (String) -> Unit,
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
                        onSelect = { onSelectBean(bean) },
                        onViewHistory = { onNavigateToShotHistory(bean.id) },
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
                        onSelect = { onSelectBean(bean) },
                        onViewHistory = { onNavigateToShotHistory(bean.id) },
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
 * Displays a bean item card as a photo-prominent profile.
 *
 * Layout (S1):
 *   header Row → (leading 64dp photo, or inline bean icon when no photo)
 *                 + meta Column[name, roast date, BeanBadgesRow, BeanStatsRow]
 *                 + trailing selection control
 *   ViewShotsLink (align End, gated on shotCount > 0)
 *
 * Destructive actions (delete/reactivate) live in the edit screen — the card
 * body opens it via tap.
 *
 * @param bean The bean to display
 * @param beanStatus Calculated quality status (DIALED_IN, EXPERIMENTING, etc.)
 * @param shotCount Number of shots recorded with this bean
 * @param lastUsedDate Last date the bean was used for a shot
 * @param grinderSetting Most-recent grinder setting for the bean (null/blank → "Not set")
 * @param isCurrentBean Whether this is the currently selected/active bean
 * @param onEdit Callback when user wants to edit the bean
 * @param onSelect Callback when user selects this bean for shots
 * @param onViewHistory Callback to navigate to shot history filtered by this bean
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
    onSelect: () -> Unit,
    onViewHistory: () -> Unit,
    onPhotoClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val daysSinceRoast = bean.daysSinceRoast().toInt()
    val isFresh = bean.isFresh()

    CoffeeCard(
        modifier = modifier,
        onClick = onEdit
    ) {
        // Header Row: leading photo (omitted when no photo — Q5: the meta Column
        // then reflows to the card edge naturally, no padding arithmetic) +
        // weighted meta Column + trailing selection control.
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Leading 64dp hero photo — only when a photo exists. Renders nothing
            // (no spacer Box) when photoPath is null, so the Row collapses.
            if (bean.hasPhoto()) {
                BeanPhotoThumbnailLarge(
                    photoPath = bean.photoPath,
                    onPhotoClick = if (onPhotoClick != null) {
                        { onPhotoClick(bean.photoPath!!) }
                    } else {
                        null
                    }
                )
            }

            // Meta Column: name (+ inline bean icon when no photo) → roast date →
            // BeanBadgesRow → BeanStatsRow. All content inherits the same start
            // indent from the photo's presence/absence (Q5 — no padding arithmetic).
            Column(modifier = Modifier.weight(1f)) {
                if (bean.hasPhoto()) {
                    Text(
                        text = bean.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // No photo → small bean icon inline before the name.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(spacing.iconSmall)
                        )
                        Text(
                            text = bean.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.small))

                // Roast date text (formerly the leading text of RoastFreshnessIndicator).
                // The freshness *pill* is now a text badge inside BeanBadgesRow below.
                Text(
                    text = stringResource(R.string.format_roasted_days_ago, daysSinceRoast),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFresh) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(spacing.small))

                BeanBadgesRow(bean = bean, beanStatus = beanStatus)

                Spacer(modifier = Modifier.height(spacing.small))

                BeanStatsRow(
                    grinderSetting = grinderSetting,
                    shotCount = shotCount,
                    lastUsedDate = lastUsedDate
                )
            }

            // Trailing selection control (header slot)
            when {
                bean.isActive && isCurrentBean -> SelectedBadge()
                bean.isActive && !isCurrentBean -> CoffeeSecondaryButton(
                    text = stringResource(R.string.bean_select_button),
                    onClick = onSelect,
                    fillMaxWidth = false
                )
            }
        }

        // View shots link — gated on shotCount > 0, re-aligned to the card end
        // (Q5: under the meta column, right-aligned). Kept as its own clickable
        // target with cd_view_shot_history semantics.
        if (shotCount > 0) {
            Spacer(modifier = Modifier.height(spacing.small))
            ViewShotsLink(
                shotCount = shotCount,
                onViewHistory = onViewHistory,
                spacing = spacing,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * Compact "Selected" badge for the header slot of the currently-selected bean.
 * Surface(primaryContainer) + Check + label, content colored onPrimaryContainer
 * (fixes the prior primary-content inconsistency flagged in research §4).
 */
@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(spacing.cornerSmall),
        modifier = modifier
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
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(spacing.iconSmall)
            )
            Text(
                text = stringResource(R.string.bean_selected_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Reusable small-label badge following the prevailing pill recipe (research §4):
 * Surface(container, cornerSmall) wrapping a labelSmall Text with tight vertical
 * padding (extraSmall/2) and horizontal small padding.
 */
@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(spacing.cornerSmall),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = spacing.small,
                vertical = spacing.extraSmall / 2
            )
        )
    }
}

/**
 * Freshness + bean-status text badges. Replaces the old colored status dot
 * (GrinderSettingWithStatus) and the roast-freshness pill (RoastFreshnessIndicator)
 * with self-describing text badges. Container/content colors come from colorScheme
 * pairs so the label stays readable; the 1:1 semantic mapping follows getStatusColor.
 *
 * Freshness tiers (by daysSinceRoast):
 *   <4   → text_too_fresh  / errorContainer    (warning — beans too green)
 *   ≤45  → text_fresh      / primaryContainer  (peak window)
 *   ≤90  → text_ok_freshness / secondaryContainer (still usable)
 *   else → text_stale      / surfaceVariant    (past peak)
 *
 * Status mapping:
 *   DIALED_IN     → primaryContainer
 *   EXPERIMENTING → tertiaryContainer
 *   NEEDS_WORK    → errorContainer
 *   FRESH_START   → surfaceVariant
 */
@Composable
private fun BeanBadgesRow(
    bean: Bean,
    beanStatus: com.jodli.coffeeshottimer.ui.util.BeanStatus,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val colorScheme = MaterialTheme.colorScheme
    val daysSinceRoast = bean.daysSinceRoast().toInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Freshness badge — always rendered (Bean.roastDate is non-nullable).
        val freshnessLabel = when {
            daysSinceRoast < FRESHNESS_TOO_FRESH_MAX_DAYS -> stringResource(R.string.text_too_fresh)
            daysSinceRoast <= FRESHNESS_FRESH_MAX_DAYS -> stringResource(R.string.text_fresh)
            daysSinceRoast <= FRESHNESS_OK_MAX_DAYS -> stringResource(R.string.text_ok_freshness)
            else -> stringResource(R.string.text_stale)
        }
        val (freshnessContainer, freshnessContent) = when {
            daysSinceRoast < FRESHNESS_TOO_FRESH_MAX_DAYS ->
                colorScheme.errorContainer to colorScheme.onErrorContainer
            daysSinceRoast <= FRESHNESS_FRESH_MAX_DAYS ->
                colorScheme.primaryContainer to colorScheme.onPrimaryContainer
            daysSinceRoast <= FRESHNESS_OK_MAX_DAYS ->
                colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
            else ->
                colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
        }
        StatusBadge(
            text = freshnessLabel,
            containerColor = freshnessContainer,
            contentColor = freshnessContent
        )

        // Status badge — always rendered.
        val statusLabel = when (beanStatus) {
            com.jodli.coffeeshottimer.ui.util.BeanStatus.DIALED_IN ->
                stringResource(R.string.bean_status_dialed_in_short)
            com.jodli.coffeeshottimer.ui.util.BeanStatus.EXPERIMENTING ->
                stringResource(R.string.bean_status_experimenting_short)
            com.jodli.coffeeshottimer.ui.util.BeanStatus.NEEDS_WORK ->
                stringResource(R.string.bean_status_needs_work_short)
            com.jodli.coffeeshottimer.ui.util.BeanStatus.FRESH_START ->
                stringResource(R.string.bean_status_fresh_start_short)
        }
        val (statusContainer, statusContent) = when (beanStatus) {
            com.jodli.coffeeshottimer.ui.util.BeanStatus.DIALED_IN ->
                colorScheme.primaryContainer to colorScheme.onPrimaryContainer
            com.jodli.coffeeshottimer.ui.util.BeanStatus.EXPERIMENTING ->
                colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
            com.jodli.coffeeshottimer.ui.util.BeanStatus.NEEDS_WORK ->
                colorScheme.errorContainer to colorScheme.onErrorContainer
            com.jodli.coffeeshottimer.ui.util.BeanStatus.FRESH_START ->
                colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
        }
        StatusBadge(
            text = statusLabel,
            containerColor = statusContainer,
            contentColor = statusContent
        )
    }
}

/**
 * Compact one-row stats: `grinder · shots · last used`.
 * Replaces the old GrinderSettingWithStatus + BeanStatistics pair with a single
 * bodySmall/onSurfaceVariant row aligned with the rest of the meta Column
 * (Q5 — lives inside the header Row's weighted Column, so it inherits the
 * same start indent as name/badges without padding arithmetic).
 *
 * Grinder falls back to `bean_grinder_not_set` when null/blank; shot count
 * switches `bean_shot_count` ↔ `bean_no_shots` at zero; last-used goes through
 * `formatLastUsed` (which itself falls back to `bean_last_used_never`).
 */
@Composable
private fun BeanStatsRow(
    grinderSetting: String?,
    shotCount: Int,
    lastUsedDate: java.time.LocalDateTime?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val statsStyle = MaterialTheme.typography.bodySmall
    val statsColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = grinderSetting?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.bean_grinder_not_set),
            style = statsStyle,
            color = statsColor
        )
        Text(
            text = "·",
            style = statsStyle,
            color = statsColor
        )
        Text(
            text = if (shotCount > 0) {
                stringResource(R.string.bean_shot_count, shotCount)
            } else {
                stringResource(R.string.bean_no_shots)
            },
            style = statsStyle,
            color = statsColor
        )
        Text(
            text = "·",
            style = statsStyle,
            color = statsColor
        )
        Text(
            text = formatLastUsed(lastUsedDate, context),
            style = statsStyle,
            color = statsColor
        )
    }
}

@Composable
private fun ViewShotsLink(
    shotCount: Int,
    onViewHistory: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
