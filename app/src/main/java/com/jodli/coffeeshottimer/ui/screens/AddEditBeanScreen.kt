package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.ui.components.*
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.AddEditBeanViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBeanScreen(
    beanId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: AddEditBeanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Initialize for edit mode if beanId is provided
    LaunchedEffect(beanId) {
        if (beanId != null) {
            viewModel.initializeForEdit(beanId)
        }
    }
    
    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
            viewModel.resetSaveSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = if (uiState.isEditMode) "Edit Bean" else "Add Bean",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(message = "Loading bean details...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Bean Name
                CoffeeTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = "Bean Name *",
                    placeholder = "Enter bean name",
                    leadingIcon = Icons.Default.Home,
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError
                )
                
                // Roast Date
                CoffeeTextField(
                    value = uiState.roastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    onValueChange = { }, // Read-only, handled by date picker
                    label = "Roast Date *",
                    placeholder = "Select roast date",
                    leadingIcon = Icons.Default.DateRange,
                    trailingIcon = Icons.Default.DateRange,
                    onTrailingIconClick = { showDatePicker = true },
                    isError = uiState.roastDateError != null,
                    errorMessage = uiState.roastDateError,
                    singleLine = true
                )
                
                // Notes
                CoffeeTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateNotes,
                    label = "Notes",
                    placeholder = "Optional notes about this bean",
                    leadingIcon = Icons.Default.Edit,
                    isError = uiState.notesError != null,
                    errorMessage = uiState.notesError,
                    singleLine = false,
                    maxLines = 4
                )
                
                // Grinder Setting
                CoffeeTextField(
                    value = uiState.lastGrinderSetting,
                    onValueChange = viewModel::updateLastGrinderSetting,
                    label = "Grinder Setting",
                    placeholder = "Optional initial grinder setting",
                    leadingIcon = Icons.Default.Settings
                )
                
                // Active Status (only show in edit mode)
                if (uiState.isEditMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Bean Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (uiState.isActive) "Active - visible in bean lists" else "Inactive - hidden from active lists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Switch(
                                checked = uiState.isActive,
                                onCheckedChange = viewModel::updateIsActive
                            )
                        }
                    }
                }
                
                // Error Message
                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            
                            TextButton(
                                onClick = viewModel::clearError
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(spacing.medium))
                
                // Save Button
                CoffeePrimaryButton(
                    text = if (uiState.isSaving) "Saving..." else if (uiState.isEditMode) "Update Bean" else "Add Bean",
                    onClick = viewModel::saveBean,
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Validation Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.medium)
                    ) {
                        Text(
                            text = "Validation Rules",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(spacing.small))
                        
                        val rules = listOf(
                            "Bean name: Required, unique, max 100 characters",
                            "Roast date: Cannot be future date, max 365 days ago",
                            "Notes: Optional, max 500 characters",
                            "Grinder setting: Optional, max 50 characters"
                        )
                        
                        rules.forEach { rule ->
                            Text(
                                text = "• $rule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Bottom padding for scrolling
                Spacer(modifier = Modifier.height(spacing.large))
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = uiState.roastDate,
            onDateSelected = { date ->
                viewModel.updateRoastDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "Select Roast Date",
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}