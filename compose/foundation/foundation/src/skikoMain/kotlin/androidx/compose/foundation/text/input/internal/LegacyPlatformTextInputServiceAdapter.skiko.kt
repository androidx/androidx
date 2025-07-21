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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import kotlinx.coroutines.Job

// TODO remove after https://youtrack.jetbrains.com/issue/COMPOSE-740/Implement-BasicTextField2
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun legacyTextInputServiceAdapterAndService():
    Pair<LegacyPlatformTextInputServiceAdapter, TextInputService> {
    return remember {
        val adapter = object : LegacyPlatformTextInputServiceAdapter() {
            private var job: Job? = null

            private var textFieldValue by mutableStateOf(TextFieldValue())
            private var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
            private var focusedRectInRoot by mutableStateOf(Rect.Zero)
            private var textFieldRectInRoot by mutableStateOf(Rect.Zero)
            private var textClippingRectInRoot by mutableStateOf(Rect.Zero)

            override fun startInput(
                value: TextFieldValue,
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ) {
                textFieldValue = value
                val node = textInputModifierNode ?: return

                job = node.launchTextInputSession {
                    startInputMethod(
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
            }

            override fun startStylusHandwriting() {}

            private fun makeRequest(
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ): SkikoPlatformTextInputMethodRequest {
                val textEditorState = object : TextEditorState {
                    override val selection: TextRange get() = textFieldValue.selection
                    override val composition: TextRange? get() = textFieldValue.composition
                    override val length: Int get() = textFieldValue.text.length
                    override fun get(index: Int): Char = textFieldValue.text[index]
                    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
                        textFieldValue.text.subSequence(startIndex, endIndex)
                }

                val editBlock: (block: TextEditingScope.() -> Unit) -> Unit = { block ->
                    object : TextEditingScope {
                        fun runOnEditCommand(command: EditCommand) {
                            onEditCommand(listOf(command))
                        }

                        override fun deleteSurroundingTextInCodePoints(
                            lengthBeforeCursor: Int,
                            lengthAfterCursor: Int
                        ) {
                            runOnEditCommand(
                                DeleteSurroundingTextCommand(lengthBeforeCursor, lengthAfterCursor)
                            )
                        }

                        override fun commitText(
                            text: CharSequence,
                            newCursorPosition: Int
                        ) {
                            runOnEditCommand(
                                CommitTextCommand(text.toString(), newCursorPosition)
                            )
                        }

                        override fun setComposingText(
                            text: CharSequence,
                            newCursorPosition: Int
                        ) {
                            runOnEditCommand(
                                SetComposingTextCommand(text.toString(), newCursorPosition)
                            )
                        }

                        override fun finishComposingText() {
                            runOnEditCommand(
                                FinishComposingTextCommand()
                            )
                        }
                    }.block()
                }

                return SkikoPlatformTextInputMethodRequest(
                    value = { textFieldValue },
                    state = textEditorState,
                    imeOptions = imeOptions,
                    onEditCommand = onEditCommand,
                    onImeAction = onImeActionPerformed,
                    textLayoutResult = { textLayoutResult },
                    focusedRectInRoot = { focusedRectInRoot },
                    textFieldRectInRoot = { textFieldRectInRoot },
                    textClippingRectInRoot = { textClippingRectInRoot },
                    editText = editBlock
                )
            }
        }
        adapter to TextInputService(adapter)
    }
}
