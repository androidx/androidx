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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
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

    private fun verifyScreenshot(isDeviceRound: Boolean = true, content: @Composable () -> Unit) {
        rule.verifyScreenshot(
            methodName = testName.methodName,
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
