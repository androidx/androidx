/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.paging.testing

import androidx.paging.DifferCallback
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import androidx.paging.PagingSource
import androidx.paging.LoadType.APPEND
import androidx.paging.PagingConfig
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest

/**
 * Contains the public APIs for load operations in tests.
 *
 * Tracks generational information and provides the listener to [DifferCallback] on
 * [PagingDataDiffer] operations.
 */
public class SnapshotLoader<Value : Any> internal constructor(
    private val differ: PagingDataDiffer<Value>
) {
    internal val generations = MutableStateFlow(Generation())

    /**
     * Refresh the data that is presented on the UI.
     *
     * [refresh] triggers a new generation of [PagingData] / [PagingSource]
     * to represent an updated snapshot of the backing dataset.
     *
     * This fake paging operation mimics UI-driven refresh signals such as swipe-to-refresh.
     */
    public suspend fun refresh(): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading()
        differ.refresh()
        differ.awaitNotLoading()
    }

    /**
     * Imitates scrolling down paged items, [appending][APPEND] data until the given
     * predicate returns false.
     *
     * Note: This API loads an item before passing it into the predicate. This means the
     * loaded pages may include the page which contains the item that does not match the
     * predicate. For example, if pageSize = 2, the predicate
     * {item: Int -> item < 3 } will return items [[1, 2],[3, 4]] where [3, 4] is the page
     * containing the boundary item[3] not matching the predicate.
     *
     * The loaded pages are also dependent on [PagingConfig] settings such as
     * [PagingConfig.prefetchDistance]:
     * - if `prefetchDistance` > 0, the resulting appends will include prefetched items.
     * For example, if pageSize = 2 and prefetchDistance = 2, the predicate
     * {item: Int -> item < 3 } will load items [[1, 2], [3, 4], [5, 6]] where [5, 6] is the
     * prefetched page.
     *
     * @param [predicate] the predicate to match (return true) to continue append scrolls
     */
    public suspend fun appendScrollWhile(
        predicate: suspend (item: @JvmSuppressWildcards Value) -> @JvmSuppressWildcards Boolean
    ): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading()
        appendOrPrepend(LoadType.APPEND, predicate)
        differ.awaitNotLoading()
    }

    private suspend fun appendOrPrepend(
        loadType: LoadType,
        predicate: suspend (item: Value) -> Boolean
    ) {
        do {
            // Get and update the index to load from. Return if index is invalid.
            val index = nextLoadIndexOrNull(loadType) ?: return
            val item = loadItem(index)
        } while (predicate(item))
    }

    /**
     * Get and update the index to load from. Returns null if next index is out of bounds.
     *
     * This method is responsible for updating the [Generation.lastAccessedIndex] that is then sent
     * to the differ to trigger load for that index.
     */
    private fun nextLoadIndexOrNull(loadType: LoadType): Int? {
        val currGen = generations.value
        return when (loadType) {
            LoadType.PREPEND -> {
                if (currGen.lastAccessedIndex.get() <= 0) {
                    return null
                }
                currGen.lastAccessedIndex.decrementAndGet()
            }
            LoadType.APPEND -> {
                if (currGen.lastAccessedIndex.get() >= differ.size - 1) {
                    return null
                }
                currGen.lastAccessedIndex.incrementAndGet()
            }
        }
    }

    // Executes actual loading by accessing the PagingDataDiffer
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun loadItem(index: Int): Value {
        differ[index]

        // awaits for the item to be loaded
        return generations.mapLatest {
            differ.peek(index)
        }.filterNotNull().first()
    }

    /**
     * The callback to be invoked by DifferCallback on a single generation.
     * Increase the callbackCount to notify SnapshotLoader that the dataset has updated
     */
    internal fun onDataSetChanged(gen: Generation) {
        val currGen = generations.value
        // we make sure the generation with the dataset change is still valid because we
        // want to disregard callbacks on stale generations
        if (gen.id == currGen.id) {
            generations.value = gen.copy(
                callbackCount = currGen.callbackCount + 1
            )
        }
    }

    private enum class LoadType {
        PREPEND,
        APPEND
    }
}

internal data class Generation(
    /**
     * Id of the current Paging generation. Incremented on each new generation (when a new
     * PagingData is received).
     */
    val id: Int = -1,

    /**
     * A count of the number of times Paging invokes a [DifferCallback] callback within a single
     * generation. Incremented on each [DifferCallback] callback invoked, i.e. on item inserted.
     *
     * The callbackCount enables [SnapshotLoader] to await for a requested item and continue
     * loading next item only after a callback is invoked.
     */
    val callbackCount: Int = 0,

    /**
     * Tracks the last accessed index on the differ for this generation
      */
    var lastAccessedIndex: AtomicInteger = AtomicInteger()
)