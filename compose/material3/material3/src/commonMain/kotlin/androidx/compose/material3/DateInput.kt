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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateInputContent(
    selectedDateMillis: Long?,
    onDateSelectionChange: (dateInMillis: Long?) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors
) {
    // Obtain the DateInputFormat for the default Locale.
    val defaultLocale = defaultLocale()
    val dateInputFormat = remember(defaultLocale) {
        calendarModel.getDateInputFormat(defaultLocale)
    }
    val errorDatePattern = getString(Strings.DateInputInvalidForPattern)
    val errorDateOutOfYearRange = getString(Strings.DateInputInvalidYearRange)
    val errorInvalidNotAllowed = getString(Strings.DateInputInvalidNotAllowed)
    val dateInputValidator = remember(dateInputFormat, dateFormatter) {
        DateInputValidator(
            yearRange = yearRange,
            selectableDates = selectableDates,
            dateInputFormat = dateInputFormat,
            dateFormatter = dateFormatter,
            errorDatePattern = errorDatePattern,
            errorDateOutOfYearRange = errorDateOutOfYearRange,
            errorInvalidNotAllowed = errorInvalidNotAllowed,
            errorInvalidRangeInput = "" // Not used for a single date input
        )
    }
    val pattern = dateInputFormat.patternWithDelimiters.uppercase()
    val labelText = getString(string = Strings.DateInputLabel)
    DateInputTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(InputTextFieldPadding),
        calendarModel = calendarModel,
        label = {
            Text(
                labelText,
                modifier = Modifier.semantics { contentDescription = "$labelText, $pattern" })
        },
        placeholder = { Text(pattern, modifier = Modifier.clearAndSetSemantics { }) },
        initialDateMillis = selectedDateMillis,
        onDateSelectionChange = onDateSelectionChange,
        inputIdentifier = InputIdentifier.SingleDateInput,
        dateInputValidator = dateInputValidator.apply {
            // Only need to apply the start date, as this is for a single date input.
            currentStartDateMillis = selectedDateMillis
        },
        dateInputFormat = dateInputFormat,
        locale = defaultLocale,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateInputTextField(
    modifier: Modifier,
    initialDateMillis: Long?,
    onDateSelectionChange: (Long?) -> Unit,
    calendarModel: CalendarModel,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    inputIdentifier: InputIdentifier,
    dateInputValidator: DateInputValidator,
    dateInputFormat: DateInputFormat,
    locale: Locale,
    colors: DatePickerColors
) {
    val errorText = rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialDateMillis?.let {
                    calendarModel.formatWithPattern(
                        it,
                        dateInputFormat.patternWithoutDelimiters,
                        locale
                    )
                } ?: "",
                TextRange(0, 0)
            )
        )
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            if (input.text.length <= dateInputFormat.patternWithoutDelimiters.length &&
                input.text.all { it.isDigit() }
            ) {
                text = input
                val trimmedText = input.text.trim()
                if (trimmedText.isEmpty() ||
                    trimmedText.length < dateInputFormat.patternWithoutDelimiters.length
                ) {
                    errorText.value = ""
                    onDateSelectionChange(null)
                } else {
                    val parsedDate = calendarModel.parse(
                        trimmedText,
                        dateInputFormat.patternWithoutDelimiters
                    )
                    errorText.value = dateInputValidator.validate(
                        dateToValidate = parsedDate,
                        inputIdentifier = inputIdentifier,
                        locale = locale
                    )
                    // Set the parsed date only if the error validation returned an empty string.
                    // Otherwise, set it to null, as the validation failed.
                    onDateSelectionChange(
                        if (errorText.value.isEmpty()) {
                            parsedDate?.utcTimeMillis
                        } else {
                            null
                        }
                    )
                }
            }
        },
        modifier = modifier
            // Add bottom padding when there is no error. Otherwise, remove it as the error text
            // will take additional height.
            .padding(
                bottom = if (errorText.value.isNotBlank()) {
                    0.dp
                } else {
                    InputTextNonErroneousBottomPadding
                }
            )
            .semantics {
                if (errorText.value.isNotBlank()) error(errorText.value)
            },
        label = label,
        placeholder = placeholder,
        supportingText = { if (errorText.value.isNotBlank()) Text(errorText.value) },
        isError = errorText.value.isNotBlank(),
        visualTransformation = DateVisualTransformation(dateInputFormat),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        colors = colors.dateTextFieldColors
    )
}

/**
 * A date input validator class.
 *
 * @param yearRange an [IntRange] that holds the year range that the date picker is be limited to
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed
 * @param dateInputFormat a [DateInputFormat] that holds date patterns information
 * @param dateFormatter a [DatePickerFormatter]
 * @param errorDatePattern a string for displaying an error message when an input does not match the
 * expected date pattern. The string expects a date pattern string as an argument to be formatted
 * into it.
 * @param errorDateOutOfYearRange a string for displaying an error message when an input date
 * exceeds the year-range defined at the DateInput's state. The string expects a start and end year
 * as arguments to be formatted into it.
 * @param errorInvalidNotAllowed a string for displaying an error message when an input date does
 * not pass the DateInput's validator check. The string expects a date argument to be formatted into
 * it.
 * @param errorInvalidRangeInput a string for displaying an error message when in a range input mode
 * and one of the input dates is out of order (i.e. the user inputs a start date that is after the
 * end date, or an end date that is before the start date)
 * @param currentStartDateMillis the currently selected start date in milliseconds. Only checked
 * against when the [InputIdentifier] is [InputIdentifier.EndDateInput].
 * @param currentEndDateMillis the currently selected end date in milliseconds. Only checked
 * against when the [InputIdentifier] is [InputIdentifier.StartDateInput].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class DateInputValidator(
    private val yearRange: IntRange,
    private val selectableDates: SelectableDates,
    private val dateInputFormat: DateInputFormat,
    private val dateFormatter: DatePickerFormatter,
    private val errorDatePattern: String,
    private val errorDateOutOfYearRange: String,
    private val errorInvalidNotAllowed: String,
    private val errorInvalidRangeInput: String,
    internal var currentStartDateMillis: Long? = null,
    internal var currentEndDateMillis: Long? = null,
) {

    /**
     * Validates a [CalendarDate] input and returns an error string in case an issue with the given
     * date is detected, or an empty string in case there are no issues.
     *
     * @param dateToValidate a [CalendarDate] input to validate
     * @param inputIdentifier an [InputIdentifier] that provides information about the input field
     * that is supposed to hold the date.
     * @param locale the current [Locale]
     */
    fun validate(
        dateToValidate: CalendarDate?,
        inputIdentifier: InputIdentifier,
        locale: Locale
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
            if (!isSelectableYear(dateToValidate.year) ||
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
        if ((inputIdentifier == InputIdentifier.StartDateInput &&
                dateToValidate.utcTimeMillis >= (currentEndDateMillis ?: Long.MAX_VALUE)) ||
            (inputIdentifier == InputIdentifier.EndDateInput &&
                dateToValidate.utcTimeMillis < (currentStartDateMillis ?: Long.MIN_VALUE))
        ) {
            // The input start date is after the end date, or the end date is before the start date.
            return errorInvalidRangeInput
        }

        return ""
    }
}

/**
 * Represents different input identifiers for the [DateInputTextField]. An `InputIdentifier` is used
 * when validating the user input, and especially when validating an input range.
 */
@Immutable
@JvmInline
internal value class InputIdentifier internal constructor(internal val value: Int) {

    companion object {
        /** Single date input */
        val SingleDateInput = InputIdentifier(0)

        /** A start date input */
        val StartDateInput = InputIdentifier(1)

        /** An end date input */
        val EndDateInput = InputIdentifier(2)
    }

    override fun toString() = when (this) {
        SingleDateInput -> "SingleDateInput"
        StartDateInput -> "StartDateInput"
        EndDateInput -> "EndDateInput"
        else -> "Unknown"
    }
}

/**
 * A [VisualTransformation] for date input. The transformation will automatically display the date
 * delimiters provided by the [DateInputFormat] as the date is being entered into the text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class DateVisualTransformation(private val dateInputFormat: DateInputFormat) :
    VisualTransformation {

    private val firstDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.indexOf(dateInputFormat.delimiter)
    private val secondDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.lastIndexOf(dateInputFormat.delimiter)
    private val dateFormatLength: Int = dateInputFormat.patternWithoutDelimiters.length

    private val dateOffsetTranslator = object : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int {
            return when {
                offset < firstDelimiterOffset -> offset
                offset < secondDelimiterOffset -> offset + 1
                offset <= dateFormatLength -> offset + 2
                else -> dateFormatLength + 2 // 10
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            return when {
                offset <= firstDelimiterOffset - 1 -> offset
                offset <= secondDelimiterOffset - 1 -> offset - 1
                offset <= dateFormatLength + 1 -> offset - 2
                else -> dateFormatLength // 8
            }
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val trimmedText =
            if (text.text.length > dateFormatLength) {
                text.text.substring(0 until dateFormatLength)
            } else {
                text.text
            }
        var transformedText = ""
        trimmedText.forEachIndexed { index, char ->
            transformedText += char
            if (index + 1 == firstDelimiterOffset || index + 2 == secondDelimiterOffset) {
                transformedText += dateInputFormat.delimiter
            }
        }
        return TransformedText(AnnotatedString(transformedText), dateOffsetTranslator)
    }
}

internal val InputTextFieldPadding = PaddingValues(
    start = 24.dp,
    end = 24.dp,
    top = 10.dp
)

// An optional padding that will only be added to the bottom of the date input text field when it's
// not showing an error message.
private val InputTextNonErroneousBottomPadding = 16.dp
