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
 * Immutable constraints used for measuring child [MeasureBox]es. A parent [MeasureBox]
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
    val minWidth: Px = 0.px,
    val maxWidth: Px = Px.Infinity,
    val minHeight: Px = 0.px,
    val maxHeight: Px = Px.Infinity
) {
    init {
        assert(minWidth.value.isFinite())
        assert(minHeight.value.isFinite())
    }

    companion object {
        /**
         * Creates constraints tight in both dimensions.
         */
        fun tightConstraints(width: Px, height: Px) =
            Constraints(width, width, height, height)

        /**
         * Creates constraints with tight width and loose height.
         */
        fun tightConstraintsForWidth(width: Px) = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0.px,
            maxHeight = Px.Infinity
        )

        /**
         * Creates constraints with tight height and loose width.
         */
        fun tightConstraintsForHeight(height: Px) = Constraints(
            minWidth = 0.px,
            maxWidth = Px.Infinity,
            minHeight = height,
            maxHeight = height
        )
    }
}

/**
 * Whether or not the upper bound on the maximum height.
 * @see hasBoundedWidth
 */
val Constraints.hasBoundedHeight get() = maxHeight.value.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
val Constraints.hasBoundedWidth get() = maxWidth.value.isFinite()

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
val Constraints.isZero get() = maxWidth == 0.px || maxHeight == 0.px

/**
 * Whether there is any size that satisfies the current constraints.
 */
val Constraints.satisfiable get() = minWidth <= maxWidth && minHeight <= maxHeight

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
fun Constraints.withTight(width: Px? = null, height: Px? = null) = Constraints(
    minWidth = width ?: this.minWidth,
    maxWidth = width ?: this.maxWidth,
    minHeight = height ?: this.minHeight,
    maxHeight = height ?: this.maxHeight
)
