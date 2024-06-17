/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.collection.IntList
import kotlin.jvm.JvmOverloads

// TODO(b/311454748): Move to :collection as public API once it's back on alpha. Also, add versions
//  for LongList and FloatList.

/**
 * [IntArray.binarySearch] For original documentation.
 *
 * Searches the list or the range of the list for the provided [element] using the binary search
 * algorithm.
 * The list is expected to be sorted, otherwise the result is undefined.
 *
 * If the list contains multiple elements equal to the specified [element], there is no
 * guarantee which one will be found.
 *
 * @param element the to search for.
 * @param fromIndex the start of the range (inclusive) to search in, 0 by default.
 * @param toIndex the end of the range (exclusive) to search in, size of this list by default.
 *
 * @return the index of the element, if it is contained in the list within the specified range;
 * otherwise, the inverted insertion point `(-insertion point - 1)`.
 * The insertion point is defined as the index at which the element should be inserted,
 * so that the list (or the specified subrange of list) still remains sorted.
 *
 * @throws IndexOutOfBoundsException if [fromIndex] is less than zero or [toIndex] is greater
 * than the size of this list.
 * @throws IllegalArgumentException if [fromIndex] is greater than [toIndex].
 */
@JvmOverloads
internal fun IntList.binarySearch(element: Int, fromIndex: Int = 0, toIndex: Int = size): Int {
    requirePrecondition(fromIndex <= toIndex) { "fromIndex($fromIndex) > toIndex($toIndex)" }
    if (fromIndex < 0) {
        throw IndexOutOfBoundsException("Index out of range: $fromIndex")
    }
    if (toIndex > size) {
        throw IndexOutOfBoundsException("Index out of range: $toIndex")
    }

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = low + high ushr 1
        val midVal = this[mid]
        if (midVal < element) {
            low = mid + 1
        } else if (midVal > element) {
            high =
                mid - 1
        } else {
            return mid // key found
        }
    }
    return -(low + 1) // key not found.
}
