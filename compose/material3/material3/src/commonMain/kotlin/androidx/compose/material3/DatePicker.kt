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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.lang.Integer.max
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external" target="_blank">Material Design date picker</a>.
 *
 * Date pickers let people select a date and preferably should be embedded into Dialogs.
 * See [DatePickerDialog].
 *
 * By default, a date picker lets you pick a date via a calendar UI. However, it also allows
 * switching into a date input mode for a manual entry of dates using the numbers on a keyboard.
 *
 * ![Date picker image](https://developer.android.com/images/reference/androidx/compose/material3/date-picker.png)
 *
 * A simple DatePicker looks like:
 * @sample androidx.compose.material3.samples.DatePickerSample
 *
 * A DatePicker with an initial UI of a date input mode looks like:
 * @sample androidx.compose.material3.samples.DateInputSample
 *
 * A DatePicker with a provided [SelectableDates] that blocks certain days from being selected looks
 * like:
 * @sample androidx.compose.material3.samples.DatePickerWithDateSelectableDatesSample
 *
 * @param state state of the date picker. See [rememberDatePickerState].
 * @param modifier the [Modifier] to be applied to this date picker
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param title the title to be displayed in the date picker
 * @param headline the headline to be displayed in the date picker
 * @param showModeToggle indicates if this DatePicker should show a mode toggle action that
 * transforms it into a date input
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 * picker in different states. See [DatePickerDefaults.colors].
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
    val calendarModel = remember { CalendarModel() }
    DateEntryContainer(
        modifier = modifier,
        title = title,
        headline = headline,
        modeToggleButton = if (showModeToggle) {
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
        headlineTextStyle = MaterialTheme.typography.fromToken(
            DatePickerModalTokens.HeaderHeadlineFont
        ),
        headerMinHeight = DatePickerModalTokens.HeaderContainerHeight,
        colors = colors
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
     * A timestamp that represents the selected date _start_ of the day in _UTC_ milliseconds
     * from the epoch.
     *
     * @throws IllegalArgumentException in case the value is set with a timestamp that does not fall
     * within the [yearRange].
     */
    @get:Suppress("AutoBoxing")
    var selectedDateMillis: Long?

    /**
     * A timestamp that represents the currently displayed month _start_ date in _UTC_ milliseconds
     * from the epoch.
     *
     * @throws IllegalArgumentException in case the value is set with a timestamp that does not fall
     * within the [yearRange].
     */
    var displayedMonthMillis: Long

    /**
     * A [DisplayMode] that represents the current UI mode (i.e. picker or input).
     */
    var displayMode: DisplayMode

    /**
     * An [IntRange] that holds the year range that the date picker will be limited to.
     */
    val yearRange: IntRange

    /**
     * A [SelectableDates] that is consulted to check if a date is allowed.
     *
     * In case a date is not allowed to be selected, it will appear disabled in the UI.
     */
    val selectableDates: SelectableDates
}

/**
 * An interface that controls the selectable dates and years in the date pickers UI.
 */
@ExperimentalMaterial3Api
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

/**
 * A date formatter interface used by [DatePicker].
 */
@ExperimentalMaterial3Api
interface DatePickerFormatter {

    /**
     * Format a given [monthMillis] to a string representation of the month and the year (i.e.
     * January 2023).
     *
     * @param monthMillis timestamp in _UTC_ milliseconds from the epoch that represents the month
     * @param locale a [Locale] to use when formatting the month and year
     */
    fun formatMonthYear(@Suppress("AutoBoxing") monthMillis: Long?, locale: Locale): String?

    /**
     * Format a given [dateMillis] to a string representation of the date (i.e. Mar 27, 2021).
     *
     * @param dateMillis timestamp in _UTC_ milliseconds from the epoch that represents the date
     * @param locale a [Locale] to use when formatting the date
     * @param forContentDescription indicates that the requested formatting is for content
     * description. In these cases, the output may include a more descriptive wording that will be
     * passed to a screen readers.
     */
    fun formatDate(
        @Suppress("AutoBoxing") dateMillis: Long?,
        locale: Locale,
        forContentDescription: Boolean = false
    ): String?
}

/**
 * Represents the different modes that a date picker can be at.
 */
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

    override fun toString() = when (this) {
        Picker -> "Picker"
        Input -> "Input"
        else -> "Unknown"
    }
}

/**
 * Creates a [DatePickerState] for a [DatePicker] that is remembered across compositions.
 *
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a date. Provide a `null` to indicate no selection.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that represents
 * an initial selection of a month to be displayed to the user. By default, in case an
 * `initialSelectedDateMillis` is provided, the initial displayed month would be the month of the
 * selected date. Otherwise, in case `null` is provided, the displayed month would be the
 * current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed. In
 * case a date is not allowed to be selected, it will appear disabled in the UI.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDatePickerState(
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long? = null,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = object : SelectableDates {}
): DatePickerState = rememberSaveable(
    saver = DatePickerStateImpl.Saver(selectableDates)
) {
    DatePickerStateImpl(
        initialSelectedDateMillis = initialSelectedDateMillis,
        initialDisplayedMonthMillis = initialDisplayedMonthMillis,
        yearRange = yearRange,
        initialDisplayMode = initialDisplayMode,
        selectableDates = selectableDates
    )
}

/**
 * Contains default values used by the [DatePicker].
 */
@ExperimentalMaterial3Api
@Stable
object DatePickerDefaults {

    /**
     * Creates a [DatePickerColors] that will potentially animate between the provided colors
     * according to the Material specification.
     *
     * @param containerColor the color used for the date picker's background
     * @param titleContentColor the color used for the date picker's title
     * @param headlineContentColor the color used for the date picker's headline
     * @param weekdayContentColor the color used for the weekday letters
     * @param subheadContentColor the color used for the month and year subhead labels that appear
     * when months are displayed at a `DateRangePicker`.
     * @param navigationContentColor the content color used for the year selection menu button and
     * the months arrow navigation when displayed at a `DatePicker`.
     * @param yearContentColor the color used for a year item content
     * @param disabledYearContentColor the color used for a disabled year item content
     * @param currentYearContentColor the color used for the current year content when selecting a
     * year
     * @param selectedYearContentColor the color used for a selected year item content
     * @param disabledSelectedYearContentColor the color used for a disabled selected year item
     * content
     * @param selectedYearContainerColor the color used for a selected year item container
     * @param disabledSelectedYearContainerColor the color used for a disabled selected year item
     * container
     * @param dayContentColor the color used for days content
     * @param disabledDayContentColor the color used for disabled days content
     * @param selectedDayContentColor the color used for selected days content
     * @param disabledSelectedDayContentColor the color used for disabled selected days content
     * @param selectedDayContainerColor the color used for a selected day container
     * @param disabledSelectedDayContainerColor the color used for a disabled selected day container
     * @param todayContentColor the color used for the day that marks the current date
     * @param todayDateBorderColor the color used for the border of the day that marks the current
     * date
     * @param dayInSelectionRangeContentColor the content color used for days that are within a date
     * range selection
     * @param dayInSelectionRangeContainerColor the container color used for days that are within a
     * date range selection
     * @param dividerColor the color used for the dividers used at the date pickers
     * @param dateTextFieldColors the [TextFieldColors] defaults for the date text field when in
     * [DisplayMode.Input]. See [OutlinedTextFieldDefaults.colors].
     */
    @Composable
    fun colors(
        containerColor: Color = DatePickerModalTokens.ContainerColor.toColor(),
        titleContentColor: Color = DatePickerModalTokens.HeaderSupportingTextColor.toColor(),
        headlineContentColor: Color = DatePickerModalTokens.HeaderHeadlineColor.toColor(),
        weekdayContentColor: Color = DatePickerModalTokens.WeekdaysLabelTextColor.toColor(),
        subheadContentColor: Color =
            DatePickerModalTokens.RangeSelectionMonthSubheadColor.toColor(),
        // TODO(b/234060211): Apply this from the MenuButton tokens or defaults.
        navigationContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        yearContentColor: Color =
            DatePickerModalTokens.SelectionYearUnselectedLabelTextColor.toColor(),
        // TODO: Using DisabledAlpha as there are no token values for the disabled states.
        disabledYearContentColor: Color = yearContentColor.copy(alpha = DisabledAlpha),
        currentYearContentColor: Color = DatePickerModalTokens.DateTodayLabelTextColor.toColor(),
        selectedYearContentColor: Color =
            DatePickerModalTokens.SelectionYearSelectedLabelTextColor.toColor(),
        disabledSelectedYearContentColor: Color =
            selectedYearContentColor.copy(alpha = DisabledAlpha),
        selectedYearContainerColor: Color =
            DatePickerModalTokens.SelectionYearSelectedContainerColor.toColor(),
        disabledSelectedYearContainerColor: Color =
            selectedYearContainerColor.copy(alpha = DisabledAlpha),
        dayContentColor: Color = DatePickerModalTokens.DateUnselectedLabelTextColor.toColor(),
        disabledDayContentColor: Color = dayContentColor.copy(alpha = DisabledAlpha),
        selectedDayContentColor: Color = DatePickerModalTokens.DateSelectedLabelTextColor.toColor(),
        disabledSelectedDayContentColor: Color =
            selectedDayContentColor.copy(alpha = DisabledAlpha),
        selectedDayContainerColor: Color =
            DatePickerModalTokens.DateSelectedContainerColor.toColor(),
        disabledSelectedDayContainerColor: Color =
            selectedDayContainerColor.copy(alpha = DisabledAlpha),
        todayContentColor: Color = DatePickerModalTokens.DateTodayLabelTextColor.toColor(),
        todayDateBorderColor: Color =
            DatePickerModalTokens.DateTodayContainerOutlineColor.toColor(),
        dayInSelectionRangeContentColor: Color =
            DatePickerModalTokens.SelectionDateInRangeLabelTextColor.toColor(),
        dayInSelectionRangeContainerColor: Color =
            DatePickerModalTokens.RangeSelectionActiveIndicatorContainerColor.toColor(),
        dividerColor: Color = DividerDefaults.color,
        dateTextFieldColors: TextFieldColors = OutlinedTextFieldDefaults.colors()
    ): DatePickerColors =
        DatePickerColors(
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

    /**
     * Returns a [DatePickerFormatter].
     *
     * The date formatter will apply the best possible localized form of the given skeleton and Locale.
     * A skeleton is similar to, and uses the same format characters as, a Unicode
     * <a href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> pattern.
     *
     * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
     * `en_US` locale, but "d. MMMM" in the `de_CH` locale.
     *
     * @param yearSelectionSkeleton a date format skeleton used to format the date picker's year
     * selection menu button (e.g. "March 2021").
     * @param selectedDateSkeleton a date format skeleton used to format a selected date (e.g.
     * "Mar 27, 2021")
     * @param selectedDateDescriptionSkeleton a date format skeleton used to format a selected date to
     * be used as content description for screen readers (e.g. "Saturday, March 27, 2021")
     */
    fun dateFormatter(
        yearSelectionSkeleton: String = YearMonthSkeleton,
        selectedDateSkeleton: String = YearAbbrMonthDaySkeleton,
        selectedDateDescriptionSkeleton: String = YearMonthWeekdayDaySkeleton
    ): DatePickerFormatter = DatePickerFormatterImpl(
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
            DisplayMode.Picker -> Text(
                text = getString(string = Strings.DatePickerTitle),
                modifier = modifier
            )

            DisplayMode.Input -> Text(
                text = getString(string = Strings.DateInputTitle),
                modifier = modifier
            )
        }
    }

    /**
     * A default date picker headline composable that displays a default headline text when there is
     * no date selection, and an actual date string when there is.
     *
     * @param selectedDateMillis a timestamp that represents the selected date _start_ of the day in
     * _UTC_ milliseconds from the epoch
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
        val formattedDate = dateFormatter.formatDate(
            dateMillis = selectedDateMillis,
            locale = defaultLocale
        )
        val verboseDateDescription = dateFormatter.formatDate(
            dateMillis = selectedDateMillis,
            locale = defaultLocale,
            forContentDescription = true
        ) ?: when (displayMode) {
            DisplayMode.Picker -> getString(Strings.DatePickerNoSelectionDescription)
            DisplayMode.Input -> getString(Strings.DateInputNoInputDescription)
            else -> ""
        }

        val headlineText = formattedDate ?: when (displayMode) {
            DisplayMode.Picker -> getString(Strings.DatePickerHeadline)
            DisplayMode.Input -> getString(Strings.DateInputHeadline)
            else -> ""
        }

        val headlineDescription = when (displayMode) {
            DisplayMode.Picker -> getString(Strings.DatePickerHeadlineDescription)
            DisplayMode.Input -> getString(Strings.DateInputHeadlineDescription)
            else -> ""
        }.format(verboseDateDescription)

        Text(
            text = headlineText,
            modifier = modifier.semantics {
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
        val density = LocalDensity.current
        return remember(density) {
            SnapFlingBehavior(
                lazyListState = lazyListState,
                decayAnimationSpec = decayAnimationSpec,
                snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                density = density
            )
        }
    }

    /** The range of years for the date picker dialogs. */
    val YearRange: IntRange = IntRange(1900, 2100)

    /** The default tonal elevation used for [DatePickerDialog]. */
    val TonalElevation: Dp = DatePickerModalTokens.ContainerElevation

    /** The default shape for date picker dialogs. */
    val shape: Shape @Composable get() = DatePickerModalTokens.ContainerShape.toShape()

    /**
     * A date format skeleton used to format the date picker's year selection menu button (e.g.
     * "March 2021")
     */
    const val YearMonthSkeleton: String = "yMMMM"

    /**
     * A date format skeleton used to format a selected date (e.g. "Mar 27, 2021")
     */
    const val YearAbbrMonthDaySkeleton: String = "yMMMd"

    /**
     * A date format skeleton used to format a selected date to be used as content description for
     * screen readers (e.g. "Saturday, March 27, 2021")
     */
    const val YearMonthWeekdayDaySkeleton: String = "yMMMMEEEEd"
}

/**
 * Represents the colors used by the date picker.
 *
 * See [DatePickerDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@ExperimentalMaterial3Api
@Immutable
class DatePickerColors internal constructor(
    internal val containerColor: Color,
    internal val titleContentColor: Color,
    internal val headlineContentColor: Color,
    internal val weekdayContentColor: Color,
    internal val subheadContentColor: Color,
    internal val navigationContentColor: Color,
    private val yearContentColor: Color,
    private val disabledYearContentColor: Color,
    private val currentYearContentColor: Color,
    private val selectedYearContentColor: Color,
    private val disabledSelectedYearContentColor: Color,
    private val selectedYearContainerColor: Color,
    private val disabledSelectedYearContainerColor: Color,
    private val dayContentColor: Color,
    private val disabledDayContentColor: Color,
    private val selectedDayContentColor: Color,
    private val disabledSelectedDayContentColor: Color,
    private val selectedDayContainerColor: Color,
    private val disabledSelectedDayContainerColor: Color,
    private val todayContentColor: Color,
    internal val todayDateBorderColor: Color,
    internal val dayInSelectionRangeContainerColor: Color,
    private val dayInSelectionRangeContentColor: Color,
    internal val dividerColor: Color,
    internal val dateTextFieldColors: TextFieldColors
) {
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
        val target = when {
            selected && enabled -> selectedDayContentColor
            selected && !enabled -> disabledSelectedDayContentColor
            inRange && enabled -> dayInSelectionRangeContentColor
            inRange && !enabled -> disabledDayContentColor
            isToday -> todayContentColor
            enabled -> dayContentColor
            else -> disabledDayContentColor
        }

        return if (inRange) {
            rememberUpdatedState(target)
        } else {
            // Animate the content color only when the day is not in a range.
            animateColorAsState(
                target,
                tween(durationMillis = MotionTokens.DurationShort2.toInt())
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
        val target = if (selected) {
            if (enabled) selectedDayContainerColor else disabledSelectedDayContainerColor
        } else {
            Color.Transparent
        }
        return if (animate) {
            animateColorAsState(
                target,
                tween(durationMillis = MotionTokens.DurationShort2.toInt())
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
        val target = when {
            selected && enabled -> selectedYearContentColor
            selected && !enabled -> disabledSelectedYearContentColor
            currentYear -> currentYearContentColor
            enabled -> yearContentColor
            else -> disabledYearContentColor
        }

        return animateColorAsState(
            target,
            tween(durationMillis = MotionTokens.DurationShort2.toInt())
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
        val target = if (selected) {
            if (enabled) selectedYearContainerColor else disabledSelectedYearContainerColor
        } else {
            Color.Transparent
        }
        return animateColorAsState(
            target,
            tween(durationMillis = MotionTokens.DurationShort2.toInt())
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
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a month to be displayed to the user. In case `null` is
 * provided, the displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited
 * to
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed.
 * In case a date is not allowed to be selected, it will appear disabled in the UI.
 * @see rememberDatePickerState
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent
 * a year that is out of the year range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal abstract class BaseDatePickerStateImpl(
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
    val yearRange: IntRange,
    val selectableDates: SelectableDates
) {

    val calendarModel = CalendarModel.Default

    private var _displayedMonth =
        mutableStateOf(if (initialDisplayedMonthMillis != null) {
            val month = calendarModel.getMonth(initialDisplayedMonthMillis)
            require(yearRange.contains(month.year)) {
                "The initial display month's year (${month.year}) is out of the years range of " +
                    "$yearRange."
            }
            month
        } else {
            // Set the displayed month to the current one.
            calendarModel.getMonth(calendarModel.today)
        })

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
 * @param initialSelectedDateMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a date. Provide a `null` to indicate no selection. Note
 * that the state's
 * [selectedDateMillis] will provide a timestamp that represents the _start_ of the day, which
 * may be different than the provided initialSelectedDateMillis.
 * @param initialDisplayedMonthMillis timestamp in _UTC_ milliseconds from the epoch that
 * represents an initial selection of a month to be displayed to the user. In case `null` is
 * provided, the displayed month would be the current one.
 * @param yearRange an [IntRange] that holds the year range that the date picker will be limited
 * to
 * @param initialDisplayMode an initial [DisplayMode] that this state will hold
 * @param selectableDates a [SelectableDates] that is consulted to check if a date is allowed.
 * In case a date is not allowed to be selected, it will appear disabled in the UI.
 * @see rememberDatePickerState
 * @throws [IllegalArgumentException] if the initial selected date or displayed month represent
 * a year that is out of the year range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class DatePickerStateImpl(
    @Suppress("AutoBoxing") initialSelectedDateMillis: Long?,
    @Suppress("AutoBoxing") initialDisplayedMonthMillis: Long?,
    yearRange: IntRange,
    initialDisplayMode: DisplayMode,
    selectableDates: SelectableDates
) : BaseDatePickerStateImpl(
    initialDisplayedMonthMillis,
    yearRange,
    selectableDates
), DatePickerState {

    /**
     * A mutable state of [CalendarDate] that represents a selected date.
     */
    private var _selectedDate =
        mutableStateOf(if (initialSelectedDateMillis != null) {
            val date = calendarModel.getCanonicalDate(initialSelectedDateMillis)
            require(yearRange.contains(date.year)) {
                "The provided initial date's year (${date.year}) is out of the years range " +
                    "of $yearRange."
            }
            date
        } else {
            null
        })

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
     * A mutable state of [DisplayMode] that represents the current display mode of the UI
     * (i.e. picker or input).
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
         * is allowed
         */
        fun Saver(selectableDates: SelectableDates): Saver<DatePickerStateImpl, Any> = listSaver(
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
                    selectableDates = selectableDates
                )
            }
        )
    }
}

/**
 * A date formatter used by [DatePicker].
 *
 * The date formatter will apply the best possible localized form of the given skeleton and Locale.
 * A skeleton is similar to, and uses the same format characters as, a Unicode
 * <a href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> pattern.
 *
 * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
 * `en_US` locale, but "d. MMMM" in the `de_CH` locale.
 *
 * @param yearSelectionSkeleton a date format skeleton used to format the date picker's year
 * selection menu button (e.g. "March 2021").
 * @param selectedDateSkeleton a date format skeleton used to format a selected date (e.g.
 * "Mar 27, 2021")
 * @param selectedDateDescriptionSkeleton a date format skeleton used to format a selected date to
 * be used as content description for screen readers (e.g. "Saturday, March 27, 2021")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Immutable
private class DatePickerFormatterImpl constructor(
    val yearSelectionSkeleton: String,
    val selectedDateSkeleton: String,
    val selectedDateDescriptionSkeleton: String
) : DatePickerFormatter {

    override fun formatMonthYear(
        monthMillis: Long?,
        locale: Locale
    ): String? {
        if (monthMillis == null) return null
        return formatWithSkeleton(monthMillis, yearSelectionSkeleton, locale)
    }

    override fun formatDate(
        dateMillis: Long?,
        locale: Locale,
        forContentDescription: Boolean
    ): String? {
        if (dateMillis == null) return null
        return formatWithSkeleton(
            dateMillis, if (forContentDescription) {
                selectedDateDescriptionSkeleton
            } else {
                selectedDateSkeleton
            },
            locale
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
        modifier = modifier
            .sizeIn(minWidth = DatePickerModalTokens.ContainerWidth)
            .semantics { isContainer = true }
    ) {
        DatePickerHeader(
            modifier = Modifier,
            title = title,
            titleContentColor = colors.titleContentColor,
            headlineContentColor = colors.headlineContentColor,
            minHeight = headerMinHeight
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val horizontalArrangement = when {
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
                            Box(modifier = Modifier.weight(1f)) {
                                headline()
                            }
                        }
                    }
                    modeToggleButton?.invoke()
                }
                // Display a divider only when there is a title, headline, or a mode toggle.
                if (title != null || headline != null || modeToggleButton != null) {
                    Divider(color = colors.dividerColor)
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
@OptIn(ExperimentalMaterial3Api::class)
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
    // TODO(b/266480386): Apply the motion spec for this once we have it. Consider replacing this
    //  with AnimatedContent when it's out of experimental.
    Crossfade(
        targetState = displayMode,
        animationSpec = spring(),
        modifier = Modifier.semantics { isContainer = true }) { mode ->
        when (mode) {
            DisplayMode.Picker -> DatePickerContent(
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

            DisplayMode.Input -> DateInputContent(
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

@OptIn(ExperimentalMaterial3Api::class)
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
        rememberLazyListState(initialFirstVisibleItemIndex = displayedMonth.indexIn(yearRange))
    val coroutineScope = rememberCoroutineScope()
    var yearPickerVisible by rememberSaveable { mutableStateOf(false) }
    val defaultLocale = defaultLocale()
    Column {
        MonthsNavigation(
            modifier = Modifier.padding(horizontal = DatePickerHorizontalPadding),
            nextAvailable = monthsListState.canScrollForward,
            previousAvailable = monthsListState.canScrollBackward,
            yearPickerVisible = yearPickerVisible,
            yearPickerText = dateFormatter.formatMonthYear(
                monthMillis = displayedMonthMillis,
                locale = defaultLocale
            ) ?: "-",
            onNextClicked = {
                coroutineScope.launch {
                    monthsListState.animateScrollToItem(
                        monthsListState.firstVisibleItemIndex + 1
                    )
                }
            },
            onPreviousClicked = {
                coroutineScope.launch {
                    monthsListState.animateScrollToItem(
                        monthsListState.firstVisibleItemIndex - 1
                    )
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
            androidx.compose.animation.AnimatedVisibility(
                visible = yearPickerVisible,
                modifier = Modifier.clipToBounds(),
                enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
                exit = shrinkVertically() + fadeOut()
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
                        modifier = Modifier
                            .requiredHeight(
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
                    Divider(color = colors.dividerColor)
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
        modifier
            .fillMaxWidth()
            .then(heightModifier),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (title != null) {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                val textStyle =
                    MaterialTheme.typography.fromToken(
                        DatePickerModalTokens.HeaderSupportingTextFont
                    )
                ProvideTextStyle(textStyle) {
                    Box(contentAlignment = Alignment.BottomStart) {
                        title()
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides headlineContentColor, content = content
        )
    }
}

/**
 * Composes a horizontal pageable list of months.
 */
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
    val firstMonth = remember(yearRange) {
        calendarModel.getMonth(
            year = yearRange.first,
            month = 1 // January
        )
    }
    LazyRow(
        // Apply this to prevent the screen reader from scrolling to the next or previous month, and
        // instead, traverse outside the Month composable when swiping from a focused first or last
        // day of the month.
        modifier = Modifier.semantics {
            horizontalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
        },
        state = lazyListState,
        // TODO(b/264687693): replace with the framework's rememberSnapFlingBehavior(lazyListState)
        //  when promoted to stable
        flingBehavior = DatePickerDefaults.rememberSnapFlingBehavior(lazyListState)
    ) {
        items(numberOfMonthsInRange(yearRange)) {
            val month = calendarModel.plusMonths(
                from = firstMonth,
                addedMonthsCount = it
            )
            Box(
                modifier = Modifier.fillParentMaxWidth()
            ) {
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
    snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
        val yearOffset = lazyListState.firstVisibleItemIndex / 12
        val month = lazyListState.firstVisibleItemIndex % 12 + 1
        onDisplayedMonthChange(
            calendarModel.getMonth(
                year = yearRange.first + yearOffset,
                month = month
            ).startUtcTimeMillis
        )
    }
}

/**
 * Composes the weekdays letters.
 */
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
    CompositionLocalProvider(LocalContentColor provides colors.weekdayContentColor) {
        val textStyle =
            MaterialTheme.typography.fromToken(DatePickerModalTokens.WeekdaysLabelTextFont)
        ProvideTextStyle(value = textStyle) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minHeight = RecommendedSizeForAccessibility
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayNames.forEach {
                    Box(
                        modifier = Modifier
                            .clearAndSetSemantics { contentDescription = it.first }
                            .size(
                                width = RecommendedSizeForAccessibility,
                                height = RecommendedSizeForAccessibility
                            ),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = it.second,
                            modifier = Modifier.wrapContentSize(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable that renders a calendar month and displays a date selection.
 */
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
    val rangeSelectionDrawModifier = if (rangeSelectionInfo != null) {
        Modifier.drawWithContent {
            drawRangeBackground(rangeSelectionInfo, colors.dayInSelectionRangeContainerColor)
            drawContent()
        }
    } else {
        Modifier
    }

    val defaultLocale = defaultLocale()
    ProvideTextStyle(
        MaterialTheme.typography.fromToken(DatePickerModalTokens.DateLabelTextFont)
    ) {
        var cellIndex = 0
        Column(
            modifier = Modifier
                .requiredHeight(RecommendedSizeForAccessibility * MaxCalendarRows)
                .then(rangeSelectionDrawModifier),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(MaxCalendarRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(DaysInWeek) {
                        if (cellIndex < month.daysFromStartOfWeekToFirstOfMonth ||
                            cellIndex >=
                            (month.daysFromStartOfWeekToFirstOfMonth + month.numberOfDays)
                        ) {
                            // Empty cell
                            Spacer(
                                modifier = Modifier.requiredSize(
                                    width = RecommendedSizeForAccessibility,
                                    height = RecommendedSizeForAccessibility
                                )
                            )
                        } else {
                            val dayNumber = cellIndex - month.daysFromStartOfWeekToFirstOfMonth
                            val dateInMillis = month.startUtcTimeMillis +
                                (dayNumber * MillisecondsIn24Hours)
                            val isToday = dateInMillis == todayMillis
                            val startDateSelected = dateInMillis == startDateMillis
                            val endDateSelected = dateInMillis == endDateMillis
                            val inRange = remember(rangeSelectionInfo, dateInMillis) {
                                derivedStateOf {
                                    rangeSelectionInfo != null &&
                                        dateInMillis >= (startDateMillis
                                        ?: Long.Companion.MAX_VALUE) &&
                                        dateInMillis <= (endDateMillis ?: Long.MIN_VALUE)
                                }
                            }
                            val dayContentDescription = dayContentDescription(
                                rangeSelectionEnabled = rangeSelectionInfo != null,
                                isToday = isToday,
                                isStartDate = startDateSelected,
                                isEndDate = endDateSelected,
                                isInRange = inRange.value
                            )
                            val formattedDateDescription = dateFormatter.formatDate(
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
                                enabled = remember(dateInMillis) {
                                    // Disabled a day in case its year is not selectable, or the
                                    // date itself is specifically not allowed by the state's
                                    // SelectableDates.
                                    with(selectableDates) {
                                        isSelectableYear(month.year) &&
                                            isSelectableDate(dateInMillis)
                                    }
                                },
                                today = isToday,
                                inRange = inRange.value,
                                description = if (dayContentDescription != null) {
                                    "$dayContentDescription, $formattedDateDescription"
                                } else {
                                    formattedDateDescription
                                },
                                colors = colors
                            ) {
                                Text(
                                    text = (dayNumber + 1).toLocalString(),
                                    // The semantics are set at the Day level.
                                    modifier = Modifier.clearAndSetSemantics { },
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
}

/**
 * Returns the number of months within the given year range.
 */
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
            isStartDate -> descriptionBuilder.append(
                getString(string = Strings.DateRangePickerStartHeadline)
            )

            isEndDate -> descriptionBuilder.append(
                getString(string = Strings.DateRangePickerEndHeadline)
            )

            isInRange -> descriptionBuilder.append(
                getString(string = Strings.DateRangePickerDayInRange)
            )
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
        modifier = modifier
            .minimumInteractiveComponentSize()
            .requiredSize(
                DatePickerModalTokens.DateStateLayerWidth,
                DatePickerModalTokens.DateStateLayerHeight
            )
            // Apply and merge semantics here. This will ensure that when scrolling the list the
            // entire Day surface is treated as one unit and holds the date semantics even when it's
            // not completely visible atm.
            .semantics(mergeDescendants = true) {
                text = AnnotatedString(description)
                role = Role.Button
            },
        enabled = enabled,
        shape = DatePickerModalTokens.DateContainerShape.toShape(),
        color = colors.dayContainerColor(
            selected = selected,
            enabled = enabled,
            animate = animateChecked
        ).value,
        contentColor = colors.dayContentColor(
            isToday = today,
            selected = selected,
            inRange = inRange,
            enabled = enabled,
        ).value,
        border = if (today && !selected) {
            BorderStroke(
                DatePickerModalTokens.DateTodayContainerOutlineWidth,
                colors.todayDateBorderColor
            )
        } else {
            null
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
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
    ProvideTextStyle(
        value = MaterialTheme.typography.fromToken(DatePickerModalTokens.SelectionYearLabelTextFont)
    ) {
        val currentYear = calendarModel.getMonth(calendarModel.today).year
        val displayedYear = calendarModel.getMonth(displayedMonthMillis).year
        val lazyGridState =
            rememberLazyGridState(
                // Set the initial index to a few years before the current year to allow quicker
                // selection of previous years.
                initialFirstVisibleItemIndex = max(
                    0, displayedYear - yearRange.first - YearsInRow
                )
            )
        // Match the years container color to any elevated surface color that is composed under it.
        val containerColor = if (colors.containerColor == MaterialTheme.colorScheme.surface) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)
        } else {
            colors.containerColor
        }
        val coroutineScope = rememberCoroutineScope()
        val scrollToEarlierYearsLabel = getString(Strings.DatePickerScrollToShowEarlierYears)
        val scrollToLaterYearsLabel = getString(Strings.DatePickerScrollToShowLaterYears)
        LazyVerticalGrid(
            columns = GridCells.Fixed(YearsInRow),
            modifier = modifier
                .background(containerColor)
                // Apply this to have the screen reader traverse outside the visible list of years
                // and not scroll them by default.
                .semantics {
                    verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                },
            state = lazyGridState,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(YearsVerticalPadding)
        ) {
            items(yearRange.count()) {
                val selectedYear = it + yearRange.first
                val localizedYear = selectedYear.toLocalString()
                Year(
                    modifier = Modifier
                        .requiredSize(
                            width = DatePickerModalTokens.SelectionYearContainerWidth,
                            height = DatePickerModalTokens.SelectionYearContainerHeight
                        )
                        .semantics {
                            // Apply a11y custom actions to the first and last items in the years
                            // grid. The actions will suggest to scroll to earlier or later years in
                            // the grid.
                            customActions = if (lazyGridState.firstVisibleItemIndex == it ||
                                lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == it
                            ) {
                                customScrollActions(
                                    state = lazyGridState,
                                    coroutineScope = coroutineScope,
                                    scrollUpLabel = scrollToEarlierYearsLabel,
                                    scrollDownLabel = scrollToLaterYearsLabel
                                )
                            } else {
                                emptyList()
                            }
                        },
                    selected = selectedYear == displayedYear,
                    currentYear = selectedYear == currentYear,
                    onClick = { onYearSelected(selectedYear) },
                    enabled = selectableDates.isSelectableYear(selectedYear),
                    description = getString(Strings.DatePickerNavigateToYearDescription)
                        .format(localizedYear),
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
    val border = remember(currentYear, selected) {
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
        modifier = modifier.semantics(mergeDescendants = true) {
            text = AnnotatedString(description)
            role = Role.Button
        },
        enabled = enabled,
        shape = DatePickerModalTokens.SelectionYearStateLayerShape.toShape(),
        color = colors.yearContainerColor(selected = selected, enabled = enabled).value,
        contentColor = colors.yearContentColor(
            currentYear = currentYear,
            selected = selected,
            enabled = enabled
        ).value,
        border = border,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            content()
        }
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
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(MonthYearHeight),
        horizontalArrangement = if (yearPickerVisible) {
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
                Text(text = yearPickerText,
                    modifier = Modifier.semantics {
                        // Make the screen reader read out updates to the menu button text as the
                        // user navigates the arrows or scrolls to change the displayed month.
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = yearPickerText
                    })
            }
            // Show arrows for traversing months (only visible when the year selection is off)
            if (!yearPickerVisible) {
                Row {
                    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    IconButton(onClick = onPreviousClicked, enabled = previousAvailable) {
                        Icon(
                            if (rtl) {
                                Icons.Filled.KeyboardArrowRight
                            } else {
                                Icons.Filled.KeyboardArrowLeft
                            },
                            contentDescription = getString(Strings.DatePickerSwitchToPreviousMonth)
                        )
                    }
                    IconButton(onClick = onNextClicked, enabled = nextAvailable) {
                        Icon(
                            if (rtl) {
                                Icons.Filled.KeyboardArrowLeft
                            } else {
                                Icons.Filled.KeyboardArrowRight
                            },
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
            contentDescription = if (expanded) {
                getString(Strings.DatePickerSwitchToDaySelection)
            } else {
                getString(Strings.DatePickerSwitchToYearSelection)
            },
            Modifier.rotate(if (expanded) 180f else 0f)
        )
    }
}

private fun customScrollActions(
    state: LazyGridState,
    coroutineScope: CoroutineScope,
    scrollUpLabel: String,
    scrollDownLabel: String
): List<CustomAccessibilityAction> {
    val scrollUpAction = {
        if (!state.canScrollBackward) {
            false
        } else {
            coroutineScope.launch {
                state.scrollToItem(state.firstVisibleItemIndex - YearsInRow)
            }
            true
        }
    }
    val scrollDownAction = {
        if (!state.canScrollForward) {
            false
        } else {
            coroutineScope.launch {
                state.scrollToItem(state.firstVisibleItemIndex + YearsInRow)
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

/**
 * Returns a string representation of an integer at the current Locale.
 */
internal fun Int.toLocalString(): String {
    val formatter = NumberFormat.getIntegerInstance()
    // Eliminate any use of delimiters when formatting the integer.
    formatter.isGroupingUsed = false
    return formatter.format(this)
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
