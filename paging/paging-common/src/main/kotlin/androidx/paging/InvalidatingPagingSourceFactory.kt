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

package androidx.paging

import androidx.annotation.VisibleForTesting
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Wrapper class for a [PagingSource] factory intended for usage in [Pager] construction.
 *
 * Calling [invalidate] on this [InvalidatingPagingSourceFactory] will forward invalidate signals
 * to all active [PagingSource]s that were produced by calling [invoke].
 *
 * This class is backed by a [CopyOnWriteArrayList], which is thread-safe for concurrent calls to
 * any mutative operations including both [invoke] and [invalidate].
 *
 * @param pagingSourceFactory The [PagingSource] factory that returns a PagingSource when called
 */
public class InvalidatingPagingSourceFactory<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>
) : PagingSourceFactory<Key, Value> {

    @VisibleForTesting
    internal val pagingSources = CopyOnWriteArrayList<PagingSource<Key, Value>>()

    /**
     * @return [PagingSource] which will be invalidated when this factory's [invalidate] method
     * is called
     */
    override fun invoke(): PagingSource<Key, Value> {
        return pagingSourceFactory().also { pagingSources.add(it) }
    }

    /**
     * Calls [PagingSource.invalidate] on each [PagingSource] that was produced by this
     * [InvalidatingPagingSourceFactory]
     */
    public fun invalidate() {
        for (pagingSource in pagingSources) {
            if (!pagingSource.invalid) {
                pagingSource.invalidate()
            }
        }

        pagingSources.removeAll { it.invalid }
    }
}