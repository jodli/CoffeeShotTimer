package com.example.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.ui.components.*
import com.example.coffeeshottimer.ui.theme.LocalSpacing
import com.example.coffeeshottimer.ui.viewmodel.BeanManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
                text = "Bean Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            CoffeePrimaryButton(
                text = "Add Bean",
                onClick = onAddBeanClick,
                icon = Icons.Default.Add,
                modifier = Modifier.widthIn(max = 140.dp)
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
                label = "Search beans",
                placeholder = "Enter bean name...",
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
                        text = if (showInactive) "All" else "Active",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = showInactive,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
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
                    LoadingIndicator(message = "Loading beans...")
                }
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(spacing.medium))
                    
                    CoffeePrimaryButton(
                        text = "Retry",
                        onClick = {
                            viewModel.clearError()
                            viewModel.refresh()
                        },
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }
            
            uiState.beans.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = if (searchQuery.isNotEmpty()) "No beans found" else "No beans yet",
                    description = if (searchQuery.isNotEmpty()) {
                        "Try adjusting your search or filter settings"
                    } else {
                        "Add your first coffee bean to start tracking shots"
                    },
                    actionText = if (searchQuery.isEmpty()) "Add Bean" else null,
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
                            } else null
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
                Text("Delete Bean")
            },
            text = {
                Text("Are you sure you want to delete \"${bean.name}\"? This will deactivate the bean and hide it from active lists.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBean(bean.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null }
                ) {
                    Text("Cancel")
                }
            }
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
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(
        modifier = modifier,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Bean name and status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = bean.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (bean.isActive) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!bean.isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = spacing.small, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                // Days since roast with freshness indicator
                val daysSinceRoast = bean.daysSinceRoast()
                val isFresh = bean.isFresh()
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = "Roasted: $daysSinceRoast days ago",
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
                            text = if (isFresh) "Fresh" else when {
                                daysSinceRoast < 4 -> "Too Fresh"
                                daysSinceRoast <= 30 -> "Good"
                                daysSinceRoast <= 60 -> "OK"
                                else -> "Stale"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFresh) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = spacing.small, vertical = 2.dp)
                        )
                    }
                }
                
                // Grinder setting if available
                if (!bean.lastGrinderSetting.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    Text(
                        text = "Last grind: ${bean.lastGrinderSetting}",
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
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                // Use for Shot button (only for active beans)
                if (bean.isActive) {
                    FilledTonalButton(
                        onClick = onUseForShot,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = spacing.small, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Use for Shot",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                if (onReactivate != null) {
                    IconButton(
                        onClick = onReactivate,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Reactivate bean",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (bean.isActive) "Delete bean" else "Permanently delete bean",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}