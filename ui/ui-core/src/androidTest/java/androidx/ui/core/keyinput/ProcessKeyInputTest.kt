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
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.setFocusableContent
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalStateException

@SmallTest
@RunWith(JUnit4::class)
class ProcessKeyInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test(expected = IllegalStateException::class)
    fun noRootFocusModifier_throwsException() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        composeTestRule.setContent {
            modifier = KeyInputModifier(null, null)
            Box(modifier = modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)

        // Act.
        runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun noFocusModifier_throwsException() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        composeTestRule.setFocusableContent {
            modifier = KeyInputModifier(null, null)
            Box(modifier = modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)

        // Act.
        runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun focusModifierNotFocused_throwsException() {

        // Arrange.
        lateinit var modifier: KeyInputModifier
        composeTestRule.setFocusableContent {
            modifier = KeyInputModifier(null, null)
            Box(modifier = FocusModifier() + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)

        // Act.
        runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }
    }

    @Test
    fun noKeyEventCallback_doesNotConsumeKey() {

        // Arrange.
        lateinit var modifier: KeyInputModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            modifier = KeyInputModifier(null, null)
            Box(modifier = focusModifier + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(keyConsumed).isFalse()
        }
    }

    @Test
    fun onKeyEvent_triggered() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        lateinit var focusModifier: FocusModifier
        var onKeyEventTriggered = false
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            modifier = KeyInputModifier(
                onKeyEvent = { onKeyEventTriggered = true; true },
                onPreviewKeyEvent = null
            )
            Box(modifier = focusModifier + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(onKeyEventTriggered).isTrue()
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onPreviewKeyEvent_triggered() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        lateinit var focusModifier: FocusModifier
        var onPreviewKeyEventTriggered = false
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            modifier = KeyInputModifier(
                onKeyEvent = null,
                onPreviewKeyEvent = { onPreviewKeyEventTriggered = true; true }
            )
            Box(modifier = focusModifier + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        val keyConsumed = runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(onPreviewKeyEventTriggered).isTrue()
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onKeyEventNotTriggered_ifOnPreviewKeyEventConsumesEvent() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        lateinit var focusModifier: FocusModifier
        var onKeyEventTriggered = false
        var onPreviewKeyEventTriggered = false
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            modifier = KeyInputModifier(
                onKeyEvent = { onKeyEventTriggered = true; true },
                onPreviewKeyEvent = { onPreviewKeyEventTriggered = true; true }
            )
            Box(modifier = focusModifier + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(onPreviewKeyEventTriggered).isTrue()
            assertThat(onKeyEventTriggered).isFalse()
        }
    }

    @Test
    fun onKeyEvent_triggeredAfter_onPreviewKeyEvent() {
        // Arrange.
        lateinit var modifier: KeyInputModifier
        lateinit var focusModifier: FocusModifier
        var triggerIndex = 1
        var onKeyEventTrigger = 0
        var onPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            modifier = KeyInputModifier(
                onKeyEvent = { onKeyEventTrigger = triggerIndex++; true },
                onPreviewKeyEvent = { onPreviewKeyEventTrigger = triggerIndex++; false }
            )
            Box(modifier = focusModifier + modifier)
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            focusModifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            modifier.processKeyInput(keyEvent)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(onPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(onKeyEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun parent_child() {
        // Arrange.
        lateinit var parentModifier: KeyInputModifier
        lateinit var childFocusModifier: FocusModifier
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            childFocusModifier = FocusModifier()
            parentModifier = KeyInputModifier(
                onKeyEvent = { parentOnKeyEventTrigger = triggerIndex++; false },
                onPreviewKeyEvent = { parentOnPreviewKeyEventTrigger = triggerIndex++; false }
            )
            Box(modifier = FocusModifier() + parentModifier) {
                Box(
                    modifier = childFocusModifier + KeyInputModifier(
                        onKeyEvent = { childOnKeyEventTrigger = triggerIndex++; false },
                        onPreviewKeyEvent = {
                            childOnPreviewKeyEventTrigger = triggerIndex++; false
                        }
                    )
                )
            }
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            parentModifier.processKeyInput(keyEvent)
        }

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
        lateinit var parentModifier: KeyInputModifier
        lateinit var childFocusModifier: FocusModifier
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        composeTestRule.setFocusableContent {
            childFocusModifier = FocusModifier()
            parentModifier = KeyInputModifier(
                onKeyEvent = { parentOnKeyEventTrigger = triggerIndex++; false },
                onPreviewKeyEvent = { parentOnPreviewKeyEventTrigger = triggerIndex++; false }
            )
            Box(modifier = parentModifier) {
                Box(
                    modifier = childFocusModifier + KeyInputModifier(
                        onKeyEvent = { childOnKeyEventTrigger = triggerIndex++; false },
                        onPreviewKeyEvent = {
                            childOnPreviewKeyEventTrigger = triggerIndex++; false
                        }
                    )
                )
            }
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            parentModifier.processKeyInput(keyEvent)
        }

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
        lateinit var grandParentModifier: KeyInputModifier
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
            grandParentModifier = KeyInputModifier(
                onKeyEvent = { grandParentOnKeyEventTrigger = triggerIndex++; false },
                onPreviewKeyEvent = { grandParentOnPreviewKeyEventTrigger = triggerIndex++; false }
            )
            Box(modifier = FocusModifier() + grandParentModifier) {
                Box(
                    modifier = FocusModifier() + KeyInputModifier(
                        onKeyEvent = { parentOnKeyEventTrigger = triggerIndex++; false },
                        onPreviewKeyEvent = {
                            parentOnPreviewKeyEventTrigger = triggerIndex++; false
                        }
                    )
                ) {
                    Box(
                        modifier = childFocusModifier + KeyInputModifier(
                            onKeyEvent = { childOnKeyEventTrigger = triggerIndex++; false },
                            onPreviewKeyEvent = {
                                childOnPreviewKeyEventTrigger = triggerIndex++; false
                            }
                        )
                    )
                }
            }
        }
        val keyEvent = KeyEvent(Key.A, KeyEventType.KeyUp)
        runOnIdleCompose {
            childFocusModifier.requestFocus()
        }

        // Act.
        runOnIdleCompose {
            grandParentModifier.processKeyInput(keyEvent)
        }

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