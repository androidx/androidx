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
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.dp
import androidx.ui.core.min
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.px
import androidx.ui.core.toRoundedPixels
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

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
 * Layout widget which takes a child composable and applies whitespace padding around it.
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 *     <Row>
 *         <Padding padding=EdgeInsets(right=20.dp)>
 *             <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height= 20.dp />
 *         </Padding>
 *         <Padding padding=EdgeInsets(left=20.dp)>
 *             <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height= 20.dp />
 *         </Padding>
 *     </Row>
 */
@Composable
fun Padding(
    padding: EdgeInsets,
    @Children children: () -> Unit
) {
    <MeasureBox> constraints ->
        val measurable = collect(children).firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val paddingLeft = padding.left.toRoundedPixels()
            val paddingTop = padding.top.toRoundedPixels()
            val paddingRight = padding.right.toRoundedPixels()
            val paddingBottom = padding.bottom.toRoundedPixels()
            val horizontalPadding = (paddingLeft + paddingRight).px
            val verticalPadding = (paddingTop + paddingBottom).px

            val newConstraints = Constraints(
                minWidth = (constraints.minWidth - horizontalPadding).coerceAtLeast(0.px),
                maxWidth = (constraints.maxWidth - horizontalPadding).coerceAtLeast(0.px),
                minHeight = (constraints.minHeight - verticalPadding).coerceAtLeast(0.px),
                maxHeight = (constraints.maxHeight - verticalPadding).coerceAtLeast(0.px)
            )
            val placeable = measurable.measure(newConstraints)
            val width =
                min(placeable.width.px + horizontalPadding, constraints.maxWidth).toRoundedPixels()
            val height =
                min(placeable.height.px + verticalPadding, constraints.maxHeight).toRoundedPixels()

            layout(width, height) {
                placeable.place(paddingLeft, paddingTop)
            }
        }
    </MeasureBox>
}
