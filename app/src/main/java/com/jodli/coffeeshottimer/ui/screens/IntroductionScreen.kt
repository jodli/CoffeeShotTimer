package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

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
 * Introduction screen that provides an interactive walkthrough of the app's main features
 * and flexible workflow options for new users.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroductionScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val slides = getIntroductionSlides()
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.screenPadding)
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.button_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Walkthrough pager
            WalkthroughPager(
                slides = slides,
                pagerState = pagerState,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(spacing.large))

            // Navigation buttons
            IntroductionNavigationButtons(
                currentPage = pagerState.currentPage,
                totalPages = slides.size,
                onPrevious = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onGetStarted = onComplete,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.medium))
        }
    }
}

/**
 * Walkthrough pager component that displays swipeable introduction slides
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalkthroughPager(
    slides: List<IntroSlide>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal pager for slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            IntroSlideContent(
                slide = slides[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                PageIndicator(
                    isActive = index == pagerState.currentPage,
                    modifier = Modifier.size(spacing.small)
                )
            }
        }
    }
}

/**
 * Individual slide content component
 */
@Composable
fun IntroSlideContent(
    slide: IntroSlide,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration
        Icon(
            imageVector = slide.illustration,
            contentDescription = null,
            modifier = Modifier.size(spacing.iconEmptyState * 1.5f),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Title
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        // Description
        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        )

        // Feature highlights (if any)
        if (slide.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                horizontalAlignment = Alignment.Start
            ) {
                slide.highlights.forEach { highlight ->
                    FeatureHighlightItem(
                        highlight = highlight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Feature highlight item component
 */
@Composable
fun FeatureHighlightItem(
    highlight: FeatureHighlight,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Icon(
            imageVector = highlight.icon,
            contentDescription = null,
            modifier = Modifier.size(spacing.iconMedium),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            Text(
                text = highlight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Page indicator component for the pager
 */
@Composable
fun PageIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .padding(2.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
        ) {}
    }
}

/**
 * Navigation buttons for the introduction screen
 */
@Composable
fun IntroductionNavigationButtons(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val isFirstPage = currentPage == 0
    val isLastPage = currentPage == totalPages - 1

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button (invisible on first page to maintain layout)
        if (isFirstPage) {
            Spacer(modifier = Modifier.width(spacing.buttonMaxWidth / 2))
        } else {
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_previous),
                onClick = onPrevious,
                modifier = Modifier.width(spacing.buttonMaxWidth / 2),
                fillMaxWidth = false
            )
        }

        // Next/Get Started button
        if (isLastPage) {
            CoffeePrimaryButton(
                text = stringResource(R.string.button_get_started),
                onClick = onGetStarted,
                modifier = Modifier.width(spacing.buttonMaxWidth / 2),
                fillMaxWidth = false
            )
        } else {
            CoffeePrimaryButton(
                text = stringResource(R.string.button_next),
                onClick = onNext,
                modifier = Modifier.width(spacing.buttonMaxWidth / 2),
                fillMaxWidth = false
            )
        }
    }
}

/**
 * Creates the list of introduction slides with content
 */
@Composable
private fun getIntroductionSlides(): List<IntroSlide> {
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
            illustration = Icons.Default.Settings
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
            illustration = Icons.Default.Coffee
        )
    )
}

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

@Preview(showBackground = true)
@Composable
fun IntroSlideContentPreview() {
    CoffeeShotTimerTheme {
        IntroSlideContent(
            slide = IntroSlide(
                title = stringResource(R.string.intro_welcome_title),
                description = stringResource(R.string.intro_welcome_description),
                illustration = Icons.Default.Coffee,
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
                    )
                )
            )
        )
    }
}