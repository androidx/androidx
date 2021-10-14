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

package androidx.collection

open class ArrayMap<K, V> : SimpleArrayMap<K, V>, MutableMap<K, V> {

    constructor(capacity: Int = 0) : super(capacity)

    constructor(map: SimpleArrayMap<K, V>) : super(map)

    /**
     * Determine if the array map contains all of the keys in the given collection.
     * @param collection The collection whose contents are to be checked against.
     * @return Returns true if this array map contains a key for every entry
     * in <var>collection</var>, else returns false.
     */
    open fun containsAll(collection: Collection<K>): Boolean {
        for (o in collection) {
            if (!containsKey(o)) {
                return false
            }
        }
        return true
    }

    /**
     * Perform a [put] of all key/value pairs in <var>map</var>
     * @param from The map whose contents are to be retrieved.
     */
    override fun putAll(from: Map<out K, V>) {
        ensureCapacity(_size + from.size)
        for ((key, value) in from.entries) {
            put(key, value)
        }
    }

    /**
     * Remove all keys in the array map that exist in the given collection.
     * @param collection The collection whose contents are to be used to remove keys.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    open fun removeAll(collection: Collection<K>): Boolean {
        val oldSize = _size
        for (o in collection) {
            remove(o)
        }
        return oldSize != _size
    }

    /**
     * Remove all keys in the array map that do <b>not</b> exist in the given collection.
     * @param collection The collection whose contents are to be used to determine which
     * keys to keep.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    open fun retainAll(collection: Collection<K>): Boolean {
        val oldSize = _size
        for (i in _size downTo 0) {
            if (!collection.contains(keyAt(i))) {
                removeAt(i)
            }
        }
        return oldSize != _size
    }

    /**
     * Return a [Set] for iterating over and interacting with all mappings
     * in the array map.
     *
     * <p><b>Note:</b> this is a very inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     *
     * <p><b>Note:</b></p> the semantics of this
     * Set are subtly different than that of a [HashMap]: most important,
     * the [Map.Entry] object returned by its iterator is a single
     * object that exists for the entire iterator, so you can <b>not</b> hold on to it
     * after calling [Iterator.next].</p>
     */
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return _entries ?: EntrySet(this).also { _entries = it }
        }

    private var _entries: EntrySet<K, V>? = null

    /**
     * Return a [Set] for iterating over and interacting with all keys
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     */
    override val keys: MutableSet<K>
        get() {
            return _keys ?: KeySet(this).also { _keys = it }
        }

    private var _keys: KeySet<K, V>? = null

    /**
     * Return a [Collection] for iterating over and interacting with all values
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     */
    override val values: MutableCollection<V>
        get() = _values ?: ValueCollection(this).also { _values = it }

    private var _values: ValueCollection<K, V>? = null

    // Required by compiler.
    open override fun getOrDefault(key: K, defaultValue: V): V =
        super<SimpleArrayMap>.getOrDefault(
            key, defaultValue
        )

    // Required by compiler.
    open override fun remove(key: K, value: V): Boolean = super<SimpleArrayMap>.remove(
        key,
        value
    )

    // Required by compiler.
    open override fun putIfAbsent(key: K, value: V): V? =
        super<SimpleArrayMap>.putIfAbsent(key, value)

    // Required by compiler.
    open override fun replace(key: K, value: V): V? =
        super<SimpleArrayMap>.replace(key, value)

    // Required by compiler.
    open override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        super<SimpleArrayMap>.replace(key, oldValue, newValue)
}
