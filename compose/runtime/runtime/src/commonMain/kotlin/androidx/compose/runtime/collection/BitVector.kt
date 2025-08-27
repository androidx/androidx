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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.runtime.collection

import kotlin.jvm.JvmField

private inline fun Boolean.toInt() = if (this) 1 else 0

private inline fun Long.setBit(index: Int, value: Boolean): Long {
    val mask = 1L shl index
    return (this and mask.inv()) or (-value.toInt().toLong() and mask)
}

private inline fun Long.getBit(index: Int) = (this and (1L shl index)) != 0L

private fun reallocate(values: LongArray, address: Int): LongArray {
    val v = LongArray(address + 1)
    values.copyInto(v)
    return v
}

internal class BitVector {
    @JvmField internal var data = LongArray(2)

    val size
        get() = data.size shl BITS_PER_CHUNK_SHIFT

    operator fun get(index: Int): Boolean {
        val address = index ushr 6
        val bit = index and 0x3f
        return data[address].getBit(bit)
    }

    operator fun set(index: Int, value: Boolean) {
        val address = index ushr 6
        var values = data
        if (address >= values.size) {
            values = reallocate(values, address)
            data = values
        }
        val bits = values[address]
        values[address] = bits.setBit(index and 0x3f, value)
    }

    fun nextSet(index: Int): Int {
        forEachChunkInRange(index, size) { chunk, startInChunk, endInChunk ->
            val maskedBitsToCheck =
                data[chunk] shr startInChunk and (-1L shl (BITS_PER_CHUNK - endInChunk))

            val chunkClearIndex = maskedBitsToCheck.countTrailingZeroBits()
            if (chunkClearIndex < BITS_PER_CHUNK) {
                return chunkClearIndex + (chunk shl BITS_PER_CHUNK_SHIFT) + startInChunk
            }
        }
        return size
    }

    fun nextClear(index: Int): Int {
        forEachChunkInRange(index, size) { chunk, startInChunk, endInChunk ->
            val maskedBitsToCheck =
                data[chunk] shr startInChunk and (-1L shl (BITS_PER_CHUNK - endInChunk))

            val chunkClearIndex = maskedBitsToCheck.inv().countTrailingZeroBits()
            if (chunkClearIndex < BITS_PER_CHUNK) {
                return chunkClearIndex + (chunk shl BITS_PER_CHUNK_SHIFT) + startInChunk
            }
        }
        return size
    }

    fun setRange(start: Int, end: Int) {
        val endAddress = end ushr 6
        var values = data
        if (endAddress >= values.size) {
            values = reallocate(values, endAddress)
            data = values
        }
        forEachChunkInRange(start, end) { chunk, startInChunk, endInChunk ->
            val bitsToSet = (-1L ushr (BITS_PER_CHUNK - endInChunk)) and (-1L shl (startInChunk))
            data[chunk] = data[chunk] or bitsToSet
        }
    }

    private inline fun forEachChunkInRange(
        startIndex: Int,
        endIndex: Int,
        action: (chunkNumber: Int, startInChunk: Int, endInChunk: Int) -> Unit,
    ) {
        val startChunk = startIndex shr BITS_PER_CHUNK_SHIFT
        val endChunk = (endIndex - 1) shr BITS_PER_CHUNK_SHIFT
        for (chunk in startChunk..endChunk) {
            val chunkStartIndex = chunk shl BITS_PER_CHUNK_SHIFT
            val chunkEndIndex = chunkStartIndex + BITS_PER_CHUNK
            action(
                chunk,
                maxOf(startIndex, chunkStartIndex) - chunkStartIndex,
                minOf(endIndex, chunkEndIndex) - chunkStartIndex,
            )
        }
    }

    override fun toString(): String = buildString {
        var first = true
        append("BitVector [")
        for (i in 0 until size) {
            if (this@BitVector[i]) {
                if (!first) append(", ")
                first = false
                append(i)
            }
        }
        append(']')
    }

    companion object {
        private const val BITS_PER_CHUNK = Long.SIZE_BITS
        private const val BITS_PER_CHUNK_SHIFT = 6
    }
}
