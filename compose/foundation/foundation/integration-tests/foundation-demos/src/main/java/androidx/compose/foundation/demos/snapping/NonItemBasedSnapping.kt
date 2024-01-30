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

package androidx.compose.foundation.demos.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A provider that doesn't use the concept of items for snapping.
 */
@OptIn(ExperimentalFoundationApi::class)
class NonItemBasedSnappingLayoutInfoProvider(
    private val currentOffset: Int,
    layoutSize: Int,
    thumbSize: Int
) : SnapLayoutInfoProvider {

    // start, middle, end of the layout
    private val offsetList = listOf(0, layoutSize / 2 - thumbSize / 2, (layoutSize - thumbSize))

    // do not approach, our snapping positions are discrete.
    override fun calculateApproachOffset(initialVelocity: Float): Float = 0f

    override fun calculateSnappingOffset(currentVelocity: Float): Float {
        val targetOffset = if (currentVelocity == 0.0f) {
            // snap to closest offset
            var closestOffset = 0
            var prevMinAbs = Int.MAX_VALUE
            offsetList.forEach {
                val absDistance = abs(currentOffset - it)
                if (absDistance < prevMinAbs) {
                    prevMinAbs = absDistance
                    closestOffset = it
                }
            }
            (closestOffset).toFloat()
        } else if (currentVelocity > 0) {
            // snap to the next offset
            val offset = offsetList.firstOrNull { it > currentOffset }
            (offset ?: 0).toFloat() // if offset is found, move there, if not, don't move
        } else {
            // snap to the previous offset
            val offset = offsetList.reversed().firstOrNull { it < currentOffset }
            (offset ?: 0).toFloat() // if offset is found, move there, if not, don't move
        }
        return targetOffset - currentOffset // distance that needs to be consumed to reach target
    }
}

private val ThumbSize = 60.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NonItemBasedLayout() {
    var thumbOffset by remember { mutableStateOf(IntOffset.Zero) }
    var layoutSize by remember { mutableIntStateOf(0) }

    val thumbSize = with(LocalDensity.current) { ThumbSize.roundToPx() }
    val maxPosition = with(LocalDensity.current) {
        layoutSize - ThumbSize.roundToPx()
    }

    val snapLayoutInfoProvider = remember(thumbOffset, layoutSize, thumbSize) {
        NonItemBasedSnappingLayoutInfoProvider(thumbOffset.x, layoutSize, thumbSize)
    }

    val fling = rememberSnapFlingBehavior(snapLayoutInfoProvider = snapLayoutInfoProvider)
    val scrollableState = rememberScrollableState {
        val previousPosition = thumbOffset.x
        val newPosition = (previousPosition + it).coerceIn(0.0f, maxPosition.toFloat()).toInt()
        thumbOffset = thumbOffset.copy(x = newPosition)
        it // need to return correct consumption
    }
    Box(
        modifier = Modifier
            .requiredHeight(ThumbSize)
            .fillMaxWidth()
            .background(Color.LightGray)
            .scrollable(
                scrollableState,
                orientation = Orientation.Horizontal,
                flingBehavior = fling
            )
            .onSizeChanged {
                layoutSize = it.width
            }
    ) {
        Box(modifier = Modifier
            .offset { thumbOffset }
            .requiredSize(ThumbSize)
            .background(Color.Red))
    }
}
