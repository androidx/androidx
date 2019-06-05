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

package androidx.ui.material.shape

import androidx.ui.baseui.shape.Border
import androidx.ui.baseui.shape.corner.CornerBasedShape
import androidx.ui.baseui.shape.corner.CornerSizes
import androidx.ui.baseui.shape.corner.PxCornerSizes
import androidx.ui.core.DensityReceiver
import androidx.ui.core.PxSize
import androidx.ui.engine.geometry.Outline
import androidx.ui.painting.Path

/**
 * A shape describing the rectangle with cut corners.
 * Corner size is representing the cut length - the size of both legs of the cut's right triangle.
 *
 * @param corners define all four corner sizes
 * @param border optional border to draw on top of the shape
 */
data class CutCornerShape(
    val corners: CornerSizes,
    override val border: Border? = null
) : CornerBasedShape(corners) {

    override fun DensityReceiver.createOutline(corners: PxCornerSizes, size: PxSize) =
        Outline.Generic(Path().apply {
            var cornerSize = corners.topLeft.value
            moveTo(0f, cornerSize)
            lineTo(cornerSize, 0f)
            cornerSize = corners.topRight.value
            lineTo(size.width.value - cornerSize, 0f)
            lineTo(size.width.value, cornerSize)
            cornerSize = corners.bottomRight.value
            lineTo(size.width.value, size.height.value - cornerSize)
            lineTo(size.width.value - cornerSize, size.height.value)
            cornerSize = corners.bottomLeft.value
            lineTo(cornerSize, size.height.value)
            lineTo(0f, size.height.value - cornerSize)
            close()
        })
}
