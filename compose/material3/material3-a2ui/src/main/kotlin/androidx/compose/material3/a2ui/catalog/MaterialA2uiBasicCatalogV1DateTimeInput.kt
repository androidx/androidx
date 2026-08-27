/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.a2ui.catalog

import android.text.format.DateFormat.is24HourFormat
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.a2ui.R
import androidx.compose.material3.isInputValid
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"DateTimeInput"`
 * component.
 */
internal object MaterialA2uiBasicCatalogV1DateTimeInput : A2uiBasicCatalogV1.DateTimeInput {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        value: Long?,
        onValueChange: ((Long?) -> Unit)?,
        enableDate: Boolean,
        enableTime: Boolean,
        min: Long?,
        max: Long?,
        label: String?,
        modifier: Modifier,
    ) {
        val isEnabled = onValueChange != null

        var showStartDateDialog by rememberSaveable { mutableStateOf(false) }
        var showStartTimeDialog by rememberSaveable { mutableStateOf(false) }

        val currentLocale = Locale.current.platformLocale

        val selectDateText = stringResource(R.string.select_date)
        val startDateText =
            remember(value, currentLocale, selectDateText) {
                if (value != null) {
                    val formatter =
                        DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    formatter.format(Date(value))
                } else {
                    selectDateText
                }
            }

        val selectTimeText = stringResource(R.string.select_time)
        val startTimeText =
            remember(value, currentLocale, selectTimeText) {
                if (value != null) {
                    val formatter =
                        DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    formatter.format(Date(value))
                } else {
                    selectTimeText
                }
            }

        val selectableDates =
            remember(min, max) {
                if (min == null && max == null) {
                    DatePickerDefaults.AllDates
                } else {
                    val utcZone = TimeZone.getTimeZone("UTC")
                    val minYear =
                        min?.let {
                            Calendar.getInstance(utcZone)
                                .apply { timeInMillis = it }
                                .get(Calendar.YEAR)
                        }
                    val maxYear =
                        max?.let {
                            Calendar.getInstance(utcZone)
                                .apply { timeInMillis = it }
                                .get(Calendar.YEAR)
                        }
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            if (min != null && utcTimeMillis < min) return false
                            if (max != null && utcTimeMillis > max) return false
                            return true
                        }

                        override fun isSelectableYear(year: Int): Boolean {
                            if (minYear != null && year < minYear) return false
                            if (maxYear != null && year > maxYear) return false
                            return true
                        }
                    }
                }
            }

        if (min != null && max != null && min > max) {
            SideEffect(min, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "Min value ($min) cannot be greater than max value ($max)."
                    )
                )
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            if (label != null) {
                Text(
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (enableDate) {
                    AssistChip(
                        onClick = { showStartDateDialog = true },
                        enabled = isEnabled,
                        label = { Text(startDateText) },
                    )
                }
                if (enableDate && enableTime) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (enableTime) {
                    AssistChip(
                        onClick = { showStartTimeDialog = true },
                        enabled = isEnabled,
                        label = { Text(startTimeText) },
                    )
                }
            }
        }

        if (showStartDateDialog && isEnabled) {
            DateInputDialog(
                value = value,
                selectableDates = selectableDates,
                enableTime = enableTime,
                onValueChange = onValueChange,
                onDismissRequest = { showStartDateDialog = false },
            )
        }

        if (showStartTimeDialog && isEnabled) {
            TimeInputDialog(
                value = value,
                enableDate = enableDate,
                onValueChange = onValueChange,
                onDismissRequest = { showStartTimeDialog = false },
            )
        }
    }
}

@Composable
private fun DateInputDialog(
    value: Long?,
    selectableDates: SelectableDates,
    enableTime: Boolean,
    onValueChange: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = value,
            selectableDates = selectableDates,
        )
    val confirmEnabled =
        remember(datePickerState) { derivedStateOf { datePickerState.selectedDateMillis != null } }

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    val newDateMillis = datePickerState.selectedDateMillis
                    if (newDateMillis != null) {
                        val utcZone = TimeZone.getTimeZone("UTC")
                        val currentCalendar =
                            Calendar.getInstance(utcZone).apply {
                                if (value != null) {
                                    timeInMillis = value
                                }
                            }
                        val newCalendar =
                            Calendar.getInstance(utcZone).apply {
                                timeInMillis = newDateMillis
                                if (enableTime && value != null) {
                                    set(
                                        Calendar.HOUR_OF_DAY,
                                        currentCalendar.get(Calendar.HOUR_OF_DAY),
                                    )
                                    set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, currentCalendar.get(Calendar.SECOND))
                                }
                            }
                        onValueChange(newCalendar.timeInMillis)
                    }
                },
                enabled = confirmEnabled.value,
            ) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.button_cancel)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TimeInputDialog(
    value: Long?,
    enableDate: Boolean,
    onValueChange: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val is24Hour = is24HourFormat(LocalContext.current)
    val (initialHour, initialMinute) =
        remember(value) {
            val calendar =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    if (value != null) {
                        timeInMillis = value
                    }
                }
            calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        }
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = is24Hour,
        )

    TimePickerDialog(
        title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    val newHour = timePickerState.hour
                    val newMinute = timePickerState.minute
                    val newCalendar =
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            if (enableDate && value != null) {
                                timeInMillis = value
                            } else {
                                clear()
                            }
                            set(Calendar.HOUR_OF_DAY, newHour)
                            set(Calendar.MINUTE, newMinute)
                            set(Calendar.SECOND, 0)
                        }
                    onValueChange(newCalendar.timeInMillis)
                },
                enabled = timePickerState.isInputValid,
            ) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.button_cancel)) }
        },
    ) {
        TimePicker(state = timePickerState)
    }
}
