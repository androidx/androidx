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
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.unit.Constraints

/**
 * State for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy
 * layouts. LazyLayout and all corresponding APIs are still under development and are subject to
 * change.
 *
 * @param prefetchExecutor the PrefetchExecutor implementation to use to execute prefetch requests.
 * If null is provided, the default PrefetchExecutor for the platform will be used.
 */
@ExperimentalFoundationApi
@Stable
class LazyLayoutPrefetchState(internal val prefetchExecutor: PrefetchExecutor? = null) {
    internal var prefetchHandleProvider: PrefetchHandleProvider? = null

    /**
     * Schedules precomposition and premeasure for the new item.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     */
    fun schedulePrefetch(index: Int, constraints: Constraints): PrefetchHandle {
        return prefetchHandleProvider?.schedulePrefetch(index, constraints) ?: DummyHandle
    }

    sealed interface PrefetchHandle {
        /**
         * Notifies the prefetcher that previously scheduled item is no longer needed. If the item
         * was precomposed already it will be disposed.
         */
        fun cancel()
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
    fun schedulePrefetch(index: Int, constraints: Constraints): PrefetchHandle =
        HandleAndRequestImpl(index, constraints).also {
            executor.requestPrefetch(it)
        }

    @ExperimentalFoundationApi
    private inner class HandleAndRequestImpl(
        private val index: Int,
        private val constraints: Constraints
    ) : PrefetchHandle, PrefetchExecutor.Request {

        private var precomposeHandle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
        private var isMeasured = false
        private var isCanceled = false

        override val isValid: Boolean
            get() = !isCanceled &&
                index in 0 until itemContentFactory.itemProvider().itemCount

        override val isComposed get() = precomposeHandle != null

        override fun cancel() {
            if (!isCanceled) {
                isCanceled = true
                precomposeHandle?.dispose()
                precomposeHandle = null
            }
        }

        override fun performComposition() {
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

        override fun performMeasure() {
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

        override fun toString(): String =
            "HandleAndRequestImpl { index = $index, constraints = $constraints, " +
                "isComposed = $isComposed, isMeasured = $isMeasured, isCanceled = $isCanceled }"
    }
}
