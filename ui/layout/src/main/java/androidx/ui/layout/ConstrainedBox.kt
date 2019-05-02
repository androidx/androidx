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

import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.enforce
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Widget that enforces additional [Constraints] to its only child. The ConstrainedBox will
 * itself create a [Layout] which will take the same size that the child chooses.
 * If there is no child, the ConstrainedBox will size itself to the min constraints that would
 * have been passed to the child.
 * Note that, if the incoming constraints from the parent of the ConstrainedBox do not allow it,
 * the intrinsic ConstrainedBox constraints will not be satisfied - these will always be coerced
 * inside the incoming constraints.
 */
@Composable
fun ConstrainedBox(
    constraints: DpConstraints,
    @Children children: @Composable() () -> Unit
) {
    Layout(layoutBlock = { measurables, incomingConstraints ->
        val measurable = measurables.firstOrNull()
        val childConstraints = Constraints(constraints).enforce(incomingConstraints)
        val placeable = measurable?.measure(childConstraints)

        val layoutWidth = placeable?.width ?: childConstraints.minWidth
        val layoutHeight = placeable?.height ?: childConstraints.minHeight
        layout(layoutWidth, layoutHeight) {
            placeable?.place(IntPx.Zero, IntPx.Zero)
        }
    }, children = children)
}
