/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.PixelDensity

/**
 * Converts this [IntVolumeSize] to a [FloatSize3d] object in meters.
 *
 * @param pixelDensity The pixel density to use for conversion.
 * @return a [FloatSize3d] object representing the volume size in meters.
 */
internal fun IntVolumeSize.toDimensionsInMeters(pixelDensity: PixelDensity): FloatSize3d =
    FloatSize3d(
        width.pxToMeters(pixelDensity),
        height.pxToMeters(pixelDensity),
        depth.pxToMeters(pixelDensity),
    )

/**
 * Creates an [IntVolumeSize] from a [FloatSize3d] object in meters.
 *
 * The dimensions in meters are rounded to the nearest pixel value.
 *
 * @param pixelDensity The pixel density to use for conversion.
 * @return an [IntVolumeSize] object representing the same volume size in pixels.
 */
internal fun FloatSize3d.toIntVolumeSize(pixelDensity: PixelDensity): IntVolumeSize =
    IntVolumeSize(
        width.roundMetersToPx(pixelDensity),
        height.roundMetersToPx(pixelDensity),
        depth.roundMetersToPx(pixelDensity),
    )

/**
 * Converts this [DpVolumeSize] to a [FloatSize3d] object in meters.
 *
 * @param density The pixel density of the display.
 * @param pixelDensity The XR pixel density.
 * @return a [FloatSize3d] object representing the volume size in meters
 */
internal fun DpVolumeSize.toDimensionsInMeters(
    density: Density,
    pixelDensity: PixelDensity,
): FloatSize3d =
    FloatSize3d(
        width.toMeters(density, pixelDensity),
        height.toMeters(density, pixelDensity),
        depth.toMeters(density, pixelDensity),
    )

/**
 * Creates a [DpVolumeSize] from a [FloatSize3d] object in meters.
 *
 * @param density The pixel density of the display.
 * @param pixelDensity The XR pixel density.
 * @return a [DpVolumeSize] object representing the same volume size in Dp.
 */
internal fun FloatSize3d.toDpVolumeSize(
    density: Density,
    pixelDensity: PixelDensity,
): DpVolumeSize =
    DpVolumeSize(
        width.metersToDp(density, pixelDensity),
        height.metersToDp(density, pixelDensity),
        depth.metersToDp(density, pixelDensity),
    )
