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
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.formatWithSkeleton
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class DateInputTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun dateInput() {
        lateinit var defaultHeadline: String
        lateinit var dateInputLabel: String
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            defaultHeadline = getString(string = Strings.DateInputHeadline)
            dateInputLabel = getString(string = Strings.DateInputLabel)
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 1)
            state =
                rememberDatePickerState(
                    initialDisplayedMonthMillis = monthInUtcMillis,
                    initialDisplayMode = DisplayMode.Input
                )
            DatePicker(state = state)
        }

        rule.onNodeWithText(defaultHeadline).assertExists()

        // Enter a date.
        rule.onNodeWithText(dateInputLabel).performClick().performTextInput("01272019")

        rule.runOnIdle {
            assertThat(state.selectedDateMillis)
                .isEqualTo(dayInUtcMilliseconds(year = 2019, month = 1, dayOfMonth = 27))
        }

        rule.onNodeWithText(defaultHeadline).assertDoesNotExist()
        rule.onNodeWithText("Jan 27, 2019").assertExists()
    }

    @Test
    fun dateInputWithInitialDate() {
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = initialDateMillis,
                        initialDisplayMode = DisplayMode.Input
                    )
            )
        }

        rule.onNodeWithText("05/11/2010").assertExists()
        rule.onNodeWithText("May 11, 2010").assertExists()
    }

    @Test
    fun dateInput_initialFocusOnInputField() {
        var delayCompleted by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = initialDateMillis,
                        initialDisplayMode = DisplayMode.Input
                    )
            )
            // Update the delayCompleted till after the focus is acquired. Note that we request the
            // focus about 400ms after the picker is shown.
            LaunchedEffect(Unit) {
                delay(500)
                delayCompleted = true
            }
        }
        rule.waitUntil("Waiting for focus", 1_000L) { delayCompleted }
        rule.onNodeWithText("05/11/2010").assertIsFocused()
    }

    @Test
    fun dateInput_noInitialFocusOnInputField() {
        var delayCompleted by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = initialDateMillis,
                        initialDisplayMode = DisplayMode.Input
                    ),
                // Prevent the focus from being requested.
                requestFocus = false
            )
            // Although a focus request is not made, apply a delay to ensure that the test checks
            // for focus after that delay.
            LaunchedEffect(Unit) {
                delay(500)
                delayCompleted = true
            }
        }
        rule.waitUntil("Waiting for delay completion", 1_000L) { delayCompleted }
        rule.onNodeWithText("05/11/2010").assertIsNotFocused()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun dateInputWithInitialDate_alternateLocale() {
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            val initialDateMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
            state =
                DatePickerState(
                    locale = Locale.forLanguageTag("HE"),
                    initialSelectedDateMillis = initialDateMillis,
                    initialDisplayMode = DisplayMode.Input
                )
            DatePicker(state = state)
        }

        // For Hebrew Locale, the month precedes the date.
        rule.onNodeWithText("11.05.2010").assertExists()
        // Setting the Locale at the state would not affect the displayed date at the headline, and
        // it will still be displayed as "May 11, 2010" with the default locale. To ensure that the
        // entire date picker UI is localized, there is a need to wrap the picker's code in a
        // CompositionLocalProvider with a new Context Configuration, but this test does not cover
        // that.
        rule.onNodeWithText("May 11, 2010").assertExists()
    }

    @Test
    fun inputDateNotAllowed() {
        lateinit var dateInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateInputLabel = getString(string = Strings.DateInputLabel)
            errorMessage = getString(string = Strings.DateInputInvalidNotAllowed)
            state =
                rememberDatePickerState(
                    initialDisplayMode = DisplayMode.Input,
                    selectableDates =
                        object : SelectableDates {
                            // All dates are invalid for the sake of this test.
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean = false
                        }
                )
            DatePicker(state = state)
        }

        rule.onNodeWithText(dateInputLabel).performClick().performTextInput("02272020")

        rule.runOnIdle { assertThat(state.selectedDateMillis).isNull() }
        rule
            .onNodeWithText("02/27/2020")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(expectValue(SemanticsProperties.Error, errorMessage.format("Feb 27, 2020")))
    }

    @Test
    fun inputDateOutOfRange() {
        lateinit var dateInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateInputLabel = getString(string = Strings.DateInputLabel)
            errorMessage = getString(string = Strings.DateInputInvalidYearRange)
            state =
                rememberDatePickerState(
                    // Limit the years selection to 2018-2023
                    yearRange = IntRange(2018, 2023),
                    initialDisplayMode = DisplayMode.Input
                )
            DatePicker(state = state)
        }

        rule.onNodeWithText(dateInputLabel).performClick().performTextInput("02272030")

        rule.runOnIdle { assertThat(state.selectedDateMillis).isNull() }
        rule
            .onNodeWithText("02/27/2030")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(
                expectValue(
                    SemanticsProperties.Error,
                    errorMessage.format(state.yearRange.first, state.yearRange.last)
                )
            )
    }

    @Test
    fun inputDateOutOfRange_withInitialDate() {
        lateinit var dateInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateInputLabel = getString(string = Strings.DateInputLabel)
            errorMessage = getString(string = Strings.DateInputInvalidNotAllowed)
            state =
                rememberDatePickerState(
                    initialDisplayMode = DisplayMode.Input,
                    initialSelectedDateMillis =
                        dayInUtcMilliseconds(year = 2030, month = 2, dayOfMonth = 27),
                    selectableDates =
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean = false
                        }
                )
            DatePicker(state = state)
        }

        rule.runOnIdle { assertThat(state.selectedDateMillis).isNull() }

        // Check that the title is displaying the default text and not a date string.
        rule.onNodeWithText(dateInputLabel).assertIsDisplayed()
        // Check for the error semantics.
        rule
            .onNodeWithText("02/27/2030")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(expectValue(SemanticsProperties.Error, errorMessage.format("Feb 27, 2030")))
    }

    @Test
    fun inputDateInvalidForPattern() {
        lateinit var dateInputLabel: String
        lateinit var errorMessage: String
        lateinit var state: DatePickerState
        rule.setMaterialContent(lightColorScheme()) {
            dateInputLabel = getString(string = Strings.DateInputLabel)
            errorMessage =
                getString(string = Strings.DateInputInvalidForPattern).format("MM/DD/YYYY")
            state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
            DatePicker(state = state)
        }

        rule.onNodeWithText(dateInputLabel).performClick().performTextInput("99272030")

        rule.runOnIdle { assertThat(state.selectedDateMillis).isNull() }
        rule
            .onNodeWithText("99/27/2030")
            .assert(keyIsDefined(SemanticsProperties.Error))
            .assert(expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun switchToDatePicker() {
        lateinit var switchToPickerDescription: String
        lateinit var dateInputLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            switchToPickerDescription = getString(string = Strings.DatePickerSwitchToCalendarMode)
            dateInputLabel = getString(string = Strings.DateInputLabel)
            DatePicker(state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input))
        }

        // Click to switch to DatePicker.
        rule.onNodeWithContentDescription(label = switchToPickerDescription).performClick()

        rule.waitForIdle()
        rule
            .onNodeWithContentDescription(label = "next", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        rule
            .onNodeWithContentDescription(label = "previous", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        rule.onNodeWithText(dateInputLabel).assertDoesNotExist()
    }

    @Test
    fun heightUnchangedWithError() {
        lateinit var dateInputLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            dateInputLabel = getString(string = Strings.DateInputLabel)
            DatePicker(
                state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input),
                modifier = Modifier.testTag(DateInputTestTag)
            )
        }
        val withoutErrorBounds = rule.onNodeWithTag(DateInputTestTag).getBoundsInRoot()
        // Type a date that will trigger an error text.
        rule.onNodeWithText(dateInputLabel).performClick().performTextInput("12123000")
        rule.waitForIdle()
        val withErrorBounds = rule.onNodeWithTag(DateInputTestTag).getBoundsInRoot()

        // Check that the height of the component did not change after having the error text visible
        withoutErrorBounds.height.assertIsEqualTo(
            withErrorBounds.height,
            subject = "Date input height"
        )
    }

    @Test
    fun defaultSemantics() {
        val selectedDateInUtcMillis = dayInUtcMilliseconds(year = 2010, month = 5, dayOfMonth = 11)
        lateinit var expectedHeadlineStringFormat: String
        rule.setMaterialContent(lightColorScheme()) {
            // e.g. "Entered date: %1$s"
            expectedHeadlineStringFormat = getString(Strings.DateInputHeadlineDescription)
            DatePicker(
                state =
                    rememberDatePickerState(
                        initialSelectedDateMillis = selectedDateInUtcMillis,
                        initialDisplayMode = DisplayMode.Input
                    )
            )
        }

        val fullDateDescription =
            formatWithSkeleton(
                selectedDateInUtcMillis,
                DatePickerDefaults.YearMonthWeekdayDaySkeleton,
                Locale.US,
                cache = mutableMapOf()
            )

        rule
            .onNodeWithText("May 11, 2010")
            .assertContentDescriptionEquals(
                expectedHeadlineStringFormat.format(fullDateDescription)
            )
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

    private val DateInputTestTag = "DateInput"
}
