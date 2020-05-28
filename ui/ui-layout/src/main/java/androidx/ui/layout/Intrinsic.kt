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
import androidx.compose.Stable
import androidx.ui.core.Constraints
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureScope
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.enforce
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.ipx

/**
 * Declare the preferred width of the content to be the same as the min or max intrinsic width of
 * the content. The incoming measurement [Constraints] may override this value, forcing the content
 * to be either smaller or larger.
 *
 * See [preferredHeight] for options of sizing to intrinsic height.
 * Also see [preferredWidth] and [preferredWidthIn] for other options to set the preferred width.
 *
 * Example usage for min intrinsic:
 * @sample androidx.ui.layout.samples.SameWidthBoxes
 *
 * Example usage for max intrinsic:
 * @sample androidx.ui.layout.samples.SameWidthTextBoxes
 */
@ExperimentalLayout
@Stable
fun Modifier.preferredWidth(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this + PreferredMinIntrinsicWidthModifier
    IntrinsicSize.Max -> this + PreferredMaxIntrinsicWidthModifier
}

/**
 * Declare the preferred height of the content to be the same as the min or max intrinsic height of
 * the content. The incoming measurement [Constraints] may override this value, forcing the content
 * to be either smaller or larger.
 *
 * See [preferredWidth] for other options of sizing to intrinsic width.
 * Also see [preferredHeight] and [preferredHeightIn] for other options to set the preferred height.
 *
 * Example usage for min intrinsic:
 * @sample androidx.ui.layout.samples.MatchParentDividerForText
 *
 * Example usage for max intrinsic:
 * @sample androidx.ui.layout.samples.MatchParentDividerForAspectRatio
 */
@ExperimentalLayout
@Stable
fun Modifier.preferredHeight(intrinsicSize: IntrinsicSize) = when (intrinsicSize) {
    IntrinsicSize.Min -> this + PreferredMinIntrinsicHeightModifier
    IntrinsicSize.Max -> this + PreferredMaxIntrinsicHeightModifier
}

/**
 * Intrinsic size used in [preferredWidth] or [preferredHeight] which can refer to width or height.
 */
enum class IntrinsicSize { Min, Max }

private object PreferredMinIntrinsicWidthModifier : PreferredIntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.minIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicWidth(height)
}

private object PreferredMinIntrinsicHeightModifier : PreferredIntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.minIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicHeight(width)
}

private object PreferredMaxIntrinsicWidthModifier : PreferredIntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val width = measurable.maxIntrinsicWidth(constraints.maxHeight)
        return Constraints.fixedWidth(width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicWidth(height, layoutDirection)
}

private object PreferredMaxIntrinsicHeightModifier : PreferredIntrinsicSizeModifier {
    override fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints {
        val height = measurable.maxIntrinsicHeight(constraints.maxWidth)
        return Constraints.fixedHeight(height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicHeight(width)
}

private interface PreferredIntrinsicSizeModifier : LayoutModifier {
    fun MeasureScope.calculateContentConstraints(
        measurable: Measurable,
        constraints: Constraints
    ): Constraints

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(
            calculateContentConstraints(
                measurable,
                constraints
            ).enforce(constraints)
        )
        return layout(placeable.width, placeable.height) {
            placeable.place(IntPxPosition.Origin)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicWidth(height, layoutDirection)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicHeight(width, layoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicWidth(height, layoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicHeight(width, layoutDirection)
}

/**
 * Layout composable that forces its child to be as wide as its min intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 */
@Deprecated("This component is deprecated. " +
        "Please use the preferredWidth(IntrinsicSize.Min) modifier instead.")
@Composable
fun MinIntrinsicWidth(children: @Composable () -> Unit) {
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
        val width = measurable?.minIntrinsicWidth(constraints.maxHeight, layoutDirection) ?: 0.ipx
        val placeable = measurable?.measure(
            Constraints.fixedWidth(width).enforce(constraints)
        )
        layout(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
            placeable?.placeAbsolute(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as tall as its min intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 */
@Deprecated("This component is deprecated. " +
        "Please use the preferredHeight(IntrinsicSize.Min) modifier instead.")
@Composable
fun MinIntrinsicHeight(children: @Composable () -> Unit) {
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
            placeable?.placeAbsolute(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as wide as its max intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 */
@Deprecated("This component is deprecated. " +
        "Please use the preferredWidth(IntrinsicSize.Max) modifier instead.")
@Composable
fun MaxIntrinsicWidth(children: @Composable () -> Unit) {
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
            placeable?.placeAbsolute(0.ipx, 0.ipx)
        }
    }
}

/**
 * Layout composable that forces its child to be as tall as its max intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 */
@Deprecated("This component is deprecated. " +
        "Please use the preferredHeight(IntrinsicSize.Max) modifier instead.")
@Composable
fun MaxIntrinsicHeight(children: @Composable () -> Unit) {
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
            placeable?.placeAbsolute(0.ipx, 0.ipx)
        }
    }
}
