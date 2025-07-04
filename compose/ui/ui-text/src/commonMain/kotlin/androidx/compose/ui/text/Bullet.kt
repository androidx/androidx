/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * Draws a bullet point next to a paragraph.
 *
 * Bullets do not add indentation. The call site must provide sufficient leading space to
 * accommodate the bullet and its padding, preventing overlap with the text.
 *
 * There are several ways to achieve this leading space. One common approach is to use
 * `ParagraphStyle` with `textIndent` within an `AnnotatedString`.
 *
 * @param shape the shape of the bullet to draw
 * @param width the width of the bullet
 * @param height the height of the bullet
 * @param padding the padding between the end of the bullet and the start of the paragraph
 * @param brush brush to draw a bullet with. If `null` is passed, the bullet will be drawn like the
 *   rest of the text
 * @param alpha opacity to be applied to [brush] drawing a bullet from 0.0f to 1.0f representing
 *   fully transparent to fully opaque respectively. When [Float.NaN] is passed, the alpha will not
 *   be changed and the one used for drawing rest of the text is used
 * @param drawStyle defines the draw style of the bullet, e.g. a fill or a stroke
 * @sample androidx.compose.ui.text.samples.AnnotatedStringWithBulletListCustomBulletSample
 */
class Bullet(
    val shape: Shape,
    // width and height is used to avoid introducing TextUnitSize
    val width: TextUnit,
    val height: TextUnit,
    val padding: TextUnit,
    val brush: Brush? = null,
    val alpha: Float = Float.NaN,
    val drawStyle: DrawStyle = Fill,
) : AnnotatedString.Annotation {
    /** Copies the existing [Bullet] replacing some of the fields as desired. */
    fun copy(
        shape: Shape = this.shape,
        width: TextUnit = this.width,
        height: TextUnit = this.height,
        padding: TextUnit = this.padding,
        brush: Brush? = this.brush,
        alpha: Float = this.alpha,
        drawStyle: DrawStyle = this.drawStyle,
    ) = Bullet(shape, width, height, padding, brush, alpha, drawStyle)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Bullet) return false

        if (shape != other.shape) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (padding != other.padding) return false
        if (brush != other.brush) return false
        if (alpha != other.alpha) return false
        if (drawStyle != other.drawStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + padding.hashCode()
        result = 31 * result + brush.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + drawStyle.hashCode()
        return result
    }

    override fun toString(): String {
        return "Bullet(shape=$shape, size=($width, $height), padding=$padding, brush=$brush, " +
            "alpha=$alpha, drawStyle=$drawStyle)"
    }

    companion object {
        /** Indentation required to fit [Default] bullet. */
        val DefaultIndentation = 1.em

        /** Height and width for [Default] bullet. */
        val DefaultSize = 0.25.em

        /** Padding between bullet and start of paragraph for [Default] bullet */
        val DefaultPadding = 0.25.em

        /** Default bullet used in AnnotatedString's bullet list */
        val Default = Bullet(CircleShape, DefaultSize, DefaultSize, DefaultPadding)
    }
}

private object CircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cornerRadius = CornerRadius(size.minDimension / 2f)
        return Outline.Rounded(
            RoundRect(
                rect = size.toRect(),
                topLeft = cornerRadius,
                topRight = cornerRadius,
                bottomRight = cornerRadius,
                bottomLeft = cornerRadius,
            )
        )
    }
}
