package com.jodli.coffeeshottimer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.net.Uri
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.BeanPhotoSection
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.components.PendingPhotoViewer
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
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.addPhoto(tempCameraUri!!)
        }
        tempCameraUri = null
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addPhoto(it) }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            // Permission granted, launch camera
            launchCamera(viewModel, cameraLauncher) { uri -> tempCameraUri = uri }
        } else {
            // Permission denied, fallback to gallery
            galleryLauncher.launch("image/*")
        }
    }

    // Function to handle photo capture
    fun handlePhotoCapture() {
        // For now, just launch gallery as a placeholder
        // TODO: Implement proper camera/gallery selection dialog
        galleryLauncher.launch("image/*")
    }

    // Function to handle photo viewing
    fun handlePhotoView() {
        if (uiState.photoPath != null || uiState.pendingPhotoUri != null) {
            showPhotoViewer = true
        }
    }

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
                    text = if (uiState.isEditMode) stringResource(R.string.cd_edit_bean) else stringResource(R.string.text_add_bean),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
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
                LoadingIndicator(message = stringResource(R.string.loading_bean_details))
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
                    label = stringResource(R.string.label_bean_name_required),
                    placeholder = stringResource(R.string.placeholder_enter_bean_name),
                    leadingIcon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError
                )

                // Roast Date
                CoffeeTextField(
                    value = uiState.roastDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    onValueChange = { }, // Read-only, handled by date picker
                    label = stringResource(R.string.label_roast_date_required),
                    placeholder = stringResource(R.string.placeholder_select_roast_date),
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
                    label = stringResource(R.string.label_notes),
                    placeholder = stringResource(R.string.placeholder_optional_bean_notes),
                    leadingIcon = Icons.Default.Edit,
                    isError = uiState.notesError != null,
                    errorMessage = uiState.notesError,
                    singleLine = false,
                    maxLines = 4
                )

                // Grinder Setting
                CoffeeTextField(
                    value = uiState.lastGrinderSetting,
                    onValueChange = viewModel::updateAndValidateGrinderSetting,
                    label = stringResource(R.string.label_grinder_setting),
                    placeholder = stringResource(R.string.placeholder_optional_grinder_setting),
                    leadingIcon = Icons.Filled.Engineering,
                    isError = uiState.grinderSettingError != null,
                    errorMessage = uiState.grinderSettingError
                )

                // Photo Section - available in both create and edit modes
                BeanPhotoSection(
                    photoPath = uiState.photoPath,
                    pendingPhotoUri = uiState.pendingPhotoUri,
                    isLoading = uiState.isPhotoLoading,
                    error = uiState.photoError,
                    onAddPhoto = ::handlePhotoCapture,
                    onReplacePhoto = ::handlePhotoCapture,
                    onDeletePhoto = viewModel::removePhoto,
                    onViewPhoto = ::handlePhotoView
                )

                // Active Status (only show in edit mode)
                if (uiState.isEditMode) {
                    CoffeeCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CardHeader(
                            icon = Icons.Default.Settings,
                            title = stringResource(R.string.text_bean_status),
                            actions = {
                                Switch(
                                    checked = uiState.isActive,
                                    onCheckedChange = viewModel::updateIsActive
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))

                        Text(
                            text = if (uiState.isActive) stringResource(R.string.text_active_long) else stringResource(R.string.text_inactive),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error Message
                if (uiState.error != null) {
                    ErrorCard(
                        title = stringResource(R.string.title_error),
                        message = uiState.error ?: "Unknown error occurred",
                        onDismiss = viewModel::clearError,
                        onRetry = { viewModel.saveBean() }
                    )
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                // Save Button
                CoffeePrimaryButton(
                    text = if (uiState.isSaving) stringResource(R.string.cd_saving) else if (uiState.isEditMode) stringResource(R.string.cd_update_bean) else stringResource(R.string.text_add_bean),
                    onClick = viewModel::saveBean,
                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )



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

    // Photo Viewer
    if (showPhotoViewer) {
        val photoPath = uiState.photoPath
        val pendingPhotoUri = uiState.pendingPhotoUri
        
        when {
            photoPath != null -> {
                PhotoViewer(
                    photoPath = photoPath,
                    onDismiss = { showPhotoViewer = false }
                )
            }
            pendingPhotoUri != null -> {
                PendingPhotoViewer(
                    photoUri = pendingPhotoUri,
                    onDismiss = { showPhotoViewer = false }
                )
            }
        }
    }
}

// Helper function to launch camera (placeholder for now)
private fun launchCamera(
    viewModel: AddEditBeanViewModel,
    cameraLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Uri, Boolean>,
    onTempUriCreated: (Uri) -> Unit
) {
    // TODO: Implement proper camera launch with PhotoCaptureManager
    // For now, this is a placeholder
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
                Text(stringResource(R.string.text_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.text_dialog_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.text_select_roast_date),
                    modifier = Modifier.padding(spacing.medium)
                )
            }
        )
    }
}