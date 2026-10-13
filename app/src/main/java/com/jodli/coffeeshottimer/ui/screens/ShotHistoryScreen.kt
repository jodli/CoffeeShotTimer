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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.ErrorState
import com.jodli.coffeeshottimer.ui.components.LEDGER_RATIO_COLUMN_WIDTH
import com.jodli.coffeeshottimer.ui.components.LEDGER_SCORE_COLUMN_WIDTH
import com.jodli.coffeeshottimer.ui.components.LEDGER_TIME_COLUMN_WIDTH
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.ScoreFigure
import com.jodli.coffeeshottimer.ui.components.ShotHistoryFilterDialog
import com.jodli.coffeeshottimer.ui.components.SquareSegmentedSwitch
import com.jodli.coffeeshottimer.ui.components.TasteMark
import com.jodli.coffeeshottimer.ui.components.analytics.AnalysisFiguresRow
import com.jodli.coffeeshottimer.ui.components.analytics.CoachingSection
import com.jodli.coffeeshottimer.ui.components.analytics.GrinderSettingsTable
import com.jodli.coffeeshottimer.ui.components.analytics.QualityTrendSection
import com.jodli.coffeeshottimer.ui.components.analytics.ShotChartSection
import com.jodli.coffeeshottimer.ui.components.analytics.TrendDataPoint
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.extractionZone
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.viewmodel.Achievement
import com.jodli.coffeeshottimer.ui.viewmodel.ShotHistoryUiState
import com.jodli.coffeeshottimer.ui.viewmodel.ShotHistoryViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ShotHistoryScreen(
    onShotClick: (String) -> Unit = {},
    initialBeanIdFilter: String? = null,
    viewModel: ShotHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    // Apply initial bean filter if provided
    LaunchedEffect(initialBeanIdFilter) {
        if (initialBeanIdFilter != null) {
            viewModel.applyFilter(ShotHistoryFilter(beanId = initialBeanIdFilter))
        }
    }

    // Auto-refresh when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshOnResume()
    }

    val spacing = LocalSpacing.current
    ShotHistoryLayout(
        uiState = uiState,
        currentFilter = currentFilter,
        onToggleAnalysisView = { viewModel.toggleAnalysisView() },
        onShowFilterDialog = { showFilterDialog = true },
        onShotClick = onShotClick,
        onRefreshData = { viewModel.refreshData() },
        onLoadMore = { viewModel.loadMore() },
        onClearFilters = { viewModel.clearFilters() },
        getBeanName = { beanId -> viewModel.getBeanName(beanId) },
        getAchievementForShot = { shot -> viewModel.getAchievementForShot(shot) },
        contentPadding = if (LocalIsLandscape.current) {
            spacing.landscapeSpacing()
        } else {
            spacing.screenPadding
        },
        modifier = Modifier.fillMaxSize()
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

/** One layout for both orientations; only the padding differs. */
@Composable
private fun ShotHistoryLayout(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onShotClick: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLoadMore: () -> Unit,
    onClearFilters: () -> Unit,
    getBeanName: (String) -> String,
    getAchievementForShot: (Shot) -> Achievement?,
    contentPadding: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        FilterAndViewBar(
            currentFilter = currentFilter,
            showAnalysis = uiState.showAnalysis,
            onToggleAnalysisView = onToggleAnalysisView,
            onShowFilterDialog = onShowFilterDialog,
            onClearFilters = onClearFilters,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.medium))

        when {
            uiState.isLoading -> {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(message = stringResource(R.string.loading_shot_history))
                }
            }

            uiState.error != null -> {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(
                        title = stringResource(R.string.error_loading_shots),
                        message = uiState.error ?: "Unknown error occurred",
                        onRetry = onRefreshData
                    )
                }
            }

            uiState.isEmpty -> {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = if (currentFilter.hasFilters()) {
                            stringResource(R.string.cd_no_shots)
                        } else {
                            stringResource(R.string.cd_no_shots_recorded)
                        },
                        description = if (currentFilter.hasFilters()) {
                            stringResource(R.string.text_search_beans_hint)
                        } else {
                            stringResource(R.string.text_record_shots_analysis)
                        }
                    )
                }
            }

            uiState.showAnalysis -> {
                ShotAnalysisView(
                    uiState = uiState,
                    currentFilter = currentFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                ShotLedgerList(
                    uiState = uiState,
                    getBeanName = getBeanName,
                    getAchievementForShot = getAchievementForShot,
                    onShotClick = onShotClick,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Square switch for the two views, square Filters chip, quiet clear button. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FilterAndViewBar(
    currentFilter: ShotHistoryFilter,
    showAnalysis: Boolean,
    onToggleAnalysisView: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareSegmentedSwitch(
            options = listOf(
                stringResource(R.string.text_view_list),
                stringResource(R.string.text_view_analysis)
            ),
            selectedIndex = if (showAnalysis) 1 else 0,
            onSelect = { index -> if ((index == 1) != showAnalysis) onToggleAnalysisView() }
        )

        FilterChip(
            selected = currentFilter.hasFilters(),
            onClick = onShowFilterDialog,
            shape = MaterialTheme.shapes.small,
            label = { Text(stringResource(R.string.text_filters)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        if (currentFilter.hasFilters()) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.button_clear_all),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

/** The ledger: a ruled column header, then one ruled line per shot. */
@Composable
private fun ShotLedgerList(
    uiState: ShotHistoryUiState,
    getBeanName: (String) -> String,
    getAchievementForShot: (Shot) -> Achievement?,
    onShotClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item(key = "header") {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    LedgerHeaderCell(stringResource(R.string.label_time), LEDGER_TIME_COLUMN_WIDTH)
                    LedgerHeaderCell(stringResource(R.string.label_ratio), LEDGER_RATIO_COLUMN_WIDTH)
                    LedgerHeaderCell(stringResource(R.string.label_score), LEDGER_SCORE_COLUMN_WIDTH)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        items(uiState.shots, key = { it.id }) { shot ->
            ShotLedgerRow(
                shot = shot,
                beanName = getBeanName(shot.beanId),
                score = uiState.qualityScores[shot.id],
                achievement = getAchievementForShot(shot),
                onClick = { onShotClick(shot.id) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (uiState.hasMorePages) {
            item {
                LoadMoreItem(
                    isLoading = uiState.isLoadingMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LedgerHeaderCell(label: String, width: Dp) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.width(width)
    )
}

/** One ledger line: figures right-aligned in fixed columns, details beneath. */
@Composable
private fun ShotLedgerRow(
    shot: Shot,
    beanName: String,
    score: Int?,
    achievement: Achievement?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val zones = MaterialTheme.zoneColors
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClickLabel = beanName, onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = beanName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            LedgerFigure(width = LEDGER_TIME_COLUMN_WIDTH) {
                Text(
                    text = shot.getFormattedExtractionTime(),
                    style = MaterialTheme.typography.titleMedium,
                    color = zones.ink(extractionZone(shot.extractionTimeSeconds)),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            LedgerFigure(width = LEDGER_RATIO_COLUMN_WIDTH) {
                Text(
                    text = shot.getFormattedBrewRatio(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            LedgerFigure(width = LEDGER_SCORE_COLUMN_WIDTH) {
                if (score != null) {
                    ScoreFigure(score = score)
                } else {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val details = buildString {
                append(shot.timestamp.format(dateFormatter))
                append(" · ")
                append(
                    stringResource(
                        R.string.format_weight_in_out,
                        shot.coffeeWeightIn.toInt(),
                        shot.coffeeWeightOut.toInt()
                    )
                )
                if (shot.grinderSetting.isNotBlank()) {
                    append(" · ")
                    append(stringResource(R.string.format_grind_inline, shot.grinderSetting))
                }
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            shot.tastePrimary?.let { primary ->
                Spacer(modifier = Modifier.width(spacing.small))
                TasteMark(
                    taste = primary,
                    secondary = shot.tasteSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        achievement?.let { achievement ->
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = achievement.label,
                style = MaterialTheme.typography.labelMedium,
                color = zones.idealInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LedgerFigure(width: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(width), contentAlignment = Alignment.CenterEnd) {
        content()
    }
}

/** Analysis: coaching, the shot chart, three figures, trend, grinder table. */
@Composable
private fun ShotAnalysisView(
    uiState: ShotHistoryUiState,
    currentFilter: ShotHistoryFilter,
    modifier: Modifier = Modifier
) {
    if (uiState.analysisLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            LoadingIndicator(message = stringResource(R.string.loading_analysis))
        }
        return
    }

    if (!uiState.hasAnalysisData) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Default.Info,
                title = stringResource(R.string.text_no_analysis_data),
                description = stringResource(R.string.text_record_shots_analysis)
            )
        }
        return
    }

    val trendPoints = uiState.shots.mapNotNull { shot ->
        uiState.qualityScores[shot.id]?.let { score ->
            TrendDataPoint(date = shot.timestamp.toLocalDate(), score = score)
        }
    }

    if (LocalIsLandscape.current) {
        val landscapeSpacing = LocalSpacing.current.landscapeSpacing()
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = LocalSpacing.current.large)
            ) {
                chartItems(uiState, currentFilter)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = LocalSpacing.current.large)
            ) {
                figuresItems(uiState, trendPoints)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = LocalSpacing.current.large)
        ) {
            chartItems(uiState, currentFilter)
            figuresItems(uiState, trendPoints)
        }
    }
}

private fun LazyListScope.chartItems(uiState: ShotHistoryUiState, currentFilter: ShotHistoryFilter) {
    item { CoachingSection(insights = uiState.coachingInsights) }
    item {
        ShotChartSection(
            shots = uiState.shots,
            connect = currentFilter.beanId != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun LazyListScope.figuresItems(uiState: ShotHistoryUiState, trendPoints: List<TrendDataPoint>) {
    item { AnalysisFigures(uiState) }
    item {
        QualityTrendSection(
            trends = uiState.shotTrends,
            points = trendPoints,
            modifier = Modifier.fillMaxWidth()
        )
    }
    item { GrinderSettingsTable(analysis = uiState.grinderSettingAnalysis) }
}

@Composable
private fun AnalysisFigures(uiState: ShotHistoryUiState) {
    AnalysisFiguresRow(
        aggregate = uiState.aggregateQualityAnalysis,
        extraction = uiState.extractionTimeAnalysis,
        brewRatio = uiState.brewRatioAnalysis
    )
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
