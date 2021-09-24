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

import androidx.paging.PagingSource.LoadResult.Page

/**
 * Load access information blob, containing information from presenter.
 */
internal sealed class ViewportHint(
    /**
     * Distance from hint to first loaded item: `anchorPosition - firstLoadedItemPosition`
     *
     * Zero indicates access at boundary
     * Positive -> Within loaded range or in placeholders if greater than size of last page.
     * Negative -> placeholder access.
     *
     * Note: Does not include placeholders.
     */
    val presentedItemsBefore: Int,
    /**
     * Distance from hint to last presented item: `size - index - placeholdersAfter - 1`
     *
     * Zero indicates access at boundary
     * Positive -> Within loaded range or in placeholders if greater than size of last page.
     * Negative -> placeholder access.
     *
     * Note: Does not include placeholders.
     */
    val presentedItemsAfter: Int,
    /**
     * [hintOriginalPageOffset][TransformablePage.hintOriginalPageOffset] of the first presented
     * [TransformablePage] when this [ViewportHint] was created.
     */
    val originalPageOffsetFirst: Int,
    /**
     * [hintOriginalPageOffset][TransformablePage.hintOriginalPageOffset] of the last presented
     * [TransformablePage] when this [ViewportHint] was created.
     */
    val originalPageOffsetLast: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewportHint) return false

        return presentedItemsBefore == other.presentedItemsBefore &&
            presentedItemsAfter == other.presentedItemsAfter &&
            originalPageOffsetFirst == other.originalPageOffsetFirst &&
            originalPageOffsetLast == other.originalPageOffsetLast
    }

    /**
     * @return Count of presented items between this hint, and either:
     *  * the beginning of the list if [loadType] == PREPEND
     *  * the end of the list if loadType == APPEND
     */
    internal fun presentedItemsBeyondAnchor(loadType: LoadType): Int = when (loadType) {
        LoadType.REFRESH -> throw IllegalArgumentException(
            "Cannot get presentedItems for loadType: REFRESH"
        )
        LoadType.PREPEND -> presentedItemsBefore
        LoadType.APPEND -> presentedItemsAfter
    }

    override fun hashCode(): Int {
        return presentedItemsBefore.hashCode() + presentedItemsAfter.hashCode() +
            originalPageOffsetFirst.hashCode() + originalPageOffsetLast.hashCode()
    }

    /**
     * [ViewportHint] reporting presenter state after receiving initial page. An [Initial] hint
     * should never take precedence over an [Access] hint and is only used to inform
     * [PageFetcher] how many items from the initial page load were presented by [PagingDataDiffer]
     */
    class Initial(
        presentedItemsBefore: Int,
        presentedItemsAfter: Int,
        originalPageOffsetFirst: Int,
        originalPageOffsetLast: Int
    ) : ViewportHint(
        presentedItemsBefore = presentedItemsBefore,
        presentedItemsAfter = presentedItemsAfter,
        originalPageOffsetFirst = originalPageOffsetFirst,
        originalPageOffsetLast = originalPageOffsetLast,
    ) {
        override fun toString(): String {
            return """ViewportHint.Initial(
            |    presentedItemsBefore=$presentedItemsBefore,
            |    presentedItemsAfter=$presentedItemsAfter,
            |    originalPageOffsetFirst=$originalPageOffsetFirst,
            |    originalPageOffsetLast=$originalPageOffsetLast,
            |)""".trimMargin()
        }
    }

    /**
     * [ViewportHint] representing an item access that should be used to trigger loads to fulfill
     * prefetch distance.
     */
    class Access(
        /**
         *  Page index offset from initial load
         */
        val pageOffset: Int,
        /**
         * Original index of item in the [Page] with [pageOffset].
         *
         * Three cases to consider:
         *  - [indexInPage] in Page.data.indices -> Hint references original item directly
         *  - [indexInPage] > Page.data.indices -> Hint references a placeholder after the last
         *    presented item.
         *  - [indexInPage] < 0 -> Hint references a placeholder before the first presented item.
         */
        val indexInPage: Int,
        presentedItemsBefore: Int,
        presentedItemsAfter: Int,
        originalPageOffsetFirst: Int,
        originalPageOffsetLast: Int
    ) : ViewportHint(
        presentedItemsBefore = presentedItemsBefore,
        presentedItemsAfter = presentedItemsAfter,
        originalPageOffsetFirst = originalPageOffsetFirst,
        originalPageOffsetLast = originalPageOffsetLast,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Access) return false

            return pageOffset == other.pageOffset &&
                indexInPage == other.indexInPage &&
                presentedItemsBefore == other.presentedItemsBefore &&
                presentedItemsAfter == other.presentedItemsAfter &&
                originalPageOffsetFirst == other.originalPageOffsetFirst &&
                originalPageOffsetLast == other.originalPageOffsetLast
        }

        override fun hashCode(): Int {
            return super.hashCode() + pageOffset.hashCode() + indexInPage.hashCode()
        }

        override fun toString(): String {
            return """ViewportHint.Access(
            |    pageOffset=$pageOffset,
            |    indexInPage=$indexInPage,
            |    presentedItemsBefore=$presentedItemsBefore,
            |    presentedItemsAfter=$presentedItemsAfter,
            |    originalPageOffsetFirst=$originalPageOffsetFirst,
            |    originalPageOffsetLast=$originalPageOffsetLast,
            |)""".trimMargin()
        }
    }
}
