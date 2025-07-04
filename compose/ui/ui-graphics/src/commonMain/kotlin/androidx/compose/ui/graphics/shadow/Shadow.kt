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

package androidx.compose.ui.graphics.shadow

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Group of parameters that represent how a drop shadow or inner shadow should be rendered.
 *
 * @property radius The blur radius of the shadow
 * @property spread Spread parameter that adds to the size of the shadow
 * @property offset The internal offset of the shadow within the geometry provided.
 * @property blendMode Blending algorithm used by the shadow
 */
@Immutable
class Shadow
private constructor(
    val radius: Dp,
    val spread: Dp,
    val offset: DpOffset,
    color: Color,
    brush: Brush?,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float,
    val blendMode: BlendMode,
) {

    /**
     * Color of the shadow. If [Color.Unspecified] is provided, [Color.Black] will be used as a
     * default. This color is only used if [brush] is null.
     */
    val color: Color

    /** Optional brush to render the shadow with. */
    val brush: Brush?

    /** Opacity of the shadow */
    val alpha: Float

    init {
        // If the brush we are given can be represented by a Color, just consume that directly
        // in order to leverage more efficient tinting through a ColorFilter
        // Otherwise consume the Brush directly so that it blended against the shadow geometry
        // specified by a BitmapShader
        if (brush is SolidColor) {
            this.color = brush.value
            this.brush = null
        } else {
            this.color = color
            this.brush = brush
        }
        this.alpha = alpha.coerceIn(0f, 1f)
    }

    /**
     * Creates a group of parameters that represent how a drop shadow or inner shadow should be
     * rendered.
     *
     * @param radius The blur radius of the shadow
     * @param brush Brush used to blend against a mask defined by the shadow geometry
     * @param spread Optional parameter to grow the shadow geometry by
     * @param offset Optional parameter to offset the shadow within the geometry bounds
     * @param alpha Optional opacity of the shadow
     * @param blendMode Optional blending algorithm used by the shadow
     */
    constructor(
        radius: Dp,
        brush: Brush,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    ) : this(
        radius = radius,
        spread = spread,
        offset = offset,
        color = Color.Black,
        brush = brush,
        alpha = alpha,
        blendMode = blendMode,
    )

    /**
     * Creates a group of parameters that represent how a drop shadow or inner shadow should be
     * rendered.
     *
     * @param radius The blur radius of the shadow
     * @param color The color of the shadow. If [Color.Unspecified] is provided, [Color.Black] will
     *   be used as a default.
     * @param spread Optional parameter to grow the shadow geometry by
     * @param offset Optional parameter to offset the shadow within the geometry bounds
     * @param alpha Optional opacity of the shadow
     * @param blendMode Optional blending algorithm used by the shadow
     */
    constructor(
        radius: Dp,
        color: Color = Color.Black,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    ) : this(
        radius = radius,
        spread = spread,
        offset = offset,
        color = if (color.isSpecified) color else Color.Black,
        brush = null,
        alpha = alpha,
        blendMode = blendMode,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shadow) return false

        if (radius != other.radius) return false
        if (spread != other.spread) return false
        if (offset != other.offset) return false
        if (alpha != other.alpha) return false
        if (blendMode != other.blendMode) return false
        if (color != other.color) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = radius.hashCode()
        result = 31 * result + spread.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + blendMode.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + (brush?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShadowParams(radius=$radius, spread=$spread, offset=$offset, alpha=$alpha, " +
            "blendMode=$blendMode, color=$color, brush=$brush)"
    }

    internal fun copyWithoutOffset() =
        Shadow(radius, spread, DpOffset.Zero, color, brush, alpha, blendMode)
}
