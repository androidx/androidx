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

import androidx.compose.Stable
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.enforce
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.isFinite

private data class SizeModifier(private val modifierConstraints: DpConstraints) : LayoutModifier {
    override fun Density.modifyConstraints(constraints: Constraints) =
        Constraints(modifierConstraints).enforce(constraints)

    override fun Density.minIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.minIntrinsicWidth(height).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

    override fun Density.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx): IntPx =
        measurable.maxIntrinsicWidth(height).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minWidth, constraints.maxWidth)
        }

    override fun Density.minIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.minIntrinsicHeight(width).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minHeight, constraints.maxHeight)
        }

    override fun Density.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx): IntPx =
        measurable.maxIntrinsicHeight(width).let {
            val constraints = Constraints(modifierConstraints)
            it.coerceIn(constraints.minHeight, constraints.maxHeight)
        }
}

/**
 * [Modifies][LayoutModifier] the width of a Compose UI layout element.
 * `LayoutWidth(16.dp)` will instruct the layout element to be exactly 16dp wide if permitted by
 * its parent.
 *
 * This modifies the incoming [Constraints] provided by a layout element's parent.
 * If the incoming constraints do not allow the modified size, the incoming constraints from
 * the parent will restrict the final size.
 *
 * See [Min], [Max], [Constrain] and [Fill] to modify the width of a layout element within a
 * range rather than to an exact size. See [LayoutHeight] to modify height, or [LayoutSize]
 * to modify both width and height at once.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleWidthModifier
 */
@Stable
data class LayoutWidth(val width: Dp)
// TODO: remove delegation here and implement inline
    : LayoutModifier by SizeModifier(DpConstraints.fixedWidth(width)) {
    init {
        require(width.isFinite()) { "width must be finite" }
        require(width >= Dp.Hairline) { "width must be >= 0.dp" }
    }

    /**
     * [Modifies][LayoutModifier] the width of a Compose UI layout element to be at least
     * [minWidth] wide if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Min(val minWidth: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(DpConstraints(minWidth = minWidth)) {
        init {
            require(minWidth.isFinite()) { "minWidth must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the width of a Compose UI layout element to be at most
     * [maxWidth] wide if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Max(val maxWidth: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(DpConstraints(maxWidth = maxWidth))

    /**
     * [Modifies][LayoutModifier] the width of a Compose UI layout element to be at least
     * [minWidth] and at most [maxWidth] wide if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Constrain(val minWidth: Dp, val maxWidth: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(DpConstraints(minWidth = minWidth, maxWidth = maxWidth)) {
        init {
            require(minWidth.isFinite()) { "minWidth must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the width of a Compose UI layout element to fill all available
     * space.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleFillWidthModifier
     */
    @Stable
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(constraints: Constraints): Constraints =
            if (constraints.hasBoundedWidth) {
                constraints.copy(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth)
            } else {
                constraints
            }
    }
}

/**
 * [Modifies][LayoutModifier] the height of a Compose UI layout element.
 * `LayoutHeight(16.dp)` will instruct the layout element to be exactly 16dp tall if permitted by
 * its parent.
 *
 * This modifies the incoming [Constraints] provided by a layout element's parent.
 * If the incoming constraints do not allow the modified size, the incoming constraints from
 * the parent will restrict the final size.
 *
 * See [Min], [Max], [Constrain] and [Fill] to modify the height of a layout element within a
 * range rather than to an exact size. See [LayoutWidth] to modify width, or [LayoutSize]
 * to modify both width and height at once.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleHeightModifier
 */
@Stable
data class LayoutHeight(val height: Dp)
// TODO: remove delegation here and implement inline
    : LayoutModifier by SizeModifier(DpConstraints.fixedHeight(height)) {
    init {
        require(height.isFinite()) { "height must be finite" }
        require(height >= Dp.Hairline) { "height must be >= 0.dp" }
    }

    /**
     * [Modifies][LayoutModifier] the height of a Compose UI layout element to be at least
     * [minHeight] tall if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Min(val minHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(DpConstraints(minHeight = minHeight)) {
        init {
            require(minHeight.isFinite()) { "minHeight must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the height of a Compose UI layout element to be at most
     * [maxHeight] tall if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Max(val maxHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(DpConstraints(maxHeight = maxHeight))

    /**
     * [Modifies][LayoutModifier] the height of a Compose UI layout element to be at least
     * [minHeight] and at most [maxHeight] tall if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Constrain(val minHeight: Dp, val maxHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(
        DpConstraints(minHeight = minHeight, maxHeight = maxHeight)
    ) {
        init {
            require(minHeight.isFinite()) { "minHeight must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the height of a Compose UI layout element to fill all available
     * space.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleFillHeightModifier
     */
    @Stable
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(constraints: Constraints): Constraints =
            if (constraints.hasBoundedHeight) {
                constraints.copy(
                    minHeight = constraints.maxHeight,
                    maxHeight = constraints.maxHeight
                )
            } else {
                constraints
            }
    }
}

/**
 * [Modifies][LayoutModifier] the width and height of a Compose UI layout element together.
 * `LayoutSize(24.dp, 16.dp)` will instruct the layout element to be exactly 24dp wide and 16dp
 * tall if permitted by its parent.
 *
 * This modifies the incoming [Constraints] provided by a layout element's parent.
 * If the incoming constraints do not allow the modified size, the incoming constraints from
 * the parent will restrict the final size.
 *
 * See [Min], [Max], [Constrain] and [Fill] to modify the height of a layout element within a
 * range rather than to an exact size. See [LayoutWidth] to modify width, or [LayoutSize]
 * to modify both width and height at once.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleSizeModifier
 */
@Stable
data class LayoutSize(val width: Dp, val height: Dp)
// TODO: remove delegation here and implement inline
    : LayoutModifier by SizeModifier(DpConstraints.fixed(width, height)) {

    /**
     * [Modifies][LayoutModifier] a Compose UI layout element to have a square size of [size].
     */
    constructor(size: Dp) : this(width = size, height = size)

    init {
        require(width.isFinite()) { "width must be finite" }
        require(height.isFinite()) { "height must be finite" }
        require(width >= Dp.Hairline) { "width must be >= 0.dp" }
        require(height >= Dp.Hairline) { "height must be >= 0.dp" }
    }

    /**
     * [Modifies][LayoutModifier] the size of a Compose UI layout element to be at least
     * [minWidth] wide and [minHeight] tall if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Min(val minWidth: Dp, val minHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(
        DpConstraints(minWidth = minWidth, minHeight = minHeight)
    ) {
        /**
         * [Modifies][LayoutModifier] a Compose UI layout element to have a square minimum size of
         * [minSize].
         */
        constructor(minSize: Dp) : this(minWidth = minSize, minHeight = minSize)

        init {
            require(minWidth.isFinite()) { "minWidth must be finite" }
            require(minHeight.isFinite()) { "minHeight must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the size of a Compose UI layout element to be at most
     * [maxWidth] wide and [maxHeight] tall if permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Max(val maxWidth: Dp, val maxHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(
        DpConstraints(maxWidth = maxWidth, maxHeight = maxHeight)
    ) {
        /**
         * [Modifies][LayoutModifier] a Compose UI layout element to have a square maximum size of
         * [maxSize].
         */
        constructor(maxSize: Dp) : this(maxWidth = maxSize, maxHeight = maxSize)
    }

    /**
     * [Modifies][LayoutModifier] the height of a Compose UI layout element to be at least
     * [minWidth] wide and [minHeight] tall, and at most [minWidth] wide and [maxHeight] tall if
     * permitted by its parent.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     */
    @Stable
    data class Constrain(
        val minWidth: Dp,
        val minHeight: Dp,
        val maxWidth: Dp,
        val maxHeight: Dp
    ) : LayoutModifier by SizeModifier(
        // TODO: remove delegation here and implement inline
        DpConstraints(
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight
        )
    ) {
        /**
         * [Modifies][LayoutModifier] a Compose UI layout element to have a square minimum
         * size of [minSize] and a square maximum size of [maxSize].
         */
        constructor(minSize: Dp, maxSize: Dp) : this(
            minWidth = minSize,
            minHeight = minSize,
            maxWidth = maxSize,
            maxHeight = maxSize
        )

        init {
            require(minWidth.isFinite()) { "minWidth must be finite" }
            require(minHeight.isFinite()) { "minHeight must be finite" }
        }
    }

    /**
     * [Modifies][LayoutModifier] the size of a Compose UI layout element to fill all available
     * space.
     *
     * This modifies the incoming [Constraints] provided by a layout element's parent.
     * If the incoming constraints do not allow the modified size, the incoming constraints from
     * the parent will restrict the final size.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleFillModifier
     */
    @Stable
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(constraints: Constraints): Constraints =
            when {
                constraints.hasBoundedWidth && constraints.hasBoundedHeight -> constraints.copy(
                    minWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight
                )
                constraints.hasBoundedWidth -> constraints.copy(
                    minWidth = constraints.maxWidth
                )
                constraints.hasBoundedHeight -> constraints.copy(
                    minHeight = constraints.maxHeight
                )
                else -> constraints
            }
    }
}
