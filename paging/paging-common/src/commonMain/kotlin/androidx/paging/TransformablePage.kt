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

internal data class TransformablePage<T : Any>(
    /**
     * List of original, pre-transformation pageOffsets from the initial refresh (0) that this
     * [TransformablePage] anchors on. This array is always sorted.
     *
     * In general this IntArray contains only the [hintOriginalPageOffset]. In the case of pages
     * inserted after data was loaded, such as separators added through [insertSeparators],
     * [originalPageOffsets] would contain the [originalPageOffsets] of the pages that the inserted
     * page anchors on (non-empty pages before and/or after the inserted page).
     *
     * For example:
     * - PRE-TRANSFORM: page1 = ["add", "ant"], page2 = [], page3 = ["cat", "cant"]
     * - POST-TRANSFORM:page1 = ["add", "ant"], page2 = [], page3 = ["C"], page4 = ["cat", "cant"]
     *
     * The inserted page's originalPageOffsets = [0, 2] which is calculated from PRE-TRANSFORM's
     * [`page1.originalPageOffsets` + `page3.originalPageOffsets`].
     *
     * Given the same example except that page1 was empty:
     * - PRE-TRANSFORM: page1 = [], page2 = [], page3 = ["cat", "cant"]
     * - POST-TRANSFORM: page1 = [], page2 = [], page3 = ["C"], page4 = ["cat", "cant"]
     *
     * The resulting inserted page's originalPageOffsets = [2], which is taken from PRE-TRANSFORM's
     * `page3.originalPageOffsets`
     */
    val originalPageOffsets: IntArray,

    /**
     * Data contained in this page
     *
     * If [hintOriginalIndices] were null, the data has not been transformed. Otherwise, the data is
     * post-transform.
     */
    val data: List<T>,

    /**
     * Original (pre-transformation) page offset relative to initial = 0, that [hintOriginalIndices]
     * are associated with.
     *
     * For example if there were 5 loaded pages consisting of 2 prepends, 1 refresh, and then 3
     * appends, their offset would be [-2] [-1] [0] [1] [2] [3] respectively.
     */
    val hintOriginalPageOffset: Int,

    /**
     * Optional lookup table for page indices. Represents the original item indices of [data] before
     * they were transformed.
     *
     * If null, the indices of [data] map directly to their original pre-transformation index. This
     * could mean that this page has yet to be transformed (i.e. created with raw loaded data), or
     * the transform was no-op.
     *
     * If provided, this page has already been transformed. For example if the original
     * TransformablePage contains data [a, b, c] and was transformed into current TransformablePage
     * with data [a, c], then current [hintOriginalIndices] = [0, 2].
     *
     * Note: [hintOriginalIndices] refers to indices of the original item which can be found in the
     * loaded pages with pageOffset == [hintOriginalPageOffset].
     */
    val hintOriginalIndices: List<Int>?,
) {
    /**
     * Simple constructor for creating pre-transformation pages, which don't need an index lookup
     * and only reference a single [originalPageOffset].
     */
    constructor(
        originalPageOffset: Int,
        data: List<T>,
    ) : this(intArrayOf(originalPageOffset), data, originalPageOffset, null)

    init {
        require(originalPageOffsets.isNotEmpty()) {
            "originalPageOffsets cannot be empty when constructing TransformablePage"
        }

        require(hintOriginalIndices == null || hintOriginalIndices.size == data.size) {
            "If originalIndices (size = ${hintOriginalIndices!!.size}) is provided," +
                " it must be same length as data (size = ${data.size})"
        }
    }

    fun viewportHintFor(
        index: Int,
        presentedItemsBefore: Int,
        presentedItemsAfter: Int,
        originalPageOffsetFirst: Int,
        originalPageOffsetLast: Int,
    ) =
        ViewportHint.Access(
            pageOffset = hintOriginalPageOffset,
            indexInPage =
                when {
                    hintOriginalIndices?.indices?.contains(index) == true ->
                        hintOriginalIndices[index]
                    else -> index
                },
            presentedItemsBefore = presentedItemsBefore,
            presentedItemsAfter = presentedItemsAfter,
            originalPageOffsetFirst = originalPageOffsetFirst,
            originalPageOffsetLast = originalPageOffsetLast,
        )

    // Do not edit. Implementation generated by Studio, since data class uses referential equality
    // for IntArray.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TransformablePage<*>

        if (!originalPageOffsets.contentEquals(other.originalPageOffsets)) return false
        if (data != other.data) return false
        if (hintOriginalPageOffset != other.hintOriginalPageOffset) return false
        if (hintOriginalIndices != other.hintOriginalIndices) return false

        return true
    }

    // Do not edit. Implementation generated by Studio, since data class uses referential  equality
    // for IntArray.
    override fun hashCode(): Int {
        var result = originalPageOffsets.contentHashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + hintOriginalPageOffset
        result = 31 * result + (hintOriginalIndices?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun <T : Any> empty(): TransformablePage<T> = TransformablePage(0, emptyList())
    }
}
