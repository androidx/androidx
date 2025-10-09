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
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
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
import androidx.compose.ui.test.junit4.createComposeRule
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

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class IndirectTouchEventTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val initialFocus = FocusRequester()
    private lateinit var rootView: View

    private val capturedTestIndirectTouchEventInformation =
        mutableListOf<CapturedTestIndirectTouchEvent>()
    private var indirectTouchCancellations = false

    // Used for tests only checking IndirectPointerInputChange values directly.
    val emptyMotionEvent: MotionEvent = MotionEvent.obtain(0, 0, ACTION_DOWN, 0f, 0f, 0)

    @Before
    fun before() {
        indirectTouchCancellations = false
        capturedTestIndirectTouchEventInformation.clear()
    }

    @Test
    fun convertMotionEventToIndirectTouchEvent_validMotionEventAndNoPrimaryAxis() {
        val offset = Offset(4f, 6f)

        val motionEvent =
            MotionEvent.obtain(
                SystemClock.uptimeMillis(), // downTime,
                SystemClock.uptimeMillis(), // eventTime,
                ACTION_DOWN,
                offset.x,
                offset.y,
                0, // metaState
            )
        motionEvent.source = SOURCE_TOUCH_NAVIGATION

        val indirectTouchEvent = IndirectTouchEvent(motionEvent = motionEvent)

        assertThat(indirectTouchEvent).isNotNull()
        assertThat(indirectTouchEvent.changes.first().position).isEqualTo(offset)
        assertThat(indirectTouchEvent.changes.first().uptimeMillis).isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(motionEvent.actionMasked))
        assertThat(indirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(indirectTouchEvent.changes.first().pressed).isEqualTo(true)
        // Since we aren't passing previous [MotionEvent]s to test function IndirectTouchEvent(),
        // previous fields will be the same as original.
        assertThat(indirectTouchEvent.changes.first().previousPosition).isEqualTo(offset)
        assertThat(indirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.changes.first().previousPressed).isEqualTo(false)
        assertThat(indirectTouchEvent.nativeEvent).isEqualTo(motionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(indirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)
    }

    @Test
    fun convertStreamOfMotionEventsToIndirectTouchEvents_validMotionEventAndIncludePreviousMotionEventHistory() {
        // --> Down event (no previous history since it is the start of the stream).
        val downOffset = Offset(4f, 6f)
        val startOfEventStreamMillis = SystemClock.uptimeMillis()
        var uptimeMillis = startOfEventStreamMillis

        val downMotionEvent =
            MotionEvent.obtain(
                /* downTime = */ startOfEventStreamMillis,
                /* eventTime = */ uptimeMillis,
                /* action = */ ACTION_DOWN,
                /* x = */ downOffset.x,
                /* y = */ downOffset.y,
                /* metaState = */ 0,
            )
        downMotionEvent.source = SOURCE_TOUCH_NAVIGATION

        val downIndirectTouchEvent = IndirectTouchEvent(motionEvent = downMotionEvent)

        assertThat(downIndirectTouchEvent).isNotNull()
        assertThat(downIndirectTouchEvent.changes.first().position).isEqualTo(downOffset)
        assertThat(downIndirectTouchEvent.changes.first().uptimeMillis)
            .isEqualTo(downMotionEvent.eventTime)
        assertThat(downIndirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(downMotionEvent.actionMasked))
        assertThat(downIndirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(downIndirectTouchEvent.changes.first().pressed).isEqualTo(true)
        // For a first event in a stream, previous fields are going to equal current (since there is
        // no previous).
        assertThat(downIndirectTouchEvent.changes.first().previousPosition).isEqualTo(downOffset)
        assertThat(downIndirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(downMotionEvent.eventTime)
        assertThat(downIndirectTouchEvent.changes.first().previousPressed).isEqualTo(false)
        assertThat(downIndirectTouchEvent.nativeEvent).isEqualTo(downMotionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(downIndirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)

        // --> Move 1 event
        val move1Offset = Offset(downOffset.x + 5f, downOffset.y + 5f)
        uptimeMillis += 100

        val move1MotionEvent =
            MotionEvent.obtain(
                /* downTime = */ startOfEventStreamMillis,
                /* eventTime = */ uptimeMillis,
                /* action = */ ACTION_MOVE,
                /* x = */ move1Offset.x,
                /* y = */ move1Offset.y,
                /* metaState = */ 0,
            )
        move1MotionEvent.source = SOURCE_TOUCH_NAVIGATION

        val move1IndirectTouchEvent =
            IndirectTouchEvent(
                motionEvent = move1MotionEvent,
                // Normally, the Android Compose system caches information from previous
                // MotionEvents
                // to create the "previous" fields in IndirectPointerInputChange. Since we are using
                // the testing IndirectTouchEvent() (which doesn't do that), we need to pass the
                // previous MotionEvent if we want the proper "previous" values (uptimes, position,
                // pressed) to show up.
                previousMotionEvent = downMotionEvent,
            )

        assertThat(move1IndirectTouchEvent).isNotNull()
        assertThat(move1IndirectTouchEvent.changes.first().position).isEqualTo(move1Offset)
        assertThat(move1IndirectTouchEvent.changes.first().uptimeMillis)
            .isEqualTo(move1MotionEvent.eventTime)
        assertThat(move1IndirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(move1MotionEvent.actionMasked))
        assertThat(move1IndirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(move1IndirectTouchEvent.changes.first().pressed).isEqualTo(true)
        assertThat(move1IndirectTouchEvent.changes.first().previousPosition).isEqualTo(downOffset)
        assertThat(move1IndirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(downMotionEvent.eventTime)
        assertThat(move1IndirectTouchEvent.changes.first().previousPressed).isEqualTo(true)
        assertThat(move1IndirectTouchEvent.nativeEvent).isEqualTo(move1MotionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(move1IndirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)

        // --> Move 2 event2
        val move2Offset = Offset(move1Offset.x + 5f, move1Offset.y + 5f)
        uptimeMillis += 100

        val move2MotionEvent =
            MotionEvent.obtain(
                /* downTime = */ startOfEventStreamMillis,
                /* eventTime = */ uptimeMillis,
                /* action = */ ACTION_MOVE,
                /* x = */ move2Offset.x,
                /* y = */ move2Offset.y,
                /* metaState = */ 0,
            )
        move2MotionEvent.source = SOURCE_TOUCH_NAVIGATION

        val move2IndirectTouchEvent =
            IndirectTouchEvent(
                motionEvent = move2MotionEvent,
                // Normally, the Android Compose system caches information from previous
                // MotionEvents
                // to create the "previous" fields in IndirectPointerInputChange. Since we are using
                // the testing IndirectTouchEvent() (which doesn't do that), we need to pass the
                // previous MotionEvent if we want the proper "previous" values (uptimes, position,
                // pressed) to show up.
                previousMotionEvent = move1MotionEvent,
            )

        assertThat(move2IndirectTouchEvent).isNotNull()
        assertThat(move2IndirectTouchEvent.changes.first().position).isEqualTo(move2Offset)
        assertThat(move2IndirectTouchEvent.changes.first().uptimeMillis)
            .isEqualTo(move2MotionEvent.eventTime)
        assertThat(move2IndirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(move2MotionEvent.actionMasked))
        assertThat(move2IndirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(move2IndirectTouchEvent.changes.first().pressed).isEqualTo(true)
        assertThat(move2IndirectTouchEvent.changes.first().previousPosition).isEqualTo(move1Offset)
        assertThat(move2IndirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(move1MotionEvent.eventTime)
        assertThat(move2IndirectTouchEvent.changes.first().previousPressed).isEqualTo(true)
        assertThat(move2IndirectTouchEvent.nativeEvent).isEqualTo(move2MotionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(move2IndirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)

        // Up event
        val upOffset = Offset(move2Offset.x + 5f, move2Offset.y + 5f)
        uptimeMillis += 100

        val upMotionEvent =
            MotionEvent.obtain(
                /* downTime = */ startOfEventStreamMillis,
                /* eventTime = */ uptimeMillis,
                /* action = */ ACTION_UP,
                /* x = */ upOffset.x,
                /* y = */ upOffset.y,
                /* metaState = */ 0,
            )
        upMotionEvent.source = SOURCE_TOUCH_NAVIGATION

        val upIndirectTouchEvent =
            IndirectTouchEvent(
                motionEvent = upMotionEvent,
                // Normally, the Android Compose system caches information from previous
                // MotionEvents
                // to create the "previous" fields in IndirectPointerInputChange. Since we are using
                // the testing IndirectTouchEvent() (which doesn't do that), we need to pass the
                // previous MotionEvent if we want the proper "previous" values (uptimes, position,
                // pressed) to show up.
                previousMotionEvent = move2MotionEvent,
            )

        assertThat(upIndirectTouchEvent).isNotNull()
        assertThat(upIndirectTouchEvent.changes.first().position).isEqualTo(upOffset)
        assertThat(upIndirectTouchEvent.changes.first().uptimeMillis)
            .isEqualTo(upMotionEvent.eventTime)
        assertThat(upIndirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(upMotionEvent.actionMasked))
        assertThat(upIndirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(upIndirectTouchEvent.changes.first().pressed).isEqualTo(false)
        assertThat(upIndirectTouchEvent.changes.first().previousPosition).isEqualTo(move2Offset)
        assertThat(upIndirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(move2MotionEvent.eventTime)
        assertThat(upIndirectTouchEvent.changes.first().previousPressed).isEqualTo(true)
        assertThat(upIndirectTouchEvent.nativeEvent).isEqualTo(upMotionEvent)
        // Default is None when a device does not specify something different. In this case,
        // because there is no device mock, it will be none.
        assertThat(upIndirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.None)
    }

    @Test
    fun convertMotionEventToIndirectTouchEvent_validMotionEventAndPrimaryAxis() {
        val offset = Offset(4f, 6f)

        val motionEvent =
            MotionEvent.obtain(
                SystemClock.uptimeMillis(), // downTime,
                SystemClock.uptimeMillis(), // eventTime,
                ACTION_DOWN,
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
        assertThat(indirectTouchEvent.changes.first().position).isEqualTo(offset)
        assertThat(indirectTouchEvent.changes.first().uptimeMillis).isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.type)
            .isEqualTo(convertActionToIndirectTouchEventType(motionEvent.actionMasked))
        assertThat(indirectTouchEvent.changes.first().isConsumed).isEqualTo(false)
        assertThat(indirectTouchEvent.changes.first().pressed).isEqualTo(true)
        // Since we aren't passing previous [MotionEvent]s to test function IndirectTouchEvent(),
        // previous fields will be the same as original.
        assertThat(indirectTouchEvent.changes.first().previousPosition.x).isEqualTo(offset.x)
        assertThat(indirectTouchEvent.changes.first().previousPosition.y).isEqualTo(offset.y)
        assertThat(indirectTouchEvent.changes.first().previousUptimeMillis)
            .isEqualTo(motionEvent.eventTime)
        assertThat(indirectTouchEvent.changes.first().previousPressed).isEqualTo(false)
        assertThat(indirectTouchEvent.nativeEvent).isEqualTo(motionEvent)
        assertThat(indirectTouchEvent.primaryDirectionalMotionAxis)
            .isEqualTo(IndirectTouchEventPrimaryDirectionalMotionAxis.X)
    }

    @Test
    fun androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(capturedTestIndirectTouchEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedTestIndirectTouchEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedTestIndirectTouchEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)
        }
    }

    @Test
    fun androidTouchNavigationEventWithParent_triggersIndirectTouchEventsInOrderAndInParentAndChild() {
        val capturedParentIndirectTouchEventInformation =
            mutableStateListOf<CapturedTestIndirectTouchEvent>()
        var indirectParentTouchCancellations = false

        var timeStamp = 0L

        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedParentIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = timeStamp,
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
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
                        Modifier.onIndirectTouchInput(
                                onEvent = {
                                    indirectTouchEvent: IndirectTouchEvent,
                                    pointerEventPass: PointerEventPass ->
                                    capturedTestIndirectTouchEventInformation.add(
                                        CapturedTestIndirectTouchEvent(
                                            timestamp = timeStamp,
                                            pass = pointerEventPass,
                                            event = indirectTouchEvent,
                                        )
                                    )
                                    timeStamp += 10
                                },
                                onCancel = { indirectTouchCancellations = true },
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
            assertThat(capturedParentIndirectTouchEventInformation).hasSize(3)
            assertThat(capturedParentIndirectTouchEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedParentIndirectTouchEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedParentIndirectTouchEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)

            // Child
            assertThat(indirectTouchCancellations).isFalse()
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(capturedTestIndirectTouchEventInformation[0].pass)
                .isEqualTo(PointerEventPass.Initial)
            assertThat(capturedTestIndirectTouchEventInformation[1].pass)
                .isEqualTo(PointerEventPass.Main)
            assertThat(capturedTestIndirectTouchEventInformation[2].pass)
                .isEqualTo(PointerEventPass.Final)

            // Check ordering is correct
            // Initial pass / tunnel pass (parent should happen first)
            val parentInitialEventTimestamp =
                capturedParentIndirectTouchEventInformation[0].timestamp
            val childInitialEventTimestamp = capturedTestIndirectTouchEventInformation[0].timestamp
            assertThat(parentInitialEventTimestamp).isLessThan(childInitialEventTimestamp)

            // Main pass / bubble pass (child should happen first)
            val parentMainEventTimestamp = capturedParentIndirectTouchEventInformation[1].timestamp
            val childMainEventTimestamp = capturedTestIndirectTouchEventInformation[1].timestamp
            assertThat(childMainEventTimestamp).isLessThan(parentMainEventTimestamp)

            // Final pass / tunnel pass (parent should happen first)
            val parentFinalEventTimestamp = capturedParentIndirectTouchEventInformation[2].timestamp
            val childFinalEventTimestamp = capturedTestIndirectTouchEventInformation[2].timestamp
            assertThat(parentFinalEventTimestamp).isLessThan(childFinalEventTimestamp)
        }
    }

    @Test
    fun androidTouchNavigationEvent_withBadData_doesNotTriggerIndirectTouchEvent() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).isEmpty()
        }
    }

    @Test
    fun delegated_androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        var receivedEvent: IndirectTouchEvent? = null
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : Modifier.Node(), IndirectTouchInputModifierNode {
                            override fun onIndirectTouchEvent(
                                event: IndirectTouchEvent,
                                pass: PointerEventPass,
                            ) {
                                receivedEvent = event
                            }

                            override fun onCancelIndirectTouchInput() {
                                indirectTouchCancellations = true
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
            assertThat(indirectTouchCancellations).isFalse()
        }
    }

    /* Tests how delegates associated with a node will behave with indirect touch events.
     * For multiple delegates, only the first node will receive the event for indirect touch events
     * because it uses a focused item (not hit testing like pointer input). Pointer input will send
     * the event to BOTH delegates; see [AndroidPointerInputTest]'s test
     * delegated_multiple_androidPointerInputEvents_triggersTouchEvent() to see how pointer input
     * behaves.
     */
    @Test
    fun delegated_multiple_androidTouchNavigationEvent_triggersIndirectTouchEvent() {
        var event1: IndirectTouchEvent? = null
        var event2: IndirectTouchEvent? = null
        val node =
            object : DelegatingNode() {
                val unused =
                    delegate(
                        object : IndirectTouchInputModifierNode, Modifier.Node() {
                            override fun onIndirectTouchEvent(
                                event: IndirectTouchEvent,
                                pass: PointerEventPass,
                            ) {
                                if (pass == PointerEventPass.Main) {
                                    event1 = event
                                }
                            }

                            override fun onCancelIndirectTouchInput() {
                                indirectTouchCancellations = true
                            }
                        }
                    )
                val unused2 =
                    delegate(
                        object : IndirectTouchInputModifierNode, Modifier.Node() {
                            override fun onIndirectTouchEvent(
                                event: IndirectTouchEvent,
                                pass: PointerEventPass,
                            ) {
                                if (pass == PointerEventPass.Main) {
                                    event2 = event
                                }
                            }

                            override fun onCancelIndirectTouchInput() {
                                indirectTouchCancellations = true
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
            // Only the first delegate of the node will receive the event for indirect touch event
            // because it uses the focused item (not hit testing like pointer input (see test below
            // for how pointer input behaves).
            assertThat(event1).isNotNull()
            assertThat(event2).isNull()
            assertThat(indirectTouchCancellations).isFalse()
        }
    }

    @Test
    fun indirectTouchEventContainsPosition() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.changes.first().position == Offset(10f, 10f)
                    }
                )
                .isTrue()
            // Because the Device (containing the motion range) can't be set from the [MotionEvent],
            // the default values for the motion ranges are null, so the scroll axis is unspecified.
            // If you want to see tests of the scroll ranges (for primary axis), view the mocked
            // tests in [IndirectTouchEventWithInputDeviceMockTest].
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.primaryDirectionalMotionAxis ==
                            IndirectTouchEventPrimaryDirectionalMotionAxis.None
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectTouchEventContainsEventTime() {
        val uptimeMs = 123L
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.changes.first().uptimeMillis == uptimeMs
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectTouchEvent_actionDown_hasIndirectTouchEventTypePress() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.type == IndirectTouchEventType.Press
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectTouchEvent_actionUp_hasIndirectTouchEventTypeRelease() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.type == IndirectTouchEventType.Release
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectTouchEvent_actionMove_hasIndirectTouchEventTypeMove() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.type == IndirectTouchEventType.Move
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun indirectTouchEvent_actionUnknown_hasIndirectTouchEventTypeUnknown() {
        ContentWithInitialFocus {
            Box(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.onIndirectTouchInput(
                            onEvent = {
                                indirectTouchEvent: IndirectTouchEvent,
                                pointerEventPass: PointerEventPass ->
                                capturedTestIndirectTouchEventInformation.add(
                                    CapturedTestIndirectTouchEvent(
                                        timestamp = SystemClock.uptimeMillis(),
                                        pass = pointerEventPass,
                                        event = indirectTouchEvent,
                                    )
                                )
                            },
                            onCancel = { indirectTouchCancellations = true },
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
            assertThat(indirectTouchCancellations).isEqualTo(false)
            assertThat(capturedTestIndirectTouchEventInformation).hasSize(3)
            assertThat(
                    capturedTestIndirectTouchEventInformation.all {
                        it.event.type == IndirectTouchEventType.Unknown
                    }
                )
                .isTrue()
        }
    }

    // Tests for setting IndirectPointerInputChange directly in AndroidIndirectTouchEvent.
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
            AndroidIndirectTouchEvent(
                listOf(change),
                IndirectTouchEventType.Press,
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
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
            AndroidIndirectTouchEvent(
                listOf(change1, change2),
                IndirectTouchEventType.Press,
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
                emptyMotionEvent,
            )
        assertThat(event.changes).containsExactly(change1, change2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_emptyChanges_throwsIllegalArgumentException() {
        AndroidIndirectTouchEvent(
            emptyList(),
            IndirectTouchEventType.Press,
            IndirectTouchEventPrimaryDirectionalMotionAxis.X,
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

    private data class CapturedTestIndirectTouchEvent(
        val timestamp: Long,
        val pass: PointerEventPass,
        val event: IndirectTouchEvent,
    )
}
