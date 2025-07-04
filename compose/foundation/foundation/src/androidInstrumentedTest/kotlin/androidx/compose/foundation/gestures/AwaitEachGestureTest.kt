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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AwaitEachGestureTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "pointerInputTag"

    @Test
    fun awaitEachGestureInternalCancellation() {
        val inputLatch = CountDownLatch(1)
        rule.setContent {
            Box(
                Modifier.testTag(tag)
                    .pointerInput(Unit) {
                        try {
                            var count = 0
                            coroutineScope {
                                awaitEachGesture {
                                    when (count++) {
                                        0 -> Unit // continue
                                        1 -> throw CancellationException("internal exception")
                                        else -> {
                                            // detectGestures will loop infinitely with nothing in
                                            // the
                                            // middle so wait for cancellation
                                            cancel("really canceled")
                                        }
                                    }
                                }
                            }
                        } catch (cancellationException: CancellationException) {
                            assertWithMessage(
                                    "The internal exception shouldn't cancel detectGestures"
                                )
                                .that(cancellationException.message)
                                .isEqualTo("really canceled")
                        }
                        inputLatch.countDown()
                    }
                    .size(10.dp)
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(tag).performTouchInput { click(Offset.Zero) }
        assertThat(inputLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun awaitEachGestureLoops() {
        val events = mutableListOf<PointerEventType>()
        val tag = "input rect"
        rule.setContent {
            Box(
                Modifier.fillMaxSize().testTag(tag).pointerInput(Unit) {
                    awaitEachGesture {
                        val event = awaitPointerEvent()
                        events += event.type
                    }
                }
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset.Zero)
            moveBy(Offset(10f, 10f))
            up()
            down(Offset(3f, 3f))
            moveBy(Offset(10f, 10f))
            moveBy(Offset(1f, 1f))
            up()
        }
        assertThat(events).hasSize(2)
        assertThat(events).containsExactly(PointerEventType.Press, PointerEventType.Press)
    }

    @Test
    fun undelegateSuspendingPointerInputModifierNodeDuringEventStream() {
        var enabled by mutableStateOf(true)
        rule.setContent { Box(Modifier.fillMaxSize().then(ParentElement(enabled))) }

        rule.onRoot().performTouchInput { down(center) }

        enabled = false
        rule.waitForIdle()

        rule.onRoot().performTouchInput { up() }
    }

    private data class ParentElement(private val enabled: Boolean) :
        ModifierNodeElement<ParentNode>() {
        override fun create(): ParentNode = ParentNode(enabled)

        override fun update(node: ParentNode) {
            node.update(enabled)
        }
    }

    private class ParentNode(enabled: Boolean) : DelegatingNode() {
        private var child: SuspendingPointerInputModifierNode? = if (enabled) childNode() else null

        fun update(enabled: Boolean) {
            if (!enabled) {
                child?.onCancelPointerInput()
                child?.let { undelegate(it) }
                child = null
            } else {
                check(child == null)
                child = childNode()
            }
        }

        private fun childNode() = delegate(SuspendingPointerInputModifierNode {})
    }
}
