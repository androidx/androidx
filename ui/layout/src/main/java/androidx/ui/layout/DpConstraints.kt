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
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.isFinite

/**
 * Similar to [Constraints], but with constraint values expressed in [Dp].
 * They are used in the APIs of certain layout models such as [Container] or [ConstrainedBox],
 * and will be translated to [Constraints] before being used by the core measuring and layout steps.
 */
data class DpConstraints(
    val minWidth: Dp = 0.dp,
    val maxWidth: Dp = Dp.Infinity,
    val minHeight: Dp = 0.dp,
    val maxHeight: Dp = Dp.Infinity
) {
    init {
        assert(minWidth.isFinite())
        assert(minHeight.isFinite())
    }

    companion object {
        /**
         * Creates constraints tight in both dimensions.
         */
        fun tightConstraints(width: Dp, height: Dp) = DpConstraints(width, width, height, height)

        /**
         * Creates constraints with tight width and loose height.
         */
        fun tightConstraintsForWidth(width: Dp) = DpConstraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0.dp,
            maxHeight = Dp.Infinity
        )

        /**
         * Creates constraints with tight height and loose width.
         */
        fun tightConstraintsForHeight(height: Dp) = DpConstraints(
            minWidth = 0.dp,
            maxWidth = Dp.Infinity,
            minHeight = height,
            maxHeight = height
        )
    }
}

/**
 * Whether or not the upper bound on the maximum height.
 * @see hasBoundedWidth
 */
val DpConstraints.hasBoundedHeight get() = maxHeight.isFinite()

/**
 * Whether or not the upper bound on the maximum width.
 * @see hasBoundedHeight
 */
val DpConstraints.hasBoundedWidth get() = maxWidth.isFinite()

/**
 * Whether there is exactly one size that satisfies the constraints.
 * @see hasTightHeight
 * @see hasTightWidth
 */
val DpConstraints.isTight get() = minWidth == maxWidth && minHeight == maxHeight

/**
 * Whether there is exactly one width value that satisfies the constraints.
 */
val DpConstraints.hasTightWidth get() = maxWidth == minWidth

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
val DpConstraints.hasTightHeight get() = maxHeight == minHeight

/**
 * Whether there is exactly one height value that satisfies the constraints.
 */
val DpConstraints.isZero get() = maxWidth == 0.dp || maxHeight == 0.dp

/**
 * Whether there is any size that satisfies the current constraints.
 */
val DpConstraints.satisfiable get() = minWidth <= maxWidth && minHeight <= maxHeight

/**
 * Returns the result of coercing the current constraints in a different set of constraints.
 */
fun DpConstraints.enforce(otherConstraints: DpConstraints) = DpConstraints(
    minWidth = minWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    maxWidth = maxWidth.coerceIn(otherConstraints.minWidth, otherConstraints.maxWidth),
    minHeight = minHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight),
    maxHeight = maxHeight.coerceIn(otherConstraints.minHeight, otherConstraints.maxHeight)
)

/**
 * Returns a copy of the current instance, overriding the specified values to be tight.
 */
fun DpConstraints.withTight(width: Dp? = null, height: Dp? = null) = DpConstraints(
    minWidth = width ?: this.minWidth,
    maxWidth = width ?: this.maxWidth,
    minHeight = height ?: this.minHeight,
    maxHeight = height ?: this.maxHeight
)

/**
 * Creates the [Constraints] corresponding to the current [DpConstraints].
 */
fun DensityReceiver.Constraints(dpConstraints: DpConstraints) = Constraints(
    dpConstraints.minWidth.toIntPx(),
    dpConstraints.maxWidth.toIntPx(),
    dpConstraints.minHeight.toIntPx(),
    dpConstraints.maxHeight.toIntPx()
)
