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

package androidx.ui.core.vectorgraphics

import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.TileMode
import androidx.ui.vectormath64.Matrix4

val EmptyBrush = object : Brush {
    override fun applyBrush(p: Paint) {
        // NO-OP
    }
}

interface Brush {
    fun applyBrush(p: Paint)
}

/* inline */ class SolidColor(private val value: Color) : Brush {
    override fun applyBrush(p: Paint) {
        p.color = value
    }
}

typealias ColorStop = Pair<Color, Float>

fun obtainBrush(brush: Any?): Brush {
    return when (brush) {
        is Int -> SolidColor(Color(brush))
        is Color -> SolidColor(brush)
        is Brush -> brush
        null -> EmptyBrush
        else -> throw IllegalArgumentException(brush.javaClass.simpleName +
                "Brush must be either a Color long, LinearGradient or RadialGradient")
    }
}

// TODO (njawad) replace with inline color class
class LinearGradient(
    vararg colorStops: ColorStop,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val colors: List<Color>
    private val stops: List<Float>

    init {
        colors = List(colorStops.size) { i -> colorStops[i].first }
        stops = List(colorStops.size) { i -> colorStops[i].second }
    }

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.linear(
            Offset(startX, startY),
            Offset(endX, endY),
            colors,
            stops,
            tileMode)
    }
}

class RadialGradient(
    vararg colorStops: ColorStop,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val colors: List<Color>
    private val stops: List<Float>

    init {
        colors = List(colorStops.size) { it -> colorStops[it].first }
        stops = List(colorStops.size) { it -> colorStops[it].second }
    }

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.radial(
            Offset(centerX, centerY),
            radius, colors, stops, tileMode, Matrix4(),
            null, 0.0f)
    }
}