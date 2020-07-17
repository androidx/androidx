/*
 * Copyright 2020 The Android Open Source Project
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
package android.graphics

import org.jetbrains.skija.FilterTileMode
import org.jetbrains.skija.GradientStyle

@Suppress("unused")
open class Shader(val skija: org.jetbrains.skija.Shader? = null) {
    enum class TileMode(val skija: FilterTileMode) {
        CLAMP(FilterTileMode.CLAMP),
        REPEAT(FilterTileMode.REPEAT),
        MIRROR(FilterTileMode.MIRROR)
    }
}

@Suppress("unused")
class LinearGradient(skija: org.jetbrains.skija.Shader) : Shader(skija) {
    constructor(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        colors: IntArray,
        positions: FloatArray?,
        tile: TileMode
    ) : this(
        org.jetbrains.skija.Shader.makeLinearGradient(
            x0, y0, x1, y1, colors, positions,
            GradientStyle(tile.skija, true, Matrix().skija)
        )
    )

    constructor(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        color0: Int,
        color1: Int,
        tile: TileMode
    ) : this(
        org.jetbrains.skija.Shader.makeLinearGradient(
            x0, y0, x1, y1, intArrayOf(color0, color1), null,
            GradientStyle(tile.skija, true, Matrix().skija)
        )
    )
}

@Suppress("unused")
class SweepGradient(skija: org.jetbrains.skija.Shader) : Shader(skija) {
    constructor(
        cx: Float,
        cy: Float,
        colors: IntArray,
        positions: FloatArray?
    ) : this(
        org.jetbrains.skija.Shader.makeSweepGradient(cx, cy, colors, positions)
    )

    constructor(cx: Float, cy: Float, color0: Int, color1: Int) : this(
        org.jetbrains.skija.Shader.makeSweepGradient(cx, cy, intArrayOf(color0, color1), null)
    )
}

@Suppress("unused")
class RadialGradient(skija: org.jetbrains.skija.Shader) : Shader(skija) {
    constructor(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: IntArray,
        stops: FloatArray?,
        tileMode: TileMode
    ) : this(
        org.jetbrains.skija.Shader.makeRadialGradient(
            centerX, centerY, radius, colors, stops,
            GradientStyle(tileMode.skija, true, Matrix().skija)
        )
    )

    constructor(
        centerX: Float,
        centerY: Float,
        radius: Float,
        centerColor: Int,
        edgeColor: Int,
        tileMode: TileMode
    ) : this(
        org.jetbrains.skija.Shader.makeRadialGradient(
            centerX, centerY, radius, intArrayOf(centerColor, edgeColor), null,
            GradientStyle(tileMode.skija, true, Matrix().skija)
        )
    )
}

@Suppress("unused")
class BitmapShader(
    bitmap: Bitmap,
    tileX: TileMode,
    tileY: TileMode
) : Shader(null) {
    init {
        println("BitmapShader not implemented yet")
    }
}