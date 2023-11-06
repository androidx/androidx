/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.PageIndicatorDefaults.MaxNumberOfIndicators
import androidx.wear.compose.materialcore.PagesState
import androidx.wear.compose.materialcore.isRoundDevice
import java.lang.Integer.min

/**
 * A horizontal indicator for a Pager, representing
 * the currently active page and total pages drawn using a [Shape]. It shows up to 6 pages
 * on the screen and doesn't represent the exact page index if there are more than 6 pages.
 * Instead of showing the exact position, [HorizontalPageIndicator] shows a half-size indicator
 * on the left or on the right if there are more pages.
 *
 * Here's how different positions 0..10 might be visually represented:
 * "X" is selected item, "O" and "o" full and half size items respectively.
 *
 * O X O O O o - 2nd position out of 10. There are no more items on the left but more on the right
 * o O O O X o - might be 6, 7 or 8 out of 10, as there are more possible items
 * on the left and on the right
 * o O O O X O - is 9 out of 10, as there're no more items on the right
 *
 * [HorizontalPageIndicator] may be linear or curved, depending on [indicatorStyle]. By default
 * it depends on the screen shape of the device - for circular screens it will be curved,
 * whilst for square screens it will be linear.
 *
 * This component also allows customising the [indicatorShape], which defines how the
 * indicator is visually represented.
 *
 * @sample androidx.wear.compose.material.samples.HorizontalPageIndicatorSample
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
 * @param indicatorShape The shape of each [HorizontalPageIndicator] item.
 * Defaults to [CircleShape]
 */
@Composable
public fun HorizontalPageIndicator(
    pageIndicatorState: PageIndicatorState,
    modifier: Modifier = Modifier,
    indicatorStyle: PageIndicatorStyle = PageIndicatorDefaults.style(),
    selectedColor: Color = MaterialTheme.colors.onBackground,
    unselectedColor: Color = selectedColor.copy(alpha = 0.3f),
    indicatorSize: Dp = 6.dp,
    spacing: Dp = 4.dp,
    indicatorShape: Shape = CircleShape
) {
    // We want to bring offset to 0..1 range.
    // However, it can come in any range. It might be for example selectedPage = 1, with offset 2.5
    // We can't work with these offsets, thus should normalize them so that offset will be
    // in 0..1 range. For example selectedPage = 3, offset = -1.5 -> could transform it
    // to selectedPage = 1, offset = 0.5
    // Other example: selectedPage = 1, offset = 2.5 -> selectedPage = 3, offset = 0.5
    val normalizedSelectedPage: Int =
        (pageIndicatorState.selectedPage + pageIndicatorState.pageOffset).toInt()
    val normalizedOffset =
        pageIndicatorState.selectedPage + pageIndicatorState.pageOffset - normalizedSelectedPage

    val horizontalPadding = spacing / 2
    val spacerDefaultSize = (indicatorSize + spacing).value

    val pagesOnScreen = min(MaxNumberOfIndicators, pageIndicatorState.pageCount)
    val pagesState = remember(pageIndicatorState.pageCount) {
        PagesState(
            totalPages = pageIndicatorState.pageCount,
            pagesOnScreen = pagesOnScreen
        )
    }
    pagesState.recalculateState(normalizedSelectedPage, normalizedOffset)

    val indicatorFactory: @Composable (Int) -> Unit = { page ->
        // An external box with a fixed indicatorSize - let us remain the same size for
        // an indicator even if it's shrinked for smooth animations
        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .size(indicatorSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .scale(pagesState.sizeRatio(page))
                    .clip(indicatorShape)
                    .alpha(pagesState.alpha(page))
                    // Interpolation between unselected and selected colors depending
                    // on selectedPageRatio
                    .background(
                        lerp(
                            unselectedColor, selectedColor,
                            pagesState.calculateSelectedRatio(page, normalizedOffset)
                        )
                    )
            )
        }
    }

    val spacerLeft = @Composable {
        Spacer(
            Modifier.width((pagesState.leftSpacerSizeRatio * spacerDefaultSize).dp)
                .height(indicatorSize)
        )
    }
    val spacerRight = @Composable {
        Spacer(
            Modifier.width((pagesState.rightSpacerSizeRatio * spacerDefaultSize).dp)
                .height(indicatorSize)

        )
    }

    when (indicatorStyle) {
        PageIndicatorStyle.Linear -> {
            LinearPageIndicator(
                modifier = modifier,
                pagesOnScreen = pagesOnScreen,
                indicatorFactory = indicatorFactory,
                spacerLeft = spacerLeft,
                spacerRight = spacerRight
            )
        }
        PageIndicatorStyle.Curved -> {
            CurvedPageIndicator(
                modifier = modifier,
                pagesOnScreen = pagesOnScreen,
                indicatorFactory = indicatorFactory,
                spacerLeft = spacerLeft,
                spacerRight = spacerRight
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
     * Default style of [HorizontalPageIndicator]. Depending on shape of device, it returns either Curved
     * or Linear style.
     */
    @Composable
    public fun style(): PageIndicatorStyle =
        if (isRoundDevice()) PageIndicatorStyle.Curved
        else PageIndicatorStyle.Linear

    internal val MaxNumberOfIndicators = 6
}

/**
 * An interface for connection between Pager and [HorizontalPageIndicator].
 */
public interface PageIndicatorState {
    /**
     * The current offset from the start of [selectedPage], as a ratio of the page width.
     *
     * Changes when a scroll (drag, swipe or fling) between pages happens in Pager.
     */
    public val pageOffset: Float

    /**
     * The currently selected page index
     */
    public val selectedPage: Int

    /**
     * Total number of pages
     */
    public val pageCount: Int
}

@Composable
private fun LinearPageIndicator(
    modifier: Modifier,
    pagesOnScreen: Int,
    indicatorFactory: @Composable (Int) -> Unit,
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
        for (page in 0..pagesOnScreen) {
            indicatorFactory(page)
        }
        spacerRight()
    }
}

@Composable
private fun CurvedPageIndicator(
    modifier: Modifier,
    pagesOnScreen: Int,
    indicatorFactory: @Composable (Int) -> Unit,
    spacerLeft: @Composable () -> Unit,
    spacerRight: @Composable () -> Unit
) {
    CurvedLayout(
        modifier = modifier,
        // 90 degrees equals to 6 o'clock position, at the bottom of the screen
        anchor = 90f,
        angularDirection = CurvedDirection.Angular.Reversed
    ) {
        // drawing 1 extra spacer for transition
        curvedComposable {
            spacerLeft()
        }
        for (page in 0..pagesOnScreen) {
            curvedComposable {
                indicatorFactory(page)
            }
        }
        curvedComposable {
            spacerRight()
        }
    }
}
