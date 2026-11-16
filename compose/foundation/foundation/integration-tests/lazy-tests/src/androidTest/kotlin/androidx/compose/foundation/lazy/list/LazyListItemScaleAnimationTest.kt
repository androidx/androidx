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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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
class LazyListItemScaleAnimationTest(orientation: Orientation) :
    BaseLazyLayoutItemAnimationTest(orientation) {
    private lateinit var state: LazyListState

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun before() {
        rule.mainClock.autoAdvance = false
        assumeTrue(ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
    }

    @Test
    fun scaleIn_oneItemAdded() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { Item(it, enter = scaleIn(ScaleSpec)) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = fraction),
            )
        }
    }

    @Test
    fun scaleIn_fromTopStart() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = scaleIn(ScaleSpec, transformOrigin = TopStart))
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = fraction, origin = TopStart),
            )
        }
    }

    @Test
    fun scaleIn_partialInitialScale() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, initialScale = 0.5f, transformOrigin = TopStart),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = interpolate(0.5f, 1f, fraction), origin = TopStart),
            )
        }
    }

    @Test
    fun noAnimationForInitialItems() {
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(listOf(Color.Black, Color.Red), key = { it.toArgb() }) {
                    Item(it, enter = scaleIn(ScaleSpec))
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
    fun oneExistsOneAdded_onlyAddedItemScales() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { Item(it, enter = scaleIn(ScaleSpec)) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                scaledItem(Color.Red, scale = fraction, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun onlyItemWithEnterTransitionScales() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = if (it == Color.Red) scaleIn(ScaleSpec) else null)
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black, Color.Red) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                scaledItem(Color.Red, scale = fraction, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun scaleOut_oneItemRemoved() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) { Item(it, exit = scaleOut(ScaleSpec)) }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = 1f - fraction),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerMainAxisSize = ItemMainAxisSize)
    }

    @Test
    fun scaleOut_toBottomEnd() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = scaleOut(ScaleSpec, transformOrigin = BottomEnd))
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = 1f - fraction, origin = BottomEnd),
            )
        }
    }

    @Test
    fun lastItemRemoved_otherItemsAreNotAffected() {
        var list by mutableStateOf(listOf(Color.Black, Color.Red))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) { Item(it, exit = scaleOut(ScaleSpec)) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                ExpectedItem(Color.Black),
                scaledItem(Color.Red, scale = 1f - fraction, mainAxis = ItemMainAxisSize),
            )
        }
    }

    @Test
    fun scaleOutWithTargetScaleOne_itemIsRemovedAfterOneFrame() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, exit = scaleOut(ScaleSpec, targetScale = 1f))
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        assertItems(containerMainAxisSize = ItemMainAxisSize, ExpectedItem(Color.Black))
        rule.mainClock.advanceTimeByFrame()
        assertItems(containerMainAxisSize = ItemMainAxisSize)
    }

    @Test
    fun scaleInCombinedWithFadeIn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = fadeIn(ScaleSpec) + scaleIn(ScaleSpec))
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black.copy(alpha = fraction), scale = fraction),
            )
        }
    }

    @Test
    fun scaleInCombinedWithSlideIn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter =
                            slideInCrossAxis(SlideSpec) { -it } +
                                scaleIn(ScaleSpec, transformOrigin = TopStart),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(
                    Color.Black,
                    scale = fraction,
                    origin = TopStart,
                    crossAxis = interpolate(-ItemCrossAxisSize, 0, fraction),
                ),
            )
        }
    }

    @Test
    fun scaleInCombinedWithPlacementAnimationOfOtherItems() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        placementSpec = SlideSpec,
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Red, Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize * 2,
                scaledItem(Color.Red, scale = fraction, origin = TopStart),
                ExpectedItem(Color.Black, mainAxis = interpolate(0, ItemMainAxisSize, fraction)),
            )
        }
    }

    @Test
    fun reAddItemBeingScaledOut_scalesBackFromCurrentScaleWithEnterSpec() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        exit = scaleOut(ScaleSpec, transformOrigin = TopStart),
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        val interruptedScale = 0.5f
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = listOf(Color.Black) }
            val scale =
                if (step <= Steps / 2) {
                    interpolate(1f, 0f, step / Steps.toFloat())
                } else {
                    interpolate(interruptedScale, 1f, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = scale, origin = TopStart),
            )
        }
    }

    @Test
    fun removeItemBeingScaledIn_scalesOutFromCurrentScaleWithExitSpec() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        exit = scaleOut(ScaleSpec, transformOrigin = TopStart),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        val interruptedScale = 0.5f
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
            val scale =
                if (step <= Steps / 2) {
                    interpolate(0f, 1f, step / Steps.toFloat())
                } else {
                    interpolate(interruptedScale, 0f, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = scale, origin = TopStart),
            )
        }
    }

    @Test
    fun removeItemBeingScaledIn_exitWithoutScale_keepsScaleAndTransformOrigin() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        // Keeps the item opaque, and so drawn, while the exit transition runs.
                        exit = fadeOut(HoldSpec),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        val interruptedScale = 0.5f
        onAnimationStep(steps = Steps + Steps / 2 - 1) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
            val scale = if (step <= Steps / 2) step / Steps.toFloat() else interruptedScale
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = scale, origin = TopStart),
            )
        }
    }

    @Test
    fun reAddItemAfterScaleOutFinished_scalesInFromInitialScale() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = scaleIn(ScaleSpec), exit = scaleOut(ScaleSpec))
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }
        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = 1f - fraction),
            )
        }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = fraction),
            )
        }
    }

    @Test
    fun enterAndExitWithDifferentTransformOrigins_eachUsesItsOwn() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        exit = scaleOut(ScaleSpec, transformOrigin = BottomEnd),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }
        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = fraction, origin = TopStart),
            )
        }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnUiThread { list = emptyList() }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = 1f - fraction, origin = BottomEnd),
            )
        }
    }

    @Test
    fun removeItemBeingScaledIn_differentTransformOrigins_keepsEnterTransformOrigin() {
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        exit = scaleOut(ScaleSpec, transformOrigin = BottomEnd),
                    )
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        val interruptedScale = 0.5f
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = emptyList() }
            val scale =
                if (step <= Steps / 2) {
                    interpolate(0f, 1f, step / Steps.toFloat())
                } else {
                    interpolate(interruptedScale, 0f, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = scale, origin = TopStart),
            )
        }
    }

    @Test
    fun reAddItemBeingScaledOut_differentTransformOrigins_keepsExitTransformOrigin() {
        var list by mutableStateOf(listOf(Color.Black))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize) {
                items(list, key = { it.toArgb() }) {
                    Item(
                        it,
                        enter = scaleIn(ScaleSpec, transformOrigin = TopStart),
                        exit = scaleOut(ScaleSpec, transformOrigin = BottomEnd),
                    )
                }
            }
        }

        rule.runOnUiThread { list = emptyList() }

        val interruptedScale = 0.5f
        onAnimationStep(steps = Steps + Steps / 2) { step ->
            if (step == Steps / 2) rule.runOnUiThread { list = listOf(Color.Black) }
            val scale =
                if (step <= Steps / 2) {
                    interpolate(1f, 0f, step / Steps.toFloat())
                } else {
                    interpolate(interruptedScale, 1f, (step - Steps / 2) / Steps.toFloat())
                }
            assertItems(
                containerMainAxisSize = ItemMainAxisSize,
                scaledItem(Color.Black, scale = scale, origin = BottomEnd),
            )
        }
    }

    @Test
    fun contentPadding_scaleInIsRelativeToPaddedItemPosition() {
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
                items(list, key = { it.toArgb() }) { Item(it, enter = scaleIn(ScaleSpec)) }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = containerMainAxisSize,
                scaledItem(
                    Color.Black,
                    scale = fraction,
                    mainAxis = padding,
                    crossAxis = padding,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    @Test
    fun scrollingDuringScaleIn_scaleContinuesRelativeToItemPosition() {
        var list by mutableStateOf(listOf(Color.Black, Color.Green, Color.Blue))
        rule.setContent {
            LazyList(containerMainAxisSize = ItemMainAxisSize * 2) {
                items(list, key = { it.toArgb() }) {
                    Item(it, enter = scaleIn(ScaleSpec, transformOrigin = TopStart))
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
                scaledItem(
                    Color.Red,
                    scale = step / Steps.toFloat(),
                    origin = TopStart,
                    mainAxis = ItemMainAxisSize - scrolled,
                ),
                ExpectedItem(Color.Green, mainAxis = ItemMainAxisSize * 2 - scrolled),
            )
        }
    }

    @Test
    fun scrollingDuringScaleOut_scaleContinuesRelativeToItemPosition() {
        assertScalingOutItemFollowsScroll()
    }

    @Test
    fun scrollingDuringScaleOut_withoutRemeasure_scaleContinuesRelativeToItemPosition() {
        assertScalingOutItemFollowsScroll(scrollWithoutRemeasure = true)
    }

    @Test
    fun scrollingDuringScaleOut_reverseLayout_scaleContinuesRelativeToItemPosition() {
        assertScalingOutItemFollowsScroll(reverseLayout = true)
    }

    @Test
    fun scrollingDuringScaleOut_reverseLayout_withoutRemeasure_scaleContinuesRelativeToItemPosition() {
        assertScalingOutItemFollowsScroll(reverseLayout = true, scrollWithoutRemeasure = true)
    }

    @Test
    fun scrollingDuringScaleOut_rtl_scaleContinuesRelativeToItemPosition() {
        assertScalingOutItemFollowsScroll(layoutDirection = LayoutDirection.Rtl)
    }

    private fun assertScalingOutItemFollowsScroll(
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
                            exit = scaleOut(ScaleSpec, transformOrigin = TopStart),
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
                scaledItem(
                    Color.Red,
                    scale = 1f - step / Steps.toFloat(),
                    origin = TopStart,
                    mainAxis = mainAxis(0),
                    crossAxis = itemCrossAxis,
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

    @Test
    fun rtl_transformOriginIsNotMirrored() {
        val containerMainAxisSize = if (vertical) ItemMainAxisSize else ItemMainAxisSize * 2
        val containerCrossAxisSize = if (vertical) ItemCrossAxisSize * 2 else ItemCrossAxisSize
        var list by mutableStateOf(emptyList<Color>())
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyList(
                    containerMainAxisSize = containerMainAxisSize,
                    containerCrossAxisSize = containerCrossAxisSize,
                ) {
                    items(list, key = { it.toArgb() }) {
                        Item(it, enter = scaleIn(ScaleSpec, transformOrigin = TopStart))
                    }
                }
            }
        }

        rule.runOnUiThread { list = listOf(Color.Black) }

        onAnimationFrame { fraction ->
            assertItems(
                containerMainAxisSize = containerMainAxisSize,
                scaledItem(
                    Color.Black,
                    scale = fraction,
                    origin = TopStart,
                    mainAxis = if (vertical) 0 else ItemMainAxisSize,
                    crossAxis = if (vertical) ItemCrossAxisSize else 0,
                ),
                containerCrossAxisSize = containerCrossAxisSize,
            )
        }
    }

    private fun scaledItem(
        color: Color,
        scale: Float,
        origin: TransformOrigin = TransformOrigin.Center,
        crossAxis: Int = 0,
        mainAxis: Int = 0,
    ): ExpectedItem {
        val itemWidth = if (vertical) ItemCrossAxisSize else ItemMainAxisSize
        val itemHeight = if (vertical) ItemMainAxisSize else ItemCrossAxisSize
        val pivotX = origin.pivotFractionX * itemWidth
        val pivotY = origin.pivotFractionY * itemHeight
        val left = (pivotX * (1 - scale)).roundToInt()
        val top = (pivotY * (1 - scale)).roundToInt()
        val right = (pivotX + (itemWidth - pivotX) * scale).roundToInt()
        val bottom = (pivotY + (itemHeight - pivotY) * scale).roundToInt()
        return ExpectedItem(
            color,
            mainAxis = mainAxis + if (vertical) top else left,
            crossAxis = crossAxis + if (vertical) left else top,
            mainAxisSize = if (vertical) bottom - top else right - left,
            crossAxisSize = if (vertical) right - left else bottom - top,
        )
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

    private val HoldSpec = tween<Float>(DurationMillis, easing = { 0f })

    private val ScaleSpec = tween<Float>(DurationMillis, easing = LinearEasing)
    private val SlideSpec = tween<IntOffset>(DurationMillis, easing = LinearEasing)
    // Outlasts the exit transitions, so the held items don't settle into their new positions
    // on the last frame of the exit transition.
    private val PlacementHoldSpec = tween<IntOffset>(DurationMillis * 2, easing = { 0f })

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

private val TopStart = TransformOrigin(0f, 0f)
private val BottomEnd = TransformOrigin(1f, 1f)
