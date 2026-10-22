/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("RemoteLinearGradientKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.shaders

import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.toRemoteTileMode
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.TileMode as ComposeTileMode

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
): RemoteShaderBrush =
    linearGradient(
        colors = colors,
        start = start,
        end = end,
        tileMode = tileMode.toRemoteTileMode(),
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
): RemoteShaderBrush =
    horizontalGradient(
        colors = colors,
        startX = startX,
        endX = endX,
        tileMode = tileMode.toRemoteTileMode(),
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
): RemoteShaderBrush =
    verticalGradient(
        colors = colors,
        startY = startY,
        endY = endY,
        tileMode = tileMode.toRemoteTileMode(),
    )
