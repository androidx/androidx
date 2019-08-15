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

import android.util.Log
import androidx.ui.engine.geometry.Rect
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorModel
import androidx.ui.input.EditorStyle
import androidx.ui.input.FinishComposingTextEditOp
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.OffsetMap
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.input.TextInputService
import androidx.ui.input.TransformedText
import androidx.ui.input.VisualTransformation
import androidx.ui.input.identityOffsetMap
import androidx.ui.painting.Canvas
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextDelegate

internal class TextFieldDelegate {
    companion object {
        /**
         * Process text layout with given constraint.
         *
         * @param textDelegate The text painter
         * @param constraints The layout constraints
         * @return the bounding box size(width and height) of the layout result
         */
        @JvmStatic
        fun layout(textDelegate: TextDelegate, constraints: Constraints): Pair<IntPx, IntPx> {
            val isEmptyText = textDelegate.text?.text?.isEmpty() ?: true
            val activeTextPainter = if (isEmptyText) {
                // Even with empty text, edit filed must have at least non-zero height widget. Use
                // "H" height for this empty text height.
                TextDelegate(
                    text = AnnotatedString(text = "H"),
                    style = textDelegate.style,
                    density = textDelegate.density,
                    resourceLoader = textDelegate.resourceLoader
                ).apply {
                    layout(constraints)
                }
            } else {
                textDelegate
            }

            // We anyway need to compute layout for preventing NPE during draw which require layout
            // result.
            // TODO(nona): Fix this?
            textDelegate.layout(constraints)

            val height = activeTextPainter.height.px.round()
            val width = constraints.maxWidth
            return Pair(width, height)
        }

        /**
         * Draw the text content to the canvas
         *
         * @param canvas The target canvas.
         * @param value The editor state
         * @param offsetMap The offset map
         * @param textDelegate The text painter
         * @param hasFocus true if this widget is focused, otherwise false
         * @param editorStyle The editor style.
         */
        @JvmStatic
        fun draw(
            canvas: Canvas,
            value: EditorModel,
            offsetMap: OffsetMap,
            textDelegate: TextDelegate,
            hasFocus: Boolean,
            editorStyle: EditorStyle
        ) {
            value.composition?.let {
                textDelegate.paintBackground(
                    offsetMap.originalToTransformed(it.start),
                    offsetMap.originalToTransformed(it.end),
                    editorStyle.compositionColor,
                    canvas
                )
            }
            if (value.selection.collapsed) {
                if (hasFocus) {
                    textDelegate.paintCursor(
                        offsetMap.originalToTransformed(value.selection.start), canvas)
                }
            } else {
                textDelegate.paintBackground(
                    offsetMap.originalToTransformed(value.selection.start),
                    offsetMap.originalToTransformed(value.selection.end),
                    editorStyle.selectionColor,
                    canvas
                )
            }
            textDelegate.paint(canvas)
        }

        /**
         * Notify system that focused input area.
         *
         * System is typically scrolled up not to be covered by keyboard.
         */
        @JvmStatic
        fun notifyFocusedRect(
            value: EditorModel,
            textDelegate: TextDelegate,
            layoutCoordinates: LayoutCoordinates,
            textInputService: TextInputService,
            hasFocus: Boolean,
            offsetMap: OffsetMap
        ) {
            if (!hasFocus) {
                return
            }

            val bbox = if (value.selection.end < value.text.length) {
                textDelegate.getBoundingBox(offsetMap.originalToTransformed(value.selection.end))
            } else if (value.selection.end != 0) {
                textDelegate.getBoundingBox(
                    offsetMap.originalToTransformed(value.selection.end) - 1)
            } else {
                Rect(0f, 0f, 1.0f, textDelegate.preferredLineHeight)
            }
            val globalLT = layoutCoordinates.localToRoot(PxPosition(bbox.left.px, bbox.top.px))

            textInputService.notifyFocusedRect(
                Rect.fromLTWH(
                    globalLT.x.value,
                    globalLT.y.value,
                    bbox.width,
                    bbox.height
                )
            )
        }

        /**
         * Called when edit operations are passed from TextInputService
         *
         * @param ops A list of edit operations.
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        internal fun onEditCommand(
            ops: List<EditOperation>,
            editProcessor: EditProcessor,
            onValueChange: (EditorModel) -> Unit
        ) {
            onValueChange(editProcessor.onEditCommands(ops))
        }

        /**
         * Called when onDrag event is fired.
         *
         * @param position The event position in widget coordinate.
         */
        @JvmStatic
        fun onDragAt(position: PxPosition) {
            // TODO(nona): Implement this function
            Log.d("TextFieldDelegate", "onDrag: $position")
        }

        /**
         * Called when onRelease event is fired.
         *
         * @param position The event position in widget coordinate.
         * @param textDelegate The text painter
         * @param editProcessor The edit processor
         * @param offsetMap The offset map
         * @param onValueChange The callback called when the new editor state arrives.
         * @param textInputService The text input service
         * @param hasFocus True if the widget has input focus, otherwise false.
         */
        @JvmStatic
        fun onRelease(
            position: PxPosition,
            textDelegate: TextDelegate,
            editProcessor: EditProcessor,
            offsetMap: OffsetMap,
            onValueChange: (EditorModel) -> Unit,
            textInputService: TextInputService?,
            hasFocus: Boolean
        ) {
            textInputService?.showSoftwareKeyboard()
            if (hasFocus) {
                val offset = offsetMap.transformedToOriginal(
                    textDelegate.getOffsetForPosition(position))
                onEditCommand(
                    listOf(SetSelectionEditOp(offset, offset)),
                    editProcessor,
                    onValueChange)
            }
        }

        /**
         * Called when the widget gained input focus
         *
         * @param textInputService The text input service
         * @param value The editor state
         * @param editProcessor The edit processor
         * @param keyboardType The keyboard type
         * @param onValueChange The callback called when the new editor state arrives.
         * @param onImeActionPerformed The callback called when the editor action arrives.
         */
        @JvmStatic
        fun onFocus(
            textInputService: TextInputService?,
            value: EditorModel,
            editProcessor: EditProcessor,
            keyboardType: KeyboardType,
            imeAction: ImeAction,
            onValueChange: (EditorModel) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            textInputService?.startInput(
                initModel = value,
                keyboardType = keyboardType,
                imeAction = imeAction,
                onEditCommand = { onEditCommand(it, editProcessor, onValueChange) },
                onImeActionPerformed = onImeActionPerformed)
        }

        /**
         * Called when the widget loses input focus
         *
         * @param textInputService The text input service
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        fun onBlur(
            textInputService: TextInputService?,
            editProcessor: EditProcessor,
            onValueChange: (EditorModel) -> Unit
        ) {
            onEditCommand(listOf(FinishComposingTextEditOp()), editProcessor, onValueChange)
            textInputService?.stopInput()
        }

        /**
         * Helper function of applying visual transformation method to the EditorModel.
         *
         * @param value An editor state
         * @param visualTransformation A visual transformation
         */
        @JvmStatic
        fun applyVisualFilter(
            value: EditorModel,
            visualTransformation: VisualTransformation?
        ): TransformedText {
            val annotatedString = AnnotatedString(value.text)
            return visualTransformation?.filter(annotatedString)
                    ?: TransformedText(annotatedString, identityOffsetMap)
        }
    }
}
