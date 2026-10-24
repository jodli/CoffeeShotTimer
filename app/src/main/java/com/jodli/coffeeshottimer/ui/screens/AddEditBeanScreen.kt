package com.jodli.coffeeshottimer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.ui.components.BeanPhotoSection
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeTextField
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.components.PhotoActionSheet
import com.jodli.coffeeshottimer.ui.components.PhotoViewer
import com.jodli.coffeeshottimer.ui.components.RuledRow
import com.jodli.coffeeshottimer.ui.components.SectionLabel
import com.jodli.coffeeshottimer.ui.components.ink
import com.jodli.coffeeshottimer.ui.components.labelRes
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.AddEditBeanUiState
import com.jodli.coffeeshottimer.ui.viewmodel.AddEditBeanViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Camera/gallery plumbing shared by the photo section. */
private class BeanPhotoLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
    val requestCameraPermission: () -> Unit,
    val isCameraPermissionGranted: () -> Boolean
)

/** Which editor dialog is open. */
private class BeanEditorDialogs {
    var datePicker by mutableStateOf(false)
    var photoViewer by mutableStateOf(false)
    var photoActionSheet by mutableStateOf(false)
    var deleteConfirm by mutableStateOf(false)
}

@Composable
private fun rememberBeanEditorDialogs(): BeanEditorDialogs = remember { BeanEditorDialogs() }

private fun pickImageRequest() = PickVisualMediaRequest(
    ActivityResultContracts.PickVisualMedia.ImageOnly
)

@Composable
private fun rememberBeanPhotoLaunchers(viewModel: AddEditBeanViewModel): BeanPhotoLaunchers {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(viewModel.isCameraPermissionGranted(context))
    }

    // Photo Picker launcher (single image)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::addPhoto)
    }
    val launchGalleryAction = {
        photoPickerLauncher.launch(pickImageRequest())
    }

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
    val launchCameraAction = {
        try {
            val cameraResult = viewModel.createCameraIntent()
            if (cameraResult != null) {
                val (_, uri) = cameraResult
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                // Camera intent creation failed, fallback to photo picker
                launchGalleryAction()
            }
        } catch (e: Exception) {
            // Camera intent creation failed, fallback to photo picker
            launchGalleryAction()
        }
    }

    // Camera permission launcher (CAMERA only)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            // Permission granted, launch camera directly
            launchCameraAction()
        } else {
            // Camera permission denied, allow user to pick from gallery without permissions
            launchGalleryAction()
        }
    }

    // Cleanup temp camera file when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            tempCameraUri?.let(viewModel::cleanupTempCameraFile)
        }
    }

    return remember {
        BeanPhotoLaunchers(
            launchCamera = launchCameraAction,
            launchGallery = launchGalleryAction,
            requestCameraPermission = {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            isCameraPermissionGranted = { cameraPermissionGranted }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBeanScreen(
    beanId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: AddEditBeanViewModel = hiltViewModel(),
    isOnboardingMode: Boolean = false,
    onSubmit: ((Bean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchers = rememberBeanPhotoLaunchers(viewModel)
    val dialogs = rememberBeanEditorDialogs()

    // Initialize for edit mode if beanId is provided
    LaunchedEffect(beanId) {
        if (beanId != null) {
            viewModel.initializeForEdit(beanId)
        }
    }

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val savedBean = uiState.savedBean
            if (isOnboardingMode && onSubmit != null && savedBean != null) {
                // In onboarding mode, call the custom submit callback
                onSubmit(savedBean)
            } else {
                // Normal mode, navigate back
                onNavigateBack()
            }
            viewModel.resetSaveSuccess()
        }
    }

    // Handle delete success — navigate back to the bean list
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onNavigateBack()
            viewModel.resetDeleteSuccess()
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
                            stringResource(R.string.cd_edit_bean)
                        } else {
                            stringResource(R.string.text_add_bean)
                        },
                        style = MaterialTheme.typography.titleLarge
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
        } else if (LocalIsLandscape.current) {
            LandscapeBeanForm(
                uiState = uiState,
                viewModel = viewModel,
                isOnboardingMode = isOnboardingMode,
                dialogs = dialogs,
                onNavigateBack = onNavigateBack
            )
        } else {
            PortraitBeanForm(
                uiState = uiState,
                viewModel = viewModel,
                isOnboardingMode = isOnboardingMode,
                dialogs = dialogs,
                onNavigateBack = onNavigateBack
            )
        }
    }

    BeanEditorDialogHost(
        dialogs = dialogs,
        uiState = uiState,
        viewModel = viewModel,
        launchers = launchers
    )
}

/** Single scroll column: details, photo, status, actions. */
@Composable
private fun PortraitBeanForm(
    uiState: AddEditBeanUiState,
    viewModel: AddEditBeanViewModel,
    isOnboardingMode: Boolean,
    dialogs: BeanEditorDialogs,
    onNavigateBack: () -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        BeanDetailsSection(
            uiState = uiState,
            isOnboardingMode = isOnboardingMode,
            onNameChange = viewModel::updateName,
            onNotesChange = viewModel::updateNotes,
            onPickRoastDate = { dialogs.datePicker = true }
        )
        BeanPhotoFormSection(
            uiState = uiState,
            viewModel = viewModel,
            dialogs = dialogs
        )
        if (uiState.isEditMode && !isOnboardingMode) {
            BeanStatusSection(
                uiState = uiState,
                viewModel = viewModel,
                dialogs = dialogs
            )
        }
        FormActions(
            uiState = uiState,
            isOnboardingMode = isOnboardingMode,
            onNavigateBack = onNavigateBack,
            onSave = viewModel::saveBean
        )
        Spacer(modifier = Modifier.height(spacing.large))
    }
}

/** Two scroll columns: details + actions | photo + status. */
@Composable
private fun LandscapeBeanForm(
    uiState: AddEditBeanUiState,
    viewModel: AddEditBeanViewModel,
    isOnboardingMode: Boolean,
    dialogs: BeanEditorDialogs,
    onNavigateBack: () -> Unit
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            BeanDetailsSection(
                uiState = uiState,
                isOnboardingMode = isOnboardingMode,
                onNameChange = viewModel::updateName,
                onNotesChange = viewModel::updateNotes,
                onPickRoastDate = { dialogs.datePicker = true }
            )
            FormActions(
                uiState = uiState,
                isOnboardingMode = isOnboardingMode,
                onNavigateBack = onNavigateBack,
                onSave = viewModel::saveBean
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            BeanPhotoFormSection(
                uiState = uiState,
                viewModel = viewModel,
                dialogs = dialogs
            )
            if (uiState.isEditMode && !isOnboardingMode) {
                BeanStatusSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    dialogs = dialogs
                )
            }
        }
    }
}

/** The editor's four dialogs, hosted once and toggled by [BeanEditorDialogs]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeanEditorDialogHost(
    dialogs: BeanEditorDialogs,
    uiState: AddEditBeanUiState,
    viewModel: AddEditBeanViewModel,
    launchers: BeanPhotoLaunchers
) {
    val context = LocalContext.current

    if (dialogs.datePicker) {
        DatePickerDialog(
            selectedDate = uiState.roastDate,
            onDateSelected = { date ->
                viewModel.updateRoastDate(date)
                dialogs.datePicker = false
            },
            onDismiss = { dialogs.datePicker = false }
        )
    }

    if (dialogs.photoViewer) {
        PhotoViewer(
            photoPath = uiState.photoPath,
            photoUri = uiState.pendingPhotoUri,
            onDismiss = { dialogs.photoViewer = false }
        )
    }

    if (dialogs.photoActionSheet) {
        PhotoActionSheet(
            onCameraCapture = {
                dialogs.photoActionSheet = false
                launchers.launchCamera()
            },
            onGallerySelect = {
                dialogs.photoActionSheet = false
                launchers.launchGallery()
            },
            onDismiss = { dialogs.photoActionSheet = false },
            cameraAvailable = viewModel.isCameraAvailable(context),
            cameraPermissionGranted = launchers.isCameraPermissionGranted(),
            onRequestCameraPermission = {
                dialogs.photoActionSheet = false
                launchers.requestCameraPermission()
            }
        )
    }

    // Delete Confirmation Dialog — the confirm action is the only red on the
    // form, matching the destructive-dialog recipe of the shot details screen.
    if (dialogs.deleteConfirm) {
        AlertDialog(
            onDismissRequest = { dialogs.deleteConfirm = false },
            title = {
                Text(stringResource(R.string.button_delete_bean))
            },
            text = {
                Text(
                    stringResource(
                        R.string.format_delete_bean_confirmation,
                        uiState.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogs.deleteConfirm = false
                        viewModel.deleteBean()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_bean_management_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { dialogs.deleteConfirm = false }
                ) {
                    Text(stringResource(R.string.text_dialog_cancel))
                }
            }
        )
    }
}

/** Name, roast date (with a live freshness verdict) and notes. */
@Composable
private fun BeanDetailsSection(
    uiState: AddEditBeanUiState,
    isOnboardingMode: Boolean,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickRoastDate: () -> Unit
) {
    val spacing = LocalSpacing.current
    val daysSinceRoast = ChronoUnit.DAYS.between(uiState.roastDate, LocalDate.now())
    val freshness = Bean.freshnessForDays(daysSinceRoast)

    SectionLabel(text = stringResource(R.string.label_bean_details))

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        // Bean Name
        CoffeeTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.label_bean_name_required),
            placeholder = stringResource(R.string.placeholder_enter_bean_name),
            leadingIcon = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
            isError = uiState.nameError != null,
            errorMessage = uiState.nameError
        )

        // Roast Date
        Column {
            CoffeeTextField(
                value = uiState.roastDate.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                ),
                onValueChange = { }, // Read-only, handled by date picker
                label = if (isOnboardingMode) {
                    stringResource(R.string.bean_form_roast_date_label)
                } else {
                    stringResource(R.string.label_roast_date_required)
                },
                placeholder = stringResource(R.string.placeholder_select_roast_date),
                leadingIcon = Icons.Default.DateRange,
                trailingIcon = Icons.Default.DateRange,
                onTrailingIconClick = onPickRoastDate,
                isError = uiState.roastDateError != null,
                errorMessage = uiState.roastDateError,
                singleLine = true
            )
            Text(
                text = stringResource(R.string.format_days_since_roast, daysSinceRoast) +
                    " · " + stringResource(freshness.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = freshness.ink(),
                modifier = Modifier.padding(top = spacing.extraSmall)
            )
        }

        // Notes
        CoffeeTextField(
            value = uiState.notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.label_notes),
            placeholder = stringResource(R.string.placeholder_optional_bean_notes),
            leadingIcon = Icons.Default.Edit,
            isError = uiState.notesError != null,
            errorMessage = uiState.notesError,
            singleLine = false,
            maxLines = 4
        )
    }
}

/** Photo management: label, section, viewer/replace/delete wiring. */
@Composable
private fun BeanPhotoFormSection(
    uiState: AddEditBeanUiState,
    viewModel: AddEditBeanViewModel,
    dialogs: BeanEditorDialogs
) {
    SectionLabel(text = stringResource(R.string.text_bean_photo))

    BeanPhotoSection(
        photoPath = uiState.photoPath,
        pendingPhotoUri = uiState.pendingPhotoUri,
        isLoading = uiState.isPhotoLoading,
        error = uiState.photoError,
        successMessage = uiState.photoSuccessMessage,
        canRetry = uiState.canRetryPhotoOperation,
        onAddPhoto = { dialogs.photoActionSheet = true },
        onReplacePhoto = { dialogs.photoActionSheet = true },
        onDeletePhoto = viewModel::removePhoto,
        onViewPhoto = { dialogs.photoViewer = true },
        onRetry = viewModel::retryPhotoOperation,
        onClearError = viewModel::clearPhotoError,
        onClearSuccess = viewModel::clearPhotoSuccessMessage
    )
}

/** Active switch on a ruled row, delete below as a quiet text action. */
@Composable
private fun BeanStatusSection(
    uiState: AddEditBeanUiState,
    viewModel: AddEditBeanViewModel,
    dialogs: BeanEditorDialogs
) {
    val spacing = LocalSpacing.current

    SectionLabel(text = stringResource(R.string.text_bean_status))

    RuledRow {
        Text(
            text = if (uiState.isActive) {
                stringResource(R.string.text_active_long)
            } else {
                stringResource(R.string.text_inactive)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = uiState.isActive,
            onCheckedChange = viewModel::updateIsActive
        )
    }

    TextButton(
        onClick = { dialogs.deleteConfirm = true },
        enabled = !uiState.isSaving,
        modifier = Modifier.padding(top = spacing.small)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(spacing.small))
        Text(
            text = stringResource(R.string.button_delete_bean),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Save error as a plain error line with retry, then the primary actions. */
@Composable
private fun FormActions(
    uiState: AddEditBeanUiState,
    isOnboardingMode: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit
) {
    val spacing = LocalSpacing.current

    if (uiState.error != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.text_retry))
            }
        }
    }

    if (isOnboardingMode) {
        // Onboarding mode: Back and Create buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeSecondaryButton(
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
                onClick = onSave,
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
            onClick = onSave,
            enabled = !uiState.isSaving && uiState.name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
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
        initialSelectedDateMillis = TimeUnit.DAYS.toMillis(selectedDate.toEpochDay())
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(millis))
                        )
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
