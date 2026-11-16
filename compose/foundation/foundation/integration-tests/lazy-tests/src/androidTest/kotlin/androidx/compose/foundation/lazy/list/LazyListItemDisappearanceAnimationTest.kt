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

package androidx.compose.foundation.lazy.list

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class LazyListItemDisappearanceAnimationTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    // the numbers should be divisible by 8 to avoid the rounding issues as we run 4 or 8 frames
    // of the animation.
    private val itemSize: Int = 4
    private var itemSizeDp: Dp = Dp.Infinity
    private val crossAxisSize: Int = 2
    private var crossAxisSizeDp: Dp = Dp.Infinity
    private val containerSize: Float = itemSize * 2f
    private var containerSizeDp: Dp = Dp.Infinity
    private lateinit var state: LazyListState

    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        with(rule.density) {
            itemSizeDp = itemSize.toDp()
            crossAxisSizeDp = crossAxisSize.toDp()
            containerSizeDp = containerSize.toDp()
        }
    }

    @Test
    fun oneItemRemoved() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertPixels(mainAxisSize = itemSize) { Color.Black.copy(alpha = 1f - fraction) }
        }
    }

    @Test
    fun threeExistTwoRemoved() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyList(containerSize = itemSizeDp * 3) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Black
                    in itemSize until itemSize * 2 -> Color.Red.copy(alpha = 1f - fraction)
                    else -> Color.Green.copy(alpha = 1f - fraction)
                }
            }
        }
    }

    // todo copy with content padding and horizontal
    @Test
    fun threeExistTwoRemoved_reverseLayout() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Green))
        rule.setContent {
            LazyList(containerSize = itemSizeDp * 3, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Green.copy(alpha = 1f - fraction)
                    in itemSize until itemSize * 2 -> Color.Red.copy(alpha = 1f - fraction)
                    else -> Color.Black
                }
            }
        }
    }

    @Test
    fun oneRemoved_reverseLayout_contentPadding() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(
                containerSize = itemSizeDp * 3,
                reverseLayout = true,
                contentPadding = PaddingValues(afterContent = itemSizeDp),
            ) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Red.copy(alpha = 1f - fraction)
                    in itemSize until itemSize * 2 -> Color.Black
                    else -> Color.Transparent
                }
            }
        }
    }

    @Test
    fun oneRemoved_rtl_contentPadding() {
        // RTL mirrors the main axis of horizontal lists only.
        assumeTrue(!vertical)
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(
                    containerSize = itemSizeDp * 3,
                    contentPadding = PaddingValues(beforeContent = itemSizeDp),
                ) {
                    items(list, key = { it.toArgb() }) { Item(it) }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 3) { offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Red.copy(alpha = 1f - fraction)
                    in itemSize until itemSize * 2 -> Color.Black
                    else -> Color.Transparent
                }
            }
        }
    }

    @Test
    fun onlyItemWithSpecsIsAnimating() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, disappearanceSpec = if (it == Color.Red) AnimSpec else null)
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertPixels(itemSize * 2) { offset ->
                when (offset) {
                    in 0 until itemSize -> Color.Transparent
                    else -> Color.Red.copy(alpha = 1f - fraction)
                }
            }
        }
    }

    @Test
    fun itemRemovedOutsideOfViewportIsNotAnimated() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red, Color.Blue, Color.Green))
        rule.setContent {
            LazyList(containerSize = itemSizeDp * 2) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.runOnUiThread {
            // Blue is removed before Green, both are outside the bounds
            list = listOf(Color.Black, Color.Red, Color.Green)
        }

        rule.runOnIdle {
            runBlocking {
                // scroll 0.5 items so we now see half of Black, Red and half of Green
                state.scrollBy(itemSize * 0.5f)
            }
        }

        onAnimationFrame {
            assertPixels(itemSize * 2) { offset ->
                when (offset) {
                    in 0 until itemSize / 2 -> Color.Black
                    in itemSize / 2 until itemSize * 3 / 2 -> Color.Red
                    else -> Color.Green
                }
            }
        }
    }

    @Test
    fun itemsBeingRemovedAreAffectingTheContainerSizeForTheDurationOfAnimation() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerSize = null) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.onNodeWithTag(ContainerTag).assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            val mainAxisSizeDp = rule.onNodeWithTag(ContainerTag).getMainAxisSizeInRoot()
            val mainAxisSizePx = with(rule.density) { mainAxisSizeDp.roundToPx() }
            assertWithMessage("Main axis size on fraction=$fraction")
                .that(mainAxisSizePx)
                .isEqualTo(if (fraction < 1f) itemSize * 2 else itemSize)

            if (fraction < 1f) {
                assertPixels(itemSize * 2) { offset ->
                    when (offset) {
                        in 0 until itemSize -> Color.Black
                        else -> Color.Red.copy(1f - fraction)
                    }
                }
            } else {
                assertPixels(itemSize) { Color.Black }
            }
        }
    }

    @Test
    fun itemsBeingRemovedAreAffectingTheContainerSizeForTheDurationOfAnimation_reverseLayout() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerSize = null, reverseLayout = true) {
                items(list, key = { it.toArgb() }) { Item(it) }
            }
        }

        rule.onNodeWithTag(ContainerTag).assertMainAxisSizeIsEqualTo(itemSizeDp * 2)

        assertPixels(itemSize * 2) { offset ->
            when (offset) {
                in 0 until itemSize -> Color.Red
                else -> Color.Black
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            val mainAxisSizeDp = rule.onNodeWithTag(ContainerTag).getMainAxisSizeInRoot()
            val mainAxisSizePx = with(rule.density) { mainAxisSizeDp.roundToPx() }
            assertWithMessage("Main axis size on fraction=$fraction")
                .that(mainAxisSizePx)
                .isEqualTo(if (fraction < 1f) itemSize * 2 else itemSize)

            if (fraction < 1f) {
                assertPixels(itemSize * 2) { offset ->
                    when (offset) {
                        in 0 until itemSize -> Color.Red.copy(1f - fraction)
                        else -> Color.Black
                    }
                }
            } else {
                assertPixels(itemSize) { Color.Black }
            }
        }
    }

    @Test
    fun reAddItemBeingAnimated_withoutAppearanceAnimation() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerSize = itemSizeDp) { items(list, key = { it.toArgb() }) { Item(it) } }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { Color.Black.copy(alpha = 1f - fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = listOf(Color.Black) }
                }
                assertPixels(itemSize) { Color.Black }
            }
        }
    }

    @Test
    fun reAddItemBeingAnimated_withAppearanceAnimation() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerSize = itemSizeDp) {
                items(list, key = { it.toArgb() }) {
                    Item(it, appearanceSpec = HalfDurationAnimSpec)
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { Color.Black.copy(alpha = 1f - fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = listOf(Color.Black) }
                }
                assertPixels(itemSize) { Color.Black.copy(alpha = fraction) }
            }
        }
    }

    @Test
    fun removeItemBeingAnimatedForAppearance() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerSize = itemSizeDp) {
                items(list, key = { it.toArgb() }) {
                    Item(it, appearanceSpec = AnimSpec, disappearanceSpec = HalfDurationAnimSpec)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            if (fraction < 0.5f) {
                assertPixels(itemSize) { Color.Black.copy(alpha = fraction) }
            } else {
                if (fraction.isCloseTo(0.5f)) {
                    rule.runOnUiThread { list = emptyList() }
                }
                assertPixels(itemSize) { Color.Black.copy(alpha = 1f - fraction) }
            }
        }
    }

    @Test
    fun disappearingItemFollowsScroll() {
        assertDisappearingItemFollowsScroll()
    }

    @Test
    fun disappearingItemFollowsScroll_withoutRemeasure() {
        assertDisappearingItemFollowsScroll(scrollWithoutRemeasure = true)
    }

    @Test
    fun disappearingItemFollowsScroll_reverseLayout() {
        assertDisappearingItemFollowsScroll(reverseLayout = true)
    }

    @Test
    fun disappearingItemFollowsScroll_reverseLayout_withoutRemeasure() {
        assertDisappearingItemFollowsScroll(reverseLayout = true, scrollWithoutRemeasure = true)
    }

    @Test
    fun disappearingItemFollowsScroll_rtl() {
        assertDisappearingItemFollowsScroll(layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun disappearingItemFollowsScroll_rtl_withoutRemeasure() {
        assertDisappearingItemFollowsScroll(
            layoutDirection = LayoutDirection.Rtl,
            scrollWithoutRemeasure = true,
        )
    }

    @Test
    fun disappearingItemFollowsScroll_rtl_reverseLayout() {
        assertDisappearingItemFollowsScroll(
            reverseLayout = true,
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun disappearingItemFollowsScroll_rtl_reverseLayout_withoutRemeasure() {
        assertDisappearingItemFollowsScroll(
            reverseLayout = true,
            layoutDirection = LayoutDirection.Rtl,
            scrollWithoutRemeasure = true,
        )
    }

    @Test
    fun disappearingItemFollowsScroll_inLookaheadScope() {
        assertDisappearingItemFollowsScroll(isInLookaheadScope = true)
    }

    @Test
    fun disappearingItemFollowsScroll_inLookaheadScope_withoutRemeasure() {
        assertDisappearingItemFollowsScroll(
            isInLookaheadScope = true,
            scrollWithoutRemeasure = true,
        )
    }

    @Test
    fun disappearingItemFollowsScroll_rtl_reverseLayout_inLookaheadScope() {
        assertDisappearingItemFollowsScroll(
            reverseLayout = true,
            layoutDirection = LayoutDirection.Rtl,
            isInLookaheadScope = true,
        )
    }

    private fun assertDisappearingItemFollowsScroll(
        reverseLayout: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        scrollWithoutRemeasure: Boolean = false,
        isInLookaheadScope: Boolean = false,
    ) {
        val startOffset = if (scrollWithoutRemeasure) itemSize / 2 else 0
        val scrollAmount = if (scrollWithoutRemeasure) itemSize / 4 else itemSize / 2
        var list by
            mutableStateOf(listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow))
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                val layout =
                    @Composable {
                        LazyList(
                            containerSize = itemSizeDp * 2,
                            startIndex = 1,
                            startOffset = startOffset,
                            reverseLayout = reverseLayout,
                        ) {
                            items(list, key = { it.toArgb() }) {
                                Item(it, placementSpec = PlacementHoldSpec)
                            }
                        }
                    }
                if (isInLookaheadScope) LookaheadScope { layout() } else layout()
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Green, Color.Blue, Color.Yellow) }

        onAnimationFrame { fraction ->
            if (fraction.isCloseTo(0.5f)) {
                rule.runOnIdle { runBlocking { state.scrollBy(-scrollAmount.toFloat()) } }
            }
            val scrolled = if (fraction >= 0.5f) scrollAmount else 0
            assertPixels(itemSize * 2) { offset ->
                expectedColorAt(
                    offset = offset,
                    // The main axis start of each item, relative to the start of the list's
                    // content, before scrolling. The held items stay in their old slots.
                    items =
                        listOf(
                            Color.Black to -itemSize - startOffset,
                            Color.Red.copy(alpha = 1f - fraction) to -startOffset,
                            Color.Green to itemSize - startOffset,
                            Color.Blue to itemSize * 2 - startOffset,
                        ),
                    scrolled = scrolled,
                    isMainAxisMirrored =
                        reverseLayout xor (!vertical && layoutDirection == LayoutDirection.Rtl),
                )
            }
        }
    }

    private fun expectedColorAt(
        offset: Int,
        items: List<Pair<Color, Int>>,
        scrolled: Int,
        isMainAxisMirrored: Boolean,
    ): Color {
        val containerSize = itemSize * 2
        return items
            .firstOrNull { (_, start) ->
                val logicalStart = start + scrolled
                val visualStart =
                    if (isMainAxisMirrored) containerSize - logicalStart - itemSize
                    else logicalStart
                offset in visualStart until visualStart + itemSize
            }
            ?.first ?: Color.Transparent
    }

    private fun assertPixels(
        mainAxisSize: Int,
        crossAxisSize: Int = this.crossAxisSize,
        expectedColorProvider: (mainAxis: Int) -> Color?,
    ) {
        rule.onNodeWithTag(ContainerTag).assertAxisPixels(mainAxisSize, crossAxisSize) { _, mainAxis
            ->
            expectedColorProvider(mainAxis)?.compositeOver(Color.White)
        }
    }

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
                Truth.assertThat(expectedTime).isEqualTo(rule.mainClock.currentTime)
            }
        }
    }

    @Composable
    private fun LazyList(
        containerSize: Dp? = containerSizeDp,
        startIndex: Int = 0,
        startOffset: Int = 0,
        crossAxisSize: Dp = crossAxisSizeDp,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        content: LazyListScope.() -> Unit,
    ) {
        state = rememberLazyListState(startIndex, startOffset)
        LazyColumnOrRow(
            modifier =
                Modifier.then(
                        if (containerSize != null) {
                            Modifier.requiredMainAxisSize(containerSize)
                        } else {
                            Modifier
                        }
                    )
                    .background(Color.White)
                    .then(
                        if (crossAxisSize != Dp.Unspecified) {
                            Modifier.requiredCrossAxisSize(crossAxisSize)
                        } else {
                            Modifier.fillMaxCrossAxis()
                        }
                    )
                    .testTag(ContainerTag),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            content = content,
        )
    }

    @Composable
    private fun LazyItemScope.Item(
        color: Color,
        size: Dp = itemSizeDp,
        crossAxisSize: Dp = crossAxisSizeDp,
        disappearanceSpec: FiniteAnimationSpec<Float>? = AnimSpec,
        appearanceSpec: FiniteAnimationSpec<Float>? = null,
        placementSpec: FiniteAnimationSpec<IntOffset>? = null,
    ) {
        Box(
            Modifier.animateItem(
                    fadeInSpec = appearanceSpec,
                    placementSpec = placementSpec,
                    fadeOutSpec = disappearanceSpec,
                )
                .background(color)
                .requiredMainAxisSize(size)
                .requiredCrossAxisSize(crossAxisSize)
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

private val FrameDuration = 16L
private val Duration = 64L // 4 frames, so we get 0f, 0.25f, 0.5f, 0.75f and 1f fractions
private val AnimSpec = tween<Float>(Duration.toInt(), easing = LinearEasing)
private val HalfDurationAnimSpec = tween<Float>(Duration.toInt() / 2, easing = LinearEasing)
// Holds items at their previous positions for longer than the disappearance animation runs.
private val PlacementHoldSpec = tween<IntOffset>(Duration.toInt() * 2, easing = { 0f })
private val ContainerTag = "container"
