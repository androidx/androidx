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
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Measurable
import androidx.ui.core.Placeable
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.ui.core.ComplexLayout
import androidx.ui.core.ParentData
import androidx.ui.core.isFinite

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
 *     FlexRow {
 *         expanded(flex = 2f) {
 *             Center {
 *                 SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
 *             }
 *         }
 *         inflexible {
 *             SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp)
 *         }
 *         expanded(flex = 1f) {
 *             SizedRectangle(color = Color(0xFF00FF00.toInt()))
 *         }
 *     }
 */
@Composable
fun FlexRow(
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    @Children(composable = false) block: FlexChildren.() -> Unit
) {
    Flex(
        orientation = FlexOrientation.Horizontal,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
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
 *     ColumnFlex {
 *         expanded(flex = 2f) {
 *             Center {
 *                 SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
 *             }
 *         }
 *         inflexible {
 *             SizedRectangle(color = Color(0xFFFF0000.toInt()), height = 40.dp)
 *         }
 *         expanded(flex = 1f) {
 *             SizedRectangle(color = Color(0xFF00FF00.toInt()))
 *         }
 *     }
 */
@Composable
fun FlexColumn(
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    @Children(composable = false) block: FlexChildren.() -> Unit
) {
    Flex(
        orientation = FlexOrientation.Vertical,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment,
        block = block
    )
}

/**
 * A widget that places its children in a horizontal sequence.
 *
 * Example usage:
 *   Row {
 *       SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
 *       SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp, height = 80.dp)
 *       SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 80.dp, height = 70.dp)
 *   }
 */
@Composable
fun Row(
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    @Children block: @Composable() () -> Unit
) {
    FlexRow(
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment
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
 *   Column {
 *       SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
 *       SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp, height = 80.dp)
 *       SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 80.dp, height = 70.dp)
 *   }
 */
@Composable
fun Column(
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    @Children block: @Composable() () -> Unit
) {
    FlexColumn(
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSize = mainAxisSize,
        crossAxisAlignment = crossAxisAlignment
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

internal enum class FlexOrientation {
    Horizontal,
    Vertical
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
     * TODO(popam): Consider rtl directionality.
     */
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
            var current = (totalSize - consumedSize) / 2
            size.forEach {
                positions.add(current)
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
            val gapSize = (totalSize - consumedSize) / (size.size + 1)
            val positions = mutableListOf<IntPx>()
            var current = gapSize
            size.forEach {
                positions.add(current)
                current += it + gapSize
            }
            return positions
        }
    }

    private class MainAxisSpaceBetweenAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val gapSize = if (size.size > 1) {
                (totalSize - consumedSize) / (size.size - 1)
            } else {
                0.ipx
            }
            val positions = mutableListOf<IntPx>()
            var current = 0.ipx
            size.forEach {
                positions.add(current)
                current += it + gapSize
            }
            return positions
        }
    }

    private class MainAxisSpaceAroundAligner : Aligner {
        override fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx> {
            val consumedSize = size.fold(0.ipx) { a, b -> a + b }
            val gapSize = if (size.isNotEmpty()) {
                (totalSize - consumedSize) / size.size
            } else {
                0.ipx
            }
            val positions = mutableListOf<IntPx>()
            var current = gapSize / 2
            size.forEach {
                positions.add(current)
                current += it + gapSize
            }
            return positions
        }
    }
}

/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
enum class MainAxisSize {
    /**
     * Minimize the amount of main axis free space, subject to the incoming layout constraints.
     */
    Min,
    /**
     * Maximize the amount of main axis free space, subject to the incoming layout constraints.
     */
    Max
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
enum class CrossAxisAlignment {
    /**
     * Place children such that their center is in the middle of the cross axis.
     */
    Center,
    /**
     * Place children such that their start edge is aligned to the start edge of the cross
     * axis. TODO(popam): Consider rtl directionality.
     */
    Start,
    /**
     * Place children such that their end edge is aligned to the end edge of the cross
     * axis. TODO(popam): Consider rtl directionality.
     */
    End,
    /**
     * Force children to occupy the entire cross axis space.
     */
    Stretch,
    /**
     * Align children by their baseline. TODO(popam): support this when baseline support is
     * added in ComplexMeasureBox.
     */
    Baseline
}

/**
 * Box [Constraints], but which abstract away width and height in favor of main axis and cross axis.
 */
private data class OrientationIndependentConstraints(
    var mainAxisMin: IntPx,
    var mainAxisMax: IntPx,
    var crossAxisMin: IntPx,
    var crossAxisMax: IntPx
) {
    constructor(c: Constraints, orientation: FlexOrientation) : this(
        if (orientation == FlexOrientation.Horizontal) c.minWidth else c.minHeight,
        if (orientation == FlexOrientation.Horizontal) c.maxWidth else c.maxHeight,
        if (orientation == FlexOrientation.Horizontal) c.minHeight else c.minWidth,
        if (orientation == FlexOrientation.Horizontal) c.maxHeight else c.maxWidth
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
    fun toBoxConstraints(orientation: FlexOrientation) =
        if (orientation == FlexOrientation.Horizontal) {
            Constraints(mainAxisMin, mainAxisMax, crossAxisMin, crossAxisMax)
        } else {
            Constraints(crossAxisMin, crossAxisMax, mainAxisMin, mainAxisMax)
        }

    // Given an orientation, resolves the max width constraint this instance represents.
    fun maxWidth(orientation: FlexOrientation) =
        if (orientation == FlexOrientation.Horizontal) {
            mainAxisMax
        } else {
            crossAxisMax
        }

    // Given an orientation, resolves the max height constraint this instance represents.
    fun maxHeight(orientation: FlexOrientation) =
        if (orientation == FlexOrientation.Horizontal) {
            crossAxisMax
        } else {
            mainAxisMax
        }
}

private val Measurable.flex: Float get() = (parentData as FlexInfo).flex
private val Measurable.fit: FlexFit get() = (parentData as FlexInfo).fit

/**
 * Layout model that places its children in a horizontal or vertical sequence, according to the
 * specified orientation, while also looking at the flex weights of the children.
 */
@Composable
private fun Flex(
    orientation: FlexOrientation,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    @Children(composable = false) block: FlexChildren.() -> Unit
) {
    fun Placeable.mainAxisSize() = if (orientation == FlexOrientation.Horizontal) width else height
    fun Placeable.crossAxisSize() = if (orientation == FlexOrientation.Horizontal) height else width

    val flexChildren: @Composable() () -> Unit = with(FlexChildren()) {
        block()
        val composable = @Composable {
            childrenList.forEach { it() }
        }
        composable
    }
    ComplexLayout(flexChildren) {
        layout { children, outerConstraints ->
            val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

            var totalFlex = 0f
            var inflexibleSpace = IntPx.Zero
            var crossAxisSpace = IntPx.Zero

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
                    placeables[i] = placeable
                }
            }

            // Then measure the rest according to their flexes in the remaining main axis space.
            val targetSpace = if (mainAxisSize == MainAxisSize.Max) {
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
            val mainAxisLayoutSize = if (constraints.mainAxisMax != IntPx.Infinity &&
                mainAxisSize == MainAxisSize.Max
            ) {
                constraints.mainAxisMax
            } else {
                max(inflexibleSpace + flexibleSpace, constraints.mainAxisMin)
            }
            val crossAxisLayoutSize = max(crossAxisSpace, constraints.crossAxisMin)
            val layoutWidth = if (orientation == FlexOrientation.Horizontal) {
                mainAxisLayoutSize
            } else {
                crossAxisLayoutSize
            }
            val layoutHeight = if (orientation == FlexOrientation.Horizontal) {
                crossAxisLayoutSize
            } else {
                mainAxisLayoutSize
            }
            layoutResult(layoutWidth, layoutHeight) {
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
                            IntPx.Zero /* TODO(popam): support baseline */
                        }
                    }
                    if (orientation == FlexOrientation.Horizontal) {
                        placeable.place(mainAxisPositions[index], crossAxis)
                    } else {
                        placeable.place(crossAxis, mainAxisPositions[index])
                    }
                }
            }
        }

        minIntrinsicWidth { children, availableHeight ->
            intrinsicSize(
                children,
                { h -> minIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                orientation,
                FlexOrientation.Horizontal
            )
        }

        minIntrinsicHeight { children, availableWidth ->
            intrinsicSize(
                children,
                { w -> minIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                orientation,
                FlexOrientation.Vertical
            )
        }

        maxIntrinsicWidth { children, availableHeight ->
            intrinsicSize(
                children,
                { h -> maxIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                orientation,
                FlexOrientation.Horizontal
            )
        }

        maxIntrinsicHeight { children, availableWidth ->
            intrinsicSize(
                children,
                { w -> maxIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                orientation,
                FlexOrientation.Vertical
            )
        }
    }
}

private fun intrinsicSize(
    children: List<Measurable>,
    intrinsicMainSize: Measurable.(IntPx) -> IntPx,
    intrinsicCrossSize: Measurable.(IntPx) -> IntPx,
    crossAxisAvailable: IntPx,
    flexOrientation: FlexOrientation,
    intrinsicOrientation: FlexOrientation
) = if (flexOrientation == intrinsicOrientation) {
    intrinsicMainAxisSize(children, intrinsicMainSize, crossAxisAvailable)
} else {
    intrinsicCrossAxisSize(children, intrinsicCrossSize, intrinsicMainSize, crossAxisAvailable)
}

private fun intrinsicMainAxisSize(
    children: List<Measurable>,
    mainAxisSize: Measurable.(IntPx) -> IntPx,
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
    children: List<Measurable>,
    mainAxisSize: Measurable.(IntPx) -> IntPx,
    crossAxisSize: Measurable.(IntPx) -> IntPx,
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
