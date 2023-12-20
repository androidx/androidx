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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input.internal

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The dependencies and actions required by a [StatelessInputConnection] connection. Decouples
 * [StatelessInputConnection] from [TextFieldState] for testability.
 */
internal interface TextInputSession {

    /**
     * The current [TextFieldValue] in this input session. This value is typically supplied by a
     * backing [TextFieldState] that is used to initialize the session.
     */
    val text: TextFieldCharSequence

    /**
     * Callback to execute for InputConnection to communicate the changes requested by the IME.
     */
    fun requestEdit(block: EditingBuffer.() -> Unit)

    /**
     * Delegates IME requested KeyEvents.
     */
    fun sendKeyEvent(keyEvent: KeyEvent)

    /**
     * Callback to run when IME sends an action via [InputConnection.performEditorAction]
     */
    fun onImeAction(imeAction: ImeAction)
}
