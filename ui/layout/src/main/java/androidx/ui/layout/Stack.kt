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
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.ParentData
import androidx.ui.core.Placeable
import androidx.ui.core.looseMin
import androidx.ui.core.max
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.ipx
import androidx.ui.core.isFinite

/**
 * Collects information about the children of a [Stack] when its body is executed
 * with a [StackChildren] instance as argument.
 */
class StackChildren {
    private val _stackChildren = mutableListOf<@Composable() () -> Unit>()
    internal val stackChildren: List<@Composable() () -> Unit>
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
        val data = StackChildData(
            leftInset = leftInset, topInset = topInset,
            rightInset = rightInset, bottomInset = bottomInset
        )
        _stackChildren += { ParentData(data = data, children = children) }
    }

    fun aligned(alignment: Alignment, children: @Composable() () -> Unit) {
        val data = StackChildData(alignment = alignment)
        _stackChildren += { ParentData(data = data, children = children) }
    }
}

/**
 * A widget that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Stack].
 *
 * [Stack] children can be:
 * - [aligned], which are aligned in the box of the [Stack]. These are also the children that
 * define the size of the [Stack] box: this will be the maximum between the minimum
 * constraints and the size of the largest child.
 * - [positioned], which are positioned in the box defined as above, according to
 * their specified insets. When the positioning of these is ambiguous in one direction (the
 * component has [null] left and right or top and bottom insets), the positioning in this direction
 * will be resolved according to the [Stack]'s defaultAlignment argument.
 *
 * Example usage:
 *     Stack {
 *         aligned(Alignment.Center) {
 *             SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 300.dp, height = 300.dp)
 *         }
 *         aligned(Alignment.TopLeft) {
 *             SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 150.dp, height = 150.dp)
 *         }
 *         aligned(Alignment.BottomRight) {
 *             SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 150.dp, height = 150.dp)
 *         }
 *         positioned(null, 20.dp, null, 20.dp) {
 *             SizedRectangle(color = Color(0xFFFFA500.toInt()), width = 80.dp)
 *             SizedRectangle(color = Color(0xFFA52A2A.toInt()), width = 20.dp)
 *         }
 *     }
 */
@Composable
fun Stack(
    defaultAlignment: Alignment = Alignment.Center,
    @Children(composable = false) block: StackChildren.() -> Unit
) {
    val children: @Composable() () -> Unit = with(StackChildren()) {
        apply(block)
        val composable = @Composable {
            stackChildren.forEach {
                it()
            }
        }
        composable
    }
    Layout(children = children, layoutBlock = { measurables, constraints ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure aligned children to get the size of the layout.
        (0 until measurables.size).filter { i -> !measurables[i].positioned }.forEach { i ->
            placeables[i] = measurables[i].measure(constraints.looseMin())
        }

        val (stackWidth, stackHeight) = with(placeables.filterNotNull()) {
            Pair(
                max(maxBy { it.width.value }?.width ?: IntPx.Zero, constraints.minWidth),
                max(maxBy { it.height.value }?.height ?: IntPx.Zero, constraints.minHeight)
            )
        }

        // Now measure positioned children.
        (0 until measurables.size).filter { i -> measurables[i].positioned }.forEach { i ->
            val childData = measurables[i].stackChildData
            // Obtain width constraints.
            val childMaxWidth = (stackWidth -
                    (childData.leftInset?.toIntPx() ?: IntPx.Zero) -
                    (childData.rightInset?.toIntPx() ?: IntPx.Zero)).coerceAtLeast(0.ipx)
            val childMinWidth = if (childData.leftInset != null && childData.rightInset != null &&
                    childMaxWidth.isFinite()) {
                childMaxWidth
            } else {
                IntPx.Zero
            }
            // Obtain height constraints.
            val childMaxHeight = (stackHeight -
                    (childData.topInset?.toIntPx() ?: IntPx.Zero) -
                    (childData.bottomInset?.toIntPx() ?: IntPx.Zero)).coerceAtLeast(0.ipx)
            val childMinHeight = if (childData.topInset != null && childData.bottomInset != null &&
                childMaxHeight.isFinite()) {
                childMaxHeight
            } else {
                IntPx.Zero
            }
            measurables[i].measure(
                Constraints(
                    childMinWidth, childMaxWidth, childMinHeight, childMaxHeight
                )
            ).also {
                placeables[i] = it
            }
        }

        // Position the children.
        layout(stackWidth, stackHeight) {
            (0 until measurables.size).forEach { i ->
                val measurable = measurables[i]
                val childData = measurable.stackChildData
                val placeable = placeables[i]!!

                if (!measurable.positioned) {
                    val position = (childData.alignment ?: defaultAlignment).align(
                        IntPxSize(
                            stackWidth - placeable.width,
                            stackHeight - placeable.height
                        )
                    )
                    placeable.place(position.x, position.y)
                } else {
                    val x = if (childData.leftInset != null) {
                        childData.leftInset.toIntPx()
                    } else if (childData.rightInset != null) {
                        stackWidth - childData.rightInset.toIntPx() - placeable.width
                    } else {
                        (childData.alignment ?: defaultAlignment).align(
                            IntPxSize(
                                stackWidth - placeable.width,
                                stackHeight - placeable.height
                            )
                        ).x
                    }

                    val y = if (childData.topInset != null) {
                        childData.topInset.toIntPx()
                    } else if (childData.bottomInset != null) {
                        stackHeight - childData.bottomInset.toIntPx() - placeable.height
                    } else {
                        (childData.alignment ?: defaultAlignment).align(
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
    })
}

internal data class StackChildData(
    val alignment: Alignment? = null,
    val leftInset: Dp? = null,
    val topInset: Dp? = null,
    val rightInset: Dp? = null,
    val bottomInset: Dp? = null,
    val width: Dp? = null,
    val height: Dp? = null
)

internal val Measurable.stackChildData: StackChildData get() = this.parentData as StackChildData
internal val Measurable.positioned: Boolean
    get() = with(stackChildData) {
        leftInset != null || topInset != null || rightInset != null || bottomInset != null
    }
