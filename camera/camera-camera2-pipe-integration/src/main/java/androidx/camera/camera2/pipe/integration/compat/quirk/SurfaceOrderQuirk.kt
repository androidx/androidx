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
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk
import androidx.camera.core.internal.compat.workaround.SurfaceSorter
import java.util.Locale

/**
 * QuirkSummary
 * - Bug Id: 196755459, 205340278
 * - Description: Quirk that requires Preview surface is in front of the MediaCodec surface when
 *   creating a CameraCaptureSession. On some Samsung devices, create CameraCaptureSession will fail
 *   silently if the input surface list does not have a Preview surface in front of a MediaCodec
 *   surface. On Pixel 1, when the MediaCodec surface size is 3840x2160(UHD) and the input list for
 *   creating CameraCaptureSession does not have Preview surface in front of the MediaCodec surface
 *   and ImageCapture surface, Preview will have interlaced color lines after start recording.
 *   MediaCodec surface resolutions for 1920x1080(FHD), 1280x720(HD) and 720x480(SD) do not have
 *   this issue. Not clearly know if there is another resolution will encounter this issue, but this
 *   quirk should be safe to apply regardless the video surface resolution since the workaround just
 *   sorts the surface list while creating CameraCaptureSession.
 * - Device(s): Some Samsung devices and Pixel 1
 *
 * @see SurfaceSorter
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class SurfaceOrderQuirk : Quirk {
    public companion object {
        private val BUILD_HARDWARE_SET = listOf("samsungexynos7570", "samsungexynos7870")

        public fun isEnabled(): Boolean {
            // Apply this quirk to all devices to avoid that there are still some other devices with
            // the same quirk. The workaround is only to sort the input surface list when creating
            // CameraCaptureSession, so it does not cost much performance and should be safe to
            // apply.
            return (isSamsungDevice() &&
                BUILD_HARDWARE_SET.contains(Build.HARDWARE.lowercase(Locale.getDefault())))
        }
    }
}
