/*
 * Copyright 2023 The Android Open Source Project
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
@file:Suppress("DEPRECATION")
@file:JvmName("ViewfinderSurfaceRequestUtil")

package androidx.camera.viewfinder

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import androidx.camera.viewfinder.CameraViewfinder.ImplementationMode

/**
 * Populates [ViewfinderSurfaceRequest.Builder] from [CameraCharacteristics].
 *
 * The [CameraCharacteristics] will be used to populate information including lens facing, sensor
 * orientation and [ImplementationMode]. If the hardware level is legacy, the [ImplementationMode]
 * will be set to [ImplementationMode.COMPATIBLE].
 */
@Deprecated(
    message = "Use androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest as argument",
    replaceWith =
        ReplaceWith(
            "populateFromCharacteristics returning " +
                "androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.Builder"
        )
)
@SuppressLint("ClassVerificationFailure")
fun ViewfinderSurfaceRequest.Builder.populateFromCharacteristics(
    cameraCharacteristics: CameraCharacteristics
): ViewfinderSurfaceRequest.Builder {
    setLensFacing(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!)
    setSensorOrientation(cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!)
    if (
        cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    ) {
        setImplementationMode(ImplementationMode.COMPATIBLE)
    }
    return this
}
