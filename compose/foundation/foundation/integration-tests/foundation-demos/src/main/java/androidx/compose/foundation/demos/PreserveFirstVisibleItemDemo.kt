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
import androidx.compose.foundation.lazy.itemsIndexed
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
        LazyRow(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(onClick = {
                    /** Shuffles all elements starting from the top. */
                    /** In the default behavior, the first item (Item 0) remains at the top during the shuffle.
                     * However, when set to 'false', the scroll offset stays at the very top regardless of the shuffle result.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(0)
                        stateFalse.scrollToItem(0)
                        delay(250)
                        items = items.shuffled()
                    }
                }) { Text("Shuffle") }
            }
            item {
                Button(onClick = {
                    /** Moves to the 50th index, maintains existing keys, and removes preceding content. */
                    /** In the default behavior, the view shifts up to stay aligned with the 50th index.
                     * However, when set to 'false', it maintains the scroll offset from the moment it was
                     * aligned to the 50th index, causing it to move to the very bottom.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..100).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(50)
                        stateFalse.scrollToItem(50)
                        delay(1000)
                        items = items.filter { it.id % 5 == 0 }
                    }
                }) { Text("Filter + Maintain Key") }
            }
            item {
                Button(onClick = {
                    /** Moves to the 50th index, then removes the preceding items including the current key. */
                    /** In the default state, because the reference point for maintaining the scroll offset
                     * is lost, it behaves the same as the 'false' state and moves to the very bottom of the scroll area.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..100).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(50)
                        stateFalse.scrollToItem(50)
                        delay(1000)
                        items = items.filter { it.id % 2 == 1 }
                    }
                }) { Text("Filter + Remove Key") }
            }
            item {
                Button(onClick = {
                    /** Clears the list. */
                    /** Both states are filtered correctly due to the boundary coercion logic of the list elements. */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = emptyList()
                    }
                }) { Text("Clear List") }
            }
            item {
                Button(onClick = {
                    /** Clears the list and then repopulates it. */
                    /** In the default state, since there is no reference point to maintain the scroll offset,
                     * it stays at the very top of the scroll area, identical to the 'false' state.
                     */
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
                    /** Moves the scroll offset to Item 10, then adds 10 items to the front of the list. */
                    /** In the default state, Item 10 remains anchored at the top position.
                     * In the 'false' state, the newly added Item 0 becomes visible instead.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (10..30).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(0)
                        stateFalse.scrollToItem(0)
                        delay(1000)
                        items = (0..30).map { DemoItem(it, "Item $it") }
                    }
                }) { Text("Add Before") }
            }
            item {
                Button(onClick = {
                    /** Moves the scroll offset to Item 10, then removes 5 items from the front of the list. */
                    /** In the default state, Item 10 remains anchored at the top even after the preceding items are removed.
                     * In the 'false' state, the list scrolls up by the number of dropped items.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(10)
                        stateFalse.scrollToItem(10)
                        delay(1000)
                        items = items.drop(5)
                    }
                }) { Text("Remove Multiple Start") }
            }
            item {
                Button(onClick = {
                    /** Moves the scroll offset to Item 10, then adds 10 items to the front of the list. */
                    /** In the default state, because no keys are being injected to maintain the scroll offset,
                     * it stays at the very top, identical to the 'false' state.
                     */
                    coroutineScope.launch {
                        useKeys = false
                        items = (10..30).map { DemoItem(it, "Item $it") }
                        delay(100)
                        stateTrue.scrollToItem(0)
                        stateFalse.scrollToItem(0)
                        delay(1000)
                        items = (0..30).map { DemoItem(it, "Item $it") }
                    }
                }) { Text("No Key") }
            }
            item {
                Button(onClick = {
                    /** Scrolls to the very bottom to display the last element, then adds 10 items to the top. */
                    /** In the default state, the scroll position remains unchanged.
                     * In the 'false' state, the list scrolls down by the number of added items.
                     */
                    coroutineScope.launch {
                        useKeys = true
                        items = (0..20).map { DemoItem(it, "Item $it") }
                        delay(100)
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
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isFirst = index == 0
                    Column(Modifier.animateItem()) {
                        if (isFirst) {
                            Text("This is First Row")
                        }
                        DemoItemView(item)
                    }
                }
            } else {
                items(items) { item ->
                    Box(Modifier.animateItem()) {
                        DemoItemView(item)
                    }
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