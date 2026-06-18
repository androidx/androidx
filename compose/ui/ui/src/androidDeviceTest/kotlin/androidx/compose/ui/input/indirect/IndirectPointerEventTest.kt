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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class IndirectPointerEventTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val initialFocus = FocusRequester()
    private lateinit var rootView: View

    private val capturedTestIndirectPointerEventInformation =
        mutableListOf<CapturedTestIndirectPointerEvent>()
    private var indirectPointerCancellations = false

    // Used for tests only checking IndirectPointerInputChange values directly.
    val emptyMotionEvent: MotionEvent = MotionEvent.obtain(0, 0, ACTION_DOWN, 0f, 0f, 0)

    @Before
    fun before() {
        indirectPointerCancellations = false
        capturedTestIndirectPointerEventInformation.clear()
    }

    @Test
    fun androidTouchNavigationEvent_triggersIndirectPointerEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle {
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(capturedTestIndirectPointerEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedTestIndirectPointerEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedTestIndirectPointerEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)
        }
    }

    @Test
    fun androidTouchNavigationEventWithParent_triggersIndirectPointerEventsInOrderAndInParentAndChild() {
        val capturedParentIndirectPointerEventInformation =
            mutableStateListOf<CapturedTestIndirectPointerEvent>()
        var indirectParentTouchCancellations = false

        var timeStamp = 0L

        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedParentIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = timeStamp,
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                                timeStamp += 10
                            },
                            onCancel = { indirectParentTouchCancellations = true },
                        )
                        .focusTarget()
            ) {
                Box(
                    modifier =
                        @Suppress("DEPRECATION")
                        Modifier.onIndirectPointerInput(
                                onEvent = {
                                    indirectPointerEvent: IndirectPointerEvent,
                                    pointerEventPass: PointerEventPass ->
                                    capturedTestIndirectPointerEventInformation.add(
                                        CapturedTestIndirectPointerEvent(
                                            timestamp = timeStamp,
                                            pass = pointerEventPass,
                                            event = indirectPointerEvent,
                                        )
                                    )
                                    timeStamp += 10
                                },
                                onCancel = { indirectPointerCancellations = true },
                            )
                            .focusable(focusRequester = initialFocus, initiallyFocused = true)
                )
            }
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle {
            // Parent
            assertThat(indirectParentTouchCancellations).isFalse()
            assertThat(capturedParentIndirectPointerEventInformation).hasSize(3)
            assertThat(capturedParentIndirectPointerEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedParentIndirectPointerEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedParentIndirectPointerEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)

            // Child
            assertThat(indirectPointerCancellations).isFalse()
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(capturedTestIndirectPointerEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedTestIndirectPointerEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedTestIndirectPointerEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)

            // Check ordering is correct
            // Initial pass / tunnel pass (parent should happen first)
            val parentInitialEventTimestamp =
                capturedParentIndirectPointerEventInformation[0].timestamp
            val childInitialEventTimestamp =
                capturedTestIndirectPointerEventInformation[0].timestamp
            assertThat(parentInitialEventTimestamp).isLessThan(childInitialEventTimestamp)

            // Main pass / bubble pass (child should happen first)
            val parentMainEventTimestamp =
                capturedParentIndirectPointerEventInformation[1].timestamp
            val childMainEventTimestamp = capturedTestIndirectPointerEventInformation[1].timestamp
            assertThat(childMainEventTimestamp).isLessThan(parentMainEventTimestamp)

            // Final pass / tunnel pass (parent should happen first)
            val parentFinalEventTimestamp =
                capturedParentIndirectPointerEventInformation[2].timestamp
            val childFinalEventTimestamp = capturedTestIndirectPointerEventInformation[2].timestamp
            assertThat(parentFinalEventTimestamp).isLessThan(childFinalEventTimestamp)
        }
    }

    @Test
    fun androidTouchNavigationEvent_withBadData_doesNotTriggerIndirectPointerEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).isEmpty()
        }
    }

    @Test
    fun delegated_androidTouchNavigationEvent_triggersIndirectPointerEvent() {
        var receivedEvent: IndirectPointerEvent? = null
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : Modifier.Node(), IndirectPointerInputModifierNode {
                            override fun onIndirectPointerEvent(
                                event: IndirectPointerEvent,
                                pass: PointerEventPass,
                            ) {
                                receivedEvent = event
                            }

                            override fun onCancelIndirectPointerInput() {
                                indirectPointerCancellations = true
                            }
                        }
                    )
            }
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.elementFor(node)
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle {
            assertThat(receivedEvent).isNotNull()
            assertThat(indirectPointerCancellations).isFalse()
        }
    }

    /* Tests how delegates associated with a node will behave with indirect pointer events.
     * For multiple delegates, only the first node will receive the event for indirect pointer
     * events because it uses a focused item (not hit testing like pointer input). Pointer input
     * will send the event to BOTH delegates; see [AndroidPointerInputTest]'s test
     * delegated_multiple_androidPointerInputEvents_triggersTouchEvent() to see how pointer input
     * behaves.
     */
    @Test
    fun delegated_multiple_androidTouchNavigationEvent_triggersIndirectPointerEvent() {
        var event1: IndirectPointerEvent? = null
        var event2: IndirectPointerEvent? = null
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : IndirectPointerInputModifierNode, Modifier.Node() {
                            override fun onIndirectPointerEvent(
                                event: IndirectPointerEvent,
                                pass: PointerEventPass,
                            ) {
                                if (pass == PointerEventPass.Main) {
                                    event1 = event
                                }
                            }

                            override fun onCancelIndirectPointerInput() {
                                indirectPointerCancellations = true
                            }
                        }
                    )
                val unused2 =
                    delegate(
                        object : IndirectPointerInputModifierNode, Modifier.Node() {
                            override fun onIndirectPointerEvent(
                                event: IndirectPointerEvent,
                                pass: PointerEventPass,
                            ) {
                                if (pass == PointerEventPass.Main) {
                                    event2 = event
                                }
                            }

                            override fun onCancelIndirectPointerInput() {
                                indirectPointerCancellations = true
                            }
                        }
                    )
            }
        ContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.elementFor(node)
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
            )
        }

        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder().setSource(SOURCE_TOUCH_NAVIGATION).build()
            )
        }

        rule.runOnIdle {
            // Only the first delegate of the node will receive the event for indirect pointer event
            // because it uses the focused item (not hit testing like pointer input (see test below
            // for how pointer input behaves).
            assertThat(event1).isNotNull()
            assertThat(event2).isNull()
            assertThat(indirectPointerCancellations).isFalse()
        }
    }

    @Test
    fun indirectPointerEventContainsPosition() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.changes.first().position == Offset(10f, 10f)
                    }
                )
                .isTrue()
            // Because the Device (containing the motion range) can't be set from the [MotionEvent],
            // the default values for the motion ranges are null, so the scroll axis is unspecified.
            // If you want to see tests of the scroll ranges (for primary axis), view the mocked
            // tests in [IndirectPointerEventWithInputDeviceMockTest].
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.primaryDirectionalMotionAxis ==
                            IndirectPointerEventPrimaryDirectionalMotionAxis.None
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectPointerEventContainsEventTime() {
        val uptimeMs = 123L
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.changes.first().uptimeMillis == uptimeMs
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectPointerEvent_actionDown_hasIndirectPointerEventTypePress() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.type == IndirectPointerEventType.Press
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectPointerEvent_actionUp_hasIndirectPointerEventTypeRelease() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.type == IndirectPointerEventType.Release
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectPointerEvent_actionMove_hasIndirectPointerEventTypeMove() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.type == IndirectPointerEventType.Move
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectPointerEvent_actionUnknown_hasIndirectPointerEventTypeUnknown() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectPointerEventInformation.add(
                                    CapturedTestIndirectPointerEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectPointerEvent,
                                    )
                                )
                            },
                            onCancel = { indirectPointerCancellations = true },
                        )
                        .focusable(focusRequester = initialFocus, initiallyFocused = true)
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

        rule.runOnIdle {
            assertThat(indirectPointerCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectPointerEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectPointerEventInformation.all {
                        it.event.type == IndirectPointerEventType.Unknown
                    }
                )
                .isTrue()
        }
    }

    // Tests for setting IndirectPointerInputChange directly in AndroidIndirectPointerEvent.
    @Test
    fun constructor_singleChange_propertiesAreCorrect() {
        val uptimeMillis = 100L
        val change =
            IndirectPointerInputChange(
                id = PointerId(0),
                uptimeMillis = uptimeMillis,
                position = Offset(1f, 2f),
                pressed = true,
                pressure = 1f,
                previousUptimeMillis = 0L,
                previousPosition = Offset(1f, 2f),
                previousPressed = false,
            )
        val event =
            AndroidIndirectPointerEvent(
                listOf(change),
                IndirectPointerEventType.Press,
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                emptyMotionEvent,
            )
        assertThat(event.changes).containsExactly(change)
    }

    @Test
    fun constructor_multipleChanges_propertiesAreCorrect() {
        val uptimeMillis1 = 200L
        val change1 =
            IndirectPointerInputChange(
                id = PointerId(0),
                uptimeMillis = uptimeMillis1,
                position = Offset(1f, 2f),
                pressed = true,
                pressure = 1f,
                previousUptimeMillis = 0L,
                previousPosition = Offset(1f, 2f),
                previousPressed = false,
            )
        val uptimeMillis2 = 300L
        val change2 =
            IndirectPointerInputChange(
                id = PointerId(1),
                uptimeMillis = uptimeMillis2,
                position = Offset(3f, 4f),
                pressed = true,
                pressure = 1f,
                previousUptimeMillis = 100L,
                previousPosition = Offset(3f, 4f),
                previousPressed = false,
            )
        val event =
            AndroidIndirectPointerEvent(
                listOf(change1, change2),
                IndirectPointerEventType.Press,
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                emptyMotionEvent,
            )
        assertThat(event.changes).containsExactly(change1, change2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_emptyChanges_throwsIllegalArgumentException() {
        AndroidIndirectPointerEvent(
            emptyList(),
            IndirectPointerEventType.Press,
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            emptyMotionEvent,
        )
    }

    private fun ContentWithInitialFocus(content: @Composable () -> Unit) {
        rule.setContent {
            rootView = LocalView.current
            Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }

    private fun Modifier.focusable(
        focusRequester: FocusRequester,
        initiallyFocused: Boolean = false,
    ) =
        this.then(if (initiallyFocused) Modifier.focusRequester(focusRequester) else Modifier)
            .focusTarget()

    private data class CapturedTestIndirectPointerEvent(
        val timestamp: Long,
        val pass: PointerEventPass,
        val event: IndirectPointerEvent,
    )
}
