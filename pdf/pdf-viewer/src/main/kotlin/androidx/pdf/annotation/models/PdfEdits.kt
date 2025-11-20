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

/**
 * An immutable, value-based representation of all edits in a document, organized by page number.
 *
 * @property editsByPage A map where the key is the page number and the value is a list of edits.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class PdfEdits(public val editsByPage: Map<Int, List<PdfEditEntry<out PdfEdit>>>) {
    /** A set of all page numbers that contain at least one edit. */
    public val pagesWithEdits: Set<Int>
        get() = editsByPage.keys

    /**
     * Retrieves the list of edits for a given page.
     *
     * @param pageNum The 0-indexed page number.
     * @return A read-only [List] of [PdfEditEntry] objects, or an empty list if the page has no
     *   edits.
     */
    public fun getEditsForPage(pageNum: Int): List<PdfEditEntry<out PdfEdit>> {
        return editsByPage[pageNum] ?: emptyList()
    }
}
