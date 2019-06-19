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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.ComplexLayout
import androidx.ui.core.IntPx
import androidx.ui.core.coerceIn
import androidx.ui.core.enforce
import androidx.ui.core.ipx

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
    ComplexLayout(children) {
        layout { measurables, incomingConstraints ->
            val measurable = measurables.firstOrNull()
            val childConstraints = Constraints(constraints).enforce(incomingConstraints)
            val placeable = measurable?.measure(childConstraints)

            val layoutWidth = placeable?.width ?: childConstraints.minWidth
            val layoutHeight = placeable?.height ?: childConstraints.minHeight
            layoutResult(layoutWidth, layoutHeight) {
                placeable?.place(IntPx.Zero, IntPx.Zero)
            }
        }

        minIntrinsicWidth { measurables, h ->
            val width = measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
            width.coerceIn(constraints.minWidth.toIntPx(), constraints.maxWidth.toIntPx())
        }

        maxIntrinsicWidth { measurables, h ->
            val width = measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
            width.coerceIn(constraints.minWidth.toIntPx(), constraints.maxWidth.toIntPx())
        }

        minIntrinsicHeight { measurables, w ->
            val height = measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
            height.coerceIn(constraints.minHeight.toIntPx(), constraints.maxHeight.toIntPx())
        }

        maxIntrinsicHeight { measurables, w ->
            val height = measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
            height.coerceIn(constraints.minHeight.toIntPx(), constraints.maxHeight.toIntPx())
        }
    }
}
