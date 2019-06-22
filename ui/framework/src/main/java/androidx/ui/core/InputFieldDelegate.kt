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

import androidx.ui.engine.geometry.Offset
import androidx.ui.input.EditorState
import androidx.ui.painting.Canvas
import androidx.ui.painting.TextPainter

/**
 * Delegate class of the UI implementation of the InputField.
 */
internal class InputFieldDelegate(
    /**
     * A text painter used for this InputField
     */
    val textPainter: TextPainter
) {

    /**
     * Process text layout with given constraint.
     *
     * @param constraints the layout constraints
     * @return the bounding box size(width and height) of the layout result
     */
    fun layout(constraints: Constraints): Pair<IntPx, IntPx> {
        textPainter.layout(constraints)
        return Pair(textPainter.width.px.round(), textPainter.height.px.round())
    }

    /**
     * Draw the text content to the canvas
     *
     * @param canvas the target canvas.
     * @param value the editor state.
     * @param editorStyle the editor style.
     */
    fun draw(canvas: Canvas, value: EditorState, editorStyle: EditorStyle) {
        if (value.selection.collapsed) {
            textPainter.paintCursor(value.selection.start, canvas)
        } else {
            textPainter.paintBackground(
                value.selection.start,
                value.selection.end,
                editorStyle.selectionColor,
                canvas,
                Offset.zero
            )
        }

        value.composition?.let {
            textPainter.paintBackground(
                it.start,
                it.end,
                editorStyle.compositionColor,
                canvas,
                Offset.zero
            )
        }
        textPainter.paint(canvas, Offset.zero)
    }
}