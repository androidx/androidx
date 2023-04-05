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

package androidx.compose.foundation.text2.input.internal

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class StatelessInputConnectionTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var ic: StatelessInputConnection
    private var activeSession: EditableTextInputSession? = null

    private var isOpen: Boolean = true
    private var value: TextFieldValue = TextFieldValue()
    private var imeOptions: ImeOptions = ImeOptions.Default
    private var onRequestEdits: ((List<EditCommand>) -> Unit)? = null
    private var onSendKeyEvent: ((KeyEvent) -> Unit)? = null
    private var onImeAction: ((ImeAction) -> Unit)? = null

    private val activeSessionProvider: () -> EditableTextInputSession? = { activeSession }

    @Before
    fun setup() {
        ic = StatelessInputConnection(activeSessionProvider)
        activeSession = object : EditableTextInputSession {
            override val isOpen: Boolean
                get() = this@StatelessInputConnectionTest.isOpen

            override val value: TextFieldValue
                get() = this@StatelessInputConnectionTest.value

            override val imeOptions: ImeOptions
                get() = this@StatelessInputConnectionTest.imeOptions

            override fun setFilter(filter: TextEditFilter?) {
                // Noop.
            }

            override fun showSoftwareKeyboard() {
                // noop
            }

            override fun hideSoftwareKeyboard() {
                // noop
            }

            override fun onImeAction(imeAction: ImeAction) {
                this@StatelessInputConnectionTest.onImeAction?.invoke(imeAction)
            }

            override fun requestEdits(editCommands: List<EditCommand>) {
                onRequestEdits?.invoke(editCommands)
            }

            override fun sendKeyEvent(keyEvent: KeyEvent) {
                onSendKeyEvent?.invoke(keyEvent)
            }

            override fun dispose() {
                this@StatelessInputConnectionTest.isOpen = false
            }
        }
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")

        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("Hello, World")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("ello, World")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("Hello, World")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("Hello")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("ello,")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("World")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("")
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getSelectedText(0)).isNull()

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 1)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("H")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 12)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("Hello, World")
    }

    @Test
    fun commitTextTest() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertThat(ic.commitText("Hello, ", 1)).isTrue()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(CommitTextCommand("Hello, ", 1))
    }

    @Test
    fun commitTextTest_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.commitText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(CommitTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(CommitTextCommand("World.", 1))
    }

    @Test
    fun setComposingRegion() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Mark first "H" as composition.
        assertThat(ic.setComposingRegion(0, 1)).isTrue()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetComposingRegionCommand(0, 1))
    }

    @Test
    fun setComposingRegion_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingRegion(0, 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.setComposingRegion(1, 2)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetComposingRegionCommand(0, 1))
        assertThat(editCommands[1]).isEqualTo(SetComposingRegionCommand(1, 2))
    }

    @Test
    fun setComposingTextTest() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
    }

    @Test
    fun setComposingTextTest_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two setComposingText API within the single batch
        // session. Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.setComposingText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(SetComposingTextCommand("World.", 1))
    }

    @Test
    fun deleteSurroundingText() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertTrue(ic.deleteSurroundingText(0, 6))

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextCommand(0, 6))
    }

    @Test
    fun deleteSurroundingText_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.deleteSurroundingText(0, 6)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.deleteSurroundingText(0, 5)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(DeleteSurroundingTextCommand(0, 5))
    }

    @Test
    fun deleteSurroundingTextInCodePoints() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertThat(ic.deleteSurroundingTextInCodePoints(0, 6)).isTrue()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 6))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.deleteSurroundingTextInCodePoints(0, 6)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.deleteSurroundingTextInCodePoints(0, 5)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 5))
    }

    @Test
    fun setSelection() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Select "Hello, "
        assertThat(ic.setSelection(0, 6)).isTrue()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetSelectionCommand(0, 6))
    }

    @Test
    fun setSelection_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setSelection(0, 6)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.setSelection(6, 11)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetSelectionCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(SetSelectionCommand(6, 11))
    }

    @Test
    fun finishComposingText() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Cancel any ongoing composition. In this example, there is no composition range, but
        // should record the API call
        assertTrue(ic.finishComposingText())

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(FinishComposingTextCommand)
    }

    @Test
    fun finishComposingText_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.finishComposingText()).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.finishComposingText()).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(FinishComposingTextCommand)
        assertThat(editCommands[1]).isEqualTo(FinishComposingTextCommand)
    }

    @Test
    fun mixedAPICalls_batchSession() {
        var editCommands = listOf<EditCommand>()
        var requestEditsCalled = 0
        onRequestEdits = {
            requestEditsCalled++
            editCommands = it
        }
        value = TextFieldValue(text = "", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.finishComposingText()).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.setSelection(0, 12)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(editCommands.size).isEqualTo(5)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(FinishComposingTextCommand)
        assertThat(editCommands[2]).isEqualTo(CommitTextCommand("World.", 1))
        assertThat(editCommands[3]).isEqualTo(SetSelectionCommand(0, 12))
        assertThat(editCommands[4]).isEqualTo(CommitTextCommand("", 1))
    }

    @Test
    fun closeConnection() {
        // Everything is internal and there is nothing to expect.
        // Just make sure it is not crashed by calling method.
        ic.closeConnection()
    }

    @Test
    fun do_not_callback_if_only_readonly_ops() {
        var requestEditsCalled = 0
        onRequestEdits = { requestEditsCalled++ }
        ic.beginBatchEdit()
        ic.getSelectedText(1)
        ic.endBatchEdit()
        assertThat(requestEditsCalled).isEqualTo(0)
    }

    @Test
    fun sendKeyEvent_whenIMERequests() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = {
            keyEvents += it
        }
        val keyEvent1 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0)
        val keyEvent2 = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0)
        ic.sendKeyEvent(keyEvent1)
        ic.sendKeyEvent(keyEvent2)

        assertThat(keyEvents.size).isEqualTo(2)
        assertThat(keyEvents.first()).isEqualTo(keyEvent1)
        assertThat(keyEvents.last()).isEqualTo(keyEvent2)
    }

    @Test
    fun sendKeyEvent_noActiveSession() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = {
            keyEvents += it
        }
        val keyEvent1 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0)
        val keyEvent2 = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0)
        activeSession = null

        ic.sendKeyEvent(keyEvent1)
        ic.sendKeyEvent(keyEvent2)

        assertThat(keyEvents.size).isEqualTo(0)
    }

    @Test
    fun performImeAction_whenIMERequests() {
        val receivedImeActions = mutableListOf<ImeAction>()
        onImeAction = {
            receivedImeActions += it
        }
        ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_GO)
        ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
        ic.performEditorAction(EditorInfo.IME_ACTION_NONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_PREVIOUS)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
        ic.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
        ic.performEditorAction(-1)

        assertThat(receivedImeActions).isEqualTo(listOf(
            ImeAction.Done,
            ImeAction.Go,
            ImeAction.Next,
            ImeAction.Default, // None is evaluated back to Default.
            ImeAction.Previous,
            ImeAction.Search,
            ImeAction.Send,
            ImeAction.Default, // Unspecified is evaluated back to Default.
            ImeAction.Default // Unrecognized is evaluated back to Default.
        ))
    }

    @Test
    fun performImeAction_noActiveSession() {
        val receivedImeActions = mutableListOf<ImeAction>()
        onImeAction = {
            receivedImeActions += it
        }
        activeSession = null
        ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_GO)
        ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
        ic.performEditorAction(EditorInfo.IME_ACTION_NONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_PREVIOUS)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
        ic.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
        ic.performEditorAction(-1)

        assertThat(receivedImeActions).isEqualTo(listOf<ImeAction>())
    }

    @Test
    fun debugMode_isDisabled() {
        // run this in presubmit to check that we are not accidentally enabling logs on prod
        assertFalse(
            SIC_DEBUG,
            "Oops, looks like you accidentally enabled logging. Don't worry, we've all " +
                "been there. Just remember to turn it off before you deploy your code."
        )
    }
}