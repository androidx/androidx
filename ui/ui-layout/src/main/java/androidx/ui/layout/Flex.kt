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
import androidx.compose.composer
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Placeable
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureBlock
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.ParentData
import androidx.ui.core.isFinite
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx

/**
 * Parent data associated with children to assign flex and fit values for them.
 */
private data class FlexInfo(val flex: Float, val fit: FlexFit)

/**
 * Collects information about the children of a [FlexColumn] or [FlexColumn]
 * when its body is executed with a [FlexChildren] instance as argument.
 */
class FlexChildren internal constructor() {
    internal val childrenList = mutableListOf<@Composable() () -> Unit>()
    fun expanded(@FloatRange(from = 0.0) flex: Float, children: @Composable() () -> Unit) {
        if (flex < 0) {
            throw IllegalArgumentException("flex must be >= 0")
        }
        childrenList += {
            ParentData(data = FlexInfo(flex = flex, fit = FlexFit.Tight), children = children)
        }
    }

    fun flexible(@FloatRange(from = 0.0) flex: Float, children: @Composable() () -> Unit) {
        if (flex < 0) {
            throw IllegalArgumentException("flex must be >= 0")
        }
        childrenList += {
            ParentData(data = FlexInfo(flex = flex, fit = FlexFit.Loose), children = children)
        }
    }

    fun inflexible(children: @Composable() () -> Unit) {
        childrenList += {
            ParentData(data = FlexInfo(flex = 0f, fit = FlexFit.Loose), children = children)
        }
    }
}

/**
 * A widget that places its children in a horizontal sequence, assigning children widths
 * according to their flex weights.
 *
 * [FlexRow] children can be:
 * - [inflexible] meaning that the child is not flex, and it should be measured with loose
 * constraints to determine its preferred width
 * - [expanded] meaning that the child is flexible, and it should be assigned a width according
 * to its flex weight relative to its flexible children. The child is forced to occupy the
 * entire width assigned by the parent
 * - [flexible] similar to [expanded], but the child can leave unoccupied width.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleFlexRow
 *
 * @param mainAxisAlignment The alignment of the layout's children in main axis direction.
 * Default is [MainAxisAlignment.Start].
 * @param mainAxisSize The size of the layout in the main axis dimension.
 * Default is [LayoutSize.Expand].
 * @param crossAxisAlignment The alignment of the layout's children in cross axis direction.
 * Default is [CrossAxisAlignment.Start].
 * @param crossAxisSize The size of the layout in the cross axis dimension.
 * Default is [LayoutSize.Wrap].
 */
@Composable
fun FlexRow(
    modifier: Modifier = Modifier.None,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: LayoutSize = LayoutSize.Expand,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    crossAxisSize: LayoutSize = LayoutSize.Wrap,
    block: FlexChildren.() -> Unit
) {
    Flex(
        orientation = LayoutOrientation.Horizontal,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSize = crossAxisSize,
        modifier = modifier,
        block = block
    )
}

/**
 * A widget that places its children in a vertical sequence, assigning children heights
 * according to their flex weights.
 *
 * [FlexRow] children can be:
 * - [inflexible] meaning that the child is not flex, and it should be measured with
 * loose constraints to determine its preferred height
 * - [expanded] meaning that the child is flexible, and it should be assigned a
 * height according to its flex weight relative to its flexible children. The child is forced
 * to occupy the entire height assigned by the parent
 * - [flexible] similar to [expanded], but the child can leave unoccupied height.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleFlexColumn
 *
 * @param mainAxisAlignment The alignment of the layout's children in main axis direction.
 * Default is [MainAxisAlignment.Start].
 * @param mainAxisSize The size of the layout in the main axis dimension.
 * Default is [LayoutSize.Expand].
 * @param crossAxisAlignment The alignment of the layout's children in cross axis direction.
 * Default is [CrossAxisAlignment.Start].
 * @param crossAxisSize The size of the layout in the cross axis dimension.
 * Default is [LayoutSize.Wrap].
 */
@Composable
fun FlexColumn(
    modifier: Modifier = Modifier.None,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: LayoutSize = LayoutSize.Expand,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    crossAxisSize: LayoutSize = LayoutSize.Wrap,
    block: FlexChildren.() -> Unit
) {
    Flex(
        orientation = LayoutOrientation.Vertical,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSize = crossAxisSize,
        modifier = modifier,
        block = block
    )
}

/**
 * A widget that places its children in a horizontal sequence.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleRow
 *
 * @param mainAxisAlignment The alignment of the layout's children in main axis direction.
 * Default is [MainAxisAlignment.Start].
 * @param mainAxisSize The size of the layout in the main axis dimension.
 * Default is [LayoutSize.Wrap].
 * @param crossAxisAlignment The alignment of the layout's children in cross axis direction.
 * Default is [CrossAxisAlignment.Start].
 * @param crossAxisSize The size of the layout in the cross axis dimension.
 * Default is [LayoutSize.Wrap].
 */
@Composable
fun Row(
    modifier: Modifier = Modifier.None,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: LayoutSize = LayoutSize.Wrap,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    crossAxisSize: LayoutSize = LayoutSize.Wrap,
    block: @Composable() () -> Unit
) {
    FlexRow(
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSize = crossAxisSize,
        modifier = modifier
    ) {
        inflexible {
            block()
        }
    }
}

/**
 * A widget that places its children in a vertical sequence.
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleColumn
 *
 * @param mainAxisAlignment The alignment of the layout's children in main axis direction.
 * Default is [MainAxisAlignment.Start].
 * @param mainAxisSize The size of the layout in the main axis dimension.
 * Default is [LayoutSize.Wrap].
 * @param crossAxisAlignment The alignment of the layout's children in cross axis direction.
 * Default is [CrossAxisAlignment.Start].
 * @param crossAxisSize The size of the layout in the cross axis dimension.
 * Default is [LayoutSize.Wrap].
 */
@Composable
fun Column(
    modifier: Modifier = Modifier.None,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: LayoutSize = LayoutSize.Wrap,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    crossAxisSize: LayoutSize = LayoutSize.Wrap,
    block: @Composable() () -> Unit
) {
    FlexColumn(
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSize = crossAxisSize,
        modifier = modifier
    ) {
        inflexible {
            block()
        }
    }
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
enum class LayoutSize {
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
enum class MainAxisAlignment(internal val aligner: Aligner) {
    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    Center(MainAxisCenterAligner()),
    /**
     * Place children such that they are as close as possible to the start of the main axis.
     */
    // TODO(popam): Consider rtl directionality.
    Start(MainAxisStartAligner()),
    /**
     * Place children such that they are as close as possible to the end of the main axis.
     */
    End(MainAxisEndAligner()),
    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    SpaceEvenly(MainAxisSpaceEvenlyAligner()),
    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    SpaceBetween(MainAxisSpaceBetweenAligner()),
    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    SpaceAround(MainAxisSpaceAroundAligner());

    internal interface Aligner {
        fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx>
    }

    private class MainAxisCenterAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
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

    private class MainAxisStartAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
            val positions = mutableListOf<IntPx>()
            var current = 0.ipx
            size.forEach {
                positions.add(current)
                current += it
            }
            return positions
        }
    }

    private class MainAxisEndAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val positions = mutableListOf<IntPx>()
            var current = totalSize - consumedSize
            size.forEach {
                positions.add(current)
                current += it
            }
            return positions
        }
    }

    private class MainAxisSpaceEvenlyAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
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

    private class MainAxisSpaceBetweenAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
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

    private class MainAxisSpaceAroundAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
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
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
// TODO(popam): refine this API surface with modifiers - add type safety for alignment orientation.
class CrossAxisAlignment private constructor(
    internal val alignmentLine: AlignmentLine?
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
         * Force children to occupy the entire cross axis space.
         */
        val Stretch = CrossAxisAlignment(null)
        /**
         * Align children by their baseline.
         */
        fun AlignmentLine(alignmentLine: AlignmentLine) = CrossAxisAlignment(alignmentLine)
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

    // Creates a new instance with the same cross axis constraints and unbounded main axis.
    fun looseMainAxis() = OrientationIndependentConstraints(
        IntPx.Zero, IntPx.Infinity, crossAxisMin, crossAxisMax
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

private val IntrinsicMeasurable.flex: Float get() = (parentData as FlexInfo).flex
private val IntrinsicMeasurable.fit: FlexFit get() = (parentData as FlexInfo).fit

/**
 * Layout model that places its children in a horizontal or vertical sequence, according to the
 * specified orientation, while also looking at the flex weights of the children.
 */
@Composable
private fun Flex(
    orientation: LayoutOrientation,
    modifier: Modifier = Modifier.None,
    mainAxisSize: LayoutSize,
    mainAxisAlignment: MainAxisAlignment,
    crossAxisSize: LayoutSize,
    crossAxisAlignment: CrossAxisAlignment,
    block: FlexChildren.() -> Unit
) {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height
    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    val flexChildren: @Composable() () -> Unit = with(FlexChildren()) {
        block()
        val composable = @Composable {
            childrenList.forEach { it() }
        }
        composable
    }
    Layout(
        flexChildren,
        modifier = modifier,
        minIntrinsicWidthMeasureBlock = MinIntrinsicWidthMeasureBlock(orientation),
        minIntrinsicHeightMeasureBlock = MinIntrinsicHeightMeasureBlock(orientation),
        maxIntrinsicWidthMeasureBlock = MaxIntrinsicWidthMeasureBlock(orientation),
        maxIntrinsicHeightMeasureBlock = MaxIntrinsicHeightMeasureBlock(orientation)
    ) { children, outerConstraints ->
        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

        var totalFlex = 0f
        var inflexibleSpace = IntPx.Zero
        var crossAxisSpace = IntPx.Zero
        var beforeCrossAxisAlignmentLine = IntPx.Zero
        var afterCrossAxisAlignmentLine = IntPx.Zero

        val placeables = arrayOfNulls<Placeable>(children.size)
        // First measure children with zero flex.
        for (i in 0 until children.size) {
            val child = children[i]
            val flex = child.flex

            if (flex > 0f) {
                totalFlex += child.flex
            } else {
                val placeable = child.measure(
                    // Ask for preferred main axis size.
                    constraints.looseMainAxis().let {
                        if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                            it.stretchCrossAxis()
                        } else {
                            it.copy(crossAxisMin = IntPx.Zero)
                        }
                    }.toBoxConstraints(orientation)
                )
                inflexibleSpace += placeable.mainAxisSize()
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                if (crossAxisAlignment.alignmentLine != null) {
                    val alignmentLinePosition = placeable[crossAxisAlignment.alignmentLine]
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
        val targetSpace = if (mainAxisSize == LayoutSize.Expand) {
            constraints.mainAxisMax
        } else {
            constraints.mainAxisMin
        }

        var flexibleSpace = IntPx.Zero

        for (i in 0 until children.size) {
            val child = children[i]
            val flex = child.flex
            if (flex > 0f) {
                val childMainAxisSize = max(
                    IntPx.Zero,
                    (targetSpace - inflexibleSpace) * child.flex / totalFlex
                )
                val placeable = child.measure(
                    OrientationIndependentConstraints(
                        if (child.fit == FlexFit.Tight && childMainAxisSize.isFinite()) {
                            childMainAxisSize
                        } else {
                            IntPx.Zero
                        },
                        childMainAxisSize,
                        if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                            constraints.crossAxisMax
                        } else {
                            IntPx.Zero
                        },
                        constraints.crossAxisMax
                    ).toBoxConstraints(orientation)
                )
                flexibleSpace += placeable.mainAxisSize()
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                placeables[i] = placeable
            }
        }

        // Compute the Flex size and position the children.
        val mainAxisLayoutSize = if (constraints.mainAxisMax.isFinite() &&
            mainAxisSize == LayoutSize.Expand
        ) {
            constraints.mainAxisMax
        } else {
            max(inflexibleSpace + flexibleSpace, constraints.mainAxisMin)
        }
        val crossAxisLayoutSize = if (constraints.crossAxisMax.isFinite() &&
            crossAxisSize == LayoutSize.Expand
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
            val mainAxisPositions = mainAxisAlignment.aligner
                .align(mainAxisLayoutSize, childrenMainAxisSize)
            placeables.forEachIndexed { index, placeable ->
                placeable!!
                val crossAxis = when (crossAxisAlignment) {
                    CrossAxisAlignment.Start -> IntPx.Zero
                    CrossAxisAlignment.Stretch -> IntPx.Zero
                    CrossAxisAlignment.End -> {
                        crossAxisLayoutSize - placeable.crossAxisSize()
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
                        val alignmentLinePosition =
                            placeable[crossAxisAlignment.alignmentLine!!]
                        if (alignmentLinePosition != null) {
                            beforeCrossAxisAlignmentLine - alignmentLinePosition
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
    val HorizontalMinWidth: IntrinsicMeasureBlock = { measurables, availableHeight: IntPx ->
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
    val VerticalMinHeight by lazy(mode = LazyThreadSafetyMode.NONE) {
        val block: IntrinsicMeasureBlock = { measurables, availableWidth ->
            intrinsicSize(
                measurables,
                { w -> minIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                LayoutOrientation.Vertical,
                LayoutOrientation.Vertical
            )
        }
        block
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
