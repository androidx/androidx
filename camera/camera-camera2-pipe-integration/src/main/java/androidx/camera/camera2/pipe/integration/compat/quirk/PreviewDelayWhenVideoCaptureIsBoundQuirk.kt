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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isHuaweiDevice
import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk

/**
 * QuirkSummary
 * - Bug Id: b/223643510
 * - Description: Quirk indicates Preview is delayed on some Huawei devices when the Preview uses
 *   certain resolutions and VideoCapture is bound. The quirk applies on all Huawei devices since
 *   there is a certain number of unknown devices with this issue.
 * - Device(s): Some Huawei devices.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class PreviewDelayWhenVideoCaptureIsBoundQuirk :
    CaptureIntentPreviewQuirk, SurfaceProcessingQuirk {
    /*
    Known devices:
            "HWELE",  // P30
            "HW-02L", // P30 Pro
            "HWVOG",  // P30 Pro
            "HWYAL",  // Nova 5T
            "HWLYA",  // Mate 20 Pro
            "HWCOL",  // Honor 10
            "HWPAR"   // Nova 3

    Known models:
            "ELS-AN00", "ELS-TN00", "ELS-NX9", "ELS-N04"  // P40 Pro

    Known others:
            mate40
            honor v2
     */

    public companion object {
        public fun isEnabled(): Boolean = isHuaweiDevice()
    }
}
