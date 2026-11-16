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
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.BaseLazyLayoutItemAnimationTest
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class LazyListItemSlideAnimationTest(orientation: Orientation) :
    BaseLazyLayoutItemAnimationTest(orientation) {

    private lateinit var state: LazyListState

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @Test
    fun slideInCrossAxis_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction)),
            )
        }
    }

    @Test
    fun slideInMainAxis_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInMainAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black, mainAxis = interpolate(-ItemMainAxisSize, 0, fraction)),
            )
        }
    }

    @Test
    fun slideInCrossAxis_defaultOffsetIsBasedOnEachItemSize() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        crossAxisSize =
                            if (it == Color.Red) ItemCrossAxisSize / 2 else ItemCrossAxisSize,
                        enter = slideInCrossAxis(SlideSpec),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(
                    Color.Black,
                    crossAxis = interpolate(-ItemCrossAxisSize / 2, 0, fraction),
                ),
                ExpectedItem(
                    Color.Red,
                    crossAxis = interpolate(-ItemCrossAxisSize / 4, 0, fraction),
                    mainAxis = ItemMainAxisSize,
                    crossAxisSize = ItemCrossAxisSize / 2,
                ),
            )
        }
    }

    @Test
    fun noAnimationForInitialItems() {
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(listOf(Color.Black, Color.Red), key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(Color.Red, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun oneExistsOneAdded_onlyAddedItemSlides() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(
                    Color.Red,
                    crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction),
                    mainAxis = ItemMainAxisSize,
                ),
            )
        }
    }

    @Test
    fun onlyItemWithEnterTransitionSlides() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = if (it == Color.Red) slideInCrossAxis(SlideSpec) { -it } else null,
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(
                    Color.Red,
                    crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction),
                    mainAxis = ItemMainAxisSize,
                ),
            )
        }
    }

    @Test
    fun slideOutCrossAxis_oneItemRemoved() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOutCrossAxis(SlideSpec) { it })
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = interpolate(0, ItemCrossAxisSize, fraction)),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerMainAxisSize = ItemMainAxisSize)
    }

    @Test
    fun lastItemRemoved_otherItemsAreNotAffected() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOutCrossAxis(SlideSpec) { it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                ExpectedItem(
                    Color.Red,
                    crossAxis = interpolate(0, ItemCrossAxisSize, fraction),
                    mainAxis = ItemMainAxisSize,
                ),
            )
        }
    }

    @Test
    fun exitTransitionNone_itemIsRemovedImmediately() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { Item(it, exit = ExitTransition.None) }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        assertItems(containerMainAxisSize = ItemMainAxisSize)
    }

    @Test
    fun slideOutWithZeroOffset_itemIsRemovedAfterOneFrame() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = slideOut(SlideSpec) { IntOffset.Zero })
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        assertItems(containerMainAxisSize = ItemMainAxisSize, ExpectedItem(Color.Black))
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerMainAxisSize = ItemMainAxisSize)
    }

    @Test
    fun slideInCombinedWithFadeIn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = fadeIn(FadeSpec) + slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(
                    Color.Black.copy(alpha = fraction),
                    crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction),
                ),
            )
        }
    }

    @Test
    fun slideInCombinedWithPlacementAnimationOfOtherItems() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInMainAxis(SlideSpec) { -it },
                        placementSpec = SlideSpec,
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Red, Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Red, mainAxis = interpolate(-ItemMainAxisSize, 0, fraction)),
                ExpectedItem(Color.Black, mainAxis = interpolate(0, ItemMainAxisSize, fraction)),
            )
        }
    }

    @Test
    fun reAddItemBeingSlidOut_slidesBackFromCurrentOffsetWithEnterSpec() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInCrossAxis(SlideSpec) { it },
                        exit = slideOutCrossAxis(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        val interruptedOffset = ItemCrossAxisSize / 2
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = listOf(Color.Black) }
            val x =
                if (step <= Steps / 2) {
                    interpolate(0, ItemCrossAxisSize, step / Steps.toFloat())
                } else {
                    interpolate(interruptedOffset, 0, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = x),
            )
        }
    }

    @Test
    fun removeItemBeingSlidIn_slidesOutFromCurrentOffsetWithExitSpec() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInCrossAxis(SlideSpec) { -it },
                        exit = slideOutCrossAxis(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        val interruptedOffset = -ItemCrossAxisSize / 2
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
            val x =
                if (step <= Steps / 2) {
                    interpolate(-ItemCrossAxisSize, 0, step / Steps.toFloat())
                } else {
                    interpolate(
                        interruptedOffset,
                        ItemCrossAxisSize,
                        (step - Steps / 2) / Steps.toFloat(),
                    )
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = x),
            )
        }
    }

    @Test
    fun removeItemBeingSlidIn_withoutExitTransition_itemIsRemovedImmediately() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationStep { step ->
            if (step < Steps / 2) {
                assertItems(
                    containerMainAxisSize = ItemMainAxisSize,
                    ExpectedItem(
                        Color.Black,
                        crossAxis = interpolate(-ItemCrossAxisSize, 0, step / Steps.toFloat()),
                    ),
                )
            } else {
                if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
                assertItems(containerMainAxisSize = ItemMainAxisSize)
            }
        }
    }

    @Test
    fun reAddItemAfterSlideOutFinished_slidesInFromInitialOffset() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = slideInCrossAxis(SlideSpec) { -it },
                        exit = slideOutCrossAxis(SlideSpec) { it },
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }
        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = interpolate(0, ItemCrossAxisSize, fraction)),
            )
        }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction)),
            )
        }
    }

    @Test
    fun itemAddedOutsideOfViewport_isNotAnimated() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Blue, Color.Green) }

        rule.runOnIdle { runBlocking { state.scrollBy(ItemMainAxisSize * 1.5f) } }

        onAnimationFrame {
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Red, mainAxis = -ItemMainAxisSize / 2),
                ExpectedItem(Color.Blue, mainAxis = ItemMainAxisSize / 2),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize * 3 / 2),
            )
        }
    }

    @Test
    fun scrollingDuringSlideIn_slideContinuesRelativeToItemPosition() {
        var list by mutableStateOf(listOf(Color.Black, Color.Green, Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInCrossAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red, Color.Green, Color.Blue) }

        val scrollAmount = ItemMainAxisSize / 2
        onAnimationStep { step ->
            if (step == Steps / 2) {
                rule.runOnIdle { runBlocking { state.scrollBy(scrollAmount.toFloat()) } }
            }
            val scrolled = if (step >= Steps / 2) scrollAmount else 0
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black, mainAxis = -scrolled),
                ExpectedItem(
                    Color.Red,
                    crossAxis = interpolate(-ItemCrossAxisSize, 0, step / Steps.toFloat()),
                    mainAxis = ItemMainAxisSize - scrolled,
                ),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize * 2 - scrolled),
            )
        }
    }

    @Test
    fun reverseLayout_mainAxisSlideIsNotMirrored() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize, reverseLayout = true) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInMainAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                ExpectedItem(Color.Black, mainAxis = interpolate(-ItemMainAxisSize, 0, fraction)),
            )
        }
    }

    @Test
    fun contentPadding_slideInIsRelativeToPaddedItemPosition() {
        val padding = ItemMainAxisSize / 2
        val containerMainAxisSize = ItemMainAxisSize + padding * 2
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
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = slideInMainAxis(SlideSpec) { -it })
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = containerMainAxisSize,
                ExpectedItem(
                    Color.Black,
                    mainAxis = padding + interpolate(-ItemMainAxisSize, 0, fraction),
                    crossAxis = padding,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Test
    fun rtl_horizontalSlideIsNotMirrored() {
        val horizontalItemSize = if (vertical) ItemCrossAxisSize else ItemMainAxisSize
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(
                    containerMainAxisSize =
                        if (vertical) ItemMainAxisSize else ItemMainAxisSize * 2,
                    containerCrossAxisSize =
                        if (vertical) ItemCrossAxisSize * 2 else ItemCrossAxisSize,
                ) {
                    items(list, key = { it.toArgb() }) {
                        Item(it, enter = slideInHorizontally(SlideSpec) { -it })
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            val x = horizontalItemSize + interpolate(-horizontalItemSize, 0, fraction)
            assertItems(
                containerMainAxisSize = if (vertical) ItemMainAxisSize else ItemMainAxisSize * 2,
                ExpectedItem(
                    Color.Black,
                    mainAxis = if (vertical) 0 else x,
                    crossAxis = if (vertical) x else 0,
                ),
                containerCrossAxisSize = if (vertical) ItemCrossAxisSize * 2 else ItemCrossAxisSize,
            )
        }
    }

    @Test
    fun scrollingDuringSlideOut_slideContinuesRelativeToItemPosition() {
        assertSlidingOutItemFollowsScroll()
    }

    @Test
    fun scrollingDuringSlideOut_withoutRemeasure_slideContinuesRelativeToItemPosition() {
        assertSlidingOutItemFollowsScroll(scrollWithoutRemeasure = true)
    }

    @Test
    fun scrollingDuringSlideOut_reverseLayout_slideContinuesRelativeToItemPosition() {
        assertSlidingOutItemFollowsScroll(reverseLayout = true)
    }

    @Test
    fun scrollingDuringSlideOut_reverseLayout_withoutRemeasure_slideContinuesRelativeToItemPosition() {
        assertSlidingOutItemFollowsScroll(reverseLayout = true, scrollWithoutRemeasure = true)
    }

    @Test
    fun scrollingDuringSlideOut_rtl_slideContinuesRelativeToItemPosition() {
        assertSlidingOutItemFollowsScroll(layoutDirection = LayoutDirection.Rtl)
    }

    private fun assertSlidingOutItemFollowsScroll(
        reverseLayout: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        scrollWithoutRemeasure: Boolean = false,
    ) {
        val startOffset = if (scrollWithoutRemeasure) ItemMainAxisSize / 2 else 0
        val scrollAmount =
            if (scrollWithoutRemeasure) ItemMainAxisSize / 4 else ItemMainAxisSize / 2
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val isMainAxisMirrored = reverseLayout xor (!vertical && isRtl)
        val isCrossAxisMirrored = vertical && isRtl
        val containerMainAxisSize = ItemMainAxisSize * 2
        val containerCrossAxisSize =
            if (isCrossAxisMirrored) ItemCrossAxisSize * 2 else ItemCrossAxisSize
        val itemCrossAxis = if (isCrossAxisMirrored) ItemCrossAxisSize else 0
        var list by
            mutableStateOf(listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyList(
                    containerMainAxisSize = containerMainAxisSize,
                    containerCrossAxisSize = containerCrossAxisSize,
                    reverseLayout = reverseLayout,
                    firstVisibleItemIndex = 1,
                    firstVisibleItemScrollOffset = startOffset,
                ) {
                    items(list, key = { it.toArgb() }) {
                        Item(
                            it,
                            exit = slideOutCrossAxis(SlideSpec) { it },
                            placementSpec = PlacementHoldSpec,
                        )
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Green, Color.Blue, Color.Yellow) }

        onAnimationStep { step ->
            if (step == Steps / 2) {
                rule.runOnIdle { runBlocking { state.scrollBy(-scrollAmount.toFloat()) } }
            }
            val scrolled = if (step >= Steps / 2) scrollAmount else 0
            fun mainAxis(start: Int): Int {
                val logicalStart = start - startOffset + scrolled
                return if (isMainAxisMirrored) {
                    containerMainAxisSize - logicalStart - ItemMainAxisSize
                } else {
                    logicalStart
                }
            }
            assertItems(
                containerMainAxisSize = containerMainAxisSize,
                ExpectedItem(
                    Color.Black,
                    mainAxis = mainAxis(-ItemMainAxisSize),
                    crossAxis = itemCrossAxis,
                ),
                ExpectedItem(
                    Color.Red,
                    mainAxis = mainAxis(0),
                    crossAxis =
                        itemCrossAxis + interpolate(0, ItemCrossAxisSize, step / Steps.toFloat()),
                ),
                ExpectedItem(
                    Color.Green,
                    mainAxis = mainAxis(ItemMainAxisSize),
                    crossAxis = itemCrossAxis,
                ),
                ExpectedItem(
                    Color.Blue,
                    mainAxis = mainAxis(ItemMainAxisSize * 2),
                    crossAxis = itemCrossAxis,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Composable
    private fun LazyList(
        containerMainAxisSize: Int,
        containerCrossAxisSize: Int = ItemCrossAxisSize,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
        content: LazyListScope.() -> Unit,
    ) {
        state = rememberLazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        val density = rule.density
        LazyColumnOrRow(
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            modifier =
                Modifier.requiredMainAxisSize(with(density) { containerMainAxisSize.toDp() })
                    .requiredCrossAxisSize(with(density) { containerCrossAxisSize.toDp() })
                    .background(Color.White)
                    .testTag(ContainerTag),
            content = content,
        )
    }

    private val SlideSpec = tween<IntOffset>(DurationMillis, easing = LinearEasing)
    private val FadeSpec = tween<Float>(DurationMillis, easing = LinearEasing)
    // Holds items at their previous positions for longer than the exit transition runs.
    private val PlacementHoldSpec = tween<IntOffset>(DurationMillis * 2, easing = { 0f })

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
