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
        return if (offset >= 0) {
            if (offset < Long.SIZE_BITS) {
                (1L shl offset.toInt()) and lowerSet != 0L
            } else if (offset < Long.SIZE_BITS * 2) {
                (1L shl offset.toInt()) and upperSet != 0L
            } else {
                false
            }
        } else {
            val array = belowBound
            array != null && array.binarySearch(id) >= 0
        }
    }

    /** Produce a copy of this set with the addition of the bit at index [id] set. */
    fun set(id: SnapshotId): SnapshotIdSet {
        val offset = id - lowerBound
        if (offset >= 0) {
            if (offset < Long.SIZE_BITS) {
                val mask = 1L shl offset.toInt()
                if (lowerSet and mask == 0L) {
                    return SnapshotIdSet(
                        upperSet = upperSet,
                        lowerSet = lowerSet or mask,
                        lowerBound = lowerBound,
                        belowBound = belowBound,
                    )
                }
            } else if (offset < Long.SIZE_BITS * 2) {
                val mask = 1L shl offset.toInt()
                if (upperSet and mask == 0L) {
                    return SnapshotIdSet(
                        upperSet = upperSet or mask,
                        lowerSet = lowerSet,
                        lowerBound = lowerBound,
                        belowBound = belowBound,
                    )
                }
            } else {
                if (upperSet == 0L && lowerSet == 0L && belowBound == null) {
                    val newLowerBound = (id / SnapshotIdSize) * SnapshotIdSize
                    return SnapshotIdSet(
                        upperSet = 0L,
                        lowerSet = 1L shl (id - newLowerBound).toInt(),
                        lowerBound = newLowerBound,
                        belowBound = null,
                    )
                }
                // Shift the bit array down. The 128-bit window has only two words (lowerSet and
                // upperSet), so advancing `lowerBound` to accommodate `id` collapses to two cases:
                // 1) `upperSet` is non-empty and shifting by one 64-bit word reaches
                //    `targetLowerBound`: evict only `lowerSet` and promote `upperSet` to
                //    `lowerSet`.
                // 2) Otherwise: evict both `lowerSet` and `upperSet` and jump directly to
                //    `(id / SnapshotIdSize) * SnapshotIdSize`.
                // Checking `targetLowerBound` (rather than assuming integer truncation on `/`)
                // preserves exact behavior on JS where `SnapshotId` is `Double`.
                val targetLowerBound = ((id / SnapshotIdSize) - 1) * SnapshotIdSize
                val singleShiftBound = lowerBound + Long.SIZE_BITS
                val newLowerBound: SnapshotId
                var newLowerSet: Long
                val evictUpperSet: Long
                if (upperSet != 0L && singleShiftBound >= targetLowerBound) {
                    newLowerBound = singleShiftBound
                    newLowerSet = upperSet
                    evictUpperSet = 0L
                } else {
                    newLowerBound = (id / SnapshotIdSize) * SnapshotIdSize
                    newLowerSet = 0L
                    evictUpperSet = upperSet
                }
                val newBelowBound =
                    belowBound.appendEvictedSets(
                        lowerSetBound = lowerBound,
                        lowerSet = lowerSet,
                        upperSetBound = singleShiftBound,
                        upperSet = evictUpperSet,
                    )

                val newOffset = (id - newLowerBound).toInt()
                val newUpperSet: Long
                if (newOffset < Long.SIZE_BITS) {
                    newLowerSet = newLowerSet or (1L shl newOffset)
                    newUpperSet = 0L
                } else {
                    newUpperSet = 1L shl newOffset
                }
                return SnapshotIdSet(
                    newUpperSet,
                    newLowerSet,
                    newLowerBound,
                    newBelowBound,
                )
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

    /**
     * Returns a new sorted [SnapshotIdArray] containing all IDs from `this` (if non-null) followed
     * by all set bits in [lowerSet] (relative to [lowerSetBound]) and [upperSet] (relative to
     * [upperSetBound]), or `this` unchanged if both masks are `0L`.
     *
     * Preserves the strictly ascending invariant of `belowBound` without sorting under the
     * preconditions naturally satisfied by [SnapshotIdSet]:
     * - `this` is sorted strictly ascending, and every element is `< lowerSetBound` (i.e.,
     *   `this[size - 1] < lowerSetBound` when non-empty).
     * - [lowerSet] encodes IDs in `[lowerSetBound, lowerSetBound + Long.SIZE_BITS)`.
     * - `lowerSetBound + Long.SIZE_BITS <= upperSetBound`, and [upperSet] encodes IDs in
     *   `[upperSetBound, upperSetBound + Long.SIZE_BITS)`.
     *
     * Because the three ranges are disjoint and strictly ordered (`this < lowerSet < upperSet`),
     * copying `this` and appending bits extracted via [forEachSetBit] (ascending bit indices
     * `0..63`) produces a strictly ascending array in a single exact-sized allocation.
     */
    private fun SnapshotIdArray?.appendEvictedSets(
        lowerSetBound: SnapshotId,
        lowerSet: Long,
        upperSetBound: SnapshotId,
        upperSet: Long,
    ): SnapshotIdArray? {
        val extra = lowerSet.countOneBits() + upperSet.countOneBits()
        if (extra == 0) return this
        val oldSize = this?.size ?: 0
        val out = snapshotIdArrayWithCapacity(oldSize + extra)
        var pos =
            if (this != null) {
                this.copyInto(out)
                oldSize
            } else {
                0
            }
        forEachSetBit(lowerSetBound, lowerSet) { out[pos++] = it }
        forEachSetBit(upperSetBound, upperSet) { out[pos++] = it }
        return out
    }

    /** Produce a copy of this set with the addition of the bit at index [id] cleared. */
    fun clear(id: SnapshotId): SnapshotIdSet {
        val offset = id - lowerBound
        if (offset >= 0) {
            if (offset < Long.SIZE_BITS) {
                val mask = 1L shl offset.toInt()
                if (lowerSet and mask != 0L) {
                    val newLower = lowerSet and mask.inv()
                    if (upperSet == 0L && newLower == 0L && belowBound == null) {
                        return EMPTY
                    }
                    return SnapshotIdSet(
                        upperSet = upperSet,
                        lowerSet = newLower,
                        lowerBound = lowerBound,
                        belowBound = belowBound,
                    )
                }
            } else if (offset < Long.SIZE_BITS * 2) {
                val mask = 1L shl offset.toInt()
                if (upperSet and mask != 0L) {
                    val newUpper = upperSet and mask.inv()
                    if (newUpper == 0L && lowerSet == 0L && belowBound == null) {
                        return EMPTY
                    }
                    return SnapshotIdSet(
                        upperSet = newUpper,
                        lowerSet = lowerSet,
                        lowerBound = lowerBound,
                        belowBound = belowBound,
                    )
                }
            }
        } else {
            val array = belowBound
            if (array != null) {
                val location = array.binarySearch(id)
                if (location >= 0) {
                    val newBelowBound = array.withIdRemovedAt(location)
                    if (upperSet == 0L && lowerSet == 0L && newBelowBound == null) {
                        return EMPTY
                    }
                    return SnapshotIdSet(
                        upperSet,
                        lowerSet,
                        lowerBound,
                        newBelowBound,
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
            val newUpper = this.upperSet and ids.upperSet.inv()
            val newLower = this.lowerSet and ids.lowerSet.inv()
            if (newUpper == 0L && newLower == 0L) {
                EMPTY
            } else if (
                newUpper == this.upperSet && newLower == this.lowerSet && this.belowBound == null
            ) {
                this
            } else {
                SnapshotIdSet(
                    newUpper,
                    newLower,
                    this.lowerBound,
                    null,
                )
            }
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
            else if (newUpper == this.upperSet && newLower == this.lowerSet) this
            else if (newUpper == ids.upperSet && newLower == ids.lowerSet) ids
            else
                SnapshotIdSet(
                    newUpper,
                    newLower,
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
            val newUpper = this.upperSet or bits.upperSet
            val newLower = this.lowerSet or bits.lowerSet
            if (newUpper == this.upperSet && newLower == this.lowerSet) {
                this
            } else if (newUpper == bits.upperSet && newLower == bits.lowerSet) {
                bits
            } else {
                SnapshotIdSet(
                    newUpper,
                    newLower,
                    this.lowerBound,
                    this.belowBound,
                )
            }
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

    override fun iterator(): Iterator<SnapshotId> = sequence {
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
        val lower = lowerSet
        if (lower != 0L) forEachSetBit(lowerBound, lower, block)
        val upper = upperSet
        if (upper != 0L) forEachSetBit(lowerBound + Long.SIZE_BITS, upper, block)
    }

    /**
     * Visits each set 1-bit in [bits] in ascending bit-index order (`0..63`), invoking [block] with
     * `bound + bitIndex`.
     *
     * Uses Brian Kernighan's bit-clearing loop: `remaining.countTrailingZeroBits()` returns the
     * lowest set bit's index, and `remaining and (remaining - 1L)` clears that lowest set bit while
     * leaving all higher bits intact (subtracting 1 borrows across the trailing zeros, flipping the
     * lowest 1-bit to 0 and all lower 0-bits to 1, so AND-ing back with `remaining` clears exactly
     * that lowest 1-bit).
     *
     * Example: `bits = 0b1010_0100` (set indices 2, 5, 7)
     * 1) `ctz = 2` -> `0b1010_0100 and 0b1010_0011 = 0b1010_0000`
     * 2) `ctz = 5` -> `0b1010_0000 and 0b1001_1111 = 0b1000_0000`
     * 3) `ctz = 7` -> `0b1000_0000 and 0b0111_1111 = 0`
     *
     * Also holds for bit 63 (`Long.MIN_VALUE`): `Long.MIN_VALUE - 1L == Long.MAX_VALUE`, whose
     * bitwise AND with `Long.MIN_VALUE` is `0L`.
     */
    internal inline fun forEachSetBit(bound: SnapshotId, bits: Long, block: (SnapshotId) -> Unit) {
        var remaining = bits
        while (remaining != 0L) {
            block(bound + remaining.countTrailingZeroBits())
            remaining = remaining and (remaining - 1L)
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
