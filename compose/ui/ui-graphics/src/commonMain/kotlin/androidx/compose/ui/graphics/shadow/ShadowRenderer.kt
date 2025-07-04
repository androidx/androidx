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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.LayoutDirection

internal abstract class ShadowRenderer(val outline: Outline) {
    /** Optional path used to draw the shadow if the shape creates a generic outline */
    private var path: Path? = null

    /**
     * ColorFilter used to tint the shadow to the color specified in the InnerShadow object If the
     * Painter itself provides a ColorFilter instead, this ColorFilter is unused
     */
    private var shadowTint: ColorFilter? = null

    /** Color used to create the [shadowTint] parameter */
    private var shadowTintColor: Color = Color.Unspecified

    /** Corner radius used to draw the shadow if the shape creates a round rect outline */
    private var cornerRadius: CornerRadius = CornerRadius.Zero

    private var generatedSize: Size = Size.Unspecified
    private var generatedLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    private var generatedDensity: Float = 1f

    fun DrawScope.drawShadow(
        colorFilter: ColorFilter?,
        size: Size,
        color: Color,
        brush: Brush?,
        alpha: Float,
        blendMode: BlendMode,
    ) {
        updateParamsFromOutline(outline)
        val filter =
            if (colorFilter != null) {
                colorFilter
            } else if (brush == null && color.isSpecified) {
                obtainTint(color)
            } else {
                null
            }
        if (
            generatedSize.isUnspecified ||
                generatedSize != size ||
                generatedLayoutDirection != layoutDirection ||
                generatedDensity != density
        ) {
            buildShadow(size, cornerRadius, path)
            generatedSize = size
            generatedLayoutDirection = layoutDirection
            generatedDensity = density
        }
        onDrawShadow(size, cornerRadius, path, alpha, filter, brush, blendMode)
    }

    protected abstract fun DrawScope.buildShadow(
        size: Size,
        cornerRadius: CornerRadius,
        path: Path?,
    )

    open fun invalidateShadow() {
        generatedSize = Size.Unspecified
        generatedLayoutDirection = LayoutDirection.Ltr
        generatedDensity = 1f
    }

    protected abstract fun DrawScope.onDrawShadow(
        size: Size,
        cornerRadius: CornerRadius,
        path: Path?,
        alpha: Float,
        colorFilter: ColorFilter?,
        brush: Brush?,
        blendMode: BlendMode,
    )

    private fun obtainTint(color: Color): ColorFilter {
        var colorFilter = shadowTint
        if (colorFilter == null || shadowTintColor != color) {
            colorFilter = ColorFilter.tint(color)
            shadowTintColor = color
            shadowTint = colorFilter
        }
        return colorFilter
    }

    private fun updateParamsFromOutline(outline: Outline) {
        when (outline) {
            is Outline.Generic -> {
                path = outline.path
                cornerRadius = CornerRadius.Zero
            }
            is Outline.Rounded -> {
                if (outline.roundRect.isSimple) {
                    path = null
                    cornerRadius = outline.roundRect.topLeftCornerRadius
                } else {
                    path = outline.roundRectPath
                    cornerRadius = CornerRadius.Zero
                }
            }
            is Outline.Rectangle -> {
                path = null
                cornerRadius = CornerRadius.Zero
            }
        }
    }
}
