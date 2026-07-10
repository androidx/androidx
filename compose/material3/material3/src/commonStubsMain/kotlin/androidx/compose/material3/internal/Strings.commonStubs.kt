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
        actual val NavigationMenu: Strings
            get() = implementedInJetBrainsFork()

        actual val CloseDrawer: Strings
            get() = implementedInJetBrainsFork()

        actual val CloseRail: Strings
            get() = implementedInJetBrainsFork()

        actual val CloseSheet: Strings
            get() = implementedInJetBrainsFork()

        actual val DefaultErrorMessage: Strings
            get() = implementedInJetBrainsFork()

        actual val SliderRangeStart: Strings
            get() = implementedInJetBrainsFork()

        actual val SliderRangeEnd: Strings
            get() = implementedInJetBrainsFork()

        actual val Dialog: Strings
            get() = implementedInJetBrainsFork()

        actual val MenuExpanded: Strings
            get() = implementedInJetBrainsFork()

        actual val MenuCollapsed: Strings
            get() = implementedInJetBrainsFork()

        actual val SnackbarDismiss: Strings
            get() = implementedInJetBrainsFork()

        actual val SnackbarPaneTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val SearchBarSearch: Strings
            get() = implementedInJetBrainsFork()

        actual val SuggestionsAvailable: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerHeadline: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerYearPickerPaneTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToYearSelection: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToDaySelection: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToNextMonth: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToPreviousMonth: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerNavigateToYearDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerHeadlineDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerNoSelectionDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerTodayDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerScrollToShowLaterYears: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerScrollToShowEarlierYears: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputHeadline: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputLabel: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputHeadlineDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputNoInputDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputInvalidNotAllowed: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputInvalidForPattern: Strings
            get() = implementedInJetBrainsFork()

        actual val DateInputInvalidYearRange: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToCalendarMode: Strings
            get() = implementedInJetBrainsFork()

        actual val DatePickerSwitchToInputMode: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerStartHeadline: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerEndHeadline: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerScrollToShowNextMonth: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerScrollToShowPreviousMonth: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangePickerDayInRange: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangeInputTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val DateRangeInputInvalidRangeInput: Strings
            get() = implementedInJetBrainsFork()

        actual val FloatingToolbarCollapse: Strings
            get() = implementedInJetBrainsFork()

        actual val FloatingToolbarExpand: Strings
            get() = implementedInJetBrainsFork()

        actual val FloatingToolbarMoreOptions: Strings
            get() = implementedInJetBrainsFork()

        actual val BottomSheetPaneTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val BottomSheetDragHandleDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val BottomSheetPartialExpandDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val BottomSheetDismissDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val BottomSheetExpandDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val TooltipLongPressLabel: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerAM: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerPM: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerPeriodToggle: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerHourSelection: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerMinuteSelection: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerHourSuffix: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePicker24HourSuffix: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerMinuteSuffix: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerHour: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerMinute: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerHourTextField: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerMinuteTextField: Strings
            get() = implementedInJetBrainsFork()

        actual val TooltipPaneDescription: Strings
            get() = implementedInJetBrainsFork()

        actual val ExposedDropdownMenu: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerDialogTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val TimeScrollDialogTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val TimeInputDialogTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerToggleKeyboard: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerToggleScroll: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerToggleTouch: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerMinuteError: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePickerHourError: Strings
            get() = implementedInJetBrainsFork()

        actual val TimePicker24HourError: Strings
            get() = implementedInJetBrainsFork()

        actual val ToggleDropdownMenu: Strings
            get() = implementedInJetBrainsFork()

        actual val WideNavigationRailPaneTitle: Strings
            get() = implementedInJetBrainsFork()

        actual val ButtonGroupMoreOptions: Strings
            get() = implementedInJetBrainsFork()
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings, vararg formatArgs: Any): String =
    implementedInJetBrainsFork()

internal actual fun formatString(string: String, vararg formatArgs: Any?): String =
    implementedInJetBrainsFork()
