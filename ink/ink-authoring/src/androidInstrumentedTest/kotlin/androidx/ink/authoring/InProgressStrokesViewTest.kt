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
import android.graphics.Matrix
import android.os.SystemClock
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.ink.authoring.testing.InputStreamBuilder
import androidx.ink.authoring.testing.MultiTouchInputBuilder
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based test of [InProgressStrokesView]. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InProgressStrokesViewTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(InProgressStrokesViewTestActivity::class.java)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val finishedStrokeCohorts = mutableListOf<Map<InProgressStrokeId, Stroke>>()
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
            activity.inProgressStrokesView.eagerInit()
        }
        yieldingSleep()
    }

    @Test
    fun startStroke_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            @Suppress("UNUSED_VARIABLE")
            val unused =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
        }

        assertThatTakingScreenshotMatchesGolden("start")
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun startAndAddToStroke_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            activity.inProgressStrokesView.addToStroke(
                stylusInputStream.getNextMoveEvent(),
                pointerIndex = 0,
                strokeId,
                prediction = null,
            )
        }

        assertThatTakingScreenshotMatchesGolden("start_and_add")
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun startAndFinishStroke_showsStrokeAndSendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            activity.inProgressStrokesView.finishStroke(
                stylusInputStream.getUpEvent(),
                pointerIndex = 0,
                strokeId,
            )
        }

        assertThatTakingScreenshotMatchesGolden("start_and_finish")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
        val stroke = finishedStrokeCohorts[0].values.iterator().next()

        // Stroke units are set to pixels, so the stroke unit length should be 1/dpi inches, which
        // is
        // 2.54/dpi cm.
        val metrics = InstrumentationRegistry.getInstrumentation().context.resources.displayMetrics
        assertThat(stroke.inputs.getStrokeUnitLengthCm()).isWithin(1e-5f).of(2.54f / metrics.xdpi)
    }

    @Test
    fun startAndFinishStroke_withNonIdentityTransforms() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25f, startY = 25f, endX = 105f, endY = 205f)
        activityScenarioRule.scenario.onActivity { activity ->
            val metrics = activity.resources.displayMetrics
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                    // MotionEvent space uses pixels, so this transform sets world units equal to
                    // inches.
                    motionEventToWorldTransform =
                        Matrix().apply { setScale(1f / metrics.xdpi, 1f / metrics.ydpi) },
                    // Set one stroke unit equal to half a world unit (i.e. half an inch).
                    strokeToWorldTransform = Matrix().apply { setScale(0.5f, 0.5f) },
                )
            activity.inProgressStrokesView.finishStroke(
                stylusInputStream.getUpEvent(),
                pointerIndex = 0,
                strokeId,
            )
        }

        yieldingSleep()
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
        val stroke = finishedStrokeCohorts[0].values.iterator().next()

        // With the transforms above, one stroke unit is 0.5 inches, which is 1.27 cm.
        assertThat(stroke.inputs.getStrokeUnitLengthCm()).isWithin(1e-5f).of(1.27f)
    }

    @Test
    fun startAndFinishStroke_withNonInvertibleTransforms() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25f, startY = 25f, endX = 105f, endY = 205f)
        activityScenarioRule.scenario.onActivity { activity ->
            assertThrows(IllegalArgumentException::class.java) {
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                    motionEventToWorldTransform = Matrix().apply { setScale(0f, 0f) },
                )
            }
            assertThrows(IllegalArgumentException::class.java) {
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                    strokeToWorldTransform = Matrix().apply { setScale(0f, 0f) },
                )
            }
        }
    }

    @Test
    fun startAndCancelStroke_hidesStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(
                startX = 25F,
                startY = 25F,
                endX = 105F,
                endY = 205F,
                endWithCancel = true,
            )
        lateinit var strokeId: InProgressStrokeId
        activityScenarioRule.scenario.onActivity { activity ->
            strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
        }
        assertThatTakingScreenshotMatchesGolden("start_and_cancel_before_cancel")
        assertThat(finishedStrokeCohorts).isEmpty()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.cancelStroke(strokeId, stylusInputStream.getUpEvent())
        }
        assertThatTakingScreenshotMatchesGolden("start_and_cancel")
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun startAndAddToAndFinishStroke_showsStrokeAndSendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            activity.inProgressStrokesView.addToStroke(
                stylusInputStream.getNextMoveEvent(),
                pointerIndex = 0,
                strokeId,
                prediction = null,
            )
            activity.inProgressStrokesView.finishStroke(
                stylusInputStream.getUpEvent(),
                pointerIndex = 0,
                strokeId,
            )
        }

        assertThatTakingScreenshotMatchesGolden("start_and_add_and_finish")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun startAndAddToAndFinishStroke_showsStrokeAndSendsCallback_strokeInputApi() {
        activityScenarioRule.scenario.onActivity { activity ->
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    StrokeInput.create(
                        x = 25f,
                        y = 25f,
                        elapsedTimeMillis = 0,
                        toolType = InputToolType.STYLUS,
                    ),
                    brush = basicBrush(TestColors.AVOCADO_GREEN),
                )
            activity.inProgressStrokesView.addToStroke(
                MutableStrokeInputBatch().apply {
                    addOrThrow(
                        StrokeInput.create(
                            x = 45f,
                            y = 70f,
                            elapsedTimeMillis = 5,
                            toolType = InputToolType.STYLUS,
                        )
                    )
                    addOrThrow(
                        StrokeInput.create(
                            x = 65f,
                            y = 115f,
                            elapsedTimeMillis = 10,
                            toolType = InputToolType.STYLUS,
                        )
                    )
                },
                strokeId,
            )
            activity.inProgressStrokesView.finishStroke(
                StrokeInput.create(
                    x = 105f,
                    y = 205f,
                    elapsedTimeMillis = 20,
                    toolType = InputToolType.STYLUS,
                ),
                strokeId,
            )
        }

        assertThatTakingScreenshotMatchesGolden("start_and_add_and_finish_stroke_input_api")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun motionEventToViewAndStartAddFinishStroke_showsRepositionedStrokeAndSendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.motionEventToViewTransform =
                Matrix().apply {
                    postScale(1.5F, 1.5F)
                    postRotate(/* degrees= */ 15F)
                    postTranslate(100F, 200F)
                }
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    stylusInputStream.getDownEvent(),
                    pointerIndex = 0,
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            activity.inProgressStrokesView.addToStroke(
                stylusInputStream.getNextMoveEvent(),
                pointerIndex = 0,
                strokeId,
                prediction = null,
            )
            activity.inProgressStrokesView.finishStroke(
                stylusInputStream.getUpEvent(),
                pointerIndex = 0,
                strokeId,
            )
        }

        assertThatTakingScreenshotMatchesGolden("motion_event_to_view_transform")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun twoSimultaneousStrokes_bothFinish_showsStrokesAndSendsCallbackAfterBothFinish() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        runMultiTouchGesture(inputStream)

        assertThatTakingScreenshotMatchesGolden("two_simultaneous_both_finish")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(2)
    }

    @Test
    fun twoSimultaneousStrokes_cancelThenFinish_showsFinishedStrokeAndSendsCallback() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        runMultiTouchGesture(inputStream, actionToCancel = MotionEvent.ACTION_POINTER_UP)

        assertThatTakingScreenshotMatchesGolden("two_simultaneous_cancel_then_finish")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun twoSimultaneousStrokes_finishThenCancel_showsFinishedStrokeAndSendsCallback() {
        val inputStream =
            MultiTouchInputBuilder.rotate90DegreesClockwise(centerX = 200F, centerY = 300F)
        runMultiTouchGesture(inputStream, actionToCancel = MotionEvent.ACTION_UP)

        assertThatTakingScreenshotMatchesGolden("two_simultaneous_finish_then_cancel")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun fiveSuccessiveStrokes_showsStrokesAndSendsFiveCallbacks() {
        /**
         * The key is chosen such that the resulting screenshot filenames are alphabetically in the
         * same order in which they are produced, making it easier to follow the sequence of stroke
         * events. To aid in that, choose [action] names that are in alphabetical order, e.g.
         * prefixing their readable name with an action number.
         *
         * For successive strokes, each stroke has all the actions applied to it before moving onto
         * the next stroke, so the stroke number comes before the action name in the file name.
         */
        fun screenshotKey(strokeCount: Int, action: String) =
            "five_successive_stroke${strokeCount}_after_$action"

        repeat(BRUSH_COLORS.size) { strokeIndex ->
            val strokeCount = strokeIndex + 1
            val stylusInputStream =
                InputStreamBuilder.stylusLine(
                    startX = 15F * strokeCount,
                    startY = 45F * strokeCount,
                    endX = 400F - 10F * strokeCount,
                    endY = 600F - 35F * strokeCount,
                )
            lateinit var strokeId: InProgressStrokeId
            activityScenarioRule.scenario.onActivity { activity ->
                strokeId =
                    activity.inProgressStrokesView.startStroke(
                        stylusInputStream.getDownEvent(),
                        pointerIndex = 0,
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step1start"))
            assertThat(finishedStrokeCohorts).hasSize(strokeIndex)

            activityScenarioRule.scenario.onActivity { activity ->
                activity.inProgressStrokesView.addToStroke(
                    stylusInputStream.getNextMoveEvent(),
                    pointerIndex = 0,
                    strokeId,
                    prediction = null,
                )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step2add"))
            assertThat(finishedStrokeCohorts).hasSize(strokeIndex)

            activityScenarioRule.scenario.onActivity { activity ->
                activity.inProgressStrokesView.finishStroke(
                    stylusInputStream.getUpEvent(),
                    pointerIndex = 0,
                    strokeId,
                )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step3finish"))
            assertThat(finishedStrokeCohorts).hasSize(strokeCount)
            assertThat(finishedStrokeCohorts[strokeIndex]).hasSize(1)
        }
    }

    @Test
    fun fiveSimultaneousStokes_showsStrokesAndSendsOneCallback() {
        /**
         * The key is chosen such that the resulting screenshot filenames are alphabetically in the
         * same order in which they are produced, making it easier to follow the sequence of stroke
         * events. To aid in that, choose [action] names that are in alphabetical order, e.g.
         * prefixing their readable name with an action number.
         *
         * For simultaneous strokes, each action is applied to all the strokes before moving onto
         * the next action, so the action name comes before the stroke number in the file name.
         */
        fun screenshotKey(strokeCount: Int, action: String) =
            "five_simultaneous_after_${action}_stroke$strokeCount"

        val stylusInputStreams =
            BRUSH_COLORS.indices.map { strokeIndex ->
                val strokeCount = strokeIndex + 1
                InputStreamBuilder.stylusLine(
                    startX = 15F * strokeCount,
                    startY = 45F * strokeCount,
                    endX = 400F - 10F * strokeCount,
                    endY = 600F - 35F * strokeCount,
                )
            }
        val strokeIds = Array<InProgressStrokeId?>(stylusInputStreams.size) { null }
        for (strokeIndex in strokeIds.indices) {
            val strokeCount = strokeIndex + 1
            activityScenarioRule.scenario.onActivity { activity ->
                strokeIds[strokeIndex] =
                    activity.inProgressStrokesView.startStroke(
                        stylusInputStreams[strokeIndex].getDownEvent(),
                        pointerIndex = 0,
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step1start"))
            assertThat(finishedStrokeCohorts).isEmpty()
        }

        for (strokeIndex in strokeIds.indices) {
            val strokeCount = strokeIndex + 1
            activityScenarioRule.scenario.onActivity { activity ->
                activity.inProgressStrokesView.addToStroke(
                    stylusInputStreams[strokeIndex].getNextMoveEvent(),
                    pointerIndex = 0,
                    checkNotNull(strokeIds[strokeIndex]),
                    prediction = null,
                )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step2add"))
            assertThat(finishedStrokeCohorts).isEmpty()
        }

        for (strokeIndex in strokeIds.indices) {
            val strokeCount = strokeIndex + 1
            activityScenarioRule.scenario.onActivity { activity ->
                activity.inProgressStrokesView.finishStroke(
                    stylusInputStreams[strokeIndex].getUpEvent(),
                    pointerIndex = 0,
                    checkNotNull(strokeIds[strokeIndex]),
                )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step3finish"))
            if (strokeCount == strokeIds.size) {
                assertThat(finishedStrokeCohorts).hasSize(1)
                assertThat(finishedStrokeCohorts[0]).hasSize(strokeIds.size)
            } else {
                assertThat(finishedStrokeCohorts).isEmpty()
            }
        }
    }

    @Test
    fun removeFinishedStrokes_showsNoMoreStrokes() {
        val strokeIds = mutableSetOf<InProgressStrokeId>()
        activityScenarioRule.scenario.onActivity { activity ->
            repeat(BRUSH_COLORS.size) { strokeIndex ->
                val strokeCount = strokeIndex + 1
                val stylusInputStream =
                    InputStreamBuilder.stylusLine(
                        startX = 15F * strokeCount,
                        startY = 45F * strokeCount,
                        endX = 400F - 10F * strokeCount,
                        endY = 600F - 35F * strokeCount,
                    )
                val strokeId =
                    activity.inProgressStrokesView.startStroke(
                        stylusInputStream.getDownEvent(),
                        pointerIndex = 0,
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
                strokeIds.add(strokeId)
                activity.inProgressStrokesView.addToStroke(
                    stylusInputStream.getNextMoveEvent(),
                    pointerIndex = 0,
                    strokeId,
                    prediction = null,
                )
                activity.inProgressStrokesView.finishStroke(
                    stylusInputStream.getUpEvent(),
                    pointerIndex = 0,
                    strokeId,
                )
            }
        }

        assertThatTakingScreenshotMatchesGolden("remove_finished_before_remove")
        // Don't care how they were grouped together for the callback, just that they all arrived.
        assertThat(finishedStrokeCohorts.sumOf(Map<InProgressStrokeId, Stroke>::size)).isEqualTo(5)
        val finishedStrokeIds = mutableSetOf<InProgressStrokeId>()
        for (cohort in finishedStrokeCohorts) {
            finishedStrokeIds.addAll(cohort.keys)
        }
        assertThat(finishedStrokeIds).containsExactlyElementsIn(strokeIds)

        assertThat(strokeIds).hasSize(5)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.removeFinishedStrokes(strokeIds)
        }
        assertThatTakingScreenshotMatchesGolden("remove_finished")
    }

    private fun runMultiTouchGesture(
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
                                pointerIndex,
                                basicBrush(color = BRUSH_COLORS[pointerIdToStrokeId.size]),
                            )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        for (pointerIndex in 0 until event.pointerCount) {
                            val pointerId = event.getPointerId(pointerIndex)
                            val strokeId = checkNotNull(pointerIdToStrokeId[pointerId])
                            activity.inProgressStrokesView.addToStroke(
                                event,
                                pointerIndex,
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
                            activity.inProgressStrokesView.finishStroke(
                                event,
                                pointerIndex,
                                strokeId
                            )
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
    private fun yieldingSleep(timeMs: Long = 1000) {
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

    private fun basicBrush(@ColorInt color: Int) =
        Brush.createWithColorIntArgb(
            family = StockBrushes.markerLatest,
            colorIntArgb = color,
            size = 25F,
            epsilon = 0.1F,
        )

    private companion object {
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
