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
 * Represents an operation to update an existing annotation in the PDF document.
 *
 * @property id The unique identifier of the annotation to be updated.
 * @property annotation The new [PdfAnnotation] data that will replace the existing annotation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpdateDraftEditOperation(public val id: String, public val annotation: PdfAnnotation) :
    DraftEditOperation {
    override val pageNum: Int
        get() = annotation.pageNum

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateDraftEditOperation) return false
        return id == other.id && annotation == other.annotation
    }

    override fun hashCode(): Int = 31 * id.hashCode() + annotation.hashCode()
}
