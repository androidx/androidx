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

import androidx.pdf.annotation.models.PdfAnnotationData

/** Provider interface for getting PDF annotations for a specific page. */
internal interface PageAnnotationsProvider {
    /**
     * Retrieves all annotations for a given page number.
     *
     * @param pageNum The 0-based index of the page.
     * @return A list of [PdfAnnotationData] objects for the specified page. Returns an empty list
     *   if there are no annotations or the page number is invalid.
     */
    fun getPageAnnotations(pageNum: Int): List<PdfAnnotationData>
}
