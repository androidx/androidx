/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridLayoutInfoTest(orientation: Orientation) :
    BaseLazyStaggeredGridWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(
                Orientation.Vertical,
                Orientation.Horizontal,
            )
    }

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 50

    @Before
    fun setUp() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun contentTypeIsCorrect() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(lanes = 1, state = state, modifier = Modifier.requiredSize(30.dp)) {
                items(2, contentType = { it }) { Box(Modifier.size(10.dp)) }
                item { Box(Modifier.size(10.dp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItemsInfo.map { it.contentType })
                .isEqualTo(listOf(0, 1, null))
        }
    }

    @Test
    fun updatedSynchronouslyDuringScroll_smallScrollForward() {
        lateinit var state: LazyStaggeredGridState
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2)
            ) {
                items(100) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(10f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(0)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
                assertThat(state.layoutInfo.itemPairs)
                    .isEqualTo(
                        listOf(
                            0 to axisIntOffset(mainAxis = -10, crossAxis = 0),
                            1 to axisIntOffset(mainAxis = -10, crossAxis = itemSizePx),
                            2 to axisIntOffset(mainAxis = itemSizePx - 10, crossAxis = 0),
                            3 to axisIntOffset(mainAxis = itemSizePx - 10, crossAxis = itemSizePx)
                        )
                    )
            }
        }
    }

    @Test
    fun updatedSynchronouslyDuringScroll_smallScrollBackward() {
        lateinit var state: LazyStaggeredGridState
        val startOffset = itemSizePx / 2
        rule.setContent {
            state =
                rememberLazyStaggeredGridState(initialFirstVisibleItemScrollOffset = startOffset)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2)
            ) {
                items(100) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-10f)
                assertThat(state.firstVisibleItemIndex).isEqualTo(0)
                val expectedOffset = startOffset - 10
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
                assertThat(state.layoutInfo.itemPairs)
                    .isEqualTo(
                        listOf(
                            0 to axisIntOffset(mainAxis = -expectedOffset, crossAxis = 0),
                            1 to axisIntOffset(mainAxis = -expectedOffset, crossAxis = itemSizePx),
                            2 to
                                axisIntOffset(
                                    mainAxis = itemSizePx - expectedOffset,
                                    crossAxis = 0
                                ),
                            3 to
                                axisIntOffset(
                                    mainAxis = itemSizePx - expectedOffset,
                                    crossAxis = itemSizePx
                                )
                        )
                    )
            }
        }
    }

    @Test
    fun updatedSynchronouslyDuringScroll_largeScrollForward() {
        lateinit var state: LazyStaggeredGridState
        rule.setContent {
            state = rememberLazyStaggeredGridState()
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2)
            ) {
                items(100) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx * 3f)
                assertThat(state.layoutInfo.itemPairs)
                    .isEqualTo(
                        listOf(
                            6 to axisIntOffset(mainAxis = 0, crossAxis = 0),
                            7 to axisIntOffset(mainAxis = 0, crossAxis = itemSizePx),
                            8 to axisIntOffset(mainAxis = itemSizePx, crossAxis = 0),
                            9 to axisIntOffset(mainAxis = itemSizePx, crossAxis = itemSizePx),
                        )
                    )
            }
        }
    }

    @Test
    fun updatedSynchronouslyDuringScroll_largeScrollBackward() {
        lateinit var state: LazyStaggeredGridState
        rule.setContent {
            state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 6)
            LazyStaggeredGrid(
                lanes = 2,
                state = state,
                modifier = Modifier.mainAxisSize(itemSizeDp * 1.5f).crossAxisSize(itemSizeDp * 2)
            ) {
                items(100) { Spacer(Modifier.mainAxisSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemSizePx * 3f)
                assertThat(state.layoutInfo.itemPairs)
                    .isEqualTo(
                        listOf(
                            0 to axisIntOffset(mainAxis = 0, crossAxis = 0),
                            1 to axisIntOffset(mainAxis = 0, crossAxis = itemSizePx),
                            2 to axisIntOffset(mainAxis = itemSizePx, crossAxis = 0),
                            3 to axisIntOffset(mainAxis = itemSizePx, crossAxis = itemSizePx),
                        )
                    )
            }
        }
    }

    @Test
    fun snapshotFlowIsNotifiedAboutNewOffsetOnSmallScrolls() {
        var firstItemOffset = 0

        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(lanes = 1, modifier = Modifier.size(15.dp), state = state) {
                items(100) { Box(Modifier.size(10.dp)) }
            }
            LaunchedEffect(state) {
                snapshotFlow { state.layoutInfo }
                    .collectLatest {
                        val offset = it.visibleItemsInfo.firstOrNull()?.offset ?: IntOffset.Zero
                        firstItemOffset = if (vertical) offset.y else offset.x
                    }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollBy(1f) } }

        rule.runOnIdle { assertThat(firstItemOffset).isEqualTo(-1) }
    }

    private val LazyStaggeredGridLayoutInfo.itemPairs: List<Pair<Int, IntOffset>>
        get() = visibleItemsInfo.map { it.index to it.offset }

    private fun axisIntOffset(mainAxis: Int, crossAxis: Int): IntOffset =
        if (vertical) IntOffset(crossAxis, mainAxis) else IntOffset(mainAxis, crossAxis)
}
