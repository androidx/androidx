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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.SynchronizedObject
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.requirePrecondition
import androidx.compose.runtime.synchronized
import kotlin.jvm.JvmName

/**
 * An implementation of [MutableList] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateListOf].
 *
 * This class closely implements the same semantics as [ArrayList].
 *
 * @see androidx.compose.runtime.mutableStateListOf
 */
@Stable
class SnapshotStateList<T> internal constructor(persistentList: PersistentList<T>) :
    StateObject, MutableList<T>, RandomAccess {
    constructor() : this(persistentListOf())

    override var firstStateRecord: StateRecord = stateRecordWith(persistentList)
        private set

    override fun prependStateRecord(value: StateRecord) {
        value.next = firstStateRecord
        @Suppress("UNCHECKED_CAST")
        firstStateRecord = value as StateListStateRecord<T>
    }

    /**
     * Return a list containing all the elements of this list.
     *
     * The list returned is immutable and returned will not change even if the content of the list
     * is changed in the same snapshot. It also will be the same instance until the content is
     * changed. It is not, however, guaranteed to be the same instance for the same list as adding
     * and removing the same item from the this list might produce a different instance with the
     * same content.
     *
     * This operation is O(1) and does not involve a physically copying the list. It instead returns
     * the underlying immutable list used internally to store the content of the list.
     *
     * It is recommended to use [toList] when using returning the value of this list from
     * [androidx.compose.runtime.snapshotFlow].
     */
    fun toList(): List<T> = readable.list

    internal val structure: Int
        get() = withCurrent { structuralChange }

    @Suppress("UNCHECKED_CAST")
    internal val readable: StateListStateRecord<T>
        get() = (firstStateRecord as StateListStateRecord<T>).readable(this)

    /** This is an internal implementation class of [SnapshotStateList]. Do not use. */
    internal class StateListStateRecord<T>
    internal constructor(internal var list: PersistentList<T>) : StateRecord() {
        internal var modification = 0
        internal var structuralChange = 0

        override fun assign(value: StateRecord) {
            synchronized(sync) {
                @Suppress("UNCHECKED_CAST")
                list = (value as StateListStateRecord<T>).list
                modification = value.modification
                structuralChange = value.structuralChange
            }
        }

        override fun create(): StateRecord = StateListStateRecord(list)
    }

    override val size: Int
        get() = readable.list.size

    override fun contains(element: T) = readable.list.contains(element)

    override fun containsAll(elements: Collection<T>) = readable.list.containsAll(elements)

    override fun get(index: Int) = readable.list[index]

    override fun indexOf(element: T): Int = readable.list.indexOf(element)

    override fun isEmpty() = readable.list.isEmpty()

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun lastIndexOf(element: T) = readable.list.lastIndexOf(element)

    override fun listIterator(): MutableListIterator<T> = StateListIterator(this, 0)

    override fun listIterator(index: Int): MutableListIterator<T> = StateListIterator(this, index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        requirePrecondition(fromIndex in 0..toIndex && toIndex <= size) {
            "fromIndex or toIndex are out of bounds"
        }
        return SubList(this, fromIndex, toIndex)
    }

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String =
        (firstStateRecord as StateListStateRecord<T>).withCurrent {
            "SnapshotStateList(value=${it.list})@${hashCode()}"
        }

    override fun add(element: T) = conditionalUpdate { it.add(element) }

    override fun add(index: Int, element: T) = update { it.add(index, element) }

    override fun addAll(index: Int, elements: Collection<T>) = mutateBoolean {
        it.addAll(index, elements)
    }

    override fun addAll(elements: Collection<T>) = conditionalUpdate { it.addAll(elements) }

    override fun clear() {
        writable {
            synchronized(sync) {
                list = persistentListOf()
                modification++
                structuralChange++
            }
        }
    }

    override fun remove(element: T) = conditionalUpdate { it.remove(element) }

    override fun removeAll(elements: Collection<T>) = conditionalUpdate { it.removeAll(elements) }

    override fun removeAt(index: Int): T = get(index).also { update { it.removeAt(index) } }

    override fun retainAll(elements: Collection<T>) = mutateBoolean { it.retainAll(elements) }

    override fun set(index: Int, element: T): T =
        get(index).also { update(structural = false) { it.set(index, element) } }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        mutate { it.subList(fromIndex, toIndex).clear() }
    }

    internal fun retainAllInRange(elements: Collection<T>, start: Int, end: Int): Int {
        val startSize = size
        mutate<Unit> { it.subList(start, end).retainAll(elements) }
        return startSize - size
    }

    /**
     * An internal function used by the debugger to display the value of the current list without
     * triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: List<T>
        @JvmName("getDebuggerDisplayValue") get() = withCurrent { list }

    private inline fun <R> writable(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateListStateRecord<T>).writable(this, block)

    private inline fun <R> withCurrent(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST") (firstStateRecord as StateListStateRecord<T>).withCurrent(block)

    private fun mutateBoolean(block: (MutableList<T>) -> Boolean): Boolean = mutate(block)

    private inline fun <R> mutate(block: (MutableList<T>) -> R): R {
        var result: R
        while (true) {
            var oldList: PersistentList<T>? = null
            var currentModification = 0
            synchronized(sync) {
                val current = withCurrent { this }
                currentModification = current.modification
                oldList = current.list
            }
            val builder = oldList!!.builder()
            result = block(builder)
            val newList = builder.build()
            if (
                newList == oldList ||
                    writable { attemptUpdate(currentModification, newList, structural = true) }
            )
                break
        }
        return result
    }

    private inline fun update(
        structural: Boolean = true,
        block: (PersistentList<T>) -> PersistentList<T>
    ) {
        conditionalUpdate(structural, block)
    }

    private inline fun conditionalUpdate(
        structural: Boolean = true,
        block: (PersistentList<T>) -> PersistentList<T>
    ) = run {
        val result: Boolean
        while (true) {
            var oldList: PersistentList<T>? = null
            var currentModification = 0
            synchronized(sync) {
                val current = withCurrent { this }
                currentModification = current.modification
                oldList = current.list
            }
            val newList = block(oldList!!)
            if (newList == oldList) {
                result = false
                break
            }
            if (writable { attemptUpdate(currentModification, newList, structural) }) {
                result = true
                break
            }
        }
        result
    }

    // NOTE: do not inline this method to avoid class verification failures, see b/369909868
    private fun StateListStateRecord<T>.attemptUpdate(
        currentModification: Int,
        newList: PersistentList<T>,
        structural: Boolean
    ): Boolean =
        synchronized(sync) {
            if (modification == currentModification) {
                list = newList
                if (structural) structuralChange++
                modification++
                true
            } else false
        }

    private fun stateRecordWith(list: PersistentList<T>): StateRecord {
        return StateListStateRecord(list).also {
            if (Snapshot.isInSnapshot) {
                it.next =
                    StateListStateRecord(list).also { next ->
                        next.snapshotId = Snapshot.PreexistingSnapshotId
                    }
            }
        }
    }
}

/**
 * Creates a new snapshot state list with the specified [size], where each element is calculated by
 * calling the specified [init] function.
 *
 * The function [init] is called for each list element sequentially starting from the first one. It
 * should return the value for a list element given its index.
 */
fun <T> SnapshotStateList(size: Int, init: (index: Int) -> T): SnapshotStateList<T> {
    if (size == 0) {
        return SnapshotStateList()
    }

    val builder = persistentListOf<T>().builder()
    for (i in 0 until size) {
        builder.add(init(i))
    }
    return SnapshotStateList(builder.build())
}

/**
 * This lock is used to ensure that the value of modification and the list in the state record, when
 * used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the list. This avoid additional allocations but introduces some contention
 * between lists. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired first to avoid deadlocks.
 */
private val sync = SynchronizedObject()

private fun modificationError(): Nothing = error("Cannot modify a state list through an iterator")

private fun validateRange(index: Int, size: Int) {
    if (index !in 0 until size) {
        throw IndexOutOfBoundsException("index ($index) is out of bound of [0, $size)")
    }
}

private fun invalidIteratorSet(): Nothing =
    error(
        "Cannot call set before the first call to next() or previous() " +
            "or immediately after a call to add() or remove()"
    )

private class StateListIterator<T>(val list: SnapshotStateList<T>, offset: Int) :
    MutableListIterator<T> {
    private var index = offset - 1
    private var lastRequested = -1
    private var structure = list.structure

    override fun hasPrevious() = index >= 0

    override fun nextIndex() = index + 1

    override fun previous(): T {
        validateModification()
        validateRange(index, list.size)
        lastRequested = index
        return list[index].also { index-- }
    }

    override fun previousIndex(): Int = index

    override fun add(element: T) {
        validateModification()
        list.add(index + 1, element)
        lastRequested = -1
        index++
        structure = list.structure
    }

    override fun hasNext() = index < list.size - 1

    override fun next(): T {
        validateModification()
        val newIndex = index + 1
        lastRequested = newIndex
        validateRange(newIndex, list.size)
        return list[newIndex].also { index = newIndex }
    }

    override fun remove() {
        validateModification()
        list.removeAt(index)
        index--
        lastRequested = -1
        structure = list.structure
    }

    override fun set(element: T) {
        validateModification()
        if (lastRequested < 0) invalidIteratorSet()
        list.set(lastRequested, element)
        structure = list.structure
    }

    private fun validateModification() {
        if (list.structure != structure) {
            throw ConcurrentModificationException()
        }
    }
}

private class SubList<T>(val parentList: SnapshotStateList<T>, fromIndex: Int, toIndex: Int) :
    MutableList<T> {
    private val offset = fromIndex
    private var structure = parentList.structure
    override var size = toIndex - fromIndex
        private set

    override fun contains(element: T): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): T {
        validateModification()
        validateRange(index, size)
        return parentList[offset + index]
    }

    override fun indexOf(element: T): Int {
        validateModification()
        (offset until offset + size).forEach { if (element == parentList[it]) return it - offset }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun lastIndexOf(element: T): Int {
        validateModification()
        var index = offset + size - 1
        while (index >= offset) {
            if (element == parentList[index]) return index - offset
            index--
        }
        return -1
    }

    override fun add(element: T): Boolean {
        validateModification()
        parentList.add(offset + size, element)
        size++
        structure = parentList.structure
        return true
    }

    override fun add(index: Int, element: T) {
        validateModification()
        parentList.add(offset + index, element)
        size++
        structure = parentList.structure
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateModification()
        val result = parentList.addAll(index + offset, elements)
        if (result) {
            size += elements.size
            structure = parentList.structure
        }
        return result
    }

    override fun addAll(elements: Collection<T>): Boolean = addAll(size, elements)

    override fun clear() {
        if (size > 0) {
            validateModification()
            parentList.removeRange(offset, offset + size)
            size = 0
            structure = parentList.structure
        }
    }

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        validateModification()
        var current = index - 1
        return object : MutableListIterator<T> {
            override fun hasPrevious() = current >= 0

            override fun nextIndex(): Int = current + 1

            override fun previous(): T {
                val oldCurrent = current
                validateRange(oldCurrent, size)
                current = oldCurrent - 1
                return this@SubList[oldCurrent]
            }

            override fun previousIndex(): Int = current

            override fun add(element: T) = modificationError()

            override fun hasNext(): Boolean = current < size - 1

            override fun next(): T {
                val newCurrent = current + 1
                validateRange(newCurrent, size)
                current = newCurrent
                return this@SubList[newCurrent]
            }

            override fun remove() = modificationError()

            override fun set(element: T) = modificationError()
        }
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        return if (index >= 0) {
            removeAt(index)
            true
        } else false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        for (element in elements) {
            removed = remove(element) || removed
        }
        return removed
    }

    override fun removeAt(index: Int): T {
        validateModification()
        return parentList.removeAt(offset + index).also {
            size--
            structure = parentList.structure
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        validateModification()
        val removed = parentList.retainAllInRange(elements, offset, offset + size)
        if (removed > 0) {
            structure = parentList.structure
            size -= removed
        }
        return removed > 0
    }

    override fun set(index: Int, element: T): T {
        validateRange(index, size)
        validateModification()
        val result = parentList.set(index + offset, element)
        structure = parentList.structure
        return result
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        requirePrecondition(fromIndex in 0..toIndex && toIndex <= size) {
            "fromIndex or toIndex are out of bounds"
        }
        validateModification()
        return SubList(parentList, fromIndex + offset, toIndex + offset)
    }

    private fun validateModification() {
        if (parentList.structure != structure) {
            throw ConcurrentModificationException()
        }
    }
}
