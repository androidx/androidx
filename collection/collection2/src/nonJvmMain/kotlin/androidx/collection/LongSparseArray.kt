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

package androidx.collection

actual class LongSparseArray<E> actual constructor(initialCapacity: Int) {
    internal actual var keys: LongArray
    internal actual var values: Array<Any?>
    init {
        if (initialCapacity == 0) {
            keys = EMPTY_LONGS
            values = EMPTY_OBJECTS
        } else {
            val length = idealLongArraySize(initialCapacity)
            keys = LongArray(length)
            values = arrayOfNulls(length)
        }
    }

    internal actual var garbage: Boolean = false
    @Suppress("PropertyName") // Normal backing field name but internal for common code.
    internal actual var _size: Int = 0

    actual constructor(array: LongSparseArray<E>) : this(0) {
        _size = array._size
        keys = array.keys.copyOf()
        values = array.values.copyOf()
        garbage = array.garbage
        gc()
    }

    actual val size: Int get() = commonSize()

    actual fun isEmpty(): Boolean = commonIsEmpty()

    actual operator fun get(key: Long): E? = commonGet(key, null)
    actual fun get(key: Long, default: E): E = commonGet(key, default)

    actual fun put(key: Long, value: E): Unit = commonPut(key, value)
    actual fun putAll(other: LongSparseArray<out E>): Unit = commonPutAll(other)
    actual fun putIfAbsent(key: Long, value: E): E? = commonPutIfAbsent(key, value)
    actual fun append(key: Long, value: E): Unit = commonAppend(key, value)

    actual fun keyAt(index: Int): Long = commonKeyAt(index)

    actual fun valueAt(index: Int): E = commonValueAt(index)
    actual fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)

    actual fun indexOfKey(key: Long): Int = commonIndexOfKey(key)
    actual fun indexOfValue(value: E): Int = commonIndexOfValue(value)

    actual fun containsKey(key: Long): Boolean = commonContainsKey(key)
    actual fun containsValue(value: E): Boolean = commonContainsValue(value)

    actual fun clear(): Unit = commonClear()

    actual fun remove(key: Long): Unit = commonRemove(key)
    actual fun remove(key: Long, value: Any?): Boolean = commonRemove(key, value)
    actual fun removeAt(index: Int): Unit = commonRemoveAt(index)

    actual fun replace(key: Long, value: E): E? = commonReplace(key, value)
    actual fun replace(key: Long, oldValue: E?, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    override fun toString(): String = commonToString()
}
