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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeysDown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher

// This file should be moved to commonTest once the infrastructure for running common tests on
// device is set up. Currently, it fails presubmit when placed in commonTest because that attempts
// to run the tests on the host.
@OptIn(ExperimentalTestApi::class)
class CommonTextFieldKeyEventTest {
    @Test
    fun textField_left() =
        singleKeyStrokeTest(
            initSelection = TextRange(1),
            key = Key.DirectionLeft,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_numPadLeft() =
        singleKeyStrokeTest(
            initSelection = TextRange(1),
            key = Key.NumPadDirectionLeft,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_right() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            key = Key.DirectionRight,
            expectedSelection = TextRange(1),
        )

    @Test
    fun textField_numPadRight() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            key = Key.NumPadDirectionRight,
            expectedSelection = TextRange(1),
        )

    @Test
    fun textField_up() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            key = Key.DirectionUp,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_numPadUp() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            key = Key.NumPadDirectionUp,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_down() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            key = Key.DirectionDown,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_numpadDown() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            key = Key.NumPadDirectionDown,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_pageUp() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange("Hello\n".length),
            key = Key.PageUp,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_numPadPageUp() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange("Hello\n".length),
            key = Key.NumPadPageUp,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_pageDown() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange(0),
            key = Key.PageDown,
            expectedSelection = TextRange("Hello\n".length),
        )

    @Test
    fun textField_numPadPageDown() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange(0),
            key = Key.NumPadPageDown,
            expectedSelection = TextRange("Hello\n".length),
        )

    @Test
    fun textField_home() =
        singleKeyStrokeTest(
            initSelection = TextRange(DEFAULT_TEST_STRING.length),
            key = Key.MoveHome,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_numPadHome() =
        singleKeyStrokeTest(
            initSelection = TextRange(DEFAULT_TEST_STRING.length),
            key = Key.NumPadMoveHome,
            expectedSelection = TextRange(0),
        )

    @Test
    fun textField_end() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            key = Key.MoveEnd,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_numPadEnd() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            key = Key.NumPadMoveEnd,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_shiftLeft() =
        singleKeyStrokeTest(
            initSelection = TextRange(1),
            keys = Key.ShiftLeft + Key.DirectionLeft,
            expectedSelection = TextRange(1, 0),
        )

    @Test
    fun textField_shiftNumPadLeft() =
        singleKeyStrokeTest(
            initSelection = TextRange(1),
            keys = Key.ShiftLeft + Key.NumPadDirectionLeft,
            expectedSelection = TextRange(1, 0),
        )

    @Test
    fun textField_shiftRight() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.DirectionRight,
            expectedSelection = TextRange(0, 1),
        )

    @Test
    fun textField_shiftNumPadRight() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.NumPadDirectionRight,
            expectedSelection = TextRange(0, 1),
        )

    @Test
    fun textField_shiftUp() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            keys = Key.ShiftLeft + Key.DirectionUp,
            expectedSelection = TextRange(2, 0),
        )

    @Test
    fun textField_shiftNumPadUp() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            keys = Key.ShiftLeft + Key.NumPadDirectionUp,
            expectedSelection = TextRange(2, 0),
        )

    @Test
    fun textField_shiftDown() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            keys = Key.ShiftLeft + Key.DirectionDown,
            expectedSelection = TextRange(2, DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_shiftNumpadDown() =
        singleKeyStrokeTest(
            initSelection = TextRange(2),
            keys = Key.ShiftLeft + Key.NumPadDirectionDown,
            expectedSelection = TextRange(2, DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_shiftPageUp() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange("Hello\n".length),
            keys = Key.ShiftLeft + Key.PageUp,
            expectedSelection = TextRange("Hello\n".length, 0),
        )

    @Test
    fun textField_shiftNumPadPageUp() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange("Hello\n".length),
            keys = Key.ShiftLeft + Key.NumPadPageUp,
            expectedSelection = TextRange("Hello\n".length, 0),
        )

    @Test
    fun textField_shiftPageDown() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.PageDown,
            expectedSelection = TextRange(0, "Hello\n".length),
        )

    @Test
    fun textField_shiftNumPadPageDown() =
        singleKeyStrokeTest(
            initText = "Hello\nWorld",
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.PageDown,
            expectedSelection = TextRange(0, "Hello\n".length),
        )

    @Test
    fun textField_shiftHome() =
        singleKeyStrokeTest(
            initSelection = TextRange(DEFAULT_TEST_STRING.length),
            keys = Key.ShiftLeft + Key.MoveHome,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length, 0),
        )

    @Test
    fun textField_shiftNumPadHome() =
        singleKeyStrokeTest(
            initSelection = TextRange(DEFAULT_TEST_STRING.length),
            keys = Key.ShiftLeft + Key.NumPadMoveHome,
            expectedSelection = TextRange(DEFAULT_TEST_STRING.length, 0),
        )

    @Test
    fun textField_shiftEnd() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.MoveEnd,
            expectedSelection = TextRange(0, DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_shiftNumPadEnd() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            keys = Key.ShiftLeft + Key.NumPadMoveEnd,
            expectedSelection = TextRange(0, DEFAULT_TEST_STRING.length),
        )

    @Test
    fun textField_shiftInsert() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            initClipboardText = "X",
            keys = Key.ShiftLeft + Key.Insert,
            expectedText = "X$DEFAULT_TEST_STRING",
        )

    @Test
    fun textField_shiftNumPadInsert() =
        singleKeyStrokeTest(
            initSelection = TextRange(0),
            initClipboardText = "X",
            keys = Key.ShiftLeft + Key.NumPadInsert,
            expectedText = "X$DEFAULT_TEST_STRING",
        )

    @Test
    fun textField_shortcutInsert() =
        singleKeyStrokeTest(
            initSelection = TextRange(0, DEFAULT_TEST_STRING.length),
            // Ctrl needs to be replaced by platform specific key when commonizing this test
            keys = Key.CtrlLeft + Key.Insert,
            expectedClipboardText = DEFAULT_TEST_STRING,
        )

    @Test
    fun textField_shortcutNumPadInsert() =
        singleKeyStrokeTest(
            initSelection = TextRange(0, DEFAULT_TEST_STRING.length),
            // Ctrl needs to be replaced by platform specific key when commonizing this test
            keys = Key.CtrlLeft + Key.NumPadInsert,
            expectedClipboardText = DEFAULT_TEST_STRING,
        )

    private class SequenceScope(
        private val state: TextFieldState,
        private val clipboard: FakeClipboard,
        private val uiTest: ComposeUiTest,
        private val keyInjectionScope: KeyInjectionScope,
    ) : KeyInjectionScope by keyInjectionScope {

        fun press(keys: List<Key>) {
            require(keys.isNotEmpty()) { "At least one key must be specified for press action" }
            if (keys.size == 1) {
                pressKey(keys.first())
            } else {
                withKeysDown(keys.dropLast(1)) { pressKey(keys.last()) }
            }
        }

        fun expectedText(text: String) {
            uiTest.waitForIdle()
            assertThat(state.text.toString()).isEqualTo(text)
        }

        fun expectedSelection(selection: TextRange) {
            uiTest.waitForIdle()
            assertThat(state.selection).isEqualTo(selection)
        }

        suspend fun expectedClipboardText(text: String) {
            uiTest.waitForIdle()
            assertThat(clipboard.getClipEntry()?.readText()).isEqualTo(text)
        }
    }

    private fun keysSequenceTest(
        initText: String = DEFAULT_TEST_STRING,
        initSelection: TextRange = TextRange.Zero,
        initClipboardText: String? = null,
        sequence: suspend SequenceScope.() -> Unit,
    ) {
        runComposeUiTest(StandardTestDispatcher()) {
            val tag = "TextFieldTestTag"
            val state = TextFieldState(initText, initSelection)
            val clipboard = FakeClipboard(initClipboardText)
            val focusRequester = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    BasicTextField(
                        state = state,
                        modifier = Modifier.focusRequester(focusRequester).testTag(tag),
                        decorator = { it() },
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }
            waitForIdle()

            onNodeWithTag(tag).performKeyInput {
                runBlocking {
                    val scope =
                        SequenceScope(
                            state = state,
                            clipboard = clipboard,
                            uiTest = this@runComposeUiTest,
                            keyInjectionScope = this@performKeyInput,
                        )
                    scope.sequence()
                }
            }
        }
    }

    infix operator fun Key.plus(other: Key): MutableList<Key> {
        return mutableListOf(this, other)
    }

    private fun singleKeyStrokeTest(
        initText: String = DEFAULT_TEST_STRING,
        initSelection: TextRange,
        initClipboardText: String? = null,
        keys: List<Key>,
        expectedText: String? = null,
        expectedSelection: TextRange? = null,
        expectedClipboardText: String? = null,
    ) {
        keysSequenceTest(
            initText = initText,
            initSelection = initSelection,
            initClipboardText = initClipboardText,
        ) {
            press(keys)
            if (expectedText != null) {
                expectedText(expectedText)
            }
            if (expectedSelection != null) {
                expectedSelection(expectedSelection)
            }
            if (expectedClipboardText != null) {
                expectedClipboardText(expectedClipboardText)
            }
        }
    }

    private fun singleKeyStrokeTest(
        initText: String = DEFAULT_TEST_STRING,
        initSelection: TextRange,
        initClipboardText: String? = null,
        key: Key,
        expectedText: String? = null,
        expectedSelection: TextRange? = null,
        expectedClipboardText: String? = null,
    ) {
        singleKeyStrokeTest(
            initText = initText,
            initSelection = initSelection,
            initClipboardText = initClipboardText,
            keys = listOf(key),
            expectedText = expectedText,
            expectedSelection = expectedSelection,
            expectedClipboardText = expectedClipboardText,
        )
    }

    private companion object {
        const val DEFAULT_TEST_STRING = "Hello"
    }
}

internal class FakeClipboard(private var clipEntry: ClipEntry?) : Clipboard {

    constructor(text: String? = null) : this(text?.let { AnnotatedString(it).toClipEntry() })

    var getClipEntryCalled: Int = 0
        private set

    var setClipEntryCalled: Int = 0
        private set

    override suspend fun getClipEntry(): ClipEntry? {
        getClipEntryCalled++
        return clipEntry
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        setClipEntryCalled++
        this@FakeClipboard.clipEntry = clipEntry
    }

    override val nativeClipboard: NativeClipboard
        get() {
            throw UnsupportedOperationException("Native Clipboard isn't needed in tests")
        }
}
