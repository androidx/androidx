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

package androidx.compose.runtime.tracing

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

@RestrictTo(Scope.LIBRARY_GROUP) public const val BIT_COUNT: Int = 6
/** Size of each block is 2 ^ 6 (64) elements. */
internal const val BLOCK_CAPACITY = 1.shl(bitCount = BIT_COUNT)
/** Size of the block pool. */
internal const val BLOCK_POOL_SIZE = 8

@RestrictTo(Scope.LIBRARY)
public class Stack<T>(blkCount: Int = 4, blkPoolSize: Int = BLOCK_POOL_SIZE) {
    @JvmField public val blkCount: Int = blkCount.coerceIn(1, Int.MAX_VALUE)
    @JvmField
    internal val pool: Pool<Array<Any?>> =
        Pool(capacity = blkPoolSize) { arrayOfNulls(size = BLOCK_CAPACITY) }

    // This is an Array<Array<T>> because it makes resizing things a _lot_ faster.
    @JvmField
    public var blkArray: Array<Array<Any?>> = Array(size = this.blkCount) { pool.obtain() }

    // These variables are being declared as public. This is because the class has to be public
    // for us to be able to write benchmarks against it. But using internal fields from public
    // inline methods makes Metalava extra unhappy, given the outer class is hidden.

    @JvmField public var blkIdx: Int = 0
    @JvmField public var bIdx: Int = 0
    @JvmField public var currentBlk: Array<Any?> = blkArray[blkIdx]

    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun plusAssign(element: T) {
        // ART can only eliminate bounds checks if bIdx and currentBlk are final fields
        val idx = bIdx
        val block = currentBlk
        // This should ideally be BLOCK_CAPACITY, but bounds checks
        if (idx < block.size) {
            block[idx] = element
            bIdx += 1
        } else {
            addSlow(element)
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun addSlow(element: T) {
        blkIdx += 1
        val idx = blkIdx
        val size = blkArray.size
        val next: Array<Any?> =
            if (idx < size) {
                val block = blkArray[idx] as Array<Any?>? // Unchecked
                if (block != null) {
                    block
                } else {
                    // This could happen after we reuse slots. Just obtain a new block if `null`.
                    val newBlk = pool.obtain()
                    blkArray[idx] = newBlk
                    newBlk
                }
            } else {
                val newBlkCnt = size.shl(1)
                val newBlkArray = arrayOfNulls<Array<Any?>>(newBlkCnt)
                System.arraycopy(blkArray, 0, newBlkArray, 0, size)
                for (i in size until newBlkCnt) {
                    newBlkArray[i] = pool.obtain()
                }
                blkArray = newBlkArray as Array<Array<Any?>> // Unchecked
                blkArray[blkIdx]
            }
        next[0] = element
        bIdx = 1
        currentBlk = next
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    public inline fun removeLastOrNull(): T? {
        val idx = bIdx - 1
        if (idx < 0) return removeLastOrNullSlow()
        val element = currentBlk[idx] as T?
        currentBlk[idx] = null
        bIdx = idx
        return element
    }

    @Suppress("UNCHECKED_CAST")
    public fun removeLastOrNullSlow(): T? {
        // bIdx is already 0
        if (blkIdx == 0) return null
        if (blkIdx >= blkCount) {
            val oldBlk = currentBlk
            // We have to many blocks
            val idx = blkIdx
            // We don't really resize here given we don't expect this to be the general case.
            // That is okay, given the larger array should still be relatively small.
            val blkArray = blkArray as Array<Array<Any?>?> // Unchecked
            blkArray[idx] = null
            pool.release(oldBlk)
        }
        blkIdx -= 1
        bIdx = BLOCK_CAPACITY
        currentBlk = blkArray[blkIdx]
        return removeLastOrNull()
    }

    @Suppress("NOTHING_TO_INLINE")
    public inline fun size(): Int = blkIdx.shl(bitCount = BIT_COUNT) + bIdx

    @Suppress("NOTHING_TO_INLINE") public inline fun isEmpty(): Boolean = size() == 0

    @Suppress("NOTHING_TO_INLINE") public inline fun isNotEmpty(): Boolean = size() > 0
}
