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

@file:Suppress("Deprecation")

package androidx.ui.layout

import androidx.compose.Stable
import androidx.ui.core.Alignment
import androidx.ui.core.Alignment.BottomCenter
import androidx.ui.core.Alignment.BottomEnd
import androidx.ui.core.Alignment.BottomStart
import androidx.ui.core.Alignment.Center
import androidx.ui.core.Alignment.CenterEnd
import androidx.ui.core.Alignment.CenterStart
import androidx.ui.core.Alignment.TopCenter
import androidx.ui.core.Alignment.TopEnd
import androidx.ui.core.Alignment.TopStart
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.Modifier
import androidx.ui.core.enforce
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.isFinite

/**
 * Declare the preferred width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * See [preferredHeight] or [preferredSize] to set other preferred dimensions.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimplePreferredWidthModifier
 */
fun Modifier.preferredWidth(width: Dp) = preferredSizeIn(minWidth = width, maxWidth = width)

/**
 * Declare the preferred height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * See [preferredWidth] or [preferredSize] to set other preferred dimensions.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimplePreferredHeightModifier
 */
fun Modifier.preferredHeight(height: Dp) = preferredSizeIn(minHeight = height, maxHeight = height)

/**
 * Declare the preferred size of the content to be exactly [size]dp square. The incoming measurement
 * [Constraints] may override this value, forcing the content to be either smaller or larger.
 *
 * See [preferredWidth] or [preferredHeight] to set width or height alone.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimplePreferredSizeModifier
 */
fun Modifier.preferredSize(size: Dp) = preferredSizeIn(size, size, size, size)

/**
 * Declare the preferred size of the content to be exactly [width]dp by [height]dp. The incoming
 * measurement [Constraints] may override this value, forcing the content to be either smaller or
 * larger.
 *
 * See [preferredWidth] or [preferredHeight] to set width or height alone.
 * See [preferredWidthIn], [preferredHeightIn] or [preferredSizeIn] to set a preferred size range.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimplePreferredSizeModifier
 */
fun Modifier.preferredSize(width: Dp, height: Dp) = preferredSizeIn(
    minWidth = width,
    maxWidth = width,
    minHeight = height,
    maxHeight = height
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
fun Modifier.preferredWidthIn(
    minWidth: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified
) = preferredSizeIn(minWidth = minWidth, maxWidth = maxWidth)

/**
 * Constrain the height of the content to be between [minHeight]dp and [maxHeight]dp as permitted
 * by the incoming measurement [Constraints]. If the incoming constraints are more restrictive
 * the requested size will obey the incoming constraints and attempt to be as close as possible
 * to the preferred size.
 */
fun Modifier.preferredHeightIn(
    minHeight: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = preferredSizeIn(minHeight = minHeight, maxHeight = maxHeight)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp and the height
 * of the content to be between [minHeight] and [maxHeight] as permitted by the incoming
 * measurement [Constraints]. If the incoming constraints are more restrictive the requested size
 * will obey the incoming constraints and attempt to be as close as possible to the preferred size.
 */
fun Modifier.preferredSizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = preferredSizeIn(
    DpConstraints(
        minWidth = if (minWidth != Dp.Unspecified) minWidth else 0.dp,
        minHeight = if (minHeight != Dp.Unspecified) minHeight else 0.dp,
        maxWidth = if (maxWidth != Dp.Unspecified) maxWidth else Dp.Infinity,
        maxHeight = if (maxHeight != Dp.Unspecified) maxHeight else Dp.Infinity
    )
)

/**
 * Constrain the size of the content to be within [constraints] as permitted by the incoming
 * measurement [Constraints]. If the incoming measurement constraints are more restrictive the
 * requested size will obey the incoming constraints and attempt to be as close as possible to
 * the preferred size.
 */
fun Modifier.preferredSizeIn(constraints: DpConstraints) = this + SizeModifier(constraints)

/**
 * Declare the width of the content to be exactly [width]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [widthIn] and [sizeIn] to set a size range.
 * See [preferredWidth] to set a preferred width, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleWidthModifier
 */
fun Modifier.width(width: Dp) = sizeIn(minWidth = width, maxWidth = width)

/**
 * Declare the height of the content to be exactly [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [heightIn] and [sizeIn] to set a size range.
 * See [preferredHeight] to set a preferred height, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleHeightModifier
 */
fun Modifier.height(height: Dp) = sizeIn(minHeight = height, maxHeight = height)

/**
 * Declare the size of the content to be exactly [size]dp width and height. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [sizeIn] to set a size range.
 * See [preferredSize] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleSizeModifier
 */
fun Modifier.size(size: Dp) = sizeIn(size, size, size, size)

/**
 * Declare the size of the content to be exactly [width]dp and [height]dp. The incoming measurement
 * [Constraints] will not override this value. If the content chooses a size that does not
 * satisfy the incoming [Constraints], the parent layout will be reported a size coerced
 * in the [Constraints], and the position of the content will be automatically offset to be
 * centered on the space assigned to the child by the parent layout under the assumption that
 * [Constraints] were respected.
 *
 * See [sizeIn] to set a size range.
 * See [preferredSize] to set a preferred size, which is only respected when the incoming
 * constraints allow it.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleWidthModifier
 */
fun Modifier.size(width: Dp, height: Dp) = sizeIn(
    minWidth = width,
    maxWidth = width,
    minHeight = height,
    maxHeight = height
)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
fun Modifier.widthIn(
    minWidth: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified
) = sizeIn(minWidth = minWidth, maxWidth = maxWidth)

/**
 * Constrain the height of the content to be between [minHeight]dp and [maxHeight]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
fun Modifier.heightIn(
    minHeight: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = sizeIn(minHeight = minHeight, maxHeight = maxHeight)

/**
 * Constrain the width of the content to be between [minWidth]dp and [maxWidth]dp, and the
 * height of the content to be between [minHeight]dp and [maxHeight]dp.
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
fun Modifier.sizeIn(
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified
) = sizeIn(
    DpConstraints(
        minWidth = if (minWidth != Dp.Unspecified) minWidth else 0.dp,
        minHeight = if (minHeight != Dp.Unspecified) minHeight else 0.dp,
        maxWidth = if (maxWidth != Dp.Unspecified) maxWidth else Dp.Infinity,
        maxHeight = if (maxHeight != Dp.Unspecified) maxHeight else Dp.Infinity
    )
)

/**
 * Constrain the size of the content to be within [constraints].
 * If the content chooses a size that does not satisfy the incoming [Constraints], the
 * parent layout will be reported a size coerced in the [Constraints], and the position
 * of the content will be automatically offset to be centered on the space assigned to
 * the child by the parent layout under the assumption that [Constraints] were respected.
 */
fun Modifier.sizeIn(constraints: DpConstraints) = this + SizeModifier(constraints, false)

/**
 * Have the content fill the [Constraints.maxWidth] of the incoming measurement constraints
 * by setting the [minimum width][Constraints.minWidth] to be equal to the
 * [maximum width][Constraints.maxWidth]. If the incoming maximum width is [IntPx.Infinity] this
 * modifier will have no effect.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleFillWidthModifier
 */
fun Modifier.fillMaxWidth() = this + LayoutWidth.Fill

/**
 * Have the content fill the [Constraints.maxHeight] of the incoming measurement constraints
 * by setting the [minimum height][Constraints.minHeight] to be equal to the
 * [maximum height][Constraints.maxHeight]. If the incoming maximum height is [IntPx.Infinity] this
 * modifier will have no effect.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleFillHeightModifier
 */
fun Modifier.fillMaxHeight() = this + LayoutHeight.Fill

/**
 * Have the content fill the [Constraints.maxWidth] and [Constraints.maxHeight] of the incoming
 * measurement constraints by setting the [minimum width][Constraints.minWidth] to be equal to the
 * [maximum width][Constraints.maxWidth] and the [minimum height][Constraints.minHeight] to be
 * equal to the [maximum height][Constraints.maxHeight]. If the incoming maximum width or height
 * is [IntPx.Infinity] this modifier will have no effect in that dimension.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleFillModifier
 */
fun Modifier.fillMaxSize() = this + LayoutSize.Fill

/**
 * Allow the content to measure at its desired width without regard for the incoming measurement
 * [minimum width constraint][Constraints.minWidth]. If the content's measured size is smaller
 * than the minimum width constraint, [align] it within that minimum width space.
 */
// TODO: Consider an axis-specific [Alignment]
fun Modifier.wrapContentWidth(align: Alignment = Center) = this + when (align) {
    TopStart, CenterStart, BottomStart -> LayoutAlign.Start
    TopCenter, Center, BottomCenter -> LayoutAlign.CenterHorizontally
    TopEnd, CenterEnd, BottomEnd -> LayoutAlign.End
}

/**
 * Allow the content to measure at its desired height without regard for the incoming measurement
 * [minimum height constraint][Constraints.minHeight]. If the content's measured size is smaller
 * than the minimum height constraint, [align] it within that minimum height space.
 */
// TODO: Consider an axis-specific [Alignment]
fun Modifier.wrapContentHeight(align: Alignment = Center) = this + when (align) {
    TopStart, TopCenter, TopEnd -> LayoutAlign.Top
    CenterStart, Center, CenterEnd -> LayoutAlign.CenterVertically
    BottomStart, BottomCenter, BottomEnd -> LayoutAlign.Bottom
}

/**
 * Allow the content to measure at its desired size without regard for the incoming measurement
 * [minimum width][Constraints.minWidth] or [minimum height][Constraints.minHeight] constraints.
 * If the content's measured size is smaller than the minimum size constraint, [align] it
 * within that minimum sized space.
 */
fun Modifier.wrapContentSize(align: Alignment = Center) = this + when (align) {
    TopStart -> LayoutAlign.TopStart
    TopCenter -> LayoutAlign.TopCenter
    TopEnd -> LayoutAlign.TopEnd
    CenterStart -> LayoutAlign.CenterStart
    Center -> LayoutAlign.Center
    CenterEnd -> LayoutAlign.CenterEnd
    BottomStart -> LayoutAlign.BottomStart
    BottomCenter -> LayoutAlign.BottomCenter
    BottomEnd -> LayoutAlign.BottomEnd
}

private data class SizeModifier(
    private val targetConstraints: DpConstraints,
    private val enforceIncoming: Boolean = true
) : LayoutModifier {
    override fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ) = Constraints(targetConstraints).let { if (enforceIncoming) it.enforce(constraints) else it }

    override fun Density.minIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicWidth(height).let {
        val constraints = Constraints(targetConstraints)
        it.coerceIn(constraints.minWidth, constraints.maxWidth)
    }

    override fun Density.maxIntrinsicWidthOf(
        measurable: Measurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicWidth(height).let {
        val constraints = Constraints(targetConstraints)
        it.coerceIn(constraints.minWidth, constraints.maxWidth)
    }

    override fun Density.minIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicHeight(width).let {
        val constraints = Constraints(targetConstraints)
        it.coerceIn(constraints.minHeight, constraints.maxHeight)
    }

    override fun Density.maxIntrinsicHeightOf(
        measurable: Measurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicHeight(width).let {
        val constraints = Constraints(targetConstraints)
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
data class LayoutWidth
@Deprecated(
    "Use Modifier.preferredWidth",
    replaceWith = ReplaceWith(
        "Modifier.preferredWidth(width)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.preferredWidth"
    )
)
constructor(val width: Dp)
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
    data class Min
    @Deprecated(
        "Use Modifier.preferredWidthIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredWidthIn(minWidth = minWidth)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredWidthIn"
        )
    )
    constructor(val minWidth: Dp)
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
    data class Max
    @Deprecated(
        "Use Modifier.preferredWidthIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredWidthIn(maxWidth = maxWidth)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredWidthIn"
        )
    )
    constructor(val maxWidth: Dp)
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
    data class Constrain
    @Deprecated(
        "Use Modifier.preferredWidthIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredWidthIn(minWidth, maxWidth)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredWidthIn"
        )
    )
    constructor(val minWidth: Dp, val maxWidth: Dp)
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
    @Deprecated(
        "Use Modifier.fillMaxWidth",
        replaceWith = ReplaceWith(
            "Modifier.fillMaxWidth()",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.fillMaxWidth"
        )
    )
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): Constraints =
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
data class LayoutHeight
@Deprecated(
    "Use Modifier.preferredHeight",
    replaceWith = ReplaceWith(
        "Modifier.preferredHeight(height)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.preferredHeight"
    )
)
constructor(val height: Dp)
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
    data class Min
    @Deprecated(
        "Use Modifier.preferredHeightIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredHeightIn(minHeight = minHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredHeightIn"
        )
    )
    constructor(val minHeight: Dp)
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
    data class Max
    @Deprecated(
        "Use Modifier.preferredHeightIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredHeightIn(maxHeight = maxHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredHeightIn"
        )
    )
    constructor(val maxHeight: Dp)
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
    data class Constrain
    @Deprecated(
        "Use Modifier.preferredHeightIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredHeightIn(minHeight, maxHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredHeightIn"
        )
    )
    constructor(val minHeight: Dp, val maxHeight: Dp)
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
    @Deprecated(
        "Use Modifier.fillMaxHeight",
        replaceWith = ReplaceWith(
            "Modifier.fillMaxHeight()",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.fillMaxHeight"
        )
    )
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): Constraints =
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
data class LayoutSize
@Deprecated(
    "Use Modifier.preferredSize",
    replaceWith = ReplaceWith(
        "Modifier.preferredSize(width, height)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.preferredSize"
    )
)
constructor(val width: Dp, val height: Dp)
// TODO: remove delegation here and implement inline
    : LayoutModifier by SizeModifier(DpConstraints.fixed(width, height)) {

    /**
     * [Modifies][LayoutModifier] a Compose UI layout element to have a square size of [size].
     */
    @Deprecated(
        "Use Modifier.preferredSize",
        replaceWith = ReplaceWith(
            "Modifier.preferredSize(size)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredSize"
        )
    )
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
    data class Min
    @Deprecated(
        "Use Modifier.preferredSizeIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredSizeIn(minWidth = minWidth, minHeight = minHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredSizeIn"
        )
    )
    constructor(val minWidth: Dp, val minHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(
        DpConstraints(minWidth = minWidth, minHeight = minHeight)
    ) {
        /**
         * [Modifies][LayoutModifier] a Compose UI layout element to have a square minimum size of
         * [minSize].
         */
        @Deprecated(
            "Use Modifier.preferredSizeIn",
            replaceWith = ReplaceWith(
                "Modifier.preferredSizeIn(minWidth = minSize, minHeight = minSize)",
                "androidx.ui.core.Modifier",
                "androidx.ui.layout.preferredSizeIn"
            )
        )
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
    data class Max
    @Deprecated(
        "Use Modifier.preferredSizeIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredSizeIn(maxWidth = maxWidth, maxHeight = maxHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredSizeIn"
        )
    )
    constructor(val maxWidth: Dp, val maxHeight: Dp)
    // TODO: remove delegation here and implement inline
        : LayoutModifier by SizeModifier(
        DpConstraints(maxWidth = maxWidth, maxHeight = maxHeight)
    ) {
        /**
         * [Modifies][LayoutModifier] a Compose UI layout element to have a square maximum size of
         * [maxSize].
         */
        @Deprecated(
            "Use Modifier.preferredSizeIn",
            replaceWith = ReplaceWith(
                "Modifier.preferredSizeIn(maxWidth = maxSize, maxHeight = maxSize)",
                "androidx.ui.core.Modifier",
                "androidx.ui.layout.preferredSizeIn"
            )
        )
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
    data class Constrain
    @Deprecated(
        "Use Modifier.preferredSizeIn",
        replaceWith = ReplaceWith(
            "Modifier.preferredSizeIn(minWidth, minHeight, maxWidth, maxHeight)",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.preferredSizeIn"
        )
    )
    constructor(
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
        @Deprecated(
            "Use Modifier.preferredSizeIn",
            replaceWith = ReplaceWith(
                "Modifier.preferredSize(minSize, minSize, maxSize, maxSize)",
                "androidx.ui.core.Modifier",
                "androidx.ui.layout.preferredSizeIn"
            )
        )
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
    @Deprecated(
        "Use Modifier.fillMaxSize",
        replaceWith = ReplaceWith(
            "Modifier.fillMaxSize()",
            "androidx.ui.core.Modifier",
            "androidx.ui.layout.fillMaxSize"
        )
    )
    object Fill : LayoutModifier {
        override fun Density.modifyConstraints(
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): Constraints =
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
