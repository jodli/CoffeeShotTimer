package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.EmptyState
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.ShotHistoryFilterDialog
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screenPadding)
    ) {
        // Header with title and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Shot History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (currentFilter.hasFilters()) {
                    Text(
                        text = "Filtered results",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                // Analysis toggle button
                IconButton(
                    onClick = { viewModel.toggleAnalysisView() }
                ) {
                    Icon(
                        imageVector = if (uiState.showAnalysis) Icons.AutoMirrored.Filled.List else Icons.Default.Info,
                        contentDescription = if (uiState.showAnalysis) "Show shot list" else "Show analysis",
                        tint = if (uiState.showAnalysis) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Filter button
                IconButton(
                    onClick = { showFilterDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Filter shots",
                        tint = if (currentFilter.hasFilters()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Active filters display
        if (currentFilter.hasFilters()) {
            ActiveFiltersDisplay(
                filter = currentFilter,
                availableBeans = uiState.availableBeans,
                onClearFilters = { viewModel.clearFilters() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        message = "Loading shot history..."
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading shots",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        CoffeePrimaryButton(
                            text = "Retry",
                            onClick = { viewModel.refreshData() },
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                }

                uiState.isEmpty -> {
                    EmptyState(
                        icon = Icons.Default.Home,
                        title = if (currentFilter.hasFilters()) "No shots found" else "No shots recorded yet",
                        description = if (currentFilter.hasFilters()) {
                            "Try adjusting your filters to see more results"
                        } else {
                            "Start recording your espresso shots to see them here"
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
                        getBeanName = { beanId -> viewModel.getBeanName(beanId) },
                        onShotClick = onShotClick,
                        isLoadingMore = uiState.isLoadingMore,
                        hasMorePages = uiState.hasMorePages,
                        onLoadMore = { viewModel.loadMore() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }


        }
    }

    // Filter dialog
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

@Composable
private fun ActiveFiltersDisplay(
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
            icon = Icons.Default.Settings,
            title = "Active Filters",
            actions = {
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Display active filters
        val filterTexts = mutableListOf<String>()

        filter.beanId?.let { beanId ->
            val beanName = availableBeans.find { it.id == beanId }?.name ?: "Unknown Bean"
            filterTexts.add("Bean: $beanName")
        }

        if (filter.startDate != null || filter.endDate != null) {
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
            val startText = filter.startDate?.format(dateFormatter) ?: "Start"
            val endText = filter.endDate?.format(dateFormatter) ?: "End"
            filterTexts.add("Date: $startText - $endText")
        }

        filter.grinderSetting?.let { setting ->
            filterTexts.add("Grinder: $setting")
        }

        if (filter.minBrewRatio != null || filter.maxBrewRatio != null) {
            val min = filter.minBrewRatio?.let { "%.1f".format(it) } ?: "0"
            val max = filter.maxBrewRatio?.let { "%.1f".format(it) } ?: "∞"
            filterTexts.add("Brew Ratio: 1:$min - 1:$max")
        }

        if (filter.minExtractionTime != null || filter.maxExtractionTime != null) {
            val min = filter.minExtractionTime ?: 0
            val max = filter.maxExtractionTime ?: 999
            filterTexts.add("Time: ${min}s - ${max}s")
        }

        if (filter.onlyOptimalExtractionTime == true) {
            filterTexts.add("Optimal extraction time only")
        }

        if (filter.onlyTypicalBrewRatio == true) {
            filterTexts.add("Typical brew ratio only")
        }

        filterTexts.forEach { text ->
            Text(
                text = "• $text",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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

                // Key metrics row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    MetricChip(
                        label = "Ratio",
                        value = shot.getFormattedBrewRatio(),
                        isGood = shot.isTypicalBrewRatio()
                    )

                    MetricChip(
                        label = "Time",
                        value = shot.getFormattedExtractionTime(),
                        isGood = shot.isOptimalExtractionTime()
                    )

                    MetricChip(
                        label = "Grinder",
                        value = shot.grinderSetting,
                        isNeutral = true
                    )
                }
            }

            // Right side - weights
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${shot.coffeeWeightIn}g → ${shot.coffeeWeightOut}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Quality indicator with success score
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    QualityIndicator(
                        isOptimalTime = shot.isOptimalExtractionTime(),
                        isTypicalRatio = shot.isTypicalBrewRatio()
                    )
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    ShotSuccessIndicator(shot = shot)
                }
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
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                .size(8.dp)
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
                .size(8.dp)
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

    if (uiState.analysisLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(message = "Loading analysis...")
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
                title = "No analysis data available",
                description = "Record some shots to see analysis and insights"
            )
        }
        return
    }

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
private fun OverallStatisticsCard(
    statistics: com.jodli.coffeeshottimer.domain.usecase.OverallStatistics,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.Info,
            title = "Overall Statistics"
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Key metrics grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "Total Shots",
                value = statistics.totalShots.toString(),
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Beans Used",
                value = statistics.uniqueBeans.toString(),
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Avg Ratio",
                value = "1:${String.format("%.1f", statistics.avgBrewRatio)}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "Avg Time",
                value = "${statistics.avgExtractionTime.toInt()}s",
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Optimal Time",
                value = "${String.format("%.0f", statistics.optimalExtractionPercentage)}%",
                isGood = statistics.optimalExtractionPercentage > 50,
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Good Ratio",
                value = "${String.format("%.0f", statistics.typicalRatioPercentage)}%",
                isGood = statistics.typicalRatioPercentage > 50,
                modifier = Modifier.weight(1f)
            )
        }

        statistics.mostUsedGrinderSetting?.let { setting ->
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = "Most used grinder setting: $setting",
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
            title = "Shot Trends (${trends.daysAnalyzed} days)"
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Trend indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TrendItem(
                label = "Shots/Day",
                value = String.format("%.1f", trends.shotsPerDay),
                modifier = Modifier.weight(1f)
            )
            TrendItem(
                label = "Ratio Trend",
                value = if (trends.brewRatioTrend >= 0) "+${
                    String.format(
                        "%.2f",
                        trends.brewRatioTrend
                    )
                }" else String.format("%.2f", trends.brewRatioTrend),
                isImproving = kotlin.math.abs(trends.brewRatioTrend) < 0.1,
                modifier = Modifier.weight(1f)
            )
            TrendItem(
                label = "Time Trend",
                value = if (trends.extractionTimeTrend >= 0) "+${
                    String.format(
                        "%.1f",
                        trends.extractionTimeTrend
                    )
                }s" else "${String.format("%.1f", trends.extractionTimeTrend)}s",
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
                    .size(12.dp)
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
                text = if (trends.isImproving) "Improving consistency" else "Room for improvement",
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
            title = "Brew Ratio Analysis"
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Key statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "Average",
                value = "1:${String.format("%.1f", analysis.avgRatio)}",
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Range",
                value = "1:${
                    String.format(
                        "%.1f",
                        analysis.minRatio
                    )
                } - 1:${String.format("%.1f", analysis.maxRatio)}",
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
                label = "Typical Range",
                value = "${String.format("%.0f", analysis.typicalRatioPercentage)}%",
                isGood = analysis.typicalRatioPercentage > 70,
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Under-extracted",
                value = "${String.format("%.0f", analysis.underExtractedPercentage)}%",
                isGood = analysis.underExtractedPercentage < 20,
                modifier = Modifier.weight(1f)
            )
            StatisticItem(
                label = "Over-extracted",
                value = "${String.format("%.0f", analysis.overExtractedPercentage)}%",
                isGood = analysis.overExtractedPercentage < 20,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Distribution
        Text(
            text = "Distribution",
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
                        text = "$count shots",
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
                text = "Extraction Time Analysis",
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
                    label = "Average",
                    value = "${analysis.avgTime.toInt()}s",
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Range",
                    value = "${analysis.minTime}s - ${analysis.maxTime}s",
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
                    label = "Optimal (25-30s)",
                    value = "${String.format("%.0f", analysis.optimalTimePercentage)}%",
                    isGood = analysis.optimalTimePercentage > 50,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Too Fast",
                    value = "${String.format("%.0f", analysis.tooFastPercentage)}%",
                    isGood = analysis.tooFastPercentage < 30,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Too Slow",
                    value = "${String.format("%.0f", analysis.tooSlowPercentage)}%",
                    isGood = analysis.tooSlowPercentage < 30,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Distribution
            Text(
                text = "Distribution",
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
                            text = "$count shots",
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
                text = "Grinder Setting Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = "${analysis.totalSettings} different settings used",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            // Most used setting
            analysis.mostUsedSetting?.let { setting ->
                Text(
                    text = "Most Used Setting",
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
                    text = "Best Performing Setting",
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
                    text = "${setting.shotCount} shots",
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
                    text = "Avg Ratio: 1:${String.format("%.1f", setting.avgBrewRatio)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Avg Time: ${setting.avgExtractionTime.toInt()}s",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Optimal: ${
                        String.format(
                            "%.0f",
                            setting.optimalExtractionPercentage
                        )
                    }%",
                    style = MaterialTheme.typography.bodySmall
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
            LoadingIndicator(message = "Loading more shots...")
        } else {
            Text(
                text = "Loading more...",
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
        successScore >= 80 -> "Excellent"
        successScore >= 60 -> "Good"
        successScore >= 40 -> "Fair"
        else -> "Needs Work"
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