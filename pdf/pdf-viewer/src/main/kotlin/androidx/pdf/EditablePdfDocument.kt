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

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.models.FormEditInfo

/** Represents a PDF document that allows for editing. */
public abstract class EditablePdfDocument : PdfDocument {

    /**
     * Applies the changes specified by [record] to the form.
     *
     * Any areas which are invalidated due to this operation are notified via
     * [OnPdfContentInvalidatedListener] callback.
     *
     * It is recommended to maintain a list of [androidx.pdf.models.FormEditInfo] applied to the
     * document so they can be saved and restored across destructive events like low memory kills or
     * configuration changes.
     *
     * @param record The [androidx.pdf.models.FormEditInfo] to apply to the form.
     * @throws IllegalArgumentException if the provided [record] cannot be applied to the widget
     *   indicated by the index, or if the index does not correspond to a widget on the page.
     */
    public abstract suspend fun applyEdit(record: FormEditInfo)

    /**
     * Applies a list of edits to the document sequentially.
     *
     * @param editsDraft: edits to be applied on pdf document.
     * @return List of annotationId for each operation in sequence of the order they were enqueued.
     * @throws [PdfEditApplyException] if any of the edit failed to be applied.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Suppress("HiddenAbstractMethod")
    public abstract suspend fun applyEdits(editsDraft: EditsDraft): List<String>

    /**
     * Creates a [PdfWriteHandle] which can be used to save the document to a
     * [ParcelFileDescriptor].
     */
    public abstract fun createWriteHandle(): PdfWriteHandle
}
