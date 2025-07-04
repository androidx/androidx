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
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

@Sampled
@Preview
@Composable
fun LazyLayoutPrefetchStateSample() {
    val items = remember { (0..100).toList().map { it.toString() } }
    var currentHandle = remember<LazyLayoutPrefetchState.PrefetchHandle?> { null }
    val prefetchState = remember { LazyLayoutPrefetchState() }
    // Create an item provider
    val itemProvider = remember {
        {
            object : LazyLayoutItemProvider {
                override val itemCount: Int
                    get() = 100

                @Composable
                override fun Item(index: Int, key: Any) {
                    Box(
                        modifier =
                            Modifier.width(100.dp)
                                .height(100.dp)
                                .background(color = if (index % 2 == 0) Color.Red else Color.Green)
                    ) {
                        Text(text = items[index])
                    }
                }
            }
        }
    }

    Column {
        Button(onClick = { currentHandle = prefetchState.schedulePrecomposition(10) }) {
            Text(text = "Prefetch Item 10")
        }
        Button(onClick = { currentHandle?.cancel() }) { Text(text = "Cancel Prefetch") }
        LazyLayout(modifier = Modifier.size(500.dp), itemProvider = itemProvider) { constraints ->
            // plug the measure policy, this is how we create and layout items.
            val placeablesCache = mutableListOf<Pair<Placeable, Int>>()
            fun Placeable.mainAxisSize() = width
            fun Placeable.crossAxisSize() = height

            val childConstraints =
                Constraints(maxWidth = Constraints.Infinity, maxHeight = constraints.maxHeight)

            var currentItemIndex = 0
            var crossAxisSize = 0
            var mainAxisSize = 0

            // measure items until we either fill in the space or run out of items.
            while (mainAxisSize < constraints.maxWidth && currentItemIndex < items.size) {
                val itemPlaceables = compose(currentItemIndex).map { it.measure(childConstraints) }
                for (item in itemPlaceables) {
                    // save placeable to be placed later.
                    placeablesCache.add(item to mainAxisSize)

                    mainAxisSize += item.mainAxisSize() // item size contributes to main axis size
                    // cross axis size will the size of tallest/widest item
                    crossAxisSize = maxOf(crossAxisSize, item.crossAxisSize())
                }
                currentItemIndex++
            }

            val layoutWidth = minOf(mainAxisSize, constraints.maxHeight)
            val layoutHeight = crossAxisSize

            layout(layoutWidth, layoutHeight) {
                // since this is a linear list all items are placed on the same cross-axis position
                for ((placeable, position) in placeablesCache) {
                    placeable.place(position, 0)
                }
            }
        }
    }
}

/** A Lazy Layout that will place items right to left with scrolling support. */
@Sampled
@Preview
@Composable
fun LazyLayoutSample() {
    val items = remember { (0..100).toList().map { it.toString() } }

    // Create an item provider
    val itemProvider = remember {
        {
            object : LazyLayoutItemProvider {
                override val itemCount: Int
                    get() = 100

                @Composable
                override fun Item(index: Int, key: Any) {
                    Box(
                        modifier =
                            Modifier.width(100.dp)
                                .height(100.dp)
                                .background(color = if (index % 2 == 0) Color.Red else Color.Green)
                    ) {
                        Text(text = items[index])
                    }
                }
            }
        }
    }

    LazyLayout(modifier = Modifier.size(500.dp), itemProvider = itemProvider) { constraints ->
        // plug the measure policy, this is how we create and layout items.
        val placeablesCache = mutableListOf<Pair<Placeable, Int>>()
        fun Placeable.mainAxisSize() = width
        fun Placeable.crossAxisSize() = height

        val childConstraints =
            Constraints(maxWidth = Constraints.Infinity, maxHeight = constraints.maxHeight)

        var currentItemIndex = 0
        var crossAxisSize = 0
        var mainAxisSize = 0

        // measure items until we either fill in the space or run out of items.
        while (mainAxisSize < constraints.maxWidth && currentItemIndex < items.size) {
            val itemPlaceables = compose(currentItemIndex).map { it.measure(childConstraints) }
            for (item in itemPlaceables) {
                // save placeable to be placed later.
                placeablesCache.add(item to mainAxisSize)

                mainAxisSize += item.mainAxisSize() // item size contributes to main axis size
                // cross axis size will the size of tallest/widest item
                crossAxisSize = maxOf(crossAxisSize, item.crossAxisSize())
            }
            currentItemIndex++
        }

        val layoutWidth = minOf(mainAxisSize, constraints.maxHeight)
        val layoutHeight = crossAxisSize

        layout(layoutWidth, layoutHeight) {
            // since this is a linear list all items are placed on the same cross-axis position
            for ((placeable, position) in placeablesCache) {
                placeable.place(position, 0)
            }
        }
    }
}

@Composable
@Sampled
fun LazyLayoutItemProviderSample() {
    class CustomItemProvider(val data: List<String>) : LazyLayoutItemProvider {
        override val itemCount: Int
            get() = data.size

        @Composable
        override fun Item(index: Int, key: Any) {
            Box(
                modifier =
                    Modifier.width(100.dp)
                        .height(100.dp)
                        .background(color = if (index % 2 == 0) Color.Red else Color.Green)
            ) {
                Text(text = data[index])
            }
        }
    }

    val items = (0..100).toList().map { it.toString() }

    val itemProvider = remember(items) { CustomItemProvider(items) }
}

/** A simple Layout that will place items right to left with scrolling support. */
@Sampled
@Preview
@Composable
fun LazyLayoutScrollableSample() {
    val items = remember { (0..100).toList().map { it.toString() } }

    /** Saves the deltas from the scroll gesture. */
    var scrollAmount by remember { mutableFloatStateOf(0f) }

    /**
     * Lazy Layout measurement starts at a known position identified by the first item's position.
     */
    var firstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var firstVisibleItemOffset by remember { mutableIntStateOf(0) }

    // A scrollable state is needed for scroll support
    val scrollableState = rememberScrollableState { delta ->
        scrollAmount += delta
        delta // assume we consumed everything.
    }

    // Create an item provider
    val itemProvider = {
        object : LazyLayoutItemProvider {
            override val itemCount: Int
                get() = items.size

            @Composable
            override fun Item(index: Int, key: Any) {
                Box(
                    modifier =
                        Modifier.width(100.dp)
                            .height(100.dp)
                            .background(color = if (index % 2 == 0) Color.Red else Color.Green)
                ) {
                    Text(text = items[index])
                }
            }
        }
    }

    fun LazyLayoutMeasureScope.createItem(
        index: Int,
        constraints: Constraints,
        placeablesCache: IntObjectMap<Placeable>,
    ): Placeable {

        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable == null) {
            val measurables = compose(index)
            require(measurables.size == 1) { "Only one composable item emission is supported." }
            measurables[0].measure(constraints)
        } else {
            cachedPlaceable
        }
    }

    fun LazyLayoutMeasureScope.measureLayout(
        scrollAmount: Float,
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Int,
        itemCount: Int,
        placeablesCache: MutableIntObjectMap<Placeable>,
        containerConstraints: Constraints,
        updatePositions: (Int, Int) -> Unit,
    ): MeasureResult {
        /** 1) Resolve layout information and constraints. */
        val viewportSize = containerConstraints.maxWidth
        // children are only restricted on the cross axis size.
        val childConstraints =
            Constraints(maxWidth = Constraints.Infinity, maxHeight = viewportSize)

        /** 2) Initialize data holders for the pass. */
        // All items that will be placed in this layout in the layout pass.
        val placeables = mutableListOf<Pair<Placeable, Int>>()
        // The anchor points, we start from a known position, the position of the first item.
        var currentFirstVisibleItemIndex = firstVisibleItemIndex
        var currentFirstVisibleItemOffset = firstVisibleItemOffset
        // represents the real amount of scroll we applied as a result of this measure pass.
        val scrollDelta = scrollAmount.fastRoundToInt()
        // The amount of space available to items.
        val maxOffset = containerConstraints.maxWidth
        // tallest item from the ones we've created in this layout.This will determined the cross
        // axis
        // size of this layout
        var maxCrossAxis = 0

        /** 3) Apply Scroll */
        // applying the whole requested scroll offset.
        currentFirstVisibleItemOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible we reset. Imagine we've
        // reached
        // the bounds at the start of the layout and we tried to scroll back.
        if (currentFirstVisibleItemIndex == 0 && currentFirstVisibleItemOffset < 0) {
            currentFirstVisibleItemOffset = 0
        }

        /** 4) Consider we scrolled back */
        while (currentFirstVisibleItemOffset < 0 && currentFirstVisibleItemIndex > 0) {
            val previous = currentFirstVisibleItemIndex - 1
            val measuredItem = createItem(previous, childConstraints, placeablesCache)
            placeables.add(0, measuredItem to currentFirstVisibleItemOffset)
            maxCrossAxis = maxOf(maxCrossAxis, measuredItem.height)
            currentFirstVisibleItemOffset += measuredItem.width
            currentFirstVisibleItemIndex = previous
        }

        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstVisibleItemOffset < 0) {
            val notConsumedScrollDelta = -currentFirstVisibleItemOffset
            currentFirstVisibleItemOffset = 0
        }

        /** 5) Compose forward. */
        var index = currentFirstVisibleItemIndex
        var currentMainAxisOffset = -currentFirstVisibleItemOffset

        // first we need to skip items we already composed while composing backward
        var indexInVisibleItems = 0
        while (indexInVisibleItems < placeables.size) {
            if (currentMainAxisOffset >= maxOffset) {
                // this item is out of the bounds and will not be visible.
                placeables.removeAt(indexInVisibleItems)
            } else {
                index++
                currentMainAxisOffset += placeables[indexInVisibleItems].first.width
                indexInVisibleItems++
            }
        }

        // then composing visible items forward until we fill the whole viewport.
        while (
            index < itemCount &&
                (currentMainAxisOffset < maxOffset ||
                    currentMainAxisOffset <= 0 ||
                    placeables.isEmpty())
        ) {
            val measuredItem = createItem(index, childConstraints, placeablesCache)
            val measuredItemPosition = currentMainAxisOffset

            currentMainAxisOffset += measuredItem.width

            if (currentMainAxisOffset <= 0 && index != itemCount - 1) {
                // this item is offscreen and will not be visible. advance firstVisibleItemIndex
                currentFirstVisibleItemIndex = index + 1
                currentFirstVisibleItemOffset -= measuredItem.width
            } else {
                maxCrossAxis = maxOf(maxCrossAxis, measuredItem.height)
                placeables.add(measuredItem to measuredItemPosition)
            }
            index++
        }

        val layoutWidth = containerConstraints.constrainWidth(currentMainAxisOffset)
        val layoutHeight = containerConstraints.constrainHeight(maxCrossAxis)

        /** 7) Update state information. */
        updatePositions(currentFirstVisibleItemIndex, currentFirstVisibleItemOffset)

        /** 8) Perform layout. */
        return layout(layoutWidth, layoutHeight) {
            // since this is a linear list all items are placed on the same cross-axis position
            for ((placeable, position) in placeables) {
                placeable.place(position, 0)
            }
        }
    }

    LazyLayout(
        modifier = Modifier.size(500.dp).scrollable(scrollableState, Orientation.Horizontal),
        itemProvider = itemProvider,
    ) { constraints ->
        // plug the measure policy, this is how we create and layout items.
        val placeablesCache = mutableIntObjectMapOf<Placeable>()
        measureLayout(
            scrollAmount, // will trigger a re-measure when it changes.
            firstVisibleItemIndex,
            firstVisibleItemOffset,
            items.size,
            placeablesCache,
            constraints,
        ) { newFirstVisibleItemIndex, newFirstVisibleItemOffset ->
            // update the information about the anchor item.
            firstVisibleItemIndex = newFirstVisibleItemIndex
            firstVisibleItemOffset = newFirstVisibleItemOffset

            // resets the scrolling state
            scrollAmount = 0f
        }
    }
}
