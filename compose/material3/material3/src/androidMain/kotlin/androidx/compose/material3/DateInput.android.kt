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
    private val yearRange: IntRange,
    private val selectableDates: SelectableDates,
    private val dateInputFormat: DateInputFormat,
    private val dateFormatter: DatePickerFormatter,
    private val errorDatePattern: String,
    private val errorDateOutOfYearRange: String,
    private val errorInvalidNotAllowed: String,
    private val errorInvalidRangeInput: String
) {
    /**
     * the currently selected start date in milliseconds. Only checked against when the
     * [InputIdentifier] is [InputIdentifier.EndDateInput].
     */
    actual var currentStartDateMillis: Long? = null

    /**
     * the currently selected end date in milliseconds. Only checked against when the
     * [InputIdentifier] is [InputIdentifier.StartDateInput].
     */
    actual var currentEndDateMillis: Long? = null

    actual fun validate(
        dateToValidate: CalendarDate?,
        inputIdentifier: InputIdentifier,
        locale: CalendarLocale
    ): String {
        if (dateToValidate == null) {
            return errorDatePattern.format(dateInputFormat.patternWithDelimiters.uppercase())
        }
        // Check that the date is within the valid range of years.
        if (!yearRange.contains(dateToValidate.year)) {
            return errorDateOutOfYearRange.format(
                yearRange.first.toLocalString(),
                yearRange.last.toLocalString()
            )
        }
        // Check that the provided SelectableDates allows this date to be selected.
        with(selectableDates) {
            if (
                !isSelectableYear(dateToValidate.year) ||
                    !isSelectableDate(dateToValidate.utcTimeMillis)
            ) {
                return errorInvalidNotAllowed.format(
                    dateFormatter.formatDate(
                        dateMillis = dateToValidate.utcTimeMillis,
                        locale = locale
                    )
                )
            }
        }

        // Additional validation when the InputIdentifier is for start of end dates in a range input
        if (
            (inputIdentifier == InputIdentifier.StartDateInput &&
                dateToValidate.utcTimeMillis > (currentEndDateMillis ?: Long.MAX_VALUE)) ||
                (inputIdentifier == InputIdentifier.EndDateInput &&
                    dateToValidate.utcTimeMillis < (currentStartDateMillis ?: Long.MIN_VALUE))
        ) {
            // The input start date is after the end date, or the end date is before the start date.
            return errorInvalidRangeInput
        }

        return ""
    }
}
