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

package androidx.pdf.annotation.processor

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PdfAnnotationData

/** Represents an interface for processing a list of [PdfAnnotationData]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PdfAnnotationsProcessor {
    /**
     * Processes a list of annotations and returns an [AnnotationResult] that reports the
     * success/failures.
     *
     * @param annotations list of [PdfAnnotationData]
     * @return [AnnotationResult] reporting the successful and failed annotations.
     */
    public fun process(annotations: List<PdfAnnotationData>): AnnotationResult

    /**
     * Processes a list of annotation edits to add and returns an [AddEditResult] that reports the
     * success/failures.
     *
     * @param annotations list of [PdfAnnotationData]
     * @return [AddEditResult] reporting the successful and failed annotation edits.
     */
    public fun processAddEdits(annotations: List<PdfAnnotationData>): AddEditResult

    /**
     * Processes a list of annotation edits to update and returns an [ModifyEditResult] that reports
     * the success/failures.
     *
     * @param annotations list of [PdfAnnotationData]
     * @return [ModifyEditResult] reporting the successful and failed annotation edits.
     */
    public fun processUpdateEdits(annotations: List<PdfAnnotationData>): ModifyEditResult

    /**
     * Processes a list of annotation edits to remove and returns an [ModifyEditResult] that reports
     * the success/failures.
     *
     * @param editIds list of [EditId]
     * @return [ModifyEditResult] reporting the successful and failed annotation edits.
     */
    public fun processRemoveEdits(editIds: List<EditId>): ModifyEditResult
}
