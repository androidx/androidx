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
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.annotation.RequiresApi
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerDefaults
import androidx.wear.compose.material.PickerGroup
import androidx.wear.compose.material.PickerGroupItem
import androidx.wear.compose.material.PickerGroupState
import androidx.wear.compose.material.PickerScope
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TouchExplorationStateProvider
import androidx.wear.compose.material.rememberPickerGroupState
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
    val touchExplorationStateProvider = remember { DefaultTouchExplorationStateProvider() }
    val touchExplorationServicesEnabled by touchExplorationStateProvider
        .touchExplorationState()

    MaterialTheme(typography = typography) {
        // When the time picker loads, none of the individual pickers are selected in talkback mode,
        // otherwise hours picker should be focused.
        val pickerGroupState = if (touchExplorationServicesEnabled) {
            rememberPickerGroupState(FocusableElementsTimePicker.NONE.index)
        } else {
            rememberPickerGroupState(FocusableElementsTimePicker.HOURS.index)
        }
        val textStyle = MaterialTheme.typography.display3
        val optionColor = MaterialTheme.colors.secondary
        val pickerOption = pickerTextOption(textStyle) { "%02d".format(it) }
        val focusRequesterConfirmButton = remember { FocusRequester() }

        val hourContentDescription =
            createDescription(pickerGroupState, hourState.selectedOption, "Hour")
        val minuteContentDescription =
            createDescription(pickerGroupState, minuteState.selectedOption, "Minute")
        val secondContentDescription =
            createDescription(pickerGroupState, secondState.selectedOption, "Second")

        val onPickerSelected =
            { current: FocusableElementsTimePicker, next: FocusableElementsTimePicker ->
                if (pickerGroupState.selectedIndex != current.index) {
                    pickerGroupState.selectedIndex = current.index
                } else {
                    // Double tapping on selected column moves the focus to the next column
                    pickerGroupState.selectedIndex = next.index
                    if (next == FocusableElementsTimePicker.CONFIRM_BUTTON) {
                        focusRequesterConfirmButton.requestFocus()
                    }
                }
            }

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (FocusableElementsTimePicker[pickerGroupState.selectedIndex]) {
                        FocusableElementsTimePicker.HOURS -> "Hour"
                        FocusableElementsTimePicker.MINUTES -> "Minute"
                        FocusableElementsTimePicker.SECONDS -> "Second"
                        else -> ""
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
                    PickerGroup(
                        pickerGroupItemWithRSB(
                            pickerState = hourState,
                            modifier = Modifier
                                .size(40.dp, 100.dp),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementsTimePicker.HOURS,
                                    FocusableElementsTimePicker.MINUTES
                                )
                            },
                            contentDescription = hourContentDescription,
                            option = pickerOption
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = minuteState,
                            modifier = Modifier
                                .size(40.dp, 100.dp),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementsTimePicker.MINUTES,
                                    FocusableElementsTimePicker.SECONDS
                                )
                            },
                            contentDescription = minuteContentDescription,
                            option = pickerOption
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = secondState,
                            modifier = Modifier
                                .size(40.dp, 100.dp),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementsTimePicker.SECONDS,
                                    FocusableElementsTimePicker.CONFIRM_BUTTON
                                )
                            },
                            contentDescription = secondContentDescription,
                            option = pickerOption
                        ),
                        pickerGroupState = pickerGroupState,
                        separator = { Separator(6.dp, textStyle) },
                        autoCenter = false,
                        touchExplorationStateProvider = touchExplorationStateProvider
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Button(
                    onClick = {
                        val confirmedTime = LocalTime.of(
                            hourState.selectedOption,
                            minuteState.selectedOption,
                            secondState.selectedOption
                        )
                        onTimeConfirm(confirmedTime)
                    },
                    modifier = Modifier
                        .semantics {
                            focused = pickerGroupState.selectedIndex ==
                                FocusableElementsTimePicker.CONFIRM_BUTTON.index
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "confirm",
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                    )
                }
                Spacer(Modifier.height(12.dp))
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
        initiallySelectedOption = time[ChronoField.AMPM_OF_DAY],
        repeatItems = false
    )

    val touchExplorationStateProvider = remember { DefaultTouchExplorationStateProvider() }

    val touchExplorationServicesEnabled by touchExplorationStateProvider
        .touchExplorationState()

    MaterialTheme(typography = typography) {
        // When the time picker loads, none of the individual pickers are selected in talkback mode,
        // otherwise hours picker should be focused.
        val pickerGroupState =
            if (touchExplorationServicesEnabled) {
                rememberPickerGroupState(FocusableElement12Hour.NONE.index)
            } else {
                rememberPickerGroupState(FocusableElement12Hour.HOURS.index)
            }
        val textStyle = MaterialTheme.typography.display3
        val focusRequesterConfirmButton = remember { FocusRequester() }

        val hoursContentDescription =
            createDescription12Hour(pickerGroupState, hourState.selectedOption + 1, "Hour")

        val minutesContentDescription =
            createDescription12Hour(pickerGroupState, minuteState.selectedOption, "Minute")

        val amString = "AM"
        val pmString = "PM"
        val periodContentDescription by remember(
            pickerGroupState.selectedIndex,
            periodState.selectedOption
        ) {
            derivedStateOf {
                if (pickerGroupState.selectedIndex == FocusableElement12Hour.NONE.index) {
                    "Period"
                } else if (periodState.selectedOption == 0) {
                    amString
                } else pmString
            }
        }
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (FocusableElement12Hour[pickerGroupState.selectedIndex]) {
                        FocusableElement12Hour.HOURS -> "Hour"
                        FocusableElement12Hour.MINUTES -> "Minute"
                        else -> ""
                    },
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.button,
                    maxLines = 1
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
                    horizontalArrangement = Arrangement.Center
                ) {
                    val doubleTapToNext =
                        { current: FocusableElement12Hour, next: FocusableElement12Hour ->
                            if (pickerGroupState.selectedIndex != current.index) {
                                pickerGroupState.selectedIndex = current.index
                            } else {
                                // Double tapping on selected column moves the focus to the next column
                                pickerGroupState.selectedIndex = next.index
                                if (next == FocusableElement12Hour.CONFIRM_BUTTON) {
                                    focusRequesterConfirmButton.requestFocus()
                                }
                            }
                        }
                    Spacer(Modifier.width(16.dp))
                    PickerGroup(
                        pickerGroupItemWithRSB(
                            pickerState = hourState,
                            modifier = Modifier.size(48.dp, 100.dp),
                            onSelected = {
                                doubleTapToNext(
                                    FocusableElement12Hour.HOURS,
                                    FocusableElement12Hour.MINUTES
                                )
                            },
                            contentDescription = hoursContentDescription,
                            option = pickerTextOption(textStyle) { "%02d".format(it + 1) }
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = minuteState,
                            modifier = Modifier.size(48.dp, 100.dp),
                            onSelected = {
                                doubleTapToNext(
                                    FocusableElement12Hour.MINUTES,
                                    FocusableElement12Hour.PERIOD
                                )
                            },
                            contentDescription = minutesContentDescription,
                            option = pickerTextOption(textStyle) { "%02d".format(it) }
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = periodState,
                            modifier = Modifier.size(64.dp, 100.dp),
                            contentDescription = periodContentDescription,
                            onSelected = {
                                doubleTapToNext(
                                    FocusableElement12Hour.PERIOD,
                                    FocusableElement12Hour.CONFIRM_BUTTON
                                )
                            },
                            option = pickerTextOption(textStyle) {
                                if (it == 0) amString else pmString
                            }
                        ),
                        autoCenter = false,
                        pickerGroupState = pickerGroupState,
                        separator = {
                            if (it == 0) {
                                Separator(2.dp, textStyle)
                            } else Spacer(Modifier.width(12.dp))
                        },
                        touchExplorationStateProvider = touchExplorationStateProvider
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Button(
                    onClick = {
                        val confirmedTime = LocalTime.of(
                            hourState.selectedOption + 1,
                            minuteState.selectedOption,
                            0
                        ).with(ChronoField.AMPM_OF_DAY, periodState.selectedOption.toLong())
                        onTimeConfirm(confirmedTime)
                    },
                    modifier = Modifier
                        .semantics {
                            focused = pickerGroupState.selectedIndex ==
                                FocusableElement12Hour.CONFIRM_BUTTON.index
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "confirm",
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center)
                    )
                }
                Spacer(Modifier.height(8.dp))
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
 * @param fromDate the minimum date to be selected in picker
 * @param toDate the maximum date to be selected in picker
 */
@SuppressLint("ClassVerificationFailure")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun DatePicker(
    onDateConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now(),
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null
) {
    if (fromDate != null && toDate != null) {
        verifyDates(date, fromDate, toDate)
    }

    val datePickerState = remember(date) {
        if (fromDate != null && toDate == null) {
            DatePickerState(date = date, fromDate = fromDate, toDate = fromDate.plusYears(3000))
        } else if (fromDate == null && toDate != null) {
            DatePickerState(date = date, fromDate = toDate.minusYears(3000), toDate = toDate)
        } else {
            DatePickerState(date, fromDate, toDate)
        }
    }

    // Omit scaling according to Settings > Display > Font size for this screen
    val typography = MaterialTheme.typography.copy(
        display2 = MaterialTheme.typography.display2.copy(
            fontSize = with(LocalDensity.current) { 34.dp.toSp() }
        )
    )
    val touchExplorationStateProvider = remember { DefaultTouchExplorationStateProvider() }
    val touchExplorationServicesEnabled by touchExplorationStateProvider
        .touchExplorationState()

    MaterialTheme(typography = typography) {
        // When the time picker loads, none of the individual pickers are selected in talkback mode,
        // otherwise day picker should be focused.
        val pickerGroupState = if (touchExplorationServicesEnabled) {
            rememberPickerGroupState(FocusableElementDatePicker.NONE.index)
        } else {
            rememberPickerGroupState(FocusableElementDatePicker.DAY.index)
        }
        val textStyle = MaterialTheme.typography.display3
        val optionColor = MaterialTheme.colors.secondary
        val focusRequesterConfirmButton = remember { FocusRequester() }

        val yearString = "Year"
        val monthString = "Month"
        val dayString = "Day"

        LaunchedEffect(
            datePickerState.yearState.selectedOption,
            datePickerState.monthState.selectedOption
        ) {
            if (datePickerState.numOfMonths != datePickerState.monthState.numberOfOptions) {
                datePickerState.monthState.numberOfOptions = datePickerState.numOfMonths
            }
            if (datePickerState.numOfDays != datePickerState.dayState.numberOfOptions) {
                if (datePickerState.dayState.selectedOption >= datePickerState.numOfDays) {
                    datePickerState.dayState.animateScrollToOption(datePickerState.numOfDays - 1)
                }
                datePickerState.dayState.numberOfOptions = datePickerState.numOfDays
            }
        }
        val shortMonthNames = remember { getMonthNames("MMM") }
        val fullMonthNames = remember { getMonthNames("MMMM") }
        val yearContentDescription by remember(
            pickerGroupState.selectedIndex,
            datePickerState.currentYear()
        ) {
            derivedStateOf {
                createDescriptionDatePicker(
                    pickerGroupState,
                    datePickerState.currentYear(),
                    yearString
                )
            }
        }
        val monthContentDescription by remember(
            pickerGroupState.selectedIndex,
            datePickerState.currentMonth()
        ) {
            derivedStateOf {
                if (pickerGroupState.selectedIndex == FocusableElementDatePicker.NONE.index) {
                    monthString
                } else {
                    fullMonthNames[(datePickerState.currentMonth() - 1) % 12]
                }
            }
        }
        val dayContentDescription by remember(
            pickerGroupState.selectedIndex,
            datePickerState.currentDay()
        ) {
            derivedStateOf {
                createDescriptionDatePicker(
                    pickerGroupState,
                    datePickerState.currentDay(),
                    dayString
                )
            }
        }
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
        ) {
            val boxConstraints = this
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when (FocusableElementDatePicker[pickerGroupState.selectedIndex]) {
                        FocusableElementDatePicker.DAY -> dayString
                        FocusableElementDatePicker.MONTH -> monthString
                        FocusableElementDatePicker.YEAR -> yearString
                        else -> ""
                    },
                    color = optionColor,
                    style = MaterialTheme.typography.button,
                    maxLines = 1
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
                val yearWidth = 100.dp
                val numberOfSpacers = 2
                val onPickerSelected =
                    { current: FocusableElementDatePicker, next: FocusableElementDatePicker ->
                        if (pickerGroupState.selectedIndex != current.index) {
                            pickerGroupState.selectedIndex = current.index
                    } else {
                        pickerGroupState.selectedIndex = next.index
                        if (next == FocusableElementDatePicker.CONFIRM_BUTTON) {
                            focusRequesterConfirmButton.requestFocus()
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(
                            getPickerGroupRowOffset(
                                boxConstraints.maxWidth,
                                dayWidth,
                                monthWidth,
                                yearWidth,
                                spacerWidth,
                                numberOfSpacers,
                                touchExplorationServicesEnabled,
                                pickerGroupState
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PickerGroup(
                        pickerGroupItemWithRSB(
                            pickerState = datePickerState.dayState,
                            modifier = Modifier.size(dayWidth, 100.dp),
                            contentDescription = dayContentDescription,
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementDatePicker.DAY,
                                    FocusableElementDatePicker.MONTH
                                )
                            },
                            option = pickerTextOption(textStyle) {
                                "%d".format(datePickerState.currentDay(it))
                            }
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = datePickerState.monthState,
                            modifier = Modifier.size(monthWidth, 100.dp),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementDatePicker.MONTH,
                                    FocusableElementDatePicker.YEAR
                                )
                            },
                            contentDescription = monthContentDescription,
                            option = pickerTextOption(textStyle) {
                                shortMonthNames[(datePickerState.currentMonth(it) - 1) % 12]
                            }
                        ),
                        pickerGroupItemWithRSB(
                            pickerState = datePickerState.yearState,
                            modifier = Modifier.size(yearWidth, 100.dp),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElementDatePicker.YEAR,
                                    FocusableElementDatePicker.CONFIRM_BUTTON
                                )
                            },
                            contentDescription = yearContentDescription,
                            option = pickerTextOption(textStyle) {
                                "%4d".format(datePickerState.currentYear(it))
                            }
                        ),
                        pickerGroupState = pickerGroupState,
                        autoCenter = true,
                        separator = { Spacer(modifier = Modifier.width(spacerWidth)) },
                        touchExplorationStateProvider = touchExplorationStateProvider
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(weightsToCenterVertically)
                )
                Button(
                    onClick = {
                        if (pickerGroupState.selectedIndex >= 2) {
                            val confirmedYear: Int = datePickerState.currentYear()
                            val confirmedMonth: Int = datePickerState.currentMonth()
                            val confirmedDay: Int = datePickerState.currentDay()
                            val confirmedDate =
                                LocalDate.of(confirmedYear, confirmedMonth, confirmedDay)
                            onDateConfirm(confirmedDate)
                        } else if (pickerGroupState.selectedIndex ==
                            FocusableElementDatePicker.DAY.index) {
                            onPickerSelected(
                                FocusableElementDatePicker.DAY,
                                FocusableElementDatePicker.MONTH
                            )
                        } else if (pickerGroupState.selectedIndex ==
                            FocusableElementDatePicker.MONTH.index) {
                            onPickerSelected(
                                FocusableElementDatePicker.MONTH,
                                FocusableElementDatePicker.YEAR
                            )
                        }
                    },
                    modifier = Modifier
                        .semantics {
                            focused = pickerGroupState.selectedIndex ==
                                FocusableElementDatePicker.CONFIRM_BUTTON.index
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable()
                ) {
                    Icon(
                        imageVector = if (pickerGroupState.selectedIndex < 2)
                            Icons.Filled.KeyboardArrowRight else Icons.Filled.Check,
                        contentDescription = if (pickerGroupState.selectedIndex < 2)
                            "next"
                        else
                            "confirm",
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// getPickerGroupRowOffset function calculates the offset of the picker group row in talkback mode.
// Autocenter doesn't work when no index is selected which is the case in talkback mode.
// Offset is required to move the first picker to the center.
private fun getPickerGroupRowOffset(
    rowWidth: Dp,
    dayPickerWidth: Dp,
    monthPickerWidth: Dp,
    yearPickerWidth: Dp,
    spacerWidth: Dp,
    numberOfSpacers: Int,
    touchExplorationServicesEnabled: Boolean,
    pickerGroupState: PickerGroupState
): Dp {
    // Calculates the extra offset named currentOffset because the total content width is more than the screen width
    var finalRowOffset = 0.dp
    val totalContentWidth =
        (dayPickerWidth + monthPickerWidth + yearPickerWidth + (spacerWidth * numberOfSpacers))
    val currentOffset = (rowWidth - totalContentWidth) / 2

    if (touchExplorationServicesEnabled &&
        pickerGroupState.selectedIndex < 0 // When talkback is on and no picker is selected
    ) {
        finalRowOffset = ((rowWidth - dayPickerWidth) / 2) - currentOffset
    } else if (touchExplorationServicesEnabled &&
        pickerGroupState.selectedIndex > 2 // When talkback is on and focus goes to confirm button
    ) {
        finalRowOffset = ((rowWidth - yearPickerWidth) / 2) -
            (dayPickerWidth + monthPickerWidth + (spacerWidth * numberOfSpacers) + currentOffset)
    }

    return finalRowOffset
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

@Composable
fun pickerGroupItemWithRSB(
    pickerState: PickerState,
    modifier: Modifier,
    contentDescription: String?,
    onSelected: () -> Unit,
    readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
    option: @Composable PickerScope.(optionIndex: Int, pickerSelected: Boolean) -> Unit
) = PickerGroupItem(
    pickerState = pickerState,
    modifier = modifier.rsbScroll(
        scrollableState = pickerState,
        flingBehavior = PickerDefaults.flingBehavior(pickerState)
    ),
    contentDescription = contentDescription,
    onSelected = onSelected,
    readOnlyLabel = readOnlyLabel,
    option = option
)

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

private fun pickerTextOption(textStyle: TextStyle, indexToText: (Int) -> String):
    (@Composable PickerScope.(optionIndex: Int, pickerSelected: Boolean) -> Unit) = {
        value: Int, pickerSelected: Boolean ->
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = indexToText(value),
            maxLines = 1,
            style = textStyle,
            color =
            if (pickerSelected) MaterialTheme.colors.secondary
            else MaterialTheme.colors.onBackground,
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentSize()
        )
    }
}

private fun createDescription(
    pickerGroupState: PickerGroupState,
    selectedValue: Int,
    label: String
): String {
    return when (pickerGroupState.selectedIndex) {
        FocusableElementsTimePicker.NONE.index -> label
        else -> if (selectedValue == 1) {
            "$selectedValue $label"
        } else {
            "$selectedValue ${label}s"
        }
    }
}

private fun createDescription12Hour(
    pickerGroupState: PickerGroupState,
    selectedValue: Int,
    label: String
): String {
    return when (pickerGroupState.selectedIndex) {
        FocusableElement12Hour.NONE.index -> label
        else -> if (selectedValue == 1) {
            "$selectedValue $label"
        } else {
            "$selectedValue ${label}s"
        }
    }
}

private fun createDescriptionDatePicker(
    pickerGroupState: PickerGroupState,
    selectedValue: Int,
    label: String
): String {
    return when (pickerGroupState.selectedIndex) {
        FocusableElementDatePicker.NONE.index -> label
        else -> "$label, $selectedValue"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun verifyDates(
    date: LocalDate,
    fromDate: LocalDate,
    toDate: LocalDate
) {
    require(toDate >= fromDate) { "toDate should be greater than or equal to fromDate" }
    require(date in fromDate..toDate) { "date should lie between fromDate and toDate" }
}

@SuppressLint("ClassVerificationFailure")
@RequiresApi(Build.VERSION_CODES.O)
private fun getMonthNames(pattern: String): List<String> {
    val monthFormatter = DateTimeFormatter.ofPattern(pattern)
    val months = 1..12
    return months.map {
        LocalDate.of(2022, it, 1).format(monthFormatter)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal class DatePickerState constructor(
    private val date: LocalDate,
    private val fromDate: LocalDate?,
    private val toDate: LocalDate?
) {
    private val yearOffset = fromDate?.year ?: 1
    val numOfYears = (toDate?.year ?: 3000) - (yearOffset - 1)
    val yearState =
        PickerState(
            initialNumberOfOptions = numOfYears,
            initiallySelectedOption = date.year - yearOffset
        )
    val selectedYearEqualsFromYear: Boolean
        get() = (yearState.selectedOption == 0)
    val selectedYearEqualsToYear: Boolean
        get() = (yearState.selectedOption == yearState.numberOfOptions - 1)

    private val monthOffset: Int
        get() = (if (selectedYearEqualsFromYear) (fromDate?.monthValue ?: 1) else 1)
    private val maxMonths: Int
        get() = (if (selectedYearEqualsToYear) (toDate?.monthValue ?: 12) else 12)
    val numOfMonths: Int
        get() = (maxMonths.minus(monthOffset - 1))
    val monthState =
        PickerState(
            initialNumberOfOptions = numOfMonths,
            initiallySelectedOption = date.monthValue - monthOffset
        )
    val selectedMonthEqualsFromMonth: Boolean
        get() = (selectedYearEqualsFromYear && monthState.selectedOption == 0)
    val selectedMonthEqualsToMonth: Boolean
        get() = (selectedYearEqualsToYear &&
            monthState.selectedOption == monthState.numberOfOptions - 1)

    private val dayOffset: Int
        get() = (if (selectedMonthEqualsFromMonth) (fromDate?.dayOfMonth ?: 1) else 1)
    private val firstDayOfMonth: LocalDate
        get() = LocalDate.of(
            currentYear(),
            currentMonth(),
            1
        )
    private val maxDaysInMonth: Int
        get() = (
            if (toDate != null && selectedMonthEqualsToMonth) {
                toDate.dayOfMonth
            } else {
                firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
            }
            )
    val numOfDays: Int
        get() = maxDaysInMonth.minus(dayOffset - 1)

    val dayState =
        PickerState(
            initialNumberOfOptions = numOfDays,
            initiallySelectedOption = date.dayOfMonth - dayOffset
        )

    fun currentYear(year: Int = yearState.selectedOption): Int {
        return year + yearOffset
    }
    fun currentMonth(month: Int = monthState.selectedOption): Int {
        return month + monthOffset
    }
    fun currentDay(day: Int = dayState.selectedOption): Int {
        return day + dayOffset
    }
}

/**
 * This is copied from [TouchExplorationStateProvider]. Please updated if something changes over there.
 */
private class DefaultTouchExplorationStateProvider : TouchExplorationStateProvider {

    @Composable
    public override fun touchExplorationState(): State<Boolean> {
        val context = LocalContext.current
        val accessibilityManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }

        val listener = remember { Listener() }

        LocalLifecycleOwner.current.lifecycle.ObserveState(
            handleEvent = { event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    listener.register(accessibilityManager)
                }
            },
            onDispose = {
                listener.unregister(accessibilityManager)
            }
        )

        return remember { derivedStateOf { listener.isEnabled() } }
    }

    @Composable
    private fun Lifecycle.ObserveState(
        handleEvent: (Lifecycle.Event) -> Unit = {},
        onDispose: () -> Unit = {}
    ) {
        DisposableEffect(this) {
            val observer = LifecycleEventObserver { _, event ->
                handleEvent(event)
            }
            this@ObserveState.addObserver(observer)
            onDispose {
                onDispose()
                this@ObserveState.removeObserver(observer)
            }
        }
    }

    private class Listener : AccessibilityStateChangeListener, TouchExplorationStateChangeListener {
        private var accessibilityEnabled by mutableStateOf(false)
        private var touchExplorationEnabled by mutableStateOf(false)

        fun isEnabled() = accessibilityEnabled && touchExplorationEnabled

        override fun onAccessibilityStateChanged(it: Boolean) {
            accessibilityEnabled = it
        }

        override fun onTouchExplorationStateChanged(it: Boolean) {
            touchExplorationEnabled = it
        }

        fun register(am: AccessibilityManager) {
            accessibilityEnabled = am.isEnabled
            touchExplorationEnabled = am.isTouchExplorationEnabled

            am.addTouchExplorationStateChangeListener(this)
            am.addAccessibilityStateChangeListener(this)
        }

        fun unregister(am: AccessibilityManager) {
            am.removeTouchExplorationStateChangeListener(this)
            am.removeAccessibilityStateChangeListener(this)
        }
    }
}

enum class FocusableElementsTimePicker(val index: Int) {
    HOURS(0),
    MINUTES(1),
    SECONDS(2),
    CONFIRM_BUTTON(3),
    NONE(-1);

    companion object {
        private val map = FocusableElementsTimePicker.values().associateBy { it.index }
        operator fun get(value: Int) = map[value]
    }
}

enum class FocusableElement12Hour(val index: Int) {
    HOURS(0),
    MINUTES(1),
    PERIOD(2),
    CONFIRM_BUTTON(3),
    NONE(-1);
    companion object {
        private val map = FocusableElement12Hour.values().associateBy { it.index }
        operator fun get(value: Int) = map[value]
    }
}

enum class FocusableElementDatePicker(val index: Int) {
    DAY(0),
    MONTH(1),
    YEAR(2),
    CONFIRM_BUTTON(3),
    NONE(-1);
    companion object {
        private val map = FocusableElementDatePicker.values().associateBy { it.index }
        operator fun get(value: Int) = map[value]
    }
}
