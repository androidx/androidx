/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.compose.remote.frontend.capture.shaders

import android.graphics.SweepGradient
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.frontend.state.RemoteMatrix3x3
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSweepShader(
    public var centerX: Float,
    public var centerY: Float,
    public var colors: IntArray,
    public var positions: FloatArray?,
) : SweepGradient(centerX, centerY, colors, positions), RemoteShader {
    override fun apply(paintBundle: PaintBundle) {
        paintBundle.setSweepGradient(colors, 0, positions, centerX, centerY)
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}

/**
 * Creates a sweep gradient with the given colors dispersed around the center with offsets defined
 * in each colorstop pair. The sweep begins relative to 3 o'clock and continues clockwise until it
 * reaches the starting position again.
 *
 * Ex:
 * ```
 *  Brush.sweepGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      center = Offset(0.0f, 100.0f)
 * )
 * ```
 *
 * @param colorStops Colors and offsets to determine how the colors are dispersed throughout the
 *   sweep gradient
 * @param center Center position of the sweep gradient circle. If this is set to
 *   [Offset.Unspecified] then the center of the drawing area is used as the center for the sweep
 *   gradient
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.sweepGradient(
    vararg colorStops: Pair<Float, Color>,
    center: Offset = Offset.Unspecified,
): RemoteSweepGradient =
    RemoteSweepGradient(
        colors = List<Color>(colorStops.size) { i -> colorStops[i].second },
        stops = List<Float>(colorStops.size) { i -> colorStops[i].first },
        center = center,
    )

/**
 * Creates a sweep gradient with the given colors dispersed evenly around the center. The sweep
 * begins relative to 3 o'clock and continues clockwise until it reaches the starting position
 * again.
 *
 * Ex:
 * ```
 *  Brush.sweepGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      center = Offset(10.0f, 20.0f)
 * )
 * ```
 *
 * @param colors List of colors to fill the sweep gradient
 * @param center Center position of the sweep gradient circle. If this is set to
 *   [Offset.Unspecified] then the center of the drawing area is used as the center for the sweep
 *   gradient
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.sweepGradient(
    colors: List<Color>,
    center: Offset = Offset.Unspecified,
): RemoteSweepGradient = RemoteSweepGradient(colors = colors, stops = null, center = center)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public data class RemoteSweepGradient(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val center: Offset,
) : RemoteBrush() {

    override fun createShader(size: Size): Shader {
        var realCenter = center
        if (center.isUnspecified) {
            realCenter = size.center
        } else if (center.x == Float.POSITIVE_INFINITY || center.y == Float.POSITIVE_INFINITY) {
            val centerX = if (center.x == Float.POSITIVE_INFINITY) size.width else center.x
            val centerY = if (center.y == Float.POSITIVE_INFINITY) size.height else center.y
            realCenter = Offset(centerX, centerY)
        }
        validateColorStops(colors = colors, colorStops = stops)
        val numTransparentColors = countTransparentColors(colors = colors)
        return RemoteSweepShader(
            realCenter.x,
            realCenter.y,
            makeTransparentColors(colors = colors, numTransparentColors = numTransparentColors),
            makeTransparentStops(
                stops = stops,
                colors = colors,
                numTransparentColors = numTransparentColors,
            ),
        )
    }

    override fun toComposeUi(): Brush {
        return if (stops != null) {
            return Brush.sweepGradient(
                *stops.zip<Float, Color>(colors).toTypedArray<Pair<Float, Color>>(),
                center = center,
            )
        } else {
            return Brush.sweepGradient(colors, center)
        }
    }
}

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
