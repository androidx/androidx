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

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class TimePickerScreenshotTest {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun timePicker24h_withoutSeconds(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutes24H,
                    initialTime = LocalTime.of(/* hour= */ 0, /* minute= */ 23),
                )
            },
        )

    @Test
    fun timePicker24h_withSeconds(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesSeconds24H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 37),
                )
            },
        )

    @Test
    fun timePicker_minutesSeconds(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.MinutesSeconds,
                    initialTime = LocalTime.of(/* hour= */ 0, /* minute= */ 23, /* second= */ 37),
                )
            },
        )

    @Test
    fun timePicker12h_displays12(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                // This test case specifically verifies the behavior for locales like English
                // that use the 'h' pattern for a 1-12 hour format.
                // For this pattern, 12 AM (LocalTime.of(0, x)) must correctly display as '12'.
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesAmPm12H,
                    initialTime = LocalTime.of(/* hour= */ 0, /* minute= */ 30),
                )
            },
        )

    @Test
    fun timePicker12h_chinese(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                val chineseConfig =
                    Configuration(LocalConfiguration.current).apply {
                        setLocale(Locale.SIMPLIFIED_CHINESE)
                    }
                CompositionLocalProvider(LocalConfiguration provides chineseConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesAmPm12H,
                        initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23),
                    )
                }
            },
        )

    @Test
    fun timePicker12h_japanese(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                // This test case specifically verifies the behavior for locales like Japanese
                // that use the 'K' pattern (hours 0-11) for the 12-hour clock.
                // For this pattern, 12 PM (LocalTime.of(12, x)) is displayed as 0.
                val japaneseConfig =
                    Configuration(LocalConfiguration.current).apply { setLocale(Locale.JAPANESE) }
                CompositionLocalProvider(LocalConfiguration provides japaneseConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesAmPm12H,
                        initialTime = LocalTime.of(/* hour= */ 12, /* minute= */ 59),
                    )
                }
            },
        )

    @Test
    fun timePicker12h_arabic_rtl(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            layoutDirection = LayoutDirection.Rtl,
            content = {
                // Set locale to Arabic for correct pattern and AM/PM strings.
                val arabicConfig =
                    Configuration(LocalConfiguration.current).apply { setLocale(Locale("ar")) }
                CompositionLocalProvider(LocalConfiguration provides arabicConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesAmPm12H,
                        initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23),
                    )
                }
            },
        )

    @Test
    fun timePicker_finnish_dotSeparator(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                // This test case verifies that the separator is correctly parsed from the
                // locale's pattern. For Finnish, the best pattern for "H:mm:ss" is "H.mm.ss",
                // so a dot should be displayed instead of a colon.
                val finnishConfig =
                    Configuration(LocalConfiguration.current).apply {
                        setLocale(Locale.forLanguageTag("fi-FI"))
                    }
                CompositionLocalProvider(LocalConfiguration provides finnishConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesSeconds24H,
                        initialTime =
                            LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 59),
                    )
                }
            },
        )

    @Test
    fun timePicker12h_longAmPmText_usesFallback(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                // This test verifies the dynamic fallback logic for locales with long AM/PM
                // strings that would otherwise overflow the screen. Latvian (`lv-LV`) is used
                // as the test case because its localized strings ("priekšpusdienā" /
                // "pēcpusdienā") are very long. We expect the component to detect that these
                // strings will not fit and display the standard "AM"/"PM" fallback text instead.
                val latvianConfig =
                    Configuration(LocalConfiguration.current).apply {
                        setLocale(Locale.forLanguageTag("lv-LV"))
                    }
                CompositionLocalProvider(LocalConfiguration provides latvianConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesAmPm12H,
                        initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23),
                    )
                }
            },
        )

    @Test
    fun timePicker_frenchCanadian_sanitized(@TestParameter screenSize: ScreenSize) =
        rule.verifyTimePickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                // This test case verifies that a complex pattern with quoted literals,
                // like fr-CA ("HH 'h' mm 'min' ss 's'"), is correctly sanitized.
                // We expect the literals to be removed and a clean HH:mm:ss format to be displayed.
                val frenchCanadianConfig =
                    Configuration(LocalConfiguration.current).apply {
                        setLocale(Locale.forLanguageTag("fr-CA"))
                    }
                CompositionLocalProvider(LocalConfiguration provides frenchCanadianConfig) {
                    TimePicker(
                        onTimePicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        timePickerType = TimePickerType.HoursMinutesSeconds24H,
                        initialTime =
                            LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 59),
                    )
                }
            },
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyTimePickerScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        testTag: String = TEST_TAG,
        screenSize: ScreenSize = ScreenSize.SMALL,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    content()
                }
            }
        }
        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule, testTag = testTag)
    }
}
