/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyGridHeadersTest(orientation: Orientation) : BaseLazyGridTestWithOrientation(orientation) {

    private val LazyGridTag = "LazyGrid"

    @Test
    fun lazyVerticalGridShowsHeader() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(300.dp)) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertDoesNotExist()
    }

    @Test
    fun lazyVerticalGridwithPinnedItem() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        var pinnableItem: PinnableContainer? = null
        rule.setContent {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(300.dp)) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                items(items) {
                    if (it == "1") {
                        pinnableItem = LocalPinnableContainer.current
                    }
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it))
                }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }
            }
        }

        rule.runOnIdle { pinnableItem?.pin() }

        rule.onNodeWithTag(firstHeaderTag).assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag(secondHeaderTag).assertDoesNotExist()

        rule.onRoot().performTouchInput { swipeUp() }

        rule.onNodeWithTag(firstHeaderTag).assertIsNotDisplayed()
        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()
    }

    @Test
    fun lazyVerticalGridShowsHeadersOnScroll() {
        val items = (1..3).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp).testTag(LazyGridTag),
                state = rememberLazyGridState().also { state = it },
            ) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = 102.dp, density = rule.density)

        rule
            .onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(IntOffset.Zero, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()
    }

    @Test
    fun lazyVerticalGridHeaderIsReplaced() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp).testTag(LazyGridTag),
            ) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = 105.dp, density = rule.density)

        rule.onNodeWithTag(firstHeaderTag).assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun lazyHorizontalGridShowsHeader() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.width(300.dp)) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertDoesNotExist()
    }

    @Test
    fun lazyHorizontalGridShowsHeadersOnScroll() {
        val items = (1..3).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.width(300.dp).testTag(LazyGridTag),
                state = rememberLazyGridState().also { state = it },
            ) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(x = 102.dp, density = rule.density)

        rule
            .onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(IntOffset.Zero, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()
    }

    @Test
    fun lazyHorizontalGridHeaderIsReplaced() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.width(300.dp).testTag(LazyGridTag),
            ) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(x = 105.dp, density = rule.density)

        rule.onNodeWithTag(firstHeaderTag).assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun headerIsDisplayedWhenItIsFullyInContentPadding() {
        val headerTag = "header"
        val itemIndexPx = 100
        val itemIndexDp = with(rule.density) { itemIndexPx.toDp() }
        lateinit var state: LazyGridState

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.requiredSize(itemIndexDp * 4),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(top = itemIndexDp * 2),
            ) {
                stickyHeader { Spacer(Modifier.requiredSize(itemIndexDp).testTag(headerTag)) }

                items((0..11).toList()) {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1, itemIndexPx / 2) } }

        rule.onNodeWithTag(headerTag).assertTopPositionInRootIsEqualTo(itemIndexDp / 2)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(
                itemIndexPx / 2 - /* content padding size */ itemIndexPx * 2,
                state.layoutInfo.visibleItemsInfo.first().offset.y,
            )
        }

        rule.onNodeWithTag("0").assertTopPositionInRootIsEqualTo(itemIndexDp * 3 / 2)
    }

    @Test
    fun lazyVerticalGridShowsHeader2() {
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        val itemSizeDp = with(rule.density) { 100.toDp() }
        val scrollDistance = 20
        val scrollDistanceDp = with(rule.density) { scrollDistance.toDp() }
        val state = LazyGridState()

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(itemSizeDp * 3.5f),
                state = state,
            ) {
                stickyHeader {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(firstHeaderTag))
                }
                stickyHeader {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(secondHeaderTag))
                }

                items(100) {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(it.toString()))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollBy(scrollDistance.toFloat()) } }

        rule.onNodeWithTag(firstHeaderTag).assertTopPositionInRootIsEqualTo(-scrollDistanceDp)
        rule
            .onNodeWithTag(secondHeaderTag)
            .assertTopPositionInRootIsEqualTo(itemSizeDp - scrollDistanceDp)
        rule.onNodeWithTag("0").assertTopPositionInRootIsEqualTo(itemSizeDp * 2 - scrollDistanceDp)
    }

    @Test
    fun lazyGrid_withEmptyHeader_shouldNotCrash() {
        val items = (1..2).map { it.toString() }
        val itemSizeDp = with(rule.density) { 100.toDp() }
        val error = runCatching {
            rule.setContent {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(itemSizeDp * 3.5f),
                ) {
                    stickyHeader {}

                    items(items) { Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(it)) }
                }
            }
        }

        assertTrue { error.isSuccess }
    }

    @Test
    fun lazyGrid_withEmptyHeader_showsHeadersOnScroll() {
        val headerTag = "headerTag"
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(300.dp).testTag(LazyGridTag),
                rememberLazyGridState().also { state = it },
            ) {
                stickyHeader { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(headerTag)) }

                repeat(10) {
                    item { Spacer(Modifier.height(101.dp).fillMaxWidth()) }

                    // this empty header shouldn't be affecting the real header
                    stickyHeader {}
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = 10.dp, density = rule.density)

        rule.onNodeWithTag(headerTag).assertIsDisplayed().assertTopPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(AxisAwareIntOffset(0, 0), state.layoutInfo.visibleItemsInfo.first().offset)
        }
    }

    @Test
    fun lazyGrid_withHeader_focusScrollsRevealsEntireItemUnderHeaderIgnoringContentPadding() {
        lateinit var state: LazyGridState
        var contentPadding by mutableStateOf(PaddingValues())
        val headerSize = 5.dp
        val focusRequesters = List(10) { FocusRequester() }
        rule.setContentWithTestViewConfiguration {
            key(contentPadding) {
                LazyGridWithFocussableItems(
                    state = rememberLazyGridState().also { state = it },
                    viewportSize = 35.dp,
                    itemSize = 10.dp,
                    reverseLayout = false,
                    focusRequesters = focusRequesters,
                ) {
                    stickyHeader { Spacer(Modifier.mainAxisSize(headerSize).fillMaxCrossAxis()) }
                }
            }
        }

        listOf(0.dp, 5.dp).forEach { padding ->
            rule.runOnIdle { contentPadding = PaddingValues(padding) }
            rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
            rule.runOnIdle { focusRequesters[0].requestFocus() }

            rule.runOnIdle {
                val headerSizePixels = with(rule.density) { headerSize.toPx() }.toInt()
                assertEquals(
                    headerSizePixels - state.layoutInfo.beforeContentPadding,
                    state.layoutInfo.visibleItemsInfo.find { it.index == 1 }!!.offset.mainAxis,
                )
            }
        }
    }

    @Test
    fun lazyGrid_withMultipleHeaders_focusScrollsRevealsEntireItemUnderHeaders() {
        lateinit var state: LazyGridState
        val headerSize = 5.dp
        val focusRequesters = List(10) { FocusRequester() }

        rule.setContentWithTestViewConfiguration {
            LazyGridWithFocussableItems(
                viewportSize = 35.dp,
                state = rememberLazyGridState().also { state = it },
                itemSize = 10.dp,
                reverseLayout = false,
                focusRequesters = focusRequesters,
            ) {
                stickyHeader { Spacer(Modifier.mainAxisSize(headerSize).fillMaxCrossAxis()) }
                stickyHeader { Spacer(Modifier.mainAxisSize(headerSize).fillMaxCrossAxis()) }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        rule.runOnIdle { focusRequesters[0].requestFocus() }

        rule.runOnIdle {
            val stickingHeaderSizePixels = with(rule.density) { headerSize.toPx() }.toInt()
            assertEquals(
                stickingHeaderSizePixels,
                state.layoutInfo.visibleItemsInfo.find { it.index == 2 }!!.offset.mainAxis,
            )
        }
    }

    @Test
    fun lazyGrid_withHeader_layoutOrientations_focusScrollsRevealsEntireItemUnderHeader() {
        lateinit var state: LazyGridState
        val headerSize = 5.dp
        val focusRequesters = List(10) { FocusRequester() }
        var layoutCombo by mutableStateOf(Pair(false, LayoutDirection.Ltr))

        rule.setContentWithTestViewConfiguration {
            key(layoutCombo) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutCombo.second) {
                    LazyGridWithFocussableItems(
                        viewportSize = 35.dp,
                        state = rememberLazyGridState().also { state = it },
                        itemSize = 10.dp,
                        reverseLayout = true,
                        focusRequesters = focusRequesters,
                    ) {
                        stickyHeader {
                            Spacer(Modifier.mainAxisSize(headerSize).fillMaxCrossAxis())
                        }
                    }
                }
            }
        }

        listOf(
                Pair(true, LayoutDirection.Ltr),
                Pair(true, LayoutDirection.Rtl),
                Pair(false, LayoutDirection.Ltr),
                Pair(false, LayoutDirection.Rtl),
            )
            .forEach { (reverseLayout, layoutDirection) ->
                rule.runOnIdle { layoutCombo = Pair(reverseLayout, layoutDirection) }

                rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
                rule.runOnIdle { focusRequesters[0].requestFocus() }

                rule.runOnIdle {
                    val headerSizePixels = with(rule.density) { headerSize.toPx() }.toInt()
                    assertEquals(
                        headerSizePixels,
                        state.layoutInfo.visibleItemsInfo.find { it.index == 1 }!!.offset.mainAxis,
                    )
                }
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

@Composable
private fun BaseLazyGridTestWithOrientation.LazyGridWithFocussableItems(
    state: LazyGridState,
    viewportSize: Dp,
    itemSize: Dp,
    reverseLayout: Boolean,
    focusRequesters: List<FocusRequester>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pre: LazyGridScope.() -> Unit,
) =
    LazyGrid(
        cells = GridCells.Fixed(1),
        modifier = Modifier.mainAxisSize(viewportSize),
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        state = state,
    ) {
        pre()
        items(focusRequesters.size) { index ->
            LocalPinnableContainer.current?.pin()?.let {
                DisposableEffect(Unit) { onDispose { it.release() } }
            }
            Spacer(
                Modifier.mainAxisSize(itemSize)
                    .fillMaxCrossAxis()
                    .focusRequester(focusRequesters[index])
                    .focusable()
            )
        }
    }
