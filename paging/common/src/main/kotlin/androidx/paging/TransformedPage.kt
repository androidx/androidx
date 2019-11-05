/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

/**
 * Load access pair - page, and index inside (or adjacent)
 */
internal data class LoadHint(
    /**
     * Index of the accessed page relative to initial load = 0
     */
    val sourcePageIndex: Int,

    /**
     * Index either inside, or (in the case of placeholders) outside of page bounds, reflecting
     * how closs access was.
     */
    val indexInPage: Int
)

internal class TransformedPage<T : Any>(
    /**
     * Index of the original page (pre-transformation) relative to initial load = 0
     */
    val sourcePageIndex: Int,

    /**
     * Data to present (post-transformation)
     */
    val data: List<T>,

    /**
     * Size of the original page (pre-transformation)
     */
    val sourcePageSize: Int,

    /**
     * Optional lookup table for page indices.
     *
     * If provided, this table provides a mapping from presentation index -> original,
     * pre-transformation index.
     */
    private val originalIndices: List<Int>?
) {
    init {
        require(originalIndices == null || originalIndices.size == data.size) {
            "If originalIndices (size = ${originalIndices!!.size}) is provided," +
                    " it must be same length as data (size = ${data.size})"
        }
    }

    fun getLoadHint(relativeIndex: Int): LoadHint {
        val indexInPage = when {
            relativeIndex < 0 -> relativeIndex
            relativeIndex >= data.size -> relativeIndex - data.size + sourcePageSize
            originalIndices != null -> originalIndices[relativeIndex]
            else -> relativeIndex
        }
        return LoadHint(sourcePageIndex, indexInPage)
    }
}