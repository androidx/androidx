/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.enforce
import androidx.ui.core.hasFixedHeight
import androidx.ui.core.hasFixedWidth
import androidx.ui.core.offset
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max

/**
 * A convenience composable that combines common layout composables for one child:
 * - padding: the padding to be applied to the child
 * - alignment: how to position the padded child if the [Container] is larger than the child
 * - constraints: additional Constraints to be enforced when measuring the Container
 * - width: the width to be used for the Container
 * - height: the height to be used for the Container
 *
 * When constraints, width and/or height are provided, these will be applied to the constraints
 * incoming from the [Container]'s parent, and might not always be satisfied if this is impossible.
 *
 * By default, the [Container] will try to be the size of its child (including padding), or as
 * small as possible within the incoming constraints if that is not possible. If expanded is
 * [true], the [Container] will be as large as possible for bounded incoming constraints.
 * If the padded child is smaller, regardless of the value of expanded, the provided alignment
 * will be used to position it. For unbounded incoming constraints, the [Container] will wrap
 * its child (same behavior as if expanded was [false]). Also, note that the measurement
 * information passed for the [Container] (constraints, width and height) will not be satisfied
 * if the incoming [Constraints] do not allow it.
 */
@Composable
fun Container(
    modifier: Modifier = Modifier.None,
    padding: EdgeInsets = EdgeInsets(0.dp),
    alignment: Alignment = Alignment.Center,
    expanded: Boolean = false,
    constraints: DpConstraints = DpConstraints(),
    width: Dp? = null,
    height: Dp? = null,
    children: @Composable() () -> Unit
) {
    Layout(children, modifier) { measurables, incomingConstraints, _ ->
        val containerConstraints = Constraints(constraints)
            .copy(
                width?.toIntPx() ?: constraints.minWidth.toIntPx(),
                width?.toIntPx() ?: constraints.maxWidth.toIntPx(),
                height?.toIntPx() ?: constraints.minHeight.toIntPx(),
                height?.toIntPx() ?: constraints.maxHeight.toIntPx()
            ).enforce(incomingConstraints)
        val totalHorizontal = padding.left.toIntPx() + padding.right.toIntPx()
        val totalVertical = padding.top.toIntPx() + padding.bottom.toIntPx()
        val childConstraints = containerConstraints
            .copy(minWidth = 0.ipx, minHeight = 0.ipx)
            .offset(-totalHorizontal, -totalVertical)
        var placeable: Placeable? = null
        val containerWidth = if ((containerConstraints.hasFixedWidth || expanded) &&
            containerConstraints.maxWidth.isFinite()
        ) {
            containerConstraints.maxWidth
        } else {
            placeable = measurables.firstOrNull()?.measure(childConstraints)
            max((placeable?.width ?: 0.ipx) + totalHorizontal, containerConstraints.minWidth)
        }
        val containerHeight = if ((containerConstraints.hasFixedHeight || expanded) &&
            containerConstraints.maxHeight.isFinite()
        ) {
            containerConstraints.maxHeight
        } else {
            if (placeable == null) {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
            }
            max((placeable?.height ?: 0.ipx) + totalVertical, containerConstraints.minHeight)
        }
        layout(containerWidth, containerHeight) {
            val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
            p?.let {
                val position = alignment.align(
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
