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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TimeTextScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    private val MockTimeSource =
        object : TimeSource {
            @Composable override fun currentTime() = "10:10"
        }

    @Test
    fun time_text_with_clock_only_on_round_device() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG),
            timeSource = MockTimeSource,
        ) {
            time()
        }
    }

    @Test
    fun time_text_with_clock_only_on_non_round_device() =
        verifyScreenshot(false) {
            TimeText(
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                time()
            }
        }

    @Test
    fun time_text_with_status_on_round_device() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG),
            timeSource = MockTimeSource,
        ) {
            text("ETA 12:48")
            separator()
            time()
        }
    }

    @Test
    fun time_text_with_status_on_non_round_device() =
        verifyScreenshot(false) {
            TimeText(
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                text("ETA 12:48")
                separator()
                time()
            }
        }

    @Test
    fun time_text_with_icon_on_round_device() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG),
            timeSource = MockTimeSource,
        ) {
            time()
            separator()
            composable {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }

    @Test
    fun time_text_with_icon_on_non_round_device() =
        verifyScreenshot(false) {
            TimeText(
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                time()
                separator()
                composable {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

    @Test
    fun time_text_with_custom_colors_on_round_device() = verifyScreenshot {
        val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
        val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
        TimeText(
            contentColor = Color.Green,
            timeTextStyle = timeTextStyle,
            modifier = Modifier.testTag(TEST_TAG),
            timeSource = MockTimeSource,
        ) {
            text("ETA", customStyle)
            composable { Spacer(modifier = Modifier.size(4.dp)) }
            text("12:48")
            separator(separatorStyle)
            time()
        }
    }

    @Test
    fun time_text_with_long_status_on_round_device() = verifyScreenshot {
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Green)
        TimeText(
            contentColor = Color.Green,
            timeTextStyle = timeTextStyle,
            modifier = Modifier.testTag(TEST_TAG),
            timeSource = MockTimeSource,
        ) {
            text("Long status that should be ellipsized.")
            composable { Spacer(modifier = Modifier.size(4.dp)) }
            time()
        }
    }

    @Test
    fun time_text_with_custom_colors_on_non_round_device() =
        verifyScreenshot(false) {
            val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
            val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
            val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
            TimeText(
                contentColor = Color.Green,
                timeTextStyle = timeTextStyle,
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                text("ETA", customStyle)
                composable { Spacer(modifier = Modifier.size(4.dp)) }
                text("12:48")
                separator(separatorStyle)
                time()
            }
        }

    @Test
    fun time_text_with_very_long_text_on_round_device() =
        verifyScreenshot(true) {
            val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
            val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
            val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
            TimeText(
                contentColor = Color.Green,
                timeTextStyle = timeTextStyle,
                maxSweepAngle = 180f,
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                text(
                    "Very long text to ensure we are respecting the maxSweep parameter",
                    customStyle
                )
                separator(separatorStyle)
                time()
            }
        }

    @Test
    fun time_text_with_very_long_text_non_round_device() =
        verifyScreenshot(false) {
            val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
            val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
            val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
            TimeText(
                contentColor = Color.Green,
                timeTextStyle = timeTextStyle,
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                text(
                    "Very long text to ensure we are not taking more than one line and " +
                        "leaving room for the time",
                    customStyle
                )
                separator(separatorStyle)
                time()
            }
        }

    @Test
    fun time_text_with_very_long_text_smaller_angle_on_round_device() =
        verifyScreenshot(true) {
            val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
            val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
            val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
            TimeText(
                contentColor = Color.Green,
                timeTextStyle = timeTextStyle,
                maxSweepAngle = 90f,
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
            ) {
                text(
                    "Very long text to ensure we are respecting the maxSweep parameter",
                    customStyle
                )
                separator(separatorStyle)
                time()
            }
        }

    @Test
    fun time_text_long_text_before_time(@TestParameter shape: ScreenShape) =
        TimeTextWithDefaults(shape.isRound) {
            text("Very long text to ensure we are respecting the weight parameter", weight = 1f)
            separator()
            time()
            separator()
            text("More")
        }

    @Test
    fun time_text_long_text_after_time(@TestParameter shape: ScreenShape) =
        TimeTextWithDefaults(shape.isRound) {
            text("More")
            separator()
            time()
            separator()
            text("Very long text to ensure we are respecting the weight parameter", weight = 1f)
        }

    // This is to get better names, so it says 'round_device' instead of 'true'
    enum class ScreenShape(val isRound: Boolean) {
        ROUND_DEVICE(true),
        SQUARE_DEVICE(false)
    }

    private fun TimeTextWithDefaults(isDeviceRound: Boolean, content: TimeTextScope.() -> Unit) =
        verifyScreenshot(isDeviceRound) {
            TimeText(
                contentColor = Color.Green,
                maxSweepAngle = 180f,
                modifier = Modifier.testTag(TEST_TAG),
                timeSource = MockTimeSource,
                content = content
            )
        }

    private fun verifyScreenshot(isDeviceRound: Boolean = true, content: @Composable () -> Unit) {
        rule.verifyScreenshot(
            // Valid characters for golden identifiers are [A-Za-z0-9_-]
            // TestParameterInjector adds '[' + parameter_values + ']' to the test name.
            methodName = testName.methodName.replace("[", "_").replace("]", ""),
            screenshotRule = screenshotRule,
            content = {
                val screenSize = LocalContext.current.resources.configuration.smallestScreenWidthDp
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(
                        DpSize(screenSize.dp, screenSize.dp)
                    ) then DeviceConfigurationOverride.RoundScreen(isDeviceRound)
                ) {
                    content()
                }
            }
        )
    }
}
