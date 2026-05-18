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

package androidx.pdf.annotation

import android.graphics.RectF
import android.util.SparseArray
import androidx.annotation.RestrictTo

/**
 * Represents the state of the viewport on which [androidx.pdf.view.PdfView] is overlaid.
 *
 * @param firstVisiblePage The index of the first visible page [0-based].
 * @param visiblePagesCount The number of currently visible pages.
 * @param pageBounds A mapping of page numbers to their bounds in view coordinates.
 * @param zoom The current zoom level of the PDF.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PdfViewportState(
    public val firstVisiblePage: Int,
    public val visiblePagesCount: Int,
    public val pageBounds: SparseArray<RectF>,
    public val zoom: Float,
)
