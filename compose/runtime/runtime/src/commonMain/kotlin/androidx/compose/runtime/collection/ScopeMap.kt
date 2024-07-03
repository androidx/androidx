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

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import kotlin.jvm.JvmInline

/** Maps values to a set of scopes. */
@JvmInline
internal value class ScopeMap<Key : Any, Scope : Any>(
    val map: MutableScatterMap<Any, Any> = mutableScatterMapOf(),
) {

    /** The number of values in the map. */
    val size
        get() = map.size

    /** Adds a [key]/[scope] pair to the map. */
    fun add(key: Key, scope: Scope) {
        map.compute(key) { _, value ->
            when (value) {
                null -> scope
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST") (value as MutableScatterSet<Scope>).add(scope)
                    value
                }
                else -> {
                    if (value !== scope) {
                        val set = MutableScatterSet<Scope>()
                        @Suppress("UNCHECKED_CAST") set.add(value as Scope)
                        set.add(scope)
                        set
                    } else {
                        value
                    }
                }
            }
        }
    }

    /** Replaces scopes for [key] with [value] */
    fun set(key: Key, value: Scope) {
        map[key] = value
    }

    /** Returns true if any scopes are associated with [element] */
    operator fun contains(element: Key): Boolean = map.containsKey(element)

    /** Executes [block] for all scopes mapped to the given [key]. */
    inline fun forEachScopeOf(key: Key, block: (scope: Scope) -> Unit) {
        when (val value = map[key]) {
            null -> {
                /* do nothing */
            }
            is MutableScatterSet<*> -> {
                @Suppress("UNCHECKED_CAST") (value as MutableScatterSet<Scope>).forEach(block)
            }
            else -> {
                @Suppress("UNCHECKED_CAST") block(value as Scope)
            }
        }
    }

    inline fun anyScopeOf(key: Key, block: (scope: Scope) -> Boolean): Boolean {
        forEachScopeOf(key) { if (block(it)) return true }
        return false
    }

    /** Removes all values and scopes from the map */
    fun clear() {
        map.clear()
    }

    /**
     * Remove [scope] from the scope set for [key]. If the scope set is empty after [scope] has been
     * remove the reference to [key] is removed as well.
     *
     * @param key the key of the scope map
     * @param scope the scope being removed
     * @return true if the value was removed from the scope
     */
    fun remove(key: Key, scope: Scope): Boolean {
        val value = map[key] ?: return false
        return when (value) {
            is MutableScatterSet<*> -> {
                @Suppress("UNCHECKED_CAST") val set = value as MutableScatterSet<Scope>

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
     * Removes all scopes that match [predicate]. If all scopes for a given value have been removed,
     * that value is removed also.
     */
    inline fun removeScopeIf(crossinline predicate: (scope: Scope) -> Boolean) {
        map.removeIf { _, value ->
            when (value) {
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST") val set = value as MutableScatterSet<Scope>
                    set.removeIf(predicate)
                    set.isEmpty()
                }
                else -> {
                    @Suppress("UNCHECKED_CAST") predicate(value as Scope)
                }
            }
        }
    }

    /**
     * Removes given scope from all sets. If all scopes for a given value are removed, that value is
     * removed as well.
     */
    fun removeScope(scope: Scope) {
        map.removeIf { _, value ->
            when (value) {
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST") val set = value as MutableScatterSet<Scope>
                    set.remove(scope)
                    set.isEmpty()
                }
                else -> {
                    value === scope
                }
            }
        }
    }

    /**
     * Converts values to regular Map to expose to instrumentation. WARNING: extremely slow, do no
     * use in production!
     */
    fun asMap(): Map<Key, Set<Scope>> {
        val result = hashMapOf<Key, Set<Scope>>()
        map.forEach { key, value ->
            @Suppress("UNCHECKED_CAST")
            result[key as Key] =
                when (value) {
                    is MutableScatterSet<*> -> {
                        val set = value as MutableScatterSet<Scope>
                        @Suppress("AsCollectionCall") set.asSet()
                    }
                    else -> mutableSetOf(value as Scope)
                }
        }
        return result
    }
}
