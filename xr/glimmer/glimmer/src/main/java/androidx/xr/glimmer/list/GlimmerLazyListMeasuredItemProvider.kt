/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastMap

internal class GlimmerLazyListMeasuredItemProvider(
    constraints: Constraints,
    private val layoutProperties: GlimmerLazyListLayoutProperties,
    private val itemProvider: GlimmerLazyListItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
) {

    private val cachedResults = mutableIntObjectMapOf<GlimmerLazyListMeasuredItem>()

    val childConstraints =
        Constraints(
            maxWidth =
                if (layoutProperties.isVertical) constraints.maxWidth else Constraints.Infinity,
            maxHeight =
                if (!layoutProperties.isVertical) constraints.maxHeight else Constraints.Infinity,
        )

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of the
     * list as an optimization.
     */
    val keyIndexMap: LazyLayoutKeyIndexMap
        get() = itemProvider.keyIndexMap

    /**
     * Used to subcompose items of lists. Composed placeables will be measured with the correct
     * constraints and wrapped into [GlimmerLazyListMeasuredItem].
     */
    fun getAndMeasure(index: Int): GlimmerLazyListMeasuredItem {
        return cachedResults.getOrPut(index) {
            val key = itemProvider.getKey(index)
            val contentType = itemProvider.getContentType(index)
            val placeables = measureScope.compose(index).fastMap { it.measure(childConstraints) }
            GlimmerLazyListMeasuredItem(
                index = index,
                key = key,
                contentType = contentType,
                placeables = placeables,
                layoutProperties = layoutProperties,
                itemProvider.itemCount,
            )
        }
    }
}
