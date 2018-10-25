/*
 * Copyright 2018 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")
package androidx.ui.core

/**
 * When a [MeasureBox] wants to know what size a child wants to be in one dimension, given a fixed
 * size in the opposite dimension, SizeBias indicates how the child should calculate the size.
 * For example, when measuring the minimum height required when a given width is provided:
 *     <MeasureBox> constraints ->
 *         if (constraints.bias == SizeBias.TowardMinimum) {
 *             if (constraints.hasTightWidth) {
 *                 // calculate height based on width
 *             } else {
 *                 // calculate width based on height
 *             }
 *         }
 *         // ...
 *     </MeasureBox>
 * Use [minIntrinsicHeight], [minIntrinsicWidth], [maxIntrinsicHeight], and
 * [maxIntrinsicWidth] for creating constraints that require bias. Constraints bias is
 * in the [Constraints.bias] property.
 */
// TODO(mount) Move this to the ui-core module
enum class SizeBias {
    /**
     * In the axis that isn't tight ([Constraints.hasTightWidth], [Constraints.hasTightHeight]),
     * layout using the size that takes up the minimum space with the given opposite axis without
     * being clipped. For example, determining the height that text would need for a given
     * horizontal space could use TowardMinimum for [Constraints.bias].
     *
     * TowardMinimum is only valid when one axis in [Constraints] is tight
     * ([Constraints.hasTightWidth], [Constraints.hasTightHeight]) and the opposite axis has an
     * unbounded range between `0.dp` and `Float.POSITIVE_INFINITY.dp`. The bounded axis may
     * be finite or infinite.
     */
    TowardMinimum,

    /**
     * In the axis that isn't tight ([Constraints.hasTightWidth], [Constraints.hasTightHeight]),
     * layout using the size that takes up the maximum space with the given opposite dimension
     * that can be filled without needing whitespace. For example, when a bitmap with a fixed
     * aspect ratio is given a certain height, a TowardMaximum bias would be used to determine
     * preferred width.
     *
     * TowardMaximum is only valid when one axis in [Constraints] is tight
     * ([Constraints.hasTightWidth], [Constraints.hasTightHeight]) and the opposite axis has an
     * unbounded range between `0.dp` and `Float.POSITIVE_INFINITY.dp`. The bounded axis may
     * be finite or infinite.
     */
    TowardMaximum,

    /**
     * Any value within the [Constraints] may be used to size the `MeasureBox`. This is the
     * default value.
     */
    Preferred
}

/**
 * Constraints used for measuring child [MeasureBox]es.
 */
// TODO(mount) Move this to the ui-core module
data class Constraints internal constructor(
    val minWidth: Dimension,
    val maxWidth: Dimension,
    val minHeight: Dimension,
    val maxHeight: Dimension,
    val bias: SizeBias
) {
    init {
        assert(minWidth.dp.isFinite())
        assert(minHeight.dp.isFinite())
        assert(
            bias == SizeBias.Preferred ||
                    (hasTightHeight && isBiasedConstraint(minWidth, maxWidth)) ||
                    (hasTightWidth && isBiasedConstraint(minHeight, maxHeight))
        )
    }

    private inline fun isBiasedConstraint(min: Dimension, max: Dimension): Boolean =
        min.dp == 0f && max.dp == Float.POSITIVE_INFINITY
}

/**
 * Whether or not the upper bound on the maximum height.
 * @see hasBoundedWidth
 */
val Constraints.hasBoundedHeight get() = maxHeight.dp.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
val Constraints.hasBoundedWidth get() = maxWidth.dp.isFinite()

/**
 * Whether there is exactly one size that satisfies the constraints.
 * @see hasTightHeight
 * @see hasTightWidth
 */
val Constraints.isTight get() = minWidth == maxWidth && minHeight == maxHeight

/**
 * Whether there is exactly one width value that satisfies the constraints.
 */
val Constraints.hasTightWidth get() = maxWidth == minWidth

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
val Constraints.hasTightHeight get() = maxHeight == minHeight

fun tightConstraints(width: Dimension, height: Dimension) =
    Constraints(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height)

fun Constraints(
    minWidth: Dimension = 0.dp,
    maxWidth: Dimension = Float.POSITIVE_INFINITY.dp,
    minHeight: Dimension = 0.dp,
    maxHeight: Dimension = Float.POSITIVE_INFINITY.dp
) = Constraints(minWidth, maxWidth, minHeight, maxHeight, SizeBias.Preferred)

fun minIntrinsicHeight(width: Dimension) =
    Constraints(
        minWidth = width,
        maxWidth = width,
        minHeight = 0.dp,
        maxHeight = Float.POSITIVE_INFINITY.dp,
        bias = SizeBias.TowardMinimum
    )

fun maxIntrinsicHeight(width: Dimension) =
    Constraints(
        minWidth = width,
        maxWidth = width,
        minHeight = 0.dp,
        maxHeight = Float.POSITIVE_INFINITY.dp,
        bias = SizeBias.TowardMaximum
    )

fun minIntrinsicWidth(height: Dimension) =
    Constraints(
        minHeight = height,
        maxHeight = height,
        minWidth = 0.dp,
        maxWidth = Float.POSITIVE_INFINITY.dp,
        bias = SizeBias.TowardMinimum
    )

fun maxIntrinsicWidth(height: Dimension) =
    Constraints(
        minHeight = height,
        maxHeight = height,
        minWidth = 0.dp,
        maxWidth = Float.POSITIVE_INFINITY.dp,
        bias = SizeBias.TowardMaximum
    )
