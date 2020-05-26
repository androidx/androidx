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

package androidx.ui.graphics.painter

import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.drawscope.DrawScope

/**
 * [Painter] implementation used to draw an [ImageAsset] into the provided canvas
 * This implementation can handle applying alpha and [ColorFilter] to it's drawn result
 *
 * @param image The [ImageAsset] to draw
 * @param srcOffset Optional offset relative to [image] used to draw a subsection of the
 * [ImageAsset]. By default this uses the origin of [image]
 * @param srcSize Optional dimensions representing size of the subsection of [image] to draw
 * Both the offset and size must have the following requirements:
 *
 * 1) Left and top bounds must be greater than or equal to zero
 * 2) Source size must be greater than zero
 * 3) Source size must be less than or equal to the dimensions of [image]
 */
data class ImagePainter(
    private val image: ImageAsset,
    private val srcOffset: Offset = Offset.zero,
    private val srcSize: Size = Size(image.width.toFloat(), image.height.toFloat())
) : Painter() {

    private val size: Size = validateSize(srcOffset, srcSize)

    private var alpha: Float = DrawScope.DefaultAlpha

    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        drawImage(
            image,
            srcOffset,
            srcSize,
            alpha = alpha,
            colorFilter = colorFilter
        )
    }

    /**
     * Return the dimension of the underlying [ImageAsset] as it's intrinsic width and height
     */
    override val intrinsicSize: Size get() = size

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    private fun validateSize(srcOffset: Offset, srcSize: Size): Size {
        require(
            srcOffset.dx >= 0 &&
            srcOffset.dy >= 0 &&
            srcSize.width >= 0 &&
            srcSize.height >= 0 &&
            srcSize.width <= image.width &&
            srcSize.height <= image.height
        )
        return srcSize
    }
}