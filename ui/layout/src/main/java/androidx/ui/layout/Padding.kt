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

import androidx.ui.core.Dp
import androidx.ui.core.Layout
import androidx.ui.core.dp
import androidx.ui.core.min
import androidx.ui.core.offset
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Describes a set of offsets from each of the four sides of a box. For example,
 * it is used to describe padding for the [Padding] widget.
 */
data class EdgeInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}

/**
 * Layout widget that takes a child composable and applies whitespace padding around it.
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 *     Row {
 *         Padding(padding=EdgeInsets(right=20.dp)) {
 *             SizedRectangle(color=Color(0xFFFF0000.toInt()), width=20.dp, height= 20.dp)
 *         }
 *     }
 */
@Composable
fun Padding(
    padding: EdgeInsets,
    @Children children: @Composable() () -> Unit
) {
    Layout(layoutBlock = { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) { }
        } else {
            val paddingLeft = padding.left.toIntPx()
            val paddingTop = padding.top.toIntPx()
            val paddingRight = padding.right.toIntPx()
            val paddingBottom = padding.bottom.toIntPx()
            val horizontalPadding = (paddingLeft + paddingRight)
            val verticalPadding = (paddingTop + paddingBottom)

            val newConstraints = constraints.offset(-horizontalPadding, -verticalPadding)
            val placeable = measurable.measure(newConstraints)
            val width =
                min(placeable.width + horizontalPadding, constraints.maxWidth)
            val height =
                min(placeable.height + verticalPadding, constraints.maxHeight)

            layout(width, height) {
                placeable.place(paddingLeft, paddingTop)
            }
        }
    }, children = children)
}

/**
 * Layout widget that takes a child composable and applies whitespace padding around it.
 *
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 *     Padding(left=20.dp, right=20.dp) {
 *         SizedRectangle(color=Color(0xFFFF0000.toInt()), width=20.dp, height= 20.dp)
 *     }
 */
@Composable
fun Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    Padding(
        padding = EdgeInsets(left = left, top = top, right = right, bottom = bottom),
        children = children
    )
}

/**
 * Layout widget that takes a child composable and applies
 * the same amount of whitespace padding around it.
 *
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 *     Padding(padding=20.dp) {
 *         SizedRectangle(color=Color(0xFFFF0000.toInt()), width=20.dp, height= 20.dp)
 *     }
 */
@Composable
fun Padding(
    padding: Dp,
    @Children children: @Composable() () -> Unit
) {
    Padding(padding = EdgeInsets(padding), children = children)
}
