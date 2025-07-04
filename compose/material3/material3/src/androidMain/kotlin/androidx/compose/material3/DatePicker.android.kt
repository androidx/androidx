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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun formatDatePickerNavigateToYearString(
    template: String,
    localizedYear: String,
): String = template.format(localizedYear)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun formatHeadlineDescription(
    template: String,
    verboseDateDescription: String,
): String = template.format(verboseDateDescription)

/**
 * Creates a [DatePickerState] for a [DatePicker] that is remembered across compositions.
 *
 * To create a date picker state outside composition, see the `DatePickerState` function.
 *
 * @sample androidx.compose.material3.samples.DatePickerApi26Sample
 * @param initialSelectedDate a [LocalDate] that represents an initial selection of a date. Provide
 *   a `null` to indicate no selection.
 * @param initialDisplayedMonth an optional [YearMonth] that represents an initial selection of a
 *   month to be displayed to the user. By default, in case an [initialSelectedDate] is provided,
 *   the initial displayed month would be the month of the selected date. Otherwise, in case `null`
 *   is provided, the displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@ExperimentalMaterial3Api
fun rememberDatePickerState(
    initialSelectedDate: LocalDate?,
    initialDisplayedMonth: YearMonth? = initialSelectedDate?.let { YearMonth.from(it) },
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DatePickerState {
    val initialSelectedDateMillis = initialSelectedDate?.let { getLocalDateMillisUtc(it) }
    val initialDisplayedMonthMillis = initialDisplayedMonth?.let { getYearMonthMillisUtc(it) }
    return rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
    )
}

/**
 * Creates a [DatePickerState].
 *
 * For most cases, you are advised to use the [rememberDatePickerState] when in a composition.
 *
 * Note that in case you provide a [locale] that is different than the default platform locale, you
 * may need to ensure that the picker's title and headline are localized correctly. The following
 * sample shows one possible way of doing so by applying a local composition of a `LocalContext` and
 * `LocaleConfiguration`.
 *
 * @sample androidx.compose.material3.samples.DatePickerCustomLocaleApi26Sample
 * @param locale the [CalendarLocale] that will be used when formatting dates, determining the input
 *   format, displaying the week-day, determining the first day of the week, and more. Note that in
 *   case the provided [CalendarLocale] differs from the platform's default Locale, you may need to
 *   ensure that the picker's title and headline are localized correctly, and in some cases, you may
 *   need to apply an RTL layout.
 * @param initialSelectedDate a [LocalDate] that represents an initial selection of a date. Provide
 *   a `null` to indicate no selection.
 * @param initialDisplayedMonth an optional [YearMonth] that represents an initial selection of a
 *   month to be displayed to the user. In case `null` is provided, the displayed month would be the
 *   current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent a
 *   year that is out of the year range.
 * @see rememberDatePickerState
 */
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterial3Api
fun DatePickerState(
    locale: CalendarLocale,
    initialSelectedDate: LocalDate?,
    initialDisplayedMonth: YearMonth? = initialSelectedDate?.let { YearMonth.from(it) },
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DatePickerState =
    DatePickerState(
        locale = locale,
        initialSelectedDateMillis = getLocalDateMillisUtc(initialSelectedDate),
        initialDisplayedMonthMillis = initialDisplayedMonth?.let { getYearMonthMillisUtc(it) },
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
    )

/**
 * Creates a [DateRangePickerState] for a [DateRangePicker] that is remembered across compositions.
 *
 * To create a date range picker state outside composition, see the `DateRangePickerState` function.
 *
 * @sample androidx.compose.material3.samples.DateRangePickerApi26Sample
 * @param initialSelectedStartDate a [LocalDate] that represents an initial selection of a start
 *   date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDate a [LocalDate] that represents an initial selection of an end date.
 *   Provide a `null` to indicate no selection.
 * @param initialDisplayedMonth an optional [YearMonth] that represents an initial selection of a
 *   month to be displayed to the user. By default, in case an [initialSelectedStartDate] is
 *   provided, the initial displayed month would be the month of the selected date. Otherwise, in
 *   case `null` is provided, the displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date range picker will be
 *   limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@ExperimentalMaterial3Api
fun rememberDateRangePickerState(
    initialSelectedStartDate: LocalDate?,
    initialSelectedEndDate: LocalDate? = null,
    initialDisplayedMonth: YearMonth? = initialSelectedStartDate?.let { YearMonth.from(it) },
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DateRangePickerState {
    val initialSelectedStartDateMillis = initialSelectedStartDate?.let { getLocalDateMillisUtc(it) }
    val initialSelectedEndDateMillis = initialSelectedEndDate?.let { getLocalDateMillisUtc(it) }
    val initialDisplayedMonthMillis = initialDisplayedMonth?.let { getYearMonthMillisUtc(it) }
    return rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
    )
}

/**
 * Creates a [DateRangePickerState].
 *
 * For most cases, you are advised to use the [rememberDateRangePickerState] when in a composition.
 *
 * Note that in case you provide a [locale] that is different than the default platform locale, you
 * may need to ensure that the picker's title and headline are localized correctly. The following
 * sample shows one possible way of doing so by applying a local composition of a `LocalContext` and
 * `LocaleConfiguration`.
 *
 * @sample androidx.compose.material3.samples.DatePickerCustomLocaleApi26Sample
 * @param locale the [CalendarLocale] that will be used when formatting dates, determining the input
 *   format, displaying the week-day, determining the first day of the week, and more. Note that in
 *   case the provided [CalendarLocale] differs from the platform's default Locale, you may need to
 *   ensure that the picker's title and headline are localized correctly, and in some cases, you may
 *   need to apply an RTL layout.
 * @param initialSelectedStartDate a [LocalDate] that represents an initial selection of a start
 *   date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDate a [LocalDate] that represents an initial selection of an end date.
 *   Provide a `null` to indicate no selection.
 * @param initialDisplayedMonth an optional [YearMonth] that represents an initial selection of a
 *   month to be displayed to the user. By default, in case an [initialSelectedStartDate] is
 *   provided, the initial displayed month would be the month of the selected date. Otherwise, in
 *   case `null` is provided, the displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI
 * @throws IllegalArgumentException if the initial timestamps do not fall within the year range this
 *   state is created with, or the end date precedes the start date, or when an end date is provided
 *   without a start date (e.g. the start date was null, while the end date was not).
 * @see rememberDateRangePickerState
 */
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterial3Api
fun DateRangePickerState(
    locale: CalendarLocale,
    initialSelectedStartDate: LocalDate?,
    initialSelectedEndDate: LocalDate? = null,
    initialDisplayedMonth: YearMonth? = initialSelectedStartDate?.let { YearMonth.from(it) },
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DateRangePickerState =
    DateRangePickerState(
        locale = locale,
        initialSelectedStartDateMillis = getLocalDateMillisUtc(initialSelectedStartDate),
        initialSelectedEndDateMillis = getLocalDateMillisUtc(initialSelectedEndDate),
        initialDisplayedMonthMillis = initialDisplayedMonth?.let { getYearMonthMillisUtc(it) },
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
    )

/**
 * Sets the [DatePickerState.selectedDateMillis] based on a given [LocalDate].
 *
 * Converts the [LocalDate] to the start of the day (midnight) in UTC and apply it to the
 * [DatePickerState] `selectedDateMillis`. Setting `null` clears the selection.
 *
 * @param date The [LocalDate] to select, or `null` to clear the selection.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerState.setSelectedDate(date: LocalDate?) {
    // The selectedDateMillis setter handles the yearRange validation.
    this.selectedDateMillis = getLocalDateMillisUtc(date)
}

/**
 * Returns a [LocalDate] representation of the selected date in this [DatePickerState], or `null` in
 * case there is no selection.
 *
 * @return The selected [LocalDate], or `null` if there is no selection.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerState.getSelectedDate(): LocalDate? {
    return getLocalDate(this.selectedDateMillis)
}

/**
 * Sets the [DatePickerState.displayedMonthMillis] based on a given [YearMonth].
 *
 * Converts the [YearMonth] to the start of the first day of that month (midnight) in UTC and apply
 * it to the [DatePickerState] `displayedMonthMillis`.
 *
 * @param yearMonth The [YearMonth] to display.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerState.setDisplayedMonth(yearMonth: YearMonth) {
    // The displayedMonthMillis setter handles the yearRange validation.
    this.displayedMonthMillis = getYearMonthMillisUtc(yearMonth)
}

/**
 * Returns a [YearMonth] representation of the displayed month in this [DatePickerState]. The
 * returned [YearMonth] is based on the [DatePickerState.displayedMonthMillis], which represents
 * midnight of the first day of the displayed month in UTC milliseconds from the epoch.
 *
 * @return The displayed [YearMonth].
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerState.getDisplayedMonth(): YearMonth? =
    getYearMonth(millisUtcFirstOfMonth = this.displayedMonthMillis)

/**
 * Sets the [DateRangePickerState] start and end dates selection.
 *
 * The provided [LocalDate]s are converted to the start of the day (midnight) in UTC and applied to
 * the [DateRangePickerState].
 *
 * The function expects the dates to be within the state's year-range, and for the start date to
 * appear before, or be equal, the end date. Also, if an end date is provided (e.g. not `null`), a
 * start date is also expected to be provided. In any other case, an [IllegalArgumentException] is
 * thrown.
 *
 * @param startDate a [LocalDate] that represents the start date selection. Provide a `null` to
 *   indicate no selection.
 * @param endDate a [LocalDate] that represents the end date selection. Provide a `null` to indicate
 *   no selection.
 * @throws IllegalArgumentException in case the given [LocalDate]s do not comply with the expected
 *   values specified above.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DateRangePickerState.setSelection(startDate: LocalDate?, endDate: LocalDate?) {
    this.setSelection(getLocalDateMillisUtc(startDate), getLocalDateMillisUtc(endDate))
}

/**
 * Returns a [LocalDate] representation of the selected start date in this [DateRangePickerState],
 * or `null` in case there is no selection.
 *
 * @return The selected start [LocalDate], or `null` if there is no selection.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DateRangePickerState.getSelectedStartDate(): LocalDate? {
    return getLocalDate(this.selectedStartDateMillis)
}

/**
 * Returns a [LocalDate] representation of the selected end date in this [DateRangePickerState], or
 * `null` in case there is no selection.
 *
 * @return The selected end [LocalDate], or `null` if there is no selection.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DateRangePickerState.getSelectedEndDate(): LocalDate? {
    return getLocalDate(this.selectedEndDateMillis)
}

/**
 * Sets the [DateRangePickerState.displayedMonthMillis] based on a given [YearMonth].
 *
 * Converts the [YearMonth] to the start of the first day of that month (midnight) in UTC and apply
 * it to the [DateRangePickerState] `displayedMonthMillis`.
 *
 * @param yearMonth The [YearMonth] to display.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DateRangePickerState.setDisplayedMonth(yearMonth: YearMonth) {
    // The displayedMonthMillis setter handles the yearRange validation.
    this.displayedMonthMillis = getYearMonthMillisUtc(yearMonth)
}

/**
 * Returns a [YearMonth] representation of the displayed month in this [DateRangePickerState]. The
 * returned [YearMonth] is based on the [DateRangePickerState.displayedMonthMillis], which
 * represents midnight of the first day of the displayed month in UTC milliseconds from the epoch.
 *
 * @return The displayed [YearMonth].
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun DateRangePickerState.getDisplayedMonth(): YearMonth? =
    getYearMonth(millisUtcFirstOfMonth = this.displayedMonthMillis)

/** Returns a [YearMonth] representation of the given [millisUtcFirstOfMonth] UTC milliseconds. */
@RequiresApi(Build.VERSION_CODES.O)
private fun getYearMonth(millisUtcFirstOfMonth: Long): YearMonth {
    // Convert the UTC epoch milliseconds to an Instant object. Since the Instant represents
    // midnight UTC we can get the correct calendar date components by interpreting this
    // Instant in the UTC time zone and return a YearMonth.
    val zonedDateTimeUtc = Instant.ofEpochMilli(millisUtcFirstOfMonth).atZone(ZoneOffset.UTC)
    return YearMonth.from(zonedDateTimeUtc)
}

/**
 * Returns a [YearMonth] in UTC milliseconds from the epoch.
 *
 * The returned time if for the first day of the month in midnight.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun getYearMonthMillisUtc(yearMonth: YearMonth): Long {
    // Get the start of the first day of the month (midnight).
    val firstDayOfMonth = yearMonth.atDay(1)
    val localDateTimeAtStart = firstDayOfMonth.atStartOfDay()
    // Interpret that LocalDateTime as being in UTC and set the state property.
    return localDateTimeAtStart.toInstant(ZoneOffset.UTC).toEpochMilli()
}

/**
 * Returns a [LocalDate] representation of the given [millisUtc] UTC milliseconds. If [millisUtc] is
 * `null`, returns `null`.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun getLocalDate(millisUtc: Long?): LocalDate? {
    if (millisUtc == null) return null
    // Convert the UTC epoch milliseconds to an Instant object. Since the Instant represents
    // midnight UTC we can get the correct calendar date components by interpreting this Instant
    // in the UTC time zone and return a LocalDate.
    return Instant.ofEpochMilli(millisUtc).atZone(ZoneOffset.UTC).toLocalDate()
}

/** Returns a [LocalDate] in UTC milliseconds from the epoch. */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
private fun getLocalDateMillisUtc(date: LocalDate?): Long? {
    return if (date == null) {
        null
    } else {
        // Get the start of the day (midnight) for the given LocalDate.
        val localDateTimeAtStart = date.atStartOfDay()
        // Interpret that LocalDateTime as being in UTC and set the state property.
        // The selectedDateMillis setter handles the yearRange validation.
        localDateTimeAtStart.toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
