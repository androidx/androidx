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

package androidx.compose.ui.input.indirect

import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalIndirectTouchTypeApi
@MediumTest
@RunWith(AndroidJUnit4::class)
class IndirectTouchEventTest {
    @get:Rule val rule = createComposeRule()

    private val initialFocus = FocusRequester()
    private lateinit var rootView: View
    private var receivedEvent: IndirectTouchEvent? = null

    @Before
    fun before() {
        receivedEvent = null
    }

    @Test
    fun convertMotionEventToIndirectTouchEvent_validMotionEventAndNoPrimaryAxis() {

        val offset = Offset(4f, 6f)

        val motionEvent =
            MotionEvent.obtain(
                SystemClock.uptimeMillis(), // downTime,
                SystemClock.uptimeMillis(), // eventTime,
                MotionEvent.ACTION_DOWN,
                offset.x,
                offset.y,
                0, // metaState
            )
        motionEvent.source = SOURCE_TOUCH_NAVIGATION

        val indirectTouchEvent = IndirectTouchEvent(motionEvent = motionEvent)

        assertThat(indirectTouchEvent).isNotNull()
        assertThat(indirectTouchEvent.position).isEqualTo(offset)
        assertThat(indirectTouchEvent.uptimeMillis).isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(motionEvent.actionMasked))
        assertThat(indirectTouchEvent.nativeEvent).isEqualTo(motionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(indirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)
    }

    @Test
    fun convertMotionEventToIndirectTouchEvent_validMotionEventAndPrimaryAxis() {

        val offset = Offset(4f, 6f)

        val motionEvent =
            MotionEvent.obtain(
                SystemClock.uptimeMillis(), // downTime,
                SystemClock.uptimeMillis(), // eventTime,
                MotionEvent.ACTION_DOWN,
                offset.x,
                offset.y,
                0, // metaState
            )
        motionEvent.source = SOURCE_TOUCH_NAVIGATION

        val indirectTouchEvent =
            IndirectTouchEvent(
                motionEvent = motionEvent,
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

        assertThat(indirectTouchEvent).isNotNull()
        assertThat(indirectTouchEvent.position).isEqualTo(offset)
        assertThat(indirectTouchEvent.uptimeMillis).isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(motionEvent.actionMasked))
        assertThat(indirectTouchEvent.nativeEvent).isEqualTo(motionEvent)
        assertThat(indirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.X)
    }

    @Test
    fun androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent).isNotNull() }
    }

    @Test
    fun androidTouchNavigationEvent_withBadData_doesNotTriggerIndirectTouchEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .setPointer(Float.NaN, Float.NaN)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent).isNull() }
    }

    @Test
    fun delegated_androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : IndirectTouchInputModifierNode, Modifier.Node() {
                            override fun onIndirectTouchEvent(event: IndirectTouchEvent): Boolean {
                                receivedEvent = event
                                return true
                            }

                            override fun onPreIndirectTouchEvent(
                                event: IndirectTouchEvent
                            ): Boolean {
                                return false
                            }
                        }
                    )
            }
        ContentWithInitialFocus {
            Box(modifier = Modifier.elementFor(node).focusable(initiallyFocused = true))
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent).isNotNull() }
    }

    @Test
    fun delegated_multiple_androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        var event1: IndirectTouchEvent? = null
        var event2: IndirectTouchEvent? = null
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : IndirectTouchInputModifierNode, Modifier.Node() {
                            override fun onIndirectTouchEvent(event: IndirectTouchEvent): Boolean {
                                event1 = event
                                return false
                            }

                            override fun onPreIndirectTouchEvent(
                                event: IndirectTouchEvent
                            ): Boolean {
                                return false
                            }
                        }
                    )
                val unused2 =
                    delegate(
                        object : IndirectTouchInputModifierNode, Modifier.Node() {
                            override fun onIndirectTouchEvent(event: IndirectTouchEvent): Boolean {
                                event2 = event
                                return false
                            }

                            override fun onPreIndirectTouchEvent(
                                event: IndirectTouchEvent
                            ): Boolean {
                                return false
                            }
                        }
                    )
            }
        ContentWithInitialFocus {
            Box(modifier = Modifier.elementFor(node).focusable(initiallyFocused = true))
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle {
            assertThat(event1).isNotNull()
            assertThat(event2).isNotNull()
        }
    }

    @Test
    fun indirectTouchEventContainsPosition() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .setPointer(10f, 10f)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.position).isEqualTo(Offset(10f, 10f)) }
        rule.runOnIdle {
            // Because the Device (containing the motion range) can't be set from the [MotionEvent],
            // the default values for the motion ranges are null, so the scroll axis is unspecified.
            // If you want to see tests of the scroll ranges (for primary axis), view the mocked
            // tests in [IndirectTouchEventWithInputDeviceMockTest].
            assertThat(receivedEvent?.primaryDirectionalMotionAxis)
                .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)
        }
    }

    @Test
    fun indirectTouchEventContainsEventTime() {
        val uptimeMs = 123L
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .setEventTime(uptimeMs)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.uptimeMillis).isEqualTo(uptimeMs) }
    }

    @Test
    fun indirectTouchEvent_actionDown_hasIndirectTouchEventTypePress() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_DOWN)
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }
    }

    @Test
    fun indirectTouchEvent_actionUp_hasIndirectTouchEventTypeRelease() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_UP)
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }
    }

    @Test
    fun indirectTouchEvent_actionMove_hasIndirectTouchEventTypeMove() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_MOVE)
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }
    }

    @Test
    fun indirectTouchEvent_actionUnknown_hasIndirectTouchEventTypeUnknown() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onIndirectTouchEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_HOVER_ENTER)
                    .setSource(SOURCE_TOUCH_NAVIGATION)
                    .build()
            )
        }

        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Unknown) }
    }

    private fun ContentWithInitialFocus(content: @Composable () -> Unit) {
        rule.setContent {
            rootView = LocalView.current
            Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }

    private fun Modifier.focusable(initiallyFocused: Boolean = false) =
        this.then(if (initiallyFocused) Modifier.focusRequester(initialFocus) else Modifier)
            .focusTarget()
}
