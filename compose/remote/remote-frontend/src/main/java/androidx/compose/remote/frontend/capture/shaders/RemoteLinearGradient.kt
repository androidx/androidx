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

import android.graphics.LinearGradient
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.frontend.state.RemoteMatrix3x3
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.toAndroidTileMode
import kotlin.math.abs

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
 * @see androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.linearGradient(
    vararg colorStops: Pair<Float, Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
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
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.linearGradient(
    colors: List<Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
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
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.horizontalGradient(
    colors: List<Color>,
    startX: Float = 0.0f,
    endX: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient = linearGradient(colors, Offset(startX, 0.0f), Offset(endX, 0.0f), tileMode)

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
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.horizontalGradient(
    vararg colorStops: Pair<Float, Color>,
    startX: Float = 0.0f,
    endX: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    linearGradient(
        *colorStops,
        start = Offset(startX, 0.0f),
        end = Offset(endX, 0.0f),
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
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.verticalGradient(
    colors: List<Color>,
    startY: Float = 0.0f,
    endY: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient = linearGradient(colors, Offset(0.0f, startY), Offset(0.0f, endY), tileMode)

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
 * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
 */
@Stable
public fun RemoteBrush.Companion.verticalGradient(
    vararg colorStops: Pair<Float, Color>,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): RemoteLinearGradient =
    linearGradient(
        *colorStops,
        start = Offset(0.0f, startY),
        end = Offset(0.0f, endY),
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
public data class RemoteLinearGradient(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val start: Offset,
    private val end: Offset,
    private val tileMode: TileMode = TileMode.Clamp,
) : RemoteBrush() {

    override val intrinsicSize: Size
        get() =
            Size(
                if (start.x.isFinite() && end.x.isFinite()) abs(start.x - end.x) else Float.NaN,
                if (start.y.isFinite() && end.y.isFinite()) abs(start.y - end.y) else Float.NaN,
            )

    override fun createShader(size: Size): Shader {
        val startX = if (start.x == Float.POSITIVE_INFINITY) size.width else start.x
        val startY = if (start.y == Float.POSITIVE_INFINITY) size.height else start.y
        val endX = if (end.x == Float.POSITIVE_INFINITY) size.width else end.x
        val endY = if (end.y == Float.POSITIVE_INFINITY) size.height else end.y
        val from = Offset(startX, startY)
        val to = Offset(endX, endY)
        validateColorStops(colors = colors, colorStops = stops)
        val numTransparentColors = countTransparentColors(colors = colors)
        return RemoteLinearShader(
            x0 = from.x,
            y0 = from.y,
            x1 = to.x,
            y1 = to.y,
            colors =
                makeTransparentColors(colors = colors, numTransparentColors = numTransparentColors),
            positions =
                makeTransparentStops(
                    stops = stops,
                    colors = colors,
                    numTransparentColors = numTransparentColors,
                ),
            tileMode = tileMode.toAndroidTileMode(),
        )
    }

    override fun toComposeUi(): Brush {
        return if (stops != null) {
            return Brush.linearGradient(
                colorStops = stops.zip<Float, Color>(colors).toTypedArray<Pair<Float, Color>>(),
                start = start,
                end = end,
                tileMode = tileMode,
            )
        } else {
            return Brush.linearGradient(
                colors = colors,
                start = start,
                end = end,
                tileMode = tileMode,
            )
        }
    }
}

/**
 * Returns the number of transparent (alpha = 0) values that aren't at the beginning or end of the
 * gradient so that the color stops can be added. On O and newer devices, this always returns 0
 * because no stops need to be added.
 */
internal fun countTransparentColors(colors: List<Color>): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return 0
    }
    var numTransparentColors = 0
    // Don't count the first and last value because we don't add stops for those
    for (i in 1 until colors.lastIndex) {
        if (colors[i].alpha == 0f) {
            numTransparentColors++
        }
    }
    return numTransparentColors
}

/**
 * There was a change in behavior between Android N and O with how transparent colors are
 * interpolated with skia gradients. More specifically Android O treats all fully transparent colors
 * the same regardless of the rgb channels, however, Android N and older releases interpolated
 * between the color channels as well. Because Color.Transparent is transparent black, this would
 * introduce some muddy colors as part of gradients with transparency for Android N and below. In
 * order to make gradient rendering consistent and match the behavior of Android O+, detect whenever
 * Color.Transparent is used and a stop matching the color of the previous value, but alpha = 0 is
 * added and another stop at the same point with the same color as the following value, but with
 * alpha = 0 is used.
 */
internal fun makeTransparentColors(colors: List<Color>, numTransparentColors: Int): IntArray {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // No change for Android O+, map the colors directly to their argb equivalent
        return IntArray(colors.size) { i -> colors[i].toArgb() }
    }
    val values = IntArray(colors.size + numTransparentColors)
    var valuesIndex = 0
    val lastIndex = colors.lastIndex
    colors.forEachIndexed { index, color ->
        if (color.alpha == 0f) {
            if (index == 0) {
                values[valuesIndex++] = colors[1].copy(alpha = 0f).toArgb()
            } else if (index == lastIndex) {
                values[valuesIndex++] = colors[index - 1].copy(alpha = 0f).toArgb()
            } else {
                val previousColor = colors[index - 1]
                values[valuesIndex++] = previousColor.copy(alpha = 0f).toArgb()

                val nextColor = colors[index + 1]
                values[valuesIndex++] = nextColor.copy(alpha = 0f).toArgb()
            }
        } else {
            values[valuesIndex++] = color.toArgb()
        }
    }
    return values
}

/**
 * See [makeTransparentColors].
 *
 * On N and earlier devices that have transparent values, we must duplicate the color stops for
 * fully transparent values so that the color value before and after can be interpolated.
 */
internal fun makeTransparentStops(
    stops: List<Float>?,
    colors: List<Color>,
    numTransparentColors: Int,
): FloatArray? {
    if (numTransparentColors == 0) {
        return stops?.toFloatArray()
    }
    val newStops = FloatArray(colors.size + numTransparentColors)
    newStops[0] = stops?.get(0) ?: 0f
    var newStopsIndex = 1
    for (i in 1 until colors.lastIndex) {
        val color = colors[i]
        val stop = stops?.get(i) ?: i.toFloat() / colors.lastIndex
        newStops[newStopsIndex++] = stop
        if (color.alpha == 0f) {
            newStops[newStopsIndex++] = stop
        }
    }
    newStops[newStopsIndex] = stops?.get(colors.lastIndex) ?: 1f
    return newStops
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
