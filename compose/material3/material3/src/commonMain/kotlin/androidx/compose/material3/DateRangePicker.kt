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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external" target="_blank">Material Design date range picker</a>.
 *
 * Date range pickers let people select a range of dates and can be embedded into Dialogs.
 *
 * A simple DateRangePicker looks like:
 * @sample androidx.compose.material3.samples.DateRangePickerSample
 *
 * @param state state of the date range picker. See [rememberDateRangePickerState].
 * @param modifier the [Modifier] to be applied to this date range picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param title the title to be displayed in the date range picker
 * @param headline the headline to be displayed in the date range picker
 * @param showModeToggle indicates if this DateRangePicker should show a mode toggle action that
 * transforms it into a date range input
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 * range picker in different states. See [DatePickerDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun DateRangePicker(
    state: DateRangePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerFormatter() },
    title: (@Composable () -> Unit)? = {
        DateRangePickerDefaults.DateRangePickerTitle(
            state = state,
            modifier = Modifier.padding(DateRangePickerTitlePadding)
        )
    },
    headline: (@Composable () -> Unit)? = {
        DateRangePickerDefaults.DateRangePickerHeadline(
            state,
            dateFormatter,
            modifier = Modifier.padding(DateRangePickerHeadlinePadding)
        )
    },
    showModeToggle: Boolean = true,
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        modeToggleButton = if (showModeToggle) {
            {
                DisplayModeToggleButton(
                    modifier = Modifier.padding(DatePickerModeTogglePadding),
                    displayMode = state.displayMode,
                    onDisplayModeChange = { displayMode ->
                        state.stateData.switchDisplayMode(
                            displayMode
                        )
                    }
                )
            }
        } else {
            null
        },
        headlineTextStyle = MaterialTheme.typography.fromToken(
            DatePickerModalTokens.RangeSelectionHeaderHeadlineFont
        ),
        headerMinHeight = DatePickerModalTokens.RangeSelectionHeaderContainerHeight -
            HeaderHeightOffset,
        colors = colors
    ) {
        SwitchableDateEntryContent(
            state = state,
            dateFormatter = dateFormatter,
            colors = colors
        )
    }
}

/**
 * Creates a [DateRangePickerState] for a [DateRangePicker] that is remembered across compositions.
 *
 * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a start date. Provide a `null` to indicate no selection.
 * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of an end date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a month to be displayed to the user. By default, in case an
 * `initialSelectedStartDateMillis` is provided, the initial displayed month would be the month of
 * the selected date. Otherwise, in case `null` is provided, the displayed month would be the
 * current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed.
 * In case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDateRangePickerState(
    @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? =
        initialSelectedStartDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = object : SelectableDates {}
): DateRangePickerState = rememberSaveable(
    saver = DateRangePickerState.Saver(selectableDates)
) {
    DateRangePickerState(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates
    )
}

/**
 * A state object that can be hoisted to observe the date picker state. See
 * [rememberDateRangePickerState].
 *
 * The state's [selectedStartDateMillis] and [selectedEndDateMillis] will provide timestamps for the
 * _beginning_ of the selected days (i.e. midnight in _UTC_ milliseconds from the epoch).
 */
@ExperimentalMaterial3Api
@Stable
class DateRangePickerState private constructor(internal val stateData: StateData) {

    /**
     * Constructs a DateRangePickerState.
     *
     * @param initialSelectedStartDateMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a start date. Provide a `null` to indicate no selection.
     * @param initialSelectedEndDateMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of an end date. Provide a `null` to indicate no selection.
     * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that
     * represents an initial selection of a month to be displayed to the user. By default, in case
     * an `initialSelectedStartDateMillis` is provided, the initial displayed month would be the
     * month of the selected date. Otherwise, in case `null` is provided, the displayed month would
     * be the current one.
     * @param yearRange an [IntRange] that holds the year range that the date picker will be limited
     * to
     * @param initialDisplayMode an initial [DisplayMode] that this state will hold
     * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed.
     * In case a date is not allowed to be selected, it will appear disabled in the UI.
     * @see rememberDatePickerState
     * @throws IllegalArgumentException if the initial timestamps do not fall within the year range
     * this state is created with, or the end date precedes the start date, or when an end date is
     * provided without a start date (e.g. the start date was null, while the end date was not).
     */
    constructor(
        @Suppress("AutoBoxing") initialSelectedStartDateMillis: Long?,
        @Suppress("AutoBoxing") initialSelectedEndDateMillis: Long?,
        @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
        yearRange: IntRange,
        initialDisplayMode: DisplayMode,
        selectableDates: SelectableDates
    ) : this(
        stateData = StateData(
            initialSelectedStartDateMillis = initialSelectedStartDateMillis,
            initialSelectedEndDateMillis = initialSelectedEndDateMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            initialDisplayMode = initialDisplayMode,
            selectableDates = selectableDates
        )
    )

    /**
     * A timestamp that represents the selected range start date.
     *
     * The timestamp would hold a value for the _start_ of the day in _UTC_ milliseconds from the
     * epoch.
     *
     * In case a start date was not selected or provided, the state will hold a `null` value.
     *
     * @see [setSelection]
     */

    val selectedStartDateMillis: Long?
        @Suppress("AutoBoxing") get() = stateData.selectedStartDate.value?.utcTimeMillis

    /**
     * A timestamp that represents the selected range end date.
     *
     * The timestamp would hold a value for the _start_ of the day in _UTC_ milliseconds from the
     * epoch.
     *
     * In case an end date was not selected or provided, the state will hold a `null` value.
     *
     * @see [setSelection]
     */
    val selectedEndDateMillis: Long?
        @Suppress("AutoBoxing") get() = stateData.selectedEndDate.value?.utcTimeMillis

    /**
     * Sets the selected date.
     *
     * @param startDateMillis timestamp in _UTC_ milliseconds from the epoch that represents the
     * start date selection, or `null` to indicate no selection.
     * @param endDateMillis timestamp in _UTC_ milliseconds from the epoch that represents the end
     * date selection, or `null` to indicate no selection.
     *
     * @throws IllegalArgumentException if the given timestamps do not fall within the year range
     * this state was created with, or the end date precedes the start date, or when an end date is
     * provided without a start date (e.g. the start date was null, while the end date was not).
     */
    fun setSelection(
        @Suppress("AutoBoxing") startDateMillis: Long?,
        @Suppress("AutoBoxing") endDateMillis: Long?
    ) {
        stateData.setSelection(startDateMillis = startDateMillis, endDateMillis = endDateMillis)
    }

    /**
     * A mutable state of [DisplayMode] that represents the current display mode of the UI
     * (i.e. picker or input).
     */
    var displayMode by stateData.displayMode

    companion object {
        /**
         * The default [Saver] implementation for [DateRangePickerState].
         *
         * @param selectableDates a [SelectableDates] instance that is consulted to check if a date
         * is allowed
         */
        fun Saver(selectableDates: SelectableDates): Saver<DateRangePickerState, *> = Saver(
            save = { with(StateData.Saver(selectableDates)) { save(it.stateData) } },
            restore = { value ->
                DateRangePickerState(stateData = with(StateData.Saver(selectableDates)) {
                    restore(
                        value
                    )!!
                })
            }
        )
    }
}

/**
 * Contains default values used by the date range pickers.
 */
@ExperimentalMaterial3Api
@Stable
object DateRangePickerDefaults {

    /**
     * A default date picker title composable.
     *
     * @param state a [DatePickerState] that will help determine the title's content
     * @param modifier a [Modifier] to be applied for the title
     */
    @Composable
    fun DateRangePickerTitle(
        state: DateRangePickerState,
        modifier: Modifier = Modifier
    ) {
        when (state.displayMode) {
            DisplayMode.Picker -> Text(
                getString(string = Strings.DateRangePickerTitle),
                modifier = modifier
            )

            DisplayMode.Input -> Text(
                getString(string = Strings.DateRangeInputTitle),
                modifier = modifier
            )
        }
    }

    /**
     * A default date picker headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param state a [DateRangePickerState] that will help determine the headline
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     */
    @Composable
    fun DateRangePickerHeadline(
        state: DateRangePickerState,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier = Modifier
    ) {
        val startDateText = getString(Strings.DateRangePickerStartHeadline)
        val endDateText = getString(Strings.DateRangePickerEndHeadline)
        DateRangePickerHeadline(
            state = state,
            dateFormatter = dateFormatter,
            modifier = modifier,
            startDateText = startDateText,
            endDateText = endDateText,
            startDatePlaceholder = { Text(text = startDateText) },
            endDatePlaceholder = { Text(text = endDateText) },
            datesDelimiter = { Text(text = "-") },
        )
    }

    /**
     * A date picker headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param state a [DateRangePickerState] that will help determine the headline
     * @param dateFormatter a [DatePickerFormatter]
     * @param modifier a [Modifier] to be applied for the headline
     * @param startDateText a string that, by default, be used as the text content for the
     * [startDatePlaceholder], as well as a prefix for the content description for the selected
     * start date
     * @param endDateText a string that, by default, be used as the text content for the
     * [endDatePlaceholder], as well as a prefix for the content description for the selected
     * end date
     * @param startDatePlaceholder a composable to be displayed as a headline placeholder for the
     * start date (i.e. a [Text] with a "Start date" string)
     * @param endDatePlaceholder a composable to be displayed as a headline placeholder for the end
     * date (i.e a [Text] with an "End date" string)
     * @param datesDelimiter a composable to be displayed as a headline delimiter between the
     * start and the end dates
     */
    @Composable
    private fun DateRangePickerHeadline(
        state: DateRangePickerState,
        dateFormatter: DatePickerFormatter,
        modifier: Modifier,
        startDateText: String,
        endDateText: String,
        startDatePlaceholder: @Composable () -> Unit,
        endDatePlaceholder: @Composable () -> Unit,
        datesDelimiter: @Composable () -> Unit,
    ) {
        with(state.stateData) {
            val defaultLocale = defaultLocale()
            val formatterStartDate = dateFormatter.formatDate(
                date = selectedStartDate.value,
                calendarModel = calendarModel,
                locale = defaultLocale
            )

            val formatterEndDate = dateFormatter.formatDate(
                date = selectedEndDate.value,
                calendarModel = calendarModel,
                locale = defaultLocale
            )

            val verboseStartDateDescription = dateFormatter.formatDate(
                date = selectedStartDate.value,
                calendarModel = calendarModel,
                locale = defaultLocale,
                forContentDescription = true
            ) ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                else -> ""
            }

            val verboseEndDateDescription = dateFormatter.formatDate(
                date = selectedEndDate.value,
                calendarModel = calendarModel,
                locale = defaultLocale,
                forContentDescription = true
            ) ?: when (displayMode.value) {
                DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
                DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
                else -> ""
            }

            val startHeadlineDescription = "$startDateText: $verboseStartDateDescription"
            val endHeadlineDescription = "$endDateText: $verboseEndDateDescription"

            Row(
                modifier = modifier.clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "$startHeadlineDescription, $endHeadlineDescription"
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (formatterStartDate != null) {
                    Text(text = formatterStartDate)
                } else {
                    startDatePlaceholder()
                }
                datesDelimiter()
                if (formatterEndDate != null) {
                    Text(text = formatterEndDate)
                } else {
                    endDatePlaceholder()
                }
            }
        }
    }
}

/**
 * Date entry content that displays a [DateRangePickerContent] or a [DateRangeInputContent]
 * according to the state's display mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchableDateEntryContent(
    state: DateRangePickerState,
    dateFormatter: DatePickerFormatter,
    colors: DatePickerColors
) {
    // TODO(b/266480386): Apply the motion spec for this once we have it. Consider replacing this
    //  with AnimatedContent when it's out of experimental.
    Crossfade(
        targetState = state.displayMode,
        animationSpec = spring(),
        modifier = Modifier.semantics { isContainer = true }) { mode ->
        when (mode) {
            DisplayMode.Picker -> DateRangePickerContent(
                stateData = state.stateData,
                dateFormatter = dateFormatter,
                colors = colors
            )

            DisplayMode.Input -> DateRangeInputContent(
                stateData = state.stateData,
                dateFormatter = dateFormatter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerContent(
    stateData: StateData,
    dateFormatter: DatePickerFormatter,
    colors: DatePickerColors
) {
    val monthsListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = stateData.displayedMonthIndex
        )

    val onDateSelected = { dateInMillis: Long ->
        updateDateSelection(stateData, dateInMillis)
    }
    Column(modifier = Modifier.padding(horizontal = DatePickerHorizontalPadding)) {
        WeekDays(colors, stateData.calendarModel)
        VerticalMonthsList(
            onDateSelected = onDateSelected,
            stateData = stateData,
            lazyListState = monthsListState,
            dateFormatter = dateFormatter,
            colors = colors
        )
    }
}

/**
 * Composes a continuous vertical scrollable list of calendar months. Each month will appear with a
 * header text indicating the month and the year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalMonthsList(
    onDateSelected: (dateInMillis: Long) -> Unit,
    stateData: StateData,
    lazyListState: LazyListState,
    dateFormatter: DatePickerFormatter,
    colors: DatePickerColors
) {
    val today = stateData.calendarModel.today
    val firstMonth = remember(stateData.yearRange) {
        stateData.calendarModel.getMonth(
            year = stateData.yearRange.first,
            month = 1 // January
        )
    }
    ProvideTextStyle(
        MaterialTheme.typography.fromToken(
            DatePickerModalTokens.RangeSelectionMonthSubheadFont
        )
    ) {
        val coroutineScope = rememberCoroutineScope()
        val scrollToPreviousMonthLabel = getString(Strings.DateRangePickerScrollToShowPreviousMonth)
        val scrollToNextMonthLabel = getString(Strings.DateRangePickerScrollToShowNextMonth)
        LazyColumn(
            // Apply this to have the screen reader traverse outside the visible list of months
            // and not scroll them by default.
            modifier = Modifier.semantics {
                verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
            },
            state = lazyListState
        ) {
            items(stateData.totalMonthsInRange) {
                val month =
                    stateData.calendarModel.plusMonths(
                        from = firstMonth,
                        addedMonthsCount = it
                    )
                Column(
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    Text(
                        text = dateFormatter.formatMonthYear(
                            month,
                            stateData.calendarModel,
                            defaultLocale()
                        ) ?: "-",
                        modifier = Modifier
                            .padding(paddingValues = CalendarMonthSubheadPadding)
                            .clickable { /* no-op (needed for customActions to operate */ }
                            .semantics {
                                customActions = customScrollActions(
                                    state = lazyListState,
                                    coroutineScope = coroutineScope,
                                    scrollUpLabel = scrollToPreviousMonthLabel,
                                    scrollDownLabel = scrollToNextMonthLabel
                                )
                            },
                        color = colors.subheadContentColor
                    )
                    Month(
                        month = month,
                        onDateSelected = onDateSelected,
                        today = today,
                        stateData = stateData,
                        rangeSelectionEnabled = true,
                        dateFormatter = dateFormatter,
                        colors = colors
                    )
                }
            }
        }
    }
    LaunchedEffect(lazyListState) {
        updateDisplayedMonth(lazyListState, stateData)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun updateDateSelection(
    stateData: StateData,
    dateInMillis: Long
) {
    with(stateData) {
        val date = calendarModel.getCanonicalDate(dateInMillis)
        val currentStart = selectedStartDate.value
        val currentEnd = selectedEndDate.value
        if ((currentStart == null && currentEnd == null) ||
            (currentStart != null && currentEnd != null) ||
            (currentStart != null && date < currentStart)
        ) {
            // Reset the selection to "start" only.
            selectedStartDate.value = date
            selectedEndDate.value = null
        } else if (currentStart != null && date >= currentStart) {
            selectedEndDate.value = date
        }
    }
}

internal val CalendarMonthSubheadPadding = PaddingValues(
    start = 24.dp,
    top = 20.dp,
    bottom = 8.dp
)

/**
 * a helper class for drawing a range selection. The class holds information about the selected
 * start and end dates as coordinates within the 7 x 6 calendar month grid, as well as information
 * regarding the first and last selected items.
 *
 * A SelectedRangeInfo is created when a [Month] is composed with an `rangeSelectionEnabled` flag.
 */
internal class SelectedRangeInfo(
    val gridCoordinates: Pair<IntOffset, IntOffset>,
    val firstIsSelectionStart: Boolean,
    val lastIsSelectionEnd: Boolean
) {
    companion object {
        /**
         * Calculates the selection coordinates within the current month's grid. The returned [Pair]
         * holds the actual item x & y coordinates within the LazyVerticalGrid, and is later used to
         * calculate the exact offset for drawing the selection rectangles when in range-selection
         * mode.
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun calculateRangeInfo(
            month: CalendarMonth,
            startDate: CalendarDate?,
            endDate: CalendarDate?
        ): SelectedRangeInfo? {
            if (startDate != null && endDate != null) {
                if (startDate.utcTimeMillis > month.endUtcTimeMillis ||
                    endDate.utcTimeMillis < month.startUtcTimeMillis
                ) {
                    return null
                }
                val firstIsSelectionStart = startDate.utcTimeMillis >= month.startUtcTimeMillis
                val lastIsSelectionEnd = endDate.utcTimeMillis <= month.endUtcTimeMillis
                val startGridItemOffset = if (firstIsSelectionStart) {
                    month.daysFromStartOfWeekToFirstOfMonth + startDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth
                }
                val endGridItemOffset = if (lastIsSelectionEnd) {
                    month.daysFromStartOfWeekToFirstOfMonth + endDate.dayOfMonth - 1
                } else {
                    month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays - 1
                }

                // Calculate the selected coordinates within the cells grid.
                val startCoordinates = IntOffset(
                    x = startGridItemOffset % DaysInWeek,
                    y = startGridItemOffset / DaysInWeek
                )
                val endCoordinates = IntOffset(
                    x = endGridItemOffset % DaysInWeek,
                    y = endGridItemOffset / DaysInWeek
                )
                return SelectedRangeInfo(
                    Pair(startCoordinates, endCoordinates),
                    firstIsSelectionStart,
                    lastIsSelectionEnd
                )
            }
            return null
        }
    }
}

/**
 * Draws the range selection background.
 *
 * This function is called during a [Modifier.drawWithContent] call when a [Month] is composed with
 * an `rangeSelectionEnabled` flag.
 */
internal fun ContentDrawScope.drawRangeBackground(
    selectedRangeInfo: SelectedRangeInfo,
    color: Color
) {
    // The LazyVerticalGrid is defined to space the items horizontally by
    // DaysHorizontalPadding (e.g. 4.dp). However, as the grid is not limited in
    // width, the spacing can go beyond that value, so this drawing takes this into
    // account.
    // TODO: Use the date's container width and height from the tokens once b/247694457 is resolved.
    val itemContainerWidth = RecommendedSizeForAccessibility.toPx()
    val itemContainerHeight = RecommendedSizeForAccessibility.toPx()
    val itemStateLayerHeight = DatePickerModalTokens.DateStateLayerHeight.toPx()
    val stateLayerVerticalPadding = (itemContainerHeight - itemStateLayerHeight) / 2
    val horizontalSpaceBetweenItems =
        (this.size.width - DaysInWeek * itemContainerWidth) / DaysInWeek

    val (x1, y1) = selectedRangeInfo.gridCoordinates.first
    val (x2, y2) = selectedRangeInfo.gridCoordinates.second
    // The endX and startX are offset to include only half the item's width when dealing with first
    // and last items in the selection in order to keep the selection edges rounded.
    var startX = x1 * (itemContainerWidth + horizontalSpaceBetweenItems) +
        (if (selectedRangeInfo.firstIsSelectionStart) itemContainerWidth / 2 else 0f) +
        horizontalSpaceBetweenItems / 2
    val startY = y1 * itemContainerHeight + stateLayerVerticalPadding
    var endX = x2 * (itemContainerWidth + horizontalSpaceBetweenItems) +
        (if (selectedRangeInfo.lastIsSelectionEnd) itemContainerWidth / 2 else itemContainerWidth) +
        horizontalSpaceBetweenItems / 2
    val endY = y2 * itemContainerHeight + stateLayerVerticalPadding

    val isRtl = layoutDirection == LayoutDirection.Rtl
    // Adjust the start and end in case the layout is RTL.
    if (isRtl) {
        startX = this.size.width - startX
        endX = this.size.width - endX
    }

    // Draw the first row background
    drawRect(
        color = color,
        topLeft = Offset(startX, startY),
        size = Size(
            width = when {
                y1 == y2 -> endX - startX
                isRtl -> -startX
                else -> this.size.width - startX
            },
            height = itemStateLayerHeight
        )
    )

    if (y1 != y2) {
        for (y in y2 - y1 - 1 downTo 1) {
            // Draw background behind the rows in between.
            drawRect(
                color = color,
                topLeft = Offset(0f, startY + (y * itemContainerHeight)),
                size = Size(
                    width = this.size.width,
                    height = itemStateLayerHeight
                )
            )
        }
        // Draw the last row selection background
        val topLeftX = if (layoutDirection == LayoutDirection.Ltr) 0f else this.size.width
        drawRect(
            color = color,
            topLeft = Offset(topLeftX, endY),
            size = Size(
                width = if (isRtl) endX - this.size.width else endX,
                height = itemStateLayerHeight
            )
        )
    }
}

private fun customScrollActions(
    state: LazyListState,
    coroutineScope: CoroutineScope,
    scrollUpLabel: String,
    scrollDownLabel: String
): List<CustomAccessibilityAction> {
    val scrollUpAction = {
        if (!state.canScrollBackward) {
            false
        } else {
            coroutineScope.launch {
                state.scrollToItem(state.firstVisibleItemIndex - 1)
            }
            true
        }
    }
    val scrollDownAction = {
        if (!state.canScrollForward) {
            false
        } else {
            coroutineScope.launch {
                state.scrollToItem(state.firstVisibleItemIndex + 1)
            }
            true
        }
    }
    return listOf(
        CustomAccessibilityAction(
            label = scrollUpLabel,
            action = scrollUpAction
        ),
        CustomAccessibilityAction(
            label = scrollDownLabel,
            action = scrollDownAction
        )
    )
}

private val DateRangePickerTitlePadding = PaddingValues(start = 64.dp, end = 12.dp)
private val DateRangePickerHeadlinePadding =
    PaddingValues(start = 64.dp, end = 12.dp, bottom = 12.dp)

// An offset that is applied to the token value for the RangeSelectionHeaderContainerHeight. The
// implementation does not render a "Save" and "X" buttons by default, so we don't take those into
// account when setting the header's max height.
private val HeaderHeightOffset = 60.dp
