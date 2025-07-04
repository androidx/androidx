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

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentSetOf

/**
 * An implementation of [MutableSet] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateSetOf].
 *
 * @see androidx.compose.runtime.mutableStateSetOf
 */
@Stable
@SuppressLint("BanParcelableUsage")
// Warning: The code of this class is duplicated in SnapshotStateSet.nonAndroid.kt. Any changes
// made here should be considered to be applied there as well.
public actual class SnapshotStateSet<T> : Parcelable, StateObject, MutableSet<T>, RandomAccess {
    actual override var firstStateRecord: StateRecord = stateRecordWith(persistentSetOf())
        private set

    actual override fun prependStateRecord(value: StateRecord) {
        value.next = firstStateRecord
        @Suppress("UNCHECKED_CAST")
        firstStateRecord = value as StateSetStateRecord<T>
    }

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
    public actual fun toSet(): Set<T> = readable.set

    actual override val size: Int
        get() = readable.set.size

    actual override fun contains(element: T): Boolean = readable.set.contains(element)

    actual override fun containsAll(elements: Collection<T>): Boolean =
        readable.set.containsAll(elements)

    actual override fun isEmpty(): Boolean = readable.set.isEmpty()

    actual override fun iterator(): MutableIterator<T> =
        StateSetIterator(this, readable.set.iterator())

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String =
        (firstStateRecord as StateSetStateRecord<T>).withCurrent {
            "SnapshotStateSet(value=${it.set})@${hashCode()}"
        }

    actual override fun add(element: T): Boolean = conditionalUpdate { it.add(element) }

    actual override fun addAll(elements: Collection<T>): Boolean = conditionalUpdate {
        it.addAll(elements)
    }

    actual override fun clear(): Unit = clearImpl()

    actual override fun remove(element: T): Boolean = conditionalUpdate { it.remove(element) }

    actual override fun removeAll(elements: Collection<T>): Boolean = conditionalUpdate {
        it.removeAll(elements)
    }

    actual override fun retainAll(elements: Collection<T>): Boolean = mutateBoolean {
        it.retainAll(elements.toSet())
    }

    /**
     * An internal function used by the debugger to display the value of the current set without
     * triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: Set<T>
        @JvmName("getDebuggerDisplayValue") get() = withCurrent { set }

    // android specific additions below

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val set = toSet()
        parcel.writeInt(size)
        val iterator = set.iterator()
        if (iterator.hasNext()) {
            parcel.writeValue(iterator.next())
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    internal companion object {
        @Suppress("unused", "NullableCollectionElement")
        @JvmField
        val CREATOR: Parcelable.Creator<SnapshotStateSet<Any?>> =
            object : Parcelable.ClassLoaderCreator<SnapshotStateSet<Any?>> {
                override fun createFromParcel(
                    parcel: Parcel,
                    loader: ClassLoader?,
                ): SnapshotStateSet<Any?> =
                    SnapshotStateSet<Any?>().apply {
                        val classLoader = loader ?: javaClass.classLoader
                        repeat(times = parcel.readInt()) { add(parcel.readValue(classLoader)) }
                    }

                override fun createFromParcel(parcel: Parcel) = createFromParcel(parcel, null)

                override fun newArray(size: Int) = arrayOfNulls<SnapshotStateSet<Any?>?>(size)
            }
    }
}
