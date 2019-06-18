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

package androidx.ui.core.input

import androidx.compose.composer
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorState

/**
 * A composable for communicating system input method.
 */
@Composable
fun TextInputClient(
    /** Initial state of the editor */
    editorState: EditorState,

    /** Called when the InputMethodService update the editor state */
    onEditorStateChange: (EditorState) -> Unit,

    /** Clled when the InputMethod requested an editor action */
    onEditorActionPerformed: (Any) -> Unit = {}, // TODO(nona): Define argument type

    /** Called when the InputMethod forwarded a key event */
    onKeyEventForwarded: (Any) -> Unit = {}, // TODO(nona): Define argument type

    @Children children: @Composable() () -> Unit
) {
    val textInputService = +ambient(TextInputServiceAmbient)

    val proc = +memo { EditProcessor() }
    proc.onNewState(editorState)

    Focusable(onFocus = {
        textInputService?.startInput(
            initState = editorState,
            onEditCommand = {
                onEditorStateChange(proc.onEditCommands(it))
            },
            onEditorActionPerformed = onEditorActionPerformed,
            onKeyEventForwarded = onKeyEventForwarded
        )
    },
        onBlur = { textInputService?.stopInput() }
    ) {
        children()
    }
}