/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.internal.ComposeInputMethodManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldSendKeyEventTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField"

    @Test
    fun sendKeyEvent_doesNotRestartInput_forTypedEvent() {
        lateinit var imm: FakeInputMethodManager
        lateinit var originalFactory: ((View) -> ComposeInputMethodManager)

        originalFactory =
            immRule.setFactory {
                val actualImm = originalFactory(it)
                imm =
                    object : FakeInputMethodManager() {
                        override fun sendKeyEvent(event: KeyEvent) {
                            actualImm.sendKeyEvent(event)
                        }
                    }
                imm
            }

        val state = TextFieldState()
        inputMethodInterceptor.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        requestFocus()

        inputMethodInterceptor.withInputConnection {
            sendKeyEvent(softKeyEvent(KeyEvent.KEYCODE_A, KeyEventType.KeyDown))
            sendKeyEvent(softKeyEvent(KeyEvent.KEYCODE_A, KeyEventType.KeyUp))
        }

        rule.waitForIdle()

        imm.expectCall("updateSelection(1, 1, -1, -1)")
        imm.expectNoMoreCalls()

        assertThat(state.text.toString()).isEqualTo("a")
    }

    @Test
    fun sendKeyEvent_doesNotRestartInput_forBackspaceEvent() {
        lateinit var imm: FakeInputMethodManager
        lateinit var originalFactory: ((View) -> ComposeInputMethodManager)

        originalFactory =
            immRule.setFactory {
                val actualImm = originalFactory(it)
                imm =
                    object : FakeInputMethodManager() {
                        override fun sendKeyEvent(event: KeyEvent) {
                            actualImm.sendKeyEvent(event)
                        }
                    }
                imm
            }

        val state = TextFieldState("abc")
        inputMethodInterceptor.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        requestFocus()

        inputMethodInterceptor.withInputConnection {
            sendKeyEvent(softKeyEvent(KeyEvent.KEYCODE_DEL, KeyEventType.KeyDown))
            sendKeyEvent(softKeyEvent(KeyEvent.KEYCODE_DEL, KeyEventType.KeyUp))
        }

        rule.waitForIdle()

        imm.expectCall("updateSelection(2, 2, -1, -1)")
        imm.expectNoMoreCalls()

        assertThat(state.text.toString()).isEqualTo("ab")
    }

    private fun requestFocus() = rule.onNodeWithTag(Tag).requestFocus()
}

/** Creates a native [KeyEvent] instance that originates from IME. */
private fun softKeyEvent(keyCode: Int, keyEventType: KeyEventType): KeyEvent {
    val action =
        when (keyEventType) {
            KeyEventType.KeyDown -> KeyEvent.ACTION_DOWN
            KeyEventType.KeyUp -> KeyEvent.ACTION_UP
            else -> error("Unknown key event type")
        }
    return KeyEvent(
        0L,
        0L,
        action,
        keyCode,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
    )
}
