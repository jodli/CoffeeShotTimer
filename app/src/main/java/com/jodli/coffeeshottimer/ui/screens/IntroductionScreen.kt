package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeePrimaryButton
import com.jodli.coffeeshottimer.ui.components.CoffeeSecondaryButton
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.components.LoadingIndicator
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.landscapeSpacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

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
    
    // State for navigation operations and error handling
    var isNavigating by remember { mutableStateOf(false) }
    var navigationError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LandscapeContainer(
            portraitContent = {
                IntroductionPortraitLayout(
                    slides = slides,
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    isNavigating = isNavigating,
                    navigationError = navigationError,
                    onSkip = onSkip,
                    onComplete = onComplete,
                    onNavigationStart = { isNavigating = true },
                    onNavigationError = { error -> 
                        navigationError = error
                        isNavigating = false
                    },
                    onDismissError = {
                        navigationError = null
                        isNavigating = false
                    }
                )
            },
            landscapeContent = {
                IntroductionLandscapeLayout(
                    slides = slides,
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                    isNavigating = isNavigating,
                    navigationError = navigationError,
                    onSkip = onSkip,
                    onComplete = onComplete,
                    onNavigationStart = { isNavigating = true },
                    onNavigationError = { error -> 
                        navigationError = error
                        isNavigating = false
                    },
                    onDismissError = {
                        navigationError = null
                        isNavigating = false
                    }
                )
            }
        )
    }
}

/**
 * Portrait layout for the introduction screen - maintains existing functionality
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroductionPortraitLayout(
    slides: List<IntroSlide>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    isNavigating: Boolean,
    navigationError: String?,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onNavigationStart: () -> Unit,
    onNavigationError: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenPadding, vertical = spacing.small)
    ) {
        // Skip button with animation - consistent top padding following design guidelines
        AnimatedVisibility(
            visible = !isNavigating,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        try {
                            onNavigationStart()
                            onSkip()
                        } catch (e: Exception) {
                            onNavigationError("Failed to skip introduction: ${e.message}")
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.button_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Loading indicator during navigation
        if (isNavigating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.touchTarget),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    message = "Loading...",
                    modifier = Modifier.size(spacing.iconMedium)
                )
            }
        }

        // Error message display
        navigationError?.let { error ->
            IntroductionErrorCard(
                message = error,
                onRetry = onDismissError,
                onDismiss = onDismissError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small)
            )
        }

        // Walkthrough pager with enhanced animations - better space utilization
        WalkthroughPager(
            slides = slides,
            pagerState = pagerState,
            isNavigating = isNavigating,
            modifier = Modifier.weight(1f)
        )

        // Navigation buttons with error handling - reduced spacing
        IntroductionNavigationButtons(
            currentPage = pagerState.currentPage,
            totalPages = slides.size,
            isNavigating = isNavigating,
            onPrevious = {
                coroutineScope.launch {
                    try {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage - 1,
                            animationSpec = tween(durationMillis = 500)
                        )
                    } catch (e: CancellationException) {
                        // Ignore rapid-tap interruption; pager state still updates
                    } catch (e: Exception) {
                        onNavigationError("Failed to navigate to previous slide: ${e.message}")
                    }
                }
            },
            onNext = {
                coroutineScope.launch {
                    try {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = tween(durationMillis = 500)
                        )
                    } catch (e: CancellationException) {
                        // Ignore rapid-tap interruption; pager state still updates
                    } catch (e: Exception) {
                        onNavigationError("Failed to navigate to next slide: ${e.message}")
                    }
                }
            },
            onGetStarted = {
                try {
                    onNavigationStart()
                    onComplete()
                } catch (e: Exception) {
                    onNavigationError("Failed to complete introduction: ${e.message}")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.medium)
        )
    }
}

/**
 * Landscape layout for the introduction screen - optimized for horizontal space
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroductionLandscapeLayout(
    slides: List<IntroSlide>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    isNavigating: Boolean,
    navigationError: String?,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onNavigationStart: () -> Unit,
    onNavigationError: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = landscapeSpacing, vertical = spacing.small)
    ) {
        val maxWidth = maxWidth
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with skip button and loading/error states
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Error display in landscape - more compact
                navigationError?.let { error ->
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = spacing.medium)
                    )
                    
                    TextButton(onClick = onDismissError) {
                        Text(
                            text = stringResource(R.string.button_dismiss),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                // Loading indicator in landscape
                if (isNavigating) {
                    LoadingIndicator(
                        message = "Loading...",
                        modifier = Modifier.size(spacing.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(spacing.medium))
                }
                
                // Skip button
                AnimatedVisibility(
                    visible = !isNavigating,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    TextButton(
                        onClick = {
                            try {
                                onNavigationStart()
                                onSkip()
                            } catch (e: Exception) {
                                onNavigationError("Failed to skip introduction: ${e.message}")
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.button_skip),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Main content area - horizontal layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(landscapeSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Slide content (75% width)
                Box(
                    modifier = Modifier
                        .weight(0.75f)
                        .fillMaxSize()
                ) {
                    WalkthroughPagerLandscape(
                        slides = slides,
                        pagerState = pagerState,
                        isNavigating = isNavigating,
                        availableWidth = maxWidth * 0.75f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Right side: Navigation and indicators (25% width) - pushed to right edge
                Column(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxSize()
                        .padding(end = landscapeSpacing), // Push to right edge
                    horizontalAlignment = Alignment.CenterHorizontally, // Align to right edge
                    verticalArrangement = Arrangement.Center
                ) {
                    // Page indicators with enhanced spacing for landscape
                    AnimatedVisibility(
                        visible = !isNavigating,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.medium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Vertical page indicators for landscape
                            repeat(slides.size) { index ->
                                PageIndicatorLandscape(
                                    isActive = index == pagerState.currentPage,
                                    modifier = Modifier.size(width = 24.dp, height = 8.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(landscapeSpacing * 2))
                    
                    // Navigation buttons - stacked vertically for landscape
                    IntroductionNavigationButtonsLandscape(
                        currentPage = pagerState.currentPage,
                        totalPages = slides.size,
                        isNavigating = isNavigating,
                        onPrevious = {
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage - 1,
                                        animationSpec = tween(durationMillis = 500)
                                    )
                                } catch (e: CancellationException) {
                                    // Ignore rapid-tap interruption; pager state still updates
                                } catch (e: Exception) {
                                    onNavigationError("Failed to navigate to previous slide: ${e.message}")
                                }
                            }
                        },
                        onNext = {
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = tween(durationMillis = 500)
                                    )
                                } catch (e: CancellationException) {
                                    // Ignore rapid-tap interruption; pager state still updates
                                } catch (e: Exception) {
                                    onNavigationError("Failed to navigate to next slide: ${e.message}")
                                }
                            }
                        },
                        onGetStarted = {
                            try {
                                onNavigationStart()
                                onComplete()
                            } catch (e: Exception) {
                                onNavigationError("Failed to complete introduction: ${e.message}")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Landscape-optimized walkthrough pager component
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalkthroughPagerLandscape(
    slides: List<IntroSlide>,
    pagerState: PagerState,
    isNavigating: Boolean,
    availableWidth: Dp,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    AnimatedVisibility(
        modifier = modifier,
        visible = !isNavigating,
        enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
            animationSpec = tween(300),
            initialOffsetX = { it / 4 }
        ),
        exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
            animationSpec = tween(300),
            targetOffsetX = { -it / 4 }
        )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = landscapeSpacing
        ) { page ->
            IntroSlideContentLandscape(
                slide = slides[page],
                isActive = page == pagerState.currentPage,
                availableWidth = availableWidth,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Loading state during navigation
    if (isNavigating) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(
                message = "Preparing next step...",
                modifier = Modifier.size(spacing.iconLarge)
            )
        }
    }
}

/**
 * Individual slide content component optimized for landscape orientation
 */
@Composable
fun IntroSlideContentLandscape(
    slide: IntroSlide,
    isActive: Boolean,
    availableWidth: Dp,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    // Animate content when slide becomes active
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInHorizontally(
            animationSpec = tween(600, delayMillis = 200),
            initialOffsetX = { it / 8 }
        ),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = landscapeSpacing, vertical = landscapeSpacing)
        ) {
            val maxHeight = maxHeight
            
            // Horizontal layout for landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(landscapeSpacing * 2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Illustration (30% width)
                Column(
                    modifier = Modifier.weight(0.3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration with smaller size for landscape - enhanced with background circle
                    val iconSize = minOf(80.dp, maxHeight * 0.3f)
                    Box(
                        modifier = Modifier.size(iconSize + 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background circle for better visual impact
                        Surface(
                            modifier = Modifier.size(iconSize),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            tonalElevation = 2.dp
                        ) {}
                        
                        Icon(
                            imageVector = slide.illustration,
                            contentDescription = slide.title,
                            modifier = Modifier.size(iconSize * 0.7f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Right side: Content (70% width)
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
                ) {
                    // Title with landscape-appropriate sizing
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineSmall, // Smaller for landscape
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.1f
                    )

                    // Description with better sizing for landscape
                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyMedium, // More compact for landscape
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                    )

                    // Feature highlights with horizontal layout for landscape
                    if (slide.highlights.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(landscapeSpacing / 2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            slide.highlights.forEachIndexed { index, highlight ->
                                // Stagger the animation of each highlight
                                LaunchedEffect(isActive) {
                                    if (isActive) {
                                        kotlinx.coroutines.delay(300L + (index * 100L))
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = isActive,
                                    enter = fadeIn(
                                        animationSpec = tween(400, delayMillis = 300 + (index * 100))
                                    ) + slideInHorizontally(
                                        animationSpec = tween(400, delayMillis = 300 + (index * 100)),
                                        initialOffsetX = { it / 4 }
                                    )
                                ) {
                                    FeatureHighlightItemLandscape(
                                        highlight = highlight,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Feature highlight item component optimized for landscape
 */
@Composable
fun FeatureHighlightItemLandscape(
    highlight: FeatureHighlight,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()

    Surface(
        modifier = modifier.padding(vertical = spacing.extraSmall),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
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
 * Landscape-specific page indicator (vertical layout)
 */
@Composable
fun PageIndicatorLandscape(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val height = if (isActive) 24.dp else 8.dp
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    
    Surface(
        modifier = modifier
            .width(8.dp)
            .height(height),
        shape = CircleShape,
        color = color
    ) {}
}

/**
 * Navigation buttons for landscape orientation (vertical layout)
 */
@Composable
fun IntroductionNavigationButtonsLandscape(
    currentPage: Int,
    totalPages: Int,
    isNavigating: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val landscapeSpacing = spacing.landscapeSpacing()
    val isFirstPage = currentPage == 0
    val isLastPage = currentPage == totalPages - 1

    AnimatedVisibility(
        visible = !isNavigating,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(landscapeSpacing)
        ) {
            // Next/Get Started button (primary action on top)
            if (isLastPage) {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_get_started),
                    onClick = onGetStarted,
                    enabled = !isNavigating,
                    modifier = Modifier.width(140.dp),
                    fillMaxWidth = false
                )
            } else {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_next),
                    onClick = onNext,
                    enabled = !isNavigating,
                    modifier = Modifier.width(120.dp),
                    fillMaxWidth = false
                )
            }
            
            // Previous button (secondary action on bottom)
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_previous),
                onClick = onPrevious,
                enabled = !isFirstPage && !isNavigating,
                modifier = Modifier.width(120.dp),
                fillMaxWidth = false
            )
        }
    }
}

/**
 * Error card component for introduction screen navigation failures
 */
@Composable
fun IntroductionErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = spacing.elevationCard
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = "Navigation Error",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.button_dismiss),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(spacing.small))
                
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.text_retry),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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
    isNavigating: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal pager for slides with enhanced animations
        AnimatedVisibility(
            modifier = Modifier.weight(1f),
            visible = !isNavigating,
            enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                animationSpec = tween(300),
                initialOffsetX = { it / 4 }
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                animationSpec = tween(300),
                targetOffsetX = { -it / 4 }
            )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = spacing.medium
            ) { page ->
                IntroSlideContent(
                    slide = slides[page],
                    isActive = page == pagerState.currentPage,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Loading state during navigation
        if (isNavigating) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    message = "Preparing next step...",
                    modifier = Modifier.size(spacing.iconLarge)
                )
            }
        }

        // Page indicators with animation - reduced spacing
        AnimatedVisibility(
            visible = !isNavigating,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = spacing.small)
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
}

/**
 * Individual slide content component with enhanced animations
 */
@Composable
fun IntroSlideContent(
    slide: IntroSlide,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Animate content when slide becomes active
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInHorizontally(
            animationSpec = tween(600, delayMillis = 200),
            initialOffsetX = { it / 8 }
        ),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large, vertical = spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Illustration with entrance animation - enhanced with background circle
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle for better visual impact
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {}
                
                Icon(
                    imageVector = slide.illustration,
                    contentDescription = slide.title,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Content section with better spacing
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Title with improved typography - more prominent and better fitting
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight * 1.1f,
                    modifier = Modifier.padding(horizontal = spacing.small)
                )

                // Description with better styling
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f,
                    modifier = Modifier.padding(horizontal = spacing.large)
                )
            }

            // Feature highlights with staggered animations - increased spacing from text
            if (slide.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.large))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    slide.highlights.forEachIndexed { index, highlight ->
                        // Stagger the animation of each highlight
                        LaunchedEffect(isActive) {
                            if (isActive) {
                                kotlinx.coroutines.delay(300L + (index * 100L))
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = isActive,
                            enter = fadeIn(
                                animationSpec = tween(400, delayMillis = 300 + (index * 100))
                            ) + slideInHorizontally(
                                animationSpec = tween(400, delayMillis = 300 + (index * 100)),
                                initialOffsetX = { it / 4 }
                            )
                        ) {
                            FeatureHighlightItem(
                                highlight = highlight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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

    Surface(
        modifier = modifier.padding(vertical = spacing.extraSmall),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f,
                    modifier = Modifier.padding(top = spacing.extraSmall)
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
 * Navigation buttons for the introduction screen with loading states
 */
@Composable
fun IntroductionNavigationButtons(
    currentPage: Int,
    totalPages: Int,
    isNavigating: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val isFirstPage = currentPage == 0
    val isLastPage = currentPage == totalPages - 1

    AnimatedVisibility(
        visible = !isNavigating,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Row(
            modifier = modifier.padding(horizontal = spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button (rendered disabled on first page to maintain layout)
            CoffeeSecondaryButton(
                text = stringResource(R.string.button_previous),
                onClick = onPrevious,
                enabled = !isFirstPage && !isNavigating,
                modifier = Modifier.width(120.dp),
                fillMaxWidth = false
            )

            // Next/Get Started button
            if (isLastPage) {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_get_started),
                    onClick = onGetStarted,
                    enabled = !isNavigating,
                    modifier = Modifier.width(140.dp),
                    fillMaxWidth = false
                )
            } else {
                CoffeePrimaryButton(
                    text = stringResource(R.string.button_next),
                    onClick = onNext,
                    enabled = !isNavigating,
                    modifier = Modifier.width(120.dp),
                    fillMaxWidth = false
                )
            }
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
            ),
            isActive = true
        )
    }
}