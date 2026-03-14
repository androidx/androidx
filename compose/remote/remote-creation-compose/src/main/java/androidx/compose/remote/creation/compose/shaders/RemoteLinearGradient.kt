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
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.TileMode as ComposeTileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastMap
import kotlin.collections.toFloatArray

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates. The
 * colors are dispersed at the provided offset defined in the colorstop pair.
 *
 * ```
 *  Brush.linearGradient(
 *      0.0f.rf to Color.Red.rc,
 *      0.3f.rf to Color.Green.rc,
 *      1.0f.rf to Color.Blue.rc,
 *      start = RemoteOffset(0.0f.rf, 50.0f.rf),
 *      end = RemoteOffset(0.0f.rf, 100.0f.rf)
 * )
 * ```
 *
 * @param colorStops Colors and their offset in the gradient area
 * @param start Starting position of the linear gradient. This can be set to [RemoteOffset.Zero] to
 *   position at the far left and top of the drawing area
 * @param end Ending position of the linear gradient. This can be set to [RemoteOffset] Inifnite to
 *   position at the far right and bottom of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun RemoteBrush.Companion.linearGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    start: RemoteOffset? = null,
    end: RemoteOffset? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
    RemoteLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        start = start,
        end = end,
        tileMode = tileMode,
    )

/**
 * Creates a linear gradient with the provided colors along the given start and end coordinates.
 *
 * ```
 *  Brush.linearGradient(
 *      listOf(Color.Red.rc, Color.Blue.rc),
 *      start = Offset(0.rf, 50.rf)
 *      end = Offset(0.rf, 100.rf)
 * )
 * ```
 *
 * @param colors Colors to be rendered as part of the gradient
 * @param start Starting position of the linear gradient. This can be set to [RemoteOffset.Zero] to
 *   position at the far left and top of the drawing area
 * @param end Ending position of the linear gradient. This can be set to [RemoteOffset] Infinite to
 *   position at the far right and bottom of the drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.linearGradient(
    colors: List<RemoteColor>,
    start: RemoteOffset? = null,
    end: RemoteOffset? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
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
 *      listOf(Color.Red.rc, Color.Green.rc, Color.Blue.rc),
 *      startX = 10.rf,
 *      endX = 20.rf
 * )
 * ```
 *
 * @param colors colors to be rendered as part of the gradient
 * @param startX Starting x position of the horizontal gradient. Defaults to 0 which represents the
 *   left of the drawing area
 * @param endX Ending x position of the horizontal gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the right of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@Stable
public fun RemoteBrush.Companion.horizontalGradient(
    colors: List<RemoteColor>,
    startX: RemoteFloat? = null,
    endX: RemoteFloat? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
    RemoteLinearGradient(
        colors = colors,
        stops = null,
        start = startX?.let { RemoteOffset(it, 0.0f.rf) },
        end = endX?.let { RemoteOffset(it, 0.0f.rf) },
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
 *      0.rf to Color.Red.rc,
 *      0.3.rf to Color.Green.rc,
 *      1.rf to Color.Blue.rc,
 *      startX = 0.rf,
 *      endX = 100.rf
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
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun RemoteBrush.Companion.horizontalGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    startX: RemoteFloat?,
    endX: RemoteFloat?,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
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
 *      listOf(Color.Red.rc, Color.Green.rc, Color.Blue.rc),
 *      startY = 0.rf,
 *      endY = 100.rf
 * )
 * ```
 *
 * @param colors colors to be rendered as part of the gradient
 * @param startY Starting y position of the vertical gradient. Defaults to 0 which represents the
 *   top of the drawing area
 * @param endY Ending y position of the vertical gradient. Defaults to [Float.POSITIVE_INFINITY]
 *   which indicates the bottom of the specified drawing area
 * @param tileMode Determines the behavior for how the shader is to fill a region outside its
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@Stable
@Suppress("PrimitiveInCollection")
public fun RemoteBrush.Companion.verticalGradient(
    colors: List<RemoteColor>,
    startY: RemoteFloat? = null,
    endY: RemoteFloat? = null,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
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
 *   bounds. Defaults to [ComposeTileMode.Clamp] to repeat the edge pixels
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun RemoteBrush.Companion.verticalGradient(
    vararg colorStops: Pair<RemoteFloat, RemoteColor>,
    startY: RemoteFloat?,
    endY: RemoteFloat?,
    tileMode: ComposeTileMode = ComposeTileMode.Clamp,
): RemoteBrush =
    RemoteLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        start = startY?.let { RemoteOffset(0.0f.rf, startY) },
        end = endY?.let { RemoteOffset(0.0f.rf, endY) },
        endVector = { size -> RemoteOffset(0f, size.height) },
        tileMode = tileMode,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteLinearShader(
    public var x0: RemoteFloat,
    public var y0: RemoteFloat,
    public var x1: RemoteFloat,
    public var y1: RemoteFloat,
    public var colors: List<RemoteColor>,
    public var positions: List<RemoteFloat>?,
    public var tileMode: ComposeTileMode,
) : RemoteShader() {
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

        paintBundle.setLinearGradient(
            colorsArray,
            mask,
            positionsArray,
            x0.getFloatIdForCreationState(creationState),
            y0.getFloatIdForCreationState(creationState),
            x1.getFloatIdForCreationState(creationState),
            y1.getFloatIdForCreationState(creationState),
            tileMode.toAndroidTileMode().ordinal,
        )
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public data class RemoteLinearGradient(
    private val colors: List<RemoteColor>,
    private val stops: List<RemoteFloat>? = null,
    private val start: RemoteOffset?,
    private val end: RemoteOffset?,
    private val endVector: (RemoteSize) -> RemoteOffset = { size ->
        RemoteOffset(size.width, size.height)
    },
    private val tileMode: ComposeTileMode = ComposeTileMode.Clamp,
) : RemoteBrush() {

    override fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader {
        val realStart = start ?: RemoteOffset(0.0f.rf, 0.0f.rf)
        val realEnd = end ?: endVector(size)

        return RemoteLinearShader(
            x0 = resolve(realStart.x, size.width),
            y0 = resolve(realStart.y, size.height),
            x1 = resolve(realEnd.x, size.width),
            y1 = resolve(realEnd.y, size.height),
            colors = colors,
            positions = stops,
            tileMode = tileMode,
        )
    }
}
