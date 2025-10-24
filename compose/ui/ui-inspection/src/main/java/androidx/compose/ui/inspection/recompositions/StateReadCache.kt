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

/** The default number of state reads kept in memory */
internal const val DEFAULT_MAX_STATE_READS = 5000

/**
 * A cache for State Reads where the oldest entries are discarded based on the total number of state
 * reads recorded in insertion order.
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

    private data class Key(val anchor: Any, val recomposition: Int)

    private val cache = LinkedHashMap<Key, ObservedStateReads>()

    fun clear() {
        stateReadsByComposable.values.forEach { it.clearStateReads() }
        cache.clear()
        currentStateReads = 0
    }

    fun removeAllExcept(anchors: Set<Any>) {
        val toRemove = stateReadsByComposable.keys.toMutableSet()
        toRemove.removeAll(anchors)
        toRemove.forEach { anchor ->
            val observed = stateReadsByComposable[anchor]?.clearStateReads()
            observed?.forEach { recomposition, _ ->
                val observed = cache.remove(Key(anchor, recomposition))
                observed?.reads?.let { currentStateReads -= it.size }
            }
        }
    }

    fun addInvalidation(anchor: Any, value: Any?) {
        produceStateReadsForAnchor(anchor).addInvalidation(value)
    }

    fun addStateRead(anchor: Any, value: Any, trace: Exception) {
        val data = produceStateReadsForAnchor(anchor)
        val observed = data.addStateRead(value, trace) ?: return
        currentStateReads++
        if (observed.reads.size == 1) {
            cache[Key(anchor, data.count)] = observed
        }
        while (currentStateReads > maxStateReads) {
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
        result.forEach { entry ->
            currentStateReads -= entry.reads.size
            cache.remove(Key(anchor, entry.recomposition))
        }
        return result
    }

    private fun produceStateReadsForAnchor(anchor: Any) =
        stateReadsByComposable.getOrPut(anchor) { RecompositionDataWithStateReads() }

    private fun discardEldest() {
        val entry = removeEldest() ?: return
        val data = stateReadsByComposable[entry.key.anchor] ?: return
        data.remove(entry.key.recomposition)
        currentStateReads -= entry.value.reads.size
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
