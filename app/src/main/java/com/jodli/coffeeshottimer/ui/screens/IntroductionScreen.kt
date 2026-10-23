package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.ChartShot
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.IconTextRow
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.ShotChart
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing

/**
 * Data model for introduction slides
 */
private data class IntroSlide(
    val title: String,
    val description: String,
    val highlights: List<FeatureHighlight> = emptyList()
)

/**
 * Data model for feature highlights within slides
 */
private data class FeatureHighlight(
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
            description = stringResource(R.string.intro_welcome_description)
        ),

        // Main screens overview slide
        IntroSlide(
            title = stringResource(R.string.intro_main_screens_title),
            description = stringResource(R.string.intro_main_screens_description),
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
            description = stringResource(R.string.intro_flexible_workflow_description)
        ),

        // Timer usage slide
        IntroSlide(
            title = stringResource(R.string.intro_timer_usage_title),
            description = stringResource(R.string.intro_timer_usage_description)
        ),

        // Get started slide
        IntroSlide(
            title = stringResource(R.string.intro_get_started_title),
            description = stringResource(R.string.intro_get_started_description)
        )
    )
}

/**
 * Main introduction screen composable that provides onboarding information.
 * Uses LandscapeContainer for responsive design.
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
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            },
            landscapeContent = {
                IntroductionLandscapeLayout(
                    slides = slides,
                    currentPage = currentPage,
                    onNext = onNext,
                    onComplete = onComplete,
                    onSkip = onSkip
                )
            }
        )
    }
}

@Composable
private fun IntroductionPortraitLayout(
    slides: List<IntroSlide>,
    currentPage: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = LocalSpacing.current
    val currentSlide = slides[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Scrollable slide content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding)
        ) {
            if (currentPage == 0) {
                IntroDialChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
                Text(
                    text = stringResource(R.string.intro_chart_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = currentSlide.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = currentSlide.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            currentSlide.highlights.forEachIndexed { index, highlight ->
                IconTextRow(
                    icon = highlight.icon,
                    title = highlight.title,
                    description = highlight.description,
                    modifier = if (index == 0) {
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))
        }

        // Page indicators
        PageIndicatorRow(currentPage = currentPage, pageCount = slides.size)

        // Navigation buttons - Skip and Next/Get Started
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_skip),
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            )

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

@Composable
private fun IntroductionLandscapeLayout(
    slides: List<IntroSlide>,
    currentPage: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    val currentSlide = slides[currentPage]

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = landscapeSpacing, vertical = spacing.small)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: chart or title (30% width)
            Column(
                modifier = Modifier.weight(0.3f),
                verticalArrangement = Arrangement.Center
            ) {
                if (currentPage == 0) {
                    IntroDialChart(modifier = Modifier.fillMaxWidth())
                } else {
                    Text(
                        text = currentSlide.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right side: Content and actions (70% width)
            Column(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
            ) {
                if (currentPage == 0) {
                    Text(
                        text = currentSlide.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = currentSlide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                currentSlide.highlights.forEach { highlight ->
                    IconTextRow(
                        icon = highlight.icon,
                        title = highlight.title,
                        description = highlight.description,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PageIndicatorRow(currentPage = currentPage, pageCount = slides.size)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(landscapeSpacing)
                ) {
                    CoffeeSecondaryButton(
                        text = stringResource(R.string.button_skip),
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    )

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

/** Sample chart for the welcome slide: three shots drifting onto the dial. */
@Composable
private fun IntroDialChart(modifier: Modifier = Modifier) {
    val shots = remember {
        listOf(
            ChartShot(seconds = 19, ratio = 1.6, number = 1),
            ChartShot(seconds = 23, ratio = 1.9, number = 2),
            ChartShot(seconds = 27, ratio = 2.2, number = 3)
        )
    }
    ShotChart(shots = shots, connect = true, modifier = modifier.height(220.dp))
}

@Composable
private fun PageIndicatorRow(currentPage: Int, pageCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            if (index == currentPage) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}
