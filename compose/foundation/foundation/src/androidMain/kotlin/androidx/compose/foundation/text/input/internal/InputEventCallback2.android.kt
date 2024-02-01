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

package androidx.compose.foundation.text.input.internal

import android.view.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction

/**
 * An interface of listening IME events.
 */
internal interface InputEventCallback2 {
    /**
     * Called when IME sends some input events.
     *
     * @param editCommands The list of edit commands.
     */
    fun onEditCommands(editCommands: List<EditCommand>)

    /**
     * Called when IME triggered IME action.
     *
     * @param imeAction An IME action.
     */
    fun onImeAction(imeAction: ImeAction)

    /**
     * Called when IME triggered a KeyEvent
     */
    fun onKeyEvent(event: KeyEvent)

    /**
     * Called when IME requests cursor information updates.
     *
     * @see CursorAnchorInfoController.requestUpdate
     */
    fun onRequestCursorAnchorInfo(
        immediate: Boolean,
        monitor: Boolean,
        includeInsertionMarker: Boolean,
        includeCharacterBounds: Boolean,
        includeEditorBounds: Boolean,
        includeLineBounds: Boolean
    )

    /**
     * Called when IME closed the input connection.
     *
     * @param inputConnection a closed input connection
     */
    fun onConnectionClosed(inputConnection: RecordingInputConnection)
}
