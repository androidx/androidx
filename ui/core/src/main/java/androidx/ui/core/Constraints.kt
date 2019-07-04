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

/**
 * Immutable constraints used for measuring child [Layout]s. A parent [Layout]
 * can measure their children using the [measure] method on the corresponding [Measurable]s,
 * method which takes the [Constraints] the child has to follow. A [measure]d child is then
 * responsible to choose for themselves and return a size which satisfies the received set
 * of [Constraints]:
 * - minWidth <= chosenWidth <= maxWidth
 * - minHeight <= chosenHeight <= maxHeight
 * The parent can then access the child chosen size on the resulting [Placeable]. The parent is
 * responsible of defining a valid positioning of the children according to their sizes, so the
 * parent needs to measure the children with appropriate [Constraints], such that whatever valid
 * sizes children choose, they can be laid out in a way that also respects the parent's incoming
 * [Constraints]. Note that different children can be [measure]d with different [Constraints].
 * A set of [Constraints] can have infinite maxWidth and/or maxHeight. This is a trick often
 * used by parents to ask their children for their preferred size: unbounded constraints force
 * children whose default behavior is to fill the available space (always size to
 * maxWidth/maxHeight) to have an opinion about their preferred size. Most commonly, when measured
 * with unbounded [Constraints], these children will fallback to size themselves to wrap their
 * content, instead of expanding to fill the available space (this is not always true
 * as it depends on the child layout model, but is a common behavior for core layout components).
 */
data class Constraints(
    val minWidth: IntPx = IntPx.Zero,
    val maxWidth: IntPx = IntPx.Infinity,
    val minHeight: IntPx = IntPx.Zero,
    val maxHeight: IntPx = IntPx.Infinity
) {
    init {
        require(minWidth.isFinite()) { "Constraints#minWidth should be finite" }
        require(minHeight.isFinite()) { "Constraints#minHeight should be finite" }
        require(minWidth <= maxWidth) {
            "Constraints should be satisfiable, but minWidth > maxWidth"
        }
        require(minHeight <= maxHeight) {
            "Constraints should be satisfiable, but minHeight > maxHeight"
        }
        require(minWidth >= IntPx.Zero) { "Constraints#minWidth should be non-negative" }
        require(maxWidth >= IntPx.Zero) { "Constraints#maxWidth should be non-negative" }
        require(minHeight >= IntPx.Zero) { "Constraints#minHeight should be non-negative" }
        require(maxHeight >= IntPx.Zero) { "Constraints#maxHeight should be non-negative" }
    }

    companion object {
        /**
         * Creates constraints tight in both dimensions.
         */
        fun tightConstraints(width: IntPx, height: IntPx) =
            Constraints(width, width, height, height)

        /**
         * Creates constraints with tight width and loose height.
         */
        fun tightConstraintsForWidth(width: IntPx) = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = IntPx.Zero,
            maxHeight = IntPx.Infinity
        )

        /**
         * Creates constraints with tight height and loose width.
         */
        fun tightConstraintsForHeight(height: IntPx) = Constraints(
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
val Constraints.hasBoundedHeight get() = maxHeight.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
val Constraints.hasBoundedWidth get() = maxWidth.isFinite()

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

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
val Constraints.isZero get() = maxWidth == IntPx.Zero || maxHeight == IntPx.Zero

/**
 * Returns the result of coercing the current constraints in a different set of constraints.
 */
fun Constraints.enforce(otherConstraints: Constraints) = Constraints(
    minWidth = minWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    maxWidth = maxWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    minHeight = minHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight),
    maxHeight = maxHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight)
)

/**
 * Returns a copy of the current instance, overriding the specified values to be tight.
 */
fun Constraints.withTight(width: IntPx? = null, height: IntPx? = null) = Constraints(
    minWidth = width ?: this.minWidth,
    maxWidth = width ?: this.maxWidth,
    minHeight = height ?: this.minHeight,
    maxHeight = height ?: this.maxHeight
)

/**
 * Takes a size and returns the closest size to it that satisfies the constraints.
 */
fun Constraints.constrain(size: IntPxSize) = IntPxSize(
    size.width.coerceIn(minWidth, maxWidth),
    size.height.coerceIn(minHeight, maxHeight)
)

/**
 * Takes a size and returns whether it satisfies the current constraints.
 */
fun Constraints.satisfiedBy(size: IntPxSize) =
        minWidth <= size.width && size.width <= maxWidth &&
                minHeight <= size.height && size.height <= maxHeight

/**
 * Returns a copy of the current instance with no min constraints.
 */
fun Constraints.looseMin() = this.copy(minWidth = 0.ipx, minHeight = 0.ipx)

/**
 * Returns a copy of the current instance with no max constraints.
 */
fun Constraints.looseMax() = this.copy(maxWidth = IntPx.Infinity, maxHeight = IntPx.Infinity)

/**
 * Returns a copy of the current instance with the constraints tightened to their smallest size.
 */
fun Constraints.tightMin() = this.withTight(width = minWidth, height = minHeight)

/**
 * Returns a copy of the current instance with the constraints tightened to their largest size.
 * Note that if any of the constraints are unbounded, they will be left unchanged.
 */
fun Constraints.tightMax() = this.copy(
    minWidth = if (hasBoundedWidth) maxWidth else minWidth,
    minHeight = if (hasBoundedHeight) maxHeight else minHeight
)

/**
 * Returns the Constraints obtained by offsetting the current instance with the given values.
 */
fun Constraints.offset(horizontal: IntPx = 0.ipx, vertical: IntPx = 0.ipx) = Constraints(
    (minWidth + horizontal).coerceAtLeast(0.ipx),
    (maxWidth + horizontal).coerceAtLeast(0.ipx),
    (minHeight + vertical).coerceAtLeast(0.ipx),
    (maxHeight + vertical).coerceAtLeast(0.ipx)
)
