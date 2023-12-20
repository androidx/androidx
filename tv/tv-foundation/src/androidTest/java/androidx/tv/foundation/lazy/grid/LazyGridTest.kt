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

package androidx.tv.foundation.lazy.grid

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.foundation.lazy.AutoTestFrameClock
import androidx.tv.foundation.lazy.list.setContentWithTestViewConfiguration
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyGridTest(
    private val orientation: Orientation
) : BaseLazyGridTestWithOrientation(orientation) {
    private val LazyGridTag = "LazyGridTag"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    @Test
    fun lazyGridShowsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyGrid(
                cells = 3
            ) {
                item {
                    Spacer(
                        Modifier.size(10.dp).testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyGridShowsOneLine() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = 3,
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridShowsSecondLineOnScroll() {
        val items = (1..12).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 3,
                modifier = Modifier.mainAxisSize(200.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Box(Modifier.mainAxisSize(101.dp).testTag(it).focusable())
                }
            }
        }

        rule.keyPress(3)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("10")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("11")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("12")
            .assertIsNotDisplayed()
    }

    @Test
    fun lazyGridScrollHidesFirstLine() {
        val items = (1..9).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 3,
                modifier = Modifier.mainAxisSize(200.dp).testTag(LazyGridTag),
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it).focusable())
                }
            }
        }

        rule.keyPress(3)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("3")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsDisplayed()

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun adaptiveLazyGridFillsAllCrossAxisSize() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(130.dp),
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertCrossAxisStartPositionInRootIsEqualTo(150.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAtLeastOneSlot() {
        val items = (1..3).map { it.toString() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(301.dp),
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAppliesHorizontalSpacings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 10.toDp() }
        val itemSize = with(rule.density) { 100.toDp() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize * 3 + spacing * 2, itemSize),
                crossAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize + spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesHorizontalSpacingsWithContentPaddings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 8.toDp() }
        val itemSize = with(rule.density) { 40.toDp() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize * 3 + spacing * 4, itemSize),
                crossAxisSpacedBy = spacing,
                contentPadding = PaddingValues(crossAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize + spacing * 2)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 3)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 4.toDp() }
        val itemSize = with(rule.density) { 32.toDp() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize, itemSize * 3 + spacing * 2),
                mainAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize + spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 16.toDp() }
        val itemSize = with(rule.density) { 72.toDp() }

        rule.setContent {
            LazyGrid(
                cells = TvGridCells.Adaptive(itemSize),
                modifier = Modifier.axisSize(itemSize, itemSize * 3 + spacing * 2),
                mainAxisSpacedBy = spacing,
                contentPadding = PaddingValues(mainAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 3 + itemSize * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 24.toDp() }
        val itemSize = with(rule.density) { 80.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize, itemSize * 2 + spacing),
                mainAxisSpacedBy = spacing,
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 15.toDp() }
        val itemSize = with(rule.density) { 30.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize * 2 + spacing, itemSize * 2),
                crossAxisSpacedBy = spacing
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 30.toDp() }
        val itemSize = with(rule.density) { 77.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize, itemSize * 2 + spacing),
                mainAxisSpacedBy = spacing,
                contentPadding = PaddingValues(mainAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertMainAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 22.toDp() }
        val itemSize = with(rule.density) { 44.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.axisSize(itemSize * 2 + spacing * 3, itemSize * 2),
                crossAxisSpacedBy = spacing,
                contentPadding = PaddingValues(crossAxis = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing)
            .assertCrossAxisSizeIsEqualTo(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertCrossAxisStartPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertCrossAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.crossAxisSize(itemSize * 2)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("4")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                Modifier.crossAxisSize(itemSize * 2)
            ) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.mainAxisSize(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3*4")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: TvLazyGridState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberTvLazyGridState()
            LazyGrid(
                cells = 1,
                modifier = Modifier.mainAxisSize(10.dp),
                state = state
            ) {
                items(count) { index ->
                    composedIndexes.add(index)
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            composedIndexes.clear()
            count = 10
            runBlocking(AutoTestFrameClock()) {
                // we try to scroll to the index after 10, but we expect that the component will
                // already be aware there is a new count and not compose items with index > 10
                state.scrollToItem(50)
            }
            composedIndexes.forEach {
                Truth.assertThat(it).isLessThan(count)
            }
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(9)
        }
    }

    @Test
    fun maxIntElements() {
        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyGrid(
                cells = 2,
                modifier = Modifier.size(itemSize * 2).testTag(LazyGridTag),
                state = TvLazyGridState(firstVisibleItemIndex = Int.MAX_VALUE - 3)
            ) {
                items(Int.MAX_VALUE) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE - 3}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE - 2}")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("${Int.MAX_VALUE - 1}")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE}").assertDoesNotExist()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun pointerInputScrollingIsAllowedWhenUserScrollingIsEnabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = true
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it").focusable())
                }
            }
        }

        rule.keyPress(3)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun pointerInputScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = false
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it").focusable())
                }
            }
        }

        rule.keyPress(2)

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun programmaticScrollingIsAllowedWhenUserScrollingIsDisabled() {
        val itemSizePx = 30f
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        lateinit var state: TvLazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3),
                state = rememberTvLazyGridState().also { state = it },
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx)
            }
        }

        rule.onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun semanticScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollBy))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollToIndex))
            // but we still have a read only scroll range property
            .assert(
                keyIsDefined(
                    if (orientation == Orientation.Vertical) {
                        SemanticsProperties.VerticalScrollAxisRange
                    } else {
                        SemanticsProperties.HorizontalScrollAxisRange
                    }
                )
            )
    }

    @Test
    fun rtl() {
        val gridCrossAxisSize = 30
        val gridCrossAxisSizeDp = with(rule.density) { gridCrossAxisSize.toDp() }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyGrid(
                    cells = 3,
                    modifier = Modifier.crossAxisSize(gridCrossAxisSizeDp)
                ) {
                    items(3) {
                        Box(Modifier.mainAxisSize(1.dp).testTag("$it"))
                    }
                }
            }
        }

        val tags = if (orientation == Orientation.Vertical) {
            listOf("0", "1", "2")
        } else {
            listOf("2", "1", "0")
        }
        rule.onNodeWithTag(tags[0])
            .assertCrossAxisStartPositionInRootIsEqualTo(gridCrossAxisSizeDp * 2 / 3)
        rule.onNodeWithTag(tags[1])
            .assertCrossAxisStartPositionInRootIsEqualTo(gridCrossAxisSizeDp / 3)
        rule.onNodeWithTag(tags[2]).assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun withMissingItems() {
        val itemMainAxisSize = with(rule.density) { 30.toDp() }
        lateinit var state: TvLazyGridState
        rule.setContent {
            state = rememberTvLazyGridState()
            LazyGrid(
                cells = 2,
                modifier = Modifier.mainAxisSize(itemMainAxisSize + 1.dp),
                state = state
            ) {
                items((0..8).map { it.toString() }) {
                    if (it != "3") {
                        Box(Modifier.mainAxisSize(itemMainAxisSize).testTag(it))
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3)
            }
        }

        rule.onNodeWithTag("0").assertIsNotDisplayed()
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertDoesNotExist()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun passingNegativeItemsCountIsNotAllowed() {
        var exception: Exception? = null
        rule.setContentWithTestViewConfiguration {
            LazyGrid(cells = 1) {
                try {
                    items(-1) {
                        Box(Modifier)
                    }
                } catch (e: Exception) {
                    exception = e
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun recomposingWithNewComposedModifierObjectIsNotCausingRemeasure() {
        var remeasureCount = 0
        val layoutModifier = Modifier.layout { measurable, constraints ->
            remeasureCount++
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        val counter = mutableStateOf(0)

        rule.setContentWithTestViewConfiguration {
            counter.value // just to trigger recomposition
            LazyGrid(
                cells = 1,
                // this will return a new object everytime causing LazyGrid recomposition
                // without causing remeasure
                modifier = Modifier.composed { layoutModifier }
            ) {
                items(1) {
                    Spacer(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(remeasureCount).isEqualTo(1)
            counter.value++
        }

        rule.runOnIdle {
            Truth.assertThat(remeasureCount).isEqualTo(1)
        }
    }

    @Test
    fun scrollingALotDoesntCauseLazyLayoutRecomposition() {
        var recomposeCount = 0
        lateinit var state: TvLazyGridState

        rule.setContentWithTestViewConfiguration {
            state = rememberTvLazyGridState()
            LazyGrid(
                cells = 1,
                modifier = Modifier.composed {
                    recomposeCount++
                    Modifier
                }.size(100.dp),
                state
            ) {
                items(1000) {
                    Spacer(Modifier.size(100.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(recomposeCount).isEqualTo(1)

            runBlocking {
                state.scrollToItem(100)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(recomposeCount).isEqualTo(1)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun zIndexOnItemAffectsDrawingOrder() {
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                cells = 1,
                modifier = Modifier.size(6.dp).testTag(LazyGridTag)
            ) {
                items(listOf(Color.Blue, Color.Green, Color.Red)) { color ->
                    Box(
                        Modifier
                            .axisSize(6.dp, 2.dp)
                            .zIndex(if (color == Color.Green) 1f else 0f)
                            .drawBehind {
                                drawRect(
                                    color,
                                    topLeft = Offset(-10.dp.toPx(), -10.dp.toPx()),
                                    size = Size(20.dp.toPx(), 20.dp.toPx())
                                )
                            })
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .captureToImage()
            .assertPixels { Color.Green }
    }

    @Test
    fun customGridCells() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyGrid(
                // Two columns in ratio 1:2
                cells = object : TvGridCells {
                    override fun Density.calculateCrossAxisCellSizes(
                        availableSize: Int,
                        spacing: Int
                    ): List<Int> {
                        val availableCrossAxis = availableSize - spacing
                        val columnSize = availableCrossAxis / 3
                        return listOf(columnSize, columnSize * 2)
                    }
                },
                modifier = Modifier.axisSize(300.dp, 100.dp)
            ) {
                items(items) {
                    Spacer(Modifier.mainAxisSize(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(100.dp)

        rule.onNodeWithTag("2")
            .assertCrossAxisStartPositionInRootIsEqualTo(100.dp)
            .assertCrossAxisSizeIsEqualTo(200.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun onlyOneInitialMeasurePass() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: TvLazyGridState
        rule.setContent {
            state = rememberTvLazyGridState()
            LazyGrid(
                1,
                Modifier.requiredSize(100.dp).testTag(LazyGridTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.numMeasurePasses).isEqualTo(1)
        }
    }

    @Test
    fun fillingFullSize_nextItemIsNotComposed() {
        val state = TvLazyGridState()
        state.prefetchingEnabled = false
        val itemSizePx = 5f
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyGrid(
                1,
                Modifier
                    .testTag(LazyGridTag)
                    .mainAxisSize(itemSize),
                state
            ) {
                items(3) { index ->
                    Box(Modifier.size(itemSize).testTag("$index"))
                }
            }
        }

        repeat(3) { index ->
            rule.onNodeWithTag("$index")
                .assertIsDisplayed()
            rule.onNodeWithTag("${index + 1}")
                .assertDoesNotExist()
            rule.runOnIdle {
                runBlocking {
                    state.scrollBy(itemSizePx)
                }
            }
        }
    }
}

internal fun IntegerSubject.isEqualTo(expected: Int, tolerance: Int) {
    isIn(Range.closed(expected - tolerance, expected + tolerance))
}

internal fun ComposeContentTestRule.keyPress(keyCode: Int, numberOfPresses: Int = 1) {
    repeat(numberOfPresses) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        waitForIdle()
    }
}
