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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class SegmentedCircularProgressIndicatorTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun supports_testtag() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                segmentCount = 5,
                progress = { 0.5f },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun allows_semantics_to_be_added_correctly() {
        val progress = mutableStateOf(0f)

        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { progress.value },
                segmentCount = 5,
                modifier =
                    Modifier.testTag(TEST_TAG).semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.value, 0f..1f)
                    },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnIdle { progress.value = 0.5f }

        rule.onNodeWithTag(TEST_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_full_contains_progress_color() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 1f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }
        rule.waitForIdle()
        // by default fully filled progress approximately takes 16% of the control.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 15f..18f)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_zero_contains_track_color() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 0f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Yellow)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 15f..18f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun change_start_end_angle() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 0.5f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                startAngle = 0f,
                endAngle = 180f,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
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
    fun set_small_stroke_width() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 0.5f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 2f..6f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 2f..6f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_large_stroke_width() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 0.5f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                strokeWidth = CircularProgressIndicatorDefaults.largeStrokeWidth,
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }
        rule.waitForIdle()
        // Because of the stroke cap, progress color takes same amount as track color.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 7f..9f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 7f..9f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_segments_on_off() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                segmentCount = 6,
                segmentValue = { it % 2 != 0 },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
                strokeWidth = 36.dp,
            )
        }

        rule.waitForIdle()
        // Because of the stroke cap, progress color takes same amount as track color.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 15f..20f)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 15f..20f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_segments_all_on() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                segmentCount = 6,
                segmentValue = { true },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Red)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Yellow, 15f..18f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun set_segments_all_off() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                segmentCount = 6,
                segmentValue = { false },
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = Color.Yellow,
                        trackColor = Color.Red,
                    ),
            )
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(Color.Yellow)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(Color.Red, 15f..18f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_disabled_contains_only_disabled_colors() {
        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 0.5f },
                segmentCount = 5,
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun progress_overflow_contains_overflow_color() {
        val customIndicatorColor = Color.Yellow
        val customTrackColor = Color.Red
        val customOverflowTrackColor = Color.Blue

        setContentWithTheme {
            SegmentedCircularProgressIndicator(
                progress = { 1.5f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = customIndicatorColor,
                        trackColor = customTrackColor,
                        overflowTrackColor = customOverflowTrackColor,
                    ),
                allowProgressOverflow = true,
            )
        }
        rule.waitForIdle()
        // When overflow is allowed then over-achieved (>100%) progress values the track should be
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
            SegmentedCircularProgressIndicator(
                progress = { 1.5f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = customIndicatorColor,
                        trackColor = customTrackColor,
                        overflowTrackColor = customOverflowTrackColor,
                    ),
                allowProgressOverflow = false,
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
            SegmentedCircularProgressIndicator(
                progress = { 2.0f },
                segmentCount = 5,
                modifier = Modifier.testTag(TEST_TAG),
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun overflow_color_appears_only_after_progress_crosses_one() {
        val initialProgress = 0.8f
        val finalProgress = 1.4f
        val progress = mutableStateOf(initialProgress)
        val indicatorColor = Color.Yellow
        val trackColor = Color.Red
        val overflowColor = Color.Blue

        val testProgressAnimationDuration = 1000 // ms
        rule.mainClock.autoAdvance = false

        rule.setContent {
            // Use precise animations for deterministic calculations
            val testProgressAnimationSpec =
                tween<Float>(durationMillis = testProgressAnimationDuration, easing = LinearEasing)
            val testColorAnimationSpec = snap<Float>()

            val testMotionScheme =
                object : MotionScheme by MaterialTheme.motionScheme {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                        // Used by the progress animation.
                        testProgressAnimationSpec as FiniteAnimationSpec<T>

                    @Suppress("UNCHECKED_CAST")
                    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                        // Used by the overflow color animation.
                        testColorAnimationSpec as FiniteAnimationSpec<T>
                }

            MaterialTheme(motionScheme = testMotionScheme) {
                SegmentedCircularProgressIndicator(
                    progress = { progress.value },
                    segmentCount = 5,
                    modifier = Modifier.testTag(TEST_TAG),
                    colors =
                        ProgressIndicatorDefaults.colors(
                            indicatorColor = indicatorColor,
                            trackColor = trackColor,
                            overflowTrackColor = overflowColor,
                        ),
                    allowProgressOverflow = true,
                )
            }
        }

        // Trigger the animation from 0.8f to 1.4f.
        rule.runOnIdle { progress.value = finalProgress }

        val totalProgressChange = finalProgress - initialProgress // 0.6f

        // The drawing logic uses `progress > 1.0f` to show the overflow color.
        // We must test against this actual boundary.
        val progressToReachBoundary = 1.0f - initialProgress // 0.2f
        val timeToReachBoundary =
            (testProgressAnimationDuration * (progressToReachBoundary / totalProgressChange))
                .toLong()

        // Advance the clock to the last frame BEFORE the boundary is crossed.
        rule.mainClock.advanceTimeBy(timeToReachBoundary)
        rule.waitForIdle()

        // At this point, the overflow color should NOT be drawn.
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(overflowColor)

        // Advance the clock by 2ms to cross the threshold. (+1ms tolerance for any rounding errors)
        rule.mainClock.advanceTimeBy(2)
        rule.waitForIdle()

        // Now, because the progress is > 1.0f and the UI is idle,
        // the overflow color should be drawn.

        // Unfortunately waitForIdle() is not enough, because the first animation draw is
        // scheduled after compose is idle. Also the next 2 draw passes draw a small and grayish
        // dot instead of a very small yellow indicator that we expect.
        // Skip 3 frames to give enough time for things to settle.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().let { image ->
            image.assertContainsColor(indicatorColor)
            image.assertContainsColor(overflowColor)
            image.assertDoesNotContainColor(trackColor)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun overflow_reverses_when_progress_animates_below_one() {
        val initialProgress = 1.4f
        val finalProgress = 0.8f
        val progress = mutableStateOf(initialProgress)
        val indicatorColor = Color.Yellow
        val trackColor = Color.Red
        val overflowColor = Color.Blue

        val testProgressAnimationDuration = 1000 // ms
        rule.mainClock.autoAdvance = false

        assert(initialProgress > finalProgress)
        assert(initialProgress > 1.0f)

        rule.setContent {
            val testProgressAnimationSpec =
                tween<Float>(durationMillis = testProgressAnimationDuration, easing = LinearEasing)
            // Use a snap animation for the color change to make the test more predictable.
            val testColorAnimationSpec = snap<Float>()

            val testMotionScheme =
                object : MotionScheme by MaterialTheme.motionScheme {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                        testProgressAnimationSpec as FiniteAnimationSpec<T>

                    @Suppress("UNCHECKED_CAST")
                    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                        testColorAnimationSpec as FiniteAnimationSpec<T>
                }

            MaterialTheme(motionScheme = testMotionScheme) {
                SegmentedCircularProgressIndicator(
                    progress = { progress.value },
                    segmentCount = 5,
                    modifier = Modifier.testTag(TEST_TAG),
                    colors =
                        ProgressIndicatorDefaults.colors(
                            indicatorColor = indicatorColor,
                            trackColor = trackColor,
                            overflowTrackColor = overflowColor,
                        ),
                    allowProgressOverflow = true,
                )
            }
        }

        // Wait for the initial composition to settle in the overflow state.
        rule.waitForIdle()

        // Verify the initial overflow state is correct.
        rule.onNodeWithTag(TEST_TAG).captureToImage().let { image ->
            image.assertContainsColor(overflowColor)
            image.assertContainsColor(indicatorColor)
            image.assertDoesNotContainColor(trackColor)
        }

        // Trigger the animation from an overflow state (1.4f) to a regular state (0.8f).
        rule.runOnIdle { progress.value = finalProgress }

        // Advance the clock to verify if reverse color animation is applied asap.
        // When updating the progres from a value > 1f to a value < 1f,
        // animation should be started since the very beginning. The behavior is different from
        // the case when update is done from a value < 1f to a value > 1f. In that case, colors are
        // animated after reaching the 1f threshold.

        rule.mainClock.advanceTimeBy(1) // progress is still very close to 1.4f
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().let { image ->
            image.assertDoesNotContainColor(overflowColor)
            image.assertDoesNotContainColor(trackColor)
            image.assertContainsColor(indicatorColor)
        }

        // Advance time to the end of the animation.
        // The overflow color should not be used.
        rule.mainClock.advanceTimeBy(testProgressAnimationDuration.toLong() - 1)
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).captureToImage().let { image ->
            image.assertDoesNotContainColor(overflowColor)
            image.assertContainsColor(trackColor)
            image.assertContainsColor(indicatorColor)
        }
    }

    private fun setContentWithTheme(composable: @Composable BoxScope.() -> Unit) {
        // Use constant size modifier to limit relative color percentage ranges.
        rule.setContentWithTheme(modifier = Modifier.size(COMPONENT_SIZE)) {
            ScreenConfiguration(SCREEN_SIZE_LARGE) { composable() }
        }
    }
}

private val COMPONENT_SIZE = 204.dp
