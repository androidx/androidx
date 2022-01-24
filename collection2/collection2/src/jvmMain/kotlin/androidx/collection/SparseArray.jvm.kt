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

actual open class SparseArray<E>
@JvmOverloads actual constructor(initialCapacity: Int) : Cloneable {
    @JvmField
    @JvmSynthetic // Hide from Java callers.
    internal actual var keys: IntArray
    @JvmField
    @JvmSynthetic // Hide from Java callers.
    internal actual var values: Array<Any?>
    init {
        if (initialCapacity == 0) {
            keys = EMPTY_INTS
            values = EMPTY_OBJECTS
        } else {
            val length = idealIntArraySize(initialCapacity)
            keys = IntArray(length)
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

    actual constructor(array: SparseArray<E>) : this(0) {
        _size = array._size
        keys = array.keys.copyOf()
        values = array.values.copyOf()
        garbage = array.garbage
        gc()
    }

    actual open val size: Int get() = commonSize()

    actual open fun isEmpty(): Boolean = commonIsEmpty()

    actual open operator fun get(key: Int): E? = commonGet(key, null)
    @Suppress("KotlinOperator")
    actual open fun get(key: Int, default: E): E = commonGet(key, default)

    actual open fun put(key: Int, value: E): Unit = commonPut(key, value)
    actual open fun putAll(other: SparseArray<out E>): Unit = commonPutAll(other)
    actual open fun putIfAbsent(key: Int, value: E): E? = commonPutIfAbsent(key, value)
    actual open fun append(key: Int, value: E): Unit = commonAppend(key, value)

    actual open fun keyAt(index: Int): Int = commonKeyAt(index)

    actual open fun valueAt(index: Int): E = commonValueAt(index)
    actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)

    actual open fun indexOfKey(key: Int): Int = commonIndexOfKey(key)
    actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)

    actual open fun containsKey(key: Int): Boolean = commonContainsKey(key)
    actual open fun containsValue(value: E): Boolean = commonContainsValue(value)

    actual open fun clear(): Unit = commonClear()

    actual open fun remove(key: Int): Unit = commonRemove(key)
    actual open fun remove(key: Int, value: Any?): Boolean = commonRemove(key, value)
    actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)

    actual open fun replace(key: Int, value: E): E? = commonReplace(key, value)
    actual open fun replace(key: Int, oldValue: E?, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    @Suppress("NoClone") // To suppress Metalava prohibition on cloning.
    public override fun clone(): SparseArray<E> {
        @Suppress("UNCHECKED_CAST") // Safe according to cloneable contract.
        val clone = super.clone() as SparseArray<E>
        clone.keys = keys.copyOf()
        clone.values = values.copyOf()
        return clone
    }

    actual override fun toString(): String = commonToString()

    @Deprecated("Use remove(key)", level = HIDDEN) // For Java binary compatibility.
    fun delete(key: Int): Unit = remove(key)

    /**
     * Remove a range of mappings as a batch.
     *
     * @param index Index to begin at
     * @param size Number of mappings to remove
     */
    open fun removeAtRange(index: Int, size: Int) {
        val end = minOf(_size, index + size)
        for (i in index until end) {
            removeAt(i)
        }
    }
}

// typealias can't be used for JVM interop, so we have to resort to this.
// TODO(KT-21489): Follow up.
@Deprecated(
    message = "Use androidx.collection.SparseArray",
    replaceWith = ReplaceWith("SparseArray", "androidx.collection.SparseArray"),
)
open class SparseArrayCompat<E> : SparseArray<E> {
    @Deprecated("Use SparseArray", ReplaceWith("SparseArray<E>()"))
    constructor() : super()
    @Deprecated("Use SparseArray", ReplaceWith("SparseArray<E>(initialCapacity)"))
    constructor(initialCapacity: Int) : super(initialCapacity)
    @Deprecated("Use SparseArray", ReplaceWith("SparseArray<E>(array)"))
    constructor(array: SparseArray<E>) : super(array)

    @Suppress("DEPRECATION", "NoClone") // For compatibility.
    override fun clone(): SparseArrayCompat<E> {
        return super.clone() as SparseArrayCompat<E>
    }

    @Suppress("DEPRECATION") // For compatibility.
    open fun putAll(other: SparseArrayCompat<out E>) {
        super.putAll(other)
    }

    /**
     * Returns the number of key-value mappings that this SparseArray
     * currently stores.
     */
    @Deprecated("Replaced with property", ReplaceWith("size"), DeprecationLevel.WARNING)
    open fun size(): Int = commonSize()
}
