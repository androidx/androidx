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

import android.os.CancellationSignal
import android.view.KeyEvent
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.PreviewableHandwritingGesture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The dependencies and actions required by a [StatelessInputConnection] connection. Decouples
 * [StatelessInputConnection] from [TextFieldState] for testability.
 */
@OptIn(ExperimentalFoundationApi::class)
internal interface TextInputSession : ImeEditCommandScope {

    /**
     * The current [TextFieldValue] in this input session. This value is typically supplied by a
     * backing [TextFieldState] that is used to initialize the session.
     */
    val text: TextFieldCharSequence

    /** Delegates IME requested KeyEvents. */
    fun sendKeyEvent(keyEvent: KeyEvent)

    /** Callback to run when IME sends an action via [InputConnection.performEditorAction] */
    fun onImeAction(imeAction: ImeAction)

    /** Callback to run when IME sends a content via [InputConnection.commitContent] */
    fun onCommitContent(transferableContent: TransferableContent): Boolean

    /** Called from [InputConnection.requestCursorUpdates]. */
    fun requestCursorUpdates(cursorUpdateMode: Int)

    /** Called from [InputConnection.performHandwritingGesture]. */
    fun performHandwritingGesture(gesture: HandwritingGesture): Int

    /** Called from [InputConnection.previewHandwritingGesture]. */
    fun previewHandwritingGesture(
        gesture: PreviewableHandwritingGesture,
        cancellationSignal: CancellationSignal?
    ): Boolean
}
