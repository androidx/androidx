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

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf

/** The state reads stored per @Composable. */
class RecompositionDataWithStateReads : RecompositionData() {
    // The state reads per recomposition count starting with 1
    private var observed: MutableIntObjectMap<ObservedStateReads>? = null
    private var firstObserved = -1

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
    fun expectStateReads() {
        addObservedStateReads(count)
    }

    // Add an observed state read for the current recomposition:
    fun addStateRead(value: Any?, trace: Exception): ObservedStateReads? {
        if (lastCountWasSkipped || count <= 0) {
            return null
        }
        val reads = addObservedStateReads(count)
        reads.addStateRead(value, trace)
        if (firstObserved < 0) {
            firstObserved = count
        }
        return reads
    }

    // Add an observed state variable invalidation for the next upcoming recomposition:
    fun addInvalidation(value: Any?) {
        val reads = addObservedStateReads(count + 1)
        reads.addInvalidation(value)
    }

    fun remove(recomposition: Int) {
        val removed = observed?.remove(recomposition)
        if (removed != null && recomposition == firstObserved) {
            firstObserved = findNext(recomposition + 1)
        }
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
        if (observed == null) {
            return emptyList()
        }
        val result = mutableListOf<ObservedReadResult>()
        val actual = maxOf(firstObserved, recompositionNumberStart)
        val max = if (includeExtra) count else minOf(recompositionNumberEnd, count)
        var found = removeNextStateRead(actual, max)
        found?.let { result.add(it) }
        val rangeSize = recompositionNumberEnd - recompositionNumberStart + 1
        while (found != null && result.size < rangeSize) {
            found = removeNextStateRead(found.recomposition + 1, max)
            found?.let { result.add(it) }
        }
        if (result.isNotEmpty()) {
            if (result.first().recomposition == firstObserved) {
                firstObserved = findNext(result.last().recomposition + 1)
            }
        }
        return result
    }

    private fun findNext(start: Int): Int {
        val existing = observed ?: return -1
        var actual = start
        while (!existing.contains(actual) && actual <= count) {
            actual++
        }
        return if (actual <= count) actual else -1
    }

    // Remove the first recomposition found by going up from [start] but not exceeding [max]
    private fun removeNextStateRead(start: Int, max: Int): ObservedReadResult? {
        if (start > max) {
            return null
        }
        val existing = observed ?: return null
        var actual = start
        var stateRead = existing.remove(actual)
        while (stateRead == null && actual < max) {
            stateRead = existing.remove(++actual)
        }
        if (actual == count) {
            // If this the most recent recomposition, there may still be state reads
            // being recorded, make a copy:
            stateRead = stateRead?.copy()
        }
        return stateRead?.let { ObservedReadResult(actual, it.reads) }
    }

    fun clearStateReads(): IntObjectMap<ObservedStateReads>? {
        val result = observed
        observed = null
        firstObserved = -1
        return result
    }

    private fun addObservedStateReads(recomposition: Int): ObservedStateReads {
        if (observed == null) {
            observed = mutableIntObjectMapOf()
        }
        return observed!!.getOrPut(recomposition) { ObservedStateReads() }
    }
}
