/*
 * Copyright (C) 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalInkCustomBrushApi::class)

package androidx.ink.authoring.compose

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.ink.authoring.testing.InputStreamBuilder
import androidx.ink.authoring.testing.MultiTouchInputBuilder
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.StockTextureBitmapStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class InProgressStrokesTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(InProgressStrokesTestActivity::class.java)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    /**
     * Accumulates results for the entire test to minimize how often the tests need to be repeated
     * when updates to the goldens are necessary.
     */
    private val screenshotFailureMessages = mutableListOf<String>()

    @Test
    fun downEvent_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                }
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
        }

        // Single dot.
        assertThatTakingScreenshotMatchesGolden("down")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun downAndMoveEvents_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                }
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getNextMoveEvent())
        }

        // Line with constant thickness.
        assertThatTakingScreenshotMatchesGolden("down_and_move")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun downAndUpEvents_sendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                }
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getUpEvent())
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).hasSize(1)
            assertThat(activity.finishedStrokeCohorts[0]).hasSize(1)
        }
    }

    @Test
    fun downAndMoveAndUpEvents_sendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                }
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getNextMoveEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getUpEvent())
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).hasSize(1)
            assertThat(activity.finishedStrokeCohorts[0]).hasSize(1)
        }
    }

    @Test
    fun downAndMoveEvents_withNonIdentityTransforms_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                },
                nextPointerEventToWorldTransform = { Matrix().apply { scale(x = 2F, y = 3F) } },
                nextStrokeToWorldTransform = { Matrix().apply { translate(x = 50F, y = 100F) } },
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getNextMoveEvent())
        }

        // Line that matches start and end point of down_and_move, but is thinner and skewed.
        assertThatTakingScreenshotMatchesGolden("down_and_move_non_identity_transform")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun downAndConsumedMoveEvents_cancelsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                consumeMoveTouchEventOfPointerNumber = 0,
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                },
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
            activity.rootView.dispatchTouchEvent(stylusInputStream.getNextMoveEvent())
        }

        // Empty screen.
        assertThatTakingScreenshotMatchesGolden("down_and_consumed_move")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun twoSimultaneousStrokesDownAndMoveAndFinish_oneCallbackWithBothStrokes() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        var usedFirstBrush = false
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(
                            StockBrushes.markerV1,
                            if (!usedFirstBrush) AVOCADO_GREEN else DEEP_PURPLE,
                            if (!usedFirstBrush) 25F else 15F,
                            0.1F,
                        )
                        .also { usedFirstBrush = true }
                }
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            inputStream.runGestureWith { event -> activity.rootView.dispatchTouchEvent(event) }
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).hasSize(1)
            assertThat(activity.finishedStrokeCohorts[0]).hasSize(2)
        }
    }

    @Test
    fun twoSimultaneousStrokesDownAndMove_moveOfFirstConsumed_showsSecondStrokeWithNoCallback() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        var usedFirstBrush = false
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(
                            StockBrushes.markerV1,
                            if (!usedFirstBrush) AVOCADO_GREEN else DEEP_PURPLE,
                            if (!usedFirstBrush) 25F else 15F,
                            0.1F,
                        )
                        .also { usedFirstBrush = true }
                },
                consumeMoveTouchEventOfPointerNumber = 0,
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            inputStream.runGestureWith { event ->
                when (event.actionMasked) {
                    // Don't process any up events to keep the strokes as in progress.
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> return@runGestureWith
                    else -> activity.rootView.dispatchTouchEvent(event)
                }
            }
        }
        yieldingSleep()

        // Just a purple stroke.
        assertThatTakingScreenshotMatchesGolden("two_simultaneous_down_and_move_first_consumed")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun twoSimultaneousStrokesDownAndMove_moveOfSecondConsumed_showsFirstStrokeWithNoCallback() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        var usedFirstBrush = false
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(
                            StockBrushes.markerV1,
                            if (!usedFirstBrush) AVOCADO_GREEN else DEEP_PURPLE,
                            if (!usedFirstBrush) 25F else 15F,
                            0.1F,
                        )
                        .also { usedFirstBrush = true }
                },
                consumeMoveTouchEventOfPointerNumber = 1,
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            inputStream.runGestureWith { event ->
                when (event.action) {
                    // Don't process any up events to keep the strokes as in progress.
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> return@runGestureWith
                    else -> activity.rootView.dispatchTouchEvent(event)
                }
            }
        }
        yieldingSleep()

        // Just a green stroke.
        assertThatTakingScreenshotMatchesGolden("two_simultaneous_down_and_move_second_consumed")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun downEvent_withMaskPath_showsMaskedStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(StockBrushes.markerV1, AVOCADO_GREEN, 25F, 0.1F)
                },
                // A rounded rectangle cutout in the center of the first input point.
                maskPath =
                    Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 20F,
                                top = 20F,
                                right = 30F,
                                bottom = 30F,
                                radiusX = 3F,
                                radiusY = 3F,
                            )
                        )
                    },
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
        }

        // Single dot.
        assertThatTakingScreenshotMatchesGolden("down_with_mask_path")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    @Test
    fun downEvent_withTextureBitmapStore_showsPencilStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.init(
                nextBrush = {
                    Brush.createWithColorIntArgb(
                        StockBrushes.pencilUnstable,
                        AVOCADO_GREEN,
                        25F,
                        0.1F,
                    )
                },
                textureBitmapStore = StockTextureBitmapStore(activity.resources),
            )
        }
        yieldingSleep()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.rootView.dispatchTouchEvent(stylusInputStream.getDownEvent())
        }

        // Single dot.
        assertThatTakingScreenshotMatchesGolden("down_with_pencil_texture")
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.finishedStrokeCohorts).isEmpty()
        }
    }

    /**
     * Waits for actions to complete, both on the render thread and the UI thread, for a specified
     * period of time. The default time is 1 second.
     */
    private fun yieldingSleep(timeMs: Long = 1000) {
        activityScenarioRule.scenario.onActivity { activity ->
            // Ensures that everything in the action queue before this point has been processed.
            activity.sync(timeMs, TimeUnit.MILLISECONDS)
        }
        repeat((timeMs / SLEEP_INTERVAL_MS).toInt()) {
            onIdle()
            SystemClock.sleep(SLEEP_INTERVAL_MS)
        }
    }

    /**
     * Take screenshots of the entire device rather than just a View in order to include all layers
     * being composed on screen. This will include the front buffer layer.
     * [InProgressStrokesViewTestActivity] is set up to exclude parts of the screen that are
     * irrelevant and may just cause flakes, such as the status bar and toolbar.
     */
    private fun assertThatTakingScreenshotMatchesGolden(key: String) {
        // Save just one failure message despite multiple attempts to improve the signal-to-noise
        // ratio.
        var lastFailureMessage: String? = null
        for (attempt in 0 until SCREENSHOT_RETRY_COUNT) {
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            if (bitmap != null) {
                lastFailureMessage = compareAgainstGolden(bitmap, key) ?: return
            }
            yieldingSleep(500L * (1 shl attempt))
        }
        // Don't fail right away, but accumulate results for the entire test.
        screenshotFailureMessages.add(checkNotNull(lastFailureMessage))
    }

    /**
     * Returns `null` if [bitmap] matches the golden image for [key], or a non-null error message if
     * they do not match.
     */
    private fun compareAgainstGolden(bitmap: Bitmap, key: String): String? {
        // The only function available is an assertion, so wrap the thrown exception and treat it as
        // a single failure in a sequence of retries. Will be rethrown at the end of the test if
        // appropriate (see `cleanup`).
        try {
            bitmap.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$key")
            return null
        } catch (e: AssertionError) {
            return e.message ?: "Image comparison failure"
        }
    }

    @After
    fun cleanup() {
        if (screenshotFailureMessages.isNotEmpty()) {
            throw AssertionError(
                "At least one screenshot did not match goldens:\n$screenshotFailureMessages"
            )
        }
    }

    private companion object {
        const val SLEEP_INTERVAL_MS = 100L
        const val SCREENSHOT_RETRY_COUNT = 4
        @ColorInt const val AVOCADO_GREEN = 0xff558b2f.toInt()
        @ColorInt const val DEEP_PURPLE = 0xff8e24aa.toInt()
    }
}
