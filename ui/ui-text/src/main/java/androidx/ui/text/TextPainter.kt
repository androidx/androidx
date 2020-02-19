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

package androidx.ui.text

import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.text.style.TextOverflow

object TextPainter {
    /**
     * Paints the text onto the given canvas.
     *
     * @param canvas a canvas to be drawn
     * @param textLayoutResult a result of text layout
     */
    fun paint(canvas: Canvas, textLayoutResult: TextLayoutResult) {
        val needClipping = textLayoutResult.hasVisualOverflow &&
                textLayoutResult.layoutInput.overflow == TextOverflow.Clip
        if (needClipping) {
            val width = textLayoutResult.size.width.value.toFloat()
            val height = textLayoutResult.size.height.value.toFloat()
            val bounds = Rect.fromLTWH(0f, 0f, width, height)
            canvas.save()
            canvas.clipRect(bounds)
        }
        try {
            textLayoutResult.multiParagraph.paint(canvas)
        } finally {
            if (needClipping) {
                canvas.restore()
            }
        }
    }
}