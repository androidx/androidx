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

import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.isFinite
import androidx.ui.core.looseMin
import androidx.compose.Composable
import androidx.ui.core.Alignment

/**
 * A layout that takes a child and aligns it within itself, according to the alignment parameter.
 * The layout will be as large as possible for finite incoming constraints,
 * or wrap content otherwise.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleAlign
 *
 * For a composable that just does center alignment, see [Center].
 * For a composable that does alignment and tries to be the same size as its child, see [Wrap].
 * @see Center
 * @see Wrap
 */
@Composable
fun Align(alignment: Alignment, children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(constraints.looseMin())

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.maxWidth.isFinite()) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.maxHeight.isFinite()) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = alignment.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}

/**
 * A layout that takes a child and centers it within itself.
 * The layout will be as large as possible for finite incoming
 * constraints, or wrap content otherwise.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleCenter
 *
 * For a composable that supports other alignments than just center, see [Align].
 * For a composable that does alignment and tries to be the same size as its child, see [Wrap].
 * @see Align
 * @see Wrap
 */
@Composable
fun Center(children: @Composable() () -> Unit) {
    Align(alignment = Alignment.Center, children = children)
}

/**
 * Provides scope-dependent alignment options for children layouts where the alignment is handled
 * by the parent layout rather than the child itself. Different layout models allow different
 * [Gravity] options. For example, [Row] provides Top and Bottom, while [Column] provides
 * Start and End.
 * Unlike [Align], layout children with [Gravity] are aligned only after the size
 * of the parent is known, therefore not affecting the size of the parent in order to achieve
 * their own alignment.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleGravityInRow
 *
 * @sample androidx.ui.layout.samples.SimpleGravityInColumn
 */
object Gravity
