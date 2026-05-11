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

package androidx.xr.glimmer.pager

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates and remembers a [GlimmerPagerState] to be used with a [GlimmerHorizontalPager].
 *
 * @param initialPage the initial page to be displayed. Defaults to 0.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount a lambda returning the total number of pages in the pager.
 */
@Composable
public fun rememberGlimmerPagerState(
    @IntRange(from = 0) initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int,
): GlimmerPagerState {
    return rememberSaveable(saver = GlimmerPagerState.Saver) {
        GlimmerPagerState(
            currentPage = initialPage,
            currentPageOffsetFraction = initialPageOffsetFraction,
            pageCount = pageCount,
        )
    }
}

/**
 * The state that can be used to control [GlimmerHorizontalPager].
 *
 * @param currentPage The index of the page to show initially. Must be non-negative. Defaults to 0.
 * @param currentPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount A lambda returning the total number of pages in the pager.
 */
@Stable
public class GlimmerPagerState
@RememberInComposition
constructor(
    @IntRange(from = 0) currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    pageCount: () -> Int,
) : ScrollableState {

    init {
        require(currentPage >= 0) { "currentPage must be non-negative" }
    }

    internal val foundationPagerState =
        PagerState(
            currentPage = currentPage,
            currentPageOffsetFraction = currentPageOffsetFraction,
            pageCount = pageCount,
        )

    /** The total amount of pages present in this pager. */
    public val pageCount: Int
        get() = foundationPagerState.pageCount

    /** A [GlimmerPagerLayoutInfo] that contains information about the Pager's last layout pass. */
    public val layoutInfo: GlimmerPagerLayoutInfo =
        DefaultGlimmerPagerLayoutInfo(foundationPagerState)

    /**
     * [InteractionSource] that will be used to dispatch drag events when this pager is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    public val interactionSource: InteractionSource
        get() = foundationPagerState.interactionSource

    /**
     * The page that sits closest to the snapped position. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     */
    public val currentPage: Int
        get() = foundationPagerState.currentPage

    /**
     * Indicates how far the current page is to the snapped position, this will vary from -0.5 (page
     * is offset towards the start of the layout) to 0.5 (page is offset towards the end of the
     * layout). This is 0.0 if the [currentPage] is in the snapped position. The value will flip
     * once the current page changes.
     */
    public val currentPageOffsetFraction: Float
        @FrequentlyChangingValue get() = foundationPagerState.currentPageOffsetFraction

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     */
    public val settledPage: Int
        get() = foundationPagerState.settledPage

    /**
     * The page that is currently being scrolled towards. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     */
    public val targetPage: Int
        get() = foundationPagerState.targetPage

    /** Whether this pager is currently scrolling by gesture, fling or animation. */
    public override val isScrollInProgress: Boolean
        get() = foundationPagerState.isScrollInProgress

    /** Whether the last scroll action was in the forward direction. */
    @get:Suppress("GetterSetterNames")
    public override val lastScrolledForward: Boolean
        get() = foundationPagerState.lastScrolledForward

    /** Whether the last scroll action was in the backward direction. */
    @get:Suppress("GetterSetterNames")
    public override val lastScrolledBackward: Boolean
        get() = foundationPagerState.lastScrolledBackward

    /** Whether this pager can scroll forward (has more pages to show). */
    @get:Suppress("GetterSetterNames")
    public override val canScrollForward: Boolean
        get() = foundationPagerState.canScrollForward

    /** Whether this pager can scroll backward (is not at the first page). */
    @get:Suppress("GetterSetterNames")
    public override val canScrollBackward: Boolean
        get() = foundationPagerState.canScrollBackward

    /**
     * Jump immediately to a given [page] with a given [pageOffsetFraction] inside a [ScrollScope].
     * Use this method to create custom animated scrolling experiences. This will update the value
     * of [currentPage] and [currentPageOffsetFraction] immediately, but can only be used inside a
     * [ScrollScope], use [scroll] to gain access to a [ScrollScope].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.xr.glimmer.samples.GlimmerPagerStateCustomAnimateScrollToPageSample
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     */
    public fun ScrollScope.updateCurrentPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f,
    ) {
        with(foundationPagerState) { updateCurrentPage(page, pageOffsetFraction) }
    }

    /**
     * Used to update [targetPage] during a programmatic scroll operation. This can only be called
     * inside a [ScrollScope] and should be called anytime a custom scroll (through [scroll]) is
     * executed in order to correctly update [targetPage]. This will not move the pages and it's
     * still the responsibility of the caller to call [ScrollScope.scrollBy] in order to actually
     * get to [targetPage]. By the end of the [scroll] block, when the [GlimmerHorizontalPager] is
     * no longer scrolling [targetPage] will assume the value of [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.xr.glimmer.samples.GlimmerPagerStateCustomAnimateScrollToPageSample
     */
    public fun ScrollScope.updateTargetPage(targetPage: Int) {
        with(foundationPagerState) { updateTargetPage(targetPage) }
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        foundationPagerState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return foundationPagerState.dispatchRawDelta(delta)
    }

    /**
     * Requests the [page] to be at the snapped position during the next remeasure, offset by
     * [pageOffsetFraction], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the current page key (when a data set change will also be applied during the next
     * remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param page the index to which to scroll. Must be non-negative.
     * @param pageOffsetFraction the offset fraction that the page should end up after the scroll.
     */
    public fun requestScrollToPage(
        @IntRange(from = 0) page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f,
    ) {
        foundationPagerState.requestScrollToPage(page, pageOffsetFraction)
    }

    /**
     * Scroll immediately to the given [page], without animating.
     *
     * @sample androidx.xr.glimmer.samples.GlimmerPagerStateScrollToPageSample
     * @param page the index of the page to scroll to.
     */
    public suspend fun scrollToPage(page: Int) {
        foundationPagerState.scrollToPage(page)
    }

    /**
     * Scroll to the given [page] with an animation.
     *
     * @sample androidx.xr.glimmer.samples.GlimmerPagerStateAnimateScrollToPageSample
     * @param page the index of the page to scroll to.
     * @param animationSpec the [AnimationSpec] to be used for the scroll animation.
     */
    public suspend fun animateScrollToPage(
        page: Int,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        foundationPagerState.animateScrollToPage(page, animationSpec = animationSpec)
    }

    /**
     * A utility function to help to calculate a given page's offset. This is an offset that
     * represents how far [page] is from the settled position (represented by [currentPage] offset).
     * The difference here is that [currentPageOffsetFraction] is a value between -0.5 and 0.5 and
     * the value calculated by this function can be larger than these numbers if [page] is different
     * than [currentPage].
     *
     * For instance, if currentPage=0 and we call [getOffsetDistanceInPages] for page 3, the result
     * will be 3, meaning the given page is 3 pages away from the current page (the sign represent
     * the direction of the offset, positive is forward, negative is backwards). Another example is
     * if currentPage=3 and we call [getOffsetDistanceInPages] for page 1, the result would be -2,
     * meaning we're 2 pages away (moving backwards) to the current page.
     *
     * This offset also works in conjunction with [currentPageOffsetFraction], so if [currentPage]
     * is out of its snapped position (i.e. currentPageOffsetFraction!=0) then the calculated value
     * will still represent the offset in number of pages (in this case, not whole pages). For
     * instance, if currentPage=1 and we're slightly offset, currentPageOffsetFraction=0.2, if we
     * call this to page 2, the result would be 0.8, that is 0.8 page away from current page (moving
     * forward).
     *
     * @param page The page to calculate the offset from. This should be between 0 and [pageCount].
     * @return The offset of [page] with respect to [currentPage].
     */
    public fun getOffsetDistanceInPages(page: Int): Float {
        return foundationPagerState.getOffsetDistanceInPages(page)
    }

    public companion object {
        /** The default [Saver] implementation for [GlimmerPagerState]. */
        public val Saver: Saver<GlimmerPagerState, *> =
            listSaver(
                save = { listOf(it.currentPage, it.currentPageOffsetFraction, it.pageCount) },
                restore = {
                    GlimmerPagerState(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        pageCount = { it[2] as Int },
                    )
                },
            )
    }
}
