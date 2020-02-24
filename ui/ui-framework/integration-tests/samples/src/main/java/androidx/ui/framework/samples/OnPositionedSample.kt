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

package androidx.ui.framework.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.globalPosition
import androidx.ui.core.positionInRoot
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Sampled
@Composable
fun OnPositionedSample() {
    Column {
        Box(LayoutSize(20.dp), backgroundColor = Color.Green)
        Box(LayoutSize(20.dp), backgroundColor = Color.Blue)
        OnPositioned(onPositioned = { coordinates ->
            // This will be the size of the Column.
            coordinates.size
            // The position of the Column relative to the application window.
            coordinates.globalPosition
            // The position of the Column relative to the Compose root.
            coordinates.positionInRoot
            // These will be the alignment lines provided to the layout (empty here for Column).
            coordinates.providedAlignmentLines
            // This will a LayoutCoordinates instance corresponding to the parent of Column.
            coordinates.parentCoordinates
        })
    }
}

@Sampled
@Composable
fun OnChildPositionedSample() {
    Column {
        Box(LayoutSize(20.dp), backgroundColor = Color.Green)
        OnChildPositioned(onPositioned = { coordinates ->
            // This will be the size of the child SizedRectangle.
            coordinates.size
            // The position of the SizedRectangle relative to the application window.
            coordinates.globalPosition
            // The position of the SizedRectangle relative to the Compose root.
            coordinates.positionInRoot
            // These will be the alignment lines provided to the layout (empty for SizedRectangle)
            coordinates.providedAlignmentLines
            // This will a LayoutCoordinates instance corresponding to the Column.
            coordinates.parentCoordinates
        }) {
            Box(LayoutSize(20.dp), backgroundColor = Color.Blue)
        }
    }
}

/**
 * Simple Column implementation.
 */
@Composable
fun Column(children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(
                Constraints(minWidth = constraints.minWidth, maxWidth = constraints.maxWidth)
            )
        }
        val columnWidth = (placeables.maxBy { it.width.value }?.width ?: 0.ipx)
            .coerceAtLeast(constraints.minWidth)
        val columnHeight = placeables.sumBy { it.height.value }.ipx.coerceIn(
            constraints.minHeight,
            constraints.maxHeight
        )
        layout(columnWidth, columnHeight) {
            var top = 0.ipx
            placeables.forEach { placeable ->
                placeable.place(0.ipx, top)
                top += placeable.height
            }
        }
    }
}
