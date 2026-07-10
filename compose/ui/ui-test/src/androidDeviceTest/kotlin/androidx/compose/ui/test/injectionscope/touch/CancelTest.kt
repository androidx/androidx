/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.injectionscope.touch.Common.performTouchInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertNoTouchGestureInProgress
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests if [TouchInjectionScope.cancel] works */
@MediumTest
class CancelTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
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
        Modifier.Node(), PointerInputModifierNode {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            // Do nothing
        }

        override fun onCancelPointerInput() {
            onCancel()
        }
    }

    @Before
    fun setUp() {
        // Given some content
        rule.setContent { ClickableTestBox(recorder.then(cancelInterceptor)) }
    }

    @Test
    fun onePointer() {
        // When we inject a down event followed by a cancel event
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
            assertThat(isCancelled).isTrue()
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by a cancel event
        rule.performTouchInput { down(1, downPosition1) }
        rule.performTouchInput { down(2, downPosition2) }
        rule.performTouchInput { cancel() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded just 2 down events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)
            }
            assertThat(isCancelled).isTrue()
        }

        // And no gesture is in progress
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertNoTouchGestureInProgress()
    }

    @Test
    fun cancel_withoutDown() {
        expectError<IllegalStateException> { rule.performTouchInput { cancel() } }
    }

    @Test
    fun cancel_afterUp() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { up() }
        expectError<IllegalStateException> { rule.performTouchInput { cancel() } }
    }

    @Test
    fun cancel_afterCancel() {
        rule.performTouchInput { down(downPosition1) }
        rule.performTouchInput { cancel() }
        expectError<IllegalStateException> { rule.performTouchInput { cancel() } }
    }
}
