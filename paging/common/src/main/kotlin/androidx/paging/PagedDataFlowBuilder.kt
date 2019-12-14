/*
 * Copyright 2019 The Android Open Source Project
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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow

internal class PagedDataFlowBuilder<Key : Any, Value : Any> {
    private var pagedSourceFactory: PagedSourceFactory<Key, Value>? = null
    private var initialKey: Key? = null
    private var config: PagedList.Config? = null

    private var isIntitialKeySet = false

    fun setPagedSourceFactory(pagedSourceFactory: PagedSourceFactory<Key, Value>) = apply {
        this.pagedSourceFactory = pagedSourceFactory
    }

    fun setInitialKey(initialKey: Key?) = apply {
        isIntitialKeySet = true
        this.initialKey = initialKey
    }

    fun setConfig(config: PagedList.Config) = apply { this.config = config }

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun build(): Flow<PagedData<Value>> {
        checkNotNull(pagedSourceFactory) { "You must call setPagedsourceFactory before build" }
        check(isIntitialKeySet) { "You must call setInitialKey before build" }
        checkNotNull(config) { "You must call setConfig before build" }

        return PageFetcher(pagedSourceFactory!!, initialKey, config!!).flow
    }
}
