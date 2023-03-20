/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.TestPagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

val pager = Pager(
    config = PagingConfig(pageSize = 5, initialLoadSize = 15, enablePlaceholders = true),
    pagingSourceFactory = { TestPagingSource() }
).flow

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
public fun PagingWithHorizontalPager() {
    val lazyPagingItems = pager.collectAsLazyPagingItems()
    val pagerState = rememberPagerState { lazyPagingItems.itemCount }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageSize = PageSize.Fixed(200.dp),
        key = lazyPagingItems.itemKey { it }
    ) { index ->
        val item = lazyPagingItems[index]
        PagingItem(item = item)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun PagingWithVerticalPager() {
    val lazyPagingItems = pager.collectAsLazyPagingItems()
    val pagerState = rememberPagerState { lazyPagingItems.itemCount }

    VerticalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageSize = PageSize.Fixed(200.dp),
        key = lazyPagingItems.itemKey { it }
    ) { index ->
        val item = lazyPagingItems[index]
        PagingItem(item = item)
    }
}

@Sampled
@Composable
public fun PagingWithLazyGrid() {
    val lazyPagingItems = pager.collectAsLazyPagingItems()

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it },
            contentType = lazyPagingItems.itemContentType { "MyPagingItems" }
        ) { index ->
            val item = lazyPagingItems[index]
            PagingItem(item = item)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
public fun PagingWithLazyList() {
    val lazyPagingItems = pager.collectAsLazyPagingItems()

    LazyColumn {
        stickyHeader(
            key = "Header",
            contentType = "My Header",
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .background(Color.Red)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Header", fontSize = 32.sp)
            }
        }
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it },
            contentType = lazyPagingItems.itemContentType { "MyPagingItems" }
        ) { index ->
            val item = lazyPagingItems[index]
            PagingItem(item = item)
        }
    }
}

@Composable
private fun PagingItem(item: Int?) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .background(Color.Blue)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            Text(text = item.toString(), fontSize = 32.sp)
        } else {
            Text(text = "placeholder", fontSize = 32.sp)
        }
    }
}