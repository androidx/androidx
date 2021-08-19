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

import kotlin.DeprecationLevel.HIDDEN

actual open class LongSparseArray<E>
@JvmOverloads actual constructor(initialCapacity: Int) : Cloneable {
    @JvmField
    @JvmSynthetic // Hide from Java callers.
    internal actual var keys: LongArray
    @JvmField
    @JvmSynthetic // Hide from Java callers.
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

    @JvmField
    @JvmSynthetic // Hide from Java callers.
    internal actual var garbage: Boolean = false
    @JvmField
    @JvmSynthetic // Hide from Java callers.
    @Suppress("PropertyName") // Normal backing field name but internal for common code.
    internal actual var _size: Int = 0

    actual constructor(array: LongSparseArray<E>) : this(0) {
        _size = array._size
        keys = array.keys.copyOf()
        values = array.values.copyOf()
        garbage = array.garbage
        gc()
    }

    // Suppression necessary, see KT-43542.
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("size")
    actual open val size: Int get() = commonSize()

    actual open fun isEmpty(): Boolean = commonIsEmpty()

    actual open operator fun get(key: Long): E? = commonGet(key, null)
    actual open fun get(key: Long, default: E): E = commonGet(key, default)

    actual open fun put(key: Long, value: E): Unit = commonPut(key, value)
    actual open fun putAll(other: LongSparseArray<out E>): Unit = commonPutAll(other)
    actual open fun putIfAbsent(key: Long, value: E): E? = commonPutIfAbsent(key, value)
    actual open fun append(key: Long, value: E): Unit = commonAppend(key, value)

    actual open fun keyAt(index: Int): Long = commonKeyAt(index)
    actual open fun valueAt(index: Int): E = commonValueAt(index)
    actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)

    actual open fun indexOfKey(key: Long): Int = commonIndexOfKey(key)
    actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)

    actual open fun containsKey(key: Long): Boolean = commonContainsKey(key)
    actual open fun containsValue(value: E): Boolean = commonContainsValue(value)

    actual open fun clear(): Unit = commonClear()

    actual open fun remove(key: Long): Unit = commonRemove(key)

    @Deprecated("", level = HIDDEN) // For binary compatibility.
    open fun delete(key: Long): Unit = remove(key)

    actual open fun remove(key: Long, value: Any?): Boolean = commonRemove(key, value)
    actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)

    actual open fun replace(key: Long, value: E): E? = commonReplace(key, value)
    actual open fun replace(key: Long, oldValue: E?, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    @Suppress("NoClone") // To suppress Metalava prohibition on cloning.
    public override fun clone(): LongSparseArray<E> {
        @Suppress("UNCHECKED_CAST") // Safe according to cloneable contract.
        val clone = super.clone() as LongSparseArray<E>
        clone.keys = keys.copyOf()
        clone.values = values.copyOf()
        return clone
    }

    override fun toString(): String = commonToString()
}
