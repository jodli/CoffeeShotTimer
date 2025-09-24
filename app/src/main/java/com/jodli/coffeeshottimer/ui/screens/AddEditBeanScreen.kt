package com.jodli.coffeeshottimer.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoSection
import com.jodli.coffeeshottimer.ui.components.CardHeader
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoActionSheet
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.AddEditBeanViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBeanScreen(
    beanId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: AddEditBeanViewModel = hiltViewModel(),
    isOnboardingMode: Boolean = false,
    onSubmit: ((com.jodli.coffeeshottimer.data.model.Bean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var showPhotoActionSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(viewModel.isCameraPermissionGranted(context)) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val currentTempUri = tempCameraUri
        if (success && currentTempUri != null) {
            viewModel.addPhoto(currentTempUri)
        } else if (currentTempUri != null) {
            // Camera capture failed or was cancelled, cleanup temp file
            viewModel.cleanupTempCameraFile(currentTempUri)
        }
        tempCameraUri = null
    }

    // Photo Picker launcher (single image)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addPhoto(it) }
    }

    // Camera permission launcher (CAMERA only)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            // Permission granted, launch camera directly
            try {
                val cameraResult = viewModel.createCameraIntent()
                if (cameraResult != null) {
                    val (intent, uri) = cameraResult
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    // Camera intent creation failed, fallback to photo picker
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            } catch (e: Exception) {
                // Camera intent creation failed, fallback to photo picker
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        } else {
            // Camera permission denied, allow user to pick from gallery without permissions
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    }

    // Function to launch camera
    val launchCamera = remember {
        {
            try {
                val cameraResult = viewModel.createCameraIntent()
                if (cameraResult != null) {
                    val (intent, uri) = cameraResult
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    // Camera intent creation failed, fallback to photo picker
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            } catch (e: Exception) {
                // Camera intent creation failed, fallback to photo picker
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        }
    }

    // Function to launch gallery (Photo Picker)
    val launchGallery = remember {
        {
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    }

    // Function to handle photo capture (shows action sheet)
    val handlePhotoCapture = remember {
        {
            showPhotoActionSheet = true
        }
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
            if (isOnboardingMode && onSubmit != null && uiState.savedBean != null) {
                // In onboarding mode, call the custom submit callback
                onSubmit(uiState.savedBean!!)
            } else {
                // Normal mode, navigate back
                onNavigateBack()
            }
            viewModel.resetSaveSuccess()
        }
    }

    // Cleanup temp camera file when component is disposed
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            tempCameraUri?.let { uri ->
                viewModel.cleanupTempCameraFile(uri)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar (only show in non-onboarding mode)
        if (!isOnboardingMode) {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) {
                            stringResource(
                                R.string.cd_edit_bean
                            )
                        } else {
                            stringResource(R.string.text_add_bean)
                        },
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
        }

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(message = stringResource(R.string.loading_bean_details))
            }
        } else {
            LandscapeContainer(
                portraitContent = {
                    // Existing single-column layout for portrait
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
                            label = if (isOnboardingMode) {
                                stringResource(
                                    R.string.bean_form_roast_date_label
                                )
                            } else {
                                stringResource(R.string.label_roast_date_required)
                            },
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

                        // Photo Section - available in both create and edit modes
                        BeanPhotoSection(
                            photoPath = uiState.photoPath,
                            pendingPhotoUri = uiState.pendingPhotoUri,
                            isLoading = uiState.isPhotoLoading,
                            error = uiState.photoError,
                            successMessage = uiState.photoSuccessMessage,
                            canRetry = uiState.canRetryPhotoOperation,
                            onAddPhoto = { handlePhotoCapture() },
                            onReplacePhoto = { handlePhotoCapture() },
                            onDeletePhoto = viewModel::removePhoto,
                            onViewPhoto = { handlePhotoView() },
                            onRetry = viewModel::retryPhotoOperation,
                            onClearError = viewModel::clearPhotoError,
                            onClearSuccess = viewModel::clearPhotoSuccessMessage
                        )

                        // Active Status (only show in edit mode and not in onboarding)
                        if (uiState.isEditMode && !isOnboardingMode) {
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
                                    text = if (uiState.isActive) {
                                        stringResource(
                                            R.string.text_active_long
                                        )
                                    } else {
                                        stringResource(R.string.text_inactive)
                                    },
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

                        // Action Buttons
                        if (isOnboardingMode) {
                            // Onboarding mode: Back and Create buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                            ) {
                                com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton(
                                    text = stringResource(R.string.button_back),
                                    onClick = onNavigateBack,
                                    enabled = !uiState.isSaving,
                                    modifier = Modifier.weight(1f)
                                )
                                CoffeePrimaryButton(
                                    text = if (uiState.isSaving) {
                                        stringResource(R.string.cd_saving)
                                    } else {
                                        stringResource(R.string.button_create_bean)
                                    },
                                    onClick = viewModel::saveBean,
                                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Normal mode: Single save/update button
                            CoffeePrimaryButton(
                                text = if (uiState.isSaving) {
                                    stringResource(R.string.cd_saving)
                                } else if (uiState.isEditMode) {
                                    stringResource(R.string.cd_update_bean)
                                } else {
                                    stringResource(R.string.text_add_bean)
                                },
                                onClick = viewModel::saveBean,
                                enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Bottom padding for scrolling
                        Spacer(modifier = Modifier.height(spacing.large))
                    }
                },
                landscapeContent = {
                    // Two-column layout for landscape
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            // Left column - Basic info and settings
                            Column(
                                modifier = Modifier.weight(1f),
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
                                    label = if (isOnboardingMode) {
                                        stringResource(
                                            R.string.bean_form_roast_date_label
                                        )
                                    } else {
                                        stringResource(R.string.label_roast_date_required)
                                    },
                                    placeholder = stringResource(R.string.placeholder_select_roast_date),
                                    leadingIcon = Icons.Default.DateRange,
                                    trailingIcon = Icons.Default.DateRange,
                                    onTrailingIconClick = { showDatePicker = true },
                                    isError = uiState.roastDateError != null,
                                    errorMessage = uiState.roastDateError,
                                    singleLine = true
                                )

                                // Notes - give more space in landscape
                                CoffeeTextField(
                                    value = uiState.notes,
                                    onValueChange = viewModel::updateNotes,
                                    label = stringResource(R.string.label_notes),
                                    placeholder = stringResource(R.string.placeholder_optional_bean_notes),
                                    leadingIcon = Icons.Default.Edit,
                                    isError = uiState.notesError != null,
                                    errorMessage = uiState.notesError,
                                    singleLine = false,
                                    maxLines = 6 // More lines in landscape
                                )

                                // Active Status (only show in edit mode and not in onboarding)
                                if (uiState.isEditMode && !isOnboardingMode) {
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
                                            text = if (uiState.isActive) {
                                                stringResource(
                                                    R.string.text_active_long
                                                )
                                            } else {
                                                stringResource(R.string.text_inactive)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Right column - Photo and notes
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(spacing.medium)
                            ) {
                                // Photo Section - more prominent in landscape
                                BeanPhotoSection(
                                    photoPath = uiState.photoPath,
                                    pendingPhotoUri = uiState.pendingPhotoUri,
                                    isLoading = uiState.isPhotoLoading,
                                    error = uiState.photoError,
                                    successMessage = uiState.photoSuccessMessage,
                                    canRetry = uiState.canRetryPhotoOperation,
                                    onAddPhoto = { handlePhotoCapture() },
                                    onReplacePhoto = { handlePhotoCapture() },
                                    onDeletePhoto = viewModel::removePhoto,
                                    onViewPhoto = { handlePhotoView() },
                                    onRetry = viewModel::retryPhotoOperation,
                                    onClearError = viewModel::clearPhotoError,
                                    onClearSuccess = viewModel::clearPhotoSuccessMessage
                                )
                            }
                        }

                        // Error Message (full width)
                        if (uiState.error != null) {
                            ErrorCard(
                                title = stringResource(R.string.title_error),
                                message = uiState.error ?: "Unknown error occurred",
                                onDismiss = viewModel::clearError,
                                onRetry = { viewModel.saveBean() }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Action Buttons (full width)
                        if (isOnboardingMode) {
                            // Onboarding mode: Back and Create buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                            ) {
                                com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton(
                                    text = stringResource(R.string.button_back),
                                    onClick = onNavigateBack,
                                    enabled = !uiState.isSaving,
                                    modifier = Modifier.weight(1f)
                                )
                                CoffeePrimaryButton(
                                    text = if (uiState.isSaving) {
                                        stringResource(R.string.cd_saving)
                                    } else {
                                        stringResource(R.string.button_create_bean)
                                    },
                                    onClick = viewModel::saveBean,
                                    enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Normal mode: Single save/update button
                            CoffeePrimaryButton(
                                text = if (uiState.isSaving) {
                                    stringResource(R.string.cd_saving)
                                } else if (uiState.isEditMode) {
                                    stringResource(R.string.cd_update_bean)
                                } else {
                                    stringResource(R.string.text_add_bean)
                                },
                                onClick = viewModel::saveBean,
                                enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Bottom padding for scrolling
                        Spacer(modifier = Modifier.height(spacing.large))
                    }
                }
            )
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
        PhotoViewer(
            photoPath = uiState.photoPath,
            photoUri = uiState.pendingPhotoUri,
            onDismiss = { showPhotoViewer = false }
        )
    }

    // Photo Action Sheet
    if (showPhotoActionSheet) {
        PhotoActionSheet(
            onCameraCapture = {
                showPhotoActionSheet = false
                launchCamera()
            },
            onGallerySelect = {
                showPhotoActionSheet = false
                launchGallery()
            },
            onDismiss = { showPhotoActionSheet = false },
            cameraAvailable = viewModel.isCameraAvailable(context),
            cameraPermissionGranted = cameraPermissionGranted,
            storagePermissionGranted = true,
            onRequestCameraPermission = {
                showPhotoActionSheet = false
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onRequestStoragePermission = {
                // No storage permission needed for Photo Picker
            }
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
