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

package androidx.paging.compose.demos.room3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

@Composable
public fun PagingWithRoom3() {
    val vm = viewModel<CatDatabaseViewModel>()
    val lazyPagingItems = vm.pager.collectAsLazyPagingItems()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { vm.insertNewCat() }) { Text(text = "Add new Cat", fontSize = 24.sp) }
        }
        LazyColumn {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.key },
                contentType = lazyPagingItems.itemContentType { "MyPagingItems" },
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null) {
                    Text(text = item.name, fontSize = 24.sp)
                }
            }
        }
    }
}
