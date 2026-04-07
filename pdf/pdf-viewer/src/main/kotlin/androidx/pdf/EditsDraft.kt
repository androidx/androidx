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

import androidx.annotation.RestrictTo

/**
 * Represents a read-only sequence of draft edit operations for a PDF document.
 *
 * @property operations The list of operations contained in this draft.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class EditsDraft internal constructor(public val operations: List<DraftEditOperation>) {

    /**
     * Splits this [EditsDraft] into two separate drafts at the specified index with bounds [0,
     * index) and [index, n).
     *
     * @param index The position at which to split the operations.
     * @return A [Pair] where `first` is an [EditsDraft] representing the range `[0, index)` and
     *   `second` is an [EditsDraft] representing the range `[index, size)`.
     * @throws IllegalArgumentException if the [index] is strictly negative or greater than the
     *   total number of operations.
     */
    public fun splitAt(index: Int): Pair<EditsDraft, EditsDraft> {
        require(index in 0..operations.size) {
            "Index $index is out of bounds [0, ${operations.size}]"
        }

        val leftOperations = operations.subList(0, index)
        val rightOperations = operations.subList(index, operations.size)

        return Pair(EditsDraft(leftOperations), EditsDraft(rightOperations))
    }

    /**
     * Combines this draft with another draft, returning a new instance containing the operations of
     * both in sequence.
     *
     * @param other The other [EditsDraft] to append to this one.
     * @return A new [EditsDraft] containing the operations of this draft followed by the operations
     *   of the other draft.
     */
    public operator fun plus(other: EditsDraft): EditsDraft {
        val combinedOperations = this.operations + other.operations
        return EditsDraft(combinedOperations)
    }

    /**
     * Retrieves the list of draft edit operations, ordered by their target page number.
     *
     * @return A list of [DraftEditOperation] sorted by page index.
     */
    public fun getOperationsSortedByPage(): List<DraftEditOperation> {
        val sortedOperations =
            operations
                .map { draftEditOperation ->
                    val pageNum =
                        when (draftEditOperation) {
                            is InsertDraftEditOperation -> draftEditOperation.annotation.pageNum
                            is UpdateDraftEditOperation -> draftEditOperation.annotation.pageNum
                            is RemoveDraftEditOperation -> draftEditOperation.pageNum
                            else -> -1
                        }

                    pageNum to draftEditOperation
                }
                .sortedBy { it.first }
                .map { it.second }
        return sortedOperations
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditsDraft) return false

        if (operations != other.operations) return false

        return true
    }

    override fun hashCode(): Int {
        return operations.hashCode()
    }
}
