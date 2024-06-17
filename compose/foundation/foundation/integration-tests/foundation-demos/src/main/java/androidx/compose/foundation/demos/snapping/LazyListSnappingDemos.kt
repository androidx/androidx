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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy

val SnapPositionDemos = listOf(
    ComposableDemo("Center") { SnapPosition(SnapPosition.Center) },
    ComposableDemo("Start") { SnapPosition(SnapPosition.Start) },
    ComposableDemo("End") { SnapPosition(SnapPosition.End) },
)

val LazyListSnappingDemos = listOf(
    ComposableDemo("Single Item - Same Size Items") { SameItemSizeDemo() },
    ComposableDemo("Single Item - Different Size Item") { DifferentItemSizeDemo() },
    ComposableDemo("Single Item - Large Items") { LargeItemSizeDemo() },
    ComposableDemo("Single Item - List with Content padding") { DifferentContentPaddingDemo() },
    ComposableDemo("Multi Item - Decayed Snapping") { DecayedSnappingDemo() },
    ComposableDemo("Multi Item - View Port Based Offset") { ViewPortBasedSnappingDemo() },
    DemoCategory("Snap Position", SnapPositionDemos)
)

/**
 * Snapping happens to the next item and items have the same size
 */
@Composable
private fun SnapPosition(snapPosition: SnapPosition) {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberNextItemSnappingLayoutInfoProvider(lazyListState, snapPosition)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(
        lazyListState = lazyListState,
        flingBehavior = flingBehavior
    ) { position ->
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
                .background(Color.White)
                .drawWithContent {
                    drawContent()
                    drawAnchor(CenterAnchor)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = position.toString(), fontSize = 40.sp)
        }
    }
}

/**
 * Snapping happens to the next item and items have the same size
 */
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
@Composable
private fun ViewPortBasedSnappingDemo() {
    val lazyListState = rememberLazyListState()
    val layoutInfoProvider = rememberViewPortSnappingLayoutInfoProvider(lazyListState)
    val flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)

    SnappingDemoMainLayout(lazyListState = lazyListState, flingBehavior = flingBehavior) {
        DefaultSnapDemoItem(position = it)
    }
}

@Composable
private fun rememberNextItemSnappingLayoutInfoProvider(
    state: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center
): SnapLayoutInfoProvider {
    return remember(state, snapPosition) {
        val basedSnappingLayoutInfoProvider =
            SnapLayoutInfoProvider(lazyListState = state, snapPosition = snapPosition)
        object : SnapLayoutInfoProvider by basedSnappingLayoutInfoProvider {
            override fun calculateApproachOffset(
                velocity: Float,
                decayOffset: Float
            ): Float = 0.0f
        }
    }
}

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
