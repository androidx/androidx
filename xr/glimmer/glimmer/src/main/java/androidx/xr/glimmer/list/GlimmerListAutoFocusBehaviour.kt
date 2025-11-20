/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.compose.ui.focus.requestFocusForChildInRootBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt

/**
 * Auto focus controls how children receive focus as the list position changes. Conceptually, it's a
 * virtual line that moves along the list — from the topmost item to the bottommost — as the user
 * scrolls. The item located under this line is gained focus.
 *
 * In long lists, the focus line stays centered within the visible area, and only begins to shift
 * when the user scrolls close to the start or end of the list.
 *
 * We need to request focus after the layout pass, but the required focus area can only be
 * determined during the measure pass. This class calculates and stores that value during measuring,
 * and later, once all children are placed, it is invoked by [GlimmerListAutoFocusNode] to perform
 * the focus request.
 */
internal class GlimmerListAutoFocusBehaviour {

    private var pendingRequestFocus = false
    internal var properties: GlimmerListAutoFocusProperties? = null
        private set

    internal fun applyAutoFocusProperties(newProperties: GlimmerListAutoFocusProperties?) {
        properties = newProperties
        pendingRequestFocus = newProperties != null
    }

    internal fun onAfterLayout(node: DelegatableNode) {
        val properties = properties
        if (pendingRequestFocus && properties != null) {
            val focusLinePosition = getFocusLinePosition(properties)
            val coordinates = node.requireLayoutCoordinates()

            val localLeftTop = getFocusLeftTopOffset(focusLinePosition, properties.layoutProperties)
            val rootTopLeft = coordinates.localToRoot(localLeftTop)

            val rootLeft = rootTopLeft.x.fastRoundToInt()
            val rootTop = rootTopLeft.y.fastRoundToInt()
            val rootBottom = rootTop + getFocusHeight(properties.layoutProperties)
            val rootRight = rootLeft + getFocusWidth(properties.layoutProperties)

            node.requestFocusForChildInRootBounds(
                left = rootLeft,
                top = rootTop,
                right = rootRight,
                bottom = rootBottom,
            )

            pendingRequestFocus = false
        }
    }
}

private fun getFocusLinePosition(state: GlimmerListAutoFocusProperties): Float {
    // The FocusScroll doesn't include paddings, but the RectList API requires us to respect them.
    val focusLinePosition = state.layoutProperties.beforeContentPadding + state.focusScroll
    // Specifies the boundaries where the focus line can be.
    val start = state.layoutProperties.beforeContentPadding.toFloat()
    val end = start + state.layoutProperties.mainAxisAvailableSize
    // If the focus line lies exactly on the edge of an item, they are considered non-overlapping.
    // This breaks the behavior at the very beginning and end of the list. To avoid this, we shrink
    // the area where the focus line can exist by one pixel on both sides.
    return focusLinePosition.fastCoerceIn(start + 1, end - 1)
}

private fun getFocusLeftTopOffset(
    focusLine: Float,
    layoutProperties: ListLayoutProperties,
): Offset {
    return Offset(
        x = if (layoutProperties.isVertical) 0f else focusLine,
        y = if (layoutProperties.isVertical) focusLine else 0f,
    )
}

private fun getFocusWidth(layoutProperties: ListLayoutProperties): Int {
    return if (layoutProperties.isVertical) {
        layoutProperties.contentConstraints.maxWidth + layoutProperties.totalHorizontalPadding
    } else {
        0
    }
}

private fun getFocusHeight(layoutProperties: ListLayoutProperties): Int {
    return if (layoutProperties.isVertical) {
        0
    } else {
        layoutProperties.contentConstraints.maxHeight + layoutProperties.totalVerticalPadding
    }
}
