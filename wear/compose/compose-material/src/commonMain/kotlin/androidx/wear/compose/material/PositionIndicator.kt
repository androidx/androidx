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

package androidx.wear.compose.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.max
import kotlinx.coroutines.delay

/**
 * An object representing the relative position of a scrollbar or rolling side button or rotating
 * bezel position. This interface is implemented by classes that adapt other state information such
 * as [ScalingLazyListState] or [ScrollState] of scrollable containers or to represent the
 * position of say a volume control that can be 'ticked' using a rolling side button or rotating
 * bezel.
 *
 * Implementing classes provide [positionFraction] to determine where in the range [0..1] that the
 * indicator should be displayed and [sizeFraction] to determine the size of the indicator in the
 * range [0..1]. E.g. If a [ScalingLazyListState] had 50 items and the last 5 were visible it
 * would have a position of 1.0f to show that the scroll is positioned at the end of the list and a
 * size of 5 / 50 = 0.1f to indicate that 10% of the visible items are currently visible.
 */
@Stable
interface PositionIndicatorState {
    /**
     * Position of the indicator in the range [0f,1f]. 0f means it is at the top|start, 1f means
     * it is positioned at the bottom|end.
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val positionFraction: Float

    /**
     * Size of the indicator in the range [0f,1f]. 1f means it takes the whole space.
     *
     * @param scrollableContainerSizePx the height or width of the container
     * in pixels depending on orientation of the indicator, (height for vertical, width for
     * horizontal)
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    fun sizeFraction(scrollableContainerSizePx: Float): Float
}

/**
 * Creates an [PositionIndicator] based on the values in a [ScrollState] object.
 * e.g. a [Column] implementing [Modifier.verticalScroll] provides a [ScrollState].
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll)
 * guide.
 *
 * @param scrollState The scrollState to use as the basis for the PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun PositionIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) = PositionIndicator(
    ScrollStateAdapter(scrollState),
    indicatorHeight = 50.dp,
    indicatorWidth = 4.dp,
    paddingRight = 5.dp,
    modifier = modifier,
    reverseDirection = reverseDirection
)

/**
 * Creates an [PositionIndicator] based on the values in a [ScalingLazyListState] object that
 * a [ScalingLazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll)
 * guide.
 *
 * @param scalingLazyListState the [ScalingLazyListState] to use as the basis for the
 * PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun PositionIndicator(
    scalingLazyListState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) = PositionIndicator(
    state = ScalingLazyColumnStateAdapter(
        state = scalingLazyListState
    ),
    indicatorHeight = 50.dp,
    indicatorWidth = 4.dp,
    paddingRight = 5.dp,
    modifier = modifier,
    reverseDirection = reverseDirection
)

/**
 * Creates an [PositionIndicator] based on the values in a [LazyListState] object that
 * a [LazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll)
 * guide.
 *
 * @param lazyListState the [LazyListState] to use as the basis for the
 * PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun PositionIndicator(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) = PositionIndicator(
    state = LazyColumnStateAdapter(
        state = lazyListState
    ),
    indicatorHeight = 50.dp,
    indicatorWidth = 4.dp,
    paddingRight = 5.dp,
    modifier = modifier,
    reverseDirection = reverseDirection
)

/**
 * Creates a [PositionIndicator] for controls like rotating side button, rotating bezel or slider.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll)
 * guide.
 *
 * @param value Value of the indicator in the [range] where 1 represents the
 * maximum value. E.g. If displaying a volume value from 0..11 then the [value] will be
 * volume/11.
 * @param range range of values that [value] can take
 * @param modifier Modifier to be applied to the component
 * @param color Color to draw the indicator on.
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun PositionIndicator(
    value: () -> Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    color: Color = MaterialTheme.colors.onBackground,
    reverseDirection: Boolean = false
) = PositionIndicator(
    state = FractionPositionIndicatorState {
        (value() - range.start) / (range.endInclusive - range.start)
    },
    indicatorHeight = 76.dp,
    indicatorWidth = 6.dp,
    paddingRight = 5.dp,
    color = color,
    modifier = modifier,
    autoHide = false,
    reverseDirection = reverseDirection
)

/**
 * An indicator on the right side on the screen to show the current [PositionIndicatorState].
 *
 * Typically used with the [Scaffold] but can be used to decorate any full screen situation.
 *
 * This composable should only be used to fill the whole screen as Wear Material Design language
 * requires the placement of the position indicator to be right center of the screen as the
 * indicator is curved on circular devices.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * Note that since this indicator can be drawn as a curve that follows the shape of the screen,
 * it needs to be able take the whole screen, but also needs the actual dimensions it needs to be
 * draw [indicatorHeight] and [indicatorWidth], and position with respect to the right edge
 * [paddingRight]
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll)
 * guide.
 *
 * @param state the [PositionIndicatorState] of the state we are displaying
 * @param indicatorHeight the height of the position indicator in Dp.
 * @param indicatorWidth the width of the position indicator in Dp.
 * @param paddingRight the padding to apply to right of the indicator
 * @param modifier The modifier to be applied to the component
 * @param color the color to draw the active part of the indicator in
 * @param background the color to draw the non-active part of the position indicator.
 * @param autoHide whether the indicator should be automatically hidden after showing the change in
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun PositionIndicator(
    state: PositionIndicatorState,
    indicatorHeight: Dp,
    indicatorWidth: Dp,
    paddingRight: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onBackground,
    background: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
    autoHide: Boolean = true,
    reverseDirection: Boolean = false
) {
    val isScreenRound = isRoundDevice()

    val actuallyVisible = remember { mutableStateOf(true) }
    val indicatorPosition = if (reverseDirection) {
        1 - state.positionFraction
    } else {
        state.positionFraction
    }

    if (autoHide) {
        // Note that neither the exact value passed to sizeFraction nor it's return matter, we just
        // need to detect if the size is changing (i.e. we are scrolling/changing volume/etc).
        LaunchedEffect(indicatorPosition, state.sizeFraction(1000f)) {
            actuallyVisible.value = true
            delay(2000)
            actuallyVisible.value = false
        }
    }

    AnimatedVisibility(
        visible = actuallyVisible.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithContent {
                    val indicatorWidthPx = indicatorWidth.toPx()

                    val actualHeight = size.height
                    val indicatorSize = state.sizeFraction(actualHeight)

                    // We want position = 0 be the indicator aligned at the top of its area and
                    // position = 1 be aligned at the bottom of the area.
                    val indicatorStart = indicatorPosition * (1 - indicatorSize)

                    val diameter = max(size.width, size.height)

                    // Note that indicators are always to the right, centered vertically.
                    val paddingRightPx = paddingRight.toPx()
                    if (isScreenRound) {
                        val usableHalf = diameter / 2f - paddingRight.toPx()
                        val sweepDegrees =
                            (2 * asin((indicatorHeight.toPx() / 2) / usableHalf)).toDegrees()

                        drawCurvedIndicator(
                            color,
                            background,
                            paddingRightPx,
                            sweepDegrees,
                            indicatorWidthPx,
                            indicatorStart,
                            indicatorSize
                        )
                    } else {
                        drawStraightIndicator(
                            color,
                            background,
                            paddingRightPx,
                            indicatorWidthPx,
                            indicatorHeightPx = indicatorHeight.toPx(),
                            indicatorStart,
                            indicatorSize
                        )
                    }
                }
        )
    }
}

/**
 * An implementation of [PositionIndicatorState] to display a value that is being incremented or
 * decremented with a rolling side button, rotating bezel or a slider e.g. a volume control.
 *
 * @param fraction Value of the indicator in the range 0..1 where 1 represents the
 * maximum value. E.g. If displaying a volume value from 0..11 then the [fraction] will be
 * volume/11.
 *
 * @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
 */
internal class FractionPositionIndicatorState(
    private val fraction: () -> Float
) : PositionIndicatorState {
    override val positionFraction = 1f // Position indicator always starts at the bottom|end

    override fun sizeFraction(scrollableContainerSizePx: Float) = fraction()

    override fun equals(other: Any?) =
        (other as? FractionPositionIndicatorState)?.fraction?.invoke() == fraction()

    override fun hashCode(): Int = fraction().hashCode()
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a component
 * implementing the [ScrollState] class such as a [Column] implementing [Modifier.verticalScroll].
 *
 * @param scrollState the [ScrollState] to adapt
 *
 * @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
 */
internal class ScrollStateAdapter(private val scrollState: ScrollState) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (scrollState.maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / scrollState.maxValue
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (scrollableContainerSizePx + scrollState.maxValue == 0.0f) {
            1.0f
        } else {
            scrollableContainerSizePx / (scrollableContainerSizePx + scrollState.maxValue)
        }

    override fun equals(other: Any?): Boolean {
        return (other as? ScrollStateAdapter)?.scrollState == scrollState
    }

    override fun hashCode(): Int {
        return scrollState.hashCode()
    }
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a
 * [ScalingLazyColumn] component via its [ScalingLazyListState].
 *
 * Note that size and position calculations ignore spacing between list items both for determining
 * the number and the number of visible items.

 * @param state the [ScalingLazyListState] to adapt.
 *
 * @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
 */
internal class ScalingLazyColumnStateAdapter(
    private val state: ScalingLazyListState
) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()
                val decimalLastItemIndexDistanceFromEnd = state.layoutInfo.totalItemsCount -
                    decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (state.layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()

            (decimalLastItemIndex - decimalFirstItemIndex) /
                state.layoutInfo.totalItemsCount.toFloat()
        }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ScalingLazyColumnStateAdapter)?.state == state
    }

    /**
     * Provide a float value that represents the index of the last visible list item in a scaling
     * lazy column. The value should be in the range from [n,n+1] for a given index n, where n is
     * the index of the last visible item and a value of n represents that only the very start|top
     * of the item is visible, and n+1 means that whole of the item is visible in the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        // This is the offset of the last item w.r.t. the ScalingLazyColumn coordinate system where
        // 0 in the center of the visible viewport and +/-(state.viewportHeightPx / 2f) are the
        // start and end of the viewport.
        //
        // Note that [ScalingLazyListAnchorType] determines how the list items are anchored to the
        // center of the viewport, it does not change viewport coordinates. As a result this
        // calculation needs to take the anchorType into account to calculate the correct end
        // of list item offset.
        val lastItemEndOffset = lastItem.startOffset(state.anchorType.value!!) + lastItem.size
        val viewportEndOffset = state.viewportHeightPx.value!! / 2f
        val lastItemVisibleFraction =
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size)).coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    /**
     * Provide a float value that represents the index of first visible list item in a scaling lazy
     * column. The value should be in the range from [n,n+1] for a given index n, where n is the
     * index of the first visible item and a value of n represents that all of the item is visible
     * in the viewport and a value of n+1 means that only the very end|bottom of the list item is
     * visible at the start|top of the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(state.anchorType.value!!)
        val viewportStartOffset = - (state.viewportHeightPx.value!! / 2f)
        val firstItemInvisibleFraction =
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size).coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a
 * [LazyColumn] component via its [LazyListState].
 *
 * @param state the [LazyListState] to adapt.
 *
 * @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
 */
internal class LazyColumnStateAdapter(
    private val state: LazyListState
) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()

                val decimalLastItemIndexDistanceFromEnd = state.layoutInfo.totalItemsCount -
                    decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (state.layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()

            (decimalLastItemIndex - decimalFirstItemIndex) /
                state.layoutInfo.totalItemsCount.toFloat()
        }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? LazyColumnStateAdapter)?.state == state
    }

    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        val lastItemVisibleSize = state.layoutInfo.viewportEndOffset - lastItem.offset
        val decimalLastItemIndex = lastItem.index.toFloat() +
            lastItemVisibleSize.toFloat() / lastItem.size.toFloat()
        return decimalLastItemIndex
    }

    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemOffset = firstItem.offset - state.layoutInfo.viewportStartOffset
        val decimalFirstItemIndex =
            if (firstItemOffset < 0)
                firstItem.index.toFloat() +
                    abs(firstItemOffset.toFloat()) / firstItem.size.toFloat()
            else firstItem.index.toFloat()
        return decimalFirstItemIndex
    }
}

// TODO(ssancho): implement min/max thumb size (1/10 & 9/10)
private fun ContentDrawScope.drawCurvedIndicator(
    color: Color,
    background: Color,
    paddingRightPx: Float,
    sweepDegrees: Float,
    indicatorWidthPx: Float,
    indicatorStart: Float,
    indicatorSize: Float
) {
    val diameter = max(size.width, size.height)
    val arcSize = Size(
        diameter - 2 * paddingRightPx,
        diameter - 2 * paddingRightPx
    )
    val arcTopLeft = Offset(
        size.width - diameter + paddingRightPx,
        (size.height - diameter) / 2f + paddingRightPx,
    )
    drawArc(
        background,
        startAngle = -sweepDegrees / 2,
        sweepDegrees,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = indicatorWidthPx, cap = StrokeCap.Round)
    )
    drawArc(
        color,
        startAngle = sweepDegrees * (-0.5f + indicatorStart),
        sweepAngle = sweepDegrees * indicatorSize,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = indicatorWidthPx, cap = StrokeCap.Round)
    )
}

private fun ContentDrawScope.drawStraightIndicator(
    color: Color,
    background: Color,
    paddingRightPx: Float,
    indicatorWidthPx: Float,
    indicatorHeightPx: Float,
    indicatorStart: Float,
    indicatorSize: Float
) {
    val lineTop = Offset(
        size.width - paddingRightPx - indicatorWidthPx / 2,
        (size.height - indicatorHeightPx) / 2f
    )
    val lineBottom = lineTop + Offset(0f, indicatorHeightPx)
    drawLine(
        color = background,
        lineTop,
        lineBottom,
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        lerp(lineTop, lineBottom, indicatorStart),
        lerp(lineTop, lineBottom, indicatorStart + indicatorSize),
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
}

internal fun Float.toDegrees() = this * 180f / PI.toFloat()
