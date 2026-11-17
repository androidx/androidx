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

package androidx.compose.foundation.lazy.list

import android.os.Build
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.animateItemWithTransitions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutItemAnimationTest
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class LazyListItemChangeSizeAnimationTest(orientation: Orientation) :
    BaseLazyLayoutItemAnimationTest(orientation) {
    private lateinit var state: LazyListState

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @Test
    fun expandMainAxis_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { ExpandingItem(enter = expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(size = interpolate(0, ItemMainAxisSize, fraction)),
            )
        }
    }

    @Test
    fun expandMainAxis_fromStart_revealsContentStartFirst() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    ExpandingItem(enter = expandMainAxis(fromEnd = false))
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(
                    size = interpolate(0, ItemMainAxisSize, fraction),
                    alignToEnd = false,
                ),
            )
        }
    }

    @Test
    fun expandMainAxis_partialInitialSize() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    ExpandingItem(enter = expandMainAxis(initialSize = { it / 2 }))
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(
                    size = interpolate(ItemMainAxisSize / 2, ItemMainAxisSize, fraction)
                ),
            )
        }
    }

    @Test
    fun expandMainAxis_onlyFollowingItemsMove() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
        }
    }

    @Test
    fun expandMainAxis_itemsAddedWithoutExpandAreFullSizeImmediately() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = size),
                ExpectedItem(Color.Black, mainAxis = size),
            )
        }
    }

    @Test
    fun expandCrossAxis_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { Item(it, enter = expandCrossAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(
                    Color.Black,
                    crossAxisSize = interpolate(0, ItemCrossAxisSize, fraction),
                ),
            )
        }
    }

    @Test
    fun expandCrossAxis_doesNotMoveFollowingItems() {
        var list by mutableStateOf(listOf(Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = if (it == Color.Black) expandCrossAxis() else null)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Blue) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(
                    Color.Black,
                    crossAxisSize = interpolate(0, ItemCrossAxisSize, fraction),
                ),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun noAnimationForInitialItems() {
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(listOf(StartColor, Color.Black), key = { it.toArgb() }) {
                    ItemOrExpandingItem(it, expandMainAxis())
                }
            }
        }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = ItemMainAxisSize),
                ExpectedItem(Color.Black, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun itemScrolledIntoView_isNotExpanded() {
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(
                    listOf(Color.Black, Color.Blue, StartColor, Color.Yellow),
                    key = { it.toArgb() },
                ) {
                    ItemOrExpandingItem(it, expandMainAxis())
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize * 2f) } }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = ItemMainAxisSize),
                ExpectedItem(Color.Yellow, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun itemAddedOutsideOfViewport_isNotExpandedWhenScrolledIntoView() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Blue, StartColor, Color.Yellow) }

        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize * 1.5f) } }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Blue, mainAxis = -ItemMainAxisSize / 2),
                *expandingItem(mainAxis = ItemMainAxisSize / 2, size = ItemMainAxisSize),
                ExpectedItem(Color.Yellow, mainAxis = ItemMainAxisSize * 3 / 2),
            )
        }
    }

    @Test
    fun fadeIn_listIsNotMeasuredAgainWhileItemFadesIn() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { Item(it, enter = fadeIn(FadeSpec)) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Blue, Color.Black) }

        var layoutInfo: Any? = null
        onAnimationStep(steps = Steps - 1) { step ->
            rule.runOnIdle {
                if (step > 0) assertThat(state.layoutInfo).isSameInstanceAs(layoutInfo)
                layoutInfo = state.layoutInfo
            }
        }
    }

    @Test
    fun expandCombinedWithFadeIn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    ExpandingItem(enter = fadeIn(FadeSpec) + expandMainAxis())
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(
                    size = interpolate(0, ItemMainAxisSize, fraction),
                    startColor = StartColor.copy(alpha = fraction),
                    endColor = EndColor.copy(alpha = fraction),
                ),
            )
        }
    }

    @Test
    fun withoutClip_contentIsDrawnOutsideExpandedBounds() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    ItemOrExpandingItem(it, expandMainAxis(fromEnd = false, clip = false))
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = size, alignToEnd = false, clip = false),
                ExpectedItem(Color.Black, mainAxis = size),
            )
        }
    }

    @Test
    fun reAddItemAfterExpandFinished_expandsFromInitialSize() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { ExpandingItem(enter = expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }
        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(size = interpolate(0, ItemMainAxisSize, fraction)),
            )
        }

        rule.runOnUiThread { list = emptyList() }
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerMainAxisSize = ItemMainAxisSize)

        rule.runOnUiThread { list = listOf(StartColor) }
        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(size = interpolate(0, ItemMainAxisSize, fraction)),
            )
        }
    }

    @Test
    fun reAddItemBeingFadedOut_doesNotExpandAgain() {
        var list by mutableStateOf(listOf(StartColor))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    ExpandingItem(enter = expandMainAxis(), exit = fadeOut(FadeSpec))
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }
        onAnimationStep(steps = Steps / 2) { step ->
            val alpha = 1f - step / Steps.toFloat()
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(
                    size = ItemMainAxisSize,
                    startColor = StartColor.copy(alpha = alpha),
                    endColor = EndColor.copy(alpha = alpha),
                ),
            )
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                *expandingItem(size = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun scrollingDuringExpand_expandContinuesRelativeToItemPosition() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }

        val scrollAmount = ItemMainAxisSize / 2
        onAnimationStep { step ->
            if (step == Steps / 2) {
                rule.runOnIdle { runBlocking { state.scrollBy(scrollAmount.toFloat()) } }
            }
            val scrolled = if (step >= Steps / 2) scrollAmount else 0
            val size = interpolate(0, ItemMainAxisSize, step / Steps.toFloat())
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black, mainAxis = -scrolled),
                *expandingItem(mainAxis = ItemMainAxisSize - scrolled, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size - scrolled),
            )
        }
    }

    @Test
    fun expandMainAxis_itemWithSeveralRootComposables() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                items(list, key = { it.toArgb() }) {
                    if (it == StartColor) {
                        ExpandingItem(enter = expandMainAxis())
                        ExpandingItem(enter = expandMainAxis())
                    } else {
                        Item(it)
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                *expandingItem(size = size),
                *expandingItem(mainAxis = size, size = size),
                ExpectedItem(Color.Black, mainAxis = size * 2),
            )
        }
    }

    @Test
    fun expandMainAxis_itemWithSeveralRootComposables_onlyOneAnimated() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                items(list, key = { it.toArgb() }) {
                    if (it == StartColor) {
                        ExpandingItem(enter = expandMainAxis())
                        ExpandingItem()
                    } else {
                        Item(it)
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                *expandingItem(size = size),
                *expandingItem(mainAxis = size, size = ItemMainAxisSize),
                ExpectedItem(Color.Black, mainAxis = size + ItemMainAxisSize),
            )
        }
    }

    @Test
    fun expandWithOvershootingSpring_followingItemStaysNextToTheItem() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        val overshootingSpec: FiniteAnimationSpec<IntSize> =
            spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium)
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 4) {
                items(list, key = { it.toArgb() }) {
                    ItemOrExpandingItem(it, expandMainAxis(animationSpec = overshootingSpec))
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }

        var maxFollowingItemOffset = 0
        onAnimationStep(steps = 80) {
            val followingItemOffset = mainAxisRangeOf(Color.Blue)!!.first
            val contentEnd = mainAxisRangeOf(EndColor)?.last
            if (contentEnd != null) assertThat(followingItemOffset).isEqualTo(contentEnd + 1)
            maxFollowingItemOffset = maxOf(maxFollowingItemOffset, followingItemOffset)
        }
        assertThat(maxFollowingItemOffset).isGreaterThan(ItemMainAxisSize * 2)
        assertThat(mainAxisRangeOf(Color.Blue)!!.first).isEqualTo(ItemMainAxisSize * 2)
    }

    @Test
    fun expandCrossAxisWithOvershootingSpring_boundsAreNoLargerThanTheListAllows() {
        var list by mutableStateOf(emptyList<Color>())
        val overshootingSpec: FiniteAnimationSpec<IntSize> =
            spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium)
        val expand =
            if (vertical) {
                expandHorizontally(overshootingSpec, initialWidth = { 0 })
            } else {
                expandVertically(overshootingSpec, initialHeight = { 0 })
            }
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { Item(it, enter = expand) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationStep(steps = 80) {
            val range = crossAxisRangeOf(Color.Black)
            if (range != null) assertThat(range.first).isEqualTo(0)
        }
        assertThat(crossAxisRangeOf(Color.Black)).isEqualTo(0 until ItemCrossAxisSize)
    }

    @Test
    fun expandingItem_doesNotTakeInputMeantForThePreviousItem() {
        var list by mutableStateOf(listOf(Color.Black))
        var hitItem: Color?
        fun Modifier.recordHits(color: Color) =
            pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        hitItem = color
                    }
                }
            }
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    if (it == StartColor) {
                        ExpandingItem(
                            enter = expandMainAxis(),
                            modifier = Modifier.recordHits(it),
                        )
                    } else {
                        Item(it, modifier = Modifier.recordHits(it))
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor) }
        onAnimationStep(steps = Steps / 2) {}

        val overPreviousItem = ItemMainAxisSize * 3 / 4
        val withinBounds = ItemMainAxisSize + ItemMainAxisSize / 4
        fun hitAt(mainAxis: Int): Color? {
            hitItem = null
            val crossAxis = ItemCrossAxisSize / 2f
            rule.onNodeWithTag(ContainerTag).performTouchInput {
                down(
                    if (vertical) {
                        Offset(crossAxis, mainAxis.toFloat())
                    } else {
                        Offset(mainAxis.toFloat(), crossAxis)
                    }
                )
                up()
            }
            return rule.runOnIdle { hitItem }
        }
        assertThat(hitAt(overPreviousItem)).isEqualTo(Color.Black)
        assertThat(hitAt(withinBounds)).isEqualTo(StartColor)
    }

    @Test
    fun expandMainAxis_withPaddingBeforeAnimateItem_paddingExpandsWithItem() {
        val padding = ItemMainAxisSize / 4
        val fullSize = ItemMainAxisSize + padding * 2
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(
                containerMainAxisSize = fullSize,
                containerCrossAxisSize = ItemCrossAxisSize + padding * 2,
            ) {
                items(list, key = { it.toArgb() }) {
                    ExpandingItem(
                        enter = expandMainAxis(),
                        modifier = Modifier.background(PaddingColor).padding(padding.toDp()),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = fullSize,
                *paddedExpandingItem(size = interpolate(0, fullSize, fraction), padding),
                containerCrossAxisSize = ItemCrossAxisSize + padding * 2,
            )
        }
    }

    private fun paddedExpandingItem(size: Int, padding: Int): Array<ExpectedItem> {
        val fullSize = ItemMainAxisSize + padding * 2
        return arrayOf(
            ExpectedItem(
                PaddingColor,
                mainAxisSize = size,
                crossAxisSize = ItemCrossAxisSize + padding * 2,
            ),
            *expandingItem(mainAxis = size - fullSize + padding, size = ItemMainAxisSize)
                .mapNotNull { it.clippedTo(mainAxisEnd = size, crossAxis = padding) }
                .toTypedArray(),
        )
    }

    private fun ExpectedItem.clippedTo(mainAxisEnd: Int, crossAxis: Int): ExpectedItem? {
        val start = maxOf(mainAxis, 0)
        val end = minOf(mainAxis + mainAxisSize, mainAxisEnd)
        return if (end > start) {
            ExpectedItem(color, mainAxis = start, crossAxis = crossAxis, mainAxisSize = end - start)
        } else {
            null
        }
    }

    @Test
    fun scrollingDuringExpand_itemBeingPushedOutScrollsWithTheOtherItems() {
        val containerSize = ItemMainAxisSize * 3 / 2
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        rule.setContent {
            LazyList(containerMainAxisSize = containerSize) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }
        onAnimationStep(steps = 1) {}

        val scrollAmount = ItemMainAxisSize / 4
        rule.runOnIdle { runBlocking { state.scrollBy(scrollAmount.toFloat()) } }

        val size = interpolate(0, ItemMainAxisSize, 1 / Steps.toFloat())
        assertItems(
            containerMainAxisSize = containerSize,
            ExpectedItem(Color.Black, mainAxis = -scrollAmount),
            *expandingItem(mainAxis = ItemMainAxisSize - scrollAmount, size = size),
            ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size - scrollAmount),
        )
    }

    @Test
    fun inLookaheadScope_followingItemsArePushed() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        ItemOrExpandingItem(it, expandMainAxis())
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = size),
                ExpectedItem(Color.Black, mainAxis = size),
            )
        }
    }

    @Test
    fun inLookaheadScope_followingItemsWithAnimationsArePushed() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        if (it == StartColor) {
                            ExpandingItem(enter = expandMainAxis())
                        } else {
                            Item(it, enter = fadeIn(FadeSpec))
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = size),
                ExpectedItem(Color.Black, mainAxis = size),
            )
        }
    }

    @Test
    fun itemsChangedAgainWhileItemIsPushed_itemNeverMovesBack() {
        assertItemNeverMovesBackWhenItemsChangeAgain(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_itemsChangedAgainWhileItemIsPushed_itemNeverMovesBack() {
        assertItemNeverMovesBackWhenItemsChangeAgain(isInLookaheadScope = true)
    }

    private fun assertItemNeverMovesBackWhenItemsChangeAgain(isInLookaheadScope: Boolean) {
        var list by mutableStateOf(listOf(Color.Black))
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                items(list, key = { it.toArgb() }) {
                    when (it) {
                        StartColor -> ExpandingItem(enter = expandMainAxis())
                        Color.Black -> Item(it, placementSpec = SlowPlacementSpec)
                        else -> Item(it)
                    }
                }
            }
        }

        val offsets = mutableListOf<Int>()
        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }
        onAnimationStep(steps = Steps / 2) { offsets += mainAxisRangeOf(Color.Black)!!.first }
        rule.runOnUiThread { list = listOf(Color.Blue, StartColor, Color.Black) }
        onAnimationStep(steps = 60) { offsets += mainAxisRangeOf(Color.Black)!!.first }

        assertThat(offsets).isInOrder()
        assertThat(offsets.first()).isEqualTo(0)
        assertThat(offsets.last()).isEqualTo(ItemMainAxisSize * 2)
        assertWithMessage("Offsets: $offsets")
            .that(offsets.zipWithNext { offset, next -> next - offset }.max())
            .isLessThan(ItemMainAxisSize * 3 / 4)
    }

    @Test
    fun inLookaheadScope_itemPushedOutOfViewport_staysUntilItIsPushedOut() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        if (it == StartColor) {
                            ExpandingItem(enter = expandMainAxis())
                        } else {
                            Item(it, enter = fadeIn(FadeSpec))
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
        }
    }

    @Test
    fun inLookaheadScope_itemPushedOutOfViewport_keepsItsAnimation() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        if (it == StartColor) {
                            ExpandingItem(enter = expandMainAxis())
                        } else {
                            Item(it, enter = fadeIn(FadeSpec))
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }

        var animation: LazyLayoutItemAnimation? = null
        onAnimationStep(steps = Steps - 1) { step ->
            val current = rule.runOnIdle { state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0) }
            assertThat(current).isNotNull()
            if (step > 0) assertThat(current).isSameInstanceAs(animation)
            animation = current
        }
    }

    @Test
    fun inLookaheadScope_itemPushedOutOfViewport_keepsItsAnimationWhenLookaheadPassRunsAgain() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        if (it == StartColor) {
                            ExpandingItem(enter = expandMainAxis())
                        } else {
                            Item(it, enter = fadeIn(FadeSpec))
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }
        onAnimationStep(steps = 1) {}
        val animation = rule.runOnIdle { state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0) }
        assertThat(animation).isNotNull()

        rule.runOnUiThread {
            list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow, Color.Magenta)
        }
        onAnimationStep(steps = 0) {}

        rule.runOnIdle {
            assertThat(state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0))
                .isSameInstanceAs(animation)
        }
    }

    @Test
    fun itemsAddedThatFit_existingItemsStayUntilTheyArePushedOut() {
        assertExistingItemsStayUntilTheyArePushedOut(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_itemsAddedThatFit_existingItemsStayUntilTheyArePushedOut() {
        assertExistingItemsStayUntilTheyArePushedOut(isInLookaheadScope = true)
    }

    @Test
    fun itemsAddedThatFit_itemsComposedBeyondTheViewportStayBeyondIt() {
        assertExistingItemsStayUntilTheyArePushedOut(
            isInLookaheadScope = false,
            beyondBoundsItemCount = 1,
        )
    }

    @Test
    fun moreItemsAddedThanFit_allAddedItemsExpandAndExistingItemsStayUntilPushedOut() {
        assertAllAddedItemsExpand(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_moreItemsAddedThanFit_allAddedItemsExpand() {
        assertAllAddedItemsExpand(isInLookaheadScope = true)
    }

    private fun assertAllAddedItemsExpand(isInLookaheadScope: Boolean) {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = expandMainAxis(), exit = ExitTransition.None)
                }
            }
        }

        rule.runOnUiThread {
            list =
                listOf(
                    Color.Black,
                    Color.Red,
                    Color.Green,
                    Color.Magenta,
                    Color.Blue,
                    Color.Yellow,
                )
        }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, mainAxis = ItemMainAxisSize, mainAxisSize = size),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize + size, mainAxisSize = size),
                ExpectedItem(
                    Color.Magenta,
                    mainAxis = ItemMainAxisSize + size * 2,
                    mainAxisSize = size,
                ),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size * 3),
            )
        }
    }

    private fun assertExistingItemsStayUntilTheyArePushedOut(
        isInLookaheadScope: Boolean,
        beyondBoundsItemCount: Int? = null,
    ) {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        setContent(isInLookaheadScope) {
            LazyList(
                containerMainAxisSize = ItemMainAxisSize * 2,
                beyondBoundsItemCount = beyondBoundsItemCount,
            ) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = expandMainAxis(), exit = ExitTransition.None)
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow)
        }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, mainAxis = ItemMainAxisSize, mainAxisSize = size),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize + size, mainAxisSize = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size * 2),
            )
        }
    }

    @Test
    fun itemsAddedThatFit_existingItemIsReleasedOnceItIsPushedOut() {
        assertExistingItemIsReleasedOncePushedOut(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_itemsAddedThatFit_existingItemIsReleasedOnceItIsPushedOut() {
        assertExistingItemIsReleasedOncePushedOut(isInLookaheadScope = true)
    }

    private fun assertExistingItemIsReleasedOncePushedOut(isInLookaheadScope: Boolean) {
        val containerSize = ItemMainAxisSize * 3 / 2
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = containerSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = expandMainAxis(), exit = ExitTransition.None)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Blue, Color.Yellow) }

        onAnimationStep(steps = Steps - 1) { step ->
            val blueOffset =
                ItemMainAxisSize + interpolate(0, ItemMainAxisSize, step / Steps.toFloat())
            val animation = rule.runOnIdle {
                state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0)
            }
            if (blueOffset < containerSize) {
                assertThat(animation).isNotNull()
            } else {
                assertThat(animation).isNull()
            }
        }
    }

    @Test
    fun itemsWithoutItemAnimations_arePushedAndStayUntilTheyArePushedOut() {
        assertItemsWithoutItemAnimationsArePushed(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_itemsWithoutItemAnimations_arePushedAndStayUntilTheyArePushedOut() {
        assertItemsWithoutItemAnimationsArePushed(isInLookaheadScope = true)
    }

    private fun assertItemsWithoutItemAnimationsArePushed(isInLookaheadScope: Boolean) {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        var blueCompositions = 0
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    if (it == StartColor) {
                        ExpandingItem(enter = expandMainAxis())
                    } else {
                        ItemWithoutItemAnimations(
                            color = it,
                            onComposed = { if (it == Color.Blue) blueCompositions++ },
                        )
                    }
                }
            }
        }
        rule.runOnIdle { assertThat(blueCompositions).isEqualTo(1) }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
            rule.runOnIdle { assertThat(blueCompositions).isEqualTo(1) }
        }
    }

    @Test
    fun replacedItem_itemsAfterTheViewportFillIt() {
        assertItemsAfterTheViewportFillIt(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_replacedItem_itemsAfterTheViewportFillIt() {
        assertItemsAfterTheViewportFillIt(isInLookaheadScope = true)
    }

    private fun assertItemsAfterTheViewportFillIt(isInLookaheadScope: Boolean) {
        var list by
            mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan))
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }
        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize.toFloat()) } }

        rule.runOnUiThread {
            list = listOf(Color.Black, StartColor, Color.Yellow, Color.Magenta, Color.Cyan)
        }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                *expandingItem(size = size),
                ExpectedItem(Color.Yellow, mainAxis = size),
                ExpectedItem(Color.Magenta, mainAxis = size + ItemMainAxisSize),
            )
        }
    }

    @Test
    fun firstVisibleItemReplaced_itemComposedBeforeTheViewportStaysWhereItWas() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow, Color.Magenta))
        var blackPosition = Int.MIN_VALUE
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2, beyondBoundsItemCount = 1) {
                items(list, key = { it.toArgb() }) {
                    if (it == StartColor) {
                        ExpandingItem(enter = expandMainAxis())
                    } else {
                        Item(
                            it,
                            modifier =
                                if (it == Color.Black) {
                                    Modifier.onPlaced { coordinates ->
                                        val position =
                                            coordinates.parentLayoutCoordinates!!.localPositionOf(
                                                coordinates
                                            )
                                        blackPosition =
                                            (if (vertical) position.y else position.x).roundToInt()
                                    }
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
            }
        }
        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize * 1.5f) } }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Yellow, Color.Magenta) }

        onAnimationFrame {
            rule.runOnIdle { assertThat(blackPosition).isEqualTo(-ItemMainAxisSize * 3 / 2) }
        }
    }

    @Test
    fun itemPushedOut_thatIsAlsoComposedBeyondTheViewport_staysUntilItIsPushedOut() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2, beyondBoundsItemCount = 1) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
        }
    }

    @Test
    fun inLookaheadScope_itemPushedOutByLargerItemThatReplacesAnother_keepsItsAnimation() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        if (it == Color.Yellow) {
                            Box(
                                Modifier.animateItemWithTransitions(
                                        enterTransition = expandMainAxis(),
                                        exitTransition = ExitTransition.None,
                                        placementSpec = null,
                                    )
                                    .background(it)
                                    .requiredMainAxisSize((ItemMainAxisSize * 2).toDp())
                                    .requiredCrossAxisSize(ItemCrossAxisSize.toDp())
                            )
                        } else {
                            Item(it, enter = fadeIn(FadeSpec), exit = ExitTransition.None)
                        }
                    }
                }
            }
        }
        val animation = rule.runOnIdle { state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0) }
        assertThat(animation).isNotNull()

        rule.runOnUiThread { list = listOf(Color.Yellow, Color.Blue) }

        onAnimationStep(steps = Steps - 1) { step ->
            val size = interpolate(0, ItemMainAxisSize * 2, step / Steps.toFloat())
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Yellow, mainAxisSize = size),
                ExpectedItem(Color.Blue, mainAxis = size),
            )
            rule.runOnIdle {
                assertThat(state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0))
                    .isSameInstanceAs(animation)
            }
        }
    }

    @Test
    fun inLookaheadScope_itemPushedOutOfViewport_isDisposedAfterLookaheadPassRanAgain() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        val composed = mutableSetOf<Color>()
        rule.setContent {
            LookaheadScope {
                LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                    items(list, key = { it.toArgb() }) {
                        DisposableEffect(it) {
                            composed += it
                            onDispose { composed -= it }
                        }
                        if (it == StartColor) {
                            ExpandingItem(enter = expandMainAxis())
                        } else {
                            Item(it, enter = fadeIn(FadeSpec))
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow) }
        onAnimationStep(steps = 1) {}
        rule.runOnUiThread {
            list = listOf(Color.Black, StartColor, Color.Blue, Color.Yellow, Color.Magenta)
        }
        onAnimationStep(steps = Steps + 2) {}

        rule.runOnIdle {
            assertThat(composed).containsExactly(Color.Black, StartColor)
            assertThat(state.itemAnimator.getAnimation(Color.Blue.toArgb(), 0)).isNull()
        }
    }

    @Test
    fun firstVisibleItemReplaced_pinnedItemBeforeTheViewportStaysWhereItWas() {
        val colors =
            listOf(Color.Black, Color.Gray, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)
        var list by mutableStateOf(colors)
        var blackPosition = Int.MIN_VALUE
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    when (it) {
                        StartColor -> ExpandingItem(enter = expandMainAxis())
                        Color.Black -> {
                            PinItem()
                            Item(
                                it,
                                modifier =
                                    Modifier.onMainAxisPositionInList { position ->
                                        blackPosition = position
                                    },
                            )
                        }
                        else -> Item(it)
                    }
                }
            }
        }
        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize * 2.5f) } }

        rule.runOnUiThread { list = colors.map { if (it == Color.Blue) StartColor else it } }

        onAnimationFrame {
            rule.runOnIdle { assertThat(blackPosition).isEqualTo(-ItemMainAxisSize * 3 / 2) }
        }
    }

    @Test
    fun pinnedItemAfterTheViewport_staysBeyondIt() {
        val containerSize = ItemMainAxisSize * 2
        val colors = listOf(Color.Black, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)
        var list by mutableStateOf(colors)
        var cyanPosition = Int.MIN_VALUE
        rule.setContent {
            LazyList(containerMainAxisSize = containerSize) {
                items(list, key = { it.toArgb() }) {
                    when (it) {
                        StartColor -> ExpandingItem(enter = expandMainAxis())
                        Color.Cyan -> {
                            PinItem()
                            Item(
                                it,
                                modifier =
                                    Modifier.onMainAxisPositionInList { position ->
                                        cyanPosition = position
                                    },
                            )
                        }
                        else -> Item(it)
                    }
                }
            }
        }
        rule.runOnIdle { runBlocking { state.scrollToItem(colors.lastIndex) } }
        rule.runOnIdle { runBlocking { state.scrollToItem(0) } }
        rule.runOnIdle { assertThat(cyanPosition).isEqualTo(containerSize) }

        rule.runOnUiThread { list = colors.take(1) + StartColor + colors.drop(1) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = containerSize,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
            rule.runOnIdle { assertThat(cyanPosition).isAtLeast(containerSize) }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun expandMainAxis_wrapContentContainerGrows() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = null) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor) }

        onAnimationFrame { fraction ->
            val containerSize = ItemMainAxisSize + interpolate(0, ItemMainAxisSize, fraction)
            rule
                .onNodeWithTag(ContainerTag)
                .assertMainAxisSizeIsEqualTo(containerSize.toDp(), tolerance = PixelTolerance)
            assertItems(
                containerMainAxisSize = containerSize,
                ExpectedItem(Color.Black),
                *expandingItem(
                    mainAxis = ItemMainAxisSize,
                    size = containerSize - ItemMainAxisSize,
                ),
            )
        }
    }

    @Test
    fun expandCrossAxis_wrapContentContainerGrows() {
        val otherItemCrossAxisSize = ItemCrossAxisSize / 2
        var list by mutableStateOf(listOf(Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2, containerCrossAxisSize = null) {
                items(list, key = { it.toArgb() }) {
                    if (it == Color.Black) {
                        Item(it, enter = expandCrossAxis())
                    } else {
                        Item(it, crossAxisSize = otherItemCrossAxisSize)
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Blue, Color.Black) }

        onAnimationFrame { fraction ->
            val itemCrossAxisSize = interpolate(0, ItemCrossAxisSize, fraction)
            val containerCrossAxisSize = maxOf(otherItemCrossAxisSize, itemCrossAxisSize)
            rule
                .onNodeWithTag(ContainerTag)
                .assertCrossAxisSizeIsEqualTo(
                    containerCrossAxisSize.toDp(),
                    tolerance = PixelTolerance,
                )
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Blue, crossAxisSize = otherItemCrossAxisSize),
                ExpectedItem(
                    Color.Black,
                    mainAxis = ItemMainAxisSize,
                    crossAxisSize = itemCrossAxisSize,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Test
    fun reverseLayout_followingItemsArePushedTowardsTheStart() {
        val containerSize = ItemMainAxisSize * 2
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = containerSize, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = containerSize,
                *expandingItem(mainAxis = containerSize - size, size = size),
                ExpectedItem(Color.Black, mainAxis = containerSize - size - ItemMainAxisSize),
            )
        }
    }

    @Test
    fun contentPadding_expandIsRelativeToPaddedItemPosition() {
        val padding = ItemMainAxisSize / 2
        val containerMainAxisSize = ItemMainAxisSize * 2 + padding * 2
        val containerCrossAxisSize = ItemCrossAxisSize + padding * 2
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(
                containerMainAxisSize = containerMainAxisSize,
                containerCrossAxisSize = containerCrossAxisSize,
                contentPadding =
                    with(rule.density) {
                        PaddingValues(mainAxis = padding.toDp(), crossAxis = padding.toDp())
                    },
            ) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = containerMainAxisSize,
                *expandingItem(mainAxis = padding, size = size, crossAxis = padding),
                ExpectedItem(Color.Black, mainAxis = padding + size, crossAxis = padding),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Test
    fun expandMainAxis_belowStickingHeader() {
        assertItemExpandsBelowStickingHeader(isInLookaheadScope = false)
    }

    @Test
    fun inLookaheadScope_expandMainAxis_belowStickingHeader() {
        assertItemExpandsBelowStickingHeader(isInLookaheadScope = true)
    }

    private fun assertItemExpandsBelowStickingHeader(isInLookaheadScope: Boolean) {
        val halfSize = ItemMainAxisSize / 2
        var list by mutableStateOf(listOf(Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan))
        setContent(isInLookaheadScope) {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                stickyHeader(key = "header") { Item(Color.Black) }
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }
        rule.runOnIdle { runBlocking { state.scrollBy(halfSize.toFloat()) } }

        rule.runOnUiThread {
            list = listOf(Color.Blue, StartColor, Color.Yellow, Color.Magenta, Color.Cyan)
        }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                ExpectedItem(Color.Blue, mainAxis = halfSize),
                *expandingItem(mainAxis = halfSize + ItemMainAxisSize, size = size),
                ExpectedItem(Color.Yellow, mainAxis = halfSize + ItemMainAxisSize + size),
                ExpectedItem(Color.Magenta, mainAxis = halfSize + ItemMainAxisSize * 2 + size),
                ExpectedItem(Color.Black),
            )
        }
    }

    @Test
    fun centerArrangement_itemsAreArrangedWithTheirAnimatedSizes() {
        val containerSize = ItemMainAxisSize * 4
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            val modifier =
                Modifier.requiredMainAxisSize(containerSize.toDp())
                    .requiredCrossAxisSize(ItemCrossAxisSize.toDp())
                    .background(Color.White)
                    .testTag(ContainerTag)
            val content: LazyListScope.() -> Unit = {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
            if (vertical) {
                LazyColumn(modifier, verticalArrangement = Arrangement.Center, content = content)
            } else {
                LazyRow(modifier, horizontalArrangement = Arrangement.Center, content = content)
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            val start = (containerSize - size - ItemMainAxisSize) / 2
            assertItems(
                containerMainAxisSize = containerSize,
                *expandingItem(mainAxis = start, size = size),
                ExpectedItem(Color.Black, mainAxis = start + size),
            )
        }
    }

    @Test
    fun spacing_isNotAnimated() {
        val spacing = ItemMainAxisSize / 4
        val containerSize = ItemMainAxisSize * 3 + spacing * 2
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = containerSize, spacing = spacing.toDp()) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            val itemStart = ItemMainAxisSize + spacing
            assertItems(
                containerMainAxisSize = containerSize,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = itemStart, size = size),
                ExpectedItem(Color.Blue, mainAxis = itemStart + size + spacing),
            )
        }
    }

    @Test
    fun reverseLayout_wrapContentContainerGrows() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = null, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            val containerSize = ItemMainAxisSize + size
            rule
                .onNodeWithTag(ContainerTag)
                .assertMainAxisSizeIsEqualTo(containerSize.toDp(), tolerance = PixelTolerance)
            assertItems(
                containerMainAxisSize = containerSize,
                *expandingItem(size = size),
                ExpectedItem(Color.Black, mainAxis = size),
            )
        }
    }

    @Test
    fun expandCrossAxis_wrapContentContainer_itemsStayCentred() {
        val otherItemCrossAxisSize = ItemCrossAxisSize / 2
        var list by mutableStateOf(listOf(Color.Blue))
        rule.setContent {
            val modifier =
                Modifier.requiredMainAxisSize((ItemMainAxisSize * 2).toDp())
                    .background(Color.White)
                    .testTag(ContainerTag)
            val content: LazyListScope.() -> Unit = {
                items(list, key = { it.toArgb() }) {
                    if (it == Color.Black) {
                        Item(it, enter = expandCrossAxis())
                    } else {
                        Item(it, crossAxisSize = otherItemCrossAxisSize)
                    }
                }
            }
            if (vertical) {
                LazyColumn(
                    modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            } else {
                LazyRow(modifier, verticalAlignment = Alignment.CenterVertically, content = content)
            }
        }

        rule.runOnUiThread { list = listOf(Color.Blue, Color.Black) }

        onAnimationFrame { fraction ->
            val itemCrossAxisSize = interpolate(0, ItemCrossAxisSize, fraction)
            val containerCrossAxisSize = maxOf(otherItemCrossAxisSize, itemCrossAxisSize)
            rule
                .onNodeWithTag(ContainerTag)
                .assertCrossAxisSizeIsEqualTo(
                    containerCrossAxisSize.toDp(),
                    tolerance = PixelTolerance,
                )
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(
                    Color.Blue,
                    crossAxis = (containerCrossAxisSize - otherItemCrossAxisSize) / 2,
                    crossAxisSize = otherItemCrossAxisSize,
                ),
                ExpectedItem(
                    Color.Black,
                    mainAxis = ItemMainAxisSize,
                    crossAxis = (containerCrossAxisSize - itemCrossAxisSize) / 2,
                    crossAxisSize = itemCrossAxisSize,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Test
    fun wrapContentContainer_wrapsItemsThatAreBeingPushedOut() {
        var list by mutableStateOf(listOf(Color.Black, Color.Blue, Color.Yellow))
        val maxSize = ItemMainAxisSize * 2
        rule.setContent {
            Box(Modifier.requiredMainAxisSize(maxSize.toDp()), propagateMinConstraints = false) {
                LazyList(containerMainAxisSize = null) {
                    items(list, key = { it.toArgb() }) {
                        Item(it, enter = expandMainAxis(), exit = ExitTransition.None)
                    }
                }
            }
        }

        rule.runOnUiThread {
            list = listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow)
        }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            rule
                .onNodeWithTag(ContainerTag)
                .assertMainAxisSizeIsEqualTo(maxSize.toDp(), tolerance = PixelTolerance)
            assertItems(
                containerMainAxisSize = maxSize,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, mainAxis = ItemMainAxisSize, mainAxisSize = size),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize + size, mainAxisSize = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size * 2),
            )
        }
    }

    @Test
    fun rtl_expandMainAxis_followingItemsArePushedTowardsTheLeft() {
        assumeTrue(!vertical)
        val containerSize = ItemMainAxisSize * 2
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(containerMainAxisSize = containerSize) {
                    items(list, key = { it.toArgb() }) {
                        ItemOrExpandingItem(it, expandMainAxis())
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            val halfSize = ItemMainAxisSize / 2
            val boundsStart = containerSize - size
            assertItems(
                containerMainAxisSize = containerSize,
                ExpectedItem(
                    StartColor,
                    mainAxis = maxOf(boundsStart, containerSize - halfSize),
                    mainAxisSize = minOf(size, halfSize),
                ),
                ExpectedItem(
                    EndColor,
                    mainAxis = boundsStart,
                    mainAxisSize = maxOf(size - halfSize, 0),
                ),
                ExpectedItem(Color.Black, mainAxis = boundsStart - ItemMainAxisSize),
            )
        }
    }

    @Test
    fun expandMainAxis_belowStickyHeader() {
        var list by mutableStateOf(listOf(Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                stickyHeader(key = "header") { Item(Color.Black) }
                items(list, key = { it.toArgb() }) { ItemOrExpandingItem(it, expandMainAxis()) }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Blue) }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                ExpectedItem(Color.Black),
                *expandingItem(mainAxis = ItemMainAxisSize, size = size),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize + size),
            )
        }
    }

    @Test
    fun stickingHeaderAdded_expands() {
        var hasHeader by mutableStateOf(false)
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 3) {
                if (hasHeader) {
                    stickyHeader(key = "header") { ExpandingItem(enter = expandMainAxis()) }
                }
                items(listOf(Color.Black, Color.Blue), key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { hasHeader = true }

        onAnimationFrame { fraction ->
            val size = interpolate(0, ItemMainAxisSize, fraction)
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 3,
                *expandingItem(size = size),
                ExpectedItem(Color.Black, mainAxis = size),
                ExpectedItem(Color.Blue, mainAxis = size + ItemMainAxisSize),
            )
        }
    }

    @Test
    fun rtl_wrapContentContainer_itemRemovedWhileAnotherExpands_disappearsWhereItWas() {
        assumeTrue(!vertical)
        var list by mutableStateOf(listOf(Color.Black, Color.Blue))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(containerMainAxisSize = null) {
                    items(list, key = { it.toArgb() }) {
                        when (it) {
                            StartColor -> ExpandingItem(enter = expandMainAxis())
                            Color.Blue -> Item(it, exit = fadeOut(SlowFadeSpec))
                            else -> Item(it)
                        }
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, StartColor, Color.Blue) }
        onAnimationStep(steps = Steps / 2) {}
        assertThat(mainAxisRangeOf(Color.Blue)!!.first).isEqualTo(0)

        rule.runOnUiThread { list = listOf(Color.Black, StartColor) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        assertThat(mainAxisRangeOf(Color.Blue)!!.first).isEqualTo(0)
    }

    @Test
    fun expandWithOvershootingSpring_wrapContentContainerWithCenterArrangement_wrapsItems() {
        val maxSize = ItemMainAxisSize * 4
        var list by mutableStateOf(listOf(Color.Black))
        val overshootingSpec: FiniteAnimationSpec<IntSize> =
            spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium)
        rule.setContent {
            Box(Modifier.requiredMainAxisSize(maxSize.toDp()), propagateMinConstraints = false) {
                val modifier =
                    Modifier.requiredCrossAxisSize(ItemCrossAxisSize.toDp())
                        .background(Color.White)
                        .testTag(ContainerTag)
                val content: LazyListScope.() -> Unit = {
                    items(list, key = { it.toArgb() }) {
                        ItemOrExpandingItem(it, expandMainAxis(animationSpec = overshootingSpec))
                    }
                }
                if (vertical) {
                    LazyColumn(
                        modifier,
                        verticalArrangement = Arrangement.Center,
                        content = content,
                    )
                } else {
                    LazyRow(modifier, horizontalArrangement = Arrangement.Center, content = content)
                }
            }
        }

        rule.runOnUiThread { list = listOf(StartColor, Color.Black) }

        var maxContainerSize = 0
        var containerSize = 0
        onAnimationStep(steps = 80) {
            val image = rule.onNodeWithTag(ContainerTag).captureToImage()
            containerSize = if (vertical) image.height else image.width
            maxContainerSize = maxOf(maxContainerSize, containerSize)
            val blackRange = mainAxisRangeOf(Color.Black)!!
            assertThat(blackRange.last + 1).isEqualTo(containerSize)
            assertThat(blackRange.first).isEqualTo(containerSize - ItemMainAxisSize)
            val contentEnd = mainAxisRangeOf(EndColor)?.last
            if (contentEnd != null) assertThat(blackRange.first).isEqualTo(contentEnd + 1)
        }
        assertThat(maxContainerSize).isGreaterThan(ItemMainAxisSize * 2)
        assertThat(containerSize).isEqualTo(ItemMainAxisSize * 2)
    }

    private var config = ListConfig()

    private val animatedState = LazyListState()
    private val plainState = LazyListState()

    private val composedKeys = mutableSetOf<Int>()

    private val plainComposedKeys = mutableSetOf<Int>()

    private val placedPositions = mutableMapOf<Int, Int>()

    private var items by mutableStateOf(emptyList<Int>())

    @Test
    fun manyItemsAddedToEmptyList() {
        setScrollingContent(initialItems = emptyList())

        changeItems { NewItems }

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun manyItemsAddedInFullList_allAddedItemsExpand() {
        setScrollingContent(initialItems = List(20) { it })
        val keysBefore = rule.runOnIdle { visibleItems().map { it.key } }

        changeItems { it.take(1) + NewItems + it.drop(1) }

        var isFirstFrame = true
        onEachFrame {
            assertLayout()
            if (isFirstFrame) {
                val addedItems = visibleItems().filter { (it.key as Int) >= FirstNewItem }
                assertThat(addedItems).isNotEmpty()
                assertWithMessage("Added items that are not expanding")
                    .that(addedItems.filter { it.size == ScrollingItemSize }.map { it.key })
                    .isEmpty()
                if (config.spacing <= 0) {
                    assertWithMessage("Added items")
                        .that(addedItems.map { it.key })
                        .isEqualTo(NewItems)
                    assertWithMessage("Existing items")
                        .that(visibleItems().map { it.key })
                        .containsAtLeastElementsIn(keysBefore)
                }
                isFirstFrame = false
            }
        }
        assertAtRest()
    }

    @Test
    fun manyItemsAddedBeforeShortList_addedItemsFillTheViewport() {
        setScrollingContent(initialItems = listOf(0, 1))

        changeItems { NewItems + it }

        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = false)
    }

    @Test
    fun manyItemsAddedAfterShortList() {
        setScrollingContent(initialItems = listOf(0, 1))

        changeItems { it + NewItems }

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun allItemsReplaced_listScrollsBackWhileNewItemsAreTooSmallToFillIt() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 3 + ScrollingItemSize / 4)

        changeItems { NewItems }

        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = config.spacing > 0)
        if (config.spacing <= 0) {
            rule.runOnIdle {
                assertThat(animatedState.firstVisibleItemIndex).isEqualTo(0)
                assertThat(animatedState.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }
    }

    @Test
    fun itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        setScrollingContent(initialItems = listOf(0, 1))
        val offsetBefore = rule.runOnIdle { visibleItem(0)!!.offset }

        changeItems { listOf(100, 101, 102) + it }

        var previousOffset = offsetBefore
        var isFirstFrame = true
        onEachFrame {
            assertLayout()
            val offset = visibleItem(0)!!.offset
            if (isFirstFrame && config.spacing == 0) {
                assertWithMessage("Offset of the first existing item in the first frame")
                    .that(offset)
                    .isEqualTo(offsetBefore)
            }
            isFirstFrame = false
            assertWithMessage("Offset of the first existing item")
                .that(offset)
                .isAtLeast(previousOffset)
            previousOffset = offset
        }
        assertWithMessage("Offset the first existing item was pushed to")
            .that(previousOffset)
            .isGreaterThan(offsetBefore)
        assertAtRest(isSameAsPlainList = fitsInViewport(itemCount = 5))
    }

    @Test
    fun itemsAddedBeforeAndAfterShortList_existingItemIsOnlyPushedAway() {
        setScrollingContent(initialItems = listOf(0))
        val offsetBefore = rule.runOnIdle { visibleItem(0)!!.offset }

        changeItems { listOf(100, 101) + it + listOf(103, 104) }

        var previousOffset = offsetBefore
        onEachFrame {
            assertLayout()
            val offset = visibleItem(0)!!.offset
            assertWithMessage("Offset of the existing item").that(offset).isAtLeast(previousOffset)
            previousOffset = offset
        }
        assertAtRest(isSameAsPlainList = fitsInViewport(itemCount = 5))
    }

    @Test
    fun itemsAddedInFullList_followingItemsStayUntilTheyArePushedOut() {
        setScrollingContent(initialItems = List(20) { it })
        val keysBefore = rule.runOnIdle { visibleItems().map { it.key } }

        changeItems { it.take(2) + 100 + 101 + it.drop(2) }

        var isFirstFrame = true
        onEachFrame {
            assertLayout()
            val visibleKeys = visibleItems().map { it.key }
            assertThat(visibleKeys).containsAtLeast(100, 101)
            val existingKeys = visibleKeys.filter { (it as Int) < FirstNewItem }
            assertWithMessage("Existing items that are visible")
                .that(existingKeys)
                .isEqualTo(keysBefore.take(existingKeys.size))
            if (isFirstFrame) {
                assertWithMessage("Existing items that are visible in the first frame")
                    .that(existingKeys)
                    .isEqualTo(keysBefore)
                isFirstFrame = false
            }
        }
        assertAtRest()
    }

    @Test
    fun itemAddedBeforeShortList() {
        setScrollingContent(initialItems = listOf(0, 1))

        changeItems { listOf(100) + it }

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun itemsAddedBeforeListThatThenOverflows_addedItemsFillTheViewport() {
        setScrollingContent(initialItems = listOf(0, 1, 2))

        changeItems { listOf(100, 101, 102, 103) + it }

        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = false)
        rule.runOnIdle {
            assertThat(animatedState.firstVisibleItemIndex).isEqualTo(0)
            assertThat(animatedState.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun itemAddedAfterShortList() {
        setScrollingContent(initialItems = listOf(0, 1))

        changeItems { it + 100 }

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun itemAddedInScrolledList() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 3 + ScrollingItemSize / 4)

        changeItems { it.take(5) + 100 + it.drop(5) }

        var wasExpanding = false
        onEachFrame {
            assertLayout()
            if (visibleItem(100)!!.size in 1 until ScrollingItemSize) wasExpanding = true
        }
        assertThat(wasExpanding).isTrue()
        assertAtRest()
    }

    @Test
    fun firstVisibleItemReplacedInScrolledList_newItemHasItsFullSize() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 3 + ScrollingItemSize / 4)
        val firstKey = rule.runOnIdle { items[animatedState.firstVisibleItemIndex] }

        changeItems { items -> items.map { if (it == firstKey) 100 else it } }

        onEachFrame {
            assertLayout()
            assertSameLayoutInfoAsPlainList()
        }
        assertAtRest()
    }

    @Test
    fun firstVisibleItemReplacedInListScrolledToItsStart_newItemExpands() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy((ScrollingItemSize + config.spacing) * 3)

        changeItems { items -> items.map { if (it == 3) 100 else it } }

        var wasExpanding = false
        onEachFrame {
            assertLayout()
            val item = visibleItem(100)
            assertWithMessage("The new first item should be laid out").that(item).isNotNull()
            if (item!!.size in 1 until ScrollingItemSize) wasExpanding = true
        }
        assertThat(wasExpanding).isTrue()
        assertAtRest()
    }

    @Test
    fun itemAddedAtEndOfListScrolledToEnd() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 20)

        changeItems { it + 100 }

        onEachFrame {
            assertLayout()
            assertSameLayoutInfoAsPlainList()
        }
        assertAtRest()
    }

    @Test
    fun itemAddedInsideViewportOfListScrolledToEnd_newItemHasItsFullSize() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 20)

        changeItems { it.dropLast(2) + 100 + it.takeLast(2) }

        onEachFrame {
            assertLayout()
            assertSameLayoutInfoAsPlainList()
        }
    }

    @Test
    fun itemAddedInsideViewportOfListOnePixelBeforeItsEnd_newItemExpands() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 20)
        scrollBothBy(-1)

        changeItems { it.dropLast(2) + 100 + it.takeLast(2) }

        var wasExpanding = false
        onEachFrame {
            assertLayout()
            if (visibleItem(100)!!.size in 1 until ScrollingItemSize) wasExpanding = true
        }
        assertThat(wasExpanding).isTrue()
        assertAtRest()
    }

    @Test
    fun itemsRemovedAndItemAddedAtEndOfListScrolledToEnd_newItemHasItsFullSize() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 20)

        changeItems { it.dropLast(2) + 100 }

        onEachFrame {
            assertLayout()
            assertSameLayoutInfoAsPlainList()
        }
        assertAtRest()
    }

    @Test
    fun lastItemReplacedInListScrolledToEnd_newItemHasItsFullSize() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 20)

        changeItems { it.dropLast(1) + 100 }

        onEachFrame {
            assertLayout()
            assertSameLayoutInfoAsPlainList()
        }
        assertAtRest()
    }

    @Test
    fun itemsChangedAgainWhileExpanding() {
        setScrollingContent(initialItems = List(20) { it })

        changeItems { it.take(2) + 100 + it.drop(2) }
        advanceToMiddleOfExpand { assertLayout() }
        changeItems { items -> listOf(101) + items.filter { it != 1 } + 102 }

        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = config.contentPadding == 0 && config.spacing >= 0)
    }

    @Test
    fun itemsChangedAgainAfterExpanding() {
        setScrollingContent(initialItems = listOf(0, 1))

        changeItems { listOf(100) + it }
        onEachFrame { assertLayout() }
        assertAtRest()
        changeItems { listOf(101, 102) + it }

        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = fitsInViewport(itemCount = 5))
    }

    @Test
    fun expandingItemRemoved() {
        setScrollingContent(initialItems = List(20) { it })

        changeItems { it.take(2) + 100 + it.drop(2) }
        advanceToMiddleOfExpand { assertLayout() }
        changeItems { items -> items.filter { it != 100 } }

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun scrollingForwardWhileExpanding_itemsReachedByScrollingHaveTheirFullSize() {
        setScrollingContent(initialItems = List(20) { it })
        changeItems { it.take(1) + NewItems + it.drop(1) }
        advanceToMiddleOfExpand { assertLayout() }
        val keysBefore = rule.runOnIdle { visibleItems().map { it.key } }

        scrollBothBy(ScrollingItemSize * 2, clearComposedKeys = false)

        if (!config.usesPlacementSpec) {
            rule.runOnIdle {
                val reached = visibleItems().filter { it.key !in keysBefore }
                reached.forEach {
                    assertWithMessage("Size of item ${it.key}")
                        .that(it.size)
                        .isEqualTo(ScrollingItemSize)
                }
            }
        }
        onEachFrame { assertLayout() }
        assertAtRest(isSameAsPlainList = false)
    }

    @Test
    fun scrollingBackwardWhileExpanding() {
        setScrollingContent(initialItems = List(20) { it })
        scrollBothBy(ScrollingItemSize * 3 + ScrollingItemSize / 4)
        changeItems { it.take(5) + 100 + 101 + it.drop(5) }
        advanceToMiddleOfExpand { assertLayout() }

        scrollBothBy(-(ScrollingItemSize + ScrollingItemSize / 2), clearComposedKeys = false)

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun scrollingToItemWhileExpanding() {
        setScrollingContent(initialItems = List(20) { it })
        changeItems { it.take(1) + NewItems + it.drop(1) }
        advanceToMiddleOfExpand { assertLayout() }

        rule.runOnUiThread {
            runBlocking {
                animatedState.scrollToItem(10, ScrollingItemSize / 4)
                plainState.scrollToItem(10, ScrollingItemSize / 4)
            }
        }
        rule.mainClock.advanceTimeByFrame()

        onEachFrame { assertLayout() }
        assertAtRest()
    }

    @Test
    fun itemAddedWhileItemAnimationsAreSkipped_appearsAtItsFullSize() {
        setScrollingContent(initialItems = List(20) { it })

        rule.runOnUiThread { animatedState.skipItemPlacementAnimation = true }
        changeItems { it.take(2) + 100 + it.drop(2) }

        onEachFrame {
            assertSameLayoutInfoAsPlainList()
            assertThat(visibleItem(100)!!.size).isEqualTo(ScrollingItemSize)
        }
    }

    @Test
    fun inLookaheadScope_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(isInLookaheadScope = true)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun inLookaheadScope_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(isInLookaheadScope = true)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun inLookaheadScope_itemAddedInScrolledList() {
        config = ListConfig(isInLookaheadScope = true)
        itemAddedInScrolledList()
    }

    @Test
    fun inLookaheadScope_itemsChangedAgainWhileExpanding() {
        config = ListConfig(isInLookaheadScope = true)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun inLookaheadScope_scrollingBackwardWhileExpanding() {
        config = ListConfig(isInLookaheadScope = true)
        scrollingBackwardWhileExpanding()
    }

    @Test
    fun reverseLayout_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(reverseLayout = true)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun reverseLayout_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(reverseLayout = true)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun reverseLayout_itemAddedInScrolledList() {
        config = ListConfig(reverseLayout = true)
        itemAddedInScrolledList()
    }

    @Test
    fun reverseLayout_itemsChangedAgainWhileExpanding() {
        config = ListConfig(reverseLayout = true)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun reverseLayout_scrollingBackwardWhileExpanding() {
        config = ListConfig(reverseLayout = true)
        scrollingBackwardWhileExpanding()
    }

    @Test
    fun contentPadding_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(contentPadding = ScrollingItemSize / 2)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun contentPadding_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(contentPadding = ScrollingItemSize / 2)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun contentPadding_itemAddedInScrolledList() {
        config = ListConfig(contentPadding = ScrollingItemSize / 2)
        itemAddedInScrolledList()
    }

    @Test
    fun contentPadding_itemsChangedAgainWhileExpanding() {
        config = ListConfig(contentPadding = ScrollingItemSize / 2)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun contentPadding_scrollingBackwardWhileExpanding() {
        config = ListConfig(contentPadding = ScrollingItemSize / 2)
        scrollingBackwardWhileExpanding()
    }

    @Test
    fun spacing_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(spacing = 4)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun spacing_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(spacing = 4)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun spacing_itemAddedInScrolledList() {
        config = ListConfig(spacing = 4)
        itemAddedInScrolledList()
    }

    @Test
    fun spacing_itemsChangedAgainWhileExpanding() {
        config = ListConfig(spacing = 4)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun spacing_scrollingBackwardWhileExpanding() {
        config = ListConfig(spacing = 4)
        scrollingBackwardWhileExpanding()
    }

    @Test
    fun negativeSpacing_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(spacing = -4)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun negativeSpacing_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(spacing = -4)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun negativeSpacing_itemAddedInScrolledList() {
        config = ListConfig(spacing = -4)
        itemAddedInScrolledList()
    }

    @Test
    fun negativeSpacing_itemsChangedAgainWhileExpanding() {
        config = ListConfig(spacing = -4)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun negativeSpacing_scrollingBackwardWhileExpanding() {
        config = ListConfig(spacing = -4)
        scrollingBackwardWhileExpanding()
    }

    @Test
    fun placementSpec_manyItemsAddedInFullList_allAddedItemsExpand() {
        config = ListConfig(usesPlacementSpec = true)
        manyItemsAddedInFullList_allAddedItemsExpand()
    }

    @Test
    fun placementSpec_itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway() {
        config = ListConfig(usesPlacementSpec = true)
        itemsAddedBeforeShortList_existingItemsAreOnlyPushedAway()
    }

    @Test
    fun placementSpec_itemAddedInScrolledList() {
        config = ListConfig(usesPlacementSpec = true)
        itemAddedInScrolledList()
    }

    @Test
    fun placementSpec_itemsChangedAgainWhileExpanding() {
        config = ListConfig(usesPlacementSpec = true)
        itemsChangedAgainWhileExpanding()
    }

    @Test
    fun placementSpec_scrollingBackwardWhileExpanding() {
        config = ListConfig(usesPlacementSpec = true)
        scrollingBackwardWhileExpanding()
    }

    private fun setScrollingContent(initialItems: List<Int>) {
        items = initialItems
        rule.setContent {
            val content =
                @Composable {
                    Column {
                        TestList(animatedState, isAnimated = true)
                        TestList(plainState, isAnimated = false)
                    }
                }
            if (config.isInLookaheadScope) LookaheadScope { content() } else content()
        }
        rule.runOnIdle { clearComposedKeys() }
    }

    private fun clearComposedKeys() {
        composedKeys.clear()
        plainComposedKeys.clear()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TestList(state: LazyListState, isAnimated: Boolean) {
        val density = rule.density
        LazyColumnOrRow(
            state = state,
            reverseLayout = config.reverseLayout,
            contentPadding =
                with(density) { PaddingValues(mainAxis = config.contentPadding.toDp()) },
            spacedBy = with(density) { config.spacing.toDp() },
            cacheWindow =
                LazyLayoutCacheWindow(
                    behindFraction = 0f,
                    aheadFraction = 0f,
                    isNonScrollCachingEnabled = false,
                ),
            modifier =
                Modifier.requiredMainAxisSize(with(density) { ScrollingViewportSize.toDp() }),
        ) {
            items(items, key = { it }) { key ->
                SideEffect { (if (isAnimated) composedKeys else plainComposedKeys).add(key) }
                Box(
                    Modifier.then(
                            if (isAnimated) {
                                Modifier.onPlaced {
                                        val position =
                                            it.parentLayoutCoordinates!!.localPositionOf(it)
                                        placedPositions[key] =
                                            (if (vertical) position.y else position.x).roundToInt()
                                    }
                                    .animateItemWithTransitions(
                                        enterTransition = expandMainAxis(ExpandSpec),
                                        exitTransition = null,
                                        placementSpec =
                                            if (config.usesPlacementSpec) SlowPlacementSpec
                                            else null,
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .requiredMainAxisSize(with(density) { ScrollingItemSize.toDp() })
                        .requiredCrossAxisSize(with(density) { ScrollingItemSize.toDp() })
                )
            }
        }
    }

    private fun changeItems(change: (List<Int>) -> List<Int>) {
        rule.runOnUiThread {
            clearComposedKeys()
            items = change(items)
        }
    }

    private fun scrollBothBy(distance: Int, clearComposedKeys: Boolean = true) {
        rule.runOnUiThread {
            runBlocking {
                animatedState.scrollBy(distance.toFloat())
                plainState.scrollBy(distance.toFloat())
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { if (clearComposedKeys) clearComposedKeys() }
    }

    private fun onEachFrame(onFrame: () -> Unit) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        repeat(Steps * 2 + 4) {
            rule.runOnIdle(onFrame)
            rule.mainClock.advanceTimeBy(FrameDuration)
        }
    }

    private fun advanceToMiddleOfExpand(onFrame: () -> Unit) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        repeat(Steps / 2) {
            rule.runOnIdle(onFrame)
            rule.mainClock.advanceTimeBy(FrameDuration)
        }
        rule.runOnIdle(onFrame)
    }

    private fun fitsInViewport(itemCount: Int) =
        itemCount * (ScrollingItemSize + config.spacing) - config.spacing <=
            ScrollingViewportSize - config.contentPadding * 2

    private fun visibleItems(): List<LazyListItemInfo> = animatedState.layoutInfo.visibleItemsInfo

    private fun visibleItem(key: Int): LazyListItemInfo? =
        visibleItems().firstOrNull { it.key == key }

    private fun assertLayout() {
        assertVisibleItemsFollowEachOther()
        assertVisibleItemsFillViewport()
        if (!config.usesPlacementSpec) assertItemsArePlacedWhereTheyAreLaidOut()
    }

    private fun assertItemsArePlacedWhereTheyAreLaidOut() {
        visibleItems().forEach {
            val boundsStart =
                if (config.reverseLayout) {
                    ScrollingViewportSize - config.contentPadding - it.offset - it.size
                } else {
                    config.contentPadding + it.offset
                }
            assertWithMessage("Position of item ${it.key}")
                .that(placedPositions[it.key])
                .isEqualTo(boundsStart + it.size - ScrollingItemSize)
        }
    }

    private fun assertVisibleItemsFillViewport() {
        val visible = visibleItems()
        val layoutInfo = animatedState.layoutInfo
        val last = visible.last()
        if (last.index < items.lastIndex) {
            assertWithMessage("End of the visible items, the last of which has key ${last.key}")
                .that(last.offset + last.size + config.spacing)
                .isAtLeast(layoutInfo.viewportEndOffset)
        }
        val first = visible.first()
        if (first.index > 0) {
            assertWithMessage("Start of the visible items, the first of which has key ${first.key}")
                .that(first.offset)
                .isAtMost(layoutInfo.viewportStartOffset)
        }
    }

    private fun assertSameLayoutInfoAsPlainList() {
        val animated = animatedState.layoutInfo
        val plain = plainState.layoutInfo
        assertWithMessage("Visible items")
            .that(animated.visibleItemsInfo.map { it.describe() })
            .isEqualTo(plain.visibleItemsInfo.map { it.describe() })
        assertWithMessage("First visible item index")
            .that(animatedState.firstVisibleItemIndex)
            .isEqualTo(plainState.firstVisibleItemIndex)
        assertWithMessage("First visible item scroll offset")
            .that(animatedState.firstVisibleItemScrollOffset)
            .isEqualTo(plainState.firstVisibleItemScrollOffset)
        assertWithMessage("Can scroll forward")
            .that(animatedState.canScrollForward)
            .isEqualTo(plainState.canScrollForward)
        assertWithMessage("Can scroll backward")
            .that(animatedState.canScrollBackward)
            .isEqualTo(plainState.canScrollBackward)
    }

    private fun assertVisibleItemsFollowEachOther() {
        val visible = visibleItems()
        assertThat(visible).isNotEmpty()
        visible.forEach {
            val minSize = if (it.index == items.lastIndex) 0 else maxOf(-config.spacing, 0)
            assertWithMessage("Size of item ${it.key}")
                .that(it.size)
                .isIn(minSize..ScrollingItemSize)
        }
        visible.zipWithNext { item, next ->
            assertWithMessage("Index of the item after item ${item.key}")
                .that(next.index)
                .isEqualTo(item.index + 1)
            assertWithMessage("Offset of item ${next.key}, after item ${item.key}")
                .that(next.offset)
                .isEqualTo(item.offset + item.size + config.spacing)
        }
    }

    private fun assertAtRest(isSameAsPlainList: Boolean = true) {
        if (config.usesPlacementSpec) {
            repeat(Steps * 4) { rule.mainClock.advanceTimeBy(FrameDuration) }
        }
        rule.runOnIdle {
            assertVisibleItemsFollowEachOther()
            assertVisibleItemsFillViewport()
            assertItemsArePlacedWhereTheyAreLaidOut()
            visibleItems().forEach {
                assertWithMessage("Size of item ${it.key}")
                    .that(it.size)
                    .isEqualTo(ScrollingItemSize)
            }
            if (isSameAsPlainList) assertSameLayoutInfoAsPlainList()
            val visibleKeys = visibleItems().map { it.key }
            items.forEach { key ->
                val animation = animatedState.itemAnimator.getAnimation(key, 0)
                if (key in visibleKeys) {
                    assertWithMessage("Animation of visible item $key").that(animation).isNotNull()
                } else {
                    assertWithMessage("Animation of item $key").that(animation).isNull()
                }
            }
        }
    }

    private fun LazyListItemInfo.describe() = "key $key, index $index at $offset, size $size"

    private fun expandCrossAxis(): EnterTransition =
        if (vertical) {
            expandHorizontally(ExpandSpec, initialWidth = { 0 })
        } else {
            expandVertically(ExpandSpec, initialHeight = { 0 })
        }

    @Composable
    private fun LazyItemScope.ItemOrExpandingItem(color: Color, expand: EnterTransition) {
        if (color == StartColor) ExpandingItem(enter = expand) else Item(color)
    }

    @Composable
    private fun LazyItemScope.ExpandingItem(
        enter: EnterTransition? = null,
        exit: ExitTransition? = null,
        modifier: Modifier = Modifier,
    ) {
        val density = rule.density
        Box(
            modifier
                .animateItemWithTransitions(
                    enterTransition = enter,
                    exitTransition = exit,
                    placementSpec = null,
                )
                .requiredMainAxisSize(with(density) { ItemMainAxisSize.toDp() })
                .requiredCrossAxisSize(with(density) { ItemCrossAxisSize.toDp() })
        ) {
            if (vertical) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().background(StartColor))
                    Box(Modifier.weight(1f).fillMaxWidth().background(EndColor))
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(StartColor))
                    Box(Modifier.weight(1f).fillMaxHeight().background(EndColor))
                }
            }
        }
    }

    private fun expandingItem(
        mainAxis: Int = 0,
        size: Int,
        alignToEnd: Boolean = true,
        clip: Boolean = true,
        crossAxis: Int = 0,
        startColor: Color = StartColor,
        endColor: Color = EndColor,
    ): Array<ExpectedItem> {
        val contentStart = if (alignToEnd) mainAxis + size - ItemMainAxisSize else mainAxis
        val halfSize = ItemMainAxisSize / 2
        fun part(color: Color, start: Int, end: Int): ExpectedItem? {
            val visibleStart = if (clip) maxOf(start, mainAxis) else start
            val visibleEnd = if (clip) minOf(end, mainAxis + size) else end
            return if (visibleEnd > visibleStart) {
                ExpectedItem(
                    color,
                    mainAxis = visibleStart,
                    crossAxis = crossAxis,
                    mainAxisSize = visibleEnd - visibleStart,
                )
            } else {
                null
            }
        }
        return listOfNotNull(
                part(startColor, contentStart, contentStart + halfSize),
                part(endColor, contentStart + halfSize, contentStart + ItemMainAxisSize),
            )
            .toTypedArray()
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    @Composable
    private fun ItemWithoutItemAnimations(color: Color, onComposed: () -> Unit) {
        remember { onComposed() }
        Box(
            Modifier.background(color)
                .requiredMainAxisSize(ItemMainAxisSize.toDp())
                .requiredCrossAxisSize(ItemCrossAxisSize.toDp())
        )
    }

    private fun setContent(isInLookaheadScope: Boolean, content: @Composable () -> Unit) {
        rule.setContent { if (isInLookaheadScope) LookaheadScope { content() } else content() }
    }

    private fun mainAxisRangeOf(color: Color): IntRange? {
        val pixels = rule.onNodeWithTag(ContainerTag).captureToImage().toPixelMap()
        val crossAxis = ItemCrossAxisSize / 2
        var first = -1
        var last = -1
        for (mainAxis in 0 until if (vertical) pixels.height else pixels.width) {
            val pixel = if (vertical) pixels[crossAxis, mainAxis] else pixels[mainAxis, crossAxis]
            if (pixel == color) {
                if (first == -1) first = mainAxis
                last = mainAxis
            }
        }
        return if (first == -1) null else first..last
    }

    private fun crossAxisRangeOf(color: Color): IntRange? {
        val pixels = rule.onNodeWithTag(ContainerTag).captureToImage().toPixelMap()
        val mainAxis = ItemMainAxisSize / 2
        var first = -1
        var last = -1
        for (crossAxis in 0 until if (vertical) pixels.width else pixels.height) {
            val pixel = if (vertical) pixels[crossAxis, mainAxis] else pixels[mainAxis, crossAxis]
            if (pixel == color) {
                if (first == -1) first = crossAxis
                last = crossAxis
            }
        }
        return if (first == -1) null else first..last
    }

    @Composable
    private fun PinItem() {
        val container = LocalPinnableContainer.current
        DisposableEffect(container) {
            val handle = container?.pin()
            onDispose { handle?.release() }
        }
    }

    private fun Modifier.onMainAxisPositionInList(onPosition: (Int) -> Unit): Modifier =
        onPlaced { coordinates ->
            val position = coordinates.parentLayoutCoordinates!!.localPositionOf(coordinates)
            onPosition((if (vertical) position.y else position.x).roundToInt())
        }

    private val PixelTolerance: Dp
        get() = with(rule.density) { 0.5f.toDp() }

    @Composable
    private fun LazyList(
        containerMainAxisSize: Int?,
        containerCrossAxisSize: Int? = ItemCrossAxisSize,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        beyondBoundsItemCount: Int? = null,
        spacing: Dp = 0.dp,
        content: LazyListScope.() -> Unit,
    ) {
        state = rememberLazyListState()
        val density = rule.density
        val modifier =
            Modifier.then(
                    if (containerMainAxisSize != null) {
                        Modifier.requiredMainAxisSize(
                            with(density) { containerMainAxisSize.toDp() }
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (containerCrossAxisSize != null) {
                        Modifier.requiredCrossAxisSize(
                            with(density) { containerCrossAxisSize.toDp() }
                        )
                    } else {
                        Modifier
                    }
                )
                .background(Color.White)
                .testTag(ContainerTag)
        if (beyondBoundsItemCount == null) {
            LazyColumnOrRow(
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                spacedBy = spacing,
                modifier = modifier,
                content = content,
            )
        } else {
            LazyColumnOrRow(
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                spacedBy = spacing,
                beyondBoundsItemCount = beyondBoundsItemCount,
                modifier = modifier,
                content = content,
            )
        }
    }

    private fun expandMainAxis(
        animationSpec: TweenSpec<IntSize> = ExpandSpec,
        fromEnd: Boolean = true,
        clip: Boolean = true,
        initialSize: (fullSize: Int) -> Int = { 0 },
    ) =
        this.expandMainAxis(
            animationSpec = animationSpec as FiniteAnimationSpec<IntSize>,
            fromEnd = fromEnd,
            clip = clip,
            initialSize = initialSize,
        )

    private val ExpandSpec = tween<IntSize>(DurationMillis, easing = LinearEasing)
    private val FadeSpec = tween<Float>(DurationMillis, easing = LinearEasing)
    private val SlowFadeSpec = tween<Float>(DurationMillis * 4, easing = LinearEasing)
    private val SlowPlacementSpec = tween<IntOffset>(DurationMillis * 2, easing = LinearEasing)

    private class ListConfig(
        val reverseLayout: Boolean = false,
        val contentPadding: Int = 0,
        val spacing: Int = 0,
        val usesPlacementSpec: Boolean = false,
        val isInLookaheadScope: Boolean = false,
    )

    companion object {
        private const val ScrollingItemSize = 20
        private const val ScrollingViewportSize = 110

        private const val FirstNewItem = 100
        private val NewItems = List(40) { FirstNewItem + it }

        private val StartColor = Color.Red
        private val EndColor = Color.Green
        private val PaddingColor = Color.Blue

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
