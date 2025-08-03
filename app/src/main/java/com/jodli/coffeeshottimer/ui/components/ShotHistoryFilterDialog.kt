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
import androidx.compose.ui.res.stringResource
import com.jodli.coffeeshottimer.R
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
            elevation = CardDefaults.cardElevation(defaultElevation = spacing.elevationDialog)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.large)
            ) {
                // Header
                CardHeader(
                    icon = Icons.Default.FilterList,
                    title = stringResource(R.string.title_filter_shots),
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cd_close)
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
                    FilterSection(title = stringResource(R.string.text_coffee_bean)) {
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
                                Text(stringResource(R.string.text_all_beans))
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
                                            text = stringResource(R.string.format_days_since_roast_filter, bean.daysSinceRoast()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Date range filter
                    FilterSection(title = stringResource(R.string.text_date_range)) {
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
                                    modifier = Modifier.size(spacing.iconSmall)
                                )
                                Spacer(modifier = Modifier.width(spacing.extraSmall))
                                Text(
                                    text = startDate?.format(DateTimeFormatter.ofPattern("MMM dd"))
                                        ?: stringResource(R.string.text_start_date)
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
                                    modifier = Modifier.size(spacing.iconSmall)
                                )
                                Spacer(modifier = Modifier.width(spacing.extraSmall))
                                Text(
                                    text = endDate?.format(DateTimeFormatter.ofPattern("MMM dd"))
                                        ?: stringResource(R.string.text_end_date)
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
                                        Text(stringResource(R.string.button_clear_start))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (endDate != null) {
                                    TextButton(
                                        onClick = { endDate = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.button_clear_end))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Grinder setting filter
                    FilterSection(title = stringResource(R.string.text_grinder_setting)) {
                        CoffeeTextField(
                            value = grinderSetting,
                            onValueChange = { grinderSetting = it },
                            label = stringResource(R.string.label_grinder_setting),
                            placeholder = stringResource(R.string.placeholder_grinder_setting)
                        )
                    }

                    // Brew ratio filter
                    FilterSection(title = stringResource(R.string.text_brew_ratio_range)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            CoffeeTextField(
                                value = minBrewRatio,
                                onValueChange = { minBrewRatio = it },
                                label = stringResource(R.string.label_min_ratio),
                                placeholder = stringResource(R.string.placeholder_min_ratio),
                                modifier = Modifier.weight(1f)
                            )

                            CoffeeTextField(
                                value = maxBrewRatio,
                                onValueChange = { maxBrewRatio = it },
                                label = stringResource(R.string.label_max_ratio),
                                placeholder = stringResource(R.string.placeholder_max_ratio),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Extraction time filter
                    FilterSection(title = stringResource(R.string.text_extraction_time_range)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            CoffeeTextField(
                                value = minExtractionTime,
                                onValueChange = { minExtractionTime = it },
                                label = stringResource(R.string.label_min_time),
                                placeholder = stringResource(R.string.placeholder_min_extraction_time),
                                modifier = Modifier.weight(1f)
                            )

                            CoffeeTextField(
                                value = maxExtractionTime,
                                onValueChange = { maxExtractionTime = it },
                                label = stringResource(R.string.label_max_time),
                                placeholder = stringResource(R.string.placeholder_max_extraction_time),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quality filters
                    FilterSection(title = stringResource(R.string.text_quality_filters)) {
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
                                    Text(stringResource(R.string.text_optimal_extraction_only))
                                    Text(
                                        text = stringResource(R.string.text_optimal_time_range),
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
                                    Text(stringResource(R.string.text_typical_ratio_only))
                                    Text(
                                        text = stringResource(R.string.text_typical_ratio_range),
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
                        text = stringResource(R.string.button_clear_all),
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
                        text = stringResource(R.string.button_apply_filters),
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
                Text(stringResource(R.string.text_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.text_dialog_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}