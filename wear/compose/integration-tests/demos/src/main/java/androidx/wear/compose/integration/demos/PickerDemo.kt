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

import android.annotation.SuppressLint
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerDefaults
import androidx.wear.compose.material.PickerScope
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters

/**
 * A full screen TimePicker with hours, minutes and seconds.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts. In order to
 * ensure that it will draw properly on smaller screens it does not take account of user font size
 * overrides for MaterialTheme.typography.display3 which is used to display the main picker
 * value.
 *
 * @param onTimeConfirm the button event handler.
 * @param modifier the modifiers for the `Box` containing the UI elements.
 * @param time the initial value to seed the picker with.
 */
@SuppressLint("ClassVerificationFailure")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun TimePicker(
    onTimeConfirm: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    time: LocalTime = LocalTime.now()
) {
    // Omit scaling according to Settings > Display > Font size for this screen
    val typography = MaterialTheme.typography.copy(
        display3 = MaterialTheme.typography.display3.copy(
            fontSize = with(LocalDensity.current) { 30.dp.toSp() }
        )
    )
    val hourState = rememberPickerState(
        initialNumberOfOptions = 24,
        initiallySelectedOption = time.hour
    )
    val minuteState = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = time.minute
    )
    val secondState = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = time.second
    )
    val hourContentDescription by remember { derivedStateOf {
        "${hourState.selectedOption} hours"
    } }
    val minuteContentDescription by remember { derivedStateOf {
        "${minuteState.selectedOption} minutes"
    } }
    val secondContentDescription by remember { derivedStateOf {
        "${secondState.selectedOption} seconds"
    } }

    MaterialTheme(typography = typography) {
        var selectedColumn by remember { mutableStateOf(0) }
        val textStyle = MaterialTheme.typography.display3
        val optionColor = MaterialTheme.colors.secondary
        val focusRequester1 = remember { FocusRequester() }
        val focusRequester2 = remember { FocusRequester() }
        val focusRequester3 = remember { FocusRequester() }
        Box(modifier = modifier.fillMaxSize()) {
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
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    PickerWithRSB(
                        readOnly = selectedColumn != 0,
                        state = hourState,
                        focusRequester = focusRequester1,
                        modifier = Modifier.size(40.dp, 100.dp),
                        onSelected = { selectedColumn = 0 },
                        contentDescription = hourContentDescription,
                    ) { hour: Int ->
                        TimePiece(
                            selected = selectedColumn == 0,
                            onSelected = { selectedColumn = 0 },
                            text = "%02d".format(hour),
                            style = textStyle,
                        )
                    }
                    Separator(6.dp, textStyle)
                    PickerWithRSB(
                        readOnly = selectedColumn != 1,
                        state = minuteState,
                        focusRequester = focusRequester2,
                        modifier = Modifier.size(40.dp, 100.dp),
                        onSelected = { selectedColumn = 1 },
                        contentDescription = minuteContentDescription,
                    ) { minute: Int ->
                        TimePiece(
                            selected = selectedColumn == 1,
                            onSelected = { selectedColumn = 1 },
                            text = "%02d".format(minute),
                            style = textStyle,
                        )
                    }
                    Separator(6.dp, textStyle)
                    PickerWithRSB(
                        readOnly = selectedColumn != 2,
                        state = secondState,
                        focusRequester = focusRequester3,
                        modifier = Modifier.size(40.dp, 100.dp),
                        onSelected = { selectedColumn = 2 },
                        contentDescription = secondContentDescription,
                    ) { second: Int ->
                        TimePiece(
                            selected = selectedColumn == 2,
                            onSelected = { selectedColumn = 2 },
                            text = "%02d".format(second),
                            style = textStyle,
                        )
                    }
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Button(onClick = {
                    val confirmedTime = LocalTime.of(
                        hourState.selectedOption,
                        minuteState.selectedOption,
                        secondState.selectedOption
                    )
                    onTimeConfirm(confirmedTime)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "check",
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            LaunchedEffect(selectedColumn) {
                listOf(focusRequester1, focusRequester2, focusRequester3)[selectedColumn]
                    .requestFocus()
            }
        }
    }
}

/**
 * A full screen TimePicker with hours and minutes and AM/PM selector.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts. In order to
 * ensure that it will draw properly on smaller screens it does not take account of user font size
 * overrides for MaterialTheme.typography.display1 which is used to display the main picker
 * value.
 *
 * @param onTimeConfirm the button event handler.
 * @param modifier the modifiers for the `Column` containing the UI elements.
 * @param time the initial value to seed the picker with.
 */
@SuppressLint("ClassVerificationFailure")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun TimePickerWith12HourClock(
    onTimeConfirm: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    time: LocalTime = LocalTime.now()
) {
    // Omit scaling according to Settings > Display > Font size for this screen,
    val typography = MaterialTheme.typography.copy(
        display1 = MaterialTheme.typography.display1.copy(
            fontSize = with(LocalDensity.current) { 40.dp.toSp() }
        )
    )
    val hourState = rememberPickerState(
        initialNumberOfOptions = 12,
        initiallySelectedOption = time[ChronoField.CLOCK_HOUR_OF_AMPM] - 1
    )
    val minuteState = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = time.minute
    )
    val periodState = rememberPickerState(
        initialNumberOfOptions = 2,
        initiallySelectedOption = time[ChronoField.AMPM_OF_DAY]
    )
    val hoursContentDescription by remember {
        derivedStateOf { "${hourState.selectedOption + 1} hours" }
    }
    val minutesContentDescription by remember {
        derivedStateOf { "${minuteState.selectedOption} minutes" }
    }

    val amString = remember {
        LocalTime.of(6, 0).format(DateTimeFormatter.ofPattern("a"))
    }
    val pmString = remember {
        LocalTime.of(18, 0).format(DateTimeFormatter.ofPattern("a"))
    }
    val periodContentDescription by remember {
        derivedStateOf { if (periodState.selectedOption == 0) amString else pmString }
    }

    MaterialTheme(typography = typography) {
        var selectedColumn by remember { mutableStateOf(0) }
        val textStyle = MaterialTheme.typography.display3
        val focusRequester1 = remember { FocusRequester() }
        val focusRequester2 = remember { FocusRequester() }
        val focusRequester3 = remember { FocusRequester() }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (selectedColumn) {
                    0 -> "Hour"
                    1 -> "Minute"
                    else -> ""
                },
                color = MaterialTheme.colors.secondary,
                style = MaterialTheme.typography.button,
                maxLines = 1,
            )
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.width(8.dp))
                PickerWithRSB(
                    readOnly = selectedColumn != 0,
                    state = hourState,
                    focusRequester = focusRequester1,
                    modifier = Modifier.size(48.dp, 100.dp),
                    onSelected = { selectedColumn = 0 },
                    contentDescription = hoursContentDescription,
                ) { hour: Int ->
                    TimePiece(
                        selected = selectedColumn == 0,
                        onSelected = { selectedColumn = 0 },
                        text = "%02d".format(hour + 1),
                        style = textStyle,
                    )
                }
                Separator(2.dp, textStyle)
                PickerWithRSB(
                    readOnly = selectedColumn != 1,
                    state = minuteState,
                    focusRequester = focusRequester2,
                    modifier = Modifier.size(48.dp, 100.dp),
                    onSelected = { selectedColumn = 1 },
                    contentDescription = minutesContentDescription,
                ) { minute: Int ->
                    TimePiece(
                        selected = selectedColumn == 1,
                        onSelected = { selectedColumn = 1 },
                        text = "%02d".format(minute),
                        style = textStyle,
                    )
                }
                Spacer(Modifier.width(6.dp))
                PickerWithRSB(
                    readOnly = selectedColumn != 2,
                    state = periodState,
                    focusRequester = focusRequester3,
                    modifier = Modifier.size(64.dp, 100.dp),
                    onSelected = { selectedColumn = 2 },
                    contentDescription = periodContentDescription,
                ) { period: Int ->
                    TimePiece(
                        selected = selectedColumn == 2,
                        onSelected = { selectedColumn = 2 },
                        text = if (period == 0) amString else pmString,
                        style = textStyle,
                    )
                }
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            )
            Button(onClick = {
                val confirmedTime = LocalTime.of(
                    hourState.selectedOption + 1,
                    minuteState.selectedOption,
                    0
                ).with(ChronoField.AMPM_OF_DAY, periodState.selectedOption.toLong())
                onTimeConfirm(confirmedTime)
            }) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "check",
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
            LaunchedEffect(selectedColumn) {
                listOf(focusRequester1, focusRequester2, focusRequester3)[selectedColumn]
                    .requestFocus()
            }
        }
    }
}

/**
 * Full screen date picker with day, month, year.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts. In order to
 * ensure that it will draw properly on smaller screens it does not take account of user font size
 * overrides for MaterialTheme.typography.display2 which is used to display the main picker
 * value.
 *
 * @param onDateConfirm the button event handler.
 * @param modifier the modifiers for the `Box` containing the UI elements.
 * @param date the initial value to seed the picker with.
 */
@SuppressLint("ClassVerificationFailure")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun DatePicker(
    onDateConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now()
) {
    // Omit scaling according to Settings > Display > Font size for this screen
    val typography = MaterialTheme.typography.copy(
        display2 = MaterialTheme.typography.display2.copy(
            fontSize = with(LocalDensity.current) { 34.dp.toSp() }
        )
    )
    MaterialTheme(typography = typography) {
        val yearState = rememberPickerState(
            initialNumberOfOptions = 3000,
            initiallySelectedOption = date.year - 1
        )
        val monthState = rememberPickerState(
            initialNumberOfOptions = 12,
            initiallySelectedOption = date.monthValue - 1
        )
        val maxDayInMonth by remember {
            derivedStateOf {
                val firstDayOfMonth =
                    LocalDate.of(
                        yearState.selectedOption + 1,
                        monthState.selectedOption + 1,
                        1
                    )
                firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
            }
        }
        val dayState = rememberPickerState(
            initialNumberOfOptions = maxDayInMonth,
            initiallySelectedOption = date.dayOfMonth - 1
        )
        val focusRequester1 = remember { FocusRequester() }
        val focusRequester2 = remember { FocusRequester() }
        val focusRequester3 = remember { FocusRequester() }

        LaunchedEffect(maxDayInMonth) {
            if (maxDayInMonth != dayState.numberOfOptions) {
                dayState.numberOfOptions = maxDayInMonth
            }
        }
        val monthNames = remember {
            val monthFormatter = DateTimeFormatter.ofPattern("MMM")
            val months = 1..12
            months.map {
                // Translate month index into 3-character month string e.g. Jan.
                LocalDate.of(2022, it, 1).format(monthFormatter)
            }
        }
        val yearContentDescription by remember { derivedStateOf {
            "${yearState.selectedOption + 1}"
        } }
        val monthContentDescription by remember { derivedStateOf {
            monthNames[monthState.selectedOption]
        } }
        val dayContentDescription by remember { derivedStateOf {
            "${dayState.selectedOption + 1}"
        } }

        var selectedColumn by remember { mutableStateOf(0) }
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                val spacerWidth = 8.dp
                val dayWidth = 54.dp
                val monthWidth = 80.dp
                val yearWidth = 128.dp
                val offset = when (selectedColumn) {
                    0 -> (boxConstraints.maxWidth - dayWidth) / 2
                    1 -> (boxConstraints.maxWidth - monthWidth) / 2 - dayWidth - spacerWidth
                    else -> (boxConstraints.maxWidth - yearWidth) / 2 - monthWidth
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(offset)
                ) {
                    if (selectedColumn < 2) {
                        DatePickerImpl(
                            state = dayState,
                            readOnly = selectedColumn != 0,
                            onSelected = { selectedColumn = 0 },
                            text = { day: Int -> "%d".format(day + 1) },
                            width = dayWidth,
                            focusRequester = focusRequester1,
                            contentDescription = dayContentDescription,
                        )
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                    DatePickerImpl(
                        state = monthState,
                        readOnly = selectedColumn != 1,
                        onSelected = { selectedColumn = 1 },
                        text = { month: Int -> monthNames[month] },
                        width = monthWidth,
                        focusRequester = focusRequester2,
                        contentDescription = monthContentDescription,
                    )
                    if (selectedColumn > 0) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                        DatePickerImpl(
                            state = yearState,
                            readOnly = selectedColumn != 2,
                            onSelected = { selectedColumn = 2 },
                            text = { year: Int -> "%4d".format(year + 1) },
                            width = yearWidth,
                            focusRequester = focusRequester3,
                            contentDescription = yearContentDescription,
                        )
                    }
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Button(
                    onClick = {
                        if (selectedColumn == 2) {
                            val confirmedDate = LocalDate.of(
                                yearState.selectedOption + 1,
                                monthState.selectedOption + 1,
                                dayState.selectedOption + 1
                            )
                            onDateConfirm(confirmedDate)
                        } else {
                            selectedColumn = (selectedColumn + 1) % 3
                        }
                    },
                ) {
                    DemoIcon(
                        resourceId =
                        if (selectedColumn < 2) R.drawable.ic_chevron_right_24
                        else R.drawable.ic_check_24px
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            LaunchedEffect(selectedColumn) {
                listOf(focusRequester1, focusRequester2, focusRequester3)[selectedColumn]
                    .requestFocus()
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
    focusRequester: FocusRequester,
    width: Dp,
    contentDescription: String,
) {
    PickerWithRSB(
        readOnly = readOnly,
        state = state,
        focusRequester = focusRequester,
        modifier = Modifier.size(width, 100.dp),
        onSelected = onSelected,
        contentDescription = contentDescription,
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
internal fun TimePiece(
    selected: Boolean,
    onSelected: () -> Unit,
    text: String,
    style: TextStyle,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val modifier = Modifier
            .align(Alignment.Center)
            .wrapContentSize()
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
private fun Separator(width: Dp, textStyle: TextStyle) {
    Spacer(Modifier.width(width))
    Text(
        text = ":",
        style = textStyle,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier.clearAndSetSemantics {}
    )
    Spacer(Modifier.width(width))
}

@Composable fun PickerWithRSB(
    state: PickerState,
    readOnly: Boolean,
    modifier: Modifier,
    focusRequester: FocusRequester,
    contentDescription: String?,
    readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
    flingBehavior: FlingBehavior = PickerDefaults.flingBehavior(state = state),
    onSelected: () -> Unit = {},
    option: @Composable PickerScope.(optionIndex: Int) -> Unit
) {
    Picker(
        state = state,
        modifier = modifier.rsbScroll(
            scrollableState = state,
            flingBehavior = flingBehavior,
            focusRequester = focusRequester
        ),
        flingBehavior = flingBehavior,
        readOnly = readOnly,
        readOnlyLabel = readOnlyLabel,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option = option
    )
}

@Composable
fun PickerWithoutGradient() {
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    val contentDescription by remember { derivedStateOf {
        "${state.selectedOption + 1}"
    } }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Picker(
            readOnly = false,
            modifier = Modifier.size(100.dp, 100.dp),
            gradientRatio = 0.0f,
            contentDescription = contentDescription,
            state = state
        ) {
            Text(items[it])
        }
    }
}