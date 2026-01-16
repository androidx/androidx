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

import androidx.annotation.RestrictTo
import androidx.pdf.EditsDraft
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.registry.AnnotationHandleRegistry

/** Manages and tracks the lifecycle of annotation modification operations within a session. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AnnotationOperationsTracker {
    /**
     * Records a new operation for a specific annotation.
     *
     * Applies the new operation against the current history of the [key]. If an operation already
     * exists for this [key], the implementation will attempt to merge (squash) the new operation
     * with the existing one.
     *
     * Z-Index Behavior: Successful addition or update of an entry should typically move the [key]
     * to the end of the tracking list, effectively bringing the annotation to the front (top
     * Z-index).
     *
     * @param operationType The type of change being performed (ADD, UPDATE, REMOVE).
     * @param key The unique identifier for the annotation (e.g., a Session ID or Canonical ID).
     * @param annotation The data payload associated with the operation.
     * @throws IllegalStateException If the requested transition is invalid based on the current
     *   state of the [key].
     */
    public fun addEntry(
        operationType: KeyedAnnotationOperation.OperationType,
        key: String,
        annotation: PdfAnnotation,
    )

    /**
     * Returns a flattened snapshot of all pending operations.
     *
     * @return A list of [KeyedAnnotationOperation] objects ready to be persisted or rendered.
     */
    public fun getSnapshot(): List<KeyedAnnotationOperation>

    /**
     * Retrieves a snapshot of all current modifications (additions, updates, and removals)
     * accumulated in this tracker.
     *
     * @return An [EditsDraft] object containing the ordered collection of operations (Inserts,
     *   Updates, Removes).
     */
    public fun getModificationsSnapshot(): EditsDraft

    /**
     * Returns the new annotation if the annotation with [key] has been updated. Returns null
     * otherwise.
     *
     * @param key The unique identifier to look up.
     */
    public fun getUpdatedAnnotation(key: String): PdfAnnotation?

    /**
     * Returns true if the persisted annotation with [key] has been marked for deletion.
     *
     * @param key The unique identifier to look up.
     */
    public fun isDeleted(key: String): Boolean

    /** Resets the internal state of the tracker. */
    public fun clear(): Unit

    public companion object {
        public fun create(registry: AnnotationHandleRegistry): AnnotationOperationsTracker {
            return SessionAnnotationOperationsTracker(registry)
        }
    }
}
