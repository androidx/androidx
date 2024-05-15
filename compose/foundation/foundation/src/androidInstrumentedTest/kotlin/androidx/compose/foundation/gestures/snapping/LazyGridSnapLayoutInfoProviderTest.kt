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
import androidx.compose.foundation.lazy.grid.BaseLazyGridTestWithOrientation
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyGridSnapLayoutInfoProviderTest(orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    lateinit var layoutInfoProvider: SnapLayoutInfoProvider
    lateinit var scope: CoroutineScope
    lateinit var state: LazyGridState

    val itemSizeDp = 200.dp
    val itemSizePx = with(rule.density) { itemSizeDp.roundToPx() }
    val minVelocityThreshold = with(rule.density) { MinFlingVelocityDp.roundToPx() }

    @Test
    fun calculateSnappingOffset_velocityIsZero_shouldReturnClosestItemOffset() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyGridState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout()
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            val offset = state.layoutInfo.visibleItemsInfo.first { it.index == 100 }.offset
            val expectedResult = if (vertical) {
                offset.y
            } else {
                offset.x
            }
            assertEquals(
                layoutInfoProvider.calculateSnapOffset(0f).roundToInt(),
                expectedResult
            )
        }
    }

    @Test
    fun calculateSnappingOffset_velocityPositive_moreThanMinThreshold_shouldReturnNextItemOffset() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyGridState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout()
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            val offset = state
                .layoutInfo
                .visibleItemsInfo.first { it.index == state.firstVisibleItemIndex + 3 }.offset
            val expectedResult = if (vertical) {
                offset.y
            } else {
                offset.x
            }
            assertEquals(
                layoutInfoProvider.calculateSnapOffset(2 * minVelocityThreshold.toFloat())
                    .roundToInt(),
                expectedResult
            )
        }
    }

    @Test
    fun calculateSnappingOffset_velocityNegative_moreThanMinThreshold_shouldReturnPrevItemOffset() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyGridState(initialFirstVisibleItemIndex = 100)
            layoutInfoProvider =
                remember(state) { SnapLayoutInfoProvider(state, SnapPosition.Start) }
            MainLayout()
        }

        rule.runOnIdle {
            scope.launch {
                // leave list out of snap
                state.scrollBy(-itemSizePx * 0.2f)
            }
        }
        rule.mainClock.advanceTimeUntil { state.firstVisibleItemScrollOffset != 0 } // apply scroll

        rule.runOnIdle {
            val offset = state
                .layoutInfo
                .visibleItemsInfo
                .first { it.index == state.firstVisibleItemIndex }.offset
            val expectedResult = if (vertical) {
                offset.y
            } else {
                offset.x
            }
            assertEquals(
                layoutInfoProvider.calculateSnapOffset(-2 * minVelocityThreshold.toFloat())
                    .roundToInt(),
                expectedResult
            )
        }
    }

    @Composable
    fun MainLayout() {
        LazyGrid(
            cells = GridCells.Fixed(3),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
        ) {
            items(200) {
                Box(modifier = Modifier.size(itemSizeDp))
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
