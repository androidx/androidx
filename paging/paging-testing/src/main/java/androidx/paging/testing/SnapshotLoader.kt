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
import androidx.paging.PagingDataDiffer
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Contains the public APIs for load operations in tests.
 *
 * Tracks generational information and provides the listener to [DifferCallback] on
 * [PagingDataDiffer] operations.
 */
public class SnapshotLoader<Value : Any> internal constructor(
    private val differ: PagingDataDiffer<Value>
) {
    internal val generation = MutableStateFlow(Generation(0))

    // TODO add public loading APIs such as scrollTo(index)

    // the callback to be invoked by DifferCallback on a single generation
    // increase the callbackCount to notify SnapshotLoader that the dataset has updated
    internal fun onDataSetChanged(gen: Generation) {
        val currGen = generation.value
        // we make sure the generation with the dataset change is still valid because we
        // want to disregard callbacks on stale generations
        if (gen.id == currGen.id) {
            generation.value = gen.copy(
                callbackCount = currGen.callbackCount + 1
            )
        }
    }
}

internal data class Generation(
    // Id of the current Paging generation. Incremented on each new generation (when a new
    // PagingData is received).
    val id: Int,

    /**
     * A count of the number of times Paging invokes a [DifferCallback] callback within a single
     * generation. Incremented on each [DifferCallback] callback invoked, i.e. on item inserted.
     *
     * The callbackCount enables [SnapshotLoader] to await for a requested item and continue
     * loading next item only after a callback is invoked.
     */
    val callbackCount: Int = 0
)