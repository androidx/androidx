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
import androidx.collection.emptyIntObjectMap
import androidx.collection.mutableIntObjectMapOf

/** The state reads stored per @Composable. */
class RecompositionDataWithStateReads : RecompositionData() {
    // The state reads per recomposition count starting with 1
    private var observed: MutableIntObjectMap<ObservedStateReads>? = null
    var firstObserved = -1
        private set

    // Number of recompositions with state reads
    val recompositionsWithObservations: Int
        get() = observed?.size ?: 0

    override fun incrementCount() {
        // The invalidations for the previous recomposition are no longer needed,
        // since new recorded state reads will be for the current recomposition count.
        observed?.get(count - 1)?.clearInvalidations()

        super.incrementCount()
    }

    // Add an observed state read for the current recomposition:
    fun addStateRead(value: Any?, trace: Exception) {
        if (!lastCountWasSkipped && count > 0) {
            val reads = addObservedStateReads(count)
            reads.addStateRead(value, trace)
            if (firstObserved < 0) {
                firstObserved = count
            }
        }
    }

    // Add an observed state variable invalidation for the next upcoming recomposition:
    fun addInvalidation(value: Any?) {
        val reads = addObservedStateReads(count + 1)
        reads.addInvalidation(value)
    }

    fun discardExcessStateReads(max: Int) {
        val observed = this.observed ?: return
        while (max > 0 && recompositionsWithObservations > max) {
            observed.remove(firstObserved++)
        }
    }

    // Return the state reads for the specified recomposition:
    fun getReads(recomposition: Int, remove: Boolean): ObservedStateReads? {
        if (!remove) {
            return observed?.get(recomposition)
        }
        val reads = observed?.remove(recomposition)
        if (firstObserved == recomposition) {
            firstObserved = if (recompositionsWithObservations > 0) firstObserved + 1 else -1
        }
        return reads
    }

    // Return all state reads up to and including [upToRecomposition], and remove them:
    fun getAndRemoveReads(upToRecomposition: Int): IntObjectMap<ObservedStateReads> {
        val result = observed ?: return emptyIntObjectMap()
        val newObserved = mutableIntObjectMapOf<ObservedStateReads>()
        for (recomposition in upToRecomposition + 1..count + 1) {
            result.remove(recomposition)?.let { newObserved.put(recomposition, it) }
        }
        observed = newObserved
        firstObserved = if (recompositionsWithObservations > 0) upToRecomposition + 1 else -1
        return result
    }

    fun clearStateReads() {
        observed = null
        firstObserved = -1
    }

    private fun addObservedStateReads(recomposition: Int): ObservedStateReads {
        if (observed == null) {
            observed = mutableIntObjectMapOf()
        }
        return observed!!.getOrPut(recomposition) { ObservedStateReads() }
    }
}
