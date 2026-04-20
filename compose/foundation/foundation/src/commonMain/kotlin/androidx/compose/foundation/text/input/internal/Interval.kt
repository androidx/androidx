/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import kotlin.jvm.JvmInline

private const val FLAG1_MASK = 1L.shl(63)
private const val FLAG2_MASK = 1L.shl(31)
private const val INT1_MASK = 0x7FFF_FFFF_0000_0000L
private const val INT2_MASK = 0x7FFF_FFFFL

/**
 * Represents an interval in the tree. It stores the [start] (inclusive) and [end] (exclusive) of
 * the interval, along with two additional flags, [flag1] and [flag2]. The start and end must be
 * positive 31-bit integers (not signed). Although this data structure doesn't enforce it,
 * [IntIntervalTree] won't add reversed intervals (where start > end) into the tree.
 *
 * The additional flag information is general-purpose, and the user of this class can interpret it
 * in different ways. For example, [flag1] and [flag2] could be used to indicate whether the start
 * and end bounds are inclusive or exclusive. [IntIntervalTree] only stores this information; its
 * value does not have any impact on the tree's behavior.
 *
 * Bit Layout (64 bits total):
 * ```
 * +--------+----------------------+--------+----------------------+
 * | Bit 63 | Bits 32-62 (31 bits) | Bit 31 | Bits 0-30 (31 bits)  |
 * +--------+----------------------+--------+----------------------+
 * | flag1  | start                | flag2  | end                  |
 * +--------+----------------------+--------+----------------------+
 * ```
 */
@JvmInline
internal value class Interval(val packed: Long) {

    /**
     * Represents an interval stored in the tree. It stores the start and end of the interval
     * together with 2 general purpose flags.
     *
     * @param start The start of the interval, inclusive. Must be a positive 31-bit integer (not
     *   signed).
     * @param end The end of the interval, exclusive. Must be a positive 31-bit integer (not
     *   signed).
     * @param flag1 A general purposed flag.
     * @param flag2 A general purposed flag.
     */
    constructor(
        start: Int,
        end: Int,
        flag1: Boolean = false,
        flag2: Boolean = false,
    ) : this(packValuesAndFlags(flag1, start, flag2, end))

    val start: Int
        get() = unpackValue1(packed)

    val end: Int
        get() = unpackValue2(packed)

    val flag1: Boolean
        get() = unpackFlag1(packed)

    val flag2: Boolean
        get() = unpackFlag2(packed)

    companion object {
        /** A special [Interval] representing an absent or "null" value. */
        val Invalid: Interval = Interval(Int.MAX_VALUE, 0)
    }
}

internal fun Interval.overlaps(start: Int, end: Int): Boolean {
    return intersect(start, end, this.start, this.end)
}

/**
 * Helper methods to pack and unpack the flags and integers in [Node] info. To distinguish from the
 * [androidx.compose.ui.util.packInts] methods this method and other helpers are named using the
 * term `Value` instead.
 */
internal fun packValuesAndFlags(flag1: Boolean, val1: Int, flag2: Boolean, val2: Int): Long {
    val val1Long = val1.toLong()
    // If the given val2 is negative, val2.toLong() will be filled with ones on the high 32 bits.
    // Here we make sure the high 32 bits are cleaned.
    val val2Long = val2.toLong() and INT2_MASK
    val highBits =
        if (flag1) {
            val1Long.shl(32) or FLAG1_MASK
        } else {
            // In case val1 is negative, it'll set the 63-th bit to 1.
            val1Long.shl(32) and FLAG1_MASK.inv()
        }

    val lowBits =
        if (flag2) {
            val2Long or FLAG2_MASK
        } else {
            val2Long
        }
    return highBits or lowBits
}

/** Unpacks the flag1 from the [packed] value. */
internal fun unpackFlag1(packed: Long): Boolean = (packed and FLAG1_MASK) != 0L

/** Unpacks the flag2 from the [packed] value. */
internal fun unpackFlag2(packed: Long): Boolean = (packed and FLAG2_MASK) != 0L

/** Unpacks the value1 from the [packed] value. */
internal fun unpackValue1(packed: Long): Int = (packed and INT1_MASK).ushr(32).toInt()

/** Unpacks the value2 from the [packed] value. */
internal fun unpackValue2(packed: Long): Int = (packed and INT2_MASK).toInt()

/** Update the value1 in the [packed] with the given [val1] and return the new packed value. */
internal fun packValue1(packed: Long, val1: Int): Long =
    (packed and INT1_MASK.inv()) or (val1.toLong().shl(32) and INT1_MASK)

/** Update the value2 in the [packed] with the given [val2] and return the new packed value. */
internal fun packValue2(packed: Long, val2: Int): Long =
    (packed and INT2_MASK.inv()) or val2.toLong()

/** Update the flag1 in the [packed] with the given [flag1] and return the new packed value. */
internal fun packFlag1(packed: Long, flag1: Boolean): Long =
    if (flag1) {
        packed or FLAG1_MASK
    } else {
        packed and FLAG1_MASK.inv()
    }

/** Update the flag2 in the [packed] with the given [flag2] and return the new packed value. */
internal fun packFlag2(packed: Long, flag2: Boolean): Long =
    if (flag2) {
        packed or FLAG2_MASK
    } else {
        packed and FLAG2_MASK.inv()
    }
