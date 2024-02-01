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
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
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
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState {
    return rememberSaveable(saver = DefaultPagerState.Saver) {
        DefaultPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount
        )
    }.apply {
        pageCountState.value = pageCount
    }
}

/**
 * Creates a default [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param currentPage The pager that should be shown first.
 * @param currentPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
fun PagerState(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState = DefaultPagerState(currentPage, currentPageOffsetFraction, pageCount)

private class DefaultPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int
) : PagerState(currentPage, currentPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<DefaultPagerState, *> = listSaver(
            save = {
                listOf(
                    it.currentPage,
                    it.currentPageOffsetFraction,
                    it.pageCount
                )
            },
            restore = {
                DefaultPagerState(
                    currentPage = it[0] as Int,
                    currentPageOffsetFraction = it[1] as Float,
                    updatedPageCount = { it[2] as Int }
                )
            }
        )
    }
}

/**
 * The state that can be used to control [VerticalPager] and [HorizontalPager]
 * @param currentPage The initial page to be displayed
 * @param currentPageOffsetFraction The offset of the initial page with respect to the start of
 * the layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
abstract class PagerState(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f
) : ScrollableState {

    /**
     * The total amount of pages present in this pager. The source of this data should be
     * observable.
     */
    abstract val pageCount: Int

    init {
        require(currentPageOffsetFraction in -0.5..0.5) {
            "currentPageOffsetFraction $currentPageOffsetFraction is " +
                "not within the range -0.5 to 0.5"
        }
    }

    /**
     * Difference between the last up and last down events of a scroll event.
     */
    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)
    private val animatedScrollScope = PagerLazyAnimateScrollScope(this)

    private var isScrollingForward: Boolean by mutableStateOf(false)

    internal val scrollPosition = PagerScrollPosition(currentPage, currentPageOffsetFraction, this)

    internal var firstVisiblePage = currentPage
        private set

    internal var firstVisiblePageOffset = 0
        private set

    private var maxScrollOffset: Long = Long.MAX_VALUE
        private set

    private var minScrollOffset: Long = 0L
        private set

    private var accumulator: Float = 0.0f

    /**
     * The prefetch will act after the measure pass has finished and it needs to know the
     * magnitude and direction of the scroll that triggered the measure pass
     */
    private var previousPassDelta = 0f

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the list.
     */
    private val scrollableState = ScrollableState { performScroll(it) }

    /**
     * Within the scrolling context we can use absolute positions to
     * determine scroll deltas and max min scrolling.
     */
    private fun performScroll(delta: Float): Float {
        val currentScrollPosition = scrollPosition.currentAbsoluteScrollOffset()
        debugLog {
            "\nDelta=$delta " +
                "\ncurrentScrollPosition=$currentScrollPosition " +
                "\naccumulator=$accumulator" +
                "\nmaxScrollOffset=$maxScrollOffset"
        }

        val decimalAccumulation = (delta + accumulator)
        val decimalAccumulationInt = decimalAccumulation.roundToLong()
        accumulator = decimalAccumulation - decimalAccumulationInt

        /**
         * The updated scroll position is the current position with the integer part of the delta
         * and accumulator applied.
         */
        val updatedScrollPosition = (currentScrollPosition + decimalAccumulationInt)

        /**
         * Check if the scroll position may be larger than the maximum possible scroll.
         */
        val coercedScrollPosition = updatedScrollPosition.coerceIn(minScrollOffset, maxScrollOffset)

        /**
         * Check if we actually coerced.
         */
        val changed = updatedScrollPosition != coercedScrollPosition

        /**
         * Calculated the actual scroll delta to be applied
         */
        val scrollDelta = coercedScrollPosition - currentScrollPosition

        previousPassDelta = scrollDelta.toFloat()

        if (scrollDelta.absoluteValue != 0L) {
            isScrollingForward = scrollDelta > 0.0f
        }

        /**
         * Apply the scroll delta
         */
        val layoutInfo = pagerLayoutInfoState.value
        if (layoutInfo.tryToApplyScrollWithoutRemeasure(-scrollDelta.toInt())) {
            debugLog { "Will Apply Without Remeasure" }
            applyMeasureResult(
                result = layoutInfo,
                visibleItemsStayedTheSame = true
            )
            // we don't need to remeasure, so we only trigger re-placement:
            placementScopeInvalidator.invalidateScope()
            layoutWithoutMeasurement++
        } else {
            debugLog { "Will Apply With Remeasure" }
            scrollPosition.applyScrollDelta(scrollDelta.toInt())
            remeasurement?.forceRemeasure()
            layoutWithMeasurement++
        }

        // Return the consumed value.
        return (if (changed) scrollDelta else delta).toFloat()
    }

    /**
     * Only used for testing to confirm that we're not making too many measure passes
     */
    internal val numMeasurePasses: Int get() = layoutWithMeasurement + layoutWithoutMeasurement

    internal var layoutWithMeasurement: Int = 0
        private set

    internal var layoutWithoutMeasurement: Int = 0
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
    private var wasPrefetchingForward = false

    /** Backing state for PagerLayoutInfo */
    private var pagerLayoutInfoState =
        mutableStateOf(EmptyLayoutInfo, neverEqualPolicy())

    /**
     * A [PagerLayoutInfo] that contains useful information about the Pager's last layout pass.
     * For instance, you can query which pages are currently visible in the layout.
     *
     * This property is observable and is updated after every scroll or remeasure.
     * If you use it in the composable function it will be recomposed on every change causing
     * potential performance issues including infinity recomposition loop.
     * Therefore, avoid using it in the composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     * @sample androidx.compose.foundation.samples.UsingPagerLayoutInfoForSideEffectSample
     */
    val layoutInfo: PagerLayoutInfo get() = pagerLayoutInfoState.value

    internal val pageSpacing: Int
        get() = pagerLayoutInfoState.value.pageSpacing

    internal val pageSize: Int
        get() = pagerLayoutInfoState.value.pageSize

    internal var density: Density = UnitDensity

    internal val pageSizeWithSpacing: Int
        get() = pageSize + pageSpacing

    /**
     * How far the current page needs to scroll so the target page is considered to be the next
     * page.
     */
    internal val positionThresholdFraction: Float
        get() = with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

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

    private var programmaticScrollTargetPage by mutableIntStateOf(-1)

    private var settledPageState by mutableIntStateOf(currentPage)

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
            this.currentPage
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
            this.currentPage
        } else if (programmaticScrollTargetPage != -1) {
            programmaticScrollTargetPage
        } else {
            // act on scroll only
            if (abs(this.currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                if (isScrollingForward) {
                    firstVisiblePage + 1
                } else {
                    firstVisiblePage
                }
            } else {
                this.currentPage
            }
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
    val currentPageOffsetFraction: Float get() = scrollPosition.currentPageOffsetFraction

    internal val prefetchState = LazyLayoutPrefetchState()

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

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
    internal var premeasureConstraints = Constraints()

    /**
     * Stores currently pinned pages which are always composed, used by for beyond bound pages.
     */
    internal val pinnedPages = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

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
    suspend fun scrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f
    ) = scroll {
        debugLog { "Scroll from page=$currentPage to page=$page" }
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        snapToItem(targetPage, pageOffsetFraction)
    }

    /**
     * Jump immediately to a given [page] with a given [pageOffsetFraction] inside
     * a [ScrollScope]. Use this method to create custom animated scrolling experiences. This will
     * update the value of [currentPage] and [currentPageOffsetFraction] immediately, but can only
     * be used inside a [ScrollScope], use [scroll] to gain access to a [ScrollScope].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     */
    @ExperimentalFoundationApi
    fun ScrollScope.updateCurrentPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f
    ) {
        with(animatedScrollScope) {
            snapToItem(page, pageOffsetFraction)
        }
    }

    /**
     * Used to update [targetPage] during a programmatic scroll operation. This can only be called
     * inside a [ScrollScope] and should be called anytime a custom scroll (through [scroll]) is
     * executed in order to correctly update [targetPage]. This will not move the pages and it's
     * still the responsibility of the caller to call [ScrollScope.scrollBy] in order to actually
     * get to [targetPage]. By the end of the [scroll] block, when the [Pager] is no longer
     * scrolling [targetPage] will assume the value of [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
     */
    @ExperimentalFoundationApi
    fun ScrollScope.updateTargetPage(targetPage: Int) {
        programmaticScrollTargetPage = targetPage.coerceInPageRange()
    }

    internal fun snapToItem(page: Int, offsetFraction: Float) {
        scrollPosition.requestPosition(page, offsetFraction)
        remeasurement?.forceRemeasure() // trigger remeasure
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
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring()
    ) {
        if (page == currentPage && currentPageOffsetFraction == pageOffsetFraction ||
            pageCount == 0
        ) return
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        val targetPageOffsetToSnappedPosition =
            (pageOffsetFraction * pageSizeWithSpacing).roundToInt()

        with(animatedScrollScope) {
            scroll {
                updateTargetPage(targetPage)
                val forward = targetPage > firstVisibleItemIndex
                val visiblePages = lastVisibleItemIndex - firstVisibleItemIndex + 1
                if (((forward && targetPage > lastVisibleItemIndex) ||
                        (!forward && targetPage < firstVisibleItemIndex)) &&
                    abs(targetPage - firstVisibleItemIndex) >= MaxPagesForAnimateScroll
                ) {
                    val preJumpPosition = if (forward) {
                        (targetPage - visiblePages).coerceAtLeast(firstVisibleItemIndex)
                    } else {
                        (targetPage + visiblePages).coerceAtMost(firstVisibleItemIndex)
                    }

                    debugLog {
                        "animateScrollToPage with pre-jump to position=$preJumpPosition"
                    }

                    // Pre-jump to 1 viewport away from destination page, if possible
                    snapToItem(preJumpPosition, 0)
                }
                val pageAvailableSpace = visibleItemsAverageSize
                val currentPosition = currentPage
                val targetOffset = targetPage * pageAvailableSpace
                val currentOffset = currentPosition * pageAvailableSpace
                val currentOffsetFraction = pageAvailableSpace * currentPageOffsetFraction

                // The final delta displacement will be the difference between the pages offsets
                // discounting whatever offset the original page had scrolled plus the offset
                // fraction requested by the user.
                val displacement = (targetOffset - currentOffset +
                    targetPageOffsetToSnappedPosition - currentOffsetFraction)

                debugLog { "animateScrollToPage $displacement pixels" }
                var previousValue = 0f
                animate(0f, displacement, animationSpec = animationSpec) { currentValue, _ ->
                    val delta = currentValue - previousValue
                    val consumed = scrollBy(delta)
                    debugLog { "Dispatched Delta=$delta Consumed=$consumed" }
                    previousValue += consumed
                }
            }
        }
    }

    private suspend fun awaitScrollDependencies() {
        awaitLayoutModifier.waitForFirstLayout()
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitScrollDependencies()
        // will scroll and it's not scrolling already update settled page
        if (!isScrollInProgress) {
            settledPageState = currentPage
        }
        scrollableState.scroll(scrollPriority, block)
        programmaticScrollTargetPage = -1 // reset animated scroll target page indicator
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
    internal fun applyMeasureResult(
        result: PagerMeasureResult,
        visibleItemsStayedTheSame: Boolean = false
    ) {
        debugLog { "Applying Measure Result" }
        if (visibleItemsStayedTheSame) {
            scrollPosition.updateCurrentPageOffsetFraction(result.currentPageOffsetFraction)
        } else {
            scrollPosition.updateFromMeasureResult(result)
            cancelPrefetchIfVisibleItemsChanged(result)
        }
        pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = result.canScrollBackward
        result.firstVisiblePage?.let { firstVisiblePage = it.index }
        firstVisiblePageOffset = result.firstVisiblePageScrollOffset
        tryRunPrefetch(result)
        maxScrollOffset = result.calculateNewMaxScrollOffset(pageCount)
        minScrollOffset = result.calculateNewMinScrollOffset(pageCount)
        debugLog {
            "Finished Applying Measure Result" +
                "\nNew maxScrollOffset=$maxScrollOffset"
        }
    }

    private fun tryRunPrefetch(result: PagerMeasureResult) = Snapshot.withoutReadObservation {
        if (abs(previousPassDelta) > 0.5f) {
            if (prefetchingEnabled && isGestureActionMatchesScroll(previousPassDelta)) {
                notifyPrefetch(previousPassDelta, result)
            }
        }
    }

    private fun Int.coerceInPageRange() = if (pageCount > 0) {
        coerceIn(0, pageCount - 1)
    } else {
        0
    }

    // check if the scrolling will be a result of a fling operation. That is, if the scrolling
    // direction is in the opposite direction of the gesture movement. Also, return true if there
    // is no applied gesture that causes the scrolling
    private fun isGestureActionMatchesScroll(scrollDelta: Float): Boolean =
        if (layoutInfo.orientation == Orientation.Vertical) {
            sign(scrollDelta) == sign(-upDownDifference.y)
        } else {
            sign(scrollDelta) == sign(-upDownDifference.x)
        } || isNotGestureAction()

    private fun isNotGestureAction(): Boolean =
        upDownDifference.x.toInt() == 0 && upDownDifference.y.toInt() == 0

    private fun notifyPrefetch(delta: Float, info: PagerLayoutInfo) {
        if (!prefetchingEnabled) {
            return
        }

        if (info.visiblePagesInfo.isNotEmpty()) {
            val isPrefetchingForward = delta > 0
            val indexToPrefetch = if (isPrefetchingForward) {
                info.visiblePagesInfo.last().index + info.outOfBoundsPageCount + PagesToPrefetch
            } else {
                info.visiblePagesInfo.first().index - info.outOfBoundsPageCount - PagesToPrefetch
            }
            if (indexToPrefetch != this.indexToPrefetch &&
                indexToPrefetch in 0 until pageCount
            ) {
                if (wasPrefetchingForward != isPrefetchingForward) {
                    // the scrolling direction has been changed which means the last prefetched
                    // is not going to be reached anytime soon so it is safer to dispose it.
                    // if this item is already visible it is safe to call the method anyway
                    // as it will be no-op
                    currentPrefetchHandle?.cancel()
                }
                this.wasPrefetchingForward = isPrefetchingForward
                this.indexToPrefetch = indexToPrefetch
                currentPrefetchHandle = prefetchState.schedulePrefetch(
                    indexToPrefetch, premeasureConstraints
                )
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: PagerLayoutInfo) {
        if (indexToPrefetch != -1 && info.visiblePagesInfo.isNotEmpty()) {
            val expectedPrefetchIndex = if (wasPrefetchingForward) {
                info.visiblePagesInfo.last().index + info.outOfBoundsPageCount + PagesToPrefetch
            } else {
                info.visiblePagesInfo.first().index - info.outOfBoundsPageCount - PagesToPrefetch
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
            "page $page is not within the range 0 to $pageCount"
        }
        return (currentPage - page) + currentPageOffsetFraction
    }

    /**
     * When the user provided custom keys for the pages we can try to detect when there were
     * pages added or removed before our current page and keep this page as the current one
     * given that its index has been changed.
     */
    internal fun matchScrollPositionWithKey(
        itemProvider: PagerLazyLayoutItemProvider,
        currentPage: Int = Snapshot.withoutReadObservation { scrollPosition.currentPage }
    ): Int = scrollPosition.matchPageWithKey(itemProvider, currentPage)
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

internal val DefaultPositionThreshold = 56.dp
private const val MaxPagesForAnimateScroll = 3
internal const val PagesToPrefetch = 1

internal val EmptyLayoutInfo = PagerMeasureResult(
    visiblePagesInfo = emptyList(),
    pageSize = 0,
    pageSpacing = 0,
    afterContentPadding = 0,
    orientation = Orientation.Horizontal,
    viewportStartOffset = 0,
    viewportEndOffset = 0,
    reverseLayout = false,
    outOfBoundsPageCount = 0,
    firstVisiblePage = null,
    firstVisiblePageScrollOffset = 0,
    currentPage = null,
    currentPageOffsetFraction = 0.0f,
    canScrollForward = false,
    snapPosition = SnapPosition.Start,
    measureResult = object : MeasureResult {
        override val width: Int = 0

        override val height: Int = 0

        @Suppress("PrimitiveInCollection")
        override val alignmentLines: Map<AlignmentLine, Int> = mapOf()

        override fun placeChildren() {}
    },
    remeasureNeeded = false
)

private val UnitDensity = object : Density {
    override val density: Float = 1f
    override val fontScale: Float = 1f
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.PagerState) {
        println("PagerState: ${generateMsg()}")
    }
}

private fun PagerMeasureResult.calculateNewMaxScrollOffset(pageCount: Int): Long {
    val pageSizeWithSpacing = pageSpacing + pageSize
    val maxScrollPossible =
        (pageCount.toLong()) * pageSizeWithSpacing + beforeContentPadding + afterContentPadding
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    /**
     * We need to take into consideration the snap position for max scroll position.
     * For instance, if SnapPosition.Start, the max scroll position is
     * pageCount * pageSize - viewport. Now if SnapPosition.End, it should be pageCount * pageSize.
     * Therefore, the snap position discount varies between 0 and viewport.
     */
    val snapPositionDiscount = layoutSize - (snapPosition.position(
        layoutSize = layoutSize,
        itemSize = pageSize,
        itemIndex = pageCount - 1,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        itemCount = pageCount
    )).coerceIn(0, layoutSize)

    debugLog {
        "maxScrollPossible=$maxScrollPossible" +
            "\nsnapPositionDiscount=$snapPositionDiscount" +
            "\nlayoutSize=$layoutSize"
    }
    return (maxScrollPossible - snapPositionDiscount).coerceAtLeast(0L)
}

private fun PagerMeasureResult.calculateNewMinScrollOffset(pageCount: Int): Long {
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    return snapPosition.position(
        layoutSize = layoutSize,
        itemSize = pageSize,
        itemIndex = 0,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        itemCount = pageCount
    ).coerceIn(0, layoutSize).toLong()
}
