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

package androidx.wear.compose.integration.demos

import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerDefaults
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimePickerWithHoursMinutesSeconds() {
    // Omit scaling according to Settings > Display > Font size for this screen
    val typography = MaterialTheme.typography.copy(
        display3 = MaterialTheme.typography.display3.copy(
            fontSize = with(LocalDensity.current) { 30.dp.toSp() }
        )
    )
    MaterialTheme(typography = typography) {
        var selectedColumn by remember { mutableStateOf(0) }
        val textStyle = MaterialTheme.typography.display3
        val optionColor = MaterialTheme.colors.secondary
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (selectedColumn) {
                        0 -> "Hour"
                        1 -> "Minute"
                        else -> "Second"
                    },
                    color = optionColor,
                    style = MaterialTheme.typography.button,
                    maxLines = 1,
                )
                val weightsToCenterVertically = 0.5f
                Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val state1 = rememberPickerState(
                        initialNumberOfOptions = 24,
                        initiallySelectedOption = 6
                    )
                    val flingBehavior1 = PickerDefaults.flingBehavior(state1)
                    val pickerModifier1 = Modifier.size(40.dp, 100.dp)
                        .rsbScroll(state1, flingBehavior1)

                    Picker(
                        readOnly = selectedColumn != 0,
                        state = state1,
                        modifier = pickerModifier1,
                        flingBehavior = flingBehavior1
                    ) { hour: Int ->
                        TimePiece(
                            selected = selectedColumn == 0,
                            onSelected = { selectedColumn = 0 },
                            text = "%02d".format(hour),
                            style = textStyle,
                        )
                    }
                    Separator(6.dp, textStyle)
                    Picker(
                        readOnly = selectedColumn != 1,
                        state = rememberPickerState(initialNumberOfOptions = 60),
                        modifier = Modifier.size(40.dp, 100.dp),
                    ) { minute: Int ->
                        TimePiece(
                            selected = selectedColumn == 1,
                            onSelected = { selectedColumn = 1 },
                            text = "%02d".format(minute),
                            style = textStyle,
                        )
                    }
                    Separator(6.dp, textStyle)
                    Picker(
                        readOnly = selectedColumn != 2,
                        state = rememberPickerState(initialNumberOfOptions = 60),
                        modifier = Modifier.size(40.dp, 100.dp),
                    ) { second: Int ->
                        TimePiece(
                            selected = selectedColumn == 2,
                            onSelected = { selectedColumn = 2 },
                            text = "%02d".format(second),
                            style = textStyle,
                        )
                    }
                }
                Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
                Button(onClick = {}) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_24px),
                        contentDescription = "check",
                        modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimePickerWith12HourClock() {
    // Omit scaling according to Settings > Display > Font size for this screen,
    val typography = MaterialTheme.typography.copy(
        display1 = MaterialTheme.typography.display1.copy(
            fontSize = with(LocalDensity.current) { 40.dp.toSp() }
        )
    )
    MaterialTheme(typography = typography) {
        var morning by remember { mutableStateOf(true) }
        var selectedColumn by remember { mutableStateOf(0) }
        val textStyle = MaterialTheme.typography.display1
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            CompactChip(
                onClick = { morning = !morning },
                modifier = Modifier.size(width = 50.dp, height = 24.dp),
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (morning) "AM" else "PM",
                            color = MaterialTheme.colors.onPrimary,
                            style = MaterialTheme.typography.button,
                        )
                    }
                },
                colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.secondary),
                contentPadding = PaddingValues(vertical = 0.dp),
            )
            Spacer(Modifier.fillMaxWidth().weight(0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.width(8.dp))
                Picker(
                    readOnly = selectedColumn != 0,
                    state = rememberPickerState(
                        initialNumberOfOptions = 12,
                        initiallySelectedOption = 6
                    ),
                    modifier = Modifier.size(64.dp, 100.dp),
                    readOnlyLabel = { LabelText("Hour") }
                ) { hour: Int ->
                    TimePiece(
                        selected = selectedColumn == 0,
                        onSelected = { selectedColumn = 0 },
                        text = "%02d".format(hour + 1),
                        style = textStyle,
                    )
                }
                Separator(8.dp, textStyle)
                Picker(
                    readOnly = selectedColumn != 1,
                    state = rememberPickerState(initialNumberOfOptions = 60),
                    modifier = Modifier.size(64.dp, 100.dp),
                    readOnlyLabel = { LabelText("Min") }
                ) { minute: Int ->
                    TimePiece(
                        selected = selectedColumn == 1,
                        onSelected = { selectedColumn = 1 },
                        text = "%02d".format(minute),
                        style = textStyle,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.fillMaxWidth().weight(0.5f))
            Button(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_24px),
                    contentDescription = "check",
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DatePicker() {
    // Omit scaling according to Settings > Display > Font size for this screen
    val typography = MaterialTheme.typography.copy(
        display2 = MaterialTheme.typography.display2.copy(
            fontSize = with(LocalDensity.current) { 34.dp.toSp() }
        )
    )
    MaterialTheme(typography = typography) {
        val today = remember { Calendar.getInstance() }
        val yearState = rememberPickerState(
            initialNumberOfOptions = 3000,
            initiallySelectedOption = today.get(Calendar.YEAR) - 1)
        val monthState = rememberPickerState(
            initialNumberOfOptions = 12,
            initiallySelectedOption = today.get(Calendar.MONTH))
        val monthCalendar = remember { Calendar.getInstance() }
        val maxDayInMonth by remember {
            derivedStateOf {
                monthCalendar.set(yearState.selectedOption + 1, monthState.selectedOption, 1)
                val max = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                max
            }
        }
        val dayState = rememberPickerState(
            initialNumberOfOptions = maxDayInMonth,
            initiallySelectedOption = today.get(Calendar.DAY_OF_MONTH) - 1
        )
        LaunchedEffect(maxDayInMonth) {
            if (maxDayInMonth != dayState.numberOfOptions) {
                dayState.numberOfOptions = maxDayInMonth
            }
        }
        val monthNames = remember {
            val months = 0..11
            months.map {
                // Translate month index into 3-character month string e.g. Jan.
                // Using deprecated Date constructor rather than LocalDate in order to avoid
                // requirement for API 26+.
                val calendar = Calendar.getInstance()
                calendar.set(2022, it, 1)
                SimpleDateFormat("MMM").format(calendar.getTime())
            }
        }
        var selectedColumn by remember { mutableStateOf(0) }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxConstraints = this
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when (selectedColumn) {
                        0 -> "Day"
                        1 -> "Month"
                        else -> "Year"
                    },
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.button,
                    maxLines = 1,
                )
                val weightsToCenterVertically = 0.5f
                Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
                val spacerWidth = 8.dp
                val dayWidth = 54.dp
                val monthWidth = 80.dp
                val yearWidth = 128.dp
                val offset = when (selectedColumn) {
                    0 -> (boxConstraints.maxWidth - dayWidth) / 2
                    1 -> (boxConstraints.maxWidth - monthWidth) / 2 - dayWidth - spacerWidth
                    else -> (boxConstraints.maxWidth - yearWidth) / 2 - monthWidth
                }
                Row(modifier = Modifier.fillMaxWidth().offset(offset)) {
                    if (selectedColumn < 2) {
                        DatePickerImpl(
                            state = dayState,
                            readOnly = selectedColumn != 0,
                            onSelected = { selectedColumn = 0 },
                            text = { day: Int -> "%d".format(day + 1) },
                            width = dayWidth
                        )
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                    DatePickerImpl(
                        state = monthState,
                        readOnly = selectedColumn != 1,
                        onSelected = { selectedColumn = 1 },
                        text = { month: Int -> monthNames[month] },
                        width = monthWidth
                    )
                    if (selectedColumn > 0) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                        DatePickerImpl(
                            state = yearState,
                            readOnly = selectedColumn != 2,
                            onSelected = { selectedColumn = 2 },
                            text = { year: Int -> "%4d".format(year + 1) },
                            width = yearWidth
                        )
                    }
                }
                Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))
                Button(
                    onClick = { selectedColumn = (selectedColumn + 1) % 3 },
                    colors =
                    if (selectedColumn < 2) ButtonDefaults.secondaryButtonColors()
                    else ButtonDefaults.primaryButtonColors()
                ) {
                    DemoIcon(
                        resourceId =
                        if (selectedColumn < 2) R.drawable.ic_chevron_right_24
                        else R.drawable.ic_check_24px
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DatePickerImpl(
    state: PickerState,
    readOnly: Boolean,
    onSelected: () -> Unit,
    text: (option: Int) -> String,
    width: Dp
) {
    Picker(
        readOnly = readOnly,
        state = state,
        modifier = Modifier.size(width, 100.dp),
    ) { option ->
        TimePiece(
            selected = !readOnly,
            onSelected = onSelected,
            text = text(option),
            style = MaterialTheme.typography.display2,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TimePiece(
    selected: Boolean,
    onSelected: () -> Unit,
    text: String,
    style: TextStyle,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val modifier = Modifier.align(Alignment.Center).wrapContentSize()
        Text(
            text = text,
            maxLines = 1,
            style = style,
            color =
                if (selected) MaterialTheme.colors.secondary
                else MaterialTheme.colors.onBackground,
            modifier =
                if (selected) modifier
                else modifier.pointerInteropFilter {
                    if (it.action == MotionEvent.ACTION_DOWN) onSelected()
                    true
                },
        )
    }
}

@Composable
private fun BoxScope.LabelText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier.align(Alignment.TopCenter).offset(y = 8.dp)
    )
}

@Composable
private fun Separator(width: Dp, textStyle: TextStyle) {
    Spacer(Modifier.width(width))
    Text(
        text = ":",
        style = textStyle,
        color = MaterialTheme.colors.onBackground
    )
    Spacer(Modifier.width(width))
}