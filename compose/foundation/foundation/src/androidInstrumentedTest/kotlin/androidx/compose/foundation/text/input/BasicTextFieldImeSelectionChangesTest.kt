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

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.View
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This class tests different ways of updating selection in TextFieldState and asserts that IME gets
 * updated for all of them.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldImeSelectionChangesTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField"

    private val imm = FakeInputMethodManager()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun perform_performContextMenuActionSelectAll() {
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        rule.onNodeWithTag(Tag).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performContextMenuAction(android.R.id.selectAll)
        }

        imm.expectCall("updateSelection(0, 5, -1, -1)")
    }

    @Test
    fun perform_setSelection() {
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        rule.onNodeWithTag(Tag).requestFocus()

        inputMethodInterceptor.withInputConnection { setSelection(1, 3) }

        imm.expectCall("updateSelection(1, 3, -1, -1)")
    }

    @Test
    fun perform_setComposingText() {
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        rule.onNodeWithTag(Tag).requestFocus()

        inputMethodInterceptor.withInputConnection {
            setComposingRegion(0, 5)
            setComposingText("World", 1)
        }

        imm.expectCall("updateSelection(5, 5, 0, 5)")
    }

    @Test
    fun perform_sendKeyEvent() {
        val state = TextFieldState("Hello")
        lateinit var view: View
        inputMethodInterceptor.setTextFieldTestContent {
            view = LocalView.current
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }
        rule.onNodeWithTag(Tag).performClick()

        inputMethodInterceptor.withInputConnection {
            // we have to use view.dispatchKeyEvent instead of InputConnection#sendKeyEvent
            // since the test overrides inputMethodManager, effectively disabling the KeyEvent
            // chain.

            // move to start
            view.dispatchKeyEvent(
                KeyEvent(
                    /* downTime = */ 0,
                    /* eventTime = */ 0,
                    /* action = */ ACTION_DOWN,
                    /* code = */ KeyEvent.KEYCODE_DPAD_LEFT,
                    /* repeat = */ 0,
                    /* metaState = */ KeyEvent.META_CTRL_ON
                )
            )
            view.dispatchKeyEvent(
                KeyEvent(
                    /* downTime = */ 0,
                    /* eventTime = */ 0,
                    /* action = */ ACTION_UP,
                    /* code = */ KeyEvent.KEYCODE_DPAD_LEFT,
                    /* repeat = */ 0,
                    /* metaState = */ KeyEvent.META_CTRL_ON
                )
            )

            // select from start to end
            view.dispatchKeyEvent(
                KeyEvent(
                    /* downTime = */ 0,
                    /* eventTime = */ 0,
                    /* action = */ ACTION_DOWN,
                    /* code = */ KeyEvent.KEYCODE_DPAD_RIGHT,
                    /* repeat = */ 0,
                    /* metaState = */ KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
                )
            )
            view.dispatchKeyEvent(
                KeyEvent(
                    /* downTime = */ 0,
                    /* eventTime = */ 0,
                    /* action = */ ACTION_UP,
                    /* code = */ KeyEvent.KEYCODE_DPAD_RIGHT,
                    /* repeat = */ 0,
                    /* metaState = */ KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
                )
            )
        }

        imm.expectCall("updateSelection(0, 0, -1, -1)")
        imm.expectCall("updateSelection(0, 5, -1, -1)")
    }
}
