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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
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
class LinearProgressIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun linear_progress_indicator_empty() = linear_progress_indicator_test {
        LinearProgressIndicator(
            progress = { 0f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
        )
    }

    @Test
    fun linear_progress_indicator_50_percent() = linear_progress_indicator_test {
        LinearProgressIndicator(
            progress = { 0.5f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
        )
    }

    @Test
    fun linear_progress_indicator_full() = linear_progress_indicator_test {
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
        )
    }

    @Test
    fun linear_progress_indicator_50_percent_disabled() = linear_progress_indicator_test {
        LinearProgressIndicator(
            progress = { 0.5f },
            enabled = false,
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
        )
    }

    @Test
    fun linear_progress_indicator_custom_color() = linear_progress_indicator_test {
        LinearProgressIndicator(
            progress = { 0.5f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            colors =
                ProgressIndicatorDefaults.colors(
                    indicatorColor = Color.Green,
                    trackColor = Color.Red.copy(alpha = 0.5f)
                )
        )
    }

    @Test
    fun linear_progress_indicator_rtl() =
        linear_progress_indicator_test(isLtr = false) {
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            )
        }

    private fun linear_progress_indicator_test(
        isLtr: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme(modifier = Modifier.background(Color.Black)) {
            val layoutDirection = if (isLtr) LayoutDirection.Ltr else LayoutDirection.Rtl
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                content = content
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
