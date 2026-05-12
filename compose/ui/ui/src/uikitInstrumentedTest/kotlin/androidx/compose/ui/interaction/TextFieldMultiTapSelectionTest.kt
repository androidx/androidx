/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.center
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.test.utils.findFirstDescendant
import androidx.compose.ui.test.utils.isLoupeView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFieldMultiTapSelectionTest {

    private val tfOptions = listOf(TextFieldFactory.BTF1, TextFieldFactory.BTF2)

    @Test
    fun double_tap_selects_word() = runUIKitInstrumentedTest(params = tfOptions) { textFieldOption ->
        textFieldOption.setup(this, MULTI_WORD_TEXT, TAG)
        focusThenDoubleTap(TAG)
        assertFalse(textFieldOption.selection.collapsed, "[${textFieldOption.name}] Expected a word to be selected after double tap")
        assertTrue(
            textFieldOption.selection.length < MULTI_WORD_TEXT.length,
            "[${textFieldOption.name}] Expected only a word to be selected, not the entire text, but got: ${textFieldOption.selection}"
        )
    }

    @Test
    fun triple_tap_selects_all_text() = runUIKitInstrumentedTest(params = tfOptions) { textFieldOption ->
        textFieldOption.setup(this, MULTI_WORD_TEXT, TAG)
        focusThenTripleTap(TAG)
        assertTrue(
            textFieldOption.selection.start == 0 && textFieldOption.selection.end == MULTI_WORD_TEXT.length,
            "[${textFieldOption.name}] Expected all text to be selected after triple tap, but got: ${textFieldOption.selection}"
        )
    }

    @Test
    fun multitap_does_not_show_magnifier() = runUIKitInstrumentedTest(params = tfOptions) { textFieldOption ->
        textFieldOption.setup(this, MULTI_WORD_TEXT, TAG)
        focusThenDoubleTap(TAG) // double tap is enough
        delay(200)
        assertEquals(
            findFirstDescendant { it.isLoupeView },
            null,
            "[${textFieldOption.name}] Magnifier should not appear during multi-tap selection"
        )
    }

    @Test
    fun BTF2_triple_tap_then_double_tap_selects_word() = runUIKitInstrumentedTest(params = listOf(TextFieldFactory.BTF2)) { textFieldOption ->
        textFieldOption.setup(this, MULTI_WORD_TEXT, TAG)

        focusThenTripleTap(TAG)
        assertTrue(
            textFieldOption.selection.start == 0 && textFieldOption.selection.end == MULTI_WORD_TEXT.length,
            "BTF2: triple tap should select all text"
        )

        // After triple tap selects all, a subsequent double tap should re-select only a word.
        // This exercises the clearSelection fix that allows selection to be updated by repeated taps.
        delay(400)
        findNodeWithTag(TAG).doubleTap()
        waitForIdle()

        val afterDoubleTap = textFieldOption.selection
        assertFalse(afterDoubleTap.collapsed, "BTF2: Expected a word to be selected after double tap")
        assertTrue(
            afterDoubleTap.length < MULTI_WORD_TEXT.length,
            "BTF2: Expected only a word to be selected after double tap, but got: $afterDoubleTap"
        )
    }

    private fun UIKitInstrumentedTest.focusThenDoubleTap(tag: String) {
        findNodeWithTag(tag).tap()
        delay(400)
        findNodeWithTag(tag).doubleTap()
        waitForIdle()
    }

    private fun UIKitInstrumentedTest.focusThenTripleTap(tag: String) {
        findNodeWithTag(tag).tap()
        delay(400)
        // Tap at 10% from left to stay inside the first word on any screen width.
        // Tapping at the frame center might be failed because the frame center
        // might be near the edge of the double-tapped word's selection handle.
        val frame = findNodeWithTag(tag).frame!!
        val tapPoint = DpOffset(frame.left + (frame.right - frame.left) * 0.1f, frame.center().y)
        tap(tapPoint)
        delay(50)
        tap(tapPoint)
        delay(50)
        tap(tapPoint)
        waitForIdle()
    }

    companion object {
        private const val TAG = "textField"
        private const val MULTI_WORD_TEXT = "accomplishment extraordinary magnificent establishment"
    }
}

private sealed class TextFieldFactory(val name: String) {
    private var _selection: (() -> TextRange)? = null
    val selection: TextRange get() = _selection!!()

    fun setup(test: UIKitInstrumentedTest, text: String, tag: String) {
        _selection = setupTextFieldAndFetchSelection(test, text, tag)
    }

    protected abstract fun setupTextFieldAndFetchSelection(test: UIKitInstrumentedTest, text: String, tag: String): () -> TextRange

    object BTF1 : TextFieldFactory("BasicTextField(value)") {
        override fun setupTextFieldAndFetchSelection(test: UIKitInstrumentedTest, text: String, tag: String): () -> TextRange {
            val valueState = mutableStateOf(TextFieldValue(text))
            test.setContent {
                Box(Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = valueState.value,
                        onValueChange = { valueState.value = it },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(tag)
                            .padding(16.dp)
                    )
                }
            }
            return { valueState.value.selection }
        }
    }

    object BTF2 : TextFieldFactory("BasicTextField(state)") {
        override fun setupTextFieldAndFetchSelection(test: UIKitInstrumentedTest, text: String, tag: String): () -> TextRange {
            val state = TextFieldState(text)
            test.setContent {
                Box(Modifier.fillMaxSize()) {
                    BasicTextField(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(tag)
                            .padding(16.dp)
                    )
                }
            }
            return { state.selection }
        }
    }
}
