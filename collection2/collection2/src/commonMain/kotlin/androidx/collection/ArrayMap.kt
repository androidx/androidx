/*
 * Copyright 2021 The Android Open Source Project
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

/** Avoid conflict (and R8 dup. classes failures) with collection-ktx. */
// TODO: Remove this after collection-ktx is merged
@file:JvmName("ArrayMap_Ext")

package androidx.collection

import kotlin.collections.MutableMap.MutableEntry
import kotlin.jvm.JvmName

internal class EntrySet<K, V>(private val map: SimpleArrayMap<K, V>) :
    AbstractMutableSet<MutableEntry<K, V>>() {
    override fun iterator(): MutableIterator<MutableEntry<K, V>> {
        return EntryIterator(map)
    }

    override val size: Int get() = map.size

    override fun add(element: MutableEntry<K, V>): Boolean {
        // This is the only correct answer, because there is no good answer to what should
        // happen when you add an entry to the set with the same key as an existing entry.
        throw UnsupportedOperationException()
    }
}

internal class EntryIterator<K, V>(private val map: SimpleArrayMap<K, V>) :
    IndexBasedMutableIterator<MutableEntry<K, V>>(map.size), MutableEntry<K, V> {
    override fun get(index: Int): MutableEntry<K, V> = this
    override fun remove(index: Int) {
        map.removeAt(index)
    }

    override val key: K get() = map.keyAt(index)
    override val value: V get() = map.valueAt(index)
    override fun setValue(newValue: V): V {
        return map.setValueAt(index, newValue)
    }
}

internal fun <T> equalsSetHelper(set: Set<T>, other: Any?): Boolean {
    if (set === other) {
        return true
    }
    if (other is Set<*>) {
        val s = other
        try {
            return set.size == s.size && set.containsAll(s)
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }
    }
    return false
}

internal class KeySet<K, V>(private val map: SimpleArrayMap<K, V>) : MutableSet<K> {
    override fun add(element: K): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<K>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        map.clear()
    }

    override fun iterator(): MutableIterator<K> {
        return KeyIterator(map)
    }

    override fun remove(element: K): Boolean {
        val index = map.indexOfKey(element)
        if (index >= 0) {
            map.removeAt(index)
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<K>): Boolean {
        val oldSize = map.size
        for (element in elements) {
            map.remove(element)
        }
        return oldSize != map.size
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        val oldSize = map.size
        for (i in oldSize - 1 downTo 0) {
            if (map.keyAt(i) !in elements) {
                map.removeAt(i)
            }
        }
        return oldSize != map.size
    }

    override val size: Int get() = map.size

    override fun contains(element: K): Boolean {
        return map.containsKey(element)
    }

    override fun containsAll(elements: Collection<K>): Boolean {
        return elements.all { map.containsKey(it) }
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        return equalsSetHelper<K>(this, other)
    }

    override fun hashCode(): Int {
        var result = 0
        for (i in map.size - 1 downTo 0) {
            val obj: K = map.keyAt(i)
            result += obj?.hashCode() ?: 0
        }
        return result
    }
}

internal class KeyIterator<K, V>(private val map: SimpleArrayMap<K, V>) :
    IndexBasedMutableIterator<K>(map.size) {

    override fun get(index: Int): K = map.keyAt(index)
    override fun remove(index: Int) {
        map.removeAt(index)
    }
}

internal class ValueCollection<K, V>(private val map: SimpleArrayMap<K, V>) : MutableCollection<V> {
    override val size: Int get() = map.size

    override fun contains(element: V): Boolean {
        return map.containsValue(element)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all(map::containsValue)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun add(element: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        map.clear()
    }

    override fun iterator(): MutableIterator<V> {
        return ValueIterator(map)
    }

    override fun remove(element: V): Boolean {
        val index = map.indexOfValue(element)
        if (index >= 0) {
            map.removeAt(index)
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        val oldSize = map.size
        for (element in elements) {
            remove(element)
        }
        return oldSize != map.size
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        val oldSize = map.size
        for (i in oldSize - 1 downTo 0) {
            if (map.valueAt(i) !in elements) {
                map.removeAt(i)
            }
        }
        return oldSize != map.size
    }
}

internal class ValueIterator<K, V>(private val map: SimpleArrayMap<K, V>) :
    IndexBasedMutableIterator<V>(map.size) {

    override fun get(index: Int): V = map.valueAt(index)
    override fun remove(index: Int) {
        map.removeAt(index)
    }
}
