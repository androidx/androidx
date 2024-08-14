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

import android.content.res.Resources
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.samples.DatePickerSample
import androidx.wear.compose.material3.samples.DatePickerYearMonthDaySample
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@MediumTest
@RunWith(AndroidJUnit4::class)
class DatePickerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            DatePicker(
                initialDate = LocalDate.now(),
                onDatePicked = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun samples_build() {
        rule.setContentWithTheme {
            DatePickerSample()
            DatePickerYearMonthDaySample()
        }
    }

    @Test
    fun dayMonthYear_initial_state() {
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = {},
                initialDate = initialDate,
                datePickerType = DatePickerType.DayMonthYear
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.dayOfMonth,
                selectionMode = SelectionMode.Day
            )
            .assertIsFocused()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .assertIsDisplayed()
        rule.nextButton().assertIsDisplayed()
        rule.confirmButton().assertDoesNotExist()
    }

    @Test
    fun monthDayYear_initial_state() {
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 2, /* dayOfMonth= */ 29)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = {},
                initialDate = initialDate,
                datePickerType = DatePickerType.MonthDayYear
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .assertIsFocused()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.dayOfMonth,
                selectionMode = SelectionMode.Day
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .assertIsDisplayed()
        rule.nextButton().assertIsDisplayed()
        rule.confirmButton().assertDoesNotExist()
    }

    @Test
    fun yearMonthDay_initial_state() {
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 12, /* dayOfMonth= */ 31)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = {},
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .assertIsFocused()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.dayOfMonth,
                selectionMode = SelectionMode.Day
            )
            .assertIsDisplayed()
        rule.nextButton().assertIsDisplayed()
        rule.confirmButton().assertDoesNotExist()
    }

    @Test
    fun dayMonthYear_switch_to_month() {
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                initialDate = initialDate,
                onDatePicked = {},
                datePickerType = DatePickerType.DayMonthYear
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .performClick()

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .assertIsFocused()
    }

    @Test
    fun dayMonthYear_switch_to_year() {
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                initialDate = initialDate,
                onDatePicked = {},
                datePickerType = DatePickerType.DayMonthYear
            )
        }

        rule.nextButton().performClick()
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performClick()

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .assertIsFocused()
    }

    @Test
    fun date_picked() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        val expectedDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 5)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.DayMonthYear
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.dayOfMonth,
                selectionMode = SelectionMode.Day
            )
            .performScrollToIndex(expectedDate.dayOfMonth - 1)
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .performScrollToIndex(expectedDate.monthValue - 1)
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(expectedDate.year - 1900)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun date_picked_between_fromDate_and_toDate() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        val expectedDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 5)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.DayMonthYear,
                minDate = LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 1),
                maxDate = LocalDate.of(/* year= */ 2025, /* month= */ 12, /* dayOfMonth= */ 6)
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.dayOfMonth,
                selectionMode = SelectionMode.Day
            )
            .performScrollToIndex(expectedDate.dayOfMonth - 1)
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .performScrollToIndex(expectedDate.monthValue - 1)
        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(expectedDate.year - 1900)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_day_to_fromDate() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 6)
        val expectedDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                minDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.monthValue,
                selectionMode = SelectionMode.Month
            )
            .performScrollToIndex(0)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_month_to_fromDate_month() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 7, /* dayOfMonth= */ 15)
        val expectedDate = LocalDate.of(/* year= */ 2023, /* month= */ 8, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                minDate = LocalDate.of(/* year= */ 2023, /* month= */ 8, /* dayOfMonth= */ 5),
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(0)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_month_and_day_to_fromDate() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 6)
        val expectedDate = LocalDate.of(/* year= */ 2023, /* month= */ 10, /* dayOfMonth= */ 15)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                minDate = LocalDate.of(/* year= */ 2023, /* month= */ 10, /* dayOfMonth= */ 15),
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(0)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_month_to_toDate_month() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 2)
        val expectedDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 2)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                maxDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 4)
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(2025 - 1900)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_day_to_toDate() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 2, /* dayOfMonth= */ 12)
        val expectedDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 4)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                maxDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 4)
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(2025 - 1900)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    @Test
    fun auto_scroll_month_and_day_to_toDate() {
        lateinit var pickedDate: LocalDate
        val initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 10)
        val expectedDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 4)
        rule.setContentWithTheme {
            DatePicker(
                onDatePicked = { pickedDate = it },
                initialDate = initialDate,
                datePickerType = DatePickerType.YearMonthDay,
                maxDate = LocalDate.of(/* year= */ 2025, /* month= */ 2, /* dayOfMonth= */ 4)
            )
        }

        rule
            .onNodeWithDateValue(
                selectedValue = initialDate.year,
                selectionMode = SelectionMode.Year
            )
            .performScrollToIndex(2025 - 1900)
        rule.nextButton().performClick()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(pickedDate).isEqualTo(expectedDate)
    }

    private fun SemanticsNodeInteractionsProvider.onNodeWithDateValue(
        selectedValue: Int,
        selectionMode: SelectionMode,
    ): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                if (selectionMode == SelectionMode.Month) {
                    monthNames[(selectedValue - 1) % 12]
                } else {
                    contentDescriptionForValue(
                        InstrumentationRegistry.getInstrumentation().context.resources,
                        selectedValue,
                        selectionMode.contentDescriptionResource
                    )
                }
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.confirmButton(): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.PickerConfirmButtonContentDescription.value)
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.nextButton(): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.PickerNextButtonContentDescription.value)
            )
            .onFirst()

    private fun contentDescriptionForValue(
        resources: Resources,
        selectedValue: Int,
        contentDescriptionResource: Strings,
    ): String = "${resources.getString(contentDescriptionResource.value)}, $selectedValue"

    private enum class SelectionMode(val contentDescriptionResource: Strings) {
        Day(Strings.DatePickerDay),
        Month(Strings.DatePickerMonth),
        Year(Strings.DatePickerYear),
    }

    private val monthNames: List<String>
        get() {
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
            val months = 1..12
            return months.map { LocalDate.of(2022, it, 1).format(monthFormatter) }
        }
}
