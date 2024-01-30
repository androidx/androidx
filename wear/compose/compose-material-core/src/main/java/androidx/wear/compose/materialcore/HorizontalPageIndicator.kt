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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import kotlin.math.abs

/**
 * Represents an internal state of pageIndicator. This state is responsible for keeping and
 * recalculating alpha and size parameters of each indicator, and selected indicators as well.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PagesState(
    val totalPages: Int,
    val pagesOnScreen: Int
) {
    // Sizes and alphas of first and last indicators on the screen. Used to show that there're more
    // pages on the left or on the right, and also for smooth transitions
    private var firstAlpha = 1f
    private var lastAlpha = 0f
    private var firstSize = 1f
    private var secondSize = 1f
    private var lastSize = 1f
    private var lastButOneSize = 1f

    private var smoothProgress = 0f

    // An offset in pages, basically meaning how many pages are hidden to the left.
    private var hiddenPagesToTheLeft = 0

    // A default size of spacers - invisible items to the left and to the right of
    // visible indicators, used for smooth transitions

    // Current visible position on the screen.
    var visibleDotIndex = 0
        private set

    // A size of a left spacer used for smooth transitions
    val leftSpacerSizeRatio
        get() = 1 - smoothProgress

    // A size of a right spacer used for smooth transitions
    val rightSpacerSizeRatio
        get() = smoothProgress

    /**
     * Depending on the page index, return an alpha for this indicator
     *
     * @param page Page index
     * @return An alpha of page index- in range 0..1
     */
    fun alpha(page: Int): Float =
        when (page) {
            0 -> firstAlpha
            pagesOnScreen -> lastAlpha
            else -> 1f
        }

    /**
     * Depending on the page index, return a size ratio for this indicator
     *
     * @param page Page index
     * @return An size ratio for page index - in range 0..1
     */
    fun sizeRatio(page: Int): Float =
        when (page) {
            0 -> firstSize
            1 -> secondSize
            pagesOnScreen - 1 -> lastButOneSize
            pagesOnScreen -> lastSize
            else -> 1f
        }

    /**
     * Returns a value in the range 0..1 where 0 is unselected state, and 1 is selected.
     * Used to show a smooth transition between page indicator items.
     */
    fun calculateSelectedRatio(targetPage: Int, offset: Float): Float =
        (1 - abs(visibleDotIndex + offset - targetPage)).coerceAtLeast(0f)

    // Main function responsible for recalculation of all parameters regarding
    // to the [selectedPage] and [offset]
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
            hiddenPagesToTheLeft = (selectedPage - (pagesOnScreen - 2))
                .coerceAtMost(totalPages - pagesOnScreen)
        } else if (pageWithOffset <= hiddenPagesToTheLeft) {
            hiddenPagesToTheLeft = (selectedPage - 1).coerceAtLeast(0)
        }

        // Condition for scrolling to the right. A smooth scroll to the right is only triggered
        // when we have more than 2 pages to the right, and currently we're on the right edge.
        // For example -> o O O O X o -> a small "o" shows that there're more pages to the right
        val scrolledToTheRight = pageWithOffset > hiddenPagesToTheLeft + pagesOnScreen - 2 &&
            pageWithOffset < totalPages - 2

        // Condition for scrolling to the left. A smooth scroll to the left is only triggered
        // when we have more than 2 pages to the left, and currently we're on the left edge.
        // For example -> o X O O O o -> a small "o" shows that there're more pages to the left
        val scrolledToTheLeft = pageWithOffset > 1 && pageWithOffset < hiddenPagesToTheLeft + 1

        smoothProgress = if (scrolledToTheLeft || scrolledToTheRight) offset else 0f

        // Calculating exact parameters for border indicators like [firstAlpha], [lastSize], etc.
        firstAlpha = 1 - smoothProgress
        lastAlpha = smoothProgress
        secondSize = 1 - 0.5f * smoothProgress

        // Depending on offsetInPages we'll either show a shrinked first indicator, or full-size
        firstSize = if (hiddenPagesToTheLeft == 0 ||
            hiddenPagesToTheLeft == 1 && scrolledToTheLeft
        ) {
            1 - smoothProgress
        } else {
            0.5f * (1 - smoothProgress)
        }

        // Depending on offsetInPages and other parameters, we'll either show a shrinked
        // last indicator, or full-size
        lastSize =
            if (hiddenPagesToTheLeft == totalPages - pagesOnScreen - 1 && scrolledToTheRight ||
                hiddenPagesToTheLeft == totalPages - pagesOnScreen && scrolledToTheLeft
            ) {
                smoothProgress
            } else {
                0.5f * smoothProgress
            }

        lastButOneSize = if (scrolledToTheRight || scrolledToTheLeft) {
            0.5f * (1 + smoothProgress)
        } else if (hiddenPagesToTheLeft < totalPages - pagesOnScreen) 0.5f else 1f

        // A visibleDot represents a currently selected page on the screen
        // As we scroll to the left, we add an invisible indicator to the left, shifting all other
        // indicators to the right. The shift is only possible when a visibleDot = 1,
        // thus we have to leave it at 1 as we always add a positive offset
        visibleDotIndex = if (scrolledToTheLeft) 1
        else selectedPage - hiddenPagesToTheLeft
    }
}
