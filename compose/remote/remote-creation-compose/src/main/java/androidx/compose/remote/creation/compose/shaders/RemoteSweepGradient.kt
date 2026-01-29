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
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastMap

/**
 * Creates a sweep gradient with the given colors dispersed around the center with offsets defined
 * in each colorstop pair. The sweep begins relative to 3 o'clock and continues clockwise until it
 * reaches the starting position again.
 *
 * Ex:
 * ```
 *  Brush.sweepGradient(
 *      0.0.rf to Color.Red.rc,
 *      0.3.rf to Color.Green.rc,
 *      1.0.rf to Color.Blue.rc,
 *      center = Offset(0.rf, 100.rf)
 * )
 * ```
 *
 * @param colorStops Colors and offsets to determine how the colors are dispersed throughout the
 *   sweep gradient
 * @param center Center position of the sweep gradient circle. If this is set to null then the
 *   center of the drawing area is used as the center for the sweep gradient
 */
@Stable
public fun RemoteBrush.Companion.sweepGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    center: RemoteOffset? = null,
): RemoteSweepGradient =
    RemoteSweepGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
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
 *      listOf(Color.Red.rc, Color.Blue.rc),
 *      center = Offset(10.rf, 20.rf)
 * )
 * ```
 *
 * @param colors List of colors to fill the sweep gradient
 * @param center Center position of the sweep gradient circle. If this is null then the center of
 *   the drawing area is used as the center for the sweep gradient
 */
@Stable
public fun RemoteBrush.Companion.sweepGradient(
    colors: List<RemoteColor>,
    center: RemoteOffset? = null,
): RemoteSweepGradient = RemoteSweepGradient(colors = colors, stops = null, center = center)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public data class RemoteSweepGradient(
    private val colors: List<RemoteColor>,
    private val stops: List<RemoteFloat>? = null,
    private val center: RemoteOffset? = null,
) : RemoteBrush() {

    override fun RemoteStateScope.createShader(size: RemoteSize): Shader {
        val realCenter = center ?: size.center
        val centerX = resolve(realCenter.x, size.width)
        val centerY = resolve(realCenter.y, size.height)
        return RemoteSweepShader(
            centerX = centerX,
            centerY = centerY,
            colors = colors,
            positions = stops,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSweepShader(
    public var centerX: RemoteFloat,
    public var centerY: RemoteFloat,
    public var colors: List<RemoteColor>,
    public var positions: List<RemoteFloat>?,
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

        paintBundle.setSweepGradient(
            colorsArray,
            mask,
            positionsArray,
            centerX.getFloatIdForCreationState(creationState),
            centerY.getFloatIdForCreationState(creationState),
        )
    }
}
