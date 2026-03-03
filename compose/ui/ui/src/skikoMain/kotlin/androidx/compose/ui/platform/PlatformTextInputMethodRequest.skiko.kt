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

package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue

actual interface PlatformTextInputMethodRequest {
    /** Returns a snapshot text field state, as a [TextFieldValue]. */
    @ExperimentalComposeUiApi
    val value: () -> TextFieldValue

    /** The text field state. */
    @ExperimentalComposeUiApi
    val state: TextEditorState

    /** Keyboard configuration such as single line, autocorrect etc. */
    @ExperimentalComposeUiApi
    val imeOptions: ImeOptions

    /** Can be called to perform edit commands on the text field state. */
    @ExperimentalComposeUiApi
    val onEditCommand: (List<EditCommand>) -> Unit

    /** The callback called when the editor action arrives. */
    @ExperimentalComposeUiApi
    val onImeAction: ((ImeAction) -> Unit)?

    /**
     * Returns the snapshot layout of text in the text field.
     *
     * `null` return values mean that the text has not been laid out yet. They may be ignored.
     */
    @ExperimentalComposeUiApi
    val textLayoutResult: () -> TextLayoutResult?

    /**
     * Returns the snapshot rectangle (relative to root) of the area where the actual editing
     * occurs.
     *
     * `null` return values mean that the text has not been laid out yet. They may be ignored.
     */
    @ExperimentalComposeUiApi
    val focusedRectInRoot: () -> Rect?

    /**
     * Returns the snapshot rectangle of the text field relative to the root layout.
     *
     * `null` return values mean that the text has not been laid out yet. They may be ignored.
     */
    @ExperimentalComposeUiApi
    val textFieldRectInRoot: () -> Rect?

    /**
     * Returns the snapshot rectangle of the text core clipping region relative to the root layout.
     * This region defines the visual bounds of the text that is currently displayed within the text
     * field.
     *
     * `null` return values mean that the text has not been laid out yet. They may be ignored.
     */
    @ExperimentalComposeUiApi
    val textClippingRectInRoot: () -> Rect?

    /**
     * Returns the top-left corner of the unclipped text block, represented as an [Offset] in the
     * root layout coordinate system. This point represents where the text begins (the (0,0) point
     * of the [TextLayoutResult]) and its position changes as the text field is scrolled.
     *
     * This offset is required to synchronize platform overlay text input views (such as those
     * implemented on iOS) with scrollable text.
     *
     * `null` return values mean that the text has not been laid out yet. They may be ignored.
     */
    @ExperimentalComposeUiApi
    val unclippedTextOffsetInRoot: () -> Offset?

    /**
     * Allows the text input service to edit the text.
     *
     * Note that changes requested in the [TextEditingScope] are buffered and applied, together, to
     * the [TextEditorState] only after `block` returns. It is therefore wrong to assume inside
     * `block` that the [TextEditorState] has changed following an invocation of a method on
     * [TextEditingScope].
     *
     * If, in the future, the editing `block` requires access to the intermediate state, this state
     * should be exposed via [TextEditingScope] itself.
     */
    @ExperimentalComposeUiApi
    val editText: (block: TextEditingScope.() -> Unit) -> Unit
}
