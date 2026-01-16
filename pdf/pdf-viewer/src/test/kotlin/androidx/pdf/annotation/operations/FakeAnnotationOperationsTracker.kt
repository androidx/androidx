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
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation

class FakeAnnotationOperationsTracker : AnnotationOperationsTracker {
    private val operations = mutableMapOf<String, KeyedAnnotationOperation>()

    override fun addEntry(
        operationType: KeyedAnnotationOperation.OperationType,
        key: String,
        annotation: PdfAnnotation,
    ) {
        val op = KeyedAnnotationOperation(operationType, KeyedPdfAnnotation(key, annotation))
        // Simple squash logic for testing
        if (operationType == KeyedAnnotationOperation.OperationType.REMOVE) {
            val previous = operations[key]
            if (previous?.operationType == KeyedAnnotationOperation.OperationType.ADD) {
                operations.remove(key)
                return
            }
        }
        operations[key] = op
    }

    override fun getSnapshot(): List<KeyedAnnotationOperation> = operations.values.toList()

    override fun getModificationsSnapshot(): EditsDraft {
        TODO("Not yet implemented")
    }

    override fun clear() = operations.clear()

    override fun isDeleted(key: String): Boolean {
        return operations[key]?.operationType == KeyedAnnotationOperation.OperationType.REMOVE
    }

    override fun getUpdatedAnnotation(key: String): PdfAnnotation? {
        val op = operations[key]
        return if (op?.operationType == KeyedAnnotationOperation.OperationType.UPDATE) {
            op.keyedAnnotation.annotation
        } else {
            null
        }
    }
}
