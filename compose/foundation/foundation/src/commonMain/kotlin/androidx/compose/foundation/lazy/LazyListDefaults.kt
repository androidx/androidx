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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.DefaultLazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Contains the default values used by [LazyColumn] and [LazyRow]. */
internal object LazyListDefaults {

    /**
     * Creates and remembers the default [LazyLayoutCacheWindow] for [LazyColumn] and [LazyRow].
     * - Prefetches items ahead in the scroll direction based on the average size of visible items
     *   from [state], clamped between 10% and 50% of the viewport.
     * - Does not retain items behind the viewport.
     * - Does not cache while the user is not scrolling.
     *
     * @param state the [LazyListState] used to calculate the average visible item size
     * @return the default [LazyLayoutCacheWindow]
     */
    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    fun cacheWindow(state: LazyListState): LazyLayoutCacheWindow =
        remember(state) {
            DefaultLazyLayoutCacheWindow(
                { state.layoutInfo.visibleItemsAverageSize() },
                ComposeFoundationFlags.isUsingDynamicDefaultCacheWindowInLists,
            )
        }
}
