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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TimePickerScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun timePicker24h_withoutSeconds() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutes24H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
                )
            }
        )

    @Test
    fun timePicker24h_withSeconds() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesSeconds24H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 37)
                )
            }
        )

    @Test
    fun timePicker12h() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesAmPm12H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
                )
            }
        )

    @Test
    fun timePicker24h_withoutSeconds_largeScreen() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutes24H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
                )
            }
        )

    @Test
    fun timePicker24h_withSeconds_largeScreen() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesSeconds24H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 37)
                )
            }
        )

    @Test
    fun timePicker12h_largeScreen() =
        rule.verifyTimePickerScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            isLargeScreen = true,
            content = {
                TimePicker(
                    onTimePicked = {},
                    modifier = Modifier.testTag(TEST_TAG),
                    timePickerType = TimePickerType.HoursMinutesAmPm12H,
                    initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
                )
            }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyTimePickerScreenshot(
        methodName: String,
        screenshotRule: AndroidXScreenshotTestRule,
        testTag: String = TEST_TAG,
        isLargeScreen: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val screenSizeDp = if (isLargeScreen) SCREENSHOT_SIZE_LARGE else SCREENSHOT_SIZE
        setContentWithTheme {
            val originalConfiguration = LocalConfiguration.current
            val fixedScreenSizeConfiguration =
                remember(originalConfiguration) {
                    Configuration(originalConfiguration).apply {
                        screenWidthDp = screenSizeDp
                        screenHeightDp = screenSizeDp
                    }
                }
            CompositionLocalProvider(LocalConfiguration provides fixedScreenSizeConfiguration) {
                Box(
                    modifier =
                        Modifier.size(screenSizeDp.dp)
                            .background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, methodName)
    }
}

private const val SCREENSHOT_SIZE = 192
private const val SCREENSHOT_SIZE_LARGE = 228
