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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * Quirk that disables concurrent camera support on devices where simultaneous front and back camera
 * streaming is broken in the vendor HAL.
 *
 * QuirkSummary
 * - Bug Id: 579702968
 * - Description: Although the device advertises
 *   [android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT] and reports concurrent front and
 *   back camera IDs via [android.hardware.camera2.CameraManager.getConcurrentCameraIds], opening
 *   the second camera while the first camera is active causes the vendor foldable HAL session
 *   (`CusCameraDeviceSessionFoldable`) to tear down and disconnect the active camera, triggering a
 *   mutual-eviction reopen loop and downstream `-38` (`ENOSYS`) errors even with minimal
 *   dual-preview streams. Because eviction occurs when the second camera is opened, stream or size
 *   adjustments, retries, and session resets do not prevent eviction or allow simultaneous open,
 *   and single-camera `StreamSharing` cannot provide physically opposite front and back lenses.
 * - Device(s): OPPO Find N3 Flip (PHT110)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ConcurrentCameraUnsupportedQuirk : Quirk {
    public companion object {
        @JvmStatic public fun isEnabled(): Boolean = isOppoFindN3Flip()

        private fun isOppoFindN3Flip(): Boolean =
            Device.isOppoDevice() && "PHT110".equals(Build.MODEL, ignoreCase = true)
    }
}
