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

package androidx.compose.runtime.collection

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf

/**
 * Maps values to a set of scopes.
 */
internal class ScopeMap<T : Any> {
    val map = mutableScatterMapOf<Any, Any>()

    /**
     * The number of values in the map.
     */
    val size get() = map.size

    /**
     * Adds a [key]/[scope] pair to the map.
     */
    fun add(key: Any, scope: T) {
        map.compute(key) { _, value ->
            when (value) {
                null -> scope
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as MutableScatterSet<T>).add(scope)
                    value
                }

                else -> {
                    if (value !== scope) {
                        val set = MutableScatterSet<T>()
                        @Suppress("UNCHECKED_CAST")
                        set.add(value as T)
                        set.add(scope)
                        set
                    } else {
                        value
                    }
                }
            }
        }
    }

    /**
     * Returns true if any scopes are associated with [element]
     */
    operator fun contains(element: Any): Boolean = map.containsKey(element)

    /**
     * Executes [block] for all scopes mapped to the given [key].
     */
    inline fun forEachScopeOf(key: Any, block: (scope: T) -> Unit) {
        when (val value = map[key]) {
            null -> { /* do nothing */ }
            is MutableScatterSet<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as MutableScatterSet<T>).forEach(block)
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                block(value as T)
            }
        }
    }

    /**
     * Removes all values and scopes from the map
     */
    fun clear() {
        map.clear()
    }

    /**
     * Remove [scope] from the scope set for [key]. If the scope set is empty after [scope] has
     * been remove the reference to [key] is removed as well.
     *
     * @param key the key of the scope map
     * @param scope the scope being removed
     * @return true if the value was removed from the scope
     */
    fun remove(key: Any, scope: T): Boolean {
        val value = map[key] ?: return false
        return when (value) {
            is MutableScatterSet<*> -> {
                @Suppress("UNCHECKED_CAST")
                val set = value as MutableScatterSet<T>

                val removed = set.remove(scope)
                if (removed && set.isEmpty()) {
                    map.remove(key)
                }
                return removed
            }
            scope -> {
                map.remove(key)
                true
            }
            else -> false
        }
    }

    /**
     * Removes all scopes that match [predicate]. If all scopes for a given value have been
     * removed, that value is removed also.
     */
    inline fun removeScopeIf(crossinline predicate: (scope: T) -> Boolean) {
        map.removeIf { _, value ->
            when (value) {
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val set = value as MutableScatterSet<T>
                    set.removeIf(predicate)
                    set.isEmpty()
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    predicate(value as T)
                }
            }
        }
    }

    /**
     * Removes given scope from all sets. If all scopes for a given value are removed, that value
     * is removed as well.
     */
    fun removeScope(scope: T) {
        removeScopeIf { it === scope }
    }
}
