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
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.internal.CalendarDate
import androidx.compose.material3.internal.CalendarModel
import androidx.compose.material3.internal.DateInputFormat
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.formatString
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline
import kotlinx.coroutines.delay

@Composable
internal fun DateInputContent(
    selectedDateMillis: Long?,
    onDateSelectionChange: (dateInMillis: Long?) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    focusRequester: FocusRequester?,
) {
    // Obtain the DateInputFormat for the default Locale.
    val dateInputFormat =
        remember(calendarModel.locale) { calendarModel.getDateInputFormat(calendarModel.locale) }
    val errorDatePattern = getString(Strings.DateInputInvalidForPattern)
    val errorDateOutOfYearRange = getString(Strings.DateInputInvalidYearRange)
    val errorInvalidNotAllowed = getString(Strings.DateInputInvalidNotAllowed)
    val dateInputValidator =
        remember(dateInputFormat, dateFormatter) {
            DateInputValidator(
                yearRange = yearRange,
                selectableDates = selectableDates,
                dateInputFormat = dateInputFormat,
                dateFormatter = dateFormatter,
                errorDatePattern = errorDatePattern,
                errorDateOutOfYearRange = errorDateOutOfYearRange,
                errorInvalidNotAllowed = errorInvalidNotAllowed,
                errorInvalidRangeInput = "", // Not used for a single date input
            )
        }
    val pattern = dateInputFormat.patternWithDelimiters.uppercase()
    val labelText = getString(string = Strings.DateInputLabel)
    DateInputTextField(
        modifier = Modifier.fillMaxWidth().padding(InputTextFieldPadding),
        calendarModel = calendarModel,
        label = {
            Text(
                labelText,
                modifier = Modifier.semantics { contentDescription = "$labelText, $pattern" },
            )
        },
        placeholder = { Text(pattern, modifier = Modifier.clearAndSetSemantics {}) },
        initialDateMillis = selectedDateMillis,
        onDateSelectionChange = onDateSelectionChange,
        inputIdentifier = InputIdentifier.SingleDateInput,
        dateInputValidator =
            dateInputValidator.apply {
                // Only need to apply the start date, as this is for a single date input.
                currentStartDateMillis = selectedDateMillis
            },
        dateInputFormat = dateInputFormat,
        locale = calendarModel.locale,
        colors = colors,
        focusRequester = focusRequester,
    )
}

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
    locale: CalendarLocale,
    colors: DatePickerColors,
    focusRequester: FocusRequester?,
) {
    val initialText =
        remember(initialDateMillis, dateInputFormat, locale) {
            initialDateMillis?.let {
                calendarModel.formatWithPattern(
                    it,
                    dateInputFormat.patternWithoutDelimiters,
                    locale,
                )
            } ?: ""
        }
    val state = rememberTextFieldState(initialText = initialText)

    val errorText =
        remember(state.text, dateInputValidator, dateInputFormat, locale, inputIdentifier) {
            val text = state.text.trim()
            if (text.length == dateInputFormat.patternWithoutDelimiters.length) {
                val parsedDate =
                    calendarModel.parse(
                        date = text.toString(),
                        pattern = dateInputFormat.patternWithoutDelimiters,
                        locale = locale,
                    )
                dateInputValidator.validate(
                    dateToValidate = parsedDate,
                    inputIdentifier = inputIdentifier,
                    locale = locale,
                )
            } else {
                ""
            }
        }

    val inputTransformation =
        remember(
            dateInputFormat,
            calendarModel,
            dateInputValidator,
            inputIdentifier,
            locale,
            onDateSelectionChange,
        ) {
            DateInputTransformation(
                dateInputFormat = dateInputFormat,
                calendarModel = calendarModel,
                dateInputValidator = dateInputValidator,
                inputIdentifier = inputIdentifier,
                locale = locale,
                onDateSelectionChange = onDateSelectionChange,
            )
        }

    val outputTransformation =
        remember(dateInputFormat) { DateOutputTransformation(dateInputFormat) }

    // Calculate how much bottom padding should be added. In case there is an error text, which is
    // added as a supportingText, take into account the default supportingText padding to ensure
    // the padding does not trigger a component height change.
    val bottomPadding =
        if (errorText.isBlank()) {
            InputTextNonErroneousBottomPadding
        } else {
            val textFieldPadding = TextFieldDefaults.supportingTextPadding()
            InputTextNonErroneousBottomPadding -
                (textFieldPadding.calculateBottomPadding() + textFieldPadding.calculateTopPadding())
        }
    OutlinedTextField(
        state = state,
        modifier =
            modifier
                .padding(bottom = bottomPadding)
                .semantics { if (errorText.isNotBlank()) error(description = errorText) }
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                ),
        label =
            if (label != null) {
                { label() }
            } else {
                null
            },
        placeholder = placeholder,
        supportingText = { if (errorText.isNotBlank()) Text(errorText) },
        isError = errorText.isNotBlank(),
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = colors.dateTextFieldColors,
    )

    LaunchedEffect(Unit) {
        // In case a focus is to be requested, delay the request to allow a smooth transition in
        // case the DateInput is in a dialog.
        if (focusRequester != null) {
            delay(MotionTokens.DurationMedium2.toLong())
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialDateMillis) {
        initialDateMillis?.let {
            val dateText =
                calendarModel.formatWithPattern(
                    it,
                    dateInputFormat.patternWithoutDelimiters,
                    locale,
                )
            if (state.text.toString() != dateText) {
                state.setTextAndPlaceCursorAtEnd(dateText)
            }
        }
    }
}

/**
 * A date input validator class.
 *
 * @param yearRange an [IntRange] that holds the year range that the date picker is be limited to
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed
 * @param dateInputFormat a [DateInputFormat] that holds date patterns information
 * @param dateFormatter a [DatePickerFormatter]
 * @param errorDatePattern a string for displaying an error message when an input does not match the
 *   expected date pattern. The string expects a date pattern string as an argument to be formatted
 *   into it.
 * @param errorDateOutOfYearRange a string for displaying an error message when an input date
 *   exceeds the year-range defined at the DateInput's state. The string expects a start and end
 *   year as arguments to be formatted into it.
 * @param errorInvalidNotAllowed a string for displaying an error message when an input date does
 *   not pass the DateInput's validator check. The string expects a date argument to be formatted
 *   into it.
 * @param errorInvalidRangeInput a string for displaying an error message when in a range input mode
 *   and one of the input dates is out of order (i.e. the user inputs a start date that is after the
 *   end date, or an end date that is before the start date)
 */
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
) {
    /**
     * the currently selected start date in milliseconds. Only checked against when the
     * [InputIdentifier] is [InputIdentifier.EndDateInput].
     */
    var currentStartDateMillis: Long? = null

    /**
     * the currently selected end date in milliseconds. Only checked against when the
     * [InputIdentifier] is [InputIdentifier.StartDateInput].
     */
    var currentEndDateMillis: Long? = null

    /**
     * Validates a [CalendarDate] input and returns an error string in case an issue with the given
     * date is detected, or an empty string in case there are no issues.
     *
     * @param dateToValidate a [CalendarDate] input to validate
     * @param inputIdentifier an [InputIdentifier] that provides information about the input field
     *   that is supposed to hold the date.
     * @param locale the current [CalendarLocale]
     */
    fun validate(
        dateToValidate: CalendarDate?,
        inputIdentifier: InputIdentifier,
        locale: CalendarLocale,
    ): String {
        if (dateToValidate == null) {
            return formatString(errorDatePattern, dateInputFormat.patternWithDelimiters.uppercase())
        }
        // Check that the date is within the valid range of years.
        if (!yearRange.contains(dateToValidate.year)) {
            return formatString(
                errorDateOutOfYearRange,
                yearRange.first.toLocalString(locale = locale),
                yearRange.last.toLocalString(locale = locale),
            )
        }
        // Check that the provided SelectableDates allows this date to be selected.
        with(selectableDates) {
            if (
                !isSelectableYear(dateToValidate.year) ||
                    !isSelectableDate(dateToValidate.utcTimeMillis)
            ) {
                return formatString(
                    errorInvalidNotAllowed,
                    dateFormatter.formatDate(
                        dateMillis = dateToValidate.utcTimeMillis,
                        locale = locale,
                    ),
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

/**
 * Represents different input identifiers for the [DateInputTextField]. An `InputIdentifier` is used
 * when validating the user input, and especially when validating an input range.
 */
@Immutable
@JvmInline
internal value class InputIdentifier internal constructor(internal val value: Int) {

    companion object {
        /** Single date input */
        val SingleDateInput
            get() = InputIdentifier(0)

        /** A start date input */
        val StartDateInput
            get() = InputIdentifier(1)

        /** An end date input */
        val EndDateInput
            get() = InputIdentifier(2)
    }

    override fun toString() =
        when (this) {
            SingleDateInput -> "SingleDateInput"
            StartDateInput -> "StartDateInput"
            EndDateInput -> "EndDateInput"
            else -> "Unknown"
        }
}

/**
 * An [InputTransformation] for date input. Rejects input changes that contain non-digit characters
 * or exceed the date format pattern length, and triggers date selection validation callback.
 */
private class DateInputTransformation(
    private val dateInputFormat: DateInputFormat,
    private val calendarModel: CalendarModel,
    private val dateInputValidator: DateInputValidator,
    private val inputIdentifier: InputIdentifier,
    private val locale: CalendarLocale,
    private val onDateSelectionChange: (Long?) -> Unit,
) : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        if (
            length > dateInputFormat.patternWithoutDelimiters.length ||
                !asCharSequence().all { it.isDigit() }
        ) {
            revertAllChanges()
            return
        }

        val text = asCharSequence().trim()
        if (text.length < dateInputFormat.patternWithoutDelimiters.length) {
            onDateSelectionChange(null)
        } else {
            val parsedDate =
                calendarModel.parse(
                    date = text.toString(),
                    pattern = dateInputFormat.patternWithoutDelimiters,
                    locale = locale,
                )
            val error =
                dateInputValidator.validate(
                    dateToValidate = parsedDate,
                    inputIdentifier = inputIdentifier,
                    locale = locale,
                )
            onDateSelectionChange(if (error.isEmpty()) parsedDate?.utcTimeMillis else null)
        }
    }
}

/**
 * An [OutputTransformation] for date input. The transformation will automatically display the date
 * delimiters provided by the [DateInputFormat] as the date is being entered into the text field.
 */
private class DateOutputTransformation(private val dateInputFormat: DateInputFormat) :
    OutputTransformation {

    private val firstDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.indexOf(dateInputFormat.delimiter)
    private val secondDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.lastIndexOf(dateInputFormat.delimiter)
    private val delimiterString: String = dateInputFormat.delimiter.toString()

    override fun TextFieldBuffer.transformOutput() {
        if (firstDelimiterOffset in 0..length) {
            insert(firstDelimiterOffset, delimiterString)
        }
        if (secondDelimiterOffset > firstDelimiterOffset && secondDelimiterOffset in 0..length) {
            insert(secondDelimiterOffset, delimiterString)
        }
    }
}

internal val InputTextFieldPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp)

// An optional padding that will only be added to the bottom of the date input text field when it's
// not showing an error message.
private val InputTextNonErroneousBottomPadding
    get() = 16.dp
