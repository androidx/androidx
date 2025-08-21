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

package androidx.compose.ui.input.focus

import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_A
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.IndirectTouchInput
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.InterruptedSoftKeyboardInput
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.KeyInput
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.RotaryInput
import androidx.compose.ui.input.indirect.AndroidIndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectTouchEventType
import androidx.compose.ui.input.indirect.onIndirectTouchEvent
import androidx.compose.ui.input.indirect.onPreIndirectTouchEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.input.rotary.findRotaryInputDevice
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performIndirectTouchEvent
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Focus-aware event propagation test. */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(Parameterized::class)
class FocusAwareEventPropagationTest(private val nodeType: NodeType) {
    @get:Rule val rule = createComposeRule()

    private val sentEvent: Any =
        when (nodeType) {
            KeyInput,
            InterruptedSoftKeyboardInput -> KeyEvent(AndroidKeyEvent(ACTION_DOWN, KEYCODE_A))
            RotaryInput -> RotaryScrollEvent(1f, 1f, 0L, findRotaryInputDevice())
            IndirectTouchInput ->
                AndroidIndirectTouchEvent(
                    position = Offset.Zero,
                    uptimeMillis = 0L,
                    type = IndirectTouchEventType.Press,
                    primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
                    nativeEvent =
                        MotionEvent.obtain(
                            SystemClock.uptimeMillis(), // downTime,
                            SystemClock.uptimeMillis(), // eventTime,
                            MotionEvent.ACTION_DOWN,
                            Offset.Zero.x,
                            Offset.Zero.y,
                            0, // metaState
                        ),
                )
        }
    private var receivedEvent: Any? = null
    private val initialFocus = FocusRequester()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "node = {0}")
        fun initParameters() =
            arrayOf(KeyInput, InterruptedSoftKeyboardInput, RotaryInput, IndirectTouchInput)
    }

    @Before
    fun ignoreEventTime() {
        // This test just checks the propagation of events and doesn't care about the event time.
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun noFocusable_doesNotDeliverEvent() {
        // Arrange.
        rule.setContent {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(receivedEvent).isNull() }
    }

    @Test
    fun unfocusedFocusable_doesNotDeliverEvent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable()
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(receivedEvent).isNull() }
    }

    @Test
    fun onFocusAwareEvent_afterFocusable_isNotTriggered() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.focusable(initiallyFocused = true).onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        when (nodeType) {
            KeyInput -> assertThat(receivedEvent).isEqualTo(sentEvent)
            InterruptedSoftKeyboardInput,
            RotaryInput -> assertThat(receivedEvent).isNull()
            IndirectTouchInput -> assertThat(receivedEvent).isNull()
        }
    }

    @Test
    fun onPreFocusAwareEvent_afterFocusable_isNotTriggered() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.focusable(initiallyFocused = true).onPreFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        when (nodeType) {
            KeyInput -> assertThat(receivedEvent).isEqualTo(sentEvent)
            InterruptedSoftKeyboardInput,
            RotaryInput -> assertThat(receivedEvent).isNull()
            IndirectTouchInput -> assertThat(receivedEvent).isNull()
        }
    }

    @Test
    fun onFocusAwareEvent_isTriggered() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(sentEvent).isEqualTo(receivedEvent) }
    }

    @Test
    fun onPreFocusAwareEvent_triggered() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onPreFocusAwareEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(sentEvent).isEqualTo(receivedEvent) }
    }

    @Test
    fun onFocusAwareEventNotTriggered_ifOnPreFocusAwareEventConsumesEvent_1() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            receivedEvent = it
                            true
                        }
                        .onPreFocusAwareEvent { true }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(receivedEvent).isNull() }
    }

    @Test
    fun onFocusAwareEventNotTriggered_ifOnPreFocusAwareEventConsumesEvent_2() {
        // Arrange.
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onPreFocusAwareEvent { true }
                        .onFocusAwareEvent {
                            receivedEvent = it
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle { assertThat(receivedEvent).isNull() }
    }

    @Test
    fun onPreFocusAwareEvent_triggeredBefore_onFocusAwareEvent_1() {
        // Arrange.
        var triggerIndex = 1
        var onFocusAwareEventTrigger = 0
        var onPreFocusAwareEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            onFocusAwareEventTrigger = triggerIndex++
                            true
                        }
                        .onPreFocusAwareEvent {
                            onPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(onPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(onFocusAwareEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun onPreFocusAwareEvent_triggeredBefore_onFocusAwareEvent_2() {
        // Arrange.
        var triggerIndex = 1
        var onFocusAwareEventTrigger = 0
        var onPreFocusAwareEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onPreFocusAwareEvent {
                            onPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onFocusAwareEvent {
                            onFocusAwareEventTrigger = triggerIndex++
                            true
                        }
                        .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(onPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(onFocusAwareEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun parent_child() {
        // Arrange.
        var triggerIndex = 1
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            parentOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            parentOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable()
            ) {
                Box(
                    modifier =
                        Modifier.onFocusAwareEvent {
                                childOnFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .onPreFocusAwareEvent {
                                childOnPreFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .focusable(initiallyFocused = true)
                )
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(3)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun parent_child_noFocusModifierForParent() {
        // Arrange.
        var triggerIndex = 1
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            parentOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            parentOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
            ) {
                Box(
                    modifier =
                        Modifier.onFocusAwareEvent {
                                childOnFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .onPreFocusAwareEvent {
                                childOnPreFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .focusable(initiallyFocused = true)
                )
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(3)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun grandParent_parent_child() {
        // Arrange.
        var triggerIndex = 1
        var grandParentOnFocusAwareEventTrigger = 0
        var grandParentOnPreFocusAwareEventTrigger = 0
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                modifier =
                    Modifier.onFocusAwareEvent {
                            grandParentOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            grandParentOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable()
            ) {
                Box(
                    modifier =
                        Modifier.onFocusAwareEvent {
                                parentOnFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .onPreFocusAwareEvent {
                                parentOnPreFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .focusable()
                ) {
                    Box(
                        modifier =
                            Modifier.onFocusAwareEvent {
                                    childOnFocusAwareEventTrigger = triggerIndex++
                                    false
                                }
                                .onPreFocusAwareEvent {
                                    childOnPreFocusAwareEventTrigger = triggerIndex++
                                    false
                                }
                                .focusable(initiallyFocused = true)
                    )
                }
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(3)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(4)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(5)
            assertThat(grandParentOnFocusAwareEventTrigger).isEqualTo(6)
        }
    }

    private fun Modifier.focusable(initiallyFocused: Boolean = false) =
        this.then(if (initiallyFocused) Modifier.focusRequester(initialFocus) else Modifier)
            .focusTarget()

    private fun SemanticsNodeInteraction.performFocusAwareInput(sentEvent: Any) {
        when (nodeType) {
            KeyInput,
            InterruptedSoftKeyboardInput -> {
                check(sentEvent is KeyEvent)
                performKeyPress(sentEvent)
            }
            RotaryInput -> {
                check(sentEvent is RotaryScrollEvent)
                @OptIn(ExperimentalTestApi::class)
                performRotaryScrollInput {
                    rotateToScrollVertically(sentEvent.verticalScrollPixels)
                }
            }
            IndirectTouchInput -> {
                check(sentEvent is IndirectTouchEvent)
                performIndirectTouchEvent(sentEvent)
            }
        }
    }

    private fun ComposeContentTestRule.setContentWithInitialFocus(content: @Composable () -> Unit) {
        setFocusableContent(content = content)
        runOnIdle { initialFocus.requestFocus() }
    }

    private fun Modifier.onFocusAwareEvent(onEvent: (Any) -> Boolean): Modifier =
        when (nodeType) {
            KeyInput -> onKeyEvent(onEvent)
            InterruptedSoftKeyboardInput -> onInterceptKeyBeforeSoftKeyboard(onEvent)
            RotaryInput -> onRotaryScrollEvent(onEvent)
            IndirectTouchInput -> onIndirectTouchEvent(onEvent)
        }

    private fun Modifier.onPreFocusAwareEvent(onPreEvent: (Any) -> Boolean) =
        when (nodeType) {
            KeyInput -> onPreviewKeyEvent(onPreEvent)
            InterruptedSoftKeyboardInput -> onPreInterceptKeyBeforeSoftKeyboard(onPreEvent)
            RotaryInput -> onPreRotaryScrollEvent(onPreEvent)
            IndirectTouchInput -> onPreIndirectTouchEvent(onPreEvent)
        }

    enum class NodeType {
        KeyInput,
        InterruptedSoftKeyboardInput,
        RotaryInput,
        IndirectTouchInput,
    }
}
