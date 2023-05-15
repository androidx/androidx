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

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class DateRangePickerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun state_initWithSelectedDates() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            dateRangePickerState = rememberDateRangePickerState(
                // 04/12/2022
                initialSelectedStartDateMillis = 1649721600000L,
                // 04/13/2022
                initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours
            )
        }
        with(dateRangePickerState) {
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)
            assertThat(displayedMonthMillis).isEqualTo(
                CalendarModel.Default.getMonth(year = 2022, month = 4).startUtcTimeMillis
            )
        }
    }

    @Test
    fun state_initWithSelectedDates_roundingToUtcMidnight() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L + 10000L,
                    // 04/13/2022
                    initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours + 10000L
                )
        }
        with(dateRangePickerState) {
            // Assert that the actual selectedDateMillis was rounded down to the start of day
            // timestamp
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)
            assertThat(displayedMonthMillis).isEqualTo(
                CalendarModel.Default.getMonth(year = 2022, month = 4).startUtcTimeMillis
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun state_initWithEndDateOnly() {
        rule.setMaterialContent(lightColorScheme()) {
            // Expecting this to throw an exception when only end-date is provided.
            rememberDateRangePickerState(
                // 04/12/2022
                initialSelectedEndDateMillis = 1649721600000L
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun state_initWithEndDateBeforeStartDate() {
        rule.setMaterialContent(lightColorScheme()) {
            // Expecting an exception with a start date that appears after the end date.
            rememberDateRangePickerState(
                // 04/12/2022
                initialSelectedStartDateMillis = 1649721600000L,
                // 04/11/2022
                initialSelectedEndDateMillis = 1649721600000L - MillisecondsIn24Hours
            )
        }
    }

    fun state_initWithEqualStartAndEndDates() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState = rememberDateRangePickerState(
                // 04/12/2022
                initialSelectedStartDateMillis = 1649721600000L,
                // 04/12/2022 + a few added milliseconds to ensure that the state is initialized
                // with a canonical date.
                initialSelectedEndDateMillis = 1649721600000L + 1000
            )
        }
        with(dateRangePickerState) {
            // Start and end are expected to be equal.
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialStartDateOutOfBounds() {
        rule.setMaterialContent(lightColorScheme()) {
            val initialStartDateMillis =
                dayInUtcMilliseconds(year = 1999, month = 5, dayOfMonth = 11)
            val initialEndDateMillis = dayInUtcMilliseconds(year = 2020, month = 5, dayOfMonth = 12)
            rememberDateRangePickerState(
                initialSelectedStartDateMillis = initialStartDateMillis,
                initialSelectedEndDateMillis = initialEndDateMillis,
                yearRange = IntRange(2000, 2050)
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialEndDateOutOfBounds() {
        rule.setMaterialContent(lightColorScheme()) {
            val initialStartDateMillis =
                dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 10)
            val initialEndDateMillis = dayInUtcMilliseconds(year = 2051, month = 5, dayOfMonth = 12)
            rememberDateRangePickerState(
                initialSelectedStartDateMillis = initialStartDateMillis,
                initialSelectedEndDateMillis = initialEndDateMillis,
                yearRange = IntRange(2000, 2050)
            )
        }
    }

    @Test
    fun datesSelection() {
        lateinit var defaultStartSelectionHeadline: String // i.e. "Start date"
        lateinit var defaultEndSelectionHeadline: String // i.e. "End date"
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultStartSelectionHeadline = getString(Strings.DateRangePickerStartHeadline)
            defaultEndSelectionHeadline = getString(Strings.DateRangePickerEndHeadline)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            dateRangePickerState = rememberDateRangePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DateRangePicker(state = dateRangePickerState)
        }

        rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
            .assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true)
            .assertExists()

        // First date selection: Select the 10th day of the displayed month.
        rule.onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .assertIsNotSelected()
        rule.onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 1,
                    dayOfMonth = 10
                )
            )
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()
        }
        // Check that the title holds the start of the selection as a date, and ends with a suffix
        // string.
        rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithText("Jan 10, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()
        rule.onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .assertIsSelected()

        // Second date selection: Select the 14th day of the displayed month.
        rule.onAllNodes(hasText("14", substring = true) and hasClickAction())
            .onFirst()
            .assertIsNotSelected()
        rule.onAllNodes(hasText("14", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid end date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedEndDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 1,
                    dayOfMonth = 14
                )
            )
        }
        rule.onNodeWithText(defaultEndSelectionHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 10, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jan 14, 2019", useUnmergedTree = true).assertExists()
    }

    /**
     * Tests that an end-date selection before the selected start date moves the start date to be
     * that date.
     */
    @Test
    fun dateSelectionStartReset() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 1)
            dateRangePickerState = rememberDateRangePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DateRangePicker(state = dateRangePickerState)
        }

        // First date selection: Select the 15th day of the first displayed month in the list.
        rule.onAllNodes(hasText("15", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 3,
                    dayOfMonth = 15
                )
            )
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()
        }

        // Select a second date that is earlier than the first date.
        rule.onAllNodes(hasText("12", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state now holds the second selection as the start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 3,
                    dayOfMonth = 12
                )
            )
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()
        }
    }

    /**
     * Tests that clicking the same date twice creates a single-day range, and that clicking it
     * a third time resets the end date.
     */
    @Test
    fun dateSelection_sameDateForStartAndEnd() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 1)
            dateRangePickerState = rememberDateRangePickerState(
                initialDisplayedMonthMillis = monthInUtcMillis
            )
            DateRangePicker(state = dateRangePickerState)
        }

        val node = rule.onAllNodes(hasText("15", substring = true) and hasClickAction()).onFirst()
        // First date selection: Select the 15th day of the first displayed month in the list.
        node.performClick()
        // Second date selection - click the same node.
        node.performClick()

        // Assert the state holds a valid start and end dates for the same date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 3,
                    dayOfMonth = 15
                )
            )
            assertThat(dateRangePickerState.selectedEndDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 3,
                    dayOfMonth = 15
                )
            )
        }

        // Click the node again to reset the end date.
        node.performClick()

        // Assert the state now holds just a start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(
                dayInUtcMilliseconds(
                    year = 2019,
                    month = 3,
                    dayOfMonth = 15
                )
            )
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()
        }
    }

    @Test
    fun state_resetSelections() {
        lateinit var defaultStartSelectionHeadline: String
        lateinit var defaultEndSelectionHeadline: String
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultStartSelectionHeadline = getString(Strings.DateRangePickerStartHeadline)
            defaultEndSelectionHeadline = getString(Strings.DateRangePickerEndHeadline)
            // 04/12/2022
            dateRangePickerState = rememberDateRangePickerState(
                // 04/12/2022
                initialSelectedStartDateMillis = 1649721600000L + 10000L,
                // 04/13/2022
                initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours
            )
            DateRangePicker(state = dateRangePickerState)
        }
        rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertExists()
        with(dateRangePickerState) {
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)

            // Reset the selections
            dateRangePickerState.setSelection(null, null)
            assertThat(selectedStartDateMillis).isNull()
            assertThat(selectedEndDateMillis).isNull()
            rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
                .assertExists()
            rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun setSelection_outOfYearsBound() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState = rememberDateRangePickerState(yearRange = IntRange(2000, 2050))
        }

        // Setting the selection to a year that is out of range.
        dateRangePickerState.setSelection(
            dayInUtcMilliseconds(
                year = 1999,
                month = 5,
                dayOfMonth = 11
            ), null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun setSelection_endBeforeStart() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState = rememberDateRangePickerState()
        }

        // Expecting an exception since the start date is after the end date.
        dateRangePickerState.setSelection(
            startDateMillis = dayInUtcMilliseconds(
                year = 2000,
                month = 10,
                dayOfMonth = 10
            ),
            endDateMillis = dayInUtcMilliseconds(
                year = 1998,
                month = 10,
                dayOfMonth = 10
            )
        )
    }

    @Test
    fun state_restoresDatePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var dateRangePickerState: DateRangePickerState? = null
        restorationTester.setContent {
            dateRangePickerState = rememberDateRangePickerState()
        }
        val calendarModel = CalendarModel.Default
        with(dateRangePickerState!!) {
            // 04/12/2022
            val startDate =
                calendarModel.getCanonicalDate(1649721600000L)
            // 04/13/2022
            val endDate =
                calendarModel.getCanonicalDate(1649721600000L + MillisecondsIn24Hours)
            val displayedMonth = calendarModel.getMonth(startDate)
            rule.runOnIdle {
                setSelection(startDate.utcTimeMillis, endDate.utcTimeMillis)
                displayedMonthMillis = displayedMonth.startUtcTimeMillis
            }

            dateRangePickerState = null

            restorationTester.emulateSavedInstanceStateRestore()

            rule.runOnIdle {
                assertThat(selectedStartDateMillis).isEqualTo(startDate.utcTimeMillis)
                assertThat(selectedEndDateMillis).isEqualTo(endDate.utcTimeMillis)
                assertThat(displayedMonthMillis).isEqualTo(displayedMonth.startUtcTimeMillis)
                assertThat(dateRangePickerState!!.selectedStartDateMillis)
                    .isEqualTo(1649721600000L)
                assertThat(dateRangePickerState!!.selectedEndDateMillis)
                    .isEqualTo(1649721600000L + MillisecondsIn24Hours)
            }
        }
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long {
        val firstDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        firstDayCalendar.clear()
        firstDayCalendar[Calendar.YEAR] = year
        firstDayCalendar[Calendar.MONTH] = month - 1
        firstDayCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
        return firstDayCalendar.timeInMillis
    }
}
