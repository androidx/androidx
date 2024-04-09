/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItemProvider
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
@OptIn(ExperimentalFoundationApi::class)
internal abstract class LazyListMeasuredItemProvider @ExperimentalFoundationApi constructor(
    constraints: Constraints,
    isVertical: Boolean,
    private val itemProvider: LazyListItemProvider,
    private val measureScope: LazyLayoutMeasureScope
) : LazyLayoutMeasuredItemProvider<LazyListMeasuredItem> {
    // the constraints we will measure child with. the main axis is not restricted
    val childConstraints = Constraints(
        maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
        maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
    )

    override fun getAndMeasure(index: Int, lane: Int, span: Int, constraints: Constraints) =
        getAndMeasure(index, constraints)

    /**
     * Used to subcompose items of lazy lists. Composed placeables will be measured with the
     * correct constraints and wrapped into [LazyListMeasuredItem].
     */
    fun getAndMeasure(
        index: Int,
        constraints: Constraints = childConstraints
    ): LazyListMeasuredItem {
        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)
        val placeables = measureScope.measure(index, constraints)
        return createItem(index, key, contentType, placeables, constraints)
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     */
    val keyIndexMap: LazyLayoutKeyIndexMap get() = itemProvider.keyIndexMap

    abstract fun createItem(
        index: Int,
        key: Any,
        contentType: Any?,
        placeables: List<Placeable>,
        constraints: Constraints
    ): LazyListMeasuredItem
}
