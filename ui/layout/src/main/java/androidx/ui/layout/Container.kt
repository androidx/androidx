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
import androidx.ui.core.Draw
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.px
import androidx.ui.core.toPx
import androidx.ui.core.withTight
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * A convenience widget that combines common layout and painting widgets for one child:
 * - padding: the padding to be applied to the child
 * - color: the background color drawn under the padded child
 * - alignment: how to position the padded child if the [Container] is larger than the child
 * - constraints: additional constraints to be enforced when measuring the padded child
 * - width: the width to be enforced to the padded child
 * - height: the height to be enforced to the padded child
 * - margin: padding to be applied to the [Container] itself (the margin is not applied to the
 *   child). Note that the margin is not part of the given constraints, width or height arguments.
 *
 * The [Container] will be as large as possible for bounded constraints (after the given
 * constraints, width and height are considered) - if the padded child is smaller, the alignment
 * will be used to position it. For unbounded constraints, the [Container] will wrap its child.
 * Also, note that the measurement information passed for the [Container] (constraints, width
 * and height) will not be satisfied if the incoming [Constraints] do not allow
 * it.
 */
@Composable
fun Container(
    padding: EdgeInsets? = null,
    color: Color? = null,
    alignment: Alignment? = null,
    margin: EdgeInsets? = null,
    constraints: Constraints? = null,
    width: Dp? = null,
    height: Dp? = null,
    @Children() children: () -> Unit
) {
    var container = children

    if (padding != null) {
        val childContainer = container
        container = @Composable {
            <Padding padding=padding>
                <childContainer />
            </Padding>
        }
    }

    run {
        // Center child in Container.
        val childContainer = container
        container = @Composable {
            <Align alignment=(alignment ?: Alignment.Center)>
                <childContainer />
            </Align>
        }
    }

    if (color != null) {
        val childContainer = container
        container = @Composable {
            <Draw> canvas, parentSize ->
                val paint = Paint()
                paint.color = color
                paint.style = PaintingStyle.fill
                canvas.drawRect(Rect(0f, 0f, parentSize.width, parentSize.height), paint)
            </Draw>

            <childContainer />
        }
    }

    if (constraints != null || width != null || height != null) {
        <DensityConsumer> density ->
            val additionalConstraints =
                (constraints ?: Constraints()).withTight(
                    width?.toPx(density)?.px,
                    height?.toPx(density)?.px
                )
            val childContainer = container
            container = @Composable {
                <ConstrainedBox additionalConstraints>
                    <childContainer />
                </ConstrainedBox>
            }
        </DensityConsumer>
    }

    if (margin != null) {
        val childContainer = container
        container = @Composable {
            <Padding padding=margin>
                <childContainer />
            </Padding>
        }
    }

    <container />
}
