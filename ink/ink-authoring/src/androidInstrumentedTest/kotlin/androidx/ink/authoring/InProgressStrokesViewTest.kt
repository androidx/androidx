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

import android.graphics.Matrix
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import androidx.ink.authoring.testing.InputStreamBuilder
import androidx.ink.authoring.testing.MultiTouchInputBuilder
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based test of [InProgressStrokesView]. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InProgressStrokesViewTest : InProgressStrokesViewTestBase() {

    @Test
    fun startStroke_showsStrokeWithNoCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        val downEvent = stylusInputStream.getDownEvent()
        activityScenarioRule.scenario.onActivity { activity ->
            @Suppress("UNUSED_VARIABLE")
            val unused =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
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
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            val moveEvent = stylusInputStream.getNextMoveEvent()
            activity.inProgressStrokesView.addToStroke(
                moveEvent,
                moveEvent.getPointerId(0),
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
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            val upEvent = stylusInputStream.getUpEvent()
            activity.inProgressStrokesView.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        }

        assertThatTakingScreenshotMatchesGolden("start_and_finish")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
        val stroke = finishedStrokeCohorts[0].values.iterator().next()

        // Stroke units are set to view coordinate units which by default is screen pixels, so the
        // stroke unit length should be 1/dpi inches, which is 2.54/dpi cm.
        val metrics = InstrumentationRegistry.getInstrumentation().context.resources.displayMetrics
        assertThat(stroke.inputs.getStrokeUnitLengthCm()).isWithin(1e-5f).of(2.54f / metrics.xdpi)
    }

    @Test
    fun startAndFinishStroke_strokeUnitLengthFactorsInViewScale() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.scaleX = 0.5f
            activity.inProgressStrokesView.scaleY = 0.5f
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            val upEvent = stylusInputStream.getUpEvent()
            activity.inProgressStrokesView.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        }

        assertThatTakingScreenshotMatchesGolden("start_and_finish_scaled")
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
        val stroke = finishedStrokeCohorts[0].values.iterator().next()

        // Stroke units are set to view coordinate units which by default is screen pixels, but the
        // view
        // is scaled down by half, so the stroke unit length should be 0.5/dpi inches, which is
        // 0.5*2.54/dpi cm.
        val metrics = InstrumentationRegistry.getInstrumentation().context.resources.displayMetrics
        assertThat(stroke.inputs.getStrokeUnitLengthCm())
            .isWithin(1e-5f)
            .of(0.5f * 2.54f / metrics.xdpi)
    }

    @Test
    fun startAndFinishStroke_withNonIdentityTransforms() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25f, startY = 25f, endX = 105f, endY = 205f)
        activityScenarioRule.scenario.onActivity { activity ->
            val metrics = activity.resources.displayMetrics
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    pointerId = downEvent.getPointerId(0),
                    Brush.createWithColorIntArgb(
                        family = StockBrushes.markerLatest,
                        colorIntArgb = TestColors.AVOCADO_GREEN,
                        size = 0.5F,
                        epsilon = 0.001F,
                    ),
                    // MotionEvent space uses pixels, so this transform sets world units equal to
                    // inches.
                    motionEventToWorldTransform =
                        Matrix().apply { setScale(1f / metrics.xdpi, 1f / metrics.ydpi) },
                    // Set one stroke unit equal to half a world unit (i.e. half an inch).
                    strokeToWorldTransform = Matrix().apply { setScale(0.5f, 0.5f) },
                )
            val upEvent = stylusInputStream.getUpEvent()
            activity.inProgressStrokesView.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
        }

        assertThatTakingScreenshotMatchesGolden("start_and_finish_non_identity")
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
            val downEvent = stylusInputStream.getDownEvent()
            assertThrows(IllegalArgumentException::class.java) {
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                    motionEventToWorldTransform = Matrix().apply { setScale(0f, 0f) },
                )
            }
            assertThrows(IllegalArgumentException::class.java) {
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
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
            val downEvent = stylusInputStream.getDownEvent()
            strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
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
    fun startAndCancelUnfinishedStrokes_hidesStrokeWithNoCallback() {
        val getMotionEvent =
            {
                action: Int,
                eventTime: Long,
                pointerProperties: Array<PointerProperties>,
                pointerCoords: Array<PointerCoords> ->
                MotionEvent.obtain(
                    /*downTime=*/ 0L,
                    /*eventTime=*/ eventTime,
                    /*action=*/ action,
                    /*pointerCount=*/ pointerProperties.size,
                    /*pointerProperties=*/ pointerProperties,
                    /*pointerCoords=*/ pointerCoords,
                    /*metaState=*/ 0,
                    /*buttonState=*/ 0,
                    /*xPrecision=*/ 1f,
                    /*yPrecision=*/ 1f,
                    /*deviceId=*/ 0,
                    /*edgeFlags=*/ 0,
                    /*source=*/ 0,
                    /*flags=*/ 0,
                )
            }
        val downEvent =
            getMotionEvent(
                MotionEvent.ACTION_DOWN,
                0L,
                arrayOf(
                    PointerProperties().apply { id = 9 },
                    PointerProperties().apply { id = 10 }
                ),
                arrayOf(
                    PointerCoords().apply {
                        x = 25f
                        y = 25f
                    },
                    PointerCoords().apply {
                        x = 50f
                        y = 25f
                    },
                ),
            )
        activityScenarioRule.scenario.onActivity { activity ->
            // Two strokes get started with different pointerIds.
            @Suppress("CheckReturnValue")
            activity.inProgressStrokesView.startStroke(
                downEvent,
                9,
                basicBrush(TestColors.AVOCADO_GREEN)
            )
            @Suppress("CheckReturnValue")
            activity.inProgressStrokesView.startStroke(downEvent, 10, basicBrush(TestColors.RED))
        }
        assertThatTakingScreenshotMatchesGolden("cancel_unfinished_strokes_two_strokes_started")
        assertThat(finishedStrokeCohorts).isEmpty()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.cancelUnfinishedStrokes()
        }
        assertThatTakingScreenshotMatchesGolden("cancel_unfinished_strokes_both_strokes_canceled")
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun startAndCancelStroke_hidesStrokeWithNoCallback_simplifiedApiImplicitStrokeId() {
        var eventTime = 0L
        val getMotionEvent =
            {
                action: Int,
                pointerProperties: Array<PointerProperties>,
                pointerCoords: Array<PointerCoords> ->
                MotionEvent.obtain(
                        /*downTime=*/ 0L,
                        /*eventTime=*/ eventTime,
                        /*action=*/ action,
                        /*pointerCount=*/ pointerProperties.size,
                        /*pointerProperties=*/ pointerProperties,
                        /*pointerCoords=*/ pointerCoords,
                        /*metaState=*/ 0,
                        /*buttonState=*/ 0,
                        /*xPrecision=*/ 1f,
                        /*yPrecision=*/ 1f,
                        /*deviceId=*/ 0,
                        /*edgeFlags=*/ 0,
                        /*source=*/ 0,
                        /*flags=*/ 0,
                    )
                    .also { eventTime += 1000L }
            }
        val downEvent =
            getMotionEvent(
                MotionEvent.ACTION_DOWN,
                arrayOf(
                    PointerProperties().apply { id = 9 },
                    PointerProperties().apply { id = 10 }
                ),
                arrayOf(
                    PointerCoords().apply {
                        x = 25f
                        y = 25f
                    },
                    PointerCoords().apply {
                        x = 50f
                        y = 25f
                    },
                ),
            )
        activityScenarioRule.scenario.onActivity { activity ->
            // Two strokes get started with different pointerIds.
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
            activity.inProgressStrokesView.startStroke(
                downEvent,
                9,
                basicBrush(TestColors.AVOCADO_GREEN)
            )
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            activity.inProgressStrokesView.startStroke(downEvent, 10, basicBrush(TestColors.RED))
        }
        assertThatTakingScreenshotMatchesGolden("cancel_strokes_simplified_api_two_strokes_started")
        assertThat(finishedStrokeCohorts).isEmpty()

        // In practice, CANCEL events will have only one pointer. But this method doesn't actually
        // care whether or not it's a cancel event and it cancels all pointers present.
        val cancelEvent =
            getMotionEvent(
                MotionEvent.ACTION_CANCEL,
                arrayOf(
                    PointerProperties().apply { id = 9 },
                    // If the pointerId doesn't correspond to a started stroke, it's ignored.
                    PointerProperties().apply { id = 3 },
                ),
                arrayOf(PointerCoords(), PointerCoords(), PointerCoords()),
            )
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.inProgressStrokesView.cancelStroke(cancelEvent, 9)).isTrue()
            assertThat(activity.inProgressStrokesView.cancelStroke(cancelEvent, 3)).isFalse()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
        }
        assertThatTakingScreenshotMatchesGolden(
            "cancel_strokes_simplified_api_green_stroke_canceled"
        )
        assertThat(finishedStrokeCohorts).isEmpty()

        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.cancelUnfinishedStrokes()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
        }
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun startAndAddToAndFinishStroke_showsStrokeAndSendsCallback() {
        val stylusInputStream =
            InputStreamBuilder.stylusLine(startX = 25F, startY = 25F, endX = 105F, endY = 205F)
        activityScenarioRule.scenario.onActivity { activity ->
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            val moveEvent = stylusInputStream.getNextMoveEvent()
            activity.inProgressStrokesView.addToStroke(
                moveEvent,
                moveEvent.getPointerId(0),
                strokeId,
                prediction = null,
            )
            val upEvent = stylusInputStream.getUpEvent()
            activity.inProgressStrokesView.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
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
    fun startAndAddToAndFinishStroke_withTransform_showsStrokeAndSendsCallback_strokeInputApi() {
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
                    strokeToViewTransform =
                        Matrix().apply {
                            postScale(2F, 3F)
                            postTranslate(100F, 200F)
                            postRotate(15F)
                        },
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

        assertThatTakingScreenshotMatchesGolden(
            "start_and_add_and_finish_stroke_input_api_with_transform"
        )
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    @Test
    fun startAndAddToAndFinishStroke_implicitStrokeIdSimplifiedApi() {
        var eventTime = 0L
        val getMotionEvent =
            {
                action: Int,
                pointerProperties: Array<PointerProperties>,
                pointerCoords: Array<PointerCoords> ->
                MotionEvent.obtain(
                        /*downTime=*/ 0L,
                        /*eventTime=*/ eventTime,
                        /*action=*/ action,
                        /*pointerCount=*/ pointerProperties.size,
                        /*pointerProperties=*/ pointerProperties,
                        /*pointerCoords=*/ pointerCoords,
                        /*metaState=*/ 0,
                        /*buttonState=*/ 0,
                        /*xPrecision=*/ 1f,
                        /*yPrecision=*/ 1f,
                        /*deviceId=*/ 0,
                        /*edgeFlags=*/ 0,
                        /*source=*/ 0,
                        /*flags=*/ 0,
                    )
                    .also { eventTime += 1000L }
            }
        val downEvent =
            getMotionEvent(
                MotionEvent.ACTION_DOWN,
                arrayOf(PointerProperties().apply { id = 9 }),
                arrayOf(
                    PointerCoords().apply {
                        x = 100f
                        y = 100f
                    }
                ),
            )
        // Need to validate we're storing and looking up strokeId=9 and using that as a strokeId.
        // Not storing strokeIndex=0 (so we put that pointer at a different index) and not trying
        // to use the strokeId as an index.
        val moveEvent =
            getMotionEvent(
                MotionEvent.ACTION_MOVE,
                arrayOf(PointerProperties().apply { id = 0 }, PointerProperties().apply { id = 9 }),
                arrayOf(
                    PointerCoords().apply {
                        // Should not use this
                        x = 0f
                        y = 0f
                    },
                    PointerCoords().apply {
                        // Should use this
                        x = 200f
                        y = 100f
                    },
                ),
            )
        val upEvent =
            getMotionEvent(
                MotionEvent.ACTION_UP,
                arrayOf(PointerProperties().apply { id = 0 }, PointerProperties().apply { id = 9 }),
                arrayOf(
                    PointerCoords().apply {
                        // Should not use this
                        x = 0f
                        y = 0f
                    },
                    PointerCoords().apply {
                        // Should use this
                        x = 200f
                        y = 200f
                    },
                ),
            )
        activityScenarioRule.scenario.onActivity { activity ->
            activity.inProgressStrokesView.startStroke(
                event = downEvent,
                pointerId = 9,
                brush = basicBrush(TestColors.AVOCADO_GREEN),
            )

            // Updates for pointers that are not part of a started stroke are ignored.
            assertThat(
                    activity.inProgressStrokesView.addToStroke(
                        moveEvent,
                        pointerId = 0,
                        prediction = null
                    )
                )
                .isFalse()
            assertThat(activity.inProgressStrokesView.finishStroke(upEvent, pointerId = 0))
                .isFalse()

            assertThat(
                    activity.inProgressStrokesView.addToStroke(
                        moveEvent,
                        pointerId = 9,
                        prediction = null
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.finishStroke(upEvent, pointerId = 9)).isTrue()
        }

        assertThatTakingScreenshotMatchesGolden("start_and_add_and_finish_simplified_api")
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
            val downEvent = stylusInputStream.getDownEvent()
            val strokeId =
                activity.inProgressStrokesView.startStroke(
                    downEvent,
                    downEvent.getPointerId(0),
                    basicBrush(TestColors.AVOCADO_GREEN),
                )
            val moveEvent = stylusInputStream.getNextMoveEvent()
            activity.inProgressStrokesView.addToStroke(
                moveEvent,
                moveEvent.getPointerId(0),
                strokeId,
                prediction = null,
            )
            val upEvent = stylusInputStream.getUpEvent()
            activity.inProgressStrokesView.finishStroke(upEvent, upEvent.getPointerId(0), strokeId)
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
                val downEvent = stylusInputStream.getDownEvent()
                strokeId =
                    activity.inProgressStrokesView.startStroke(
                        downEvent,
                        downEvent.getPointerId(0),
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step1start"))
            assertThat(finishedStrokeCohorts).hasSize(strokeIndex)

            activityScenarioRule.scenario.onActivity { activity ->
                val moveEvent = stylusInputStream.getNextMoveEvent()
                activity.inProgressStrokesView.addToStroke(
                    moveEvent,
                    moveEvent.getPointerId(0),
                    strokeId,
                    prediction = null,
                )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step2add"))
            assertThat(finishedStrokeCohorts).hasSize(strokeIndex)

            activityScenarioRule.scenario.onActivity { activity ->
                val upEvent = stylusInputStream.getUpEvent()
                activity.inProgressStrokesView.finishStroke(
                    upEvent,
                    upEvent.getPointerId(0),
                    strokeId
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
            BRUSH_COLORS.indices.mapIndexed { i, strokeIndex ->
                val strokeCount = strokeIndex + 1
                InputStreamBuilder.stylusLine(
                    startX = 15F * strokeCount,
                    startY = 45F * strokeCount,
                    endX = 400F - 10F * strokeCount,
                    endY = 600F - 35F * strokeCount,
                    pointerId = i,
                )
            }
        val strokeIds = Array<InProgressStrokeId?>(stylusInputStreams.size) { null }
        for (strokeIndex in strokeIds.indices) {
            val strokeCount = strokeIndex + 1
            activityScenarioRule.scenario.onActivity { activity ->
                val downEvent = stylusInputStreams[strokeIndex].getDownEvent()
                strokeIds[strokeIndex] =
                    activity.inProgressStrokesView.startStroke(
                        downEvent,
                        downEvent.getPointerId(0),
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
            }
            assertThatTakingScreenshotMatchesGolden(screenshotKey(strokeCount, "step1start"))
            assertThat(finishedStrokeCohorts).isEmpty()
        }

        for (strokeIndex in strokeIds.indices) {
            val strokeCount = strokeIndex + 1
            activityScenarioRule.scenario.onActivity { activity ->
                val moveEvent = stylusInputStreams[strokeIndex].getNextMoveEvent()
                activity.inProgressStrokesView.addToStroke(
                    moveEvent,
                    moveEvent.getPointerId(0),
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
                val upEvent = stylusInputStreams[strokeIndex].getUpEvent()
                activity.inProgressStrokesView.finishStroke(
                    upEvent,
                    upEvent.getPointerId(0),
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
                val downEvent = stylusInputStream.getDownEvent()
                val strokeId =
                    activity.inProgressStrokesView.startStroke(
                        downEvent,
                        downEvent.getPointerId(0),
                        basicBrush(BRUSH_COLORS[strokeIndex]),
                    )
                strokeIds.add(strokeId)
                val moveEvent = stylusInputStream.getNextMoveEvent()
                activity.inProgressStrokesView.addToStroke(
                    moveEvent,
                    moveEvent.getPointerId(0),
                    strokeId,
                    prediction = null,
                )
                val upEvent = stylusInputStream.getUpEvent()
                activity.inProgressStrokesView.finishStroke(
                    upEvent,
                    upEvent.getPointerId(0),
                    strokeId
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
}
