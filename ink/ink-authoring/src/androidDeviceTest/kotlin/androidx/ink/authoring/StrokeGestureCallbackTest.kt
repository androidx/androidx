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
@OptIn(ExperimentalInkCustomShapeWorkflowApi::class)
class StrokeGestureCallbackTest() : InProgressStrokesViewTestBase() {

    @Test
    fun strokeGestureCallback_withRestrictToSingleStroke_doesNotAllowMultipleStrokes() {
        val greenBrush = basicBrush(TestColors.AVOCADO_GREEN)
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            val strokeGestureCallback: StrokeGestureCallback? =
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
                    eventTime,
                )
            eventTime += 1000L
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
                    eventTime,
                )
            assertThat(strokeGestureCallback!!.onTouch(activity.inProgressStrokesView, firstTDown))
                .isTrue()
            assertThat(strokeGestureCallback.onTouch(activity.inProgressStrokesView, secondTDown))
                .isFalse()
        }
    }

    @Test
    fun strokeGestureCallback_showsStrokesAndSendsCallbacks() {
        val greenBrush = basicBrush(TestColors.AVOCADO_GREEN)
        val blueBrush = basicBrush(TestColors.COBALT_BLUE)
        val yellowBrush = basicBrush(TestColors.YELLOW)
        val redBrush = basicBrush(TestColors.RED)
        var strokeGestureCallback: StrokeGestureCallback? = null
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            strokeGestureCallback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    // Used for first stroke
                    brushForNewStrokes = greenBrush,
                )
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            // Used for second stroke
            strokeGestureCallback.brushForNewStrokes = blueBrush
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            // Used for third (cancelled early) stroke
            strokeGestureCallback.brushForNewStrokes = yellowBrush
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            // Used for forth stroke (canceled later)
            strokeGestureCallback.brushForNewStrokes = redBrush
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
        }
        assertThatTakingScreenshotMatchesGolden("start_or_update_strokes_four_strokes_started")

        activityScenarioRule.scenario.onActivity { activity ->
            eventTime += 1000L
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
        }
        assertThatTakingScreenshotMatchesGolden(
            "start_or_update_strokes_yellow_stroke_canceled_three_strokes_extended"
        )

        activityScenarioRule.scenario.onActivity { activity ->
            eventTime += 1000L
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isFalse()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
                        ),
                    )
                )
                .isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()
            eventTime += 1000L
            assertThat(
                    strokeGestureCallback.onTouch(
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
                            eventTime,
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

    /**
     * Verifies that when an unstarted [MotionEvent.ACTION_MOVE] event from a second input device
     * (different [MotionEvent.getDeviceId]) arrives mid-gesture, it is ignored without canceling
     * the in-progress stroke from the first device.
     */
    @Test
    fun strokeGestureCallback_unstartedMoveFromSecondDevice_doesNotCancelFirstDeviceStroke() {
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            val callback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                )

            // Device 1 starts stroke.
            val downDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            eventTime += 1000L
            val moveDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_MOVE,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, downDev1)).isTrue()
            assertThat(callback.onTouch(activity.inProgressStrokesView, moveDev1)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()

            // Device 2 sends a MOVE event without a prior ACTION_DOWN.
            eventTime += 1000L
            val moveDev2 =
                getMotionEvent(
                    MotionEvent.ACTION_MOVE,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 200f
                            y = 200f
                        }
                    ),
                    eventTime,
                    deviceId = 2,
                )
            // Unstarted move from Device 2 is ignored; Device 1's stroke remains in progress.
            assertThat(callback.onTouch(activity.inProgressStrokesView, moveDev2)).isFalse()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes(1)).isTrue()

            // Device 1 finishes normally.
            eventTime += 1000L
            val upDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_UP,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, upDev1)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
        }
        yieldingSleep()
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    /**
     * Verifies that a framework [MotionEvent.ACTION_CANCEL] event (`actionIndex == 0`) cancels all
     * active in-progress strokes across multiple pointers, rather than only the pointer at index 0.
     */
    @Test
    fun strokeGestureCallback_actionCancelCancelsAllStrokes() {
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            val callback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                )

            // Start 2 strokes.
            val down1 =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        }
                    ),
                    eventTime,
                )
            eventTime += 1000L
            val down2 =
                getMotionEvent(
                    MotionEvent.ACTION_POINTER_DOWN or
                        (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    arrayOf(
                        PointerProperties().apply { id = 0 },
                        PointerProperties().apply { id = 1 },
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
                    eventTime,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, down1)).isTrue()
            assertThat(callback.onTouch(activity.inProgressStrokesView, down2)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()

            // Framework ACTION_CANCEL (actionIndex = 0) with both pointers.
            eventTime += 1000L
            val cancelEvent =
                getMotionEvent(
                    MotionEvent.ACTION_CANCEL,
                    arrayOf(
                        PointerProperties().apply { id = 0 },
                        PointerProperties().apply { id = 1 },
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
                    eventTime,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, cancelEvent)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
        }
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    /**
     * Verifies that when `isRestrictedToSingleStroke` is `true`, input events from a second device
     * are ignored while a stroke from the first device is in progress, allowing the first device's
     * stroke to finish uninterrupted.
     */
    @Test
    fun strokeGestureCallback_withRestrictToSingleStroke_secondDeviceDoesNotInterrupt() {
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            val callback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                    isRestrictedToSingleStroke = true,
                )

            // Device 1 starts stroke.
            val downDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, downDev1)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()

            // Device 2 attempts to send ACTION_DOWN while Device 1's stroke is in progress.
            eventTime += 1000L
            val downDev2 =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 200f
                            y = 200f
                        }
                    ),
                    eventTime,
                    deviceId = 2,
                )
            // Device 2 is rejected because single stroke mode is restricted and Device 1 is active.
            assertThat(callback.onTouch(activity.inProgressStrokesView, downDev2)).isFalse()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()

            // Device 2 move is also rejected.
            eventTime += 1000L
            val moveDev2 =
                getMotionEvent(
                    MotionEvent.ACTION_MOVE,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 210f
                            y = 210f
                        }
                    ),
                    eventTime,
                    deviceId = 2,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, moveDev2)).isFalse()

            // Device 1 continues and finishes normally.
            eventTime += 1000L
            val moveDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_MOVE,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            eventTime += 1000L
            val upDev1 =
                getMotionEvent(
                    MotionEvent.ACTION_UP,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                    deviceId = 1,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, moveDev1)).isTrue()
            assertThat(callback.onTouch(activity.inProgressStrokesView, upDev1)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()
        }
        yieldingSleep()
        assertThat(finishedStrokeCohorts).hasSize(1)
    }

    /**
     * Verifies that after [InProgressStrokesView.cancelUnfinishedStrokes] is called externally,
     * subsequent move and up events for the canceled pointer are ignored without crashing.
     */
    @Test
    fun strokeGestureCallback_cancelUnfinishedStrokes_subsequentMoveIgnoredWithoutCrash() {
        var eventTime = 0L
        activityScenarioRule.scenario.onActivity { activity ->
            val callback =
                StrokeGestureCallback(
                    inProgressStrokesView = activity.inProgressStrokesView,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                )

            val down =
                getMotionEvent(
                    MotionEvent.ACTION_DOWN,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 100f
                            y = 100f
                        }
                    ),
                    eventTime,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, down)).isTrue()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isTrue()

            // Cancel all unfinished strokes externally.
            activity.inProgressStrokesView.cancelUnfinishedStrokes()
            assertThat(activity.inProgressStrokesView.hasUnfinishedStrokes()).isFalse()

            // Subsequent MOVE event for the same pointer is ignored without crashing.
            eventTime += 1000L
            val move =
                getMotionEvent(
                    MotionEvent.ACTION_MOVE,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, move)).isFalse()

            eventTime += 1000L
            val up =
                getMotionEvent(
                    MotionEvent.ACTION_UP,
                    arrayOf(PointerProperties().apply { id = 0 }),
                    arrayOf(
                        PointerCoords().apply {
                            x = 150f
                            y = 100f
                        }
                    ),
                    eventTime,
                )
            assertThat(callback.onTouch(activity.inProgressStrokesView, up)).isFalse()
        }
        assertThat(finishedStrokeCohorts).isEmpty()
    }

    @Test
    fun multiDevice_separateCallbacks_interleavedGesturesDoNotCancelEachOther() {
        activityScenarioRule.scenario.onActivity { activity ->
            val view = activity.inProgressStrokesView
            val callbackDevice1 =
                StrokeGestureCallback(
                    inProgressStrokesView = view,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                )
            val callbackDevice2 =
                StrokeGestureCallback(
                    inProgressStrokesView = view,
                    brushForNewStrokes = basicBrush(TestColors.RED),
                )

            var eventTime = 0L

            // 1. Device 1 starts stroke
            val downDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_DOWN,
                    deviceId = 1,
                    pointerId = 0,
                    x = 100f,
                    y = 100f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callbackDevice1.onTouch(view, downDev1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()

            // 2. Device 2 starts stroke (interleaved)
            val downDev2 =
                getMotionEvent(
                    action = MotionEvent.ACTION_DOWN,
                    deviceId = 2,
                    pointerId = 0,
                    x = 200f,
                    y = 200f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callbackDevice2.onTouch(view, downDev2)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isTrue()

            // 3. Device 1 moves
            val moveDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_MOVE,
                    deviceId = 1,
                    pointerId = 0,
                    x = 120f,
                    y = 120f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callbackDevice1.onTouch(view, moveDev1)).isTrue()

            // 4. Device 2 receives ACTION_CANCEL (cancels only Device 2's gesture)
            val cancelDev2 =
                getMotionEvent(
                    action = MotionEvent.ACTION_CANCEL,
                    deviceId = 2,
                    pointerId = 0,
                    x = 200f,
                    y = 200f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callbackDevice2.onTouch(view, cancelDev2)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()

            // 5. Device 1 finishes normally with ACTION_UP
            val upDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_UP,
                    deviceId = 1,
                    pointerId = 0,
                    x = 150f,
                    y = 150f,
                    eventTime = eventTime,
                )
            assertThat(callbackDevice1.onTouch(view, upDev1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isFalse()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()
        }

        yieldingSleep()
        // Exactly 1 stroke finished: Device 1's stroke! Device 2's stroke was canceled.
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(1)
    }

    /**
     * Verifies that a single [StrokeGestureCallback] instance supports simultaneous interleaved
     * strokes from multiple input devices (even when both devices report `pointerId = 0`),
     * finishing or canceling each device's stroke independently.
     */
    @Test
    fun multiDevice_singleCallback_interleavedGesturesCompleteAndCancelIndependently() {
        activityScenarioRule.scenario.onActivity { activity ->
            val view = activity.inProgressStrokesView
            val callback =
                StrokeGestureCallback(
                    inProgressStrokesView = view,
                    brushForNewStrokes = basicBrush(TestColors.AVOCADO_GREEN),
                )

            var eventTime = 0L

            // 1. Device 1 starts stroke (pointerId = 0)
            val downDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_DOWN,
                    deviceId = 1,
                    pointerId = 0,
                    x = 100f,
                    y = 100f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callback.onTouch(view, downDev1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()

            // 2. Device 2 starts stroke on the same callback (pointerId = 0)
            callback.brushForNewStrokes = basicBrush(TestColors.RED)
            val downDev2 =
                getMotionEvent(
                    action = MotionEvent.ACTION_DOWN,
                    deviceId = 2,
                    pointerId = 0,
                    x = 200f,
                    y = 200f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callback.onTouch(view, downDev2)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isTrue()

            // 3. Device 1 and Device 2 both move
            val moveDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_MOVE,
                    deviceId = 1,
                    pointerId = 0,
                    x = 120f,
                    y = 120f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callback.onTouch(view, moveDev1)).isTrue()

            val moveDev2 =
                getMotionEvent(
                    action = MotionEvent.ACTION_MOVE,
                    deviceId = 2,
                    pointerId = 0,
                    x = 220f,
                    y = 220f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callback.onTouch(view, moveDev2)).isTrue()

            // 4. Device 2 finishes with ACTION_UP while Device 1 is still drawing
            val upDev2 =
                getMotionEvent(
                    action = MotionEvent.ACTION_UP,
                    deviceId = 2,
                    pointerId = 0,
                    x = 250f,
                    y = 250f,
                    eventTime = eventTime,
                )
            eventTime += 1000L
            assertThat(callback.onTouch(view, upDev2)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()

            // 5. Device 1 finishes with ACTION_UP
            val upDev1 =
                getMotionEvent(
                    action = MotionEvent.ACTION_UP,
                    deviceId = 1,
                    pointerId = 0,
                    x = 150f,
                    y = 150f,
                    eventTime = eventTime,
                )
            assertThat(callback.onTouch(view, upDev1)).isTrue()
            assertThat(view.hasUnfinishedStrokes(1)).isFalse()
            assertThat(view.hasUnfinishedStrokes(2)).isFalse()
        }

        yieldingSleep()
        assertThat(finishedStrokeCohorts).hasSize(1)
        assertThat(finishedStrokeCohorts[0]).hasSize(2)
    }

    private fun getMotionEvent(
        action: Int,
        pointerProperties: Array<PointerProperties>,
        pointerCoords: Array<PointerCoords>,
        eventTime: Long,
        deviceId: Int = 0,
    ): MotionEvent =
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
            /*deviceId=*/ deviceId,
            /*edgeFlags=*/ 0,
            /*source=*/ 0,
            /*flags=*/ 0,
        )

    private fun getMotionEvent(
        action: Int,
        deviceId: Int,
        pointerId: Int,
        x: Float,
        y: Float,
        eventTime: Long,
    ): MotionEvent =
        getMotionEvent(
            action = action,
            pointerProperties = arrayOf(PointerProperties().apply { id = pointerId }),
            pointerCoords =
                arrayOf(
                    PointerCoords().apply {
                        this.x = x
                        this.y = y
                    }
                ),
            eventTime = eventTime,
            deviceId = deviceId,
        )
}
