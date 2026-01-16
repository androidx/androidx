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

package androidx.pdf.annotation.manager

import androidx.pdf.EditsDraft
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation
import java.util.UUID

class FakePdfAnnotationsManager : PdfAnnotationsManager {
    private val storage = mutableMapOf<String, PdfAnnotation>()

    override suspend fun getAnnotations(pageNum: Int): List<KeyedPdfAnnotation> {
        return storage.entries
            .filter { it.value.pageNum == pageNum }
            .map { KeyedPdfAnnotation(it.key, it.value) }
    }

    override suspend fun getAnnotationModifications(): EditsDraft {
        TODO("Not yet implemented")
    }

    override fun addAnnotation(keyedAnnotation: KeyedPdfAnnotation): String {
        storage[keyedAnnotation.key] = keyedAnnotation.annotation
        return keyedAnnotation.key
    }

    override fun addAnnotation(annotation: PdfAnnotation): String {
        val id = UUID.randomUUID().toString()
        storage[id] = annotation
        return id
    }

    override suspend fun removeAnnotation(annotationId: String): PdfAnnotation? {
        return storage.remove(annotationId)
    }

    override suspend fun updateAnnotation(
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation {
        val oldAnnotation =
            storage[annotationId]
                ?: throw NoSuchElementException("Annotation $annotationId not found")

        storage[annotationId] = newAnnotation
        return oldAnnotation
    }

    override fun discardChanges() {
        storage.clear()
    }

    fun getAnnotation(id: String): PdfAnnotation? = storage[id]

    fun count(): Int = storage.size
}
