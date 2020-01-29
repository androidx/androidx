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

package androidx.ui.foundation.shape.corner

import androidx.ui.graphics.Outline
import androidx.ui.graphics.Shape
import androidx.ui.unit.Density
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.px
import androidx.ui.unit.toRect

/**
 * Base class for [Shape]s defined by four [CornerSize]s.
 *
 * @see RoundedCornerShape for an example of the usage.
 *
 * @param topLeft a size of the top left corner
 * @param topRight a size of the top right corner
 * @param bottomRight a size of the bottom left corner
 * @param bottomLeft a size of the bottom right corner
 */
abstract class CornerBasedShape(
    private val topLeft: CornerSize,
    private val topRight: CornerSize,
    private val bottomRight: CornerSize,
    private val bottomLeft: CornerSize
) : Shape {

    final override fun createOutline(size: PxSize, density: Density): Outline {
        val topLeft = topLeft.toPx(size, density)
        val topRight = topRight.toPx(size, density)
        val bottomRight = bottomRight.toPx(size, density)
        val bottomLeft = bottomLeft.toPx(size, density)
        require(topLeft >= 0.px && topRight >= 0.px && bottomRight >= 0.px && bottomLeft >= 0.px) {
            "Corner size in Px can't be negative!"
        }
        return if (topLeft + topRight + bottomLeft + bottomRight == 0.px) {
            Outline.Rectangle(size.toRect())
        } else {
            createOutline(size, topLeft, topRight, bottomRight, bottomLeft)
        }
    }

    /**
     * Creates [Outline] of this shape for the given [size].
     *
     * @param size the size of the shape boundary.
     * @param topLeft the resolved size of the top left corner
     * @param topRight the resolved size for the top right corner
     * @param bottomRight the resolved size for the bottom left corner
     * @param bottomLeft the resolved size for the bottom right corner
     */
    abstract fun createOutline(
        size: PxSize,
        topLeft: Px,
        topRight: Px,
        bottomRight: Px,
        bottomLeft: Px
    ): Outline
}
