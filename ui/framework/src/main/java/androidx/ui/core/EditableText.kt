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
package androidx.ui.core

import androidx.ui.core.input.TextInputClient
import androidx.ui.input.EditorState
import androidx.ui.graphics.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Data class holding text display attributes used for editors.
 */
data class EditorStyle(
    /** The  editor text style */
    val textStyle: TextStyle,

    /**
     * The composition background color
     *
     * @see EditorState.composition
     */
    val compositionColor: Color = Color(alpha = 0xFF, red = 0xB0, green = 0xE0, blue = 0xE6),

    /**
     *  The selection background color
     *
     *  @see EditorState.selection
     */
    // TODO(nona): share with Text.DEFAULT_SELECTION_COLOR
    val selectionColor: Color = Color(alpha = 0x66, red = 0x33, green = 0xB5, blue = 0xE5)
)

internal fun makeEditorDisplayText(editorState: EditorState, editorStyle: EditorStyle): TextSpan {
    val compositionTextStyle =
        editorStyle.textStyle.merge(TextStyle(background = editorStyle.compositionColor))
    // TODO(nona): Implement selection highlight
    return editorState.composition?.let {
        TextSpan(
            children = listOf(
                TextSpan(
                    text = editorState.text.substring(TextRange(0, it.start)),
                    style = editorStyle.textStyle
                ),
                TextSpan(
                    text = editorState.text.substring(it),
                    style = compositionTextStyle
                ),
                TextSpan(
                    text = editorState.text.substring(it.end),
                    style = editorStyle.textStyle
                )
            )
        )
    } ?: TextSpan(text = editorState.text, style = editorStyle.textStyle)
}

/**
 * A default implementation of EditableText
 *
 * To make EditableText work with platoform input service, you must keep the editor state and update
 * in [onValueChagne] callback.
 *
 * Example:
 *     var state = +state { EditorState() }
 *     EditableText(
 *         value = state.value,
 *         onValueChange = { state.value = it })
 */
@Composable
fun EditableText(
    /** Initial editor state value */
    value: EditorState,

    /** The editor style */
    editorStyle: EditorStyle,

    /** Called when the InputMethodService update the editor state */
    onValueChange: (EditorState) -> Unit = {},

    /** Called when the InputMethod requested an editor action */
    onEditorActionPerformed: (Any) -> Unit = {}, // TODO(nona): Define argument type

    /** Called when the InputMethod forwarded a key event */
    onKeyEventForwarded: (Any) -> Unit = {} // TODO(nona): Define argument type
) {
    TextInputClient(
        editorState = value,
        onEditorStateChange = onValueChange,
        onEditorActionPerformed = onEditorActionPerformed,
        onKeyEventForwarded = onKeyEventForwarded
    ) {
        Text(text = makeEditorDisplayText(value, editorStyle))
    }
}
