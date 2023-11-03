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

package androidx.compose.foundation.demos.snapping

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy

val LazyListSnappingDemos = listOf(
    ComposableDemo("Single Item - Same Size Items") { SameItemSizeDemo() },
    ComposableDemo("Single Item - Different Size Item") { DifferentItemSizeDemo() },
    ComposableDemo("Single Item - Large Items") { LargeItemSizeDemo() },
    ComposableDemo("Single Item - List with Content padding") { DifferentContentPaddingDemo() },
    ComposableDemo("Multi Item - Decayed Snapping") { DecayedSnappingDemo() },
    ComposableDemo("Multi Item - View Port Based Offset") { ViewPortBasedSnappingDemo() },
)

/**
 * Snapping happens to the next item and items have the same size
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SameItemSizeDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberNextItemSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior
    ) {
        DefaultSnapDemoItem(it)
    }
}

/**
 * Snapping happens to the next item and items have the different sizes
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DifferentItemSizeDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberNextItemSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            width = PagesSizes[it],
            height = 500.dp,
            position = it
        )
    }
}

/**
 * Snapping happens to the next item and items are larger than the view port
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LargeItemSizeDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberNextItemSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        ResizableSnapDemoItem(
            width = 350.dp,
            height = 500.dp,
            position = it
        )
    }
}

/**
 * Snapping happens to the next item and list has content paddings
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DifferentContentPaddingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider =
        remember(lazyListState) { SnapLayoutInfoProvider(lazyListState) }
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior,
        contentPaddingValues = PaddingValues(start = 20.dp, end = 50.dp)
    ) {
        DefaultSnapDemoItem(position = it)
    }
}

/**
 * Snapping happens after a decay animation and items have the same size
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DecayedSnappingDemo() {
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

/**
 * Snapping happens to at max one view port item's worth distance.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewPortBasedSnappingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberViewPortSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberNextItemSnappingLayoutInfoProvider(
    state: LazyListState
): SnapLayoutInfoProvider {
    return remember(state) {
        val basedSnappingLayoutInfoProvider = SnapLayoutInfoProvider(lazyListState = state)
        object : SnapLayoutInfoProvider by basedSnappingLayoutInfoProvider {
            override fun calculateApproachOffset(initialVelocity: Float): Float {
                return 0f
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberViewPortSnappingLayoutInfoProvider(
    state: LazyListState
): SnapLayoutInfoProvider {
    val decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(state) {
        ViewPortBasedSnappingLayoutInfoProvider(
            SnapLayoutInfoProvider(lazyListState = state),
            decayAnimationSpec,
            {
                val visibleItemsSum = state.layoutInfo.visibleItemsInfo.fastSumBy { it.size }
                visibleItemsSum / state.layoutInfo.visibleItemsInfo.size.toFloat()
            }
        ) { state.layoutInfo.viewportSize.width.toFloat() }
    }
}

private val PagesSizes = (0..ItemNumber).toList().map { (50..500).random().dp }
