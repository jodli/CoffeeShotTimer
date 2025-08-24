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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing

/**
 * Data model for introduction slides
 */
data class IntroSlide(
    val title: String,
    val description: String,
    val illustration: ImageVector,
    val highlights: List<FeatureHighlight> = emptyList()
)

/**
 * Data model for feature highlights within slides
 */
data class FeatureHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Creates the list of introduction slides with content
 */
@Composable
private fun createIntroSlides(): List<IntroSlide> {
    return listOf(
        // Welcome slide
        IntroSlide(
            title = stringResource(R.string.intro_welcome_title),
            description = stringResource(R.string.intro_welcome_description),
            illustration = Icons.Default.Coffee
        ),

        // Main screens overview slide
        IntroSlide(
            title = stringResource(R.string.intro_main_screens_title),
            description = stringResource(R.string.intro_main_screens_description),
            illustration = Icons.Default.Timeline,
            highlights = listOf(
                FeatureHighlight(
                    title = stringResource(R.string.intro_record_shot_title),
                    description = stringResource(R.string.intro_record_shot_description),
                    icon = Icons.Default.Coffee
                ),
                FeatureHighlight(
                    title = stringResource(R.string.intro_shot_history_title),
                    description = stringResource(R.string.intro_shot_history_description),
                    icon = Icons.Default.History
                ),
                FeatureHighlight(
                    title = stringResource(R.string.intro_bean_management_title),
                    description = stringResource(R.string.intro_bean_management_description),
                    icon = Icons.Default.Settings
                )
            )
        ),

        // Flexible workflow slide
        IntroSlide(
            title = stringResource(R.string.intro_flexible_workflow_title),
            description = stringResource(R.string.intro_flexible_workflow_description),
            illustration = Icons.AutoMirrored.Filled.AltRoute
        ),

        // Timer usage slide
        IntroSlide(
            title = stringResource(R.string.intro_timer_usage_title),
            description = stringResource(R.string.intro_timer_usage_description),
            illustration = Icons.Default.Schedule
        ),

        // Get started slide
        IntroSlide(
            title = stringResource(R.string.intro_get_started_title),
            description = stringResource(R.string.intro_get_started_description),
            illustration = ImageVector.vectorResource(R.drawable.coffee_bean_icon)
        )
    )
}

/**
 * Main introduction screen composable that provides onboarding information.
 * Uses LandscapeContainer for responsive design like GuidedBeanCreationScreen.
 */
@Composable
fun IntroductionScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val slides = createIntroSlides()
    var currentPage by remember { mutableIntStateOf(0) }

    val onNext = {
        if (currentPage < slides.size - 1) {
            currentPage++
        }
    }

    val onPrevious = {
        if (currentPage > 0) {
            currentPage--
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LandscapeContainer(
            portraitContent = {
                IntroductionPortraitLayout(
                    slides = slides,
                    currentPage = currentPage,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            },
            landscapeContent = {
                IntroductionLandscapeLayout(
                    slides = slides,
                    currentPage = currentPage,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            }
        )
    }
}

/**
 * Portrait layout for IntroductionScreen - simplified to match GuidedBeanCreationScreen style
 */
@Composable
fun IntroductionPortraitLayout(
    slides: List<IntroSlide>,
    currentPage: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = LocalSpacing.current
    val currentSlide = slides[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(spacing.screenPadding)
    ) {
        // Main content area - scrollable (removed separate skip button)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.large))

            // Icon with background circle
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
                    imageVector = currentSlide.illustration,
                    contentDescription = currentSlide.title,
                    modifier = Modifier.size(70.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))

            // Title
            Text(
                text = currentSlide.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            // Description
            Text(
                text = currentSlide.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
            )

            // Feature highlights if available
            if (currentSlide.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.large))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    horizontalAlignment = Alignment.Start
                ) {
                    currentSlide.highlights.forEach { highlight ->
                        FeatureHighlightItem(
                            highlight = highlight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.extraLarge))
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.medium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.forEachIndexed { index, _ ->
                PageIndicator(
                    isActive = index == currentPage,
                    modifier = Modifier.padding(horizontal = spacing.extraSmall)
                )
            }
        }

        // Navigation buttons - Skip and Next/Get Started (like GuidedBeanCreationScreen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Skip button (replaces Previous button)
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_skip),
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            )

            // Next/Complete button
            CoffeePrimaryButton(
                text = if (currentPage == slides.size - 1) {
                    stringResource(R.string.button_get_started)
                } else {
                    stringResource(R.string.button_next)
                },
                onClick = if (currentPage == slides.size - 1) {
                    onComplete
                } else {
                    onNext
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Landscape layout for IntroductionScreen - matches GuidedBeanCreationScreen's 30%/70% pattern
 */
@Composable
fun IntroductionLandscapeLayout(
    slides: List<IntroSlide>,
    currentPage: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    val currentSlide = slides[currentPage]

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content with proper padding
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = landscapeSpacing, vertical = spacing.small)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon and title (30% width) - matches GuidedBeanCreationScreen
            Column(
                modifier = Modifier.weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon with background circle
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
                        imageVector = currentSlide.illustration,
                        contentDescription = currentSlide.title,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(landscapeSpacing))

                Text(
                    text = currentSlide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Right side: Content and actions (70% width) - matches GuidedBeanCreationScreen
            Column(
                modifier = Modifier.weight(0.7f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
            ) {
                // Description (no separate skip button needed)
                Text(
                    text = currentSlide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                )

                // Feature highlights if available using CoffeeCard for consistency
                if (currentSlide.highlights.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(landscapeSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentSlide.highlights.forEach { highlight ->
                            FeatureHighlightItemLandscape(
                                highlight = highlight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Page indicators - horizontal layout for landscape
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { index, _ ->
                        PageIndicator(
                            isActive = index == currentPage,
                            modifier = Modifier.padding(horizontal = spacing.extraSmall)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(landscapeSpacing))

                // Navigation buttons - Skip and Next/Get Started (like GuidedBeanCreationScreen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
                ) {
                    // Skip button (replaces Previous button)
                    CoffeeSecondaryButton(
                        text = stringResource(R.string.button_skip),
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    )

                    // Next/Complete button
                    CoffeePrimaryButton(
                        text = if (currentPage == slides.size - 1) {
                            stringResource(R.string.button_get_started)
                        } else {
                            stringResource(R.string.button_next)
                        },
                        onClick = if (currentPage == slides.size - 1) {
                            onComplete
                        } else {
                            onNext
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Feature highlight item component for portrait mode
 */
@Composable
fun FeatureHighlightItem(
    highlight: FeatureHighlight,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    CoffeeCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Icon with background circle
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {}

                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                )
            }
        }
    }
}

/**
 * Feature highlight item component for landscape mode - more compact
 */
@Composable
fun FeatureHighlightItemLandscape(
    highlight: FeatureHighlight,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    CoffeeCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(landscapeSpacing),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            // Icon with background circle - smaller for landscape
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {}

                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2f,
                    modifier = Modifier.padding(top = spacing.extraSmall / 2)
                )
            }
        }
    }
}

/**
 * Enhanced page indicator component for the pager
 */
@Composable
fun PageIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val width = if (isActive) 24.dp else 8.dp
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .width(width)
            .height(8.dp),
        shape = CircleShape,
        color = color
    ) {}
}

/**
 * Preview for the introduction screen
 */
@Preview(showBackground = true)
@Composable
fun IntroductionScreenPreview() {
    CoffeeShotTimerTheme {
        IntroductionScreen(
            onComplete = {},
            onSkip = {}
        )
    }
}
