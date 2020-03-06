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
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.enforce
import androidx.ui.unit.ipx

/**
 * Layout composable that forces its child to be as wide as its min intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SameWidthBoxes
 *
 * The sample builds a layout containing three [Container] having the same width as the widest one.
 *
 * Here [MinIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose minimum intrinsic width will correspond to the preferred width of the largest
 * [Container]. Then [MinIntrinsicWidth] will measure the [Column] with tight width, the same
 * as the premeasured minimum intrinsic width.
 */
@Composable
fun MinIntrinsicWidth(children: @Composable() () -> Unit) {
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        },
        minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    ) { measurables, constraints, _ ->
        val measurable = measurables.firstOrNull()
        val width = measurable?.minIntrinsicWidth(constraints.maxHeight) ?: 0.ipx
        val placeable = measurable?.measure(
            Constraints.fixedWidth(width).enforce(constraints)
        )
        layout(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as tall as its min intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.MatchParentDividerForText
 *
 * The sample builds a layout containing two pieces of text separated by a divider, where the
 * divider is sized according to the height of the longest text.
 *
 * Here [MinIntrinsicHeight] is adding a speculative height measurement pass for the [Row],
 * whose minimum intrinsic height will correspond to the height of the largest [Text]. Then
 * [MinIntrinsicHeight] will measure the [Row] with tight height, the same as the premeasured
 * minimum intrinsic height, which due to [LayoutHeight.Fill] will force the [Text]s and
 * the divider to use the same height.
 */
@Composable
fun MinIntrinsicHeight(children: @Composable() () -> Unit) {
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        },
        minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        }
    ) { measurables, constraints, _ ->
        val measurable = measurables.firstOrNull()
        val height = measurable?.minIntrinsicHeight(constraints.maxWidth) ?: 0.ipx
        val placeable = measurable?.measure(
            Constraints.fixedHeight(height).enforce(constraints)
        )
        layout(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as wide as its max intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SameWidthTextBoxes
 *
 * The sample builds a layout containing three [Text] boxes having the same width as the widest one.
 *
 * Here [MaxIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose maximum intrinsic width will correspond to the preferred width of the largest
 * [Container]. Then [MaxIntrinsicWidth] will measure the [Column] with tight width, the same
 * as the premeasured maximum intrinsic width, which due to [LayoutWidth.Fill] will force
 * the [Container]s to use the same width.
 * The sample is a layout containing three composables having the same width as the widest one.
 */
@Composable
fun MaxIntrinsicWidth(children: @Composable() () -> Unit) {
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        },
        minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    ) { measurables, constraints, _ ->
        val measurable = measurables.firstOrNull()
        val width = measurable?.maxIntrinsicWidth(constraints.maxHeight) ?: 0.ipx
        val placeable = measurable?.measure(
            Constraints.fixedWidth(width).enforce(constraints)
        )
        layout(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as tall as its max intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.MatchParentDividerForAspectRatio
 *
 * The sample builds a layout containing two [LayoutAspectRatio]s separated by a divider, where the
 * divider is sized according to the height of the taller [LayoutAspectRatio].
 *
 * Here [MaxIntrinsicHeight] is adding a speculative height measurement pass for the [Row],
 * whose maximum intrinsic height will correspond to the height of the taller [AspectRatio]. Then
 * [MaxIntrinsicHeight] will measure the [Row] with tight height, the same as the premeasured
 * maximum intrinsic height, which due to [LayoutHeight.Fill] will force the [AspectRatio]s
 * and the divider to use the same height.
 */
@Composable
fun MaxIntrinsicHeight(children: @Composable() () -> Unit) {
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        },
        minIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h, _ ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w, _ ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    ) { measurables, constraints, _ ->
        val measurable = measurables.firstOrNull()
        val height = measurable?.maxIntrinsicHeight(constraints.maxWidth) ?: 0.ipx
        val placeable = measurable?.measure(
            Constraints.fixedHeight(height).enforce(constraints)
        )
        layout(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}
