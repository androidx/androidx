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

package androidx.pdf.annotation.draftstate

import androidx.annotation.RestrictTo
import androidx.pdf.EditsDraft
import androidx.pdf.MutableEditsDraft
import androidx.pdf.annotation.AnnotationHandleIdGenerator
import androidx.pdf.annotation.AnnotationHandleIdGenerator.composeAnnotationId
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A simple implementation of [AnnotationEditsDraftState] that stores annotation edits in memory.
 *
 * ## Thread-Safety
 *
 * This class is thread-safe. All public methods that modify the internal state are protected by a
 * [ReentrantLock] to ensure atomicity of compound operations. Read operations rely on the
 * thread-safe nature of the underlying synchronized collections.
 *
 * ## Time Complexity
 *
 * The time complexity of the operations is as follows:
 * - [getDraftAnnotations]: O(k) where k is the number of annotations on the page (due to list
 *   creation).
 * - [addDraftAnnotation]: O(1) on average.
 * - [removeAnnotation]: O(1) on average.
 * - [updateDraftAnnotation]: O(1) on average.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InMemoryAnnotationEditsDraftState() : AnnotationEditsDraftState {
    /**
     * The primary data store for all draft annotations. The inner map uses [String] as the key for
     * efficient lookups and [KeyedPdfAnnotation] as the value.
     *
     * A [LinkedHashMap] is used as the inner map to preserve the insertion order of annotations,
     * which is a critical requirement.
     */
    private val draftAnnotationsPerPage: MutableMap<Int, MutableMap<String, KeyedPdfAnnotation>> =
        Collections.synchronizedMap(HashMap())

    private val lock: ReentrantLock = ReentrantLock()

    override fun getDraftAnnotation(pageNum: Int, handleId: String): PdfAnnotation? {
        return draftAnnotationsPerPage[pageNum]?.get(handleId)?.annotation
    }

    override fun getDraftAnnotations(pageNum: Int): List<KeyedPdfAnnotation> {
        return draftAnnotationsPerPage[pageNum]?.values?.toList() ?: emptyList()
    }

    override fun getModificationsSnapshot(): EditsDraft {
        val mutableEditsDraft = MutableEditsDraft()
        draftAnnotationsPerPage.forEach { (_, pageAnnotationsMap) ->
            pageAnnotationsMap.forEach { (_, keyedAnnotation) ->
                mutableEditsDraft.insert(keyedAnnotation.annotation)
            }
        }
        return mutableEditsDraft.toEditsDraft()
    }

    override fun addDraftAnnotation(keyedAnnotation: KeyedPdfAnnotation): String {
        val pageNum = keyedAnnotation.annotation.pageNum
        val handleId = keyedAnnotation.key
        val keyedPdfAnnotation = KeyedPdfAnnotation(handleId, keyedAnnotation.annotation)

        lock.withLock {
            val draftPageAnnotations =
                draftAnnotationsPerPage.getOrPut(pageNum) {
                    // Using LinkedHashMap to maintain insertion order for each page's annotations.
                    Collections.synchronizedMap(LinkedHashMap())
                }
            draftPageAnnotations[handleId] = keyedPdfAnnotation
        }
        return handleId
    }

    override fun addDraftAnnotation(annotation: PdfAnnotation): String {
        val pageNum = annotation.pageNum
        val handleId = composeAnnotationId(pageNum, id = AnnotationHandleIdGenerator.generateId())
        val keyedPdfAnnotation = KeyedPdfAnnotation(handleId, annotation)

        lock.withLock {
            val draftPageAnnotations =
                draftAnnotationsPerPage.getOrPut(pageNum) {
                    // Using LinkedHashMap to maintain insertion order for each page's annotations.
                    Collections.synchronizedMap(LinkedHashMap())
                }
            draftPageAnnotations[handleId] = keyedPdfAnnotation
        }
        return handleId
    }

    override fun removeAnnotation(pageNum: Int, annotationId: String): PdfAnnotation {
        lock.withLock {
            val draftPageAnnotations =
                draftAnnotationsPerPage[pageNum]
                    ?: throw NoSuchElementException("No annotations present on page ${pageNum}.")

            val removedData =
                draftPageAnnotations.remove(annotationId)
                    ?: throw NoSuchElementException("Annotation with ID $annotationId not found.")

            if (draftPageAnnotations.isEmpty()) {
                draftAnnotationsPerPage.remove(pageNum)
            }
            return removedData.annotation
        }
    }

    override fun updateDraftAnnotation(
        pageNum: Int,
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation {
        lock.withLock {
            val draftPageAnnotations =
                draftAnnotationsPerPage[pageNum]
                    ?: throw NoSuchElementException("No annotations present on page ${pageNum}.")

            if (!draftPageAnnotations.containsKey(annotationId)) {
                throw NoSuchElementException("Annotation with ID $annotationId not found.")
            }

            val keyedAnnotation = KeyedPdfAnnotation(annotationId, newAnnotation)
            val previousData =
                requireNotNull(draftPageAnnotations.put(annotationId, keyedAnnotation))
            return previousData.annotation
        }
    }

    /** Clears all annotation edits from the draft state. */
    override fun clear() {
        lock.withLock { draftAnnotationsPerPage.clear() }
    }
}
