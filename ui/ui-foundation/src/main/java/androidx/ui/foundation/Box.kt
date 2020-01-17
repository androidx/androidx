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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.offset
import androidx.ui.layout.EdgeInsets
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max

/**
 * A convenience composable that combines common layout and draw logic.
 *
 * In order to define the size of the [Box], the [androidx.ui.layout.LayoutWidth],
 * [androidx.ui.layout.LayoutHeight] and [androidx.ui.layout.LayoutSize] modifiers can be used.
 * The [Box] will try to be only as small as its content. However, if it is constrained
 * otherwise, [Box] will allow its content to be smaller and will position the content inside,
 * according to [gravity].
 *
 * The specified [padding] will be applied inside the [Box]. In order to apply padding outside
 * the [Box], the [androidx.ui.layout.LayoutPadding] modifier should be used.
 *
 * @param modifier The modifier to be applied to the Box
 * @param padding The padding to be applied inside Box, along its edges
 * @param gravity The gravity of the content inside Box
 */
@Composable
fun Box(
    modifier: Modifier = Modifier.None,
    padding: ContentPadding = ContentPadding(0.dp),
    gravity: ContentGravity = ContentGravity.TopLeft,
    children: @Composable() () -> Unit
) {
    Layout(children, modifier) { measurables, constraints ->
        val totalHorizontal = padding.left.toIntPx() + padding.right.toIntPx()
        val totalVertical = padding.top.toIntPx() + padding.bottom.toIntPx()

        val childConstraints = constraints
            .offset(-totalHorizontal, -totalVertical)
            .copy(minWidth = 0.ipx, minHeight = 0.ipx)

        val placeables = measurables.map { it.measure(childConstraints) }
        var containerWidth = constraints.minWidth
        var containerHeight = constraints.minHeight
        placeables.forEach {
            containerWidth = max(containerWidth, it.width)
            containerHeight = max(containerHeight, it.height)
        }

        layout(containerWidth, containerHeight) {
            placeables.forEach {
                val position = gravity.align(
                    IntPxSize(
                        containerWidth - it.width - totalHorizontal,
                        containerHeight - it.height - totalVertical
                    )
                )
                it.place(
                    padding.left.toIntPx() + position.x,
                    padding.top.toIntPx() + position.y
                )
            }
        }
    }
}

// TODO(popam/148015674): move EdgeInsets here and rename the API to ContentPadding
typealias ContentPadding = EdgeInsets
// TODO(popam/148014745): add a Gravity class consistent with cross axis alignment for Row/Column
typealias ContentGravity = Alignment