/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.collection.MutableObjectList
import androidx.collection.MutableScatterMap
import androidx.collection.ObjectList
import androidx.collection.emptyObjectList
import androidx.collection.mutableObjectListOf
import kotlin.jvm.JvmInline

/**
 * A mutable multi-map of values. This is a secondary implementation to [MultiValueMap] that removes
 * the restrictions on V. V can be nullable and instances of V are allowed to also be instances of
 * `MutableList<*>`. Note that this implementation is slightly more expensive as it will wrap values
 * that satisfy either of those constraints.
 */
@JvmInline
@Suppress("UNCHECKED_CAST")
internal value class SafeMultiValueMap<K : Any?, V : Any?>(
    private val map: MutableScatterMap<Any, Any> = MutableScatterMap()
) {
    fun add(key: K, value: V) {
        // Only create a list if there more than one value is stored in the map. Otherwise,
        // the value is stored in the map directly. If V is a MutableList<*>, we need to wrap it
        // for safety.
        map.compute(key ?: NULL_SENTINEL) { _, previous ->
            when (previous) {
                null -> value.safeWrapIfNecessary()
                is MutableObjectList<*> -> {
                    val list = previous as MutableObjectList<Any?>
                    list.add(value)
                    list
                }
                else -> mutableObjectListOf(previous.unwrapSafeValue(), value)
            }
        }
    }

    fun clear() = map.clear()

    operator fun contains(key: K) = (key ?: NULL_SENTINEL) in map

    fun isEmpty() = map.isEmpty()

    fun isNotEmpty() = map.isNotEmpty()

    fun removeLast(key: K): V? {
        val safeKey = key ?: NULL_SENTINEL
        return when (val entry = map[safeKey]) {
            null -> null
            is MutableObjectList<*> -> {
                val list = entry as MutableObjectList<V>
                val result = list.removeLast()
                if (list.size == 1) map[safeKey] = list.first().safeWrapIfNecessary()
                result
            }
            else -> {
                map.remove(safeKey)
                entry.unwrapSafeValue()
            }
        }
    }

    fun removeLast(key: K, defaultIfAbsent: V): V? {
        val safeKey = key ?: NULL_SENTINEL
        return when (val entry = map[safeKey]) {
            null -> defaultIfAbsent
            is MutableObjectList<*> -> {
                val list = entry as MutableObjectList<V>
                val result = list.removeLast()
                if (list.isEmpty()) map.remove(safeKey)
                if (list.size == 1) map[safeKey] = list.first().safeWrapIfNecessary()
                result
            }
            else -> {
                map.remove(safeKey)
                entry.unwrapSafeValue()
            }
        }
    }

    fun removeFirst(key: K, defaultIfAbsent: V): V? {
        val safeKey = key ?: NULL_SENTINEL
        return when (val entry = map[safeKey]) {
            null -> defaultIfAbsent
            is MutableObjectList<*> -> {
                val list = entry as MutableObjectList<V>
                val result = list.removeAt(0)
                if (list.isEmpty()) map.remove(safeKey)
                if (list.size == 1) map[safeKey] = list.first().safeWrapIfNecessary()
                result
            }
            else -> {
                map.remove(safeKey).unwrapSafeValue()
            }
        }
    }

    fun values(): ObjectList<V> {
        if (map.isEmpty()) return emptyObjectList()
        val result = mutableObjectListOf<V>()
        map.forEachValue { value ->
            when (value) {
                is MutableObjectList<*> -> result.addAll(value as MutableObjectList<V>)
                else -> result.add(value as V)
            }
        }
        return result
    }

    inline fun forEachValue(key: K, block: (value: V) -> Unit) {
        map[key ?: NULL_SENTINEL]?.let {
            when (it) {
                is MutableObjectList<*> -> {
                    (it as MutableObjectList<V>).forEach { value -> block(value) }
                }
                else -> block(it.unwrapSafeValue())
            }
        }
    }

    inline fun forEachValue(block: (value: V) -> Unit) {
        map.forEachValue {
            when (it) {
                is MutableObjectList<*> -> {
                    (it as MutableObjectList<V>).forEach { value -> block(value) }
                }
                else -> block(it as V)
            }
        }
    }

    internal class ValueSafetyWrapper(val value: Any)

    internal fun Any?.safeWrapIfNecessary(): Any =
        when {
            this is MutableObjectList<*> -> ValueSafetyWrapper(this)
            this != null -> this
            else -> NULL_SENTINEL
        }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun Any?.unwrapSafeValue(): V =
        when {
            this is ValueSafetyWrapper -> value
            this === NULL_SENTINEL -> null
            else -> this
        }
            as V
}

private val NULL_SENTINEL = Any()
