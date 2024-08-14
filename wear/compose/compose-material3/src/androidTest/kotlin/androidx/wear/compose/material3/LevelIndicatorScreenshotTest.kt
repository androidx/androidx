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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
class LevelIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun level_indicator_0_percent(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 0f, shape = shape, testName = testName)

    @Test
    fun level_indicator_25_percent(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 25f, shape = shape, testName = testName)

    @Test
    fun level_indicator_75_percent(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 75f, shape = shape, testName = testName)

    @Test
    fun level_indicator_100_percent(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 100f, shape = shape, testName = testName)

    @Test
    fun level_indicator_25_percent_reversed(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 25f, reverseDirection = true, shape = shape, testName = testName)

    @Test
    fun level_indicator_disabled(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 25f, enabled = false, shape = shape, testName = testName)

    @Test
    fun level_indicator_rtl(@TestParameter shape: ScreenShape) =
        verifyScreenshot(value = 25f, ltr = false, shape = shape, testName = testName)

    @Test
    fun level_indicator_double_stroke_width(@TestParameter shape: ScreenShape) =
        verifyScreenshot(
            value = 25f,
            strokeWidth = LevelIndicatorDefaults.StrokeWidth * 2,
            shape = shape,
            testName = testName
        )

    @Test
    fun level_indicator_half_sweep_angle(@TestParameter shape: ScreenShape) =
        verifyScreenshot(
            value = 25f,
            sweepAngle = LevelIndicatorDefaults.SweepAngle / 2f,
            shape = shape,
            testName = testName
        )

    private fun verifyScreenshot(
        value: Float,
        testName: TestName,
        shape: ScreenShape,
        ltr: Boolean = true,
        enabled: Boolean = true,
        strokeWidth: Dp = LevelIndicatorDefaults.StrokeWidth,
        sweepAngle: Float = LevelIndicatorDefaults.SweepAngle,
        reverseDirection: Boolean = false,
    ) {
        val valueRange = 0f..100f
        val screenSizeDp = SCREEN_SIZE_SMALL

        rule.setContentWithTheme {
            val actualLayoutDirection = if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl

            val currentConfig = LocalConfiguration.current
            val updatedConfig =
                Configuration().apply {
                    setTo(currentConfig)
                    screenLayout =
                        if (shape == ScreenShape.ROUND_DEVICE) Configuration.SCREENLAYOUT_ROUND_YES
                        else Configuration.SCREENLAYOUT_ROUND_NO
                    screenWidthDp = screenSizeDp
                    screenHeightDp = screenSizeDp
                }
            CompositionLocalProvider(
                LocalLayoutDirection provides actualLayoutDirection,
                LocalConfiguration provides updatedConfig
            ) {
                Box(
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .size(screenSizeDp.dp)
                            .background(MaterialTheme.colorScheme.background)
                ) {
                    LevelIndicator(
                        value = { value },
                        valueRange = valueRange,
                        modifier = Modifier.align(Alignment.CenterStart),
                        enabled = enabled,
                        strokeWidth = strokeWidth,
                        sweepAngle = sweepAngle,
                        reverseDirection = reverseDirection,
                    )
                }
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }
}
