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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Constraints

/**
 * State for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 */
@ExperimentalFoundationApi
@Stable
class LazyLayoutPrefetchState {
    internal var prefetcher: Prefetcher? by mutableStateOf(null)

    /**
     * Schedules precomposition and premeasure for the new item.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     */
    fun schedulePrefetch(index: Int, constraints: Constraints): PrefetchHandle {
        return prefetcher?.schedulePrefetch(index, constraints) ?: DummyHandle
    }

    sealed interface PrefetchHandle {
        /**
         * Notifies the prefetcher that previously scheduled item is no longer needed. If the item
         * was precomposed already it will be disposed.
         */
        fun cancel()
    }

    internal interface Prefetcher {
        fun schedulePrefetch(index: Int, constraints: Constraints): PrefetchHandle

        val timeTracker: AverageTimeTracker
    }

    internal abstract class AverageTimeTracker {

        /**
         * Average time the prefetching operations takes. Keeping it allows us to not start the work
         * if in this frame we are most likely not going to finish the work in time to not delay the
         * next frame.
         */
        var compositionTimeNs: Long = 0
            private set
        var measurementTimeNs: Long = 0
            private set

        abstract fun currentTime(): Long

        inline fun <T> trackComposition(block: () -> T): T {
            val beforeTimeNs = currentTime()
            val returnValue = block()
            compositionTimeNs = calculateAverageTime(
                currentTime() - beforeTimeNs,
                compositionTimeNs
            )
            return returnValue
        }

        inline fun <T> trackMeasurement(block: () -> T): T {
            val beforeTimeNs = currentTime()
            val returnValue = block()
            measurementTimeNs = calculateAverageTime(
                currentTime() - beforeTimeNs,
                measurementTimeNs
            )
            return returnValue
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
}

@ExperimentalFoundationApi
private object DummyHandle : LazyLayoutPrefetchState.PrefetchHandle {
    override fun cancel() {}
}
