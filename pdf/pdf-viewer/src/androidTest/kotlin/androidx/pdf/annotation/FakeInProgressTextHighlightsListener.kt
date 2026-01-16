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

package androidx.pdf.annotation

import android.graphics.PointF
import androidx.pdf.annotation.highlights.InProgressTextHighlightsListener
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.exceptions.RequestFailedException
import androidx.test.espresso.idling.CountingIdlingResource

internal class FakeInProgressTextHighlightsListener(
    private val highlightIdlingResource: CountingIdlingResource
) : InProgressTextHighlightsListener {
    var isStarted: Boolean = false
        private set

    var isFailed: Boolean = false
        private set

    var startedId: InProgressHighlightId? = null
        private set

    val finishedAnnotations = mutableMapOf<InProgressHighlightId, PdfAnnotation>()

    var exceptionThrown: RequestFailedException? = null

    override fun onTextHighlightStarted(
        viewPoint: PointF,
        inProgressHighlightId: InProgressHighlightId,
    ) {
        startedId = inProgressHighlightId
        isStarted = true
        if (!highlightIdlingResource.isIdleNow) highlightIdlingResource.decrement()
    }

    override fun onTextHighlightRejected(viewPoint: PointF) {
        isFailed = true
        if (!highlightIdlingResource.isIdleNow) highlightIdlingResource.decrement()
    }

    override fun onTextHighlightFinished(annotations: Map<InProgressHighlightId, PdfAnnotation>) {
        finishedAnnotations.putAll(annotations)
        if (!highlightIdlingResource.isIdleNow) {
            highlightIdlingResource.decrement()
        }
    }

    override fun onTextHighlightError(exception: RequestFailedException) {
        exceptionThrown = exception
        if (!highlightIdlingResource.isIdleNow) {
            highlightIdlingResource.decrement()
        }
    }
}
