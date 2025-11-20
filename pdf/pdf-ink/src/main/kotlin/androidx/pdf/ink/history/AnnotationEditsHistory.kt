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

package androidx.pdf.ink.history

import androidx.pdf.annotation.models.AnnotationEditOperation

/** Responsible for maintaining the history of annotations to support undo and redo operations. */
internal interface AnnotationEditsHistory {
    /**
     * Adds a new annotation to the history. This operation typically pushes the annotation onto the
     * undo stack and clears the redo stack.
     *
     * @param editOperation annotation to be added with the operation.
     */
    fun addEntry(editOperation: AnnotationEditOperation)

    /**
     * Executes an undo operation. This moves the most recent annotation from the undo stack to the
     * redo stack.
     *
     * @return The [AnnotationEditOperation] that was undone, or null if there is nothing to undo.
     */
    fun undo(): AnnotationEditOperation?

    /**
     * Executes a redo operation. This moves the most recent annotation from the redo stack to the
     * undo stack.
     *
     * @return The [AnnotationEditOperation] that was redone, or null if there is nothing to redo.
     */
    fun redo(): AnnotationEditOperation?

    /**
     * Checks if there are any annotations available to be undone.
     *
     * @return true if the undo stack is not empty, false otherwise.
     */
    fun canUndo(): Boolean

    /**
     * Checks if there are any annotations available to be redone.
     *
     * @return true if the redo stack is not empty, false otherwise.
     */
    fun canRedo(): Boolean

    /** Clears all annotations from both the undo and redo history. */
    fun clear()
}
