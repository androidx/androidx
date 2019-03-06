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
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureBox
import androidx.ui.core.Placeable
import androidx.ui.core.div
import androidx.ui.core.max
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.times
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Collects information about the children of a [FlexColumn] or [FlexColumn]
 * when its body is executed with a [FlexChildren] instance as argument.
 * TODO(popam): make this the receiver scope of the Flex lambda
 */
class FlexChildren internal constructor(private val collect: (() -> Unit) -> List<Measurable>) {
    internal var flexSum = 0f
    private val _flexChildren = mutableListOf<FlexChild>()
    internal val flexChildren: List<FlexChild>
        get() = _flexChildren

    fun expanded(flex: Float, children: @Composable() () -> Unit) {
        addChildren(flex, FlexFit.Tight, children)
    }

    fun flexible(flex: Float, children: @Composable() () -> Unit) {
        addChildren(flex, FlexFit.Loose, children)
    }

    fun inflexible(children: @Composable() () -> Unit) {
        addChildren(0f, FlexFit.Loose, children)
    }

    private fun addChildren(flex: Float, fit: Int /*FlexFit*/, children: @Composable() () -> Unit) {
        flexSum += flex
        val measurables = collect(children)
        measurables.forEach { measurable ->
            _flexChildren.add(FlexChild(measurable, flex / measurables.size, fit))
        }
    }
}

internal data class FlexChild(
    val measurable: Measurable,
    val flex: Float,
    val fit: Int
) {
    lateinit var placeable: Placeable
}

/**
 * A widget that places its children in a horizontal sequence, assigning children widths
 * according to their flex weights.
 *
 * [FlexRow] children can be:
 * - [FlexChildren.inflexible] meaning that the child is not flex, and it should be measured with
 * loose constraints to determine its preferred width
 * - [FlexChildren.expanded] meaning that the child is flexible, and it should be assigned a
 * width according to its flex weight relative to its flexible children. The child is forced
 * to occupy the entire width assigned by the parent
 * - [FlexChildren.flexible] similar to [FlexChildren.expanded], but the child can leave
 * unoccupied width.
 *
 * Example usage:
 *     <RowFlex> children ->
 *         children.expanded(/*flex=*/2f) {
 *             <Center>
 *                 <SizedRectangle color=Color(0xFF0000FF.toInt()) width = 40.dp height = 40.dp />
 *             </Center>
 *         }
 *         children.inflexible {
 *             <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp />
 *         }
 *         children.expanded(/*flex=*/1f) {
 *             <SizedRectangle color=Color(0xFF00FF00.toInt()) />
 *         }
 *     </RowFlex>
 */
@Composable
fun FlexRow(
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
    @Children(composable = false) block: FlexChildren.() -> Unit
) {
    <Flex orientation=FlexOrientation.Horizontal crossAxisAlignment block />
}

/**
 * A widget that places its children in a vertical sequence, assigning children heights
 * according to their flex weights.
 *
 * [FlexRow] children can be:
 * - [FlexChildren.inflexible] meaning that the child is not flex, and it should be measured with
 * loose constraints to determine its preferred height
 * - [FlexChildren.expanded] meaning that the child is flexible, and it should be assigned a
 * height according to its flex weight relative to its flexible children. The child is forced
 * to occupy the entire height assigned by the parent
 * - [FlexChildren.flexible] similar to [FlexChildren.expanded], but the child can leave
 * unoccupied height.
 *
 * Example usage:
 *     <ColumnFlex> children ->
 *         children.expanded(/*flex=*/2f) {
 *             <Center>
 *                 <SizedRectangle color=Color(0xFF0000FF.toInt()) width = 40.dp height = 40.dp />
 *             </Center>
 *         }
 *         children.inflexible {
 *             <SizedRectangle color=Color(0xFFFF0000.toInt()) height=40.dp />
 *         }
 *         children.expanded(/*flex=*/1f) {
 *             <SizedRectangle color=Color(0xFF00FF00.toInt()) />
 *         }
 *     </ColumnFlex>
 */
@Composable
fun FlexColumn(
    crossAxisAlignment: Int = CrossAxisAlignment.Center,
    @Children(composable=false) block: FlexChildren.() -> Unit
) {
    <Flex orientation=FlexOrientation.Vertical crossAxisAlignment block />
}

/**
 * A widget that places its children in a horizontal sequence.
 *
 * Example usage:
 *   <Row>
 *       <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
 *       <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp height=80.dp />
 *       <SizedRectangle color=Color(0xFF00FF00.toInt()) width=80.dp height=70.dp />
 *   </Row>
 */
@Composable
fun Row(crossAxisAlignment: Int = CrossAxisAlignment.Center, @Children block: () -> Unit) {
    <FlexRow crossAxisAlignment>
        inflexible {
            <block />
        }
    </FlexRow>
}

/**
 * A widget that places its children in a vertical sequence.
 *
 * Example usage:
 *   <Column>
 *       <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
 *       <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp height=80.dp />
 *       <SizedRectangle color=Color(0xFF00FF00.toInt()) width=80.dp height=70.dp />
 *   </Column>
 */
@Composable
fun Column(crossAxisAlignment: Int = CrossAxisAlignment.Center, @Children block: () -> Unit) {
    <FlexColumn crossAxisAlignment>
        inflexible {
            <block />
        }
    </FlexColumn>
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

    // Creates a new instance with the same cross axis constraints and unbounded main axis.
    fun stretchCrossAxis() = OrientationIndependentConstraints(
        mainAxisMin, mainAxisMax, crossAxisMax, crossAxisMax
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

/**
 * Layout model that places its children in a horizontal or vertical sequence, according to the
 * specified orientation, while also looking at the flex weights of the children.
 */
@Composable
private fun Flex(
    orientation: Int /*FlexOrientation*/,
    crossAxisAlignment: Int /*CrossAxisAlignment*/ = CrossAxisAlignment.Center,
    @Children(composable = false) block: FlexChildren.() -> Unit
) {
    fun Placeable.mainAxisSize() = if (orientation == FlexOrientation.Horizontal) width else height
    fun Placeable.crossAxisSize() = if (orientation == FlexOrientation.Horizontal) height else width

    <MeasureBox> constraints ->
        val constraints = OrientationIndependentConstraints(constraints, orientation)

        var totalFlex: Float /* TODO(popam): make these val when compiler bug is fixed */
        var children: List<FlexChild>
        with(FlexChildren(::collect)) {
            block(this)
            totalFlex = this.flexSum
            children = this.flexChildren
        }

        // First measure children with zero flex.
        children.filter { it.flex == 0f }.forEach { child ->
            child.placeable = child.measurable.measure(
                // Ask for preferred main axis size.
                constraints.looseMainAxis().let {
                    if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                        it.stretchCrossAxis()
                    } else {
                        it
                    }
                }.toBoxConstraints(orientation)
            )
        }

        // Then measure the rest according to their flexes in the remaining main axis space.
        val remainingSpace = constraints.mainAxisMax - children.filter { it.flex == 0f }
                .fold(IntPx.Zero) { sum, c -> sum + c.placeable.mainAxisSize() }
        children.filter { it.flex > 0f }.forEach { child ->
            val childMainAxisSize = remainingSpace * child.flex / totalFlex
            child.placeable = child.measurable.measure(
                OrientationIndependentConstraints(
                    if (child.fit == FlexFit.Tight) childMainAxisSize else IntPx.Zero,
                    childMainAxisSize,
                    if (crossAxisAlignment == CrossAxisAlignment.Stretch) {
                        constraints.crossAxisMax
                    } else {
                        constraints.crossAxisMin
                    },
                    constraints.crossAxisMax
                ).toBoxConstraints(orientation)
            )
        }

        // Compute the Flex size and position the children.
        val mainAxisLayoutSize = if (constraints.mainAxisMax != IntPx.Infinity) {
            constraints.mainAxisMax
        } else {
            max(
                children.fold(IntPx.Zero) { a, b -> a + b.placeable.mainAxisSize() },
                constraints.mainAxisMin
            )
        }
        val crossAxisLayoutSize = max(
            children.fold(IntPx.Zero) { a, b -> max(a, b.placeable.crossAxisSize()) },
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
            var consumedMainAxisSpace = IntPx.Zero
            children.forEach { child ->
                val crossAxis = when (crossAxisAlignment) {
                    CrossAxisAlignment.Start -> IntPx.Zero
                    CrossAxisAlignment.Stretch -> IntPx.Zero
                    CrossAxisAlignment.End -> {
                        crossAxisLayoutSize - child.placeable.crossAxisSize()
                    }
                    CrossAxisAlignment.Center -> {
                        Alignment.Center.align(
                            IntPxSize(mainAxisLayoutSize - child.placeable.mainAxisSize(),
                            crossAxisLayoutSize - child.placeable.crossAxisSize())
                        ).y
                    }
                    else -> { IntPx.Zero /* TODO(popam): support baseline and use enum */}
                }
                if (orientation == FlexOrientation.Horizontal) {
                    child.placeable.place(consumedMainAxisSpace, crossAxis)
                } else {
                    child.placeable.place(crossAxis, consumedMainAxisSpace)
                }
                consumedMainAxisSpace += child.placeable.mainAxisSize()
            }
        }
    </MeasureBox>
}
