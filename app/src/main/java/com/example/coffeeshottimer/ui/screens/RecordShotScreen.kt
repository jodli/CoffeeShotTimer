package com.example.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.ui.components.*
import com.example.coffeeshottimer.ui.theme.LocalSpacing
import com.example.coffeeshottimer.ui.viewmodel.ShotRecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordShotScreen(
    onNavigateToBeanManagement: () -> Unit = {},
    viewModel: ShotRecordingViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()
    
    // State from ViewModel
    val activeBeans by viewModel.activeBeans.collectAsStateWithLifecycle()
    val selectedBean by viewModel.selectedBean.collectAsStateWithLifecycle()
    val coffeeWeightIn by viewModel.coffeeWeightIn.collectAsStateWithLifecycle()
    val coffeeWeightOut by viewModel.coffeeWeightOut.collectAsStateWithLifecycle()
    val grinderSetting by viewModel.grinderSetting.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val coffeeWeightInError by viewModel.coffeeWeightInError.collectAsStateWithLifecycle()
    val coffeeWeightOutError by viewModel.coffeeWeightOutError.collectAsStateWithLifecycle()
    val grinderSettingError by viewModel.grinderSettingError.collectAsStateWithLifecycle()
    val brewRatio by viewModel.brewRatio.collectAsStateWithLifecycle()
    val formattedBrewRatio by viewModel.formattedBrewRatio.collectAsStateWithLifecycle()
    val isOptimalBrewRatio by viewModel.isOptimalBrewRatio.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val isDraftSaved by viewModel.isDraftSaved.collectAsStateWithLifecycle()
    val suggestedGrinderSetting by viewModel.suggestedGrinderSetting.collectAsStateWithLifecycle()
    
    // Local UI state
    var showBeanSelector by remember { mutableStateOf(false) }
    
    // Convert timer state for UI
    val uiTimerState = when {
        timerState.isRunning -> TimerState.RUNNING
        timerState.elapsedTimeSeconds > 0 -> TimerState.PAUSED
        else -> TimerState.STOPPED
    }
    val currentTime = (timerState.elapsedTimeSeconds * 1000).toLong()
    val targetTime: Long? = null // Can be set for target extraction time if needed
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Header
        SectionHeader(
            title = "Record Shot",
            subtitle = "Track your espresso extraction"
        )
        
        // Bean Selection with navigation to bean management
        BeanSelectionCard(
            selectedBean = selectedBean,
            onBeanSelect = { showBeanSelector = true },
            onManageBeans = onNavigateToBeanManagement,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Timer Section
        TimerSection(
            currentTime = currentTime,
            targetTime = targetTime,
            timerState = uiTimerState,
            onStartPause = {
                if (timerState.isRunning) {
                    viewModel.stopTimer()
                } else {
                    viewModel.startTimer()
                }
            },
            onStop = {
                viewModel.stopTimer()
            },
            onReset = {
                viewModel.resetTimer()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Weight Inputs
        WeightInputsSection(
            coffeeWeightIn = coffeeWeightIn,
            onCoffeeWeightInChange = viewModel::updateCoffeeWeightIn,
            coffeeWeightInError = coffeeWeightInError,
            coffeeWeightOut = coffeeWeightOut,
            onCoffeeWeightOutChange = viewModel::updateCoffeeWeightOut,
            coffeeWeightOutError = coffeeWeightOutError,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Brew Ratio Display
        BrewRatioCard(
            brewRatio = brewRatio,
            formattedBrewRatio = formattedBrewRatio,
            isOptimal = isOptimalBrewRatio,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Grinder Setting with suggestions
        GrinderSettingSection(
            grinderSetting = grinderSetting,
            onGrinderSettingChange = viewModel::updateGrinderSetting,
            grinderSettingError = grinderSettingError,
            suggestedSetting = suggestedGrinderSetting,
            onUseSuggestion = { suggestion ->
                viewModel.updateGrinderSetting(suggestion)
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Notes (Optional)
        NotesSection(
            notes = notes,
            onNotesChange = viewModel::updateNotes,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Save Button
        SaveShotButton(
            enabled = isFormValid,
            isLoading = isLoading,
            onClick = {
                viewModel.recordShot()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Success message
        successMessage?.let { success ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = success,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = { viewModel.clearSuccessMessage() }
                    ) {
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(spacing.medium)
                )
            }
        }
        
        // Draft status indicator
        if (isDraftSaved) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Draft saved automatically",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(spacing.small)
                )
            }
        }
        
        // Bottom spacing for navigation bar
        Spacer(modifier = Modifier.height(spacing.large))
    }
    
    // Bean Selection Bottom Sheet
    if (showBeanSelector) {
        BeanSelectorBottomSheet(
            beans = activeBeans,
            selectedBean = selectedBean,
            onBeanSelected = { bean ->
                viewModel.selectBean(bean)
                showBeanSelector = false
            },
            onManageBeans = {
                showBeanSelector = false
                onNavigateToBeanManagement()
            },
            onDismiss = { showBeanSelector = false }
        )
    }
}

@Composable
private fun BeanSelectionCard(
    selectedBean: Bean?,
    onBeanSelect: () -> Unit,
    onManageBeans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(
        onClick = onBeanSelect,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Bean",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = onManageBeans,
                        contentPadding = PaddingValues(horizontal = spacing.small, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Manage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = selectedBean?.name ?: "Tap to select bean",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedBean != null) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedBean != null) FontWeight.Medium else FontWeight.Normal
                )
                
                selectedBean?.let { bean ->
                    val daysSinceRoast = bean.daysSinceRoast()
                    Text(
                        text = "$daysSinceRoast days since roast",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bean.isFresh()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select bean",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerSection(
    currentTime: Long,
    targetTime: Long?,
    timerState: TimerState,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            Text(
                text = "Extraction Timer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Large timer display
            CircularTimer(
                currentTime = currentTime,
                targetTime = targetTime,
                isRunning = timerState == TimerState.RUNNING,
                modifier = Modifier.size(200.dp)
            )
            
            // Timer controls
            TimerControls(
                isRunning = timerState == TimerState.RUNNING,
                onStartPause = onStartPause,
                onStop = onStop,
                onReset = onReset,
                showReset = currentTime > 0L
            )
        }
    }
}

@Composable
private fun WeightInputsSection(
    coffeeWeightIn: String,
    onCoffeeWeightInChange: (String) -> Unit,
    coffeeWeightInError: String?,
    coffeeWeightOut: String,
    onCoffeeWeightOutChange: (String) -> Unit,
    coffeeWeightOutError: String?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Text(
            text = "Weight Measurements",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Coffee Weight In
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = coffeeWeightIn,
                    onValueChange = onCoffeeWeightInChange,
                    label = { Text("Coffee In (g)") },
                    placeholder = { Text("18.0") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = coffeeWeightInError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                coffeeWeightInError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = spacing.medium, top = spacing.extraSmall)
                    )
                }
            }
            
            // Coffee Weight Out
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = coffeeWeightOut,
                    onValueChange = onCoffeeWeightOutChange,
                    label = { Text("Coffee Out (g)") },
                    placeholder = { Text("36.0") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = coffeeWeightOutError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                coffeeWeightOutError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = spacing.medium, top = spacing.extraSmall)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrewRatioCard(
    brewRatio: Double?,
    formattedBrewRatio: String?,
    isOptimal: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brew Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = formattedBrewRatio ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOptimal) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        
        brewRatio?.let {
            val status = when {
                it < 1.5 -> "Strong extraction"
                it > 3.0 -> "Weak extraction"
                else -> "Typical espresso range"
            }
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GrinderSettingSection(
    grinderSetting: String,
    onGrinderSettingChange: (String) -> Unit,
    grinderSettingError: String?,
    suggestedSetting: String?,
    onUseSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grinder Setting",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Show suggestion button if available and different from current setting
            if (suggestedSetting != null && suggestedSetting != grinderSetting && grinderSetting.isEmpty()) {
                TextButton(
                    onClick = { onUseSuggestion(suggestedSetting) },
                    contentPadding = PaddingValues(horizontal = spacing.small, vertical = 2.dp)
                ) {
                    Text(
                        text = "Use: $suggestedSetting",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        OutlinedTextField(
            value = grinderSetting,
            onValueChange = onGrinderSettingChange,
            label = { Text("Grinder Setting") },
            placeholder = { Text(suggestedSetting ?: "e.g., 15, Fine, 2.5") },
            isError = grinderSettingError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Show suggestion hint if available
        if (suggestedSetting != null && grinderSetting.isEmpty()) {
            Text(
                text = "Suggested based on last use with this bean: $suggestedSetting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = spacing.medium, top = spacing.extraSmall)
            )
        }
        
        grinderSettingError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = spacing.medium, top = spacing.extraSmall)
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Text(
            text = "Notes (Optional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            placeholder = { Text("Tasting notes, adjustments, etc.") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SaveShotButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoffeePrimaryButton(
        text = if (isLoading) "Saving..." else "Save Shot",
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeanSelectorBottomSheet(
    beans: List<Bean>,
    selectedBean: Bean?,
    onBeanSelected: (Bean) -> Unit,
    onManageBeans: () -> Unit,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    val spacing = LocalSpacing.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Bean",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                TextButton(
                    onClick = onManageBeans
                ) {
                    Text(
                        text = "Manage Beans",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            if (beans.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Home,
                    title = "No Beans Available",
                    description = "Add some beans in Bean Management to get started",
                    modifier = Modifier.padding(spacing.large)
                )
            } else {
                beans.forEach { bean ->
                    BeanCard(
                        bean = bean,
                        onEdit = { /* Not needed in selector */ },
                        onSelect = { onBeanSelected(bean) },
                        isSelected = bean.id == selectedBean?.id,
                        showActions = false,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}



enum class TimerState {
    STOPPED, RUNNING, PAUSED
}