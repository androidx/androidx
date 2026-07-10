/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink.state

import androidx.pdf.PdfDocument

/** Represents the current drawing mode for annotations. */
internal sealed interface AnnotationDrawingMode {
    /**
     * Represents the pen mode.
     *
     * @param size The size of the brush.
     * @param color The color of the brush.
     */
    data class PenMode(val size: Float, val color: Int) : AnnotationDrawingMode

    /**
     * Represents the highlighter mode.
     *
     * @param size The size of the brush.
     * @param color The color of the brush.
     */
    data class HighlighterMode(val size: Float, val color: Int, val document: PdfDocument) :
        AnnotationDrawingMode

    /** Represents the eraser mode. */
    object EraserMode : AnnotationDrawingMode
}
