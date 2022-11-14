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

package androidx.compose.foundation.pager

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * A Pager that scrolls horizontally. Pages are lazily placed in accordance to the available
 * viewport size. You can use [beyondBoundsPageCount] to place more pages before and after the
 * visible pages.
 * @param pageCount The number of pages this Pager will contain
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param state The state to control this pager
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondBoundsPageCount Pages to load before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using Lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param flingBehavior The [FlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param pageContent This Pager's page Composable.
 */
@Composable
@ExperimentalFoundationApi
internal fun HorizontalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: FlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageContent: @Composable (page: Int) -> Unit
) {
    Pager(
        modifier = modifier,
        state = state,
        pageCount = pageCount,
        pageSpacing = pageSpacing,
        userScrollEnabled = userScrollEnabled,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSize = pageSize,
        flingBehavior = flingBehavior,
        pageContent = pageContent
    )
}

/**
 * A Pager that scrolls vertically. Tha backing mechanism for this is a LazyList, therefore
 * pages are lazily placed in accordance to the available viewport size. You can use
 * [beyondBoundsPageCount] to place more pages before and after the visible pages.
 *
 * @param pageCount The number of pages this Pager will contain
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param state The state to control this pager
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondBoundsPageCount Pages to load before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using Lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param horizontalAlignment How pages are aligned horizontally in this Pager.
 * @param flingBehavior The [FlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param pageContent This Pager's page Composable.
 */
@Composable
@ExperimentalFoundationApi
internal fun VerticalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageContent: @Composable (page: Int) -> Unit
) {
    Pager(
        modifier = modifier,
        state = state,
        pageCount = pageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        orientation = Orientation.Vertical,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSize = pageSize,
        flingBehavior = flingBehavior,
        pageContent = pageContent
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Pager(
    modifier: Modifier,
    state: PagerState,
    pageCount: Int,
    pageSize: PageSize,
    pageSpacing: Dp,
    orientation: Orientation,
    beyondBoundsPageCount: Int,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    contentPadding: PaddingValues,
    flingBehavior: FlingBehavior,
    userScrollEnabled: Boolean,
    reverseLayout: Boolean,
    pageContent: @Composable (page: Int) -> Unit
) {
    require(beyondBoundsPageCount >= 0) {
        "beyondBoundsPageCount should be greater than or equal to 0, " +
            "you selected $beyondBoundsPageCount"
    }

    val isVertical = orientation == Orientation.Vertical
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val calculatedContentPaddings = remember(contentPadding, orientation, layoutDirection) {
        calculateContentPaddings(
            contentPadding,
            orientation,
            layoutDirection
        )
    }

    LaunchedEffect(density, state, pageSpacing) {
        with(density) { state.pageSpacing = pageSpacing.roundToPx() }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .drop(1) // Initial scroll is false
            .collect { state.updateOnScrollStopped() }
    }

    BoxWithConstraints(modifier = modifier) {
        val mainAxisSize = if (isVertical) constraints.maxHeight else constraints.maxWidth
        // Calculates how pages are shown across the main axis
        val pageAvailableSize = remember(
            density,
            mainAxisSize,
            pageSpacing,
            calculatedContentPaddings
        ) {
            with(density) {
                val pageSpacingPx = pageSpacing.roundToPx()
                val contentPaddingPx = calculatedContentPaddings.roundToPx()
                with(pageSize) {
                    density.calculateMainAxisPageSize(
                        mainAxisSize - contentPaddingPx,
                        pageSpacingPx
                    )
                }.toDp()
            }
        }

        val horizontalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Start else Alignment.End
        val verticalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Top else Alignment.Bottom

        LazyList(
            modifier = Modifier,
            state = state.lazyListState,
            contentPadding = contentPadding,
            flingBehavior = flingBehavior,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = Arrangement.spacedBy(
                pageSpacing,
                horizontalAlignmentForSpacedArrangement
            ),
            verticalArrangement = Arrangement.spacedBy(
                pageSpacing,
                verticalAlignmentForSpacedArrangement
            ),
            verticalAlignment = verticalAlignment,
            isVertical = isVertical,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            beyondBoundsItemCount = beyondBoundsPageCount
        ) {
            items(pageCount) {
                val pageMainAxisSizeModifier = if (isVertical) {
                    Modifier.height(pageAvailableSize)
                } else {
                    Modifier.width(pageAvailableSize)
                }
                Box(
                    modifier = Modifier.then(pageMainAxisSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    pageContent(it)
                }
            }
        }
    }
}

private fun calculateContentPaddings(
    contentPadding: PaddingValues,
    orientation: Orientation,
    layoutDirection: LayoutDirection
): Dp {

    val startPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateTopPadding()
    } else {
        contentPadding.calculateLeftPadding(layoutDirection)
    }

    val endPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateBottomPadding()
    } else {
        contentPadding.calculateRightPadding(layoutDirection)
    }

    return startPadding + endPadding
}

/**
 * This is used to determine how Pages are laid out in [Pager]. By changing the size of the pages
 * one can change how many pages are shown.
 */
@ExperimentalFoundationApi
internal interface PageSize {

    /**
     * Based on [availableSpace] pick a size for the pages
     * @param availableSpace The amount of space the pages in this Pager can use.
     * @param pageSpacing The amount of space used to separate pages.
     */
    fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int

    /**
     * Pages take up the whole Pager size.
     */
    @ExperimentalFoundationApi
    object Fill : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return availableSpace
        }
    }

    /**
     * Multiple pages in a viewport
     * @param pageSize A fixed size for pages
     */
    @ExperimentalFoundationApi
    class Fixed(val pageSize: Dp) : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return pageSize.roundToPx()
        }
    }
}

@ExperimentalFoundationApi
internal object PagerDefaults {

    /**
     * @param state The [PagerState] that controls the which to which this FlingBehavior will
     * be applied to.
     * @param pagerSnapDistance A way to control the snapping destination for this [Pager].
     * The default behavior will result in any fling going to the next page in the direction of the
     * fling (if the fling has enough velocity, otherwise we'll bounce back). Use
     * [PagerSnapDistance.atMost] to define a maximum number of pages this [Pager] is allowed to
     * fling after scrolling is finished and fling has started.
     * @param lowVelocityAnimationSpec The animation spec used to approach the target offset. When
     * the fling velocity is not large enough. Large enough means large enough to naturally decay.
     * @param highVelocityAnimationSpec The animation spec used to approach the target offset. When
     * the fling velocity is large enough. Large enough means large enough to naturally decay.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     *
     * @return An instance of [FlingBehavior] that will perform Snapping to the next page. The
     * animation will be governed by the post scroll velocity and we'll use either
     * [lowVelocityAnimationSpec] or [highVelocityAnimationSpec] to approach the snapped position
     * and the last step of the animation will be performed by [snapAnimationSpec].
     */
    @Composable
    fun flingBehavior(
        state: PagerState,
        pagerSnapDistance: PagerSnapDistance = PagerSnapDistance.atMost(1),
        lowVelocityAnimationSpec: AnimationSpec<Float> = tween(easing = LinearOutSlowInEasing),
        highVelocityAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    ): FlingBehavior {
        val density = LocalDensity.current

        return remember(
            lowVelocityAnimationSpec,
            highVelocityAnimationSpec,
            snapAnimationSpec,
            pagerSnapDistance,
            density
        ) {
            val snapLayoutInfoProvider =
                SnapLayoutInfoProvider(state, pagerSnapDistance, highVelocityAnimationSpec)
            SnapFlingBehavior(
                snapLayoutInfoProvider = snapLayoutInfoProvider,
                lowVelocityAnimationSpec = lowVelocityAnimationSpec,
                highVelocityAnimationSpec = highVelocityAnimationSpec,
                snapAnimationSpec = snapAnimationSpec,
                density = density
            )
        }
    }
}

/**
 * [PagerSnapDistance] defines the way the [Pager] will treat the distance between the current
 * page and the page where it will settle.
 */
@ExperimentalFoundationApi
@Stable
internal interface PagerSnapDistance {

    /** Provides a chance to change where the [Pager] fling will settle.
     *
     * @param startPage The current page right before the fling starts.
     * @param suggestedTargetPage The proposed target page where this fling will stop. This target
     * will be the page that will be correctly positioned (snapped) after naturally decaying with
     * [velocity] using a [DecayAnimationSpec].
     * @param velocity The initial fling velocity.
     * @param pageSize The page size for this [Pager].
     * @param pageSpacing The spacing used between pages.
     *
     * @return An updated target page where to settle. Note that this value needs to be between 0
     * and the total count of pages in this pager. If an invalid value is passed, the pager will
     * coerce within the valid values.
     */
    fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int
    ): Int

    companion object {
        /**
         * Limits the maximum number of pages that can be flung per fling gesture.
         * @param pages The maximum number of extra pages that can be flung at once.
         */
        fun atMost(pages: Int): PagerSnapDistance {
            require(pages >= 0) {
                "pages should be greater than or equal to 0. You have used $pages."
            }
            return PagerSnapDistanceMaxPages(pages)
        }
    }
}

/**
 * Limits the maximum number of pages that can be flung per fling gesture.
 * @param pagesLimit The maximum number of extra pages that can be flung at once.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PagerSnapDistanceMaxPages(private val pagesLimit: Int) : PagerSnapDistance {
    override fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int,
    ): Int {
        return suggestedTargetPage.coerceIn(startPage - pagesLimit, startPage + pagesLimit)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is PagerSnapDistanceMaxPages) {
            this.pagesLimit == other.pagesLimit
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return pagesLimit.hashCode()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun SnapLayoutInfoProvider(
    pagerState: PagerState,
    pagerSnapDistance: PagerSnapDistance,
    decayAnimationSpec: DecayAnimationSpec<Float>
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider by SnapLayoutInfoProvider(
        lazyListState = pagerState.lazyListState,
        positionInLayout = SnapAlignmentStartToStart
    ) {
        override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
            val effectivePageSize = pagerState.pageSize + pagerState.pageSpacing
            val initialOffset = pagerState.currentPageOffset * effectivePageSize
            val animationOffset =
                decayAnimationSpec.calculateTargetValue(0f, initialVelocity)

            val startPage = pagerState.currentPage
            val startPageOffset = startPage * effectivePageSize

            val targetOffset =
                (startPageOffset + initialOffset + animationOffset) / effectivePageSize

            val targetPage = targetOffset.toInt()
            val correctedTargetPage = pagerSnapDistance.calculateTargetPage(
                startPage,
                targetPage,
                initialVelocity,
                pagerState.pageSize,
                pagerState.pageSpacing
            ).coerceIn(0, pagerState.pageCount)

            val finalOffset = (correctedTargetPage - startPage) * effectivePageSize

            return (finalOffset - initialOffset)
        }
    }
}