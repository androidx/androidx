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

import androidx.ui.core.DensityReceiver
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.px

/**
 * Contains sizes of all four corner sizes for a shape resolved in Pixels
 *
 * @param topLeft a size for the top left corner
 * @param topRight a size for the top right corner
 * @param bottomRight a size for the bottom left corner
 * @param bottomLeft a size for the bottom right corner
 */
data class PxCornerSizes(
    val topLeft: Px,
    val topRight: Px,
    val bottomRight: Px,
    val bottomLeft: Px
) {
    init {
        if (topLeft < 0.px || topRight < 0.px || bottomRight < 0.px || bottomLeft < 0.px) {
            throw IllegalArgumentException("Corner size in Px can't be negative!")
        }
    }
}

/**
 * @return true if all the sizes are equals to 0 pixels.
 */
/*inline*/ fun PxCornerSizes.isEmpty() =
    topLeft + topRight + bottomLeft + bottomRight == 0.px

/**
 * @param corners define all four corner sizes
 * @param size the size of the shape
 *
 * @return resolved [PxCornerSizes].
 */
/*inline*/ fun DensityReceiver.PxCornerSizes(
    corners: CornerSizes,
    size: PxSize
): PxCornerSizes = with(corners) {
    PxCornerSizes(
        topLeft = topLeft(size),
        topRight = topRight(size),
        bottomRight = bottomRight(size),
        bottomLeft = bottomLeft(size)
    )
}
