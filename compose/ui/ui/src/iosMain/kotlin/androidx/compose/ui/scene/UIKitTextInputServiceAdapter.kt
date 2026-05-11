/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Mimics the functionality of the LegacyPlatformTextInputServiceAdapter. Used to provide backward
 * compatibility for the [androidx.compose.ui.text.input.PlatformTextInputService].
 */
@Suppress("DEPRECATION")
internal class UIKitTextInputServiceAdapter(
    private val textInputService: UIKitTextInputService,
    private val coroutineScope: CoroutineScope,
): androidx.compose.ui.text.input.PlatformTextInputService {

    override fun showSoftwareKeyboard() = textInputService.showSoftwareKeyboard()

    override fun hideSoftwareKeyboard() = textInputService.hideSoftwareKeyboard()

    private var job: Job? = null

    private var textFieldValue by mutableStateOf(TextFieldValue())
    private var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    private var focusedRectInRoot by mutableStateOf(Rect.Zero)
    private var textFieldRectInRoot by mutableStateOf(Rect.Zero)
    private var textClippingRectInRoot by mutableStateOf(Rect.Zero)
    private var unclippedTextOffsetInRoot by mutableStateOf(Offset.Zero)

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        textFieldValue = value
        job = coroutineScope.launch {
            textInputService.startInputMethod(
                makeRequest(
                    imeOptions = imeOptions,
                    onEditCommand = onEditCommand,
                    onImeActionPerformed = onImeActionPerformed
                )
            )
        }
    }

    override fun stopInput() {
        job?.cancel()
        job = null
        textFieldValue = TextFieldValue()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        this.textFieldValue = newValue
    }

    override fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
        this.textFieldValue = textFieldValue
        this.textLayoutResult = textLayoutResult

        val matrix = Matrix().also { textFieldToRootTransform(it) }
        textFieldRectInRoot = matrix.map(decorationBoxBounds)
        textClippingRectInRoot = matrix.map(innerTextFieldBounds)
        val cursorOffset = offsetMapping.originalToTransformed(textFieldValue.selection.max)
        focusedRectInRoot = matrix.map(textLayoutResult.getCursorRect(cursorOffset))
        unclippedTextOffsetInRoot = textClippingRectInRoot.topLeft - innerTextFieldBounds.topLeft
    }

    private fun makeRequest(
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ): PlatformTextInputMethodRequest {
        val textEditorState = object : TextEditorState {
            override val selection: TextRange get() = textFieldValue.selection
            override val composition: TextRange? get() = textFieldValue.composition
            override val length: Int get() = textFieldValue.text.length
            override fun get(index: Int): Char = textFieldValue.text[index]
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
                textFieldValue.text.subSequence(startIndex, endIndex)
            override val text: String get() = textFieldValue.text
        }

        return object : PlatformTextInputMethodRequest {
            override val value: () -> TextFieldValue get() = { textFieldValue }
            override val state get() = textEditorState
            override val imeOptions: ImeOptions get() = imeOptions
            override val onEditCommand: (List<EditCommand>) -> Unit get() = onEditCommand
            override val onImeAction: (ImeAction) -> Unit get() = onImeActionPerformed
            override val textLayoutResult: () -> TextLayoutResult?
                get() = { this@UIKitTextInputServiceAdapter.textLayoutResult }
            override val focusedRectInRoot: () -> Rect?
                get() = { this@UIKitTextInputServiceAdapter.focusedRectInRoot }
            override val textFieldRectInRoot: () -> Rect?
                get() = { this@UIKitTextInputServiceAdapter.textFieldRectInRoot }
            override val textClippingRectInRoot: () -> Rect?
                get() = { this@UIKitTextInputServiceAdapter.textClippingRectInRoot }
            override val unclippedTextOffsetInRoot: () -> Offset?
                get() = { this@UIKitTextInputServiceAdapter.unclippedTextOffsetInRoot }
            override val editText: (block: TextEditingScope.() -> Unit) -> Unit
                get() = { block ->
                    val commands = mutableListOf<EditCommand>()

                    with(TextEditingScope(commands)) {
                        block()
                        onEditCommand(commands)
                    }
                }
        }
    }
}

private fun TextEditingScope(commands: MutableList<EditCommand>) = object : TextEditingScope {
    override fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int) {
        commands.add(DeleteSurroundingTextCommand(lengthBeforeCursor, lengthAfterCursor))
    }

    override fun setSelection(start: Int, end: Int) {
        commands.add(SetSelectionCommand(start, end))
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int) {
        commands.add(CommitTextCommand(text.toString(), newCursorPosition))
    }

    override fun setComposingRegion(start: Int, end: Int) {
        commands.add(SetComposingRegionCommand(start, end))
    }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int) {
        commands.add(SetComposingTextCommand(text.toString(), newCursorPosition))
    }

    override fun finishComposingText() {
        commands.add(FinishComposingTextCommand())
    }
}
