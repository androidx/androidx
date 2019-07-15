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
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorState
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.input.TextInputService
import androidx.ui.painting.Canvas
import androidx.ui.text.TextPainter

internal class InputFieldDelegate {
    companion object {
        /**
         * Process text layout with given constraint.
         *
         * @param textPainter The text painter
         * @param constraints The layout constraints
         * @return the bounding box size(width and height) of the layout result
         */
        @JvmStatic
        fun layout(textPainter: TextPainter, constraints: Constraints): Pair<IntPx, IntPx> {
            textPainter.layout(constraints)
            return Pair(textPainter.width.px.round(), textPainter.height.px.round())
        }

        /**
         * Draw the text content to the canvas
         *
         * @param canvas The target canvas.
         * @param value The editor state
         * @param textPainter The text painter
         * @param hasFocus true if this widget is focused, otherwise false
         * @param editorStyle The editor style.
         */
        @JvmStatic
        fun draw(
            canvas: Canvas,
            value: EditorState,
            textPainter: TextPainter,
            hasFocus: Boolean,
            editorStyle: EditorStyle
        ) {
            value.composition?.let {
                textPainter.paintBackground(
                    it.start,
                    it.end,
                    editorStyle.compositionColor,
                    canvas,
                    Offset.zero
                )
            }
            if (value.selection.collapsed) {
                if (hasFocus) {
                    textPainter.paintCursor(value.selection.start, canvas)
                }
            } else {
                textPainter.paintBackground(
                    value.selection.start,
                    value.selection.end,
                    editorStyle.selectionColor,
                    canvas,
                    Offset.zero
                )
            }
            textPainter.paint(canvas, Offset.zero)
        }

        /**
         * Notify system that focused input area.
         *
         * System is typically scrolled up not to be covered by keyboard.
         */
        @JvmStatic
        fun notifyFocusedRect(
            value: EditorState,
            textPainter: TextPainter,
            layoutCoordinates: LayoutCoordinates,
            textInputService: TextInputService,
            hasFocus: Boolean
        ) {
            if (!hasFocus) {
                return
            }

            val bbox = textPainter.getBoundingBox(value.selection.end)
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
            onValueChange: (EditorState) -> Unit
        ) {
            onValueChange(editProcessor.onEditCommands(ops))
        }

        /**
         * Called when onPress event is fired.
         *
         * @param textInputService The text input service
         */
        @JvmStatic
        fun onPress(textInputService: TextInputService?) {
            textInputService?.showSoftwareKeyboard()
        }

        /**
         * Called when onDrag event is fired.
         *
         * @param position The event position in widget coordinate.
         */
        @JvmStatic
        fun onDragAt(position: PxPosition) {
            // TODO(nona): Implement this function
            Log.d("InputFieldDelegate", "onDrag: $position")
        }

        /**
         * Called when onRelease event is fired.
         *
         * @param position The event position in widget coordinate.
         * @param textPainter The text painter
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        fun onRelease(
            position: PxPosition,
            textPainter: TextPainter,
            editProcessor: EditProcessor,
            onValueChange: (EditorState) -> Unit
        ) {
            val offset = textPainter.getPositionForOffset(position.toOffset())
            onEditCommand(listOf(SetSelectionEditOp(offset, offset)), editProcessor, onValueChange)
        }

        /**
         * Called when the widget gained input focus
         *
         * @param textInputService The text input service
         * @param value The editor state
         * @param editProcessor The edit processor
         * @param keyboardType The keyboard type
         * @param onValueChange The callback called when the new editor state arrives.
         * @param onEditorActionPerformed The callback called when the editor action arrives.
         */
        @JvmStatic
        fun onFocus(
            textInputService: TextInputService?,
            value: EditorState,
            editProcessor: EditProcessor,
            keyboardType: KeyboardType,
            imeAction: ImeAction,
            onValueChange: (EditorState) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            textInputService?.startInput(
                initState = value,
                keyboardType = keyboardType,
                imeAction = imeAction,
                onEditCommand = { onEditCommand(it, editProcessor, onValueChange) },
                onImeActionPerformed = onImeActionPerformed)
        }

        /**
         * Called when the widget loses input focus
         *
         * @param textInputService The text input service
         */
        @JvmStatic
        fun onBlur(textInputService: TextInputService?) {
            textInputService?.stopInput()
        }
    }
}
