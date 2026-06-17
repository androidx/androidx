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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.unit.metersToPx
import androidx.xr.compose.unit.pxToMeters
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PixelDensity

/** Converts the translation from pixels to meters. */
internal fun Pose.pxToMeters(pixelDensity: PixelDensity): Pose =
    Pose(translation = translation.pxToMeters(pixelDensity), rotation = rotation)

/** Converts the translation from meters to pixels. */
internal fun Pose.metersToPx(pixelDensity: PixelDensity): Pose =
    Pose(translation = translation.metersToPx(pixelDensity), rotation = rotation)

/** Converts values from pixels to meters. */
internal fun Vector3.pxToMeters(pixelDensity: PixelDensity): Vector3 =
    Vector3(x.pxToMeters(pixelDensity), y.pxToMeters(pixelDensity), z.pxToMeters(pixelDensity))

/** Converts values from meters to pixels. */
internal fun Vector3.metersToPx(pixelDensity: PixelDensity): Vector3 =
    Vector3(x.metersToPx(pixelDensity), y.metersToPx(pixelDensity), z.metersToPx(pixelDensity))
