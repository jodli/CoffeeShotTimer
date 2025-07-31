package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.jodli.coffeeshottimer.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    leadingIcon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
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
                    leadingIcon = Icons.Filled.Engineering
                )

                // Active Status (only show in edit mode)
                if (uiState.isEditMode) {
                    CoffeeCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CardHeader(
                            icon = Icons.Default.Settings,
                            title = "Bean Status",
                            actions = {
                                Switch(
                                    checked = uiState.isActive,
                                    onCheckedChange = viewModel::updateIsActive
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        
                        Text(
                            text = if (uiState.isActive) "Active - visible in bean lists" else "Inactive - hidden from active lists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error Message
                if (uiState.error != null) {
                    CoffeeCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        CardHeader(
                            icon = Icons.Default.Error,
                            title = "Error",
                            actions = {
                                TextButton(
                                    onClick = viewModel::clearError
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
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
                CoffeeCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    CardHeader(
                        icon = Icons.Default.Info,
                        title = "Validation Rules"
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
    val spacing = LocalSpacing.current
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
                    modifier = Modifier.padding(spacing.medium)
                )
            }
        )
    }
}