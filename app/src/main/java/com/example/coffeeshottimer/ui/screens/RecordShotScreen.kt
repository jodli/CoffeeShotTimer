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
    viewModel: ShotRecordingViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()
    
    // State from ViewModel
    val activeBeans by viewModel.activeBeans.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    // Local UI state
    var selectedBean by remember { mutableStateOf<Bean?>(null) }
    var showBeanSelector by remember { mutableStateOf(false) }
    var coffeeWeightIn by remember { mutableStateOf("") }
    var coffeeWeightOut by remember { mutableStateOf("") }
    var grinderSetting by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Timer state
    var timerState by remember { mutableStateOf(TimerState.STOPPED) }
    var currentTime by remember { mutableStateOf(0L) }
    var targetTime by remember { mutableStateOf<Long?>(null) }
    
    // Validation state
    var coffeeWeightInError by remember { mutableStateOf<String?>(null) }
    var coffeeWeightOutError by remember { mutableStateOf<String?>(null) }
    var grinderSettingError by remember { mutableStateOf<String?>(null) }
    
    // Auto-select first bean if available and none selected
    LaunchedEffect(activeBeans) {
        if (selectedBean == null && activeBeans.isNotEmpty()) {
            selectedBean = activeBeans.first()
            // Pre-fill grinder setting if available
            selectedBean?.lastGrinderSetting?.let { setting ->
                grinderSetting = setting
            }
        }
    }
    
    // Timer effect
    LaunchedEffect(timerState) {
        if (timerState == TimerState.RUNNING) {
            while (timerState == TimerState.RUNNING) {
                kotlinx.coroutines.delay(100L)
                currentTime += 100L
            }
        }
    }
    
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
        
        // Bean Selection
        BeanSelectionCard(
            selectedBean = selectedBean,
            onBeanSelect = { showBeanSelector = true },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Timer Section
        TimerSection(
            currentTime = currentTime,
            targetTime = targetTime,
            timerState = timerState,
            onStartPause = {
                timerState = when (timerState) {
                    TimerState.STOPPED, TimerState.PAUSED -> TimerState.RUNNING
                    TimerState.RUNNING -> TimerState.PAUSED
                }
            },
            onStop = {
                timerState = TimerState.STOPPED
                currentTime = 0L
            },
            onReset = {
                timerState = TimerState.STOPPED
                currentTime = 0L
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Weight Inputs
        WeightInputsSection(
            coffeeWeightIn = coffeeWeightIn,
            onCoffeeWeightInChange = { 
                coffeeWeightIn = it
                coffeeWeightInError = validateWeight(it, "Coffee input weight", 0.1, 50.0)
            },
            coffeeWeightInError = coffeeWeightInError,
            coffeeWeightOut = coffeeWeightOut,
            onCoffeeWeightOutChange = { 
                coffeeWeightOut = it
                coffeeWeightOutError = validateWeight(it, "Coffee output weight", 0.1, 100.0)
            },
            coffeeWeightOutError = coffeeWeightOutError,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Brew Ratio Display
        BrewRatioCard(
            coffeeWeightIn = coffeeWeightIn.toDoubleOrNull(),
            coffeeWeightOut = coffeeWeightOut.toDoubleOrNull(),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Grinder Setting
        GrinderSettingSection(
            grinderSetting = grinderSetting,
            onGrinderSettingChange = { 
                grinderSetting = it
                grinderSettingError = validateGrinderSetting(it)
            },
            grinderSettingError = grinderSettingError,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Notes (Optional)
        NotesSection(
            notes = notes,
            onNotesChange = { notes = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Save Button
        SaveShotButton(
            enabled = isFormValid(
                selectedBean = selectedBean,
                coffeeWeightIn = coffeeWeightIn,
                coffeeWeightOut = coffeeWeightOut,
                grinderSetting = grinderSetting,
                currentTime = currentTime,
                coffeeWeightInError = coffeeWeightInError,
                coffeeWeightOutError = coffeeWeightOutError,
                grinderSettingError = grinderSettingError
            ),
            isLoading = isLoading,
            onClick = {
                // TODO: Implement save functionality in next task
            },
            modifier = Modifier.fillMaxWidth()
        )
        
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
        
        // Bottom spacing for navigation bar
        Spacer(modifier = Modifier.height(spacing.large))
    }
    
    // Bean Selection Bottom Sheet
    if (showBeanSelector) {
        BeanSelectorBottomSheet(
            beans = activeBeans,
            selectedBean = selectedBean,
            onBeanSelected = { bean ->
                selectedBean = bean
                // Pre-fill grinder setting if available
                bean.lastGrinderSetting?.let { setting ->
                    grinderSetting = setting
                }
                showBeanSelector = false
            },
            onDismiss = { showBeanSelector = false }
        )
    }
}

@Composable
private fun BeanSelectionCard(
    selectedBean: Bean?,
    onBeanSelect: () -> Unit,
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
                Text(
                    text = "Selected Bean",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
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
    coffeeWeightIn: Double?,
    coffeeWeightOut: Double?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val ratio = if (coffeeWeightIn != null && coffeeWeightOut != null && coffeeWeightIn > 0) {
        coffeeWeightOut / coffeeWeightIn
    } else null
    
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
                text = ratio?.let { "1:${String.format("%.1f", it)}" } ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (ratio != null && ratio in 1.5..3.0) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        
        ratio?.let {
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
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    CoffeeCard(modifier = modifier) {
        Text(
            text = "Grinder Setting",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        OutlinedTextField(
            value = grinderSetting,
            onValueChange = onGrinderSettingChange,
            label = { Text("Grinder Setting") },
            placeholder = { Text("e.g., 15, Fine, 2.5") },
            isError = grinderSettingError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
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
            Text(
                text = "Select Bean",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = spacing.medium)
            )
            
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

// Helper functions
private fun validateWeight(value: String, fieldName: String, min: Double, max: Double): String? {
    if (value.isBlank()) return "$fieldName is required"
    
    val weight = value.toDoubleOrNull()
    return when {
        weight == null -> "Please enter a valid number"
        weight < min -> "$fieldName must be at least ${min}g"
        weight > max -> "$fieldName cannot exceed ${max}g"
        else -> null
    }
}

private fun validateGrinderSetting(value: String): String? {
    return when {
        value.isBlank() -> "Grinder setting is required"
        value.length > 50 -> "Grinder setting cannot exceed 50 characters"
        else -> null
    }
}

private fun isFormValid(
    selectedBean: Bean?,
    coffeeWeightIn: String,
    coffeeWeightOut: String,
    grinderSetting: String,
    currentTime: Long,
    coffeeWeightInError: String?,
    coffeeWeightOutError: String?,
    grinderSettingError: String?
): Boolean {
    return selectedBean != null &&
            coffeeWeightIn.isNotBlank() &&
            coffeeWeightOut.isNotBlank() &&
            grinderSetting.isNotBlank() &&
            currentTime > 0L &&
            coffeeWeightInError == null &&
            coffeeWeightOutError == null &&
            grinderSettingError == null
}

enum class TimerState {
    STOPPED, RUNNING, PAUSED
}