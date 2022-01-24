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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

class CircularIndeterminateProgressIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            CircularProgressIndicator(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun shows_indeterminate_progress() {
        rule.setContentWithTheme {
            CircularProgressIndicator(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contains_progress_color() {
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(300)

        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 12f..20f)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Red, 5f..15f)
    }
}

class CircularDeterminateProgressIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            CircularProgressIndicator(progress = 0.5f, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun changes_progress() {
        val progress = mutableStateOf(0f)

        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = progress.value
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnIdle {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contains_progress_color() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 1f,
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        // by default fully filled progress approximately takes 23-26% of the control
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 23f..26f)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contains_progress_incomplete_color() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0f,
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Yellow)
        // by default progress track approximately takes 23-26% of the control
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Red, 23f..26f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun change_start_end_angle() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.5f,
                startAngle = 0f,
                endAngle = 180f,
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        // Color should take approximately a quarter of what it normally takes
        // (a little bit less), eg 25% / 4 ≈ 6%
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 5f..7f)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Red, 5f..7f)
    }

    @Test
    fun coerces_highest_out_of_bound_progress() {
        val progress = mutableStateOf(0f)

        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = progress.value
            )
        }

        rule.runOnIdle {
            progress.value = 1.5f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun coerces_lowest_out_of_bound_progress() {
        val progress = mutableStateOf(0f)

        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = progress.value
            )
        }

        rule.runOnIdle {
            progress.value = -1.5f
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_small_stroke_width() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.5f,
                strokeWidth = 2.dp,
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 5f..7f)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Red, 5f..7f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_large_stroke_width() {
        rule.setContentWithTheme {
            CircularProgressIndicator(
                modifier = Modifier.testTag(TEST_TAG),
                progress = 0.5f,
                strokeWidth = 8.dp,
                indicatorColor = Color.Yellow,
                trackColor = Color.Red
            )
        }
        rule.waitForIdle()
        // Because of the stroke cap, progress color takes a little bit more space than track color
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 24f..27f)
        rule.onNodeWithTag(TEST_TAG).captureToImage()
            .assertColorInPercentageRange(Color.Red, 18f..23f)
    }
}
