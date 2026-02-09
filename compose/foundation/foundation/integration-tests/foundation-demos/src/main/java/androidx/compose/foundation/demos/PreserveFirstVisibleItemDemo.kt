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

package androidx.compose.foundation.demos

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PreserveFirstVisibleItemDemo() {
    val coroutineScope = rememberCoroutineScope()
    // Shared state
    var items by remember { mutableStateOf((0..20).map { DemoItem(it, "Item $it") }) }
    val stateTrue = rememberLazyListState()
    val stateFalse = rememberLazyListState()
    var useKeys by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        Text("Scenarios", modifier = Modifier.padding(8.dp))
        LazyRow(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..100).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(50)
                        stateFalse.scrollToItem(50)
                        delay(1000)
                        items = items.filter { it.id % 5 == 0 }
                    }
                }) { Text("Bounds Clamp") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..100).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(50)
                        stateFalse.scrollToItem(50)
                        delay(1000)
                        items = items.filter { it.id % 2 == 1 }
                    }
                }) { Text("Key Fallback") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = emptyList()
                    }
                }) { Text("Clear List") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = emptyList()
                        delay(500)
                        items = (0..20).map { DemoItem(it, "Item $it") }
                    }
                }) { Text("Populate Empty") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (10..30).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(0)
                        stateFalse.scrollToItem(0)
                        delay(1000)
                        items = (0..30).map { DemoItem(it, "Item $it") }
                    }
                }) { Text("Add Before") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = items.drop(5)
                    }
                }) { Text("Remove Multiple Start") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(5)
                        stateFalse.scrollToItem(5)
                        delay(1000)
                        items = items.shuffled()
                    }
                }) { Text("Shuffle") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = items.filter { it.id % 2 == 0 }
                    }
                }) { Text("Filter Keep") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = items.filter { it.id % 2 == 1 }
                    }
                }) { Text("Filter Remove") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..50).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(20)
                        stateFalse.scrollToItem(20)
                        delay(1000)
                        items = items.drop(10)
                    }
                }) { Text("Remove Before") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = false
                        items = (10..30).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(0)
                        stateFalse.scrollToItem(0)
                        delay(1000)
                        items = (0..30).map { DemoItem(it, "Item $it") }
                    }
                }) { Text("No Key") }
            }
            item {
                Button(onClick = {
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        stateTrue.scrollToItem(20)
                        stateFalse.scrollToItem(20)
                        delay(1000)
                        val newItems = (-10..-1).map { DemoItem(it, "Item $it") } + items
                        items = newItems
                    }
                }) { Text("Scroll End Add") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            PreserveTestList(
                modifier = Modifier.weight(1f),
                title = "True",
                items = items,
                state = stateTrue,
                preserveFirstVisibleItem = true,
                useKeys = useKeys
            )
            PreserveTestList(
                modifier = Modifier.weight(1f),
                title = "False",
                items = items,
                state = stateFalse,
                preserveFirstVisibleItem = false,
                useKeys = useKeys
            )
        }
    }
}

data class DemoItem(
    val id: Int,
    val title: String,
)

@Composable
private fun PreserveTestList(
    modifier: Modifier = Modifier,
    title: String,
    items: List<DemoItem>,
    state: LazyListState,
    preserveFirstVisibleItem: Boolean,
    useKeys: Boolean
) {
    Column(modifier) {
        Text("$title: Index=${state.firstVisibleItemIndex} Offset=${state.firstVisibleItemScrollOffset}")
        LazyColumn(
            state = state,
            preserveFirstVisibleItem = preserveFirstVisibleItem,
            modifier = Modifier.fillMaxSize().background(Color.LightGray)
        ) {
            if (useKeys) {
                items(items, key = { it.id }) { item ->
                    DemoItemView(item)
                }
            } else {
                items(items) { item ->
                    DemoItemView(item)
                }
            }
        }
    }
}

@Composable
private fun DemoItemView(item: DemoItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(4.dp)
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = item.title, color = Color.Black)
    }
}