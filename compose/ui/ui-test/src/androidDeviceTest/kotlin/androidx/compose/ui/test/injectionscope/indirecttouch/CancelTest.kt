/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.indirecttouch

import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.injectionscope.indirecttouch.Common.performIndirectPointerInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertNoIndirectPointerGestureInProgress
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests if [IndirectPointerInjectionScope.cancel] works */
@MediumTest
class CancelTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
        private val inputDeviceSize = IntSize(3082, 616)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = MultiPointerInputRecorder()
    private var isCancelled = false

    private val cancelInterceptor =
        object : ModifierNodeElement<CancelInterceptorNode>() {
            override fun create(): CancelInterceptorNode = CancelInterceptorNode {
                isCancelled = true
            }

            override fun update(node: CancelInterceptorNode) {
                node.onCancel = { isCancelled = true }
            }

            override fun equals(other: Any?): Boolean = other === this

            override fun hashCode(): Int = System.identityHashCode(this)
        }

    private class CancelInterceptorNode(var onCancel: () -> Unit) :
        Modifier.Node(), IndirectPointerInputModifierNode {
        override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
            // Do nothing
        }

        override fun onCancelIndirectPointerInput() {
            onCancel()
        }
    }

    @Before
    fun setUp() {
        // Given some content
        rule.setContent { ClickableTestBox(recorder.then(cancelInterceptor)) }
        rule.onNodeWithTag(ClickableTestBox.defaultTag).requestFocus()
    }

    @Test
    fun onePointer() {
        // When we inject a down event followed by a cancel event
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
            assertThat(isCancelled).isTrue()
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoIndirectPointerGestureInProgress()
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by a cancel event
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(2, downPosition2)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 2 down events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)
            }
            assertThat(isCancelled).isTrue()
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoIndirectPointerGestureInProgress()
    }

    @Test
    fun cancel_withoutDown() {
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                cancel()
            }
        }
    }

    @Test
    fun cancel_afterUp() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            up()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                cancel()
            }
        }
    }

    @Test
    fun cancel_afterCancel() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                cancel()
            }
        }
    }
}
