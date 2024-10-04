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

package androidx.wear.compose.material3

import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Strings.Companion.DatePickerDay
import androidx.wear.compose.material3.internal.Strings.Companion.DatePickerMonth
import androidx.wear.compose.material3.internal.Strings.Companion.DatePickerYear
import androidx.wear.compose.material3.internal.Strings.Companion.PickerConfirmButtonContentDescription
import androidx.wear.compose.material3.internal.Strings.Companion.PickerNextButtonContentDescription
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.DatePickerTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full screen date picker with day, month, year.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts.
 *
 * Example of a [DatePicker]:
 *
 * @sample androidx.wear.compose.material3.samples.DatePickerSample
 *
 * Example of a [DatePicker] shows the picker options in year-month-day order:
 *
 * @sample androidx.wear.compose.material3.samples.DatePickerYearMonthDaySample
 *
 * Example of a [DatePicker] with minDate and maxDate:
 *
 * @sample androidx.wear.compose.material3.samples.DatePickerMinDateMaxDateSample
 * @param initialDate The initial value to be displayed in the DatePicker.
 * @param onDatePicked The callback that is called when the user confirms the date selection. It
 *   provides the selected date as [LocalDate]
 * @param modifier Modifier to be applied to the `Box` containing the UI elements.
 * @param minDate Optional minimum date that can be selected in the DatePicker (inclusive).
 * @param maxDate Optional maximum date that can be selected in the DatePicker (inclusive).
 * @param datePickerType The different [DatePickerType] supported by this date picker.
 * @param colors [DatePickerColors] to be applied to the DatePicker.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePicker(
    initialDate: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    datePickerType: DatePickerType = DatePickerDefaults.datePickerType,
    colors: DatePickerColors = DatePickerDefaults.datePickerColors()
) {
    val inspectionMode = LocalInspectionMode.current
    val fullyDrawn = remember { Animatable(if (inspectionMode) 1f else 0f) }

    if (minDate != null && maxDate != null) {
        verifyDates(initialDate, minDate, maxDate)
    }

    val datePickerState = remember(initialDate) { DatePickerState(initialDate, minDate, maxDate) }

    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    /** The current selected [Picker] index. */
    var selectedIndex by
        remember(touchExplorationServicesEnabled) {
            // When the date picker loads, none of the individual pickers are selected in talkback
            // mode,
            // otherwise first picker should be focused (depends on the picker ordering given by
            // datePickerType)
            val initiallySelectedIndex =
                if (touchExplorationServicesEnabled) {
                    null
                } else {
                    0
                }
            mutableStateOf(initiallySelectedIndex)
        }

    val isLargeScreen = LocalConfiguration.current.screenWidthDp > 225
    val labelTextStyle =
        if (isLargeScreen) {
            DatePickerTokens.PickerLabelLargeTypography.value
        } else {
            DatePickerTokens.PickerLabelTypography.value
        }
    val optionTextStyle =
        if (isLargeScreen) {
            DatePickerTokens.PickerContentLargeTypography.value
        } else {
            DatePickerTokens.PickerContentTypography.value
        }
    val optionHeight = if (isLargeScreen) 48.dp else 36.dp

    val focusRequesterConfirmButton = remember { FocusRequester() }

    val yearString = getString(DatePickerYear)
    val monthString = getString(DatePickerMonth)
    val dayString = getString(DatePickerDay)

    LaunchedEffect(datePickerState.isMinYearSelected, datePickerState.isMaxYearSelected) {
        datePickerState.adjustMonthOption()
    }
    LaunchedEffect(
        datePickerState.yearState.selectedOptionIndex,
        datePickerState.monthState.selectedOptionIndex
    ) {
        datePickerState.adjustDayOption()
    }

    val shortMonthNames = remember { getMonthNames("MMM") }
    val fullMonthNames = remember { getMonthNames("MMMM") }
    val yearContentDescription by
        remember(
            selectedIndex,
            datePickerState.currentYear(),
        ) {
            derivedStateOf {
                createDescriptionDatePicker(
                    selectedIndex,
                    datePickerState.currentYear(),
                    yearString,
                )
            }
        }
    val monthContentDescription by
        remember(
            selectedIndex,
            datePickerState.currentMonth(),
        ) {
            derivedStateOf {
                if (selectedIndex == null) {
                    monthString
                } else {
                    fullMonthNames[(datePickerState.currentMonth() - 1) % 12]
                }
            }
        }
    val dayContentDescription by
        remember(
            selectedIndex,
            datePickerState.currentDay(),
        ) {
            derivedStateOf {
                createDescriptionDatePicker(
                    selectedIndex,
                    datePickerState.currentDay(),
                    dayString,
                )
            }
        }

    val datePickerOptions = datePickerType.toDatePickerOptions()
    val confirmButtonIndex = datePickerOptions.size

    val onPickerSelected = { current: Int, next: Int ->
        if (selectedIndex != current) {
            selectedIndex = current
        } else {
            selectedIndex = next
            if (next == confirmButtonIndex) {
                focusRequesterConfirmButton.requestFocus()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().alpha(fullyDrawn.value)) {
        val boxConstraints = this
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text =
                    selectedIndex?.let {
                        when (datePickerOptions.getOrNull(it)) {
                            DatePickerOption.Day -> dayString
                            DatePickerOption.Month -> monthString
                            DatePickerOption.Year -> yearString
                            else -> ""
                        }
                    } ?: "",
                color = colors.pickerLabelColor,
                style = labelTextStyle,
                maxLines = 1,
            )
            Spacer(Modifier.height(if (isLargeScreen) 6.dp else 4.dp))
            FontScaleIndependent {
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val (digitWidth, maxMonthWidth) =
                    remember(
                        density.density,
                        LocalConfiguration.current.screenWidthDp,
                    ) {
                        val mm =
                            measurer.measure(
                                "0123456789\n" + shortMonthNames.joinToString("\n"),
                                style = optionTextStyle,
                                density = density,
                            )

                        ((0..9).maxOf { mm.getBoundingBox(it).width }) to
                            ((1..12).maxOf { mm.getLineRight(it) - mm.getLineLeft(it) })
                    }

                // Add spaces on to allow room to grow
                val dayWidth =
                    with(LocalDensity.current) {
                        maxOf(
                            // Add 1dp buffer to compensate for potential conversion loss
                            (digitWidth * 2).toDp() + 1.dp,
                            minimumInteractiveComponentSize
                        )
                    }
                val monthYearWidth =
                    with(LocalDensity.current) {
                        maxOf(
                            // Add 1dp buffer to compensate for potential conversion loss
                            maxOf(maxMonthWidth.toDp(), (digitWidth * 4).toDp()) + 1.dp,
                            minimumInteractiveComponentSize
                        )
                    }

                Row(
                    modifier =
                        Modifier.fillMaxWidth().weight(1f).offset {
                            IntOffset(
                                getPickerGroupRowOffset(
                                        boxConstraints.maxWidth,
                                        dayWidth,
                                        monthYearWidth,
                                        monthYearWidth,
                                        touchExplorationServicesEnabled,
                                        selectedIndex,
                                    )
                                    .roundToPx(),
                                0
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val spacing = if (isLargeScreen) 6.dp else 4.dp
                    // Pass a negative value as the selected picker index when none is selected.
                    PickerGroup(
                        selectedPickerIndex = selectedIndex ?: -1,
                        onPickerSelected = { selectedIndex = it },
                        autoCenter = true,
                        separator = { Spacer(Modifier.width(if (isLargeScreen) 12.dp else 8.dp)) },
                    ) {
                        datePickerOptions.forEachIndexed { index, datePickerOption ->
                            when (datePickerOption) {
                                DatePickerOption.Day ->
                                    pickerGroupItem(
                                        pickerState = datePickerState.dayState,
                                        modifier = Modifier.width(dayWidth).fillMaxHeight(),
                                        onSelected = { onPickerSelected(index, index + 1) },
                                        contentDescription = dayContentDescription,
                                        option =
                                            pickerTextOption(
                                                textStyle = optionTextStyle,
                                                indexToText = {
                                                    "%02d".format(datePickerState.currentDay(it))
                                                },
                                                optionHeight = optionHeight,
                                                selectedContentColor =
                                                    colors.selectedPickerContentColor,
                                                unselectedContentColor =
                                                    colors.unselectedPickerContentColor,
                                            ),
                                        spacing = spacing,
                                    )
                                DatePickerOption.Month ->
                                    pickerGroupItem(
                                        pickerState = datePickerState.monthState,
                                        modifier = Modifier.width(monthYearWidth).fillMaxHeight(),
                                        onSelected = { onPickerSelected(index, index + 1) },
                                        contentDescription = monthContentDescription,
                                        option =
                                            pickerTextOption(
                                                textStyle = optionTextStyle,
                                                indexToText = {
                                                    shortMonthNames[
                                                        (datePickerState.currentMonth(it) - 1) % 12]
                                                },
                                                optionHeight = optionHeight,
                                                selectedContentColor =
                                                    colors.selectedPickerContentColor,
                                                unselectedContentColor =
                                                    colors.unselectedPickerContentColor,
                                            ),
                                        spacing = spacing,
                                    )
                                DatePickerOption.Year ->
                                    pickerGroupItem(
                                        pickerState = datePickerState.yearState,
                                        modifier = Modifier.width(monthYearWidth).fillMaxHeight(),
                                        onSelected = { onPickerSelected(index, index + 1) },
                                        contentDescription = yearContentDescription,
                                        option =
                                            pickerTextOption(
                                                textStyle = optionTextStyle,
                                                indexToText = {
                                                    "%4d".format(datePickerState.currentYear(it))
                                                },
                                                optionHeight = optionHeight,
                                                selectedContentColor =
                                                    colors.selectedPickerContentColor,
                                                unselectedContentColor =
                                                    colors.unselectedPickerContentColor,
                                            ),
                                        spacing = spacing,
                                    )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (isLargeScreen) 6.dp else 4.dp))
            EdgeButton(
                onClick = {
                    selectedIndex?.let { selectedIndex ->
                        if (selectedIndex >= 2) {
                            val confirmedYear: Int = datePickerState.currentYear()
                            val confirmedMonth: Int = datePickerState.currentMonth()
                            val confirmedDay: Int = datePickerState.currentDay()
                            val confirmedDate =
                                LocalDate.of(confirmedYear, confirmedMonth, confirmedDay)
                            onDatePicked(confirmedDate)
                        } else {
                            onPickerSelected(selectedIndex, selectedIndex + 1)
                        }
                    }
                },
                modifier =
                    Modifier.semantics { focused = (selectedIndex == confirmButtonIndex) }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable(),
                colors =
                    if (selectedIndex?.let { it >= 2 } == true) {
                        buttonColors(
                            contentColor = colors.confirmButtonContentColor,
                            containerColor = colors.confirmButtonContainerColor,
                        )
                    } else {
                        filledTonalButtonColors(
                            contentColor = colors.nextButtonContentColor,
                            containerColor = colors.nextButtonContainerColor,
                        )
                    }
            ) {
                // If none is selected (selectedIndex == null) we show 'next' instead of 'confirm'.
                val showConfirm = selectedIndex?.let { it >= 2 } == true
                Icon(
                    imageVector =
                        if (showConfirm) {
                            Icons.Check
                        } else {
                            Icons.AutoMirrored.KeyboardArrowRight
                        },
                    contentDescription =
                        if (showConfirm) {
                            getString(PickerConfirmButtonContentDescription)
                        } else {
                            // If none is selected, return the 'next' content description.
                            getString(PickerNextButtonContentDescription)
                        },
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    if (!inspectionMode) {
        LaunchedEffect(Unit) { fullyDrawn.animateTo(1f) }
    }
}

/** Specifies the types of columns to display in the DatePicker. */
@Immutable
@JvmInline
value class DatePickerType internal constructor(internal val value: Int) {

    companion object {
        val DayMonthYear = DatePickerType(0)
        val MonthDayYear = DatePickerType(1)
        val YearMonthDay = DatePickerType(2)
    }

    override fun toString(): String {
        return when (this) {
            DayMonthYear -> "DayMonthYear"
            MonthDayYear -> "MonthDayYear"
            YearMonthDay -> "YearMonthDay"
            else -> "Unknown"
        }
    }
}

/** Contains the default values used by [DatePicker] */
object DatePickerDefaults {

    /** The default [DatePickerType] for [DatePicker] aligns with the current system date format. */
    val datePickerType: DatePickerType
        @Composable
        get() {
            val formatOrder = DateFormat.getDateFormatOrder(LocalContext.current)
            return when (formatOrder[0]) {
                'M' -> DatePickerType.MonthDayYear
                'y' -> DatePickerType.YearMonthDay
                else -> DatePickerType.DayMonthYear
            }
        }

    /** Creates a [DatePickerColors] for a [DatePicker]. */
    @Composable fun datePickerColors() = MaterialTheme.colorScheme.defaultDatePickerColors

    /**
     * Creates a [DatePickerColors] for a [DatePicker].
     *
     * @param selectedPickerContentColor The content color of selected picker.
     * @param unselectedPickerContentColor The content color of unselected picker.
     * @param pickerLabelColor The color of the picker label.
     * @param nextButtonContentColor The content color of the next button.
     * @param nextButtonContainerColor The container color of the next button.
     * @param confirmButtonContentColor The content color of the confirm button.
     * @param confirmButtonContainerColor The container color of the confirm button.
     */
    @Composable
    fun datePickerColors(
        selectedPickerContentColor: Color = Color.Unspecified,
        unselectedPickerContentColor: Color = Color.Unspecified,
        pickerLabelColor: Color = Color.Unspecified,
        nextButtonContentColor: Color = Color.Unspecified,
        nextButtonContainerColor: Color = Color.Unspecified,
        confirmButtonContentColor: Color = Color.Unspecified,
        confirmButtonContainerColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultDatePickerColors.copy(
            selectedPickerContentColor = selectedPickerContentColor,
            unselectedPickerContentColor = unselectedPickerContentColor,
            pickerLabelColor = pickerLabelColor,
            nextButtonContentColor = nextButtonContentColor,
            nextButtonContainerColor = nextButtonContainerColor,
            confirmButtonContentColor = confirmButtonContentColor,
            confirmButtonContainerColor = confirmButtonContainerColor,
        )

    private val ColorScheme.defaultDatePickerColors: DatePickerColors
        get() {
            return defaultDatePickerColorsCached
                ?: DatePickerColors(
                        selectedPickerContentColor =
                            fromToken(DatePickerTokens.SelectedPickerContentColor),
                        unselectedPickerContentColor =
                            fromToken(DatePickerTokens.UnselectedPickerContentColor),
                        pickerLabelColor = fromToken(DatePickerTokens.PickerLabelColor),
                        nextButtonContentColor = fromToken(DatePickerTokens.NextButtonContentColor),
                        nextButtonContainerColor =
                            fromToken(DatePickerTokens.NextButtonContainerColor),
                        confirmButtonContentColor =
                            fromToken(DatePickerTokens.ConfirmButtonContentColor),
                        confirmButtonContainerColor =
                            fromToken(DatePickerTokens.ConfirmButtonContainerColor),
                    )
                    .also { defaultDatePickerColorsCached = it }
        }
}

@Immutable
class DatePickerColors(
    val selectedPickerContentColor: Color,
    val unselectedPickerContentColor: Color,
    val pickerLabelColor: Color,
    val nextButtonContentColor: Color,
    val nextButtonContainerColor: Color,
    val confirmButtonContentColor: Color,
    val confirmButtonContainerColor: Color,
) {
    internal fun copy(
        selectedPickerContentColor: Color,
        unselectedPickerContentColor: Color,
        pickerLabelColor: Color,
        nextButtonContentColor: Color,
        nextButtonContainerColor: Color,
        confirmButtonContentColor: Color,
        confirmButtonContainerColor: Color,
    ) =
        DatePickerColors(
            selectedPickerContentColor =
                selectedPickerContentColor.takeOrElse { this.selectedPickerContentColor },
            unselectedPickerContentColor =
                unselectedPickerContentColor.takeOrElse { this.unselectedPickerContentColor },
            pickerLabelColor = pickerLabelColor.takeOrElse { this.pickerLabelColor },
            nextButtonContentColor =
                nextButtonContentColor.takeOrElse { this.nextButtonContentColor },
            nextButtonContainerColor =
                nextButtonContainerColor.takeOrElse { this.nextButtonContainerColor },
            confirmButtonContentColor =
                confirmButtonContentColor.takeOrElse { this.confirmButtonContentColor },
            confirmButtonContainerColor =
                confirmButtonContainerColor.takeOrElse { this.confirmButtonContainerColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DatePickerColors) return false

        if (selectedPickerContentColor != other.selectedPickerContentColor) return false
        if (unselectedPickerContentColor != other.unselectedPickerContentColor) return false
        if (pickerLabelColor != other.pickerLabelColor) return false
        if (nextButtonContentColor != other.nextButtonContentColor) return false
        if (nextButtonContainerColor != other.nextButtonContainerColor) return false
        if (confirmButtonContentColor != other.confirmButtonContentColor) return false
        if (confirmButtonContainerColor != other.confirmButtonContainerColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedPickerContentColor.hashCode()
        result = 31 * result + unselectedPickerContentColor.hashCode()
        result = 31 * result + pickerLabelColor.hashCode()
        result = 31 * result + nextButtonContentColor.hashCode()
        result = 31 * result + nextButtonContainerColor.hashCode()
        result = 31 * result + confirmButtonContentColor.hashCode()
        result = 31 * result + confirmButtonContainerColor.hashCode()

        return result
    }
}

/** Represents the possible column options for the DatePicker. */
private enum class DatePickerOption {
    Day,
    Month,
    Year
}

private fun DatePickerType.toDatePickerOptions() =
    when (value) {
        DatePickerType.YearMonthDay.value ->
            arrayOf(DatePickerOption.Year, DatePickerOption.Month, DatePickerOption.Day)
        DatePickerType.MonthDayYear.value ->
            arrayOf(DatePickerOption.Month, DatePickerOption.Day, DatePickerOption.Year)
        else -> arrayOf(DatePickerOption.Day, DatePickerOption.Month, DatePickerOption.Year)
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun verifyDates(
    date: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
) {
    require(maxDate >= minDate) { "maxDate should be greater than or equal to minDate" }
    require(date in minDate..maxDate) { "date should lie between minDate and maxDate" }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun getMonthNames(pattern: String): List<String> {
    val monthFormatter = DateTimeFormatter.ofPattern(pattern)
    val months = 1..12
    return months.map { LocalDate.of(2022, it, 1).format(monthFormatter) }
}

private fun getPickerGroupRowOffset(
    rowWidth: Dp,
    dayPickerWidth: Dp,
    monthPickerWidth: Dp,
    yearPickerWidth: Dp,
    touchExplorationServicesEnabled: Boolean,
    selectedIndex: Int?,
): Dp {
    val currentOffset = (rowWidth - (dayPickerWidth + monthPickerWidth + yearPickerWidth)) / 2

    return if (touchExplorationServicesEnabled && selectedIndex == null) {
        ((rowWidth - dayPickerWidth) / 2) - currentOffset
    } else if (touchExplorationServicesEnabled && selectedIndex!! > 2) {
        ((rowWidth - yearPickerWidth) / 2) - (dayPickerWidth + monthPickerWidth + currentOffset)
    } else {
        0.dp
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private class DatePickerState(
    initialDate: LocalDate,
    initialDateMinYear: LocalDate?,
    initialDateMaxYear: LocalDate?,
) {
    // Year range 1900 - 2100 was suggested in b/277885199
    private val minDate = initialDateMinYear ?: LocalDate.of(1900, 1, 1)
    private val maxDate = initialDateMaxYear ?: LocalDate.of(2100, 12, 31)

    /** The [PickerState] for the year picker. */
    val yearState =
        (maxDate.year - minDate.year + 1).let {
            PickerState(
                initialNumberOfOptions = it,
                initiallySelectedIndex = initialDate.year - minDate.year,
                shouldRepeatOptions = it > 2,
            )
        }

    /** The [PickerState] for the month picker. */
    val monthState: PickerState
        get() = dynamicMonthState.activeState

    /** The [DynamicDayState] instance that manages the state of the day picker. */
    val dayState
        get() = dynamicDayState.activeState

    /** The [DynamicMonthState] instance that manages the state of the month picker. */
    private val dynamicMonthState =
        DynamicMonthState(
            date = initialDate,
            minDate = minDate,
            maxDate = maxDate,
            currentYear = ::currentYear,
        )

    /** The [DynamicDayState] instance that manages the state of the day picker. */
    private val dynamicDayState =
        DynamicDayState(
            date = initialDate,
            minDate = minDate,
            maxDate = maxDate,
            currentYear = ::currentYear,
            currentMonth = ::currentMonth,
        )

    /** Adjusts the month options based on the currently selected year. */
    suspend fun adjustMonthOption() = dynamicMonthState.adjustMonthOption()

    /** Adjusts the day options based on the currently selected year and month. */
    suspend fun adjustDayOption() = dynamicDayState.adjustDayOption()

    fun currentYear(year: Int = yearState.selectedOptionIndex): Int {
        return year + minDate.year
    }

    fun currentMonth(monthIndex: Int = monthState.selectedOptionIndex): Int {
        return monthIndex + dynamicMonthState.currentMinMonthOption
    }

    fun currentDay(day: Int = dayState.selectedOptionIndex): Int {
        return day + dynamicDayState.currentMinDayOption
    }

    val isMinYearSelected: Boolean
        get() = minDate?.year == currentYear()

    val isMaxYearSelected: Boolean
        get() = maxDate?.year == currentYear()
}

/**
 * Manages the state of the month picker dynamically, switching between repeating and non-repeating
 * PickerState instances based on the selected year and the provided minimum and maximum dates.
 *
 * @param date The initial date.
 * @param minDate The minimum selectable date.
 * @param maxDate The maximum selectable date.
 * @param currentYear A function that returns the currently selected year.
 */
@RequiresApi(Build.VERSION_CODES.O)
private class DynamicMonthState(
    date: LocalDate,
    private val minDate: LocalDate,
    private val maxDate: LocalDate,
    private val currentYear: () -> Int,
) {
    /** The currently active [PickerState] for the month picker. */
    val activeState: PickerState
        get() = getMonthState(currentYear())

    /** The minimum month option currently selectable. */
    val currentMinMonthOption: Int
        get() = minMonthOption(currentYear())

    private fun minMonthOption(year: Int) =
        if (year == minDate.year) {
            minDate.monthValue
        } else {
            1
        }

    private fun maxMonthOption(year: Int) =
        if (year == maxDate.year) {
            maxDate.monthValue
        } else {
            12
        }

    private fun numberOfOptions(year: Int): Int = maxMonthOption(year) - minMonthOption(year) + 1

    private var prevAdjustedYear: Int? = null

    /**
     * Adjusts the month options and scrolls to the appropriate month when the selected year
     * changes.
     */
    suspend fun adjustMonthOption() {
        val prevYear = prevAdjustedYear
        prevAdjustedYear = currentYear()
        if (prevYear == null) {
            return
        }

        val prevSelectedMonth =
            minMonthOption(prevYear) + getMonthState(prevYear).selectedOptionIndex

        val currentState = activeState
        val currentNumberOfOptions = numberOfOptions(currentYear())
        if (
            currentState.shouldRepeatOptions &&
                currentState.numberOfOptions != currentNumberOfOptions
        ) {
            currentState.numberOfOptions = currentNumberOfOptions
        }

        val scrollToMonthIndex =
            (prevSelectedMonth - currentMinMonthOption).coerceIn(
                0,
                currentState.numberOfOptions - 1
            )
        if (currentState.selectedOptionIndex != scrollToMonthIndex) {
            currentState.scrollToOption(scrollToMonthIndex)
        }
    }

    private fun getMonthState(year: Int) =
        when {
            // If minDate.year == maxDate.year and no repeat, minNoRepeatMonthState will be used.
            year == minDate.year && minNoRepeatMonthState != null -> minNoRepeatMonthState
            year == maxDate.year && maxNoRepeatMonthState != null -> maxNoRepeatMonthState
            else -> repeatMonthState
        }

    /**
     * The non-repeating [PickerState] used when the minimum date restricts the month options to
     * less than 3.
     */
    private val minNoRepeatMonthState: PickerState? =
        createNoRepeatMonthState(year = minDate.year, initialMonth = date.monthValue)

    /**
     * The non-repeating [PickerState] used when the maximum date restricts the month options to
     * less than 3.
     */
    private val maxNoRepeatMonthState: PickerState? =
        createNoRepeatMonthState(year = maxDate.year, initialMonth = date.monthValue)

    private fun createNoRepeatMonthState(year: Int, initialMonth: Int): PickerState? {
        val numberOfOptions = numberOfOptions(year)
        return if (numberOfOptions < 3) {
            PickerState(
                initialNumberOfOptions = numberOfOptions,
                initiallySelectedIndex =
                    (initialMonth - minMonthOption(year)).coerceIn(0, numberOfOptions - 1),
                shouldRepeatOptions = false,
            )
        } else {
            null
        }
    }

    /**
     * The repeating [PickerState] used when there are no minimum/maximum date restrictions on the
     * month options, or when there are at least 3 month options available.
     */
    private val repeatMonthState =
        PickerState(
            initialNumberOfOptions = numberOfOptions(currentYear()),
            initiallySelectedIndex = date.monthValue - currentMinMonthOption,
            shouldRepeatOptions = true,
        )
}

/**
 * Manages the state of the day picker dynamically, switching between repeating and non-repeating
 * PickerState instances based on the selected year and month, and the provided minimum and maximum
 * dates.
 *
 * @param date The initial date.
 * @param minDate The minimum selectable date, or null if there is no minimum.
 * @param maxDate The maximum selectable date, or null if there is no maximum.
 * @param currentYear A function that returns the currently selected year.
 * @param currentMonth A function that returns the currently selected month.
 */
@RequiresApi(Build.VERSION_CODES.O)
private class DynamicDayState(
    date: LocalDate,
    private val minDate: LocalDate,
    private val maxDate: LocalDate,
    private val currentYear: () -> Int,
    private val currentMonth: () -> Int,
) {

    /**
     * The currently active [PickerState] for the day picker. This is determined dynamically based
     * on the selected year and month, and whether the minimum/maximum dates restrict the day
     * options to less than 3.
     */
    val activeState
        get() =
            when {
                // If minMonth == maxMonth and no repeat, minNoRepeatDayState will be used.
                isMinMonth(currentYear(), currentMonth()) && minNoRepeatDayState != null ->
                    minNoRepeatDayState
                isMaxMonth(currentYear(), currentMonth()) && maxNoRepeatDayState != null ->
                    maxNoRepeatDayState
                else -> repeatDayState
            }

    /** The minimum day option currently selectable. */
    val currentMinDayOption: Int
        get() = minDayOption(currentYear(), currentMonth())

    private fun minDayOption(year: Int, month: Int) =
        if (isMinMonth(year, month)) {
            minDate.dayOfMonth
        } else {
            1
        }

    private fun maxDayOption(year: Int, month: Int): Int =
        if (isMaxMonth(year, month)) {
            maxDate.dayOfMonth
        } else {
            LocalDate.of(year, month, 1).lengthOfMonth()
        }

    private fun numberOfOptions(year: Int, month: Int): Int =
        maxDayOption(year, month) - minDayOption(year, month) + 1

    private fun isMinMonth(year: Int, month: Int): Boolean =
        year == minDate.year && month == minDate.monthValue

    private fun isMaxMonth(year: Int, month: Int): Boolean =
        year == maxDate.year && month == maxDate.monthValue

    private var prevAdjustedYear: Int? = null
    private var prevAdjustedMonth: Int? = null

    /**
     * Adjusts the day options and scrolls to the appropriate day when the selected year or month
     * changes.
     */
    suspend fun adjustDayOption() {
        val prevSelectedYear = prevAdjustedYear
        val prevSelectedMonth = prevAdjustedMonth
        prevAdjustedYear = currentYear()
        prevAdjustedMonth = currentMonth()
        if (prevSelectedYear == null || prevSelectedMonth == null) {
            return
        }

        val prevUsedState = getDayState(prevSelectedYear, prevSelectedMonth)
        val prevSelectedDay =
            if (minDate.year == prevSelectedYear && minDate.monthValue == prevSelectedMonth) {
                minDate.dayOfMonth + prevUsedState.selectedOptionIndex
            } else {
                prevUsedState.selectedOptionIndex + 1
            }

        val currentDayState = activeState
        val numberOfDayOptions = numberOfOptions(currentYear(), currentMonth())
        if (
            currentDayState.shouldRepeatOptions &&
                currentDayState.numberOfOptions != numberOfDayOptions
        ) {
            currentDayState.numberOfOptions = numberOfDayOptions
        }

        val scrollToDayIndex =
            (prevSelectedDay - currentMinDayOption).coerceIn(0, currentDayState.numberOfOptions - 1)
        if (currentDayState.selectedOptionIndex != scrollToDayIndex) {
            currentDayState.scrollToOption(scrollToDayIndex)
        }
    }

    private fun getDayState(year: Int, month: Int) =
        when {
            isMinMonth(year, month) && minNoRepeatDayState != null -> minNoRepeatDayState
            isMaxMonth(year, month) && maxNoRepeatDayState != null -> maxNoRepeatDayState
            else -> repeatDayState
        }

    /**
     * The non-repeating [PickerState] used when the minimum date restricts the day options to less
     * than 3.
     */
    private val minNoRepeatDayState: PickerState? =
        createNoRepeatDayState(
            year = minDate.year,
            month = minDate.monthValue,
            initialDay = date.dayOfMonth
        )

    /**
     * The non-repeating [PickerState] used when the maximum date restricts the day options to less
     * t han 3.
     */
    private val maxNoRepeatDayState: PickerState? =
        createNoRepeatDayState(
            year = maxDate.year,
            month = maxDate.monthValue,
            initialDay = date.dayOfMonth
        )

    private fun createNoRepeatDayState(year: Int, month: Int, initialDay: Int): PickerState? {
        val initialNumberOfOptions = numberOfOptions(year, month)
        return if (initialNumberOfOptions < 3) {
            PickerState(
                initialNumberOfOptions = initialNumberOfOptions,
                initiallySelectedIndex =
                    (initialDay - minDayOption(year, month)).coerceIn(
                        0,
                        initialNumberOfOptions - 1
                    ),
                shouldRepeatOptions = false,
            )
        } else {
            null
        }
    }

    /**
     * The repeating [PickerState] used when there are no minimum/maximum date restrictions on the
     * day options, or when there are at least 3 day options available.
     */
    private val repeatDayState =
        PickerState(
            initialNumberOfOptions = numberOfOptions(currentYear(), currentMonth()),
            initiallySelectedIndex = date.dayOfMonth - currentMinDayOption,
            shouldRepeatOptions = true,
        )
}

private fun createDescriptionDatePicker(
    selectedIndex: Int?,
    selectedValue: Int,
    label: String,
): String = if (selectedIndex == null) label else "$label, $selectedValue"
