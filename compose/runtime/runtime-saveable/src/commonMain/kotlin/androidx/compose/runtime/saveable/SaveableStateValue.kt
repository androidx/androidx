/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.saveable

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.saveable.SaveableStateRegistry.Entry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateValue
import androidx.savedstate.read
import androidx.savedstate.savedState

internal const val SAVEABLE_STATE_VALUE_KEY = "androidx.compose.runtime.saveable.SaveableStateValue"

internal class SaveableStateValue(
    restored: Map<String, List<Any?>>? = null,
    private val canBeSaved: (Any) -> Boolean,
) : SavedStateValue<Map<String, List<Any?>>> {

    private var restored: MutableScatterMap<String, List<Any?>>? =
        if (!restored.isNullOrEmpty()) restored.toMutableScatterMap() else null

    private var valueProviders: MutableScatterMap<String, MutableList<() -> Any?>>? = null

    fun consumeRestored(key: String): Any? {
        val list = restored?.remove(key)
        return if (!list.isNullOrEmpty()) {
            if (list.size > 1) {
                restored?.put(key, ArrayList(list.subList(1, list.size)))
            }
            list[0]
        } else {
            null
        }
    }

    fun registerProvider(key: String, valueProvider: () -> Any?): Entry {
        require(!key.fastIsBlank()) { "Registered key is empty or blank" }
        val valueProviders =
            valueProviders
                ?: mutableScatterMapOf<String, MutableList<() -> Any?>>().also {
                    this.valueProviders = it
                }
        valueProviders.getOrPut(key) { mutableListOf() }.add(valueProvider)
        return object : Entry {
            override fun unregister() {
                val list = valueProviders.remove(key)
                list?.remove(valueProvider)
                if (!list.isNullOrEmpty()) {
                    // if there are other providers for this key return list back to the map
                    valueProviders[key] = list
                }
            }
        }
    }

    override val value: Map<String, List<Any?>>
        get() {
            val restored = restored
            val valueProviders = valueProviders
            // Return early if no restored state and no providers exist.
            if (restored == null && valueProviders == null) {
                return emptyMap()
            }

            // Set initial capacity to prevent map resizing.
            val size = (restored?.size ?: 0) + (valueProviders?.size ?: 0)
            val map = MutableScatterMap<String, List<Any?>>(initialCapacity = size)

            // Keep restored state for composables not composed in this cycle.
            restored?.let { map.putAll(it) }

            // Evaluate all registered providers to collect current state.
            valueProviders?.forEach { key, list ->
                // Optimize single-provider case (most common). Prevents list creation
                // if the single provider returns null.
                if (list.size == 1) {
                    val value = list[0].invoke()
                    if (value != null) {
                        check(canBeSaved(value)) { generateCannotBeSavedErrorMessage(value) }
                        // On JVM, listOf(value) calls Collections.singletonList to
                        // prevent backing array allocation.
                        map[key] = listOf(value)
                    }
                } else {
                    // Keep nulls for multiple providers to preserve order.
                    // Example: First returns null, second returns "1". On restore,
                    // first restores null and second restores "1".
                    map[key] =
                        List(list.size) { index ->
                            val value = list[index].invoke()
                            if (value != null) {
                                check(canBeSaved(value)) {
                                    generateCannotBeSavedErrorMessage(value)
                                }
                            }
                            value
                        }
                }
            }

            return ScatterMapWrapper(map)
        }

    override fun saveState(): SavedState = savedState(value)

    @Suppress("UNCHECKED_CAST")
    override fun restoreState(savedState: SavedState?) {
        val restoredMap = savedState?.read { toMap() } as? Map<String, List<Any?>>
        restored = if (!restoredMap.isNullOrEmpty()) restoredMap.toMutableScatterMap() else null
    }
}

// CharSequence.isBlank() allocates an iterator because it calls indices.all{}
private fun CharSequence.fastIsBlank(): Boolean {
    var blank = true
    for (i in 0 until length) {
        if (!this[i].isWhitespace()) {
            blank = false
            break
        }
    }
    return blank
}

private fun <K, V> Map<K, V>.toMutableScatterMap(): MutableScatterMap<K, V> {
    return MutableScatterMap<K, V>(size).also { it += this }
}
