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
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
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
    private val activeSession: TextInputSession = object : TextInputSession {
        override val text: TextFieldCharSequence
            get() = this@StatelessInputConnectionTest.value

        override fun onImeAction(imeAction: ImeAction) {
            this@StatelessInputConnectionTest.onImeAction?.invoke(imeAction)
        }

        override fun requestEdit(block: EditingBuffer.() -> Unit) {
            onRequestEdit?.invoke(block)
        }

        override fun sendKeyEvent(keyEvent: KeyEvent) {
            onSendKeyEvent?.invoke(keyEvent)
        }
    }

    private var state: TextFieldState = TextFieldState()
    private var value: TextFieldCharSequence = TextFieldCharSequence()
        set(value) {
            field = value
            state = TextFieldState(value.toString(), value.selectionInChars)
        }
    private var onRequestEdit: ((EditingBuffer.() -> Unit) -> Unit)? = null
    private var onSendKeyEvent: ((KeyEvent) -> Unit)? = null
    private var onImeAction: ((ImeAction) -> Unit)? = null

    @Before
    fun setup() {
        ic = StatelessInputConnection(activeSession)
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")

        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("Hello, World")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("ello, World")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("Hello, World")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("Hello")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("ello,")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("World")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("")
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getSelectedText(0)).isNull()

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(0, 1)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("H")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(0, 12)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("Hello, World")
    }

    @Test
    fun commitTextTest_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = {
            requestEditsCalled++
            state.mainBuffer.it()
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.commitText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(state.mainBuffer.toString()).isEqualTo("Hello, World.")
        assertThat(state.mainBuffer.selection).isEqualTo(TextRange(13))
    }

    @Test
    fun mixedAPICalls_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = {
            requestEditsCalled++
            state.mainBuffer.it()
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

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
        assertThat(state.mainBuffer.toString()).isEqualTo(".")
        assertThat(state.mainBuffer.selection).isEqualTo(TextRange(0))
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
        onRequestEdit = { requestEditsCalled++ }
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
    fun debugMode_isDisabled() {
        // run this in presubmit to check that we are not accidentally enabling logs on prod
        assertFalse(
            SIC_DEBUG,
            "Oops, looks like you accidentally enabled logging. Don't worry, we've all " +
                "been there. Just remember to turn it off before you deploy your code."
        )
    }
}
