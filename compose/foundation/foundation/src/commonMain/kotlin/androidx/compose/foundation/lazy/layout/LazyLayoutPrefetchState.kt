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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import kotlin.system.measureNanoTime

/**
 * State for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy
 * layouts. LazyLayout and all corresponding APIs are still under development and are subject to
 * change.
 *
 * @param prefetchExecutor the PrefetchExecutor implementation to use to execute prefetch requests.
 * If null is provided, the default PrefetchExecutor for the platform will be used.
 * @param onNestedPrefetch a callback which will be invoked when this LazyLayout is prefetched in
 * context of a parent LazyLayout, giving a chance to recursively prefetch its own children. See
 * [NestedPrefetchScope].
 */
@ExperimentalFoundationApi
@Stable
class LazyLayoutPrefetchState(
    internal val prefetchExecutor: PrefetchExecutor? = null,
    private val onNestedPrefetch: (NestedPrefetchScope.() -> Unit)? = null
) {

    private val prefetchMetrics: PrefetchMetrics = PrefetchMetrics()
    internal var prefetchHandleProvider: PrefetchHandleProvider? = null

    /**
     * Schedules precomposition and premeasure for the new item.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring. If null, the child will not
     * be premeasured.
     */
    fun schedulePrefetch(index: Int, constraints: Constraints? = null): PrefetchHandle {
        return prefetchHandleProvider?.schedulePrefetch(index, constraints, prefetchMetrics)
            ?: DummyHandle
    }

    internal fun collectNestedPrefetchRequests(): List<PrefetchRequest> {
        val onNestedPrefetch = onNestedPrefetch ?: return listOf()

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
    }

    private inner class NestedPrefetchScopeImpl : NestedPrefetchScope {

        val requests: List<PrefetchRequest>
            get() = _requests
        private val _requests: MutableList<PrefetchRequest> = mutableListOf()

        override fun schedulePrefetch(index: Int, constraints: Constraints?) {
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
@ExperimentalFoundationApi
sealed interface NestedPrefetchScope {

    /**
     * Requests a child index to be prefetched as part of the prefetch of a parent LazyLayout.
     * @param index the index of the child to prefetch.
     * @param constraints [Constraints] to use for premeasuring. If null, the child will not
     * be premeasured.
     */
    fun schedulePrefetch(index: Int, constraints: Constraints? = null)
}

/**
 * [PrefetchMetrics] tracks composition and measure timings for subcompositions so that they can be
 * used to estimate whether we can fit prefetch work into idle time without delaying the start of
 * the next frame.
 */
@ExperimentalFoundationApi
internal class PrefetchMetrics {

    /**
     * The current average time composition has taken during prefetches of this LazyLayout.
     */
    var averageCompositionTimeNanos: Long = 0L
        private set

    /**
     * The current average time measure has taken during prefetches of this LazyLayout.
     */
    var averageMeasureTimeNanos: Long = 0L
        private set

    /**
     * Executes the [doComposition] block and updates [averageCompositionTimeNanos] with the new
     * average.
     */
    internal inline fun recordCompositionTiming(doComposition: () -> Unit) {
        val executionTime = measureNanoTime(doComposition)
        averageCompositionTimeNanos =
            calculateAverageTime(executionTime, averageCompositionTimeNanos)
    }

    /**
     * Executes the [doMeasure] block and updates [averageMeasureTimeNanos] with the new average.
     */
    internal inline fun recordMeasureTiming(doMeasure: () -> Unit) {
        val executionTime = measureNanoTime(doMeasure)
        averageMeasureTimeNanos = calculateAverageTime(executionTime, averageMeasureTimeNanos)
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

@ExperimentalFoundationApi
private object DummyHandle : PrefetchHandle {
    override fun cancel() {}
}

/**
 * PrefetchHandleProvider is used to connect the [LazyLayoutPrefetchState], which provides the API
 * to schedule prefetches, to a [LazyLayoutItemContentFactory] which resolves key and content from
 * an index, [SubcomposeLayoutState] which knows how to precompose/premeasure,
 * and a specific [PrefetchExecutor] used to execute a request.
 */
@ExperimentalFoundationApi
internal class PrefetchHandleProvider(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    private val executor: PrefetchExecutor
) {
    fun schedulePrefetch(
        index: Int,
        constraints: Constraints?,
        prefetchMetrics: PrefetchMetrics
    ): PrefetchHandle =
        HandleAndRequestImpl(index, constraints, prefetchMetrics).also {
            executor.requestPrefetch(it)
        }

    fun createNestedPrefetchRequest(
        index: Int,
        constraints: Constraints?,
        prefetchMetrics: PrefetchMetrics,
    ): PrefetchRequest =
        HandleAndRequestImpl(index, constraints = constraints, prefetchMetrics)

    @ExperimentalFoundationApi
    private inner class HandleAndRequestImpl(
        private val index: Int,
        private val constraints: Constraints?,
        private val prefetchMetrics: PrefetchMetrics,
    ) : PrefetchHandle, PrefetchRequest {

        private var precomposeHandle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
        private var isMeasured = false
        private var isCanceled = false
        private val isComposed get() = precomposeHandle != null
        private var hasResolvedNestedPrefetches = false
        private var nestedPrefetchController: NestedPrefetchController? = null

        private val isValid
            get() = !isCanceled &&
                index in 0 until itemContentFactory.itemProvider().itemCount

        override fun cancel() {
            if (!isCanceled) {
                isCanceled = true
                precomposeHandle?.dispose()
                precomposeHandle = null
            }
        }

        override fun PrefetchRequestScope.execute(): Boolean {
            if (!isValid) {
                return false
            }

            if (!isComposed) {
                if (prefetchMetrics.averageCompositionTimeNanos < availableTimeNanos) {
                    prefetchMetrics.recordCompositionTiming {
                        trace("compose:lazy:prefetch:compose") {
                            performComposition()
                        }
                    }
                } else {
                    return true
                }
            }

            // Nested prefetch logic is best-effort: if nested LazyLayout children are
            // added/removed/updated after we've resolved nested prefetch states here or resolved
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

            val hasMoreWork = nestedPrefetchController?.run { executeNestedPrefetches() } ?: false
            if (hasMoreWork) {
                return true
            }

            if (!isMeasured && constraints != null) {
                if (prefetchMetrics.averageMeasureTimeNanos < availableTimeNanos) {
                    prefetchMetrics.recordMeasureTiming {
                        trace("compose:lazy:prefetch:measure") {
                            performMeasure(constraints)
                        }
                    }
                } else {
                    return true
                }
            }

            // All our work is done
            return false
        }

        private fun performComposition() {
            require(isValid) {
                "Callers should check whether the request is still valid before calling " +
                    "performComposition()"
            }
            require(precomposeHandle == null) { "Request was already composed!" }
            val itemProvider = itemContentFactory.itemProvider()
            val key = itemProvider.getKey(index)
            val contentType = itemProvider.getContentType(index)
            val content = itemContentFactory.getContent(index, key, contentType)
            precomposeHandle = subcomposeLayoutState.precompose(key, content)
        }

        private fun performMeasure(constraints: Constraints) {
            require(!isCanceled) {
                "Callers should check whether the request is still valid before calling " +
                    "performMeasure()"
            }
            require(!isMeasured) { "Request was already measured!" }
            isMeasured = true
            val handle = requireNotNull(precomposeHandle) {
                "performComposition() must be called before performMeasure()"
            }
            repeat(handle.placeablesCount) { placeableIndex ->
                handle.premeasure(placeableIndex, constraints)
            }
        }

        private fun resolveNestedPrefetchStates(): NestedPrefetchController? {
            val precomposedSlotHandle = requireNotNull(precomposeHandle) {
                "Should precompose before resolving nested prefetch states"
            }

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

            init {
                require(states.isNotEmpty()) {
                    "NestedPrefetchController shouldn't be created with no states"
                }
            }

            fun PrefetchRequestScope.executeNestedPrefetches(): Boolean {
                if (stateIndex >= states.size) {
                    return false
                }
                check(!isCanceled) { "Should not execute nested prefetch on canceled request" }

                trace("compose:lazy:prefetch:nested") {
                    while (stateIndex < states.size) {
                        if (requestsByState[stateIndex] == null) {
                            if (availableTimeNanos <= 0) {
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
    "androidx.compose.foundation.lazy.layout.TraversablePrefetchStateNode"

/**
 * A modifier which lets the [LazyLayoutPrefetchState] for a [LazyLayout] to be discoverable via
 * [TraversableNode] traversal.
 */
@ExperimentalFoundationApi
internal fun Modifier.traversablePrefetchState(
    lazyLayoutPrefetchState: LazyLayoutPrefetchState?
): Modifier {
    return lazyLayoutPrefetchState?.let {
        this then TraversablePrefetchStateModifierElement(it)
    } ?: this
}

@ExperimentalFoundationApi
private class TraversablePrefetchStateNode(
    var prefetchState: LazyLayoutPrefetchState,
) : Modifier.Node(), TraversableNode {

    override val traverseKey: String = TraversablePrefetchStateNodeKey
}

@ExperimentalFoundationApi
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
