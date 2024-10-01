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
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.internal.Strings
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class DatePickerScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun datePicker_dayMonthYear_ltr() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            content = { DatePickerDayMonthYear() }
        )
    }

    @Test
    fun datePicker_dayMonthYear_rtl() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerDayMonthYear() }
        )
    }

    @Test
    fun datePicker_dayMonthYear_largeScreen() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = { DatePickerDayMonthYear() }
        )
    }

    @Test
    fun datePicker_dayMonthYear_monthFocused_ltr() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            action = { rule.nextButton().performClick() },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_dayMonthYear_monthFocused_rtl() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            action = { rule.nextButton().performClick() },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_dayMonthYear_monthFocused_largeScreen() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            action = { rule.nextButton().performClick() },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_dayMonthYear_yearFocused_ltr() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            action = {
                rule.nextButton().performClick()
                rule.nextButton().performClick()
            },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_dayMonthYear_yearFocused_rtl() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            action = {
                rule.nextButton().performClick()
                rule.nextButton().performClick()
            },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_dayMonthYear_yearFocused_largeScreen() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            action = {
                rule.nextButton().performClick()
                rule.nextButton().performClick()
            },
            content = { DatePickerDayMonthYear() }
        )

    @Test
    fun datePicker_monthDayYear_ltr() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            content = { DatePickerMonthDayYear() }
        )

    @Test
    fun datePicker_monthDayYear_rtl() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerMonthDayYear() }
        )

    @Test
    fun datePicker_monthDayYear_largeScreen() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = { DatePickerMonthDayYear() }
        )

    @Test
    fun datePicker_yearMonthDay_ltr() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Ltr,
            content = { DatePickerYearMonthDay() }
        )
    }

    @Test
    fun datePicker_yearMonthDay_rtl() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            layoutDirection = LayoutDirection.Rtl,
            content = { DatePickerYearMonthDay() }
        )
    }

    @Test
    fun datePicker_yearMonthDay_largeScreen() {
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = { DatePickerYearMonthDay() }
        )
    }

    @Test
    fun datePicker_yearMonthDay_year_does_not_repeat() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.YearMonthDay,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 9, /* dayOfMonth= */ 15),
                    minDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15),
                    maxDate = LocalDate.of(/* year= */ 2024, /* month= */ 10, /* dayOfMonth= */ 15),
                )
            }
        )

    @Test
    fun datePicker_monthYearDay_month_does_not_repeat() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.MonthDayYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 15),
                    minDate = LocalDate.of(/* year= */ 2024, /* month= */ 1, /* dayOfMonth= */ 1),
                    maxDate = LocalDate.of(/* year= */ 2024, /* month= */ 2, /* dayOfMonth= */ 15),
                )
            }
        )

    @Test
    fun datePicker_dayMonthYear_day_does_not_repeat() =
        rule.verifyDatePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                DatePicker(
                    onDatePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    datePickerType = DatePickerType.DayMonthYear,
                    initialDate =
                        LocalDate.of(/* year= */ 2024, /* month= */ 2, /* dayOfMonth= */ 1),
                    maxDate = LocalDate.of(/* year= */ 2024, /* month= */ 2, /* dayOfMonth= */ 1),
                )
            }
        )

    @Composable
    private fun DatePickerDayMonthYear() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.DayMonthYear,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        )
    }

    @Composable
    private fun DatePickerMonthDayYear() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.MonthDayYear,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
        )
    }

    @Composable
    private fun DatePickerYearMonthDay() {
        DatePicker(
            onDatePicked = {},
            modifier = Modifier.testTag(TEST_TAG),
            datePickerType = DatePickerType.YearMonthDay,
            initialDate = LocalDate.of(/* year= */ 2024, /* month= */ 8, /* dayOfMonth= */ 15)
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
        methodName: String,
        screenshotRule: AndroidXScreenshotTestRule,
        testTag: String = TEST_TAG,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        isLargeScreen: Boolean = false,
        action: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        val screenSizeDp = if (isLargeScreen) SCREENSHOT_SIZE_LARGE else SCREENSHOT_SIZE
        setContentWithTheme {
            ScreenConfiguration(screenSizeDp) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    content()
                }
            }
        }
        action?.let { it() }
        rule.waitForIdle()

        onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, methodName)
    }
}

private const val SCREENSHOT_SIZE = 192
private const val SCREENSHOT_SIZE_LARGE = 228
