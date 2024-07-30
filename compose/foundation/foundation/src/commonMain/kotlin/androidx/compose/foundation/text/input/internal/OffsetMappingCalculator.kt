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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.ui.text.TextRange

/**
 * Builds up bidirectional mapping functions to map offsets from an original string to corresponding
 * in a string that has some edit operations applied.
 *
 * Edit operations must be reported via [recordEditOperation]. Offsets can be mapped
 * [from the source string][mapFromSource] and [back to the source][mapFromDest]. Mapping between
 * source and transformed offsets is a symmetric operation – given a sequence of edit operations,
 * the result of mapping each offset from the source to transformed strings will be the same as
 * mapping backwards on the inverse sequence of edit operations.
 *
 * By default, offsets map one-to-one. However, around an edit operation, some alternative rules
 * apply. In general, offsets are mapped one-to-one where they can be unambiguously. When there
 * isn't enough information, the mapping is ambiguous, and the mapping result will be a [TextRange]
 * instead of a single value, where the range represents all the possible offsets that it could map
 * to.
 *
 * _Note: In the following discussion, `I` refers to the start offset in the source text, `N` refers
 * to the length of the range in the source text, and `N'` refers to the length of the replacement
 * text. So given `"abc"` and replacing `"bc"` with `"xyz"`, `I=1`, `N=2`, and `N'=3`._
 *
 * ### Insertions
 * When text is inserted (i.e. zero-length range is replaced with non-zero-length range), the
 * mapping is ambiguous because all of the offsets in the inserted text map back to the same offset
 * in the source text - the offset where the text was inserted. That means the insertion point can
 * map to any of the offsets in the inserted text. I.e. I -> I..N'
 * - This is slightly different than the replacement case, because the offset of the start of the
 *   operation and the offset of the end of the operation (which are the same) map to a range
 *   instead of a scalar. This is because there is not enough information to map the start and end
 *   offsets 1-to-1 to offsets in the transformed text.
 * - This is symmetric with deletion: Mapping backward from an insertion is the same as mapping
 *   forward from a deletion.
 *
 * ### Deletions
 * In the inverse case, when text is deleted, mapping is unambiguous. All the offsets in the deleted
 * range map to the start of the deleted range. I.e. I..N -> I
 * - This is symmetric with insertion: Mapping backward from a deletion is the same as mapping
 *   forward from an insertion.
 *
 * ### Replacements
 * When text is replaced, there are both ambiguous and unambiguous mappings:
 * - The offsets at the _ends_ of the replaced range map unambiguously to the offsets at the
 *   corresponding edges of the replaced text. I -> I and I+1 -> I+N
 * - The offsets _inside_ the replaced range (exclusive) map ambiguously to the entire replaced
 *   range. I+1..I+N-1 -> I+1..I+N'-1
 * - Note that this means that when a string with length >1 is replaced by a single character, all
 *   the offsets inside that string will map not to the index of the replacement character but to a
 *   single-char _range_ containing that character.
 *
 * ### Examples
 *
 * #### Inserting text
 *
 * ```
 *     012
 * A: "ab"
 *     | \
 *     |  \
 * B: "azzzb"
 *     012345
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   |
 * |--------:|:---:|:---:|:---:|
 * |   to B: |  0  | 1-4 |  5  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   | 3   | 4   | 5   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|:---:|
 * |   to A: |  0  |  1  |  1  |  1  |  1  |  2  |
 *
 * #### Deleting text
 *
 * ```
 *     012345
 * A: "azzzb"
 *     |  /
 *     | /
 * B: "ab"
 *     012
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   | 3   | 4   | 5   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|:---:|
 * |   to B: |  0  |  1  |  1  |  1  |  1  |  2  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   |
 * |--------:|:---:|:---:|:---:|
 * |   to A: |  0  | 1-4 |  5  |
 *
 * #### Replacing text: single char with char
 *
 * ```
 *     0123
 * A: "abc"
 *      |
 *      |
 * B: "azc"
 *     0123
 * ```
 *
 * Forward/reverse mapping: identity
 *
 * | from: | 0   | 1   | 2   | 3   |
 * |------:|:---:|:---:|:---:|:---:|
 * |   to: |  0  |  1  |  2  |  3  |
 *
 * #### Replacing text: char with chars
 *
 * ```
 *     0123
 * A: "abc"
 *      |
 *      |\
 * B: "azzc"
 *     01234
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   | 3   |
 * |--------:|:---:|:---:|:---:|:---:|
 * |   to B: |  0  |  1  |  3  |  4  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   | 3   | 4   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|
 * |   to A: |  0  |  1  |  1  |  2  |  3  |
 *
 * #### Replacing text: chars with chars
 *
 * ```
 *     012345
 * A: "abcde"
 *      | |
 *      | /
 * B: "azze"
 *     01234
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   | 3   | 4   | 5   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|:---:|
 * |   to B: |  0  |  1  | 1-3 | 1-3 |  3  |  4  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   | 3   | 4   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|
 * |   to A: |  0  |  1  | 1-4 |  4  |  5  |
 *
 * ### Multiple operations
 *
 * While the above apply to single edit operations, when multiple edit operations are recorded the
 * same rules apply. The rules are applied to the first operation, then the result of that is
 * effectively used as the input text for the next operation, etc. Because an offset can map to a
 * range at each step, we track both a start and end offset (which start as the same value), and at
 * each step combine the start and end _ranges_ by taking their union.
 *
 * #### Multiple char-to-char replacements (codepoint transformation):
 * ```
 *     0123
 * A: "abc"
 *     |
 *    "•bc"
 *      |
 *    "••c"
 *       |
 * B: "•••"
 *     0123
 * ```
 *
 * Forward/reverse mapping: identity
 *
 * | from: | 0   | 1   | 2   | 3   |
 * |------:|:---:|:---:|:---:|:---:|
 * |   to: |  0  |  1  |  2  |  3  |
 *
 * #### Multiple inserts:
 * ```
 *     01234
 * A: "abcd"
 *      |
 *    "a(bcd"
 *         |
 * B: "a(bc)d"
 *     0123456
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   | 3   | 4   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|
 * |   to B: |  0  | 1-2 |  3  | 4-5 |  6  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   | 3   | 4   | 5   | 6   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
 * |   to A: |  0  |  1  |  1  |  2  |  3  |  3  |  4  |
 *
 * #### Multiple replacements of the same range:
 * ```
 *     01234
 * A: "abcd"
 *      ||
 *    "awxd"
 *      ||
 * B: "ayzd"
 *     01234
 * ```
 *
 * Forward mapping:
 *
 * | from A: | 0   | 1   | 2   | 3   | 4   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|
 * |   to B: |  0  |  1  | 1-3 |  3  |  4  |
 *
 * Reverse mapping:
 *
 * | from B: | 0   | 1   | 2   | 3   | 4   |
 * |--------:|:---:|:---:|:---:|:---:|:---:|
 * |   to A: |  0  |  1  | 1-3 |  3  |  4  |
 *
 * For other edge cases, including overlapping replacements, see `OffsetMappingCalculatorTest`.
 */
internal class OffsetMappingCalculator {
    /** Resizable array of edit operations, size is defined by [opsSize]. */
    private var ops = OpArray(size = 10)
    private var opsSize = 0

    /**
     * Records an edit operation that replaces the range from [sourceStart] (inclusive) to
     * [sourceEnd] (exclusive) in the original text with some text with length [newLength].
     */
    fun recordEditOperation(sourceStart: Int, sourceEnd: Int, newLength: Int) {
        requirePrecondition(newLength >= 0) { "Expected newLen to be ≥ 0, was $newLength" }
        val sourceMin = minOf(sourceStart, sourceEnd)
        val sourceMax = maxOf(sourceMin, sourceEnd)
        val sourceLength = sourceMax - sourceMin

        // 0,0 is a noop, and 1,1 is always a 1-to-1 mapping so we don't need to record it.
        if (sourceLength < 2 && sourceLength == newLength) return

        // Append the operation to the array, growing it if necessary.
        val newSize = opsSize + 1
        if (newSize > ops.size) {
            val newCapacity = maxOf(newSize * 2, ops.size * 2)
            ops = ops.copyOf(newCapacity)
        }
        ops.set(opsSize, sourceMin, sourceLength, newLength)
        opsSize = newSize
    }

    /**
     * Maps an [offset] in the original string to the corresponding offset, or range of offsets, in
     * the transformed string.
     */
    fun mapFromSource(offset: Int): TextRange = map(offset, fromSource = true)

    /**
     * Maps an [offset] in the original string to the corresponding offset, or range of offsets, in
     * the transformed string.
     */
    fun mapFromDest(offset: Int): TextRange = map(offset, fromSource = false)

    private fun map(offset: Int, fromSource: Boolean): TextRange {
        var start = offset
        var end = offset

        // This algorithm works for both forward and reverse mapping, we just need to iterate
        // backwards to do reverse mapping.
        ops.forEach(max = opsSize, reversed = !fromSource) { opOffset, opSrcLen, opDestLen ->
            val newStart =
                mapStep(
                    offset = start,
                    opOffset = opOffset,
                    untransformedLen = opSrcLen,
                    transformedLen = opDestLen,
                    fromSource = fromSource
                )
            val newEnd =
                mapStep(
                    offset = end,
                    opOffset = opOffset,
                    untransformedLen = opSrcLen,
                    transformedLen = opDestLen,
                    fromSource = fromSource
                )
            // range = newStart ∪ newEnd
            // Note we don't read TextRange.min/max here because the above code always returns
            // normalized ranges. It's no less correct, but there's no need to do the additional
            // min/max calls inside the min/max properties.
            start = minOf(newStart.start, newEnd.start)
            end = maxOf(newStart.end, newEnd.end)
        }

        return TextRange(start, end)
    }

    private fun mapStep(
        offset: Int,
        opOffset: Int,
        untransformedLen: Int,
        transformedLen: Int,
        fromSource: Boolean
    ): TextRange {
        val srcLen = if (fromSource) untransformedLen else transformedLen
        val destLen = if (fromSource) transformedLen else untransformedLen
        return when {
            // Before the operation, no change.
            offset < opOffset -> TextRange(offset)
            offset == opOffset -> {
                if (srcLen == 0) {
                    // On insertion point, map to inserted range.
                    TextRange(opOffset, opOffset + destLen)
                } else {
                    // On start of replacement, map to start of replaced range.
                    TextRange(opOffset)
                }
            }
            offset < opOffset + srcLen -> {
                if (destLen == 0) {
                    // In deleted range, map to start of deleted range.
                    TextRange(opOffset)
                } else {
                    // In replaced range, map to transformed range.
                    TextRange(opOffset, opOffset + destLen)
                }
            }

            // On end of or after replaced range, offset the offset.
            else -> TextRange(offset - srcLen + destLen)
        }
    }
}

/**
 * An array of 3-tuples of ints. Each element's values are stored as individual values in the
 * underlying array.
 */
@kotlin.jvm.JvmInline
private value class OpArray private constructor(private val values: IntArray) {
    constructor(size: Int) : this(IntArray(size * ElementSize))

    val size: Int
        get() = values.size / ElementSize

    fun set(index: Int, offset: Int, srcLen: Int, destLen: Int) {
        values[index * ElementSize] = offset
        values[index * ElementSize + 1] = srcLen
        values[index * ElementSize + 2] = destLen
    }

    fun copyOf(newSize: Int) = OpArray(values.copyOf(newSize * ElementSize))

    /**
     * Loops through the array between 0 and [max] (exclusive). If [reversed] is false (the
     * default), iterates forward from 0. When it's true, iterates backwards from `max-1`.
     */
    inline fun forEach(
        max: Int,
        reversed: Boolean = false,
        block: (offset: Int, srcLen: Int, destLen: Int) -> Unit
    ) {
        if (max < 0) return
        // Note: This stamps out block twice at the callsite, which is normally bad for an inline
        // function to do. However, this is a file-private function which is only called in one
        // place that would need to have two copies of mostly-identical code anyway. Doing the
        // duplication here keeps the more complicated logic at the callsite more readable.
        if (reversed) {
            for (i in max - 1 downTo 0) {
                val offset = values[i * ElementSize]
                val srcLen = values[i * ElementSize + 1]
                val destLen = values[i * ElementSize + 2]
                block(offset, srcLen, destLen)
            }
        } else {
            for (i in 0 until max) {
                val offset = values[i * ElementSize]
                val srcLen = values[i * ElementSize + 1]
                val destLen = values[i * ElementSize + 2]
                block(offset, srcLen, destLen)
            }
        }
    }

    private companion object {
        const val ElementSize = 3
    }
}
