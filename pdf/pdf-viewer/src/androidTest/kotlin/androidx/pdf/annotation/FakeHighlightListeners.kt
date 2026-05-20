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

import androidx.pdf.annotation.models.PdfAnnotation
import androidx.test.espresso.idling.CountingIdlingResource

internal class FakeHighlightListeners(private val highlightIdlingResource: CountingIdlingResource) :
    OnGestureClaimListener, OnAnnotationEditListener {
    var isStarted: Boolean = false
        private set

    var isFailed: Boolean = false
        private set

    val finishedAnnotations = mutableListOf<PdfAnnotation>()

    var errorThrown: Throwable? = null

    override fun onGestureClaimed() {
        isStarted = true
        if (!highlightIdlingResource.isIdleNow) highlightIdlingResource.decrement()
    }

    override fun onGestureAbandoned() {
        isFailed = true
        if (!highlightIdlingResource.isIdleNow) highlightIdlingResource.decrement()
    }

    override fun onAnnotationCreated(annotation: PdfAnnotation) {
        finishedAnnotations.add(annotation)
        if (!highlightIdlingResource.isIdleNow) {
            highlightIdlingResource.decrement()
        }
    }

    override fun onAnnotationError(throwable: Throwable) {
        errorThrown = throwable
        if (!highlightIdlingResource.isIdleNow) {
            highlightIdlingResource.decrement()
        }
    }
}
