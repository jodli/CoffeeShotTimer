package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.DataCell
import com.jodli.coffeeshottimer.ui.components.ErrorCard
import com.jodli.coffeeshottimer.ui.components.IconTextRow
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LedgerSection
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import com.jodli.coffeeshottimer.ui.theme.zoneColors
import com.jodli.coffeeshottimer.ui.viewmodel.GuidedBeanCreationViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    Box(
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            BeanCreationPhase.FORM -> {
                // Form phase uses AddEditBeanScreen which should handle landscape on its own
                AddEditBeanScreen(
                    beanId = null, // Always creating a new bean in onboarding
                    onNavigateBack = viewModel::returnToEducation,
                    isOnboardingMode = true,
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Content for the education phase of guided bean creation.
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
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.bean_education_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 24.dp)
        )

        IconTextRow(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.bean_education_freshness_title),
            description = stringResource(R.string.bean_education_freshness_description),
            modifier = Modifier.padding(top = 16.dp)
        )

        IconTextRow(
            icon = Icons.AutoMirrored.Default.TrendingUp,
            title = stringResource(R.string.bean_education_history_title),
            description = stringResource(R.string.bean_education_history_description)
        )

        IconTextRow(
            icon = Icons.Default.PhotoCamera,
            title = stringResource(R.string.bean_education_visual_title),
            description = stringResource(R.string.bean_education_visual_description)
        )

        if (error != null) {
            ErrorCard(
                title = stringResource(R.string.error_title),
                message = error,
                onDismiss = onClearError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.medium)
            )
        }

        if (isLoading) {
            LoadingIndicator(
                message = stringResource(R.string.text_preparing),
                modifier = Modifier.padding(top = spacing.medium)
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

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
 */
@Composable
fun BeanEducationContentLandscape(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = landscapeSpacing, vertical = landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2)
    ) {
        Column(
            modifier = Modifier.weight(0.3f)
        ) {
            Text(
                text = stringResource(R.string.bean_education_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(0.7f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            IconTextRow(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.bean_education_freshness_title),
                description = stringResource(R.string.bean_education_freshness_description)
            )

            IconTextRow(
                icon = Icons.AutoMirrored.Default.TrendingUp,
                title = stringResource(R.string.bean_education_history_title),
                description = stringResource(R.string.bean_education_history_description)
            )

            IconTextRow(
                icon = Icons.Default.PhotoCamera,
                title = stringResource(R.string.bean_education_visual_title),
                description = stringResource(R.string.bean_education_visual_description)
            )

            if (error != null) {
                ErrorCard(
                    title = stringResource(R.string.error_title),
                    message = error,
                    onDismiss = onClearError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                LoadingIndicator(message = stringResource(R.string.text_preparing))
            }

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
 * Success screen shown after bean creation is completed.
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
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
            contentDescription = stringResource(R.string.cd_bean_creation_success),
            modifier = Modifier
                .padding(top = 24.dp)
                .size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.bean_creation_success_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = stringResource(R.string.bean_creation_success_message, createdBean.name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        BeanSummaryLedger(
            bean = createdBean,
            freshnessMessage = freshnessMessage,
            modifier = Modifier.padding(top = 16.dp)
        )

        if (isLoading) {
            LoadingIndicator(
                message = stringResource(R.string.text_finalizing),
                modifier = Modifier.padding(top = spacing.medium)
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        CoffeePrimaryButton(
            text = stringResource(R.string.button_continue_to_shot),
            onClick = onContinue,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.large))
    }
}

/** Roast date as a relative figure: today, yesterday, or N days ago. */
private fun daysSinceRoast(bean: Bean): Long =
    ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now())

/**
 * Bean summary as ruled ledger cells: roast date, freshness, notes.
 */
@Composable
private fun BeanSummaryLedger(
    bean: Bean,
    freshnessMessage: String,
    modifier: Modifier = Modifier
) {
    val days = daysSinceRoast(bean)
    val roastDateText = when {
        days == 0L -> stringResource(R.string.format_roasted_today)
        days == 1L -> stringResource(R.string.format_roasted_yesterday)
        else -> stringResource(R.string.format_roasted_days_ago, days)
    }

    LedgerSection(title = bean.name, modifier = modifier) {
        DataCell(label = stringResource(R.string.bean_summary_roast_date_label)) {
            Text(
                text = roastDateText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        DataCell(label = stringResource(R.string.bean_summary_freshness_label)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (days == 0L) Icons.Outlined.Schedule else Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (days == 0L) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.zoneColors.idealInk
                    },
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = freshnessMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (bean.notes.isNotBlank()) {
            DataCell(label = stringResource(R.string.bean_summary_notes_label)) {
                Text(
                    text = bean.notes,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Landscape-optimized success screen shown after bean creation is completed.
 */
@Composable
fun BeanCreationSuccessLandscape(
    createdBean: Bean,
    freshnessMessage: String,
    onContinue: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = landscapeSpacing, vertical = landscapeSpacing),
        horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2)
    ) {
        Column(
            modifier = Modifier.weight(0.4f)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.coffee_bean_icon),
                contentDescription = stringResource(R.string.cd_bean_creation_success),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.bean_creation_success_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(R.string.bean_creation_success_message, createdBean.name),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            BeanSummaryLedger(
                bean = createdBean,
                freshnessMessage = freshnessMessage
            )

            if (isLoading) {
                LoadingIndicator(message = stringResource(R.string.text_finalizing))
            }

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
                        modifier = Modifier.fillMaxWidth()
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
