/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.requirePrecondition
import kotlin.collections.ArrayList
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
// Warning: The code of this class is duplicated in SnapshotStateList.android.kt. Any changes
// made here should be considered to be applied there as well.
public actual class SnapshotStateList<T>
internal actual constructor(persistentList: PersistentList<T>) :
    StateObject, MutableList<T>, RandomAccess {
    public actual constructor() : this(persistentListOf())

    actual override var firstStateRecord: StateRecord = stateRecordWith(persistentList)
        private set

    actual override fun prependStateRecord(value: StateRecord) {
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
    public actual fun toList(): List<T> = readable.list

    actual override val size: Int
        get() = readable.list.size

    actual override fun contains(element: T): Boolean = readable.list.contains(element)

    actual override fun containsAll(elements: Collection<T>): Boolean =
        readable.list.containsAll(elements)

    actual override fun get(index: Int): T = readable.list[index]

    actual override fun indexOf(element: T): Int = readable.list.indexOf(element)

    actual override fun isEmpty(): Boolean = readable.list.isEmpty()

    actual override fun iterator(): MutableIterator<T> = listIterator()

    actual override fun lastIndexOf(element: T): Int = readable.list.lastIndexOf(element)

    actual override fun listIterator(): MutableListIterator<T> = StateListIterator(this, 0)

    actual override fun listIterator(index: Int): MutableListIterator<T> =
        StateListIterator(this, index)

    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
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

    actual override fun add(element: T): Boolean = conditionalUpdate { it.add(element) }

    actual override fun add(index: Int, element: T): Unit = update { it.add(index, element) }

    actual override fun addAll(index: Int, elements: Collection<T>): Boolean = mutateBoolean {
        it.addAll(index, elements)
    }

    actual override fun addAll(elements: Collection<T>): Boolean = conditionalUpdate {
        it.addAll(elements)
    }

    actual override fun clear(): Unit = clearImpl()

    actual override fun remove(element: T): Boolean = conditionalUpdate { it.remove(element) }

    actual override fun removeAll(elements: Collection<T>): Boolean = conditionalUpdate {
        it.removeAll(elements)
    }

    actual override fun removeAt(index: Int): T = get(index).also { update { it.removeAt(index) } }

    actual override fun retainAll(elements: Collection<T>): Boolean = mutateBoolean {
        it.retainAll(elements)
    }

    actual override fun set(index: Int, element: T): T =
        get(index).also { update(structural = false) { it.set(index, element) } }

    public actual fun removeRange(fromIndex: Int, toIndex: Int) {
        mutate { it.subList(fromIndex, toIndex).clear() }
    }

    internal actual fun retainAllInRange(elements: Collection<T>, start: Int, end: Int): Int {
        val startSize = size
        mutate<Unit, T> { it.subList(start, end).retainAll(elements) }
        return startSize - size
    }

    /**
     * An internal function used by the debugger to display the value of the current list without
     * triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: List<T>
        @JvmName("getDebuggerDisplayValue") get() = withCurrent { list }
}
