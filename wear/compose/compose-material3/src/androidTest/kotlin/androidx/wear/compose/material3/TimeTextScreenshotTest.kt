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
import androidx.compose.foundation.background
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.weight
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
    fun time_text_with_clock_only() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        )
    }

    @Test
    fun time_text_with_status() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText("ETA 12:48")
            timeTextSeparator()
            curvedText(time)
        }
    }

    @Test
    fun time_text_with_icon() = verifyScreenshot {
        TimeText(
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText(time)
            timeTextSeparator()
            curvedComposable {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }

    @Test
    fun time_text_with_custom_colors() = verifyScreenshot {
        val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
        val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
        TimeText(
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText("ETA 12:48", style = customStyle)
            curvedComposable { Spacer(modifier = Modifier.size(4.dp)) }
            curvedText("12:48", style = customStyle)
            timeTextSeparator(separatorStyle)
            curvedText(time, style = timeTextStyle)
        }
    }

    @Test
    fun time_text_with_long_status() = verifyScreenshot {
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Green)
        TimeText(
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText(
                "Long status that should be ellipsized.",
                CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                style = timeTextStyle
            )
            curvedComposable { Spacer(modifier = Modifier.size(4.dp)) }
            curvedText(time, style = timeTextStyle)
        }
    }

    @Test
    fun time_text_with_very_long_text() = verifyScreenshot {
        val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
        val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
        TimeText(
            maxSweepAngle = 180f,
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText(
                "Very long text to ensure we are respecting the maxSweep parameter",
                CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxSweepAngle = 180f,
                style = customStyle
            )
            timeTextSeparator(separatorStyle)
            curvedText(time, style = timeTextStyle)
        }
    }

    @Test
    fun time_text_with_very_long_text_smaller_angle() = verifyScreenshot {
        val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)
        val timeTextStyle = TimeTextDefaults.timeTextStyle(color = Color.Cyan)
        val separatorStyle = TimeTextDefaults.timeTextStyle(color = Color.Yellow)
        TimeText(
            maxSweepAngle = 90f,
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
        ) { time ->
            curvedText(
                "Very long text to ensure we are respecting the maxSweep parameter",
                CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                style = customStyle
            )
            timeTextSeparator(separatorStyle)
            curvedText(time, style = timeTextStyle)
        }
    }

    @Test
    fun time_text_long_text_before_time() = TimeTextWithDefaults { time ->
        curvedText(
            "Very long text to ensure we are respecting the weight parameter",
            CurvedModifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )
        timeTextSeparator()
        curvedText(time)
        timeTextSeparator()
        curvedText("More")
    }

    @Test
    fun time_text_long_text_after_time() = TimeTextWithDefaults { time ->
        curvedText("More")
        timeTextSeparator()
        curvedText(time)
        timeTextSeparator()
        curvedText(
            "Very long text to ensure we are respecting the weight parameter",
            CurvedModifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )
    }

    private fun TimeTextWithDefaults(content: CurvedScope.(String) -> Unit) = verifyScreenshot {
        TimeText(
            maxSweepAngle = 180f,
            modifier = Modifier.testTag(TEST_TAG).background(Color.DarkGray),
            timeSource = MockTimeSource,
            content = content
        )
    }

    private fun verifyScreenshot(content: @Composable () -> Unit) {
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                val screenSize = LocalContext.current.resources.configuration.smallestScreenWidthDp
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(screenSize.dp, screenSize.dp))
                ) {
                    content()
                }
            }
        )
    }
}
