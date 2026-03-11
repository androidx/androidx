/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.trackpad

import android.os.Build
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerEventType.Companion.ScaleChange
import androidx.compose.ui.input.pointer.PointerEventType.Companion.ScaleEnd
import androidx.compose.ui.input.pointer.PointerEventType.Companion.ScaleStart
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.injectionscope.trackpad.Common.verifyTrackpadEvent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.scale
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ScaleTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private const val TAG = "SCALE"
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = MultiPointerInputRecorder()

    @Test
    fun pinchTogether() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, width = 300f, height = 300f, tag = TAG)
            }
        }

        rule.onNodeWithTag(TAG).performTrackpadInput {
            moveTo(center)
            scale(0.9f)
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                // expect up and down events for each pointer as well as the move events
                assertThat(events.size).isEqualTo(7)
                events[0].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(T, Enter, false, Offset(150f, 150f))
                        assertThat(pointer.classification)
                            .isEqualTo(MotionEvent.CLASSIFICATION_NONE)
                    }
                }
                events[1].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleStart
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(50f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)

                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                }
                events[2].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(50f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(250f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[3].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 2,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Move
                                },
                            expectedDown = true,
                            expectedPosition = Offset(60f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(0.9f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T * 2,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Move
                                },
                            expectedDown = true,
                            expectedPosition = Offset(240f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[4].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Release
                                },
                            expectedDown = true,
                            expectedPosition = Offset(60f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Release
                                },
                            expectedDown = false,
                            expectedPosition = Offset(240f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[5].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleEnd
                                } else {
                                    Release
                                },
                            expectedDown = false,
                            expectedPosition = Offset(60f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                }
                events[6].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(T * 3, Enter, false, Offset(150f, 150f))
                        assertThat(pointer.classification)
                            .isEqualTo(MotionEvent.CLASSIFICATION_NONE)
                    }
                }
            }
        }
    }

    @Test
    fun pinchAway() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, width = 300f, height = 300f, tag = TAG)
            }
        }

        rule.onNodeWithTag(TAG).performTrackpadInput {
            moveTo(center)
            scale(1.1f)
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                // expect up and down events for each pointer as well as the move events
                assertThat(events.size).isEqualTo(7)
                events[0].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(T, Enter, false, Offset(150f, 150f))
                        assertThat(pointer.classification)
                            .isEqualTo(MotionEvent.CLASSIFICATION_NONE)
                    }
                }
                events[1].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleStart
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(50f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                }
                events[2].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(50f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Press
                                },
                            expectedDown = true,
                            expectedPosition = Offset(250f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[3].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 2,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Move
                                },
                            expectedDown = true,
                            expectedPosition = Offset(40f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1.1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T * 2,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Move
                                },
                            expectedDown = true,
                            expectedPosition = Offset(260f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[4].let { event ->
                    assertThat(event.pointers.size).isEqualTo(2)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Release
                                },
                            expectedDown = true,
                            expectedPosition = Offset(40f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                    event
                        .getPointer(1)
                        .verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleChange
                                } else {
                                    Release
                                },
                            expectedDown = false,
                            expectedPosition = Offset(260f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                }
                events[5].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(
                            expectedTimestamp = T * 3,
                            expectedEventType =
                                if (
                                    ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                                        Build.VERSION.SDK_INT >= 34
                                ) {
                                    ScaleEnd
                                } else {
                                    Release
                                },
                            expectedDown = false,
                            expectedPosition = Offset(40f, 150f),
                            expectedPointerType = PointerType.Touch,
                        )
                        assertThat(pointer.axisGestureScaleFactor).isEqualTo(1f)
                        assertThat(pointer.classification)
                            .isEqualTo(
                                if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                                else MotionEvent.CLASSIFICATION_NONE
                            )
                    }
                }
                events[6].let { event ->
                    assertThat(event.pointers.size).isEqualTo(1)
                    event.getPointer(0).let { pointer ->
                        pointer.verifyTrackpadEvent(T * 3, Enter, false, Offset(150f, 150f))
                        assertThat(pointer.classification)
                            .isEqualTo(MotionEvent.CLASSIFICATION_NONE)
                    }
                }
            }
        }
    }
}
