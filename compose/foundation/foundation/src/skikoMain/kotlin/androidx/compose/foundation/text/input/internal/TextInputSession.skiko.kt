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

import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setSelectionCoerced
import androidx.compose.foundation.text.offsetByCodePoints
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    updateSelectionState: (() -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?,
    updateTouchMode: (Boolean) -> Unit,
): Nothing {
    val editProcessor = EditProcessor()
    fun onEditCommand(commands: List<EditCommand>) {
        editProcessor.reset(
            value = state.untransformedText.toTextFieldValue(),
            textInputSession = null
        )
        val newValue = editProcessor.apply(commands)

        state.editUntransformedTextAsUser(restartImeIfContentChanges = false) {
            // Update text
            replace(0, length, newValue.text)

            // Update selection
            val selection = newValue.selection
            setSelectionCoerced(selection.start, selection.end)

            // Update composition
            val composition = newValue.composition
            if (composition == null) {
                commitComposition()
            } else {
                setComposition(composition.start, composition.end)
            }
        }
    }

    fun editText(block: TextEditingScope.() -> Unit) {
        state.editUntransformedTextAsUser(restartImeIfContentChanges = false) {
            with(TextEditingScope(this)) {
                block()
            }
        }
    }

    coroutineScope {
        fun focusedRectInRoot(): Rect? {
            val layoutResult = layoutState.layoutResult ?: return null
            val layoutCoords = layoutState.textLayoutNodeCoordinates ?: return null
            return layoutResult
                .getCursorRect(state.visualText.selection.max)
                .translate(layoutCoords.localToRoot(Offset.Zero))
        }

        fun textFieldRectInRoot() = layoutState.decoratorNodeCoordinates?.boundsInRoot()

        fun textClippingRectInRoot() = layoutState.coreNodeCoordinates?.boundsInRoot()

        fun unclippedTextOffsetInRoot() = layoutState.textLayoutNodeCoordinates?.positionInRoot()

        startInputMethod(
            SkikoPlatformTextInputMethodRequest(
                value = { state.untransformedText.toTextFieldValue() },
                state = state::untransformedText.asTextEditorState(),
                imeOptions = imeOptions,
                onEditCommand = ::onEditCommand,
                onImeAction = onImeAction,
                textLayoutResult = layoutState::layoutResult,
                focusedRectInRoot = ::focusedRectInRoot,
                textFieldRectInRoot = ::textFieldRectInRoot,
                textClippingRectInRoot = ::textClippingRectInRoot,
                unclippedTextOffsetInRoot = ::unclippedTextOffsetInRoot,
                editText = ::editText
            )
        )
    }
}

private fun TextFieldCharSequence.toTextFieldValue() =
    TextFieldValue(toString(), selection, composition)

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalComposeUiApi::class)
private inline fun (() -> TextFieldCharSequence).asTextEditorState() = object : TextEditorState {

    override val length: Int
        get() = this@asTextEditorState().length

    override fun get(index: Int): Char = this@asTextEditorState()[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return this@asTextEditorState().subSequence(startIndex, endIndex)
    }

    override val selection: TextRange
        get() = this@asTextEditorState().selection

    override val composition: TextRange?
        get() = this@asTextEditorState().composition

    override val text: String get() = this@asTextEditorState().toString()
}

@OptIn(ExperimentalComposeUiApi::class)
private fun TextEditingScope(buffer: TextFieldBuffer) = object : TextEditingScope {
    // Be careful about using TextRange.start/end, as the selection can be reversed (start > end).
    // Prefer to use TextRange.min/max.

    override fun deleteSurroundingTextInCodePoints(
        lengthBeforeCursor: Int,
        lengthAfterCursor: Int
    ) {
        val charSequence = buffer.asCharSequence()
        val selection = buffer.selection
        buffer.delete(
            start = selection.max,
            end = charSequence.offsetByCodePoints(
                index = selection.max,
                offset = lengthAfterCursor
            )
        )
        buffer.delete(
            start = charSequence.offsetByCodePoints(
                index = selection.min,
                offset = -lengthBeforeCursor
            ),
            end = selection.min
        )
    }

    override fun setSelection(start: Int, end: Int) {
        buffer.setSelectionCoerced(start, end)
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int) {
        // API description says replace ongoing composition text if there. Then, if there is no
        // composition text, insert text into the cursor position or replace selection.
        val replacementRange = buffer.composition ?: buffer.selection
        buffer.replace(replacementRange.min, replacementRange.max, text)

        val newCursor = replacementRange.min + text.length

        // See API description for the meaning of newCursorPosition.
        val newCursorInBuffer =
            if (newCursorPosition > 0) {
                newCursor + newCursorPosition - 1
            } else {
                newCursor + newCursorPosition - text.length
            }
        buffer.setSelectionCoerced(newCursorInBuffer, newCursorInBuffer)
    }

    override fun setComposingRegion(start: Int, end: Int) {
        buffer.setComposition(start, end)
    }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int) {
        val replacementRange = buffer.composition ?: buffer.selection
        // API doc says, if there is ongoing composing text, replace it with new text.
        // If there is no composing text, insert composing text into the cursor position with
        // removing selected text if any.
        buffer.replace(replacementRange.min, replacementRange.max, text)
        if (text.isNotEmpty()) {
            buffer.setComposition(replacementRange.min, replacementRange.min + text.length)
        }

        val newCursor = replacementRange.min + text.length

        // See API description for the meaning of newCursorPosition.
        val newCursorInBuffer =
            if (newCursorPosition > 0) {
                newCursor + newCursorPosition - 1
            } else {
                newCursor + newCursorPosition - text.length
            }

        buffer.setSelectionCoerced(newCursorInBuffer, newCursorInBuffer)
    }

    override fun finishComposingText() {
        buffer.commitComposition()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal data class SkikoPlatformTextInputMethodRequest(
    override val value: () -> TextFieldValue,
    override val state: TextEditorState,
    override val imeOptions: ImeOptions,
    override val onEditCommand: (List<EditCommand>) -> Unit,
    override val onImeAction: ((ImeAction) -> Unit)?,
    override val textLayoutResult: () -> TextLayoutResult?,
    override val focusedRectInRoot: () -> Rect?,
    override val textFieldRectInRoot: () -> Rect?,
    override val textClippingRectInRoot: () -> Rect?,
    override val unclippedTextOffsetInRoot: () -> Offset?,
    override val editText: (block: TextEditingScope.() -> Unit) -> Unit
): PlatformTextInputMethodRequest
