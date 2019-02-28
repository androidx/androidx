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
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Placeable
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.max
import androidx.ui.core.minus
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Collects information about the children of a [Stack] when its body is executed
 * with a [StackChildren] instance as argument.
 * TODO(popam): make this the receiver scope of the Stack lambda
 */
class StackChildren() {
    internal val _stackChildren = mutableListOf<StackChild>()
    internal val stackChildren: List<StackChild>
        get() = _stackChildren

    fun positioned(
        leftInset: Dp? = null,
        topInset: Dp? = null,
        rightInset: Dp? = null,
        bottomInset: Dp? = null,
        children: @Composable() () -> Unit
    ) {
        require(
            leftInset != null || topInset != null || rightInset != null ||
                    bottomInset != null
        ) { "Please specify at least one inset for a positioned." }
        _stackChildren.add(
            StackChild(
                leftInset = leftInset, topInset = topInset,
                rightInset = rightInset, bottomInset = bottomInset, children = children
            )
        )
    }

    fun aligned(alignment: Alignment, children: @Composable() () -> Unit) {
        _stackChildren.add(StackChild(alignment = alignment, children = children))
    }
}

/**
 * A widget that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Stack].
 *
 * [Stack] children can be:
 * - [StackChildren.aligned], which are aligned in the box of the [Stack]. These are also the
 * children that define the size of the [Stack] box: this will be the maximum between the
 * minimum constraints and the size of the largest child.
 * - [StackChildren.positioned], which are positioned in the box defined as above, according to
 * their specified insets. When the positioning of these is ambiguous in one direction (the
 * component has [null] left and right or top and bottom insets), the positioning in this direction
 * will be resolved according to the [Stack]'s defaultAlignment argument.
 *
 * Example usage:
 *     <Stack> children ->
 *         children.aligned(Alignment.Center) {
 *             <SizedRectangle color=Color(0xFF0000FF.toInt()) width=300.dp height=300.dp />
 *         }
 *         children.aligned(Alignment.TopLeft) {
 *             <SizedRectangle color=Color(0xFF00FF00.toInt()) width=150.dp height=150.dp />
 *         }
 *         children.aligned(Alignment.BottomRight) {
 *             <SizedRectangle color=Color(0xFFFF0000.toInt()) width=150.dp height=150.dp />
 *         }
 *         children.positioned(null, 20.dp, null, 20.dp) {
 *             <SizedRectangle color=Color(0xFFFFA500.toInt()) width=80.dp />
 *             <SizedRectangle color=Color(0xFFA52A2A.toInt()) width=20.dp />
 *         }
 *     </Stack>
 */
@Composable
fun Stack(
    defaultAlignment: Alignment,
    @Children(composable = false) block: (children: StackChildren) -> Unit
) {
    <MeasureBox> constraints ->
        val children = with(StackChildren()) {
            apply(block)
            stackChildren
        }

        val placeables = arrayOfNulls<List<Placeable>?>(children.size)
        // First measure aligned children to get the size of the layout.
        (0 until children.size).filter { i -> !children[i].positioned }.forEach { i ->
            collect(children[i].children).map { measurable ->
                measurable.measure(
                    Constraints(IntPx.Zero, constraints.maxWidth, IntPx.Zero, constraints.maxHeight)
                )
            }.also {
                placeables[i] = it
            }
        }

        val (stackWidth, stackHeight) = with(placeables.filterNotNull().flatten()) {
            Pair(
                max(maxBy { it.width.value }?.width ?: IntPx.Zero, constraints.minWidth),
                max(maxBy { it.height.value }?.height ?: IntPx.Zero, constraints.minHeight)
            )
        }

        // Now measure positioned children.
        (0 until children.size).filter { i -> children[i].positioned }.forEach { i ->
            val stackChild = children[i]
            // Obtain width constraints.
            val childMaxWidth =
                stackWidth - (stackChild.leftInset?.toIntPx() ?: IntPx.Zero) -
                        (stackChild.rightInset?.toIntPx() ?: IntPx.Zero)
            val childMinWidth = if (stackChild.leftInset != null && stackChild.rightInset != null) {
                childMaxWidth
            } else {
                IntPx.Zero
            }
            // Obtain height constraints.
            val childMaxHeight =
                stackHeight - (stackChild.topInset?.toIntPx() ?: IntPx.Zero) -
                        (stackChild.bottomInset?.toIntPx() ?: IntPx.Zero)
            val childMinHeight =
                if (stackChild.topInset != null && stackChild.bottomInset != null) {
                    childMaxHeight
                } else {
                    IntPx.Zero
                }
            collect(stackChild.children).map { measurable ->
                measurable.measure(
                    Constraints(
                        childMinWidth, childMaxWidth, childMinHeight, childMaxHeight
                    )
                )
            }.also {
                placeables[i] = it
            }
        }

        // Position the children.
        layout(stackWidth, stackHeight) {
            (0 until children.size).forEach { i ->
                val stackChild = children[i]
                if (!stackChild.positioned) {
                    placeables[i]?.forEach { placeable ->
                        val position = (stackChild.alignment ?: defaultAlignment).align(
                            IntPxSize(
                                stackWidth - placeable.width,
                                stackHeight - placeable.height
                            )
                        )
                        placeable.place(position.x, position.y)
                    }
                } else {
                    placeables[i]?.forEach { placeable ->
                        val x = if (stackChild.leftInset != null) {
                            stackChild.leftInset.toIntPx()
                        } else if (stackChild.rightInset != null) {
                            stackWidth - stackChild.rightInset.toIntPx() - placeable.width
                        } else {
                            (stackChild.alignment ?: defaultAlignment).align(
                                IntPxSize(
                                    stackWidth - placeable.width,
                                    stackHeight - placeable.height
                                )
                            ).x
                        }

                        val y = if (stackChild.topInset != null) {
                            stackChild.topInset.toIntPx()
                        } else if (stackChild.bottomInset != null) {
                            stackHeight - stackChild.bottomInset.toIntPx() - placeable.height
                        } else {
                            (stackChild.alignment ?: defaultAlignment).align(
                                IntPxSize(
                                    stackWidth - placeable.width,
                                    stackHeight - placeable.height
                                )
                            ).y
                        }
                        placeable.place(x, y)
                    }
                }
            }
        }
    </MeasureBox>
}

internal data class StackChild(
    val alignment: Alignment? = null,
    val leftInset: Dp? = null,
    val topInset: Dp? = null,
    val rightInset: Dp? = null,
    val bottomInset: Dp? = null,
    val width: Dp? = null,
    val height: Dp? = null,
    val children: @Composable() () -> Unit
) {
    val positioned =
        leftInset != null || topInset != null || rightInset != null || bottomInset != null
}
