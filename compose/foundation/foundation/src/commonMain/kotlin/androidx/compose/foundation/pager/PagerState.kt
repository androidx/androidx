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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
@ExperimentalFoundationApi
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState {
    return rememberSaveable(saver = PagerStateImpl.Saver) {
        PagerStateImpl(
            initialPage,
            initialPageOffsetFraction,
            pageCount
        )
    }.apply {
        pageCountState.value = pageCount
    }
}

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 */
@Deprecated(
    "Please use the overload where you can provide a source of truth for the pageCount.",
    ReplaceWith(
        """rememberPagerState(
                initialPage = initialPage,
                initialPageOffsetFraction = initialPageOffsetFraction
            ){
                // provide pageCount
            }"""
    )
)
@ExperimentalFoundationApi
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f
): PagerState {
    return rememberSaveable(saver = PagerStateImpl.Saver) {
        PagerStateImpl(
            initialPage = initialPage,
            initialPageOffsetFraction = initialPageOffsetFraction
        ) { 0 }
    }
}

@ExperimentalFoundationApi
internal class PagerStateImpl(
    initialPage: Int,
    initialPageOffsetFraction: Float,
    updatedPageCount: () -> Int
) : PagerState(initialPage, initialPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<PagerStateImpl, *> = listSaver(
            save = {
                listOf(
                    it.currentPage,
                    it.currentPageOffsetFraction,
                    it.pageCount
                )
            },
            restore = {
                PagerStateImpl(
                    initialPage = it[0] as Int,
                    initialPageOffsetFraction = it[1] as Float,
                    updatedPageCount = { it[2] as Int }
                )
            }
        )
    }
}

/**
 * The state that can be used to control [VerticalPager] and [HorizontalPager]
 * @param initialPage The initial page to be displayed
 * @param initialPageOffsetFraction The offset of the initial page with respect to the start of
 * the layout.
 */
@ExperimentalFoundationApi
@Stable
abstract class PagerState(
    val initialPage: Int = 0,
    val initialPageOffsetFraction: Float = 0f
) : ScrollableState {

    /**
     * The total amount of pages present in this pager
     */
    abstract val pageCount: Int

    init {
        require(initialPageOffsetFraction in -0.5..0.5) {
            "initialPageOffsetFraction $initialPageOffsetFraction is " +
                "not within the range -0.5 to 0.5"
        }
    }

    /**
     * Difference between the last up and last down events of a scroll event.
     */
    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)
    internal var snapRemainingScrollOffset by mutableStateOf(0f)

    private val scrollPosition = PagerScrollPosition(initialPage, 0)

    internal val firstVisiblePage: Int get() = scrollPosition.firstVisiblePage

    internal val firstVisiblePageOffset: Int get() = scrollPosition.scrollOffset

    internal var scrollToBeConsumed = 0f
        private set

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the list.
     */
    private val scrollableState = ScrollableState { -performScroll(-it) }

    /**
     * Only used for testing to confirm that we're not making too many measure passes
     */
    internal var numMeasurePasses: Int = 0
        private set

    /**
     * Only used for testing to disable prefetching when needed to test the main logic.
     */
    internal var prefetchingEnabled: Boolean = true

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var indexToPrefetch = -1

    /**
     * The handle associated with the current index from [indexToPrefetch].
     */
    private var currentPrefetchHandle: LazyLayoutPrefetchState.PrefetchHandle? = null

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to
     * detect the scrolling direction change.
     */
    private var wasScrollingForward = false

    /** Backing state for PagerLayoutInfo **/
    private var pagerLayoutInfoState = mutableStateOf(EmptyLayoutInfo)

    internal val layoutInfo: PagerLayoutInfo get() = pagerLayoutInfoState.value

    internal val pageSpacing: Int
        get() = pagerLayoutInfoState.value.pageSpacing

    internal val pageSize: Int
        get() = pagerLayoutInfoState.value.pageSize

    internal var density: Density by mutableStateOf(UnitDensity)

    private val visiblePages: List<PageInfo>
        get() = pagerLayoutInfoState.value.visiblePagesInfo

    private val pageAvailableSpace: Int
        get() = pageSize + pageSpacing

    /**
     * How far the current page needs to scroll so the target page is considered to be the next
     * page.
     */
    private val positionThresholdFraction: Float
        get() = with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

    private val distanceToSnapPosition: Float
        get() = layoutInfo.closestPageToSnapPosition?.let {
            density.calculateDistanceToDesiredSnapPosition(
                layoutInfo,
                it,
                SnapAlignmentStartToStart
            )
        } ?: 0f

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or animated scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    /**
     * The page that sits closest to the snapped position. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     *
     */
    val currentPage: Int get() = scrollPosition.currentPage

    private var animationTargetPage by mutableStateOf(-1)

    private var settledPageState by mutableStateOf(initialPage)

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val settledPage by derivedStateOf(structuralEqualityPolicy()) {
        if (isScrollInProgress) {
            settledPageState
        } else {
            currentPage
        }
    }

    /**
     * The page this [Pager] intends to settle to.
     * During fling or animated scroll (from [animateScrollToPage] this will represent the page
     * this pager intends to settle to. When no scroll is ongoing, this will be equal to
     * [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val targetPage: Int by derivedStateOf(structuralEqualityPolicy()) {
        val finalPage = if (!isScrollInProgress) {
            currentPage
        } else if (animationTargetPage != -1) {
            animationTargetPage
        } else if (snapRemainingScrollOffset == 0.0f) {
            // act on scroll only
            if (abs(currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                currentPage + currentPageOffsetFraction.sign.toInt()
            } else {
                currentPage
            }
        } else {
            // act on flinging
            val pageDisplacement = snapRemainingScrollOffset / pageAvailableSpace
            (currentPage + pageDisplacement.roundToInt())
        }
        finalPage.coerceInPageRange()
    }

    /**
     * Indicates how far the current page is to the snapped position, this will vary from
     * -0.5 (page is offset towards the start of the layout) to 0.5 (page is offset towards the end
     * of the layout). This is 0.0 if the [currentPage] is in the snapped position. The value will
     * flip once the current page changes.
     *
     * This property is observable and shouldn't be used as is in a composable function due to
     * potential performance issues. To use it in the composition, please consider using a
     * derived state (e.g [derivedStateOf]) to only have recompositions when the derived
     * value changes.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val currentPageOffsetFraction: Float by derivedStateOf(structuralEqualityPolicy()) {
        val currentPagePositionOffset =
            layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == currentPage }?.offset ?: 0
        val pageUsedSpace = pageAvailableSpace.toFloat()
        if (pageUsedSpace == 0f) {
            // Default to 0 when there's no info about the page size yet.
            initialPageOffsetFraction
        } else {
            ((-currentPagePositionOffset) / (pageUsedSpace)).coerceIn(
                MinPageOffset, MaxPageOffset
            )
        }
    }

    internal val prefetchState = LazyLayoutPrefetchState()

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll)
     * until layout is ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? by mutableStateOf(null)
        private set

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@PagerState.remeasurement = remeasurement
        }
    }

    /**
     * Constraints passed to the prefetcher for premeasuring the prefetched items.
     */
    internal var premeasureConstraints by mutableStateOf(Constraints())

    /**
     * Stores currently pinned pages which are always composed, used by for beyond bound pages.
     */
    internal val pinnedPages = LazyLayoutPinnedItemList()

    /**
     * Scroll (jump immediately) to a given [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ScrollToPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     */
    suspend fun scrollToPage(page: Int, pageOffsetFraction: Float = 0f) = scroll {
        debugLog { "Scroll from page=$currentPage to page=$page" }
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        scrollPosition.requestPosition(
            targetPage,
            (pageAvailableSpace * pageOffsetFraction).roundToInt()
        )
        remeasurement?.forceRemeasure()
    }

    /**
     * Scroll animate to a given [page]. If the [page] is too far away from [currentPage] we will
     * not compose all pages in the way. We will pre-jump to a nearer page, compose and animate
     * the rest of the pages until [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.AnimateScrollPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     * default animation.
     */
    suspend fun animateScrollToPage(
        page: Int,
        pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
    ) {
        if (page == currentPage && currentPageOffsetFraction == pageOffsetFraction) return
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        var currentPosition = currentPage
        val targetPage = page.coerceInPageRange()
        animationTargetPage = targetPage
        // If our future page is too far off, that is, outside of the current viewport
        val firstVisiblePageIndex = visiblePages.first().index
        val lastVisiblePageIndex = visiblePages.last().index
        if (((page > currentPage && page > lastVisiblePageIndex) ||
                (page < currentPage && page < firstVisiblePageIndex)) &&
            abs(page - currentPage) >= MaxPagesForAnimateScroll
        ) {
            val preJumpPosition = if (page > currentPage) {
                (page - visiblePages.size).coerceAtLeast(currentPosition)
            } else {
                page + visiblePages.size.coerceAtMost(currentPosition)
            }

            debugLog {
                "animateScrollToPage with pre-jump to position=$preJumpPosition"
            }

            // Pre-jump to 1 viewport away from destination page, if possible
            scrollToPage(preJumpPosition)
            currentPosition = preJumpPosition
        }

        val targetOffset = targetPage * pageAvailableSpace
        val currentOffset = currentPosition * pageAvailableSpace
        val pageOffsetToSnappedPosition =
            distanceToSnapPosition + pageOffsetFraction * pageAvailableSpace

        val displacement = targetOffset - currentOffset + pageOffsetToSnappedPosition

        debugLog { "animateScrollToPage $displacement pixels" }
        animateScrollBy(displacement, animationSpec)
        animationTargetPage = -1
    }

    private suspend fun awaitScrollDependencies() {
        awaitLayoutModifier.waitForFirstLayout()
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitScrollDependencies()
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return scrollableState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    final override var canScrollForward: Boolean by mutableStateOf(false)
        private set
    final override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    /**
     *  Updates the state with the new calculated scroll position and consumed scroll.
     */
    internal fun applyMeasureResult(result: PagerMeasureResult) {
        scrollPosition.updateFromMeasureResult(result)
        scrollToBeConsumed -= result.consumedScroll
        pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = (result.firstVisiblePage?.index ?: 0) != 0 ||
            result.firstVisiblePageOffset != 0
        numMeasurePasses++
        cancelPrefetchIfVisibleItemsChanged(result)
        if (!isScrollInProgress) {
            settledPageState = currentPage
        }
    }

    private fun Int.coerceInPageRange() = if (pageCount > 0) {
        coerceIn(0, pageCount - 1)
    } else {
        0
    }

    private fun performScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        check(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            remeasurement?.forceRemeasure()
            if (prefetchingEnabled) {
                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    private fun notifyPrefetch(delta: Float) {
        if (!prefetchingEnabled) {
            return
        }
        val info = layoutInfo
        if (info.visiblePagesInfo.isNotEmpty()) {
            val scrollingForward = delta < 0
            val indexToPrefetch = if (scrollingForward) {
                info.visiblePagesInfo.last().index + 1
            } else {
                info.visiblePagesInfo.first().index - 1
            }
            if (indexToPrefetch != this.indexToPrefetch &&
                indexToPrefetch in 0 until info.pagesCount
            ) {
                if (wasScrollingForward != scrollingForward) {
                    // the scrolling direction has been changed which means the last prefetched
                    // is not going to be reached anytime soon so it is safer to dispose it.
                    // if this item is already visible it is safe to call the method anyway
                    // as it will be no-op
                    currentPrefetchHandle?.cancel()
                }
                this.wasScrollingForward = scrollingForward
                this.indexToPrefetch = indexToPrefetch
                currentPrefetchHandle = prefetchState.schedulePrefetch(
                    indexToPrefetch, premeasureConstraints
                )
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: PagerLayoutInfo) {
        if (indexToPrefetch != -1 && info.visiblePagesInfo.isNotEmpty()) {
            val expectedPrefetchIndex = if (wasScrollingForward) {
                info.visiblePagesInfo.last().index + 1
            } else {
                info.visiblePagesInfo.first().index - 1
            }
            if (indexToPrefetch != expectedPrefetchIndex) {
                indexToPrefetch = -1
                currentPrefetchHandle?.cancel()
                currentPrefetchHandle = null
            }
        }
    }

    /**
     * An utility function to help to calculate a given page's offset. Since this is based off
     * [currentPageOffsetFraction] the same concept applies: a fraction offset that represents
     * how far [page] is from the settled position (represented by [currentPage] offset). The
     * difference here is that [currentPageOffsetFraction] is a value between -0.5 and 0.5 and
     * the value calculate by this function can be larger than these numbers if [page] is different
     * than [currentPage].
     *
     * @param page The page to calculate the offset from. This should be between 0 and [pageCount].
     * @return The offset of [page] with respect to [currentPage].
     */
    fun getOffsetFractionForPage(page: Int): Float {
        require(page in 0..pageCount) {
            "page $page is not within the range 0 to pageCount"
        }
        return (currentPage - page) + currentPageOffsetFraction
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

private const val MinPageOffset = -0.5f
private const val MaxPageOffset = 0.5f
internal val DefaultPositionThreshold = 56.dp
private const val MaxPagesForAnimateScroll = 3

@OptIn(ExperimentalFoundationApi::class)
internal val EmptyLayoutInfo = object : PagerLayoutInfo {
    override val visiblePagesInfo: List<PageInfo> = emptyList()
    override val closestPageToSnapPosition: PageInfo? = null
    override val pagesCount: Int = 0
    override val pageSize: Int = 0
    override val pageSpacing: Int = 0
    override val beforeContentPadding: Int = 0
    override val afterContentPadding: Int = 0
    override val viewportSize: IntSize = IntSize.Zero
    override val orientation: Orientation = Orientation.Horizontal
    override val viewportStartOffset: Int = 0
    override val viewportEndOffset: Int = 0
    override val reverseLayout: Boolean = false
}

private val UnitDensity = object : Density {
    override val density: Float = 1f
    override val fontScale: Float = 1f
}

internal val SnapAlignmentStartToStart: Density.(layoutSize: Float, itemSize: Float) -> Float =
    { _, _ -> 0f }

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("PagerState: ${generateMsg()}")
    }
}