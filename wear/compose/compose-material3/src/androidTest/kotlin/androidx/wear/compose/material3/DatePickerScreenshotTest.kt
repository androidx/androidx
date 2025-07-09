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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.internal.Strings
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class DatePickerScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun datePicker_dayMonthYear(@TestParameter screenSize: ScreenSize) {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            screenSize = screenSize,
            content = { DatePickerDayMonthYear() },
        )
    }

    @Test
    fun datePicker_dayMonthYear_rtl() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerDayMonthYear() },
        )
    }

    @Test
    fun datePicker_dayMonthYear_monthFocused(@TestParameter screenSize: ScreenSize) {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            screenSize = screenSize,
            action = { rule.nextButton().performClick() },
            content = { DatePickerDayMonthYear() },
        )
    }

    @Test
    fun datePicker_dayMonthYear_monthFocused_rtl() =
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            action = { rule.nextButton().performClick() },
            content = { DatePickerDayMonthYear() },
        )

    @Test
    fun datePicker_dayMonthYear_yearFocused(@TestParameter screenSize: ScreenSize) {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            screenSize = screenSize,
            action = {
                rule.nextButton().performClick()
                rule.nextButton().performClick()
            },
            content = { DatePickerDayMonthYear() },
        )
    }

    @Test
    fun datePicker_dayMonthYear_yearFocused_rtl() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            action = {
                rule.nextButton().performClick()
                rule.nextButton().performClick()
            },
            content = { DatePickerDayMonthYear() },
        )
    }

    @Test
    fun datePicker_monthDayYear(@TestParameter screenSize: ScreenSize) {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            screenSize = screenSize,
            content = { DatePickerMonthDayYear() },
        )
    }

    @Test
    fun datePicker_monthDayYear_rtl() =
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerMonthDayYear() },
        )

    @Test
    fun datePicker_yearMonthDay(@TestParameter screenSize: ScreenSize) {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            layoutDirection = LayoutDirection.Ltr,
            content = { DatePickerYearMonthDay() },
        )
    }

    @Test
    fun datePicker_yearMonthDay_rtl() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerYearMonthDay() },
        )
    }

    @Test
    fun datePicker_dayMonthYear_maxDate() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.DayMonthYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                    maxValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                )
            },
        )
    }

    @Test
    fun datePicker_dayMonthYear_minDate() =
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.DayMonthYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                    minValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                )
            },
        )

    @Test
    fun datePicker_yearMonthDay_single_year_option() =
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.YearMonthDay,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 15),
                    minValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                    maxValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 10, /* dayOfMonth= */ 15),
                )
            },
        )

    @Test
    fun datePicker_monthYearDay_single_month_valid() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.MonthDayYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                    minValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 1),
                    maxValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                )
            },
        )
    }

    @Test
    fun datePicker_dayMonthYear_single_day_valid() {
        rule.verifyDatePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.DayMonthYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                    minValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                    maxValidDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                )
            },
        )
    }

    @Composable
    private fun DatePickerDayMonthYear() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.DayMonthYear,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
        )
    }

    @Composable
    private fun DatePickerMonthDayYear() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.MonthDayYear,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
        )
    }

    @Composable
    private fun DatePickerYearMonthDay() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.YearMonthDay,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
        )
    }

    private fun SemanticsNodeInteractionsProvider.nextButton(): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.PickerNextButtonContentDescription.value)
            )
            .onFirst()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyDatePickerScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        testTag: String = TEST_TAG,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        screenSize: ScreenSize = ScreenSize.SMALL,
        action: (() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    content()
                }
            }
        }
        action?.let { it() }
        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }
}
