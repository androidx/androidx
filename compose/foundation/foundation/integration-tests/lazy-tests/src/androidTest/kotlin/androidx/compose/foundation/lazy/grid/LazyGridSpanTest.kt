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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyGridSpanTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun spans() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3),
                state = state,
            ) {
                items(
                    count = 6,
                    span = { index ->
                        when (index) {
                            0 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(3)
                            }
                            1 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(1)
                                GridItemSpan(1)
                            }
                            2 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            3 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(3)
                            }
                            4 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            5 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(1)
                            }
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule
            .onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
        rule
            .onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule
            .onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
        rule
            .onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule
            .onNodeWithTag("5")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItemsInfo[0].span).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItemsInfo[1].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[2].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[3].span).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItemsInfo[4].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[5].span).isEqualTo(1)
        }
    }

    @Test
    fun spansInHorizontalGrid() {
        val rows = 4
        val rowHeight = with(rule.density) { 5.toDp() }
        val itemWidth = with(rule.density) { 10.toDp() }
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                modifier = Modifier.requiredSize(itemWidth * 3, rowHeight * rows),
                state = state,
            ) {
                items(
                    count = 6,
                    span = { index ->
                        when (index) {
                            0 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(3)
                            }
                            1 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(1)
                                GridItemSpan(1)
                            }
                            2 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            3 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(3)
                            }
                            4 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            5 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(1)
                            }
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.width(itemWidth).testTag("$it"))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule
            .onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(rowHeight * 3)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule
            .onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemWidth)
        rule
            .onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(rowHeight)
            .assertLeftPositionInRootIsEqualTo(itemWidth)
        rule
            .onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemWidth * 2)
        rule
            .onNodeWithTag("5")
            .assertTopPositionInRootIsEqualTo(rowHeight)
            .assertLeftPositionInRootIsEqualTo(itemWidth * 2)
        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItemsInfo[0].span).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItemsInfo[1].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[2].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[3].span).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItemsInfo[4].span).isEqualTo(1)
            assertThat(state.layoutInfo.visibleItemsInfo[5].span).isEqualTo(1)
        }
    }

    @Test
    fun spansWithHorizontalSpacing() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        val spacing = with(rule.density) { 4.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier =
                    Modifier.requiredSize(
                        columnWidth * columns + spacing * (columns - 1),
                        itemHeight,
                    ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(
                    count = 2,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            1 -> GridItemSpan(3)
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        rule
            .onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth + spacing)
            .assertWidthIsEqualTo(columnWidth * 3 + spacing * 2)
    }

    @Test
    fun spansMultipleBlocks() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight),
            ) {
                items(
                    count = 1,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                item(
                    span = {
                        if (maxCurrentLineSpan != 3) error("Wrong maxSpan")
                        GridItemSpan(2)
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("1"))
                }
                items(
                    count = 1,
                    span = { index ->
                        if (maxCurrentLineSpan != 1 || index != 0) {
                            error("Wrong span calculation parameters")
                        }
                        GridItemSpan(1)
                    },
                ) {
                    if (it != 0) error("Wrong index")
                    Box(Modifier.height(itemHeight).testTag("2"))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        rule
            .onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule
            .onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
            .assertWidthIsEqualTo(columnWidth)
    }

    @Test
    fun spansLineBreak() {
        val columns = 4
        val columnWidth = with(rule.density) { 5.toDp() }
        val itemHeight = with(rule.density) { 10.toDp() }
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3),
            ) {
                item(
                    span = {
                        if (maxCurrentLineSpan != 4) error("Wrong maxSpan")
                        GridItemSpan(3)
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                items(
                    count = 4,
                    span = { index ->
                        if (
                            maxCurrentLineSpan !=
                                when (index) {
                                    0 -> 1
                                    1 -> 2
                                    2 -> 1
                                    3 -> 2
                                    else -> error("Wrong index")
                                }
                        )
                            error("Wrong maxSpan")
                        GridItemSpan(listOf(2, 1, 2, 2)[index])
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag((it + 1).toString()))
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 3)
        rule
            .onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule
            .onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth)
        rule
            .onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        rule
            .onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth * 2)
    }

    @Test
    fun spansCalculationDoesntCrash() {
        // regression from b/222530458
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state,
                modifier = Modifier.size(100.dp),
            ) {
                repeat(100) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(1.dp))
                    }
                    items(10) { Box(Modifier.fillMaxWidth().height(1.dp)) }
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(state.layoutInfo.totalItemsCount) } }
    }

    @Test
    fun crossAxisCellCount_availableInItemScope_fixedGrid() {
        val columns = 3
        var reportedCount = 0
        rule.setContent {
            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.size(100.dp)) {
                item {
                    reportedCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
            }
        }
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(columns) }
    }

    @Test
    fun crossAxisCellCount_availableInItemScope_horizontalGrid() {
        val rows = 4
        var reportedCount = 0
        rule.setContent {
            LazyHorizontalGrid(rows = GridCells.Fixed(rows), modifier = Modifier.size(100.dp)) {
                item {
                    reportedCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
            }
        }
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(rows) }
    }

    @Test
    fun crossAxisCellCount_availableInItemScope_adaptiveGrid() {
        var reportedCount = 0
        var horizontalPadding by mutableStateOf(0.dp)
        // 350dp width with 100dp minSize and 10dp spacing -> 3 columns initially (3*100 + 2*10 =
        // 320 <= 350)
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.width(350.dp).height(200.dp),
            ) {
                item {
                    reportedCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
            }
        }
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(3) }

        // Adding 20dp horizontal padding on each side reduces available width to 310dp -> 2 columns
        rule.runOnIdle { horizontalPadding = 20.dp }
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(2) }
    }

    @Test
    fun crossAxisCellCount_availableInItemScope_fixedSizeGrid() {
        var width by mutableStateOf(200.dp)
        var reportedCount = 0
        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.FixedSize(60.dp),
                modifier = Modifier.width(width).height(200.dp),
            ) {
                item {
                    reportedCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
            }
        }
        // 200dp width with 60dp fixed cell size -> 3 columns
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(3) }

        // 40dp width (< 60dp cell size) -> falls back to 1 column
        rule.runOnIdle { width = 40.dp }
        rule.runOnIdle { assertThat(reportedCount).isEqualTo(1) }
    }

    @Test
    fun crossAxisCellCount_recomposesWhenColumnCountChanges() {
        var width by mutableStateOf(220.dp)
        var reportedCount = 0
        var compositionCount = 0
        val gridContent: LazyGridScope.() -> Unit = {
            item {
                compositionCount++
                reportedCount = crossAxisCellCount
                Box(Modifier.height((crossAxisCellCount * 10).dp).testTag("adaptiveItem"))
            }
        }

        rule.setContent {
            // Adaptive grid where 220dp..250dp width -> 2 columns, 420dp width -> 4 columns
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.requiredWidth(width).height(200.dp),
                content = gridContent,
            )
        }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(2)
            assertThat(compositionCount).isEqualTo(1)
        }
        rule.onNodeWithTag("adaptiveItem").assertHeightIsEqualTo(20.dp)

        // Resizing within the same adaptive cell-count bucket (2 columns) should not recompose item
        rule.runOnIdle { width = 250.dp }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(2)
            assertThat(compositionCount).isEqualTo(1)
        }
        rule.onNodeWithTag("adaptiveItem").assertHeightIsEqualTo(20.dp)

        rule.runOnIdle { width = 420.dp }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(4)
            assertThat(compositionCount).isEqualTo(2)
        }
        rule.onNodeWithTag("adaptiveItem").assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun crossAxisCellCount_recomposesWhenFixedCellsCountChanges() {
        var columns by mutableIntStateOf(1)
        var reportedCount = 0
        var compositionCount = 0
        val gridContent: LazyGridScope.() -> Unit = {
            item {
                compositionCount++
                reportedCount = crossAxisCellCount
                Box(Modifier.size(10.dp))
            }
        }

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.size(200.dp),
                content = gridContent,
            )
        }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(1)
            assertThat(compositionCount).isEqualTo(1)
        }

        rule.runOnIdle { columns = 3 }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(3)
            assertThat(compositionCount).isEqualTo(2)
        }

        rule.runOnIdle { columns = 1 }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(1)
            assertThat(compositionCount).isEqualTo(3)
        }
    }

    @Test
    fun crossAxisCellCount_recomposesWhenRowCountChanges_horizontalGrid() {
        var height by mutableStateOf(220.dp)
        var reportedCount = 0
        var compositionCount = 0
        val gridContent: LazyGridScope.() -> Unit = {
            item {
                compositionCount++
                reportedCount = crossAxisCellCount
                Box(Modifier.size(10.dp))
            }
        }

        rule.setContent {
            // Adaptive horizontal grid where 220dp height -> 2 rows, 350dp height -> 3 rows
            LazyHorizontalGrid(
                rows = GridCells.Adaptive(100.dp),
                modifier = Modifier.width(200.dp).height(height),
                content = gridContent,
            )
        }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(2)
            assertThat(compositionCount).isEqualTo(1)
        }

        rule.runOnIdle { height = 350.dp }

        rule.runOnIdle {
            assertThat(reportedCount).isEqualTo(3)
            assertThat(compositionCount).isEqualTo(2)
        }
    }

    @Test
    fun crossAxisCellCount_doesNotRecomposeOnScroll() {
        lateinit var state: LazyGridState
        val observedCountsByIndex = mutableMapOf<Int, Int>()
        var item0CompositionCount = 0
        val smallScrollPx = with(rule.density) { 10.dp.toPx() }
        val rowScrollPx = with(rule.density) { 60.dp.toPx() }

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state,
                modifier = Modifier.size(100.dp),
            ) {
                items(50) { index ->
                    if (index == 0) {
                        item0CompositionCount++
                    }
                    observedCountsByIndex[index] = crossAxisCellCount
                    Box(Modifier.size(50.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(observedCountsByIndex[0]).isEqualTo(2)
            assertThat(item0CompositionCount).isEqualTo(1)
        }

        rule.runOnIdle { runBlocking { state.scrollBy(smallScrollPx) } }

        rule.runOnIdle {
            // Scroll by 10.dp (< 50.dp row height) so item 0 remains visible in the viewport,
            // and verify that scrolling did not trigger recomposition of item 0.
            assertThat(state.layoutInfo.visibleItemsInfo.map { it.index }).contains(0)
            assertThat(observedCountsByIndex[0]).isEqualTo(2)
            assertThat(item0CompositionCount).isEqualTo(1)
        }

        // Scroll further so new off-screen items enter the viewport and verify they also observe 2.
        rule.runOnIdle { runBlocking { state.scrollBy(rowScrollPx) } }

        rule.runOnIdle {
            assertThat(observedCountsByIndex[4]).isEqualTo(2)
            assertThat(observedCountsByIndex[5]).isEqualTo(2)
        }
    }

    @Test
    fun crossAxisCellCount_withCustomItemSpansAndStickyHeader() {
        lateinit var state: LazyGridState
        var headerCount = 0
        var doubleSpanCount = 0
        var singleSpanCount = 0
        var fullSpanCount = 0
        var spanScopeMaxLineSpan = 0
        var columns by mutableIntStateOf(4)

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = state,
                modifier = Modifier.size(200.dp),
            ) {
                stickyHeader {
                    headerCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
                item(span = { GridItemSpan(2) }) {
                    doubleSpanCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
                item(span = { GridItemSpan(1) }) {
                    singleSpanCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
                item(
                    span = {
                        spanScopeMaxLineSpan = maxLineSpan
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    fullSpanCount = crossAxisCellCount
                    Box(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            // Sticky headers and items observe the grid's total line capacity (4 columns),
            // matching LazyGridItemSpanScope.maxLineSpan and LazyGridLayoutInfo.maxSpan.
            assertThat(headerCount).isEqualTo(4)
            assertThat(doubleSpanCount).isEqualTo(4)
            assertThat(singleSpanCount).isEqualTo(4)
            assertThat(fullSpanCount).isEqualTo(4)
            assertThat(spanScopeMaxLineSpan).isEqualTo(4)
            assertThat(state.layoutInfo.maxSpan).isEqualTo(4)
        }

        rule.runOnIdle { columns = 6 }

        rule.runOnIdle {
            assertThat(headerCount).isEqualTo(6)
            assertThat(doubleSpanCount).isEqualTo(6)
            assertThat(singleSpanCount).isEqualTo(6)
            assertThat(fullSpanCount).isEqualTo(6)
            assertThat(spanScopeMaxLineSpan).isEqualTo(6)
            assertThat(state.layoutInfo.maxSpan).isEqualTo(6)
        }
    }

    @Test
    fun crossAxisCellCount_preservedWhenItemsUpdate() {
        var itemCount by mutableIntStateOf(2)
        var columns by mutableIntStateOf(3)
        val item0ObservedCounts = mutableListOf<Int>()
        var item0DerivedCount = 0
        val observedCountsByIndex = mutableMapOf<Int, Int>()
        val gridContent: LazyGridScope.() -> Unit = {
            items(itemCount, key = { it }) { index ->
                if (index == 0) {
                    item0ObservedCounts.add(crossAxisCellCount)
                    val derived by remember { derivedStateOf { crossAxisCellCount } }
                    item0DerivedCount = derived
                }
                observedCountsByIndex[index] = crossAxisCellCount
                Box(Modifier.size(10.dp))
            }
        }

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.size(200.dp),
                content = gridContent,
            )
        }

        rule.runOnIdle {
            assertThat(item0ObservedCounts).containsExactly(3)
            assertThat(item0DerivedCount).isEqualTo(3)
            assertThat(observedCountsByIndex).containsExactlyEntriesIn(mapOf(0 to 3, 1 to 3))
        }

        // Updating itemCount recreates LazyGridIntervalContent; item 0 continues observing
        // crossAxisCellCount == 3 without transiently resetting to 1, while newly added items
        // 2 and 3 also observe 3.
        rule.runOnIdle { itemCount = 4 }

        rule.runOnIdle {
            assertThat(item0ObservedCounts).containsExactly(3, 3)
            assertThat(item0DerivedCount).isEqualTo(3)
            assertThat(observedCountsByIndex)
                .containsExactlyEntriesIn(mapOf(0 to 3, 1 to 3, 2 to 3, 3 to 3))
        }

        // Changing column count after a dataset update still updates remembered derivedStateOf
        // inside item 0 because the same LazyGridItemScopeImpl instance is retained.
        rule.runOnIdle { columns = 5 }

        rule.runOnIdle {
            assertThat(item0ObservedCounts).containsExactly(3, 3, 5).inOrder()
            assertThat(item0DerivedCount).isEqualTo(5)
            assertThat(observedCountsByIndex)
                .containsExactlyEntriesIn(mapOf(0 to 5, 1 to 5, 2 to 5, 3 to 5))
        }
    }

    /**
     * Verifies that toggling a dynamic span state while scrolling through the grid does not cause a
     * crash with an [IllegalStateException] from duplicate item placement.
     */
    @Test
    fun dynamicSpanStateChangeDuringScroll_doesNotCrashWithPlacementException() {
        var wideSpans by mutableStateOf(true)
        lateinit var state: LazyGridState
        val itemSize = with(rule.density) { 10.toDp() }
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state,
                modifier = Modifier.requiredSize(itemSize * 2, itemSize * 4),
            ) {
                items(
                    count = 51,
                    span = { index ->
                        if (wideSpans && index % 3 == 0) GridItemSpan(2) else GridItemSpan(1)
                    },
                ) {
                    Box(Modifier.size(itemSize))
                }
            }
        }

        // Scroll forward, backward, and to an intermediate position to populate multiple buckets,
        // lastLineIndex, and cachedBucket entries under the initial wide-span configuration.
        rule.runOnIdle { runBlocking { state.scrollToItem(47, 1) } }
        rule.runOnIdle { runBlocking { state.scrollToItem(1, 14) } }
        rule.runOnIdle { runBlocking { state.scrollToItem(20, 8) } }

        rule.runOnIdle { wideSpans = false }

        rule.runOnIdle {
            // While wideSpans = true, item 20 was in column 1 of row [19, 20], so scrollToItem(20)
            // anchored at row start item 19. When wideSpans = false, item 19 is in column 1 of
            // row [18, 19], so remeasuring anchors at row start item 18.
            assertThat(state.firstVisibleItemIndex).isEqualTo(18)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(8)
            assertThat(state.layoutInfo.visibleItemsInfo).isNotEmpty()
            state.layoutInfo.visibleItemsInfo.forEach {
                assertThat(it.row).isEqualTo(it.index / 2)
                assertThat(it.column).isEqualTo(it.index % 2)
                assertThat(it.span).isEqualTo(1)
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(20, 28) } }
        rule.runOnIdle {
            // With itemSize = 10px and 2 items per row, scrollOffset = 28px advances 2 full rows
            // (4 items) past item 20 to item 24 with an 8px scroll offset.
            assertThat(state.firstVisibleItemIndex).isEqualTo(24)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(8)
            assertThat(state.layoutInfo.visibleItemsInfo).isNotEmpty()
            state.layoutInfo.visibleItemsInfo.forEach {
                assertThat(it.row).isEqualTo(it.index / 2)
                assertThat(it.column).isEqualTo(it.index % 2)
                assertThat(it.span).isEqualTo(1)
            }
        }
    }

    /**
     * Verifies that when dynamic spans are toggled after populating the line cache, remaining items
     * correctly relayout and occupy the expected rows and columns based on the new span layout.
     */
    @Test
    fun dynamicSpanStateChange_relayoutsRemainingItemsCorrectly() {
        var wideSpans by mutableStateOf(true)
        lateinit var state: LazyGridState
        val itemSize = with(rule.density) { 10.toDp() }
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = state,
                modifier = Modifier.requiredSize(itemSize * 4, itemSize * 3),
            ) {
                items(
                    count = 200,
                    span = { index ->
                        if (wideSpans && index % 10 == 0) GridItemSpan(4) else GridItemSpan(1)
                    },
                ) {
                    Box(Modifier.size(itemSize))
                }
            }
        }

        // Scroll far enough (past initial nearestRange) that the line cache is populated with the
        // wide layout.
        rule.runOnIdle { runBlocking { state.scrollToItem(120) } }

        rule.runOnIdle { wideSpans = false }

        // Verify visible items at index 120 immediately relayout before any subsequent scroll.
        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(120)
            assertThat(state.layoutInfo.visibleItemsInfo).isNotEmpty()
            state.layoutInfo.visibleItemsInfo.forEach {
                assertThat(it.row).isEqualTo(it.index / 4)
                assertThat(it.column).isEqualTo(it.index % 4)
                assertThat(it.span).isEqualTo(1)
            }
        }

        // Every span is now 1, so with 4 columns item N has to live on row N / 4 across the grid.
        for (targetItemIndex in intArrayOf(0, 40, 90, 150, 30, 199, 7)) {
            rule.runOnIdle {
                runBlocking { state.scrollToItem(targetItemIndex) }
                assertThat(state.layoutInfo.visibleItemsInfo.map { it.index })
                    .contains(targetItemIndex)
                state.layoutInfo.visibleItemsInfo.forEach {
                    assertThat(it.row).isEqualTo(it.index / 4)
                    assertThat(it.column).isEqualTo(it.index % 4)
                    assertThat(it.span).isEqualTo(1)
                }
            }
        }
    }

    /**
     * Verifies that changing a snapshot state read only by off-screen items (e.g. index < 50 while
     * scrolled to item 200) invalidates the cached bucket bookmarks and triggers a remeasure of the
     * visible items without requiring a manual scroll.
     */
    @Test
    fun indexDependentOffscreenSpanChange_triggersRemeasureAndRelayoutsVisibleItems() {
        var headerItemsWide by mutableStateOf(true)
        lateinit var state: LazyGridState
        val itemSize = with(rule.density) { 10.toDp() }
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = state,
                modifier = Modifier.requiredSize(itemSize * 3, itemSize * 4),
            ) {
                items(
                    count = 300,
                    span = { index ->
                        if (index < 50) {
                            GridItemSpan(if (headerItemsWide) 3 else 1)
                        } else {
                            GridItemSpan(1)
                        }
                    },
                ) {
                    Box(Modifier.size(itemSize))
                }
            }
        }

        // Scroll to item 200 (far past index 50).
        // When headerItemsWide = true:
        // - items 0..49 take 50 lines (1 per line)
        // - items 50..199 (150 items) take 50 lines (3 per line)
        // So item 200 is at column (200 - 50) % 3 = 0 on line 100.
        rule.runOnIdle {
            runBlocking { state.scrollToItem(200) }
            val firstVisibleBefore = state.layoutInfo.visibleItemsInfo.first { it.index == 200 }
            assertThat(firstVisibleBefore.row).isEqualTo(100)
            assertThat(firstVisibleBefore.column).isEqualTo(0)
        }

        // Toggle state read only by off-screen items (index < 50).
        // Now all items 0..200 have span 1, so item 200 belongs at column 200 % 3 = 2 on line 66.
        rule.runOnIdle { headerItemsWide = false }

        rule.runOnIdle {
            val firstVisibleAfter = state.layoutInfo.visibleItemsInfo.first { it.index == 200 }
            assertThat(firstVisibleAfter.row).isEqualTo(200 / 3)
            assertThat(firstVisibleAfter.column).isEqualTo(200 % 3)
        }

        // Toggle off-screen state a second time (false -> true) to verify re-subscription after
        // cache invalidation cleared observedStates.
        rule.runOnIdle { headerItemsWide = true }

        rule.runOnIdle {
            val firstVisibleRestored = state.layoutInfo.visibleItemsInfo.first { it.index == 200 }
            assertThat(firstVisibleRestored.row).isEqualTo(100)
            assertThat(firstVisibleRestored.column).isEqualTo(0)
        }
    }
}
