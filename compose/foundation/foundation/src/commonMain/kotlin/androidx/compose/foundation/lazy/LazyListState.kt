/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.annotation.IntRange as AndroidXIntRange
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.lazy.LazyListState.Companion.Saver
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.animateScrollToItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.ranges.IntRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    return rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
}

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyListPrefetchStrategy] to use for prefetching content in this
 *   list
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    prefetchStrategy: LazyListPrefetchStrategy = remember { LazyListPrefetchStrategy() },
): LazyListState {
    return rememberSaveable(prefetchStrategy, saver = LazyListState.saver(prefetchStrategy)) {
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            prefetchStrategy
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberLazyListState].
 *
 * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyListPrefetchStrategy] to use for prefetching content in this
 *   list
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class LazyListState
@ExperimentalFoundationApi
constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    private val prefetchStrategy: LazyListPrefetchStrategy = LazyListPrefetchStrategy(),
) : ScrollableState {

    /**
     * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
     * @param firstVisibleItemScrollOffset the initial value for
     *   [LazyListState.firstVisibleItemScrollOffset]
     */
    constructor(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0
    ) : this(firstVisibleItemIndex, firstVisibleItemScrollOffset, LazyListPrefetchStrategy())

    internal var hasLookaheadPassOccurred: Boolean = false
        private set

    internal var postLookaheadLayoutInfo: LazyListMeasureResult? = null
        private set

    /** The holder class for the current scroll position. */
    private val scrollPosition =
        LazyListScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    /**
     * The index of the first item that is visible within the scrollable viewport area not including
     * items in the content padding region. For the first visible item that includes items in the
     * content padding please use [LazyListLayoutInfo.visibleItemsInfo].
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change causing potential performance issues.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingListScrollPositionForSideEffectSample
     *
     * If you need to use it in the composition then consider wrapping the calculation into a
     * derived state in order to only have recompositions when the derived value changes:
     *
     * @sample androidx.compose.foundation.samples.UsingListScrollPositionInCompositionSample
     */
    val firstVisibleItemIndex: Int
        get() = scrollPosition.index

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the amount
     * that the item is offset backwards.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     *
     * @see firstVisibleItemIndex for samples with the recommended usage patterns.
     */
    val firstVisibleItemScrollOffset: Int
        get() = scrollPosition.scrollOffset

    /** Backing state for [layoutInfo] */
    private val layoutInfoState = mutableStateOf(EmptyLazyListMeasureResult, neverEqualPolicy())

    /**
     * The object of [LazyListLayoutInfo] calculated during the last layout pass. For example, you
     * can use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure. If you
     * use it in the composable function it will be recomposed on every change causing potential
     * performance issues including infinity recomposition loop. Therefore, avoid using it in the
     * composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingListLayoutInfoForSideEffectSample
     */
    val layoutInfo: LazyListLayoutInfo
        get() = layoutInfoState.value

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * The amount of scroll to be consumed in the next layout pass. Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    internal var scrollToBeConsumed = 0f
        private set

    internal val density: Density
        get() = layoutInfoState.value.density

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once we
     * reached the end of the list.
     */
    private val scrollableState = ScrollableState { -onScroll(-it) }

    /** Only used for testing to confirm that we're not making too many measure passes */
    /*@VisibleForTesting*/
    internal var numMeasurePasses: Int = 0
        private set

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@LazyListState.remeasurement = remeasurement
            }
        }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    internal val itemAnimator = LazyLayoutItemAnimator<LazyListMeasuredItem>()

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    internal val prefetchState =
        LazyLayoutPrefetchState(prefetchStrategy.prefetchScheduler) {
            with(prefetchStrategy) {
                onNestedPrefetch(Snapshot.withoutReadObservation { firstVisibleItemIndex })
            }
        }

    private val prefetchScope: LazyListPrefetchScope =
        object : LazyListPrefetchScope {
            override fun schedulePrefetch(index: Int): LazyLayoutPrefetchState.PrefetchHandle {
                // Without read observation since this can be triggered from scroll - this will then
                // cause us to recompose when the measure result changes. We don't care since the
                // prefetch is best effort.
                val constraints =
                    Snapshot.withoutReadObservation { layoutInfoState.value.childConstraints }
                return prefetchState.schedulePrefetch(index, constraints)
            }
        }

    private val animatedScrollScope = LazyLayoutAnimateScrollScope(this)

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    /**
     * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun scrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll { snapToItemIndexInternal(index, scrollOffset, forceRemeasure = true) }
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the item at [index] to be at the start of the viewport during the next remeasure,
     * offset by [scrollOffset], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the first visible item key (when a data set change will also be applied during the
     * next remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    fun requestScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            layoutInfoState.value.coroutineScope.launch { scroll {} }
        }

        snapToItemIndexInternal(index, scrollOffset, forceRemeasure = false)
    }

    /**
     * Snaps to the requested scroll position. Synchronously executes remeasure if [forceRemeasure]
     * is true, and schedules a remeasure if false.
     */
    internal fun snapToItemIndexInternal(index: Int, scrollOffset: Int, forceRemeasure: Boolean) {
        val positionChanged =
            scrollPosition.index != index || scrollPosition.scrollOffset != scrollOffset
        // sometimes this method is called not to scroll, but to stay on the same index when
        // the data changes, as by default we maintain the scroll position by key, not index.
        // when this happens we don't need to reset the animations as from the user perspective
        // we didn't scroll anywhere and if there is an offset change for an item, this change
        // should be animated.
        // however, when the request is to really scroll to a different position, we have to
        // reset previously known item positions as we don't want offset changes to be animated.
        // this offset should be considered as a scroll, not the placement change.
        if (positionChanged) {
            itemAnimator.reset()
        }
        scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)

        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        } else {
            measurementScopeInvalidator.invalidateScope()
        }
    }

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitLayoutModifier.waitForFirstLayout()
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
    //  fine-grained control over scrolling
    /*@VisibleForTesting*/
    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        checkPrecondition(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            val intDelta = scrollToBeConsumed.fastRoundToInt()

            var scrolledLayoutInfo =
                layoutInfoState.value.copyWithScrollDeltaWithoutRemeasure(
                    delta = intDelta,
                    updateAnimations = !hasLookaheadPassOccurred
                )
            if (scrolledLayoutInfo != null && this.postLookaheadLayoutInfo != null) {
                // if we were able to scroll the lookahead layout info without remeasure, lets
                // try to do the same for post lookahead layout info (sometimes they diverge).
                val scrolledPostLookaheadLayoutInfo =
                    postLookaheadLayoutInfo?.copyWithScrollDeltaWithoutRemeasure(
                        delta = intDelta,
                        updateAnimations = true
                    )
                if (scrolledPostLookaheadLayoutInfo != null) {
                    // we can apply scroll delta for both phases without remeasure
                    postLookaheadLayoutInfo = scrolledPostLookaheadLayoutInfo
                } else {
                    // we can't apply scroll delta for post lookahead, so we have to remeasure
                    scrolledLayoutInfo = null
                }
            }

            if (scrolledLayoutInfo != null) {
                applyMeasureResult(
                    result = scrolledLayoutInfo,
                    isLookingAhead = hasLookaheadPassOccurred,
                    visibleItemsStayedTheSame = true
                )
                // we don't need to remeasure, so we only trigger re-placement:
                placementScopeInvalidator.invalidateScope()

                notifyPrefetchOnScroll(
                    preScrollToBeConsumed - scrollToBeConsumed,
                    scrolledLayoutInfo
                )
            } else {
                remeasurement?.forceRemeasure()
                notifyPrefetchOnScroll(preScrollToBeConsumed - scrollToBeConsumed, this.layoutInfo)
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

    private fun notifyPrefetchOnScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (prefetchingEnabled) {
            with(prefetchStrategy) { prefetchScope.onScroll(delta, layoutInfo) }
        }
    }

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun animateScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll {
            animatedScrollScope.animateScrollToItem(
                index,
                scrollOffset,
                NumberOfItemsToTeleport,
                density,
                this
            )
        }
    }

    /** Updates the state with the new calculated scroll position and consumed scroll. */
    internal fun applyMeasureResult(
        result: LazyListMeasureResult,
        isLookingAhead: Boolean,
        visibleItemsStayedTheSame: Boolean = false
    ) {
        if (!isLookingAhead && hasLookaheadPassOccurred) {
            // If there was already a lookahead pass, record this result as postLookahead result
            postLookaheadLayoutInfo = result
        } else {
            if (isLookingAhead) {
                hasLookaheadPassOccurred = true
            }

            canScrollBackward = result.canScrollBackward
            canScrollForward = result.canScrollForward
            scrollToBeConsumed -= result.consumedScroll
            layoutInfoState.value = result

            if (visibleItemsStayedTheSame) {
                scrollPosition.updateScrollOffset(result.firstVisibleItemScrollOffset)
            } else {
                scrollPosition.updateFromMeasureResult(result)
                if (prefetchingEnabled) {
                    with(prefetchStrategy) { prefetchScope.onVisibleItemsUpdated(result) }
                }
            }

            if (isLookingAhead) {
                updateScrollDeltaForPostLookahead(
                    result.scrollBackAmount,
                    result.density,
                    result.coroutineScope
                )
            }
            numMeasurePasses++
        }
    }

    internal val scrollDeltaBetweenPasses: Float
        get() = _scrollDeltaBetweenPasses.value

    private var _scrollDeltaBetweenPasses: AnimationState<Float, AnimationVector1D> =
        AnimationState(Float.VectorConverter, 0f, 0f)

    // Updates the scroll delta between lookahead & post-lookahead pass
    private fun updateScrollDeltaForPostLookahead(
        delta: Float,
        density: Density,
        coroutineScope: CoroutineScope
    ) {
        if (delta <= with(density) { DeltaThresholdForScrollAnimation.toPx() }) {
            // If the delta is within the threshold, scroll by the delta amount instead of animating
            return
        }

        // Scroll delta is updated during lookahead, we don't need to trigger lookahead when
        // the delta changes.
        Snapshot.withoutReadObservation {
            val currentDelta = _scrollDeltaBetweenPasses.value

            if (_scrollDeltaBetweenPasses.isRunning) {
                _scrollDeltaBetweenPasses = _scrollDeltaBetweenPasses.copy(currentDelta - delta)
                coroutineScope.launch {
                    _scrollDeltaBetweenPasses.animateTo(
                        0f,
                        spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.5f),
                        true
                    )
                }
            } else {
                _scrollDeltaBetweenPasses = AnimationState(Float.VectorConverter, -delta)
                coroutineScope.launch {
                    _scrollDeltaBetweenPasses.animateTo(
                        0f,
                        spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.5f),
                        true
                    )
                }
            }
        }
    }

    /**
     * When the user provided custom keys for the items we can try to detect when there were items
     * added or removed before our current first visible item and keep this item as the first
     * visible one even given that its index has been changed. The scroll position will not be
     * updated if [requestScrollToItem] was called since the last time this method was called.
     */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: LazyListItemProvider,
        firstItemIndex: Int
    ): Int = scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

    companion object {
        /** The default [Saver] implementation for [LazyListState]. */
        val Saver: Saver<LazyListState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyListState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1]
                    )
                }
            )

        /**
         * A [Saver] implementation for [LazyListState] that handles setting a custom
         * [LazyListPrefetchStrategy].
         */
        internal fun saver(prefetchStrategy: LazyListPrefetchStrategy): Saver<LazyListState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyListState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                        prefetchStrategy
                    )
                }
            )
    }
}

private val DeltaThresholdForScrollAnimation = 1.dp

private val EmptyLazyListMeasureResult =
    LazyListMeasureResult(
        firstVisibleItem = null,
        firstVisibleItemScrollOffset = 0,
        canScrollForward = false,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        scrollBackAmount = 0f,
        visibleItemsInfo = emptyList(),
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        totalItemsCount = 0,
        reverseLayout = false,
        orientation = Orientation.Vertical,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        remeasureNeeded = false,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        density = Density(1f),
        childConstraints = Constraints()
    )

private const val NumberOfItemsToTeleport = 100
