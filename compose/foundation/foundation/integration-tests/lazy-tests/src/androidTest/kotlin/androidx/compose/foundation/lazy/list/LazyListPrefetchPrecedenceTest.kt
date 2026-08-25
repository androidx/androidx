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
@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.DefaultLazyListCacheWindow
import androidx.compose.foundation.lazy.DefaultLazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListCacheWindowStrategy
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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

class LazyListPrefetchPrecedenceTest {
    @get:Rule val rule = createComposeRule()

    lateinit var state: LazyListState

    private fun composeLazyList(state: LazyListState, cacheWindow: LazyLayoutCacheWindow?) =
        rule.setContent {
            val content: LazyListScope.() -> Unit = {
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
                LazyColumn(modifier = Modifier.height(150.dp), state = state, content = content)
            } else {
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    state = state,
                    cacheWindow = cacheWindow,
                    content = content,
                )
            }
        }

    @Before
    fun setup() {
        ComposeFoundationFlags.isPreferDefaultCacheWindowOverPrefetchStrategyLazyList = true
    }

    @Test
    fun usesDefaultLayoutCacheWindow() {
        composeLazyList(state = LazyListState().also { state = it }, cacheWindow = null)

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyListCacheWindowStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(DefaultLazyListCacheWindow)
    }

    @Test
    fun usesDefaultPrefetchStrategyWhenFeatureFlagDisabled() {
        ComposeFoundationFlags.isPreferDefaultCacheWindowOverPrefetchStrategyLazyList = false
        composeLazyList(state = LazyListState().also { state = it }, cacheWindow = null)

        assertThat(state.layoutInfoState.value.prefetchStrategy)
            .isInstanceOf(DefaultLazyListPrefetchStrategy::class.java)
    }

    @Test
    fun providedCacheWindowIsUsedWhenNoStateStrategy() {
        val layoutCacheWindow = LazyLayoutCacheWindow(0.dp)
        composeLazyList(
            state = LazyListState().also { state = it },
            cacheWindow = layoutCacheWindow,
        )

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyListCacheWindowStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(layoutCacheWindow)
    }

    @Test
    fun prefetchStrategyPreferredWhenProvidedAlongsideCacheWindow() {
        val stateCacheWindow = LazyLayoutCacheWindow(0.dp)
        composeLazyList(
            state = LazyListState(cacheWindow = stateCacheWindow).also { state = it },
            cacheWindow = LazyLayoutCacheWindow(100.dp),
        )

        val cacheWindowPrefetchStrategy =
            state.layoutInfoState.value.prefetchStrategy as? LazyListCacheWindowStrategy
        assertThat(cacheWindowPrefetchStrategy?.cacheWindow).isEqualTo(stateCacheWindow)
    }
}
