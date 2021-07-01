/*
 * Copyright 2020 The Android Open Source Project
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
@file:JvmName("LongSparseArray_Ext")
@file:Suppress("NOTHING_TO_INLINE") // Avoiding additional invocation indirection.

package androidx.collection

import kotlin.jvm.JvmName

expect class LongSparseArray<E>(initialCapacity: Int = 10) {
    internal var keys: LongArray
    internal var values: Array<Any?>
    internal var garbage: Boolean

    @Suppress("PropertyName") // Normal backing field name but internal for common code.
    internal var _size: Int

    constructor(array: LongSparseArray<E>)

    val size: Int
    fun isEmpty(): Boolean

    operator fun get(key: Long): E?
    fun get(key: Long, default: E): E

    fun put(key: Long, value: E) // TODO operator
    fun putAll(other: LongSparseArray<out E>)
    fun putIfAbsent(key: Long, value: E): E?
    fun append(key: Long, value: E)

    fun keyAt(index: Int): Long

    fun valueAt(index: Int): E
    fun setValueAt(index: Int, value: E)

    fun indexOfKey(key: Long): Int
    fun indexOfValue(value: E): Int

    fun containsKey(key: Long): Boolean
    fun containsValue(value: E): Boolean

    fun clear()

    fun remove(key: Long)
    fun remove(key: Long, value: Any?): Boolean
    fun removeAt(index: Int)

    fun replace(key: Long, value: E): E?
    fun replace(key: Long, oldValue: E?, newValue: E): Boolean
}

internal inline fun <E> LongSparseArray<E>.commonSize(): Int {
    if (garbage) {
        gc()
    }
    return _size
}

internal inline fun <E> LongSparseArray<E>.commonIsEmpty(): Boolean {
    return size == 0
}

internal inline fun <T, E : T> LongSparseArray<E>.commonGet(key: Long, default: T): T {
    val i = keys.binarySearch(_size, key)
    if (i >= 0) {
        val value = values[i]
        if (value !== DELETED) {
            @Suppress("UNCHECKED_CAST") // Guaranteed by positive index and DELETED check.
            return value as E
        }
    }
    return default
}

internal inline fun <E> LongSparseArray<E>.commonPut(key: Long, value: E) {
    var index = keys.binarySearch(_size, key)
    if (index >= 0) {
        values[index] = value
    } else {
        index = index.inv()
        if (index < _size && values[index] === DELETED) {
            keys[index] = key
            values[index] = value
            return
        }
        if (garbage && _size >= keys.size) {
            gc()
            // Search again because indices may have changed.
            index = keys.binarySearch(_size, key).inv()
        }
        if (_size >= keys.size) {
            val newSize = idealIntArraySize(_size + 1)
            keys = keys.copyOf(newSize)
            values = values.copyOf(newSize)
        }
        if (_size - index != 0) {
            keys.copyInto(keys, destinationOffset = index + 1, startIndex = index, endIndex = _size)
            values.copyInto(
                values,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = _size
            )
        }
        keys[index] = key
        values[index] = value
        _size++
    }
}

internal inline fun <E> LongSparseArray<E>.commonPutAll(other: LongSparseArray<out E>) {
    for (i in 0 until other.size) {
        @Suppress("UNCHECKED_CAST") // Guaranteed by valid index.
        put(other.keys[i], other.values[i] as E)
    }
}

internal inline fun <E> LongSparseArray<E>.commonPutIfAbsent(key: Long, value: E): E? {
    val mapValue = get(key)
    if (mapValue == null) {
        // TODO avoid double binary search here
        put(key, value)
    }
    return mapValue
}

internal inline fun <E> LongSparseArray<E>.commonAppend(key: Long, value: E) {
    if (_size != 0 && key <= keys[_size - 1]) {
        put(key, value)
        return
    }
    if (garbage && _size >= keys.size) {
        gc()
    }
    val pos = _size
    if (pos >= keys.size) {
        val newSize = idealIntArraySize(pos + 1)
        keys = keys.copyOf(newSize)
        values = values.copyOf(newSize)
    }
    keys[pos] = key
    values[pos] = value
    _size = pos + 1
}

internal inline fun <E> LongSparseArray<E>.commonKeyAt(index: Int): Long {
    if (garbage) {
        gc()
    }
    return keys[index]
}

internal inline fun <E> LongSparseArray<E>.commonValueAt(index: Int): E {
    if (garbage) {
        gc()
    }
    @Suppress("UNCHECKED_CAST") // Guaranteed by having run GC.
    return values[index] as E
}

internal inline fun <E> LongSparseArray<E>.commonSetValueAt(index: Int, value: E) {
    if (garbage) {
        gc()
    }
    values[index] = value
}

internal inline fun <E> LongSparseArray<E>.commonIndexOfKey(key: Long): Int {
    if (garbage) {
        gc()
    }
    return keys.binarySearch(_size, key)
}

internal inline fun <E> LongSparseArray<E>.commonIndexOfValue(value: E): Int {
    if (garbage) {
        gc()
    }
    for (i in 0 until _size) {
        if (values[i] === value) {
            return i
        }
    }
    return -1
}

internal inline fun <E> LongSparseArray<E>.commonContainsKey(key: Long): Boolean {
    return indexOfKey(key) >= 0
}

internal inline fun <E> LongSparseArray<E>.commonContainsValue(value: E): Boolean {
    return indexOfValue(value) >= 0
}

internal inline fun <E> LongSparseArray<E>.commonClear() {
    values.fill(null, toIndex = _size)
    _size = 0
    garbage = false
}

internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long) {
    val index = keys.binarySearch(_size, key)
    if (index >= 0 && values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long, value: Any?): Boolean {
    val index = indexOfKey(key)
    if (index >= 0) {
        val mapValue = valueAt(index)
        if (value == mapValue) {
            removeAt(index)
            return true
        }
    }
    return false
}

internal inline fun <E> LongSparseArray<E>.commonRemoveAt(index: Int) {
    if (values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

internal inline fun <E> LongSparseArray<E>.commonReplace(key: Long, value: E): E? {
    val index = indexOfKey(key)
    if (index >= 0) {
        @Suppress("UNCHECKED_CAST") // Guaranteed by index which would have run GC.
        val oldValue = values[index] as E
        values[index] = value
        return oldValue
    }
    return null
}

internal inline fun <E> LongSparseArray<E>.commonReplace(
    key: Long,
    oldValue: E?,
    newValue: E
): Boolean {
    val index = indexOfKey(key)
    if (index >= 0) {
        val mapValue = values[index]
        if (mapValue == oldValue) {
            values[index] = newValue
            return true
        }
    }
    return false
}

internal inline fun <E> LongSparseArray<E>.commonClone(): LongSparseArray<E> {
    val new = LongSparseArray<E>(0)
    new._size = _size
    new.keys = keys.copyOf()
    new.values = values.copyOf()
    new.garbage = garbage
    new.gc()
    return new
}

internal inline fun <E> LongSparseArray<E>.commonToString(): String {
    if (size == 0) {
        return "{}"
    }
    return buildString(_size * 20) {
        append('{')
        for (i in 0 until _size) {
            if (i > 0) {
                append(", ")
            }
            val key = keyAt(i)
            append(key)
            append('=')
            val value = valueAt(i)
            if (value !== this) {
                append(value)
            } else {
                append("(this Map)")
            }
        }
        append('}')
    }
}

internal fun <E> LongSparseArray<E>.gc() {
    var newSize = 0
    val keys = keys
    val values = values
    for (i in 0 until _size) {
        val value = values[i]
        if (value !== DELETED) {
            if (i != newSize) {
                keys[newSize] = keys[i]
                values[newSize] = value
                values[i] = null
            }
            newSize++
        }
    }
    garbage = false
    _size = newSize
}
