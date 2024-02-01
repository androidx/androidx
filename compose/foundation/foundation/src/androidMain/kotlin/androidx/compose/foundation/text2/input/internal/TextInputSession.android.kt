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
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The dependencies and actions required by a [StatelessInputConnection] connection. Decouples
 * [StatelessInputConnection] from [TextFieldState] for testability.
 */
@OptIn(ExperimentalFoundationApi::class)
internal interface TextInputSession {

    /**
     * The current [TextFieldValue] in this input session. This value is typically supplied by a
     * backing [TextFieldState] that is used to initialize the session.
     */
    val text: TextFieldCharSequence

    /**
     * Callback to execute for InputConnection to communicate the changes requested by the IME.
     *
     * @param notifyImeOfChanges Normally any request coming from IME should not be
     * back-communicated but [InputConnection.performContextMenuAction] does not behave like a
     * regular IME command. Its changes must be resent to IME to keep it in sync with
     * [TextFieldState].
     * @param block Lambda scoped to an EditingBuffer to apply changes direct onto a buffer.
     */
    fun requestEdit(notifyImeOfChanges: Boolean = false, block: EditingBuffer.() -> Unit)

    /**
     * Delegates IME requested KeyEvents.
     */
    fun sendKeyEvent(keyEvent: KeyEvent)

    /**
     * Callback to run when IME sends an action via [InputConnection.performEditorAction]
     */
    fun onImeAction(imeAction: ImeAction)

    /**
     * Callback to run when IME sends a content via [InputConnection.commitContent]
     */
    fun onCommitContent(transferableContent: TransferableContent): Boolean

    /**
     * Called from [InputConnection.requestCursorUpdates].
     */
    fun requestCursorUpdates(cursorUpdateMode: Int)
}
