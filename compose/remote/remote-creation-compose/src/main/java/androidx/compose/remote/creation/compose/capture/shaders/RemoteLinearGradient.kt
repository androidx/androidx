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

package androidx.compose.remote.creation.compose.capture.shaders

import android.graphics.LinearGradient
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates. The
 * colors are dispersed at the provided offset defined in the colorstop pair.
 *
 * ```
 *  Brush.linearGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      start = Offset(0.0f, 50.0f),
 *      end = Offset(0.0f, 100.0f)
 * )
 * ```
 *
 * @param colorStops Colors and their offset in the gradient area
 * @param start Starting position of the linear gradient. This can be set to [Offset.Zero] to
 *   position at the far left and top of the drawing area
 * @param end Ending position of the linear gradient. This can be set to [Offset.Infinite] to
 *   position at the far right and bottom of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.linearGradient(
    vararg colorStops: Pair<Float, Color>,
    start: RemoteOffset,
    end: RemoteOffset,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = List<Color>(colorStops.size) { i -> colorStops[i].second },
        stops = List<Float>(colorStops.size) { i -> colorStops[i].first },
        start = start,
        end = end,
        tileMode = tileMode,
    )

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates. The
 * colors are
 *
 * ```
 *  Brush.linearGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      start = Offset(0.0f, 50.0f)
 *      end = Offset(0.0f, 100.0f)
 * )
 * ```
 *
 * @param colors Colors to be rendered as part of the gradient
 * @param start Starting position of the linear gradient. This can be set to [Offset.Zero] to
 *   position at the far left and top of the drawing area
 * @param end Ending position of the linear gradient. This can be set to [Offset.Infinite] to
 *   position at the far right and bottom of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
@Suppress("PrimitiveInCollection")
public fun RemoteBrush.Companion.linearGradient(
    colors: List<Color>,
    start: RemoteOffset,
    end: RemoteOffset,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = colors,
        stops = null,
        start = start,
        end = end,
        tileMode = tileMode,
    )

/**
 * Creates a horizontal gradient with the given colors evenly dispersed within the gradient
 *
 * Ex:
 * ```
 *  Brush.horizontalGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      startX = 10.0f,
 *      endX = 20.0f
 * )
 * ```
 *
 * @param colors colors Colors to be rendered as part of the gradient
 * @param startX Starting x position of the horizontal gradient. Defaults to 0 which represents the
 *   left of the drawing area
 * @param endX Ending x position of the horizontal gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the right of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
@Suppress("PrimitiveInCollection")
public fun RemoteBrush.Companion.horizontalGradient(
    colors: List<Color>,
    startX: RemoteFloat? = null,
    endX: RemoteFloat? = null,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = colors,
        stops = null,
        start = startX?.let { RemoteOffset(it, 0.0f) },
        end = endX?.let { RemoteOffset(it, 0.0f) },
        endVector = { size -> RemoteOffset(size.width, 0f) },
        tileMode = tileMode,
    )

/**
 * Creates a horizontal gradient with the given colors dispersed at the provided offset defined in
 * the colorstop pair.
 *
 * Ex:
 * ```
 *  Brush.horizontalGradient(
 *      0.0f to Color.Red,
 *      0.3f to Color.Green,
 *      1.0f to Color.Blue,
 *      startX = 0.0f,
 *      endX = 100.0f
 * )
 * ```
 *
 * @param colorStops Colors and offsets to determine how the colors are dispersed throughout the
 *   vertical gradient
 * @param startX Starting x position of the horizontal gradient. Defaults to 0 which represents the
 *   left of the drawing area
 * @param endX Ending x position of the horizontal gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the right of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.horizontalGradient(
    vararg colorStops: Pair<Float, Color>,
    startX: RemoteFloat?,
    endX: RemoteFloat?,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        start = startX?.let { RemoteOffset(startX, 0f.rf) },
        end = endX?.let { RemoteOffset(endX, 0f.rf) },
        endVector = { size -> RemoteOffset(size.width, 0f) },
        tileMode = tileMode,
    )

/**
 * Creates a vertical gradient with the given colors evenly dispersed within the gradient Ex:
 * ```
 *  Brush.verticalGradient(
 *      listOf(Color.Red, Color.Green, Color.Blue),
 *      startY = 0.0f,
 *      endY = 100.0f
 * )
 *
 * ```
 *
 * @param colors colors Colors to be rendered as part of the gradient
 * @param startY Starting y position of the vertical gradient. Defaults to 0 which represents the
 *   top of the drawing area
 * @param endY Ending y position of the vertical gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the bottom of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
@Suppress("PrimitiveInCollection")
public fun RemoteBrush.Companion.verticalGradient(
    colors: List<Color>,
    startY: RemoteFloat? = null,
    endY: RemoteFloat? = null,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = colors,
        stops = null,
        start = startY?.let { RemoteOffset(0f.rf, it) },
        end = endY?.let { RemoteOffset(0f.rf, it) },
        endVector = { size -> RemoteOffset(0f, size.height) },
        tileMode = tileMode,
    )

/**
 * Creates a vertical gradient with the given colors at the provided offset defined in the
 * [Pair<Float, Color>]
 *
 * Ex:
 * ```
 *  Brush.verticalGradient(
 *      0.1f to Color.Red,
 *      0.3f to Color.Green,
 *      0.5f to Color.Blue,
 *      startY = 0.0f,
 *      endY = 100.0f
 * )
 * ```
 *
 * @param colorStops Colors and offsets to determine how the colors are dispersed throughout the
 *   vertical gradient
 * @param startY Starting y position of the vertical gradient. Defaults to 0 which represents the
 *   top of the drawing area
 * @param endY Ending y position of the vertical gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the bottom of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.verticalGradient(
    vararg colorStops: Pair<Float, Color>,
    startY: RemoteFloat?,
    endY: RemoteFloat?,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    RemoteLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        start = startY?.let { RemoteOffset(0.0f, startY) },
        end = endY?.let { RemoteOffset(0.0f, endY) },
        endVector = { size -> RemoteOffset(0f, size.height) },
        tileMode = tileMode,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteLinearShader(
    public val x0: Float,
    public var y0: Float,
    public var x1: Float,
    public var y1: Float,
    public var colors: IntArray,
    public var positions: FloatArray?,
    public var tileMode: TileMode,
) : LinearGradient(x0, y0, x1, y1, colors, positions, tileMode), RemoteShader {
    override fun apply(paintBundle: PaintBundle) {
        paintBundle.setLinearGradient(colors, 0, positions, x0, y0, x1, y1, tileMode.ordinal)
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@Suppress("PrimitiveInCollection")
public data class RemoteLinearGradient(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val start: RemoteOffset?,
    private val end: RemoteOffset?,
    private val endVector: (RemoteSize) -> RemoteOffset = { size ->
        RemoteOffset(size.width, size.height)
    },
    private val tileMode: TileMode = TileMode.Clamp,
) : RemoteBrush() {

    override fun createShader(size: RemoteSize): Shader {
        val realStart = start ?: RemoteOffset(0.0f, 0.0f)
        val realEnd = end ?: endVector(size)
        validateColorStops(colors = colors, colorStops = stops)
        return RemoteLinearShader(
            x0 = realStart.x.toFloat(),
            y0 = realStart.y.toFloat(),
            x1 = realEnd.x.toFloat(),
            y1 = realEnd.y.toFloat(),
            colors =
                // No change for Android O+, map the colors directly to their argb equivalent
                IntArray(colors.size) { i -> colors[i].toArgb() },
            positions = stops?.toFloatArray(),
            tileMode = tileMode.toAndroidTileMode(),
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
