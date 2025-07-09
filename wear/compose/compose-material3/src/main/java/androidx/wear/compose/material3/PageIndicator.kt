/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.inverseLerp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.materialcore.BoundsLimiter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Horizontal page indicator for use with [HorizontalPager], representing the currently active page
 * and the approximate number of pages. Pages are indicated as a Circle shape. The indicator shows
 * up to six pages individually - if there are more than six pages, [HorizontalPageIndicator] shows
 * a smaller indicator to the left and/or right to indicate that more pages are available.
 *
 * This is a full screen component and will occupy the whole screen. However it's not actionable, so
 * it's not expected to interfere with anything on the screen.
 *
 * Here's how different positions 0..10 might be visually represented: "X" is selected item, "O" and
 * "o" full and half size items respectively.
 *
 * O X O O O o - 2nd position out of 10. There are no more items on the left but more on the right.
 *
 * o O O O X o - current page could be 6, 7 or 8 out of 10, as there are more potential pages on the
 * left and on the right.
 *
 * o O O O X O - current page is 9 out of 10, as there no more items on the right
 *
 * [HorizontalPageIndicator] can be linear or curved, depending on the screen shape of the device -
 * for circular screens it will be curved, whilst for square screens it will be linear.
 *
 * Example usage with [HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.HorizontalPageIndicatorWithPagerSample
 * @param pagerState State of the [HorizontalPager] used to control this indicator
 * @param modifier Modifier to be applied to the [HorizontalPageIndicator]
 * @param selectedColor The color which will be used for a selected indicator item.
 * @param unselectedColor The color which will be used for an unselected indicator item.
 * @param backgroundColor The color which will be used for an indicator background.
 */
@Composable
public fun HorizontalPageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
) {
    PageIndicatorImpl(
        state = pagerState,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        backgroundColor = backgroundColor,
        modifier = modifier,
        indicatorSize = PageIndicatorItemSize,
        spacing = PageIndicatorSpacing,
        isHorizontal = true,
    )
}

/**
 * Vertical page indicator for use with [VerticalPager], representing the currently active page and
 * the approximate number of pages. Pages are indicated as a Circle shape. The indicator shows up to
 * six pages individually - if there are more than six pages, [VerticalPageIndicator] shows a
 * smaller indicator to the top and/or bottom to indicate that more pages are available.
 *
 * This is a full screen component and will occupy the whole screen. However it's not actionable, so
 * it's not expected to interfere with anything on the screen.
 *
 * [VerticalPageIndicator] can be linear or curved, depending on the screen shape of the device -
 * for circular screens it will be curved, whilst for square screens it will be linear.
 *
 * Example usage with [VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.VerticalPageIndicatorWithPagerSample
 * @param pagerState State of the [VerticalPager] used to control this indicator
 * @param modifier Modifier to be applied to the [VerticalPageIndicator]
 * @param selectedColor The color which will be used for a selected indicator item.
 * @param unselectedColor The color which will be used for an unselected indicator item.
 * @param backgroundColor The color which will be used for an indicator background.
 */
@Composable
public fun VerticalPageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
) {
    PageIndicatorImpl(
        state = pagerState,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        backgroundColor = backgroundColor,
        modifier = modifier,
        indicatorSize = PageIndicatorItemSize,
        spacing = PageIndicatorSpacing,
        isHorizontal = false,
    )
}

/** Contains the default values used by [HorizontalPageIndicator] and [VerticalPageIndicator] */
public object PageIndicatorDefaults {

    /**
     * The recommended color to use for the selected indicator item in [VerticalPageIndicator] and
     * [HorizontalPageIndicator].
     */
    public val selectedColor: Color
        @ReadOnlyComposable @Composable get() = ColorSchemeKeyTokens.OnBackground.value

    /**
     * The recommended color to use for the unselected indicator item in [VerticalPageIndicator] and
     * [HorizontalPageIndicator].
     */
    public val unselectedColor: Color
        @ReadOnlyComposable
        @Composable
        get() = ColorSchemeKeyTokens.OnBackground.value.copy(alpha = 0.3f)

    /**
     * The recommended color to use for the background in [VerticalPageIndicator] and
     * [HorizontalPageIndicator].
     */
    public val backgroundColor: Color
        @ReadOnlyComposable
        @Composable
        get() = ColorSchemeKeyTokens.Background.value.copy(alpha = 0.85f)
}

@Composable
internal fun PageIndicatorImpl(
    state: PagerState,
    selectedColor: Color,
    unselectedColor: Color,
    backgroundColor: Color,
    modifier: Modifier,
    indicatorSize: Dp,
    spacing: Dp,
    isHorizontal: Boolean,
) {
    val layoutDirection = LocalLayoutDirection.current
    val edgePadding = PaddingDefaults.edgePadding

    // Converting offsetFraction into range 0..1f
    val currentPageOffsetWithFraction = state.currentPage + state.currentPageOffsetFraction

    val isLastPage =
        currentPageOffsetWithFraction.equalsWithTolerance(
            number = state.pageCount - 1f,
            tolerance = 0.001f,
        )

    // If it's the last page, then we decrease its index by 1 and put a 1f to the offset
    val selectedPage: Int =
        if (isLastPage) currentPageOffsetWithFraction.toInt() - 1
        else currentPageOffsetWithFraction.toInt()
    val offset = currentPageOffsetWithFraction - selectedPage

    val pagesOnScreen = Integer.min(MaxNumberOfIndicators, state.pageCount)
    val pagesState =
        remember(state.pageCount) {
            PagesState(
                totalPages = state.pageCount,
                pagesOnScreen = pagesOnScreen,
                smallIndicatorSizeFraction = smallIndicatorSizeFraction,
                shrinkThresholdStart = calculateShrinkThresholdStart(spacing, indicatorSize),
                shrinkThresholdEnd = calculateShrinkThresholdEnd(spacing, indicatorSize),
            )
        }

    if (pagesState.totalPages > 1) {
        pagesState.recalculateState(selectedPage, offset)
    }

    val spacerSize = indicatorSize + spacing
    val boundsSize: Density.() -> IntSize = {
        val width = (spacerSize.toPx() * pagesOnScreen).roundToInt()
        val height = (indicatorSize * 2).roundToPx().coerceAtLeast(0)
        val size =
            IntSize(
                width = if (isHorizontal) width else height,
                height = if (isHorizontal) height else width,
            )
        size
    }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val boundsOffset: Density.() -> IntOffset = {
        val measuredSize = boundsSize()
        if (isHorizontal) {
            // Offset here is the distance between top left corner of the outer container to
            // the top left corner of the indicator. Its placement should look similar to
            // Alignment.BottomCenter.
            IntOffset(
                x = (containerSize.width - measuredSize.width) / 2 - edgePadding.roundToPx(),
                y = containerSize.height - measuredSize.height - edgePadding.roundToPx() * 2,
            )
        } else {
            // Offset here is the distance between top left corner of the outer container to
            // the top left corner of the indicator. Its placement should look similar to
            // Alignment.CenterEnd.
            IntOffset(
                x =
                    if (layoutDirection == LayoutDirection.Ltr) {
                        containerSize.width - measuredSize.width - edgePadding.roundToPx() * 2
                    } else edgePadding.roundToPx(),
                y = (containerSize.height - measuredSize.height) / 2 - edgePadding.roundToPx(),
            )
        }
    }

    BoundsLimiter(
        offset = boundsOffset,
        size = boundsSize,
        modifier = modifier.padding(edgePadding),
        onSizeChanged = { containerSize = it },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val indicatorSizePx = indicatorSize.roundToPx().toFloat()
            val spacerSizePx = if (pagesOnScreen > 1) spacerSize.toPx() else 0f
            val backgroundStrokeWidthPx = BackgroundRadius.toPx() * 2 + indicatorSizePx
            val radius = (min(size.width, size.height) - backgroundStrokeWidthPx) / 2
            var topLeftX: Float = backgroundStrokeWidthPx / 2f
            var topLeftY: Float = backgroundStrokeWidthPx / 2f

            // Support screens with width != height
            if (size.width > size.height) {
                topLeftX += (size.width - size.height) / 2f
            } else if (size.height > size.width) {
                topLeftY += (size.height - size.width) / 2f
            }
            val topLeft = Offset(topLeftX, topLeftY)

            // The length of the page indicator with, for example, 6 indicators is:
            // +--------------+
            // o  o  o  o  o  o
            // +--------------+
            // where o is an indicator of width indicatorSizePx and space between indicators is
            // spacing
            // That gives us:
            // indicatorSizePx / 2 + indicatorSizePx * 4 + indicatorSizePx / 2 + spacing.toPx() * 5
            // or:
            // indicatorSizePx * 5 + spacing.toPx * 5
            // spacerSizePx = indicatorSize + spacing, which gives us final formula:
            // spacerSizePx * 5, where 5 = pagesOnScreen - 1
            val indicatorLength = spacerSizePx * (pagesOnScreen - 1)
            val indicatorLengthAngle = distanceToAngle(indicatorLength, radius)

            // Center indicator around Anchor
            val anchor =
                if (isHorizontal) {
                    HorizontalPagerAnchor
                } else if (layoutDirection == LayoutDirection.Ltr) {
                    VerticalPagerAnchor
                } else {
                    VerticalPagerRtlAnchor
                }

            // lambda function to rotate angle either clockwise or anti clock-wise
            val rotateBy: Float.(Float) -> Float =
                if (isHorizontal) {
                    // Horizontal mode: move selected dot anti clock-wise in LTR and clock wise in
                    // LTR
                    if (layoutDirection == LayoutDirection.Ltr) {
                        { angle -> this - angle }
                    } else {
                        { angle -> this + angle }
                    }
                } else {
                    // Vertical mode: move selected dot clock-wise in LTR and anti clock wise in
                    // RTL
                    if (layoutDirection == LayoutDirection.Ltr) {
                        { angle -> this + angle }
                    } else {
                        { angle -> this - angle }
                    }
                }

            // negation since we need to rotate start angle by half of the indicator width to the
            // opposite direction from anchor in order for indicator to be center around anchor
            val startAngle = anchor.rotateBy(-indicatorLengthAngle / 2)
            val endAngle = anchor.rotateBy(indicatorLengthAngle / 2)

            if (pagesOnScreen == 1) {
                drawCircleAtAngle(startAngle, radius, backgroundStrokeWidthPx / 2, backgroundColor)
                drawCircleAtAngle(startAngle, radius, indicatorSizePx / 2, selectedColor)
            } else {
                drawIndicatorArcBackground(
                    radius,
                    startAngle,
                    endAngle,
                    topLeft,
                    backgroundColor,
                    Stroke(width = backgroundStrokeWidthPx, cap = StrokeCap.Round),
                )

                drawIndicators(
                    radius,
                    rotateBy,
                    startAngle,
                    indicatorSizePx,
                    spacerSizePx,
                    selectedColor,
                    unselectedColor,
                    pagesState,
                    offset,
                    topLeft,
                )
            }
        }
    }
}

private fun DrawScope.drawSelectedIndicatorArc(
    radius: Float,
    rotateBy: Float.(Float) -> Float,
    angle: Float,
    spacerAngle: Float,
    offset: Float,
    color: Color,
    topLeft: Offset,
    indicatorSizePx: Float,
) {
    val startWeight = (1 - offset * 2).coerceAtLeast(0f)
    val endWeight = (offset * 2 - 1).coerceAtLeast(0f)
    val blurbWeight = (1 - startWeight - endWeight).coerceAtLeast(0.01f)

    val startAngle = angle.rotateBy(spacerAngle).rotateBy(spacerAngle * endWeight)
    val sweepAngle = rotateBy(0f, spacerAngle * blurbWeight)

    drawArc(
        color,
        startAngle.toDegrees(),
        sweepAngle.toDegrees(),
        false,
        topLeft = topLeft,
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = indicatorSizePx, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawIndicators(
    radius: Float,
    rotateBy: Float.(Float) -> Float,
    startAngle: Float,
    indicatorSizePx: Float,
    spacerSizePx: Float,
    selectedColor: Color,
    unselectedColor: Color,
    pagesState: PagesState,
    offset: Float,
    topLeft: Offset,
) {
    val spacerAngle = distanceToAngle(spacerSizePx, radius)
    var angle = startAngle.rotateBy(-spacerAngle)
    for (page in 0 until pagesState.pagesOnScreen + 1) {
        if (page == pagesState.visibleDotIndex) {
            drawSelectedIndicatorArc(
                radius,
                rotateBy,
                angle,
                spacerAngle,
                offset,
                selectedColor,
                topLeft,
                indicatorSizePx,
            )
        }

        // Adjust angle by spacerAngle. Angle translates to the actual indicator (x, y) position
        angle = angle.rotateBy(spacerAngle * pagesState.spacersSizeRatio[page])
        drawIndicator(angle, page, radius, unselectedColor, pagesState, indicatorSizePx)
    }
}

private fun DrawScope.drawIndicatorArcBackground(
    radius: Float,
    startAngle: Float,
    endAngle: Float,
    topLeft: Offset,
    color: Color,
    stroke: Stroke,
) {
    val sweepAngle = endAngle - startAngle
    val signValue = if (sweepAngle < 0f) -1 else 1
    // Values below 0.2f also give some artifacts b/291753164. Do coerceAtLeast() with keeping sign
    // When bug is fixed, it will be possible to remove special logic for one page and always use
    // this function to draw the background
    val sweepAngleDeg = signValue * abs(sweepAngle.toDegrees()).coerceAtLeast(0.2f)

    drawArc(
        color,
        startAngle.toDegrees(),
        sweepAngleDeg,
        false,
        topLeft = topLeft,
        size = Size(radius * 2, radius * 2),
        style = stroke,
    )
}

/**
 * Draws a circle of circleRadius along the circumference of a circle with a bigRadius at a given
 * angle
 */
private fun DrawScope.drawCircleAtAngle(
    angle: Float,
    bigRadius: Float,
    circleRadius: Float,
    color: Color,
) {
    val x1 = center.x + bigRadius * cos(angle)
    val y1 = center.y + bigRadius * sin(angle)
    drawCircle(color, circleRadius, Offset(x1, y1))
}

/**
 * Translates the distance in pixels along the circumference of a circle to an angle. Formula:
 * angle(in radians) = distance / radius
 */
private fun distanceToAngle(distance: Float, radius: Float): Float = distance / radius

private fun DrawScope.drawIndicator(
    angle: Float,
    page: Int,
    arcRadius: Float,
    color: Color,
    pagesState: PagesState,
    indicatorSizePx: Float,
) {
    drawCircleAtAngle(
        angle,
        arcRadius,
        indicatorSizePx / 2f * pagesState.indicatorsSizeRatio[page],
        color.copy(alpha = color.alpha * pagesState.indicatorsAlpha[page]),
    )
}

/**
 * Represents an internal state of pageIndicator. This state is responsible for keeping and
 * recalculating alpha and size parameters of each indicator, spacers between them, and selected
 * indicators.
 */
private class PagesState(
    val totalPages: Int,
    val pagesOnScreen: Int,
    val smallIndicatorSizeFraction: Float,
    val shrinkThresholdStart: Float,
    val shrinkThresholdEnd: Float,
) {
    private val dotsCount = pagesOnScreen + 1
    private val spacersCount = pagesOnScreen + 2

    private var smoothProgress = 0f

    // An offset in pages, basically meaning how many pages are hidden to the left.
    private var hiddenPagesToTheLeft = 0

    // Current visible position on the screen.
    var visibleDotIndex = 0
        private set

    // Sizes and alphas of all indicators on the screen. These parameters depend on the currently
    // selected page, and how many pages are at the front and at the back of the selected page.
    val indicatorsAlpha = FloatArray(dotsCount)
    val indicatorsSizeRatio = FloatArray(dotsCount)

    // Sizes of the spacers between dots
    val spacersSizeRatio = FloatArray(spacersCount)

    // Main function responsible for recalculation of all parameters based on [selectedPage] and
    // [offset] parameters
    fun recalculateState(selectedPage: Int, offset: Float) {
        val pageWithOffset = selectedPage + offset
        // Calculating offsetInPages relating to the [selectedPage].

        // For example, for [selectedPage] = 4 we will see this picture :
        // O O O O X o. [offsetInPages] will be 0.
        // But when [selectedPage] will be incremented to 5, it will be seen as
        // o O O O X o, with [offsetInPages] = 1
        if (selectedPage > hiddenPagesToTheLeft + pagesOnScreen - 2) {
            // Set an offset as a difference between current page and pages on the screen,
            // except if this is not the last page - then offsetInPages is not changed
            hiddenPagesToTheLeft =
                (selectedPage - (pagesOnScreen - 2)).coerceAtMost(totalPages - pagesOnScreen)
        } else if (pageWithOffset <= hiddenPagesToTheLeft) {
            hiddenPagesToTheLeft = (selectedPage - 1).coerceAtLeast(0)
        }

        // Condition for scrolling to the right. A smooth scroll to the right is only triggered
        // when we have more than 2 pages to the right, and currently we're on the right edge.
        // For example -> o O O O X o -> a small "o" shows that there're more pages to the right
        val scrolledToTheRight =
            pageWithOffset > hiddenPagesToTheLeft + pagesOnScreen - 2 &&
                pageWithOffset < totalPages - 2

        // Condition for scrolling to the left. A smooth scroll to the left is only triggered
        // when we have more than 2 pages to the left, and currently we're on the left edge.
        // For example -> o X O O O o -> a small "o" shows that there're more pages to the left
        val scrolledToTheLeft = pageWithOffset > 1 && pageWithOffset < hiddenPagesToTheLeft + 1

        smoothProgress = if (scrolledToTheLeft || scrolledToTheRight) offset else 0f

        // Calculating alphas of indicators
        for (i in indicatorsAlpha.indices) {
            indicatorsAlpha[i] =
                when (i) {
                    0 -> 1 - smoothProgress
                    dotsCount - 1 -> smoothProgress
                    else -> 1f
                }
        }

        // Calculating spacer sizes between indicators
        for (i in spacersSizeRatio.indices) {
            spacersSizeRatio[i] =
                when (i) {
                    0 -> 1 - smoothProgress
                    spacersCount - 1 -> smoothProgress
                    else -> 1f
                }
        }

        // Calculating indicator sizes
        for (i in indicatorsSizeRatio.indices) {
            indicatorsSizeRatio[i] =
                when (i) {
                    // Depending on offsetInPages we'll either show a shrinked first indicator, or
                    // full-size indicator
                    0 -> {
                        if (
                            hiddenPagesToTheLeft == 0 ||
                                hiddenPagesToTheLeft == 1 && scrolledToTheLeft
                        ) {
                            1 - smoothProgress
                        } else {
                            smallIndicatorSizeFraction * (1 - smoothProgress)
                        }
                    }
                    1 -> 1 - (1 - smallIndicatorSizeFraction) * smoothProgress
                    dotsCount - 2 -> {
                        if (scrolledToTheRight || scrolledToTheLeft) {
                            lerp(smallIndicatorSizeFraction, 1f, smoothProgress)
                        } else if (hiddenPagesToTheLeft < totalPages - pagesOnScreen)
                            smallIndicatorSizeFraction
                        else 1f
                    }
                    // Depending on offsetInPages and other parameters,the last indicator will be
                    // a fraction of a shrinked or full-size indicator.
                    dotsCount - 1 -> {
                        if (
                            hiddenPagesToTheLeft == totalPages - pagesOnScreen - 1 &&
                                scrolledToTheRight ||
                                hiddenPagesToTheLeft == totalPages - pagesOnScreen &&
                                    scrolledToTheLeft
                        ) {
                            smoothProgress
                        } else {
                            smallIndicatorSizeFraction * smoothProgress
                        }
                    }
                    else -> 1f
                }
        }

        // A visibleDot represents a currently selected page on the screen
        // As we scroll to the left, we add an invisible indicator to the left, shifting all other
        // indicators to the right. The shift is only possible when a visibleDot = 1,
        // thus we have to leave it at 1 as we always add a positive offset
        visibleDotIndex =
            (if (scrolledToTheLeft) 1 else selectedPage - hiddenPagesToTheLeft).coerceAtLeast(0)

        calculateAdjacentDotParameters(
            shrinkThresholdStart,
            shrinkThresholdEnd,
            visibleDotIndex,
            offset,
        )
    }

    /**
     * This function calculates a size and alpha parameters of adjacent indicators to selected
     * indicator. It also modifies spacer sizes for properly placing adjacent indicators.
     */
    private fun calculateAdjacentDotParameters(
        shrinkThresholdStart: Float,
        shrinkThresholdEnd: Float,
        visibleDotIndex: Int,
        offset: Float,
    ) {
        val shrinkFractionPrev =
            inverseLerp(1 - shrinkThresholdStart, 1 - shrinkThresholdEnd, offset)
        val shrinkFractionNext = inverseLerp(shrinkThresholdStart, shrinkThresholdEnd, offset)

        // We change the size of the current and next visible indicator.
        indicatorsSizeRatio[visibleDotIndex] *= (1 - shrinkFractionPrev)
        indicatorsSizeRatio[visibleDotIndex + 1] *= (1 - shrinkFractionNext)

        // We have one more spacer than indicators, so spacers[visibleDotIndex] represents a spacer
        // before selected indicator, and spacers[visibleDotIndex + 2] after selected indicator
        spacersSizeRatio[visibleDotIndex] = 1 - shrinkFractionPrev / 3
        spacersSizeRatio[visibleDotIndex + 1] = 1 + shrinkFractionNext / 3 + shrinkFractionPrev / 3
        spacersSizeRatio[visibleDotIndex + 2] = 1 - shrinkFractionNext / 3

        indicatorsAlpha[visibleDotIndex] *=
            inverseLerp(0f, 0.5f, indicatorsSizeRatio[visibleDotIndex])
        indicatorsAlpha[visibleDotIndex + 1] *=
            inverseLerp(0f, 0.5f, indicatorsSizeRatio[visibleDotIndex + 1])
    }
}

private fun calculateShrinkThresholdStart(spacing: Dp, indicatorSize: Dp): Float =
    spacing / (spacing + indicatorSize) / 4

private fun calculateShrinkThresholdEnd(spacing: Dp, indicatorSize: Dp): Float =
    (spacing / 2 + indicatorSize) / (spacing + indicatorSize) / 2

private const val smallIndicatorSizeFraction = 0.66f
private const val MaxNumberOfIndicators = 6

// 0 degrees equals to 3 o'clock position, at the right of the screen
private val VerticalPagerAnchor = 0f.toRadians()
// 180 degrees equals to 9 o'clock position, at the left of the screen
private val VerticalPagerRtlAnchor = 180f.toRadians()
// 90 degrees equals to 6 o'clock position, at the bottom of the screen
private val HorizontalPagerAnchor = 90f.toRadians()
/** The default size of the indicator */
internal val PageIndicatorItemSize = 6.dp
/** The default spacing between the indicators */
internal val PageIndicatorSpacing = 4.dp
internal val BackgroundRadius = 3.dp
