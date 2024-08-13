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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.test.filters.LargeTest
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListSnapLayoutInfoProviderTest(val config: Config) :
    BaseLazyListTestWithOrientation(config.orientation) {

    lateinit var layoutInfoProvider: SnapLayoutInfoProvider
    lateinit var scope: CoroutineScope
    lateinit var state: LazyListState

    val itemSizeDp = 200.dp
    val itemSizePx = with(rule.density) { itemSizeDp.roundToPx() }
    val minVelocityThreshold = with(rule.density) { MinFlingVelocityDp.roundToPx() }

    @Test
    fun calculateSnappingOffset_velocityIsZero_shouldReturnClosestItemOffset() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout(config.useHeader)
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            assertEquals(
                layoutInfoProvider.calculateSnapOffset(0f).roundToInt(),
                state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 100 }?.offset ?: 0
            )
        }
    }

    @Test
    fun calculateSnappingOffset_velocityPositive_moreThanMinThreshold_shouldReturnNextItemOffset() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout(config.useHeader)
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            val offset =
                state.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == state.firstVisibleItemIndex + 1 }
                    ?.offset
            assertEquals(
                layoutInfoProvider
                    .calculateSnapOffset(2 * minVelocityThreshold.toFloat())
                    .roundToInt(),
                offset ?: 0
            )
        }
    }

    @Test
    fun calculateSnappingOffset_velocityNegative_moreThanMinThreshold_shouldReturnPrevItemOffset() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyListState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout(config.useHeader)
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            val offset =
                state.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == state.firstVisibleItemIndex }
                    ?.offset
            assertEquals(
                layoutInfoProvider
                    .calculateSnapOffset(-2 * minVelocityThreshold.toFloat())
                    .roundToInt(),
                offset ?: 0
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainLayout(includeHeader: Boolean = false) {
        LazyColumnOrRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
        ) {
            if (includeHeader) {
                stickyHeader { Box(modifier = Modifier.size(2 * itemSizeDp)) }
            }
            items(200) { Box(modifier = Modifier.size(itemSizeDp)) }
        }
    }

    companion object {
        class Config(val orientation: Orientation, val useHeader: Boolean)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            arrayOf(
                Config(Orientation.Vertical, true),
                Config(Orientation.Vertical, false),
                Config(Orientation.Horizontal, true),
                Config(Orientation.Horizontal, false)
            )
    }
}
