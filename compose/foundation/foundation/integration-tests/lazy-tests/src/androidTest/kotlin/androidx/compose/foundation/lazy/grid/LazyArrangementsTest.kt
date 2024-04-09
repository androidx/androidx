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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.scrollBy
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class LazyArrangementsTest {

    private val ContainerTag = "ContainerTag"

    @get:Rule
    val rule = createComposeRule()

    private var itemSize: Dp = Dp.Infinity
    private var smallerItemSize: Dp = Dp.Infinity
    private var containerSize: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSize = 50.toDp()
        }
        with(rule.density) {
            smallerItemSize = 40.toDp()
        }
        containerSize = itemSize * 5
    }

    // cases when we have not enough items to fill min constraints:

    @Test
    fun vertical_defaultArrangementIsTop() {
        rule.setContent {
            LazyVerticalGrid(
                modifier = Modifier.requiredSize(containerSize),
                columns = GridCells.Fixed(1)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)
    }

    @Test
    fun vertical_centerArrangement() {
        composeVerticalGridWith(Arrangement.Center)
        assertArrangementForTwoItems(Arrangement.Center)
    }

    @Test
    fun vertical_bottomArrangement() {
        composeVerticalGridWith(Arrangement.Bottom)
        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun vertical_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeVerticalGridWith(arrangement)
        assertArrangementForTwoItems(arrangement)
    }

    @Test
    fun horizontal_defaultArrangementIsStart() {
        rule.setContent {
            LazyHorizontalGrid(
                modifier = Modifier.requiredSize(containerSize),
                rows = GridCells.Fixed(1)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)
    }

    @Test
    fun horizontal_centerArrangement() {
        composeHorizontalWith(Arrangement.Center, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Ltr)
    }

    @Test
    fun horizontal_endArrangement() {
        composeHorizontalWith(Arrangement.End, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    @Test
    fun horizontal_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeHorizontalWith(arrangement, LayoutDirection.Ltr)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Ltr)
    }

    @Test
    fun horizontal_rtl_startArrangement() {
        composeHorizontalWith(Arrangement.Center, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Rtl)
    }

    @Test
    fun horizontal_rtl_endArrangement() {
        composeHorizontalWith(Arrangement.End, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Rtl)
    }

    @Test
    fun horizontal_rtl_spacedArrangementNotFillingViewport() {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeHorizontalWith(arrangement, LayoutDirection.Rtl)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Rtl)
    }

    // wrap content and spacing

    @Test
    fun vertical_spacing_affects_wrap_content() {
        rule.setContent {
            LazyVerticalGrid(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.width(itemSize).testTag(ContainerTag),
                columns = GridCells.Fixed(1)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize)
            .assertHeightIsEqualTo(itemSize * 3)
    }

    @Test
    fun horizontal_spacing_affects_wrap_content() {
        rule.setContent {
            LazyHorizontalGrid(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.height(itemSize).testTag(ContainerTag),
                rows = GridCells.Fixed(1)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize * 3)
            .assertHeightIsEqualTo(itemSize)
    }

    // spacing added when we have enough items to fill the viewport

    @Test
    fun vertical_spacing_scrolledToTheTop() {
        rule.setContent {
            LazyVerticalGrid(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f),
                columns = GridCells.Fixed(1)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun vertical_spacing_scrolledToTheBottom() {
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag),
                columns = GridCells.Fixed(1)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(y = itemSize * 2, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 0.5f)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun horizontal_spacing_scrolledToTheStart() {
        rule.setContent {
            LazyHorizontalGrid(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f),
                rows = GridCells.Fixed(1)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun horizontal_spacing_scrolledToTheEnd() {
        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag),
                rows = GridCells.Fixed(1)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = itemSize * 2, density = rule.density)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 0.5f)

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun vertical_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                modifier = Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize),
                columns = GridCells.Fixed(1)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun vertical_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                modifier = Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize),
                columns = GridCells.Fixed(1)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    @Test
    fun horizontal_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun horizontal_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        val spacingSize = with(rule.density) { spacingSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    // with reverseLayout == true

    @Test
    fun vertical_defaultArrangementIsBottomWithReverseLayout() {
        rule.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                reverseLayout = true,
                modifier = Modifier.size(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Bottom, reverseLayout = true)
    }

    @Test
    fun horizontal_defaultArrangementIsEndWithReverseLayout() {
        rule.setContent {
            LazyHorizontalGrid(
                GridCells.Fixed(1),
                reverseLayout = true,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(
            Arrangement.End, LayoutDirection.Ltr, reverseLayout = true
        )
    }

    @Test
    fun vertical_whenArrangementChanges() {
        var arrangement by mutableStateOf(Arrangement.Top)
        rule.setContent {
            LazyVerticalGrid(
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = arrangement,
                columns = GridCells.Fixed(1)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)

        rule.runOnIdle {
            arrangement = Arrangement.Bottom
        }

        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun horizontal_whenArrangementChanges() {
        var arrangement by mutableStateOf(Arrangement.Start)
        rule.setContent {
            LazyHorizontalGrid(
                GridCells.Fixed(1),
                modifier = Modifier.requiredSize(containerSize),
                horizontalArrangement = arrangement
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)

        rule.runOnIdle {
            arrangement = Arrangement.End
        }

        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    @Test
    fun vertical_negativeSpacing_itemsVisible() {
        val state = LazyGridState()
        val halfItemSize = itemSize / 2
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(itemSize),
                verticalArrangement = Arrangement.spacedBy(-halfItemSize),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(halfItemSize)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

            runBlocking {
                state.scrollBy(with(rule.density) { halfItemSize.toPx() })
            }

            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(-halfItemSize)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun horizontal_negativeSpacing_negative_itemsVisible() {
        val state = LazyGridState()
        val halfItemSize = itemSize / 2
        rule.setContent {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(1),
                modifier = Modifier.requiredSize(itemSize),
                horizontalArrangement = Arrangement.spacedBy(-halfItemSize),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(halfItemSize)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

            runBlocking {
                state.scrollBy(with(rule.density) { halfItemSize.toPx() })
            }

            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(-halfItemSize)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun vertical_negativeSpacingLargerThanItem_itemsVisible() {
        val state = LazyGridState(firstVisibleItemIndex = 2)
        val largerThanItemSize = itemSize * 1.5f
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.requiredSize(width = itemSize * 2, height = itemSize),
                verticalArrangement = Arrangement.spacedBy(-largerThanItemSize),
                state = state
            ) {
                items(8) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        repeat(8) {
            rule.onNodeWithTag("$it")
                .assertTopPositionInRootIsEqualTo(0.dp)
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun horizontal_negativeSpacingLargerThanItem_itemsVisible() {
        val state = LazyGridState(firstVisibleItemIndex = 2)
        val largerThanItemSize = itemSize * 1.5f
        rule.setContent {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier = Modifier.requiredSize(width = itemSize, height = itemSize * 2),
                horizontalArrangement = Arrangement.spacedBy(-largerThanItemSize),
                state = state
            ) {
                items(8) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        repeat(8) {
            rule.onNodeWithTag("$it")
                .assertLeftPositionInRootIsEqualTo(0.dp)
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun horizontal_nonDefaultCrossAxisArrangement() {
        val state = LazyGridState()
        rule.setContent {
            LazyHorizontalGrid(
                rows = GridCells.FixedSize(itemSize * 2),
                modifier = Modifier.requiredSize(width = itemSize * 5, height = itemSize * 5),
                verticalArrangement = Arrangement.Center,
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize * 0.5f)
            .assertHeightIsEqualTo(itemSize * 2f)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f)
            .assertHeightIsEqualTo(itemSize * 2f)
    }

    @Test
    fun vertical_nonDefaultCrossAxisArrangement() {
        val state = LazyGridState()
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.FixedSize(itemSize * 2),
                modifier = Modifier.requiredSize(width = itemSize * 5, height = itemSize * 5),
                horizontalArrangement = Arrangement.Center,
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize * 0.5f)
            .assertWidthIsEqualTo(itemSize * 2f)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f)
            .assertWidthIsEqualTo(itemSize * 2f)
    }

    @Test
    fun horizontal_crossAxisArrangement_withAlignment() {
        val state = LazyGridState()
        rule.setContent {
            LazyHorizontalGrid(
                rows = GridCells.FixedSize(itemSize * 2),
                modifier = Modifier.requiredSize(width = itemSize * 5, height = itemSize * 5),
                verticalArrangement = Arrangement.spacedBy(itemSize * 0.5f, Alignment.Bottom),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize * 0.5f)
            .assertHeightIsEqualTo(itemSize * 2f)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 3f)
            .assertHeightIsEqualTo(itemSize * 2f)
    }

    @Test
    fun vertical_crossAxisArrangement_withAlignment() {
        val state = LazyGridState()
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.FixedSize(itemSize * 2),
                modifier = Modifier.requiredSize(width = itemSize * 5, height = itemSize * 5),
                horizontalArrangement = Arrangement.spacedBy(itemSize * 0.5f, Alignment.End),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize * 0.5f)
            .assertWidthIsEqualTo(itemSize * 2f)

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 3f)
            .assertWidthIsEqualTo(itemSize * 2f)
    }

    fun composeVerticalGridWith(arrangement: Arrangement.Vertical) {
        rule.setContent {
            LazyVerticalGrid(
                verticalArrangement = arrangement,
                modifier = Modifier.requiredSize(containerSize),
                columns = GridCells.Fixed(1)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }
    }

    fun composeHorizontalWith(
        arrangement: Arrangement.Horizontal,
        layoutDirection: LayoutDirection
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyHorizontalGrid(
                    horizontalArrangement = arrangement,
                    modifier = Modifier.requiredSize(containerSize),
                    rows = GridCells.Fixed(1)
                ) {
                    items(2) {
                        Item(it)
                    }
                }
            }
        }
    }

    @Composable
    fun Item(index: Int) {
        require(index < 2)
        val size = if (index == 0) itemSize else smallerItemSize
        Box(Modifier.requiredSize(size).testTag(index.toString()))
    }

    fun assertArrangementForTwoItems(
        arrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false
    ) {
        with(rule.density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) { arrange(containerSize.roundToPx(), sizes, outPositions) }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                rule.onNodeWithTag("$realIndex")
                    .assertTopPositionInRootIsEqualTo(position.toDp())
            }
        }
    }

    fun assertArrangementForTwoItems(
        arrangement: Arrangement.Horizontal,
        layoutDirection: LayoutDirection,
        reverseLayout: Boolean = false
    ) {
        with(rule.density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) {
                arrange(containerSize.roundToPx(), sizes, layoutDirection, outPositions)
            }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                val expectedPosition = position.toDp()
                rule.onNodeWithTag("$realIndex")
                    .assertLeftPositionInRootIsEqualTo(expectedPosition)
            }
        }
    }
}
