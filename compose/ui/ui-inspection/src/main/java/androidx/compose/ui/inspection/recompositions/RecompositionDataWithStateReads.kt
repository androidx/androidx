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

package androidx.compose.ui.inspection.recompositions

import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.SparseArrayCompat
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.compose.ui.inspection.inspector.RawParameter

// Marker stored in [RecompositionDataWithStateReads.observed] for a recomposition at or after
// [firstObserved] that was already returned, when earlier recompositions were not returned yet.
private val RETURNED: ObservedStateReads? = null

/** The state reads stored per @Composable. */
class RecompositionDataWithStateReads : RecompositionData() {
    // The state reads per recomposition count starting with 1, sorted by recomposition number.
    // Entries are usually added in increasing order, which makes adding an entry cheap.
    // A [RETURNED] value marks a recomposition >= [firstObserved] that was already returned.
    private var observed: SparseArrayCompat<ObservedStateReads?>? = null

    /**
     * The first recomposition with complete observations, or -1 if this composable is not observed.
     *
     * A recomposition in [firstObserved]..[count] without an entry in [observed] was recomposed
     * without state reads or parameter changes. This lets [getReadsAndRemove] tell such a
     * recomposition apart from a recomposition that has not happened yet, without storing an empty
     * entry for every recomposition.
     *
     * Set by [markObserved] for the first recomposition after observation started, and reset by
     * [clearStateReads] when observation stops. Moves forward when older entries are evicted with
     * [remove], or returned by [getReadsAndRemove].
     */
    private var firstObserved = -1

    // The invalidated state variables (identityHashCode) by the recomposition they apply to.
    // These are kept outside [observed] such that every entry in [observed] has either state
    // reads or parameter changes, and is accounted for in the [StateReadCache].
    private var invalidations: MutableIntObjectMap<MutableIntSet>? = null

    override fun incrementCount() {
        super.incrementCount()
        // Invalidations recorded for earlier recompositions are no longer needed.
        removeInvalidationsBefore(count)
    }

    /**
     * Mark the current recomposition as observed. Observations are complete from the first
     * recomposition marked after [clearStateReads].
     */
    fun markObserved() {
        if (firstObserved < 0 && count > 0) {
            firstObserved = count
        }
    }

    // Add an observed state read for the current recomposition:
    fun addStateRead(value: Any?, trace: Exception): ObservedStateReads? {
        if (lastCountWasSkipped || count <= 0) {
            return null
        }
        val reads = addObservedStateReads(count)
        reads.addStateRead(value, trace, invalidations?.get(count))
        return reads
    }

    fun addParameterChanges(parameterChanges: List<RawParameter>): ObservedStateReads? {
        if (lastCountWasSkipped || count <= 0) {
            return null
        }
        val reads = addObservedStateReads(count)
        reads.addParameterChanges(parameterChanges)
        return reads
    }

    // Add an observed state variable invalidation for the next upcoming recomposition:
    fun addInvalidation(value: Any?) {
        val valueInstanceHash = value?.let { System.identityHashCode(it) } ?: return
        val map =
            invalidations ?: mutableIntObjectMapOf<MutableIntSet>().also { invalidations = it }
        map.getOrPut(count + 1) { mutableIntSetOf() }.add(valueInstanceHash)
    }

    private fun removeInvalidationsBefore(recomposition: Int) {
        val map = invalidations ?: return
        map.removeIf { key, _ -> key < recomposition }
        if (map.isEmpty()) {
            invalidations = null
        }
    }

    fun remove(recomposition: Int) {
        observed?.remove(recomposition)
        if (firstObserved in 0..recomposition) {
            // The entries are evicted oldest first, so the observations before this point are gone.
            advanceFirstObservedTo(recomposition + 1)
        }
    }

    /**
     * Get the state reads from the specified range of recompositions as [ObservedReadResult] and
     * remove them from the [observed] state reads.
     *
     * A recomposition from [firstObserved] without an entry in [observed] is returned with no state
     * reads and no parameter changes, since it was recomposed without any.
     *
     * @param recompositionNumberStart the lower bounds of the recomposition range
     * @param recompositionNumberEnd the upper bounds of the recomposition range
     * @param includeExtra include extra state reads after recompositionNumberEnd if state reads are
     *   missing from the requested range
     */
    fun getReadsAndRemove(
        recompositionNumberStart: Int,
        recompositionNumberEnd: Int,
        includeExtra: Boolean,
    ): List<ObservedReadResult> {
        val existing = observed
        if (existing == null && firstObserved < 0) {
            return emptyList()
        }
        val max = if (includeExtra) count else minOf(recompositionNumberEnd, count)
        val rangeSize = recompositionNumberEnd - recompositionNumberStart + 1
        val firstEmpty = if (firstObserved < 0) Int.MAX_VALUE else firstObserved
        val result = mutableListOf<ObservedReadResult>()
        val first =
            if (existing != null) {
                indexOfFirstRecompositionAtOrAfter(existing, recompositionNumberStart)
            } else {
                0
            }
        var index = first
        var recomposition = recompositionNumberStart
        val existingSize = existing?.size() ?: 0
        while (result.isEmpty() || result.size < rangeSize) {
            val nextEntry =
                if (existing != null && index < existingSize) existing.keyAt(index)
                else Int.MAX_VALUE
            if (recomposition < firstEmpty) {
                // Before the observations are complete, only recompositions with entries are known:
                recomposition = maxOf(recomposition, minOf(nextEntry, firstEmpty))
            }
            if (recomposition > max) {
                break
            }
            if (recomposition == nextEntry) {
                existing?.valueAt(index++)?.let { stateRead ->
                    // If this is the most recent recomposition, there may still be state reads
                    // being recorded, make a copy:
                    val copy = if (recomposition == count) stateRead.copy() else stateRead
                    result.add(copy.toObservedReadResult(recomposition))
                }
            } else {
                result.add(ObservedReadResult(recomposition, emptyList(), emptyList()))
            }
            recomposition++
        }
        if (firstObserved < 0 || recompositionNumberStart <= firstObserved) {
            // Note: Do not use SparseArrayCompat.removeAtRange, which removes too few mappings in
            // released versions of androidx.collection. The version used at runtime is the one
            // used by the inspected app.
            // Note: removeAt only marks the entry as deleted, so the indices do not shift.
            for (i in first until index) {
                existing?.removeAt(i)
            }
            advanceFirstObservedTo(recomposition)
        } else if (result.isNotEmpty()) {
            // Earlier recompositions in [firstObserved, recompositionNumberStart) were not
            // returned yet, so mark the returned recompositions >= [firstObserved] as [RETURNED]
            // to avoid returning them again as recompositions without state reads.
            val target = existing ?: SparseArrayCompat<ObservedStateReads?>().also { observed = it }
            for (entry in result) {
                if (entry.recomposition < firstObserved) {
                    target.remove(entry.recomposition)
                } else {
                    target.put(entry.recomposition, RETURNED)
                }
            }
        }
        return result
    }

    /**
     * Advance [firstObserved] to [recomposition] and remove any [RETURNED] markers below or
     * contiguous with the new [firstObserved].
     */
    private fun advanceFirstObservedTo(recomposition: Int) {
        if (firstObserved !in 0..<recomposition) {
            return
        }
        firstObserved = recomposition
        val existing = observed ?: return
        var i = indexOfFirstRecompositionAtOrAfter(existing, firstObserved)
        while (
            i < existing.size() &&
                existing.keyAt(i) == firstObserved &&
                existing.valueAt(i) == RETURNED
        ) {
            firstObserved++
            i++
        }
        // Iterate backwards because valueAt compacts deleted entries if garbage is true.
        for (j in i - 1 downTo 0) {
            if (existing.valueAt(j) == RETURNED) {
                existing.removeAt(j)
            }
        }
    }

    /**
     * Return the index in [existing] of the first recomposition at or after [recomposition], or the
     * size of [existing] if there is none.
     */
    private fun indexOfFirstRecompositionAtOrAfter(
        existing: SparseArrayCompat<ObservedStateReads?>,
        recomposition: Int,
    ): Int {
        // The keys are sorted, so do a binary search:
        var low = 0
        var high = existing.size()
        while (low < high) {
            val mid = (low + high) ushr 1
            if (existing.keyAt(mid) < recomposition) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    fun clearStateReads(): SparseArrayCompat<ObservedStateReads?>? {
        val result = observed
        observed = null
        firstObserved = -1
        invalidations = null
        return result
    }

    private fun addObservedStateReads(recomposition: Int): ObservedStateReads {
        val existing = observed ?: SparseArrayCompat<ObservedStateReads?>().also { observed = it }
        return existing.get(recomposition)
            ?: ObservedStateReads().also { existing.append(recomposition, it) }
    }
}
