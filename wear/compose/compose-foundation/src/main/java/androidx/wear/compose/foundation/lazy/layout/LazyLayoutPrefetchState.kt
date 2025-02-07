/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy.layout

import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.trace
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutPrefetchState.LazyLayoutPrefetchResultScope
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * State for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy layouts.
 * LazyLayout and all corresponding APIs are still under development and are subject to change.
 *
 * @param prefetchScheduler the [PrefetchScheduler] implementation to use to execute prefetch
 *   requests. If null is provided, the default [PrefetchScheduler] for the platform will be used.
 * @param onNestedPrefetch a callback which will be invoked when this LazyLayout is prefetched in
 *   context of a parent LazyLayout, giving a chance to recursively prefetch its own children. See
 *   [NestedPrefetchScope].
 */
@Stable
internal class LazyLayoutPrefetchState(
    internal val prefetchScheduler: PrefetchScheduler? = null,
    private val onNestedPrefetch: (NestedPrefetchScope.() -> Unit)? = null
) {

    private val prefetchMetrics: PrefetchMetrics = PrefetchMetrics()
    internal var prefetchHandleProvider: PrefetchHandleProvider? = null

    /**
     * Schedules precomposition for the new item. If you also want to premeasure the item please use
     * a second overload accepting a [Constraints] param.
     *
     * @param index item index to prefetch.
     */
    fun schedulePrefetch(index: Int): PrefetchHandle = schedulePrefetch(index, ZeroConstraints)

    /**
     * Schedules precomposition and premeasure for the new item.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     * @param onItemPrefetched This callback is called when the item premeasuring is finished. If
     *   the request is canceled or no measuring is performed this callback won't be called. Use
     *   [LazyLayoutPrefetchResultScope.getSize] to get the item's size.
     */
    fun schedulePrefetch(
        index: Int,
        constraints: Constraints,
        onItemPrefetched: (LazyLayoutPrefetchResultScope.() -> Unit)? = null
    ): PrefetchHandle {

        return prefetchHandleProvider?.schedulePrefetch(
            index,
            constraints,
            prefetchMetrics,
            onItemPrefetched
        ) ?: DummyHandle
    }

    internal fun collectNestedPrefetchRequests(): List<PrefetchRequest> {
        val onNestedPrefetch = onNestedPrefetch ?: return emptyList()

        return NestedPrefetchScopeImpl().run {
            onNestedPrefetch()
            requests
        }
    }

    sealed interface PrefetchHandle {
        /**
         * Notifies the prefetcher that previously scheduled item is no longer needed. If the item
         * was precomposed already it will be disposed.
         */
        fun cancel()

        /**
         * Marks this prefetch request as urgent, which is a way to communicate that the requested
         * item is expected to be needed during the next frame.
         *
         * For urgent requests we can proceed with doing the prefetch even if the available time in
         * the frame is less than we spend on similar prefetch requests on average.
         */
        fun markAsUrgent()
    }

    /**
     * A scope for [schedulePrefetch] callbacks. The scope provides additional information about a
     * prefetched item.
     */
    interface LazyLayoutPrefetchResultScope {

        /** The amount of placeables composed into this item. */
        val placeablesCount: Int

        /** Retrieves the latest measured size for a given placeable [index]. */
        fun getSize(index: Int): IntSize
    }

    private inner class NestedPrefetchScopeImpl : NestedPrefetchScope {

        val requests: List<PrefetchRequest>
            get() = _requests

        private val _requests: MutableList<PrefetchRequest> = mutableListOf()

        override fun schedulePrefetch(index: Int) {
            schedulePrefetch(index, ZeroConstraints)
        }

        override fun schedulePrefetch(index: Int, constraints: Constraints) {
            val prefetchHandleProvider = prefetchHandleProvider ?: return
            _requests.add(
                prefetchHandleProvider.createNestedPrefetchRequest(
                    index,
                    constraints,
                    prefetchMetrics
                )
            )
        }
    }
}

/**
 * A scope which allows nested prefetches to be requested for the precomposition of a LazyLayout.
 */
internal sealed interface NestedPrefetchScope {

    /**
     * Requests a child index to be prefetched as part of the prefetch of a parent LazyLayout.
     *
     * The prefetch will only do the precomposition for the new item. If you also want to premeasure
     * please use a second overload accepting a [Constraints] param.
     *
     * @param index item index to prefetch.
     */
    fun schedulePrefetch(index: Int)

    /**
     * Requests a child index to be prefetched as part of the prefetch of a parent LazyLayout.
     *
     * @param index the index of the child to prefetch.
     * @param constraints [Constraints] to use for premeasuring. If null, the child will not be
     *   premeasured.
     */
    fun schedulePrefetch(index: Int, constraints: Constraints)
}

/**
 * [PrefetchMetrics] tracks timings for subcompositions so that they can be used to estimate whether
 * we can fit prefetch work into idle time without delaying the start of the next frame.
 */
internal class PrefetchMetrics {

    /**
     * We keep the overall average numbers and averages for each content type separately. the idea
     * is once we encounter a new content type we don't want to start with no averages, instead we
     * use the overall averages initially until we collected more data.
     */
    private fun getAverage(contentType: Any?): Averages {
        val lastUsedAverage = this@PrefetchMetrics.lastUsedAverage
        return if (lastUsedContentType === contentType && lastUsedAverage != null) {
            lastUsedAverage
        } else {
            averagesByContentType
                .getOrPut(contentType) { overallAverage.copy() }
                .also {
                    this.lastUsedContentType = contentType
                    this.lastUsedAverage = it
                }
        }
    }

    private val overallAverage = Averages()
    private val averagesByContentType = mutableScatterMapOf<Any?, Averages>()

    private var lastUsedContentType: Any? = null
    private var lastUsedAverage: Averages? = null

    fun getCompositionTimeNanos(contentType: Any?) = getAverage(contentType).compositionTimeNanos

    fun getMeasureTimeNanos(contentType: Any?) = getAverage(contentType).measureTimeNanos

    fun saveCompositionTime(contentType: Any?, timeNanos: Long) {
        overallAverage.saveCompositionTimeNanos(timeNanos)
        getAverage(contentType).saveCompositionTimeNanos(timeNanos)
    }

    fun saveMeasureTime(contentType: Any?, timeNanos: Long) {
        overallAverage.saveMeasureTimeNanos(timeNanos)
        getAverage(contentType).saveMeasureTimeNanos(timeNanos)
    }
}

private class Averages {
    /** Average time the full composition phase has taken. */
    var compositionTimeNanos: Long = 0L
    /** Average time the measure phase has taken. */
    var measureTimeNanos: Long = 0L

    fun saveCompositionTimeNanos(timeNanos: Long) {
        compositionTimeNanos = calculateAverageTime(timeNanos, compositionTimeNanos)
    }

    fun saveMeasureTimeNanos(timeNanos: Long) {
        measureTimeNanos = calculateAverageTime(timeNanos, measureTimeNanos)
    }

    fun copy() =
        Averages().also {
            it.compositionTimeNanos = compositionTimeNanos
            it.measureTimeNanos = measureTimeNanos
        }

    private fun calculateAverageTime(new: Long, current: Long): Long {
        // Calculate a weighted moving average of time taken to compose an item. We use weighted
        // moving average to bias toward more recent measurements, and to minimize storage /
        // computation cost. (the idea is taken from RecycledViewPool)
        return if (current == 0L) {
            new
        } else {
            // dividing first to avoid a potential overflow
            current / 4 * 3 + new / 4
        }
    }
}

private object DummyHandle : PrefetchHandle {
    override fun cancel() {}

    override fun markAsUrgent() {}
}

/**
 * PrefetchHandleProvider is used to connect the [LazyLayoutPrefetchState], which provides the API
 * to schedule prefetches, to a [LazyLayoutItemContentFactory] which resolves key and content from
 * an index, [SubcomposeLayoutState] which knows how to precompose/premeasure, and a specific
 * [PrefetchScheduler] used to execute a request.
 */
internal class PrefetchHandleProvider(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    private val executor: PrefetchScheduler,
) {
    fun schedulePrefetch(
        index: Int,
        constraints: Constraints,
        prefetchMetrics: PrefetchMetrics,
        onItemPrefetched: (LazyLayoutPrefetchResultScope.() -> Unit)?
    ): PrefetchHandle =
        HandleAndRequestImpl(index, constraints, prefetchMetrics, onItemPrefetched).also {
            executor.schedulePrefetch(it)
        }

    fun createNestedPrefetchRequest(
        index: Int,
        constraints: Constraints,
        prefetchMetrics: PrefetchMetrics,
    ): PrefetchRequest =
        HandleAndRequestImpl(index, constraints = constraints, prefetchMetrics, null)

    private inner class HandleAndRequestImpl(
        private val index: Int,
        private val constraints: Constraints,
        private val prefetchMetrics: PrefetchMetrics,
        private val onItemPrefetched: (LazyLayoutPrefetchResultScope.() -> Unit)?
    ) : PrefetchHandle, PrefetchRequest, LazyLayoutPrefetchResultScope {

        private var precomposeHandle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
        private var isMeasured = false
        private var isCanceled = false
        private val isComposed
            get() = precomposeHandle != null

        private var hasResolvedNestedPrefetches = false
        private var nestedPrefetchController: NestedPrefetchController? = null
        private var isUrgent = false

        override fun cancel() {
            if (!isCanceled) {
                isCanceled = true
                precomposeHandle?.dispose()
                precomposeHandle = null
            }
        }

        override fun markAsUrgent() {
            isUrgent = true
        }

        override val placeablesCount: Int
            get() = (precomposeHandle?.placeablesCount ?: 0)

        override fun getSize(index: Int): IntSize {
            return (precomposeHandle?.getSize(index) ?: IntSize.Zero)
        }

        private fun shouldExecute(available: Long, average: Long): Boolean {
            // even for urgent request we only do the work if we have time available, as otherwise
            // it is better to just return early to allow the next frame to start and do the work.
            return (isUrgent && available > 0) || average < available
        }

        private var availableTimeNanos = 0L
        private var elapsedTimeNanos = 0L
        @OptIn(ExperimentalTime::class) private var startTime = markNow()

        @OptIn(ExperimentalTime::class)
        private fun resetAvailableTimeTo(availableTimeNanos: Long) {
            this.availableTimeNanos = availableTimeNanos
            startTime = markNow()
            elapsedTimeNanos = 0L
        }

        @OptIn(ExperimentalTime::class)
        private fun updateElapsedAndAvailableTime() {
            val now = markNow()
            elapsedTimeNanos = (now - startTime).inWholeNanoseconds
            availableTimeNanos -= elapsedTimeNanos
            startTime = now
        }

        override fun PrefetchRequestScope.execute(): Boolean {
            val itemProvider = itemContentFactory.itemProvider()

            val isValid = !isCanceled && index in 0 until itemProvider.itemCount
            if (!isValid) {
                return false
            }

            val contentType = itemProvider.getContentType(index)

            // we save the value we get from availableTimeNanos() into a local variable once
            // and manually update it later by calling updateElapsedAndAvailableTime()
            resetAvailableTimeTo(availableTimeNanos())

            if (!isComposed) {
                if (
                    shouldExecute(
                        availableTimeNanos,
                        prefetchMetrics.getCompositionTimeNanos(contentType)
                    )
                ) {
                    trace("compose:lazy:prefetch:compose") {
                        performFullComposition(itemProvider, contentType)
                    }
                    updateElapsedAndAvailableTime()
                    prefetchMetrics.saveCompositionTime(contentType, elapsedTimeNanos)
                } else {
                    return true
                }
            }

            // if the request is urgent we better proceed with the measuring straight away instead
            // of spending time trying to split the work more via nested prefetch. nested prefetch
            // is always an estimation and it could potentially do work we will not need in the end,
            // but the measuring will only do exactly the needed work (including composing nested
            // lazy layouts)
            if (!isUrgent) {
                // Nested prefetch logic is best-effort: if nested LazyLayout children are
                // added/removed/updated after we've resolved nested prefetch states here or
                // resolved
                // nestedPrefetchRequests below, those changes won't be taken into account.
                if (!hasResolvedNestedPrefetches) {
                    if (availableTimeNanos > 0) {
                        trace("compose:lazy:prefetch:resolve-nested") {
                            nestedPrefetchController = resolveNestedPrefetchStates()
                            hasResolvedNestedPrefetches = true
                        }
                    } else {
                        return true
                    }
                }

                val hasMoreWork =
                    nestedPrefetchController?.run { executeNestedPrefetches() } ?: false
                if (hasMoreWork) {
                    return true
                }
                updateElapsedAndAvailableTime()
            }

            if (!isMeasured && !constraints.isZero) {
                if (
                    shouldExecute(
                        availableTimeNanos,
                        prefetchMetrics.getMeasureTimeNanos(contentType)
                    )
                ) {
                    trace("compose:lazy:prefetch:measure") { performMeasure(constraints) }
                    updateElapsedAndAvailableTime()
                    prefetchMetrics.saveMeasureTime(contentType, elapsedTimeNanos)
                    onItemPrefetched?.invoke(this@HandleAndRequestImpl)
                } else {
                    return true
                }
            }

            // All our work is done
            return false
        }

        private fun performFullComposition(
            itemProvider: LazyLayoutItemProvider,
            contentType: Any?
        ) {
            val key = itemProvider.getKey(index)
            val content = itemContentFactory.getContent(index, key, contentType)
            precomposeHandle = subcomposeLayoutState.precompose(key, content)
        }

        private fun performMeasure(constraints: Constraints) {
            isMeasured = true
            val handle = precomposeHandle!!
            repeat(handle.placeablesCount) { placeableIndex ->
                handle.premeasure(placeableIndex, constraints)
            }
        }

        private fun resolveNestedPrefetchStates(): NestedPrefetchController? {
            val precomposedSlotHandle = precomposeHandle!!

            var nestedStates: MutableList<LazyLayoutPrefetchState>? = null
            precomposedSlotHandle.traverseDescendants(TraversablePrefetchStateNodeKey) {
                val prefetchState = (it as TraversablePrefetchStateNode).prefetchState
                nestedStates =
                    nestedStates?.apply { add(prefetchState) } ?: mutableListOf(prefetchState)
                TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
            }
            return nestedStates?.let { NestedPrefetchController(it) }
        }

        override fun toString(): String =
            "HandleAndRequestImpl { index = $index, constraints = $constraints, " +
                "isComposed = $isComposed, isMeasured = $isMeasured, isCanceled = $isCanceled }"

        private inner class NestedPrefetchController(
            private val states: List<LazyLayoutPrefetchState>
        ) {

            // This array is parallel to nestedPrefetchStates, so index 0 in nestedPrefetchStates
            // corresponds to index 0 in this array, etc.
            private val requestsByState: Array<List<PrefetchRequest>?> = arrayOfNulls(states.size)
            private var stateIndex: Int = 0
            private var requestIndex: Int = 0

            init {}

            fun PrefetchRequestScope.executeNestedPrefetches(): Boolean {
                if (stateIndex >= states.size) {
                    return false
                }

                trace("compose:lazy:prefetch:nested") {
                    while (stateIndex < states.size) {
                        if (requestsByState[stateIndex] == null) {
                            if (availableTimeNanos() <= 0) {
                                // When we have time again, we'll resolve nested requests for this
                                // state
                                return true
                            }

                            requestsByState[stateIndex] =
                                states[stateIndex].collectNestedPrefetchRequests()
                        }

                        val nestedRequests = requestsByState[stateIndex]!!
                        while (requestIndex < nestedRequests.size) {
                            val hasMoreWork = with(nestedRequests[requestIndex]) { execute() }
                            if (hasMoreWork) {
                                return true
                            } else {
                                requestIndex++
                            }
                        }

                        requestIndex = 0
                        stateIndex++
                    }
                }

                return false
            }
        }
    }
}

private const val TraversablePrefetchStateNodeKey =
    "androidx.wear.compose.foundation.lazy.layout.TraversablePrefetchStateNode"

/**
 * A modifier which lets the [LazyLayoutPrefetchState] for a [LazyLayout] to be discoverable via
 * [TraversableNode] traversal.
 */
internal fun Modifier.traversablePrefetchState(
    lazyLayoutPrefetchState: LazyLayoutPrefetchState?
): Modifier {
    return lazyLayoutPrefetchState?.let { this then TraversablePrefetchStateModifierElement(it) }
        ?: this
}

private class TraversablePrefetchStateNode(
    var prefetchState: LazyLayoutPrefetchState,
) : Modifier.Node(), TraversableNode {

    override val traverseKey: String = TraversablePrefetchStateNodeKey
}

private data class TraversablePrefetchStateModifierElement(
    private val prefetchState: LazyLayoutPrefetchState,
) : ModifierNodeElement<TraversablePrefetchStateNode>() {
    override fun create() = TraversablePrefetchStateNode(prefetchState)

    override fun update(node: TraversablePrefetchStateNode) {
        node.prefetchState = prefetchState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "traversablePrefetchState"
        value = prefetchState
    }
}

private val ZeroConstraints = Constraints(maxWidth = 0, maxHeight = 0)
