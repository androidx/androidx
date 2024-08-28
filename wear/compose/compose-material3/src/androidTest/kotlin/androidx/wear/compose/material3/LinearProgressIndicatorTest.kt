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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

class LinearProgressIndicatorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun allows_semantics_to_be_added_correctly() {
        val progress = mutableStateOf(0f)

        rule.setContentWithTheme {
            LinearProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG).semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.value, 0f..1f)
                    },
                progress = { progress.value }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnIdle { progress.value = 0.5f }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_stroke_width_throws_exception() {
        rule.setContentWithTheme { LinearProgressIndicator(progress = { 1f }, strokeWidth = 1.dp) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_full_contains_progress_color() {
        rule.setContentWithTheme {
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }

        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Yellow)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_zero_contains_track_color() {
        rule.setContentWithTheme {
            LinearProgressIndicator(
                progress = { 0f },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Yellow)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Red)

        // The empty progress bar should contain a small dot of progress color
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 0f..3f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_half_contains_progress_and_track_colors() {
        rule.setContentWithTheme {
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }

        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Yellow)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Red)

        // Contains around half progress color
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 45f..55f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_disabled_contains_only_disabled_colors() {
        rule.setContentWithTheme {
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.testTag(TEST_TAG),
                enabled = false,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                        disabledIndicatorColor = Color.Blue,
                        disabledTrackColor = Color.Green,
                    ),
            )
        }

        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Yellow)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Blue)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Green)
    }
}
