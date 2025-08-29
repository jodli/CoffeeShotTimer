package com.jodli.coffeeshottimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.CoffeeCard
import com.jodli.coffeeshottimer.ui.components.LandscapeContainer
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToGrinderSettings: () -> Unit,
    onNavigateToBasketSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    LandscapeContainer(
        modifier = modifier.fillMaxSize(),
        portraitContent = {
            MoreScreenContent(
                onNavigateToGrinderSettings = onNavigateToGrinderSettings,
                onNavigateToBasketSettings = onNavigateToBasketSettings,
                onNavigateToAbout = onNavigateToAbout,
                spacing = spacing,
                context = context
            )
        },
        landscapeContent = {
            MoreScreenContent(
                onNavigateToGrinderSettings = onNavigateToGrinderSettings,
                onNavigateToBasketSettings = onNavigateToBasketSettings,
                onNavigateToAbout = onNavigateToAbout,
                spacing = spacing,
                context = context
            )
        }
    )
}

@Composable
private fun MoreScreenContent(
    onNavigateToGrinderSettings: () -> Unit,
    onNavigateToBasketSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    spacing: com.jodli.coffeeshottimer.ui.theme.Spacing,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Grinder Settings
        CoffeeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToGrinderSettings
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.cardPadding, vertical = spacing.small)
                    .height(spacing.touchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Icon(Icons.Filled.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = stringResource(id = R.string.title_equipment_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Basket Settings (Portafilter Configuration)
        CoffeeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToBasketSettings
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.cardPadding, vertical = spacing.small)
                    .height(spacing.touchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Icon(Icons.Filled.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = stringResource(id = R.string.title_basket_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // About (navigate to dedicated screen)
        CoffeeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToAbout
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.cardPadding, vertical = spacing.small)
                    .height(spacing.touchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = stringResource(id = R.string.title_about),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Send Feedback (email)
        CoffeeCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val recipient = "jobst87+coffeeshottimer@gmail.com"
                val subject = context.getString(R.string.feedback_email_subject)
                val body = buildString {
                    append("\n\n")
                    append("—\n")
                    append("App: Coffee Shot Timer\n")
                    append("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                    append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
                }
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    // Use an empty mailto: so Gmail and others appear, and set recipient via EXTRA_EMAIL
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                try {
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.button_send_feedback)))
                } catch (_: ActivityNotFoundException) {
                    // If no email app is available, fall back to opening a generic mailto URL without body
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$recipient"))
                    context.startActivity(fallback)
                }
            }
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.cardPadding, vertical = spacing.small)
                    .height(spacing.touchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(id = R.string.button_send_feedback),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom spacing for navigation clearance
        Spacer(modifier = Modifier.height(spacing.large))
    }
}

