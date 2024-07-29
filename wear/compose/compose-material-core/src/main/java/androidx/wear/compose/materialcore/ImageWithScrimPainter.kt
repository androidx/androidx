/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * A painter which wraps another [Painter] for drawing a background image and a [Brush] which is
 * used to create an effect over the image to ensure that text drawn over it will be legible.
 *
 * This painter is intended for a background, and the size param if non-null will override the
 * Painters intrinsicSize.
 *
 * For more control of the background image, an image loading library like Coil should be used which
 * allows explicit contentScale handling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ImageWithScrimPainter(
    val imagePainter: Painter,
    val brush: Brush,
    private var scrimAlpha: Float = 1.0f,
    private var alpha: Float = 1.0f,
    private val forcedSize: Size? = null,
) : Painter() {

    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        val size = this.size
        with(imagePainter) { draw(size = size, alpha = alpha, colorFilter = colorFilter) }
        drawRect(brush = brush, alpha = scrimAlpha * alpha, colorFilter = colorFilter)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as ImageWithScrimPainter

        if (imagePainter != other.imagePainter) return false
        if (brush != other.brush) return false
        if (scrimAlpha != other.scrimAlpha) return false
        if (alpha != other.alpha) return false
        if (forcedSize != other.forcedSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imagePainter.hashCode()
        result = 31 * result + brush.hashCode()
        result = 31 * result + scrimAlpha.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + forcedSize.hashCode()
        return result
    }

    override fun toString(): String {
        return "ImageWithScrimPainter(imagePainter=$imagePainter, brush=$brush, " +
            "scrimAlpha=$scrimAlpha, alpha=$alpha, forcedSize=$forcedSize)"
    }

    /**
     * Size of the combined painter. Returns imagePainter.intrinsicSize unless size is non-null in
     * constructor.
     * - [Size.Unspecified] - the composable size should be used without considering the Painter
     *   size. This likely involves scaling of the painter.
     * - [Painter.intrinsicSize] - the Painter's size should be used, this likely increases the size
     *   of the component.
     */
    override val intrinsicSize: Size = forcedSize ?: imagePainter.intrinsicSize
}
