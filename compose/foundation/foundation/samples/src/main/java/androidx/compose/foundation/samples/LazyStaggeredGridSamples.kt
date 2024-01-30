/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Sampled
@Preview
@Composable
fun LazyVerticalStaggeredGridSample() {
    val itemsList = (0..5).toList()
    val itemsIndexedList = listOf("A", "B", "C")

    val itemModifier = Modifier.border(1.dp, Color.Blue).wrapContentSize()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(3)
    ) {
        items(itemsList) {
            Text("Item is $it", itemModifier.height(80.dp))
        }
        item {
            Text("Single item", itemModifier.height(100.dp))
        }
        itemsIndexed(itemsIndexedList) { index, item ->
            Text("Item at index $index is $item", itemModifier.height(60.dp))
        }
    }
}

@Sampled
@Preview
@Composable
fun LazyVerticalStaggeredGridSpanSample() {
    val sections = (0 until 25).toList().chunked(5)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp
    ) {
        sections.forEachIndexed { index, items ->
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "This is section $index",
                    Modifier.border(1.dp, Color.Gray).height(80.dp).wrapContentSize()
                )
            }
            items(
                items,
                // not required as it is the default
                span = { StaggeredGridItemSpan.SingleLane }
            ) {
                Text(
                    "Item $it",
                    Modifier.border(1.dp, Color.Blue).height(80.dp).wrapContentSize()
                )
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun LazyHorizontalStaggeredGridSample() {
    val itemsList = (0..5).toList()
    val itemsIndexedList = listOf("A", "B", "C")

    val itemModifier = Modifier.border(1.dp, Color.Blue).padding(16.dp).wrapContentSize()

    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(3)
    ) {
        items(itemsList) {
            Text("Item is $it", itemModifier)
        }
        item {
            Text("Single item", itemModifier)
        }
        itemsIndexed(itemsIndexedList) { index, item ->
            Text("Item at index $index is $item", itemModifier)
        }
    }
}

@Sampled
@Preview
@Composable
fun LazyHorizontalStaggeredGridSpanSample() {
    val sections = (0 until 25).toList().chunked(5)
    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalItemSpacing = 16.dp
    ) {
        sections.forEachIndexed { index, items ->
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "This is section $index",
                    Modifier.border(1.dp, Color.Gray).padding(16.dp).wrapContentSize()
                )
            }
            items(
                items,
                // not required as it is the default
                span = { StaggeredGridItemSpan.SingleLane }
            ) {
                Text(
                    "Item $it",
                    Modifier.border(1.dp, Color.Blue).width(80.dp).wrapContentSize()
                )
            }
        }
    }
}
