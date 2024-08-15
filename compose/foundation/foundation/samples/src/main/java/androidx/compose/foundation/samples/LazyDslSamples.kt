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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyLayoutAnimateScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Sampled
@Composable
fun LazyColumnSample() {
    val itemsList = (0..5).toList()
    val itemsIndexedList = listOf("A", "B", "C")

    LazyColumn {
        items(itemsList) { Text("Item is $it") }

        item { Text("Single item") }

        itemsIndexed(itemsIndexedList) { index, item -> Text("Item at index $index is $item") }
    }
}

@Sampled
@Composable
fun LazyRowSample() {
    val itemsList = (0..5).toList()
    val itemsIndexedList = listOf("A", "B", "C")

    LazyRow {
        items(itemsList) { Text("Item is $it") }

        item { Text("Single item") }

        itemsIndexed(itemsIndexedList) { index, item -> Text("Item at index $index is $item") }
    }
}

@Sampled
@Composable
fun StickyHeaderSample() {
    val sections = listOf("A", "B", "C", "D", "E", "F", "G")

    LazyColumn(reverseLayout = true, contentPadding = PaddingValues(6.dp)) {
        sections.forEach { section ->
            stickyHeader {
                Text(
                    "Section $section",
                    Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp)
                )
            }
            items(10) { Text("Item $it from the section $section") }
        }
    }
}

@Sampled
@Composable
fun StickyHeaderHeaderIndexSample() {
    /**
     * Checks if [index] is in the sticking position, that is, it's the first visible item and its
     * offset is equal to the content padding.
     */
    fun LazyListState.isSticking(index: Int): State<Boolean> {
        return derivedStateOf {
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisible?.index == index && firstVisible.offset == -layoutInfo.beforeContentPadding
        }
    }

    val sections = listOf("A", "B", "C", "D", "E", "F", "G")
    val state = rememberLazyListState()

    LazyColumn(state = state, reverseLayout = true, contentPadding = PaddingValues(6.dp)) {
        sections.forEach { section ->
            stickyHeader { headerIndex ->
                // change color when header is sticking
                val isSticking by remember(state) { state.isSticking(headerIndex) }
                Text(
                    "Section $section",
                    Modifier.fillMaxWidth()
                        .background(if (isSticking) Color.Red else Color.LightGray)
                        .padding(8.dp)
                )
            }
            items(10) { Text("Item $it from the section $section") }
        }
    }
}

@Sampled
@Composable
fun AnimateItemSample() {
    var list by remember { mutableStateOf(listOf("1", "2", "3")) }
    Column {
        Button(onClick = { list = list + "${list.count() + 1}" }) { Text("Add new item") }
        Button(onClick = { list = list.shuffled() }) { Text("Shuffle") }
        LazyColumn { items(list, key = { it }) { Text("Item $it", Modifier.animateItem()) } }
    }
}

@Sampled
@Composable
fun UsingListScrollPositionForSideEffectSample() {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect {
                // use the new index
            }
    }
}

@Sampled
@Composable
fun UsingListScrollPositionInCompositionSample() {
    val listState = rememberLazyListState()
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    if (!isAtTop) {
        ScrollToTopButton(listState)
    }
}

@Sampled
@Composable
fun UsingListLayoutInfoForSideEffectSample() {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .collect {
                // use the new items count
            }
    }
}

@Composable private fun ScrollToTopButton(@Suppress("UNUSED_PARAMETER") listState: LazyListState) {}

@Sampled
@Composable
fun CustomLazyListAnimateToItemScrollSample() {
    val itemsList = (0..100).toList()
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val animatedScrollScope = remember(state) { LazyLayoutAnimateScrollScope(state) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Button(
            onClick = {
                scope.launch {
                    state.scroll {
                        with(animatedScrollScope) {
                            snapToItem(40, 0) // teleport to item 40
                            val distance = calculateDistanceTo(50).toFloat()
                            var previousValue = 0f
                            androidx.compose.animation.core.animate(
                                0f,
                                distance,
                                animationSpec = tween(5_000)
                            ) { currentValue, _ ->
                                previousValue += scrollBy(currentValue - previousValue)
                            }
                        }
                    }
                }
            }
        ) {
            Text("Scroll To Item 50")
        }
        LazyRow(state = state) {
            items(itemsList) {
                Box(Modifier.padding(2.dp).background(Color.Red).size(45.dp)) {
                    Text(it.toString())
                }
            }
        }
    }
}
