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
package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.impl.Quirk

/**
 * A quirk to denote a new surface should be acquired while the camera is going to create a new
 * [android.hardware.camera2.CameraCaptureSession].
 *
 * QuirkSummary
 * - Bug Id: 145725334
 * - Description: When using TextureView below Android API 23, it releases
 *   [android.graphics.SurfaceTexture] when activity is stopped.
 * - Device(s): Devices in Android API version <= 23
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class TextureViewIsClosedQuirk : Quirk {
    public companion object {
        @Suppress("UNUSED_PARAMETER")
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
        }
    }
}
