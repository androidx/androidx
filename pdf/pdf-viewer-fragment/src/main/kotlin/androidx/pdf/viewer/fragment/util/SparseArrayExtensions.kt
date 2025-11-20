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

package androidx.pdf.viewer.fragment.util

import android.util.SparseArray
import androidx.core.util.isEmpty
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import androidx.pdf.content.PageMatchBounds

/**
 * Counts total number of results in sparse array when flattened.
 *
 * @return total number of results
 */
internal fun SparseArray<List<PageMatchBounds>>.countTotalElements(): Int =
    valueIterator().asSequence().map { it.size }.sum()

/**
 * Calculates flatten index in sparse array, given key-index and list-index.
 *
 * @param selectedResultPageNum: Page num for current selection.
 * @param resultIndex: index of result on [selectedResultPageNum]
 * @return flatten index of current selected result out of total matches.
 */
internal fun SparseArray<List<PageMatchBounds>>.getFlattenedIndex(
    selectedResultPageNum: Int,
    resultIndex: Int,
): Int {
    // Count results up to selectedResultPageNum
    var currentSelection =
        keyIterator()
            .asSequence()
            .takeWhile { it < selectedResultPageNum }
            .map { valueAt(indexOfKey(it)).size }
            .sum()

    // Now, add the item index within the specified list
    currentSelection += resultIndex

    return if (isEmpty()) 0 else currentSelection
}
