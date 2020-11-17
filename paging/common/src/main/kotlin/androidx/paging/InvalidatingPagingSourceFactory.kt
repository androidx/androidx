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

/**
 * A PagingSource Factory that can be kept as a reference and stores a list of paging sources
 * created with the factory. Includes invalidate() function to invalidate the list of [PagingSource]
 * when data changes.
 */
abstract class InvalidatingPagingSourceFactory<Key: Any, Value: Any> : () ->  PagingSource<Key, Value>{

    @VisibleForTesting
    internal val listOfPagingSources = mutableListOf<PagingSource<Key, Value>>();

    /**
     * Returns a PagingSource
     *
     * Implement the logic to create a PagingSource
     */
    abstract fun create() : PagingSource<Key, Value>;

    /**
     * Returns a PagingSource which will also be stored into listOfPagingSources.
     */
    final override fun invoke(): PagingSource<Key, Value> {
        return create(). also {listOfPagingSources.add(it)};
    }

    /**
     * Calls [PagingSource] invalidate() on each PagingSource stored within listOfPagingSources
     * and removes it from the list
     *
     * Invalidated [PagingSource] will have its invalid status equal 'true'
     */
    fun invalidate() {
        while (listOfPagingSources.isNotEmpty()) {
            listOfPagingSources.removeFirst().invalidate();
        }
    }
}