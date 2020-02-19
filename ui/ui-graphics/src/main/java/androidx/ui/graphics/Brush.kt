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
import androidx.ui.unit.Px

interface Brush {
    fun applyTo(p: Paint)
}

data class SolidColor(val value: Color) : Brush {
    override fun applyTo(p: Paint) {
        p.color = value
    }
}

typealias ColorStop = Pair<Float, Color>

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 * The colors are
 *
 * ```
 *  LinearGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      startX = Px.Zero,
 *      startY = Px(50.0f),
 *      endY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 * ```
 */
fun LinearGradient(
    colors: List<Color>,
    startX: Px,
    startY: Px,
    endX: Px,
    endY: Px,
    tileMode: TileMode = TileMode.Clamp
): LinearGradient {
    return LinearGradient(
        colors,
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
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
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
    tileMode: TileMode = TileMode.Clamp
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
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 */
fun RadialGradient(
    vararg colorStops: ColorStop,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.Clamp
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
 *      Color.Red,
 *      Color.Green,
 *      Color.Blue,
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 */
fun RadialGradient(
    colors: List<Color>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.Clamp
): RadialGradient {
    return RadialGradient(colors, null, centerX, centerY, radius, tileMode)
}

/**
 * Creates a vertical gradient with the given colors at the provided offset defined in the [ColorStop]
 * Ex:
 * ```
 *  VerticalGradient(
 *      Color.Red,
 *      Color.Green,
 *      Color.Blue,
 *      startY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 *
 * ```
 */
fun VerticalGradient(
    colors: List<Color>,
    startY: Px,
    endY: Px,
    tileMode: TileMode = TileMode.Clamp
): LinearGradient {
    return LinearGradient(
        colors,
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
 *      Color.Red,
 *      Color.Green,
 *      Color.Blue,
 *      startY = Px.Zero,
 *      endY = Px(100.0f)
 * )
 * ```
 */
fun VerticalGradient(
    vararg colorStops: ColorStop,
    startY: Px,
    endY: Px,
    tileMode: TileMode = TileMode.Clamp
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
 *      Color.Red,
 *      Color.Green,
 *      Color.Blue,
 *      startX = Px(10.0f),
 *      endX = Px(20.0f)
 * )
 * ```
 */
fun HorizontalGradient(
    colors: List<Color>,
    startX: Px,
    endX: Px,
    tileMode: TileMode = TileMode.Clamp
): LinearGradient {
    return LinearGradient(
        colors,
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
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      startX = Px.Zero,
 *      endX = Px(100.0f)
 * )
 * ```
 */
fun HorizontalGradient(
    vararg colorStops: ColorStop,
    startX: Px,
    endX: Px,
    tileMode: TileMode = TileMode.Clamp
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
    private val tileMode: TileMode = TileMode.Clamp
) : ShaderBrush(
    LinearGradientShader(
        Offset(startX.value, startY.value),
        Offset(endX.value, endY.value),
        colors,
        stops,
        tileMode
    )
    )

/**
 * Brush implementation used to apply a radial gradient on a given [Paint]
 */
data class RadialGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.Clamp
) : ShaderBrush(
    RadialGradientShader(
        Offset(centerX, centerY),
        radius,
        colors,
        stops,
        tileMode
    )
    )

/**
 * Brush implementation that wraps and applies a the provided shader to a [Paint]
 */
open class ShaderBrush(val shader: Shader) : Brush {
    override fun applyTo(p: Paint) {
        p.shader = shader
    }
}