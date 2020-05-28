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
@file:Suppress("NOTHING_TO_INLINE")
package androidx.ui.core

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.coerceAtLeast
import androidx.ui.unit.coerceIn
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite

/**
 * Immutable constraints used for measuring child [Layout]s or [LayoutModifier]s. A parent layout
 * can measure their children using the measure method on the corresponding [Measurable]s,
 * method which takes the [Constraints] the child has to follow. A measured child is then
 * responsible to choose for themselves and return a size which satisfies the set of [Constraints]
 * received from their parent:
 * - minWidth <= chosenWidth <= maxWidth
 * - minHeight <= chosenHeight <= maxHeight
 * The parent can then access the child chosen size on the resulting [Placeable]. The parent is
 * responsible of defining a valid positioning of the children according to their sizes, so the
 * parent needs to measure the children with appropriate [Constraints], such that whatever valid
 * sizes children choose, they can be laid out in a way that also respects the parent's incoming
 * [Constraints]. Note that different children can be measured with different [Constraints].
 * A child is allowed to choose a size that does not satisfy its constraints. However, when this
 * happens, the parent will not read from the [Placeable] the real size of the child, but rather
 * one that was coerced in the child's constraints; therefore, a parent can assume that its
 * children will always respect the constraints in their layout algorithm. When this does not
 * happen in reality, the position assigned to the child will be automatically offset to be centered
 * on the space assigned by the parent under the assumption that constraints were respected.
 * A set of [Constraints] can have infinite maxWidth and/or maxHeight. This is a trick often
 * used by parents to ask their children for their preferred size: unbounded constraints force
 * children whose default behavior is to fill the available space (always size to
 * maxWidth/maxHeight) to have an opinion about their preferred size. Most commonly, when measured
 * with unbounded [Constraints], these children will fallback to size themselves to wrap their
 * content, instead of expanding to fill the available space (this is not always true
 * as it depends on the child layout model, but is a common behavior for core layout components).
 *
 * @param minWidth The minimum width of a layout satisfying the constraints.
 * @param maxWidth The maximum width of a layout satisfying the constraints.
 * @param minHeight The minimum height of a layout satisfying the constraints.
 * @param maxHeight The maximum height of a layout satisfying the constraints.
 */
@Immutable
data class Constraints(
    @Stable
    val minWidth: IntPx = IntPx.Zero,
    @Stable
    val maxWidth: IntPx = IntPx.Infinity,
    @Stable
    val minHeight: IntPx = IntPx.Zero,
    @Stable
    val maxHeight: IntPx = IntPx.Infinity
) {
    init {
        // TODO(mount/popam): This verification is costly. Can we avoid it sometimes or at least on production?
        require(minWidth.isFinite()) { "minWidth $minWidth should be finite" }
        require(minHeight.isFinite()) { "minHeight $minHeight should be finite" }
        require(minWidth <= maxWidth) {
            "Constraints should be satisfiable, but minWidth($minWidth) > maxWidth($maxWidth)"
        }
        require(minHeight <= maxHeight) {
            "Constraints should be satisfiable, but minHeight($minHeight) > maxHeight($maxHeight)"
        }
        require(minWidth >= IntPx.Zero) { "minWidth $minWidth should be non-negative" }
        require(maxWidth >= IntPx.Zero) { "maxWidth $maxWidth should be non-negative" }
        require(minHeight >= IntPx.Zero) { "minHeight $minHeight should be non-negative" }
        require(maxHeight >= IntPx.Zero) { "maxHeight $maxHeight should be non-negative" }
    }

    companion object {
        /**
         * Creates constraints for fixed size in both dimensions.
         */
        @Stable
        fun fixed(width: IntPx, height: IntPx) = Constraints(width, width, height, height)

        /**
         * Creates constraints for fixed width and unspecified height.
         */
        @Stable
        fun fixedWidth(width: IntPx) = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = IntPx.Zero,
            maxHeight = IntPx.Infinity
        )

        /**
         * Creates constraints for fixed height and unspecified width.
         */
        @Stable
        fun fixedHeight(height: IntPx) = Constraints(
            minWidth = IntPx.Zero,
            maxWidth = IntPx.Infinity,
            minHeight = height,
            maxHeight = height
        )
    }
}

/**
 * Whether or not the upper bound on the maximum height.
 * @see hasBoundedWidth
 */
@Stable
val Constraints.hasBoundedHeight get() = maxHeight.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
@Stable
val Constraints.hasBoundedWidth get() = maxWidth.isFinite()

/**
 * Whether there is exactly one width value that satisfies the constraints.
 */
@Stable
val Constraints.hasFixedWidth get() = maxWidth == minWidth

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
@Stable
val Constraints.hasFixedHeight get() = maxHeight == minHeight

/**
 * Whether the area of a component respecting these constraints will definitely be 0.
 * This is true when at least one of maxWidth and maxHeight are 0.
 */
@Stable
val Constraints.isZero get() = maxWidth == IntPx.Zero || maxHeight == IntPx.Zero

/**
 * Returns the result of coercing the current constraints in a different set of constraints.
 */
@Stable
fun Constraints.enforce(otherConstraints: Constraints) = Constraints(
    minWidth = minWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    maxWidth = maxWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    minHeight = minHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight),
    maxHeight = maxHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight)
)

/**
 * Takes a size and returns the closest size to it that satisfies the constraints.
 */
@Stable
fun Constraints.constrain(size: IntPxSize) = IntPxSize(
    size.width.coerceIn(minWidth, maxWidth),
    size.height.coerceIn(minHeight, maxHeight)
)

/**
 * Takes a size and returns whether it satisfies the current constraints.
 */
@Stable
fun Constraints.satisfiedBy(size: IntPxSize) =
        minWidth <= size.width && size.width <= maxWidth &&
                minHeight <= size.height && size.height <= maxHeight

/**
 * Returns the Constraints obtained by offsetting the current instance with the given values.
 */
@Stable
fun Constraints.offset(horizontal: IntPx = 0.ipx, vertical: IntPx = 0.ipx) = Constraints(
    (minWidth + horizontal).coerceAtLeast(0.ipx),
    (maxWidth + horizontal).coerceAtLeast(0.ipx),
    (minHeight + vertical).coerceAtLeast(0.ipx),
    (maxHeight + vertical).coerceAtLeast(0.ipx)
)
