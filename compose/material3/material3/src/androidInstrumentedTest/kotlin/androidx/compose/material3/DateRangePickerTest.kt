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

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePickerDefaults.defaultDatePickerColors
import androidx.compose.material3.internal.MillisecondsIn24Hours
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.createCalendarModel
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class DateRangePickerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun state_initWithoutRemember() {
        val dateRangePickerState =
            DateRangePickerState(
                locale = Locale.getDefault(),
                initialSelectedStartDateMillis = 1649721600000L, // 04/12/2022
                initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours, // 04/13/2022
            )
        with(dateRangePickerState) {
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    // Using the JVM Locale.getDefault() for testing purposes only.
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            assertThat(locale).isEqualTo(Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @Test
    fun state_initWithSelectedDates() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // 04/12/2022
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L,
                    // 04/13/2022
                    initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours,
                )
        }
        with(dateRangePickerState) {
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    // Using the JVM Locale.getDefault() for testing purposes only.
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
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
                    initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours + 10000L,
                )
        }
        with(dateRangePickerState) {
            // Assert that the actual selectedDateMillis was rounded down to the start of day
            // timestamp
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)
            assertThat(displayedMonthMillis)
                .isEqualTo(
                    createCalendarModel(Locale.getDefault())
                        .getMonth(year = 2022, month = 4)
                        .startUtcTimeMillis
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))
                assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
            }
        }
    }

    @Test
    fun state_initWithEndDateOnly() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // Expecting this to throw an exception when only end-date is provided.
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedEndDateMillis = 1649721600000L
                )
        }
        // Expecting the selected dates to stay null.
        assertThat(dateRangePickerState.selectedStartDateMillis).isNull()
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            assertThat(dateRangePickerState.getSelectedStartDate()).isNull()
        }
    }

    @Test
    fun state_initWithEndDateBeforeStartDate() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            // Expecting an exception with a start date that appears after the end date.
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L,
                    // 04/11/2022
                    initialSelectedEndDateMillis = 1649721600000L - MillisecondsIn24Hours,
                )
        }

        // Expecting the selected dates to stay null.
        assertThat(dateRangePickerState.selectedStartDateMillis).isNull()
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            assertThat(dateRangePickerState.getSelectedStartDate()).isNull()
        }
    }

    @Test
    fun state_initWithEqualStartAndEndDates() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L,
                    // 04/12/2022 + a few added milliseconds to ensure that the state is initialized
                    // with a canonical date.
                    initialSelectedEndDateMillis = 1649721600000L + 1000,
                )
        }
        with(dateRangePickerState) {
            // Start and end are expected to be equal.
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 12))
            }
        }
    }

    @Test
    fun initialStartDateOutOfBounds() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialStartDateMillis =
                dayInUtcMilliseconds(year = 1999, month = 5, dayOfMonth = 11)
            val initialEndDateMillis = dayInUtcMilliseconds(year = 2020, month = 5, dayOfMonth = 12)
            dateRangePickerState =
                rememberDateRangePickerState(
                    initialSelectedStartDateMillis = initialStartDateMillis,
                    initialSelectedEndDateMillis = initialEndDateMillis,
                    yearRange = IntRange(2000, 2050),
                )
        }

        // Expecting nulls since the dates are out of range.
        assertThat(dateRangePickerState.selectedStartDateMillis).isNull()
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedStartDate()).isNull()
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
        }
    }

    @Test
    fun initialEndDateOutOfBounds() {
        lateinit var dateRangePickerState: DateRangePickerState
        val initialStartDateMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 10)
        val initialEndDateMillis = dayInUtcMilliseconds(year = 2051, month = 5, dayOfMonth = 12)
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState =
                rememberDateRangePickerState(
                    initialSelectedStartDateMillis = initialStartDateMillis,
                    initialSelectedEndDateMillis = initialEndDateMillis,
                    yearRange = IntRange(2000, 2050),
                )
        }

        assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(initialStartDateMillis)
        // Expecting nulls end date as it's out of range.
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedStartDate())
                .isEqualTo(LocalDate.of(2020, 1, 10))
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
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
            dateRangePickerState =
                rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = dateRangePickerState)
        }

        rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()

        // First date selection: Select the 10th day of the displayed month.
        rule
            .onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .assertIsNotSelected()
        rule
            .onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 10))
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedStartDate())
                    .isEqualTo(LocalDate.of(2019, 1, 10))
                assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            }
        }
        // Check that the title holds the start of the selection as a date, and ends with a suffix
        // string.
        rule
            .onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithText("Jan 10, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()
        rule
            .onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .assertIsSelected()

        // Second date selection: Select the 14th day of the displayed month.
        rule
            .onAllNodes(hasText("14", substring = true) and hasClickAction())
            .onFirst()
            .assertIsNotSelected()
        rule
            .onAllNodes(hasText("14", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid end date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedEndDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 14))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedEndDate())
                    .isEqualTo(LocalDate.of(2019, 1, 14))
            }
        }
        rule.onNodeWithText(defaultEndSelectionHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 10, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jan 14, 2019", useUnmergedTree = true).assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun datesSelection_withLocalDate() {
        lateinit var defaultStartSelectionHeadline: String // i.e. "Start date"
        lateinit var defaultEndSelectionHeadline: String // i.e. "End date"
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultStartSelectionHeadline = getString(Strings.DateRangePickerStartHeadline)
            defaultEndSelectionHeadline = getString(Strings.DateRangePickerEndHeadline)
            val monthInUtcMillis =
                YearMonth.of(2019, 1)
                    .atDay(1)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            dateRangePickerState =
                rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = dateRangePickerState)
        }

        rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()

        // First date selection: Select the 10th day of the displayed month.
        rule
            .onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.getSelectedStartDate())
                .isEqualTo(LocalDate.of(2019, 1, 10))
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
        }

        // Second date selection: Select the 14th day of the displayed month.
        rule
            .onAllNodes(hasText("14", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start and end dates.
        rule.runOnIdle {
            assertThat(dateRangePickerState.getSelectedStartDate())
                .isEqualTo(LocalDate.of(2019, 1, 10))
            assertThat(dateRangePickerState.getSelectedEndDate())
                .isEqualTo(LocalDate.of(2019, 1, 14))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun datesSelection_changeWithLocalDate() {
        lateinit var defaultStartSelectionHeadline: String // i.e. "Start date"
        lateinit var defaultEndSelectionHeadline: String // i.e. "End date"
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultStartSelectionHeadline = getString(Strings.DateRangePickerStartHeadline)
            defaultEndSelectionHeadline = getString(Strings.DateRangePickerEndHeadline)
            val monthInUtcMillis =
                YearMonth.of(2019, 1)
                    .atDay(1)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            dateRangePickerState =
                rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = dateRangePickerState)
        }

        rule.onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()

        // Select dates through an API call
        dateRangePickerState.setSelection(
            startDate = LocalDate.of(2019, 1, 10),
            endDate = LocalDate.of(2019, 1, 14),
        )

        // Assert the state holds a valid start and end dates.
        rule.runOnIdle {
            assertThat(dateRangePickerState.getSelectedStartDate())
                .isEqualTo(LocalDate.of(2019, 1, 10))
            assertThat(dateRangePickerState.getSelectedEndDate())
                .isEqualTo(LocalDate.of(2019, 1, 14))
        }

        rule.onNodeWithText(defaultStartSelectionHeadline).assertDoesNotExist()
        rule.onNodeWithText(defaultEndSelectionHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 10, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jan 14, 2019", useUnmergedTree = true).assertExists()

        // Select a different set of dates through an API call
        dateRangePickerState.setSelection(
            startDate = LocalDate.of(2019, 1, 4),
            endDate = LocalDate.of(2019, 1, 23),
        )

        rule.runOnIdle {
            assertThat(dateRangePickerState.getSelectedStartDate())
                .isEqualTo(LocalDate.of(2019, 1, 4))
            assertThat(dateRangePickerState.getSelectedEndDate())
                .isEqualTo(LocalDate.of(2019, 1, 23))
        }

        rule.onNodeWithText("Jan 4, 2019", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jan 23, 2019", useUnmergedTree = true).assertExists()
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
            dateRangePickerState =
                rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = dateRangePickerState)
        }

        // First date selection: Select the 15th day of the first displayed month in the list.
        rule
            .onAllNodes(hasText("15", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state holds a valid start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 15))
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedStartDate())
                    .isEqualTo(LocalDate.of(2019, 3, 15))
                assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            }
        }

        // Select a second date that is earlier than the first date.
        rule
            .onAllNodes(hasText("12", substring = true) and hasClickAction())
            .onFirst()
            .performClick()

        // Assert the state now holds the second selection as the start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 12))
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedStartDate())
                    .isEqualTo(LocalDate.of(2019, 3, 12))
                assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            }
        }
    }

    /**
     * Tests that clicking the same date twice creates a single-day range, and that clicking it a
     * third time resets the end date.
     */
    @Test
    fun dateSelection_sameDateForStartAndEnd() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 1)
            dateRangePickerState =
                rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = dateRangePickerState)
        }

        val node = rule.onAllNodes(hasText("15", substring = true) and hasClickAction()).onFirst()
        // First date selection: Select the 15th day of the first displayed month in the list.
        node.performClick()
        // Second date selection - click the same node.
        node.performClick()

        // Assert the state holds a valid start and end dates for the same date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 15))
            assertThat(dateRangePickerState.selectedEndDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 15))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedStartDate())
                    .isEqualTo(LocalDate.of(2019, 3, 15))
                assertThat(dateRangePickerState.getSelectedEndDate())
                    .isEqualTo(LocalDate.of(2019, 3, 15))
            }
        }

        // Click the node again to reset the end date.
        node.performClick()

        // Assert the state now holds just a start date.
        rule.runOnIdle {
            assertThat(dateRangePickerState.selectedStartDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 3, dayOfMonth = 15))
            assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(dateRangePickerState.getSelectedStartDate())
                    .isEqualTo(LocalDate.of(2019, 3, 15))
                assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
            }
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
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L + 10000L,
                    // 04/13/2022
                    initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours,
                )
            DateRangePicker(state = dateRangePickerState)
        }
        rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertExists()
        with(dateRangePickerState) {
            assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
            assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))
            }

            // Reset the selections
            dateRangePickerState.setSelection(null, null)
            assertThat(selectedStartDateMillis).isNull()
            assertThat(selectedEndDateMillis).isNull()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(getSelectedStartDate()).isNull()
                assertThat(getSelectedEndDate()).isNull()
            }

            rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule
                .onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
                .assertExists()
            rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_resetSelections_withLocalDates() {
        lateinit var defaultStartSelectionHeadline: String
        lateinit var defaultEndSelectionHeadline: String
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultStartSelectionHeadline = getString(Strings.DateRangePickerStartHeadline)
            defaultEndSelectionHeadline = getString(Strings.DateRangePickerEndHeadline)
            // 04/12/2022
            dateRangePickerState =
                rememberDateRangePickerState(
                    // 04/12/2022
                    initialSelectedStartDateMillis = 1649721600000L + 10000L,
                    // 04/13/2022
                    initialSelectedEndDateMillis = 1649721600000L + MillisecondsIn24Hours,
                )
            DateRangePicker(state = dateRangePickerState)
        }
        rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertExists()
        with(dateRangePickerState) {
            assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
            assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))

            // Reset the selections
            dateRangePickerState.setSelection(null as LocalDate?, null as LocalDate?)
            assertThat(getSelectedStartDate()).isNull()
            assertThat(getSelectedEndDate()).isNull()

            rule.onNodeWithText("Apr 12, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText("Apr 13, 2022", useUnmergedTree = true).assertDoesNotExist()
            rule
                .onNodeWithText(defaultStartSelectionHeadline, useUnmergedTree = true)
                .assertExists()
            rule.onNodeWithText(defaultEndSelectionHeadline, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun setSelection_outOfYearsBound() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState = rememberDateRangePickerState(yearRange = IntRange(2000, 2050))
        }

        // Setting the selection to a year that is out of range.
        dateRangePickerState.setSelection(
            dayInUtcMilliseconds(year = 1999, month = 5, dayOfMonth = 11),
            null,
        )

        // Assert that the start date is null.
        assertThat(dateRangePickerState.selectedStartDateMillis).isNull()
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedStartDate()).isNull()
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
        }
    }

    @Test
    fun setSelection_endBeforeStart() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateRangePickerState = rememberDateRangePickerState()
        }

        // Expecting an exception since the start date is after the end date.
        dateRangePickerState.setSelection(
            startDateMillis = dayInUtcMilliseconds(year = 2000, month = 10, dayOfMonth = 10),
            endDateMillis = dayInUtcMilliseconds(year = 1998, month = 10, dayOfMonth = 10),
        )

        // Expecting the selected dates to be null.
        assertThat(dateRangePickerState.selectedStartDateMillis).isNull()
        assertThat(dateRangePickerState.selectedEndDateMillis).isNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(dateRangePickerState.getSelectedStartDate()).isNull()
            assertThat(dateRangePickerState.getSelectedEndDate()).isNull()
        }
    }

    @Test
    fun state_restoresDatePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var dateRangePickerState: DateRangePickerState? = null
        restorationTester.setContent { dateRangePickerState = rememberDateRangePickerState() }
        // Using the JVM Locale.getDefault() for testing purposes only.
        val calendarModel = createCalendarModel(Locale.getDefault())
        // 04/12/2022
        val startDate = calendarModel.getCanonicalDate(1649721600000L)
        // 04/13/2022
        val endDate = calendarModel.getCanonicalDate(1649721600000L + MillisecondsIn24Hours)
        val displayedMonth = calendarModel.getMonth(startDate)

        rule.runOnIdle {
            dateRangePickerState!!.setSelection(startDate.utcTimeMillis, endDate.utcTimeMillis)
            dateRangePickerState!!.displayedMonthMillis = displayedMonth.startUtcTimeMillis
        }

        dateRangePickerState = null

        restorationTester.emulateSavedInstanceStateRestore()

        with(dateRangePickerState!!) {
            rule.runOnIdle {
                assertThat(selectedStartDateMillis).isEqualTo(startDate.utcTimeMillis)
                assertThat(selectedEndDateMillis).isEqualTo(endDate.utcTimeMillis)
                assertThat(displayedMonthMillis).isEqualTo(displayedMonth.startUtcTimeMillis)
                assertThat(selectedStartDateMillis).isEqualTo(1649721600000L)
                assertThat(selectedEndDateMillis).isEqualTo(1649721600000L + MillisecondsIn24Hours)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    assertThat(getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
                    assertThat(getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 4, 13))
                    assertThat(getDisplayedMonth()).isEqualTo(YearMonth.of(2022, 4))
                }
            }
        }
    }

    @Test
    fun state_changeDisplayedMonth() {
        var futureMonthInUtcMillis = 0L
        lateinit var state: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 1, dayOfMonth = 1)
            futureMonthInUtcMillis = dayInUtcMilliseconds(year = 2020, month = 7, dayOfMonth = 1)
            state = rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = state)
        }

        rule.onNodeWithText("January 2020").assertExists()

        // Update the displayed month to be ~6 months in the future.
        state.displayedMonthMillis = futureMonthInUtcMillis

        rule.waitForIdle()
        rule.onNodeWithText("July 2020").assertExists()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(state.getDisplayedMonth()).isEqualTo(YearMonth.of(2020, 7))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_changeDisplayedMonth_withYearMonth() {
        lateinit var state: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis =
                YearMonth.of(2020, 1)
                    .atDay(1)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            state = rememberDateRangePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
            DateRangePicker(state = state)
        }

        rule.onNodeWithText("January 2020").assertExists()

        // Update the displayed month to be ~6 months in the future.
        state.setDisplayedMonth(YearMonth.of(2020, 7))

        rule.waitForIdle()
        rule.onNodeWithText("July 2020").assertExists()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertThat(state.getDisplayedMonth()).isEqualTo(YearMonth.of(2020, 7))
            assertThat(state.displayedMonthMillis)
                .isEqualTo(
                    YearMonth.of(2020, 7)
                        .atDay(1)
                        .atStartOfDay(ZoneId.of("UTC"))
                        .toInstant()
                        .toEpochMilli()
                )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_initWithJavaTimeApi() {
        val startDateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 4, dayOfMonth = 12)
        val endDateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 6, dayOfMonth = 20)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2024, month = 1, dayOfMonth = 1)
        lateinit var dateRangePickerState: DateRangePickerState
        lateinit var dateRangePickerStateWithJavaTimeApi: DateRangePickerState
        rule.setContent {
            dateRangePickerState =
                rememberDateRangePickerState(
                    initialSelectedStartDateMillis = startDateInUtcMillis,
                    initialSelectedEndDateMillis = endDateInUtcMillis,
                    initialDisplayedMonthMillis = monthInUtcMillis,
                )
            dateRangePickerStateWithJavaTimeApi =
                rememberDateRangePickerState(
                    initialSelectedStartDate = LocalDate.of(2022, 4, 12),
                    initialSelectedEndDate = LocalDate.of(2022, 6, 20),
                    initialDisplayedMonth = YearMonth.of(2024, 1),
                )
        }

        assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(startDateInUtcMillis)
        assertThat(dateRangePickerState.selectedEndDateMillis).isEqualTo(endDateInUtcMillis)
        assertThat(dateRangePickerState.displayedMonthMillis).isEqualTo(monthInUtcMillis)

        assertThat(dateRangePickerStateWithJavaTimeApi.selectedStartDateMillis)
            .isEqualTo(startDateInUtcMillis)
        assertThat(dateRangePickerStateWithJavaTimeApi.selectedEndDateMillis)
            .isEqualTo(endDateInUtcMillis)
        assertThat(dateRangePickerStateWithJavaTimeApi.displayedMonthMillis)
            .isEqualTo(monthInUtcMillis)

        assertThat(dateRangePickerState.getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(dateRangePickerState.getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 6, 20))
        assertThat(dateRangePickerState.getDisplayedMonth()).isEqualTo(YearMonth.of(2024, 1))
        assertThat(dateRangePickerStateWithJavaTimeApi.getSelectedStartDate())
            .isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(dateRangePickerStateWithJavaTimeApi.getSelectedEndDate())
            .isEqualTo(LocalDate.of(2022, 6, 20))
        assertThat(dateRangePickerStateWithJavaTimeApi.getDisplayedMonth())
            .isEqualTo(YearMonth.of(2024, 1))

        assertThat(dateRangePickerState.yearRange)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.yearRange)
        assertThat(dateRangePickerState.selectableDates)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.selectableDates)
        assertThat(dateRangePickerState.locale)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.locale)
        assertThat(dateRangePickerState.displayMode)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.displayMode)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun state_initWithJavaTimeApi_withoutRemember() {
        val startDateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 4, dayOfMonth = 12)
        val endDateInUtcMillis = dayInUtcMilliseconds(year = 2022, month = 6, dayOfMonth = 20)
        val monthInUtcMillis = dayInUtcMilliseconds(year = 2024, month = 1, dayOfMonth = 1)
        val dateRangePickerState =
            DateRangePickerState(
                locale = Locale.getDefault(),
                initialSelectedStartDateMillis = startDateInUtcMillis,
                initialSelectedEndDateMillis = endDateInUtcMillis,
                initialDisplayedMonthMillis = monthInUtcMillis,
            )
        val dateRangePickerStateWithJavaTimeApi =
            DateRangePickerState(
                locale = Locale.getDefault(),
                initialSelectedStartDate = LocalDate.of(2022, 4, 12),
                initialSelectedEndDate = LocalDate.of(2022, 6, 20),
                initialDisplayedMonth = YearMonth.of(2024, 1),
            )
        assertThat(dateRangePickerState.selectedStartDateMillis).isEqualTo(startDateInUtcMillis)
        assertThat(dateRangePickerState.selectedEndDateMillis).isEqualTo(endDateInUtcMillis)
        assertThat(dateRangePickerState.displayedMonthMillis).isEqualTo(monthInUtcMillis)

        assertThat(dateRangePickerStateWithJavaTimeApi.selectedStartDateMillis)
            .isEqualTo(startDateInUtcMillis)
        assertThat(dateRangePickerStateWithJavaTimeApi.selectedEndDateMillis)
            .isEqualTo(endDateInUtcMillis)
        assertThat(dateRangePickerStateWithJavaTimeApi.displayedMonthMillis)
            .isEqualTo(monthInUtcMillis)

        assertThat(dateRangePickerState.getSelectedStartDate()).isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(dateRangePickerState.getSelectedEndDate()).isEqualTo(LocalDate.of(2022, 6, 20))
        assertThat(dateRangePickerState.getDisplayedMonth()).isEqualTo(YearMonth.of(2024, 1))

        assertThat(dateRangePickerStateWithJavaTimeApi.getSelectedStartDate())
            .isEqualTo(LocalDate.of(2022, 4, 12))
        assertThat(dateRangePickerStateWithJavaTimeApi.getSelectedEndDate())
            .isEqualTo(LocalDate.of(2022, 6, 20))
        assertThat(dateRangePickerStateWithJavaTimeApi.getDisplayedMonth())
            .isEqualTo(YearMonth.of(2024, 1))

        assertThat(dateRangePickerState.yearRange)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.yearRange)
        assertThat(dateRangePickerState.selectableDates)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.selectableDates)
        assertThat(dateRangePickerState.locale)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.locale)
        assertThat(dateRangePickerState.displayMode)
            .isEqualTo(dateRangePickerStateWithJavaTimeApi.displayMode)
    }

    @Test
    fun selectableDates_updatedSelection() {
        lateinit var dateRangePickerState: DateRangePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val allEnabled =
                object : SelectableDates {
                    // All dates are valid.
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                }
            val allDisabled =
                object : SelectableDates {
                    // All dates are invalid.
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = false
                }

            var selectableDates: SelectableDates by remember { mutableStateOf(allEnabled) }
            Column {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
                dateRangePickerState =
                    rememberDateRangePickerState(
                        initialDisplayedMonthMillis = monthInUtcMillis,
                        selectableDates = selectableDates,
                    )
                // Apply a Modifier.weight(1f) to ensure that the Button below will be visible.
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
                Button(
                    onClick = { selectableDates = allDisabled },
                    modifier = Modifier.testTag("disableSelection"),
                ) {
                    Text("Disable selection")
                }
            }
        }

        // Pick the 27th and ensure it's enabled for all the displayed months.
        rule.onAllNodes(hasText("27", substring = true) and hasClickAction()).assertAll(isEnabled())

        // Click the button to disable all dates.
        rule.onNodeWithTag("disableSelection").performClick()
        rule.waitForIdle()
        // Assert that the 27th of the month is disabled for all the displayed months.
        rule
            .onAllNodes(hasText("27", substring = true) and hasClickAction())
            .assertAll(isNotEnabled())
    }

    @Test
    fun yearRange_minYearAfterCurrentYear() {
        var currentYear = 0
        rule.setMaterialContent(lightColorScheme()) {
            currentYear = createCalendarModel(Locale.getDefault()).today.year
            DateRangePicker(
                state =
                    rememberDateRangePickerState(
                        yearRange = IntRange(currentYear + 1, currentYear + 10)
                    )
            )
        }

        rule.onNodeWithText("January ${currentYear + 1}").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun customColorsSupersedeTypographyColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
            val startSelectionMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
            val endSelectionMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 10)

            // Wrap in a MaterialTheme that has a typography that was set with custom colors.
            // The date picker is using BodyLarge for days. See
            // DatePickerModalTokens.DateLabelTextFont.
            MaterialTheme(
                typography =
                    MaterialTheme.typography.copy(
                        // Set the body-large with a yellow color. We would like to test that no
                        // yellow appears on any day.
                        bodyLarge = TextStyle(fontSize = 30.sp, color = Color.Yellow),
                        headlineLarge = TextStyle(fontSize = 30.sp, color = Color.Green),
                    )
            ) {
                DateRangePicker(
                    state =
                        rememberDateRangePickerState(
                            initialDisplayedMonthMillis = monthInUtcMillis,
                            initialSelectedStartDateMillis = startSelectionMillis,
                            initialSelectedEndDateMillis = endSelectionMillis,
                        ),
                    colors =
                        MaterialTheme.colorScheme.defaultDatePickerColors.copy(
                            dayContentColor = Color.Blue,
                            selectedDayContentColor = Color.Red,
                            headlineContentColor = Color.Yellow,
                            dayInSelectionRangeContentColor = Color.Green,
                        ),
                )
            }
        }

        // Check that the 6th day of the displayed month is selected and is with a red content
        // color.
        rule
            .onAllNodes(hasText("6", substring = true) and hasClickAction())
            .onFirst()
            .captureToImage()
            .assertDoesNotContainColor(Color.Yellow)
            .assertContainsColor(Color.Red)

        // Check that the 11th day of the displayed month is selected and is with a red content
        // color.
        rule
            .onAllNodes(hasText("10", substring = true) and hasClickAction())
            .onFirst()
            .captureToImage()
            .assertDoesNotContainColor(Color.Yellow)
            .assertContainsColor(Color.Red)

        // A day in the selection range should have a green content color.
        rule
            .onAllNodes(hasText("7", substring = true) and hasClickAction())
            .onFirst()
            .captureToImage()
            .assertDoesNotContainColor(Color.Yellow)
            .assertContainsColor(Color.Green)

        // The headline color should the yellow, as we override the typography green color for
        // "headlineLarge".
        rule
            .onNodeWithText("Mar 6, 2021", useUnmergedTree = true)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
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
