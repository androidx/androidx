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

package androidx.ink.authoring

import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests that drive [InProgressStrokesView] with a [StrokeGestureCallback]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalInkCustomBrushApi::class)
class StrokeGestureCallbackTest() : InProgressStrokesViewTestBase() {

    @Test
    fun strokeGestureCallback_withRestrictToSingleStroke_doesNotAllowMultipleStrokes() {
        val greenBrush = basicBrush(TestColors.AVOCADO_GREEN)
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
        activityScenarioRule.scenario.onActivity { activity ->
            var strokeGestureCallback: StrokeGestureCallback? =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    // Used for first stroke
                    brushForNewStrokes = greenBrush,
                    isRestrictedToSingleStroke = true,
                )
            val firstTDown =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN or (0 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    arrayOf(PointerProperties().apply { id = 5 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        }
                    ),
                )
            val secondTDown =
                getMotionEvent(
                    MotionEvent.ACTION_POINTER_DOWN or
                        (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    arrayOf(
                        PointerProperties().apply { id = 5 },
                        PointerProperties().apply { id = 6 },
                    ),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        },
                        PointerCoords().apply {
                            x = 200f
                            y = 100f
                        },
                    ),
                )
            assertThat(strokeGestureCallback!!.onTouch(activity.inProgressStrokesView, firstTDown))
                .isTrue()
            assertThat(strokeGestureCallback!!.onTouch(activity.inProgressStrokesView, secondTDown))
                .isFalse()
        }
    }

    @Test
    fun strokeGestureCallback_showsStrokesAndSendsCallbacks() {
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
        val greenBrush = basicBrush(TestColors.AVOCADO_GREEN)
        val blueBrush = basicBrush(TestColors.COBALT_BLUE)
        val yellowBrush = basicBrush(TestColors.YELLOW)
        val redBrush = basicBrush(TestColors.RED)
        var strokeGestureCallback: StrokeGestureCallback? = null
        activityScenarioRule.scenario.onActivity { activity ->
            strokeGestureCallback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    // Used for first stroke
                    brushForNewStrokes = greenBrush,
                )
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_DOWN or
                                (0 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(PointerProperties().apply { id = 5 }),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 100f
                                }
                            ),
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            // Used for second stroke
            strokeGestureCallback!!.brushForNewStrokes = blueBrush
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_POINTER_DOWN or
                                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 200f
                                    y = 100f
                                },
                            ),
                        ),
                    )
                )
                .isTrue()
            // Used for third (cancelled early) stroke
            strokeGestureCallback!!.brushForNewStrokes = yellowBrush
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_POINTER_DOWN or
                                (2 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 200f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 300f
                                    y = 100f
                                },
                            ),
                        ),
                    )
                )
                .isTrue()
            // Used for forth stroke (canceled later)
            strokeGestureCallback!!.brushForNewStrokes = redBrush
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_POINTER_DOWN or
                                (3 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                                PointerProperties().apply { id = 8 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 200f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 300f
                                    y = 100f
                                },
                                PointerCoords().apply {
                                    x = 400f
                                    y = 100f
                                },
                            ),
                        ),
                    )
                )
                .isTrue()
        }
        assertThatTakingScreenshotMatchesGolden("start_or_update_strokes_four_strokes_started")

        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_CANCEL or
                                (2 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                                PointerProperties().apply { id = 8 },
                            ),
                            arrayOf(
                                PointerCoords(),
                                PointerCoords(),
                                PointerCoords(),
                                PointerCoords(),
                            ),
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_MOVE,
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                                PointerProperties().apply { id = 8 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 200f
                                },
                                PointerCoords().apply {
                                    x = 200f
                                    y = 200f
                                },
                                PointerCoords().apply {
                                    x = 300f
                                    y = 200f
                                },
                                PointerCoords().apply {
                                    x = 400f
                                    y = 200f
                                },
                            ),
                        ),
                    )
                )
                .isTrue()
        }
        assertThatTakingScreenshotMatchesGolden(
            "start_or_update_strokes_yellow_stroke_canceled_three_strokes_extended"
        )

        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_CANCEL or
                                (3 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                                PointerProperties().apply { id = 8 },
                            ),
                            arrayOf(
                                PointerCoords(),
                                PointerCoords(),
                                PointerCoords(),
                                PointerCoords(),
                            ),
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_POINTER_UP or
                                (2 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                                PointerProperties().apply { id = 7 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 200f
                                },
                                PointerCoords().apply {
                                    x = 200f
                                    y = 200f
                                },
                                // Not used because the stroke is already canceled
                                PointerCoords().apply {
                                    x = 350f
                                    y = 200f
                                },
                            ),
                        ),
                    )
                )
                .isFalse()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_POINTER_UP or
                                (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                            arrayOf(
                                PointerProperties().apply { id = 5 },
                                PointerProperties().apply { id = 6 },
                            ),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 100f
                                    y = 200f
                                },
                                PointerCoords().apply {
                                    x = 250f
                                    y = 200f
                                },
                            ),
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            assertThat(
                    strokeGestureCallback!!.onTouch(
                        activity.inProgressStrokesView,
                        getMotionEvent(
                            MotionEvent.ACTION_UP,
                            arrayOf(PointerProperties().apply { id = 5 }),
                            arrayOf(
                                PointerCoords().apply {
                                    x = 150f
                                    y = 200f
                                }
                            ),
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
        }
        assertThatTakingScreenshotMatchesGolden(
            "start_or_update_strokes_red_stroke_canceled_two_strokes_finished_with_right_angle"
        )
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(2)
    }
}
