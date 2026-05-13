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

package androidx.pdf.models

import androidx.pdf.annotation.models.KeyedPdfObject

/** Interface for providing objects on a specific page of a PDF document. */
internal interface PageObjectsProvider {
    /**
     * Retrieves all objects for the specified page.
     *
     * @param pageNum The 0-indexed page number.
     * @param types The types of objects to retrieve as a bitmask.
     * @return A list of [KeyedPdfObject] objects.
     */
    fun getPageObjects(pageNum: Int, types: Long): List<KeyedPdfObject>
}
