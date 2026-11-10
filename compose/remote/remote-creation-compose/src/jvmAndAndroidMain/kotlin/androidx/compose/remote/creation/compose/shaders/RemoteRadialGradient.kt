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

@file:JvmName("RemoteRadialGradientKt")

package androidx.compose.remote.creation.compose.shaders

import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.util.fastMap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Creates a radial gradient with the given colors at the provided offset defined in the colorstop
 * pair.
 *
 * ```
 * RemoteBrush.radialGradient(
 *      0.0f.rf to 0xFFFF0000.toInt().rc,
 *      0.3f.rf to 0xFF00FF00.toInt().rc,
 *      1.0f.rf to 0xFF0000FF.toInt().rc,
 *      center = RemoteOffset(side1 / 2.0f, side2 / 2.0f),
 *      radius = side1 / 2.0f,
 *      tileMode = RemoteTileMode.Repeated,
 * )
 * ```
 *
 * @param colorStops colors and offsets to determine how the colors are dispersed throughout the
 *   radial gradient
 * @param center center position of the radial gradient circle, or `null` to center within the
 *   drawing area. [Float.POSITIVE_INFINITY] can be used for either [RemoteOffset.x] or
 *   [RemoteOffset.y] to indicate the far right or far bottom of the drawing area respectively.
 * @param radius radius for the radial gradient, or `null` to use the largest radius fitting the
 *   drawing area
 * @param tileMode determines how the shader fills regions outside its bounds
 * @return a [RemoteShaderBrush] that draws a radial gradient with the given [colorStops]
 */
@Stable
public fun RemoteBrush.Companion.radialGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: RemoteTileMode = RemoteTileMode.Clamp,
): RemoteShaderBrush =
    RemoteRadialGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

/**
 * Creates a radial gradient with the given colors evenly dispersed within the gradient.
 *
 * ```
 * RemoteBrush.radialGradient(
 *      listOf(0xFFFF0000.toInt().rc, 0xFF0000FF.toInt().rc),
 *      center = RemoteOffset(side1 / 2.0f, side2 / 2.0f),
 *      radius = side1 / 2.0f,
 *      tileMode = RemoteTileMode.Repeated,
 * )
 * ```
 *
 * @param colors colors to be rendered as part of the gradient
 * @param center center position of the radial gradient circle, or `null` to center within the
 *   drawing area. [Float.POSITIVE_INFINITY] can be used for either [RemoteOffset.x] or
 *   [RemoteOffset.y] to indicate the far right or far bottom of the drawing area respectively.
 * @param radius radius for the radial gradient, or `null` to use the largest radius fitting the
 *   drawing area
 * @param tileMode determines how the shader fills regions outside its bounds
 * @return a [RemoteShaderBrush] that draws a radial gradient across the given [colors]
 */
@Stable
public fun RemoteBrush.Companion.radialGradient(
    colors: List<RemoteColor>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: RemoteTileMode = RemoteTileMode.Clamp,
): RemoteShaderBrush =
    RemoteRadialGradient(
        colors = colors,
        stops = null,
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

@Immutable
internal data class RemoteRadialGradient(
    private val colors: List<RemoteColor>,
    private val stops: List<RemoteFloat>? = null,
    private val center: RemoteOffset?,
    private val radius: RemoteFloat?,
    private val tileMode: RemoteTileMode = RemoteTileMode.Clamp,
) : RemoteShaderBrush() {

    override fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader {
        val realCenter = center ?: size.center
        val realRadius = radius ?: (size.width.min(size.height) / 2f)

        val centerX = resolve(realCenter.x, size.width)
        val centerY = resolve(realCenter.y, size.height)
        val resolvedRadius = resolve(realRadius, size.minDimension / 2f)

        return RemoteRadialShader(
            centerX = centerX,
            centerY = centerY,
            radius = resolvedRadius,
            colors = colors,
            positions = stops,
            tileMode = tileMode,
        )
    }
}

internal class RemoteRadialShader(
    val centerX: RemoteFloat,
    val centerY: RemoteFloat,
    val radius: RemoteFloat,
    val colors: List<RemoteColor>,
    val positions: List<RemoteFloat>?,
    val tileMode: RemoteTileMode,
) : RemoteShader() {

    override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
        var mask = 0
        val colorsArray =
            IntArray(colors.size) { i ->
                val color = colors[i]
                val constantValue = color.constantValueOrNull
                if (constantValue != null) {
                    constantValue
                } else {
                    mask = mask or (1 shl i)
                    color.getIdForCreationState(creationState)
                }
            }
        val positionsArray =
            positions?.fastMap { it.getFloatIdForCreationState(creationState) }?.toFloatArray()

        paintBundle.setRadialGradient(
            colorsArray,
            mask,
            positionsArray,
            centerX.getFloatIdForCreationState(creationState),
            centerY.getFloatIdForCreationState(creationState),
            radius.getFloatIdForCreationState(creationState),
            tileMode.value,
        )
    }
}
