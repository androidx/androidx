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

package androidx.compose.foundation.text2.service

import android.view.inputmethod.InputConnection
import androidx.compose.foundation.text2.TextFieldState
import androidx.compose.foundation.text2.input.EditCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Represents a disposable text input session that starts when an editable BasicTextField2 gains
 * focus. [TextInputSession] is the main interface for BasicTextField2 to interact with
 * IME. A session is destroyed when text input is no longer active.
 */
internal interface TextInputSession {

    /**
     * Whether this session is still active.
     *
     * This value can only go through two phases. It starts as true and becomes false when session
     * is destroyed. It can never become true again so a destroyed session should always be cleared
     * from memory.
     */
    val isOpen: Boolean

    /**
     * Destroy this session and clear resources.
     */
    fun dispose()
}

/**
 * Extended [TextInputSession] that handles [EditCommand]s and keeps track of current
 * [TextFieldValue]. This interface meant to be completely internal to [AndroidTextInputAdapter].
 * Please use [TextInputSession] to manage focus from the editor.
 */
internal interface EditableTextInputSession : TextInputSession {

    /**
     * The current [TextFieldValue] in this input session. This value is typically supplied by a
     * backing [TextFieldState] that is used to initialize the session.
     */
    val value: TextFieldValue

    /**
     * Callback to execute for InputConnection to communicate the changes requested by the IME.
     */
    fun requestEdits(editCommands: List<EditCommand>)

    /**
     * IME configuration to use when creating new [InputConnection]s while this session is active.
     */
    val imeOptions: ImeOptions
}
