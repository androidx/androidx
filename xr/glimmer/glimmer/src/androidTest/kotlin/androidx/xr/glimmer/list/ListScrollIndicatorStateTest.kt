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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.setGlimmerThemeContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ListScrollIndicatorStateTest(orientation: Orientation) :
    BaseListTestWithOrientation(orientation) {
    @Test
    fun noContentSet_allValuesIntMaxValue() {
        val state = ListState()
        rule.setGlimmerThemeContent {}
        assertThat(state.scrollIndicatorState.scrollOffset).isEqualTo(Int.MAX_VALUE)
        assertThat(state.scrollIndicatorState.contentSize).isEqualTo(Int.MAX_VALUE)
        assertThat(state.scrollIndicatorState.viewportSize).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun contentSetButNoItems_allValuesZero() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues.Zero,
                mainAxisSpacing = 5.dp,
                itemsCount = 0,
                itemSize = 100.dp,
            )
        }
        state.assertStateIs(viewportSize = 0.dp, contentSize = 0, scrollOffset = 0)
    }

    @Test
    fun shortList_viewportAndContentSizeEqual() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues.Zero,
                mainAxisSpacing = 5.dp,
                itemsCount = 2,
                itemSize = 100.dp,
            )
        }

        // 2 items of 100dp each, plus a spacing of 5dp
        rule.onNodeWithTag("list").assertMainAxisSizeIsEqualTo(205.dp)
        state.assertStateIs(
            viewportSize = 205.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 2,
                    spacingSize = 5.dp,
                    beforePadding = 0.dp,
                    afterPadding = 0.dp,
                ),
            scrollOffset = 0,
        )
    }

    @Test
    fun longList_contentSizeIsLargerThanViewport() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues.Zero,
                mainAxisSpacing = 0.dp,
                itemsCount = 50,
                itemSize = 100.dp,
            )
        }

        // Maxed out the viewport, there is still more content to show
        rule.onNodeWithTag("list").assertMainAxisSizeIsEqualTo(320.dp)
        state.assertStateIs(
            viewportSize = 320.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 50,
                    spacingSize = 0.dp,
                    beforePadding = 0.dp,
                    afterPadding = 0.dp,
                ),
            scrollOffset = 0,
        )
    }

    @Test
    fun spacing_accountedForBetweenItems() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues.Zero,
                mainAxisSpacing = 5.dp,
                itemsCount = 50,
                itemSize = 100.dp,
            )
        }

        // Maxed out the viewport, there is still more content to show
        rule.onNodeWithTag("list").assertMainAxisSizeIsEqualTo(320.dp)
        state.assertStateIs(
            viewportSize = 320.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 50,
                    spacingSize = 5.dp,
                    beforePadding = 0.dp,
                    afterPadding = 0.dp,
                ),
            scrollOffset = 0,
        )
    }

    @Test
    fun shortList_contentPadding_accountedForAtBeginningAndEnd() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues(beforeContent = 10.dp, afterContent = 20.dp),
                mainAxisSpacing = 5.dp,
                itemsCount = 2,
                itemSize = 100.dp,
            )
        }

        rule.onNodeWithTag("list").assertMainAxisSizeIsEqualTo(235.dp)
        state.assertStateIs(
            viewportSize = 235.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 2,
                    spacingSize = 5.dp,
                    beforePadding = 10.dp,
                    afterPadding = 20.dp,
                ),
            scrollOffset = 0,
        )
    }

    @Test
    fun longList_contentPadding_accountedForAtBeginningAndEnd() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues(beforeContent = 10.dp, afterContent = 20.dp),
                mainAxisSpacing = 5.dp,
                itemsCount = 50,
                itemSize = 100.dp,
            )
        }

        rule.onNodeWithTag("list").assertMainAxisSizeIsEqualTo(320.dp)
        state.assertStateIs(
            viewportSize = 320.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 50,
                    spacingSize = 5.dp,
                    beforePadding = 10.dp,
                    afterPadding = 20.dp,
                ),
            scrollOffset = 0,
        )
    }

    @Test
    fun scrollToMiddleOfList_updatesScrollOffset() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            ScrollIndicatorTestList(
                state = state,
                maxMainViewportSize = 320.dp,
                contentPadding = PaddingValues(beforeContent = 10.dp, afterContent = 20.dp),
                mainAxisSpacing = 5.dp,
                itemsCount = 50,
                itemSize = 100.dp,
            )
        }
        // Scroll to the start of the last item
        rule.runOnIdle { runBlocking { state.scrollToItem(25) } }

        state.assertStateIs(
            viewportSize = 320.dp,
            contentSize =
                calculateContentSize(
                    eachItemSize = 100.dp,
                    itemsCount = 50,
                    spacingSize = 5.dp,
                    beforePadding = 10.dp,
                    afterPadding = 20.dp,
                ),
            scrollOffset =
                calculateScrollOffset(eachItemSize = 100.dp, scrollOffset = 25, spacingSize = 5.dp),
        )
    }

    private fun ListState.assertStateIs(viewportSize: Dp, contentSize: Int, scrollOffset: Int) =
        with(rule.density) {
            scrollIndicatorState.viewportSize.toDp().assertIsEqualTo(viewportSize, "viewportSize")
            assertThat(scrollIndicatorState.contentSize).isEqualTo(contentSize)
            assertThat(scrollIndicatorState.scrollOffset).isEqualTo(scrollOffset)
        }

    /**
     * Calculates content size in px, with the same rounding errors that are inevitable in the
     * actual Glimmer List.
     *
     * These inaccuracies are because each item and each spacing needs to get rounded to an integer
     * pixel number, and these rounding errors add up across many items in the content.
     *
     * For example, if we have 50 items at 100dp, and 100dp -> 262.5px, but the layout measures
     * these items at 263px, we will end up with 0.5*50=25px of inaccuracy unless we do the same
     * per-item rounding with our expected sizes.
     */
    private fun calculateContentSize(
        eachItemSize: Dp,
        itemsCount: Int,
        spacingSize: Dp,
        beforePadding: Dp,
        afterPadding: Dp,
    ): Int =
        with(rule.density) {
            val itemSizes = eachItemSize.roundToPx() * itemsCount
            val spacingSizes = spacingSize.roundToPx() * (itemsCount - 1)
            itemSizes + spacingSizes + beforePadding.roundToPx() + afterPadding.roundToPx()
        }

    private fun calculateScrollOffset(eachItemSize: Dp, scrollOffset: Int, spacingSize: Dp): Int =
        with(rule.density) {
            val itemSizes = eachItemSize.roundToPx() * scrollOffset
            val spacingSizes = spacingSize.roundToPx() * scrollOffset
            itemSizes + spacingSizes
        }

    /**
     * Creates a list for scroll indicator testing in a [Box] with main-axis size
     * [maxMainViewportSize].
     *
     * Each item is [itemSize] along the main axis and there are [itemsCount] items, with
     * [mainAxisSpacing] spacing between each item.
     */
    @Composable
    private fun ScrollIndicatorTestList(
        state: ListState,
        maxMainViewportSize: Dp,
        contentPadding: PaddingValues,
        mainAxisSpacing: Dp,
        itemsCount: Int,
        itemSize: Dp,
    ) {
        Box(Modifier.mainAxisSize(maxMainViewportSize)) {
            TestList(
                modifier = Modifier.testTag("list"),
                state = state,
                contentPadding = contentPadding,
                horizontalArrangement =
                    if (vertical) Arrangement.Center else Arrangement.spacedBy(mainAxisSpacing),
                verticalArrangement =
                    if (vertical) Arrangement.spacedBy(mainAxisSpacing) else Arrangement.Center,
                itemsCount = itemsCount,
            ) { index ->
                Text(modifier = Modifier.mainAxisSize(itemSize), text = "Item $index")
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        internal fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
