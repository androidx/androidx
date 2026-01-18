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

package androidx.pdf.annotation.highlights

import android.graphics.PointF
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.exceptions.RequestFailedException

/** Callback interface for events related to the creation of text highlights. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface InProgressTextHighlightsListener {
    /**
     * Called when a highlight gesture successfully finds text and begins a new highlight.
     *
     * @param viewPoint The view coordinates where the highlight gesture started.
     * @param inProgressHighlightId The ID associated with the highlight that has just started.
     */
    public fun onTextHighlightStarted(
        viewPoint: PointF,
        inProgressHighlightId: InProgressHighlightId,
    )

    /**
     * Called when a highlight gesture is rejected, typically because no selectable text was found
     * at the gesture's starting location. In this case, no annotation is created.
     *
     * @param viewPoint The view coordinates where the highlight gesture was attempted.
     */
    public fun onTextHighlightRejected(viewPoint: PointF)

    /**
     * Called when a highlight gesture is successfully finished and converted to a [PdfAnnotation].
     *
     * @param annotations A map of newly finished annotations, mapping the in-progress ID to the
     *   final [PdfAnnotation].
     */
    public fun onTextHighlightFinished(annotations: Map<InProgressHighlightId, PdfAnnotation>)

    /**
     * Notifies that a non-fatal error has occurred during a highlight operation. The gesture will
     * continue despite the error.
     *
     * @param exception The exception that occurred.
     */
    public fun onTextHighlightError(exception: RequestFailedException)
}
