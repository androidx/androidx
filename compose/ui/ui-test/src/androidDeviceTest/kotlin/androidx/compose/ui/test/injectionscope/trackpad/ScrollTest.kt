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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.verifyTrackpadEvent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
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
@OptIn(ExperimentalTestApi::class)
class ScrollTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private const val TAG = "SCROLL"
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = SinglePointerInputRecorder()

    @Test
    fun scrollVertically() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, width = 300f, height = 300f, tag = TAG)
            }
        }

        rule.onNodeWithTag(TAG).performTrackpadInput {
            enter()
            // scroll vertically
            scroll(Offset(0f, 10f))
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                assertThat(events.size).isEqualTo(5)
                events[0].verifyTrackpadEvent(T, Enter, false, Offset.Zero)
                // TODO: b/461873914
                //       the system sends an exit here, but we don't see it in Compose currently
                events[1].let { event ->
                    event.verifyTrackpadEvent(T, Press, true, Offset.Zero)
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                events[2].let { event ->
                    event.verifyTrackpadEvent(T * 2, Move, true, Offset(0f, 10f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(-10f)
                }
                events[3].let { event ->
                    event.verifyTrackpadEvent(T * 3, Release, false, Offset(0f, 10f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                // TODO: b/461873914
                //       since we didn't see the exit before, the enter gets overwritten to be a
                //       move
                events[4].verifyTrackpadEvent(T * 3, Move, false, Offset.Zero)
            }
        }
    }

    @Test
    fun scrollHorizontally() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, width = 300f, height = 300f, tag = TAG)
            }
        }

        rule.onNodeWithTag(TAG).performTrackpadInput {
            enter()
            // scroll horizontally
            scroll(Offset(10f, 0f))
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                assertThat(events.size).isEqualTo(5)
                events[0].verifyTrackpadEvent(T, Enter, false, Offset.Zero)
                // TODO: b/461873914
                //       the system sends an exit here, but we don't see it in Compose currently
                events[1].let { event ->
                    event.verifyTrackpadEvent(T, Press, true, Offset.Zero)
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                events[2].let { event ->
                    event.verifyTrackpadEvent(T * 2, Move, true, Offset(10f, 0f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(-10f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                events[3].let { event ->
                    event.verifyTrackpadEvent(T * 3, Release, false, Offset(10f, 0f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                // TODO: b/461873914
                //       since we didn't see the exit before, the enter gets overwritten to be a
                //       move
                events[4].verifyTrackpadEvent(T * 3, Move, false, Offset.Zero)
            }
        }
    }

    @Test
    fun scrollWithPrimaryDownReleasesButton() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, width = 300f, height = 300f, tag = TAG)
            }
        }

        rule.onNodeWithTag(TAG).performTrackpadInput {
            enter()
            // press primary button
            press(MouseButton.Primary)
            // scroll
            scroll(Offset(10f, 0f))
        }

        rule.runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()

                assertThat(events.size).isEqualTo(7)
                events[0].verifyTrackpadEvent(T, Enter, false, Offset.Zero)
                events[1].verifyTrackpadEvent(T, Press, true, Offset.Zero, PrimaryButton)
                events[2].verifyTrackpadEvent(T, Release, false, Offset.Zero)
                // TODO: b/461873914
                //       the system sends an exit here, but we don't see it in Compose currently
                events[3].let { event ->
                    event.verifyTrackpadEvent(T, Press, true, Offset.Zero)
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                events[4].let { event ->
                    event.verifyTrackpadEvent(T * 2, Move, true, Offset(10f, 0f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(-10f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                events[5].let { event ->
                    event.verifyTrackpadEvent(T * 3, Release, false, Offset(10f, 0f))
                    assertThat(event.classification)
                        .isEqualTo(
                            if (Build.VERSION.SDK_INT >= 34)
                                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                            else MotionEvent.CLASSIFICATION_NONE
                        )
                    assertThat(event.axisGestureScrollXDistance).isEqualTo(0f)
                    assertThat(event.axisGestureScrollYDistance).isEqualTo(0f)
                }
                // TODO: b/461873914
                //       since we didn't see the exit before, the enter gets overwritten to be a
                //       move
                events[6].verifyTrackpadEvent(T * 3, Move, false, Offset.Zero)
            }
        }
    }
}
