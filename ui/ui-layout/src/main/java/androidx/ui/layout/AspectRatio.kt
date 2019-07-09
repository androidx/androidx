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
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.ipx
import androidx.ui.core.isFinite
import androidx.ui.core.satisfiedBy

/**
 * Layout widget that attempts to size itself and a potential layout child to match a specified
 * aspect ratio. The widget will try to match one of the incoming constraints, in the following
 * order: maxWidth, maxHeight, minWidth, minHeight. The size in the other dimension will then be
 * computed according to the aspect ratio. Note that the provided aspectRatio will always
 * correspond to the width/height ratio.
 *
 * If a valid size that satisfies the constraints is found this way, the widget will size itself to
 * this size. If a child is present, this will be measured with tight constraints to match the size.
 * If no valid size is found, the aspect ratio will not be satisfied, and the widget will
 * wrap its child, which is measured with the same constraints. If there is no child, the widget
 * will size itself to the incoming min constraints.
 *
 * Example usage:
 *     AspectRatio(2f) {
 *         DrawRectangle(color = Color.Blue)
 *         Container(padding = EdgeInsets(20.dp)) {
 *             SizedRectangle(color = Color.Black)
 *         }
 *     }
 * The AspectRatio widget will make the Container have the width twice its height.
 */
@Composable
fun AspectRatio(
    aspectRatio: Float,
    @Children children: @Composable() () -> Unit
) {
    Layout(children) { measurables, constraints ->
        val size = listOf(
            IntPxSize(constraints.maxWidth, constraints.maxWidth / aspectRatio),
            IntPxSize(constraints.maxHeight * aspectRatio, constraints.maxHeight),
            IntPxSize(constraints.minWidth, constraints.minWidth / aspectRatio),
            IntPxSize(constraints.minHeight * aspectRatio, constraints.minHeight)
        ).find {
            constraints.satisfiedBy(it) &&
                    it.width != 0.ipx && it.height != 0.ipx &&
                    it.width.isFinite() && it.height.isFinite()
        }

        val measurable = measurables.firstOrNull()
        val childConstraints = if (size != null) {
            Constraints.tightConstraints(size.width, size.height)
        } else {
            constraints
        }
        val placeable = measurable?.measure(childConstraints)

        layout(
            size?.width ?: placeable?.width ?: constraints.minWidth,
            size?.height ?: placeable?.height ?: constraints.minHeight
        ) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}
