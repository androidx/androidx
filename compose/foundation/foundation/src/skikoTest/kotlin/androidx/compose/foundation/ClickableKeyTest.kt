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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ClickableKeyTest {
    @Test
    fun trigger_clickable_with_enter_key() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)
    }

    @Test
    fun trigger_clickable_with_enter_key_and_shift() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_enter_key_and_alt() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_enter_key_and_ctrl() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_enter_key_and_meta() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_numpad_enter_key() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.NumPadEnter)
            keyUp(Key.NumPadEnter)
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)
    }

    @Test
    fun trigger_clickable_with_numpad_enter_key_and_shift() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_numpad_enter_key_and_alt() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_numpad_enter_key_and_ctrl() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_numpad_enter_key_and_meta() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_spacebar_key() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Spacebar)
            keyUp(Key.Spacebar)
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)
    }

    @Test
    fun trigger_clickable_with_spacebar_key_and_shift() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_spacebar_key_and_alt() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.AltRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_spacebar_key_and_ctrl() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_clickable_with_spacebar_key_and_meta() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(1)

        onNodeWithTag("my-clickable").performKeyInput {
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun trigger_toggleable_with_enter_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .toggleable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }

    @Test
    fun trigger_selectable_with_enter_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .selectable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }

    @Test
    fun trigger_toggleable_with_numpad_enter_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .toggleable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.NumPadEnter)
            keyUp(Key.NumPadEnter)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }

    @Test
    fun trigger_selectable_with_numpad_enter_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .selectable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.NumPadEnter)
            keyUp(Key.NumPadEnter)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.NumPadEnter)
                keyUp(Key.NumPadEnter)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }

    @Test
    fun trigger_toggleable_with_spacebar_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .toggleable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Spacebar)
            keyUp(Key.Spacebar)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }

    @Test
    fun trigger_selectable_with_spacebar_key_too() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var clicksCount = 0
        var focused = false

        setContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .selectable(false) { clicksCount++ }
                    .size(10.dp, 20.dp)
                    .testTag("my-clickable")
            )
        }

        waitForIdle()
        focusRequester.requestFocus()

        waitForIdle()
        assertThat(focused, "Component is focused").isTrue()

        onNodeWithTag("my-clickable").performKeyInput {
            keyDown(Key.Spacebar)
            keyUp(Key.Spacebar)

            withKeyDown(Key.ShiftLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.ShiftRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.CtrlLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.CtrlRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.AltLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.AltRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.MetaLeft) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
            withKeyDown(Key.MetaRight) {
                keyDown(Key.Spacebar)
                keyUp(Key.Spacebar)
            }
        }

        waitForIdle()
        assertThat(clicksCount).isEqualTo(9)
    }
}
