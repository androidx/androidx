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

import androidx.ui.baseui.shape.Shape
import androidx.ui.core.DensityReceiver
import androidx.ui.core.PxSize
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Outline

/**
 * Base class for [Shape]s defined by [CornerSizes].
 *
 * @see RoundedCornerShape for an example of the usage
 *
 * @param corners define all four corner sizes
 */
abstract class CornerBasedShape(
    private val corners: CornerSizes
) : Shape {

    final override fun DensityReceiver.createOutline(size: PxSize): Outline {
        val corners = PxCornerSizes(corners, size)
        return if (corners.isEmpty()) {
            Outline.Rectangle(size.toRect())
        } else {
            createOutline(corners, size)
        }
    }

    /**
     * @param corners the resolved sizes of all the four corners in pixels.
     * @param size the size of the shape boundary.
     *
     * @return [Outline] of this shape for the given [size].
     */
    abstract fun DensityReceiver.createOutline(corners: PxCornerSizes, size: PxSize): Outline
}
