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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import kotlin.math.abs

/**
 * A horizontal indicator for a Pager, representing
 * the currently active page and total pages drawn using a [Shape].
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
 **/
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
    when (indicatorStyle) {
        PageIndicatorStyle.Linear -> {
            LinearPageIndicator(
                pageIndicatorState,
                modifier,
                selectedColor,
                unselectedColor,
                indicatorSize,
                spacing,
                indicatorShape
            )
        }
        PageIndicatorStyle.Curved -> {
            CurvedPageIndicator(
                pageIndicatorState,
                modifier,
                selectedColor,
                unselectedColor,
                indicatorSize,
                spacing,
                indicatorShape
            )
        }
    }
}

/**
 * The style of [HorizontalPageIndicator]. May be Curved or Linear
 */
public enum class PageIndicatorStyle {
    Curved,
    Linear
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
    fun style(): PageIndicatorStyle =
        if (isRoundDevice()) PageIndicatorStyle.Curved
        else PageIndicatorStyle.Linear
}

/**
 * An interface for connection between Pager and [HorizontalPageIndicator].
 */
public interface PageIndicatorState {
    /**
     * The current offset from the start of [selectedPage], as a ratio of the page width.
     * May be in range -1..1.
     *
     * Changes when a scroll (drag, swipe or fling) between pages happens in Pager.
     */
    val pageOffset: Float

    /**
     * The currently selected page index
     */
    val selectedPage: Int

    /**
     * Total number of pages
     */
    val pageCount: Int
}

@Composable
private fun LinearPageIndicator(
    pageIndicatorState: PageIndicatorState,
    modifier: Modifier,
    selectedColor: Color,
    unselectedColor: Color,
    indicatorSize: Dp,
    spacing: Dp,
    indicatorShape: Shape
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        for (page in 0 until pageIndicatorState.pageCount) {
            Item(
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                indicatorSize = indicatorSize,
                horizontalPadding = spacing / 2,
                indicatorShape = indicatorShape,
                selectedPageRatio = calculateSelectedRatio(
                    page,
                    pageIndicatorState.selectedPage,
                    pageIndicatorState.pageOffset
                )
            )
        }
    }
}

@Composable
private fun CurvedPageIndicator(
    pageIndicatorState: PageIndicatorState,
    modifier: Modifier,
    selectedColor: Color,
    unselectedColor: Color,
    indicatorSize: Dp,
    spacing: Dp,
    indicatorShape: Shape
) {
    // TODO: b/218985697 additional flexible layout will be added later to properly show pages > 6
    CurvedLayout(
        modifier = modifier,
        // 90 degrees equals to 6 o'clock position, at the bottom of the screen
        anchor = 90f,
        // Since Swipe to dismiss is always LtR, we want to follow the same direction when
        // navigating amongst pages, so the first page is always on the left.
        angularDirection = CurvedDirection.Angular.CounterClockwise
    ) {
        for (page in 0 until pageIndicatorState.pageCount) {
            curvedComposable {
                Item(
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    indicatorSize = indicatorSize,
                    horizontalPadding = spacing / 2,
                    indicatorShape = indicatorShape,
                    selectedPageRatio = calculateSelectedRatio(
                        page,
                        pageIndicatorState.selectedPage,
                        pageIndicatorState.pageOffset
                    )
                )
            }
        }
    }
}

/**
 * Returns a value in the range 0..1 where 0 is unselected state, and 1 is selected.
 * Used to show a smooth transition between page indicator items.
 */
private fun calculateSelectedRatio(targetPage: Int, selectedPage: Int, pageOffset: Float): Float =
    (1 - abs(selectedPage + pageOffset - targetPage)).coerceAtLeast(0f)

@Composable
private fun Item(
    selectedColor: Color,
    unselectedColor: Color,
    indicatorSize: Dp,
    horizontalPadding: Dp,
    indicatorShape: Shape,
    selectedPageRatio: Float
) {
    Box(
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .clip(indicatorShape)
            // Interpolation between unselected and selected colors depending on selectedPageRatio
            .background(lerp(unselectedColor, selectedColor, selectedPageRatio))
            .size(indicatorSize)
    )
}