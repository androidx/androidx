/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import android.util.Log
import androidx.annotation.IntRange

/**
 * Flattened list of categorized `ItemViewData` (`List<List<ItemViewData>>`) with placeholder
 * entries and category separators.
 *
 * Keyword "position" is defined in `RecyclerView`.
 */
internal class ItemViewDataFlatList(
    categorizedSources: List<List<ItemViewData>>,
    @IntRange(from = 1) columns: Int
) : AbstractList<ItemViewData>() {

    companion object {
        const val LOG_TAG = "ItemViewDataFlatList"
    }

    override val size: Int
        get() = totalSize

    /** Returns number of categories  */
    /** # of categories.  */
    @get:IntRange(from = 0)
    val numberOfCategories: Int
    private val categorizedSources: MutableList<List<ItemViewData>>
    private val categorySizes: IntArray
    private val categoryStartPositions: IntArray
    private val columns: Int

    /** == `size()`, including all types of `ItemViewData`s.  */
    private var totalSize = 0

    init {
        this.categorizedSources = ArrayList(categorizedSources)
        this.columns = columns
        numberOfCategories = this.categorizedSources.size
        categorySizes = IntArray(numberOfCategories)
        categoryStartPositions = IntArray(numberOfCategories)
        updateIndex()
        if (categorizedSources.isEmpty()) {
            Log.wtf(LOG_TAG, "Initialized with empty categorized sources")
        }
    }

    private fun updateIndex() {
        var categoryStartPosition = 0
        for (currentCategoryIndex in 0 until numberOfCategories) {
            val sources: List<ItemViewData> = categorizedSources[currentCategoryIndex]
            val sourcesSize: Int = sources.size
            categoryStartPositions[currentCategoryIndex] = categoryStartPosition
            var sourcesSizeIncludingEmpty: Int
            var rowsInCategory = Math.ceil(sourcesSize / columns.toDouble()).toInt()
            // Guarantee showing at least `minRowsPerCategory` rows for each category.
            rowsInCategory = Math.max(rowsInCategory, EmojiPickerConstants.MIN_ROWS_PER_CATEGORY)
            sourcesSizeIncludingEmpty =
                if (sourcesSize <= 0 || sourcesSize == 1 && sources[0] is EmptyCategoryViewData) {
                    // category separator(occupy entire row) + empty category indicator(occupy entire row)
                    // + placeholder view items
                    1 + 1 + if (rowsInCategory >= 1) (rowsInCategory - 1) * columns else 0
                } else {
                    rowsInCategory * columns + 1 // +1 for category separator
                }
            categorySizes[currentCategoryIndex] = sourcesSizeIncludingEmpty
            categoryStartPosition += sourcesSizeIncludingEmpty
        }
        totalSize = categoryStartPosition
    }

    override fun get(@IntRange(from = 0) index: Int): ItemViewData {
        val currentCategoryIndex = getCategoryIndex(index)
        val indexInCategory = index - categoryStartPositions[currentCategoryIndex]
        return if (indexInCategory < 0) {
            Log.wtf(
                LOG_TAG,
                String.format(
                    "position (%d) for category (%d) is invalid",
                    index,
                    currentCategoryIndex
                )

            )
            DummyViewData.INSTANCE
        } else if (indexInCategory == 0) {
            // Category separator occupies first place.
            CategorySeparatorViewData(
                currentCategoryIndex, indexInCategory, /* categoryName= */""
            )
        } else if (indexInCategory < categorizedSources[currentCategoryIndex].size + 1) {
            // Concrete ItemViewData.
            categorizedSources[currentCategoryIndex][indexInCategory - 1]
        } else if (indexInCategory == 1 && categorizedSources[currentCategoryIndex].isEmpty()) {
            // Empty category indicator.
            EmptyCategoryViewData.INSTANCE
        } else {
            // Placeholder entries located at the end of category.
            DummyViewData.INSTANCE
        }
    }

    /** Returns category index for given `position`  */
    @IntRange(from = 0)
    fun getCategoryIndex(@IntRange(from = 0) position: Int): Int {
        var currentCategoryIndex = 0
        while (currentCategoryIndex + 1 < numberOfCategories &&
            position >= categoryStartPositions[currentCategoryIndex + 1]
        ) {
            currentCategoryIndex++
        }
        return currentCategoryIndex
    }
}