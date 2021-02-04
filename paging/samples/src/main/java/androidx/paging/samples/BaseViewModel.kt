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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.rxjava2.cachedIn
import androidx.paging.rxjava2.flowable
import androidx.paging.liveData
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * No-op ViewModel base class to be used by sample code that doesn't show ViewModel impl
 */
open class BaseViewModel<T : Any> : ViewModel() {
    private lateinit var pagingSourceFactory: () -> PagingSource<String, T>

    private val pager = Pager(
        config = PagingConfig(pageSize = 40),
        pagingSourceFactory = pagingSourceFactory
    )

    val pagingFlow = pager.flow.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlowable = pager.flowable.cachedIn(viewModelScope)

    val pagingLiveData = pager.liveData.cachedIn(viewModelScope)
}