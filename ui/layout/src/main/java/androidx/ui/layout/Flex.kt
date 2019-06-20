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
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.ParentData
import androidx.ui.core.Placeable
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.isFinite

/**
 * Parent data associated with children to assign flex and fit values for them.
 */
private data class FlexInfo(val flex: Float, val fit: Int)

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
    mainAxisAlignment: Int = MainAxisAlignment.Start,
    mainAxisSize: Int = MainAxisSize.Max,
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
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
    mainAxisAlignment: Int = MainAxisAlignment.Start,
    mainAxisSize: Int = MainAxisSize.Max,
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
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
    mainAxisAlignment: Int = MainAxisAlignment.Start,
    mainAxisSize: Int = MainAxisSize.Max,
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
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
    mainAxisAlignment: Int = MainAxisAlignment.Start,
    mainAxisSize: Int = MainAxisSize.Max,
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
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

// TODO(popam): convert this to enum when possible
internal class FlexFit {
    companion object {
        val Tight = 0
        val Loose = 1
    }
}

// TODO(popam): make orientation a shared enum for layouts if needed in multiple places
internal class FlexOrientation {
    companion object {
        val Horizontal = 0
        val Vertical = 1
    }
}

// TODO(popam): convert this to enum when possible
/**
 * Used to specify the alignment of a layout's children, in main axis direction.
 */
class MainAxisAlignment {
    companion object {
        /**
         * Place children such that they are as close as possible to the middle of the main axis.
         */
        val Center = 0
        /**
         * Place children such that they are as close as possible to the start of the main axis.
         * TODO(popam): Consider rtl directionality.
         */
        val Start = 1
        /**
         * Place children such that they are as close as possible to the end of the main axis.
         */
        val End = 2
        /**
         * Place children such that they are spaced evenly across the main axis, including free
         * space before the first child and after the last child.
         */
        val SpaceEvenly = 3
        /**
         * Place children such that they are spaced evenly across the main axis, without free
         * space before the first child or after the last child.
         */
        val SpaceBetween = 4
        /**
         * Place children such that they are spaced evenly across the main axis, including free
         * space before the first child and after the last child, but half the amount of space
         * existing otherwise between two consecutive children.
         */
        val SpaceAround = 5
        // TODO(popam): get rid of this array when MainAxisAlignment becomes enum
        internal val values = arrayOf(
            MainAxisCenterAligner(),
            MainAxisStartAligner(),
            MainAxisEndAligner(),
            MainAxisSpaceEvenlyAligner(),
            MainAxisSpaceBetweenAligner(),
            MainAxisSpaceAroundAligner()
        )

        internal interface Aligner {
            fun align(totalSize: IntPx, size: List<IntPx>): List<IntPx>
        }

        internal class MainAxisCenterAligner : Aligner {
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

        internal class MainAxisStartAligner : Aligner {
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

        internal class MainAxisEndAligner : Aligner {
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

        internal class MainAxisSpaceEvenlyAligner : Aligner {
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

        internal class MainAxisSpaceBetweenAligner : Aligner {
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

        internal class MainAxisSpaceAroundAligner : Aligner {
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
}

// TODO(popam): convert this to enum when possible
/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
class MainAxisSize {
    companion object {
        /**
         * Minimize the amount of main axis free space, subject to the incoming layout constraints.
         */
        val Min = 0
        /**
         * Maximize the amount of main axis free space, subject to the incoming layout constraints.
         */
        val Max = 1
    }
}

// TODO(popam): convert this to enum when possible
/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
class CrossAxisAlignment {
    companion object {
        /**
         * Place children such that their center is in the middle of the cross axis.
         */
        val Center = 0
        /**
         * Place children such that their start edge is aligned to the start edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        val Start = 1
        /**
         * Place children such that their end edge is aligned to the end edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        val End = 2
        /**
         * Force children to occupy the entire cross axis space.
         */
        val Stretch = 3
        /**
         * Align children by their baseline. TODO(popam): support this when baseline support is
         * added in ComplexMeasureBox.
         */
        val Baseline = 4
    }
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
    constructor(c: Constraints, orientation: Int) : this(
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
    fun toBoxConstraints(orientation: Int) = if (orientation == FlexOrientation.Horizontal) {
        Constraints(mainAxisMin, mainAxisMax, crossAxisMin, crossAxisMax)
    } else {
        Constraints(crossAxisMin, crossAxisMax, mainAxisMin, mainAxisMax)
    }

    // Given an orientation, resolves the max width constraint this instance represents.
    fun maxWidth(orientation: Int) = if (orientation == FlexOrientation.Horizontal) {
        mainAxisMax
    } else {
        crossAxisMax
    }

    // Given an orientation, resolves the max height constraint this instance represents.
    fun maxHeight(orientation: Int) = if (orientation == FlexOrientation.Horizontal) {
        crossAxisMax
    } else {
        mainAxisMax
    }
}

private val Measurable.flex: Float get() = (parentData as FlexInfo).flex
private val Measurable.fit: Int get() = (parentData as FlexInfo).fit

/**
 * Layout model that places its children in a horizontal or vertical sequence, according to the
 * specified orientation, while also looking at the flex weights of the children.
 */
@Composable
private fun Flex(
    orientation: Int /*FlexOrientation*/,
    mainAxisSize: Int /*MainAxisSize*/ = MainAxisSize.Max,
    mainAxisAlignment: Int /*MainAxisAlignment*/ = MainAxisAlignment.Start,
    crossAxisAlignment: Int /*CrossAxisAlignment*/ = CrossAxisAlignment.Center,
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
    Layout(children = flexChildren, layoutBlock = { children, outerConstraints ->

        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

        val totalFlex = children.sumByDouble { it.flex.toDouble() }.toFloat()

        val placeables = mutableMapOf<Measurable, Placeable>()
        // First measure children with zero flex.
        children.filter { it.flex == 0f }.forEach { child ->
            placeables[child] = child.measure(
                // Ask for preferred main axis size.
                constraints.looseMainAxis().let {
                    if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                        it.stretchCrossAxis()
                    } else {
                        it.copy(crossAxisMin = IntPx.Zero)
                    }
                }.toBoxConstraints(orientation)
            )
        }

        // Then measure the rest according to their flexes in the remaining main axis space.

        val inflexibleSpace = children.filter { it.flex == 0f }
            .fold(IntPx.Zero) { sum, c -> sum + placeables[c]!!.mainAxisSize() }
        val targetSpace = if (mainAxisSize == MainAxisSize.Max) {
            constraints.mainAxisMax
        } else {
            constraints.mainAxisMin
        }
        children.filter { it.flex > 0f }.forEach { child ->
            val childMainAxisSize = max(
                IntPx.Zero,
                (targetSpace - inflexibleSpace) * child.flex / totalFlex
            )
            placeables[child] = child.measure(
                OrientationIndependentConstraints(
                    if (child.fit == FlexFit.Tight) childMainAxisSize else IntPx.Zero,
                    childMainAxisSize,
                    if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                        constraints.crossAxisMax
                    } else {
                        IntPx.Zero
                    },
                    constraints.crossAxisMax
                ).toBoxConstraints(orientation)
            )
        }

        // Compute the Flex size and position the children.
        val mainAxisLayoutSize = if (constraints.mainAxisMax != IntPx.Infinity &&
            mainAxisSize == MainAxisSize.Max
        ) {
            constraints.mainAxisMax
        } else {
            max(
                children.fold(IntPx.Zero) { a, b -> a + placeables[b]!!.mainAxisSize() },
                constraints.mainAxisMin
            )
        }
        val crossAxisLayoutSize = max(
            children.fold(IntPx.Zero) { a, b -> max(a, placeables[b]!!.crossAxisSize()) },
            constraints.crossAxisMin
        )
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
        layout(layoutWidth, layoutHeight) {
            val childrenMainAxisSize = children.map { placeables[it]!!.mainAxisSize() }
            val mainAxisPositions = MainAxisAlignment.values[mainAxisAlignment]
                .align(mainAxisLayoutSize, childrenMainAxisSize)
            children.forEachIndexed { index, child ->
                val placeable = placeables[child]!!
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
                        IntPx.Zero /* TODO(popam): support baseline and use enum */
                    }
                }
                if (orientation == FlexOrientation.Horizontal) {
                    placeable.place(mainAxisPositions[index], crossAxis)
                } else {
                    placeable.place(crossAxis, mainAxisPositions[index])
                }
            }
        }
    })
}
