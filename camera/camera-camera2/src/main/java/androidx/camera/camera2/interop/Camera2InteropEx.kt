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

@file:JvmName("Camera2InteropEx")

package androidx.camera.camera2.interop

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector

/**
 * Returns the Camera2 camera ID from this [CameraInfo].
 *
 * @throws IllegalArgumentException if this [CameraInfo] does not contain Camera2 information
 */
@get:JvmSynthetic
public val CameraInfo.cameraId: String
    get() = Camera2Interop.getCameraId(this)

/**
 * Returns the [CameraCharacteristics] from this [CameraInfo].
 *
 * @throws IllegalArgumentException if this [CameraInfo] does not contain Camera2 information
 */
@get:JvmSynthetic
public val CameraInfo.cameraCharacteristics: CameraCharacteristics
    get() = Camera2Interop.getCameraCharacteristics(this)

/**
 * Creates a [CameraSelector] targeting this camera ID string.
 *
 * @return [CameraSelector] matching this camera ID
 */
@JvmSynthetic
public fun String.toCameraSelector(): CameraSelector =
    Camera2Interop.getCameraSelectorFromCameraId(this)
