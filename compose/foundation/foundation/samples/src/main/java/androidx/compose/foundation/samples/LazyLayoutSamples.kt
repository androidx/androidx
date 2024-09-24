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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private val Items = (0..100).toList().map { it.toString() }

/**
 * In this example, the lazy layout simply lays out content based on its visibility on the screen.
 */
@Composable
@Preview
fun LazyLayoutDisplayVisibleItemsOnlySample() {
    BasicNonScrollableLazyLayout(
        modifier = Modifier.size(500.dp),
        orientation = Orientation.Vertical,
        items = Items
    ) { item, index ->
        Box(
            modifier =
                Modifier.width(100.dp)
                    .height(100.dp)
                    .background(color = if (index % 2 == 0) Color.Red else Color.Green)
        ) {
            Text(text = item)
        }
    }
}

/**
 * A simple Layout that will place items top to down, or right to left, without any scrolling or
 * offset until they fit the viewport.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> BasicNonScrollableLazyLayout(
    items: List<T>,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable (item: T, index: Int) -> Unit
) {
    val measurePolicy = remember(items, orientation) { basicMeasurePolicy(orientation, items.size) }
    val itemProvider = remember(items, content) { { BasicLazyLayoutItemProvider(items, content) } }
    LazyLayout(modifier = modifier, itemProvider = itemProvider, measurePolicy = measurePolicy)
}

@OptIn(ExperimentalFoundationApi::class)
private fun basicMeasurePolicy(
    orientation: Orientation,
    itemCount: Int
): LazyLayoutMeasureScope.(Constraints) -> MeasureResult = { constraints ->
    fun Placeable.mainAxisSize() = if (orientation == Orientation.Vertical) height else width
    fun Placeable.crossAxisSize() = if (orientation == Orientation.Vertical) width else height

    val viewportSize =
        if (orientation == Orientation.Vertical) constraints.maxHeight else constraints.maxWidth

    val childConstraints =
        Constraints(
            maxWidth =
                if (orientation == Orientation.Vertical) viewportSize else Constraints.Infinity,
            maxHeight =
                if (orientation == Orientation.Horizontal) viewportSize else Constraints.Infinity
        )

    var currentItemIndex = 0
    // saves placeables and their main axis position
    val placeables = mutableListOf<Pair<Placeable, Int>>()
    var crossAxisSize = 0
    var mainAxisSize = 0

    // measure items until we either fill in the space or run out of items.
    while (mainAxisSize < viewportSize && currentItemIndex < itemCount) {
        val itemPlaceables = measure(currentItemIndex, childConstraints)
        for (item in itemPlaceables) {
            // save placeable to be placed later.
            placeables.add(item to mainAxisSize)

            mainAxisSize += item.mainAxisSize() // item size contributes to main axis size
            // cross axis size will the size of tallest/widest item
            crossAxisSize = maxOf(crossAxisSize, item.crossAxisSize())
        }
        currentItemIndex++
    }

    val layoutWidth =
        if (orientation == Orientation.Horizontal) {
            minOf(mainAxisSize, viewportSize)
        } else {
            crossAxisSize
        }
    val layoutHeight =
        if (orientation == Orientation.Vertical) {
            minOf(mainAxisSize, viewportSize)
        } else {
            crossAxisSize
        }

    layout(layoutWidth, layoutHeight) {
        // since this is a linear list all items are placed on the same cross-axis position
        for ((placeable, position) in placeables) {
            if (orientation == Orientation.Vertical) {
                placeable.place(0, position)
            } else {
                placeable.place(position, 0)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class BasicLazyLayoutItemProvider<T>(
    private val items: List<T>,
    private val content: @Composable (item: T, index: Int) -> Unit
) : LazyLayoutItemProvider {
    override val itemCount: Int = items.size

    @Composable
    override fun Item(index: Int, key: Any) {
        content.invoke(items[index], index)
    }
}
