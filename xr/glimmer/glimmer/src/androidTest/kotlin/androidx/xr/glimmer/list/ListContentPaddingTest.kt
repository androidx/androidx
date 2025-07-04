/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ListContentPaddingTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    private val ListTag = "List"
    private val ItemTag = "item"
    private val ContainerTag = "container"

    private var itemSize: Dp = Dp.Infinity
    private var smallPaddingSize: Dp = Dp.Infinity
    private var itemSizePx = 50f
    private var smallPaddingSizePx = 12f

    @Before
    fun before() {
        with(rule.density) {
            itemSize = itemSizePx.toDp()
            smallPaddingSize = smallPaddingSizePx.toDp()
        }
    }

    @Test
    fun contentPaddingIsApplied() {
        lateinit var state: ListState
        val containerSize = itemSize * 2
        val largePaddingSize = itemSize

        rule.setContentAndSaveScope {
            TestList(
                modifier = Modifier.requiredSize(containerSize).testTag(ListTag),
                state = rememberListState().also { state = it },
                contentPadding =
                    PaddingValues(mainAxis = largePaddingSize, crossAxis = smallPaddingSize),
                itemsCount = 1,
            ) {
                Spacer(Modifier.fillCrossAxisSize().mainAxisSize(itemSize).testTag(ItemTag))
            }
        }

        rule
            .onNodeWithTag(ItemTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertStartPositionInRootIsEqualTo(largePaddingSize)
            .assertCrossAxisSizeIsEqualTo(containerSize - smallPaddingSize * 2)
            .assertMainAxisSizeIsEqualTo(itemSize)

        rule.runOnIdle {
            scope.launch { state.scrollBy(with(rule.density) { largePaddingSize.toPx() }) }
        }

        rule
            .onNodeWithTag(ItemTag)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(itemSize)
    }

    @Test
    fun contentPaddingIsNotAffectingScrollPosition() {
        lateinit var state: ListState
        rule.setContentAndSaveScope {
            TestList(
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ListTag),
                state = rememberListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = itemSize),
                itemsCount = 1,
            ) {
                Spacer(Modifier.fillCrossAxisSize().mainAxisSize(itemSize).testTag(ItemTag))
            }
        }

        state.assertScrollPosition(0, 0.dp)

        rule.runOnIdle { scope.launch { state.scrollBy(with(rule.density) { itemSize.toPx() }) } }

        state.assertScrollPosition(0, itemSize)
    }

    @Test
    fun scrollForwardItemWithinStartPaddingDisplayed() {
        lateinit var state: ListState
        val padding = itemSize * 1.5f
        rule.setContentAndSaveScope {
            TestList(
                modifier = Modifier.requiredSize(padding * 2 + itemSize).testTag(ListTag),
                state = rememberListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding),
                itemsCount = 4,
            ) {
                Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
            }
        }

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(padding)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(itemSize + padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(itemSize * 2 + padding)

        rule.runOnIdle { scope.launch { state.scrollBy(with(rule.density) { padding.toPx() }) } }

        state.assertScrollPosition(1, padding - itemSize)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(itemSize * 2)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(itemSize * 3)
    }

    @Test
    fun scrollBackwardItemWithinStartPaddingDisplayed() {
        lateinit var state: ListState
        val padding = itemSize * 1.5f
        rule.setContentAndSaveScope {
            TestList(
                modifier = Modifier.requiredSize(itemSize + padding * 2).testTag(ListTag),
                state = rememberListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding),
                itemsCount = 4,
            ) {
                Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
            }
        }

        rule.runOnIdle {
            scope.launch { state.scrollBy(with(rule.density) { (itemSize * 3).toPx() }) }
            scope.launch { state.scrollBy(with(rule.density) { (-itemSize * 1.5f).toPx() }) }
        }

        state.assertScrollPosition(1, itemSize * 0.5f)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(itemSize * 1.5f - padding)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(itemSize * 2.5f - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(itemSize * 3.5f - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(itemSize * 4.5f - padding)
    }

    @Test
    fun scrollForwardTillTheEnd() {
        lateinit var state: ListState
        val padding = itemSize * 1.5f
        rule.setContentAndSaveScope {
            TestList(
                modifier = Modifier.requiredSize(padding * 2 + itemSize).testTag(ListTag),
                state = rememberListState().also { state = it },
                contentPadding = PaddingValues(mainAxis = padding),
                itemsCount = 4,
            ) {
                Spacer(Modifier.requiredSize(itemSize).testTag(it.toString()))
            }
        }

        rule.runOnIdle {
            scope.launch { state.scrollBy(with(rule.density) { (itemSize * 3).toPx() }) }
        }

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(itemSize * 3 - padding)

        // there are no space to scroll anymore, so it should change nothing
        rule.runOnIdle { scope.launch { state.scrollBy(with(rule.density) { 10.dp.toPx() }) } }

        state.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(itemSize - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(itemSize * 2 - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(itemSize * 3 - padding)
    }

    @Test
    fun unevenPaddingWithRtl() {
        val padding = PaddingValues(start = 20.dp, end = 8.dp)
        lateinit var state: ListState
        rule.setContentAndSaveScope {
            state = rememberListState()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TestList(
                    modifier = Modifier.testTag("list").mainAxisSize(itemSize * 2),
                    state = state,
                    contentPadding = padding,
                    itemsCount = 4,
                ) {
                    Box(Modifier.testTag("$it").background(Color.Red).size(itemSize)) {
                        BasicText("$it")
                    }
                }
            }
        }

        if (vertical) {
            rule
                .onNodeWithTag("0")
                .assertStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )

            rule.onNodeWithTag("list").assertWidthIsEqualTo(28.dp + itemSize)
        } else {
            rule
                .onNodeWithTag("0")
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
                .assertStartPositionInRootIsEqualTo(
                    // list width - itemSize - padding
                    itemSize * 2 - itemSize - padding.calculateRightPadding(LayoutDirection.Rtl)
                )
        }

        rule.runOnIdle {
            scope.launch { state.scrollBy(with(rule.density) { (itemSize * 4).toPx() }) }
        }

        if (vertical) {
            rule
                .onNodeWithTag("3")
                .assertStartPositionInRootIsEqualTo(itemSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )
        } else {
            rule
                .onNodeWithTag("3")
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
                .assertStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )
        }
    }

    private fun ListState.assertScrollPosition(index: Int, offset: Dp) =
        with(rule.density) {
            assertThat(firstVisibleItemIndex).isEqualTo(index)
            assertThat(firstVisibleItemScrollOffset.toDp().value).isWithin(0.5f).of(offset.value)
        }

    private fun ListState.assertLayoutInfoOffsetRange(from: Dp, to: Dp) =
        with(rule.density) {
            assertThat(layoutInfo.viewportStartOffset to layoutInfo.viewportEndOffset)
                .isEqualTo(from.roundToPx() to to.roundToPx())
        }

    private fun ListState.assertVisibleItems(vararg expected: Pair<Int, Dp>) =
        with(rule.density) {
            assertThat(layoutInfo.visibleItemsInfo.map { it.index to it.offset })
                .isEqualTo(expected.map { it.first to it.second.roundToPx() })
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
