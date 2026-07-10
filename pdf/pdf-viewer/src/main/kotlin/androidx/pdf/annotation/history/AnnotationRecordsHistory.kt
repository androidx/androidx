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

package androidx.pdf.annotation.history

import androidx.pdf.annotation.models.KeyedAnnotationRecord
import kotlinx.coroutines.flow.StateFlow

/** Responsible for maintaining the history of annotations to support undo and redo operations. */
internal interface AnnotationRecordsHistory {

    /**
     * A flow that emits `true` when there is an operation that can be undone, `false` otherwise.
     */
    val canUndo: StateFlow<Boolean>

    /**
     * A flow that emits `true` when there is an operation that can be redone, `false` otherwise.
     */
    val canRedo: StateFlow<Boolean>

    /**
     * Adds a new annotation to the history. This operation typically pushes the annotation onto the
     * undo stack and clears the redo stack.
     *
     * @param record annotation operational record.
     */
    fun addEntry(record: KeyedAnnotationRecord)

    /**
     * Executes an undo operation. This moves the most recent annotation from the undo stack to the
     * redo stack.
     *
     * @return The [KeyedAnnotationRecord] that was undone, or null if there is nothing to undo.
     */
    fun undo(): KeyedAnnotationRecord?

    /**
     * Executes a redo operation. This moves the most recent annotation from the redo stack to the
     * undo stack.
     *
     * @return The [KeyedAnnotationRecord] that was redone, or null if there is nothing to redo.
     */
    fun redo(): KeyedAnnotationRecord?

    /** Clears both the undo and redo stacks, effectively resetting the history. */
    fun clear()
}
