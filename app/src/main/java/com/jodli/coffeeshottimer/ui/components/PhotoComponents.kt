package com.jodli.coffeeshottimer.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.io.File

/**
 * Main photo section component for displaying and managing bean photos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanPhotoSection(
    photoPath: String?,
    pendingPhotoUri: Uri? = null,
    isLoading: Boolean = false,
    error: String? = null,
    successMessage: String? = null,
    canRetry: Boolean = false,
    onAddPhoto: () -> Unit,
    onReplacePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRetry: () -> Unit = {},
    onClearError: () -> Unit = {},
    onClearSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    CoffeeCard(modifier = modifier) {
        CardHeader(
            icon = Icons.Default.PhotoCamera,
            title = stringResource(R.string.text_bean_photo)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Success message display
        successMessage?.let { message ->
            PhotoSuccessState(
                message = message,
                onDismiss = onClearSuccess
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        when {
            isLoading -> {
                PhotoLoadingState()
            }
            error != null -> {
                PhotoErrorState(
                    error = error,
                    canRetry = canRetry,
                    onRetry = onRetry,
                    onTryAgain = onAddPhoto,
                    onDismiss = onClearError
                )
            }
            photoPath != null -> {
                PhotoDisplaySection(
                    photoPath = photoPath,
                    onViewPhoto = onViewPhoto,
                    onReplacePhoto = onReplacePhoto,
                    onDeletePhoto = { showDeleteDialog = true }
                )
            }
            pendingPhotoUri != null -> {
                PendingPhotoDisplaySection(
                    pendingPhotoUri = pendingPhotoUri,
                    onViewPhoto = onViewPhoto,
                    onReplacePhoto = onReplacePhoto,
                    onDeletePhoto = { showDeleteDialog = true }
                )
            }
            else -> {
                PhotoEmptyState(
                    onAddPhoto = onAddPhoto
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            PhotoDeleteDialog(
                onConfirm = {
                    showDeleteDialog = false
                    onDeletePhoto()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

/**
 * Loading state for photo operations
 */
@Composable
private fun PhotoLoadingState(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = stringResource(R.string.text_loading_photo)
        )
    }
}

/**
 * Success state for photo operations
 */
@Composable
private fun PhotoSuccessState(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(spacing.cornerMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(spacing.iconSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_success),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(spacing.iconSmall)
                )
            }
        }
    }
}

/**
 * Error state for photo operations with retry functionality
 */
@Composable
private fun PhotoErrorState(
    error: String,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(spacing.cornerMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Error message
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(spacing.iconSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_dismiss_error),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(spacing.iconSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                if (canRetry) {
                    CoffeeSecondaryButton(
                        text = stringResource(R.string.text_retry),
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    )
                }

                CoffeePrimaryButton(
                    text = stringResource(R.string.text_try_again),
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Photo display section when photo exists
 */
@Composable
private fun PhotoDisplaySection(
    photoPath: String,
    onViewPhoto: () -> Unit,
    onReplacePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Photo display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable { onViewPhoto() },
            shape = RoundedCornerShape(spacing.cornerMedium),
            elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationCard)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(photoPath))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_bean_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.text_view_short),
                onClick = onViewPhoto,
                icon = Icons.Default.Image,
                modifier = Modifier.weight(1f)
            )

            CoffeeSecondaryButton(
                text = stringResource(R.string.text_replace_short),
                onClick = onReplacePhoto,
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f)
            )

            CoffeeSecondaryButton(
                text = stringResource(R.string.text_delete_short),
                onClick = onDeletePhoto,
                icon = Icons.Default.Delete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Pending photo display section for create mode when photo is selected but not yet saved
 */
@Composable
private fun PendingPhotoDisplaySection(
    pendingPhotoUri: Uri,
    onViewPhoto: () -> Unit,
    onReplacePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Photo display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable { onViewPhoto() },
            shape = RoundedCornerShape(spacing.cornerMedium),
            elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationCard)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(pendingPhotoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_bean_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Pending indicator
        Text(
            text = stringResource(R.string.text_photo_will_be_saved),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.small)
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.text_view_short),
                onClick = onViewPhoto,
                icon = Icons.Default.Image,
                modifier = Modifier.weight(1f)
            )

            CoffeeSecondaryButton(
                text = stringResource(R.string.text_replace_short),
                onClick = onReplacePhoto,
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f)
            )

            CoffeeSecondaryButton(
                text = stringResource(R.string.text_delete_short),
                onClick = onDeletePhoto,
                icon = Icons.Default.Delete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Empty state when no photo is present
 */
@Composable
private fun PhotoEmptyState(
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empty photo placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable { onAddPhoto() },
            shape = RoundedCornerShape(spacing.cornerMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_photo),
                        modifier = Modifier.size(spacing.iconLarge),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.small))

                    Text(
                        text = stringResource(R.string.text_no_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Add photo button
        CoffeePrimaryButton(
            text = stringResource(R.string.text_add_photo),
            onClick = onAddPhoto,
            icon = Icons.Default.CameraAlt
        )
    }
}

/**
 * Bottom sheet for photo action selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoActionSheet(
    onCameraCapture: () -> Unit,
    onGallerySelect: () -> Unit,
    onDismiss: () -> Unit,
    cameraAvailable: Boolean = true,
    cameraPermissionGranted: Boolean = true,
    storagePermissionGranted: Boolean = true,
    onRequestCameraPermission: () -> Unit = {},
    onRequestStoragePermission: () -> Unit = {},
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            // Header
            Text(
                text = stringResource(R.string.text_photo_options),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = spacing.medium)
            )

            // Camera option
            when {
                !cameraAvailable -> {
                    PhotoActionItemDisabled(
                        icon = Icons.Default.PhotoCamera,
                        title = stringResource(R.string.text_take_photo),
                        subtitle = stringResource(R.string.error_camera_unavailable)
                    )
                }
                !cameraPermissionGranted -> {
                    PhotoActionItem(
                        icon = Icons.Default.PhotoCamera,
                        title = stringResource(R.string.text_take_photo),
                        subtitle = stringResource(R.string.text_grant_permission),
                        onClick = onRequestCameraPermission
                    )
                }
                else -> {
                    PhotoActionItem(
                        icon = Icons.Default.PhotoCamera,
                        title = stringResource(R.string.text_take_photo),
                        onClick = onCameraCapture
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // Gallery option
            when {
                !storagePermissionGranted -> {
                    PhotoActionItem(
                        icon = Icons.Default.PhotoLibrary,
                        title = stringResource(R.string.text_choose_from_gallery),
                        subtitle = stringResource(R.string.text_grant_permission),
                        onClick = onRequestStoragePermission
                    )
                }
                else -> {
                    PhotoActionItem(
                        icon = Icons.Default.PhotoLibrary,
                        title = stringResource(R.string.text_choose_from_gallery),
                        onClick = onGallerySelect
                    )
                }
            }

            // Bottom spacing for navigation
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}

/**
 * Individual action item in the photo action sheet
 */
@Composable
private fun PhotoActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(spacing.cornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.iconMedium)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Disabled action item in the photo action sheet
 */
@Composable
private fun PhotoActionItemDisabled(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.cornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(spacing.iconMedium)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Full-screen photo viewer that can handle both file paths and URIs
 */
@Composable
fun PhotoViewer(
    photoPath: String? = null,
    photoUri: Uri? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // Photo
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoPath?.let { File(it) } ?: photoUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_bean_photo),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close_photo_viewer),
                tint = Color.White,
                modifier = Modifier.size(spacing.iconMedium)
            )
        }
    }
}

/**
 * Delete photo confirmation dialog
 */
@Composable
private fun PhotoDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_delete_photo_title))
        },
        text = {
            Text(text = stringResource(R.string.dialog_delete_photo_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.text_dialog_ok),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.text_dialog_cancel))
            }
        }
    )
}

/**
 * Small photo thumbnail for bean list items
 */
@Composable
fun BeanPhotoThumbnail(
    photoPath: String?,
    onPhotoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    if (photoPath != null) {
        Card(
            modifier = modifier
                .size(spacing.thumbnailSize)
                .then(
                    if (onPhotoClick != null) {
                        Modifier.clickable { onPhotoClick() }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(spacing.cornerSmall),
            elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationCard)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(photoPath))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_bean_photo_thumbnail),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        // Empty placeholder for consistent spacing
        Box(
            modifier = modifier.size(spacing.thumbnailSize)
        )
    }
}
