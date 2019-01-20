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
import androidx.ui.core.MeasureBox
import androidx.ui.core.Placeable
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.times
import androidx.ui.rendering.flex.FlexFit
import com.google.r4a.Children
import com.google.r4a.Composable

// TODO(popam): convert this to enum when possible
internal class FlexFit {
    companion object {
        val TIGHT = 0
        val LOOSE = 1
    }
}

internal data class FlexChild(
    val flex: Float,
    val fit: FlexFit,
    val child: @Composable() () -> Unit
)

/**
 * Collects information about the children of a [RowFlex] when its body is executed
 * with a [FlexChildren] instance as argument.
 * TODO(popam): make this the receiver scope of the RowFlex lambda
 */
class FlexChildren {
    private val _flexChildren = mutableListOf<FlexChild>()
    internal val flexChildren: List<FlexChild>
        get() = _flexChildren

    fun expanded(flex: Float, children: @Composable() () -> Unit) {
       _flexChildren.add(FlexChild(flex, FlexFit.TIGHT, children))
    }

    fun flexible(flex: Float, children: @Composable() () -> Unit) {
        _flexChildren.add(FlexChild(flex, FlexFit.LOOSE, children))
    }

    fun inflexible(children: @Composable() () -> Unit) {
        _flexChildren.add(FlexChild(0f, FlexFit.LOOSE, children))
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
fun FlexRow(@Children(composable=false) block: (children: FlexChildren) -> Unit) {
    <MeasureBox> constraints, measureOperations ->
        val children = with(FlexChildren()) {
            apply(block)
            flexChildren
        }

        val placeables = arrayOfNulls<List<Placeable>?>(children.size)
        val allocatedWidth = Array(children.size) { 0.dp }
        // First measure children with zero flex.
        (0 until children.size).filter { i -> children[i].flex == 0f }.forEach { i ->
            measureOperations.collect(children[i].child).map { measurable ->
                measureOperations.measure(
                    measurable,
                    Constraints(
                        0.dp, Float.POSITIVE_INFINITY.dp, /* ask for preferred width */
                        constraints.minHeight, constraints.maxHeight
                    )
                )
            }.also {
                allocatedWidth[i] = it.map { it.width }.fold(0.dp) { a, b -> a + b }
                placeables[i] = it
            }
        }

        // Then measure the rest according to their flexes in the remaining width.
        val remainingSpace = constraints.maxWidth - allocatedWidth.fold(0.dp) { a, b -> a + b }
        val totalFlex = children.map { it.flex }.sum()
        (0 until children.size).filter { i -> children[i].flex > 0f }.forEach { i ->
            val child = children[i]
            val measurables = measureOperations.collect(child.child)
            if (measurables.isEmpty()) {
                return@forEach
            }
            allocatedWidth[i] = remainingSpace * (child.flex / totalFlex)
            val childMaxWidth = allocatedWidth[i] / measurables.size
            measurables.map { measurable ->
                measureOperations.measure(
                    measurable,
                    Constraints(
                        if (child.fit == FlexFit.TIGHT) childMaxWidth else 0.dp,
                        childMaxWidth,
                        constraints.minHeight,
                        constraints.maxHeight
                    )
                )
            }.also {
                placeables[i] = it
            }
        }

        // Position the children.
        measureOperations.layout(constraints.maxWidth, constraints.maxHeight) {
            var currentLeft = 0.dp
            placeables.forEachIndexed { i, childPlaceables ->
                childPlaceables?.map {
                    it.place(currentLeft, 0.dp /* TODO(popam): cross axis alignment */)
                    currentLeft += allocatedWidth[i] / childPlaceables.size
                }
            }
        }
    </MeasureBox>
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
