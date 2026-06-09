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

package androidx.pdf.annotation.content

import androidx.annotation.RestrictTo

/**
 * Represents an operation to insert a new annotation into the PDF document.
 *
 * @property annotation The [PdfAnnotation] object to be inserted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InsertDraftEditOperation(public val annotation: PdfAnnotation) : DraftEditOperation {
    override val pageNum: Int
        get() = annotation.pageNum

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InsertDraftEditOperation) return false
        return annotation == other.annotation
    }

    override fun hashCode(): Int = annotation.hashCode()
}
