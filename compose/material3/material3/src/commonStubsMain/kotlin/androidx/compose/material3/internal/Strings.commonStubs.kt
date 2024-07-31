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

package androidx.compose.material3.internal

import androidx.compose.material3.implementedInJetBrainsFork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import kotlin.jvm.JvmInline

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings): String = implementedInJetBrainsFork()

@JvmInline
@Immutable
internal actual value class Strings constructor(val value: Int) {
    actual companion object {
        actual val NavigationMenu: Strings = implementedInJetBrainsFork()
        actual val CloseDrawer: Strings = implementedInJetBrainsFork()
        actual val CloseRail: Strings = implementedInJetBrainsFork()
        actual val CloseSheet: Strings = implementedInJetBrainsFork()
        actual val DefaultErrorMessage: Strings = implementedInJetBrainsFork()
        actual val SliderRangeStart: Strings = implementedInJetBrainsFork()
        actual val SliderRangeEnd: Strings = implementedInJetBrainsFork()
        actual val Dialog: Strings = implementedInJetBrainsFork()
        actual val MenuExpanded: Strings = implementedInJetBrainsFork()
        actual val MenuCollapsed: Strings = implementedInJetBrainsFork()
        actual val SnackbarDismiss: Strings = implementedInJetBrainsFork()
        actual val SnackbarPaneTitle: Strings = implementedInJetBrainsFork()
        actual val SearchBarSearch: Strings = implementedInJetBrainsFork()
        actual val SuggestionsAvailable: Strings = implementedInJetBrainsFork()
        actual val DatePickerTitle: Strings = implementedInJetBrainsFork()
        actual val DatePickerHeadline: Strings = implementedInJetBrainsFork()
        actual val DatePickerYearPickerPaneTitle: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToYearSelection: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToDaySelection: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToNextMonth: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToPreviousMonth: Strings = implementedInJetBrainsFork()
        actual val DatePickerNavigateToYearDescription: Strings = implementedInJetBrainsFork()
        actual val DatePickerHeadlineDescription: Strings = implementedInJetBrainsFork()
        actual val DatePickerNoSelectionDescription: Strings = implementedInJetBrainsFork()
        actual val DatePickerTodayDescription: Strings = implementedInJetBrainsFork()
        actual val DatePickerScrollToShowLaterYears: Strings = implementedInJetBrainsFork()
        actual val DatePickerScrollToShowEarlierYears: Strings = implementedInJetBrainsFork()
        actual val DateInputTitle: Strings = implementedInJetBrainsFork()
        actual val DateInputHeadline: Strings = implementedInJetBrainsFork()
        actual val DateInputLabel: Strings = implementedInJetBrainsFork()
        actual val DateInputHeadlineDescription: Strings = implementedInJetBrainsFork()
        actual val DateInputNoInputDescription: Strings = implementedInJetBrainsFork()
        actual val DateInputInvalidNotAllowed: Strings = implementedInJetBrainsFork()
        actual val DateInputInvalidForPattern: Strings = implementedInJetBrainsFork()
        actual val DateInputInvalidYearRange: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToCalendarMode: Strings = implementedInJetBrainsFork()
        actual val DatePickerSwitchToInputMode: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerTitle: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerStartHeadline: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerEndHeadline: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerScrollToShowNextMonth: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerScrollToShowPreviousMonth: Strings = implementedInJetBrainsFork()
        actual val DateRangePickerDayInRange: Strings = implementedInJetBrainsFork()
        actual val DateRangeInputTitle: Strings = implementedInJetBrainsFork()
        actual val DateRangeInputInvalidRangeInput: Strings = implementedInJetBrainsFork()
        actual val BottomSheetPaneTitle: Strings = implementedInJetBrainsFork()
        actual val BottomSheetDragHandleDescription: Strings = implementedInJetBrainsFork()
        actual val BottomSheetPartialExpandDescription: Strings = implementedInJetBrainsFork()
        actual val BottomSheetDismissDescription: Strings = implementedInJetBrainsFork()
        actual val BottomSheetExpandDescription: Strings = implementedInJetBrainsFork()
        actual val TooltipLongPressLabel: Strings = implementedInJetBrainsFork()
        actual val TimePickerAM: Strings = implementedInJetBrainsFork()
        actual val TimePickerPM: Strings = implementedInJetBrainsFork()
        actual val TimePickerPeriodToggle: Strings = implementedInJetBrainsFork()
        actual val TimePickerHourSelection: Strings = implementedInJetBrainsFork()
        actual val TimePickerMinuteSelection: Strings = implementedInJetBrainsFork()
        actual val TimePickerHourSuffix: Strings = implementedInJetBrainsFork()
        actual val TimePicker24HourSuffix: Strings = implementedInJetBrainsFork()
        actual val TimePickerMinuteSuffix: Strings = implementedInJetBrainsFork()
        actual val TimePickerHour: Strings = implementedInJetBrainsFork()
        actual val TimePickerMinute: Strings = implementedInJetBrainsFork()
        actual val TimePickerHourTextField: Strings = implementedInJetBrainsFork()
        actual val TimePickerMinuteTextField: Strings = implementedInJetBrainsFork()
        actual val TooltipPaneDescription: Strings = implementedInJetBrainsFork()
        actual val ExposedDropdownMenu: Strings = implementedInJetBrainsFork()
        actual val ToggleDropdownMenu: Strings = implementedInJetBrainsFork()
        actual val WideNavigationRailPaneTitle: Strings = implementedInJetBrainsFork()
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings, vararg formatArgs: Any): String =
    implementedInJetBrainsFork()
