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

import android.view.KeyEvent
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_CTRL_ON
import android.view.KeyEvent.META_SHIFT_ON
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HardwareKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textField_typedEvents() {
        keysSequenceTest {
            Key.H.downAndUp()
            Key.I.downAndUp(META_SHIFT_ON)
            expectedText("hI")
        }
    }

    @Test
    fun textField_copyPaste() {
        keysSequenceTest(initText = "hello") {
            Key.A.downAndUp(META_CTRL_ON)
            Key.C.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp()
            Key.Spacebar.downAndUp()
            Key.V.downAndUp(META_CTRL_ON)
            expectedText("hello hello")
        }
    }

    @Test
    fun textField_directCopyPaste() {
        keysSequenceTest(initText = "hello") {
            Key.A.downAndUp(META_CTRL_ON)
            Key.Copy.downAndUp()
            expectedText("hello")
            Key.DirectionRight.downAndUp()
            Key.Spacebar.downAndUp()
            Key.Paste.downAndUp()
            expectedText("hello hello")
        }
    }

    @Test
    fun textField_directCutPaste() {
        keysSequenceTest(initText = "hello") {
            Key.A.downAndUp(META_CTRL_ON)
            Key.Cut.downAndUp()
            expectedText("")
            Key.Paste.downAndUp()
            expectedText("hello")
        }
    }

    @Test
    fun textField_linesNavigation() {
        keysSequenceTest(initText = "hello\nworld") {
            Key.DirectionDown.downAndUp()
            Key.Zero.downAndUp()
            Key.DirectionUp.downAndUp()
            Key.Zero.downAndUp()
            expectedText("h0ello\n0world")
            Key.DirectionUp.downAndUp()
            Key.Zero.downAndUp()
            expectedText("0h0ello\n0world")
        }
    }

    @Test
    fun textField_linesNavigation_cache() {
        keysSequenceTest(initText = "hello\n\nworld") {
            Key.DirectionRight.downAndUp()
            Key.DirectionDown.downAndUp()
            Key.DirectionDown.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hello\n\nw0orld")
        }
    }

    @Test
    fun textField_newLine() {
        keysSequenceTest(initText = "hello") {
            Key.Enter.downAndUp()
            expectedText("\nhello")
        }
    }

    @Test
    fun textField_backspace() {
        keysSequenceTest(initText = "hello") {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp()
            Key.Backspace.downAndUp()
            expectedText("hllo")
        }
    }

    @Test
    fun textField_delete() {
        keysSequenceTest(initText = "hello") {
            Key.Delete.downAndUp()
            expectedText("ello")
        }
    }

    @Test
    fun textField_delete_atEnd() {
        val text = "hello"
        val value = mutableStateOf(
            TextFieldValue(
                text,
                // Place cursor at end.
                selection = TextRange(text.length)
            )
        )
        keysSequenceTest(value = value) {
            Key.Delete.downAndUp()
            expectedText("hello")
        }
    }

    @Test
    fun textField_delete_whenEmpty() {
        keysSequenceTest(initText = "") {
            Key.Delete.downAndUp()
            expectedText("")
        }
    }

    @Test
    fun textField_nextWord() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello0 world")
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello0 world0")
        }
    }

    @Test
    fun textField_nextWord_doubleSpace() {
        keysSequenceTest(initText = "hello  world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello  world0")
        }
    }

    @Test
    fun textField_prevWord() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionLeft.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello 0world")
        }
    }

    @Test
    fun textField_HomeAndEnd() {
        keysSequenceTest(initText = "hello world") {
            Key.MoveEnd.downAndUp()
            Key.Zero.downAndUp()
            Key.MoveHome.downAndUp()
            Key.Zero.downAndUp()
            expectedText("0hello world0")
        }
    }

    @Test
    fun textField_byWordSelection() {
        keysSequenceTest(initText = "hello  world\nhi") {
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 5))
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 12))
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 15))
            Key.DirectionLeft.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 13))
        }
    }

    @Test
    fun textField_lineEndStart() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            Key.MoveEnd.downAndUp()
            Key.DirectionRight.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hi\n0hello world\nhi")
            Key.MoveEnd.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hi\n0hello world0\nhi")
            Key.MoveHome.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(16, 3))
            Key.MoveHome.downAndUp()
            Key.DirectionRight.downAndUp()
            Key.MoveEnd.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(4, 16))
            expectedText("hi\n0hello world0\nhi")
        }
    }

    @Test
    fun textField_altLineLeftRight() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            Key.DirectionRight.downAndUp(META_ALT_ON)
            Key.DirectionRight.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hi\n0hello world\nhi")
            Key.DirectionRight.downAndUp(META_ALT_ON)
            Key.Zero.downAndUp()
            expectedText("hi\n0hello world0\nhi")
            Key.DirectionLeft.downAndUp(META_ALT_ON or META_SHIFT_ON)
            expectedSelection(TextRange(16, 3))
            Key.DirectionLeft.downAndUp(META_ALT_ON)
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_ALT_ON or META_SHIFT_ON)
            expectedSelection(TextRange(4, 16))
            expectedText("hi\n0hello world0\nhi")
        }
    }

    @Test
    fun textField_altTop() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            Key.MoveEnd.downAndUp()
            repeat(3) { Key.DirectionRight.downAndUp() }
            Key.Zero.downAndUp()
            expectedText("hi\nhe0llo world\nhi")
            Key.DirectionUp.downAndUp(META_ALT_ON)
            Key.Zero.downAndUp()
            expectedText("0hi\nhe0llo world\nhi")
            Key.MoveEnd.downAndUp()
            repeat(3) { Key.DirectionRight.downAndUp() }
            Key.DirectionUp.downAndUp(META_ALT_ON or META_SHIFT_ON)
            expectedSelection(TextRange(6, 0))
            expectedText("0hi\nhe0llo world\nhi")
        }
    }

    @Test
    fun textField_altBottom() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            Key.MoveEnd.downAndUp()
            repeat(3) { Key.DirectionRight.downAndUp() }
            Key.Zero.downAndUp()
            expectedText("hi\nhe0llo world\nhi")
            Key.DirectionDown.downAndUp(META_ALT_ON or META_SHIFT_ON)
            expectedSelection(TextRange(6, 18))
            Key.DirectionLeft.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hi\nhe00llo world\nhi")
            Key.DirectionDown.downAndUp(META_ALT_ON)
            Key.Zero.downAndUp()
            expectedText("hi\nhe00llo world\nhi0")
        }
    }

    @Test
    fun textField_deleteWords() {
        keysSequenceTest(initText = "hello world\nhi world") {
            Key.MoveEnd.downAndUp()
            Key.Backspace.downAndUp(META_CTRL_ON)
            expectedText("hello \nhi world")
            Key.Delete.downAndUp(META_CTRL_ON)
            expectedText("hello  world")
        }
    }

    @Test
    fun textField_deleteToBeginningOfLine() {
        keysSequenceTest(initText = "hello world\nhi world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Backspace.downAndUp(META_ALT_ON)
            expectedText(" world\nhi world")
            Key.Backspace.downAndUp(META_ALT_ON)
            expectedText(" world\nhi world")
            repeat(3) { Key.DirectionRight.downAndUp() }
            Key.Backspace.downAndUp(META_ALT_ON)
            expectedText("rld\nhi world")
            Key.DirectionDown.downAndUp()
            Key.MoveEnd.downAndUp()
            Key.Backspace.downAndUp(META_ALT_ON)
            expectedText("rld\n")
            Key.Backspace.downAndUp(META_ALT_ON)
            expectedText("rld\n")
        }
    }

    @Test
    fun textField_deleteToEndOfLine() {
        keysSequenceTest(initText = "hello world\nhi world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Delete.downAndUp(META_ALT_ON)
            expectedText("hello\nhi world")
            Key.Delete.downAndUp(META_ALT_ON)
            expectedText("hello\nhi world")
            repeat(3) { Key.DirectionRight.downAndUp() }
            Key.Delete.downAndUp(META_ALT_ON)
            expectedText("hello\nhi")
            Key.MoveHome.downAndUp()
            Key.Delete.downAndUp(META_ALT_ON)
            expectedText("hello\n")
            Key.Delete.downAndUp(META_ALT_ON)
            expectedText("hello\n")
        }
    }

    @Test
    fun textField_paragraphNavigation() {
        keysSequenceTest(initText = "hello world\nhi") {
            Key.DirectionDown.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello world0\nhi")
            Key.DirectionDown.downAndUp(META_CTRL_ON)
            Key.DirectionUp.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello world0\n0hi")
            Key.DirectionUp.downAndUp(META_CTRL_ON)
            Key.DirectionUp.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("0hello world0\n0hi")
        }
    }

    @Test
    fun textField_selectionCaret() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON or META_SHIFT_ON)
            expectedSelection(TextRange(0, 5))
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(0, 6))
            Key.Backslash.downAndUp(META_CTRL_ON)
            expectedSelection(TextRange(6, 6))
            Key.DirectionLeft.downAndUp(META_CTRL_ON or META_SHIFT_ON)
            expectedSelection(TextRange(6, 0))
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(6, 1))
        }
    }

    @Test
    fun textField_onValueChangeRecomposeTest() {
        // sample code in b/200577798
        val value = mutableStateOf(TextFieldValue(""))
        var lastNewValue: TextFieldValue? = null
        val onValueChange: (TextFieldValue) -> Unit = { newValue ->
            lastNewValue = newValue
            if (newValue.text.isBlank() || newValue.text.startsWith("z")) {
                value.value = newValue
            }
        }

        keysSequenceTest(value = value, onValueChange = onValueChange) {
            // based on repro steps in the ticket, one of the values would become "aa"
            // check 10 times to make sure it is not "aa"
            repeat(10) {
                Key.A.downAndUp()
                // should always be "a" and buffer should not accumulate
                assertThat(lastNewValue?.text).isEqualTo("a")
            }
        }
    }

    @Test
    fun textField_pageNavigation() {
        keysSequenceTest(
            initText = "1\n2\n3\n4\n5",
            modifier = Modifier.requiredSize(27.dp)
        ) {
            // By page down, the cursor should be at the visible top line. In this case the height
            // constraint is 27dp which covers from 1, 2 and middle of 3. Thus, by page down, the
            // first line should be 3, and cursor should be the before letter 3, i.e. index = 4.
            Key.PageDown.downAndUp()
            expectedSelection(TextRange(4))
        }
    }

    @Test
    fun textField_tabSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.Tab.downAndUp()
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_tabMultiLine() {
        keysSequenceTest(initText = "text") {
            Key.Tab.downAndUp()
            expectedText("\ttext")
        }
    }

    @Test
    fun textField_shiftTabSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.Tab.downAndUp(metaState = META_SHIFT_ON)
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_enterSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.Enter.downAndUp()
            expectedText("text") // no change, should do ime action instead
        }
    }

    @Test
    fun textField_enterMultiLine() {
        keysSequenceTest(initText = "text") {
            Key.Enter.downAndUp()
            expectedText("\ntext")
        }
    }

    @Test
    fun textField_withActiveSelection_tabSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.Tab.downAndUp()
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_withActiveSelection_tabMultiLine() {
        keysSequenceTest(initText = "text") {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.Tab.downAndUp()
            expectedText("t\tt")
        }
    }

    @Test
    fun textField_withActiveSelection_shiftTabSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.Tab.downAndUp(metaState = META_SHIFT_ON)
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_withActiveSelection_enterSingleLine() {
        keysSequenceTest(initText = "text", singleLine = true) {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.Enter.downAndUp()
            expectedText("text") // no change, should do ime action instead
        }
    }

    @Test
    fun textField_withActiveSelection_enterMultiLine() {
        keysSequenceTest(initText = "text") {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            Key.Enter.downAndUp()
            expectedText("t\nt")
        }
    }

    private inner class SequenceScope(
        val state: MutableState<TextFieldValue>,
        val nodeGetter: () -> SemanticsNodeInteraction
    ) {
        fun Key.downAndUp(metaState: Int = 0) {
            this.down(metaState)
            this.up(metaState)
        }

        fun Key.down(metaState: Int = 0) {
            nodeGetter().performKeyPress(downEvent(this, metaState))
        }

        fun Key.up(metaState: Int = 0) {
            nodeGetter().performKeyPress(upEvent(this, metaState))
        }

        fun expectedText(text: String) {
            rule.runOnIdle {
                assertThat(state.value.text).isEqualTo(text)
            }
        }

        fun expectedSelection(selection: TextRange) {
            rule.runOnIdle {
                assertThat(state.value.selection).isEqualTo(selection)
            }
        }
    }

    private fun keysSequenceTest(
        initText: String = "",
        modifier: Modifier = Modifier.fillMaxSize(),
        singleLine: Boolean = false,
        sequence: SequenceScope.() -> Unit,
    ) {
        val value = mutableStateOf(TextFieldValue(initText))
        keysSequenceTest(
            value = value,
            modifier = modifier,
            singleLine = singleLine,
            sequence = sequence
        )
    }

    private fun keysSequenceTest(
        value: MutableState<TextFieldValue>,
        modifier: Modifier = Modifier.fillMaxSize(),
        onValueChange: (TextFieldValue) -> Unit = { value.value = it },
        singleLine: Boolean = false,
        sequence: SequenceScope.() -> Unit,
    ) {
        lateinit var clipboardManager: ClipboardManager
        rule.setContent {
            clipboardManager = LocalClipboardManager.current
            BasicTextField(
                value = value.value,
                textStyle = TextStyle(
                    fontFamily = TEST_FONT_FAMILY,
                    fontSize = 10.sp
                ),
                modifier = modifier.testTag("textfield"),
                onValueChange = onValueChange,
                singleLine = singleLine,
            )
        }

        rule.onNodeWithTag("textfield").requestFocus()
        rule.runOnIdle {
            clipboardManager.setText(AnnotatedString("InitialTestText"))
        }

        sequence(SequenceScope(value) { rule.onNode(hasSetTextAction()) })
    }
}

private fun downEvent(key: Key, metaState: Int = 0): androidx.compose.ui.input.key.KeyEvent {
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, key.nativeKeyCode, 0, metaState)
    )
}

private fun upEvent(key: Key, metaState: Int = 0): androidx.compose.ui.input.key.KeyEvent {
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(0L, 0L, KeyEvent.ACTION_UP, key.nativeKeyCode, 0, metaState)
    )
}
