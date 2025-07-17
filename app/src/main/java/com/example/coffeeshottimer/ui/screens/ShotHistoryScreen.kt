package com.example.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.example.coffeeshottimer.ui.components.*
import com.example.coffeeshottimer.ui.theme.LocalSpacing
import com.example.coffeeshottimer.ui.viewmodel.ShotHistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotHistoryScreen(
    onShotClick: (String) -> Unit = {},
    viewModel: ShotHistoryViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }

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
                
                // Refresh button
                IconButton(
                    onClick = { viewModel.refreshData() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                
                else -> {
                    ShotHistoryList(
                        shots = uiState.shots,
                        getBeanName = { beanId -> viewModel.getBeanName(beanId) },
                        onShotClick = onShotClick,
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
    availableBeans: List<com.example.coffeeshottimer.data.model.Bean>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Filters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }
            
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
}

@Composable
private fun ShotHistoryList(
    shots: List<Shot>,
    getBeanName: (String) -> String,
    onShotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
    ) {
        items(shots) { shot ->
            ShotHistoryItem(
                shot = shot,
                beanName = getBeanName(shot.beanId),
                onClick = { onShotClick(shot.id) }
            )
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
                
                // Quality indicator
                QualityIndicator(
                    isOptimalTime = shot.isOptimalExtractionTime(),
                    isTypicalRatio = shot.isTypicalBrewRatio()
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