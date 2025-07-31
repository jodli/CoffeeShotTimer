package com.jodli.coffeeshottimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.ui.theme.LocalSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ShotHistoryFilterDialog(
    currentFilter: ShotHistoryFilter,
    availableBeans: List<Bean>,
    onApplyFilter: (ShotHistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current

    // Filter state
    var selectedBeanId by remember { mutableStateOf(currentFilter.beanId) }
    var startDate by remember { mutableStateOf(currentFilter.startDate?.toLocalDate()) }
    var endDate by remember { mutableStateOf(currentFilter.endDate?.toLocalDate()) }
    var grinderSetting by remember { mutableStateOf(currentFilter.grinderSetting ?: "") }
    var minBrewRatio by remember { mutableStateOf(currentFilter.minBrewRatio?.toString() ?: "") }
    var maxBrewRatio by remember { mutableStateOf(currentFilter.maxBrewRatio?.toString() ?: "") }
    var minExtractionTime by remember {
        mutableStateOf(
            currentFilter.minExtractionTime?.toString() ?: ""
        )
    }
    var maxExtractionTime by remember {
        mutableStateOf(
            currentFilter.maxExtractionTime?.toString() ?: ""
        )
    }
    var onlyOptimalTime by remember {
        mutableStateOf(
            currentFilter.onlyOptimalExtractionTime ?: false
        )
    }
    var onlyTypicalRatio by remember { mutableStateOf(currentFilter.onlyTypicalBrewRatio ?: false) }

    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.large)
            ) {
                // Header
                CardHeader(
                    icon = Icons.Default.FilterList,
                    title = "Filter Shots",
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    // Bean filter
                    FilterSection(title = "Coffee Bean") {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedBeanId == null,
                                        onClick = { selectedBeanId = null },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBeanId == null,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text("All beans")
                            }

                            availableBeans.forEach { bean ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedBeanId == bean.id,
                                            onClick = { selectedBeanId = bean.id },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBeanId == bean.id,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(spacing.small))
                                    Column {
                                        Text(bean.name)
                                        Text(
                                            text = "${bean.daysSinceRoast()} days since roast",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Date range filter
                    FilterSection(title = "Date Range") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            // Start date
                            OutlinedButton(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(spacing.extraSmall))
                                Text(
                                    text = startDate?.format(DateTimeFormatter.ofPattern("MMM dd"))
                                        ?: "Start Date"
                                )
                            }

                            // End date
                            OutlinedButton(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(spacing.extraSmall))
                                Text(
                                    text = endDate?.format(DateTimeFormatter.ofPattern("MMM dd"))
                                        ?: "End Date"
                                )
                            }
                        }

                        if (startDate != null || endDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.small)
                            ) {
                                if (startDate != null) {
                                    TextButton(
                                        onClick = { startDate = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear Start")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (endDate != null) {
                                    TextButton(
                                        onClick = { endDate = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear End")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Grinder setting filter
                    FilterSection(title = "Grinder Setting") {
                        CoffeeTextField(
                            value = grinderSetting,
                            onValueChange = { grinderSetting = it },
                            label = "Grinder Setting",
                            placeholder = "e.g., 15, Fine, Medium-Fine"
                        )
                    }

                    // Brew ratio filter
                    FilterSection(title = "Brew Ratio Range") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            CoffeeTextField(
                                value = minBrewRatio,
                                onValueChange = { minBrewRatio = it },
                                label = "Min Ratio",
                                placeholder = "1.5",
                                modifier = Modifier.weight(1f)
                            )

                            CoffeeTextField(
                                value = maxBrewRatio,
                                onValueChange = { maxBrewRatio = it },
                                label = "Max Ratio",
                                placeholder = "3.0",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Extraction time filter
                    FilterSection(title = "Extraction Time Range (seconds)") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            CoffeeTextField(
                                value = minExtractionTime,
                                onValueChange = { minExtractionTime = it },
                                label = "Min Time",
                                placeholder = "20",
                                modifier = Modifier.weight(1f)
                            )

                            CoffeeTextField(
                                value = maxExtractionTime,
                                onValueChange = { maxExtractionTime = it },
                                label = "Max Time",
                                placeholder = "35",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quality filters
                    FilterSection(title = "Quality Filters") {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.extraSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = onlyOptimalTime,
                                    onCheckedChange = { onlyOptimalTime = it }
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                Column {
                                    Text("Optimal extraction time only")
                                    Text(
                                        text = "25-30 seconds",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.extraSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = onlyTypicalRatio,
                                    onCheckedChange = { onlyTypicalRatio = it }
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                Column {
                                    Text("Typical brew ratio only")
                                    Text(
                                        text = "1:1.5 to 1:3.0",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    CoffeeSecondaryButton(
                        text = "Clear All",
                        onClick = {
                            selectedBeanId = null
                            startDate = null
                            endDate = null
                            grinderSetting = ""
                            minBrewRatio = ""
                            maxBrewRatio = ""
                            minExtractionTime = ""
                            maxExtractionTime = ""
                            onlyOptimalTime = false
                            onlyTypicalRatio = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CoffeePrimaryButton(
                        text = "Apply Filters",
                        onClick = {
                            val filter = ShotHistoryFilter(
                                beanId = selectedBeanId,
                                startDate = startDate?.atStartOfDay(),
                                endDate = endDate?.atTime(23, 59, 59),
                                grinderSetting = grinderSetting.takeIf { it.isNotBlank() },
                                minBrewRatio = minBrewRatio.toDoubleOrNull(),
                                maxBrewRatio = maxBrewRatio.toDoubleOrNull(),
                                minExtractionTime = minExtractionTime.toIntOrNull(),
                                maxExtractionTime = maxExtractionTime.toIntOrNull(),
                                onlyOptimalExtractionTime = onlyOptimalTime.takeIf { it },
                                onlyTypicalBrewRatio = onlyTypicalRatio.takeIf { it }
                            )
                            onApplyFilter(filter)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            initialDate = startDate
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            initialDate = endDate
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.small))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toEpochDay()?.times(24 * 60 * 60 * 1000)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}