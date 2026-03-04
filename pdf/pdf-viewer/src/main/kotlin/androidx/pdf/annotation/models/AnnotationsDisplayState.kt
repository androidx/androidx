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

package androidx.pdf.annotation.models

import android.graphics.Matrix
import androidx.annotation.RestrictTo

/**
 * Represents the complete display state for annotations on a PDF document.
 *
 * @property transformationMatrices A map where the key is the page number (0-indexed) and the value
 *   is the [Matrix] required to transform the annotations for that page from PDF coordinates to
 *   screen coordinates, accounting for zoom and pan.
 * @property visiblePageAnnotations map of annotations currently visible on screen by page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class AnnotationsDisplayState(
    val transformationMatrices: Map<Int, Matrix>,
    val visiblePageAnnotations: VisiblePdfAnnotations = VisiblePdfAnnotations.EMPTY,
) {
    public companion object {
        public val EMPTY: AnnotationsDisplayState =
            AnnotationsDisplayState(
                transformationMatrices = emptyMap(),
                visiblePageAnnotations = VisiblePdfAnnotations.EMPTY,
            )
    }
}
