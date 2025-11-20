/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastRoundToInt

internal fun containerPainter(
    painter: Painter,
    scrim: Brush? = null,
    sizeToIntrinsics: Boolean = false,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
): Painter =
    DefaultContainerPainter(painter, scrim, sizeToIntrinsics, alignment, contentScale, alpha)

@Stable
private class DefaultContainerPainter(
    private val painter: Painter,
    private val scrim: Brush?,
    private val sizeToIntrinsics: Boolean,
    private val alignment: Alignment,
    private val contentScale: ContentScale,
    private val alpha: Float,
) : Painter() {
    override val intrinsicSize: Size
        get() = if (sizeToIntrinsics) painter.intrinsicSize else Size.Unspecified

    override fun DrawScope.onDraw() {
        // Code "Inspired" by Modifier.paint
        val srcWidth =
            if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                intrinsicSize.width
            } else {
                size.width
            }

        val srcHeight =
            if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                intrinsicSize.height
            } else {
                size.height
            }

        val srcSize = Size(srcWidth, srcHeight)

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize =
            if (size.width != 0f && size.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, size)
            } else {
                Size.Zero
            }

        val alignedPosition =
            alignment.align(
                IntSize(scaledSize.width.fastRoundToInt(), scaledSize.height.fastRoundToInt()),
                IntSize(size.width.fastRoundToInt(), size.height.fastRoundToInt()),
                layoutDirection,
            )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
        translate(dx, dy) { with(painter) { draw(size = scaledSize, alpha = alpha) } }

        // Maintain the same pattern as Modifier.drawBehind to allow chaining of DrawModifiers
        scrim?.let { drawRect(brush = it, alpha = alpha) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DefaultContainerPainter) return false

        if (painter != other.painter) return false
        if (scrim != other.scrim) return false
        if (sizeToIntrinsics != other.sizeToIntrinsics) return false
        if (alignment != other.alignment) return false
        if (contentScale != other.contentScale) return false
        if (alpha != other.alpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + scrim.hashCode()
        result = 31 * result + sizeToIntrinsics.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + contentScale.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }
}

internal fun containerPainter(painter: Painter, alpha: Float): Painter =
    DefaultDisabledContainerPainter(painter, alpha)

@Stable
private class DefaultDisabledContainerPainter(
    private val painter: Painter,
    private val alpha: Float,
) : Painter() {
    override val intrinsicSize: Size
        get() = painter.intrinsicSize

    override fun DrawScope.onDraw() {
        with(painter) { draw(size = size, alpha = alpha) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DefaultDisabledContainerPainter) return false

        if (painter != other.painter) return false
        if (alpha != other.alpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + alpha.hashCode()
        return result
    }
}

private fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.fastIsFinite()

private fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.fastIsFinite()
