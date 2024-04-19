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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TextFieldUndoTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun undo_redo_withCtrlShiftZ() {
        undoRedoTest(redoKeys = listOf(Key.CtrlLeft, Key.ShiftLeft, Key.Z))
    }

    @Test
    fun undo_redo_withCtrlY() {
        undoRedoTest(redoKeys = listOf(Key.CtrlLeft, Key.Y))
    }

    private fun undoRedoTest(redoKeys: List<Key>) {
        val state = mutableStateOf("hi")
        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it }
            )
        }

        state.value = "hello"

        // undo command
        with(rule.onNode(hasSetTextAction())) {
            requestFocus()
            performKeyInput {
                withKeyDown(Key.CtrlLeft) {
                    pressKey(Key.Z)
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("hi")
        }

        // redo command
        rule.onNode(hasSetTextAction()).performKeyInput {
            redoKeys.forEach { keyDown(it) }
            advanceEventTime(100)
            redoKeys.forEach { keyUp(it) }
        }

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("hello")
        }
    }
}
