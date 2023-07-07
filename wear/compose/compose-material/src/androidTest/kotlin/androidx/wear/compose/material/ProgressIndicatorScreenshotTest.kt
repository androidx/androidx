/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material

import android.os.Build
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class ProgressIndicatorScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun indeterminate_progress_indicator() {
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                indicatorColor = Color.Green,
                trackColor = Color.LightGray
            )
        }

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(300)

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun indeterminate_progress_indicator_custom_stroke_width() {
        rule.mainClock.autoAdvance = false

        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                indicatorColor = Color.Green,
                trackColor = Color.LightGray,
                strokeWidth = 10.dp
            )
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(300)

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun determinate_progress_indicator_no_gap() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.4f,
                indicatorColor = Color.Green,
                trackColor = Color.LightGray
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun determinate_progress_indicator_with_gap() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.5f,
                startAngle = -45f,
                endAngle = 225f,
                indicatorColor = Color.Green,
                trackColor = Color.LightGray
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Test
    fun determinate_progress_indicator_custom_stroke_width() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.4f,
                indicatorColor = Color.Green,
                trackColor = Color.Yellow,
                strokeWidth = 10.dp
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
