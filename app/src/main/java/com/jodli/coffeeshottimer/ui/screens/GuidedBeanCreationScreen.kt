package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.GuidedBeanCreationViewModel
import java.time.LocalDate

/**
 * Main screen for the guided bean creation flow during onboarding.
 * Provides an educational and user-friendly way for first-time users to create their first bean.
 *
 * @param onComplete Called when bean creation is successfully completed with the created bean
 * @param onSkip Called when the user chooses to skip bean creation
 * @param viewModel The ViewModel managing the guided bean creation state
 */
@Composable
fun GuidedBeanCreationScreen(
    onComplete: (Bean) -> Unit,
    onSkip: () -> Unit,
    viewModel: GuidedBeanCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LandscapeContainer(
            portraitContent = {
                GuidedBeanCreationPortraitLayout(
                    viewModel = viewModel,
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            },
            landscapeContent = {
                GuidedBeanCreationLandscapeLayout(
                    viewModel = viewModel,
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            }
        )
    }
}

/**
 * Portrait layout for the guided bean creation screen - maintains existing functionality
 */
@Composable
fun GuidedBeanCreationPortraitLayout(
    viewModel: GuidedBeanCreationViewModel,
    onComplete: (Bean) -> Unit,
    onSkip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(spacing.screenPadding)
    ) {
        when (uiState.currentPhase) {
            BeanCreationPhase.EDUCATION -> {
                BeanEducationContent(
                    onContinue = viewModel::proceedToForm,
                    onSkip = onSkip,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onClearError = viewModel::clearError
                )
            }

            BeanCreationPhase.FORM -> {
                AddEditBeanScreen(
                    beanId = null, // Always creating a new bean in onboarding
                    onNavigateBack = viewModel::returnToEducation,
                    isOnboardingMode = true,
                    onboardingTitle = stringResource(R.string.bean_form_title),
                    onSubmit = { createdBean ->
                        // Update the guided bean creation state with the created bean
                        viewModel.onBeanCreated(createdBean)
                    }
                )
            }

            BeanCreationPhase.SUCCESS -> {
                BeanCreationSuccess(
                    createdBean = uiState.createdBean!!,
                    freshnessMessage = viewModel.getFreshnessMessage(uiState.createdBean!!),
                    onContinue = { onComplete(uiState.createdBean!!) },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

/**
 * Landscape layout for the guided bean creation screen - optimized for horizontal space
 */
@Composable
fun GuidedBeanCreationLandscapeLayout(
    viewModel: GuidedBeanCreationViewModel,
    onComplete: (Bean) -> Unit,
    onSkip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = landscapeSpacing, vertical = spacing.small)
    ) {
        when (uiState.currentPhase) {
            BeanCreationPhase.EDUCATION -> {
                BeanEducationContentLandscape(
                    onContinue = viewModel::proceedToForm,
                    onSkip = onSkip,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onClearError = viewModel::clearError,
                    availableWidth = maxWidth,
                    modifier = Modifier.fillMaxSize()
                )
            }

            BeanCreationPhase.FORM -> {
                // Form phase uses AddEditBeanScreen which should handle landscape on its own
                AddEditBeanScreen(
                    beanId = null, // Always creating a new bean in onboarding
                    onNavigateBack = viewModel::returnToEducation,
                    isOnboardingMode = true,
                    onboardingTitle = stringResource(R.string.bean_form_title),
                    onSubmit = { createdBean ->
                        // Update the guided bean creation state with the created bean
                        viewModel.onBeanCreated(createdBean)
                    }
                )
            }

            BeanCreationPhase.SUCCESS -> {
                BeanCreationSuccessLandscape(
                    createdBean = uiState.createdBean!!,
                    freshnessMessage = viewModel.getFreshnessMessage(uiState.createdBean!!),
                    onContinue = { onComplete(uiState.createdBean!!) },
                    isLoading = uiState.isLoading,
                    availableWidth = maxWidth,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Content for the education phase of guided bean creation.
 * Explains the importance of bean tracking with coffee-themed education cards.
 */
@Composable
fun BeanEducationContent(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.large))

        // Coffee bean icon using existing resources with background circle
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                tonalElevation = 2.dp
            ) {}

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                contentDescription = stringResource(R.string.cd_bean_education_freshness),
                modifier = Modifier.size(70.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        Text(
            text = stringResource(R.string.bean_education_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Education cards using existing CoffeeCard
        BeanEducationCard(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.bean_education_freshness_title),
            description = stringResource(R.string.bean_education_freshness_description)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        BeanEducationCard(
            icon = Icons.Default.TrendingUp,
            title = stringResource(R.string.bean_education_history_title),
            description = stringResource(R.string.bean_education_history_description)
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        BeanEducationCard(
            icon = Icons.Default.PhotoCamera,
            title = stringResource(R.string.bean_education_visual_title),
            description = stringResource(R.string.bean_education_visual_description)
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Error display
        if (error != null) {
            ErrorCard(
                title = "Error",
                message = error,
                onDismiss = onClearError,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Loading indicator
        if (isLoading) {
            LoadingIndicator(
                message = "Preparing..."
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_skip_for_now),
                onClick = onSkip,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            )
            CoffeePrimaryButton(
                text = stringResource(R.string.button_add_my_first_bean),
                onClick = onContinue,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Landscape-optimized content for the education phase of guided bean creation.
 * Uses horizontal layout to better utilize landscape space.
 */
@Composable
fun BeanEducationContentLandscape(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    availableWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = landscapeSpacing, vertical = landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Icon and title (30% width)
        Column(
            modifier = Modifier.weight(0.3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Coffee bean icon with background circle
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {}

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                    contentDescription = stringResource(R.string.cd_bean_education_freshness),
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(landscapeSpacing))

            Text(
                text = stringResource(R.string.bean_education_title),
                style = MaterialTheme.typography.headlineSmall, // Smaller for landscape
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        // Right side: Content and actions (70% width)
        Column(
            modifier = Modifier.weight(0.7f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            // Education cards in compact layout
            BeanEducationCardLandscape(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.bean_education_freshness_title),
                description = stringResource(R.string.bean_education_freshness_description)
            )

            BeanEducationCardLandscape(
                icon = Icons.Default.TrendingUp,
                title = stringResource(R.string.bean_education_history_title),
                description = stringResource(R.string.bean_education_history_description)
            )

            BeanEducationCardLandscape(
                icon = Icons.Default.PhotoCamera,
                title = stringResource(R.string.bean_education_visual_title),
                description = stringResource(R.string.bean_education_visual_description)
            )

            Spacer(modifier = Modifier.height(landscapeSpacing))

            // Error display
            if (error != null) {
                ErrorCard(
                    title = "Error",
                    message = error,
                    onDismiss = onClearError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(landscapeSpacing))
            }

            // Loading indicator
            if (isLoading) {
                LoadingIndicator(
                    message = "Preparing..."
                )
                Spacer(modifier = Modifier.height(landscapeSpacing))
            }

            // Action buttons - horizontal layout for landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
            ) {
                CoffeeSecondaryButton(
                    text = stringResource(R.string.button_skip_for_now),
                    onClick = onSkip,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_add_my_first_bean),
                    onClick = onContinue,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual education card component using existing CoffeeCard
 */
@Composable
fun BeanEducationCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual education card component optimized for landscape - more compact
 */
@Composable
fun BeanEducationCardLandscape(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    CoffeeCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(landscapeSpacing),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(landscapeSpacing)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp), // Smaller for landscape
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall, // Smaller for landscape
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall, // Smaller for landscape
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Success screen shown after bean creation is completed.
 * Displays the created bean information and celebration message.
 */
@Composable
fun BeanCreationSuccess(
    createdBean: Bean,
    freshnessMessage: String,
    onContinue: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.large))

        // Success icon using existing coffee bean icon with background circle
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                tonalElevation = 2.dp
            ) {}

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                contentDescription = stringResource(R.string.cd_bean_creation_success),
                modifier = Modifier.size(70.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        Text(
            text = stringResource(R.string.bean_creation_success_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        Text(
            text = stringResource(R.string.bean_creation_success_message, createdBean.name),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Bean summary card
        BeanSummaryCard(
            bean = createdBean,
            freshnessMessage = freshnessMessage,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Loading indicator
        if (isLoading) {
            LoadingIndicator(
                message = "Finalizing..."
            )
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Continue button
        CoffeePrimaryButton(
            text = stringResource(R.string.button_continue_to_shot),
            onClick = onContinue,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.large))
    }
}

/**
 * Bean summary card displayed in the success screen
 */
@Composable
fun BeanSummaryCard(
    bean: Bean,
    freshnessMessage: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier
    ) {
        Column {
            // Bean name and details
            Text(
                text = bean.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // Roast date information
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now())
                val dateText = if (daysSinceRoast == 0L) {
                    "Roasted today"
                } else {
                    "Roasted $daysSinceRoast day${if (daysSinceRoast != 1L) "s" else ""} ago"
                }

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Freshness message with highlight
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = freshnessMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Notes if available
            if (bean.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.medium))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = bean.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * Landscape-optimized success screen shown after bean creation is completed.
 * Uses horizontal layout to better utilize landscape space.
 */
@Composable
fun BeanCreationSuccessLandscape(
    createdBean: Bean,
    freshnessMessage: String,
    onContinue: () -> Unit,
    isLoading: Boolean,
    availableWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = landscapeSpacing, vertical = landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Icon and success message (40% width)
        Column(
            modifier = Modifier.weight(0.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon using existing coffee bean icon with background circle
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {}

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                    contentDescription = stringResource(R.string.cd_bean_creation_success),
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(landscapeSpacing))

            Text(
                text = stringResource(R.string.bean_creation_success_title),
                style = MaterialTheme.typography.headlineSmall, // Smaller for landscape
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(landscapeSpacing))

            Text(
                text = stringResource(R.string.bean_creation_success_message, createdBean.name),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Right side: Bean summary and actions (60% width)
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            // Bean summary card
            BeanSummaryCard(
                bean = createdBean,
                freshnessMessage = freshnessMessage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(landscapeSpacing))

            // Loading indicator
            if (isLoading) {
                LoadingIndicator(
                    message = "Finalizing..."
                )
                Spacer(modifier = Modifier.height(landscapeSpacing))
            }

            // Continue button
            CoffeePrimaryButton(
                text = stringResource(R.string.button_continue_to_shot),
                onClick = onContinue,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Preview for the guided bean creation screen in education phase
 */
@Preview(showBackground = true)
@Composable
fun GuidedBeanCreationScreenEducationPreview() {
    CoffeeShotTimerTheme {
        GuidedBeanCreationScreenPreview(BeanCreationPhase.EDUCATION)
    }
}

/**
 * Preview for the guided bean creation screen in form phase
 */
@Preview(showBackground = true)
@Composable
fun GuidedBeanCreationScreenFormPreview() {
    CoffeeShotTimerTheme {
        GuidedBeanCreationScreenPreview(BeanCreationPhase.FORM)
    }
}

/**
 * Preview for the guided bean creation screen in success phase
 */
@Preview(showBackground = true)
@Composable
fun GuidedBeanCreationScreenSuccessPreview() {
    CoffeeShotTimerTheme {
        GuidedBeanCreationScreenPreview(BeanCreationPhase.SUCCESS)
    }
}

/**
 * Preview helpers and mock data
 */
object GuidedBeanCreationPreviewData {
    val mockBean = Bean(
        id = "preview-bean",
        name = "Ethiopian Yirgacheffe",
        roastDate = LocalDate.now().minusDays(7),
        notes = "Floral and bright",
        isActive = true
    )
}

/**
 * Main preview composable that shows different phases
 */
@Composable
fun GuidedBeanCreationScreenPreview(
    phase: BeanCreationPhase,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(spacing.screenPadding)
        ) {
            when (phase) {
                BeanCreationPhase.EDUCATION -> {
                    BeanEducationContent(
                        onContinue = {},
                        onSkip = {},
                        isLoading = false,
                        error = null,
                        onClearError = {}
                    )
                }

                BeanCreationPhase.FORM -> {
                    // Show placeholder for form in preview
                    Text(
                        text = "Bean Creation Form",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                BeanCreationPhase.SUCCESS -> {
                    BeanCreationSuccess(
                        createdBean = GuidedBeanCreationPreviewData.mockBean,
                        freshnessMessage = "Perfect timing – these beans are in their optimal freshness window!",
                        onContinue = {},
                        isLoading = false
                    )
                }
            }
        }
    }
}
