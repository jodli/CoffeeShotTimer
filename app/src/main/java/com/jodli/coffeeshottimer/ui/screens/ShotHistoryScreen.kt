package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.SectionHeader
import com.jodli.coffeeshottimer.ui.components.ShotHistoryFilterDialog
import com.jodli.coffeeshottimer.ui.components.CompactTasteDisplay
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.Spacing
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.ShotHistoryUiState
import com.jodli.coffeeshottimer.ui.viewmodel.ShotHistoryViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ShotHistoryScreen(
    onShotClick: (String) -> Unit = {},
    viewModel: ShotHistoryViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    // Auto-refresh when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshOnResume()
    }

    LandscapeContainer(
        modifier = Modifier.fillMaxSize(),
        portraitContent = {
            ShotHistoryPortraitContent(
                uiState = uiState,
                currentFilter = currentFilter,
                onToggleAnalysisView = { viewModel.toggleAnalysisView() },
                onShowFilterDialog = { showFilterDialog = true },
                onShotClick = onShotClick,
                onRefreshData = { viewModel.refreshData() },
                onLoadMore = { viewModel.loadMore() },
                onClearFilters = { viewModel.clearFilters() },
                getBeanName = { beanId -> viewModel.getBeanName(beanId) },
                spacing = spacing
            )
        },
        landscapeContent = {
            ShotHistoryLandscapeContent(
                uiState = uiState,
                currentFilter = currentFilter,
                onToggleAnalysisView = { viewModel.toggleAnalysisView() },
                onShowFilterDialog = { showFilterDialog = true },
                onShotClick = onShotClick,
                onRefreshData = { viewModel.refreshData() },
                onLoadMore = { viewModel.loadMore() },
                onClearFilters = { viewModel.clearFilters() },
                getBeanName = { beanId -> viewModel.getBeanName(beanId) },
                spacing = spacing
            )
        }
    )
    // Filter dialog (shared between portrait and landscape)
    if (showFilterDialog) {
        ShotHistoryFilterDialog(
            currentFilter = currentFilter,
            availableBeans = uiState.availableBeans,
            onApplyFilter = { filter ->
                viewModel.applyFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

/**
 * Portrait layout content for ShotHistoryScreen
 * Clean layout without redundant screen title
 */
@Composable
private fun ShotHistoryPortraitContent(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onShotClick: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLoadMore: () -> Unit,
    onClearFilters: () -> Unit,
    getBeanName: (String) -> String,
    spacing: Spacing,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.screenPadding)
    ) {
        // Action buttons only
        ShotHistoryActionButtons(
            currentFilter = currentFilter,
            showAnalysis = uiState.showAnalysis,
            onToggleAnalysisView = onToggleAnalysisView,
            onShowFilterDialog = onShowFilterDialog
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Active filters display
        if (currentFilter.hasFilters()) {
            ActiveFiltersDisplay(
                filter = currentFilter,
                availableBeans = uiState.availableBeans,
                onClearFilters = onClearFilters,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Content
        ShotHistoryContent(
            uiState = uiState,
            currentFilter = currentFilter,
            onShotClick = onShotClick,
            onRefreshData = onRefreshData,
            onLoadMore = onLoadMore,
            getBeanName = getBeanName,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Landscape layout content for ShotHistoryScreen  
 * Clean layout with landscape-aware spacing, no redundant title
 */
@Composable
private fun ShotHistoryLandscapeContent(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onShotClick: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLoadMore: () -> Unit,
    onClearFilters: () -> Unit,
    getBeanName: (String) -> String,
    spacing: Spacing,
    modifier: Modifier = Modifier
) {
    val landscapeSpacing = spacing.landscapeSpacing()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(landscapeSpacing)
    ) {
        // Action buttons only
        ShotHistoryActionButtons(
            currentFilter = currentFilter,
            showAnalysis = uiState.showAnalysis,
            onToggleAnalysisView = onToggleAnalysisView,
            onShowFilterDialog = onShowFilterDialog
        )

        Spacer(modifier = Modifier.height(landscapeSpacing))

        // Active filters display
        if (currentFilter.hasFilters()) {
            ActiveFiltersDisplay(
                filter = currentFilter,
                availableBeans = uiState.availableBeans,
                onClearFilters = onClearFilters,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(landscapeSpacing))
        }

        // Content with landscape-optimized shot items
        ShotHistoryLandscapeList(
            uiState = uiState,
            currentFilter = currentFilter,
            onShotClick = onShotClick,
            onRefreshData = onRefreshData,
            onLoadMore = onLoadMore,
            getBeanName = getBeanName,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Action buttons for analysis toggle and filtering
 * No redundant screen title since navigation bar provides context
 */
@Composable
private fun ShotHistoryActionButtons(
    currentFilter: ShotHistoryFilter,
    showAnalysis: Boolean,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        // Analysis toggle button
        IconButton(
            onClick = onToggleAnalysisView
        ) {
            Icon(
                imageVector = if (showAnalysis) Icons.AutoMirrored.Filled.List else Icons.Default.Info,
                contentDescription = if (showAnalysis) stringResource(R.string.cd_shot_list) else stringResource(R.string.cd_analysis),
                tint = if (showAnalysis) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Filter button
        IconButton(
            onClick = onShowFilterDialog
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.cd_filter_shots),
                tint = if (currentFilter.hasFilters()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Content section that handles different states (loading, error, empty, analysis, list)
 * Used by both portrait and landscape layouts
 */
@Composable
private fun ShotHistoryContent(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    onShotClick: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLoadMore: () -> Unit,
    getBeanName: (String) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    message = stringResource(R.string.loading_shot_history)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.error_loading_shots),
                    message = uiState.error ?: "Unknown error occurred",
                    onRetry = onRefreshData,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.isEmpty -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = if (currentFilter.hasFilters()) stringResource(R.string.cd_no_shots) else stringResource(R.string.cd_no_shots_recorded),
                    description = if (currentFilter.hasFilters()) {
                        stringResource(R.string.text_search_beans_hint)
                    } else {
                        stringResource(R.string.text_record_shots_analysis)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.showAnalysis -> {
                ShotAnalysisView(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                ShotHistoryList(
                    shots = uiState.shots,
                    getBeanName = getBeanName,
                    onShotClick = onShotClick,
                    isLoadingMore = uiState.isLoadingMore,
                    hasMorePages = uiState.hasMorePages,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Landscape-optimized content that uses enhanced shot items
 */
@Composable
private fun ShotHistoryLandscapeList(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    onShotClick: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLoadMore: () -> Unit,
    getBeanName: (String) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    message = stringResource(R.string.loading_shot_history)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.error_loading_shots),
                    message = uiState.error ?: "Unknown error occurred",
                    onRetry = onRefreshData,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.isEmpty -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = if (currentFilter.hasFilters()) stringResource(R.string.cd_no_shots) else stringResource(R.string.cd_no_shots_recorded),
                    description = if (currentFilter.hasFilters()) {
                        stringResource(R.string.text_search_beans_hint)
                    } else {
                        stringResource(R.string.text_record_shots_analysis)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.showAnalysis -> {
                ShotAnalysisView(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                ShotHistoryLandscapeListContent(
                    shots = uiState.shots,
                    getBeanName = getBeanName,
                    onShotClick = onShotClick,
                    isLoadingMore = uiState.isLoadingMore,
                    hasMorePages = uiState.hasMorePages,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Enhanced shot list for landscape mode
 * Features wider cards with horizontal metric chip layout
 */
@Composable
private fun ShotHistoryLandscapeListContent(
    shots: List<Shot>,
    getBeanName: (String) -> String,
    onShotClick: (String) -> Unit,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
    ) {
        items(shots) { shot ->
            ShotHistoryLandscapeItem(
                shot = shot,
                beanName = getBeanName(shot.beanId),
                onClick = { onShotClick(shot.id) }
            )
        }

        // Load more indicator and trigger
        if (hasMorePages) {
            item {
                LoadMoreItem(
                    isLoading = isLoadingMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Enhanced shot history item optimized for landscape layout
 * Features horizontal metric chip arrangement and better space utilization
 */
@Composable
private fun ShotHistoryLandscapeItem(
    shot: Shot,
    beanName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val isLandscape = LocalIsLandscape.current

    CoffeeCard(
        onClick = onClick,
        modifier = modifier
    ) {
        // Single row layout that utilizes the full width in landscape
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: Bean name and timestamp (compact)
            Column(
                modifier = Modifier.weight(0.25f)
            ) {
                Text(
                    text = beanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shot.timestamp.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Center section: Horizontal metric chips (main landscape enhancement)
            Row(
                modifier = Modifier.weight(0.5f),
                horizontalArrangement = Arrangement.spacedBy(landscapeSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricChip(
                    label = stringResource(R.string.label_ratio),
                    value = shot.getFormattedBrewRatio(),
                    isGood = shot.isTypicalBrewRatio()
                )
                MetricChip(
                    label = stringResource(R.string.label_time),
                    value = shot.getFormattedExtractionTime(),
                    isGood = shot.isOptimalExtractionTime()
                )
                if (shot.grinderSetting.isNotBlank()) {
                    MetricChip(
                        label = stringResource(R.string.label_grinder),
                        value = shot.grinderSetting,
                        isNeutral = true
                    )
                }
            }

            // Right section: Weights, quality indicator, and success indicator
            Column(
                modifier = Modifier.weight(0.25f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.format_weight_in_out, shot.coffeeWeightIn.toInt(), shot.coffeeWeightOut.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    QualityIndicator(
                        isOptimalTime = shot.isOptimalExtractionTime(),
                        isTypicalRatio = shot.isTypicalBrewRatio()
                    )
                }
                ShotSuccessIndicator(
                    shot = shot,
                    modifier = Modifier.padding(top = spacing.extraSmall)
                )
            }
        }
    }
}

@Composable
private fun ActiveFiltersDisplay(
    filter: ShotHistoryFilter,
    availableBeans: List<com.jodli.coffeeshottimer.data.model.Bean>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val isLandscape = LocalIsLandscape.current

    if (isLandscape) {
        // Compact landscape version - horizontal chip layout
        ActiveFiltersLandscapeDisplay(
            filter = filter,
            availableBeans = availableBeans,
            onClearFilters = onClearFilters,
            modifier = modifier
        )
    } else {
        // Full portrait version - vertical card layout
        ActiveFiltersPortraitDisplay(
            filter = filter,
            availableBeans = availableBeans,
            onClearFilters = onClearFilters,
            modifier = modifier
        )
    }
}

@Composable
private fun ActiveFiltersPortraitDisplay(
    filter: ShotHistoryFilter,
    availableBeans: List<com.jodli.coffeeshottimer.data.model.Bean>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        CardHeader(
            icon = Icons.Default.FilterList,
            title = stringResource(R.string.text_active_filters),
            actions = {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.button_clear_all))
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Display active filters
        val filterTexts = getActiveFilterTexts(filter, availableBeans)
        
        filterTexts.forEach { text ->
            Text(
                text = stringResource(R.string.symbol_bullet, text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ActiveFiltersLandscapeDisplay(
    filter: ShotHistoryFilter,
    availableBeans: List<com.jodli.coffeeshottimer.data.model.Bean>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val filterTexts = getActiveFilterTexts(filter, availableBeans)

    // Compact horizontal layout for landscape
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.cornerLarge),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter icon
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(spacing.iconSmall)
            )

            // Filter chips in horizontal flow
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                filterTexts.take(3).forEach { text -> // Limit to 3 filters to prevent overflow
                    FilterChip(
                        text = text,
                        modifier = Modifier
                    )
                }
                
                if (filterTexts.size > 3) {
                    FilterChip(
                        text = stringResource(R.string.format_more_filters, filterTexts.size - 3),
                        modifier = Modifier
                    )
                }
            }

            // Clear button
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = spacing.small, vertical = spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.button_clear_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.cornerMedium),
        color = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall / 2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getActiveFilterTexts(
    filter: ShotHistoryFilter,
    availableBeans: List<com.jodli.coffeeshottimer.data.model.Bean>
): List<String> {
    val filterTexts = mutableListOf<String>()

    filter.beanId?.let { beanId ->
        val beanName = availableBeans.find { it.id == beanId }?.name ?: "Unknown Bean"
        filterTexts.add("Bean: $beanName")
    }

    if (filter.startDate != null || filter.endDate != null) {
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
        val startText = filter.startDate?.format(dateFormatter) ?: "Start"
        val endText = filter.endDate?.format(dateFormatter) ?: "End"
        filterTexts.add("$startText - $endText")
    }

    filter.grinderSetting?.let { setting ->
        filterTexts.add("Grinder: $setting")
    }

    if (filter.minBrewRatio != null || filter.maxBrewRatio != null) {
        val min = filter.minBrewRatio?.let { "%.1f".format(it) } ?: "0"
        val max = filter.maxBrewRatio?.let { "%.1f".format(it) } ?: "∞"
        filterTexts.add("Ratio: $min-$max")
    }

    if (filter.minExtractionTime != null || filter.maxExtractionTime != null) {
        val min = filter.minExtractionTime ?: 0
        val max = filter.maxExtractionTime ?: 999
        filterTexts.add("Time: ${min}s-${max}s")
    }

    if (filter.onlyOptimalExtractionTime == true) {
        filterTexts.add("Optimal Time")
    }

    if (filter.onlyTypicalBrewRatio == true) {
        filterTexts.add("Typical Ratio")
    }

    return filterTexts
}

@Composable
private fun ShotHistoryList(
    shots: List<Shot>,
    getBeanName: (String) -> String,
    onShotClick: (String) -> Unit,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        items(shots) { shot ->
            ShotHistoryItem(
                shot = shot,
                beanName = getBeanName(shot.beanId),
                onClick = { onShotClick(shot.id) }
            )
        }

        // Load more indicator and trigger
        if (hasMorePages) {
            item {
                LoadMoreItem(
                    isLoading = isLoadingMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShotHistoryItem(
    shot: Shot,
    beanName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    CoffeeCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left side - main info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Bean name and timestamp
                Text(
                    text = beanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = shot.timestamp.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Key metrics chips (wrap when not enough width)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    MetricChip(
                        label = stringResource(R.string.label_ratio),
                        value = shot.getFormattedBrewRatio(),
                        isGood = shot.isTypicalBrewRatio()
                    )
                    MetricChip(
                        label = stringResource(R.string.label_time),
                        value = shot.getFormattedExtractionTime(),
                        isGood = shot.isOptimalExtractionTime()
                    )
                    if (shot.grinderSetting.isNotBlank()) {
                        MetricChip(
                            label = stringResource(R.string.label_grinder),
                            value = shot.grinderSetting,
                            isNeutral = true
                        )
                    }
                    
                    // Taste feedback display
                    CompactTasteDisplay(
                        tastePrimary = shot.tastePrimary,
                        tasteSecondary = shot.tasteSecondary
                    )
                }
            }

            // Right side - weights + quality inline, success below
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.format_weight_in_out, shot.coffeeWeightIn.toInt(), shot.coffeeWeightOut.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    QualityIndicator(
                        isOptimalTime = shot.isOptimalExtractionTime(),
                        isTypicalRatio = shot.isTypicalBrewRatio()
                    )
                }
                ShotSuccessIndicator(
                    shot = shot,
                    modifier = Modifier.padding(top = spacing.extraSmall)
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    isGood: Boolean = false,
    isNeutral: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val backgroundColor = when {
        isNeutral -> MaterialTheme.colorScheme.surfaceVariant
        isGood -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when {
        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.cornerLarge - spacing.extraSmall),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun QualityIndicator(
    isOptimalTime: Boolean,
    isTypicalRatio: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
    ) {
        // Time indicator
        Box(
            modifier = Modifier
                .size(spacing.qualityIndicator)
                .clip(CircleShape)
                .background(
                    if (isOptimalTime) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
        )

        // Ratio indicator
        Box(
            modifier = Modifier
                .size(spacing.qualityIndicator)
                .clip(CircleShape)
                .background(
                    if (isTypicalRatio) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
        )
    }
}

@Composable
private fun ShotAnalysisView(
    uiState: ShotHistoryUiState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val isLandscape = LocalIsLandscape.current

    if (uiState.analysisLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(message = stringResource(R.string.loading_analysis))
        }
        return
    }

    if (!uiState.hasAnalysisData) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Default.Info,
                title = stringResource(R.string.text_no_analysis_data),
                description = stringResource(R.string.text_record_shots_analysis)
            )
        }
        return
    }

    if (isLandscape) {
        ShotAnalysisLandscapeContent(
            uiState = uiState,
            modifier = modifier
        )
    } else {
        ShotAnalysisPortraitContent(
            uiState = uiState,
            modifier = modifier
        )
    }
}

@Composable
private fun ShotAnalysisPortraitContent(
    uiState: ShotHistoryUiState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = spacing.large)
    ) {
        // Overall Statistics
        uiState.overallStatistics?.let { stats ->
            item {
                OverallStatisticsCard(statistics = stats)
            }
        }

        // Shot Trends
        uiState.shotTrends?.let { trends ->
            item {
                ShotTrendsCard(trends = trends)
            }
        }

        // Brew Ratio Analysis
        uiState.brewRatioAnalysis?.let { analysis ->
            item {
                BrewRatioAnalysisCard(analysis = analysis)
            }
        }

        // Extraction Time Analysis
        uiState.extractionTimeAnalysis?.let { analysis ->
            item {
                ExtractionTimeAnalysisCard(analysis = analysis)
            }
        }

        // Grinder Setting Analysis (if available)
        uiState.grinderSettingAnalysis?.let { analysis ->
            item {
                GrinderSettingAnalysisCard(analysis = analysis)
            }
        }
    }
}

@Composable
private fun ShotAnalysisLandscapeContent(
    uiState: ShotHistoryUiState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
    ) {
        // Left column - Primary analysis
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing),
            contentPadding = PaddingValues(bottom = spacing.large)
        ) {
            // Overall Statistics
            uiState.overallStatistics?.let { stats ->
                item {
                    OverallStatisticsCard(statistics = stats)
                }
            }

            // Shot Trends
            uiState.shotTrends?.let { trends ->
                item {
                    ShotTrendsCard(trends = trends)
                }
            }

            // Grinder Setting Analysis (if available)
            uiState.grinderSettingAnalysis?.let { analysis ->
                item {
                    GrinderSettingAnalysisCard(analysis = analysis)
                }
            }
        }

        // Right column - Detailed analysis
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing),
            contentPadding = PaddingValues(bottom = spacing.large)
        ) {
            // Brew Ratio Analysis
            uiState.brewRatioAnalysis?.let { analysis ->
                item {
                    BrewRatioAnalysisCard(analysis = analysis)
                }
            }

            // Extraction Time Analysis
            uiState.extractionTimeAnalysis?.let { analysis ->
                item {
                    ExtractionTimeAnalysisCard(analysis = analysis)
                }
            }
        }
    }
}

@Composable
private fun OverallStatisticsCard(
    statistics: com.jodli.coffeeshottimer.domain.usecase.OverallStatistics,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_overall_statistics)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Key metrics grid
                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = stringResource(R.string.label_total_shots),
                    value = statistics.totalShots.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_beans_used),
                    value = statistics.uniqueBeans.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_avg_ratio),
                    value = stringResource(R.string.format_avg_brew_ratio_display, statistics.avgBrewRatio),
                    modifier = Modifier.weight(1f)
                )
                    }

        Spacer(modifier = Modifier.height(spacing.medium))

                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = stringResource(R.string.label_avg_time),
                    value = stringResource(R.string.format_avg_extraction_time_display, statistics.avgExtractionTime.toInt()),
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_optimal_time),
                    value = stringResource(R.string.format_optimal_extraction_percentage, statistics.optimalExtractionPercentage),
                    isGood = statistics.optimalExtractionPercentage > 50,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_good_ratio),
                    value = stringResource(R.string.format_typical_ratio_percentage, statistics.typicalRatioPercentage),
                    isGood = statistics.typicalRatioPercentage > 50,
                    modifier = Modifier.weight(1f)
                )
                    }

        statistics.mostUsedGrinderSetting?.let { setting ->
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = stringResource(R.string.format_most_used_grinder_setting, setting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShotTrendsCard(
    trends: com.jodli.coffeeshottimer.domain.usecase.ShotTrends,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.AutoMirrored.Filled.List,
            title = stringResource(R.string.format_shot_trends, trends.daysAnalyzed)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Trend indicators
                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendItem(
                    label = stringResource(R.string.label_shots_per_day),
                    value = String.format(java.util.Locale.ROOT, "%.1f", trends.shotsPerDay),
                    modifier = Modifier.weight(1f)
                )
                TrendItem(
                    label = stringResource(R.string.label_ratio_trend),
                    value = if (trends.brewRatioTrend >= 0) "+${
                        String.format(
                            java.util.Locale.ROOT,
                            "%.2f",
                            trends.brewRatioTrend
                        )
                    }" else String.format(java.util.Locale.ROOT, "%.2f", trends.brewRatioTrend),
                    isImproving = kotlin.math.abs(trends.brewRatioTrend) < 0.1,
                    modifier = Modifier.weight(1f)
                )
                TrendItem(
                    label = stringResource(R.string.label_time_trend),
                    value = if (trends.extractionTimeTrend >= 0) "+${
                        String.format(
                            java.util.Locale.ROOT,
                            "%.1f",
                            trends.extractionTimeTrend
                        )
                    }s" else stringResource(R.string.format_extraction_time_trend_display, trends.extractionTimeTrend),
                    isImproving = kotlin.math.abs(trends.extractionTimeTrend) < 2,
                    modifier = Modifier.weight(1f)
                )
                    }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Overall improvement indicator
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.qualityIndicator + spacing.extraSmall)
                    .clip(CircleShape)
                    .background(
                        if (trends.isImproving) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(
                text = if (trends.isImproving) stringResource(R.string.text_improving_consistency) else stringResource(R.string.text_room_for_improvement),
                style = MaterialTheme.typography.bodyMedium,
                color = if (trends.isImproving) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun BrewRatioAnalysisCard(
    analysis: com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.text_brew_ratio_analysis)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Key statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = stringResource(R.string.label_average),
                value = stringResource(R.string.format_avg_brew_ratio_display, analysis.avgRatio),
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = stringResource(R.string.label_range),
                value = stringResource(R.string.format_brew_ratio_range, analysis.minRatio, analysis.maxRatio),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Quality breakdown
                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = stringResource(R.string.label_typical_range),
                    value = stringResource(R.string.format_typical_ratio_percentage, analysis.typicalRatioPercentage),
                    isGood = analysis.typicalRatioPercentage > 70,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_under_extracted),
                    value = stringResource(R.string.format_under_extracted_percentage, analysis.underExtractedPercentage),
                    isGood = analysis.underExtractedPercentage < 20,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_over_extracted),
                    value = stringResource(R.string.format_over_extracted_percentage, analysis.overExtractedPercentage),
                    isGood = analysis.overExtractedPercentage < 20,
                    modifier = Modifier.weight(1f)
                )
                    }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Distribution
        Text(
            text = stringResource(R.string.text_distribution),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(spacing.small))

        analysis.distribution.forEach { (range, count) ->
            if (count > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = range,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.format_shots_count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtractionTimeAnalysisCard(
    analysis: com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.text_extraction_time_analysis),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            // Key statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = stringResource(R.string.label_average),
                    value = stringResource(R.string.format_avg_extraction_time_display, analysis.avgTime.toInt()),
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = stringResource(R.string.label_range),
                    value = stringResource(R.string.format_time_range_display, analysis.minTime, analysis.maxTime),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Quality breakdown
                            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        label = stringResource(R.string.label_optimal_time_range_short),
                        value = stringResource(R.string.format_optimal_extraction_percentage, analysis.optimalTimePercentage),
                        isGood = analysis.optimalTimePercentage > 50,
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        label = stringResource(R.string.label_too_fast),
                        value = stringResource(R.string.format_too_fast_percentage, analysis.tooFastPercentage),
                        isGood = analysis.tooFastPercentage < 30,
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        label = stringResource(R.string.label_too_slow),
                        value = stringResource(R.string.format_too_slow_percentage, analysis.tooSlowPercentage),
                        isGood = analysis.tooSlowPercentage < 30,
                        modifier = Modifier.weight(1f)
                    )
                            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Distribution
            Text(
                text = stringResource(R.string.text_distribution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(spacing.small))

            analysis.distribution.forEach { (range, count) ->
                if (count > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = range,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.format_shots_count, count),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrinderSettingAnalysisCard(
    analysis: com.jodli.coffeeshottimer.domain.usecase.GrinderSettingAnalysis,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.text_grinder_setting_analysis),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = stringResource(R.string.format_settings_different_used, analysis.totalSettings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            // Most used setting
            analysis.mostUsedSetting?.let { setting ->
                Text(
                    text = stringResource(R.string.text_most_used_setting),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing.small))

                GrinderSettingItem(
                    setting = setting,
                    isHighlighted = true
                )

                Spacer(modifier = Modifier.height(spacing.medium))
            }

            // Best performing setting
            analysis.bestPerformingSetting?.let { setting ->
                Text(
                    text = stringResource(R.string.text_best_performing_setting),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing.small))

                GrinderSettingItem(
                    setting = setting,
                    isHighlighted = true
                )
            }
        }
    }
}

@Composable
private fun GrinderSettingItem(
    setting: com.jodli.coffeeshottimer.domain.usecase.GrinderSettingStats,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = setting.setting,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.format_shots_count, setting.shotCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(R.string.format_avg_ratio, setting.avgBrewRatio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.format_avg_time, setting.avgExtractionTime.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.format_optimal_extraction_percentage_long, setting.optimalExtractionPercentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    isGood: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when (isGood) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendItem(
    label: String,
    value: String,
    isImproving: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (isImproving) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadMoreItem(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LaunchedEffect(Unit) {
        if (!isLoading) {
            onLoadMore()
        }
    }

    Box(
        modifier = modifier.padding(spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            LoadingIndicator(message = stringResource(R.string.loading_more_shots))
        } else {
            Text(
                text = stringResource(R.string.loading_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShotSuccessIndicator(
    shot: Shot,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Calculate success score based on optimal parameters
    val successScore = calculateShotSuccessScore(shot)
    val successText = when {
        successScore >= 80 -> stringResource(R.string.text_excellent)
        successScore >= 60 -> stringResource(R.string.text_good)
        successScore >= 40 -> stringResource(R.string.text_fair)
        else -> stringResource(R.string.text_needs_work)
    }

    val successColor = when {
        successScore >= 80 -> MaterialTheme.colorScheme.primary
        successScore >= 60 -> MaterialTheme.colorScheme.tertiary
        successScore >= 40 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Text(
        text = successText,
        style = MaterialTheme.typography.labelSmall,
        color = successColor,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

private fun calculateShotSuccessScore(shot: Shot): Int {
    var score = 0

    // Optimal extraction time (25-30s) = 40 points
    if (shot.isOptimalExtractionTime()) {
        score += 40
    } else {
        // Partial points for close times
        val timeDiff = when {
            shot.extractionTimeSeconds < 25 -> 25 - shot.extractionTimeSeconds
            shot.extractionTimeSeconds > 30 -> shot.extractionTimeSeconds - 30
            else -> 0
        }
        score += maxOf(0, 40 - (timeDiff * 4))
    }

    // Typical brew ratio (1.5-3.0) = 40 points
    if (shot.isTypicalBrewRatio()) {
        score += 40
    } else {
        // Partial points for close ratios
        val ratioDiff = when {
            shot.brewRatio < 1.5 -> 1.5 - shot.brewRatio
            shot.brewRatio > 3.0 -> shot.brewRatio - 3.0
            else -> 0.0
        }
        score += maxOf(0, 40 - (ratioDiff * 20.0).toInt())
    }

    // Consistency bonus (reasonable input/output weights) = 20 points
    val isReasonableWeights = shot.coffeeWeightIn in 15.0..25.0 &&
            shot.coffeeWeightOut in 25.0..60.0
    if (isReasonableWeights) {
        score += 20
    }

    return score.coerceIn(0, 100)
}
