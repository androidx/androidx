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

import androidx.ui.core.Px
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.Shader
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

data class SolidColor(private val value: Color) : Brush {
    override fun applyBrush(p: Paint) {
        p.color = value
    }
}

typealias ColorStop = Pair<Float, Color>

/**
 * Obtains actual Brush instance from Union type, throws an IllegalArgumentException
 * if the type is other than Int, Color, Brush or null
 */
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

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 * The colors are
 *
 * ```
 *  LinearGradient(
 *      0.0f to Color.Aqua,
 *      0.3f to Color.Lime,
 *      1.0f to Color.Fuchsia,
 *      startX = Px.Zero,
 *      startY = Px(50.0f),
 *      endY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 * ```
 */
fun LinearGradient(
    vararg colors: Color,
    startX: Px,
    startY: Px,
    endX: Px,
    endY: Px,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    return LinearGradient(
        colors.asList(),
        null,
        startX,
        startY,
        endX,
        endY,
        tileMode
    )
}

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 * The colors are dispersed at the provided offset defined in the [ColorStop]
 *
 * ```
 *  LinearGradient(
 *      0.0f to Color.Aqua,
 *      0.3f to Color.Lime,
 *      1.0f to Color.Fuchsia,
 *      startX = Px.Zero,
 *      startY = Px(50.0f),
 *      endY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 * ```
 */
fun LinearGradient(
    vararg colorStops: ColorStop,
    startX: Px,
    startY: Px,
    endX: Px,
    endY: Px,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    return LinearGradient(
        List<Color>(colorStops.size) { i -> colorStops[i].second },
        List<Float>(colorStops.size) { i -> colorStops[i].first },
        startX,
        startY,
        endX,
        endY,
        tileMode
    )
}

/**
 * Creates a radial gradient with the given colors at the provided offset defined in the [ColorStop]
 * ```
 * RadialGradient(
 *      0.0f to Color.Navy,
 *      0.3f to Color.Olive,
 *      1.0f to Color.Teal,
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.repeated
 * )
 * ```
 */
fun RadialGradient(
    vararg colorStops: ColorStop,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.clamp
): RadialGradient {
    return RadialGradient(
        List<Color>(colorStops.size) { i -> colorStops[i].second },
        List<Float>(colorStops.size) { i -> colorStops[i].first },
        centerX,
        centerY,
        radius,
        tileMode
    )
}

/**
 * Creates a radial gradient with the given colors evenly dispersed within the gradient
 * ```
 * RadialGradient(
 *      Color.Navy,
 *      Color.Olive,
 *      Color.Teal,
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.repeated
 * )
 * ```
 */
fun RadialGradient(
    vararg colors: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.clamp
): RadialGradient {
    return RadialGradient(colors.asList(), null, centerX, centerY, radius, tileMode)
}

/**
 * Creates a vertical gradient with the given colors at the provided offset defined in the [ColorStop]
 * Ex:
 * ```
 *  VerticalGradient(
 *      Color.Aqua,
 *      Color.Lime,
 *      Color.Fuchsia,
 *      startY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 *
 * ```
 */
fun VerticalGradient(
    vararg colors: Color,
    startY: Px,
    endY: Px,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    return LinearGradient(
        colors.asList(),
        null,
        startX = Px.Zero,
        startY = startY,
        endX = Px.Zero,
        endY = endY,
        tileMode = tileMode
    )
}

/**
 * Creates a vertical gradient with the given colors evenly dispersed within the gradient
 * Ex:
 * ```
 *  VerticalGradient(
 *      Color.Aqua,
 *      Color.Lime,
 *      Color.Fuchsia,
 *      startY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 * ```
 */
fun VerticalGradient(
    vararg colorStops: ColorStop,
    startY: Px,
    endY: Px,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    return LinearGradient(
        List<Color>(colorStops.size) { i -> colorStops[i].second },
        List<Float>(colorStops.size) { i -> colorStops[i].first },
        startX = Px.Zero,
        startY = startY,
        endX = Px.Zero,
        endY = endY,
        tileMode = tileMode
    )
}

/**
 * Creates a horizontal gradient with the given colors evenly dispersed within the gradient
 *
 * Ex:
 * ```
 *  HorizontalGradient(
 *      Color.Aqua,
 *      Color.Lime,
 *      Color.Fuchsia,
 *      startX = Px(10.0f),
 *      endX = Px(20.0f)
 * )
 * ```
 */
fun HorizontalGradient(
    vararg colors: Color,
    startX: Px,
    endX: Px,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    return LinearGradient(
        colors.asList(),
        null,
        startX = startX,
        startY = Px.Zero,
        endX = endX,
        endY = Px.Zero,
        tileMode = tileMode
    )
}

/**
 * Creates a horizontal gradient with the given colors dispersed at the provided offset defined in the [ColorStop]
 *
 * Ex:
 * ```
 *  HorizontalGradient(
 *      0.0f to Color.Aqua,
 *      0.3f to Color.Lime,
 *      1.0f to Color.Fuchsia,
 *      startX = Px.Zero,
 *      endX = Px(100.0f)
 * )
 * ```
 */
fun HorizontalGradient(
    vararg colorStops: ColorStop,
    startX: Px,
    endX: Px,
    tileMode: TileMode = TileMode.clamp
): Brush {
    return LinearGradient(
        List<Color>(colorStops.size) { i -> colorStops[i].second },
        List<Float>(colorStops.size) { i -> colorStops[i].first },
        startX = startX,
        startY = Px.Zero,
        endX = endX,
        endY = Px.Zero,
        tileMode = tileMode
    )
}

/**
 * Brush implementation used to apply a linear gradient on a given [Paint]
 */
data class LinearGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val startX: Px,
    private val startY: Px,
    private val endX: Px,
    private val endY: Px,
    private val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val shader: Shader

    init {
        shader = Gradient.linear(
            Offset(startX.value, startY.value),
            Offset(endX.value, endY.value),
            colors,
            stops,
            tileMode)
    }

    override fun applyBrush(p: Paint) {
        p.shader = shader
    }
}

/**
 * Brush implementation used to apply a radial gradient on a given [Paint]
 */
data class RadialGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val shader: Shader

    init {
        shader = Gradient.radial(
            Offset(centerX, centerY),
            radius, colors, stops, tileMode, Matrix4())
    }

    override fun applyBrush(p: Paint) {
        p.shader = shader
    }
}