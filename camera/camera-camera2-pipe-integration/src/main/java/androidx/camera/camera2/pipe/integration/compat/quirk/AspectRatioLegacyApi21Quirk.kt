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

import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.integration.compat.workaround.TargetAspectRatio
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary Bug Id: b/128924712 Description: Quirk that produces stretched use cases on all the
 * legacy API 21 devices. If the device is LEGACY + Android 5.0, then return the same aspect ratio
 * as maximum JPEG resolution. The Camera2 LEGACY mode API always sends the HAL a configure call
 * with the same aspect ratio as the maximum JPEG resolution, and do the cropping/scaling before
 * returning the output. There is a bug because of a flipped scaling factor in the intermediate
 * texture transform matrix, and it was fixed in L MR1. Device(s): All the legacy API 21 devices
 *
 * @see androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio
 */
public class AspectRatioLegacyApi21Quirk : Quirk {
    /** Get the corrected aspect ratio. */
    @TargetAspectRatio.Ratio
    public fun getCorrectedAspectRatio(): Int {
        return TargetAspectRatio.RATIO_MAX_JPEG
    }

    public companion object {
        public fun isEnabled(cameraMetadata: CameraMetadata): Boolean =
            cameraMetadata.isHardwareLevelLegacy && Build.VERSION.SDK_INT == 21
    }
}
