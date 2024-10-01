/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.internal

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.R

@Composable
@ReadOnlyComposable
internal fun getString(string: Strings): String {
    return stringResource(string.value)
}

@Composable
@ReadOnlyComposable
internal fun getString(string: Strings, vararg formatArgs: Any): String {
    return stringResource(string.value, *formatArgs)
}

@Composable
@ReadOnlyComposable
internal fun getPlurals(plurals: Plurals, quantity: Int): String {
    return pluralStringResource(plurals.value, quantity)
}

@Composable
@ReadOnlyComposable
internal fun getPlurals(plurals: Plurals, quantity: Int, vararg formatArgs: Any): String {
    return pluralStringResource(plurals.value, quantity, *formatArgs)
}

@JvmInline
@Immutable
internal value class Strings(@StringRes val value: Int) {
    companion object {
        inline val TimePickerHour
            get() = Strings(R.string.wear_m3c_time_picker_hour)

        inline val TimePickerMinute
            get() = Strings(R.string.wear_m3c_time_picker_minute)

        inline val TimePickerSecond
            get() = Strings(R.string.wear_m3c_time_picker_second)

        inline val TimePickerPeriod
            get() = Strings(R.string.wear_m3c_time_picker_period)

        inline val DatePickerYear
            get() = Strings(R.string.wear_m3c_date_picker_year)

        inline val DatePickerMonth
            get() = Strings(R.string.wear_m3c_date_picker_month)

        inline val DatePickerDay
            get() = Strings(R.string.wear_m3c_date_picker_day)

        inline val PickerConfirmButtonContentDescription
            get() = Strings(R.string.wear_m3c_picker_confirm_button_content_description)

        inline val PickerNextButtonContentDescription
            get() = Strings(R.string.wear_m3c_picker_next_button_content_description)

        inline val SliderDecreaseButtonContentDescription
            get() = Strings(R.string.wear_m3c_slider_decrease_content_description)

        inline val SliderIncreaseButtonContentDescription
            get() = Strings(R.string.wear_m3c_slider_increase_content_description)

        inline val StepperDecreaseIconContentDescription
            get() = Strings(R.string.wear_m3c_stepper_decrease_content_description)

        inline val StepperIncreaseIconContentDescription
            get() = Strings(R.string.wear_m3c_stepper_increase_content_description)
    }
}

@JvmInline
@Immutable
internal value class Plurals(@PluralsRes val value: Int) {
    companion object {
        inline val TimePickerHoursContentDescription
            get() = Plurals(R.plurals.wear_m3c_time_picker_hours_content_description)

        inline val TimePickerMinutesContentDescription
            get() = Plurals(R.plurals.wear_m3c_time_picker_minutes_content_description)

        inline val TimePickerSecondsContentDescription
            get() = Plurals(R.plurals.wear_m3c_time_picker_seconds_content_description)
    }
}
