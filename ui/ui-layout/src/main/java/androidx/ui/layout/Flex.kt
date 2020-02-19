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

import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureBlock
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.Placeable
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import kotlin.math.sign

/**
 * Base class for scopes of [Row] and [Column], containing scoped modifiers.
 */
@LayoutScopeMarker
sealed class FlexScope {
    /**
     * A layout modifier within a [Column] or [Row] that makes the target component flexible in
     * the main direction of the parent (vertically in [Column] and horizontally in [Row]).
     * It will be assigned a space according to its flex weight, proportional to the flex
     * weights of other flexible siblings. If a sibling is not flexible, its flex weight will be 0.
     * When [tight] is set to true, the target component is forced to occupy the entire space
     * assigned to it by the parent. [LayoutFlexible] children will be measured after all the
     * inflexible ones have been measured, in order to divide the unclaimed space between
     * them.
     */
    fun LayoutFlexible(
        @FloatRange(from = 0.0, fromInclusive = false) flex: Float,
        tight: Boolean = true
    ): ParentDataModifier {
        require(flex > 0.0) { "Flex value should be greater than zero." }
        return if (tight) {
            FlexModifier(FlexChildProperties(flex, FlexFit.Tight))
        } else {
            FlexModifier(FlexChildProperties(flex, FlexFit.Loose))
        }
    }

    /**
     * A layout modifier within a [Column] or [Row] that positions its target component relative
     * to all other elements within the container which have [LayoutGravity.RelativeToSiblings].
     * The [alignmentLineBlock] accepts the [Placeable] of the targeted layout and returns the
     * position, perpendicular to the layout direction, along which the target should align
     * such that it coincides with the alignment lines of all other siblings with
     * [LayoutGravity.RelativeToSiblings].
     * Within a [Column] or [Row], all components with [LayoutGravity.RelativeToSiblings] will
     * align using the specified [AlignmentLine]s or values obtained from [alignmentLineBlock]s,
     * forming a sibling group. At least one element of the sibling group will be placed as it had
     * [LayoutGravity.Start][ColumnScope.Start] in [Column] or [LayoutGravity.Top][RowScope.Top]
     * in [Row], respectively, and the alignment of the other siblings will be then determined
     * such that the alignment lines coincide. Note that if the target component is the only one
     * with the [RelativeToSiblings] modifier specified, then the component will be positioned
     * using [LayoutGravity.Start][ColumnScope.Start] in [Column] or
     * [LayoutGravity.Top][RowScope.Top] in [Row] respectively.
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblings
     */
    @Suppress("unused")
    fun LayoutGravity.RelativeToSiblings(
        alignmentLineBlock: (Placeable) -> IntPx
    ): ParentDataModifier = SiblingsAlignedModifier.WithAlignmentLineBlock(alignmentLineBlock)
}

/**
 * A ColumnScope provides a scope for the children of a [Column].
 */
@Suppress("unused") // Note: Gravity object provides a scope only but is never used itself
class ColumnScope private constructor() : FlexScope() {
    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its start edge is aligned to the start edge of the [Column].
     */
    // TODO: Consider ltr/rtl.
    val LayoutGravity.Start: ParentDataModifier get() = StartGravityModifier
    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its center is in the middle of the [Column].
     */
    val LayoutGravity.Center: ParentDataModifier get() = CenterGravityModifier
    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * such that its end edge is aligned to the end edge of the [Column].
     */
    val LayoutGravity.End: ParentDataModifier get() = EndGravityModifier
    /**
     * A layout modifier within a [Column] that positions its target component horizontally
     * according to the specified [VerticalAlignmentLine], such that the position of the alignment
     * line coincides with the alignment lines of all other siblings having their gravity set to
     * [LayoutGravity.RelativeToSiblings].
     * Within a [Column], all components with [LayoutGravity.RelativeToSiblings] will align
     * horizontally using the specified [AlignmentLine]s or values obtained from
     * [alignmentLineBlocks][FlexScope.RelativeToSiblings], forming a sibling group.
     * At least one element of the sibling group will be placed as it had
     * [LayoutGravity.Start][ColumnScope.Start] in [Column], and the alignment of the other
     * siblings will be then determined such that the alignment lines coincide. Note that if
     * the target component is the only one with the [RelativeToSiblings] modifier specified,
     * then the component will be positioned using [LayoutGravity.Start][ColumnScope.Start].
     *
     * Example usage:
     *
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    fun LayoutGravity.RelativeToSiblings(alignmentLine: VerticalAlignmentLine): ParentDataModifier =
        SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    internal companion object {
        internal val Instance = ColumnScope()

        val StartGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.Start)
        val CenterGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.Center)
        val EndGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.End)
    }
}

/**
 * A RowScope provides a scope for the children of a [Row].
 */
@Suppress("unused") // Note: Gravity object provides a scope only but is never used itself
class RowScope private constructor() : FlexScope() {
    /**
     * A layout modifier within a [Row] that positions its target component vertically
     * such that its top edge is aligned to the top edge of the [Row].
     */
    val LayoutGravity.Top: ParentDataModifier get() = TopGravityModifier
    /**
     * A layout modifier within a Row that positions target component vertically
     * such that its center is in the middle of the [Row].
     */
    val LayoutGravity.Center: ParentDataModifier get() = CenterGravityModifier
    /**
     * A layout modifier within a Row that positions target component vertically
     * such that its bottom edge is aligned to the bottom edge of the [Row].
     */
    val LayoutGravity.Bottom: ParentDataModifier get() = BottomGravityModifier
    /**
     * A layout modifier within a [Row] that positions its target component vertically
     * according to the specified [HorizontalAlignmentLine], such that the position of the alignment
     * line coincides with the alignment lines of all other siblings having their gravity set to
     * [LayoutGravity.RelativeToSiblings].
     * Within a [Row], all components with [LayoutGravity.RelativeToSiblings] will align
     * vertically using the specified [AlignmentLine]s or values obtained from
     * [alignmentLineBlocks][FlexScope.RelativeToSiblings], forming a sibling group.
     * At least one element of the sibling group will be placed as it had
     * [LayoutGravity.Top][RowScope.Top] in [Row], and the alignment of the other
     * siblings will be then determined such that the alignment lines coincide. Note that if
     * the target component is the only one with the [RelativeToSiblings] modifier specified,
     * then the component will be positioned using [LayoutGravity.Top][RowScope.Top].
     *
     * Example usage:
     * @sample androidx.ui.layout.samples.SimpleRelativeToSiblingsInRow
     */
    fun LayoutGravity.RelativeToSiblings(
        alignmentLine: HorizontalAlignmentLine
    ): ParentDataModifier = SiblingsAlignedModifier.WithAlignmentLine(alignmentLine)

    internal companion object {
        internal val Instance = RowScope()

        val TopGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.Start)
        val CenterGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.Center)
        val BottomGravityModifier: ParentDataModifier = GravityModifier(CrossAxisAlignment.End)
    }
}

/**
 * A layout composable that places its children in a horizontal sequence.
 *
 * The layout model is able to assign children widths according to their flex weights provided
 * using the [androidx.ui.layout.FlexScope.LayoutFlexible] modifier. If a child is not
 * [flexible][androidx.ui.layout.FlexScope.LayoutFlexible], it will be considered inflexible
 * and will be sized to its preferred width.
 *
 * When all children of a [Row] are inflexible, it will be as small as possible to fit its
 * children one next to the other. In order to change the size of the [Row], use the
 * [LayoutWidth] modifiers; to make it fill the available width [LayoutWidth.Fill] can be used.
 * If at least one child of a [Row] is [flexible][FlexScope.LayoutFlexible], the [Row] will
 * fill the available space, so there is no need for [LayoutWidth.Fill]. However, if [Row]'s
 * size should be limited, the [LayoutWidth] or [LayoutWidth.Max] layout modifiers should be
 * applied.
 *
 * When the size of the [Row] is larger than the sum of of its children sizes, an [arrangement]
 * can be specified to define the positioning of the children inside the [Row]. See [Arrangement]
 * for available positioning behaviors; a custom arrangement can also be defined using the
 * constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleRow
 *
 * @param modifier The modifier to be applied to the Row.
 * @param arrangement The horizontal arrangement of the layout's children.
 */
@Composable
fun Row(
    modifier: Modifier = Modifier.None,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    children: @Composable() RowScope.() -> Unit
) {
    FlexLayout(
        orientation = LayoutOrientation.Horizontal,
        modifier = modifier,
        arrangement = arrangement,
        crossAxisAlignment = CrossAxisAlignment.Start,
        crossAxisSize = SizeMode.Wrap,
        children = { RowScope.Instance.children() }
    )
}

/**
 * A layout composable that places its children in a vertical sequence.
 *
 * The layout model is able to assign children heights according to their flex weights provided
 * using the [androidx.ui.layout.FlexScope.LayoutFlexible] modifier. If a child is not
 * [flexible][androidx.ui.layout.FlexScope.LayoutFlexible], it will be considered inflexible
 * and will be sized to its preferred width.
 *
 * When all children of a [Column] are inflexible, it will be as small as possible to fit its
 * children one on top of the other. In order to change the size of the [Column], use the
 * [LayoutHeight] modifiers; to make it fill the available height [LayoutWidth.Fill] can be used.
 * If at least one child of a [Column] is [flexible][FlexScope.LayoutFlexible], the [Column] will
 * fill the available space, so there is no need for [LayoutWidth.Fill]. However, if [Column]'s
 * size should be limited, the [LayoutHeight] or [LayoutHeight.Max] layout modifiers should be
 * applied.
 *
 * When the size of the [Column] is larger than the sum of of its children sizes, an [arrangement]
 * can be specified to define the positioning of the children inside the [Column]. See
 * [Arrangement] for available positioning behaviors; a custom arrangement can also be defined
 * using the constructor of [Arrangement].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleColumn
 *
 * @param modifier The modifier to be applied to the Column.
 * @param arrangement The vertical arrangement of the layout's children.
 */
@Composable
fun Column(
    modifier: Modifier = Modifier.None,
    arrangement: Arrangement.Vertical = Arrangement.Top,
    children: @Composable() ColumnScope.() -> Unit
) {
    FlexLayout(
        orientation = LayoutOrientation.Vertical,
        modifier = modifier,
        arrangement = arrangement,
        crossAxisAlignment = CrossAxisAlignment.Start,
        crossAxisSize = SizeMode.Wrap,
        children = { ColumnScope.Instance.children() }
    )
}

internal enum class FlexFit {
    Tight,
    Loose
}

internal enum class LayoutOrientation {
    Horizontal,
    Vertical
}

/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
enum class SizeMode {
    /**
     * Minimize the amount of free space by wrapping the children,
     * subject to the incoming layout constraints.
     */
    Wrap,
    /**
     * Maximize the amount of free space by expanding to fill the available space,
     * subject to the incoming layout constraints.
     */
    Expand
}

/**
 * Used to specify the alignment of a layout's children, in main axis direction.
 */
enum class MainAxisAlignment(internal val arrangement: Arrangement) {
    // TODO(soboleva) support RTl in Flow
    // workaround for now - use Arrangement that equals to previous Arrangement
    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    Center(Arrangement.Center),
    /**
     * Place children such that they are as close as possible to the start of the main axis.
     */
    Start(Arrangement.Top),
    /**
     * Place children such that they are as close as possible to the end of the main axis.
     */
    End(Arrangement.Bottom),
    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    SpaceEvenly(Arrangement.SpaceEvenly),
    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    SpaceBetween(Arrangement.SpaceBetween),
    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    SpaceAround(Arrangement.SpaceAround);
}

/**
 * Used to specify the arrangement of the layout's children in [Row] or [Column] in the main axis
 * direction (horizontal and vertical, respectively).
 * @constructor Creates an arrangement using the [arrange] function. Use it to provide your own
 * arrangement of the layout's children.
 */
@Immutable
interface Arrangement {
    /**
     * Places the layout children inside the parent layout along the main axis.
     *
     * @param totalSize Available space that can be occupied by the children.
     * @param size A list of sizes of all children.
     * @param layoutDirection A layout direction, left-to-right or right-to-left, of the parent
     * layout that should be taken into account when determining positions of the children in
     * horizontal direction.
     */
    fun arrange(totalSize: IntPx, size: List<IntPx>, layoutDirection: LayoutDirection): List<IntPx>

    /**
     * Used to specify the vertical arrangement of the layout's children in a [Column].
     */
    interface Vertical : Arrangement
    /**
     * Used to specify the horizontal arrangement of the layout's children in a [Row].
     */
    interface Horizontal : Arrangement

    /**
     * Place children horizontally such that they are as close as possible to the beginning of the
     * main axis.
     */
    object Start : Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeLeftOrTop(size)
        } else {
            placeRightOrBottom(totalSize, size)
        }
    }

    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     */
    object End : Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeRightOrBottom(totalSize, size)
        } else {
            placeLeftOrTop(size)
        }
    }

    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     */
    object Top : Vertical {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ) = placeLeftOrTop(size)
    }

    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     */
    object Bottom : Vertical {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ) = placeRightOrBottom(totalSize, size)
    }

    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    object Center : Vertical, Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val positions = mutableListOf<IntPx>()
            var current = (totalSize - consumedSize).toPx() / 2
            size.forEach {
                positions.add(current.round())
                current += it
            }
            return positions
        }
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    object SpaceEvenly : Vertical, Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val gapSize = (totalSize - consumedSize).toPx() / (size.size + 1)
            val positions = mutableListOf<IntPx>()
            var current = gapSize
            size.forEach {
                positions.add(current.round())
                current += it.toPx() + gapSize
            }
            return positions
        }
    }

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    object SpaceBetween : Vertical, Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val gapSize = if (size.size > 1) {
                (totalSize - consumedSize).toPx() / (size.size - 1)
            } else {
                0.px
            }
            val positions = mutableListOf<IntPx>()
            var current = 0.px
            size.forEach {
                positions.add(current.round())
                current += it.toPx() + gapSize
            }
            return positions
        }
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    object SpaceAround : Vertical, Horizontal {
        override fun arrange(
            totalSize: IntPx,
            size: List<IntPx>,
            layoutDirection: LayoutDirection
        ): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val gapSize = if (size.isNotEmpty()) {
                (totalSize - consumedSize).toPx() / size.size
            } else {
                0.px
            }
            val positions = mutableListOf<IntPx>()
            var current = gapSize / 2
            size.forEach {
                positions.add(current.round())
                current += it.toPx() + gapSize
            }
            return positions
        }
    }

    private companion object {
        private fun placeRightOrBottom(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val positions = mutableListOf<IntPx>()
            var current = totalSize - consumedSize
            size.forEach {
                positions.add(current)
                current += it
            }
            return positions
        }
        private fun placeLeftOrTop(size: List<IntPx>): List<IntPx> {
            val positions = mutableListOf<IntPx>()
            var current = 0.ipx
            size.forEach {
                positions.add(current)
                current += it
            }
            return positions
        }
    }
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
// TODO(popam): refine this API surface with modifiers - add type safety for alignment orientation.
class CrossAxisAlignment private constructor(
    internal val alignmentLineProvider: AlignmentLineProvider? = null
) {
    companion object {
        /**
         * Place children such that their center is in the middle of the cross axis.
         */
        val Center = CrossAxisAlignment(null)
        /**
         * Place children such that their start edge is aligned to the start edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        val Start = CrossAxisAlignment(null)
        /**
         * Place children such that their end edge is aligned to the end edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        val End = CrossAxisAlignment(null)
        /**
         * Align children by their baseline.
         */
        fun AlignmentLine(alignmentLine: AlignmentLine) =
            CrossAxisAlignment(AlignmentLineProvider.Value(alignmentLine))
        /**
         * Align children relative to their siblings using the alignment line provided as a
         * parameter using [AlignmentLineProvider].
         */
        internal fun Relative(alignmentLineProvider: AlignmentLineProvider) =
            CrossAxisAlignment(alignmentLineProvider)
    }
}

/**
 * Box [Constraints], but which abstract away width and height in favor of main axis and cross axis.
 */
internal data class OrientationIndependentConstraints(
    val mainAxisMin: IntPx,
    val mainAxisMax: IntPx,
    val crossAxisMin: IntPx,
    val crossAxisMax: IntPx
) {
    constructor(c: Constraints, orientation: LayoutOrientation) : this(
        if (orientation === LayoutOrientation.Horizontal) c.minWidth else c.minHeight,
        if (orientation === LayoutOrientation.Horizontal) c.maxWidth else c.maxHeight,
        if (orientation === LayoutOrientation.Horizontal) c.minHeight else c.minWidth,
        if (orientation === LayoutOrientation.Horizontal) c.maxHeight else c.maxWidth
    )

    // Creates a new instance with the same main axis constraints and maximum tight cross axis.
    fun stretchCrossAxis() = OrientationIndependentConstraints(
        mainAxisMin,
        mainAxisMax,
        if (crossAxisMax.isFinite()) crossAxisMax else crossAxisMin,
        crossAxisMax
    )

    // Given an orientation, resolves the current instance to traditional constraints.
    fun toBoxConstraints(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            Constraints(mainAxisMin, mainAxisMax, crossAxisMin, crossAxisMax)
        } else {
            Constraints(crossAxisMin, crossAxisMax, mainAxisMin, mainAxisMax)
        }

    // Given an orientation, resolves the max width constraint this instance represents.
    fun maxWidth(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            mainAxisMax
        } else {
            crossAxisMax
        }

    // Given an orientation, resolves the max height constraint this instance represents.
    fun maxHeight(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            crossAxisMax
        } else {
            mainAxisMax
        }
}

private val IntrinsicMeasurable.flex: Float
    get() = (parentData as? FlexChildProperties)?.flex ?: 0f

private val IntrinsicMeasurable.fit: FlexFit
    get() = (parentData as? FlexChildProperties)?.fit ?: FlexFit.Loose

private val IntrinsicMeasurable.crossAxisAlignment: CrossAxisAlignment?
    get() = (parentData as? FlexChildProperties)?.crossAxisAlignment

/**
 * Layout model that places its children in a horizontal or vertical sequence, according to the
 * specified orientation, while also looking at the flex weights of the children.
 */
@Composable
private fun FlexLayout(
    orientation: LayoutOrientation,
    modifier: Modifier = Modifier.None,
    arrangement: Arrangement,
    crossAxisSize: SizeMode,
    crossAxisAlignment: CrossAxisAlignment,
    children: @Composable() () -> Unit
) {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height
    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    Layout(
        children,
        modifier = modifier,
        minIntrinsicWidthMeasureBlock = MinIntrinsicWidthMeasureBlock(orientation),
        minIntrinsicHeightMeasureBlock = MinIntrinsicHeightMeasureBlock(orientation),
        maxIntrinsicWidthMeasureBlock = MaxIntrinsicWidthMeasureBlock(orientation),
        maxIntrinsicHeightMeasureBlock = MaxIntrinsicHeightMeasureBlock(orientation)
    ) { ltrMeasurables, outerConstraints ->
        // rtl support
        val measurables = if (orientation == LayoutOrientation.Horizontal &&
                layoutDirection == LayoutDirection.Rtl) {
            ltrMeasurables.asReversed()
        } else {
            ltrMeasurables
        }

        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

        var totalFlex = 0f
        var inflexibleSpace = IntPx.Zero
        var crossAxisSpace = IntPx.Zero
        var beforeCrossAxisAlignmentLine = IntPx.Zero
        var afterCrossAxisAlignmentLine = IntPx.Zero

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure children with zero flex.
        for (i in 0 until measurables.size) {
            val child = measurables[i]
            val flex = child.flex

            if (flex > 0f) {
                totalFlex += child.flex
            } else {
                val placeable = child.measure(
                    // Ask for preferred main axis size.
                    constraints.copy(
                        mainAxisMin = IntPx.Zero,
                        mainAxisMax = constraints.mainAxisMax - inflexibleSpace,
                        crossAxisMin = IntPx.Zero
                    ).toBoxConstraints(orientation)
                )
                inflexibleSpace += placeable.mainAxisSize()
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())

                val lineProvider = measurables[i].crossAxisAlignment?.alignmentLineProvider
                    ?: crossAxisAlignment.alignmentLineProvider
                if (lineProvider != null) {
                    val alignmentLinePosition = when (lineProvider) {
                        is AlignmentLineProvider.Block -> lineProvider.lineProviderBlock(placeable)
                        is AlignmentLineProvider.Value -> placeable[lineProvider.line]
                    }
                    beforeCrossAxisAlignmentLine = max(
                        beforeCrossAxisAlignmentLine,
                        alignmentLinePosition ?: 0.ipx
                    )
                    afterCrossAxisAlignmentLine = max(
                        afterCrossAxisAlignmentLine,
                        placeable.crossAxisSize() -
                                (alignmentLinePosition ?: placeable.crossAxisSize())
                    )
                }
                placeables[i] = placeable
            }
        }

        // Then measure the rest according to their flexes in the remaining main axis space.
        val targetSpace = if (totalFlex > 0f && constraints.mainAxisMax.isFinite()) {
            constraints.mainAxisMax
        } else {
            constraints.mainAxisMin
        }

        val flexSliceSpace = if (totalFlex > 0) {
            (targetSpace.toPx() - inflexibleSpace) / totalFlex
        } else {
            0.px
        }

        var remainder = targetSpace - inflexibleSpace - measurables.sumBy {
            (flexSliceSpace * it.flex).round().value
        }.ipx

        var flexibleSpace = IntPx.Zero

        for (i in 0 until measurables.size) {
            val child = measurables[i]
            val flex = child.flex
            if (flex > 0f) {
                val remainderUnit = remainder.value.sign.ipx
                remainder -= remainderUnit
                val childMainAxisSize = max(
                    IntPx.Zero,
                    (flexSliceSpace * child.flex).round() + remainderUnit
                )
                val placeable = child.measure(
                    OrientationIndependentConstraints(
                        if (child.fit == FlexFit.Tight && childMainAxisSize.isFinite()) {
                            childMainAxisSize
                        } else {
                            IntPx.Zero
                        },
                        childMainAxisSize,
                        IntPx.Zero,
                        constraints.crossAxisMax
                    ).toBoxConstraints(orientation)
                )
                flexibleSpace += placeable.mainAxisSize()
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                placeables[i] = placeable
            }
        }

        // Compute the Flex size and position the children.
        val mainAxisLayoutSize = if (totalFlex > 0f && constraints.mainAxisMax.isFinite()) {
            constraints.mainAxisMax
        } else {
            max(inflexibleSpace + flexibleSpace, constraints.mainAxisMin)
        }
        val crossAxisLayoutSize = if (constraints.crossAxisMax.isFinite() &&
            crossAxisSize == SizeMode.Expand
        ) {
            constraints.crossAxisMax
        } else {
            max(
                crossAxisSpace,
                max(
                    constraints.crossAxisMin,
                    beforeCrossAxisAlignmentLine + afterCrossAxisAlignmentLine
                )
            )
        }
        val layoutWidth = if (orientation == LayoutOrientation.Horizontal) {
            mainAxisLayoutSize
        } else {
            crossAxisLayoutSize
        }
        val layoutHeight = if (orientation == LayoutOrientation.Horizontal) {
            crossAxisLayoutSize
        } else {
            mainAxisLayoutSize
        }
        layout(layoutWidth, layoutHeight) {
            val childrenMainAxisSize = placeables.map { it!!.mainAxisSize() }
            val mainAxisPositions = arrangement.arrange(
                mainAxisLayoutSize,
                childrenMainAxisSize,
                layoutDirection
            )
            placeables.forEachIndexed { index, placeable ->
                placeable!!
                val measurable = measurables[index]
                val childCrossAlignment = measurable.crossAxisAlignment ?: crossAxisAlignment
                val isRtlColumn = orientation == LayoutOrientation.Vertical &&
                        layoutDirection == LayoutDirection.Rtl
                val crossAxis = when (childCrossAlignment) {
                    CrossAxisAlignment.Start -> {
                        if (isRtlColumn) {
                            crossAxisLayoutSize - placeable.crossAxisSize()
                        } else {
                            IntPx.Zero
                        }
                    }
                    CrossAxisAlignment.End -> {
                        if (isRtlColumn) {
                            IntPx.Zero
                        } else {
                            crossAxisLayoutSize - placeable.crossAxisSize()
                        }
                    }
                    CrossAxisAlignment.Center -> {
                        Alignment.Center.align(
                            IntPxSize(
                                mainAxisLayoutSize - placeable.mainAxisSize(),
                                crossAxisLayoutSize - placeable.crossAxisSize()
                            )
                        ).y
                    }
                    else -> {
                        val provider = childCrossAlignment.alignmentLineProvider
                        val alignmentLinePosition = when (provider) {
                            is AlignmentLineProvider.Block -> provider.lineProviderBlock(placeable)
                            is AlignmentLineProvider.Value -> placeable[provider.line]
                            else -> null
                        }
                        if (alignmentLinePosition != null) {
                            val line = beforeCrossAxisAlignmentLine - alignmentLinePosition
                            if (orientation == LayoutOrientation.Vertical &&
                                layoutDirection == LayoutDirection.Rtl
                            ) {
                                layoutWidth - line - placeable.width
                            } else {
                                line
                            }
                        } else {
                            IntPx.Zero
                        }
                    }
                }
                if (orientation == LayoutOrientation.Horizontal) {
                    placeable.place(mainAxisPositions[index], crossAxis)
                } else {
                    placeable.place(crossAxis, mainAxisPositions[index])
                }
            }
        }
    }
}

private /*inline*/ fun MinIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMinWidth
    }

private /*inline*/ fun MinIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMinHeight
    }

private /*inline*/ fun MaxIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMaxWidth
    }

private /*inline*/ fun MaxIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMaxHeight
    }

private object IntrinsicMeasureBlocks {
    val HorizontalMinWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> minIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Horizontal
        )
    }
    val VerticalMinWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> minIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Vertical,
            LayoutOrientation.Horizontal
        )
    }
    val HorizontalMinHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> minIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Vertical
        )
    }
    val VerticalMinHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> minIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Vertical,
            LayoutOrientation.Vertical
        )
    }
    val HorizontalMaxWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> maxIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Horizontal
        )
    }
    val VerticalMaxWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> maxIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Vertical,
            LayoutOrientation.Horizontal
        )
    }
    val HorizontalMaxHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> maxIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Vertical
        )
    }
    val VerticalMaxHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> maxIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Vertical,
            LayoutOrientation.Vertical
        )
    }
}

private fun intrinsicSize(
    children: List<IntrinsicMeasurable>,
    intrinsicMainSize: IntrinsicMeasurable.(IntPx) -> IntPx,
    intrinsicCrossSize: IntrinsicMeasurable.(IntPx) -> IntPx,
    crossAxisAvailable: IntPx,
    flexOrientation: LayoutOrientation,
    intrinsicOrientation: LayoutOrientation
) = if (flexOrientation == intrinsicOrientation) {
    intrinsicMainAxisSize(children, intrinsicMainSize, crossAxisAvailable)
} else {
    intrinsicCrossAxisSize(children, intrinsicCrossSize, intrinsicMainSize, crossAxisAvailable)
}

private fun intrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(IntPx) -> IntPx,
    crossAxisAvailable: IntPx
): IntPx {
    var maxFlexibleSpace = 0.ipx
    var inflexibleSpace = 0.ipx
    var totalFlex = 0f
    children.forEach { child ->
        val flex = child.flex
        val size = child.mainAxisSize(crossAxisAvailable)
        if (flex == 0f) {
            inflexibleSpace += size
        } else if (flex > 0f) {
            totalFlex += flex
            maxFlexibleSpace = max(maxFlexibleSpace, size / flex)
        }
    }
    return maxFlexibleSpace * totalFlex + inflexibleSpace
}

private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(IntPx) -> IntPx,
    crossAxisSize: IntrinsicMeasurable.(IntPx) -> IntPx,
    mainAxisAvailable: IntPx
): IntPx {
    var inflexibleSpace = 0.ipx
    var crossAxisMax = 0.ipx
    var totalFlex = 0f
    children.forEach { child ->
        val flex = child.flex
        if (flex == 0f) {
            val mainAxisSpace = child.mainAxisSize(IntPx.Infinity)
            inflexibleSpace += mainAxisSpace
            crossAxisMax = max(crossAxisMax, child.crossAxisSize(mainAxisSpace))
        } else if (flex > 0f) {
            totalFlex += flex
        }
    }

    val flexSection = if (totalFlex == 0f) {
        IntPx.Zero
    } else {
        max(mainAxisAvailable - inflexibleSpace, IntPx.Zero) / totalFlex
    }

    children.forEach { child ->
        if (child.flex > 0f) {
            crossAxisMax = max(crossAxisMax, child.crossAxisSize(flexSection * child.flex))
        }
    }
    return crossAxisMax
}

private data class FlexModifier(val flexProperties: FlexChildProperties) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): FlexChildProperties {
        return ((parentData as? FlexChildProperties) ?: FlexChildProperties()).also {
            it.flex = flexProperties.flex
            it.fit = flexProperties.fit
        }
    }
}

private sealed class SiblingsAlignedModifier : ParentDataModifier {
    abstract override fun Density.modifyParentData(parentData: Any?): Any?

    internal data class WithAlignmentLineBlock(val block: (Placeable) -> IntPx) :
        SiblingsAlignedModifier() {
        override fun Density.modifyParentData(parentData: Any?): Any? {
            return ((parentData as? FlexChildProperties) ?: FlexChildProperties()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Block(block))
            }
        }
    }

    internal data class WithAlignmentLine(val line: AlignmentLine) :
        SiblingsAlignedModifier() {
        override fun Density.modifyParentData(parentData: Any?): Any? {
            return ((parentData as? FlexChildProperties) ?: FlexChildProperties()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Value(line))
            }
        }
    }
}

private data class GravityModifier(val alignment: CrossAxisAlignment) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): FlexChildProperties {
        return ((parentData as? FlexChildProperties) ?: FlexChildProperties()).also {
            it.crossAxisAlignment = alignment
        }
    }
}

/**
 * Parent data associated with children.
 */
private data class FlexChildProperties(
    var flex: Float? = null,
    var fit: FlexFit? = null,
    var crossAxisAlignment: CrossAxisAlignment? = null
)

/**
 * Provides the alignment line.
 */
internal sealed class AlignmentLineProvider {
    data class Block(val lineProviderBlock: (Placeable) -> IntPx) : AlignmentLineProvider()
    data class Value(val line: AlignmentLine) : AlignmentLineProvider()
}