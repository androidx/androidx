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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.radialSize
import androidx.wear.compose.foundation.size
import androidx.wear.compose.foundation.weight
import androidx.wear.compose.material3.PageIndicatorDefaults.MaxNumberOfIndicators
import androidx.wear.compose.materialcore.PagesState
import androidx.wear.compose.materialcore.isLayoutDirectionRtl
import androidx.wear.compose.materialcore.isRoundDevice

/**
 * Horizontal page indicator for use with [HorizontalPager], representing
 * the currently active page and the total number of pages.
 * Pages are indicated as a Circle shape.
 * The indicator shows up to six pages individually -
 * if there are more than six pages, [HorizontalPageIndicator] shows a
 * half-size indicator to the left or right to indicate that more are available.
 *
 * Here's how different positions 0..10 might be visually represented:
 * "X" is selected item, "O" and "o" full and half size items respectively.
 *
 * O X O O O o - 2nd position out of 10. There are no more items on the left but more on the right
 * o O O O X o - current page could be 6, 7 or 8 out of 10, as there are more possible items
 * on the left and on the right
 * o O O O X O - current page is 9 out of 10, as there're no more items on the right
 *
 * [HorizontalPageIndicator] may be linear or curved, depending on [indicatorStyle]. By default
 * it depends on the screen shape of the device - for circular screens it will be curved,
 * whilst for square screens it will be linear.
 *
 * @sample androidx.wear.compose.material3.samples.HorizontalPageIndicatorSample
 *
 * @param pageIndicatorState The state object of a [HorizontalPageIndicator] to be used to
 * observe the Pager's state.
 * @param modifier Modifier to be applied to the [HorizontalPageIndicator]
 * @param indicatorStyle The style of [HorizontalPageIndicator] - may be linear or curved.
 * By default determined by the screen shape.
 * @param selectedColor The color of the selected [HorizontalPageIndicator] item
 * @param unselectedColor The color of unselected [HorizontalPageIndicator] items.
 * Defaults to [selectedColor] with 30% alpha
 * @param indicatorSize The size of each [HorizontalPageIndicator] item in [Dp]
 * @param spacing The spacing between indicator items in [Dp]
 **/
@Composable
public fun HorizontalPageIndicator(
    pageIndicatorState: PageIndicatorState,
    modifier: Modifier = Modifier,
    indicatorStyle: PageIndicatorStyle = PageIndicatorDefaults.style(),
    selectedColor: Color = MaterialTheme.colorScheme.onBackground,
    unselectedColor: Color = selectedColor.copy(alpha = 0.3f),
    indicatorSize: Dp = 6.dp,
    spacing: Dp = 4.dp
) {
    val selectedPage: Int = pageIndicatorState.selectedPageWithOffset().toInt()
    val offset = pageIndicatorState.selectedPageWithOffset() - selectedPage

    val pagesOnScreen = Integer.min(MaxNumberOfIndicators, pageIndicatorState.pageCount)
    val pagesState = remember(pageIndicatorState.pageCount) {
        PagesState(
            totalPages = pageIndicatorState.pageCount,
            pagesOnScreen = pagesOnScreen
        )
    }
    pagesState.recalculateState(selectedPage, offset)

    val leftSpacerSize = (indicatorSize + spacing) * pagesState.leftSpacerSizeRatio
    val rightSpacerSize = (indicatorSize + spacing) * pagesState.rightSpacerSizeRatio

    when (indicatorStyle) {
        PageIndicatorStyle.Linear -> {
            LinearPageIndicator(
                modifier = modifier,
                visibleDotIndex = pagesState.visibleDotIndex,
                pagesOnScreen = pagesOnScreen,
                indicator = { page ->
                    LinearIndicator(
                        page = page,
                        pagesState = pagesState,
                        unselectedColor = unselectedColor,
                        indicatorSize = indicatorSize,
                        spacing = spacing,
                    )
                },
                selectedIndicator = {
                    LinearSelectedIndicator(
                        indicatorSize = indicatorSize,
                        spacing = spacing,
                        selectedColor = selectedColor,
                        progress = offset
                    )
                },
                spacerLeft = { LinearSpacer(leftSpacerSize) },
                spacerRight = { LinearSpacer(rightSpacerSize) }
            )
        }

        PageIndicatorStyle.Curved -> {
            CurvedPageIndicator(
                modifier = modifier,
                visibleDotIndex = pagesState.visibleDotIndex,
                pagesOnScreen = pagesOnScreen,
                indicator = { page ->
                    curvedIndicator(
                        page = page,
                        size = indicatorSize,
                        unselectedColor = unselectedColor,
                        pagesState = pagesState
                    )
                },
                itemsSpacer = { curvedSpacer(indicatorSize + spacing) },
                selectedIndicator = {
                    curvedSelectedIndicator(
                        indicatorSize = indicatorSize,
                        spacing = spacing,
                        selectedColor = selectedColor,
                        progress = offset
                    )
                },
                spacerLeft = { curvedSpacer(leftSpacerSize) },
                spacerRight = { curvedSpacer(rightSpacerSize) }
            )
        }
    }
}

/**
 * The style of [HorizontalPageIndicator]. May be Curved or Linear
 */
@kotlin.jvm.JvmInline
public value class PageIndicatorStyle internal constructor(internal val value: Int) {
    companion object {
        /**
         * Curved style of [HorizontalPageIndicator]
         */
        public val Curved = PageIndicatorStyle(0)

        /**
         * Linear style of [HorizontalPageIndicator]
         */
        public val Linear = PageIndicatorStyle(1)
    }
}

/**
 * Contains the default values used by [HorizontalPageIndicator]
 */
public object PageIndicatorDefaults {

    /**
     * Default style of [HorizontalPageIndicator]. Depending on shape of device,
     * it returns either Curved or Linear style.
     */
    @Composable
    public fun style(): PageIndicatorStyle =
        if (isRoundDevice()) PageIndicatorStyle.Curved
        else PageIndicatorStyle.Linear

    internal val MaxNumberOfIndicators = 6
}

// TODO(b/290732498): Add rememberPageIndicatorState for HorizontalPager
//  once HorizontalPager is stable

/**
 * Creates and remembers [PageIndicatorState] based on [maxPages] and [selectedPageWithOffset]
 * parameters.
 */
@ExperimentalWearMaterial3Api
@Composable
public fun rememberPageIndicatorState(
    maxPages: Int,
    @Suppress("PrimitiveInLambda")
    selectedPageWithOffset: () -> Float
): PageIndicatorState =
    remember(maxPages, selectedPageWithOffset) {
        object : PageIndicatorState {

            override val selectedPageWithOffset: () -> Float
                get() = selectedPageWithOffset

            override val pageCount: Int
                get() = maxPages
        }
    }

/**
 * An interface for connection between Pager and [HorizontalPageIndicator].
 */
public interface PageIndicatorState {
    /**
     * The currently selected page index with offset.
     * Integer part represents the selected page index and the fractional part represents
     * the offset as a fraction of the transition from the selected page
     * to the next page in the range 0f..1f
     *
     * For example 5.5f equals to selectedPage = 5, offset 0.5f
     *
     * Changes when a scroll (drag, swipe or fling) between pages happens in Pager.
     */
    @Suppress("PrimitiveInLambda")
    @get:FloatRange(from = 0.0)
    public val selectedPageWithOffset: () -> Float

    /**
     * Total number of pages
     */
    @get:IntRange(from = 0)
    public val pageCount: Int
}

@Composable
private fun LinearPageIndicator(
    modifier: Modifier,
    visibleDotIndex: Int,
    pagesOnScreen: Int,
    @Suppress("PrimitiveInLambda")
    indicator: @Composable (Int) -> Unit,
    selectedIndicator: @Composable () -> Unit,
    spacerLeft: @Composable () -> Unit,
    spacerRight: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // drawing 1 extra spacer for transition
        spacerLeft()
        for (page in 0 until visibleDotIndex) {
            indicator(page)
        }
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                indicator(visibleDotIndex)
                indicator(visibleDotIndex + 1)
            }
            Box {
                selectedIndicator()
            }
        }
        for (page in visibleDotIndex + 2..pagesOnScreen) {
            indicator(page)
        }
        spacerRight()
    }
}

@Composable
private fun LinearSelectedIndicator(
    indicatorSize: Dp,
    spacing: Dp,
    selectedColor: Color,
    progress: Float
) {
    val horizontalPadding = spacing / 2
    val isRtl = isLayoutDirectionRtl()
    Spacer(
        modifier = Modifier
            .drawWithCache {
                // Adding 2px to fully cover edges of non-selected indicators
                val strokeWidth = indicatorSize.toPx() + 2
                val startX = horizontalPadding.toPx() + strokeWidth / 2
                val endX = this.size.width - horizontalPadding.toPx() - strokeWidth / 2
                val drawWidth = endX - startX

                val startSpacerWeight = (progress * 2 - 1).coerceAtLeast(0f)
                val endSpacerWeight = (1 - progress * 2).coerceAtLeast(0f)

                // Adding +1 or -1 for cases when start and end have the same coordinates -
                // otherwise on APIs <= 26 line will not be drawn
                val additionalPixel = if (isRtl) -1 else 1

                val start = Offset(
                    startX + drawWidth * (if (isRtl) startSpacerWeight else endSpacerWeight) +
                        additionalPixel,
                    this.size.height / 2
                )
                val end = Offset(
                    endX - drawWidth * (if (isRtl) endSpacerWeight else startSpacerWeight),
                    this.size.height / 2
                )
                onDrawBehind {
                    drawLine(
                        color = selectedColor,
                        start = start,
                        end = end,
                        cap = StrokeCap.Round,
                        strokeWidth = strokeWidth
                    )
                }
            }
    )
}

@Composable
private fun LinearIndicator(
    page: Int,
    pagesState: PagesState,
    unselectedColor: Color,
    indicatorSize: Dp,
    spacing: Dp,
) {
    Spacer(
        modifier = Modifier
            .padding(horizontal = spacing / 2)
            .size(indicatorSize)
            .drawWithCache {
                val strokeWidth = indicatorSize.toPx() * pagesState.sizeRatio(page)
                val start = Offset(strokeWidth / 2 + 1, this.size.height / 2)
                val end = Offset(strokeWidth / 2, this.size.height / 2)
                onDrawBehind {
                    drawLine(
                        color = unselectedColor,
                        start = start,
                        end = end,
                        cap = StrokeCap.Round,
                        alpha = pagesState.alpha(page),
                        strokeWidth = strokeWidth
                    )
                }
            }
    )
}

@Composable
private fun LinearSpacer(leftSpacerSize: Dp) {
    Spacer(Modifier.size(leftSpacerSize, 0.dp))
}

@Composable
private fun CurvedPageIndicator(
    modifier: Modifier,
    visibleDotIndex: Int,
    pagesOnScreen: Int,
    @Suppress("PrimitiveInLambda")
    indicator: CurvedScope.(Int) -> Unit,
    itemsSpacer: CurvedScope.() -> Unit,
    selectedIndicator: CurvedScope.() -> Unit,
    spacerLeft: CurvedScope.() -> Unit,
    spacerRight: CurvedScope.() -> Unit
) {
    CurvedLayout(
        modifier = modifier,
        // 90 degrees equals to 6 o'clock position, at the bottom of the screen
        anchor = 90f,
        angularDirection = CurvedDirection.Angular.Reversed
    ) {
        // drawing 1 extra spacer for transition
        spacerLeft()

        curvedRow(radialAlignment = CurvedAlignment.Radial.Center) {
            for (page in 0 until visibleDotIndex) {
                indicator(page)
                itemsSpacer()
            }
            curvedBox(radialAlignment = CurvedAlignment.Radial.Center) {
                curvedRow(radialAlignment = CurvedAlignment.Radial.Center) {
                    indicator(visibleDotIndex)
                    itemsSpacer()
                    indicator(visibleDotIndex + 1)
                }
                selectedIndicator()
            }
            for (page in visibleDotIndex + 2..pagesOnScreen) {
                itemsSpacer()
                indicator(page)
            }
        }
        spacerRight()
    }
}

private fun CurvedScope.curvedSelectedIndicator(
    indicatorSize: Dp,
    spacing: Dp,
    selectedColor: Color,
    progress: Float
) {

    val startSpacerWeight = (1 - progress * 2).coerceAtLeast(0f)
    val endSpacerWeight = (progress * 2 - 1).coerceAtLeast(0f)
    val blurbWeight = (1 - startSpacerWeight - endSpacerWeight).coerceAtLeast(0.01f)

    // Add 0.5dp to cover the sweepDegrees of unselected indicators
    curvedRow(CurvedModifier.angularSizeDp(spacing + indicatorSize + 0.5.dp)) {
        if (endSpacerWeight > 0f) {
            curvedRow(CurvedModifier.weight(endSpacerWeight)) { }
        }
        curvedRow(
            CurvedModifier
                .background(selectedColor, cap = StrokeCap.Round)
                .weight(blurbWeight)
                // Adding 0.3dp to fully cover edges of non-selected indicators
                .radialSize(indicatorSize + 0.3.dp)
        ) { }
        if (startSpacerWeight > 0f) {
            curvedRow(CurvedModifier.weight(startSpacerWeight)) { }
        }
    }
}

private fun CurvedScope.curvedIndicator(
    page: Int,
    unselectedColor: Color,
    pagesState: PagesState,
    size: Dp
) {
    curvedBox(
        CurvedModifier
            // Ideally we want sweepDegrees to be = 0f, because the circular shape is drawn
            // by the Round StrokeCap.
            // But it can't have 0f value due to limitations of underlying Canvas.
            // Values below 0.2f also give some artifacts b/291753164
            .size(0.2f, size * pagesState.sizeRatio(page))
            .background(
                color = unselectedColor.copy(
                    alpha = unselectedColor.alpha * pagesState.alpha(page)
                ),
                cap = StrokeCap.Round
            )
    ) { }
}

private fun CurvedScope.curvedSpacer(size: Dp) {
    curvedBox(CurvedModifier.angularSizeDp(size).radialSize(0.dp)) { }
}
