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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

@MediumTest
class StylusEventTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())
    private val tag = "Stylus Event Test Tag"

    /**
     * When a relayout triggers during an active hover stream, the system will resend the hover
     * event AFTER the relayout is finished (in case any of the UI elements have moved their
     * location and the local coordinates of the pointer event need to be changed). If a hover exit
     * occurs from a non-mouse between the relayout starting and the execution of resending the
     * event, the resend should be cancelled (since there is no longer an active hover stream).
     */
    @Test
    fun relayoutDuringActiveHoverNonMouse_hoverExitBeforeRetriggerOfHoverMove_doesNotSendMove() {
        var eventContainsNonHoverMoveMotionEventAction = false

        val events = mutableListOf<PointerEventType>()
        // Size change triggers Relayout
        var layoutSize by mutableStateOf(100.dp)

        // Used to manually trigger a generic MotionEvent
        lateinit var view: View
        // Used to set up a Layout Completed Listener
        lateinit var owner: Owner

        rule.setContent {
            view = LocalView.current
            owner = view as Owner

            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.size(layoutSize).testTag(tag).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.motionEvent?.actionMasked == MotionEvent.ACTION_MOVE) {
                                    eventContainsNonHoverMoveMotionEventAction = true
                                }
                                events += event.type
                            }
                        }
                    }
                )
            }
        }

        // 1. Set up hover enter and move (so last event AndroidComposeView's previousMotionEvent is
        // a hover move).
        rule.onNodeWithTag(tag).performStylusInput {
            hoverEnter(Offset(0f, 0f))
            hoverMoveTo(Offset(5f, 5f))
        }

        rule.runOnIdle {
            // Callback triggered right after layout is complete but before the POST of
            // AndroidComposeView's resendMotionEventRunnable can execute.
            owner.registerOnLayoutCompletedListener(
                object : Owner.OnLayoutCompletedListener {
                    override fun onLayoutComplete() {
                        // 3. Trigger hover exit (and the AndroidComposeView's sendHoverExitEvent
                        // runnable is
                        // set to post with delay ) before resendMotionEventRunnable runs.
                        val time = System.currentTimeMillis()

                        val motionEvent =
                            MotionEvent.obtain(
                                /* downTime = */ time,
                                /* eventTime = */ time,
                                /* action = */ MotionEvent.ACTION_HOVER_EXIT,
                                /* pointerCount = */ 1,
                                /* pointerProperties = */ arrayOf(
                                    MotionEvent.PointerProperties().apply {
                                        id = 0
                                        toolType = MotionEvent.TOOL_TYPE_STYLUS
                                    }
                                ),
                                /* pointerCoords = */ arrayOf(
                                    MotionEvent.PointerCoords().apply {
                                        val startOffset = Offset(10f, 10f)

                                        // Allows for non-valid numbers/Offsets to be passed along
                                        // to Compose to
                                        // test if it handles them properly (versus breaking here
                                        // and we not knowing
                                        // if Compose properly handles these values).
                                        x =
                                            if (startOffset.isValid()) {
                                                startOffset.x
                                            } else {
                                                Float.NaN
                                            }

                                        y =
                                            if (startOffset.isValid()) {
                                                startOffset.y
                                            } else {
                                                Float.NaN
                                            }
                                    }
                                ),
                                /* metaState = */ 0,
                                /* buttonState = */ 0,
                                /* xPrecision = */ 1f,
                                /* yPrecision = */ 1f,
                                /* deviceId = */ 0,
                                /* edgeFlags = */ 0,
                                /* source = */ InputDeviceCompat.SOURCE_STYLUS,
                                /* flags = */ 0,
                            )

                        view.dispatchGenericMotionEvent(motionEvent)
                        motionEvent.recycle()
                    }
                }
            )

            // 2. Trigger relayout via size change which POSTs AndroidComposeView's
            // resendMotionEventRunnable (but doesn't execute before the onLayoutComplete() above).
            layoutSize = 98.dp
        }

        // 4. resendMotionEventRunnable runs
        // 5. sendHoverExitEvent runs
        waitForPointerUpdate(rule = rule, delayMillis = 1000)

        assertThat(eventContainsNonHoverMoveMotionEventAction).isFalse()
        assertThat(events.size).isEqualTo(3)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        assertThat(events[1]).isEqualTo(PointerEventType.Move)
        assertThat(events[2]).isEqualTo(PointerEventType.Exit)
    }
}
