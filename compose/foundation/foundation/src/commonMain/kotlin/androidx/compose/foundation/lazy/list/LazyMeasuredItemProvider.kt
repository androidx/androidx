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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceable
import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceablesProvider
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
internal class LazyMeasuredItemProvider(
    constraints: Constraints,
    isVertical: Boolean,
    private val itemsProvider: LazyListItemsProvider,
    private val placeablesProvider: LazyLayoutPlaceablesProvider,
    private val measuredItemFactory: MeasuredItemFactory
) {
    // the constraints we will measure child with. the main axis is not restricted
    val childConstraints = Constraints(
        maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
        maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
    )

    /**
     * Used to subcompose items of lazy lists. Composed placeables will be measured with the
     * correct constraints and wrapped into [LazyMeasuredItem].
     */
    fun getAndMeasure(index: DataIndex): LazyMeasuredItem {
        val key = itemsProvider.getKey(index.value)
        val placeables = placeablesProvider.getAndMeasure(index.value, childConstraints)
        return measuredItemFactory.createItem(index, key, placeables)
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = itemsProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredItemFactory {
    fun createItem(
        index: DataIndex,
        key: Any,
        placeables: Array<LazyLayoutPlaceable>
    ): LazyMeasuredItem
}
