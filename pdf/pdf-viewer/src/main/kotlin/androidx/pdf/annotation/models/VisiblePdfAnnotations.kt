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

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.KeyedPdfAnnotation

/**
 * An immutable, value-based representation of all keyed annotations in a document, organized by
 * page number.
 *
 * @property pageAnnotations A map where the key is the page number and the value is a list of
 *   annotations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class VisiblePdfAnnotations(
    public val pageAnnotations: Map<Int, List<KeyedPdfAnnotation>>
) {
    /**
     * Retrieves the list of annotations for a given page in order of z-index
     *
     * @param pageNum The 0-indexed page number.
     * @return A read-only [List] of [KeyedPdfAnnotation] objects, or an empty list if the page has
     *   no annotations.
     */
    public fun getKeyedAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> =
        pageAnnotations.getOrDefault(pageNum, emptyList())

    public companion object {
        public val EMPTY: VisiblePdfAnnotations = VisiblePdfAnnotations(pageAnnotations = mapOf())
    }
}
