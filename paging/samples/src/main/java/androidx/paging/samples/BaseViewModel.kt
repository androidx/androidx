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

package androidx.paging.samples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagingData
import androidx.paging.PagingConfig
import androidx.paging.PagingDataFlow
import androidx.paging.PagingDataFlowable
import androidx.paging.PagingSource
import androidx.paging.cachedIn

/**
 * No-op ViewModel base class to be used by sample code that doesn't show ViewModel impl
 */
open class BaseViewModel<T : Any> : ViewModel() {
    private lateinit var pagingSourceFactory: () -> PagingSource<String, T>

    val pagingFlow = PagingDataFlow(PagingConfig(pageSize = 40), pagingSourceFactory)
        .cachedIn(viewModelScope)

    val pagingFlowable = PagingDataFlowable(
        config = PagingConfig(pageSize = 40),
        pagingSourceFactory = pagingSourceFactory
    )

    val pagingLiveData = LivePagingData(
        config = PagingConfig(pageSize = 40),
        pagingSourceFactory = pagingSourceFactory
    )
}