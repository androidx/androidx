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

package androidx.pdf

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.annotation.models.PdfAnnotation

/** A mutable builder for creating a sequence of draft edit operations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableEditsDraft
private constructor(private val mutableOperations: MutableList<DraftEditOperation>) :
    EditsDraft(mutableOperations) {

    public constructor() : this(ArrayList())

    /**
     * Enqueues a `insert` operation for a new [PdfAnnotation].
     *
     * @param annotation The [PdfAnnotation] to be created.
     */
    public fun insert(annotation: PdfAnnotation) {
        mutableOperations.add(InsertDraftEditOperation(annotation))
    }

    /**
     * Enqueues an 'update' operation for an existing annotation.
     *
     * @param annotationId The id of the annotation to be updated.
     * @param annotation The new [PdfAnnotation] data for the update.
     */
    public fun update(annotationId: String, annotation: PdfAnnotation) {
        mutableOperations.add(UpdateDraftEditOperation(annotationId, annotation))
    }

    /**
     * Enqueues a 'remove' operation for an existing annotation.
     *
     * @param annotationId The id of the annotation to be removed.
     * @param pageNum The page number where the annotation is located.
     */
    public fun remove(annotationId: String, pageNum: Int) {
        mutableOperations.add(RemoveDraftEditOperation(annotationId, pageNum))
    }

    /**
     * Adds a raw operation directly.
     *
     * @param operation The [DraftEditOperation] to add.
     */
    @VisibleForTesting
    public fun addOperation(operation: DraftEditOperation) {
        mutableOperations.add(operation)
    }

    /**
     * Transforms the enqueued edits to an immutable request.
     *
     * @return An [EditsDraft] containing the accumulated operations.
     */
    public fun toEditsDraft(): EditsDraft = this
}
