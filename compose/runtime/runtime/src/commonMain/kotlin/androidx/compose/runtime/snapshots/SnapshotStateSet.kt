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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentSetOf
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import kotlin.js.JsName

/**
 * An implementation of [MutableSet] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateSetOf].
 *
 * The returned set iteration order is in the order the items were inserted into the set.
 *
 * @see androidx.compose.runtime.mutableStateSetOf
 */
@Stable
public expect class SnapshotStateSet<T> : StateObject, MutableSet<T>, RandomAccess {
    public constructor()

    override var firstStateRecord: StateRecord
        private set

    override fun prependStateRecord(value: StateRecord)

    /**
     * Return a set containing all the elements of this set.
     *
     * The set returned is immutable and returned will not change even if the content of the set is
     * changed in the same snapshot. It also will be the same instance until the content is changed.
     * It is not, however, guaranteed to be the same instance for the same set as adding and
     * removing the same item from the this set might produce a different instance with the same
     * content.
     *
     * This operation is O(1) and does not involve a physically copying the set. It instead returns
     * the underlying immutable set used internally to store the content of the set.
     *
     * It is recommended to use [toSet] when returning the value of this set from
     * [androidx.compose.runtime.snapshotFlow].
     */
    public fun toSet(): Set<T>

    override val size: Int

    override fun contains(element: T): Boolean

    override fun containsAll(elements: Collection<T>): Boolean

    override fun isEmpty(): Boolean

    override fun iterator(): MutableIterator<T>

    override fun add(element: T): Boolean

    override fun addAll(elements: Collection<T>): Boolean

    override fun clear()

    override fun remove(element: T): Boolean

    override fun removeAll(elements: Collection<T>): Boolean

    override fun retainAll(elements: Collection<T>): Boolean
}

/** This is an internal implementation class of [SnapshotStateSet]. Do not use. */
internal class StateSetStateRecord<T>
internal constructor(snapshotId: SnapshotId, internal var set: PersistentSet<T>) :
    StateRecord(snapshotId) {
    internal var modification = 0

    override fun assign(value: StateRecord) {
        synchronized(sync) {
            @Suppress("UNCHECKED_CAST")
            set = (value as StateSetStateRecord<T>).set
            modification = value.modification
        }
    }

    override fun create(): StateRecord = StateSetStateRecord(currentSnapshot().snapshotId, set)

    override fun create(snapshotId: SnapshotId): StateRecord = StateSetStateRecord(snapshotId, set)
}

internal val <T> SnapshotStateSet<T>.modification: Int
    get() = withCurrent { modification }

@Suppress("UNCHECKED_CAST")
internal val <T> SnapshotStateSet<T>.readable: StateSetStateRecord<T>
    get() = (firstStateRecord as StateSetStateRecord<T>).readable(this)

internal inline fun <R, T> SnapshotStateSet<T>.writable(block: StateSetStateRecord<T>.() -> R): R =
    @Suppress("UNCHECKED_CAST") (firstStateRecord as StateSetStateRecord<T>).writable(this, block)

internal inline fun <R, T> SnapshotStateSet<T>.withCurrent(
    block: StateSetStateRecord<T>.() -> R
): R = @Suppress("UNCHECKED_CAST") (firstStateRecord as StateSetStateRecord<T>).withCurrent(block)

internal fun <T> SnapshotStateSet<T>.mutateBoolean(block: (MutableSet<T>) -> Boolean): Boolean =
    mutate(block)

internal inline fun <R, T> SnapshotStateSet<T>.mutate(block: (MutableSet<T>) -> R): R {
    var result: R
    while (true) {
        var oldSet: PersistentSet<T>? = null
        var currentModification = 0
        synchronized(sync) {
            val current = withCurrent { this }
            currentModification = current.modification
            oldSet = current.set
        }
        val builder = oldSet?.builder() ?: error("No set to mutate")
        result = block(builder)
        val newSet = builder.build()
        if (newSet == oldSet || writable { attemptUpdate(currentModification, newSet) }) break
    }
    return result
}

internal inline fun <T> SnapshotStateSet<T>.conditionalUpdate(
    block: (PersistentSet<T>) -> PersistentSet<T>
) = run {
    val result: Boolean
    while (true) {
        var oldSet: PersistentSet<T>? = null
        var currentModification = 0
        synchronized(sync) {
            val current = withCurrent { this }
            currentModification = current.modification
            oldSet = current.set
        }
        val newSet = block(oldSet!!)
        if (newSet == oldSet) {
            result = false
            break
        }
        if (writable { attemptUpdate(currentModification, newSet) }) {
            result = true
            break
        }
    }
    result
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> SnapshotStateSet<T>.clearImpl() {
    writable {
        synchronized(sync) {
            set = persistentSetOf()
            modification++
        }
    }
}

// NOTE: do not inline this method to avoid class verification failures, see b/369909868
internal fun <T> StateSetStateRecord<T>.attemptUpdate(
    currentModification: Int,
    newSet: PersistentSet<T>,
): Boolean =
    synchronized(sync) {
        if (modification == currentModification) {
            set = newSet
            modification++
            true
        } else false
    }

internal fun <T> SnapshotStateSet<T>.stateRecordWith(set: PersistentSet<T>): StateRecord {
    return StateSetStateRecord(currentSnapshot().snapshotId, set).also {
        if (Snapshot.isInSnapshot) {
            it.next = StateSetStateRecord(Snapshot.PreexistingSnapshotId.toSnapshotId(), set)
        }
    }
}

/**
 * This lock is used to ensure that the value of modification and the set in the state record, when
 * used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the set. This avoids additional allocations but introduces some contention
 * between sets. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired last to avoid deadlocks. In other words, the
 * lock must be taken in the `writable` lambda, if `writable` is used.
 */
private val sync = makeSynchronizedObject()

internal class StateSetIterator<T>(val set: SnapshotStateSet<T>, val iterator: Iterator<T>) :
    MutableIterator<T> {
    var current: T? = null
    @JsName("var_next") var next: T? = null
    var modification = set.modification

    init {
        advance()
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): T {
        validateModification()
        advance()
        return current ?: throw IllegalStateException()
    }

    override fun remove() = modify {
        val value = current

        if (value != null) {
            set.remove(value)
            current = null
        } else {
            throw IllegalStateException()
        }
    }

    private fun advance() {
        current = next
        next = if (iterator.hasNext()) iterator.next() else null
    }

    private inline fun <T> modify(block: () -> T): T {
        validateModification()
        return block().also { modification = set.modification }
    }

    private fun validateModification() {
        if (set.modification != modification) {
            throw ConcurrentModificationException()
        }
    }
}
