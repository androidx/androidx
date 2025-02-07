/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates a [TransformingLazyColumnState] that is remembered across compositions.
 *
 * @param initialAnchorItemIndex the index of an item that is going to be placed in the center of
 *   the screen (if possible). This correlates with [TransformingLazyColumnState.anchorItemIndex].
 * @param initialAnchorItemScrollOffset the offset of an item to be used when placing the item in
 *   the center of the screen (if possible). This correlates with
 *   [TransformingLazyColumnState.anchorItemScrollOffset].
 */
@Composable
public fun rememberTransformingLazyColumnState(
    initialAnchorItemIndex: Int = 0,
    initialAnchorItemScrollOffset: Int = 0
): TransformingLazyColumnState =
    rememberSaveable(saver = TransformingLazyColumnState.Saver) {
        TransformingLazyColumnState(
            initialAnchorItemIndex = initialAnchorItemIndex,
            initialAnchorItemScrollOffset = initialAnchorItemScrollOffset,
        )
    }

/**
 * Creates a [TransformingLazyColumnState] that is remembered across compositions.
 *
 * @param initialAnchorItemIndex the index of an item that is going to be placed in the center of
 *   the screen (if possible). This correlates with [TransformingLazyColumnState.anchorItemIndex].
 * @param initialAnchorItemScrollOffset the offset of an item to be used when placing the item in
 *   the center of the screen (if possible). This correlates with
 *   [TransformingLazyColumnState.anchorItemScrollOffset].
 * @param prefetchStrategy The prefetching strategy to use.
 */
@Composable
internal fun rememberTransformingLazyColumnState(
    initialAnchorItemIndex: Int = 0,
    initialAnchorItemScrollOffset: Int = 0,
    prefetchStrategy: TransformingLazyColumnPrefetchStrategy = remember {
        DefaultTransformingLazyColumnPrefetchStrategy()
    },
): TransformingLazyColumnState =
    rememberSaveable(saver = TransformingLazyColumnState.Saver) {
        TransformingLazyColumnState(
            initialAnchorItemIndex = initialAnchorItemIndex,
            initialAnchorItemScrollOffset = initialAnchorItemScrollOffset,
            prefetchStrategy = prefetchStrategy
        )
    }

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberTransformingLazyColumnState].
 */
@Stable
public class TransformingLazyColumnState
internal constructor(
    initialAnchorItemIndex: Int,
    initialAnchorItemScrollOffset: Int,
    private val prefetchStrategy: TransformingLazyColumnPrefetchStrategy =
        DefaultTransformingLazyColumnPrefetchStrategy(),
) : ScrollableState {

    /**
     * @param initialAnchorItemIndex the index of an item that is going to be placed in the center
     *   of the screen (if possible). This correlates with
     *   [TransformingLazyColumnState.anchorItemIndex].
     * @param initialAnchorItemScrollOffset the offset of an item to be used when placing the item
     *   in the center of the screen (if possible). This correlates with
     *   [TransformingLazyColumnState.anchorItemScrollOffset].
     */
    public constructor(
        initialAnchorItemIndex: Int = 0,
        initialAnchorItemScrollOffset: Int = 0,
    ) : this(
        initialAnchorItemIndex = initialAnchorItemIndex,
        initialAnchorItemScrollOffset = initialAnchorItemScrollOffset,
        prefetchStrategy = DefaultTransformingLazyColumnPrefetchStrategy()
    )

    public constructor() :
        this(
            initialAnchorItemIndex = 0,
            initialAnchorItemScrollOffset = 0,
            prefetchStrategy = DefaultTransformingLazyColumnPrefetchStrategy()
        )

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        scrollableState.scroll(scrollPriority, block)
    }

    internal val layoutInfoState =
        mutableStateOf(EmptyTransformingLazyColumnMeasureResult, neverEqualPolicy())

    internal val itemsCount: Int
        get() = layoutInfoState.value.totalItemsCount

    /**
     * The object of LazyColumnLayoutInfo calculated during the last layout pass. For example, you
     * can use it to calculate what items are currently visible. Note that this property is
     * observable and is updated after every scroll or remeasure. If you use it in the composable
     * function it will be recomposed on every change causing potential performance issues including
     * infinity recomposition loop. Therefore, avoid using it in the composition. If you want to run
     * some side effects like sending an analytics event or updating a state based on this value
     * consider using "snapshotFlow":
     */
    public val layoutInfo: TransformingLazyColumnLayoutInfo
        get() = layoutInfoState.value

    internal val density: Density
        get() = layoutInfoState.value.density

    internal var scrollToBeConsumed = 0f
        private set

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    /**
     * The index of the item that is used in scrolling. For the most cases that is the item closest
     * to the center of viewport from [TransformingLazyColumnLayoutInfo.visibleItems], however it
     * might change during scroll.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change causing potential performance issues.
     *
     * If you need to use it in the composition then consider wrapping the calculation into a
     * derived state in order to only have recompositions when the derived value changes:
     *
     * @sample androidx.wear.compose.foundation.samples.UsingListAnchorItemPositionInCompositionSample
     */
    public var anchorItemIndex: Int by mutableIntStateOf(initialAnchorItemIndex)
        private set

    /**
     * The scroll offset of the anchor item. Scrolling forward is positive - i.e., the amount that
     * the item is offset backwards.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     *
     * @see anchorItemIndex for samples with the recommended usage patterns.
     */
    public var anchorItemScrollOffset: Int by mutableIntStateOf(initialAnchorItemScrollOffset)
        private set

    internal var nearestRange: IntRange by
        mutableStateOf(
            calculateNearestItemsRange(initialAnchorItemIndex),
            structuralEqualityPolicy()
        )
        private set

    internal var lastMeasuredAnchorItemHeight: Int = Int.MIN_VALUE
        private set

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@TransformingLazyColumnState.remeasurement = remeasurement
            }
        }

    internal val prefetchState =
        LazyLayoutPrefetchState(prefetchStrategy.prefetchScheduler) {
            with(prefetchStrategy) {
                onNestedPrefetch(Snapshot.withoutReadObservation { anchorItemIndex })
            }
        }

    private val prefetchScope: TransformingLazyColumnPrefetchScope =
        object : TransformingLazyColumnPrefetchScope {
            override fun schedulePrefetch(index: Int): LazyLayoutPrefetchState.PrefetchHandle {
                // Without read observation since this can be triggered from scroll - this will then
                // cause us to recompose when the measure result changes. We don't care since the
                // prefetch is best effort.
                val constraints =
                    Snapshot.withoutReadObservation { layoutInfoState.value.childConstraints }
                return prefetchState.schedulePrefetch(index, constraints)
            }
        }

    private fun notifyPrefetchOnScroll(
        delta: Float,
        measureResult: TransformingLazyColumnMeasureResult
    ) {
        if (prefetchingEnabled) {
            with(prefetchStrategy) { prefetchScope.onScroll(delta, measureResult) }
        }
    }

    internal val animator = LazyLayoutItemAnimator<TransformingLazyColumnMeasuredItem>()

    internal fun applyMeasureResult(measureResult: TransformingLazyColumnMeasureResult) {
        // TODO(artemiy): Don't consume all scroll.
        scrollToBeConsumed = 0f
        anchorItemIndex = measureResult.anchorItemIndex
        anchorItemScrollOffset = measureResult.anchorItemScrollOffset
        lastMeasuredAnchorItemHeight = measureResult.lastMeasuredItemHeight
        layoutInfoState.value = measureResult
        canScrollBackward = measureResult.canScrollBackward
        canScrollForward = measureResult.canScrollForward
        nearestRange = calculateNearestItemsRange(measureResult.anchorItemIndex)
        if (prefetchingEnabled) {
            with(prefetchStrategy) { prefetchScope.onVisibleItemsUpdated(measureResult) }
        }
    }

    internal companion object {
        /**
         * We use the idea of sliding window as an optimization, so user can scroll up to this
         * number of items until we have to regenerate the key to index map.
         */
        private const val NearestItemsSlidingWindowSize = 20

        /**
         * The minimum amount of items near the current first visible item we want to have mapping
         * for.
         */
        private const val NearestItemsExtraItemCount = 30

        private fun calculateNearestItemsRange(anchorItemIndex: Int): IntRange {
            val slidingWindowStart =
                NearestItemsSlidingWindowSize * (anchorItemIndex / NearestItemsSlidingWindowSize)

            val start = maxOf(slidingWindowStart - NearestItemsExtraItemCount, 0)
            val end =
                slidingWindowStart + NearestItemsSlidingWindowSize + NearestItemsExtraItemCount
            return start until end
        }

        /** The default [Saver] implementation for [TransformingLazyColumnState]. */
        internal val Saver =
            listSaver<TransformingLazyColumnState, Int>(
                save = {
                    listOf(
                        it.anchorItemIndex,
                        it.anchorItemScrollOffset,
                    )
                },
                restore = {
                    TransformingLazyColumnState(
                        initialAnchorItemIndex = it[0],
                        initialAnchorItemScrollOffset = it[1]
                    )
                }
            )
    }

    private val scrollableState = ScrollableState { -onScroll(-it) }

    /**
     * Scrolls the item specified by [index] to the center of the screen.
     *
     * The scroll position [anchorItemIndex] and [anchorItemScrollOffset] will be updated to take
     * into account the new layout. There is no guarantee that [index] will become the new
     * [anchorItemIndex] since requested [scrollOffset] may position item with another index closer
     * to the anchor point.
     *
     * This operation happens instantly without animation.
     *
     * @param index The index of the item to scroll to. Must be non-negative.
     * @param scrollOffset The offset between the center of the screen and item's center. Positive
     *   offset means the item will be scrolled up.
     */
    public suspend fun scrollToItem(
        @androidx.annotation.IntRange(from = 0) index: Int,
        scrollOffset: Int = 0
    ) {
        scroll { snapToItemIndexInternal(index, scrollOffset) }
    }

    /**
     * Requests the item at [index] to be at the center of the viewport during the next remeasure,
     * offset by [scrollOffset].
     *
     * The scroll position [anchorItemIndex] and [anchorItemScrollOffset] will be updated to take
     * into account the new layout. There is no guarantee that [index] will become the new
     * [anchorItemIndex] since requested [scrollOffset] may position item with another index closer
     * to the anchor point.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the center item key (when a data set change will also be applied during the next
     * remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset The offset between the center of the screen and item's center. Positive
     *   offset means the item will be scrolled up.
     */
    public fun requestScrollToItem(
        @androidx.annotation.IntRange(from = 0) index: Int,
        scrollOffset: Int = 0
    ) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            layoutInfoState.value.coroutineScope.launch { scroll {} }
        }

        snapToItemIndexInternal(index, scrollOffset, forceRemeasure = false)
    }

    /**
     * Animate (smooth scroll) to the given item.
     *
     * The scroll position [anchorItemIndex] and [anchorItemScrollOffset] will be updated to take
     * into account the new layout. There is no guarantee that [index] will become the new
     * [anchorItemIndex] since requested [scrollOffset] may position item with another index closer
     * to the anchor point.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset The offset between the center of the screen and item's center. Positive
     *   offset means the item will be scrolled up.
     */
    public suspend fun animateScrollToItem(
        @androidx.annotation.IntRange(from = 0) index: Int,
        scrollOffset: Int = 0
    ) {
        scroll {
            TransformingLazyColumnScrollScope(this@TransformingLazyColumnState, this)
                .animateScrollToItem(index, scrollOffset, density, this)
        }
    }

    internal fun snapToItemIndexInternal(
        index: Int,
        scrollOffset: Int,
        forceRemeasure: Boolean = true
    ) {
        anchorItemIndex = index
        anchorItemScrollOffset = scrollOffset
        lastMeasuredAnchorItemHeight = Int.MIN_VALUE
        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        }
        nearestRange = calculateNearestItemsRange(anchorItemIndex)
    }

    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }

        scrollToBeConsumed += distance
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            animator.releaseAnimations()
            remeasurement?.forceRemeasure()

            notifyPrefetchOnScroll(
                preScrollToBeConsumed - scrollToBeConsumed,
                layoutInfoState.value
            )
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
}

private val EmptyTransformingLazyColumnMeasureResult =
    TransformingLazyColumnMeasureResult(
        anchorItemIndex = 0,
        anchorItemScrollOffset = 0,
        visibleItems = emptyList(),
        totalItemsCount = 0,
        lastMeasuredItemHeight = Int.MIN_VALUE,
        canScrollForward = false,
        canScrollBackward = false,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        density = Density(1f),
        itemSpacing = 0,
        beforeContentPadding = 0,
        afterContentPadding = 0,
        childConstraints = Constraints(),
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            }
    )
