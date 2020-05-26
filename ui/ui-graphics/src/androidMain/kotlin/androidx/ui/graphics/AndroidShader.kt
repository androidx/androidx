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
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.RadialGradient

actual typealias NativeShader = android.graphics.Shader

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    return Shader(
        LinearGradient(
            from.dx,
            from.dy,
            to.dx,
            to.dy,
            colors.toIntArray(),
            colorStops?.toFloatArray(),
            tileMode.toNativeTileMode()
        )
    )
}

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    return Shader(
        RadialGradient(
            center.dx,
            center.dy,
            radius,
            colors.toIntArray(),
            colorStops?.toFloatArray(),
            tileMode.toNativeTileMode()
        )
    )
}

internal actual fun ActualImageShader(
    image: ImageAsset,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader {
    return Shader(
        BitmapShader(
            image.asAndroidBitmap(),
            tileModeX.toNativeTileMode(),
            tileModeY.toNativeTileMode()
        )
    )
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