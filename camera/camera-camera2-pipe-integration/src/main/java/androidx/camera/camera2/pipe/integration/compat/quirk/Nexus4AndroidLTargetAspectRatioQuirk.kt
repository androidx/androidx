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
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isGoogleDevice
import androidx.camera.camera2.pipe.integration.compat.workaround.TargetAspectRatio
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary Bug Id: b/19606058 Description: Quirk that produces stretched preview on Nexus 4
 * devices running Android L (API levels 21 and 22). There is a Camera1/HAL1 issue on the Nexus 4.
 * The preview will be stretched when configuring a JPEG that doesn't actually have the same aspect
 * ratio as the maximum JPEG resolution. Device(s): Google Nexus 4
 *
 * @see androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio
 */
public class Nexus4AndroidLTargetAspectRatioQuirk : Quirk {
    /** Get the corrected aspect ratio. */
    @TargetAspectRatio.Ratio
    public fun getCorrectedAspectRatio(): Int {
        return TargetAspectRatio.RATIO_MAX_JPEG
    }

    public companion object {
        // List of devices with the issue.
        private val DEVICE_MODELS =
            listOf(
                "NEXUS 4" // b/158749159
            )

        public fun isEnabled(): Boolean {
            return isGoogleDevice() &&
                Build.VERSION.SDK_INT < 23 &&
                DEVICE_MODELS.contains(Build.MODEL.uppercase())
        }
    }
}
