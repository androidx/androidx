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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Build
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk

/**
 * QuirkSummary
 * - Bug Id: 315071023
 * - Description: Addresses a potential issue where JPEG photo captures may result in
 *   smaller-than-expected output resolutions. In certain cases, even when configuring the maximum
 *   supported JPEG output size using
 *   [android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP], the captured
 *   ImageProxy may contain a photo buffer with a smaller resolution. This can lead to unexpected
 *   cropping or transformation issues during post-capture processing.
 * - Device(s): Redmi note 8 pro - front camera
 */
@SuppressLint("CameraXQuirksClassDetector")
public object JpegCaptureDownsizingQuirk : SoftwareJpegEncodingPreferredQuirk {

    private val KNOWN_AFFECTED_FRONT_CAMERA_DEVICES = setOf("redmi note 8 pro")

    public fun isEnabled(cameraMetadata: androidx.camera.camera2.pipe.CameraMetadata): Boolean {
        return KNOWN_AFFECTED_FRONT_CAMERA_DEVICES.contains(Build.MODEL.lowercase()) &&
            cameraMetadata[LENS_FACING] == LENS_FACING_FRONT
    }
}
