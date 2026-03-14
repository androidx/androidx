/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.Sampled
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.ItemSnapshotList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.asState
import androidx.paging.insertSeparators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Sampled
fun PagerAsStateSample() {
    lateinit var pagingSourceFactory: () -> PagingSource<String, SampleData.Item>

    /** Initialize Pager in ViewModel */
    val pager =
        Pager(config = PagingConfig(pageSize = 40), pagingSourceFactory = pagingSourceFactory)

    val itemsFlow = pager.flow.asState()
}

@Sampled
fun PagerAsStateWithSeparatorsSample() {
    lateinit var pagingSourceFactory: () -> PagingSource<String, SampleData.Item>

    /** Initialize Pager in ViewModel */
    val pager =
        Pager(config = PagingConfig(pageSize = 40), pagingSourceFactory = pagingSourceFactory)

    val itemsFlow: Flow<ItemSnapshotList<SampleData>> =
        pager.flow
            /**
             * To improve performance, apply mapping and transformations on [Pager.flow] before
             * calling asState() to ensure that transforms are executed incrementally on newly
             * loaded data.
             */
            .map { pagingData ->
                pagingData.insertSeparators { _, next ->
                    if (next?.value?.rem(10) == 0) SampleData.Separator(next.value.toString())
                    else null
                }
            }
            .asState()
}

@Sampled
fun PagerAsStateLoadErrorSample() {
    lateinit var pagingSourceFactory: () -> PagingSource<String, SampleData.Item>

    /** Initialize Pager in ViewModel */
    val pager =
        Pager(config = PagingConfig(pageSize = 40), pagingSourceFactory = pagingSourceFactory)
    val itemsFlow =
        pager.flow.asState { _ ->
            /** Retries the failed load without invalidating the PagingSource */
            pager.retry()
        }
}

@Sampled
@Composable
fun PagerLoadMoreDataSample() {
    val viewModel = viewModel { PagerViewModel() }

    // collect on Pager
    val itemsFlow = viewModel.pagerStateFlow.collectAsStateWithLifecycle()
    val items: List<SampleData.Item> = itemsFlow.value.items

    LazyColumn {
        item {
            /**
             * Load more items when users scroll near the top of loaded items.
             *
             * This example uses `viewModel` as the LaunchedEffect key to ensure that `prepend` is
             * only triggered once per scroll to the top. **Important** This assumes that `prepend`
             * loads enough new data to push this item composition out of the screen and LazyList
             * prefetch scope.
             */
            LaunchedEffect(viewModel) { viewModel.prepend() }
        }
        items(items) { item -> Text("Item: $item") }
        item {
            /**
             * Load more items when users scroll near the bottom of loaded items.
             *
             * This example uses `viewModel` as the LaunchedEffect key to ensure that `append` is
             * only triggered once per scroll to the bottom. **Important** This assumes that
             * `append` loads enough new data to push this item composition out of the screen and
             * LazyList prefetch scope.
             */
            LaunchedEffect(viewModel) { viewModel.append() }
        }
    }
}

/** No-op ViewModel base class to be used by sample code that doesn't show ViewModel impl */
private class PagerViewModel : ViewModel() {
    private lateinit var pagingSourceFactory: () -> PagingSource<String, SampleData.Item>

    val initialList = ItemSnapshotList(0, 0, items = listOf(1, 2, 3).map { SampleData.Item(it) })

    private val pager =
        Pager(config = PagingConfig(pageSize = 40), pagingSourceFactory = pagingSourceFactory)

    val pagerStateFlow: StateFlow<ItemSnapshotList<SampleData.Item>> =
        pager.flow.asState().stateIn(viewModelScope, SharingStarted.Lazily, initialList)

    /** Load more data at the end of the list */
    fun append() {
        pager.append()
    }

    /** Load more data at the start of the list */
    fun prepend() {
        pager.prepend()
    }
}

private sealed class SampleData {
    data class Item(val value: Int) : SampleData()

    data class Separator(val title: String) : SampleData()
}
