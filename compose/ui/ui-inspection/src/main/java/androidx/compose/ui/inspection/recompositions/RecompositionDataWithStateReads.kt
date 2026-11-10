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

import androidx.collection.SparseArrayCompat

/** The state reads stored per @Composable. */
class RecompositionDataWithStateReads : RecompositionData() {
    // The state reads per recomposition count starting with 1, sorted by recomposition number.
    // Entries are usually added in increasing order, which makes adding an entry cheap.
    private var observed: SparseArrayCompat<ObservedStateReads>? = null

    override fun incrementCount() {
        // The invalidations for the previous recomposition are no longer needed,
        // since new recorded state reads will be for the current recomposition count.
        observed?.get(count - 1)?.clearInvalidations()

        super.incrementCount()
    }

    /**
     * Expect state reads to be recorded for the last recorded recomposition. Add empty
     * observations.
     */
    fun expectStateReads(): ObservedStateReads {
        return addObservedStateReads(count)
    }

    // Add an observed state read for the current recomposition:
    fun addStateRead(value: Any?, trace: Exception): ObservedStateReads? {
        if (lastCountWasSkipped || count <= 0) {
            return null
        }
        val reads = addObservedStateReads(count)
        reads.addStateRead(value, trace)
        return reads
    }

    // Add an observed state variable invalidation for the next upcoming recomposition:
    fun addInvalidation(value: Any?) {
        val reads = addObservedStateReads(count + 1)
        reads.addInvalidation(value)
    }

    fun remove(recomposition: Int) {
        observed?.remove(recomposition)
    }

    /**
     * Get the state reads from the specified range of recompositions as [ObservedReadResult] and
     * remove them from the [observed] state reads. Note: there may be recompositions without state
     * reads in the [observed] map.
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
        val existing = observed ?: return emptyList()
        val max = if (includeExtra) count else minOf(recompositionNumberEnd, count)
        val rangeSize = recompositionNumberEnd - recompositionNumberStart + 1
        val result = mutableListOf<ObservedReadResult>()
        val first = indexOfFirstRecompositionAtOrAfter(existing, recompositionNumberStart)
        var index = first
        while (index < existing.size() && (result.isEmpty() || result.size < rangeSize)) {
            val recomposition = existing.keyAt(index)
            if (recomposition > max) {
                break
            }
            var stateRead = existing.valueAt(index)
            if (recomposition == count) {
                // If this the most recent recomposition, there may still be state reads
                // being recorded, make a copy:
                stateRead = stateRead.copy()
            }
            result.add(stateRead.toObservedReadResult(recomposition))
            index++
        }
        // Note: Do not use SparseArrayCompat.removeAtRange, which removes too few mappings in
        // released versions of androidx.collection. The version used at runtime is the one used by
        // the inspected app.
        // Note: removeAt only marks the entry as deleted, so the indices do not shift.
        for (i in first until index) {
            existing.removeAt(i)
        }
        return result
    }

    /**
     * Return the index in [existing] of the first recomposition at or after [recomposition], or the
     * size of [existing] if there is none.
     */
    private fun indexOfFirstRecompositionAtOrAfter(
        existing: SparseArrayCompat<ObservedStateReads>,
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

    fun clearStateReads(): SparseArrayCompat<ObservedStateReads>? {
        val result = observed
        observed = null
        return result
    }

    private fun addObservedStateReads(recomposition: Int): ObservedStateReads {
        val existing = observed ?: SparseArrayCompat<ObservedStateReads>().also { observed = it }
        return existing.get(recomposition)
            ?: ObservedStateReads().also { existing.append(recomposition, it) }
    }
}
