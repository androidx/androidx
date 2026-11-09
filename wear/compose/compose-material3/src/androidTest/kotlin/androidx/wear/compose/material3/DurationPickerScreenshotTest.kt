/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class DurationPickerScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun durationPicker_hoursMinutesSeconds(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            },
        )

    @Test
    fun durationPicker_minutesSeconds(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(23).plusSeconds(45),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.MinutesSeconds,
                )
            },
        )

    @Test
    fun durationPicker_hoursMinutes(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(5).plusMinutes(30),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            },
        )

    @Test
    fun durationPicker_hoursMinutesSeconds_minutesFocused(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                    initialSelection = DurationPickerSelection.Minutes,
                )
            },
        )

    @Test
    fun durationPicker_hoursMinutesSeconds_secondsFocused(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                    initialSelection = DurationPickerSelection.Seconds,
                )
            },
        )

    @Test
    fun durationPicker_hoursMinutesSeconds_rtl(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            layoutDirection = LayoutDirection.Rtl,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            },
        )

    @Test
    fun durationPicker_maximumDuration_over24Hours(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ofHours(36).plusMinutes(45),
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    maxValidDuration = Duration.ofHours(45).plusMinutes(30),
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            },
        )

    @Test
    fun durationPicker_minimumDuration_initialZeroInvalidState(
        @TestParameter screenSize: ScreenSize
    ) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    minValidDuration = Duration.ofSeconds(10),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            },
        )

    @Test
    fun durationPicker_danish_dotSeparator(@TestParameter screenSize: ScreenSize) =
        rule.verifyDurationPickerScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            screenSize = screenSize,
            content = {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.Locales(LocaleList(Locale("da-DK")))
                ) {
                    DurationPicker(
                        initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30),
                        onDurationPicked = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        durationPickerType = DurationPickerType.HoursMinutesSeconds,
                    )
                }
            },
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyDurationPickerScreenshot(
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

        rule.verifyScreenshot(testName, screenshotRule, testTagNode = onNodeWithTag(testTag))
    }
}
