/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.interactionBarrier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusChangedTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun active_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun activeParent_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        val (focusRequester, childFocusRequester) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            ) {
                Box(
                    modifier =
                        Modifier.onFocusChanged { childFocusState = it }
                            .focusRequester(childFocusRequester)
                            .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            childFocusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
            assertThat(childFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun captured_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState.isCaptured).isTrue()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isCaptured).isTrue() }
    }

    @Test
    fun deactivated_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusProperties { canFocus = false }
                        .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun deactivatedParent_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        val (focusRequester, childFocusRequester) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusProperties { canFocus = false }
                        .focusTarget()
            ) {
                Box(
                    modifier =
                        Modifier.onFocusChanged { childFocusState = it }
                            .focusRequester(childFocusRequester)
                            .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            childFocusRequester.requestFocus()
            assertThat(childFocusState.isFocused).isTrue()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(childFocusState.isFocused).isTrue()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun inactive_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun inactive_requestFocus_multipleObservers() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        lateinit var focusState4: FocusState
        lateinit var focusState5: FocusState
        lateinit var focusState6: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState1 = it }.onFocusChanged { focusState2 = it }
            ) {
                Box {
                    Box(
                        modifier =
                            Modifier.onFocusChanged { focusState3 = it }
                                .onFocusChanged { focusState4 = it }
                    ) {
                        Box(
                            modifier =
                                Modifier.onFocusChanged { focusState5 = it }
                                    .onFocusChanged { focusState6 = it }
                                    .focusRequester(focusRequester)
                                    .focusTarget()
                        )
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1.isFocused).isTrue()
            assertThat(focusState2.isFocused).isTrue()
            assertThat(focusState3.isFocused).isTrue()
            assertThat(focusState4.isFocused).isTrue()
            assertThat(focusState5.isFocused).isTrue()
            assertThat(focusState6.isFocused).isTrue()
        }
    }

    @Test
    fun active_requestFocus_multipleObserversWithExtraFocusTargetInBetween() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        lateinit var focusState4: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier =
                    Modifier.onFocusChanged { focusState1 = it }
                        .onFocusChanged { focusState2 = it }
                        .focusTarget()
                        .onFocusChanged { focusState3 = it }
                        .onFocusChanged { focusState4 = it }
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1.hasFocus).isTrue()
            assertThat(focusState2.hasFocus).isTrue()
            assertThat(focusState3.isFocused).isTrue()
            assertThat(focusState4.isFocused).isTrue()
        }
    }

    @Test
    fun focusSearch_barrierBlocksFocusToBackground() {
        // Arrange.
        val (button1, button2, button3) = FocusRequester.createRefs()
        var button2Focused = false
        var button3Focused = false
        lateinit var focusManager: FocusManager

        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(Modifier.size(200.dp)) {
                // Button 1 (starts focused)
                Box(Modifier.focusRequester(button1).focusTarget())

                // Button 2 (behind barrier, should be blocked)
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(button2)
                        .onFocusChanged { button2Focused = it.isFocused }
                        .focusTarget()
                )

                // Barrier (covers Button 2)
                Box(Modifier.size(100.dp).interactionBarrier())

                // Button 3 (outside barrier, should be focusable)
                Box(
                    Modifier.offset(150.dp, 0.dp)
                        .focusRequester(button3)
                        .onFocusChanged { button3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle { button1.requestFocus() }

        // Act: Move focus next
        val moved = rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        // Assert.
        rule.runOnIdle {
            assertThat(moved).isTrue()
            assertThat(button2Focused).isFalse() // Skipped!
            assertThat(button3Focused).isTrue() // Focused!
        }
    }

    @Test
    fun focusSearch_barrierDoesNotBlockFocusToChildren() {
        // Arrange.
        val (button1, button2) = FocusRequester.createRefs()
        var button2Focused = false
        lateinit var focusManager: FocusManager

        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(Modifier.size(200.dp)) {
                // Button 1 (starts focused)
                Box(Modifier.focusRequester(button1).focusTarget())

                // Barrier
                Box(Modifier.size(200.dp).interactionBarrier()) {
                    // Button 2 (inside barrier, should NOT be blocked)
                    Box(
                        Modifier.size(100.dp)
                            .focusRequester(button2)
                            .onFocusChanged { button2Focused = it.isFocused }
                            .focusTarget()
                    )
                }
            }
        }

        rule.runOnIdle { button1.requestFocus() }

        // Act: Move focus next
        val moved = rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        // Assert.
        rule.runOnIdle {
            assertThat(moved).isTrue()
            assertThat(button2Focused).isTrue() // Focused!
        }
    }

    @Test
    fun dispatchKeyEvent_barrierBlocksKeyEventsToOccludedFocusedNode() {
        // Arrange.
        val button = FocusRequester()
        var keyEventReceived = false
        var showBarrier by mutableStateOf(false)
        lateinit var focusOwner: FocusOwner

        rule.setFocusableContent {
            focusOwner = LocalFocusManager.current as FocusOwner
            Box(Modifier.size(200.dp)) {
                // Button that starts focused
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(button)
                        .onKeyEvent {
                            keyEventReceived = true
                            true
                        }
                        .focusTarget()
                )

                // Barrier placed on top of the button
                if (showBarrier) {
                    Box(Modifier.size(100.dp).interactionBarrier())
                }
            }
        }

        rule.runOnIdle { button.requestFocus() }

        // Act 1: Send key event before barrier - should succeed
        rule.runOnIdle {
            val event =
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_A,
                    )
                )
            val handled = focusOwner.dispatchKeyEvent(event)
            assertThat(handled).isTrue()
            assertThat(keyEventReceived).isTrue()
        }

        // Reset and show barrier on top of the focused button
        keyEventReceived = false
        rule.runOnIdle { showBarrier = true }

        // Act 2: Send key event after barrier covers the button - should be blocked!
        rule.runOnIdle {
            val event =
                KeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_A,
                    )
                )
            val handled = focusOwner.dispatchKeyEvent(event)
            assertThat(handled).isFalse()
            assertThat(keyEventReceived).isFalse()
        }
    }

    @Test
    fun activeFocusTarget_clearsFocusWhenBarrierPlacedOnTop() {
        // Arrange.
        val button = FocusRequester()
        var buttonFocused = false
        var showBarrier by mutableStateOf(false)

        rule.setFocusableContent {
            Box(Modifier.size(200.dp)) {
                // Button that starts focused
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(button)
                        .onFocusChanged { buttonFocused = it.isFocused }
                        .focusTarget()
                )

                // Barrier placed on top of the button
                if (showBarrier) {
                    Box(Modifier.size(100.dp).interactionBarrier())
                }
            }
        }

        // Initially focus the button
        rule.runOnIdle {
            button.requestFocus()
            assertThat(buttonFocused).isTrue()
        }

        // Act: Show barrier on top of the focused button
        rule.runOnIdle { showBarrier = true }

        // Assert: Active target becomes occluded and its focus is immediately cleared!
        rule.runOnIdle { assertThat(buttonFocused).isFalse() }
    }

    @Test
    fun requestFocus_rejectedWhenTargetIsOccludedByBarrier() {
        // Arrange.
        val button = FocusRequester()
        var buttonFocused = false

        rule.setFocusableContent {
            Box(Modifier.size(200.dp)) {
                // Button behind barrier
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(button)
                        .onFocusChanged { buttonFocused = it.isFocused }
                        .focusTarget()
                )

                // Barrier on top of button
                Box(Modifier.size(100.dp).interactionBarrier())
            }
        }

        // Act: Try requesting focus on occluded button
        rule.runOnIdle { button.requestFocus() }

        // Assert: Focus is rejected
        rule.runOnIdle { assertThat(buttonFocused).isFalse() }
    }

    @Test
    fun activeFocusTarget_retainsFocusWhenBarrierDoesNotCoverIt() {
        // Arrange.
        val button = FocusRequester()
        var buttonFocused = false
        var showBarrier by mutableStateOf(false)

        rule.setFocusableContent {
            Box(Modifier.size(300.dp)) {
                // Button at (0, 0)
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(button)
                        .onFocusChanged { buttonFocused = it.isFocused }
                        .focusTarget()
                )

                // Barrier at (150, 0), does not occlude the button
                if (showBarrier) {
                    Box(Modifier.offset(150.dp, 0.dp).size(100.dp).interactionBarrier())
                }
            }
        }

        // Initially focus the button
        rule.runOnIdle {
            button.requestFocus()
            assertThat(buttonFocused).isTrue()
        }

        // Act: Show barrier elsewhere
        rule.runOnIdle { showBarrier = true }

        // Assert: Button retains focus because it is not occluded
        rule.runOnIdle { assertThat(buttonFocused).isTrue() }
    }

    @Test
    fun activeFocusTarget_insideBarrierSubtree_retainsFocus() {
        // Arrange.
        val button = FocusRequester()
        var buttonFocused = false

        rule.setFocusableContent {
            Box(Modifier.size(200.dp)) {
                // Barrier with child inside
                Box(Modifier.size(200.dp).interactionBarrier()) {
                    Box(
                        Modifier.size(100.dp)
                            .focusRequester(button)
                            .onFocusChanged { buttonFocused = it.isFocused }
                            .focusTarget()
                    )
                }
            }
        }

        // Act: Focus button inside barrier
        rule.runOnIdle { button.requestFocus() }

        // Assert: Button inside barrier gets and retains focus
        rule.runOnIdle { assertThat(buttonFocused).isTrue() }
    }
}
