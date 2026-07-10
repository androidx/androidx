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

package androidx.xr.compose.unit

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.PixelDensity
import kotlin.math.roundToInt

/** Converts a Float value representing meters in [ActivitySpace] to virtual pixels. */
internal fun Float.metersToPx(pixelDensity: PixelDensity): Float =
    pixelDensity.convertMetersToPixels(this)

/** Converts a Float value representing virtual pixels to meters in [ActivitySpace]. */
internal fun Float.pxToMeters(pixelDensity: PixelDensity): Float =
    pixelDensity.convertPixelsToMeters(this)

/** Converts an Int value representing virtual pixels to meters in [ActivitySpace]. */
internal fun Int.pxToMeters(pixelDensity: PixelDensity): Float =
    this.toFloat().pxToMeters(pixelDensity)

/** Converts a Float value representing meters in [ActivitySpace] to rounded virtual pixels. */
internal fun Float.roundMetersToPx(pixelDensity: PixelDensity): Int =
    metersToPx(pixelDensity).roundToInt()

/** Uses [density] to convert a [Dp] value to meters in [ActivitySpace]. */
internal fun Dp.toMeters(density: Density, pixelDensity: PixelDensity): Float =
    with(density) { toPx().pxToMeters(pixelDensity) }

/** Uses [density] to convert a Float value representing meters in [ActivitySpace] to [Dp]. */
internal fun Float.metersToDp(density: Density, pixelDensity: PixelDensity): Dp =
    with(density) { metersToPx(pixelDensity).toDp() }
