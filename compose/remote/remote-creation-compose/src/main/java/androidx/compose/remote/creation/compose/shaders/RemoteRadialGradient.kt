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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode as ComposeTileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastMap

/**
 * Creates a radial gradient with the given colors at the provided offset defined in the colorstop
 * pair.
 *
 * ```
 * Brush.radialGradient(
 *      0.0f to Color.Red.rc,
 *      0.3f to Color.Green.rc,
 *      1.0f to Color.Blue.rc,
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
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.radialGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteRadialGradient =
    RemoteRadialGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

/**
 * Creates a radial gradient with the given colors evenly dispersed within the gradient
 *
 * ```
 * Brush.radialGradient(
 *      listOf(Color.Red.rc, Color.Blue.rc),
 *      center = Offset(side1 / 2.0f, side2 / 2.0f),
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
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.radialGradient(
    colors: List<RemoteColor>,
    center: RemoteOffset? = null,
    radius: RemoteFloat? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
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
public data class RemoteRadialGradient(
    private val colors: List<RemoteColor>,
    private val stops: List<RemoteFloat>? = null,
    private val center: RemoteOffset?,
    private val radius: RemoteFloat?,
    private val tileMode: ComposeTileMode = ComposeTileMode.Clamp,
) : RemoteBrush() {

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteRadialShader(
    public var centerX: RemoteFloat,
    public var centerY: RemoteFloat,
    public var radius: RemoteFloat,
    public var colors: List<RemoteColor>,
    public var positions: List<RemoteFloat>?,
    public var tileMode: ComposeTileMode,
) : RemoteShader() {
    override var remoteMatrix3x3: RemoteMatrix3x3? = null

    override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
        var mask = 0
        val colorsArray =
            IntArray(colors.size) { i ->
                val color = colors[i]
                val constantValue = color.constantValueOrNull
                if (constantValue != null) {
                    constantValue.toArgb()
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
            tileMode.toAndroidTileMode().ordinal,
        )
    }
}
