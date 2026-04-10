/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

private const val InfinitePageCount = 100_000

/**
 * A state object that can be hoisted to observe and control the scrolling behavior of a
 * [ScrollField].
 *
 * In most cases, this state should be created via [rememberScrollFieldState].
 *
 * @param pagerState the underlying [PagerState] used to handle the scroll logic.
 * @param itemCount the total number of unique items available in the scroll field.
 */
@ExperimentalMaterial3ExpressiveApi
@Stable
class ScrollFieldState(internal val pagerState: PagerState, val itemCount: Int) {
    /**
     * The index of the currently selected option.
     *
     * This value is always clamped between 0 and [itemCount] - 1. When the internal pager is
     * scrolled, this value updates to reflect the item closest to the snap position.
     */
    val selectedOption: Int
        get() = if (itemCount > 0) pagerState.currentPage % itemCount else 0

    /**
     * Instantly scrolls to the specified [option].
     *
     * @param option the index of the item to scroll to.
     * @see animateScrollToOption for a smooth transition.
     */
    suspend fun scrollToOption(option: Int) {
        val targetPage = calculateTargetPage(option)
        pagerState.scrollToPage(targetPage)
    }

    /**
     * Animates the scroll to the specified [option].
     *
     * @param option the index of the item to animate to.
     * @see scrollToOption for an instant scroll.
     */
    suspend fun animateScrollToOption(option: Int) {
        val targetPage = calculateTargetPage(option)
        pagerState.animateScrollToPage(targetPage)
    }

    private fun calculateTargetPage(option: Int): Int {
        val currentContextPage = pagerState.currentPage
        val currentOption = currentContextPage % itemCount
        val diff = option - currentOption
        return currentContextPage + diff
    }
}

/**
 * Creates and remembers a [ScrollFieldState] to be used with a [ScrollField].
 *
 * @param itemCount the total number of unique items to be displayed in the scrollable wheel.
 * @param index the initial selected index of the scroll field.
 * @return a [ScrollFieldState] that can be used to control or observe the scroll field.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun rememberScrollFieldState(itemCount: Int, index: Int = 0): ScrollFieldState {
    val initialPage =
        remember(itemCount, index) {
            (InfinitePageCount / 2) - (InfinitePageCount / 2 % itemCount) + index
        }
    val pagerState = rememberPagerState(initialPage = initialPage) { InfinitePageCount }

    return remember(pagerState, itemCount) { ScrollFieldState(pagerState, itemCount) }
}

/**
 * ScrollField's can be used to provide a more interactive way to select a time or other numerical
 * value.
 *
 * Generic ScrollField for scrollable numerical selection:
 *
 * @sample androidx.compose.material3.samples.ScrollFieldSample
 *
 * ScrollField for time selection:
 *
 * @sample androidx.compose.material3.samples.TimeScrollFieldSample
 * @param state the state object to be used to control or observe the pager's state.
 * @param modifier the [Modifier] to be applied to the ScrollField container.
 * @param colors [ScrollFieldColors] that will be used to resolve the colors used for this
 *   ScrollField in different states.
 * @param field the composable used to render each item in the wheel.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ScrollField(
    state: ScrollFieldState,
    modifier: Modifier = Modifier,
    colors: ScrollFieldColors = ScrollFieldDefaults.colors(),
    field: @Composable (index: Int, selected: Boolean) -> Unit = { index, selected ->
        ScrollFieldDefaults.Item(index = index, selected = selected, colors = colors)
    },
) {
    VerticalPager(
        state = state.pagerState,
        modifier = modifier.background(colors.containerColor, shape = ScrollFieldDefaults.shape),
        pageSize = PageSize.Fixed(ScrollFieldDefaults.ScrollFieldHeight / 3),
        horizontalAlignment = Alignment.CenterHorizontally,
        snapPosition = SnapPosition.Center,
    ) { page ->
        val index = page % state.itemCount
        val isSelected = state.pagerState.currentPage == page

        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            field(index, isSelected)
        }
    }
}

/** Represents the colors used by a [ScrollField] in different states. */
@ExperimentalMaterial3ExpressiveApi
@Immutable
class ScrollFieldColors(
    val containerColor: Color,
    val unselectedContentColor: Color,
    val selectedContentColor: Color,
) {

    /**
     * Returns a copy of this ScrollFieldColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source".
     */
    fun copy(
        containerColor: Color = this.containerColor,
        unselectedContentColor: Color = this.unselectedContentColor,
        selectedContentColor: Color = this.selectedContentColor,
    ) =
        ScrollFieldColors(
            containerColor.takeOrElse { this.containerColor },
            unselectedContentColor.takeOrElse { this.unselectedContentColor },
            selectedContentColor.takeOrElse { this.selectedContentColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ScrollFieldColors) return false

        if (containerColor != other.containerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (selectedContentColor != other.selectedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        return result
    }
}

/** Object to hold defaults used by [ScrollField]. */
@ExperimentalMaterial3ExpressiveApi
@Stable
object ScrollFieldDefaults {
    /**
     * The default height for a [ScrollField]. This can be used as a reference when providing a
     * Modifier.height to the ScrollField to ensure enough vertical space is available to display
     * the typical three-item layout.
     */
    val ScrollFieldHeight = 200.dp

    /** The default shape for the [ScrollField] container background. */
    val shape: Shape
        @Composable get() = ShapeDefaults.Large

    /** Default colors used by a [ScrollField]. */
    @Composable fun colors(): ScrollFieldColors = MaterialTheme.colorScheme.defaultScrollFieldColors

    /**
     * Creates a [ScrollFieldColors] that represents the default container, unselected, and selected
     * colors used in a [ScrollField].
     *
     * @param containerColor The color of the [ScrollField] container.
     * @param unselectedContentColor The color of the numerical value(s) visible on the screen that
     *   are not chosen.
     * @param selectedContentColor The color of the numerical value that is centered and snapped
     *   into place.
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        unselectedContentColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultScrollFieldColors.copy(
            containerColor = containerColor,
            unselectedContentColor = unselectedContentColor,
            selectedContentColor = selectedContentColor,
        )

    internal val ColorScheme.defaultScrollFieldColors: ScrollFieldColors
        @Composable
        get() {
            return defaultScrollFieldColorsCached
                ?: ScrollFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unselectedContentColor = MaterialTheme.colorScheme.outline,
                        selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                    .also { defaultScrollFieldColorsCached = it }
        }

    /**
     * The default item implementation for [ScrollField].
     *
     * @param index the current item index.
     * @param selected whether this item is currently selected (centered).
     * @param colors the colors to use for the text content.
     */
    @Composable
    fun Item(index: Int, selected: Boolean, colors: ScrollFieldColors = colors()) {
        Text(
            text = index.toLocalString(minDigits = 2),
            style =
                if (selected) {
                    MaterialTheme.typography.displayLarge
                } else {
                    MaterialTheme.typography.displayMedium
                },
            color = if (selected) colors.selectedContentColor else colors.unselectedContentColor,
        )
    }
}
