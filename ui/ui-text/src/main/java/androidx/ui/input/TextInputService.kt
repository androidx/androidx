/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.input

import androidx.annotation.RestrictTo

/**
 * An interface for text input service.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface TextInputService {
    /**
     * Start text input session for given client.
     */
    fun startInput(
        initState: EditorState,
        keyboardType: KeyboardType,
        onEditCommand: (List<EditOperation>) -> Unit,
        onEditorActionPerformed: (Any) -> Unit /* TODO(nona): decide type */
    )

    /**
     * Stop text input session.
     */
    fun stopInput()

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     */
    fun showSoftwareKeyboard()

    /*
     * Notify the new editor state to IME.
     */
    fun onStateUpdated(state: EditorState)
}