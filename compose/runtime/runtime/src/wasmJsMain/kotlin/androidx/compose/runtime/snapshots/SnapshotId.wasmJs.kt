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

@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package androidx.compose.runtime.snapshots

import androidx.collection.mutableLongListOf

public actual typealias SnapshotId = Long

internal actual const val SnapshotIdZero: SnapshotId = 0L
internal actual const val SnapshotIdMax: SnapshotId = Long.MAX_VALUE
internal actual const val SnapshotIdSize: Int = Long.SIZE_BITS
internal actual const val SnapshotIdInvalidValue: SnapshotId = -1

internal actual inline operator fun SnapshotId.compareTo(other: SnapshotId): Int =
    this.compareTo(other)

internal actual inline operator fun SnapshotId.compareTo(other: Int): Int =
    this.compareTo(other.toLong())

internal actual inline operator fun SnapshotId.plus(other: Int): SnapshotId = this + other.toLong()

internal actual inline operator fun SnapshotId.minus(other: SnapshotId): SnapshotId = this - other

internal actual inline operator fun SnapshotId.minus(other: Int): SnapshotId = this - other.toLong()

internal actual inline operator fun SnapshotId.div(other: Int): SnapshotId = this / other.toLong()

internal actual inline operator fun SnapshotId.times(other: Int): SnapshotId = this * other.toLong()

public actual inline fun SnapshotId.toInt(): Int = this.toInt()

public actual inline fun SnapshotId.toLong(): Long = this

public actual typealias SnapshotIdArray = LongArray

internal actual fun snapshotIdArrayWithCapacity(capacity: Int): SnapshotIdArray =
    LongArray(capacity)

internal actual inline operator fun SnapshotIdArray.get(index: Int): SnapshotId = this[index]

internal actual inline operator fun SnapshotIdArray.set(index: Int, value: SnapshotId) {
    this[index] = value
}

internal actual inline val SnapshotIdArray.size: Int
    get() = this.size

internal actual inline fun SnapshotIdArray.copyInto(other: SnapshotIdArray) {
    this.copyInto(other, 0)
}

internal actual inline fun SnapshotIdArray.first(): SnapshotId = this[0]

internal actual fun SnapshotIdArray.binarySearch(id: SnapshotId): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1)
        val midVal = get(mid)
        if (id > midVal) low = mid + 1 else if (id < midVal) high = mid - 1 else return mid
    }
    return -(low + 1)
}

internal actual inline fun SnapshotIdArray.forEach(block: (SnapshotId) -> Unit) {
    for (value in this) {
        block(value)
    }
}

internal actual fun SnapshotIdArray.withIdInsertedAt(index: Int, id: SnapshotId): SnapshotIdArray {
    val newSize = size + 1
    val newArray = LongArray(newSize)
    this.copyInto(destination = newArray, destinationOffset = 0, startIndex = 0, endIndex = index)
    this.copyInto(
        destination = newArray,
        destinationOffset = index + 1,
        startIndex = index,
        endIndex = newSize - 1,
    )
    newArray[index] = id
    return newArray
}

internal actual fun SnapshotIdArray.withIdRemovedAt(index: Int): SnapshotIdArray? {
    val newSize = this.size - 1
    if (newSize == 0) {
        return null
    }
    val newArray = LongArray(newSize)
    if (index > 0) {
        this.copyInto(
            destination = newArray,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = index,
        )
    }
    if (index < newSize) {
        this.copyInto(
            destination = newArray,
            destinationOffset = index,
            startIndex = index + 1,
            endIndex = newSize + 1,
        )
    }
    return newArray
}

internal actual class SnapshotIdArrayBuilder actual constructor(array: SnapshotIdArray?) {
    private val list = array?.let { mutableLongListOf(*array) } ?: mutableLongListOf()

    actual fun add(id: SnapshotId) {
        list.add(id)
    }

    actual fun toArray(): SnapshotIdArray? {
        val size = list.size
        if (size == 0) return null
        val result = LongArray(size)
        list.forEachIndexed { index, element -> result[index] = element }
        return result
    }
}

internal actual inline fun snapshotIdArrayOf(id: SnapshotId): SnapshotIdArray = longArrayOf(id)

internal actual fun Int.toSnapshotId(): SnapshotId = toLong()

internal actual fun Long.toSnapshotId(): SnapshotId = this
