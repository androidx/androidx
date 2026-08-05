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
@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "DEPRECATION",
) // b/407927787 // b/420551535
@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LazyGridPrefetchPrecedenceTest {
    @get:Rule val rule = createComposeRule()

    lateinit var state: LazyGridState

    private fun composeLazyGrid(state: LazyGridState, cacheWindow: LazyLayoutCacheWindow?) =
        rule.setContent {
            val content: LazyGridScope.() -> Unit = {
                items(10) {
                    Spacer(
                        Modifier.height(100.dp).testTag("$it").layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    )
                }
            }
            if (cacheWindow == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.height(150.dp),
                    state = state,
                    content = content,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.height(150.dp),
                    state = state,
                    cacheWindow = cacheWindow,
                    content = content,
                )
            }
        }

    @Before
    fun setup() {
        ComposeFoundationFlags.isPreferDefaultCacheWindowOverPrefetchStrategy = true
    }

    @Test
    fun usesDefaultLayoutCacheWindow() {
        composeLazyGrid(state = LazyGridState().also { state = it }, cacheWindow = null)

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyGridCacheWindowPrefetchStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(DefaultLazyGridCacheWindow)
    }

    @Test
    fun usesDefaultPrefetchStrategyWhenFeatureFlagDisabled() {
        ComposeFoundationFlags.isPreferDefaultCacheWindowOverPrefetchStrategy = false
        composeLazyGrid(state = LazyGridState().also { state = it }, cacheWindow = null)

        assertThat(state.layoutInfoState.value.prefetchStrategy)
            .isInstanceOf(DefaultLazyGridPrefetchStrategy::class.java)
    }

    @Test
    fun providedCacheWindowIsUsedWhenNoStateStrategy() {
        val layoutCacheWindow = LazyLayoutCacheWindow(0.dp)
        composeLazyGrid(
            state = LazyGridState().also { state = it },
            cacheWindow = layoutCacheWindow,
        )

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyGridCacheWindowPrefetchStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(layoutCacheWindow)
    }

    @Test
    fun preferPrefetchStrategyPreferredWhenProvidedAlongsideCacheWindow() {
        val stateCacheWindow = LazyLayoutCacheWindow(0.dp)
        composeLazyGrid(
            state = LazyGridState(cacheWindow = stateCacheWindow).also { state = it },
            cacheWindow = LazyLayoutCacheWindow(100.dp),
        )

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyGridCacheWindowPrefetchStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(stateCacheWindow)
    }
}
