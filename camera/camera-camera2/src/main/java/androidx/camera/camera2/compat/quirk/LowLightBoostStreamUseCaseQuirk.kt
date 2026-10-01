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
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsLowLightBoost
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/563170954
 * - Description: On Samsung devices supporting Low Light Boost, when a single Preview stream is
 *   configured with `SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL` (4), the camera HAL
 *   treats the stream as a `RECORD` scenario and rejects
 *   `CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY` (6), falling back to reporting
 *   `CONTROL_AE_MODE_ON` (1) in `CaptureResult`. Prioritizing `(PRIV, PREVIEW,
 *   StreamUseCase.PREVIEW)` ensures a single Preview stream uses
 *   `SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW` (1) so hardware Low Light Boost can be enabled.
 * - Device(s): Samsung devices supporting Low Light Boost
 */
@SuppressLint("CameraXQuirksClassDetector")
public class LowLightBoostStreamUseCaseQuirk : Quirk {
    public companion object {
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean =
            isSamsungDevice() && cameraMetadata.supportsLowLightBoost
    }
}
