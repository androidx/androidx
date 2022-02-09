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

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.list.scrollBy
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyGridTest {
    private val LazyGridTag = "LazyGridTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyGridShowsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3)
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
    fun lazyGridShowsOneRow() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.height(100.dp).width(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.height(101.dp).testTag(it))
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
    fun lazyGridShowsSecondRowOnScroll() {
        val items = (1..9).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.height(100.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.height(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 50.dp, density = rule.density)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("8")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("9")
            .assertIsNotDisplayed()
    }

    @Test
    fun lazyGridScrollHidesFirstRow() {
        val items = (1..9).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.height(200.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.height(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 103.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

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
    fun adaptiveLazyGridFillsAllWidth() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(130.dp),
                modifier = Modifier.height(100.dp).width(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.height(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(150.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAtLeastOneColumn() {
        val items = (1..3).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(301.dp),
                modifier = Modifier.height(100.dp).width(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.height(101.dp).testTag(it))
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
            LazyVerticalGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.height(itemSize).width(itemSize * 3 + spacing * 2),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(itemSize + spacing)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertWidthIsAtLeast(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesHorizontalSpacingsWithContentPaddings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 8.toDp() }
        val itemSize = with(rule.density) { 40.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.height(itemSize).width(itemSize * 3 + spacing * 4),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(horizontal = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(itemSize + spacing * 2)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 + spacing * 3)
            .assertWidthIsAtLeast(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacings() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 4.toDp() }
        val itemSize = with(rule.density) { 32.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.height(itemSize * 3 + spacing * 2).width(itemSize),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(itemSize + spacing)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(itemSize * 2 + spacing * 2)
            .assertHeightIsAtLeast(itemSize)
    }

    @Test
    fun adaptiveLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..3).map { it.toString() }

        val spacing = with(rule.density) { 16.toDp() }
        val itemSize = with(rule.density) { 72.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(itemSize),
                modifier = Modifier.height(itemSize * 3 + spacing * 2).width(itemSize),
                verticalArrangement = Arrangement.spacedBy(space = spacing),
                contentPadding = PaddingValues(vertical = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing * 3 + itemSize * 2)
            .assertHeightIsAtLeast(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 24.toDp() }
        val itemSize = with(rule.density) { 80.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.height(itemSize * 2 + spacing).width(itemSize),
                verticalArrangement = Arrangement.spacedBy(space = spacing),
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing + itemSize)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing + itemSize)
            .assertHeightIsAtLeast(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacings() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 15.toDp() }
        val itemSize = with(rule.density) { 30.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.height(itemSize * 2).width(itemSize * 2 + spacing),
                horizontalArrangement = Arrangement.spacedBy(space = spacing),
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing + itemSize)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing + itemSize)
            .assertWidthIsAtLeast(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesVerticalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 30.toDp() }
        val itemSize = with(rule.density) { 77.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.height(itemSize * 2 + spacing).width(itemSize),
                verticalArrangement = Arrangement.spacedBy(space = spacing),
                contentPadding = PaddingValues(vertical = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertHeightIsAtLeast(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertHeightIsAtLeast(itemSize)
    }

    @Test
    fun fixedLazyGridAppliesHorizontalSpacingsWithContentPadding() {
        val items = (1..4).map { it.toString() }

        val spacing = with(rule.density) { 22.toDp() }
        val itemSize = with(rule.density) { 44.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.height(itemSize * 2).width(itemSize * 2 + spacing * 3),
                horizontalArrangement = Arrangement.spacedBy(space = spacing),
                contentPadding = PaddingValues(horizontal = spacing)
            ) {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing)
            .assertWidthIsAtLeast(itemSize)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(spacing * 2 + itemSize)
            .assertWidthIsAtLeast(itemSize)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.requiredWidth(itemSize * 2)
            ) {
                items(items) {
                    Spacer(Modifier.requiredHeight(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.requiredWidth(itemSize * 2)
            ) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.requiredHeight(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3*4")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: LazyGridState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.fillMaxWidth().height(10.dp),
                state
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
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.requiredSize(itemSize * 2).testTag(LazyGridTag),
                state = LazyGridState(firstVisibleItemIndex = Int.MAX_VALUE - 3)
            ) {
                items(Int.MAX_VALUE) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE - 3}")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE - 2}")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("${Int.MAX_VALUE - 1}")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("${Int.MAX_VALUE}").assertDoesNotExist()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun pointerInputScrollingIsAllowedWhenUserScrollingIsEnabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = true,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = itemSize, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun pointerInputScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3).testTag(LazyGridTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = itemSize, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun programmaticScrollingIsAllowedWhenUserScrollingIsDisabled() {
        val itemSizePx = 30f
        val itemSize = with(rule.density) { itemSizePx.toDp() }
        lateinit var state: LazyGridState
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3),
                state = rememberLazyGridState().also { state = it },
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
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun semanticScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.size(itemSize * 3).testTag(LazyGridTag),
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
            .assert(keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
    }

    @Test
    fun rtl() {
        val gridWidth = 30
        val gridWidthDp = with(rule.density) { gridWidth.toDp() }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.width(gridWidthDp)) {
                    items(3) {
                        Box(Modifier.height(1.dp).testTag("$it"))
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertLeftPositionInRootIsEqualTo(gridWidthDp * 2 / 3)
        rule.onNodeWithTag("1").assertLeftPositionInRootIsEqualTo(gridWidthDp / 3)
        rule.onNodeWithTag("2").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun withMissingItems() {
        val itemHeight = with(rule.density) { 30.toDp() }
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                modifier = Modifier.height(itemHeight + 1.dp),
                state = state
            ) {
                items((0..8).map { it.toString() }) {
                    if (it != "3") {
                        Box(Modifier.size(itemHeight).testTag(it))
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
            LazyVerticalGrid(
                GridCells.Fixed(1),
                // this will return a new object everytime causing LazyVerticalGrid recomposition
                // without causing remeasure
                Modifier.composed { layoutModifier }
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
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.composed {
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
}
