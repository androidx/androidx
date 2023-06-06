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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
@OptIn(ExperimentalFoundationApi::class)
internal abstract class LazyGridMeasuredItemProvider @ExperimentalFoundationApi constructor(
    private val itemProvider: LazyGridItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val defaultMainAxisSpacing: Int
) {
    /**
     * Used to subcompose individual items of lazy grids. Composed placeables will be measured
     * with the provided [constraints] and wrapped into [LazyGridMeasuredItem].
     */
    fun getAndMeasure(
        index: Int,
        mainAxisSpacing: Int = defaultMainAxisSpacing,
        constraints: Constraints
    ): LazyGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)
        val placeables = measureScope.measure(index, constraints)
        val crossAxisSize = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            require(constraints.hasFixedHeight)
            constraints.minHeight
        }
        return createItem(
            index,
            key,
            contentType,
            crossAxisSize,
            mainAxisSpacing,
            placeables
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyIndexMap: LazyLayoutKeyIndexMap get() = itemProvider.keyIndexMap

    abstract fun createItem(
        index: Int,
        key: Any,
        contentType: Any?,
        crossAxisSize: Int,
        mainAxisSpacing: Int,
        placeables: List<Placeable>
    ): LazyGridMeasuredItem
}
