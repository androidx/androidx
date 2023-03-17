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

package androidx.compose.foundation.pager.lazy

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

internal class MeasuredPage(
    val index: Int,
    val size: Int,
    val placeables: List<Placeable>,
    val visualOffset: IntOffset,
    val key: Any,
    val orientation: Orientation,
    val horizontalAlignment: Alignment.Horizontal?,
    val verticalAlignment: Alignment.Vertical?,
    val layoutDirection: LayoutDirection,
    val reverseLayout: Boolean,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
) {

    val crossAxisSize: Int

    init {
        var maxCrossAxis = 0
        placeables.fastForEach {
            maxCrossAxis = maxOf(
                maxCrossAxis,
                if (orientation != Orientation.Vertical) it.height else it.width
            )
        }
        crossAxisSize = maxCrossAxis
    }

    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ): PositionedPage {
        val wrappers = mutableListOf<PagerPlaceableWrapper>()
        val mainAxisLayoutSize =
            if (orientation == Orientation.Vertical) layoutHeight else layoutWidth
        var mainAxisOffset = if (reverseLayout) {
            mainAxisLayoutSize - offset - size
        } else {
            offset
        }
        var index = if (reverseLayout) placeables.lastIndex else 0
        while (if (reverseLayout) index >= 0 else index < placeables.size) {
            val it = placeables[index]
            val addIndex = if (reverseLayout) 0 else wrappers.size
            val placeableOffset = if (orientation == Orientation.Vertical) {
                val x = requireNotNull(horizontalAlignment)
                    .align(it.width, layoutWidth, layoutDirection)
                IntOffset(x, mainAxisOffset)
            } else {
                val y = requireNotNull(verticalAlignment).align(it.height, layoutHeight)
                IntOffset(mainAxisOffset, y)
            }
            mainAxisOffset += if (orientation == Orientation.Vertical) it.height else it.width
            wrappers.add(
                addIndex,
                PagerPlaceableWrapper(placeableOffset, it, placeables[index].parentData)
            )
            if (reverseLayout) index-- else index++
        }
        return PositionedPage(
            offset = offset,
            index = this.index,
            key = key,
            orientation = orientation,
            wrappers = wrappers,
            visualOffset = visualOffset,
        )
    }
}

internal class PagerPlaceableWrapper(
    val offset: IntOffset,
    val placeable: Placeable,
    val parentData: Any?
)