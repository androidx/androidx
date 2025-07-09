/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.ink.authoring.testing.MultiTouchInputBuilder
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule

/** Base class for emulator-based tests using [InProgressStrokesView]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
open class InProgressStrokesViewTestBase {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(InProgressStrokesViewTestActivity::class.java)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    protected val finishedStrokeCohorts = mutableListOf<Map<InProgressStrokeId, Stroke>>()
    private val onStrokesFinishedListener =
        object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                finishedStrokeCohorts.add(strokes)
            }
        }

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    /**
     * Accumulates results for the entire test to minimize how often the tests need to be repeated
     * when updates to the goldens are necessary.
     */
    private val screenshotFailureMessages = mutableListOf<String>()

    @Before
    fun setup() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.addFinishedStrokesListener(onStrokesFinishedListener)
        }
        yieldingSleep()
    }

    protected fun runMultiTouchGesture(
        inputStream: MultiTouchInputBuilder,
        actionToCancel: Int? = null,
    ) {
        activityScenarioRule.scenario.onActivity { activity ->
            val pointerIdToStrokeId = mutableMapOf<Int, InProgressStrokeId>()
            inputStream.runGestureWith { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        pointerIdToStrokeId[pointerId] =
                            activity.inProgressStrokesView.startStroke(
                                event,
                                pointerId,
                                basicBrush(color = BRUSH_COLORS[pointerIdToStrokeId.size]),
                            )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        for (pointerIndex in 0 until event.pointerCount) {
                            val pointerId = event.getPointerId(pointerIndex)
                            val strokeId = checkNotNull(pointerIdToStrokeId[pointerId])
                            activity.inProgressStrokesView.addToStroke(
                                event,
                                pointerId,
                                strokeId,
                                prediction = null,
                            )
                        }
                    }
                    MotionEvent.ACTION_POINTER_UP,
                    MotionEvent.ACTION_UP -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        val strokeId = checkNotNull(pointerIdToStrokeId[pointerId])
                        if (event.actionMasked == actionToCancel) {
                            activity.inProgressStrokesView.cancelStroke(strokeId, event)
                        } else {
                            activity.inProgressStrokesView.finishStroke(event, pointerId, strokeId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Waits for actions to complete, both on the render thread and the UI thread, for a specified
     * period of time. The default time is 1 second.
     */
    protected fun yieldingSleep(timeMs: Long = 1000) {
        activityScenarioRule.scenario.onActivity { activity ->
            // Ensures that everything in the action queue before this point has been processed.
            activity.inProgressStrokesView.sync(timeMs, TimeUnit.MILLISECONDS)
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
    protected fun assertThatTakingScreenshotMatchesGolden(key: String) {
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
    protected fun compareAgainstGolden(bitmap: Bitmap, key: String): String? {
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

    protected fun basicBrush(@ColorInt color: Int) =
        Brush.createWithColorIntArgb(
            family = StockBrushes.markerLatest,
            colorIntArgb = color,
            size = 25F,
            epsilon = 0.1F,
        )

    protected companion object {
        const val SLEEP_INTERVAL_MS = 100L
        const val SCREENSHOT_RETRY_COUNT = 4

        val BRUSH_COLORS =
            listOf(
                TestColors.AVOCADO_GREEN,
                TestColors.HOT_PINK,
                TestColors.COBALT_BLUE,
                TestColors.ORANGE,
                TestColors.DEEP_PURPLE,
            )
    }
}
