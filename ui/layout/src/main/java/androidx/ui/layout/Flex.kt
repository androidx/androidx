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
import androidx.ui.core.Dp
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.Placeable
import androidx.ui.core.Px
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.px
import androidx.ui.core.times
import androidx.ui.core.toRoundedPixels
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer
import kotlin.math.roundToInt

/**
 * Collects information about the children of a [FlexColumn] or [FlexColumn]
 * when its body is executed with a [FlexChildren] instance as argument.
 * TODO(popam): make this the receiver scope of the Flex lambda
 */
class FlexChildren {
    private val _flexChildren = mutableListOf<FlexChild>()
    internal val flexChildren: List<FlexChild>
        get() = _flexChildren

    fun expanded(flex: Float, children: @Composable() () -> Unit) {
       _flexChildren.add(FlexChild(flex, FlexFit.Tight, children))
    }

    fun flexible(flex: Float, children: @Composable() () -> Unit) {
        _flexChildren.add(FlexChild(flex, FlexFit.Loose, children))
    }

    fun inflexible(children: @Composable() () -> Unit) {
        _flexChildren.add(FlexChild(0f, FlexFit.Loose, children))
    }
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
fun FlexRow(@Children(composable=false) block: (children: FlexChildren) -> Unit) {
    <Flex orientation=FlexOrientation.Horizontal block />
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
fun FlexColumn(@Children(composable=false) block: (children: FlexChildren) -> Unit) {
    <Flex orientation=FlexOrientation.Vertical block />
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
fun Row(@Children block: () -> Unit) {
    <FlexRow> children ->
        children.inflexible {
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
fun Column(@Children block: () -> Unit) {
    <FlexColumn> children ->
        children.inflexible {
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

internal data class FlexChild(
    val flex: Float,
    val fit: Int,
    val child: @Composable() () -> Unit
)

/**
 * Box [Constraints], but which abstract away width and height in favor of main axis and cross axis.
 */
private data class OrientationIndependentConstraints(
    var mainAxisMin: Px,
    var mainAxisMax: Px,
    var crossAxisMin: Px,
    var crossAxisMax: Px
) {
    constructor(c: Constraints, orientation: Int) : this(
        if (orientation == FlexOrientation.Horizontal) c.minWidth else c.minHeight,
        if (orientation == FlexOrientation.Horizontal) c.maxWidth else c.maxHeight,
        if (orientation == FlexOrientation.Horizontal) c.minHeight else c.minWidth,
        if (orientation == FlexOrientation.Horizontal) c.maxHeight else c.maxWidth
    )

    // Creates a new instance with the same cross axis constraints and unbounded main axis.
    fun looseMainAxis() = OrientationIndependentConstraints(
        0.px, Px.Infinity, crossAxisMin, crossAxisMax
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
    @Children(composable=false) block: (children: FlexChildren) -> Unit)
{
    <MeasureBox> constraints ->
        val constraints = OrientationIndependentConstraints(constraints, orientation)

        val children = with(FlexChildren()) {
            apply(block)
            flexChildren
        }

        val placeables = arrayOfNulls<List<Placeable>?>(children.size)
        val mainAxisSize = Array(children.size) { 0 }
        // First measure children with zero flex.
        (0 until children.size).filter { i -> children[i].flex == 0f }.forEach { i ->
            collect(children[i].child).map { measurable ->
                measurable.measure(
                    // Ask for preferred main axis size.
                    constraints.looseMainAxis().toBoxConstraints(orientation)
                )
            }.also {
                mainAxisSize[i] = it.map {
                    if (orientation == FlexOrientation.Horizontal) it.width else it.height
                }.fold(0) { a, b -> a + b }
                placeables[i] = it
            }
        }

        // Then measure the rest according to their flexes in the remaining main axis space.
        val remainingSpace = constraints.mainAxisMax.toRoundedPixels() -
                mainAxisSize.fold(0) { a, b -> a + b }
        val totalFlex = children.map { it.flex }.sum()
        (0 until children.size).filter { i -> children[i].flex > 0f }.forEach { i ->
            val child = children[i]
            val measurables = collect(child.child)
            if (measurables.isEmpty()) {
                return@forEach
            }
            mainAxisSize[i] = (remainingSpace * (child.flex / totalFlex)).roundToInt()
            val childMaxMainAxisSize = mainAxisSize[i] / measurables.size
            measurables.map { measurable ->
                measurable.measure(
                    OrientationIndependentConstraints(
                        if (child.fit == FlexFit.Tight) 0.px else childMaxMainAxisSize.px,
                        childMaxMainAxisSize.px,
                        constraints.crossAxisMin,
                        constraints.crossAxisMax
                    ).toBoxConstraints(orientation)
                )
            }.also {
                placeables[i] = it
            }
        }

        // Position the children.
        val layoutWidth = constraints.maxWidth(orientation).toRoundedPixels()
        val layoutHeight = constraints.maxHeight(orientation).toRoundedPixels()
        layout(layoutWidth, layoutHeight) {
            var consumedMainAxisSpace = 0
            placeables.forEachIndexed { i, childPlaceables ->
                childPlaceables?.map {
                    if (orientation == FlexOrientation.Horizontal) {
                        it.place(consumedMainAxisSpace, 0 /*TODO(popam): cross axis alignment*/)
                    } else {
                        it.place(0 /*TODO(popam): cross axis alignment*/, consumedMainAxisSpace)
                    }
                    consumedMainAxisSpace += mainAxisSize[i] / childPlaceables.size
                }
            }
        }
    </MeasureBox>
}
