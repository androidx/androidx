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

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.pdf.DraftEditOperation
import androidx.pdf.DraftEditResult
import androidx.pdf.EditsDraft
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.PdfEditApplyException

/**
 * A processor for handling a list of [DraftEditOperation] objects by batching them and applying the
 * edits to a remote PDF document.
 *
 * @property remoteDocument The [PdfDocumentRemote] interface used to apply the annotation edits.
 */
internal class BatchPdfAnnotationsProcessor(private val remoteDocument: PdfDocumentRemote) {

    /**
     * Processes a draft of edits by applying them to the remote PDF document in batches.
     *
     * This method prevents large lists of operations from causing
     * [android.os.TransactionTooLargeException] when sent over an AIDL connection. It splits the
     * list of operations from the [EditsDraft] into smaller batches based on a maximum size limit
     * and processes each batch individually. The results from each batch are then combined into a
     * single list of success IDs.
     *
     * @param editsDraft The [EditsDraft] containing the operations to be applied.
     * @param onBatchedEditsApplied callback method invoked when a batch is applied.
     * @return A list of unique identifiers for the successfully applied edits.
     * @throws PdfEditApplyException if there is an error in applying the edits. The exception
     *   contains details about which operations succeeded before the failure.
     */
    fun process(
        editsDraft: EditsDraft,
        onBatchedEditsApplied: (List<AppliedEdit>) -> Unit,
    ): List<String> =
        processInBatches(operations = editsDraft.getOperationsSortedByPage(), onBatchedEditsApplied)

    private fun processInBatches(
        operations: List<DraftEditOperation>,
        onBatchedEditsApplied: (List<AppliedEdit>) -> Unit,
    ): List<String> {
        val annotationIds = mutableListOf<String>()
        if (operations.isEmpty()) return annotationIds

        val batchedOperations = operations.unflatten(MAX_BATCH_SIZE_IN_BYTES)

        var processedCount = 0
        batchedOperations.forEach { batch ->
            when (val result = remoteDocument.applyDraftEdits(batch)) {
                is DraftEditResult.Success -> {
                    annotationIds += result.ids
                    processedCount += batch.size

                    val appliedEdits =
                        result.ids.mapIndexed { index, id ->
                            AppliedEdit(batch[index].getPage(), id)
                        }
                    onBatchedEditsApplied(appliedEdits)
                }

                is DraftEditResult.Failure -> {
                    val appliedEdits =
                        result.appliedIds.mapIndexed { index, id ->
                            AppliedEdit(batch[index].getPage(), id)
                        }
                    onBatchedEditsApplied(appliedEdits)
                    throw PdfEditApplyException(
                        failureIndex = processedCount + result.failedBatchIndex,
                        appliedEditIds = annotationIds + result.appliedIds,
                        error = Exception(result.errorMessage),
                    )
                }
            }
        }
        return annotationIds
    }

    companion object {
        const val MAX_BATCH_SIZE_IN_BYTES: Int = 1000000

        /**
         * Splits this list of [Parcelable] items into multiple sublists (batches), where the total
         * parcel size of the items in each batch does not exceed a specified maximum.
         *
         * Note: Any single item whose individual parcel size is larger than [maxSizeInBytes] will
         * be ignored and will not be included in any of the resulting batches.
         *
         * @param T The type of [Parcelable] items in the list.
         * @param maxSizeInBytes The maximum permitted size in bytes for the parcelled content of
         *   each batch.
         * @return A `List<List<T>>` where each inner list represents a batch.
         */
        fun <T : Parcelable> List<T>.unflatten(maxSizeInBytes: Int): List<List<T>> {
            if (isEmpty()) {
                return emptyList()
            }

            val batches = mutableListOf<List<T>>()
            var currentBatch = mutableListOf<T>()
            var currentBatchSize = 0

            for (item in this) {
                val itemSize = item.parcelSizeInBytes()

                // Ignore items that are individually larger than the max size.
                if (itemSize > maxSizeInBytes) {
                    continue
                }

                // If adding the new item would exceed the max size,
                // finalize the current batch and start a new one.
                if (currentBatch.isNotEmpty() && currentBatchSize + itemSize > maxSizeInBytes) {
                    batches.add(currentBatch)
                    currentBatch = mutableListOf()
                    currentBatchSize = 0
                }

                currentBatch.add(item)
                currentBatchSize += itemSize
            }

            // Add the last batch if it has any items.
            if (currentBatch.isNotEmpty()) {
                batches.add(currentBatch)
            }

            return batches
        }

        /**
         * Calculates the size of a [Parcelable] object when flattened into a [Parcel].
         *
         * @return The size in bytes of the `Parcelable` object when written to a [Parcel].
         */
        @VisibleForTesting
        internal fun Parcelable.parcelSizeInBytes(): Int {
            val parcel = Parcel.obtain()
            this.writeToParcel(parcel, 0)
            val size = parcel.dataSize()
            parcel.recycle()
            return size
        }
    }

    /**
     * Represents an edit applied to a document.
     *
     * @param pageNum page number of the edit.
     * @param editId id of the edit.
     */
    internal class AppliedEdit(public val pageNum: Int, public val editId: String) {
        override fun equals(other: Any?): Boolean {
            return other != null &&
                other is AppliedEdit &&
                other.pageNum == pageNum &&
                other.editId == editId
        }

        override fun hashCode(): Int {
            var result = pageNum
            result = 31 * result + editId.hashCode()
            return result
        }

        override fun toString(): String {
            return "AppliedEdit(pageNum=$pageNum, editId='$editId')"
        }
    }
}
