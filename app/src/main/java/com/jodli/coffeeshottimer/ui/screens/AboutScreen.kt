package com.jodli.coffeeshottimer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.components.DebugDialog
import com.jodli.coffeeshottimer.ui.components.DebugTapDetector
import com.jodli.coffeeshottimer.ui.components.LedgerLinkRow
import com.jodli.coffeeshottimer.ui.components.LedgerSection
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import com.jodli.coffeeshottimer.ui.viewmodel.DebugViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    debugViewModel: DebugViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val debugUiState by debugViewModel.uiState.collectAsState()

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_about),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding)
        ) {
            // The app's dial mark, name and version
            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_icon),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name_prod),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DebugTapDetector(
                        onDebugActivated = { debugViewModel.showDialog() }
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.text_version_format,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.text_about_app),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = stringResource(id = R.string.text_about_philosophy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            LedgerSection(title = stringResource(id = R.string.text_links)) {
                LedgerLinkRow(
                    label = stringResource(id = R.string.link_github_repo),
                    detail = stringResource(id = R.string.link_github_repo_description),
                    opensExternally = true,
                    onClick = { openUrl("https://github.com/jodli/CoffeeShotTimer") }
                )
                LedgerLinkRow(
                    label = stringResource(id = R.string.link_privacy_policy),
                    detail = stringResource(id = R.string.link_privacy_policy_description),
                    opensExternally = true,
                    onClick = { openUrl("https://jodli.github.io/CoffeeShotTimer/") }
                )
            }

            Text(
                text = stringResource(id = R.string.text_made_with_love),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }

    // Debug dialog (only available in debug builds)
    DebugDialog(
        isVisible = debugUiState.isDialogVisible,
        onDismiss = { debugViewModel.hideDialog() },
        onFillDatabase = { debugViewModel.fillDatabase() },
        onClearDatabase = { debugViewModel.clearDatabase() },
        onResetToNewUser = { debugViewModel.resetToNewUser() },
        onResetToExistingUserNoBeans = { debugViewModel.resetToExistingUserNoBeans() },
        onForceEquipmentSetup = { debugViewModel.forceEquipmentSetup() },
        isLoading = debugUiState.isLoading,
        operationResult = debugUiState.operationResult,
        showConfirmation = debugUiState.showConfirmation,
        onShowConfirmation = { debugViewModel.showConfirmation() },
        onHideConfirmation = { debugViewModel.hideConfirmation() }
    )
}
