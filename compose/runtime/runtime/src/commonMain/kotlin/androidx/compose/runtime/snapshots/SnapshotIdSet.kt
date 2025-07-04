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

import androidx.compose.runtime.Immutable

/**
 * An implementation of a bit set that that is optimized around for the top 128 bits and sparse
 * access for bits below that. Is is O(1) to set, clear and get the bit value of the top 128 values
 * of the set. Below lowerBound it is O(log N) to get a bit and O(N) to set or clear a bit where N
 * is the number of bits set below lowerBound. Clearing a cleared bit or setting a set bit is the
 * same complexity of get.
 *
 * The set is immutable and calling the set or clear methods produce the modified set, leaving the
 * previous set unmodified. If the operation does not modify the set, such as setting a set bit or
 * clearing a clear bit, returns the same instance.
 *
 * This class is highly biased to a bits being set at the top 128 values of the range and bits lower
 * than the that range to be mostly or completely clear.
 *
 * This class does not implement equals intentionally. Equals is hard and expensive as a normal form
 * for a particular set is not guaranteed (that is, two sets that compare equal might have different
 * field values). As snapshots does not need this, it is not implemented.
 */
@Immutable
internal class SnapshotIdSet
private constructor(
    // Bit set from (lowerBound + 64)-(lowerBound+127) of the set
    private val upperSet: Long,
    // Bit set from (lowerBound)-(lowerBound+63) of the set
    private val lowerSet: Long,
    // Lower bound of the bit set. All values above lowerBound+127 are clear.
    // Values between lowerBound and lowerBound+127 are recorded in lowerSet and upperSet
    private val lowerBound: SnapshotId,
    // A sorted array of the index of bits set below lowerBound
    private val belowBound: SnapshotIdArray?,
) : Iterable<SnapshotId> {

    /** The value of the bit at index [id] */
    fun get(id: SnapshotId): Boolean {
        val offset = id - lowerBound
        return if (offset >= 0 && offset < Long.SIZE_BITS) {
            (1L shl offset.toInt()) and lowerSet != 0L
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            (1L shl (offset.toInt() - Long.SIZE_BITS)) and upperSet != 0L
        } else if (offset > 0) {
            false
        } else belowBound?.let { it.binarySearch(id) >= 0 } ?: false
    }

    /** Produce a copy of this set with the addition of the bit at index [id] set. */
    fun set(id: SnapshotId): SnapshotIdSet {
        val offset = id - lowerBound
        if (offset >= 0 && offset < Long.SIZE_BITS) {
            val mask = 1L shl offset.toInt()
            if (lowerSet and mask == 0L) {
                return SnapshotIdSet(
                    upperSet = upperSet,
                    lowerSet = lowerSet or mask,
                    lowerBound = lowerBound,
                    belowBound = belowBound,
                )
            }
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            val mask = 1L shl (offset.toInt() - Long.SIZE_BITS)
            if (upperSet and mask == 0L) {
                return SnapshotIdSet(
                    upperSet = upperSet or mask,
                    lowerSet = lowerSet,
                    lowerBound = lowerBound,
                    belowBound = belowBound,
                )
            }
        } else if (offset >= Long.SIZE_BITS * 2) {
            if (!get(id)) {
                // Shift the bit array down
                var newUpperSet = upperSet
                var newLowerSet = lowerSet
                var newLowerBound = lowerBound
                var newBelowBound: SnapshotIdArrayBuilder? = null
                val targetLowerBound =
                    (((id + 1) / SnapshotIdSize) * SnapshotIdSize).let {
                        if (it < 0) SnapshotIdMax - (SnapshotIdSize * 2) + 1 else it
                    }
                while (newLowerBound < targetLowerBound) {
                    // Shift the lower set into the array
                    if (newLowerSet != 0L) {
                        if (newBelowBound == null)
                            newBelowBound = SnapshotIdArrayBuilder(belowBound)
                        repeat(Long.SIZE_BITS) { bitOffset ->
                            if (newLowerSet and (1L shl bitOffset) != 0L) {
                                newBelowBound.add(newLowerBound + bitOffset)
                            }
                        }
                    }
                    if (newUpperSet == 0L) {
                        newLowerBound = targetLowerBound
                        newLowerSet = 0L
                        break
                    }
                    newLowerSet = newUpperSet
                    newUpperSet = 0
                    newLowerBound += Long.SIZE_BITS
                }

                return SnapshotIdSet(
                        newUpperSet,
                        newLowerSet,
                        newLowerBound,
                        newBelowBound?.toArray() ?: belowBound,
                    )
                    .set(id)
            }
        } else {
            val array =
                belowBound
                    ?: return SnapshotIdSet(upperSet, lowerSet, lowerBound, snapshotIdArrayOf(id))

            val location = array.binarySearch(id)
            if (location < 0) {
                val insertLocation = -(location + 1)
                val newBelowBound = array.withIdInsertedAt(insertLocation, id)
                return SnapshotIdSet(upperSet, lowerSet, lowerBound, newBelowBound)
            }
        }

        // No changes
        return this
    }

    /** Produce a copy of this set with the addition of the bit at index [id] cleared. */
    fun clear(id: SnapshotId): SnapshotIdSet {
        val offset = id - lowerBound
        if (offset >= 0 && offset < Long.SIZE_BITS) {
            val mask = 1L shl offset.toInt()
            if (lowerSet and mask != 0L) {
                return SnapshotIdSet(
                    upperSet = upperSet,
                    lowerSet = lowerSet and mask.inv(),
                    lowerBound = lowerBound,
                    belowBound = belowBound,
                )
            }
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            val mask = 1L shl (offset.toInt() - Long.SIZE_BITS)
            if (upperSet and mask != 0L) {
                return SnapshotIdSet(
                    upperSet = upperSet and mask.inv(),
                    lowerSet = lowerSet,
                    lowerBound = lowerBound,
                    belowBound = belowBound,
                )
            }
        } else if (offset < 0) {
            val array = belowBound
            if (array != null) {
                val location = array.binarySearch(id)
                if (location >= 0) {
                    return SnapshotIdSet(
                        upperSet,
                        lowerSet,
                        lowerBound,
                        array.withIdRemovedAt(location),
                    )
                }
            }
        }

        return this
    }

    /** Produce a copy of this with all the values in [ids] cleared (`a & ~b`) */
    fun andNot(ids: SnapshotIdSet): SnapshotIdSet {
        if (ids === EMPTY) return this
        if (this === EMPTY) return EMPTY
        return if (ids.lowerBound == this.lowerBound && ids.belowBound === this.belowBound) {
            SnapshotIdSet(
                this.upperSet and ids.upperSet.inv(),
                this.lowerSet and ids.lowerSet.inv(),
                this.lowerBound,
                this.belowBound,
            )
        } else {
            ids.fastFold(this) { previous, index -> previous.clear(index) }
        }
    }

    fun and(ids: SnapshotIdSet): SnapshotIdSet {
        if (ids == EMPTY) return EMPTY
        if (this == EMPTY) return EMPTY
        return if (ids.lowerBound == this.lowerBound && ids.belowBound === this.belowBound) {
            val newUpper = this.upperSet and ids.upperSet
            val newLower = this.lowerSet and ids.lowerSet
            if (newUpper == 0L && newLower == 0L && this.belowBound == null) EMPTY
            else
                SnapshotIdSet(
                    this.upperSet and ids.upperSet,
                    this.lowerSet and ids.lowerSet,
                    this.lowerBound,
                    this.belowBound,
                )
        } else {
            if (this.belowBound == null)
                this.fastFold(EMPTY) { previous, index ->
                    if (ids.get(index)) previous.set(index) else previous
                }
            else
                ids.fastFold(EMPTY) { previous, index ->
                    if (this.get(index)) previous.set(index) else previous
                }
        }
    }

    /** Produce a set that if the value is set in this set or [bits] (`a | b`) */
    fun or(bits: SnapshotIdSet): SnapshotIdSet {
        if (bits === EMPTY) return this
        if (this === EMPTY) return bits
        return if (bits.lowerBound == this.lowerBound && bits.belowBound === this.belowBound) {
            SnapshotIdSet(
                this.upperSet or bits.upperSet,
                this.lowerSet or bits.lowerSet,
                this.lowerBound,
                this.belowBound,
            )
        } else {
            if (this.belowBound == null) {
                // We are probably smaller than bits, or at least, small enough
                this.fastFold(bits) { previous, index -> previous.set(index) }
            } else {
                // Otherwise assume bits is smaller than this.
                bits.fastFold(this) { previous, index -> previous.set(index) }
            }
        }
    }

    override fun iterator(): Iterator<SnapshotId> =
        sequence {
                this@SnapshotIdSet.belowBound?.forEach { yield(it) }
                if (lowerSet != 0L) {
                    for (index in 0 until Long.SIZE_BITS) {
                        if (lowerSet and (1L shl index) != 0L) {
                            yield(lowerBound + index)
                        }
                    }
                }
                if (upperSet != 0L) {
                    for (index in 0 until Long.SIZE_BITS) {
                        if (upperSet and (1L shl index) != 0L) {
                            yield(lowerBound + index + Long.SIZE_BITS)
                        }
                    }
                }
            }
            .iterator()

    private inline fun fastFold(
        initial: SnapshotIdSet,
        operation: (acc: SnapshotIdSet, SnapshotId) -> SnapshotIdSet,
    ): SnapshotIdSet {
        var accumulator = initial
        fastForEach { element -> accumulator = operation(accumulator, element) }
        return accumulator
    }

    inline fun fastForEach(block: (SnapshotId) -> Unit) {
        this.belowBound?.forEach(block)
        if (lowerSet != 0L) {
            for (index in 0 until Long.SIZE_BITS) {
                if (lowerSet and (1L shl index) != 0L) {
                    block(lowerBound + index)
                }
            }
        }
        if (upperSet != 0L) {
            for (index in 0 until Long.SIZE_BITS) {
                if (upperSet and (1L shl index) != 0L) {
                    block(lowerBound + index + Long.SIZE_BITS)
                }
            }
        }
    }

    fun lowest(default: SnapshotId): SnapshotId {
        val belowBound = belowBound
        if (belowBound != null) return belowBound.first()
        if (lowerSet != 0L) return lowerBound + lowerSet.countTrailingZeroBits()
        if (upperSet != 0L) return lowerBound + Long.SIZE_BITS + upperSet.countTrailingZeroBits()
        return default
    }

    override fun toString(): String =
        "${super.toString()} [${this.map {
        it.toString()
    }.fastJoinToString()}]"

    companion object {
        /** An empty frame it set */
        val EMPTY = SnapshotIdSet(0, 0, SnapshotIdZero, null)
    }
}
