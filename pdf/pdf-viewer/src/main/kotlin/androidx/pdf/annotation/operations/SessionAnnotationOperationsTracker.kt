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

package androidx.pdf.annotation.operations

import androidx.pdf.EditsDraft
import androidx.pdf.MutableEditsDraft
import androidx.pdf.annotation.AnnotationHandleIdGenerator.decomposeAnnotationId
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.registry.AnnotationHandleRegistry
import java.util.Collections

/**
 * An in-memory, thread-safe implementation of [AnnotationOperationsTracker] designed to manage the
 * state of a temporary editing session.
 *
 * Acts as the "Dirty State" buffer for the PDF document. It stores operations using a
 * [LinkedHashMap] to strictly maintain **insertion order**, which serves as the proxy for the
 * Z-index (rendering order) of the annotations.
 *
 * **Key Behaviors:**
 * 1. Squash-on-Write: Incoming operations are immediately resolved against existing operations for
 *    the same key. This ensures the tracker never holds redundant history.
 * - `ADD` + `UPDATE` -> squashed to single `ADD` (with updated content).
 * - `ADD` + `REMOVE` -> squashed to `null` (entry is removed entirely).
 * - `UPDATE` + `REMOVE` -> squashed to `REMOVE`.
 * 2. Z-Index Management (Bring-to-Front): Any write operation (Add or Update) removes the existing
 *    entry and re-appends the resolved operation to the tail of the underlying map. This ensures
 *    that the most recently modified annotations are rendered on top of older ones.
 * 3. Thread Safety: All access to the internal storage is synchronized, making it safe to record
 *    operations from the UI thread while simultaneously snapshotting from a background sync thread.
 */
internal class SessionAnnotationOperationsTracker(
    private val handleRegistry: AnnotationHandleRegistry
) : AnnotationOperationsTracker {
    private val operationsMap: MutableMap<String, KeyedAnnotationOperation> =
        Collections.synchronizedMap(LinkedHashMap())

    override fun addEntry(
        operationType: KeyedAnnotationOperation.OperationType,
        key: String,
        annotation: PdfAnnotation,
    ) {
        synchronized(operationsMap) {
            val lastOp = operationsMap[key]
            val lastType = lastOp?.operationType

            if (lastType != null && !lastType.canTransitionTo(operationType)) {
                throw IllegalStateException(
                    "Cannot transition from $lastType to $operationType for $key"
                )
            }

            val resolvedOperation = lastOp.resolve(operationType, key, annotation)

            if (resolvedOperation != null) {
                // CRITICAL STEP: Remove strictly BEFORE putting.
                // In LinkedHashMap, this forces the new entry to append at the TAIL.
                operationsMap.remove(key)
                operationsMap[key] = resolvedOperation
            } else {
                // The operation was cancelled (e.g. Add -> Remove), just delete it.
                operationsMap.remove(key)
            }
        }
    }

    override fun getSnapshot(): List<KeyedAnnotationOperation> {
        synchronized(operationsMap) {
            return ArrayList(operationsMap.values)
        }
    }

    override fun getModificationsSnapshot(): EditsDraft {
        val mutableEditsDraft = MutableEditsDraft()
        operationsMap.forEach { (_, operation) ->
            val handleId = operation.keyedAnnotation.key
            val sourceId = handleRegistry.getSourceId(handleId)

            // If sourceId is null, it's a draft or invalid, so we skip it.
            if (sourceId != null) {
                when (operation.operationType) {
                    KeyedAnnotationOperation.OperationType.ADD -> {
                        mutableEditsDraft.insert(operation.keyedAnnotation.annotation)
                    }
                    KeyedAnnotationOperation.OperationType.UPDATE -> {
                        mutableEditsDraft.update(sourceId, operation.keyedAnnotation.annotation)
                    }
                    KeyedAnnotationOperation.OperationType.REMOVE -> {
                        val (pageNum, _) = decomposeAnnotationId(handleId)
                        mutableEditsDraft.remove(sourceId, pageNum)
                    }
                }
            }
        }
        return mutableEditsDraft.toEditsDraft()
    }

    override fun isDeleted(key: String): Boolean {
        val op = operationsMap[key]
        return op?.operationType == KeyedAnnotationOperation.OperationType.REMOVE
    }

    override fun getUpdatedAnnotation(key: String): PdfAnnotation? {
        val op = operationsMap[key]
        return if (op?.operationType == KeyedAnnotationOperation.OperationType.UPDATE) {
            op.keyedAnnotation.annotation
        } else {
            null
        }
    }

    override fun clear() = operationsMap.clear()

    private fun KeyedAnnotationOperation?.resolve(
        newType: KeyedAnnotationOperation.OperationType,
        key: String,
        annotation: PdfAnnotation,
    ): KeyedAnnotationOperation? {
        val lastType = this?.operationType
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)
        val newOp = KeyedAnnotationOperation(operationType = newType, keyedAnnotation)

        return when (lastType) {
            null -> newOp
            KeyedAnnotationOperation.OperationType.ADD ->
                when (newType) {
                    KeyedAnnotationOperation.OperationType.ADD -> newOp
                    KeyedAnnotationOperation.OperationType.UPDATE ->
                        KeyedAnnotationOperation(
                            KeyedAnnotationOperation.OperationType.ADD,
                            keyedAnnotation,
                        )
                    KeyedAnnotationOperation.OperationType.REMOVE -> newOp
                }
            KeyedAnnotationOperation.OperationType.UPDATE -> newOp
            KeyedAnnotationOperation.OperationType.REMOVE -> {
                if (newType == KeyedAnnotationOperation.OperationType.ADD) newOp else this
            }
        }
    }
}
