/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import java.util.Locale

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings): String {
    return when (string) {
        Strings.NavigationMenu -> "Navigation menu"
        Strings.CloseDrawer -> "Close navigation menu"
        Strings.CloseSheet -> "Close sheet"
        Strings.DefaultErrorMessage -> "Invalid input"
        Strings.SliderRangeStart -> "Range Start"
        Strings.SliderRangeEnd -> "Range End"
        Strings.Dialog -> "Dialog"
        Strings.MenuExpanded -> "Expanded"
        Strings.MenuCollapsed -> "Collapsed"
        Strings.SnackbarDismiss -> "Dismiss"
        Strings.SearchBarSearch -> "Search"
        Strings.SuggestionsAvailable -> "Suggestions below"
        Strings.DatePickerTitle -> "Select date"
        Strings.DatePickerHeadline -> "Selected date"
        Strings.DatePickerYearPickerPaneTitle -> "Year picker visible"
        Strings.DatePickerSwitchToYearSelection -> "Switch to selecting a year"
        Strings.DatePickerSwitchToDaySelection -> "Swipe to select a year, or tap to switch " +
            "back to selecting a day"

        Strings.DatePickerSwitchToNextMonth -> "Change to next month"
        Strings.DatePickerSwitchToPreviousMonth -> "Change to previous month"
        Strings.DatePickerNavigateToYearDescription -> "Navigate to year %1$"
        Strings.DatePickerHeadlineDescription -> "Current selection: %1$"
        Strings.DatePickerNoSelectionDescription -> "None"
        Strings.DatePickerTodayDescription -> "Today"
        Strings.DatePickerScrollToShowLaterYears -> "Scroll to show later years"
        Strings.DatePickerScrollToShowEarlierYears -> "Scroll to show earlier years"
        Strings.DateInputTitle -> "Select date"
        Strings.DateInputHeadline -> "Entered date"
        Strings.DateInputLabel -> "Date"
        Strings.DateInputHeadlineDescription -> "Entered date: %1$"
        Strings.DateInputNoInputDescription -> "None"
        Strings.DateInputInvalidNotAllowed -> "Date not allowed: %1$"
        Strings.DateInputInvalidForPattern -> "Date does not match expected pattern: %1$"
        Strings.DateInputInvalidYearRange -> "Date out of expected year range %1$ - %2$"
        Strings.DatePickerSwitchToCalendarMode -> "Switch to calendar input mode"
        Strings.DatePickerSwitchToInputMode -> "Switch to text input mode"
        Strings.DateRangePickerTitle -> "Select dates"
        Strings.DateRangePickerStartHeadline -> "Start date"
        Strings.DateRangePickerEndHeadline -> "End date"
        Strings.DateRangePickerScrollToShowNextMonth -> "Scroll to show the next month"
        Strings.DateRangePickerScrollToShowPreviousMonth -> "Scroll to show the previous month"
        Strings.DateRangePickerDayInRange -> "In range"
        Strings.DateRangeInputTitle -> "Enter dates"
        Strings.DateRangeInputInvalidRangeInput -> "Invalid date range input"
        Strings.BottomSheetPaneTitle -> "Bottom Sheet"
        Strings.BottomSheetDragHandleDescription -> "Drag Handle"
        Strings.BottomSheetPartialExpandDescription -> "Collapse bottom sheet"
        Strings.BottomSheetDismissDescription -> "Dismiss bottom sheet"
        Strings.BottomSheetExpandDescription -> "Expand bottom sheet"
        Strings.TooltipLongPressLabel -> "Show tooltip"
        Strings.TimePickerAM -> "AM"
        Strings.TimePickerPM -> "PM"
        Strings.TimePickerPeriodToggle -> "Select AM or PM"
        Strings.TimePickerMinuteSelection -> "Select minutes"
        Strings.TimePickerHourSelection -> "Select hour"
        Strings.TimePickerHourSuffix -> "%1$ o\\'clock"
        Strings.TimePickerMinuteSuffix -> "%1$ minutes"
        Strings.TimePicker24HourSuffix -> "%1$ hours"
        Strings.TimePickerMinute -> "Minute"
        Strings.TimePickerHour -> "Hour"
        Strings.TimePickerMinuteTextField -> "for minutes"
        Strings.TimePickerHourTextField -> "for hour"
        Strings.TooltipPaneDescription -> "Tooltip"
        else -> ""
    }
}

@JvmInline
internal actual value class Strings constructor(val value: Int) {
    actual companion object {
        actual val NavigationMenu = Strings(0)
        actual val CloseDrawer = Strings(1)
        actual val CloseSheet = Strings(2)
        actual val DefaultErrorMessage = Strings(3)
        actual val SliderRangeStart = Strings(4)
        actual val SliderRangeEnd = Strings(5)
        actual val Dialog = Strings(6)
        actual val MenuExpanded = Strings(7)
        actual val MenuCollapsed = Strings(8)
        actual val SnackbarDismiss = Strings(9)
        actual val SearchBarSearch = Strings(10)
        actual val SuggestionsAvailable = Strings(11)
        actual val DatePickerTitle = Strings(12)
        actual val DatePickerHeadline = Strings(13)
        actual val DatePickerYearPickerPaneTitle = Strings(14)
        actual val DatePickerSwitchToYearSelection = Strings(15)
        actual val DatePickerSwitchToDaySelection = Strings(16)
        actual val DatePickerSwitchToNextMonth = Strings(17)
        actual val DatePickerSwitchToPreviousMonth = Strings(18)
        actual val DatePickerNavigateToYearDescription = Strings(19)
        actual val DatePickerHeadlineDescription = Strings(20)
        actual val DatePickerNoSelectionDescription = Strings(21)
        actual val DatePickerTodayDescription = Strings(22)
        actual val DatePickerScrollToShowLaterYears = Strings(23)
        actual val DatePickerScrollToShowEarlierYears = Strings(24)
        actual val DateInputTitle = Strings(25)
        actual val DateInputHeadline = Strings(26)
        actual val DateInputLabel = Strings(27)
        actual val DateInputHeadlineDescription = Strings(28)
        actual val DateInputNoInputDescription = Strings(29)
        actual val DateInputInvalidNotAllowed = Strings(30)
        actual val DateInputInvalidForPattern = Strings(31)
        actual val DateInputInvalidYearRange = Strings(32)
        actual val DatePickerSwitchToCalendarMode = Strings(33)
        actual val DatePickerSwitchToInputMode = Strings(34)
        actual val DateRangePickerTitle = Strings(35)
        actual val DateRangePickerStartHeadline = Strings(36)
        actual val DateRangePickerEndHeadline = Strings(37)
        actual val DateRangePickerScrollToShowNextMonth = Strings(38)
        actual val DateRangePickerScrollToShowPreviousMonth = Strings(39)
        actual val DateRangePickerDayInRange = Strings(40)
        actual val DateRangeInputTitle = Strings(41)
        actual val DateRangeInputInvalidRangeInput = Strings(42)
        actual val BottomSheetPaneTitle = Strings(43)
        actual val BottomSheetDragHandleDescription = Strings(44)
        actual val BottomSheetPartialExpandDescription = Strings(45)
        actual val BottomSheetDismissDescription = Strings(46)
        actual val BottomSheetExpandDescription = Strings(47)
        actual val TooltipLongPressLabel = Strings(48)
        actual val TimePickerAM = Strings(49)
        actual val TimePickerPM = Strings(50)
        actual val TimePickerPeriodToggle = Strings(51)
        actual val TimePickerHourSelection = Strings(52)
        actual val TimePickerMinuteSelection = Strings(53)
        actual val TimePickerHourSuffix = Strings(54)
        actual val TimePicker24HourSuffix = Strings(55)
        actual val TimePickerMinuteSuffix = Strings(56)
        actual val TimePickerHour = Strings(57)
        actual val TimePickerMinute = Strings(58)
        actual val TimePickerHourTextField = Strings(59)
        actual val TimePickerMinuteTextField = Strings(60)
        actual val TooltipPaneDescription = Strings(61)
        actual val ExposedDropdownMenu = Strings(62)
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings, vararg formatArgs: Any): String =
    String.format(getString(string), Locale.getDefault(), *formatArgs)
