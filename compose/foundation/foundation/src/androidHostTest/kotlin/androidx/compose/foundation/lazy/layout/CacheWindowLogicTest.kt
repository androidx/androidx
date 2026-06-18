/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class CacheWindowLogicTest {
    val isMultiLaneCacheWindowEnabled = ComposeFoundationFlags.isMultiLaneCacheWindowEnabled

    @After
    fun after() {
        ComposeFoundationFlags.isMultiLaneCacheWindowEnabled = isMultiLaneCacheWindowEnabled
    }

    @Test
    fun handleLaneResizeResetsStrategy() {
        ComposeFoundationFlags.isMultiLaneCacheWindowEnabled = true
        var lanes = 2
        val cacheWindow = LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f)
        val logic =
            CacheWindowLogic(
                cacheWindow = cacheWindow,
                enableInitialPrefetch = true,
                laneCount = { lanes },
            )

        // Verify initial lane count/sizes
        assertThat(logic.perLaneCacheWindowStartIndex.size).isEqualTo(2)
        assertThat(logic.perLaneCacheWindowEndItemIndex.size).isEqualTo(2)

        // Let's populate some data first so that reset strategy has something to clear/reset.
        val scope = FakeCacheWindowScope()
        with(logic) { scope.onVisibleItemsUpdated() }

        // Verify they got populated/initialized
        assertThat(logic.perLaneCacheWindowStartIndex[0]).isNotEqualTo(Int.MAX_VALUE)
        assertThat(logic.perLaneCacheWindowStartIndex[1]).isNotEqualTo(Int.MAX_VALUE)

        // Change lane count
        lanes = 3

        // Trigger handleLaneResize via hasValidBounds
        logic.hasValidBounds()

        // Verify lane count resized and reset
        assertThat(logic.perLaneCacheWindowStartIndex.size).isEqualTo(3)
        assertThat(logic.perLaneCacheWindowEndItemIndex.size).isEqualTo(3)
        // Verify they are reset to initial values
        assertThat(logic.perLaneCacheWindowStartIndex[0]).isEqualTo(Int.MAX_VALUE)
        assertThat(logic.perLaneCacheWindowStartIndex[1]).isEqualTo(Int.MAX_VALUE)
        assertThat(logic.perLaneCacheWindowStartIndex[2]).isEqualTo(Int.MAX_VALUE)
    }

    private class FakeCacheWindowScope(
        override val totalItemsCount: Int = 10,
        override val visibleLineCount: Int = 2,
        override val hasVisibleItems: Boolean = true,
        override val firstVisibleItemIndex: Int = 0,
        override val lastVisibleItemIndex: Int = 1,
        override val mainAxisViewportSize: Int = 100,
        override val density: Density = Density(1f),
    ) : CacheWindowScope {
        override fun updatePerLaneMainAxisExtraStartSpace(
            perLaneMainAxisExtraStartSpace: IntArray
        ) {}

        override fun updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace: IntArray) {}

        override fun updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex: IntArray) {}

        override fun updatePerLaneVisibleItemIndexes(perLaneVisibleItemIndexes: IntArray) {}

        override fun schedulePrefetch(
            lane: Int,
            itemIndex: Int,
            onItemPrefetched: (itemSize: Int) -> Unit,
        ): List<LazyLayoutPrefetchState.PrefetchHandle> = emptyList()

        override fun getVisibleItemSize(indexInVisibleItems: Int): Int = 10

        override fun getVisibleItemIndex(indexInVisibleItems: Int): Int = indexInVisibleItems

        override fun getVisibleItemKey(indexInVisibleItems: Int): Any = indexInVisibleItems

        override fun getVisibleItemLane(indexInVisibleItems: Int): Int = indexInVisibleItems

        override fun lastItemIndexInLine(currentItemIndex: Int): Int = currentItemIndex

        override fun getLastItemIndex(): Int = totalItemsCount - 1
    }
}
