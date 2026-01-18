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

package androidx.pdf.ink.util

import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.pdf.annotation.AnnotationsView
import androidx.pdf.ink.state.AnnotationDrawingMode

/** Converts this [AnnotationDrawingMode.PenMode] to a pen [Brush]. */
internal fun AnnotationDrawingMode.PenMode.toInkBrush(): Brush {
    return Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePen(),
        colorIntArgb = this.color,
        size = this.size,
        epsilon = InkDefaults.EPSILON_VALUE,
    )
}

/** Converts this [AnnotationDrawingMode.HighlighterMode] to a highlighter [Brush]. */
internal fun AnnotationDrawingMode.HighlighterMode.toInkBrush(): Brush {
    return Brush.createWithColorIntArgb(
        family = StockBrushes.highlighter(),
        colorIntArgb = this.color,
        size = this.size,
        epsilon = InkDefaults.EPSILON_VALUE,
    )
}

/**
 * Converts this [AnnotationDrawingMode.HighlighterMode] to an [AnnotationsView.HighlighterConfig].
 */
internal fun AnnotationDrawingMode.HighlighterMode.toHighlighterConfig():
    AnnotationsView.HighlighterConfig {
    return AnnotationsView.HighlighterConfig(color = this.color, pdfDocument = this.document)
}
