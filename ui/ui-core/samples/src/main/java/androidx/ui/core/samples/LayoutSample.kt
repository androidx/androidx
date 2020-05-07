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

package androidx.ui.core.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.tag
import androidx.ui.foundation.Box
import androidx.ui.unit.ipx

@Sampled
@Composable
fun LayoutWithProvidedIntrinsicsUsage(children: @Composable () -> Unit) {
    // We build a layout that will occupy twice as much space as its children,
    // and will position them to be bottom right aligned.
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            // The min intrinsic width of this layout will be twice the largest min intrinsic
            // width of a child. Note that we call minIntrinsicWidth with h / 2 for children,
            // since we should be double the size of the children.
            (measurables.map { it.minIntrinsicWidth(h / 2) }.maxBy { it.value } ?: 0.ipx) * 2
        },
        minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            (measurables.map { it.minIntrinsicHeight(w / 2) }.maxBy { it.value } ?: 0.ipx) * 2
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            (measurables.map { it.maxIntrinsicHeight(h / 2) }.maxBy { it.value } ?: 0.ipx) * 2
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            (measurables.map { it.maxIntrinsicHeight(w / 2) }.maxBy { it.value } ?: 0.ipx) * 2
        }
    ) { measurables, constraints, _ ->
        // measurables contains one element corresponding to each of our layout children.
        // constraints are the constraints that our parent is currently measuring us with.
        val childConstraints = Constraints(
            minWidth = constraints.minWidth / 2,
            minHeight = constraints.minHeight / 2,
            maxWidth = constraints.maxWidth / 2,
            maxHeight = constraints.maxHeight / 2
        )
        // We measure the children with half our constraints, to ensure we can be double
        // the size of the children.
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = (placeables.maxBy { it.width.value }?.width ?: 0.ipx) * 2
        val layoutHeight = (placeables.maxBy { it.height.value }?.height ?: 0.ipx) * 2
        // We call layout to set the size of the current layout and to provide the positioning
        // of the children. The children are placed relative to the current layout place.
        layout(layoutWidth, layoutHeight) {
            placeables.forEach { it.place(layoutWidth - it.width, layoutHeight - it.height) }
        }
    }
}

@Sampled
@Composable
fun LayoutUsage(children: @Composable () -> Unit) {
    // We build a layout that will occupy twice as much space as its children,
    // and will position them to be bottom right aligned.
    Layout(children) { measurables, constraints, _ ->
        // measurables contains one element corresponding to each of our layout children.
        // constraints are the constraints that our parent is currently measuring us with.
        val childConstraints = Constraints(
            minWidth = constraints.minWidth / 2,
            minHeight = constraints.minHeight / 2,
            maxWidth = constraints.maxWidth / 2,
            maxHeight = constraints.maxHeight / 2
        )
        // We measure the children with half our constraints, to ensure we can be double
        // the size of the children.
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = (placeables.maxBy { it.width.value }?.width ?: 0.ipx) * 2
        val layoutHeight = (placeables.maxBy { it.height.value }?.height ?: 0.ipx) * 2
        // We call layout to set the size of the current layout and to provide the positioning
        // of the children. The children are placed relative to the current layout place.
        layout(layoutWidth, layoutHeight) {
            placeables.forEach { it.place(layoutWidth - it.width, layoutHeight - it.height) }
        }
    }
}

@Sampled
@Composable
fun LayoutTagChildrenUsage(header: @Composable () -> Unit, footer: @Composable () -> Unit) {
    Layout({
        // Here the Containers are only needed to apply the modifiers. You could use the
        // modifier on header and footer directly if they are composables accepting modifiers.
        Box(Modifier.tag("header"), children = header)
        Box(Modifier.tag("footer"), children = footer)
    }) { measurables, constraints, _ ->
        val placeables = measurables.map { measurable ->
            when (measurable.tag) {
                // You should use appropriate constraints. Here we measure with dummy constraints.
                "header" -> measurable.measure(Constraints.fixed(100.ipx, 100.ipx))
                "footer" -> measurable.measure(constraints)
                else -> error("Unexpected tag")
            }
        }
        // Size should be derived from children measured sizes on placeables,
        // but this is simplified for the purposes of the example.
        layout(100.ipx, 100.ipx) {
            placeables.forEach { it.place(0.ipx, 0.ipx) }
        }
    }
}
