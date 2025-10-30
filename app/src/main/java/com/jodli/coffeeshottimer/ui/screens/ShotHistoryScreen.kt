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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.ui.components.CoachingInsightsCard
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CompactTasteDisplay
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.ShotHistoryFilterDialog
import com.jodli.coffeeshottimer.ui.components.analytics.EnhancedBrewRatioCard
import com.jodli.coffeeshottimer.ui.components.analytics.EnhancedExtractionQualityCard
import com.jodli.coffeeshottimer.ui.components.analytics.EnhancedShotTrendsCard
import com.jodli.coffeeshottimer.ui.components.analytics.QualityScoreGaugeCard
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.Spacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.Achievement
import com.jodli.coffeeshottimer.ui.viewmodel.AchievementType
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
    var coachingInsightsExpanded by remember { mutableStateOf(false) }

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
                coachingInsightsExpanded = coachingInsightsExpanded,
                onToggleCoachingInsights = { coachingInsightsExpanded = !coachingInsightsExpanded },
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
                coachingInsightsExpanded = coachingInsightsExpanded,
                onToggleCoachingInsights = { coachingInsightsExpanded = !coachingInsightsExpanded },
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
    coachingInsightsExpanded: Boolean,
    onToggleCoachingInsights: () -> Unit,
    spacing: Spacing,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.screenPadding)
    ) {
        // Integrated filter and view toggle bar
        IntegratedFilterAndViewBar(
            currentFilter = currentFilter,
            showAnalysis = uiState.showAnalysis,
            onToggleAnalysisView = onToggleAnalysisView,
            onShowFilterDialog = onShowFilterDialog,
            onClearFilters = onClearFilters,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Coaching insights card (only in list view, not in analysis view)
        if (!uiState.showAnalysis) {
            uiState.coachingInsights?.let { insights ->
                CoachingInsightsCard(
                    insights = insights,
                    isExpanded = coachingInsightsExpanded,
                    onToggleExpanded = onToggleCoachingInsights,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(spacing.medium))
            }
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
    coachingInsightsExpanded: Boolean,
    onToggleCoachingInsights: () -> Unit,
    spacing: Spacing,
    modifier: Modifier = Modifier
) {
    val landscapeSpacing = spacing.landscapeSpacing()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(landscapeSpacing)
    ) {
        // Integrated filter and view toggle bar
        IntegratedFilterAndViewBar(
            currentFilter = currentFilter,
            showAnalysis = uiState.showAnalysis,
            onToggleAnalysisView = onToggleAnalysisView,
            onShowFilterDialog = onShowFilterDialog,
            onClearFilters = onClearFilters,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(landscapeSpacing))

        // Coaching insights card (only in list view, not in analysis view)
        if (!uiState.showAnalysis) {
            uiState.coachingInsights?.let { insights ->
                CoachingInsightsCard(
                    insights = insights,
                    isExpanded = coachingInsightsExpanded,
                    onToggleExpanded = onToggleCoachingInsights,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(landscapeSpacing))
            }
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
 * Integrated bar combining filters and view toggle (List/Analysis)
 * Always visible for better discoverability
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun IntegratedFilterAndViewBar(
    currentFilter: ShotHistoryFilter,
    showAnalysis: Boolean,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Filter chip with badge indicator
        Box {
            FilterChip(
                selected = currentFilter.hasFilters(),
                onClick = onShowFilterDialog,
                label = { Text(stringResource(R.string.text_filters)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Active filter indicator badge
            if (currentFilter.hasFilters()) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(stringResource(R.string.badge_recommendation_alert))
                }
            }
        }

        // Center: Segmented button for List/Analysis toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(horizontal = spacing.small)
        ) {
            SegmentedButton(
                selected = !showAnalysis,
                onClick = { if (showAnalysis) onToggleAnalysisView() },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(
                    text = stringResource(R.string.text_view_list),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            SegmentedButton(
                selected = showAnalysis,
                onClick = { if (!showAnalysis) onToggleAnalysisView() },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(
                    text = stringResource(R.string.text_view_analysis),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Right: Clear filters button (when active)
        if (currentFilter.hasFilters()) {
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
                    title = if (currentFilter.hasFilters()) {
                        stringResource(
                            R.string.cd_no_shots
                        )
                    } else {
                        stringResource(R.string.cd_no_shots_recorded)
                    },
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
                    title = if (currentFilter.hasFilters()) {
                        stringResource(
                            R.string.cd_no_shots
                        )
                    } else {
                        stringResource(R.string.cd_no_shots_recorded)
                    },
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
                onClick = { onShotClick(shot.id) },
                viewModel = hiltViewModel()
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
    viewModel: ShotHistoryViewModel,
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

                // Achievement badge
                viewModel.getAchievementForShot(shot)?.let { achievement ->
                    AchievementBadge(achievement = achievement)
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
                        text = stringResource(
                            R.string.format_weight_in_out,
                            shot.coffeeWeightIn.toInt(),
                            shot.coffeeWeightOut.toInt()
                        ),
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
                onClick = { onShotClick(shot.id) },
                viewModel = hiltViewModel()
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
    viewModel: ShotHistoryViewModel,
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

                    // Achievement badge
                    viewModel.getAchievementForShot(shot)?.let { achievement ->
                        AchievementBadge(achievement = achievement)
                    }
                }
            }

            // Right side - weights + quality inline, success below
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = stringResource(
                            R.string.format_weight_in_out,
                            shot.coffeeWeightIn.toInt(),
                            shot.coffeeWeightOut.toInt()
                        ),
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

/**
 * Achievement badge component showing bean-specific milestones.
 */
@Composable
private fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Color based on achievement type
    val backgroundColor = when (achievement.type) {
        AchievementType.FIRST_PERFECT -> MaterialTheme.colorScheme.primaryContainer
        AchievementType.DIALED_IN -> MaterialTheme.colorScheme.tertiaryContainer
        AchievementType.CONSISTENCY -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when (achievement.type) {
        AchievementType.FIRST_PERFECT -> MaterialTheme.colorScheme.onPrimaryContainer
        AchievementType.DIALED_IN -> MaterialTheme.colorScheme.onTertiaryContainer
        AchievementType.CONSISTENCY -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(spacing.cornerLarge),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.small,
                vertical = spacing.extraSmall
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Text(
                text = achievement.emoji,
                style = MaterialTheme.typography.labelMedium
            )

            // Label
            Text(
                text = achievement.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    modifier: Modifier = Modifier,
    viewModel: ShotHistoryViewModel = hiltViewModel()
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
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        ShotAnalysisPortraitContent(
            uiState = uiState,
            viewModel = viewModel,
            modifier = modifier
        )
    }
}

@Composable
private fun ShotAnalysisPortraitContent(
    uiState: ShotHistoryUiState,
    viewModel: ShotHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = spacing.large)
    ) {
        // 1. Quality Score Gauge (NEW)
        uiState.aggregateQualityAnalysis?.let { analysis ->
            item {
                QualityScoreGaugeCard(
                    analysis = analysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Enhanced Shot Trends (NEW)
        uiState.shotTrends?.let { trends ->
            item {
                val qualityScores = uiState.shots.map { shot ->
                    shot.timestamp to viewModel.getShotQualityScore(shot)
                }
                EnhancedShotTrendsCard(
                    trends = trends,
                    qualityScores = qualityScores,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Enhanced Extraction Quality (NEW)
        uiState.extractionTimeAnalysis?.let { analysis ->
            item {
                EnhancedExtractionQualityCard(
                    analysis = analysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Enhanced Brew Ratio (NEW)
        uiState.brewRatioAnalysis?.let { analysis ->
            item {
                EnhancedBrewRatioCard(
                    analysis = analysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 5. Grinder Setting Analysis (preserved - conditional on bean filter)
        uiState.grinderSettingAnalysis?.let { analysis ->
            item {
                GrinderSettingAnalysisCard(
                    analysis = analysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShotAnalysisLandscapeContent(
    uiState: ShotHistoryUiState,
    viewModel: ShotHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
    ) {
        // Left column - Primary analysis
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing),
            contentPadding = PaddingValues(bottom = spacing.large)
        ) {
            // Quality Score Gauge (NEW)
            uiState.aggregateQualityAnalysis?.let { analysis ->
                item {
                    QualityScoreGaugeCard(
                        analysis = analysis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Enhanced Shot Trends (NEW)
            uiState.shotTrends?.let { trends ->
                item {
                    val qualityScores = uiState.shots.map { shot ->
                        shot.timestamp to viewModel.getShotQualityScore(shot)
                    }
                    EnhancedShotTrendsCard(
                        trends = trends,
                        qualityScores = qualityScores,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Right column - Detailed analysis
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing),
            contentPadding = PaddingValues(bottom = spacing.large)
        ) {
            // Enhanced Extraction Quality (NEW)
            uiState.extractionTimeAnalysis?.let { analysis ->
                item {
                    EnhancedExtractionQualityCard(
                        analysis = analysis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Enhanced Brew Ratio (NEW)
            uiState.brewRatioAnalysis?.let { analysis ->
                item {
                    EnhancedBrewRatioCard(
                        analysis = analysis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Grinder Setting Analysis (preserved - conditional on bean filter)
            uiState.grinderSettingAnalysis?.let { analysis ->
                item {
                    GrinderSettingAnalysisCard(
                        analysis = analysis,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    text = stringResource(
                        R.string.format_optimal_extraction_percentage_long,
                        setting.optimalExtractionPercentage
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
