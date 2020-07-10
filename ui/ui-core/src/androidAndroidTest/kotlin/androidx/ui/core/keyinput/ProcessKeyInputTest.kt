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

package androidx.ui.core.keyinput

import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.setFocusableContent
import androidx.ui.core.keyinput.Key.Companion.A
import androidx.ui.core.keyinput.KeyEventType.KeyUp
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.doSendKeyEvent
import androidx.ui.test.findRoot
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalKeyInput::class)
class ProcessKeyInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test(expected = IllegalStateException::class)
    fun noRootFocusModifier_throwsException() {
        // Arrange.
        composeTestRule.setContent {
            Box(modifier = KeyInputModifier(null, null))
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))
    }

    @Test(expected = IllegalStateException::class)
    fun noFocusModifier_throwsException() {
        // Arrange.
        composeTestRule.setFocusableContent {
            Box(modifier = KeyInputModifier(null, null))
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))
    }

    @Test(expected = IllegalStateException::class)
    fun focusModifierNotFocused_throwsException() {

        // Arrange.
        composeTestRule.setFocusableContent {
            Box(modifier = FocusModifier() + KeyInputModifier(null, null))
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))
    }

    @Test
    fun noKeyEventCallback_doesNotConsumeKey() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(modifier = focusModifier + KeyInputModifier(null, null))
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            assertThat(keyConsumed).isFalse()
        }
    }

    @Test
    fun onKeyEvent_triggered() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        lateinit var receivedKeyEvent: KeyEvent2
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(
                modifier = focusModifier.keyInputFilter {
                    receivedKeyEvent = it
                    true
                }
            )
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            receivedKeyEvent.assertEqualTo(keyEvent(A, KeyUp))
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onPreviewKeyEvent_triggered() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        lateinit var receivedKeyEvent: KeyEvent2
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(
                modifier = focusModifier.previewKeyInputFilter {
                    receivedKeyEvent = it
                    true
                }
            )
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            receivedKeyEvent.assertEqualTo(keyEvent(A, KeyUp))
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onKeyEventNotTriggered_ifOnPreviewKeyEventConsumesEvent() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        lateinit var receivedPreviewKeyEvent: KeyEvent2
        var receivedKeyEvent: KeyEvent2? = null
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(
                modifier = focusModifier
                    .keyInputFilter {
                        receivedKeyEvent = it
                        true
                    }
                    .previewKeyInputFilter {
                        receivedPreviewKeyEvent = it
                        true
                    }
            )
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            receivedPreviewKeyEvent.assertEqualTo(keyEvent(A, KeyUp))
            assertThat(receivedKeyEvent).isNull()
        }
    }

    @Test
    fun onKeyEvent_triggeredAfter_onPreviewKeyEvent() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        var triggerIndex = 1
        var onKeyEventTrigger = 0
        var onPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(
                modifier = focusModifier
                    .keyInputFilter {
                        onKeyEventTrigger = triggerIndex++
                        true
                    }
                    .previewKeyInputFilter {
                        onPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            )
        }
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            assertThat(onPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(onKeyEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun parent_child() {
        // Arrange.
        lateinit var childFocusModifier: FocusModifier
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            childFocusModifier = FocusModifier()
            Box(
                modifier = FocusModifier()
                    .keyInputFilter {
                        parentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .previewKeyInputFilter {
                        parentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = childFocusModifier
                        .keyInputFilter {
                            childOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .previewKeyInputFilter {
                            childOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                )
            }
        }
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnKeyEventTrigger).isEqualTo(3)
            assertThat(parentOnKeyEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun parent_child_noFocusModifierForParent() {
        // Arrange.
        lateinit var childFocusModifier: FocusModifier
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            childFocusModifier = FocusModifier()
            Box(
                modifier = Modifier
                    .keyInputFilter {
                        parentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .previewKeyInputFilter {
                        parentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = childFocusModifier
                        .keyInputFilter {
                            childOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .previewKeyInputFilter {
                            childOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                )
            }
        }
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnKeyEventTrigger).isEqualTo(3)
            assertThat(parentOnKeyEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun grandParent_parent_child() {
        // Arrange.
        lateinit var childFocusModifier: FocusModifier
        var triggerIndex = 1
        var grandParentOnKeyEventTrigger = 0
        var grandParentOnPreviewKeyEventTrigger = 0
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            childFocusModifier = FocusModifier()
            Box(
                modifier = FocusModifier()
                    .keyInputFilter {
                        grandParentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .previewKeyInputFilter {
                        grandParentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = FocusModifier()
                        .keyInputFilter {
                            parentOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .previewKeyInputFilter {
                            parentOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                ) {
                    Box(
                        modifier = childFocusModifier
                            .keyInputFilter {
                                childOnKeyEventTrigger = triggerIndex++
                                false
                            }
                            .previewKeyInputFilter {
                                childOnPreviewKeyEventTrigger = triggerIndex++
                                false
                            }
                    )
                }
            }
        }
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        findRoot().doSendKeyEvent(keyEvent(A, KeyUp))

        // Assert.
        runOnIdleCompose {
            assertThat(grandParentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(3)
            assertThat(childOnKeyEventTrigger).isEqualTo(4)
            assertThat(parentOnKeyEventTrigger).isEqualTo(5)
            assertThat(grandParentOnKeyEventTrigger).isEqualTo(6)
        }
    }
}
