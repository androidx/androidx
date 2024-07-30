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

package androidx.compose.foundation.lazy.list

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListAnimateItemPlacementTest(private val config: Config) {

    private val isVertical: Boolean
        get() = config.isVertical

    private val reverseLayout: Boolean
        get() = config.reverseLayout

    private val isInLookaheadScope: Boolean
        get() = config.isInLookaheadScope

    @get:Rule val rule = createComposeRule()

    // the numbers should be divisible by 8 to avoid the rounding issues as we run 4 or 8 frames
    // of the animation.
    private val itemSize: Float = 40f
    private var itemSizeDp: Dp = Dp.Infinity
    private val itemSize2: Float = 24f
    private var itemSize2Dp: Dp = Dp.Infinity
    private val itemSize3: Float = 16f
    private var itemSize3Dp: Dp = Dp.Infinity
    private val containerSize: Float = itemSize * 5
    private var containerSizeDp: Dp = Dp.Infinity
    private val spacing: Float = 8f
    private var spacingDp: Dp = Dp.Infinity
    private val itemSizePlusSpacing = itemSize + spacing
    private var itemSizePlusSpacingDp = Dp.Infinity
    private lateinit var state: LazyListState

    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        with(rule.density) {
            itemSizeDp = itemSize.toDp()
            itemSize2Dp = itemSize2.toDp()
            itemSize3Dp = itemSize3.toDp()
            containerSizeDp = containerSize.toDp()
            spacingDp = spacing.toDp()
            itemSizePlusSpacingDp = itemSizePlusSpacing.toDp()
        }
    }

    @Test
    fun reorderTwoItems() {
        var list by mutableStateOf(listOf(0, 1))
        rule.setContent { LazyList { items(list, key = { it }) { Item(it) } } }

        assertPositions(0 to 0f, 1 to itemSize)

        rule.runOnUiThread { list = listOf(1, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSize * fraction,
                1 to itemSize - itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderTwoItems_layoutInfoHasFinalPositions() {
        var list by mutableStateOf(listOf(0, 1))
        rule.setContent { LazyList { items(list, key = { it }) { Item(it) } } }

        assertLayoutInfoPositions(0 to 0f, 1 to itemSize)

        rule.runOnUiThread { list = listOf(1, 0) }

        onAnimationFrame {
            // fraction doesn't affect the offsets in layout info
            assertLayoutInfoPositions(1 to 0f, 0 to itemSize)
        }
    }

    @Test
    fun reorderFirstAndLastItems() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent { LazyList { items(list, key = { it }) { Item(it) } } }

        assertPositions(
            0 to 0f,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
            4 to itemSize * 4,
        )

        rule.runOnUiThread { list = listOf(4, 1, 2, 3, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSize * 4 * fraction,
                1 to itemSize,
                2 to itemSize * 2,
                3 to itemSize * 3,
                4 to itemSize * 4 - itemSize * 4 * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent { LazyList { items(list, key = { it }) { Item(it) } } }

        assertPositions(
            0 to 0f,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
            4 to itemSize * 4,
        )

        rule.runOnUiThread { list = listOf(1, 2, 3, 4, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSize * 4 * fraction,
                1 to itemSize - itemSize * fraction,
                2 to itemSize * 2 - itemSize * fraction,
                3 to itemSize * 3 - itemSize * fraction,
                4 to itemSize * 4 - itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun itemSizeChangeAnimatesNextItems() {
        var size by mutableStateOf(itemSizeDp)
        rule.setContent {
            LazyList(minSize = itemSizeDp * 5, maxSize = itemSizeDp * 5) {
                items(listOf(0, 1, 2, 3), key = { it }) {
                    Item(it, size = if (it == 1) size else itemSizeDp)
                }
            }
        }

        rule.runOnUiThread { size = itemSizeDp * 2 }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("1").assertMainAxisSizeIsEqualTo(size)

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0f,
                1 to itemSize,
                2 to itemSize * 2 + itemSize * fraction,
                3 to itemSize * 3 + itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun onlyItemsWithModifierAnimates() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it, animSpec = if (it == 1 || it == 3) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread { list = listOf(1, 2, 3, 4, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to itemSize * 4,
                1 to itemSize - itemSize * fraction,
                2 to itemSize,
                3 to itemSize * 3 - itemSize * fraction,
                4 to itemSize * 3,
                fraction = fraction
            )
        }
    }

    @Test
    fun animationsWithDifferentDurations() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    val duration = if (it == 1 || it == 3) Duration * 2 else Duration
                    Item(it, animSpec = tween(duration.toInt(), easing = LinearEasing))
                }
            }
        }

        rule.runOnUiThread { list = listOf(1, 2, 3, 4, 0) }

        onAnimationFrame(duration = Duration * 2) { fraction ->
            val shorterAnimFraction = (fraction * 2).coerceAtMost(1f)
            assertPositions(
                0 to 0 + itemSize * 4 * shorterAnimFraction,
                1 to itemSize - itemSize * fraction,
                2 to itemSize * 2 - itemSize * shorterAnimFraction,
                3 to itemSize * 3 - itemSize * fraction,
                4 to itemSize * 4 - itemSize * shorterAnimFraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItem() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                    Item(it + 1)
                }
            }
        }

        assertPositions(
            0 to 0f,
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
        )

        rule.runOnUiThread { list = listOf(2, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSize * 2 * fraction,
                1 to itemSize + itemSize * 2 * fraction,
                2 to itemSize * 2 - itemSize * 2 * fraction,
                3 to itemSize * 3 - itemSize * 2 * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun multipleChildrenPerItemSomeDoNotAnimate() {
        var list by mutableStateOf(listOf(0, 2))
        rule.setContent {
            LazyList {
                items(list, key = { it }) {
                    Item(it)
                    Item(it + 1, animSpec = null)
                }
            }
        }

        rule.runOnUiThread { list = listOf(2, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSize * 2 * fraction,
                1 to itemSize * 3,
                2 to itemSize * 2 - itemSize * 2 * fraction,
                3 to itemSize,
                fraction = fraction
            )
        }
    }

    @Test
    fun animateArrangementChange() {
        var arrangement by mutableStateOf(Arrangement.Center)
        rule.setContent {
            LazyList(
                arrangement = arrangement,
                minSize = itemSizeDp * 5,
                maxSize = itemSizeDp * 5
            ) {
                items(listOf(1, 2, 3), key = { it }) { Item(it) }
            }
        }

        assertPositions(
            1 to itemSize,
            2 to itemSize * 2,
            3 to itemSize * 3,
        )

        rule.runOnUiThread { arrangement = Arrangement.SpaceBetween }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to itemSize - itemSize * fraction,
                2 to itemSize * 2,
                3 to itemSize * 3 + itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize * 3
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        assertPositions(0 to 0f, 1 to itemSize, 2 to itemSize * 2)

        rule.runOnUiThread { list = listOf(0, 4, 2, 3, 1, 5) }

        onAnimationFrame { fraction ->
            // item 1 moves to and item 4 moves from `listSize`, right after the end edge
            val item1Offset = itemSize + (listSize - itemSize) * fraction
            val item4Offset = listSize - (listSize - itemSize) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < itemSize * 3) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(2 to itemSize * 2)
                    if (item4Offset < itemSize * 3) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize, 5 to itemSize * 2)

        rule.runOnUiThread { list = listOf(2, 4, 0, 3, 1, 5) }

        onAnimationFrame { fraction ->
            // item 1 moves from and item 4 moves to `0 - itemSize`, right before the start edge
            val item1Offset = -itemSize + itemSize * 2 * fraction
            val item4Offset = itemSize - itemSize * 2 * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item4Offset > -itemSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item1Offset > -itemSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(5 to itemSize * 2)
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun moveItemToTheTopOutsideOfBounds_withStickyHeader() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 2f, startIndex = 4) {
                // the existence of this header shouldn't affect the animation aside from
                // the fact that we need to adjust startIndex because of it`s existence.
                stickyHeader {}
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize)

        rule.runOnUiThread { list = listOf(2, 4, 0, 3, 1) }

        onAnimationFrame { fraction ->
            // item 1 moves from and item 4 moves to `0 - itemSize`, right before the start edge
            val item1Offset = -itemSize + itemSize * 2 * fraction
            val item4Offset = itemSize - itemSize * 2 * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item4Offset > -itemSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item1Offset > -itemSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveFirstItemToEndCausingAllItemsToAnimate_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        rule.setContent {
            LazyList(arrangement = Arrangement.spacedBy(spacingDp)) {
                items(list, key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(1, 2, 3, 0) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to 0 + itemSizePlusSpacing * 3 * fraction,
                1 to itemSizePlusSpacing - itemSizePlusSpacing * fraction,
                2 to itemSizePlusSpacing * 2 - itemSizePlusSpacing * fraction,
                3 to itemSizePlusSpacing * 3 - itemSizePlusSpacing * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToTheTop_itemWithMoreChildren_outsideBounds_shouldNotCrash() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize * 3
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp, startIndex = 3) {
                items(list, key = { it }) {
                    Item(it)
                    if (it != list.last()) {
                        Box(modifier = Modifier)
                    }
                }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize, 5 to itemSize * 2)

        // move item 5 out of bounds
        rule.runOnUiThread { list = listOf(5, 0, 1, 2, 3, 4) }

        // should not crash
        onAnimationFrame { fraction ->
            if (fraction == 1.0f) {
                assertPositions(2 to 0f, 3 to itemSize, 4 to itemSize * 2)
            }
        }
    }

    @Test
    fun movingAwayItem_itemWithMoreChildren_crossAxisAlignmentDefined_shouldNotCrash() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize * 3
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(
                maxSize = listSizeDp,
                startIndex = 3,
                crossAxisAlignment = CrossAxisAlignment.Center
            ) {
                items(list, key = { it }) {
                    Item(it)
                    if (it != list.last()) {
                        Box(modifier = Modifier)
                    }
                }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize, 5 to itemSize * 2)

        // move item 5 out of bounds
        rule.runOnUiThread { list = listOf(5, 0, 1, 2, 3, 4) }

        // should not crash
        onAnimationFrame { fraction ->
            if (fraction == 1.0f) {
                assertPositions(2 to 0f, 3 to itemSize, 4 to itemSize * 2)
            }
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize * 3 + spacing * 2
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp, arrangement = Arrangement.spacedBy(spacingDp)) {
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(0 to 0f, 1 to itemSizePlusSpacing, 2 to itemSizePlusSpacing * 2)

        rule.runOnUiThread { list = listOf(0, 4, 2, 3, 1, 5) }

        val afterLastVisibleItem = itemSize * 3 + spacing * 3

        onAnimationFrame { fraction ->
            // item 1 moves to and item 4 moves from `listSize`, right after the end edge
            val item1Offset =
                if (isInLookaheadScope) {
                    itemSizePlusSpacing + 2 * itemSizePlusSpacing * fraction
                } else itemSizePlusSpacing + (afterLastVisibleItem - itemSizePlusSpacing) * fraction
            val item4Offset =
                afterLastVisibleItem - (afterLastVisibleItem - itemSizePlusSpacing) * fraction

            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < afterLastVisibleItem) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(2 to itemSizePlusSpacing * 2)
                    if (item4Offset < afterLastVisibleItem) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds_withSpacing() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        rule.setContent {
            LazyList(
                maxSize = itemSizeDp * 3 + spacingDp * 2,
                startIndex = 3,
                arrangement = Arrangement.spacedBy(spacingDp)
            ) {
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(3 to 0f, 4 to itemSizePlusSpacing, 5 to itemSizePlusSpacing * 2)

        rule.runOnUiThread { list = listOf(2, 4, 0, 3, 1, 5, 6, 7) }

        onAnimationFrame { fraction ->
            // item 4 moves to and item 1 moves from `-itemSize`, right before the start edge
            val item1Offset = -itemSizePlusSpacing + (2 * itemSizePlusSpacing) * fraction
            val item4Offset = itemSizePlusSpacing - (2 * itemSizePlusSpacing) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item4Offset > -itemSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item1Offset > -itemSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(5 to itemSizePlusSpacing * 2)
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveItemToTheTopOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSize2Dp + itemSize3Dp + itemSizeDp, startIndex = 3) {
                items(list, key = { it }) {
                    val size =
                        if (it == 3) itemSize2Dp else if (it == 1) itemSize3Dp else itemSizeDp
                    Item(it, size = size)
                }
            }
        }

        val item3Size = itemSize2
        val item4Size = itemSize
        assertPositions(3 to 0f, 4 to item3Size, 5 to item3Size + item4Size)

        rule.runOnUiThread {
            // swap 4 and 1
            list = listOf(0, 4, 2, 3, 1, 5)
        }

        onAnimationFrame { fraction ->
            // item 2 was between 1 and 3 but we don't compose it
            rule.onNodeWithTag("2").assertDoesNotExist()
            val item1Size = itemSize3 /* the real size of the item 1 */
            // item 1 moves from and item 4 moves to `0 - item size`, right before the start edge
            val startItem1Offset = -item1Size
            val item1Offset = startItem1Offset + (itemSize2 - startItem1Offset) * fraction
            val endItem4Offset = -item4Size
            val item4Offset = item3Size - (item3Size - endItem4Offset) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item4Offset > -item4Size) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item1Offset > -item1Size) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(5 to item3Size + item4Size - (item4Size - item1Size) * fraction)
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveItemToTheBottomOutsideOfBounds_differentSizes() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        val listSize = itemSize2 + itemSize3 + itemSize
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) {
                items(list, key = { it }) {
                    val size =
                        if (it == 0) itemSize2Dp else if (it == 4) itemSize3Dp else itemSizeDp
                    Item(it, size = size)
                }
            }
        }

        val item0Size = itemSize2
        val item1Size = itemSize
        assertPositions(0 to 0f, 1 to item0Size, 2 to item0Size + item1Size)

        rule.runOnUiThread { list = listOf(0, 4, 2, 3, 1, 5) }
        val afterLastVisibleItem = itemSize2 + itemSize3 + itemSize
        onAnimationFrame { fraction ->
            val startItem4Offset = afterLastVisibleItem
            val endItem1Offset = afterLastVisibleItem

            val item4Size = itemSize3
            val item1Offset =
                if (isInLookaheadScope) {
                    item0Size + (item4Size + itemSize) * fraction
                } else {
                    item0Size + (endItem1Offset - item0Size) * fraction
                }
            val item4Offset = startItem4Offset - (startItem4Offset - item0Size) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < afterLastVisibleItem) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    add(2 to item0Size + item1Size - (item1Size - item4Size) * fraction)
                    if (item4Offset < listSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun animateAlignmentChange() {
        var alignment by mutableStateOf(CrossAxisAlignment.End)
        rule.setContent {
            LazyList(crossAxisAlignment = alignment, crossAxisSize = itemSizeDp) {
                items(listOf(1, 2, 3), key = { it }) {
                    val crossAxisSize =
                        if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                    Item(it, crossAxisSize = crossAxisSize)
                }
            }
        }

        val item2Start = itemSize - itemSize2
        val item3Start = itemSize - itemSize3
        assertPositions(
            1 to 0f,
            2 to itemSize,
            3 to itemSize * 2,
            crossAxis =
                listOf(
                    1 to 0f,
                    2 to item2Start,
                    3 to item3Start,
                )
        )

        rule.runOnUiThread { alignment = CrossAxisAlignment.Center }
        rule.mainClock.advanceTimeByFrame()

        val item2End = itemSize / 2 - itemSize2 / 2
        val item3End = itemSize / 2 - itemSize3 / 2
        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0f,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis =
                    listOf(
                        1 to 0f,
                        2 to item2Start + (item2End - item2Start) * fraction,
                        3 to item3Start + (item3End - item3Start) * fraction,
                    ),
                fraction = fraction
            )
        }
    }

    @Test
    fun animateAlignmentChange_multipleChildrenPerItem() {
        var alignment by mutableStateOf(CrossAxisAlignment.Start)
        rule.setContent {
            LazyList(crossAxisAlignment = alignment, crossAxisSize = itemSizeDp * 2) {
                items(1) {
                    listOf(1, 2, 3).forEach {
                        val crossAxisSize =
                            if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                        Item(it, crossAxisSize = crossAxisSize)
                    }
                }
            }
        }

        rule.runOnUiThread { alignment = CrossAxisAlignment.End }
        rule.mainClock.advanceTimeByFrame()

        val containerSize = itemSize * 2
        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0f,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis =
                    listOf(
                        1 to (containerSize - itemSize) * fraction,
                        2 to (containerSize - itemSize2) * fraction,
                        3 to (containerSize - itemSize3) * fraction
                    ),
                fraction = fraction
            )
        }
    }

    @Test
    fun animateAlignmentChange_rtl() {
        // this test is not applicable to LazyRow
        assumeTrue(isVertical)

        var alignment by mutableStateOf(CrossAxisAlignment.End)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(crossAxisAlignment = alignment, crossAxisSize = itemSizeDp) {
                    items(listOf(1, 2, 3), key = { it }) {
                        val crossAxisSize =
                            if (it == 1) itemSizeDp else if (it == 2) itemSize2Dp else itemSize3Dp
                        Item(it, crossAxisSize = crossAxisSize)
                    }
                }
            }
        }

        assertPositions(
            1 to 0f,
            2 to itemSize,
            3 to itemSize * 2,
            crossAxis =
                listOf(
                    1 to 0f,
                    2 to 0f,
                    3 to 0f,
                )
        )

        rule.runOnUiThread { alignment = CrossAxisAlignment.Center }
        rule.mainClock.advanceTimeByFrame()

        onAnimationFrame { fraction ->
            assertPositions(
                1 to 0f,
                2 to itemSize,
                3 to itemSize * 2,
                crossAxis =
                    listOf(
                        1 to 0f,
                        2 to (itemSize / 2 - itemSize2 / 2) * fraction,
                        3 to (itemSize / 2 - itemSize3 / 2) * fraction
                    ),
                fraction = fraction
            )
        }
    }

    @Test
    fun moveItemToEndCausingNextItemsToAnimate_withContentPadding() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val rawStartPadding = 8f
        val rawEndPadding = 12f
        val (startPaddingDp, endPaddingDp) =
            with(rule.density) { rawStartPadding.toDp() to rawEndPadding.toDp() }
        rule.setContent {
            LazyList(startPadding = startPaddingDp, endPadding = endPaddingDp) {
                items(list, key = { it }) { Item(it) }
            }
        }

        val startPadding = if (reverseLayout) rawEndPadding else rawStartPadding
        assertPositions(
            0 to startPadding,
            1 to startPadding + itemSize,
            2 to startPadding + itemSize * 2,
            3 to startPadding + itemSize * 3,
            4 to startPadding + itemSize * 4,
        )

        rule.runOnUiThread { list = listOf(0, 2, 3, 4, 1) }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to startPadding,
                1 to startPadding + itemSize + itemSize * 3 * fraction,
                2 to startPadding + itemSize * 2 - itemSize * fraction,
                3 to startPadding + itemSize * 3 - itemSize * fraction,
                4 to startPadding + itemSize * 4 - itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun removingItemsCauseOutOfBoundsItemToPopUp_withContentPadding() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val rawStartPadding = 8f
        val rawEndPadding = 12f
        val (startPaddingDp, endPaddingDp) =
            with(rule.density) { rawStartPadding.toDp() to rawEndPadding.toDp() }
        rule.setContent {
            // only 4 items will be visible 0, 1, 2, 3
            LazyList(
                maxSize = itemSizeDp * 4,
                startPadding = startPaddingDp,
                endPadding = endPaddingDp
            ) {
                items(list, key = { it }) { Item(it) }
            }
        }

        val startPadding = if (reverseLayout) rawEndPadding else rawStartPadding
        assertPositions(
            0 to startPadding,
            1 to startPadding + itemSize,
            2 to startPadding + itemSize * 2,
            3 to startPadding + itemSize * 3
        )

        rule.runOnUiThread { list = listOf(1, 2, 3, 4) }

        onAnimationFrame { fraction ->
            assertPositions(
                1 to startPadding + itemSize - itemSize * fraction,
                2 to startPadding + itemSize * 2 - itemSize * fraction,
                3 to startPadding + itemSize * 3 - itemSize * fraction,
                4 to startPadding + itemSize * 4 - itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun reorderFirstAndLastItems_noNewLayoutInfoProduced() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))

        var measurePasses = 0
        rule.setContent {
            LazyList { items(list, key = { it }) { Item(it) } }
            LaunchedEffect(Unit) { snapshotFlow { state.layoutInfo }.collect { measurePasses++ } }
        }

        rule.runOnUiThread { list = listOf(4, 1, 2, 3, 0) }

        var startMeasurePasses = Int.MIN_VALUE
        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                startMeasurePasses = measurePasses
            }
        }
        rule.mainClock.advanceTimeByFrame()
        // new layoutInfo is produced on every remeasure of Lazy lists.
        // but we want to avoid remeasuring and only do relayout on each animation frame.
        // two extra measures are possible as we switch inProgress flag.
        assertThat(measurePasses).isAtMost(startMeasurePasses + 2)
    }

    @Test
    fun noAnimationWhenScrolledToOtherPosition() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollToItem(0, (itemSize / 2).roundToInt()) } }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardBySmallOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollBy(itemSize / 2f) } }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardBySmallOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 2) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollBy(-itemSize / 2f) } }

        onAnimationFrame { fraction ->
            assertPositions(
                1 to -itemSize / 2,
                2 to itemSize / 2,
                3 to itemSize * 3 / 2,
                4 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollBy(itemSize * 2.5f) } }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to -itemSize / 2,
                3 to itemSize / 2,
                4 to itemSize * 3 / 2,
                5 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollBy(-itemSize * 2.5f) } }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize * 3 / 2,
                3 to itemSize * 5 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnUiThread { runBlocking { state.scrollBy(itemSize + itemSize2 + itemSize / 2f) } }

        onAnimationFrame { fraction ->
            assertPositions(
                2 to -itemSize / 2,
                3 to itemSize / 2,
                4 to itemSize2 + itemSize / 2,
                5 to itemSize2 + itemSize * 3 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollBackwardByLargeOffset_differentSizes() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3, startIndex = 3) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) {
                    Item(it, size = if (it % 2 == 0) itemSizeDp else itemSize2Dp)
                }
            }
        }

        rule.runOnUiThread {
            runBlocking { state.scrollBy(-(itemSize + itemSize2 + itemSize / 2f)) }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                0 to -itemSize / 2,
                1 to itemSize / 2,
                2 to itemSize2 + itemSize / 2,
                3 to itemSize2 + itemSize * 3 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun noAnimationWhenScrollForwardBySmallOffsetAndThenLargeOffset() {
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 2.2f) {
                items(listOf(0, 1, 2, 3, 4, 5, 6, 7), key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread {
            runBlocking {
                // first a small scroll, which will only require a relayout
                state.scrollBy(itemSize * 0.5f)
                // then a larger scroll, which requires composing new items
                state.scrollBy(itemSize * 1f)
            }
        }

        onAnimationFrame { fraction ->
            assertPositions(
                1 to -itemSize / 2,
                2 to itemSize / 2,
                3 to itemSize * 3 / 2,
                fraction = fraction
            )
        }
    }

    @Test
    fun itemWithSpecsIsMovingOut() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val listSize = itemSize * 2
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) {
                items(list, key = { it }) { Item(it, animSpec = if (it == 1) AnimSpec else null) }
            }
        }

        rule.runOnUiThread { list = listOf(0, 2, 3, 1) }

        onAnimationFrame { fraction ->
            // item 1 moves to `listSize`
            val item1Offset = itemSize + (listSize - itemSize) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < listSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveTwoItemsToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize, 5 to itemSize * 2)

        rule.runOnUiThread { list = listOf(0, 4, 5, 3, 1, 2) }

        onAnimationFrame { fraction ->
            // item 2 moves from and item 5 moves to `-itemSize`, right before the start edge
            val item2Offset = -itemSize + itemSize * 3 * fraction
            val item5Offset = itemSize * 2 - itemSize * 3 * fraction
            // item 1 moves from and item 4 moves to `-itemSize * 2`, right before item 2
            val item1Offset = -itemSize * 2 + itemSize * 3 * fraction
            val item4Offset = itemSize - itemSize * 3 * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item1Offset > -itemSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    if (item2Offset > -itemSize) {
                        add(2 to item2Offset)
                    } else {
                        rule.onNodeWithTag("2").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item4Offset > -itemSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    if (item5Offset > -itemSize) {
                        add(5 to item5Offset)
                    } else {
                        rule.onNodeWithTag("5").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveTwoItemsToTheTopOutsideOfBounds_withReordering() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4, 5))
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 3f, startIndex = 3) {
                items(list, key = { it }) { Item(it) }
            }
        }

        assertPositions(3 to 0f, 4 to itemSize, 5 to itemSize * 2)

        rule.runOnUiThread { list = listOf(0, 5, 4, 3, 2, 1) }

        onAnimationFrame { fraction ->
            // item 2 moves from and item 4 moves to `-itemSize`, right before the start edge
            val item2Offset = -itemSize + itemSize * 2 * fraction
            val item4Offset = itemSize - itemSize * 2 * fraction
            // item 1 moves from and item 5 moves to `-itemSize * 2`, right before item 2
            val item1Offset = -itemSize * 2 + itemSize * 4 * fraction
            val item5Offset = itemSize * 2 - itemSize * 4 * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    if (item1Offset > -itemSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    if (item2Offset > -itemSize) {
                        add(2 to item2Offset)
                    } else {
                        rule.onNodeWithTag("2").assertIsNotDisplayed()
                    }
                    add(3 to 0f)
                    if (item4Offset > -itemSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                    if (item5Offset > -itemSize) {
                        add(5 to item5Offset)
                    } else {
                        rule.onNodeWithTag("5").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveTwoItemsToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val listSize = itemSize * 3
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        assertPositions(0 to 0f, 1 to itemSize, 2 to itemSize * 2)

        rule.runOnUiThread { list = listOf(0, 3, 4, 1, 2) }

        onAnimationFrame { fraction ->
            // item 1 moves to and item 3 moves from `listSize`, right after the end edge
            val item1Offset = itemSize + (listSize - itemSize) * fraction
            val item3Offset = listSize - (listSize - itemSize) * fraction
            // item 2 moves to and item 4 moves from `listSize + itemSize`, right after item 4
            val item2Offset = itemSize * 2 + (listSize - itemSize) * fraction
            val item4Offset = listSize + itemSize - (listSize - itemSize) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < listSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    if (item2Offset < listSize) {
                        add(2 to item2Offset)
                    } else {
                        rule.onNodeWithTag("2").assertIsNotDisplayed()
                    }
                    if (item3Offset < listSize) {
                        add(3 to item3Offset)
                    } else {
                        rule.onNodeWithTag("3").assertIsNotDisplayed()
                    }
                    if (item4Offset < listSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun moveTwoItemsToTheBottomOutsideOfBounds_withReordering() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val listSize = itemSize * 3
        val listSizeDp = with(rule.density) { listSize.toDp() }
        rule.setContent {
            LazyList(maxSize = listSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        assertPositions(0 to 0f, 1 to itemSize, 2 to itemSize * 2)

        rule.runOnUiThread { list = listOf(0, 4, 3, 2, 1) }

        onAnimationFrame { fraction ->
            // item 2 moves to and item 3 moves from `listSize`, right after the end edge
            val item2Offset = itemSize * 2 + (listSize - itemSize * 2) * fraction
            val item3Offset = listSize - (listSize - itemSize * 2) * fraction
            // item 1 moves to and item 4 moves from `listSize + itemSize`, right after item 4
            val item1Offset = itemSize + (listSize + itemSize - itemSize) * fraction
            val item4Offset = listSize + itemSize - (listSize + itemSize - itemSize) * fraction
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    if (item1Offset < listSize) {
                        add(1 to item1Offset)
                    } else {
                        rule.onNodeWithTag("1").assertIsNotDisplayed()
                    }
                    if (item2Offset < listSize) {
                        add(2 to item2Offset)
                    } else {
                        rule.onNodeWithTag("2").assertIsNotDisplayed()
                    }
                    if (item3Offset < listSize) {
                        add(3 to item3Offset)
                    } else {
                        rule.onNodeWithTag("3").assertIsNotDisplayed()
                    }
                    if (item4Offset < listSize) {
                        add(4 to item4Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    @Test
    fun noAnimationWhenParentSizeShrinks() {
        var size by mutableStateOf(itemSizeDp * 3)
        rule.setContent {
            LazyList(maxSize = size) { items(listOf(0, 1, 2), key = { it }) { Item(it) } }
        }

        rule.runOnUiThread { size = itemSizeDp * 2 }

        onAnimationFrame { fraction ->
            assertPositions(0 to 0f, 1 to itemSize, fraction = fraction)
            rule.onNodeWithTag("2").assertIsNotDisplayed()
        }
    }

    @Test
    fun noAnimationWhenParentSizeExpands() {
        var size by mutableStateOf(itemSizeDp * 2)
        rule.setContent {
            LazyList(maxSize = size) { items(listOf(0, 1, 2), key = { it }) { Item(it) } }
        }

        rule.runOnUiThread { size = itemSizeDp * 3 }

        onAnimationFrame { fraction ->
            assertPositions(0 to 0f, 1 to itemSize, 2 to itemSize * 2, fraction = fraction)
        }
    }

    @Test
    fun scrollIsAffectingItemsMovingWithinViewport() {
        var list by mutableStateOf(listOf(0, 1, 2, 3))
        val scrollDelta = spacing
        rule.setContent {
            LazyList(maxSize = itemSizeDp * 2) { items(list, key = { it }) { Item(it) } }
        }

        rule.runOnUiThread { list = listOf(0, 2, 1, 3) }

        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                assertPositions(0 to 0f, 1 to itemSize, 2 to itemSize * 2, fraction = fraction)
                rule.runOnUiThread { runBlocking { state.scrollBy(scrollDelta) } }
            }
            assertPositions(
                0 to -scrollDelta,
                1 to itemSize - scrollDelta + itemSize * fraction,
                2 to itemSize * 2 - scrollDelta - itemSize * fraction,
                fraction = fraction
            )
        }
    }

    @Test
    fun scrollIsNotAffectingItemMovingToTheBottomOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val scrollDelta = spacing
        // Make the container size slightly bigger than 2 items in lookahead, so that scrolling
        // does not compose new items and interrupt existing item placement animation.
        val containerSizeDp = itemSizeDp * 2
        val containerSize = itemSize * 2
        rule.setContent {
            LazyList(maxSize = containerSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        rule.runOnUiThread { list = listOf(0, 4, 2, 3, 1) }

        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                assertPositions(0 to 0f, 1 to itemSize, fraction = fraction)
                rule.runOnUiThread { runBlocking { state.scrollBy(scrollDelta) } }
            }
            if (isInLookaheadScope) {
                assertPositions(
                    0 to -scrollDelta,
                    1 to
                        spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)
                            .getValueAtFrame(
                                (fraction * Duration / FrameDuration).toInt(),
                                from = itemSize - scrollDelta,
                                to = itemSize * 3 - scrollDelta
                            ),
                    fraction = fraction
                )
            } else {
                assertPositions(
                    0 to -scrollDelta,
                    1 to itemSize + (containerSize - itemSize) * fraction,
                    fraction = fraction
                )
            }
        }
    }

    @Test
    fun scrollIsNotAffectingItemMovingToTheTopOutsideOfBounds() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val scrollDelta = -spacing
        val containerSizeDp = itemSizeDp * 2
        rule.setContent {
            LazyList(maxSize = containerSizeDp, startIndex = 2) {
                items(list, key = { it }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(3, 0, 1, 2, 4) }

        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                assertPositions(2 to 0f, 3 to itemSize, fraction = fraction)
                rule.runOnUiThread { runBlocking { state.scrollBy(scrollDelta) } }
            }
            if (isInLookaheadScope) {
                // Expect interruption to lookahead placement animation on 0th frame, from
                // an additional item (i.e. item with key = 1) being composed due to scrolling.
                assertPositions(
                    2 to -scrollDelta,
                    3 to
                        interruptionSpec.getValueAtFrame(
                            (Duration / FrameDuration * fraction).toInt(),
                            from = itemSize,
                            to = -2 * itemSize - scrollDelta
                        ),
                    fraction = fraction
                )
            } else {
                assertPositions(
                    2 to -scrollDelta,
                    3 to itemSize - (itemSize * 2 * fraction),
                    fraction = fraction
                )
            }
        }
    }

    @Test
    fun afterScrollingEnoughToReachNewPositionScrollDeltasStartAffectingPosition() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        val containerSizeDp = itemSizeDp * 2
        val scrollDelta = spacing
        rule.setContent {
            LazyList(maxSize = containerSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        rule.runOnUiThread { list = listOf(0, 4, 2, 3, 1) }

        onAnimationFrame { fraction ->
            if (fraction == 0f) {
                assertPositions(0 to 0f, 1 to itemSize, fraction = fraction)
                rule.runOnUiThread { runBlocking { state.scrollBy(itemSize * 2) } }
                val postFirstScrollItem2Offset = if (isInLookaheadScope) -itemSize else itemSize
                assertPositions(
                    2 to 0f,
                    3 to itemSize,
                    // after the first scroll the new position of item 1 is still not reached
                    // so the target didn't change, we still aim to end right after the bounds
                    1 to postFirstScrollItem2Offset,
                    fraction = fraction
                )
                rule.runOnUiThread { runBlocking { state.scrollBy(scrollDelta) } }
                assertPositions(
                    2 to 0f - scrollDelta,
                    3 to itemSize - scrollDelta,
                    // after the second scroll the item 1 is visible, so we know its new target
                    // position. the animation is now targeting the real end position and now
                    // we are reacting on the scroll deltas
                    1 to postFirstScrollItem2Offset - scrollDelta,
                    fraction = fraction
                )
            }
            if (!isInLookaheadScope) {
                assertPositions(
                    2 to -scrollDelta,
                    3 to itemSize - scrollDelta,
                    1 to itemSize - scrollDelta + itemSize * fraction,
                    fraction = fraction
                )
            } else {
                // Expect interruption to lookahead placement animation on 0th frame.
                assertPositions(
                    2 to -scrollDelta,
                    3 to itemSize - scrollDelta,
                    1 to
                        interruptionSpec.getValueAtFrame(
                            (Duration / FrameDuration * fraction).toInt(),
                            from = -itemSize - scrollDelta,
                            to = 2 * itemSize - scrollDelta
                        ),
                    fraction = fraction
                )
            }
        }
    }

    @Test
    fun animationWhenTryingToStayInTheStart() {
        var list by mutableStateOf(listOf(1, 2, 3, 4))
        val listSize = itemSize * 2.5
        val listSizeDp = itemSizeDp * 3f
        rule.setContent {
            LazyList(maxSize = listSizeDp) { items(list, key = { it }) { Item(it) } }
        }

        rule.runOnUiThread {
            list = listOf(0, 1, 2, 3)
            runBlocking { state.scrollToItem(0, 0) }
        }

        onAnimationFrame { fraction ->
            val expected =
                mutableListOf<Pair<Any, Float>>().apply {
                    add(0 to 0f)
                    add(1 to itemSize * fraction)
                    add(2 to itemSize + itemSize * fraction)
                    val item3Offset = itemSize * 2 + itemSize * fraction
                    if (item3Offset < listSize) {
                        add(3 to item3Offset)
                    } else {
                        rule.onNodeWithTag("4").assertIsNotDisplayed()
                    }
                }
            assertPositions(expected = expected.toTypedArray(), fraction = fraction)
        }
    }

    private val interruptionSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)

    @Test
    fun interruptedSizeChange() {
        var item0Size by mutableStateOf(itemSizeDp)
        val animSpec = spring(visibilityThreshold = IntOffset.VisibilityThreshold)
        rule.setContent {
            LazyList {
                items(2, key = { it }) {
                    Item(it, if (it == 0) item0Size else itemSizeDp, animSpec = animSpec)
                }
            }
        }

        rule.runOnUiThread { item0Size = itemSize2Dp }

        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        onAnimationFrame(duration = FrameDuration) { fraction ->
            if (fraction == 0f) {
                assertPositions(0 to 0f, 1 to itemSize)
            } else {
                assertThat(fraction).isEqualTo(1f)
                val valueAfterOneFrame =
                    animSpec.getValueAtFrame(1, from = itemSize, to = itemSize2)
                assertPositions(0 to 0f, 1 to valueAfterOneFrame, fraction = fraction)
            }
        }

        rule.runOnUiThread { item0Size = 0.dp }

        rule.waitForIdle()
        val startValue = animSpec.getValueAtFrame(2, from = itemSize, to = itemSize2)
        val startVelocity = animSpec.getVelocityAtFrame(2, from = itemSize, to = itemSize2)
        onAnimationFrame(duration = FrameDuration) { fraction ->
            if (fraction == 0f) {
                assertPositions(0 to 0f, 1 to startValue)
            } else {
                assertThat(fraction).isEqualTo(1f)
                val valueAfterThreeFrames =
                    animSpec.getValueAtFrame(
                        1,
                        from = startValue,
                        to = 0f,
                        initialVelocity = startVelocity
                    )
                assertPositions(0 to 0f, 1 to valueAfterThreeFrames)
            }
        }
    }

    private fun assertPositions(
        vararg expected: Pair<Any, Float>,
        crossAxis: List<Pair<Any, Float>>? = null,
        fraction: Float? = null,
        autoReverse: Boolean = reverseLayout
    ) {
        val roundedExpected = expected.map { it.first to it.second.roundToInt() }
        val actualBounds =
            rule
                .onAllNodes(NodesWithTagMatcher)
                .fetchSemanticsNodes()
                .associateBy(
                    keySelector = { it.config.get(SemanticsProperties.TestTag) },
                    valueTransform = { IntRect(it.positionInRoot.round(), it.size) }
                )
        val actualOffsets =
            expected.map {
                it.first to
                    actualBounds.getValue(it.first.toString()).let { bounds ->
                        if (isVertical) bounds.top else bounds.left
                    }
            }
        val subject =
            if (fraction == null) {
                assertThat(actualOffsets)
            } else {
                assertWithMessage("Fraction=$fraction").that(actualOffsets)
            }
        subject.isEqualTo(
            roundedExpected.let { list ->
                if (!autoReverse) {
                    list
                } else {
                    val containerSize =
                        actualBounds.getValue(ContainerTag).let { bounds ->
                            if (isVertical) bounds.height else bounds.width
                        }
                    list.map {
                        val itemSize =
                            actualBounds.getValue(it.first.toString()).let { bounds ->
                                if (isVertical) bounds.height else bounds.width
                            }
                        it.first to (containerSize - itemSize - it.second)
                    }
                }
            }
        )
        if (crossAxis != null) {
            val actualCrossOffset =
                expected.map {
                    it.first to
                        actualBounds.getValue(it.first.toString()).let { bounds ->
                            if (isVertical) bounds.left else bounds.top
                        }
                }
            assertWithMessage("CrossAxis" + if (fraction != null) "for fraction=$fraction" else "")
                .that(actualCrossOffset)
                .isEqualTo(crossAxis.map { it.first to it.second.roundToInt() })
        }
    }

    private fun assertLayoutInfoPositions(vararg offsets: Pair<Any, Float>) {
        rule.runOnIdle {
            assertThat(visibleItemsOffsets)
                .isEqualTo(offsets.map { it.first to it.second.roundToInt() })
        }
    }

    private val visibleItemsOffsets: List<Pair<Any, Int>>
        get() = state.layoutInfo.visibleItemsInfo.map { it.key to it.offset }

    private fun onAnimationFrame(duration: Long = Duration, onFrame: (fraction: Float) -> Unit) {
        require(duration.mod(FrameDuration) == 0L)
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        var expectedTime = rule.mainClock.currentTime
        for (i in 0..duration step FrameDuration) {
            val fraction = i / duration.toFloat()
            onFrame(fraction)
            if (i < duration) {
                rule.mainClock.advanceTimeBy(FrameDuration)
                expectedTime += FrameDuration
                assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            }
        }
    }

    @Composable
    private fun LazyList(
        arrangement: Arrangement.HorizontalOrVertical? = null,
        minSize: Dp = 0.dp,
        maxSize: Dp = containerSizeDp,
        startIndex: Int = 0,
        crossAxisSize: Dp = Dp.Unspecified,
        crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
        startPadding: Dp = 0.dp,
        endPadding: Dp = 0.dp,
        content: LazyListScope.() -> Unit
    ) {
        val container: @Composable (@Composable () -> Unit) -> Unit =
            if (isInLookaheadScope) {
                { LookaheadScope { it() } }
            } else {
                { it() }
            }
        container {
            state = rememberLazyListState(startIndex)
            if (isVertical) {
                val verticalArrangement =
                    arrangement ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom
                val horizontalAlignment =
                    if (crossAxisAlignment == CrossAxisAlignment.Start) {
                        Alignment.Start
                    } else if (crossAxisAlignment == CrossAxisAlignment.Center) {
                        Alignment.CenterHorizontally
                    } else {
                        Alignment.End
                    }
                LazyColumn(
                    state = state,
                    modifier =
                        Modifier.requiredHeightIn(min = minSize, max = maxSize)
                            .then(
                                if (crossAxisSize != Dp.Unspecified) {
                                    Modifier.requiredWidth(crossAxisSize)
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            )
                            .testTag(ContainerTag),
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = horizontalAlignment,
                    reverseLayout = reverseLayout,
                    contentPadding = PaddingValues(top = startPadding, bottom = endPadding),
                    content = content
                )
            } else {
                val horizontalArrangement =
                    arrangement ?: if (!reverseLayout) Arrangement.Start else Arrangement.End
                val verticalAlignment =
                    if (crossAxisAlignment == CrossAxisAlignment.Start) {
                        Alignment.Top
                    } else if (crossAxisAlignment == CrossAxisAlignment.Center) {
                        Alignment.CenterVertically
                    } else {
                        Alignment.Bottom
                    }
                LazyRow(
                    state = state,
                    modifier =
                        Modifier.requiredWidthIn(min = minSize, max = maxSize)
                            .then(
                                if (crossAxisSize != Dp.Unspecified) {
                                    Modifier.requiredHeight(crossAxisSize)
                                } else {
                                    Modifier.fillMaxHeight()
                                }
                            )
                            .testTag(ContainerTag),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = verticalAlignment,
                    reverseLayout = reverseLayout,
                    contentPadding = PaddingValues(start = startPadding, end = endPadding),
                    content = content
                )
            }
        }
    }

    @Composable
    private fun LazyItemScope.Item(
        tag: Int,
        size: Dp = itemSizeDp,
        crossAxisSize: Dp = size,
        animSpec: FiniteAnimationSpec<IntOffset>? = AnimSpec
    ) {
        Box(
            if (animSpec != null) {
                    Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = animSpec
                    )
                } else {
                    Modifier
                }
                .then(
                    if (isVertical) {
                        Modifier.requiredHeight(size).requiredWidth(crossAxisSize)
                    } else {
                        Modifier.requiredWidth(size).requiredHeight(crossAxisSize)
                    }
                )
                .testTag(tag.toString())
        )
    }

    private fun SemanticsNodeInteraction.assertMainAxisSizeIsEqualTo(
        expected: Dp
    ): SemanticsNodeInteraction {
        return if (isVertical) assertHeightIsEqualTo(expected) else assertWidthIsEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            arrayOf(
                Config(isVertical = true, reverseLayout = false, isInLookaheadScope = false),
                Config(isVertical = false, reverseLayout = false, isInLookaheadScope = false),
                Config(isVertical = true, reverseLayout = true, isInLookaheadScope = false),
                Config(isVertical = false, reverseLayout = true, isInLookaheadScope = false),
                Config(isVertical = true, reverseLayout = false, isInLookaheadScope = true)
            )

        class Config(
            val isVertical: Boolean,
            val reverseLayout: Boolean,
            val isInLookaheadScope: Boolean
        ) {
            override fun toString() =
                (if (isVertical) "LazyColumn" else "LazyRow") +
                    (if (reverseLayout) "(reverse)" else "") +
                    (if (isInLookaheadScope) "(in LookaheadScope)" else "")
        }
    }
}

private val FrameDuration = 16L
private val Duration = 64L // 4 frames, so we get 0f, 0.25f, 0.5f, 0.75f and 1f fractions
private val AnimSpec = tween<IntOffset>(Duration.toInt(), easing = LinearEasing)
private val ContainerTag = "container"
private val NodesWithTagMatcher =
    SemanticsMatcher("NodesWithTag") { it.config.contains(SemanticsProperties.TestTag) }

private enum class CrossAxisAlignment {
    Start,
    End,
    Center
}

internal fun SpringSpec<IntOffset>.getValueAtFrame(
    frameCount: Int,
    from: Float,
    to: Float,
    initialVelocity: IntOffset = IntOffset.Zero
): Float {
    val frameInNanos = TimeUnit.MILLISECONDS.toNanos(FrameDuration)
    val vectorized = vectorize(converter = IntOffset.VectorConverter)
    return IntOffset.VectorConverter.convertFromVector(
            vectorized.getValueFromNanos(
                playTimeNanos = frameInNanos * frameCount,
                initialValue =
                    IntOffset.VectorConverter.convertToVector(IntOffset(0, from.toInt())),
                targetValue = IntOffset.VectorConverter.convertToVector(IntOffset(0, to.toInt())),
                initialVelocity = IntOffset.VectorConverter.convertToVector(initialVelocity)
            )
        )
        .y
        .toFloat()
}

internal fun SpringSpec<IntOffset>.getVelocityAtFrame(
    frameCount: Int,
    from: Float,
    to: Float,
    initialVelocity: IntOffset = IntOffset.Zero
): IntOffset {
    val frameInNanos = TimeUnit.MILLISECONDS.toNanos(FrameDuration)
    val vectorized = vectorize(converter = IntOffset.VectorConverter)
    return IntOffset.VectorConverter.convertFromVector(
        vectorized.getVelocityFromNanos(
            playTimeNanos = frameInNanos * frameCount,
            initialValue = IntOffset.VectorConverter.convertToVector(IntOffset(0, from.toInt())),
            targetValue = IntOffset.VectorConverter.convertToVector(IntOffset(0, to.toInt())),
            initialVelocity = IntOffset.VectorConverter.convertToVector(initialVelocity)
        )
    )
}
