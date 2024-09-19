/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.material3.samples

import android.os.Build
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DatePickerSample() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pre-select a date for January 4, 2020
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = 1578096000000)
        DatePicker(state = datePickerState, modifier = Modifier.padding(16.dp))

        Text(
            "Selected date timestamp: ${datePickerState.selectedDateMillis ?: "no selection"}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DatePickerDialogSample() {
    // Decoupled snackbar host state from scaffold state for demo purposes.
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    SnackbarHost(hostState = snackState, Modifier)
    val openDialog = remember { mutableStateOf(true) }
    // TODO demo how to read the selected date from the state.
    if (openDialog.value) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }
        DatePickerDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                openDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        snackScope.launch {
                            snackState.showSnackbar(
                                "Selected date timestamp: ${datePickerState.selectedDateMillis}"
                            )
                        }
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) { Text("Cancel") }
            }
        ) {
            // The verticalScroll will allow scrolling to show the entire month in case there is not
            // enough horizontal space (for example, when in landscape mode).
            // Note that it's still currently recommended to use a DisplayMode.Input at the state in
            // those cases.
            DatePicker(
                state = datePickerState,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

@Suppress("ClassVerificationFailure")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DatePickerWithDateSelectableDatesSample() {
    val datePickerState =
        rememberDatePickerState(
            selectableDates =
                object : SelectableDates {
                    // Blocks Sunday and Saturday from being selected.
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val dayOfWeek =
                                Instant.ofEpochMilli(utcTimeMillis)
                                    .atZone(ZoneId.of("UTC"))
                                    .toLocalDate()
                                    .dayOfWeek
                            dayOfWeek != DayOfWeek.SUNDAY && dayOfWeek != DayOfWeek.SATURDAY
                        } else {
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = utcTimeMillis
                            calendar[Calendar.DAY_OF_WEEK] != Calendar.SUNDAY &&
                                calendar[Calendar.DAY_OF_WEEK] != Calendar.SATURDAY
                        }
                    }

                    // Allow selecting dates from year 2023 forward.
                    override fun isSelectableYear(year: Int): Boolean {
                        return year > 2022
                    }
                }
        )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatePicker(state = datePickerState)
        Text(
            "Selected date timestamp: ${datePickerState.selectedDateMillis ?: "no selection"}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DateInputSample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
        DatePicker(state = state, modifier = Modifier.padding(16.dp))

        Text(
            "Entered date timestamp: ${state.selectedDateMillis ?: "no input"}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun DateRangePickerSample() {
    // Decoupled snackbar host state from scaffold state for demo purposes.
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    SnackbarHost(hostState = snackState, Modifier.zIndex(1f))

    val state = rememberDateRangePickerState()
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        // Add a row with "Save" and dismiss actions.
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(DatePickerDefaults.colors().containerColor)
                    .padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { /* dismiss the UI */ }) {
                Icon(Icons.Filled.Close, contentDescription = "Localized description")
            }
            TextButton(
                onClick = {
                    snackScope.launch {
                        val range = state.selectedStartDateMillis!!..state.selectedEndDateMillis!!
                        snackState.showSnackbar("Saved range (timestamps): $range")
                    }
                },
                enabled = state.selectedEndDateMillis != null
            ) {
                Text(text = "Save")
            }
        }
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
