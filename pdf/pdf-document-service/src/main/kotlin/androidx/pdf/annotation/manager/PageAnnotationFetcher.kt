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

package androidx.pdf.annotation.manager

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.PdfAnnotation

/** A functional interface for fetching annotations for a given page. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface PageAnnotationFetcher {
    /**
     * Fetches all annotations for a given page.
     *
     * @param pageNum The page number (0-indexed).
     * @return A list of [PdfAnnotation]s for the specified page.
     */
    public suspend fun fetchAnnotations(pageNum: Int): List<PdfAnnotation>
}
