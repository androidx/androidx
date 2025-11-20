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
import androidx.collection.objectListOf
import androidx.compose.runtime.debugRuntimeCheck
import kotlin.jvm.JvmInline

/**
 * A mutable multi-map of values.
 *
 * Warning: the type constraints are insufficient to adequately describe the limitations of this
 * class. This can only be used if V is not nullable and V is not a MutableList<*> or can be
 * implemented by one (i.e. for all instances of v: V there is no v for which v is MutableList<*> is
 * true).
 */
@JvmInline
@Suppress("UNCHECKED_CAST")
internal value class MultiValueMap<K : Any, V : Any>(
    private val map: MutableScatterMap<Any, Any> = MutableScatterMap()
) {
    fun add(key: K, value: V) {
        // Only create a list if there more than one value is stored in the map. Otherwise,
        // the value is stored in the map directly. This only works if V is not a MutableList<*>.
        map.compute(key) { _, previous ->
            debugRuntimeCheck(previous !is MutableList<*>) { "Unexpected value" }
            when (previous) {
                null -> value
                is MutableObjectList<*> -> {
                    val list = previous as MutableObjectList<Any>
                    list.add(value)
                    list
                }
                else -> mutableObjectListOf(previous, value)
            }
        }
    }

    fun clear() = map.clear()

    operator fun contains(key: K) = key in map

    operator fun get(key: K): ObjectList<V> =
        when (val entry = map[key]) {
            null -> emptyObjectList()
            is MutableObjectList<*> -> entry as ObjectList<V>
            else -> objectListOf(entry as V)
        }

    fun isEmpty() = map.isEmpty()

    fun isNotEmpty() = map.isNotEmpty()

    fun removeLast(key: K): V? =
        when (val entry = map[key]) {
            null -> null
            is MutableObjectList<*> -> {
                val list = entry as MutableObjectList<Any>
                val result = list.removeLast() as V
                if (list.isEmpty()) map.remove(key)
                if (list.size == 1) map[key] = list.first()
                result
            }
            else -> {
                map.remove(key)
                entry as V
            }
        }

    fun removeFirst(key: K): V? =
        when (val entry = map[key]) {
            null -> null
            is MutableObjectList<*> -> {
                val list = entry as MutableObjectList<V>
                val result = list.removeAt(0)
                if (list.isEmpty()) map.remove(key)
                if (list.size == 1) map[key] = list.first()
                result
            }
            else -> {
                map.remove(key)
                entry as V
            }
        }

    fun values(): ObjectList<V> {
        if (map.isEmpty()) return emptyObjectList()
        val result = mutableObjectListOf<V>()
        map.forEachValue { entry ->
            when (entry) {
                is MutableObjectList<*> -> result.addAll(entry as MutableObjectList<V>)
                else -> result.add(entry as V)
            }
        }
        return result
    }

    inline fun forEachValue(key: K, block: (value: V) -> Unit) {
        map[key]?.let {
            when (it) {
                is MutableObjectList<*> -> {
                    it.forEach { value -> block(value as V) }
                }
                else -> block(it as V)
            }
        }
    }

    inline fun forEachValue(block: (value: V) -> Unit) {
        map.forEachValue {
            when (it) {
                is MutableObjectList<*> -> {
                    it.forEach { value -> block(value as V) }
                }
                else -> block(it as V)
            }
        }
    }

    fun removeValueIf(key: K, condition: (value: V) -> Boolean) {
        map[key]?.let {
            when (it) {
                is MutableObjectList<*> -> {
                    (it as MutableObjectList<V>).removeIf(condition)
                    if (it.isEmpty()) map.remove(key)
                    if (it.size == 0) map[key] = it.first()
                }
                else -> if (condition(it as V)) map.remove(key)
            }
        }
    }
}
