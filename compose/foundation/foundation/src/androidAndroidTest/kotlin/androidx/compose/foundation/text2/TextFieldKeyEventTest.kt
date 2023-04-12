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

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text2.input.TextFieldLineLimits.SingleLine
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.withKeysDown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalTestApi::class
)
class TextFieldKeyEventTest {
    @get:Rule
    val rule = createComposeRule()

    private val tag = "TextFieldTestTag"

    private var defaultDensity = Density(1f)

    @Test
    fun textField_typedEvents() {
        keysSequenceTest {
            pressKey(Key.H)
            press(Key.ShiftLeft + Key.I)
            expectedText("hI")
        }
    }

    @Ignore // re-enable after copy-cut-paste is supported
    @Test
    fun textField_copyPaste() {
        keysSequenceTest("hello") {
            withKeyDown(Key.CtrlLeft) {
                pressKey(Key.A)
                pressKey(Key.C)
            }
            pressKey(Key.DirectionRight)
            pressKey(Key.Spacebar)
            press(Key.CtrlLeft + Key.V)
            expectedText("hello hello")
        }
    }

    @Ignore // re-enable after copy-cut-paste is supported
    @Test
    fun textField_directCopyPaste() {
        keysSequenceTest("hello") {
            press(Key.CtrlLeft + Key.A)
            pressKey(Key.Copy)
            expectedText("hello")
            pressKey(Key.DirectionRight)
            pressKey(Key.Spacebar)
            pressKey(Key.Paste)
            expectedText("hello hello")
        }
    }

    @Ignore // re-enable after copy-cut-paste is supported
    @Test
    fun textField_directCutPaste() {
        keysSequenceTest("hello") {
            press(Key.CtrlLeft + Key.A)
            pressKey(Key.Cut)
            expectedText("")
            pressKey(Key.Paste)
            expectedText("hello")
        }
    }

    @Test
    fun textField_linesNavigation() {
        keysSequenceTest("hello\nworld") {
            pressKey(Key.DirectionDown)
            pressKey(Key.A)
            pressKey(Key.DirectionUp)
            pressKey(Key.A)
            expectedText("haello\naworld")
            pressKey(Key.DirectionUp)
            pressKey(Key.A)
            expectedText("ahaello\naworld")
        }
    }

    @Test
    fun textField_linesNavigation_cache() {
        keysSequenceTest("hello\n\nworld") {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
            pressKey(Key.Zero)
            expectedText("hello\n\nw0orld")
        }
    }

    @Test
    fun textField_newLine() {
        keysSequenceTest("hello") {
            pressKey(Key.Enter)
            expectedText("\nhello")
        }
    }

    @Test
    fun textField_backspace() {
        keysSequenceTest("hello") {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
            pressKey(Key.Backspace)
            expectedText("hllo")
        }
    }

    @Test
    fun textField_delete() {
        keysSequenceTest("hello") {
            pressKey(Key.Delete)
            expectedText("ello")
        }
    }

    @Test
    fun textField_delete_atEnd() {
        keysSequenceTest("hello", TextRange(5)) {
            pressKey(Key.Delete)
            expectedText("hello")
        }
    }

    @Test
    fun textField_delete_whenEmpty() {
        keysSequenceTest {
            pressKey(Key.Delete)
            expectedText("")
        }
    }

    @Test
    fun textField_nextWord() {
        keysSequenceTest("hello world") {
            press(Key.CtrlLeft + Key.DirectionRight)
            pressKey(Key.Zero)
            expectedText("hello0 world")
            press(Key.CtrlLeft + Key.DirectionRight)
            pressKey(Key.Zero)
            expectedText("hello0 world0")
        }
    }

    @Test
    fun textField_nextWord_doubleSpace() {
        keysSequenceTest("hello  world") {
            press(Key.CtrlLeft + Key.DirectionRight)
            pressKey(Key.DirectionRight)
            press(Key.CtrlLeft + Key.DirectionRight)
            pressKey(Key.Zero)
            expectedText("hello  world0")
        }
    }

    @Test
    fun textField_prevWord() {
        keysSequenceTest("hello world") {
            withKeyDown(Key.CtrlLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionLeft)
            }
            pressKey(Key.Zero)
            expectedText("hello 0world")
        }
    }

    @Test
    fun textField_HomeAndEnd() {
        keysSequenceTest("hello world") {
            pressKey(Key.MoveEnd)
            pressKey(Key.Zero)
            pressKey(Key.MoveHome)
            pressKey(Key.Zero)
            expectedText("0hello world0")
        }
    }

    @Test
    fun textField_byWordSelection() {
        keysSequenceTest("hello  world\nhi") {
            withKeysDown(listOf(Key.ShiftLeft, Key.CtrlLeft)) {
                pressKey(Key.DirectionRight)
                expectedSelection(TextRange(0, 5))
                pressKey(Key.DirectionRight)
                expectedSelection(TextRange(0, 12))
                pressKey(Key.DirectionRight)
                expectedSelection(TextRange(0, 15))
                pressKey(Key.DirectionLeft)
                expectedSelection(TextRange(0, 13))
            }
        }
    }

    @Ignore // TODO(halilibo): Remove ignore when backing buffer supports reversed selection
    @Test
    fun textField_lineEndStart() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            pressKey(Key.MoveEnd)
            pressKey(Key.DirectionRight)
            pressKey(Key.Zero)
            expectedText("hi\n0hello world\nhi")
            pressKey(Key.MoveEnd)
            pressKey(Key.Zero)
            expectedText("hi\n0hello world0\nhi")
            withKeyDown(Key.ShiftLeft) { pressKey(Key.MoveHome) }
            expectedSelection(TextRange(16, 3))
            pressKey(Key.MoveHome)
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) { pressKey(Key.MoveEnd) }
            expectedSelection(TextRange(4, 16))
            expectedText("hi\n0hello world0\nhi")
        }
    }

    @Ignore // TODO(halilibo): Remove ignore when backing buffer supports reversed selection
    @Test
    fun textField_altLineLeftRight() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            withKeyDown(Key.AltLeft) { pressKey(Key.DirectionRight) }
            pressKey(Key.DirectionRight)
            pressKey(Key.Zero)
            expectedText("hi\n0hello world\nhi")
            withKeyDown(Key.AltLeft) { pressKey(Key.DirectionRight) }
            pressKey(Key.Zero)
            expectedText("hi\n0hello world0\nhi")
            withKeysDown(listOf(Key.ShiftLeft, Key.AltLeft)) { pressKey(Key.DirectionLeft) }
            expectedSelection(TextRange(16, 3))
            withKeyDown(Key.AltLeft) { pressKey(Key.DirectionLeft) }
            pressKey(Key.DirectionRight)
            withKeysDown(listOf(Key.ShiftLeft, Key.AltLeft)) { pressKey(Key.DirectionRight) }
            expectedSelection(TextRange(4, 16))
            expectedText("hi\n0hello world0\nhi")
        }
    }

    @Ignore // TODO(halilibo): Remove ignore when backing buffer supports reversed selection
    @Test
    fun textField_altTop() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            pressKey(Key.MoveEnd)
            repeat(3) { pressKey(Key.DirectionRight) }
            pressKey(Key.Zero)
            expectedText("hi\nhe0llo world\nhi")
            withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) }
            pressKey(Key.Zero)
            expectedText("0hi\nhe0llo world\nhi")
            pressKey(Key.MoveEnd)
            repeat(3) { pressKey(Key.DirectionRight) }
            withKeysDown(listOf(Key.ShiftLeft, Key.AltLeft)) { pressKey(Key.DirectionUp) }
            expectedSelection(TextRange(6, 0))
            expectedText("0hi\nhe0llo world\nhi")
        }
    }

    @Test
    fun textField_altBottom() {
        keysSequenceTest(initText = "hi\nhello world\nhi") {
            pressKey(Key.MoveEnd)
            repeat(3) { pressKey(Key.DirectionRight) }
            pressKey(Key.Zero)
            expectedText("hi\nhe0llo world\nhi")
            withKeysDown(listOf(Key.ShiftLeft, Key.AltLeft)) { pressKey(Key.DirectionDown) }
            expectedSelection(TextRange(6, 18))
            pressKey(Key.DirectionLeft)
            pressKey(Key.Zero)
            expectedText("hi\nhe00llo world\nhi")
            withKeyDown(Key.AltLeft) { pressKey(Key.DirectionDown) }
            pressKey(Key.Zero)
            expectedText("hi\nhe00llo world\nhi0")
        }
    }

    @Test
    fun textField_deleteWords() {
        keysSequenceTest("hello world\nhi world") {
            pressKey(Key.MoveEnd)
            withKeyDown(Key.CtrlLeft) {
                pressKey(Key.Backspace)
                expectedText("hello \nhi world")
                pressKey(Key.Delete)
            }
            expectedText("hello  world")
        }
    }

    @Test
    fun textField_deleteToBeginningOfLine() {
        keysSequenceTest("hello world\nhi world") {
            press(Key.CtrlLeft + Key.DirectionRight)

            withKeyDown(Key.AltLeft) {
                pressKey(Key.Backspace)
                expectedText(" world\nhi world")
                pressKey(Key.Backspace)
                expectedText(" world\nhi world")
            }

            repeat(3) { pressKey(Key.DirectionRight) }

            press(Key.AltLeft + Key.Backspace)
            expectedText("rld\nhi world")
            pressKey(Key.DirectionDown)
            pressKey(Key.MoveEnd)

            withKeyDown(Key.AltLeft) {
                pressKey(Key.Backspace)
                expectedText("rld\n")
                pressKey(Key.Backspace)
                expectedText("rld\n")
            }
        }
    }

    @Test
    fun textField_deleteToEndOfLine() {
        keysSequenceTest("hello world\nhi world") {
            press(Key.CtrlLeft + Key.DirectionRight)
            withKeyDown(Key.AltLeft) {
                pressKey(Key.Delete)
                expectedText("hello\nhi world")
                pressKey(Key.Delete)
                expectedText("hello\nhi world")
            }

            repeat(3) { pressKey(Key.DirectionRight) }

            press(Key.AltLeft + Key.Delete)
            expectedText("hello\nhi")

            pressKey(Key.MoveHome)
            withKeyDown(Key.AltLeft) {
                pressKey(Key.Delete)
                expectedText("hello\n")
                pressKey(Key.Delete)
                expectedText("hello\n")
            }
        }
    }

    @Test
    fun textField_paragraphNavigation() {
        keysSequenceTest("hello world\nhi") {
            press(Key.CtrlLeft + Key.DirectionDown)
            pressKey(Key.Zero)
            expectedText("hello world0\nhi")
            withKeyDown(Key.CtrlLeft) {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionUp)
            }
            pressKey(Key.Zero)
            expectedText("hello world0\n0hi")
        }
    }

    @Ignore // TODO(halilibo): Remove ignore when backing buffer supports reversed selection
    @Test
    fun textField_selectionCaret() {
        keysSequenceTest("hello world") {
            press(Key.CtrlLeft + Key.ShiftLeft + Key.DirectionRight)
            expectedSelection(TextRange(0, 5))
            press(Key.ShiftLeft + Key.DirectionRight)
            expectedSelection(TextRange(0, 6))
            press(Key.CtrlLeft + Key.Backslash)
            expectedSelection(TextRange(6, 6))
            press(Key.CtrlLeft + Key.ShiftLeft + Key.DirectionLeft)
            expectedSelection(TextRange(6, 0))
            press(Key.ShiftLeft + Key.DirectionRight)
            expectedSelection(TextRange(1, 6))
        }
    }

    @Test
    fun textField_pageNavigationDown() {
        keysSequenceTest(
            initText = "A\nB\nC\nD\nE",
            modifier = Modifier.requiredSize(73.dp)
        ) {
            pressKey(Key.PageDown)
            expectedSelection(TextRange(4))
        }
    }

    @Test
    fun textField_pageNavigationDown_exactFit() {
        keysSequenceTest(
            initText = "A\nB\nC\nD\nE",
            modifier = Modifier.requiredSize(90.dp) // exactly 3 lines fit
        ) {
            pressKey(Key.PageDown)
            expectedSelection(TextRange(6))
        }
    }

    @Test
    fun textField_pageNavigationUp() {
        keysSequenceTest(
            initText = "A\nB\nC\nD\nE",
            initSelection = TextRange(8), // just before 5
            modifier = Modifier.requiredSize(73.dp)
        ) {
            pressKey(Key.PageUp)
            expectedSelection(TextRange(4))
        }
    }

    @Test
    fun textField_pageNavigationUp_exactFit() {
        keysSequenceTest(
            initText = "A\nB\nC\nD\nE",
            initSelection = TextRange(8), // just before 5
            modifier = Modifier.requiredSize(90.dp) // exactly 3 lines fit
        ) {
            pressKey(Key.PageUp)
            expectedSelection(TextRange(2))
        }
    }

    @Test
    fun textField_pageNavigationUp_cantGoUp() {
        keysSequenceTest(
            initText = "1\n2\n3\n4\n5",
            initSelection = TextRange(0),
            modifier = Modifier.requiredSize(90.dp)
        ) {
            pressKey(Key.PageUp)
            expectedSelection(TextRange(0))
        }
    }

    @Test
    fun textField_tabSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            pressKey(Key.Tab)
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_tabMultiLine() {
        keysSequenceTest("text") {
            pressKey(Key.Tab)
            expectedText("\ttext")
        }
    }

    @Test
    fun textField_shiftTabSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            press(Key.ShiftLeft + Key.Tab)
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_enterSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            pressKey(Key.Enter)
            expectedText("text") // no change, should do ime action instead
        }
    }

    @Test
    fun textField_enterMultiLine() {
        keysSequenceTest("text") {
            pressKey(Key.Enter)
            expectedText("\ntext")
        }
    }

    @Test
    fun textField_withActiveSelection_tabSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
            }
            pressKey(Key.Tab)
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_withActiveSelection_tabMultiLine() {
        keysSequenceTest("text") {
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
            }
            pressKey(Key.Tab)
            expectedText("t\tt")
        }
    }

    @Ignore // TODO(halilibo): Remove ignore when backing buffer supports reversed selection
    @Test
    fun textField_selectToLeft() {
        keysSequenceTest("hello world hello") {
            pressKey(Key.MoveEnd)
            expectedSelection(TextRange(17))
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionLeft)
            }
            expectedSelection(TextRange(17, 14))
        }
    }

    @Test
    fun textField_withActiveSelection_shiftTabSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
                pressKey(Key.Tab)
            }
            expectedText("text") // no change, should try focus change instead
        }
    }

    @Test
    fun textField_withActiveSelection_enterSingleLine() {
        keysSequenceTest("text", singleLine = true) {
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
            }
            pressKey(Key.Enter)
            expectedText("text") // no change, should do ime action instead
        }
    }

    @Test
    fun textField_withActiveSelection_enterMultiLine() {
        keysSequenceTest("text") {
            pressKey(Key.DirectionRight)
            withKeyDown(Key.ShiftLeft) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
            }
            pressKey(Key.Enter)
            expectedText("t\nt")
        }
    }

    private inner class SequenceScope(
        val state: TextFieldState,
        private val keyInjectionScope: KeyInjectionScope
    ) : KeyInjectionScope by keyInjectionScope {

        fun press(keys: List<Key>) {
            require(keys.isNotEmpty()) { "At least one key must be specified for press action" }
            if (keys.size == 1) {
                pressKey(keys.first())
            } else {
                withKeysDown(keys.dropLast(1)) { pressKey(keys.last()) }
            }
        }

        infix operator fun Key.plus(other: Key): MutableList<Key> {
            return mutableListOf(this, other)
        }

        fun expectedText(text: String) {
            rule.runOnIdle {
                assertThat(state.text.toString()).isEqualTo(text)
            }
        }

        fun expectedSelection(selection: TextRange) {
            rule.runOnIdle {
                assertThat(state.text.selectionInChars).isEqualTo(selection)
            }
        }
    }

    private fun keysSequenceTest(
        initText: String = "",
        initSelection: TextRange = TextRange.Zero,
        modifier: Modifier = Modifier.fillMaxSize(),
        singleLine: Boolean = false,
        sequence: SequenceScope.() -> Unit,
    ) {
        val state = TextFieldState(initText, initSelection)
        val focusRequester = FocusRequester()
        rule.setContent {
            LocalClipboardManager.current.setText(AnnotatedString("InitialTestText"))
            CompositionLocalProvider(LocalDensity provides defaultDensity) {
                BasicTextField2(
                    state = state,
                    textStyle = TextStyle(
                        fontFamily = TEST_FONT_FAMILY,
                        fontSize = 30.sp
                    ),
                    modifier = modifier
                        .focusRequester(focusRequester)
                        .testTag(tag),
                    lineLimits = if (singleLine) SingleLine else MultiLine(),
                )
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag(tag).performKeyInput {
            sequence(SequenceScope(state, this@performKeyInput))
        }
    }
}