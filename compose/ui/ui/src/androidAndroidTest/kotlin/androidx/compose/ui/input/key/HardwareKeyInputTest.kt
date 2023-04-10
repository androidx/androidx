/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.input.key

import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class HardwareKeyInputTest {
    @get:Rule
    val rule = createComposeRule()

    val initialFocus = FocusRequester()

    @Test
    fun onKeyToSoftKeyboardInterceptedEventTriggered() {
        // Arrange.
        var receivedKeyEvent: KeyEvent? = null
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onInterceptKeyBeforeSoftKeyboard {
                        receivedKeyEvent = it
                        true
                    }
                    .focusRequester(initialFocus)
                    .focusTarget()
            )
        }

        // Act.
        val keyConsumed = rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.key).isEqualTo(Key.A)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onPreKeyToSoftKeyboardInterceptedEventTriggered() {
        // Arrange.
        var receivedKeyEvent: KeyEvent? = null
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onPreInterceptKeyBeforeSoftKeyboard {
                        receivedKeyEvent = it
                        true
                    }
                    .focusRequester(initialFocus)
                    .focusTarget()
            )
        }

        // Act.
        val keyConsumed = rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.key).isEqualTo(Key.A)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onKeyEventNotTriggered_ifOnPreKeyEventConsumesEvent() {
        // Arrange.
        var receivedPreKeyEvent: KeyEvent? = null
        var receivedKeyEvent: KeyEvent? = null
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onInterceptKeyBeforeSoftKeyboard {
                        receivedKeyEvent = it
                        true
                    }
                    .onPreInterceptKeyBeforeSoftKeyboard {
                        receivedPreKeyEvent = it
                        true
                    }
                    .focusRequester(initialFocus)
                    .focusTarget()
            )
        }

        // Act.
        val keyConsumed = rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedPreKeyEvent)
            assertThat(keyEvent.key).isEqualTo(Key.A)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(keyConsumed).isTrue()

            assertThat(receivedKeyEvent).isNull()
        }
    }

    @Test
    fun onKeyEvent_triggeredAfter_onPreviewKeyEvent() {
        // Arrange.
        var triggerIndex = 1
        var onInterceptedKeyEventTrigger = 0
        var onInterceptedPreKeyEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onInterceptKeyBeforeSoftKeyboard {
                        onInterceptedKeyEventTrigger = triggerIndex++
                        true
                    }
                    .onPreInterceptKeyBeforeSoftKeyboard {
                        onInterceptedPreKeyEventTrigger = triggerIndex++
                        false
                    }
                    .focusRequester(initialFocus)
                    .focusTarget()
            )
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(onInterceptedPreKeyEventTrigger).isEqualTo(1)
            assertThat(onInterceptedKeyEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun onKeyEvent_onKeyToSoftKeyboardInterceptedEvent_interaction() {
        // Arrange.
        var triggerIndex = 1
        var onInterceptedKeyEventTrigger = 0
        var onInterceptedPreKeyEventTrigger = 0
        var onKeyEventTrigger = 0
        var onPreviewKeyEventTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onKeyEvent {
                        onKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        onPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onInterceptKeyBeforeSoftKeyboard {
                        onInterceptedKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreInterceptKeyBeforeSoftKeyboard {
                        onInterceptedPreKeyEventTrigger = triggerIndex++
                        false
                    }
                    .focusRequester(initialFocus)
                    .focusTarget()
            )
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(onInterceptedPreKeyEventTrigger).isEqualTo(1)
            assertThat(onInterceptedKeyEventTrigger).isEqualTo(2)
            assertThat(onPreviewKeyEventTrigger).isEqualTo(3)
            assertThat(onKeyEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun onKeyEvent_onKeyToSoftKeyboardInterceptedEvent_parentChildInteraction() {
        // Arrange.
        var triggerIndex = 1
        var onInterceptedKeyEventChildTrigger = 0
        var onInterceptedKeyEventParentTrigger = 0
        var onPreInterceptedKeyEventChildTrigger = 0
        var onPreInterceptedKeyEvenParentTrigger = 0
        var onKeyEventChildTrigger = 0
        var onKeyEventParentTrigger = 0
        var onPreKeyEventChildTrigger = 0
        var onPreKeyEventParentTrigger = 0
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onKeyEvent {
                        onKeyEventParentTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        onPreKeyEventParentTrigger = triggerIndex++
                        false
                    }
                    .onInterceptKeyBeforeSoftKeyboard {
                        onInterceptedKeyEventParentTrigger = triggerIndex++
                        false
                    }
                    .onPreInterceptKeyBeforeSoftKeyboard {
                        onPreInterceptedKeyEvenParentTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    Modifier
                        .onKeyEvent {
                            onKeyEventChildTrigger = triggerIndex++
                            false
                        }
                        .onPreviewKeyEvent {
                            onPreKeyEventChildTrigger = triggerIndex++
                            false
                        }
                        .onInterceptKeyBeforeSoftKeyboard {
                            onInterceptedKeyEventChildTrigger = triggerIndex++
                            false
                        }
                        .onPreInterceptKeyBeforeSoftKeyboard {
                            onPreInterceptedKeyEventChildTrigger = triggerIndex++
                            false
                        }
                        .focusRequester(initialFocus)
                        .focusable()
                )
            }
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(onPreInterceptedKeyEvenParentTrigger).isEqualTo(1)
            assertThat(onPreInterceptedKeyEventChildTrigger).isEqualTo(2)
            assertThat(onInterceptedKeyEventChildTrigger).isEqualTo(3)
            assertThat(onInterceptedKeyEventParentTrigger).isEqualTo(4)
            assertThat(onPreKeyEventParentTrigger).isEqualTo(5)
            assertThat(onPreKeyEventChildTrigger).isEqualTo(6)
            assertThat(onKeyEventChildTrigger).isEqualTo(7)
            assertThat(onKeyEventParentTrigger).isEqualTo(8)
        }
    }

    private fun ComposeContentTestRule.setContentWithInitialFocus(content: @Composable () -> Unit) {
        setFocusableContent {
            Box(modifier = Modifier.requiredSize(100.dp, 100.dp)) { content() }
        }
        runOnIdle {
            initialFocus.requestFocus()
        }
    }

    /**
     * The [KeyEvent] is usually created by the system. This function creates an instance of
     * [KeyEvent] that can be used in tests.
     */
    private fun keyEvent(keycode: Int, keyEventType: KeyEventType): KeyEvent {
        val action = when (keyEventType) {
            KeyDown -> ACTION_DOWN
            KeyUp -> ACTION_UP
            else -> error("Unknown key event type")
        }
        return KeyEvent(AndroidKeyEvent(0L, 0L, action, keycode, 0, 0))
    }
}
