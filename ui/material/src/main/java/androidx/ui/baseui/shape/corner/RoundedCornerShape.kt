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

package androidx.ui.baseui.shape.corner

import androidx.ui.baseui.shape.Border
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius

/**
 * A shape describing the rectangle with rounded corners.
 *
 * @param corners define all four corner sizes
 * @param border optional border to draw on top of the shape
 */
data class RoundedCornerShape(
    val corners: CornerSizes,
    override val border: Border? = null
) : CornerBasedShape(corners) {

    override fun DensityReceiver.createOutline(corners: PxCornerSizes, size: PxSize) =
        Outline.Rounded(
            RRect(
                rect = size.toRect(),
                topLeft = corners.topLeft.toRadius(),
                topRight = corners.topRight.toRadius(),
                bottomRight = corners.bottomRight.toRadius(),
                bottomLeft = corners.bottomLeft.toRadius()
            )
        )

    private /*inline*/ fun Px.toRadius() = Radius.circular(this.value)
}

/**
 * A shape describing the rectangle with rounded corners.
 *
 * @param corner size to apply for all four corners
 * @param border optional border to draw on top of the shape
 */
fun RoundedCornerShape(corner: CornerSize, border: Border? = null) =
    RoundedCornerShape(CornerSizes(corner), border)

/**
 * Circular [Shape] with all the corners sized as the 50 percent of the shape size.
 *
 * @param border optional border to draw on top of the shape
 */
fun CircleShape(border: Border? = null) =
    RoundedCornerShape(CornerSizes(CornerSize(50)), border)
