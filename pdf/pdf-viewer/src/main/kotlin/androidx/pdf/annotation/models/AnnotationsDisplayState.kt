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

import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.PdfViewportState

/**
 * Represents the complete display state for annotations on a PDF document.
 *
 * @property viewportState Represents the state of the associated viewport, contains information
 *   about visiblePages, pageBounds and zoom.
 * @property visiblePageAnnotations map of annotations currently visible on screen by page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class AnnotationsDisplayState(
    val viewportState: PdfViewportState = EMPTY_VIEWPORT,
    val visiblePageAnnotations: VisiblePdfAnnotations = VisiblePdfAnnotations.EMPTY,
) {
    public companion object {
        public val EMPTY_VIEWPORT: PdfViewportState = PdfViewportState(0, 0, SparseArray(), 1.0f)
        public val EMPTY: AnnotationsDisplayState =
            AnnotationsDisplayState(
                viewportState = EMPTY_VIEWPORT,
                visiblePageAnnotations = VisiblePdfAnnotations.EMPTY,
            )
    }
}
