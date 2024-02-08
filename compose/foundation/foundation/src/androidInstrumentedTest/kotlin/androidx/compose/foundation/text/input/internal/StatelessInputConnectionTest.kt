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

package androidx.compose.foundation.text.input.internal

import android.content.ClipDescription
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

        override fun requestEdit(
            notifyImeOfChanges: Boolean,
            block: EditingBuffer.() -> Unit
        ) {
            onRequestEdit?.invoke(notifyImeOfChanges, block)
        }

        override fun sendKeyEvent(keyEvent: KeyEvent) {
            onSendKeyEvent?.invoke(keyEvent)
        }

        override fun requestCursorUpdates(cursorUpdateMode: Int) {
        }

        override fun onCommitContent(transferableContent: TransferableContent): Boolean {
            return this@StatelessInputConnectionTest.onCommitContent?.invoke(transferableContent)
                ?: false
        }
    }

    private var state: TextFieldState = TextFieldState()
    private var value: TextFieldCharSequence = TextFieldCharSequence()
        set(value) {
            field = value
            state = TextFieldState(value.toString(), value.selectionInChars)
        }
    private var onRequestEdit: ((Boolean, EditingBuffer.() -> Unit) -> Unit)? = null
    private var onSendKeyEvent: ((KeyEvent) -> Unit)? = null
    private var onImeAction: ((ImeAction) -> Unit)? = null
    private var onCommitContent: ((TransferableContent) -> Boolean)? = null

    @Before
    fun setup() {
        ic = StatelessInputConnection(activeSession, EditorInfo())
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        Truth.assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        Truth.assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")

        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        Truth.assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        Truth.assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("Hello, World")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(1)
        )

        Truth.assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("H")
        Truth.assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("ello, World")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(12)
        )

        Truth.assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("Hello, World")
        Truth.assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        Truth.assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("")
        Truth.assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("Hello")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(1)
        )

        Truth.assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("H")
        Truth.assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("ello,")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(12)
        )

        Truth.assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("World")
        Truth.assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("")
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        Truth.assertThat(ic.getSelectedText(0)).isNull()

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(0, 1)
        )

        Truth.assertThat(ic.getSelectedText(0)).isEqualTo("H")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(
            text = "Hello, World",
            selection = TextRange(0, 12)
        )

        Truth.assertThat(ic.getSelectedText(0)).isEqualTo("Hello, World")
    }

    @Test
    fun commitTextTest_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = { _, block ->
            requestEditsCalled++
            state.mainBuffer.block()
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        Truth.assertThat(ic.commitText("Hello, ", 1)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        Truth.assertThat(ic.commitText("World.", 1)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        Truth.assertThat(requestEditsCalled).isEqualTo(1)
        Truth.assertThat(state.mainBuffer.toString()).isEqualTo("Hello, World.")
        Truth.assertThat(state.mainBuffer.selection).isEqualTo(TextRange(13))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_parsesToTransferableContent() {
        var transferableContent: TransferableContent? = null
        onCommitContent = {
            transferableContent = it
            true
        }
        val contentUri = Uri.parse("content://com.example/content")
        val linkUri = Uri.parse("https://example.com")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val extras = Bundle().apply { putString("key", "value") }
        val result = ic.commitContent(
            InputContentInfo(contentUri, description, linkUri),
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            extras
        )

        Truth.assertThat(transferableContent).isNotNull()
        Truth.assertThat(transferableContent?.clipEntry).isNotNull()
        Truth.assertThat(transferableContent?.clipEntry?.firstUriOrNull()).isEqualTo(contentUri)
        Truth.assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
        Truth.assertThat(transferableContent?.clipMetadata?.clipDescription)
            .isSameInstanceAs(description)

        Truth.assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
        Truth.assertThat(transferableContent?.platformTransferableContent?.linkUri)
            .isEqualTo(linkUri)
        Truth.assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("key")
        Truth.assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("EXTRA_INPUT_CONTENT_INFO")

        assertTrue(result)
    }

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_returnsResultIfFalse() {
        onCommitContent = {
            false
        }
        val contentUri = Uri.parse("content://com.example/content")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val result = ic.commitContent(InputContentInfo(contentUri, description), 0, null)

        assertFalse(result)
    }

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_returnsFalseWhenNotDefined() {
        onCommitContent = null
        val contentUri = Uri.parse("content://com.example/content")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val result = ic.commitContent(InputContentInfo(contentUri, description), 0, null)

        assertFalse(result)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(maxSdkVersion = 24)
    @Test
    fun performPrivateCommand_parsesToTransferableContent() {
        var transferableContent: TransferableContent? = null
        onCommitContent = {
            transferableContent = it
            true
        }

        val editorInfo = EditorInfo()
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("text/plain"))

        ic = StatelessInputConnection(activeSession, editorInfo)

        val contentUri = Uri.parse("content://com.example/content")
        val linkUri = Uri.parse("https://example.com")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val extras = Bundle().apply { putString("key", "value") }
        // this will internally call performPrivateCommand when SDK <= 24
        val result = InputConnectionCompat.commitContent(
            ic,
            editorInfo,
            InputContentInfoCompat(contentUri, description, linkUri),
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            extras
        )

        Truth.assertThat(transferableContent).isNotNull()
        Truth.assertThat(transferableContent?.clipEntry).isNotNull()
        Truth.assertThat(transferableContent?.clipEntry?.firstUriOrNull()).isEqualTo(contentUri)
        Truth.assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
        Truth.assertThat(transferableContent?.clipMetadata?.clipDescription)
            .isSameInstanceAs(description)

        Truth.assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
        Truth.assertThat(transferableContent?.platformTransferableContent?.linkUri)
            .isEqualTo(linkUri)
        Truth.assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("key")
        // Permissions do not exist below SDK 25
        Truth.assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .doesNotContain("EXTRA_INPUT_CONTENT_INFO")

        assertTrue(result)
    }

    @Test
    fun mixedAPICalls_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = { _, block ->
            requestEditsCalled++
            state.mainBuffer.block()
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        Truth.assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        Truth.assertThat(ic.finishComposingText()).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        Truth.assertThat(ic.commitText("World.", 1)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        Truth.assertThat(ic.setSelection(0, 12)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        Truth.assertThat(ic.commitText("", 1)).isTrue()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        Truth.assertThat(requestEditsCalled).isEqualTo(1)
        Truth.assertThat(state.mainBuffer.toString()).isEqualTo(".")
        Truth.assertThat(state.mainBuffer.selection).isEqualTo(TextRange(0))
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
        onRequestEdit = { _, _ -> requestEditsCalled++ }
        ic.beginBatchEdit()
        ic.getSelectedText(1)
        ic.endBatchEdit()
        Truth.assertThat(requestEditsCalled).isEqualTo(0)
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

        Truth.assertThat(keyEvents.size).isEqualTo(2)
        Truth.assertThat(keyEvents.first()).isEqualTo(keyEvent1)
        Truth.assertThat(keyEvents.last()).isEqualTo(keyEvent2)
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

        Truth.assertThat(receivedImeActions).isEqualTo(listOf(
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
    fun selectAll_contextMenuAction_triggersSelectionAndImeNotification() {
        value = TextFieldCharSequence("Hello")
        var callCount = 0
        var isNotifyIme = false
        onRequestEdit = { notify, block ->
            isNotifyIme = notify
            callCount++
            state.mainBuffer.block()
        }

        ic.performContextMenuAction(android.R.id.selectAll)

        Truth.assertThat(callCount).isEqualTo(1)
        Truth.assertThat(isNotifyIme).isTrue()
        Truth.assertThat(state.mainBuffer.selection).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun cut_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.cut)

        Truth.assertThat(keyEvents.size).isEqualTo(2)
        Truth.assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        Truth.assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_CUT)
        Truth.assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        Truth.assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_CUT)
    }

    @Test
    fun copy_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.copy)

        Truth.assertThat(keyEvents.size).isEqualTo(2)
        Truth.assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        Truth.assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_COPY)
        Truth.assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        Truth.assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_COPY)
    }

    @Test
    fun paste_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.paste)

        Truth.assertThat(keyEvents.size).isEqualTo(2)
        Truth.assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        Truth.assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_PASTE)
        Truth.assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        Truth.assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_PASTE)
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
