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

package androidx.ui.graphics

import androidx.ui.geometry.Offset
import org.jetbrains.skija.FilterTileMode
import org.jetbrains.skija.GradientStyle

internal fun DesktopLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    val skijaShader = org.jetbrains.skija.Shader.makeLinearGradient(
        from.x, from.y, to.x, to.y, colors.toIntArray(), colorStops?.toFloatArray(),
        GradientStyle(tileMode.toSkija(), true, identityMatrix33())
    )
    return Shader(skijaShader)
}

internal fun DesktopRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    val skijaShader = org.jetbrains.skija.Shader.makeRadialGradient(
        center.x,
        center.y,
        radius,
        colors.toIntArray(),
        colorStops?.toFloatArray(),
        GradientStyle(tileMode.toSkija(), true, identityMatrix33())
    )
    return Shader(skijaShader)
}

@Suppress("UNUSED_PARAMETER")
internal fun DesktopImageShader(
    image: ImageAsset,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader {
    println("ImageShader not implemented yet")
    return Shader()
}

private fun List<Color>.toIntArray(): IntArray =
    IntArray(size) { i -> this[i].toArgb() }

private fun validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
    if (colorStops == null) {
        if (colors.size < 2) {
            throw IllegalArgumentException(
                "colors must have length of at least 2 if colorStops " +
                        "is omitted."
            )
        }
    } else if (colors.size != colorStops.size) {
        throw IllegalArgumentException(
            "colors and colorStops arguments must have" +
                    " equal length."
        )
    }
}

private fun TileMode.toSkija() = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
}