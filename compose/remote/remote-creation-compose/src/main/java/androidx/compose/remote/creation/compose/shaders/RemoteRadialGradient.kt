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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.shaders

import android.graphics.RadialGradient
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a radial gradient with the given colors at the provided offset defined in the colorstop
 * pair.
 *
 * ```
 * Brush.radialGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      center = Offset(side1 / 2.0f, side2 / 2.0f),
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 *
 * @param colorStops Colors and offsets to determine how the colors are dispersed throughout the
 *   radial gradient
 * @param center Center position of the radial gradient circle. If this is set to
 *   [Offset.Unspecified] then the center of the drawing area is used as the center for the radial
 *   gradient. [Float.POSITIVE_INFINITY] can be used for either [Offset.x] or [Offset.y] to indicate
 *   the far right or far bottom of the drawing area respectively.
 * @param radius Radius for the radial gradient. Defaults to positive infinity to indicate the
 *   largest radius that can fit within the bounds of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.radialGradient(
    vararg colorStops: Pair<Float, Color>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: TileMode = TileMode.Clamp,
): RemoteRadialGradient =
    RemoteRadialGradient(
        colors = List<Color>(colorStops.size) { i -> colorStops[i].second },
        stops = List<Float>(colorStops.size) { i -> colorStops[i].first },
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

/**
 * Creates a radial gradient with the given colors evenly dispersed within the gradient
 *
 * ```
 * Brush.radialGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      centerX = side1 / 2.0f,
 *      centerY = side2 / 2.0f,
 *      radius = side1 / 2.0f,
 *      tileMode = TileMode.Repeated
 * )
 * ```
 *
 * @param colors Colors to be rendered as part of the gradient
 * @param center Center position of the radial gradient circle. If this is set to
 *   [Offset.Unspecified] then the center of the drawing area is used as the center for the radial
 *   gradient. [Float.POSITIVE_INFINITY] can be used for either [Offset.x] or [Offset.y] to indicate
 *   the far right or far bottom of the drawing area respectively.
 * @param radius Radius for the radial gradient. Defaults to positive infinity to indicate the
 *   largest radius that can fit within the bounds of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
@Suppress("PrimitiveInCollection")
public fun RemoteBrush.Companion.radialGradient(
    colors: List<Color>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: TileMode = TileMode.Clamp,
): RemoteRadialGradient =
    RemoteRadialGradient(
        colors = colors,
        stops = null,
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@Suppress("PrimitiveInCollection")
public data class RemoteRadialGradient(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val center: RemoteOffset?,
    private val radius: RemoteFloat?,
    private val tileMode: TileMode = TileMode.Clamp,
) : RemoteBrush() {

    override fun createShader(size: RemoteSize): Shader {

        validateColorStops(colors = colors, colorStops = stops)
        val realCenter = center ?: size.center
        val centerX = resolve(realCenter.x, size.width)
        val centerY = resolve(realCenter.y, size.height)
        val realRadius = radius ?: (size.width.min(size.height) / 2f)
        val resolvedRadius = resolve(realRadius, size.minDimension / 2f)

        return RemoteRadialShader(
            centerX.toFloat(),
            centerY.toFloat(),
            resolvedRadius.toFloat(),
            // No change for Android O+, map the colors directly to their argb equivalent
            IntArray(colors.size) { i -> colors[i].toArgb() },
            stops?.toFloatArray(),
            tileMode.toAndroidTileMode(),
        )
    }
}

@Suppress("PrimitiveInCollection")
private fun validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
    if (colorStops == null) {
        if (colors.size < 2) {
            throw IllegalArgumentException(
                "colors must have length of at least 2 if colorStops " + "is omitted."
            )
        }
    } else if (colors.size != colorStops.size) {
        throw IllegalArgumentException(
            "colors and colorStops arguments must have" + " equal length."
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteRadialShader(
    public var centerX: Float,
    public var centerY: Float,
    public var radius: Float,
    public var colors: IntArray,
    public var positions: FloatArray?,
    public var tileMode: TileMode,
) : RadialGradient(centerX, centerY, radius, colors, positions, tileMode), RemoteShader {
    override fun apply(paintBundle: PaintBundle) {
        paintBundle.setRadialGradient(
            colors,
            0,
            positions,
            centerX,
            centerY,
            radius,
            tileMode.ordinal,
        )
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}
