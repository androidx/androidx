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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk

/**
 * Quirk that still capture with flash on/auto or front-camera high-zoom multi-stream capture
 * requires stopRepeating() being called ahead of capture.
 *
 * QuirkSummary
 * - Bug Id: 172036589, 578857056
 * - Description: On some devices like Samsung SM-A716B, it could lead to CaptureRequest not being
 *   completed when taking photos in dark environment with flash on/auto. On Snapdragon Samsung
 *   Galaxy Note10+ (device "d2q", e.g., SM-N975U1) front camera (camera ID "1"), submitting a still
 *   capture with digital zoom while multiple repeating streams (e.g., Preview + ImageAnalysis) are
 *   active can cause the CamX JPEGEncodeLiveSnapshotPreview IPE0 and IPE1 nodes to deadlock on
 *   output fences. Calling stopRepeating ahead of still capture and setRepeating again after
 *   capture is done fixes the issue.
 * - Device(s): Samsung SM-A716, Snapdragon Samsung Galaxy Note10+ (d2q / SM-N975U1)
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class StillCaptureFlashStopRepeatingQuirk : Quirk {
    public fun shouldStopRepeatingForFlash(): Boolean = isSamsungSmA716

    public fun shouldStopRepeatingForFrontCameraZoom(
        cameraId: String?,
        hasMultipleRepeatingStreams: Boolean,
        isZoomed: Boolean,
    ): Boolean =
        isSamsungNote10PlusSnapdragon &&
            cameraId == FRONT_CAMERA_ID &&
            hasMultipleRepeatingStreams &&
            isZoomed

    public companion object {
        private const val FRONT_CAMERA_ID = "1"

        private val isSamsungSmA716: Boolean
            get() = isSamsungDevice() && Build.MODEL.uppercase().startsWith("SM-A716")

        private val isSamsungNote10PlusSnapdragon: Boolean
            get() = isSamsungDevice() && "d2q".equals(Build.DEVICE, ignoreCase = true)

        public fun isEnabled(): Boolean {
            return isSamsungSmA716 || isSamsungNote10PlusSnapdragon
        }
    }
}
