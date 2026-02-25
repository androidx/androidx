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

package androidx.pdf.annotation.repository

import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument
import androidx.pdf.annotation.KeyedPdfAnnotation

/** The single source of truth for fetching persisted annotation data. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AnnotationsRepository {
    /**
     * Retrieves the persisted [KeyedPdfAnnotation] for the specified page and id.
     *
     * @param pageNum The specified page number.
     * @param annotationId The id of the annotation.
     * @return The [KeyedPdfAnnotation] object if found else null.
     */
    public suspend fun getAnnotation(pageNum: Int, annotationId: String): KeyedPdfAnnotation?

    /**
     * Retrieves the list of persisted annotations for a specific page.
     *
     * @param pageNum The 0-based index of the page to fetch.
     * @return A list of [KeyedPdfAnnotation] objects found on the page. Returns an empty list if no
     *   annotations exist.
     */
    public suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation>

    /**
     * Cleans up the internal cache of the repository. The next call to [getAnnotationsForPage] or
     * [getAnnotation] would repopulate the cache.
     */
    public fun clear()

    public companion object {
        public fun create(document: PdfDocument): AnnotationsRepository {
            return PdfDocumentAnnotationsRepository(document)
        }
    }
}
