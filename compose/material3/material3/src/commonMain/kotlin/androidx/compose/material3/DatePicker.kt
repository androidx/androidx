/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextFieldDefaults.defaultOutlinedTextFieldColors
import androidx.compose.material3.internal.CalendarModel
import androidx.compose.material3.internal.CalendarMonth
import androidx.compose.material3.internal.DaysInWeek
import androidx.compose.material3.internal.Icons
import androidx.compose.material3.internal.MillisecondsIn24Hours
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.createCalendarModel
import androidx.compose.material3.internal.formatWithSkeleton
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.material3.tokens.DividerTokens
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external"
 * target="_blank">Material Design date picker</a>.
 *
 * Date pickers let people select a date and preferably should be embedded into Dialogs. See
 * [DatePickerDialog].
 *
 * By default, a date picker lets you pick a date via a calendar UI. However, it also allows
 * switching into a date input mode for a manual entry of dates using the numbers on a keyboard.
 *
 * ![Date picker
 * image](https://developer.android.com/images/reference/androidx/compose/material3/date-picker.png)
 *
 * A simple DatePicker looks like:
 *
 * @sample androidx.compose.material3.samples.DatePickerSample
 *
 * A DatePicker with an initial UI of a date input mode looks like:
 *
 * @sample androidx.compose.material3.samples.DateInputSample
 *
 * A DatePicker with a provided [SelectableDates] that blocks certain days from being selected looks
 * like:
 *
 * @sample androidx.compose.material3.samples.DatePickerWithDateSelectableDatesSample
 * @param state state of the date picker. See [rememberDatePickerState].
 * @param modifier the [Modifier] to be applied to this date picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param title the title to be displayed in the date picker
 * @param headline the headline to be displayed in the date picker
 * @param showModeToggle indicates if this DatePicker should show a mode toggle action that
 *   transforms it into a date input
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 *   picker in different states. See [DatePickerDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    title: (@Composable () -> Unit)? = {
        DatePickerDefaults.DatePickerTitle(
            displayMode = state.displayMode,
            modifier = Modifier.padding(DatePickerTitlePadding)
        )
    },
    headline: (@Composable () -> Unit)? = {
        DatePickerDefaults.DatePickerHeadline(
            selectedDateMillis = state.selectedDateMillis,
            displayMode = state.displayMode,
            dateFormatter = dateFormatter,
            modifier = Modifier.padding(DatePickerHeadlinePadding)
        )
    },
    showModeToggle: Boolean = true,
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    val defaultLocale = defaultLocale()
    val calendarModel = remember(defaultLocale) { createCalendarModel(defaultLocale) }
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        modeToggleButton =
            if (showModeToggle) {
                {
                    DisplayModeToggleButton(
                        modifier = Modifier.padding(DatePickerModeTogglePadding),
                        displayMode = state.displayMode,
                        onDisplayModeChange = { displayMode -> state.displayMode = displayMode },
                    )
                }
            } else {
                null
            },
        headlineTextStyle = DatePickerModalTokens.HeaderHeadlineFont.value,
        headerMinHeight = DatePickerModalTokens.HeaderContainerHeight,
        colors = colors,
    ) {
        SwitchableDateEntryContent(
            selectedDateMillis = state.selectedDateMillis,
            displayedMonthMillis = state.displayedMonthMillis,
            displayMode = state.displayMode,
            onDateSelectionChange = { dateInMillis -> state.selectedDateMillis = dateInMillis },
            onDisplayedMonthChange = { monthInMillis ->
                state.displayedMonthMillis = monthInMillis
            },
            calendarModel = calendarModel,
            yearRange = state.yearRange,
            dateFormatter = dateFormatter,
            selectableDates = state.selectableDates,
            colors = colors
        )
    }
}

/**
 * A state object that can be hoisted to observe the date picker state. See
 * [rememberDatePickerState].
 */
@ExperimentalMaterial3Api
@Stable
interface DatePickerState {

    /**
     * A timestamp that represents the selected date _start_ of the day in _UTC_ milliseconds from
     * the epoch.
     *
     * @throws IllegalArgumentException in case the value is set with a timestamp that does not fall
     *   within the [yearRange].
     */
    @get:Suppress("AutoBoxing") var selectedDateMillis: Long?

    /**
     * A timestamp that represents the currently displayed month _start_ date in _UTC_ milliseconds
     * from the epoch.
     *
     * @throws IllegalArgumentException in case the value is set with a timestamp that does not fall
     *   within the [yearRange].
     */
    var displayedMonthMillis: Long

    /** A [DisplayMode] that represents the current UI mode (i.e. picker or input). */
    var displayMode: DisplayMode

    /** An [IntRange] that holds the year range that the date picker will be limited to. */
    val yearRange: IntRange

    /**
     * A [SelectableDates] that is consulted to check if a date is allowed.
     *
     * In case a date is not allowed to be selected, it will appear disabled in the UI.
     */
    val selectableDates: SelectableDates
}

/** An interface that controls the selectable dates and years in the date pickers UI. */
@ExperimentalMaterial3Api
@Stable
interface SelectableDates {

    /**
     * Returns true if the date item representing the [utcTimeMillis] should be enabled for
     * selection in the UI.
     */
    fun isSelectableDate(utcTimeMillis: Long) = true

    /**
     * Returns true if a given [year] should be enabled for selection in the UI. When a year is
     * defined as non selectable, all the dates in that year will also be non selectable.
     */
    fun isSelectableYear(year: Int) = true
}

/** A date formatter interface used by [DatePicker]. */
@ExperimentalMaterial3Api
interface DatePickerFormatter {

    /**
     * Format a given [monthMillis] to a string representation of the month and the year (i.e.
     * January 2023).
     *
     * @param monthMillis timestamp in _UTC_ milliseconds from the epoch that represents the month
     * @param locale a [CalendarLocale] to use when formatting the month and year
     * @see defaultLocale
     */
    fun formatMonthYear(@Suppress("AutoBoxing") monthMillis: Long?, locale: CalendarLocale): String?

    /**
     * Format a given [dateMillis] to a string representation of the date (i.e. Mar 27, 2021).
     *
     * @param dateMillis timestamp in _UTC_ milliseconds from the epoch that represents the date
     * @param locale a [CalendarLocale] to use when formatting the date
     * @param forContentDescription indicates that the requested formatting is for content
     *   description. In these cases, the output may include a more descriptive wording that will be
     *   passed to a screen readers.
     * @see defaultLocale
     */
    fun formatDate(
        @Suppress("AutoBoxing") dateMillis: Long?,
        locale: CalendarLocale,
        forContentDescription: Boolean = false
    ): String?
}

/** Represents the different modes that a date picker can be at. */
@Immutable
@JvmInline
@ExperimentalMaterial3Api
value class DisplayMode internal constructor(internal val value: Int) {

    companion object {
        /** Date picker mode */
        val Picker = DisplayMode(0)

        /** Date text input mode */
        val Input = DisplayMode(1)
    }

    override fun toString() =
        when (this) {
            Picker -> "Picker"
            Input -> "Input"
            else -> "Unknown"
        }
}

/**
 * Creates a [DatePickerState] for a [DatePicker] that is remembered across compositions.
 *
 * To create a date picker state outside composition, see the `DatePickerState` function.
 *
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. By default, in case an
 *   `initialSelectedDateMillis` is provided, the initial displayed month would be the month of the
 *   selected date. Otherwise, in case `null` is provided, the displayed month would be the current
 *   one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDatePickerState(
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
): DatePickerState {
    val locale = defaultLocale()
    return rememberSaveable(saver = DatePickerStateImpl.Saver(selectableDates, locale)) {
            DatePickerStateImpl(
                initialSelectedDateMillis = initialSelectedDateMillis,
                initialDisplayedMonthMillis = initialDisplayedMonthMillis,
                yearRange = yearRange,
                initialDisplayMode = initialDisplayMode,
                selectableDates = selectableDates,
                locale = locale
            )
        }
        .apply {
            // Update the state's selectable dates if they were changed.
            this.selectableDates = selectableDates
        }
}

/**
 * Creates a [DatePickerState].
 *
 * Note that in most cases, you are advised to use the [rememberDatePickerState] when in a
 * composition.
 *
 * @param locale a [CalendarLocale] to be used when formatting dates, determining the input format,
 *   and more
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a date. Provide a `null` to indicate no selection. Note that the
 *   state's [DatePickerState.selectedDateMillis] will provide a timestamp that represents the
 *   _start_ of the day, which may be different than the provided initialSelectedDateMillis.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. In case `null` is provided, the
 *   displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent a
 *   year that is out of the year range.
 * @see rememberDatePickerState
 */
@ExperimentalMaterial3Api
fun DatePickerState(
    locale: CalendarLocale,
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
): DatePickerState =
    DatePickerStateImpl(
        initialSelectedDateMillis = initialSelectedDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates,
        locale = locale
    )

/** Contains default values used by the [DatePicker]. */
@ExperimentalMaterial3Api
@Stable
object DatePickerDefaults {

    /**
     * Creates a [DatePickerColors] that will potentially animate between the provided colors
     * according to the Material specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultDatePickerColors

    /**
     * Creates a [DatePickerColors] that will potentially animate between the provided colors
     * according to the Material specification.
     *
     * @param containerColor the color used for the date picker's background
     * @param titleContentColor the color used for the date picker's title
     * @param headlineContentColor the color used for the date picker's headline
     * @param weekdayContentColor the color used for the weekday letters
     * @param subheadContentColor the color used for the month and year subhead labels that appear
     *   when months are displayed at a `DateRangePicker`.
     * @param navigationContentColor the content color used for the year selection menu button and
     *   the months arrow navigation when displayed at a `DatePicker`.
     * @param yearContentColor the color used for a year item content
     * @param disabledYearContentColor the color used for a disabled year item content
     * @param currentYearContentColor the color used for the current year content when selecting a
     *   year
     * @param selectedYearContentColor the color used for a selected year item content
     * @param disabledSelectedYearContentColor the color used for a disabled selected year item
     *   content
     * @param selectedYearContainerColor the color used for a selected year item container
     * @param disabledSelectedYearContainerColor the color used for a disabled selected year item
     *   container
     * @param dayContentColor the color used for days content
     * @param disabledDayContentColor the color used for disabled days content
     * @param selectedDayContentColor the color used for selected days content
     * @param disabledSelectedDayContentColor the color used for disabled selected days content
     * @param selectedDayContainerColor the color used for a selected day container
     * @param disabledSelectedDayContainerColor the color used for a disabled selected day container
     * @param todayContentColor the color used for the day that marks the current date
     * @param todayDateBorderColor the color used for the border of the day that marks the current
     *   date
     * @param dayInSelectionRangeContentColor the content color used for days that are within a date
     *   range selection
     * @param dayInSelectionRangeContainerColor the container color used for days that are within a
     *   date range selection
     * @param dividerColor the color used for the dividers used at the date pickers
     * @param dateTextFieldColors the [TextFieldColors] defaults for the date text field when in
     *   [DisplayMode.Input]. See [OutlinedTextFieldDefaults.colors].
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        headlineContentColor: Color = Color.Unspecified,
        weekdayContentColor: Color = Color.Unspecified,
        subheadContentColor: Color = Color.Unspecified,
        navigationContentColor: Color = Color.Unspecified,
        yearContentColor: Color = Color.Unspecified,
        disabledYearContentColor: Color = Color.Unspecified,
        currentYearContentColor: Color = Color.Unspecified,
        selectedYearContentColor: Color = Color.Unspecified,
        disabledSelectedYearContentColor: Color = Color.Unspecified,
        selectedYearContainerColor: Color = Color.Unspecified,
        disabledSelectedYearContainerColor: Color = Color.Unspecified,
        dayContentColor: Color = Color.Unspecified,
        disabledDayContentColor: Color = Color.Unspecified,
        selectedDayContentColor: Color = Color.Unspecified,
        disabledSelectedDayContentColor: Color = Color.Unspecified,
        selectedDayContainerColor: Color = Color.Unspecified,
        disabledSelectedDayContainerColor: Color = Color.Unspecified,
        todayContentColor: Color = Color.Unspecified,
        todayDateBorderColor: Color = Color.Unspecified,
        dayInSelectionRangeContentColor: Color = Color.Unspecified,
        dayInSelectionRangeContainerColor: Color = Color.Unspecified,
        dividerColor: Color = Color.Unspecified,
        dateTextFieldColors: TextFieldColors? = null
    ): DatePickerColors =
        MaterialTheme.colorScheme.defaultDatePickerColors.copy(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            headlineContentColor = headlineContentColor,
            weekdayContentColor = weekdayContentColor,
            subheadContentColor = subheadContentColor,
            navigationContentColor = navigationContentColor,
            yearContentColor = yearContentColor,
            disabledYearContentColor = disabledYearContentColor,
            currentYearContentColor = currentYearContentColor,
            selectedYearContentColor = selectedYearContentColor,
            disabledSelectedYearContentColor = disabledSelectedYearContentColor,
            selectedYearContainerColor = selectedYearContainerColor,
            disabledSelectedYearContainerColor = disabledSelectedYearContainerColor,
            dayContentColor = dayContentColor,
            disabledDayContentColor = disabledDayContentColor,
            selectedDayContentColor = selectedDayContentColor,
            disabledSelectedDayContentColor = disabledSelectedDayContentColor,
            selectedDayContainerColor = selectedDayContainerColor,
            disabledSelectedDayContainerColor = disabledSelectedDayContainerColor,
            todayContentColor = todayContentColor,
            todayDateBorderColor = todayDateBorderColor,
            dayInSelectionRangeContentColor = dayInSelectionRangeContentColor,
            dayInSelectionRangeContainerColor = dayInSelectionRangeContainerColor,
            dividerColor = dividerColor,
            dateTextFieldColors = dateTextFieldColors
        )

    internal val ColorScheme.defaultDatePickerColors: DatePickerColors
        @Composable
        get() {
            return defaultDatePickerColorsCached
                ?: DatePickerColors(
                        containerColor = fromToken(DatePickerModalTokens.ContainerColor),
                        titleContentColor =
                            fromToken(DatePickerModalTokens.HeaderSupportingTextColor),
                        headlineContentColor = fromToken(DatePickerModalTokens.HeaderHeadlineColor),
                        weekdayContentColor =
                            fromToken(DatePickerModalTokens.WeekdaysLabelTextColor),
                        subheadContentColor =
                            fromToken(DatePickerModalTokens.RangeSelectionMonthSubheadColor),
                        // TODO(b/234060211): Apply this from the MenuButton tokens or defaults.
                        navigationContentColor = onSurfaceVariant,
                        yearContentColor =
                            fromToken(DatePickerModalTokens.SelectionYearUnselectedLabelTextColor),
                        // TODO: Using DisabledAlpha as there are no token values for the disabled
                        // states.
                        disabledYearContentColor =
                            fromToken(DatePickerModalTokens.SelectionYearUnselectedLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                        currentYearContentColor =
                            fromToken(DatePickerModalTokens.DateTodayLabelTextColor),
                        selectedYearContentColor =
                            fromToken(DatePickerModalTokens.SelectionYearSelectedLabelTextColor),
                        disabledSelectedYearContentColor =
                            fromToken(DatePickerModalTokens.SelectionYearSelectedLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                        selectedYearContainerColor =
                            fromToken(DatePickerModalTokens.SelectionYearSelectedContainerColor),
                        disabledSelectedYearContainerColor =
                            fromToken(DatePickerModalTokens.SelectionYearSelectedContainerColor)
                                .copy(alpha = DisabledAlpha),
                        dayContentColor =
                            fromToken(DatePickerModalTokens.DateUnselectedLabelTextColor),
                        disabledDayContentColor =
                            fromToken(DatePickerModalTokens.DateUnselectedLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                        selectedDayContentColor =
                            fromToken(DatePickerModalTokens.DateSelectedLabelTextColor),
                        disabledSelectedDayContentColor =
                            fromToken(DatePickerModalTokens.DateSelectedLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                        selectedDayContainerColor =
                            fromToken(DatePickerModalTokens.DateSelectedContainerColor),
                        disabledSelectedDayContainerColor =
                            fromToken(DatePickerModalTokens.DateSelectedContainerColor)
                                .copy(alpha = DisabledAlpha),
                        todayContentColor =
                            fromToken(DatePickerModalTokens.DateTodayLabelTextColor),
                        todayDateBorderColor =
                            fromToken(DatePickerModalTokens.DateTodayContainerOutlineColor),
                        dayInSelectionRangeContentColor =
                            fromToken(DatePickerModalTokens.SelectionDateInRangeLabelTextColor),
                        dayInSelectionRangeContainerColor =
                            fromToken(
                                DatePickerModalTokens.RangeSelectionActiveIndicatorContainerColor
                            ),
                        dividerColor = fromToken(DividerTokens.Color),
                        dateTextFieldColors = defaultOutlinedTextFieldColors
                    )
                    .also { defaultDatePickerColorsCached = it }
        }

    /**
     * Returns a [DatePickerFormatter].
     *
     * The date formatter will apply the best possible localized form of the given skeleton and
     * Locale. A skeleton is similar to, and uses the same format characters as, a Unicode <a
     * href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> pattern.
     *
     * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
     * `en_US` locale, but "d. MMMM" in the `de_CH` locale.
     *
     * @param yearSelectionSkeleton a date format skeleton used to format the date picker's year
     *   selection menu button (e.g. "March 2021").
     * @param selectedDateSkeleton a date format skeleton used to format a selected date (e.g. "Mar
     *   27, 2021")
     * @param selectedDateDescriptionSkeleton a date format skeleton used to format a selected date
     *   to be used as content description for screen readers (e.g. "Saturday, March 27, 2021")
     */
    fun dateFormatter(
        yearSelectionSkeleton: String = YearMonthSkeleton,
        selectedDateSkeleton: String = YearAbbrMonthDaySkeleton,
        selectedDateDescriptionSkeleton: String = YearMonthWeekdayDaySkeleton
    ): DatePickerFormatter =
        DatePickerFormatterImpl(
            yearSelectionSkeleton = yearSelectionSkeleton,
            selectedDateSkeleton = selectedDateSkeleton,
            selectedDateDescriptionSkeleton = selectedDateDescriptionSkeleton
        )

    /**
     * A default date picker title composable.
     *
     * @param displayMode the current [DisplayMode]
     * @param modifier a [Modifier] to be applied for the title
     */
    @Composable
    fun DatePickerTitle(displayMode: DisplayMode, modifier: Modifier = Modifier) {
        when (displayMode) {
            DisplayMode.Picker ->
                Text(text = getString(string = Strings.DatePickerTitle), modifier = modifier)
            DisplayMode.Input ->
                Text(text = getString(string = Strings.DateInputTitle), modifier = modifier)
        }
    }

    /**
     * A default date picker headline composable that displays a default headline text when there is
     * no date selection, and an actual date string when there is.
     *
     * @param selectedDateMillis a timestamp that represents the selected date _start_ of the day in
     *   _UTC_ milliseconds from the epoch
     * @param displayMode the current [DisplayMode]
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     */
    @Composable
    fun DatePickerHeadline(
        @Suppress("AutoBoxing") selectedDateMillis: Long?,
        displayMode: DisplayMode,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier = Modifier
    ) {
        val defaultLocale = defaultLocale()
        val formattedDate =
            dateFormatter.formatDate(dateMillis = selectedDateMillis, locale = defaultLocale)
        val verboseDateDescription =
            dateFormatter.formatDate(
                dateMillis = selectedDateMillis,
                locale = defaultLocale,
                forContentDescription = true
            )
                ?: when (displayMode) {
                    DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                    DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                    else -> ""
                }

        val headlineText =
            formattedDate
                ?: when (displayMode) {
                    DisplayMode.Picker -> getString(Strings.DatePickerHeadline)
                    DisplayMode.Input -> getString(Strings.DateInputHeadline)
                    else -> ""
                }

        val headlineDescription =
            formatHeadlineDescription(
                when (displayMode) {
                    DisplayMode.Picker -> getString(Strings.DatePickerHeadlineDescription)
                    DisplayMode.Input -> getString(Strings.DateInputHeadlineDescription)
                    else -> ""
                },
                verboseDateDescription
            )

        Text(
            text = headlineText,
            modifier =
                modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = headlineDescription
                },
            maxLines = 1
        )
    }

    /**
     * Creates and remembers a [FlingBehavior] that will represent natural fling curve with snap to
     * the most visible month in the months list.
     *
     * @param lazyListState a [LazyListState]
     * @param decayAnimationSpec the decay to use
     */
    @Composable
    internal fun rememberSnapFlingBehavior(
        lazyListState: LazyListState,
        decayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()
    ): FlingBehavior {
        // TODO Load the motionScheme tokens from the component tokens file
        val animationSpec: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultEffects.value()
        return remember(decayAnimationSpec, lazyListState) {
            val original = SnapLayoutInfoProvider(lazyListState)
            val snapLayoutInfoProvider =
                object : SnapLayoutInfoProvider by original {
                    override fun calculateApproachOffset(
                        velocity: Float,
                        decayOffset: Float
                    ): Float = 0.0f
                }

            snapFlingBehavior(
                snapLayoutInfoProvider = snapLayoutInfoProvider,
                decayAnimationSpec = decayAnimationSpec,
                snapAnimationSpec = animationSpec,
            )
        }
    }

    /** The range of years for the date picker dialogs. */
    val YearRange: IntRange = IntRange(1900, 2100)

    /** The default tonal elevation used for [DatePickerDialog]. */
    val TonalElevation: Dp = ElevationTokens.Level0

    /** The default shape for date picker dialogs. */
    val shape: Shape
        @Composable get() = DatePickerModalTokens.ContainerShape.value

    /** A default [SelectableDates] that allows all dates to be selected. */
    val AllDates: SelectableDates = object : SelectableDates {}

    /**
     * A date format skeleton used to format the date picker's year selection menu button (e.g.
     * "March 2021")
     */
    const val YearMonthSkeleton: String = "yMMMM"

    /** A date format skeleton used to format a selected date (e.g. "Mar 27, 2021") */
    const val YearAbbrMonthDaySkeleton: String = "yMMMd"

    /**
     * A date format skeleton used to format a selected date to be used as content description for
     * screen readers (e.g. "Saturday, March 27, 2021")
     */
    const val YearMonthWeekdayDaySkeleton: String = "yMMMMEEEEd"
}

internal expect inline fun formatHeadlineDescription(
    template: String,
    verboseDateDescription: String
): String

/**
 * Represents the colors used by the date picker.
 *
 * @param containerColor the color used for the date picker's background
 * @param titleContentColor the color used for the date picker's title
 * @param headlineContentColor the color used for the date picker's headline
 * @param weekdayContentColor the color used for the weekday letters
 * @param subheadContentColor the color used for the month and year subhead labels that appear when
 *   months are displayed at a `DateRangePicker`.
 * @param navigationContentColor the content color used for the year selection menu button and the
 *   months arrow navigation when displayed at a `DatePicker`.
 * @param yearContentColor the color used for a year item content
 * @param disabledYearContentColor the color used for a disabled year item content
 * @param currentYearContentColor the color used for the current year content when selecting a year
 * @param selectedYearContentColor the color used for a selected year item content
 * @param disabledSelectedYearContentColor the color used for a disabled selected year item content
 * @param selectedYearContainerColor the color used for a selected year item container
 * @param disabledSelectedYearContainerColor the color used for a disabled selected year item
 *   container
 * @param dayContentColor the color used for days content
 * @param disabledDayContentColor the color used for disabled days content
 * @param selectedDayContentColor the color used for selected days content
 * @param disabledSelectedDayContentColor the color used for disabled selected days content
 * @param selectedDayContainerColor the color used for a selected day container
 * @param disabledSelectedDayContainerColor the color used for a disabled selected day container
 * @param todayContentColor the color used for the day that marks the current date
 * @param todayDateBorderColor the color used for the border of the day that marks the current date
 * @param dayInSelectionRangeContentColor the content color used for days that are within a date
 *   range selection
 * @param dayInSelectionRangeContainerColor the container color used for days that are within a date
 *   range selection
 * @param dividerColor the color used for the dividers used at the date pickers
 * @param dateTextFieldColors the [TextFieldColors] defaults for the date text field when in
 *   [DisplayMode.Input]. See [OutlinedTextFieldDefaults.colors].
 * @constructor create an instance with arbitrary colors, see [DatePickerDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@ExperimentalMaterial3Api
@Immutable
class DatePickerColors
constructor(
    val containerColor: Color,
    val titleContentColor: Color,
    val headlineContentColor: Color,
    val weekdayContentColor: Color,
    val subheadContentColor: Color,
    val navigationContentColor: Color,
    val yearContentColor: Color,
    val disabledYearContentColor: Color,
    val currentYearContentColor: Color,
    val selectedYearContentColor: Color,
    val disabledSelectedYearContentColor: Color,
    val selectedYearContainerColor: Color,
    val disabledSelectedYearContainerColor: Color,
    val dayContentColor: Color,
    val disabledDayContentColor: Color,
    val selectedDayContentColor: Color,
    val disabledSelectedDayContentColor: Color,
    val selectedDayContainerColor: Color,
    val disabledSelectedDayContainerColor: Color,
    val todayContentColor: Color,
    val todayDateBorderColor: Color,
    val dayInSelectionRangeContainerColor: Color,
    val dayInSelectionRangeContentColor: Color,
    val dividerColor: Color,
    val dateTextFieldColors: TextFieldColors
) {
    /**
     * Returns a copy of this DatePickerColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source” // For `dateTextFieldColors`
     * use null to mean "use the value from source"
     */
    fun copy(
        containerColor: Color = this.containerColor,
        titleContentColor: Color = this.titleContentColor,
        headlineContentColor: Color = this.headlineContentColor,
        weekdayContentColor: Color = this.weekdayContentColor,
        subheadContentColor: Color = this.subheadContentColor,
        navigationContentColor: Color = this.navigationContentColor,
        yearContentColor: Color = this.yearContentColor,
        disabledYearContentColor: Color = this.disabledYearContentColor,
        currentYearContentColor: Color = this.currentYearContentColor,
        selectedYearContentColor: Color = this.selectedYearContentColor,
        disabledSelectedYearContentColor: Color = this.disabledSelectedYearContentColor,
        selectedYearContainerColor: Color = this.selectedYearContainerColor,
        disabledSelectedYearContainerColor: Color = this.disabledSelectedYearContainerColor,
        dayContentColor: Color = this.dayContentColor,
        disabledDayContentColor: Color = this.disabledDayContentColor,
        selectedDayContentColor: Color = this.selectedDayContentColor,
        disabledSelectedDayContentColor: Color = this.disabledSelectedDayContentColor,
        selectedDayContainerColor: Color = this.selectedDayContainerColor,
        disabledSelectedDayContainerColor: Color = this.disabledSelectedDayContainerColor,
        todayContentColor: Color = this.todayContentColor,
        todayDateBorderColor: Color = this.todayDateBorderColor,
        dayInSelectionRangeContainerColor: Color = this.dayInSelectionRangeContainerColor,
        dayInSelectionRangeContentColor: Color = this.dayInSelectionRangeContentColor,
        dividerColor: Color = this.dividerColor,
        dateTextFieldColors: TextFieldColors? = this.dateTextFieldColors
    ) =
        DatePickerColors(
            containerColor.takeOrElse { this.containerColor },
            titleContentColor.takeOrElse { this.titleContentColor },
            headlineContentColor.takeOrElse { this.headlineContentColor },
            weekdayContentColor.takeOrElse { this.weekdayContentColor },
            subheadContentColor.takeOrElse { this.subheadContentColor },
            navigationContentColor.takeOrElse { this.navigationContentColor },
            yearContentColor.takeOrElse { this.yearContentColor },
            disabledYearContentColor.takeOrElse { this.disabledYearContentColor },
            currentYearContentColor.takeOrElse { this.currentYearContentColor },
            selectedYearContentColor.takeOrElse { this.selectedYearContentColor },
            disabledSelectedYearContentColor.takeOrElse { this.disabledSelectedYearContentColor },
            selectedYearContainerColor.takeOrElse { this.selectedYearContainerColor },
            disabledSelectedYearContainerColor.takeOrElse {
                this.disabledSelectedYearContainerColor
            },
            dayContentColor.takeOrElse { this.dayContentColor },
            disabledDayContentColor.takeOrElse { this.disabledDayContentColor },
            selectedDayContentColor.takeOrElse { this.selectedDayContentColor },
            disabledSelectedDayContentColor.takeOrElse { this.disabledSelectedDayContentColor },
            selectedDayContainerColor.takeOrElse { this.selectedDayContainerColor },
            disabledSelectedDayContainerColor.takeOrElse { this.disabledSelectedDayContainerColor },
            todayContentColor.takeOrElse { this.todayContentColor },
            todayDateBorderColor.takeOrElse { this.todayDateBorderColor },
            dayInSelectionRangeContainerColor.takeOrElse { this.dayInSelectionRangeContainerColor },
            dayInSelectionRangeContentColor.takeOrElse { this.dayInSelectionRangeContentColor },
            dividerColor.takeOrElse { this.dividerColor },
            dateTextFieldColors.takeOrElse { this.dateTextFieldColors }
        )

    internal fun TextFieldColors?.takeOrElse(block: () -> TextFieldColors): TextFieldColors =
        this ?: block()

    /**
     * Represents the content color for a calendar day.
     *
     * @param isToday indicates that the color is for a date that represents today
     * @param selected indicates that the color is for a selected day
     * @param inRange indicates that the day is part of a selection range of days
     * @param enabled indicates that the day is enabled for selection
     */
    @Composable
    internal fun dayContentColor(
        isToday: Boolean,
        selected: Boolean,
        inRange: Boolean,
        enabled: Boolean
    ): State<Color> {
        val target =
            when {
                selected && enabled -> selectedDayContentColor
                selected && !enabled -> disabledSelectedDayContentColor
                inRange && enabled -> dayInSelectionRangeContentColor
                inRange && !enabled -> disabledDayContentColor
                isToday && enabled -> todayContentColor
                enabled -> dayContentColor
                else -> disabledDayContentColor
            }

        return if (inRange) {
            rememberUpdatedState(target)
        } else {
            // Animate the content color only when the day is not in a range.
            animateColorAsState(
                target,
                // TODO Load the motionScheme tokens from the component tokens file
                MotionSchemeKeyTokens.DefaultEffects.value()
            )
        }
    }

    /**
     * Represents the container color for a calendar day.
     *
     * @param selected indicates that the color is for a selected day
     * @param enabled indicates that the day is enabled for selection
     * @param animate whether or not to animate a container color change
     */
    @Composable
    internal fun dayContainerColor(
        selected: Boolean,
        enabled: Boolean,
        animate: Boolean
    ): State<Color> {
        val target =
            if (selected) {
                if (enabled) selectedDayContainerColor else disabledSelectedDayContainerColor
            } else {
                Color.Transparent
            }
        return if (animate) {
            animateColorAsState(
                target,
                // TODO Load the motionScheme tokens from the component tokens file
                MotionSchemeKeyTokens.DefaultEffects.value()
            )
        } else {
            rememberUpdatedState(target)
        }
    }

    /**
     * Represents the content color for a calendar year.
     *
     * @param currentYear indicates that the color is for a year that represents the current year
     * @param selected indicates that the color is for a selected year
     * @param enabled indicates that the year is enabled for selection
     */
    @Composable
    internal fun yearContentColor(
        currentYear: Boolean,
        selected: Boolean,
        enabled: Boolean
    ): State<Color> {
        val target =
            when {
                selected && enabled -> selectedYearContentColor
                selected && !enabled -> disabledSelectedYearContentColor
                currentYear -> currentYearContentColor
                enabled -> yearContentColor
                else -> disabledYearContentColor
            }

        return animateColorAsState(
            target,
            // TODO Load the motionScheme tokens from the component tokens file
            MotionSchemeKeyTokens.DefaultEffects.value()
        )
    }

    /**
     * Represents the container color for a calendar year.
     *
     * @param selected indicates that the color is for a selected day
     * @param enabled indicates that the year is enabled for selection
     */
    @Composable
    internal fun yearContainerColor(selected: Boolean, enabled: Boolean): State<Color> {
        val target =
            if (selected) {
                if (enabled) selectedYearContainerColor else disabledSelectedYearContainerColor
            } else {
                Color.Transparent
            }
        return animateColorAsState(
            target,
            // TODO Load the motionScheme tokens from the component tokens file
            MotionSchemeKeyTokens.DefaultEffects.value()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatePickerColors) return false
        if (containerColor != other.containerColor) return false
        if (titleContentColor != other.titleContentColor) return false
        if (headlineContentColor != other.headlineContentColor) return false
        if (weekdayContentColor != other.weekdayContentColor) return false
        if (subheadContentColor != other.subheadContentColor) return false
        if (yearContentColor != other.yearContentColor) return false
        if (disabledYearContentColor != other.disabledYearContentColor) return false
        if (currentYearContentColor != other.currentYearContentColor) return false
        if (selectedYearContentColor != other.selectedYearContentColor) return false
        if (disabledSelectedYearContentColor != other.disabledSelectedYearContentColor) return false
        if (selectedYearContainerColor != other.selectedYearContainerColor) return false
        if (disabledSelectedYearContainerColor != other.disabledSelectedYearContainerColor)
            return false
        if (dayContentColor != other.dayContentColor) return false
        if (disabledDayContentColor != other.disabledDayContentColor) return false
        if (selectedDayContentColor != other.selectedDayContentColor) return false
        if (disabledSelectedDayContentColor != other.disabledSelectedDayContentColor) return false
        if (selectedDayContainerColor != other.selectedDayContainerColor) return false
        if (disabledSelectedDayContainerColor != other.disabledSelectedDayContainerColor) {
            return false
        }
        if (todayContentColor != other.todayContentColor) return false
        if (todayDateBorderColor != other.todayDateBorderColor) return false
        if (dayInSelectionRangeContainerColor != other.dayInSelectionRangeContainerColor) {
            return false
        }
        if (dayInSelectionRangeContentColor != other.dayInSelectionRangeContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + titleContentColor.hashCode()
        result = 31 * result + headlineContentColor.hashCode()
        result = 31 * result + weekdayContentColor.hashCode()
        result = 31 * result + subheadContentColor.hashCode()
        result = 31 * result + yearContentColor.hashCode()
        result = 31 * result + disabledYearContentColor.hashCode()
        result = 31 * result + currentYearContentColor.hashCode()
        result = 31 * result + selectedYearContentColor.hashCode()
        result = 31 * result + disabledSelectedYearContentColor.hashCode()
        result = 31 * result + selectedYearContainerColor.hashCode()
        result = 31 * result + disabledSelectedYearContainerColor.hashCode()
        result = 31 * result + dayContentColor.hashCode()
        result = 31 * result + disabledDayContentColor.hashCode()
        result = 31 * result + selectedDayContentColor.hashCode()
        result = 31 * result + disabledSelectedDayContentColor.hashCode()
        result = 31 * result + selectedDayContainerColor.hashCode()
        result = 31 * result + disabledSelectedDayContainerColor.hashCode()
        result = 31 * result + todayContentColor.hashCode()
        result = 31 * result + todayDateBorderColor.hashCode()
        result = 31 * result + dayInSelectionRangeContainerColor.hashCode()
        result = 31 * result + dayInSelectionRangeContentColor.hashCode()
        return result
    }
}

/**
 * An abstract for the date pickers states.
 *
 * This base class common state properties and provides a base implementation that is extended by
 * the different state classes.
 *
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. In case `null` is provided, the
 *   displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI.
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent a
 *   year that is out of the year range.
 * @see rememberDatePickerState
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal abstract class BaseDatePickerStateImpl(
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
    val yearRange: IntRange,
    selectableDates: SelectableDates,
    locale: CalendarLocale
) {

    val calendarModel = createCalendarModel(locale)

    var selectableDates by mutableStateOf(selectableDates)

    private var _displayedMonth =
        mutableStateOf(
            if (initialDisplayedMonthMillis != null) {
                val month = calendarModel.getMonth(initialDisplayedMonthMillis)
                require(yearRange.contains(month.year)) {
                    "The initial display month's year (${month.year}) is out of the years range of " +
                        "$yearRange."
                }
                month
            } else {
                // Set the displayed month to the current one.
                calendarModel.getMonth(calendarModel.today)
            }
        )

    var displayedMonthMillis: Long
        get() = _displayedMonth.value.startUtcTimeMillis
        set(monthMillis) {
            val month = calendarModel.getMonth(monthMillis)
            require(yearRange.contains(month.year)) {
                "The display month's year (${month.year}) is out of the years range of $yearRange."
            }
            _displayedMonth.value = month
        }
}

/**
 * A default implementation of the [DatePickerState]. See [rememberDatePickerState].
 *
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a date. Provide a `null` to indicate no selection. Note that the
 *   state's [selectedDateMillis] will provide a timestamp that represents the _start_ of the day,
 *   which may be different than the provided initialSelectedDateMillis.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 *   an initial selection of a month to be displayed to the user. In case `null` is provided, the
 *   displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 *   case a date is not allowed to be selected, it will appear disabled in the UI
 * @param locale a [CalendarLocale] to be used when formatting dates, determining the input format,
 *   and more
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent a
 *   year that is out of the year range.
 * @see rememberDatePickerState
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class DatePickerStateImpl(
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long?,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
    yearRange: IntRange,
    initialDisplayMode: DisplayMode,
    selectableDates: SelectableDates,
    locale: CalendarLocale
) :
    BaseDatePickerStateImpl(initialDisplayedMonthMillis, yearRange, selectableDates, locale),
    DatePickerState {

    /** A mutable state of [CalendarDate] that represents a selected date. */
    private var _selectedDate =
        mutableStateOf(
            if (initialSelectedDateMillis != null) {
                val date = calendarModel.getCanonicalDate(initialSelectedDateMillis)
                require(yearRange.contains(date.year)) {
                    "The provided initial date's year (${date.year}) is out of the years range " +
                        "of $yearRange."
                }
                date
            } else {
                null
            }
        )

    override var selectedDateMillis: Long?
        @Suppress("AutoBoxing") get() = _selectedDate.value?.utcTimeMillis
        set(@Suppress("AutoBoxing") dateMillis) {
            if (dateMillis != null) {
                val date = calendarModel.getCanonicalDate(dateMillis)
                // Validate that the give date is within the valid years range.
                require(yearRange.contains(date.year)) {
                    "The provided date's year (${date.year}) is out of the years range of " +
                        "$yearRange."
                }
                _selectedDate.value = date
            } else {
                _selectedDate.value = null
            }
        }

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI (i.e.
     * picker or input).
     */
    private var _displayMode = mutableStateOf(initialDisplayMode)

    override var displayMode
        get() = _displayMode.value
        set(displayMode) {
            selectedDateMillis?.let {
                displayedMonthMillis = calendarModel.getMonth(it).startUtcTimeMillis
            }
            _displayMode.value = displayMode
        }

    companion object {
        /**
         * The default [Saver] implementation for [DatePickerStateImpl].
         *
         * @param selectableDates a [SelectableDates] instance that is consulted to check if a date
         *   is allowed
         */
        fun Saver(
            selectableDates: SelectableDates,
            locale: CalendarLocale
        ): Saver<DatePickerStateImpl, Any> =
            listSaver(
                save = {
                    listOf(
                        it.selectedDateMillis,
                        it.displayedMonthMillis,
                        it.yearRange.first,
                        it.yearRange.last,
                        it.displayMode.value
                    )
                },
                restore = { value ->
                    DatePickerStateImpl(
                        initialSelectedDateMillis = value[0] as Long?,
                        initialDisplayedMonthMillis = value[1] as Long?,
                        yearRange = IntRange(value[2] as Int, value[3] as Int),
                        initialDisplayMode = DisplayMode(value[4] as Int),
                        selectableDates = selectableDates,
                        locale = locale
                    )
                }
            )
    }
}

/**
 * A date formatter used by [DatePicker].
 *
 * The date formatter will apply the best possible localized form of the given skeleton and Locale.
 * A skeleton is similar to, and uses the same format characters as, a Unicode <a
 * href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> pattern.
 *
 * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
 * `en_US` locale, but "d. MMMM" in the `de_CH` locale.
 *
 * @param yearSelectionSkeleton a date format skeleton used to format the date picker's year
 *   selection menu button (e.g. "March 2021").
 * @param selectedDateSkeleton a date format skeleton used to format a selected date (e.g. "Mar 27,
 *   2021")
 * @param selectedDateDescriptionSkeleton a date format skeleton used to format a selected date to
 *   be used as content description for screen readers (e.g. "Saturday, March 27, 2021")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Immutable
private class DatePickerFormatterImpl(
    val yearSelectionSkeleton: String,
    val selectedDateSkeleton: String,
    val selectedDateDescriptionSkeleton: String
) : DatePickerFormatter {

    // A map for caching formatter related results for better performance
    private val formatterCache = mutableMapOf<String, Any>()

    override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String? {
        if (monthMillis == null) return null
        return formatWithSkeleton(monthMillis, yearSelectionSkeleton, locale, formatterCache)
    }

    override fun formatDate(
        dateMillis: Long?,
        locale: CalendarLocale,
        forContentDescription: Boolean
    ): String? {
        if (dateMillis == null) return null
        return formatWithSkeleton(
            dateMillis,
            if (forContentDescription) {
                selectedDateDescriptionSkeleton
            } else {
                selectedDateSkeleton
            },
            locale,
            formatterCache
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatePickerFormatterImpl) return false

        if (yearSelectionSkeleton != other.yearSelectionSkeleton) return false
        if (selectedDateSkeleton != other.selectedDateSkeleton) return false
        if (selectedDateDescriptionSkeleton != other.selectedDateDescriptionSkeleton) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yearSelectionSkeleton.hashCode()
        result = 31 * result + selectedDateSkeleton.hashCode()
        result = 31 * result + selectedDateDescriptionSkeleton.hashCode()
        return result
    }
}

/**
 * A base container for the date picker and the date input. This container composes the top common
 * area of the UI, and accepts [content] for the actual calendar picker or text field input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateEntryContainer(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    headline: (@Composable () -> Unit)?,
    modeToggleButton: (@Composable () -> Unit)?,
    colors: DatePickerColors,
    headlineTextStyle: TextStyle,
    headerMinHeight: Dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier =
            modifier
                .sizeIn(minWidth = DatePickerModalTokens.ContainerWidth)
                .semantics {
                    // TODO(b/347038246): replace `isContainer` with `isTraversalGroup` with new
                    // pruning API.
                    @Suppress("DEPRECATION")
                    isContainer = true
                }
                .background(colors.containerColor)
    ) {
        DatePickerHeader(
            modifier = Modifier,
            title = title,
            titleContentColor = colors.titleContentColor,
            headlineContentColor = colors.headlineContentColor,
            minHeight = headerMinHeight
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val horizontalArrangement =
                    when {
                        headline != null && modeToggleButton != null -> Arrangement.SpaceBetween
                        headline != null -> Arrangement.Start
                        else -> Arrangement.End
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (headline != null) {
                        ProvideTextStyle(value = headlineTextStyle) {
                            Box(modifier = Modifier.weight(1f)) { headline() }
                        }
                    }
                    modeToggleButton?.invoke()
                }
                // Display a divider only when there is a title, headline, or a mode toggle.
                if (title != null || headline != null || modeToggleButton != null) {
                    HorizontalDivider(color = colors.dividerColor)
                }
            }
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisplayModeToggleButton(
    modifier: Modifier,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit
) {
    if (displayMode == DisplayMode.Picker) {
        IconButton(onClick = { onDisplayModeChange(DisplayMode.Input) }, modifier = modifier) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = getString(Strings.DatePickerSwitchToInputMode)
            )
        }
    } else {
        IconButton(onClick = { onDisplayModeChange(DisplayMode.Picker) }, modifier = modifier) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = getString(Strings.DatePickerSwitchToCalendarMode)
            )
        }
    }
}

/**
 * Date entry content that displays a [DatePickerContent] or a [DateInputContent] according to the
 * state's display mode.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchableDateEntryContent(
    selectedDateMillis: Long?,
    displayedMonthMillis: Long,
    displayMode: DisplayMode,
    onDateSelectionChange: (dateInMillis: Long?) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors
) {
    // Parallax effect offset that will slightly scroll in and out the navigation part of the picker
    // when the display mode changes.
    val parallaxTarget = with(LocalDensity.current) { -48.dp.roundToPx() }
    // TODO Load the motionScheme tokens from the component tokens file
    val effectsInAnimationSpec: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultEffects.value()
    val effectsOutAnimationSpec: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.FastEffects.value()
    val spatialInOutAnimationSpec: FiniteAnimationSpec<IntOffset> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val spatialSizeAnimationSpec: FiniteAnimationSpec<IntSize> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    AnimatedContent(
        targetState = displayMode,
        modifier =
            Modifier.semantics {
                // TODO(b/347038246): replace `isContainer` with `isTraversalGroup` with new
                // pruning API.
                @Suppress("DEPRECATION")
                isContainer = true
            },
        transitionSpec = {
            // When animating the input mode, fade out the calendar picker and slide in the text
            // field from the bottom with a delay to show up after the picker is hidden.
            if (targetState == DisplayMode.Input) {
                    slideInVertically(animationSpec = spatialInOutAnimationSpec) { height ->
                        height
                    } + fadeIn(animationSpec = effectsInAnimationSpec) togetherWith
                        fadeOut(effectsOutAnimationSpec) +
                            slideOutVertically(
                                animationSpec = spatialInOutAnimationSpec,
                                targetOffsetY = { _ -> parallaxTarget }
                            )
                } else {
                    // When animating the picker mode, slide out text field and fade in calendar
                    // picker with a delay to show up after the text field is hidden.
                    slideInVertically(
                        animationSpec = spatialInOutAnimationSpec,
                        initialOffsetY = { _ -> parallaxTarget }
                    ) + fadeIn(animationSpec = effectsInAnimationSpec) togetherWith
                        slideOutVertically(
                            animationSpec = spatialInOutAnimationSpec,
                            targetOffsetY = { fullHeight -> fullHeight }
                        ) + fadeOut(animationSpec = effectsOutAnimationSpec)
                }
                .using(
                    SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ -> spatialSizeAnimationSpec }
                    )
                )
        },
        label = "DatePickerDisplayModeAnimation"
    ) { mode ->
        when (mode) {
            DisplayMode.Picker ->
                DatePickerContent(
                    selectedDateMillis = selectedDateMillis,
                    displayedMonthMillis = displayedMonthMillis,
                    onDateSelectionChange = onDateSelectionChange,
                    onDisplayedMonthChange = onDisplayedMonthChange,
                    calendarModel = calendarModel,
                    yearRange = yearRange,
                    dateFormatter = dateFormatter,
                    selectableDates = selectableDates,
                    colors = colors
                )
            DisplayMode.Input ->
                DateInputContent(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelectionChange = onDateSelectionChange,
                    calendarModel = calendarModel,
                    yearRange = yearRange,
                    dateFormatter = dateFormatter,
                    selectableDates = selectableDates,
                    colors = colors
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DatePickerContent(
    selectedDateMillis: Long?,
    displayedMonthMillis: Long,
    onDateSelectionChange: (dateInMillis: Long) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors
) {
    val displayedMonth = calendarModel.getMonth(displayedMonthMillis)
    val monthsListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = displayedMonth.indexIn(yearRange).coerceAtLeast(0)
        )
    val coroutineScope = rememberCoroutineScope()
    var yearPickerVisible by rememberSaveable { mutableStateOf(false) }
    val defaultLocale = defaultLocale()
    Column {
        MonthsNavigation(
            modifier = Modifier.padding(horizontal = DatePickerHorizontalPadding),
            nextAvailable = monthsListState.canScrollForward,
            previousAvailable = monthsListState.canScrollBackward,
            yearPickerVisible = yearPickerVisible,
            yearPickerText =
                dateFormatter.formatMonthYear(
                    monthMillis = displayedMonthMillis,
                    locale = defaultLocale
                ) ?: "-",
            onNextClicked = {
                coroutineScope.launch {
                    try {
                        monthsListState.animateScrollToItem(
                            monthsListState.firstVisibleItemIndex + 1
                        )
                    } catch (_: IllegalArgumentException) {
                        // Ignore. This may happen if the user clicked the "next" arrow fast while
                        // the list was still animating to the next item.
                    }
                }
            },
            onPreviousClicked = {
                coroutineScope.launch {
                    try {
                        monthsListState.animateScrollToItem(
                            monthsListState.firstVisibleItemIndex - 1
                        )
                    } catch (_: IllegalArgumentException) {
                        // Ignore. This may happen if the user clicked the "previous" arrow fast
                        // while  the list was still animating to the previous item.
                    }
                }
            },
            onYearPickerButtonClicked = { yearPickerVisible = !yearPickerVisible },
            colors = colors
        )

        Box {
            Column(modifier = Modifier.padding(horizontal = DatePickerHorizontalPadding)) {
                WeekDays(colors, calendarModel)
                HorizontalMonthsList(
                    lazyListState = monthsListState,
                    selectedDateMillis = selectedDateMillis,
                    onDateSelectionChange = onDateSelectionChange,
                    onDisplayedMonthChange = onDisplayedMonthChange,
                    calendarModel = calendarModel,
                    yearRange = yearRange,
                    dateFormatter = dateFormatter,
                    selectableDates = selectableDates,
                    colors = colors
                )
            }
            // TODO Load the motionScheme tokens from the component tokens file
            val fadeInAnimationSpec: FiniteAnimationSpec<Float> =
                MotionSchemeKeyTokens.DefaultEffects.value()
            val fadeOutAnimationSpec: FiniteAnimationSpec<Float> =
                MotionSchemeKeyTokens.FastEffects.value()
            val shrinkExpandAnimationSpec: FiniteAnimationSpec<IntSize> =
                MotionSchemeKeyTokens.DefaultEffects.value()
            androidx.compose.animation.AnimatedVisibility(
                visible = yearPickerVisible,
                modifier = Modifier.clipToBounds(),
                enter =
                    expandVertically(animationSpec = shrinkExpandAnimationSpec) +
                        fadeIn(animationSpec = fadeInAnimationSpec, initialAlpha = 0.6f),
                exit =
                    shrinkVertically(animationSpec = shrinkExpandAnimationSpec) +
                        fadeOut(animationSpec = fadeOutAnimationSpec)
            ) {
                // Apply a paneTitle to make the screen reader focus on a relevant node after this
                // column is hidden and disposed.
                // TODO(b/186443263): Have the screen reader focus on a year in the list when the
                //  list is revealed.
                val yearsPaneTitle = getString(Strings.DatePickerYearPickerPaneTitle)
                Column(modifier = Modifier.semantics { paneTitle = yearsPaneTitle }) {
                    YearPicker(
                        // Keep the height the same as the monthly calendar + weekdays height, and
                        // take into account the thickness of the divider that will be composed
                        // below it.
                        modifier =
                            Modifier.requiredHeight(
                                    RecommendedSizeForAccessibility * (MaxCalendarRows + 1) -
                                        DividerDefaults.Thickness
                                )
                                .padding(horizontal = DatePickerHorizontalPadding),
                        displayedMonthMillis = displayedMonthMillis,
                        onYearSelected = { year ->
                            // Switch back to the monthly calendar and scroll to the selected year.
                            yearPickerVisible = !yearPickerVisible
                            coroutineScope.launch {
                                // Scroll to the selected year (maintaining the month of year).
                                // A LaunchEffect at the MonthsList will take care of rest and will
                                // update the state's displayedMonth to the month we scrolled to.
                                monthsListState.scrollToItem(
                                    (year - yearRange.first) * 12 + displayedMonth.month - 1
                                )
                            }
                        },
                        selectableDates = selectableDates,
                        calendarModel = calendarModel,
                        yearRange = yearRange,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.dividerColor)
                }
            }
        }
    }
}

@Composable
internal fun DatePickerHeader(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    titleContentColor: Color,
    headlineContentColor: Color,
    minHeight: Dp,
    content: @Composable () -> Unit
) {
    // Apply a defaultMinSize only when the title is not null.
    val heightModifier =
        if (title != null) {
            Modifier.defaultMinSize(minHeight = minHeight)
        } else {
            Modifier
        }
    Column(
        modifier.fillMaxWidth().then(heightModifier),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (title != null) {
            val textStyle = DatePickerModalTokens.HeaderSupportingTextFont.value
            ProvideContentColorTextStyle(contentColor = titleContentColor, textStyle = textStyle) {
                Box(contentAlignment = Alignment.BottomStart) { title() }
            }
        }
        CompositionLocalProvider(LocalContentColor provides headlineContentColor, content = content)
    }
}

/** Composes a horizontal pageable list of months. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorizontalMonthsList(
    lazyListState: LazyListState,
    selectedDateMillis: Long?,
    onDateSelectionChange: (dateInMillis: Long) -> Unit,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors
) {
    val today = calendarModel.today
    val firstMonth =
        remember(yearRange) {
            calendarModel.getMonth(
                year = yearRange.first,
                month = 1 // January
            )
        }
    ProvideTextStyle(DatePickerModalTokens.DateLabelTextFont.value) {
        LazyRow(
            // Apply this to prevent the screen reader from scrolling to the next or previous month,
            // and instead, traverse outside the Month composable when swiping from a focused first
            // or last day of the month.
            modifier =
                Modifier.semantics {
                    horizontalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                },
            state = lazyListState,
            flingBehavior = DatePickerDefaults.rememberSnapFlingBehavior(lazyListState)
        ) {
            items(numberOfMonthsInRange(yearRange)) {
                val month = calendarModel.plusMonths(from = firstMonth, addedMonthsCount = it)
                Box(modifier = Modifier.fillParentMaxWidth()) {
                    Month(
                        month = month,
                        onDateSelectionChange = onDateSelectionChange,
                        todayMillis = today.utcTimeMillis,
                        startDateMillis = selectedDateMillis,
                        endDateMillis = null,
                        rangeSelectionInfo = null,
                        dateFormatter = dateFormatter,
                        selectableDates = selectableDates,
                        colors = colors
                    )
                }
            }
        }
    }

    LaunchedEffect(lazyListState) {
        updateDisplayedMonth(
            lazyListState = lazyListState,
            onDisplayedMonthChange = onDisplayedMonthChange,
            calendarModel = calendarModel,
            yearRange = yearRange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun updateDisplayedMonth(
    lazyListState: LazyListState,
    onDisplayedMonthChange: (monthInMillis: Long) -> Unit,
    calendarModel: CalendarModel,
    yearRange: IntRange
) {
    snapshotFlow { lazyListState.firstVisibleItemIndex }
        .collect {
            val yearOffset = lazyListState.firstVisibleItemIndex / 12
            val month = lazyListState.firstVisibleItemIndex % 12 + 1
            onDisplayedMonthChange(
                calendarModel
                    .getMonth(year = yearRange.first + yearOffset, month = month)
                    .startUtcTimeMillis
            )
        }
}

/** Composes the weekdays letters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeekDays(colors: DatePickerColors, calendarModel: CalendarModel) {
    val firstDayOfWeek = calendarModel.firstDayOfWeek
    val weekdays = calendarModel.weekdayNames
    val dayNames = arrayListOf<Pair<String, String>>()
    // Start with firstDayOfWeek - 1 as the days are 1-based.
    for (i in firstDayOfWeek - 1 until weekdays.size) {
        dayNames.add(weekdays[i])
    }
    for (i in 0 until firstDayOfWeek - 1) {
        dayNames.add(weekdays[i])
    }
    val textStyle = DatePickerModalTokens.WeekdaysLabelTextFont.value

    Row(
        modifier =
            Modifier.defaultMinSize(minHeight = RecommendedSizeForAccessibility).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayNames.fastForEach {
            Box(
                modifier =
                    Modifier.clearAndSetSemantics { contentDescription = it.first }
                        // Match the minimum size to the Day's required size. This will ensure an
                        // aligned layout for the weekday letters to the days below, even when
                        // the LocalMinimumInteractiveComponentSize is set to a lower value than the
                        // recommended 48.dp.
                        .sizeIn(
                            minWidth = DatePickerModalTokens.DateContainerWidth,
                            minHeight = DatePickerModalTokens.DateContainerHeight
                        )
                        .size(
                            width = LocalMinimumInteractiveComponentSize.current,
                            height = LocalMinimumInteractiveComponentSize.current
                        ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.second,
                    modifier = Modifier.wrapContentSize(),
                    color = colors.weekdayContentColor,
                    style = textStyle,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** A composable that renders a calendar month and displays a date selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Month(
    month: CalendarMonth,
    onDateSelectionChange: (dateInMillis: Long) -> Unit,
    todayMillis: Long,
    startDateMillis: Long?,
    endDateMillis: Long?,
    rangeSelectionInfo: SelectedRangeInfo?,
    dateFormatter: DatePickerFormatter,
    selectableDates: SelectableDates,
    colors: DatePickerColors
) {
    val rangeSelectionDrawModifier =
        if (rangeSelectionInfo != null) {
            Modifier.drawWithContent {
                drawRangeBackground(rangeSelectionInfo, colors.dayInSelectionRangeContainerColor)
                drawContent()
            }
        } else {
            Modifier
        }

    val defaultLocale = defaultLocale()
    var cellIndex = 0
    Column(
        modifier =
            Modifier.requiredHeight(RecommendedSizeForAccessibility * MaxCalendarRows)
                .then(rangeSelectionDrawModifier),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (weekIndex in 0 until MaxCalendarRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (dayIndex in 0 until DaysInWeek) {
                    if (
                        cellIndex < month.daysFromStartOfWeekToFirstOfMonth ||
                            cellIndex >=
                                (month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays)
                    ) {
                        // Empty cell.
                        // Match the spacer's minimum size to the Day's required size. This will
                        // ensure an aligned layout, even when a
                        // LocalMinimumInteractiveComponentSize is set to a lower value than the
                        // recommended 48.dp.
                        Spacer(
                            modifier =
                                Modifier.sizeIn(
                                        minWidth = DatePickerModalTokens.DateContainerWidth,
                                        minHeight = DatePickerModalTokens.DateContainerHeight
                                    )
                                    .size(
                                        width = LocalMinimumInteractiveComponentSize.current,
                                        height = LocalMinimumInteractiveComponentSize.current
                                    )
                        )
                    } else {
                        val dayNumber = cellIndex - month.daysFromStartOfWeekToFirstOfMonth
                        val dateInMillis =
                            month.startUtcTimeMillis + (dayNumber * MillisecondsIn24Hours)
                        val isToday = dateInMillis == todayMillis
                        val startDateSelected = dateInMillis == startDateMillis
                        val endDateSelected = dateInMillis == endDateMillis
                        val inRange =
                            if (rangeSelectionInfo != null) {
                                remember(rangeSelectionInfo, dateInMillis) {
                                        mutableStateOf(
                                            dateInMillis >=
                                                (startDateMillis ?: Long.Companion.MAX_VALUE) &&
                                                dateInMillis <= (endDateMillis ?: Long.MIN_VALUE)
                                        )
                                    }
                                    .value
                            } else {
                                false
                            }
                        val dayContentDescription =
                            dayContentDescription(
                                rangeSelectionEnabled = rangeSelectionInfo != null,
                                isToday = isToday,
                                isStartDate = startDateSelected,
                                isEndDate = endDateSelected,
                                isInRange = inRange
                            )
                        val formattedDateDescription =
                            dateFormatter.formatDate(
                                dateInMillis,
                                defaultLocale,
                                forContentDescription = true
                            ) ?: ""
                        Day(
                            modifier = Modifier,
                            selected = startDateSelected || endDateSelected,
                            onClick = { onDateSelectionChange(dateInMillis) },
                            // Only animate on the first selected day. This is important to
                            // disable when drawing a range marker behind the days on an
                            // end-date selection.
                            animateChecked = startDateSelected,
                            enabled =
                                remember(dateInMillis, selectableDates) {
                                    // Disabled a day in case its year is not selectable, or the
                                    // date itself is specifically not allowed by the state's
                                    // SelectableDates.
                                    with(selectableDates) {
                                        isSelectableYear(month.year) &&
                                            isSelectableDate(dateInMillis)
                                    }
                                },
                            today = isToday,
                            inRange = inRange,
                            description =
                                if (dayContentDescription != null) {
                                    "$dayContentDescription, $formattedDateDescription"
                                } else {
                                    formattedDateDescription
                                },
                            colors = colors
                        ) {
                            Text(
                                text = (dayNumber + 1).toLocalString(),
                                // The semantics are set at the Day level.
                                modifier = Modifier.clearAndSetSemantics {},
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    cellIndex++
                }
            }
        }
    }
}

/** Returns the number of months within the given year range. */
internal fun numberOfMonthsInRange(yearRange: IntRange) =
    (yearRange.last - yearRange.first + 1) * 12

@Composable
private fun dayContentDescription(
    rangeSelectionEnabled: Boolean,
    isToday: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean,
    isInRange: Boolean
): String? {
    val descriptionBuilder = StringBuilder()
    if (rangeSelectionEnabled) {
        when {
            isStartDate ->
                descriptionBuilder.append(getString(string = Strings.DateRangePickerStartHeadline))
            isEndDate ->
                descriptionBuilder.append(getString(string = Strings.DateRangePickerEndHeadline))
            isInRange ->
                descriptionBuilder.append(getString(string = Strings.DateRangePickerDayInRange))
        }
    }
    if (isToday) {
        if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append(", ")
        descriptionBuilder.append(getString(string = Strings.DatePickerTodayDescription))
    }
    return if (descriptionBuilder.isEmpty()) null else descriptionBuilder.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    animateChecked: Boolean,
    enabled: Boolean,
    today: Boolean,
    inRange: Boolean,
    description: String,
    colors: DatePickerColors,
    content: @Composable () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
            modifier
                // Apply and merge semantics here. This will ensure that when scrolling the list the
                // entire Day surface is treated as one unit and holds the date semantics even when
                // it's not completely visible atm.
                .semantics(mergeDescendants = true) {
                    text = AnnotatedString(description)
                    role = Role.Button
                },
        enabled = enabled,
        shape = DatePickerModalTokens.DateContainerShape.value,
        color =
            colors
                .dayContainerColor(selected = selected, enabled = enabled, animate = animateChecked)
                .value,
        contentColor =
            colors
                .dayContentColor(
                    isToday = today,
                    selected = selected,
                    inRange = inRange,
                    enabled = enabled,
                )
                .value,
        border =
            if (today && !selected) {
                BorderStroke(
                    DatePickerModalTokens.DateTodayContainerOutlineWidth,
                    colors.todayDateBorderColor
                )
            } else {
                null
            }
    ) {
        Box(
            modifier =
                Modifier.requiredSize(
                    DatePickerModalTokens.DateContainerWidth,
                    DatePickerModalTokens.DateContainerHeight
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearPicker(
    modifier: Modifier,
    displayedMonthMillis: Long,
    onYearSelected: (year: Int) -> Unit,
    selectableDates: SelectableDates,
    calendarModel: CalendarModel,
    yearRange: IntRange,
    colors: DatePickerColors
) {
    ProvideTextStyle(value = DatePickerModalTokens.SelectionYearLabelTextFont.value) {
        val currentYear = calendarModel.getMonth(calendarModel.today).year
        val displayedYear = calendarModel.getMonth(displayedMonthMillis).year
        val lazyGridState =
            rememberLazyGridState(
                // Set the initial index to a few years before the current year to allow quicker
                // selection of previous years.
                initialFirstVisibleItemIndex = max(0, displayedYear - yearRange.first - YearsInRow)
            )
        LazyVerticalGrid(
            columns = GridCells.Fixed(YearsInRow),
            // Match the years container color to any elevated surface color that is composed under
            // it.
            modifier = modifier.background(colors.containerColor),
            state = lazyGridState,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(YearsVerticalPadding)
        ) {
            items(yearRange.count()) {
                val selectedYear = it + yearRange.first
                val localizedYear = selectedYear.toLocalString()
                Year(
                    modifier =
                        Modifier.requiredSize(
                            width = DatePickerModalTokens.SelectionYearContainerWidth,
                            height = DatePickerModalTokens.SelectionYearContainerHeight
                        ),
                    selected = selectedYear == displayedYear,
                    currentYear = selectedYear == currentYear,
                    onClick = { onYearSelected(selectedYear) },
                    enabled = selectableDates.isSelectableYear(selectedYear),
                    description =
                        formatDatePickerNavigateToYearString(
                            getString(Strings.DatePickerNavigateToYearDescription),
                            localizedYear
                        ),
                    colors = colors
                ) {
                    Text(
                        text = localizedYear,
                        // The semantics are set at the Year level.
                        modifier = Modifier.clearAndSetSemantics {},
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

internal expect inline fun formatDatePickerNavigateToYearString(
    template: String,
    localizedYear: String
): String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Year(
    modifier: Modifier,
    selected: Boolean,
    currentYear: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    colors: DatePickerColors,
    content: @Composable () -> Unit
) {
    val border =
        remember(currentYear, selected) {
            if (currentYear && !selected) {
                // Use the day's spec to draw a border around the current year.
                BorderStroke(
                    DatePickerModalTokens.DateTodayContainerOutlineWidth,
                    colors.todayDateBorderColor
                )
            } else {
                null
            }
        }
    Surface(
        selected = selected,
        onClick = onClick,
        // Apply and merge semantics here. This will ensure that when scrolling the list the entire
        // Year surface is treated as one unit and holds the date semantics even when it's not
        // completely visible atm.
        modifier =
            modifier.semantics(mergeDescendants = true) {
                text = AnnotatedString(description)
                role = Role.Button
            },
        enabled = enabled,
        shape = DatePickerModalTokens.SelectionYearStateLayerShape.value,
        color = colors.yearContainerColor(selected = selected, enabled = enabled).value,
        contentColor =
            colors
                .yearContentColor(currentYear = currentYear, selected = selected, enabled = enabled)
                .value,
        border = border,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * A composable that shows a year menu button and a couple of buttons that enable navigation between
 * displayed months.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthsNavigation(
    modifier: Modifier,
    nextAvailable: Boolean,
    previousAvailable: Boolean,
    yearPickerVisible: Boolean,
    yearPickerText: String,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onYearPickerButtonClicked: () -> Unit,
    colors: DatePickerColors
) {
    Row(
        modifier = modifier.fillMaxWidth().requiredHeight(MonthYearHeight),
        horizontalArrangement =
            if (yearPickerVisible) {
                Arrangement.Start
            } else {
                Arrangement.SpaceBetween
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.navigationContentColor) {
            // A menu button for selecting a year.
            YearPickerMenuButton(
                onClick = onYearPickerButtonClicked,
                expanded = yearPickerVisible
            ) {
                Text(
                    text = yearPickerText,
                    modifier =
                        Modifier.semantics {
                            // Make the screen reader read out updates to the menu button text as
                            // the user navigates the arrows or scrolls to change the displayed
                            // month.
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = yearPickerText
                        }
                )
            }
            // Show arrows for traversing months (only visible when the year selection is off)
            if (!yearPickerVisible) {
                Row {
                    IconButton(onClick = onPreviousClicked, enabled = previousAvailable) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = getString(Strings.DatePickerSwitchToPreviousMonth)
                        )
                    }
                    IconButton(onClick = onNextClicked, enabled = nextAvailable) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = getString(Strings.DatePickerSwitchToNextMonth)
                        )
                    }
                }
            }
        }
    }
}

// TODO: Replace with the official MenuButton when implemented.
@Composable
private fun YearPickerMenuButton(
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        elevation = null,
        border = null,
    ) {
        content()
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription =
                if (expanded) {
                    getString(Strings.DatePickerSwitchToDaySelection)
                } else {
                    getString(Strings.DatePickerSwitchToYearSelection)
                },
            Modifier.rotate(if (expanded) 180f else 0f)
        )
    }
}

internal val RecommendedSizeForAccessibility = 48.dp
internal val MonthYearHeight = 56.dp
internal val DatePickerHorizontalPadding = 12.dp
internal val DatePickerModeTogglePadding = PaddingValues(end = 12.dp, bottom = 12.dp)

private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)
private val DatePickerHeadlinePadding = PaddingValues(start = 24.dp, end = 12.dp, bottom = 12.dp)

private val YearsVerticalPadding = 16.dp

private const val MaxCalendarRows = 6
private const val YearsInRow: Int = 3
