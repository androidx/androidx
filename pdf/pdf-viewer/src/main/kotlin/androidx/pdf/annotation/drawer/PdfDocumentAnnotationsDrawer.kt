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

package androidx.pdf.annotation.drawer

import android.graphics.Canvas
import android.util.SparseArray
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData

/** Draws a collection of PDF annotations. */
internal interface PdfDocumentAnnotationsDrawer {
    /**
     * Draws the annotations onto the provided [Canvas].
     *
     * @param pagesAnnotationData A [SparseArray] mapping page numbers to their
     *   [PageAnnotationsData], which includes the list of annotations and their transformation
     *   matrix.
     * @param canvas The [Canvas] on which to draw the annotations.
     */
    fun draw(pagesAnnotationData: SparseArray<PageAnnotationsData>, canvas: Canvas)
}
