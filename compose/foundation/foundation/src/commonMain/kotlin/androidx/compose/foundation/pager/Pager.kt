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

package androidx.compose.foundation.pager

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.calculateFinalSnappingBound
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.pageDown
import androidx.compose.ui.semantics.pageLeft
import androidx.compose.ui.semantics.pageRight
import androidx.compose.ui.semantics.pageUp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A Pager that scrolls horizontally. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [outOfBoundsPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [SnapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the samples to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleHorizontalPagerSample
 * @sample androidx.compose.foundation.samples.HorizontalPagerWithScrollableContent
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param outOfBoundsPageCount Pages to compose and layout before and after the list of visible
 * pages. Note: Be aware that using a large value for [outOfBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones. This does not include the pages automatically composed and laid out by the pre-fetcher in
 * the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one. If null
 * is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager]
 * behaves with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of pages.
 * Use this to provide different settling to different positions in the layout. This is used by
 * [Pager] as a way to calculate [PagerState.currentPage], currentPage is the page closest
 * to the snap position in the layout (e.g. if the snap position is the start of the layout, then
 * currentPage will be the page closest to that).
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    outOfBoundsPageCount: Int = PagerDefaults.OutOfBoundsPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = remember(state) {
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal)
    },
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        outOfBoundsPageCount = outOfBoundsPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        pageContent = pageContent
    )
}

/**
 * A Pager that scrolls vertically. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [outOfBoundsPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [SnapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleVerticalPagerSample
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param outOfBoundsPageCount Pages to compose and layout before and after the list of visible
 * pages. Note: Be aware that using a large value for [outOfBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones. This does not include the pages automatically composed and laid out by the pre-fetcher in
 *  * the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param horizontalAlignment How pages are aligned horizontally in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one. If null
 * is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager] behaves
 * with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of Pages.
 * Use this to provide different settling to different positions in the layout. This is used by
 * [Pager] as a way to calculate [PagerState.currentPage], currentPage is the page closest
 * to the snap position in the layout (e.g. if the snap position is the start of the layout, then
 * currentPage will be the page closest to that).
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    outOfBoundsPageCount: Int = PagerDefaults.OutOfBoundsPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = remember(state) {
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Vertical)
    },
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        outOfBoundsPageCount = outOfBoundsPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Vertical,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        pageContent = pageContent
    )
}

/**
 * Contains the default values used by [Pager].
 */
object PagerDefaults {

    /**
     * A [SnapFlingBehavior] that will snap pages to the start of the layout. One can use the
     * given parameters to control how the snapping animation will happen.
     * @see androidx.compose.foundation.gestures.snapping.SnapFlingBehavior for more information
     * on what which parameter controls in the overall snapping animation.
     *
     * The animation specs used by the fling behavior will depend on 2 factors:
     * 1) The gesture velocity.
     * 2) The target page proposed by [pagerSnapDistance].
     *
     * If you're using single page snapping (the most common use case for [Pager]), there won't
     * be enough space to actually run a decay animation to approach the target page, so the Pager
     * will always use the snapping animation from [snapAnimationSpec].
     * If you're using multi-page snapping (this means you're abs(targetPage - currentPage) > 1)
     * the Pager may use [decayAnimationSpec] or [snapAnimationSpec] to approach the
     * targetPage, it will depend on the velocity generated by the triggering gesture.
     * If the gesture has a high enough velocity to approach the target page, the Pager will use
     * [decayAnimationSpec] followed by [snapAnimationSpec] for the final step of the
     * animation. If the gesture doesn't have enough velocity, the Pager will use
     * [snapAnimationSpec] + [snapAnimationSpec] in a similar fashion.
     *
     * @param state The [PagerState] that controls the which to which this FlingBehavior will
     * be applied to.
     * @param pagerSnapDistance A way to control the snapping destination for this [Pager].
     * The default behavior will result in any fling going to the next page in the direction of the
     * fling (if the fling has enough velocity, otherwise  the Pager will bounce back). Use
     * [PagerSnapDistance.atMost] to define a maximum number of pages this [Pager] is allowed to
     * fling after scrolling is finished and fling has started.
     * @param decayAnimationSpec The animation spec used to approach the target offset. When
     * the fling velocity is large enough. Large enough means large enough to naturally decay. For
     * single page snapping this usually never happens since there won't be enough space to run a
     * decay animation.
     * @param snapAnimationSpec The animation spec used to finally snap to the position. This
     * animation will be often used in 2 cases: 1) There was enough space to an approach animation,
     * the Pager will use [snapAnimationSpec] in the last step of the animation to settle the page
     * into position. 2) There was not enough space to run the approach animation.
     * @param snapPositionalThreshold If the fling has a low velocity (e.g. slow scroll),
     * this fling behavior will use this snap threshold in order to determine if the pager should
     * snap back or move forward. Use a number between 0 and 1 as a fraction of the page size that
     * needs to be scrolled before the Pager considers it should move to the next page.
     * For instance, if snapPositionalThreshold = 0.35, it means if this pager is scrolled with a
     * slow velocity and the Pager scrolls more than 35% of the page size, then will jump to the
     * next page, if not it scrolls back.
     * Note that any fling that has high enough velocity will *always* move to the next page
     * in the direction of the fling.
     *
     * @return An instance of [FlingBehavior] that will perform Snapping to the next page by
     * default. The animation will be governed by the post scroll velocity and the Pager will use
     * either
     * [snapAnimationSpec] or [decayAnimationSpec] to approach the snapped position
     * If a velocity is not high enough the pager will use [snapAnimationSpec] to reach the snapped
     * position. If the velocity is high enough, the Pager will use the logic described in
     * [decayAnimationSpec] and [snapAnimationSpec].
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun flingBehavior(
        state: PagerState,
        pagerSnapDistance: PagerSnapDistance = PagerSnapDistance.atMost(1),
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
        @FloatRange(from = 0.0, to = 1.0) snapPositionalThreshold: Float = 0.5f
    ): TargetedFlingBehavior {
        require(snapPositionalThreshold in 0f..1f) {
            "snapPositionalThreshold should be a number between 0 and 1. " +
                "You've specified $snapPositionalThreshold"
        }
        val density = LocalDensity.current
        return remember(
            state,
            decayAnimationSpec,
            snapAnimationSpec,
            pagerSnapDistance,
            density
        ) {
            val snapLayoutInfoProvider =
                SnapLayoutInfoProvider(
                    state,
                    pagerSnapDistance,
                    decayAnimationSpec
                ) { flingVelocity, lowerBound, upperBound ->
                    calculateFinalSnappingBound(
                        pagerState = state,
                        snapPositionalThreshold = snapPositionalThreshold,
                        flingVelocity = flingVelocity,
                        lowerBoundOffset = lowerBound,
                        upperBoundOffset = upperBound
                    )
                }

            SnapFlingBehavior(
                snapLayoutInfoProvider = snapLayoutInfoProvider,
                decayAnimationSpec = decayAnimationSpec,
                snapAnimationSpec = snapAnimationSpec
            )
        }
    }

    /**
     * The default implementation of Pager's pageNestedScrollConnection.
     *
     * @param state state of the pager
     * @param orientation The orientation of the pager. This will be used to determine which
     * direction the nested scroll connection will operate and react on.
     */
    fun pageNestedScrollConnection(
        state: PagerState,
        orientation: Orientation
    ): NestedScrollConnection {
        return DefaultPagerNestedScrollConnection(state, orientation)
    }

    /**
     * The default value of outOfBoundsPageCount used to specify the number of pages to compose
     * and layout before and after the visible pages. It does not include the pages automatically
     * composed and laid out by the pre-fetcher in the direction of the scroll during scroll events.
     */
    const val OutOfBoundsPageCount = 0
}

internal fun SnapPosition.currentPageOffset(
    layoutSize: Int,
    pageSize: Int,
    spaceBetweenPages: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int
): Int {
    val snapOffset = position(
        layoutSize,
        pageSize,
        beforeContentPadding,
        afterContentPadding,
        currentPage,
        pageCount
    )

    return (snapOffset - currentPageOffsetFraction * (pageSize + spaceBetweenPages)).roundToInt()
}

private class DefaultPagerNestedScrollConnection(
    val state: PagerState,
    val orientation: Orientation
) : NestedScrollConnection {

    fun Velocity.consumeOnOrientation(orientation: Orientation): Velocity {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    fun Offset.consumeOnOrientation(orientation: Orientation): Offset {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (
        // rounding error and drag only
            source == NestedScrollSource.Drag && abs(state.currentPageOffsetFraction) > 0e-6
        ) {
            // find the current and next page (in the direction of dragging)
            val currentPageOffset = state.currentPageOffsetFraction * state.pageSize
            val pageAvailableSpace = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
            val nextClosestPageOffset =
                currentPageOffset + pageAvailableSpace * -sign(state.currentPageOffsetFraction)

            val minBound: Float
            val maxBound: Float
            // build min and max bounds in absolute coordinates for nested scroll
            if (state.currentPageOffsetFraction > 0f) {
                minBound = nextClosestPageOffset
                maxBound = currentPageOffset
            } else {
                minBound = currentPageOffset
                maxBound = nextClosestPageOffset
            }

            val delta = if (orientation == Orientation.Horizontal) available.x else available.y
            val coerced = delta.coerceIn(minBound, maxBound)
            // dispatch and return reversed as usual
            val consumed = -state.dispatchRawDelta(-coerced)
            available.copy(
                x = if (orientation == Orientation.Horizontal) consumed else available.x,
                y = if (orientation == Orientation.Vertical) consumed else available.y,
            )
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source == NestedScrollSource.Fling && available != Offset.Zero) {
            throw CancellationException()
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return available.consumeOnOrientation(orientation)
    }
}

@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.pagerSemantics(state: PagerState, isVertical: Boolean): Modifier {
    val scope = rememberCoroutineScope()
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch {
                state.animateToNextPage()
            }
            true
        } else {
            false
        }
    }

    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch {
                state.animateToPreviousPage()
            }
            true
        } else {
            false
        }
    }

    return this.then(Modifier.semantics {
        if (isVertical) {
            pageUp { performBackwardPaging() }
            pageDown { performForwardPaging() }
        } else {
            pageLeft { performBackwardPaging() }
            pageRight { performForwardPaging() }
        }
    })
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.MainPagerComposable) {
        println("Pager: ${generateMsg()}")
    }
}

internal object PagerDebugConfig {
    const val MainPagerComposable = false
    const val PagerState = false
    const val MeasureLogic = false
    const val ScrollPosition = false
    const val PagerSnapDistance = false
    const val PagerSnapLayoutInfoProvider = false
}
