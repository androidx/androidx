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

package androidx.compose.material3

import androidx.compose.material3.internal.CalendarDate
import androidx.compose.material3.internal.DateInputFormat
import androidx.compose.runtime.Stable

@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal actual class DateInputValidator
actual constructor(
    yearRange: IntRange,
    selectableDates: SelectableDates,
    dateInputFormat: DateInputFormat,
    dateFormatter: DatePickerFormatter,
    errorDatePattern: String,
    errorDateOutOfYearRange: String,
    errorInvalidNotAllowed: String,
    errorInvalidRangeInput: String
) {
    actual var currentStartDateMillis: Long? = implementedInJetBrainsFork()
    actual var currentEndDateMillis: Long? = implementedInJetBrainsFork()

    actual fun validate(
        dateToValidate: CalendarDate?,
        inputIdentifier: InputIdentifier,
        locale: CalendarLocale
    ): String = implementedInJetBrainsFork()
}
