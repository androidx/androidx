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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridArrangementsTest(
    orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )

        private const val LazyStaggeredGrid = "Lazy"
    }

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 100

    private lateinit var state: LazyStaggeredGridState

    @Before
    fun setUp() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @Test
    fun arrangement_addsSpacingInBothDirections() {
        state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGrid)
                    .axisSize(itemSizeDp * 3, itemSizeDp * 5),
                state = state,
                mainAxisSpacing = itemSizeDp,
                crossAxisArrangement = Arrangement.spacedBy(itemSizeDp / 2)
            ) {
                items(100) {
                    Spacer(Modifier.testTag("$it").mainAxisSize(itemSizeDp))
                }
            }
        }

        val crossAxisSizeDp = (itemSizeDp * 2.5f) / 2
        val spacing = itemSizeDp / 2

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(crossAxisSizeDp)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(crossAxisSizeDp + spacing)
            .assertCrossAxisSizeIsEqualTo(crossAxisSizeDp)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2) // item + spacing
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(crossAxisSizeDp)

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2) // item + spacing
            .assertMainAxisSizeIsEqualTo(itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(crossAxisSizeDp + spacing)
            .assertCrossAxisSizeIsEqualTo(crossAxisSizeDp)
    }

    @Test
    fun arrangement_lastItem_noSpacingMainAxis() {
        state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGrid)
                    .axisSize(itemSizeDp * 2, itemSizeDp * 5),
                state = state,
                mainAxisSpacing = itemSizeDp
            ) {
                items(100) {
                    BasicText(
                        text = "$it",
                        modifier = Modifier
                            .testTag("$it")
                            .mainAxisSize(itemSizeDp)
                            .debugBorder()
                    )
                }
            }
        }

        state.scrollTo(100)

        rule.onNodeWithTag("98")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 5 - itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("99")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 5 - itemSizeDp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp)
    }

    @Test
    fun negativeSpacing_itemsVisible() {
        state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGrid)
                    .axisSize(itemSizeDp * 2, itemSizeDp * 5),
                state = state,
                mainAxisSpacing = -itemSizeDp
            ) {
                items(100) {
                    BasicText(
                        text = "$it",
                        modifier = Modifier
                            .testTag("$it")
                            .mainAxisSize(itemSizeDp * 2)
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        state.scrollBy(itemSizeDp)

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(-itemSizeDp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun negativeSpacing_withContentPadding_itemsVisible() {
        state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGrid)
                    .axisSize(itemSizeDp * 2, itemSizeDp * 5),
                state = state,
                mainAxisSpacing = -itemSizeDp,
                contentPadding = PaddingValues(beforeContent = itemSizeDp)
            ) {
                items(100) {
                    BasicText(
                        text = "$it",
                        modifier = Modifier
                            .testTag("$it")
                            .mainAxisSize(itemSizeDp * 2)
                    )
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp * 2)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        state.scrollBy(itemSizeDp)

        rule.onNodeWithTag("0")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp)
            .assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        assertThat(state.firstVisibleItemIndex).isEqualTo(2)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun negativeSpacingLargerThanItem_itemsVisible() {
        val state = LazyStaggeredGridState(initialFirstVisibleItemIndex = 2)
        val largerThanItemSize = itemSizeDp * 1.5f
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 2, mainAxis = itemSizeDp),
                mainAxisSpacing = -largerThanItemSize,
                state = state
            ) {
                items(8) { index ->
                    Box(Modifier.size(itemSizeDp).testTag(index.toString()))
                }
            }
        }

        repeat(8) {
            rule.onNodeWithTag("$it")
                .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun nonStartCrossAxisArrangement() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                cells = StaggeredGridCells.FixedSize(itemSizeDp * 2),
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 5, mainAxis = itemSizeDp * 5),
                crossAxisArrangement = Arrangement.Center,
                state = state
            ) {
                items(10) { index ->
                    Box(Modifier.size(itemSizeDp).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 0.5f)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 2.5f)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
    }

    @Test
    fun spacedByWithAlignment() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                cells = StaggeredGridCells.FixedSize(itemSizeDp * 2),
                modifier = Modifier.axisSize(crossAxis = itemSizeDp * 5, mainAxis = itemSizeDp * 5),
                crossAxisArrangement = object : Arrangement.HorizontalOrVertical,
                    Arrangement.Horizontal by Arrangement.spacedBy(
                        itemSizeDp * 0.5f, Alignment.End
                    ),
                    Arrangement.Vertical by Arrangement.spacedBy(
                        itemSizeDp * 0.5f, Alignment.Bottom
                    ) {
                        override val spacing: Dp = itemSizeDp * 0.5f
                    },
                state = state
            ) {
                items(10) { index ->
                    Box(Modifier.size(itemSizeDp).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 0.5f)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSizeDp * 3f)
            .assertCrossAxisSizeIsEqualTo(itemSizeDp * 2)
    }
}
