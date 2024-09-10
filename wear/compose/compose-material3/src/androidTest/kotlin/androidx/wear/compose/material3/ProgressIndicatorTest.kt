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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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

class CircularProgressIndicatorTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        setContentWithTheme {
            CircularProgressIndicator(progress = { 0.5f }, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun allows_semantics_to_be_added_correctly() {
        val progress = mutableStateOf(0f)

        setContentWithTheme {
            CircularProgressIndicator(
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contains_progress_color() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 1f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        // With default stroke width the filled progress approximately takes 16% of the control.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 15f..18f)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contains_progress_incomplete_color() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Yellow)
        // With default stroke width the progress track approximately takes 16% of the control.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 15f..18f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun change_start_end_angle() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0.5f },
                startAngle = 0f,
                endAngle = 180f,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        // Color should take approximately a quarter of the full screen color percentages,
        // eg 16% / 4 ≈ 4%.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 3f..5f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 3f..5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_small_progress_value() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0.02f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        // Small progress values like 2% should be rounded up to at least the stroke width.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 0.2f..0.5f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 15f..18f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_small_stroke_width() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0.5f },
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 5f..7f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 5f..7f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_large_stroke_width() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0.5f },
                strokeWidth = CircularProgressIndicatorDefaults.largeStrokeWidth,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red
                    ),
            )
        }
        rule.waitForIdle()
        // Because of the stroke cap, progress color takes same amount as track color.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 8f..10f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 8f..10f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_disabled_contains_disabled_colors() {
        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 0.5f },
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_overflow_contains_overflow_color() {
        val customIndicatorColor = Color.Yellow
        val customTrackColor = Color.Red
        val customOverflowTrackColor = Color.Blue

        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 1.5f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = customIndicatorColor,
                        trackColor = customTrackColor,
                        overflowTrackColor = customOverflowTrackColor,
                    ),
                allowProgressOverflow = true
            )
        }
        rule.waitForIdle()

        // When overflow is allowed with over-achieved (>100%) progress values, the track should be
        // in overflowTrackColor and the indicator should still be in indicatorColor.
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(customTrackColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIndicatorColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customOverflowTrackColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_overflow_not_allowed_contains_only_indicator_color() {
        val customIndicatorColor = Color.Yellow
        val customTrackColor = Color.Red
        val customOverflowTrackColor = Color.Blue

        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 1.5f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = customIndicatorColor,
                        trackColor = customTrackColor,
                        overflowTrackColor = customOverflowTrackColor,
                    ),
                allowProgressOverflow = false
            )
        }
        rule.waitForIdle()

        // When progress overflow is disabled, then overflow progress values should be coerced to 1
        // and overflowTrackColor should not appear, only customIndicatorColor.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(customOverflowTrackColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(customTrackColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIndicatorColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_overflow_200_percent_contains_only_indicator_color() {
        val customIndicatorColor = Color.Yellow
        val customTrackColor = Color.Red
        val customOverflowTrackColor = Color.Blue

        setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = { 2.0f },
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = customIndicatorColor,
                        trackColor = customTrackColor,
                        overflowTrackColor = customOverflowTrackColor,
                    ),
            )
        }
        rule.waitForIdle()

        // For 200% over-achieved progress the indicator should take the whole progress
        // circle, just like for 100%.
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(customTrackColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(customOverflowTrackColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIndicatorColor)
    }

    private fun setContentWithTheme(composable: @Composable BoxScope.() -> Unit) {
        // Use constant size modifier to limit relative color percentage ranges.
        rule.setContentWithTheme(modifier = Modifier.size(204.dp), composable = composable)
    }
}
