/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.collection.mutableObjectIntMapOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.unit.Constraints

/**
 * A layout that only composes and lays out currently needed items. Can be used to build efficient
 * scrollable layouts.
 *
 * @param itemProvider lambda producing an item provider containing all the needed info about the
 *   items which could be used to compose and measure items as part of [measurePolicy].
 * @param modifier to apply on the layout
 * @param prefetchState allows to schedule items for prefetching
 * @param measurePolicy Measure policy which allows to only compose and measure needed items.
 *
 * Note: this function is a part of [LazyLayout] harness that allows for building custom lazy
 * layouts. LazyLayout and all corresponding APIs are still under development and are subject to
 * change.
 */
@ExperimentalFoundationApi
@Composable
fun LazyLayout(
    itemProvider: () -> LazyLayoutItemProvider,
    modifier: Modifier = Modifier,
    prefetchState: LazyLayoutPrefetchState? = null,
    measurePolicy: LazyLayoutMeasureScope.(Constraints) -> MeasureResult
) {
    val currentItemProvider = rememberUpdatedState(itemProvider)

    LazySaveableStateHolderProvider { saveableStateHolder ->
        val itemContentFactory = remember {
            LazyLayoutItemContentFactory(saveableStateHolder) { currentItemProvider.value() }
        }
        val subcomposeLayoutState = remember {
            SubcomposeLayoutState(LazyLayoutItemReusePolicy(itemContentFactory))
        }
        if (prefetchState != null) {
            val executor = prefetchState.prefetchScheduler ?: rememberDefaultPrefetchScheduler()
            DisposableEffect(prefetchState, itemContentFactory, subcomposeLayoutState, executor) {
                prefetchState.prefetchHandleProvider =
                    PrefetchHandleProvider(itemContentFactory, subcomposeLayoutState, executor)
                onDispose { prefetchState.prefetchHandleProvider = null }
            }
        }

        SubcomposeLayout(
            subcomposeLayoutState,
            modifier.traversablePrefetchState(prefetchState),
            remember(itemContentFactory, measurePolicy) {
                { constraints ->
                    with(LazyLayoutMeasureScopeImpl(itemContentFactory, this)) {
                        measurePolicy(constraints)
                    }
                }
            }
        )
    }
}

private class LazyLayoutItemReusePolicy(private val factory: LazyLayoutItemContentFactory) :
    SubcomposeSlotReusePolicy {
    private val countPerType = mutableObjectIntMapOf<Any?>()

    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        countPerType.clear()
        with(slotIds.iterator()) {
            while (hasNext()) {
                val slotId = next()
                val type = factory.getContentType(slotId)
                val currentCount = countPerType.getOrDefault(type, 0)
                if (currentCount == MaxItemsToRetainForReuse) {
                    remove()
                } else {
                    countPerType[type] = currentCount + 1
                }
            }
        }
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean =
        factory.getContentType(slotId) == factory.getContentType(reusableSlotId)
}

/**
 * We currently use the same number of items to reuse (recycle) items as RecyclerView does: 5
 * (RecycledViewPool.DEFAULT_MAX_SCRAP) + 2 (Recycler.DEFAULT_CACHE_SIZE)
 */
private const val MaxItemsToRetainForReuse = 7
