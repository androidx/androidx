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

/**
 * Quirk that still capture with flash on/auto requires stopRepeating() being called ahead of
 * capture.
 *
 * QuirkSummary
 * - Bug Id: 172036589
 * - Description: On some devices like Samsung SM-A716B, it could lead to CaptureRequest not being
 *   completed when taking photos in dark environment with flash on/auto. Calling stopRepeating
 *   ahead of still capture and setRepeating again after capture is done can fix the issue.
 * - Device(s): Samsung SM-A716
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class StillCaptureFlashStopRepeatingQuirk : Quirk {
    public companion object {
        public fun isEnabled(): Boolean {
            return isSamsungDevice() && Build.MODEL.uppercase().startsWith("SM-A716")
        }
    }
}
