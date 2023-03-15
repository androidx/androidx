/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangeInputContent(
    selectedStartDateMillis: Long?,
    selectedEndDateMillis: Long?,
    onDatesSelectionChange: (startDateMillis: Long?, endDateMillis: Long?) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates
) {
    // Obtain the DateInputFormat for the default Locale.
    val defaultLocale = defaultLocale()
    val dateInputFormat = remember(defaultLocale) {
        calendarModel.getDateInputFormat(defaultLocale)
    }
    val errorDatePattern = getString(Strings.DateInputInvalidForPattern)
    val errorDateOutOfYearRange = getString(Strings.DateInputInvalidYearRange)
    val errorInvalidNotAllowed = getString(Strings.DateInputInvalidNotAllowed)
    val errorInvalidRange = getString(Strings.DateRangeInputInvalidRangeInput)
    val dateInputValidator = remember(dateInputFormat, dateFormatter) {
        DateInputValidator(
            yearRange = yearRange,
            selectableDates = selectableDates,
            dateInputFormat = dateInputFormat,
            dateFormatter = dateFormatter,
            errorDatePattern = errorDatePattern,
            errorDateOutOfYearRange = errorDateOutOfYearRange,
            errorInvalidNotAllowed = errorInvalidNotAllowed,
            errorInvalidRangeInput = errorInvalidRange
        )
    }
    // Apply both start and end dates for proper validation.
    dateInputValidator.apply {
        currentStartDateMillis = selectedStartDateMillis
        currentEndDateMillis = selectedEndDateMillis
    }
    Row(
        modifier = Modifier.padding(paddingValues = InputTextFieldPadding),
        horizontalArrangement = Arrangement.spacedBy(TextFieldSpacing)
    ) {
        val pattern = dateInputFormat.patternWithDelimiters.uppercase()
        val startRangeText = getString(string = Strings.DateRangePickerStartHeadline)
        DateInputTextField(
            modifier = Modifier.weight(0.5f),
            calendarModel = calendarModel,
            label = {
                Text(startRangeText,
                    modifier = Modifier.semantics {
                        contentDescription = "$startRangeText, $pattern"
                    })
            },
            placeholder = { Text(pattern, modifier = Modifier.clearAndSetSemantics { }) },
            initialDateMillis = selectedStartDateMillis,
            onDateSelectionChange = { startDateMillis ->
                // Delegate to the onDatesSelectionChange and change just the start date.
                onDatesSelectionChange(startDateMillis, selectedEndDateMillis)
            },
            inputIdentifier = InputIdentifier.StartDateInput,
            dateInputValidator = dateInputValidator,
            dateInputFormat = dateInputFormat,
            locale = defaultLocale
        )
        val endRangeText = getString(string = Strings.DateRangePickerEndHeadline)
        DateInputTextField(
            modifier = Modifier.weight(0.5f),
            calendarModel = calendarModel,
            label = {
                Text(endRangeText,
                    modifier = Modifier.semantics {
                        contentDescription = "$endRangeText, $pattern"
                    })
            },
            placeholder = { Text(pattern, modifier = Modifier.clearAndSetSemantics { }) },
            initialDateMillis = selectedEndDateMillis,
            onDateSelectionChange = { endDateMillis ->
                // Delegate to the onDatesSelectionChange and change just the end date.
                onDatesSelectionChange(selectedStartDateMillis, endDateMillis)
            },
            inputIdentifier = InputIdentifier.EndDateInput,
            dateInputValidator = dateInputValidator,
            dateInputFormat = dateInputFormat,
            locale = defaultLocale
        )
    }
}

private val TextFieldSpacing = 8.dp
