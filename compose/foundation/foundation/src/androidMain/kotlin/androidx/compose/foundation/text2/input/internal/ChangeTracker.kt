/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldBuffer.ChangeList
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.text.TextRange

/**
 * Keeps track of changes made to a text buffer via [trackChange], and reports changes as a
 * [ChangeList].
 *
 * @param initialChanges If non-null, used to initialize this tracker by copying changes.
 */
internal class ChangeTracker(initialChanges: ChangeTracker? = null) : ChangeList {

    private var _changes = mutableVectorOf<Change>()
    private var _changesTemp = mutableVectorOf<Change>()

    init {
        initialChanges?._changes?.forEach {
            _changes += Change(it.preStart, it.preEnd, it.originalStart, it.originalEnd)
        }
    }

    override val changeCount: Int
        get() = _changes.size

    /**
     * This function deals with three "coordinate spaces":
     *  - `original`: The text before any changes were made.
     *  - `pre`: The text before this change is applied, but with all previous changes applied.
     *  - `post`: The text after this change is applied, including all the previous changes.
     *
     * When this function is called, the existing changes map ranges in `original` to ranges in
     * `pre`. The new change is a mapping from `pre` to `post`. This function must determine the
     * corresponding range in `original` for this change, and convert all other changes' `pre`
     * ranges to `post`. It must also ensure that any adjacent or overlapping ranges are merged to
     * ensure the [ChangeList] invariant that all changes are non-overlapping.
     *
     * The algorithm works as follows:
     *  1. Find all the changes that are adjacent to or overlap with this one. This search is
     *     performed in the `pre` space since that's the space the new change shares with the
     *     existing changes.
     *  2. Merge all the changes from (1) into a single range in the `original` and `pre` spaces.
     *  3. Merge the new change with the change from (2), updating the end of the range to account
     *     for the new text.
     *  3. Offset all remaining changes are to account for the new text.
     */
    fun trackChange(preRange: TextRange, postLength: Int) {
        if (preRange.collapsed && postLength == 0) {
            // Ignore noop changes.
            return
        }

        var i = 0
        var recordedNewChange = false
        val postDelta = postLength - preRange.length

        var mergedOverlappingChange: Change? = null
        while (i < _changes.size) {
            val change = _changes[i]

            // Merge adjacent and overlapping changes as we go.
            if (
                change.preStart in preRange.min..preRange.max ||
                change.preEnd in preRange.min..preRange.max
            ) {
                if (mergedOverlappingChange == null) {
                    mergedOverlappingChange = change
                } else {
                    mergedOverlappingChange.preEnd = change.preEnd
                    mergedOverlappingChange.originalEnd = change.originalEnd
                }
                // Don't append overlapping changes to the temp list until we're finished merging.
                i++
                continue
            }

            if (change.preStart > preRange.max && !recordedNewChange) {
                // First non-overlapping change after the new one – record the change before
                // proceeding.
                appendNewChange(mergedOverlappingChange, preRange, postDelta)
                recordedNewChange = true
            }

            if (recordedNewChange) {
                change.preStart += postDelta
                change.preEnd += postDelta
            }
            _changesTemp += change
            i++
        }

        if (!recordedNewChange) {
            // The new change is after or overlapping all previous changes so it hasn't been
            // appended yet.
            appendNewChange(mergedOverlappingChange, preRange, postDelta)
        }

        // Swap the lists.
        val oldChanges = _changes
        _changes = _changesTemp
        _changesTemp = oldChanges
        _changesTemp.clear()
    }

    fun clearChanges() {
        _changes.clear()
    }

    override fun getRange(changeIndex: Int): TextRange =
        _changes[changeIndex].let { TextRange(it.preStart, it.preEnd) }

    override fun getOriginalRange(changeIndex: Int): TextRange =
        _changes[changeIndex].let { TextRange(it.originalStart, it.originalEnd) }

    override fun toString(): String = buildString {
        append("ChangeList(changes=[")
        _changes.forEachIndexed { i, change ->
            append(
                "(${change.originalStart},${change.originalEnd})->" +
                    "(${change.preStart},${change.preEnd})"
            )
            if (i < changeCount - 1) append(", ")
        }
        append("])")
    }

    private fun appendNewChange(
        mergedOverlappingChange: Change?,
        preRange: TextRange,
        postDelta: Int
    ) {
        var originalDelta = if (_changesTemp.isEmpty()) 0 else {
            _changesTemp.last().let { it.preEnd - it.originalEnd }
        }
        val newChange: Change
        if (mergedOverlappingChange == null) {
            // There were no overlapping changes, so allocate a new one.
            val originalStart = preRange.min - originalDelta
            val originalEnd = originalStart + preRange.length
            newChange = Change(
                preStart = preRange.min,
                preEnd = preRange.max + postDelta,
                originalStart = originalStart,
                originalEnd = originalEnd
            )
        } else {
            newChange = mergedOverlappingChange
            // Convert the merged overlapping changes to the `post` space.
            // Merge the new changed with the merged overlapping changes.
            if (newChange.preStart > preRange.min) {
                // The new change starts before the merged overlapping set.
                newChange.preStart = preRange.min
                newChange.originalStart = preRange.min
            }
            if (preRange.max > newChange.preEnd) {
                // The new change ends after the merged overlapping set.
                originalDelta = newChange.preEnd - newChange.originalEnd
                newChange.preEnd = preRange.max
                newChange.originalEnd = preRange.max - originalDelta
            }
            newChange.preEnd += postDelta
        }
        _changesTemp += newChange
    }

    private data class Change(
        var preStart: Int,
        var preEnd: Int,
        var originalStart: Int,
        var originalEnd: Int
    )
}