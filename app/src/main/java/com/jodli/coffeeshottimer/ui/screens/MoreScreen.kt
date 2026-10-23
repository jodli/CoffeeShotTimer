package com.jodli.coffeeshottimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.LedgerLinkRow
import com.jodli.coffeeshottimer.ui.components.LedgerSection
import com.jodli.coffeeshottimer.ui.components.formatStepSize
import com.jodli.coffeeshottimer.ui.theme.LocalIsLandscape
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.MoreUiState
import com.jodli.coffeeshottimer.ui.viewmodel.MoreViewModel
import kotlin.math.roundToInt

@Composable
fun MoreScreen(
    onNavigateToGrinderSettings: () -> Unit,
    onNavigateToBasketSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    if (LocalIsLandscape.current) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            EquipmentSection(
                uiState = uiState,
                onGrinder = onNavigateToGrinderSettings,
                onBasket = onNavigateToBasketSettings,
                modifier = Modifier.weight(1f)
            )
            AppSection(
                onAbout = onNavigateToAbout,
                onFeedback = { sendFeedback(context) },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding)
        ) {
            EquipmentSection(
                uiState = uiState,
                onGrinder = onNavigateToGrinderSettings,
                onBasket = onNavigateToBasketSettings
            )
            AppSection(
                onAbout = onNavigateToAbout,
                onFeedback = { sendFeedback(context) }
            )
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}

@Composable
private fun EquipmentSection(
    uiState: MoreUiState,
    onGrinder: () -> Unit,
    onBasket: () -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerSection(title = stringResource(R.string.label_equipment), modifier = modifier) {
        LedgerLinkRow(
            label = stringResource(R.string.title_equipment_settings),
            detail = uiState.grinder?.let {
                stringResource(
                    R.string.format_grinder_summary,
                    it.scaleMin,
                    it.scaleMax,
                    formatStepSize(it.stepSize)
                )
            },
            onClick = onGrinder
        )
        LedgerLinkRow(
            label = stringResource(R.string.title_basket_settings),
            detail = uiState.basket?.let {
                stringResource(
                    R.string.format_basket_summary,
                    it.coffeeInMin.roundToInt(),
                    it.coffeeInMax.roundToInt(),
                    it.coffeeOutMin.roundToInt(),
                    it.coffeeOutMax.roundToInt()
                )
            },
            onClick = onBasket
        )
    }
}

@Composable
private fun AppSection(
    onAbout: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerSection(title = stringResource(R.string.label_app), modifier = modifier) {
        LedgerLinkRow(
            label = stringResource(R.string.title_about),
            onClick = onAbout
        )
        LedgerLinkRow(
            label = stringResource(R.string.button_send_feedback),
            onClick = onFeedback,
            opensExternally = true
        )
    }
}

private fun sendFeedback(context: Context) {
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
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.button_send_feedback))
        )
    } catch (_: ActivityNotFoundException) {
        // If no email app is available, fall back to opening a generic mailto URL without body
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$recipient"))
        context.startActivity(fallback)
    }
}
