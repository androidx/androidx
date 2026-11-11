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

import androidx.collection.forEach
import androidx.compose.ui.inspection.inspector.RawParameter

/** The default number of state reads kept in memory */
internal const val DEFAULT_MAX_STATE_READS = 5000

/**
 * A cache for state reads and parameter changes per recomposition, where the oldest entries are
 * discarded in insertion order when there are more than [maxStateReads] state reads, or more than
 * [maxRecompositionsWithParameterChanges] recompositions with parameter changes.
 *
 * @param stateReadsByComposable Holds recomposition counts and state reads per composable.
 */
class StateReadCache(
    private val stateReadsByComposable: MutableMap<Any, RecompositionDataWithStateReads>
) {
    /** The maximum number of state reads recorded before data will be discarded to save space. */
    var maxStateReads = DEFAULT_MAX_STATE_READS

    /** The currently recorded number of state reads among all composable and recompositions. */
    var currentStateReads: Int = 0
        private set

    /**
     * The maximum number of recompositions with parameter changes recorded before data will be
     * discarded to save space.
     *
     * Note: Unlike [maxStateReads] which limits the number of individual state reads, this limits
     * the number of recompositions: A recomposition with several parameter changes counts as 1.
     */
    var maxRecompositionsWithParameterChanges = DEFAULT_MAX_STATE_READS

    /** The current number of recompositions with parameter changes. */
    var currentRecompositionsWithParameterChanges: Int = 0
        private set

    /** The number of state reads that was silently removed in the cache */
    var purgedStateReads: Long = 0L
        private set

    /** Key for the FIFO cache */
    private data class Key(val anchor: Any, val recomposition: Int)

    /** FIFO cache of ObservedStateReads instances */
    private val cache = LinkedHashMap<Key, ObservedStateReads>()

    fun clear() {
        stateReadsByComposable.values.forEach { it.clearStateReads() }
        cache.clear()
        currentStateReads = 0
        currentRecompositionsWithParameterChanges = 0
    }

    fun removeAllExcept(anchors: Set<Any>) {
        val toRemove = stateReadsByComposable.keys.toMutableSet()
        toRemove.removeAll(anchors)
        toRemove.forEach { anchor ->
            val observed = stateReadsByComposable[anchor]?.clearStateReads()
            observed?.forEach { recomposition, _ -> removeFromCache(anchor, recomposition) }
        }
    }

    fun addInvalidation(anchor: Any, value: Any?) {
        produceStateReadsForAnchor(anchor).addInvalidation(value)
    }

    fun addStateRead(anchor: Any, value: Any, trace: Exception) {
        val data = produceStateReadsForAnchor(anchor)
        val observed = data.addStateRead(value, trace) ?: return
        currentStateReads++
        if (observed.readSize == 1) {
            cache[Key(anchor, data.count)] = observed
        }
        while (currentStateReads > maxStateReads) {
            discardEldest()
        }
    }

    fun addParameterChanges(anchor: Any, parameterChanges: List<RawParameter>) {
        if (parameterChanges.isEmpty()) {
            return
        }
        val data = produceStateReadsForAnchor(anchor)
        val key = Key(anchor, data.count)
        val hadParameterChanges = cache[key]?.hasParameterChanges == true

        val observed = data.addParameterChanges(parameterChanges) ?: return
        cache[key] = observed
        if (!hadParameterChanges) {
            currentRecompositionsWithParameterChanges++
        }
        while (currentRecompositionsWithParameterChanges > maxRecompositionsWithParameterChanges) {
            discardEldest()
        }
    }

    fun getReadsAndRemove(
        anchor: Any,
        recompositionNumberStart: Int,
        recompositionNumberEnd: Int,
        includeExtra: Boolean,
    ): List<ObservedReadResult> {
        val data = stateReadsByComposable[anchor] ?: return emptyList()
        val result =
            data.getReadsAndRemove(recompositionNumberStart, recompositionNumberEnd, includeExtra)
        result.forEach { entry -> removeFromCache(anchor, entry.recomposition) }
        return result
    }

    private fun removeFromCache(anchor: Any, recomposition: Int) {
        val removed = cache.remove(Key(anchor, recomposition)) ?: return
        currentStateReads -= removed.readSize
        if (removed.hasParameterChanges) {
            currentRecompositionsWithParameterChanges--
        }
    }

    private fun produceStateReadsForAnchor(anchor: Any) =
        stateReadsByComposable.getOrPut(anchor) { RecompositionDataWithStateReads() }

    private fun discardEldest() {
        val entry = removeEldest()
        if (entry == null) {
            // This should not happen. Reset the counts to avoid endless loop.
            currentStateReads = 0
            currentRecompositionsWithParameterChanges = 0
            return
        }
        stateReadsByComposable[entry.key.anchor]?.remove(entry.key.recomposition)
        if (entry.value.hasParameterChanges) {
            currentRecompositionsWithParameterChanges--
        }
        currentStateReads -= entry.value.readSize
        purgedStateReads += entry.value.readSize
    }

    private fun removeEldest(): Map.Entry<Key, ObservedStateReads>? {
        val it = cache.iterator()
        if (!it.hasNext()) {
            return null
        }
        val entry = it.next()
        it.remove()
        return entry
    }
}
