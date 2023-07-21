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

package androidx.paging.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow

private val DATA = (0..60).toList().map { "[Item $it is from backend]" }

@Sampled
@Composable
fun PagingBackendSample() {
    val myBackend = remember { TestBackend(DATA) }

    val pager = remember {
        Pager(
            PagingConfig(
                pageSize = myBackend.DataBatchSize,
                enablePlaceholders = true,
                maxSize = 200
            )
        ) { myBackend.getAllData() }
    }

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn {
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }

        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            Text("Index=$index: $item", fontSize = 20.sp)
        }

        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Sampled
@Composable
fun ItemsDemo(flow: Flow<PagingData<String>>) {
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    LazyColumn {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey()
        ) { index ->
            val item = lazyPagingItems[index]
            Text("Item is $item")
        }
    }
}

@Sampled
@Composable
fun ItemsIndexedDemo(flow: Flow<PagingData<String>>) {
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    LazyColumn {
        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            Text("Item at index $index is $item")
        }
    }
}